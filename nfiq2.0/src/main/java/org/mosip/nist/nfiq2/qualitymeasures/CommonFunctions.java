package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;

/**
 * Helpers shared by the quality modules (NIST {@code quality_modules/common_functions.cpp}).
 * <p>
 * Blocks are addressed inside a row-major 8-bit image by their top-left corner and size.
 */
public final class CommonFunctions {
	/** Side of the local analysis square in pixels ({@code NFIQ2::Sizes::LocalRegionSquare}). */
	public static final int LOCAL_REGION_SQUARE = 32;
	/** Suffix of histogram mean features. */
	static final String MEAN_SUFFIX = "Mean";
	/** Suffix of histogram standard-deviation features. */
	static final String STDDEV_SUFFIX = "StdDev";
	/** Number of histogram bins. */
	static final int BIN_COUNT = 10;
	/** {@code DBL_EPSILON}. */
	private static final double DBL_EPSILON = Math.ulp(1.0);
	/** Scale of the 10-decimal rounding applied to the ridge/valley regression. */
	private static final double REGRESSION_ROUNDING = 10000000000.0;

	/** Static helpers only. */
	private CommonFunctions() {
	}

	/**
	 * Ridge/valley classification of a block: per-column flags and linear trend.
	 *
	 * @param ridval 1 where the column mean is below the trend (ridge), else 0
	 * @param dt     trend value per column
	 */
	record RidgeValley(int[] ridval, double[] dt) {
	}

	/**
	 * {@code ridgesegment}: normalises the image to zero mean and unit deviation, then marks every
	 * {@code blksze x blksze} block whose standard deviation exceeds {@code thresh}.
	 *
	 * @param img    pixels
	 * @param w      width
	 * @param h      height
	 * @param blksze block size
	 * @param thresh deviation threshold
	 * @return per-pixel mask (true = ridge region)
	 */
	static boolean[] ridgeSegment(byte[] img, int w, int h, int blksze, double thresh) {
		int n = w * h;
		double[] norm = new double[n];
		double[] ms = Cv.meanStdDev(img, n);
		double alpha = 1.0 / ms[1];
		double beta = -ms[0] * alpha;
		for (int i = 0; i < n; i++) {
			norm[i] = Math.fma(img[i] & 0xFF, alpha, beta);
		}
		boolean[] mask = new boolean[n];
		double[] stds = Cv.blockStdDev(norm, w, h, blksze);
		int bw = (w + blksze - 1) / blksze;
		for (int r = 0; r < h; r += blksze) {
			int rEnd = Math.min(r + blksze, h);
			for (int c = 0; c < w; c += blksze) {
				int cEnd = Math.min(c + blksze, w);
				boolean on = stds[(r / blksze) * bw + c / blksze] > thresh;
				if (on) {
					for (int y = r; y < rEnd; y++) {
						Arrays.fill(mask, y * w + c, y * w + cEnd, true);
					}
				}
			}
		}
		return mask;
	}

	/**
	 * {@code allfun}: whether every mask pixel of a block is set.
	 *
	 * @param mask per-pixel mask
	 * @param w    mask width
	 * @param x0   left
	 * @param y0   top
	 * @param bw   block width
	 * @param bh   block height
	 * @return true when all are set
	 */
	static boolean allfun(boolean[] mask, int w, int x0, int y0, int bw, int bh) {
		for (int y = y0; y < y0 + bh; y++) {
			for (int x = x0; x < x0 + bw; x++) {
				if (!mask[y * w + x]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * {@code covcoef} with centred differences: means of {@code fx^2}, {@code fy^2} and {@code fx*fy} over
	 * a block.
	 *
	 * @param img pixels
	 * @param w   image width
	 * @param x0  left
	 * @param y0  top
	 * @param bw  block width (at least 2)
	 * @param bh  block height (at least 2)
	 * @return {@code {a, b, c}}
	 */
	static double[] covcoef(byte[] img, int w, int x0, int y0, int bw, int bh) {
		double[] fx2 = new double[bw * bh];
		double[] fy2 = new double[bw * bh];
		double[] fxy = new double[bw * bh];
		for (int r = 0; r < bh; r++) {
			for (int c = 0; c < bw; c++) {
				double fx = diff(img, w, x0, y0, r, c, bw, true);
				double fy = diff(img, w, x0, y0, r, c, bh, false);
				int i = r * bw + c;
				fx2[i] = fx * fx;
				fy2[i] = fy * fy;
				fxy[i] = fx * fy;
			}
		}
		int n = bw * bh;
		return new double[] { Cv.mean(fx2, 0, n), Cv.mean(fy2, 0, n), Cv.mean(fxy, 0, n) };
	}

	/**
	 * {@code diffGrad}: forward differences at the edges, central differences inside.
	 *
	 * @param img        pixels
	 * @param w          image width
	 * @param x0         block left
	 * @param y0         block top
	 * @param r          row inside the block
	 * @param c          column inside the block
	 * @param len        block extent along the differentiation axis
	 * @param horizontal true to differentiate along the row, false along the column
	 * @return gradient
	 */
	private static double diff(byte[] img, int w, int x0, int y0, int r, int c, int len, boolean horizontal) {
		int pos = horizontal ? c : r;
		int lo = pos == 0 ? 0 : (pos == len - 1 ? len - 2 : pos - 1);
		int hi = pos == 0 ? 1 : (pos == len - 1 ? len - 1 : pos + 1);
		int a = horizontal ? px(img, w, x0 + hi, y0 + r) : px(img, w, x0 + c, y0 + hi);
		int b = horizontal ? px(img, w, x0 + lo, y0 + r) : px(img, w, x0 + c, y0 + lo);
		double d = (double) a - b;
		return pos == 0 || pos == len - 1 ? d : d / 2.0;
	}

	/**
	 * Unsigned pixel value.
	 *
	 * @param img pixels
	 * @param w   width
	 * @param x   column
	 * @param y   row
	 * @return 0..255
	 */
	static int px(byte[] img, int w, int x, int y) {
		return img[y * w + x] & 0xFF;
	}

	/**
	 * {@code ridgeorient}: angle of the line perpendicular to the ridge flow.
	 *
	 * @param a covariance {@code a}
	 * @param b covariance {@code b}
	 * @param c covariance {@code c}
	 * @return angle in radians
	 */
	static double ridgeOrient(double a, double b, double c) {
		double temp = a - b;
		double denom = (c * c + temp * temp) + DBL_EPSILON;
		double sin2theta = c / denom;
		double cos2theta = temp / denom;
		return StrictMath.atan2(sin2theta, cos2theta) / 2;
	}

	/**
	 * {@code getRotatedBlock} of the square window at {@code (x0, y0)} of an image.
	 * <p>
	 * The NIST window is a region of the whole image, so its 2-pixel {@code copyMakeBorder} padding takes the
	 * neighbouring image pixels (OpenCV does that for sub-matrices unless {@code BORDER_ISOLATED} is given)
	 * and is 0 only outside the image.
	 *
	 * @param img         pixels
	 * @param w           image width
	 * @param h           image height
	 * @param x0          window left
	 * @param y0          window top
	 * @param size        window side
	 * @param orientation angle in radians
	 * @param pad         add the 2-pixel border before rotating
	 * @return rotated window, {@code size x size}
	 * @throws Nfiq2Exception when {@code size} is odd
	 */
	static byte[] rotatedWindow(byte[] img, int w, int h, int x0, int y0, int size, double orientation,
			boolean pad) {
		checkEvenBlock(size);
		int border = pad ? 2 : 0;
		int in = size + 2 * border;
		byte[] src = new byte[in * in];
		for (int y = 0; y < in; y++) {
			int sy = y0 - border + y;
			if (sy < 0 || sy >= h) {
				continue;
			}
			int sx0 = Math.max(x0 - border, 0);
			int sx1 = Math.min(x0 - border + in, w);
			if (sx1 > sx0) {
				System.arraycopy(img, sy * w + sx0, src, y * in + (sx0 - (x0 - border)), sx1 - sx0);
			}
		}
		return Cv.rotateBlock(src, in, size, orientation);
	}

	/**
	 * Block-size sanity check of the NIST block functions.
	 *
	 * @param rows block rows
	 * @throws Nfiq2Exception when {@code rows} is odd
	 */
	static void checkEvenBlock(int rows) {
		if (rows % 2 != 0) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Wrong block size! Consider block with size of even number (block rows = " + rows + ')');
		}
	}

	/**
	 * {@code getRidgeValleyStructure}: column means, least-squares linear trend (rounded to 10 decimals),
	 * and ridge flags ({@code mean < trend}).
	 *
	 * @param blk  pixels of the enclosing block
	 * @param w    width of {@code blk}
	 * @param x0   left of the region
	 * @param y0   top of the region
	 * @param cols region width
	 * @param rows region height
	 * @return flags and trend
	 */
	static RidgeValley ridgeValleyStructure(byte[] blk, int w, int x0, int y0, int cols, int rows) {
		double[] v3 = new double[cols];
		for (int i = 0; i < cols; i++) {
			v3[i] = Cv.mean(blk, w, x0 + i, y0, 1, rows);
		}
		double[] a = new double[cols * 2];
		double[] b = v3.clone();
		for (int i = 0; i < cols; i++) {
			a[i * 2] = 1;
			a[i * 2 + 1] = i + 1.0;
		}
		double[] dt1 = Cv.solveQr(a, cols, 2, b);
		for (int k = 0; k < 2; k++) {
			dt1[k] = roundHalfAway(dt1[k] * REGRESSION_ROUNDING) / REGRESSION_ROUNDING;
		}
		double[] dt = new double[cols];
		int[] ridval = new int[cols];
		for (int i = 0; i < cols; i++) {
			dt[i] = (i + 1.0) * dt1[1] + dt1[0];
			ridval[i] = v3[i] < dt[i] ? 1 : 0;
		}
		return new RidgeValley(ridval, dt);
	}

	/**
	 * C {@code round()}: nearest integer, halves away from zero.
	 *
	 * @param x value
	 * @return rounded value
	 */
	static double roundHalfAway(double x) {
		if (Double.isNaN(x) || Double.isInfinite(x)) {
			return x;
		}
		double t = x < 0 ? Math.ceil(x) : Math.floor(x);
		if (Math.abs(x - t) >= 0.5) {
			t += Math.copySign(1.0, x);
		}
		return t;
	}

	/**
	 * Numerical gradients of a block ({@code computeNumericalGradients}): forward differences at the
	 * edges, central differences inside.
	 *
	 * @param img pixels
	 * @param w   image width
	 * @param x0  left
	 * @param y0  top
	 * @param bw  block width
	 * @param bh  block height
	 * @return {@code {gx, gy}}, each row-major {@code bh x bw}; an axis shorter than 2 pixels gives zeros
	 */
	static double[][] numericalGradients(byte[] img, int w, int x0, int y0, int bw, int bh) {
		double[] gx = new double[bw * bh];
		double[] gy = new double[bw * bh];
		for (int r = 0; r < bh; r++) {
			for (int c = 0; c < bw; c++) {
				int i = r * bw + c;
				gx[i] = bw > 1 ? diff(img, w, x0, y0, r, c, bw, true) : 0;
				gy[i] = bh > 1 ? diff(img, w, x0, y0, r, c, bh, false) : 0;
			}
		}
		return new double[][] { gx, gy };
	}

	/**
	 * {@code addHistogramFeatures}: counts of the sorted values per bin (bounds plus +infinity), then the
	 * mean and standard deviation of the values.
	 *
	 * @param prefix feature prefix, for example {@code "FDA_Bin10_"}
	 * @param bounds nine inner bin boundaries
	 * @param data   values
	 * @return features {@code prefix0 .. prefix9}, {@code prefixMean}, {@code prefixStdDev}
	 * @throws Nfiq2Exception when there are no values ({@code cv::meanStdDev} rejects empty input)
	 */
	static Map<String, Double> histogramFeatures(String prefix, double[] bounds, List<Double> data) {
		if (bounds.length + 1 != BIN_COUNT) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError, "Wrong histogram bin count for "
					+ prefix + ". Should be " + BIN_COUNT + " but is " + (bounds.length + 1));
		}
		if (data.isEmpty()) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Cannot compute histogram " + prefix + ": no data");
		}
		double[] limits = Arrays.copyOf(bounds, BIN_COUNT);
		limits[BIN_COUNT - 1] = Double.POSITIVE_INFINITY;
		double[] values = new double[data.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = data.get(i);
		}
		Arrays.sort(values);
		int[] bins = new int[BIN_COUNT];
		int bucket = 0;
		double bound = limits[0];
		for (double v : values) {
			while (!Double.isInfinite(v) && v >= bound) {
				bucket++;
				bound = limits[bucket];
			}
			bins[bucket]++;
		}
		Map<String, Double> out = new LinkedHashMap<>();
		for (int i = 0; i < BIN_COUNT; i++) {
			out.put(prefix + i, (double) bins[i]);
		}
		double[] ms = Cv.meanStdDev(values, 0, values.length);
		out.put(prefix + MEAN_SUFFIX, ms[0]);
		out.put(prefix + STDDEV_SUFFIX, ms[1]);
		return out;
	}

	/**
	 * Logistic function {@code 1 / (1 + exp((ip - v) / s))}.
	 *
	 * @param nativeQuality   value
	 * @param inflectionPoint inflection point
	 * @param scaling         scale
	 * @return sigmoid
	 */
	static double sigmoid(double nativeQuality, double inflectionPoint, double scaling) {
		return 1.0 / (1 + StrictMath.exp((inflectionPoint - nativeQuality) / scaling));
	}

	/**
	 * Maps a value of known range to 0..100 ({@code floor(101 * (v - min) / (max - min + eps))}), with the
	 * {@code uint8_t} conversion of the C++ code.
	 *
	 * @param nativeQuality value
	 * @param min           range minimum
	 * @param max           range maximum
	 * @return quality block value
	 */
	static int knownRange(double nativeQuality, double min, double max) {
		return toUint8(Math.floor(101 * ((nativeQuality - min) / (max - min + DBL_EPSILON))));
	}

	/**
	 * {@code getQualityBlockValue}: maps a native quality measure to the {@code 0..100} value stored in an
	 * ISO/IEC 29794-1 quality block; 255 when the measure has no defined mapping.
	 *
	 * @param featureIdentifier native quality measure identifier
	 * @param nativeQuality     native value
	 * @return quality block value
	 */
	public static int getQualityBlockValue(String featureIdentifier, double nativeQuality) {
		switch (featureIdentifier) {
		case Mu.IMAGE_MEAN, Mu.MEAN_OF_BLOCK_MEANS, ImgProcROI.MEAN:
			return knownRange(nativeQuality, 0, 255);
		case FingerJetFX.COUNT, FingerJetFX.COUNT_COM:
			return toUint8(Math.min(nativeQuality, 100.0));
		case FJFXMinutiaeQuality.PERCENT_ORIENTATION_CERTAINTY_80, QualityMap.COHERENCE_MEAN,
				FJFXMinutiaeQuality.PERCENT_IMAGE_MEAN_50, OCLHistogram.MEAN, LCS.MEAN, FDA.MEAN, OCLHistogram.STDDEV,
				LCS.STDDEV, FDA.STDDEV, OF.STDDEV:
			return knownRange(nativeQuality, 0, 1);
		case QualityMap.COHERENCE_SUM:
			return knownRange(nativeQuality, 0, 3150);
		case OF.MEAN: {
			double deg2Rad = Math.PI / 180.0;
			double thetaMin = OF.ANGLE_MIN * deg2Rad;
			double denominator = (90.0 * deg2Rad) - thetaMin;
			double min = ((0.0 * deg2Rad) - thetaMin) / denominator;
			double max = ((180.0 * deg2Rad) - thetaMin) / denominator;
			return knownRange(nativeQuality, min, max);
		}
		case RVUPHistogram.MEAN, RVUPHistogram.STDDEV:
			return toUint8(Math.floor(100 * sigmoid(nativeQuality, 1, 0.5) + 0.5));
		default:
			return 0xFF;
		}
	}

	/**
	 * Conversion of a double to {@code uint8_t} as compiled on x86-64 (truncate to int, keep the low byte).
	 *
	 * @param v value
	 * @return 0..255
	 */
	static int toUint8(double v) {
		return ((int) v) & 0xFF;
	}
}
