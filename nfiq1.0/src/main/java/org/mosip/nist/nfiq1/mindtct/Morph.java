package org.mosip.nist.nfiq1.mindtct;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IMorph;

/**
 * Binary morphology operations (erosion and dilation) used by MINDTCT.
 * <p>
 * Port of NIST LFS {@code morph.c}. The routines operate on binary images
 * stored one pixel per {@code int} (row-major, {@link ILfs#TRUE} = set pixel,
 * {@link ILfs#FALSE} = clear pixel) using the 4-connected neighbourhood, and
 * are used, for example, to clean up the low-quality/low-flow block maps.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class Morph extends MindTct implements IMorph {
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Morph instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Morph() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Morph} instance
	 */
	public static synchronized Morph getInstance() {
		if (instance == null) {
			instance = new Morph();
		}
		return instance;
	}

	/**
	 * Erodes an 8-bit binary image by setting true pixels to zero if any of their
	 * 4 neighbours is zero.
	 * <p>
	 * NIST: {@code erode_charimage_2()}. Allocation of the output image is the
	 * responsibility of the caller. The input image remains unchanged; it is
	 * first copied into the output and then eroded pixels are cleared. This
	 * routine will NOT erode pixels indiscriminately along the image border:
	 * neighbours outside the image are treated as true.
	 *
	 * @param inputImageData  input 8-bit binary image to be eroded
	 * @param outputImageData output: receives the resulting eroded image (must be
	 *                        at least as long as {@code inputImageData})
	 * @param imageWidth      width (in pixels) of image
	 * @param imageHeight     height (in pixels) of image
	 */
	public void erodeImage2(int[] inputImageData, int[] outputImageData, final int imageWidth, final int imageHeight) {
		int row;
		int col;
		System.arraycopy(inputImageData, 0, outputImageData, 0, inputImageData.length);

		int inputImageDataIndex = 0;
		int outputImageDataIndex = 0;

		/* for true pixels. kill pixel if there is at least one false neighbor */
		for (row = 0; row < imageHeight; row++) {
			for (col = 0; col < imageWidth; col++) {
				if ((inputImageData[inputImageDataIndex]) == ILfs.TRUE) // 1 erode only operates on true pixels
				{
					/* more efficient with C's left to right evaluation of */
					/* conjuctions. E N S functions not executed if W is false */
					if (!(getWest82(inputImageData, inputImageDataIndex, col, 1) == ILfs.TRUE && // 1
							getEast82(inputImageData, inputImageDataIndex, col, imageWidth, 1) == ILfs.TRUE && // 1
							getNorth82(inputImageData, inputImageDataIndex, row, imageWidth, 1) == ILfs.TRUE && // 1
							getSouth82(inputImageData, inputImageDataIndex, row, imageWidth, imageHeight,
									1) == ILfs.TRUE)// 1
					) {
						outputImageData[outputImageDataIndex] = ILfs.FALSE;// 0
					}
				}
				inputImageDataIndex++;
				outputImageDataIndex++;
			}
		}
	}

	/**
	 * Dilates an 8-bit binary image by setting false pixels to one if any of
	 * their 4 neighbours is non-zero.
	 * <p>
	 * NIST: {@code dilate_charimage_2()}. Allocation of the output image is the
	 * responsibility of the caller. The input image remains unchanged; it is
	 * first copied into the output and then dilated pixels are set. Neighbours
	 * outside the image are treated as false.
	 *
	 * @param inputImageData  input 8-bit binary image to be dilated
	 * @param outputImageData output: receives the resulting dilated image (must
	 *                        be at least as long as {@code inputImageData})
	 * @param imageWidth      width (in pixels) of image
	 * @param imageHeight     height (in pixels) of image
	 */
	public void dilateImage2(int[] inputImageData, int[] outputImageData, final int imageWidth, final int imageHeight) {
		int row;
		int col;

		System.arraycopy(inputImageData, 0, outputImageData, 0, inputImageData.length);

		int inputImageDataIndex = 0;
		int outputImageDataIndex = 0;

		/* for true pixels. kill pixel if there is at least one false neighbor */
		for (row = 0; row < imageHeight; row++) {
			for (col = 0; col < imageWidth; col++) {
				if ((inputImageData[inputImageDataIndex]) == ILfs.FALSE) /*
																			 * pixel is already true, neighbors
																			 * irrelevant
																			 */
				{
					/* more efficient with C's left to right evaluation of */
					/* conjuctions. E N S functions not executed if W is false */
					if (getWest82(inputImageData, inputImageDataIndex, col, 0) == ILfs.TRUE
							|| getEast82(inputImageData, inputImageDataIndex, col, imageWidth, 0) == ILfs.TRUE
							|| getNorth82(inputImageData, inputImageDataIndex, row, imageWidth, 0) == ILfs.TRUE
							|| getSouth82(inputImageData, inputImageDataIndex, row, imageWidth, imageHeight,
									0) == ILfs.TRUE) {
						outputImageData[outputImageDataIndex] = ILfs.TRUE;
					}
				}
				inputImageDataIndex++;
				outputImageDataIndex++;
			}
		}
	}

	/**
	 * Returns the value of the 8-bit image pixel one row below the current pixel
	 * if defined, else returns {@code failCode}.
	 * <p>
	 * NIST: {@code get_south8_2()}.
	 *
	 * @param inputImageData      image pixel data (row-major)
	 * @param inputImageDataIndex index of the current pixel in the image
	 * @param row                 y-coord of current pixel
	 * @param imageWidth          width (in pixels) of image
	 * @param imageHeight         height (in pixels) of image
	 * @param failCode            return value if the desired pixel does not exist
	 * @return {@code failCode} if the neighbouring pixel is undefined (outside of
	 *         image boundaries); otherwise the value of the neighbouring pixel
	 */
	public int getSouth82(int[] inputImageData, int inputImageDataIndex, int row, int imageWidth, int imageHeight,
			int failCode) {
		if (row >= (imageHeight - 1)) // catch case where image is undefined southwards
		{
			return failCode; // use plane geometry and return code.
		}
		return (inputImageData[inputImageDataIndex + imageWidth]);
	}

	/**
	 * Returns the value of the 8-bit image pixel one row above the current pixel
	 * if defined, else returns {@code failCode}.
	 * <p>
	 * NIST: {@code get_north8_2()}.
	 *
	 * @param inputImageData      image pixel data (row-major)
	 * @param inputImageDataIndex index of the current pixel in the image
	 * @param row                 y-coord of current pixel
	 * @param imageWidth          width (in pixels) of image
	 * @param failCode            return value if the desired pixel does not exist
	 * @return {@code failCode} if the neighbouring pixel is undefined (outside of
	 *         image boundaries); otherwise the value of the neighbouring pixel
	 */
	public int getNorth82(int[] inputImageData, int inputImageDataIndex, int row, int imageWidth, int failCode) {
		if (row < 1) /* catch case where image is undefined northwards */
			return failCode; /* use plane geometry and return code. */

		return inputImageData[inputImageDataIndex - imageWidth];
	}

	/**
	 * Returns the value of the 8-bit image pixel one column right of the current
	 * pixel if defined, else returns {@code failCode}.
	 * <p>
	 * NIST: {@code get_east8_2()}.
	 *
	 * @param inputImageData      image pixel data (row-major)
	 * @param inputImageDataIndex index of the current pixel in the image
	 * @param col                 x-coord of current pixel
	 * @param imageWidth          width (in pixels) of image
	 * @param failCode            return value if the desired pixel does not exist
	 * @return {@code failCode} if the neighbouring pixel is undefined (outside of
	 *         image boundaries); otherwise the value of the neighbouring pixel
	 */
	public int getEast82(int[] inputImageData, int inputImageDataIndex, int col, int imageWidth, int failCode) {
		if (col >= (imageWidth - 1)) // catch case where image is undefined eastwards
		{
			return failCode; // use plane geometry and return code.
		}

		return (inputImageData[inputImageDataIndex + 1]);
	}

	/**
	 * Returns the value of the 8-bit image pixel one column left of the current
	 * pixel if defined, else returns {@code failCode}.
	 * <p>
	 * NIST: {@code get_west8_2()}.
	 *
	 * @param inputImageData      image pixel data (row-major)
	 * @param inputImageDataIndex index of the current pixel in the image
	 * @param col                 x-coord of current pixel
	 * @param failCode            return value if the desired pixel does not exist
	 * @return {@code failCode} if the neighbouring pixel is undefined (outside of
	 *         image boundaries); otherwise the value of the neighbouring pixel
	 */
	public int getWest82(int[] inputImageData, int inputImageDataIndex, int col, int failCode) {
		if (col < 1) // catch case where image is undefined westwards
		{
			return failCode; // use plane geometry and return code.
		}
		return (inputImageData[inputImageDataIndex - 1]);
	}
}