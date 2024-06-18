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

public class Block extends MindTct implements IBlock {
	private static final Logger logger = LoggerFactory.getLogger(Block.class);
	private static Block instance;

	private Block() {
		super();
	}

	public static synchronized Block getInstance() {
		if (instance == null) {
			instance = new Block();
		}
		return instance;
	}

	public Defs getDefs() {
		return Defs.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: blockOffsets - Divides an image into mw X mh equally sized blocks,
	 * #cat: returning a list of offsets to the top left corner of each block. #cat:
	 * For images that are even multiples of BLOCKSIZE, blocks do not #cat: not
	 * overlap and are immediately adjacent to each other. For image #cat: that are
	 * NOT even multiples of BLOCKSIZE, blocks continue to be #cat: non-overlapping
	 * up to the last column and/or last row of blocks. #cat: In these cases the
	 * blocks are adjacent to the edge of the image and #cat: extend inwards
	 * BLOCKSIZE units, overlapping the neighboring column #cat: or row of blocks.
	 * This routine also accounts for image padding #cat: which makes things a
	 * little more "messy". This routine is primarily #cat: responsible providing
	 * the ability to processs arbitrarily-sized #cat: images. The strategy used
	 * here is simple, but others are possible. Input: imageWidth - width (in
	 * pixels) of the orginal input image imageHeight - height (in pixels) of the
	 * orginal input image pad - the padding (in pixels) required to support the
	 * desired range of block orientations for DFT analysis. This padding is
	 * required along the entire perimeter of the input image. For certain
	 * applications, the pad may be zero. blockSize - the width and height (in
	 * pixels) of each image block Output: blockOffsets - points to the list of
	 * pixel offsets to the origin of each block in the "padded" input image
	 * oImageWidth - the number of horizontal blocks in the input image oImageHeight
	 * - the number of vertical blocks in the input image Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 * #cat: lowContrastBlock - Takes the offset to an image block of specified
	 * #cat: dimension, and analyzes the pixel intensities in the block #cat: to
	 * determine if there is sufficient contrast for further #cat: processing.
	 * Input: blockOffset - byte offset into the padded input image to the origin of
	 * the block to be analyzed blockSize - dimension (in pixels) of the width and
	 * height of the block (passing separate blocksize from LFSPARMS on purpose)
	 * paddedImageData - padded input image data (8 bits [0..256) grayscale)
	 * paddedImageWidth - width (in pixels) of the padded input image
	 * paddedImageHeight - height (in pixels) of the padded input image lfsparms -
	 * parameters and thresholds for controlling LFS Return Code: TRUE - block has
	 * sufficiently low contrast FALSE - block has sufficiently hight contrast
	 * Negative - system error
	 **************************************************************************
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: findValidBlock - Take a Direction Map, Low Contrast Map, #cat: Starting
	 * block address, a direction and searches the #cat: maps in the specified
	 * direction until either a block valid #cat: direction is encountered or a
	 * block flagged as LOW CONTRAST #cat: is encountered. If a valid direction is
	 * located, it and the #cat: address of the corresponding block are returned
	 * with a #cat: code of FOUND. Otherwise, a code of NOT_FOUND is returned.
	 * Input: directionMap - map of blocks containing directional ridge flows
	 * lowContrastMap - map of blocks flagged as LOW CONTRAST startX - X-block coord
	 * where search starts in maps startY - Y-block coord where search starts in
	 * maps mappedImageWidth - number of blocks horizontally in the maps
	 * mappedImageHeight - number of blocks vertically in the maps xIncr - X-block
	 * increment to direct search yIncr - Y-block increment to direct search Output:
	 * nbrDir - valid direction found nbrX - X-block coord where valid direction
	 * found nbrY - Y-block coord where valid direction found Return Code: FOUND -
	 * neighboring block with valid direction found NOT_FOUND - neighboring block
	 * with valid direction NOT found
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: setMarginBlocks - Take an image map and sets its perimeter values to
	 * #cat: the specified value. Input: oMap - map of blocks to be modified
	 * mappedImageWidth - number of blocks horizontally in the map mappedImageHeight
	 * - number of blocks vertically in the map marginValue - value to be assigned
	 * to the perimeter blocks Output: oMap - resulting map
	 **************************************************************************/
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