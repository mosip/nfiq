package org.mosip.nist.nfiq1.mindtct;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IBlock;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block-level image partitioning and map utilities for MINDTCT.
 * <p>
 * Port of NIST LFS {@code block.c} ({@code block_offsets}, {@code low_contrast_block},
 * {@code find_valid_block}, {@code set_margin_blocks}). MINDTCT analyses the fingerprint in fixed-size blocks
 * (e.g. 8 x 8 pixels); these routines compute block origins in the padded image, flag low-contrast blocks, walk
 * the maps looking for valid neighbouring directions, and set map borders.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state.
 */
public class Block extends MindTct implements IBlock {
	/** SLF4J logger for input and processing errors. */
	private static final Logger logger = LoggerFactory.getLogger(Block.class);
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Block instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Block() {
		super();
	}

	/**
	 * Returns the shared {@code Block} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Block getInstance() {
		if (instance == null) {
			instance = new Block();
		}
		return instance;
	}

	/**
	 * Returns the shared numeric-definitions helper (rounding and precision truncation).
	 *
	 * @return the {@link Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Divides an image into {@code mw x mh} equally sized blocks and returns the offset of each block's
	 * top-left corner in the padded image (NIST {@code block_offsets}).
	 * <p>
	 * For images whose dimensions are exact multiples of BLOCKSIZE, blocks do not overlap and sit right next to
	 * each other. Otherwise, blocks stay non-overlapping up to the last column and/or row; the last column/row
	 * of blocks is placed against the image edge and extends BLOCKSIZE pixels inwards, overlapping its
	 * neighbours. The routine also accounts for image padding, which makes things a little more "messy". It is
	 * what lets MINDTCT process images of arbitrary size; the strategy is simple, but others are possible.
	 *
	 * @param ret          output: 0 ({@link ILfs#FALSE}) on success; {@link ILfs#ERROR_CODE_80} if the image
	 *                     is smaller than one block
	 * @param oImageWidth  output: number of horizontal blocks in the image
	 * @param oImageHeight output: number of vertical blocks in the image
	 * @param imageWidth   width (in pixels) of the original (unpadded) input image
	 * @param imageHeight  height (in pixels) of the original (unpadded) input image
	 * @param pad          padding (in pixels) needed around the whole image to support the desired range of
	 *                     block orientations for DFT analysis; may be zero for some applications
	 * @param blockSize    width and height (in pixels) of each image block
	 * @return the pixel offsets to the origin of each block in the padded input image (row-major block order),
	 *         or {@code null} on error
	 */
	public AtomicIntegerArray blockOffsets(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
			int imageWidth, int imageHeight, int pad, int blockSize) {
		AtomicIntegerArray blockOffsets;
		int bx;
		int by;
		int blockImageWidth;
		int blockImageHeight;
		int bi;
		int blockImageSize;
		int blockRowStart;
		int blockRowSize;
		int offset;
		int lastbw;
		int lastbh;
		int pad2;
		int paddedImageWidth;
		int paddedImageHeight;

		/* Test if unpadded image is smaller than a single block */
		if ((imageWidth < blockSize) || (imageHeight < blockSize)) {
			logger.error("ERROR : block_offsets : image must be at least {} by {} in size", blockSize, blockSize);
			ret.set(ILfs.ERROR_CODE_80);
			return null;
		}

		/* Compute padded width and height of image */
		pad2 = pad << 1;
		paddedImageWidth = imageWidth + pad2;
		paddedImageHeight = imageHeight + pad2;

		/* Compute the number of columns and rows of blocks in the image. */
		/* Take the ceiling to account for "leftovers" at the right and */
		/* bottom of the unpadded image */
		blockImageWidth = (int) Math.ceil(imageWidth / (double) blockSize);
		blockImageHeight = (int) Math.ceil(imageHeight / (double) blockSize);

		/* Total number of blocks in the image */
		blockImageSize = blockImageWidth * blockImageHeight;

		/* The index of the last column */
		lastbw = blockImageWidth - 1;
		/* The index of the last row */
		lastbh = blockImageHeight - 1;

		/* Allocate list of block offsets */
		blockOffsets = new AtomicIntegerArray(blockImageSize);

		/* Current block index */
		bi = 0;

		/* Current offset from top of padded image to start of new row of */
		/* unpadded image blocks. It is initialize to account for the */
		/* padding and will always be indented the size of the padding */
		/* from the left edge of the padded image. */
		blockRowStart = (pad * paddedImageWidth) + pad;

		/* Number of pixels in a row of blocks in the padded image */
		blockRowSize = paddedImageWidth * blockSize; // row width X block height

		/* Foreach non-overlapping row of blocks in the image */
		for (by = 0; by < lastbh; by++) {
			/* Current offset from top of padded image to beginning of */
			/* the next block */
			offset = blockRowStart;
			/* Foreach non-overlapping column of blocks in the image */
			for (bx = 0; bx < lastbw; bx++) {
				/* Store current block offset */
				blockOffsets.set(bi++, offset);
				/* Bump to the beginning of the next block */
				offset += blockSize;
			}

			/* Compute and store "left-over" block in row. */
			/* This is the block in the last column of row. */
			/* Start at far right edge of unpadded image data */
			/* and come in BLOCKSIZE pixels. */
			blockOffsets.set(bi++, blockRowStart + imageWidth - blockSize);
			/* Bump to beginning of next row of blocks */
			blockRowStart += blockRowSize;
		}

		/* Compute and store "left-over" row of blocks at bottom of image */
		/* Start at bottom edge of unpadded image data and come up */
		/* BLOCKSIZE pixels. This too must account for padding. */
		blockRowStart = ((pad + imageHeight - blockSize) * paddedImageWidth) + pad;
		/* Start the block offset for the last row at this point */
		offset = blockRowStart;
		/* Foreach non-overlapping column of blocks in last row of the image */
		for (bx = 0; bx < lastbw; bx++) {
			/* Store current block offset */
			blockOffsets.set(bi++, offset);
			/* Bump to the beginning of the next block */
			offset += blockSize;
		}

		/* Compute and store last "left-over" block in last row. */
		/* Start at right edge of unpadded image data and come in */
		/* BLOCKSIZE pixels. */
		blockOffsets.set(bi++, blockRowStart + imageWidth - blockSize);

		oImageWidth.set(blockImageWidth);
		oImageHeight.set(blockImageHeight);
		ret.set(ILfs.FALSE);
		return blockOffsets;
	}

	/**
	 * Decides whether an image block has too little contrast for further processing (NIST
	 * {@code low_contrast_block}).
	 * <p>
	 * Builds a histogram of the block's pixel intensities and finds the pixel values at the lower and upper
	 * {@code lfsparms.getPercentileMinMax()} percentiles. If their difference is below
	 * {@code lfsparms.getMinContrastDelta()}, the block is low contrast. The histogram has
	 * {@link ILfs#IMG_6BIT_PIX_LIMIT} bins, so pixel values are expected to fit that range.
	 *
	 * @param blockOffset       offset into the padded input image to the origin of the block to analyse
	 * @param blockSize         width and height (in pixels) of the block (passed separately from LFSPARMS on
	 *                          purpose)
	 * @param paddedImageData   padded input image data (8-bit grayscale)
	 * @param paddedImageWidth  width (in pixels) of the padded input image
	 * @param paddedImageHeight height (in pixels) of the padded input image
	 * @param lfsparms          parameters and thresholds that control LFS
	 * @return {@link ILfs#TRUE} if the block has low contrast; {@link ILfs#FALSE} if it has enough contrast;
	 *         {@link ILfs#ERROR_CODE_510} / {@link ILfs#ERROR_CODE_511} (negative system errors) if the min / max
	 *         percentile pixel cannot be found
	 */
	public int lowContrastBlock(int blockOffset, int blockSize, int[] paddedImageData, int paddedImageWidth,
			int paddedImageHeight, LfsParams lfsparms) {
		int[] pixTable = new int[ILfs.IMG_6BIT_PIX_LIMIT];
		int numOfPix;
		int pi;
		int currentPaddedImageIndex;
		int paddedImageIndex;
		int delta;
		double tdbl;
		int prctMin = 0;
		int prctMax = 0;
		int prctThresh;
		int pixSum;
		int found;

		numOfPix = blockSize * blockSize;
		Arrays.fill(pixTable, 0);

		tdbl = (lfsparms.getPercentileMinMax() / 100.0) * (numOfPix - 1);
		tdbl = getDefs().truncDoublePrecision(tdbl, ILfs.TRUNC_SCALE);
		prctThresh = getDefs().sRound(tdbl);

		currentPaddedImageIndex = 0 + blockOffset;
		for (int py = 0; py < blockSize; py++) {
			paddedImageIndex = currentPaddedImageIndex;
			for (int px = 0; px < blockSize; px++) {
				pixTable[paddedImageData[paddedImageIndex]]++;
				paddedImageIndex++;
			}
			currentPaddedImageIndex += paddedImageWidth;
		}

		pi = 0;
		pixSum = 0;
		found = ILfs.FALSE;
		while (pi < ILfs.IMG_6BIT_PIX_LIMIT) {
			pixSum += pixTable[pi];
			if (pixSum >= prctThresh) {
				prctMin = pi;
				found = ILfs.TRUE;
				break;
			}
			pi++;
		}
		if (found == ILfs.FALSE) {
			logger.error("ERROR : lowContrastBlock : min percentile pixel not found\n");
			return (ILfs.ERROR_CODE_510);
		}

		pi = ILfs.IMG_6BIT_PIX_LIMIT - 1;
		pixSum = 0;
		found = ILfs.FALSE;
		while (pi >= 0) {
			pixSum += pixTable[pi];
			if (pixSum >= prctThresh) {
				prctMax = pi;
				found = ILfs.TRUE;
				break;
			}
			pi--;
		}
		if (found == ILfs.FALSE) {
			logger.error("ERROR : lowContrastBlock : max percentile pixel not found\n");
			return (ILfs.ERROR_CODE_511);
		}

		delta = prctMax - prctMin;
		if (delta < lfsparms.getMinContrastDelta()) {
			return (ILfs.TRUE);
		} else {
			return (ILfs.FALSE);
		}
	}

	/**
	 * Walks the maps from a starting block in a fixed direction until it finds a block with a valid direction or
	 * hits a LOW CONTRAST block (NIST {@code find_valid_block}).
	 * <p>
	 * The search starts at {@code (startX + xIncr, startY + yIncr)} and keeps adding the increments until it
	 * leaves the map. If a valid direction (value {@code >= 0}) is found, the direction and the block's
	 * coordinates are returned with {@link ILfs#FOUND}.
	 *
	 * @param nbrDir            output: the valid direction found
	 * @param nbrX              output: X-block coordinate where the valid direction was found
	 * @param nbrY              output: Y-block coordinate where the valid direction was found
	 * @param directionMap      map of blocks holding ridge-flow directions
	 * @param lowContrastMap    map of blocks flagged as LOW CONTRAST (1 = low contrast)
	 * @param startX            X-block coordinate where the search starts
	 * @param startY            Y-block coordinate where the search starts
	 * @param mappedImageWidth  number of blocks horizontally in the maps
	 * @param mappedImageHeight number of blocks vertically in the maps
	 * @param xIncr             X-block increment that sets the search direction
	 * @param yIncr             Y-block increment that sets the search direction
	 * @return {@link ILfs#FOUND} if a neighbouring block with a valid direction was found, otherwise
	 *         {@link ILfs#NOT_FOUND}
	 */
	public int findValidBlock(AtomicInteger nbrDir, AtomicInteger nbrX, AtomicInteger nbrY,
			AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap, int startX, int startY,
			int mappedImageWidth, int mappedImageHeight, int xIncr, int yIncr) {
		int xPixel;
		int yPixel;
		int dir;

		/* Initialize starting block coords. */
		xPixel = startX + xIncr;
		yPixel = startY + yIncr;

		/* While we are not outside the boundaries of the map ... */
		while ((xPixel >= 0) && (xPixel < mappedImageWidth) && (yPixel >= 0) && (yPixel < mappedImageHeight)) {
			/* Stop unsuccessfully if we encounter a LOW CONTRAST block. */
			if (lowContrastMap.get((yPixel * mappedImageWidth) + xPixel) == 1) {
				return (ILfs.NOT_FOUND);
			}

			/* Stop successfully if we encounter a block with valid direction. */
			if ((dir = directionMap.get((yPixel * mappedImageWidth) + xPixel)) >= ILfs.FALSE) {
				nbrDir.set(dir);
				nbrX.set(xPixel);
				nbrY.set(yPixel);
				return (ILfs.FOUND);
			}

			/* Otherwise, advance to the next block in the map. */
			xPixel += xIncr;
			yPixel += yIncr;
		}

		/* If we get here, then we did not find a valid block in the given */
		/* direction in the map. */
		return (ILfs.NOT_FOUND);
	}

	/**
	 * Sets every block on the perimeter of a map to a given value (NIST {@code set_margin_blocks}).
	 *
	 * @param oMap              input/output: the map of blocks to modify (row-major), updated in place
	 * @param mappedImageWidth  number of blocks horizontally in the map
	 * @param mappedImageHeight number of blocks vertically in the map
	 * @param marginValue       value to assign to the perimeter blocks
	 */
	public void setMarginBlocks(AtomicIntegerArray oMap, int mappedImageWidth, int mappedImageHeight, int marginValue) {
		int mapIndex1;
		int mapIndex2;

		mapIndex1 = 0;
		mapIndex2 = 0 + ((mappedImageHeight - 1) * mappedImageWidth);
		for (int x = 0; x < mappedImageWidth; x++) {
			oMap.set(mapIndex1++, marginValue);
			oMap.set(mapIndex2++, marginValue);
		}

		mapIndex1 = 0 + mappedImageWidth;
		mapIndex2 = 0 + mappedImageWidth + mappedImageWidth - 1;
		for (int y = 1; y < mappedImageHeight - 1; y++) {
			oMap.set(mapIndex1, marginValue);
			oMap.set(mapIndex2, marginValue);
			mapIndex1 += mappedImageWidth;
			mapIndex2 += mappedImageWidth;
		}
	}
}