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

/**
 * Result reporting and diagnostic image annotation routines for MINDTCT.
 * <p>
 * Port of NIST LFS {@code results.c}. Only a subset is actually implemented in
 * this Java port ({@link #dumpMap}, {@link #drawBlocks}, {@link #drawRotGrid});
 * the remaining methods are no-op stubs kept for API parity with the NIST
 * module and the {@link IResults} interface. None of these routines are needed
 * to compute the NFIQ score.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class Results extends MindTct implements IResults {
	/** SLF4J logger for error reporting in this class. */
	private static final Logger logger = LoggerFactory.getLogger(Results.class);
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Results instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Results() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Results} instance
	 */
	public static synchronized Results getInstance() {
		if (instance == null) {
			instance = new Results();
		}
		return instance;
	}

	/**
	 * Writes minutiae and block maps as text result files.
	 * <p>
	 * NIST: {@code write_text_results()}. <b>Not implemented</b> in this port:
	 * this is a stub that writes nothing.
	 *
	 * @param file            output file (unused)
	 * @param m1flag          if non-zero, write minutiae in M1 (ANSI INCITS
	 *                        378) orientation (unused)
	 * @param imageWidth      width (in pixels) of the image (unused)
	 * @param imageHeight     height (in pixels) of the image (unused)
	 * @param oMinutiae       detected minutiae (unused)
	 * @param oQualityMap     quality map (unused)
	 * @param oDirectionMap   direction map (unused)
	 * @param oLowContrastMap low contrast map (unused)
	 * @param oLowFlowMap     low ridge flow map (unused)
	 * @param oHighCurveMap   high curvature map (unused)
	 * @param mapWidth        width (in blocks) of the maps (unused)
	 * @param mapHeight       height (in blocks) of the maps (unused)
	 * @return always 0
	 */
	public int writeTextResults(File file, int m1flag, int imageWidth, int imageHeight,
			AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oQualityMap, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oLowContrastMap, AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap,
			int mapWidth, int mapHeight) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Writes minutiae as X, Y, Theta, Quality records.
	 * <p>
	 * NIST: {@code write_minutiae_XYTQ()}. <b>Not implemented</b> in this port:
	 * this is a stub that writes nothing.
	 *
	 * @param file        output file (unused)
	 * @param repType     minutiae representation type, e.g. NIST internal or M1
	 *                    (unused)
	 * @param oMinutiae   detected minutiae (unused)
	 * @param imageWidth  width (in pixels) of the image (unused)
	 * @param imageHeight height (in pixels) of the image (unused)
	 * @return always 0
	 */
	public int writeMinutiaeXYTQ(File file, int repType, AtomicReference<Minutiae> oMinutiae, int imageWidth,
			int imageHeight) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Prints a text report of the integer values in a 2D integer map to the
	 * specified file.
	 * <p>
	 * NIST: {@code dump_map()}. Each map row is written on its own line with
	 * every value formatted as {@code %2d}. The file is (re)created/overwritten.
	 *
	 * @param file      destination file
	 * @param oMap      map of integer values, e.g. directions (-1 means invalid
	 *                  direction), row-major
	 * @param mapWidth  width (number of blocks) of map
	 * @param mapHeight height (number of blocks) of map
	 * @throws IOException if the file cannot be created or written
	 */
	public void dumpMap(File file, AtomicIntegerArray oMap, int mapWidth, int mapHeight) throws IOException {
		int mx;
		int my;
		int mapIndex;
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())) {
			/* Simply print the map matrix out to the specified file pointer. */
			mapIndex = 0;
			for (my = 0; my < mapHeight; my++) {
				for (mx = 0; mx < mapWidth; mx++) {
					myWriter.write(String.format("%2d ", oMap.get(mapIndex++)));
				}
				myWriter.write("\n");
			}
		}
	}

	/**
	 * Annotates an image with the directions in an integer direction map.
	 * <p>
	 * NIST: {@code drawimap()}. <b>Not implemented</b> in this port: this is a
	 * stub that leaves the image unchanged.
	 *
	 * @param oInputBlockImageMap direction map (unused)
	 * @param mapWidth            width (in blocks) of the map (unused)
	 * @param mapHeight           height (in blocks) of the map (unused)
	 * @param imageData           image to annotate (unused)
	 * @param imageWidth          width (in pixels) of the image (unused)
	 * @param imageHeight         height (in pixels) of the image (unused)
	 * @param rotGrids            rotated grid offsets (unused)
	 * @param drawPixel           pixel intensity used for drawing (unused)
	 * @return always 0
	 */
	public int drawInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, int mapWidth, int mapHeight,
			int[] imageData, int imageWidth, int imageHeight, RotGrids rotGrids, int drawPixel) {
		return 0;
	}

	/**
	 * Annotates a padded image with the directions in an integer direction map
	 * using block offsets.
	 * <p>
	 * NIST: {@code drawimap2()}. <b>Not implemented</b> in this port: this is a
	 * stub that leaves the image unchanged.
	 *
	 * @param oInputBlockImageMap direction map (unused)
	 * @param oBlockOffsets       pixel offsets to each block origin (unused)
	 * @param mapWidth            width (in blocks) of the map (unused)
	 * @param mapHeight           height (in blocks) of the map (unused)
	 * @param paddedImageData     padded image to annotate (unused)
	 * @param paddedImageWidth    width (in pixels) of the padded image (unused)
	 * @param paddedImageHeight   height (in pixels) of the padded image (unused)
	 * @param startAngle          angle (in radians) of the first direction (unused)
	 * @param nDirs               number of directions (unused)
	 * @param blocksize           block size in pixels (unused)
	 */
	public void drawInputBlockImageMap2(AtomicIntegerArray oInputBlockImageMap, AtomicIntegerArray oBlockOffsets,
			int mapWidth, int mapHeight, int[] paddedImageData, int paddedImageWidth, int paddedImageHeight,
			double startAngle, int nDirs, int blocksize) {
	}

	/**
	 * Annotates an input image with the location of each block's origin.
	 * <p>
	 * NIST: {@code drawblocks()}. This routine is useful to see how blocks are
	 * assigned to arbitrarily-sized images that are not an even width or height
	 * of the block size. In these cases the last column pair and row pair of
	 * blocks overlap each other. Note that the input image is modified upon
	 * return from this routine.
	 * <p>
	 * Note: this port iterates over {@code paddedImageWidth * paddedImageHeight}
	 * entries of {@code oBlockOffsets} (the NIST original iterates over
	 * {@code mapWidth * mapHeight} blocks), so {@code oBlockOffsets} must be at
	 * least that long.
	 *
	 * @param oBlockOffsets     offsets to the pixel origin of each block in the
	 *                          image
	 * @param mapWidth          number of blocks horizontally in the input image
	 * @param mapHeight         number of blocks vertically in the input image
	 * @param paddedImageData   input/output: image data to be annotated, with
	 *                          pixel dimensions compatible with the offsets in
	 *                          {@code oBlockOffsets}; on return contains the
	 *                          annotation
	 * @param paddedImageWidth  width (in pixels) of the input image
	 * @param paddedImageHeight height (in pixels) of the input image
	 * @param drawPixel         pixel intensity to be used when drawing on the
	 *                          image
	 */
	public void drawBlocks(AtomicIntegerArray oBlockOffsets, int mapWidth, int mapHeight, int[] paddedImageData,
			int paddedImageWidth, int paddedImageHeight, int drawPixel) {
		int paddedImageIndex;

		for (int bi = 0; bi < mapWidth * mapHeight; bi++) {
			paddedImageIndex = 0 + oBlockOffsets.get(bi);
			paddedImageData[paddedImageIndex] = drawPixel;
		}
	}

	/**
	 * Annotates an input image with a specified rotated grid.
	 * <p>
	 * NIST: {@code drawrotgrid()}. This routine is useful to see the location
	 * and orientation of a specific rotated grid within a specific block in the
	 * image. Pixels of every other rotated row are drawn, representing the
	 * direction of the line sums used in DFT processing. Note that the input
	 * image is modified upon return from this routine.
	 *
	 * @param rotGrids    structure containing the rotated pixel grid offsets
	 * @param nDir        integer direction of the rotated grid to be annotated
	 * @param imageData   input/output: image data to be annotated
	 * @param blockOffset the pixel offset from the origin of the input image to
	 *                    the origin of the specific block to be annotated
	 * @param imageWidth  width (in pixels) of the input image
	 * @param imageHeight height (in pixels) of the input image
	 * @param drawPixel   pixel intensity to be used when drawing on the image
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative
	 *         ({@link ILfs#ERROR_CODE_140}) if {@code nDir} exceeds the range of
	 *         rotated grids
	 */
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

	/**
	 * Prints a text report of a minutia link table.
	 * <p>
	 * NIST: {@code dump_link_table()}. <b>Not implemented</b> in this port: this
	 * is a stub that writes nothing.
	 *
	 * @param file      output file (unused)
	 * @param linkTable square link table of minutia scores (unused)
	 * @param xAxis     minutia indices along the table x-axis (unused)
	 * @param yAxis     minutia indices along the table y-axis (unused)
	 * @param nxAxis    number of entries on the x-axis (unused)
	 * @param nyAxis    number of entries on the y-axis (unused)
	 * @param tblDim    allocated dimension of the square link table (unused)
	 * @param oMinutiae detected minutiae (unused)
	 */
	public void dumpLinkTable(File file, int[] linkTable, int[] xAxis, int[] yAxis, int nxAxis, int nyAxis, int tblDim,
			AtomicReference<Minutiae> oMinutiae) {
	}

	/**
	 * Draws a direction map onto an image and writes it to a file.
	 * <p>
	 * NIST: {@code draw_imap()}. <b>Not implemented</b> in this port: this is a
	 * stub that writes nothing.
	 *
	 * @param fileName      output file name (unused)
	 * @param oDirectionMap direction map (unused)
	 * @param oBlockOffsets pixel offsets to each block origin (unused)
	 * @param mapWidth      width (in blocks) of the map (unused)
	 * @param mapHeight     height (in blocks) of the map (unused)
	 * @param blocksize     block size in pixels (unused)
	 * @param imageData     image to draw on (unused)
	 * @param imageWidth    width (in pixels) of the image (unused)
	 * @param imageHeight   height (in pixels) of the image (unused)
	 * @param flag          drawing option flag (unused)
	 * @return always 0
	 */
	public int drawDirectionMap(StringBuilder fileName, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oBlockOffsets, int mapWidth, int mapHeight, int blocksize, int[] imageData,
			int imageWidth, int imageHeight, int flag) {
		return 0;
	}

	/**
	 * Draws a TRUE/FALSE block map (e.g. low contrast, low flow or high
	 * curvature) onto an image and writes it to a file.
	 * <p>
	 * NIST: {@code draw_TF_map()}. <b>Not implemented</b> in this port: this is a
	 * stub that writes nothing.
	 *
	 * @param fileName      output file name (unused)
	 * @param oMap          TRUE/FALSE block map (unused)
	 * @param oBlockOffsets pixel offsets to each block origin (unused)
	 * @param mapWidth      width (in blocks) of the map (unused)
	 * @param mapHeight     height (in blocks) of the map (unused)
	 * @param blocksize     block size in pixels (unused)
	 * @param imageData     image to draw on (unused)
	 * @param imageWidth    width (in pixels) of the image (unused)
	 * @param imageHeight   height (in pixels) of the image (unused)
	 * @param flag          drawing option flag (unused)
	 * @return always 0
	 */
	public int drawTFMap(StringBuilder fileName, AtomicIntegerArray oMap, AtomicIntegerArray oBlockOffsets,
			int mapWidth, int mapHeight, int blocksize, int[] imageData, int imageWidth, int imageHeight, int flag) {
		return 0;
	}
}