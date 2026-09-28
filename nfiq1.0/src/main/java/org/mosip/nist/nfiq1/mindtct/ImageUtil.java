package org.mosip.nist.nfiq1.mindtct;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IImageUtil;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General image manipulation utilities used by MINDTCT.
 * <p>
 * Port of NIST LFS {@code imgutil.c}. Provides bit-depth shifting (8-bit to
 * 6-bit and back), grayscale-to-binary thresholding, image padding, 1-pixel
 * hole filling in binary images, free-path testing between two points and
 * directional pixel searching. Images are stored one pixel per {@code int} in
 * row-major order.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class ImageUtil extends MindTct implements IImageUtil {
	/** SLF4J logger for this class. */
	private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static ImageUtil instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private ImageUtil() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code ImageUtil} instance
	 */
	public static synchronized ImageUtil getInstance() {
		if (instance == null) {
			synchronized (ImageUtil.class) {
				if (instance == null) {
					instance = new ImageUtil();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Defs} helper (rounding utilities).
	 *
	 * @return the {@code Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the shared {@link Line} helper (line rasterization).
	 *
	 * @return the {@code Line} singleton
	 */
	public Line getLine() {
		return Line.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper used to release buffers.
	 *
	 * @return the {@code Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link Contour} helper (edge pixel pair fixing).
	 *
	 * @return the {@code Contour} singleton
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Takes an array of unsigned 8-bit values and bitwise shifts each value two
	 * positions to the left.
	 * <p>
	 * NIST: {@code bits_6to8()}. This is equivalent to multiplying each value by
	 * 4, which puts original values on the range [0..64) onto the range
	 * [0..256); in other words the original 6-bit values now fit in 8 bits. This
	 * is used to undo the effects of {@link #bits8To6(int[], int, int)}.
	 *
	 * @param imageData   input/output: image data; on return contains the
	 *                    bit-shifted results
	 * @param imageWidth  width (in pixels) of the input array
	 * @param imageHeight height (in pixels) of the input array
	 */
	public void bits6To8(int[] imageData, int imageWidth, int imageHeight) {
		int imageSize;
		int iptrIndex;

		imageSize = imageWidth * imageHeight;
		iptrIndex = 0;
		for (int i = 0; i < imageSize; i++) {
			/* Multiply every pixel value by 4 so that [0..64) -> [0..255) */
			imageData[iptrIndex++] <<= 2;
		}
	}

	/**
	 * Takes an array of unsigned 8-bit values and bitwise shifts each value two
	 * positions to the right.
	 * <p>
	 * NIST: {@code bits_8to6()}. This is equivalent to dividing each value by 4,
	 * which puts original values on the range [0..256) onto the range [0..64);
	 * in other words the original 8-bit values now fit in 6 bits (as required by
	 * the LFS DFT/direction analysis). The NIST author noted a desire to make
	 * this dependency go away.
	 *
	 * @param imageData   input/output: image data; on return contains the
	 *                    bit-shifted results
	 * @param imageWidth  width (in pixels) of the input array
	 * @param imageHeight height (in pixels) of the input array
	 */
	public void bits8To6(int[] imageData, int imageWidth, int imageHeight) {
		int imageSize;
		int iptrIndex;

		imageSize = imageWidth * imageHeight;
		iptrIndex = 0;
		for (int i = 0; i < imageSize; i++) {
			/* Divide every pixel value by 4 so that [0..256) -> [0..64) */
			imageData[iptrIndex++] >>= 2;
		}
	}

	/**
	 * Thresholds an 8-bit image into two specified pixel values.
	 * <p>
	 * NIST: {@code gray2bin()}. Pixels in the image less than the threshold are
	 * set to the first specified pixel value, whereas pixels greater than or
	 * equal to the threshold are set to the second specified pixel value. One
	 * application for this routine is to convert binary images from 8-bit pixels
	 * valued {0,255} to {1,0} and vice versa.
	 *
	 * @param threshold         8-bit pixel threshold
	 * @param lessPixel         pixel value used when image pixel is
	 *                          {@code < threshold}
	 * @param greaterPixel      pixel value used when image pixel is
	 *                          {@code >= threshold}
	 * @param binarizedmageData input/output: 8-bit image data, altered in place
	 * @param imageWidth        width (in pixels) of the image
	 * @param imageHeight       height (in pixels) of the image
	 */
	public void grayToBinary(final int threshold, final int lessPixel, final int greaterPixel, int[] binarizedmageData,
			final int imageWidth, final int imageHeight) {
		int imageSize = imageWidth * imageHeight;
		for (int i = 0; i < imageSize; i++) {
			if (binarizedmageData[i] >= threshold) {
				binarizedmageData[i] = greaterPixel;
			} else {
				binarizedmageData[i] = lessPixel;
			}
		}
	}

	/**
	 * Copies an 8-bit grayscale image into a larger output image, centering the
	 * input image so as to add a specified amount of pixel padding along the
	 * entire perimeter of the input image.
	 * <p>
	 * NIST: {@code pad_uchar_image()}. The amount of pixel padding and the
	 * intensity of the pixel padding are specified. An alternative to padding
	 * with a constant intensity would be to copy the edge pixels of the centered
	 * image into the adjacent pad area. The padded image has dimensions
	 * {@code (imageWidth + 2*pad) x (imageHeight + 2*pad)}.
	 *
	 * @param ret         output: zero ({@link ILfs#FALSE}) on successful
	 *                    completion (negative would indicate a system error)
	 * @param ow          output: width (in pixels) of the padded image
	 * @param oh          output: height (in pixels) of the padded image
	 * @param imageData   input 8-bit grayscale image
	 * @param imageWidth  width (in pixels) of the input image
	 * @param imageHeight height (in pixels) of the input image
	 * @param pad         size of padding (in pixels) to be added on each side
	 * @param padValue    intensity of the padded area
	 * @return the newly allocated padded image
	 */
	public int[] padImage(AtomicInteger ret, AtomicInteger ow, AtomicInteger oh, int[] imageData, final int imageWidth,
			final int imageHeight, final int pad, final int padValue) {
		int[] paddedImagedata;
		int pptrIndex;
		int imageDataIndex;
		int pdataIndex;
		int paddedImageWidth;
		int paddedImageHeight;
		int pad2;
		int paddedImageSize;

		/* Account for pad on both sides of image */
		pad2 = pad << 1;

		/* Compute new pad sizes */
		paddedImageWidth = imageWidth + pad2;
		paddedImageHeight = imageHeight + pad2;
		paddedImageSize = paddedImageWidth * paddedImageHeight;

		/* Allocate padded image */
		paddedImagedata = new int[paddedImageSize];

		/* Initialize values to a constant PAD value */
		Arrays.fill(paddedImagedata, 0, paddedImageSize, padValue);

		/* Copy input image into padded image one scanline at a time */
		imageDataIndex = 0;
		pdataIndex = 0;
		pptrIndex = pdataIndex + (pad * paddedImageWidth) + pad;

		for (int i = 0; i < imageHeight; i++) {
			System.arraycopy(imageData, imageDataIndex, paddedImagedata, pptrIndex, imageWidth);
			imageDataIndex += imageWidth;
			pptrIndex += paddedImageWidth;
		}

		ow.set(paddedImageWidth);
		oh.set(paddedImageHeight);
		ret.set(ILfs.FALSE);
		return paddedImagedata;
	}

	/**
	 * Fills 1-pixel wide holes in a binary image.
	 * <p>
	 * NIST: {@code fill_holes()}. Analyzes triplets of horizontal pixels first
	 * and then triplets of vertical pixels, filling in holes of width 1. A hole
	 * is defined as the case where the two neighbouring pixels are equal AND the
	 * centre pixel is different. Each hole is filled with the value of its
	 * immediate neighbours. This routine modifies the input image.
	 *
	 * @param binarizedmageData input/output: binary image data to be processed;
	 *                          on return contains the results
	 * @param imageWidth        width (in pixels) of the binary input image
	 * @param imageHeight       height (in pixels) of the binary input image
	 */
	public void fillHoles(int[] binarizedmageData, final int imageWidth, final int imageHeight) {
		int xIndex;
		int yIndex;
		int iw2;
		int leftPixelIndex;
		int middlePixelIndex;
		int rightPixelIndex;
		int topPixelIndex;
		int bottomPixelIndex;
		int imagePixelIndex;

		/* 1. Fill 1-pixel wide holes in horizontal runs first ... */
		imagePixelIndex = (0 + 1);
		/* Foreach row in image ... */
		for (yIndex = 0; yIndex < imageHeight; yIndex++) {
			/* Initialize pointers to start of next line ... */
			leftPixelIndex = (imagePixelIndex - 1); // Left pixel
			middlePixelIndex = imagePixelIndex; // Middle pixel
			rightPixelIndex = (imagePixelIndex + 1); // Right pixel
			/* Foreach column in image (less far left and right pixels) ... */
			for (xIndex = 1; xIndex < imageWidth - 1; xIndex++) {
				/* Do we have a horizontal hole of length 1? */
				if ((binarizedmageData[leftPixelIndex] != binarizedmageData[middlePixelIndex])
						&& (binarizedmageData[leftPixelIndex] == binarizedmageData[rightPixelIndex])) {
					/* If so, then fill it. */
					binarizedmageData[middlePixelIndex] = binarizedmageData[leftPixelIndex];
					/* Bump passed right pixel because we know it will not */
					/* be a hole. */
					leftPixelIndex += 2;
					middlePixelIndex += 2;
					rightPixelIndex += 2;
					/* We bump ix once here and then the FOR bumps it again. */
					xIndex++;
				} else {
					/* Otherwise, bump to the next pixel to the right. */
					leftPixelIndex++;
					middlePixelIndex++;
					rightPixelIndex++;
				}
			}
			/* Bump to start of next row. */
			imagePixelIndex += imageWidth;
		}

		/* 2. Now, fill 1-pixel wide holes in vertical runs ... */
		iw2 = imageWidth << 1;
		/* Start processing column one row down from the top of the image. */
		imagePixelIndex = (0 + imageWidth);
		/* Foreach column in image ... */
		for (xIndex = 0; xIndex < imageWidth; xIndex++) {
			/* Initialize pointers to start of next column ... */
			topPixelIndex = (imagePixelIndex - imageWidth); // Top pixel
			middlePixelIndex = imagePixelIndex; // Middle pixel
			bottomPixelIndex = (imagePixelIndex + imageWidth); // Bottom pixel
			/* Foreach row in image (less top and bottom row) ... */
			for (yIndex = 1; yIndex < imageHeight - 1; yIndex++) {
				/* Do we have a vertical hole of length 1? */
				if ((binarizedmageData[topPixelIndex] != binarizedmageData[middlePixelIndex])
						&& (binarizedmageData[topPixelIndex] == binarizedmageData[bottomPixelIndex])) {
					/* If so, then fill it. */
					binarizedmageData[middlePixelIndex] = binarizedmageData[topPixelIndex];
					/* Bump passed bottom pixel because we know it will not */
					/* be a hole. */
					topPixelIndex += iw2;
					middlePixelIndex += iw2;
					bottomPixelIndex += iw2;
					/* We bump iy once here and then the FOR bumps it again. */
					yIndex++;
				} else {
					/* Otherwise, bump to the next pixel below. */
					topPixelIndex += imageWidth;
					middlePixelIndex += imageWidth;
					bottomPixelIndex += imageWidth;
				}
			}
			/* Bump to start of next column. */
			imagePixelIndex++;
		}
	}

	/**
	 * Traverses a straight line between two pixel points in an image and
	 * determines whether a "free path" exists between them.
	 * <p>
	 * NIST: {@code free_path()}. Counts the number of pixel value transitions
	 * between adjacent pixels along the trajectory (computed with
	 * {@link Line#linePoints}); the path is free if the count does not exceed
	 * {@link LfsParams#getMaxTrans()}.
	 *
	 * @param x1                x-pixel coord of first point
	 * @param y1                y-pixel coord of first point
	 * @param x2                x-pixel coord of second point
	 * @param y2                y-pixel coord of second point
	 * @param binarizedmageData binary image data (0 = white, 1 = black)
	 * @param imageWidth        width (in pixels) of image
	 * @param imageHeight       height (in pixels) of image
	 * @param lfsParams         parameters and thresholds for controlling LFS
	 * @return {@link ILfs#TRUE} if a free path is determined to exist;
	 *         {@link ILfs#FALSE} if a free path is determined not to exist;
	 *         negative on system error (propagated from line computation)
	 */
	public int freePath(final int x1, final int y1, final int x2, final int y2, int[] binarizedmageData,
			final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		int[] xList;
		int[] yList;
		AtomicInteger num = new AtomicInteger(0);
		int ret;
		int i;
		int trans;
		int preval;
		int nextval;
		int asize;
		/* Compute maximum number of points needed to hold line segment. */
		asize = Math.max(Math.abs(x2 - x1) + 2, Math.abs(y2 - y1) + 2);
		xList = new int[asize];
		yList = new int[asize];

		/* Compute points along line segment between the two points. */
		if ((ret = getLine().linePoints(xList, yList, num, x1, y1, x2, y2)) != ILfs.FALSE) {
			return (ret);
		}

		/* Intialize the number of transitions to 0. */
		trans = 0;
		/* Get the pixel value of first point along line segment. */
		preval = binarizedmageData[0 + (y1 * imageWidth) + x1];

		/* Foreach remaining point along line segment ... */
		for (i = 1; i < num.get(); i++) {
			/* Get pixel value of next point along line segment. */
			nextval = binarizedmageData[0 + (yList[i] * imageWidth) + xList[i]];

			/* If next pixel value different from previous pixel value ... */
			if (nextval != preval) {
				/* Then we have detected a transition, so bump counter. */
				trans++;
				/* If number of transitions seen > than threshold (ex. 2) ... */
				if (trans > lfsParams.getMaxTrans()) {
					/* Deallocate the line segment's coordinate lists. */
					getFree().free(xList);
					getFree().free(yList);
					/* Return free path to be FALSE. */
					return (ILfs.FALSE);
				}
				/* Otherwise, maximum number of transitions not yet exceeded. */
				/* Assign the next pixel value to the previous pixel value. */
				preval = nextval;
			}
			/* Otherwise, no transition detected this interation. */
		}

		/* If we get here we did not exceed the maximum allowable number */
		/* of transitions. So, deallocate the line segment's coordinate lists. */
		getFree().free(xList);
		getFree().free(yList);

		/* Return free path to be TRUE. */
		return (ILfs.TRUE);
	}

	/**
	 * Takes a specified maximum number of steps in a specified direction looking
	 * for the first occurrence of a pixel with a specified value.
	 * <p>
	 * NIST: {@code search_in_direction()}. Once found, adjustments are
	 * potentially made (via {@link Contour#fixEdgePixelPair}) to make sure the
	 * resulting pixel and its associated edge pixel are 4-connected, so the pair
	 * can be used for contour tracing. If the pixel is not found, or the search
	 * steps outside the image, all outputs are set to -1.
	 *
	 * @param ox                output: x coord of located pixel
	 * @param oy                output: y coord of located pixel
	 * @param oex               output: x coord of associated edge pixel
	 * @param oey               output: y coord of associated edge pixel
	 * @param pix               value of pixel to be searched for
	 * @param startX            x-pixel coord to start search
	 * @param startY            y-pixel coord to start search
	 * @param deltaX            increment in x for each step
	 * @param deltaY            increment in y for each step
	 * @param maxsteps          maximum number of steps to conduct search
	 * @param binarizedmageData binary image data (0 = white, 1 = black)
	 * @param imageWidth        width (in pixels) of image
	 * @param imageHeight       height (in pixels) of image
	 * @return {@link ILfs#TRUE} if a pixel of the specified value was found;
	 *         {@link ILfs#FALSE} if it was NOT found
	 */
	public int searchInDirection(AtomicInteger ox, AtomicInteger oy, AtomicInteger oex, AtomicInteger oey,
			final int pix, final int startX, final int startY, final double deltaX, final double deltaY,
			final int maxsteps, int[] binarizedmageData, final int imageWidth, final int imageHeight) {
		int i;
		AtomicInteger x = new AtomicInteger(0);
		AtomicInteger y = new AtomicInteger(0);
		AtomicInteger px = new AtomicInteger(0);
		AtomicInteger py = new AtomicInteger(0);
		double fx;
		double fy;

		/* Set previous point to starting point. */
		px.set(startX);
		py.set(startY);
		/* Set floating point accumulators to starting point. */
		fx = startX;
		fy = startY;

		/* Foreach step up to the specified maximum ... */
		for (i = 0; i < maxsteps; i++) {
			/* Increment accumulators. */
			fx += deltaX;
			fy += deltaY;
			/* Round to get next step. */
			x.set(getDefs().sRound(fx));
			y.set(getDefs().sRound(fy));

			/* If we stepped outside the image boundaries ... */
			if ((x.get() < 0) || (x.get() >= imageWidth) || (y.get() < 0) || (y.get() >= imageHeight)) {
				/* Return FALSE (we did not find what we were looking for). */
				ox.set(-1);
				oy.set(-1);
				oex.set(-1);
				oey.set(-1);
				return (ILfs.FALSE);
			}

			/* Otherwise, test to see if we found our pixel with value 'pix'. */
			if (binarizedmageData[0 + (y.get() * imageWidth) + x.get()] == pix) {
				/* The previous and current pixels form a feature, edge pixel */
				/* pair, which we would like to use for edge following. The */
				/* previous pixel may be a diagonal neighbor however to the */
				/* current pixel, in which case the pair could not be used by */
				/* the contour tracing (which requires the edge pixel in the */
				/* pair neighbor to the N,S,E or W. */
				/* This routine adjusts the pair so that the results may be */
				/* used by the contour tracing. */
				getContour().fixEdgePixelPair(x, y, px, py, binarizedmageData, imageWidth, imageHeight);

				/* Return TRUE (we found what we were looking for). */
				ox.set(x.get());
				oy.set(y.get());
				oex.set(px.get());
				oey.set(py.get());
				return (ILfs.TRUE);
			}

			/* Otherwise, still haven't found pixel with desired value, */
			/* so set current point to previous and take another step. */
			px.set(x.get());
			py.set(y.get());
		}

		/* Return FALSE (we did not find what we were looking for). */
		ox.set(-1);
		oy.set(-1);
		oex.set(-1);
		oey.set(-1);
		return (ILfs.FALSE);
	}
}