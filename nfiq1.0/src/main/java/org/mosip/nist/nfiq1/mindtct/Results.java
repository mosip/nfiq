package org.mosip.nist.nfiq1.mindtct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IResults;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Results extends MindTct implements IResults {
	private static final Logger logger = LoggerFactory.getLogger(Results.class);
	private static Results instance;

	private Results() {
		super();
	}

	public static synchronized Results getInstance() {
		if (instance == null) {
			instance = new Results();
		}
		return instance;
	}

	public int writeTextResults(File file, int m1flag, int imageWidth, int imageHeight,
			AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oQualityMap, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oLowContrastMap, AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap,
			int mapWidth, int mapHeight) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int writeMinutiaeXYTQ(File file, int repType, AtomicReference<Minutiae> oMinutiae, int imageWidth,
			int imageHeight) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: dumpMap - Prints a text report to the specified open file pointer #cat:
	 * of the integer values in a 2D integer vector. Input: file - open file pointer
	 * oMap - vector of integer directions (-1 ==> invalid direction) mapWidth -
	 * width (number of blocks) of map vector mapHeight - height (number of blocks)
	 * of map vector
	 * 
	 * @throws IOException
	 **************************************************************************/
	public void dumpMap(File file, AtomicIntegerArray oMap, int mapWidth, int mapHeight) throws IOException {
		int mx;
		int my;
		int mapIndex;
		FileWriter myWriter = new FileWriter(file.getAbsoluteFile());
		/* Simply print the map matrix out to the specified file pointer. */
		mapIndex = 0;
		for (my = 0; my < mapHeight; my++) {
			for (mx = 0; mx < mapWidth; mx++) {
				myWriter.write(String.format("%2d", oMap.get(mapIndex++)));
			}
			myWriter.write("\n");
		}
		myWriter.flush();
		myWriter.close();
	}

	public int drawInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, int mapWidth, int mapHeight,
			int[] imageData, int imageWidth, int imageHeight, RotGrids rotGrids, int drawPixel) {
		return 0;
	}

	public void drawInputBlockImageMap2(AtomicIntegerArray oInputBlockImageMap, AtomicIntegerArray oBlockOffsets,
			int mapWidth, int mapHeight, int[] paddedImageData, int paddedImageWidth, int paddedImageHeight,
			double startAngle, int nDirs, int blocksize) {
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: drawblocks - Annotates an input image with the location of each block's
	 * #cat: origin. This routine is useful to see how blocks are #cat: assigned to
	 * arbitrarily-sized images that are not an even #cat: width or height of the
	 * block size. In these cases the last #cat: column pair and row pair of blocks
	 * overlap each other. #cat: Note that the input image is modified upon return
	 * form #cat: this routine. Input: oBlockOffsets - offsets to the pixel origin
	 * of each block in the image mapWidth - number of blocks horizontally in the
	 * input image mapHeight - number of blocks vertically in the input image
	 * paddedImageData - input image data to be annotated that has pixel dimensions
	 * compatible with the offsets in blkoffs paddedImageWidth - width (in pixels)
	 * of the input image paddedImageHeight - height (in pixels) of the input image
	 * drawPixel - pixel intensity to be used when drawing on the image Output:
	 * paddedImageData - input image contains the results of the annoatation
	 **************************************************************************/
	public void drawBlocks(AtomicIntegerArray oBlockOffsets, int mapWidth, int mapHeight, int[] paddedImageData,
			int paddedImageWidth, int paddedImageHeight, int drawPixel) {
		int paddedImageIndex;

		for (int bi = 0; bi < paddedImageWidth * paddedImageHeight; bi++) {
			paddedImageIndex = 0 + oBlockOffsets.get(bi);
			paddedImageData[paddedImageIndex] = drawPixel;
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: drawRotGrid - Annotates an input image with a specified rotated grid.
	 * #cat: This routine is useful to see the location and orientation #cat: of a
	 * specific rotated grid within a specific block in the #cat: image. Note that
	 * the input image is modified upon return #cat: form this routine. Input:
	 * rotGrids - structure containing the rotated pixel grid offsets nDir - integer
	 * direction of the rotated grid to be annontated imageData - input image data
	 * to be annotated. blockOffset - the pixel offset from the origin of the input
	 * image to the origin of the specific block to be annoted imageWidth - width
	 * (in pixels) of the input image imageHeight - height (in pixels) of the input
	 * image drawPixel - pixel intensity to be used when drawing on the image Return
	 * Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int drawRotGrid(RotGrids rotGrids, int nDir, int[] imageData, int blockOffset, int imageWidth,
			int imageHeight, int drawPixel) {
		int i;
		int j;
		int gi;

		/* Check if specified rotation direction is within range of */
		/* rotated grids. */
		if (nDir >= rotGrids.getNoOfGrids()) {
			logger.error("ERROR : drawRotGrid : input direction exceeds range of rotated grids\n");
			return (ILfs.ERROR_CODE_140);
		}

		/* Intialize grid offset index */
		gi = 0;
		/* Foreach row in rotated grid ... */
		for (i = 0; i < rotGrids.getGridHeight(); i++) {
			/* Foreach column in rotated grid ... */
			for (j = 0; j < rotGrids.getGridWidth(); j++) {
				/* Draw pixels from every other rotated row to represent direction */
				/* of line sums used in DFT processing. */
				if ((i % 2) != 0) {
					imageData[blockOffset + rotGrids.getGrids()[nDir][gi]] = drawPixel;
				}
				/* Bump grid offset index */
				gi++;
			}
		}

		return ILfs.FALSE;
	}

	public void dumpLinkTable(File file, int[] linkTable, int[] xAxis, int[] yAxis, int nxAxis, int nyAxis, int tblDim,
			AtomicReference<Minutiae> oMinutiae) {
	}

	public int drawDirectionMap(StringBuilder fileName, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oBlockOffsets, int mapWidth, int mapHeight, int blocksize, int[] imageData,
			int imageWidth, int imageHeight, int flag) {
		return 0;
	}

	public int drawTFMap(StringBuilder fileName, AtomicIntegerArray oMap, AtomicIntegerArray oBlockOffsets,
			int mapWidth, int mapHeight, int blocksize, int[] imageData, int imageWidth, int imageHeight, int flag) {
		return 0;
	}
}