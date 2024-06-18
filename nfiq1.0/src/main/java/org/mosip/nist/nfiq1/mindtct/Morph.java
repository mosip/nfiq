package org.mosip.nist.nfiq1.mindtct;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IMorph;

public class Morph extends MindTct implements IMorph {
	private static Morph instance;

	private Morph() {
		super();
	}

	public static synchronized Morph getInstance() {
		if (instance == null) {
			instance = new Morph();
		}
		return instance;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: erodeImage2 - Erodes an 8-bit image by setting true pixels to zero
	 * #cat: if any of their 4 neighbors is zero. Allocation of the #cat: output
	 * image is the responsibility of the caller. The #cat: input image remains
	 * unchanged. This routine will NOT #cat: erode pixels indiscriminately along
	 * the image border. Input: inputImageData - input 8-bit image to be eroded
	 * imageWidth - width (in pixels) of image imageHeight - height (in pixels) of
	 * image Output: outputImageData - contains to the resulting eroded image
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: dilateImage2 - Dilates an 8-bit image by setting false pixels to #cat:
	 * one if any of their 4 neighbors is non-zero. Allocation #cat: of the output
	 * image is the responsibility of the caller. #cat: The input image remains
	 * unchanged. Input: inputImageData - input 8-bit image to be dilated imageWidth
	 * - width (in pixels) of image imageHeight - height (in pixels) of image
	 * Output: outputImageData - contains to the resulting dilated image
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: getSouth8_2 - Returns the value of the 8-bit image pixel 1 below the
	 * #cat: current pixel if defined else it returns (char)0. Input: inputImageData
	 * - points to current pixel in image inputImageDataIndex- current pixel index
	 * row - y-coord of current pixel imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image failCode - return value if desired
	 * pixel does not exist Return Code: Zero - if neighboring pixel is undefined
	 * (outside of image boundaries) Pixel - otherwise, value of neighboring pixel
	 **************************************************************************/
	public int getSouth82(int[] inputImageData, int inputImageDataIndex, int row, int imageWidth, int imageHeight,
			int failCode) {
		if (row >= (imageHeight - 1)) // catch case where image is undefined southwards
		{
			return failCode; // use plane geometry and return code.
		}
		return (inputImageData[inputImageDataIndex + imageWidth]);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getNorth8_2 - Returns the value of the 8-bit image pixel 1 above the
	 * #cat: current pixel if defined else it returns (char)0. Input: inputImageData
	 * - points to current pixel in image inputImageDataIndex- current pixel index
	 * row - y-coord of current pixel imageWidth - width (in pixels) of image
	 * failCode - return value if desired pixel does not exist Return Code: Zero -
	 * if neighboring pixel is undefined (outside of image boundaries) Pixel -
	 * otherwise, value of neighboring pixel
	 **************************************************************************/
	public int getNorth82(int[] inputImageData, int inputImageDataIndex, int row, int imageWidth, int failCode) {
		if (row < 1) /* catch case where image is undefined northwards */
			return failCode; /* use plane geometry and return code. */

		return inputImageData[inputImageDataIndex - imageWidth];
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getEast8_2 - Returns the value of the 8-bit image pixel 1 right of the
	 * #cat: current pixel if defined else it returns (char)0. Input: inputImageData
	 * - points to current pixel in image inputImageDataIndex- current pixel index
	 * col - x-coord of current pixel imageWidth - width (in pixels) of image
	 * failCode - return value if desired pixel does not exist Return Code: Zero -
	 * if neighboring pixel is undefined (outside of image boundaries) Pixel -
	 * otherwise, value of neighboring pixel
	 **************************************************************************/
	public int getEast82(int[] inputImageData, int inputImageDataIndex, int col, int imageWidth, int failCode) {
		if (col >= (imageWidth - 1)) // catch case where image is undefined eastwards
		{
			return failCode; // use plane geometry and return code.
		}

		return (inputImageData[inputImageDataIndex + 1]);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getWest8_2 - Returns the value of the 8-bit image pixel 1 left of the
	 * #cat: current pixel if defined else it returns (char)0. Input: inputImageData
	 * - points to current pixel in image inputImageDataIndex- current pixel index
	 * col - x-coord of current pixel failCode - return value if desired pixel does
	 * not exist Return Code: Zero - if neighboring pixel is undefined (outside of
	 * image boundaries) Pixel - otherwise, value of neighboring pixel
	 **************************************************************************/
	public int getWest82(int[] inputImageData, int inputImageDataIndex, int col, int failCode) {
		if (col < 1) // catch case where image is undefined westwards
		{
			return failCode; // use plane geometry and return code.
		}
		return (inputImageData[inputImageDataIndex - 1]);
	}
}