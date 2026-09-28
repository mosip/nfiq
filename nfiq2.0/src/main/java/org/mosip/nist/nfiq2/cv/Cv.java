package org.mosip.nist.nfiq2.cv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * The OpenCV operations NFIQ 2 relies on, over row-major {@code byte[]} / {@code double[]} buffers.
 * <p>
 * Image processing (blur, erosion, Otsu, contours, flood fill, rotation, QR solve, DFT) calls OpenCV
 * ({@code org.openpnp:opencv}) with the same arguments as the NIST C++, and so do the reductions
 * ({@code Core.mean}, {@code Core.sumElems}, {@code Core.meanStdDev}).
 * <p>
 * Images are 8-bit greyscale with {@code stride == width}. Stateless and thread-safe.
 */
public final class Cv {
	static {
		nu.pattern.OpenCV.loadLocally();
	}

	/** Radians to degrees, as in NIST {@code getRotatedBlock}. */
	private static final double RAD_TO_DEG = 180.0 / Math.PI;

	/** Static helpers only. */
	private Cv() {
	}

	/**
	 * {@code cv::mean} of a rectangle of an 8-bit image: exact integer sum times {@code 1.0 / n}.
	 *
	 * @param img    pixels
	 * @param stride row length of {@code img}
	 * @param x0     left
	 * @param y0     top
	 * @param w      width
	 * @param h      height
	 * @return mean grey value, 0 for an empty rectangle
	 */
	public static double mean(byte[] img, int stride, int x0, int y0, int w, int h) {
		if (w * h == 0) {
			return 0.0;
		}
		byte[] rect = new byte[w * h];
		for (int y = 0; y < h; y++) {
			System.arraycopy(img, (y0 + y) * stride + x0, rect, y * w, w);
		}
		Mat m = toMat(rect, w, h);
		try {
			return Core.mean(m).val[0];
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::meanStdDev} of the first {@code n} pixels of an 8-bit image.
	 *
	 * @param img pixels
	 * @param n   number of pixels to use
	 * @return {@code {mean, stddev}}, zeros when {@code n == 0}
	 */
	public static double[] meanStdDev(byte[] img, int n) {
		if (n == 0) {
			return new double[2];
		}
		Mat m = toMat(img, n, 1);
		try {
			return meanStdDev(m);
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::meanStdDev} of a contiguous run of doubles.
	 *
	 * @param v   values
	 * @param off first index
	 * @param len number of values
	 * @return {@code {mean, stddev}}, zeros when {@code len == 0}
	 */
	public static double[] meanStdDev(double[] v, int off, int len) {
		if (len == 0) {
			return new double[2];
		}
		Mat m = toMat(v, off, len, 1);
		try {
			return meanStdDev(m);
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::meanStdDev(...)[1]} of every {@code blk x blk} block of a double image, blocks clipped at
	 * the right and bottom edges.
	 *
	 * @param img values, row-major
	 * @param w   width
	 * @param h   height
	 * @param blk block size
	 * @return standard deviations, row-major over the {@code ceil(h/blk) x ceil(w/blk)} block grid
	 */
	public static double[] blockStdDev(double[] img, int w, int h, int blk) {
		int bw = (w + blk - 1) / blk;
		int bh = (h + blk - 1) / blk;
		double[] out = new double[bw * bh];
		Mat m = toMat(img, 0, w, h);
		try {
			for (int by = 0; by < bh; by++) {
				for (int bx = 0; bx < bw; bx++) {
					int x = bx * blk;
					int y = by * blk;
					Mat sub = m.submat(new Rect(x, y, Math.min(blk, w - x), Math.min(blk, h - y)));
					try {
						out[by * bw + bx] = meanStdDev(sub)[1];
					} finally {
						sub.release();
					}
				}
			}
			return out;
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::sum} of a contiguous run of doubles.
	 *
	 * @param v   values
	 * @param off first index
	 * @param len number of values
	 * @return sum, 0 when {@code len == 0}
	 */
	public static double sum(double[] v, int off, int len) {
		if (len == 0) {
			return 0.0;
		}
		Mat m = toMat(v, off, len, 1);
		try {
			return Core.sumElems(m).val[0];
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::mean} of a contiguous run of doubles.
	 *
	 * @param v   values
	 * @param off first index
	 * @param len number of values
	 * @return mean, 0 when {@code len == 0}
	 */
	public static double mean(double[] v, int off, int len) {
		if (len == 0) {
			return 0.0;
		}
		Mat m = toMat(v, off, len, 1);
		try {
			return Core.mean(m).val[0];
		} finally {
			m.release();
		}
	}

	/**
	 * {@code cv::meanStdDev} of a single-channel matrix.
	 *
	 * @param m matrix
	 * @return {@code {mean, stddev}}
	 */
	private static double[] meanStdDev(Mat m) {
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble std = new MatOfDouble();
		try {
			Core.meanStdDev(m, mean, std);
			return new double[] { mean.get(0, 0)[0], std.get(0, 0)[0] };
		} finally {
			mean.release();
			std.release();
		}
	}

	/**
	 * {@code cv::GaussianBlur(src, dst, Size(k, k), 0)} on an 8-bit image.
	 *
	 * @param src   pixels
	 * @param w     width
	 * @param h     height
	 * @param ksize odd kernel size (41 or 91 in NFIQ 2)
	 * @return blurred image
	 */
	public static byte[] gaussianBlur(byte[] src, int w, int h, int ksize) {
		Mat in = toMat(src, w, h);
		Mat out = new Mat();
		try {
			Imgproc.GaussianBlur(in, out, new Size(ksize, ksize), 0.0);
			return toBytes(out, w * h);
		} finally {
			in.release();
			out.release();
		}
	}

	/**
	 * {@code cv::erode} with a {@code k x k} rectangle of ones and the default anchor, iterations and border.
	 *
	 * @param src pixels
	 * @param w   width
	 * @param h   height
	 * @param k   window size
	 * @return eroded image
	 */
	public static byte[] erode(byte[] src, int w, int h, int k) {
		Mat in = toMat(src, w, h);
		Mat element = Mat.ones(k, k, CvType.CV_8U);
		Mat out = new Mat();
		try {
			Imgproc.erode(in, out, element);
			return toBytes(out, w * h);
		} finally {
			in.release();
			element.release();
			out.release();
		}
	}

	/**
	 * {@code cv::threshold(src, dst, 0, 255, THRESH_OTSU)}: Otsu's threshold followed by
	 * {@code THRESH_BINARY}.
	 *
	 * @param src pixels
	 * @param n   number of pixels
	 * @return binarised image
	 */
	public static byte[] thresholdOtsu(byte[] src, int n) {
		Mat in = toMat(src, n, 1);
		Mat out = new Mat();
		try {
			Imgproc.threshold(in, out, 0, 255, Imgproc.THRESH_OTSU);
			return toBytes(out, n);
		} finally {
			in.release();
			out.release();
		}
	}

	/**
	 * Fills the white holes of the black foreground of a binary image, in place, as NIST {@code computeROI}
	 * does: {@code findContours(~img, RETR_CCOMP, CHAIN_APPROX_SIMPLE)}, then a filled black
	 * {@code drawContours} of every contour that has a parent.
	 *
	 * @param img binary pixels (0 or 255), modified in place
	 * @param w   width
	 * @param h   height
	 */
	public static void fillHoles(byte[] img, int w, int h) {
		Mat mat = toMat(img, w, h);
		Mat inverted = new Mat();
		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<>();
		try {
			Core.bitwise_not(mat, inverted);
			Imgproc.findContours(inverted, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE,
					new Point(0, 0));
			for (int i = 0; i < contours.size(); i++) {
				if (hierarchy.get(0, i)[3] != -1) {
					Imgproc.drawContours(mat, contours, i, new Scalar(0), Imgproc.FILLED, Imgproc.LINE_8, hierarchy);
				}
			}
			mat.get(0, 0, img);
		} finally {
			mat.release();
			inverted.release();
			hierarchy.release();
			contours.forEach(Mat::release);
		}
	}

	/**
	 * {@code cv::floodFill(img, seed, Scalar(newVal), &rect)}: 4-connectivity, zero tolerance, in place.
	 *
	 * @param img    pixels, modified in place
	 * @param w      width
	 * @param h      height
	 * @param seedX  seed column
	 * @param seedY  seed row
	 * @param newVal new grey value
	 * @return bounding rectangle {@code {x, y, width, height}} of the repainted region
	 */
	public static int[] floodFill(byte[] img, int w, int h, int seedX, int seedY, int newVal) {
		Mat mat = toMat(img, w, h);
		Mat mask = new Mat();
		Rect rect = new Rect();
		try {
			Imgproc.floodFill(mat, mask, new Point(seedX, seedY), new Scalar(newVal, newVal, newVal, 0), rect);
			mat.get(0, 0, img);
			return new int[] { rect.x, rect.y, rect.width, rect.height };
		} finally {
			mat.release();
			mask.release();
		}
	}

	/**
	 * The rotation step of NIST {@code getRotatedBlock}: {@code getRotationMatrix2D} about
	 * {@code Point2f(in / 2f, in / 2f)}, then {@code warpAffine(INTER_NEAREST)} into a {@code size x size}
	 * block with a constant 0 border.
	 *
	 * @param src         square input, {@code in x in} (already padded when NIST pads)
	 * @param in          input side
	 * @param size        output side
	 * @param orientation rotation angle in radians (counter-clockwise)
	 * @return rotated block, {@code size x size}
	 */
	public static byte[] rotateBlock(byte[] src, int in, int size, double orientation) {
		Mat block = toMat(src, in, in);
		float center = in / 2.0f;
		Mat rot = Imgproc.getRotationMatrix2D(new Point(center, center), orientation * RAD_TO_DEG, 1);
		Mat out = new Mat(size, size, CvType.CV_8UC1);
		try {
			Imgproc.warpAffine(block, out, rot, new Size(size, size), Imgproc.INTER_NEAREST);
			return toBytes(out, size * size);
		} finally {
			block.release();
			rot.release();
			out.release();
		}
	}

	/**
	 * {@code cv::solve(A, b, x, DECOMP_QR)} for an over-determined system with one right-hand side.
	 *
	 * @param a row-major {@code m x n} matrix
	 * @param m rows
	 * @param n columns ({@code m >= n})
	 * @param b right-hand side of length {@code m}
	 * @return least-squares solution of length {@code n} (zeros when {@code A} is singular, as in OpenCV)
	 */
	public static double[] solveQr(double[] a, int m, int n, double[] b) {
		Mat am = new Mat(m, n, CvType.CV_64F);
		Mat bm = new Mat(m, 1, CvType.CV_64F);
		Mat x = new Mat();
		try {
			am.put(0, 0, a);
			bm.put(0, 0, b);
			Core.solve(am, bm, x, Core.DECOMP_QR);
			double[] out = new double[n];
			x.get(0, 0, out);
			return out;
		} finally {
			am.release();
			bm.release();
			x.release();
		}
	}

	/**
	 * Magnitude spectrum of a real row as in NIST {@code FDA}: zero-padding to {@code getOptimalDFTSize},
	 * {@code dft(DFT_COMPLEX_OUTPUT | DFT_ROWS)} of the complex row, then {@code magnitude}.
	 *
	 * @param re input samples
	 * @return {@code |X[k]|} for {@code k = 0 .. getOptimalDFTSize(re.length) - 1}
	 */
	public static double[] dftMagnitude(double[] re) {
		int n = Core.getOptimalDFTSize(re.length);
		Mat real = Mat.zeros(1, n, CvType.CV_64F);
		Mat imag = Mat.zeros(1, n, CvType.CV_64F);
		Mat complex = new Mat();
		Mat mag = new Mat();
		List<Mat> planes = new ArrayList<>(List.of(real, imag));
		try {
			real.put(0, 0, re);
			Core.merge(planes, complex);
			Core.dft(complex, complex, Core.DFT_COMPLEX_OUTPUT | Core.DFT_ROWS);
			planes.clear();
			Core.split(complex, planes);
			Core.magnitude(planes.get(0), planes.get(1), mag);
			double[] out = new double[n];
			mag.get(0, 0, out);
			return out;
		} finally {
			real.release();
			imag.release();
			complex.release();
			mag.release();
			planes.forEach(Mat::release);
		}
	}

	/**
	 * Copies pixels into a new {@code h x w} 8-bit matrix.
	 *
	 * @param data pixels
	 * @param w    width
	 * @param h    height
	 * @return matrix, to be released by the caller
	 */
	private static Mat toMat(byte[] data, int w, int h) {
		Mat mat = new Mat(h, w, CvType.CV_8UC1);
		mat.put(0, 0, data, 0, w * h);
		return mat;
	}

	/**
	 * Copies {@code w * h} doubles starting at {@code off} into a new {@code h x w} double matrix.
	 *
	 * @param data values
	 * @param off  first index
	 * @param w    width
	 * @param h    height
	 * @return matrix, to be released by the caller
	 */
	private static Mat toMat(double[] data, int off, int w, int h) {
		Mat mat = new Mat(h, w, CvType.CV_64FC1);
		int n = w * h;
		mat.put(0, 0, off == 0 && data.length == n ? data : Arrays.copyOfRange(data, off, off + n));
		return mat;
	}

	/**
	 * Copies the pixels of a continuous 8-bit matrix.
	 *
	 * @param mat matrix
	 * @param n   number of pixels
	 * @return pixels
	 */
	private static byte[] toBytes(Mat mat, int n) {
		byte[] out = new byte[n];
		mat.get(0, 0, out);
		return out;
	}
}
