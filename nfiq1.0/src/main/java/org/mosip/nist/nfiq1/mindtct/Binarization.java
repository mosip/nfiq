package org.mosip.nist.nfiq1.mindtct;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IBinarization;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a grayscale fingerprint image into a black/white image, pixel by pixel, guided by the ridge-flow
 * direction map (MINDTCT binarization stage).
 * <p>
 * Port of NIST LFS {@code binar.c} ({@code binarize}, {@code binarize_V2}, {@code binarize_image},
 * {@code binarize_image_V2}, {@code dirbinarize}, {@code isobinarize}). In a block with a valid ridge
 * direction, each pixel is binarized by comparing the center row of a rotated grid aligned with the ridge flow
 * against the whole grid. The V1 NMAP path uses isotropic (neighborhood-average) binarization for invalid or
 * high-curvature blocks. The binary image produced here is what minutiae detection scans.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state.
 */
public class Binarization extends MindTct implements IBinarization {
	/** SLF4J logger for allocation errors. */
	private static final Logger logger = LoggerFactory.getLogger(Binarization.class);

	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Binarization instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Binarization() {
		super();
	}

	/**
	 * Returns the shared {@code Binarization} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Binarization getInstance() {
		if (instance == null) {
			instance = new Binarization();
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
	 * Returns the MINDTCT image utility helper, used here to fill holes in the binary image.
	 *
	 * @return the {@link ImageUtil} singleton (MINDTCT package version)
	 */
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/**
	 * Binarizes a padded grayscale image using its NMAP, then fills holes (NIST {@code binarize}).
	 * <p>
	 * Takes a padded grayscale input image and its ridge-flow direction NMAP and produces a binarized version
	 * of the image with {@link #binarizeImage}. It then fills horizontal and vertical "holes" in the binary
	 * image, repeating {@code lfsParms.getNumFillHoles()} times (3 in LFS).
	 *
	 * @param ret               output: 0 ({@link ILfs#FALSE}) on success; negative on system error
	 * @param oBinarizedWidth   output: width (in pixels) of the binary image
	 * @param oBinarizedHeight  output: height (in pixels) of the binary image
	 * @param paddedImageData   padded input grayscale image
	 * @param paddedImageWidth  padded width (in pixels) of the input image
	 * @param paddedImageHeight padded height (in pixels) of the input image
	 * @param mapDirectionArr   2-D vector (row-major) of IMAP directions and other NMAP codes
	 * @param mappedImageWidth  width (in blocks) of the NMAP
	 * @param mappedImageHeight height (in blocks) of the NMAP
	 * @param dirBinGrids       set of rotated grid offsets used for directional binarization
	 * @param lfsParms          parameters and thresholds that control LFS
	 * @return the new (unpadded) binary image, or an empty array on error
	 */
	public int[] binarize(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight,
			int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
			AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
			final RotGrids dirBinGrids, final LfsParams lfsParms) {
		int[] binarizedImageData;
		int i;
		AtomicInteger binarizedWidth = new AtomicInteger(0);
		AtomicInteger binarizedHeight = new AtomicInteger(0);
		// return code

		/* 1. Binarize the padded input image using NMAP information. */
		binarizedImageData = binarizeImage(ret, binarizedWidth, binarizedHeight, paddedImageData, paddedImageWidth,
				paddedImageHeight, mapDirectionArr, mappedImageWidth, mappedImageHeight, lfsParms.getBlockOffsetSize(),
				dirBinGrids, lfsParms.getIsoBinGridDim());
		if (ret.get() != ILfs.FALSE) {
			return new int[0];
		}

		/* 2. Fill black and white holes in binary image. */
		/* LFS scans the binary image, filling holes, 3 times. */
		for (i = 0; i < lfsParms.getNumFillHoles(); i++) {
			getImageUtil().fillHoles(binarizedImageData, binarizedWidth.get(), binarizedHeight.get());
		}

		/* Return binarized input image. */
		oBinarizedWidth.set(binarizedWidth.get());
		oBinarizedHeight.set(binarizedHeight.get());
		return binarizedImageData;
	}

	/**
	 * Binarizes a padded grayscale image using its Direction Map, then fills holes (NIST {@code binarize_V2}).
	 * <p>
	 * Takes a padded grayscale input image and its Direction Map and produces a binarized version of the
	 * image with {@link #binarizeImageV2}. It then fills horizontal and vertical "holes" in the binary image,
	 * repeating {@code lfsParms.getNumFillHoles()} times. The input image must be padded enough to hold the
	 * rotated directional binarization grids applied to pixels along its perimeter.
	 *
	 * @param ret               output: 0 ({@link ILfs#FALSE}) on success; negative on system error
	 * @param oBinarizedWidth   output: width (in pixels) of the binary image
	 * @param oBinarizedHeight  output: height (in pixels) of the binary image
	 * @param paddedImageData   padded input grayscale image
	 * @param paddedImageWidth  padded width (in pixels) of the input image
	 * @param paddedImageHeight padded height (in pixels) of the input image
	 * @param directionMap      2-D vector (row-major) of discrete ridge-flow directions
	 * @param mappedImageWidth  width (in blocks) of the map
	 * @param mappedImageHeight height (in blocks) of the map
	 * @param dirBinGrids       set of rotated grid offsets used for directional binarization
	 * @param lfsParms          parameters and thresholds that control LFS
	 * @return the new (unpadded) binary image, or an empty array on error
	 */
	public int[] binarizeV2(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight,
			int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
			AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
			final RotGrids dirBinGrids, final LfsParams lfsParms) {
		int[] binarizeImagedata;
		AtomicInteger binarizedWidth = new AtomicInteger(0);
		AtomicInteger binarizedHeight = new AtomicInteger(0);
		// return code
		/* 1. Binarize the padded input image using NMAP information. */
		binarizeImagedata = binarizeImageV2(ret, binarizedWidth, binarizedHeight, paddedImageData, paddedImageWidth,
				paddedImageHeight, directionMap, mappedImageWidth, mappedImageHeight, lfsParms.getBlockOffsetSize(),
				dirBinGrids);
		if (ret.get() != ILfs.FALSE) {
			return new int[0];
		}

		/* 2. Fill black and white holes in binary image. */
		/* LFS scans the binary image, filling holes, 3 times. */
		for (int i = 0; i < lfsParms.getNumFillHoles(); i++) {
			getImageUtil().fillHoles(binarizeImagedata, binarizedWidth.get(), binarizedHeight.get());
		}

		/* Return binarized input image. */
		oBinarizedWidth.set(binarizedWidth.get());
		oBinarizedHeight.set(binarizedHeight.get());

		return binarizeImagedata;
	}

	/**
	 * Generates a binary image from a padded grayscale image and its NMAP (NIST {@code binarize_image}).
	 * <p>
	 * For each pixel of the unpadded region, looks up the NMAP value of its block. Blocks with
	 * {@link ILfs#NO_VALID_NBRS} become white ({@link ILfs#WHITE_PIXEL}). Blocks with a valid direction
	 * ({@code >= 0}) use {@link #dirbinarize}. INVALID or HIGH-CURVATURE blocks use {@link #isoBinarize}. The
	 * output is {@code 2 * pad} pixels smaller than the input in each dimension.
	 *
	 * @param ret               output: 0 ({@link ILfs#FALSE}) on success; {@link ILfs#ERROR_CODE_110} if
	 *                          allocation fails
	 * @param oBinarizedWidth   output: binary image width (in pixels)
	 * @param oBinarizedHeight  output: binary image height (in pixels)
	 * @param paddedImageData   padded input grayscale image
	 * @param paddedImageWidth  padded width (in pixels) of the input image
	 * @param paddedImageHeight padded height (in pixels) of the input image
	 * @param mapDirectionArr   2-D vector (row-major) of IMAP directions and other NMAP codes
	 * @param mappedImageWidth  width (in blocks) of the NMAP
	 * @param mappedImageHeight height (in blocks) of the NMAP
	 * @param imapBlockSize     dimension (in pixels) of each NMAP block
	 * @param dirBinGrids       set of rotated grid offsets used for directional binarization
	 * @param isoBinGridDim     dimension (in pixels) of the grid used for isotropic binarization
	 * @return the binary image, or an empty array on error
	 */
	@SuppressWarnings("unused")
	public int[] binarizeImage(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight,
			int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
			AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
			final int imapBlockSize, RotGrids dirBinGrids, final int isoBinGridDim) {
		int binarizedWidth;
		int binarizedHeight;
		int binarizedXPixel;
		int binarizedYPixel;
		int nMapValue;
		int[] binarizedImageData;
		int binarizedImageIndex;
		int paddedImageIndex;
		int currentPaddedImageIndex;

		/* Compute dimensions of "unpadded" binary image results. */
		binarizedWidth = paddedImageWidth - (dirBinGrids.getPad() << 1);
		binarizedHeight = paddedImageHeight - (dirBinGrids.getPad() << 1);

		binarizedImageData = new int[binarizedWidth * binarizedHeight];
		if (Objects.isNull(binarizedImageData)) {
			logger.error("ERROR : binarizeImage : binarizedImageData : null");
			ret.set(ILfs.ERROR_CODE_110);
			return new int[0];
		}

		binarizedImageIndex = 0;
		currentPaddedImageIndex = 0 + (dirBinGrids.getPad() * paddedImageWidth) + dirBinGrids.getPad();
		for (int iy = 0; iy < binarizedHeight; iy++) {
			/* Set pixel pointer to start of next row in grid. */
			paddedImageIndex = currentPaddedImageIndex;
			for (int ix = 0; ix < binarizedWidth; ix++) {
				/* Compute which block the current pixel is in. */
				binarizedXPixel = (ix / imapBlockSize);
				binarizedYPixel = (iy / imapBlockSize);
				/* Get corresponding value in NMAP */
				nMapValue = mapDirectionArr.get((binarizedYPixel * mappedImageWidth) + binarizedXPixel);
				/* If current block has no neighboring blocks with */
				/* VALID directions ... */
				if (nMapValue == ILfs.NO_VALID_NBRS) {
					/* Set binary pixel to white (255). */
					binarizedImageData[binarizedImageIndex] = ILfs.WHITE_PIXEL;
				}
				/* Otherwise, if block's NMAP has a valid direction ... */
				else if (nMapValue >= 0) {
					/* Use directional binarization based on NMAP direction. */
					binarizedImageData[binarizedImageIndex] = dirbinarize(paddedImageData, paddedImageIndex, nMapValue,
							dirBinGrids);
				} else {
					/* Otherwise, the block's NMAP is either INVALID or */
					/* HIGH-CURVATURE, so use isotropic binarization. */
					binarizedImageData[binarizedImageIndex] = isoBinarize(paddedImageData, paddedImageIndex,
							paddedImageWidth, paddedImageHeight, isoBinGridDim);
				}
				/* Bump input and output pixel pointers. */
				paddedImageIndex++;
				binarizedImageIndex++;
			}
			/* Bump pointer to the next row in padded input image. */
			currentPaddedImageIndex += paddedImageWidth;
		}

		oBinarizedWidth.set(binarizedWidth);
		oBinarizedHeight.set(binarizedHeight);
		ret.set(ILfs.FALSE);
		return binarizedImageData;
	}

	/**
	 * Generates a binary image from a padded grayscale image and its Direction Map (NIST
	 * {@code binarize_image_V2}).
	 * <p>
	 * This version uses no "isotropic" binarization. Pixels in blocks whose direction is
	 * {@link ILfs#INVALID_DIR} become white ({@link ILfs#WHITE_PIXEL}); all others use {@link #dirbinarize}.
	 * The output is {@code 2 * pad} pixels smaller than the input in each dimension.
	 *
	 * @param ret               output: always 0 ({@link ILfs#FALSE}) in this implementation
	 * @param oBinarizedWidth   output: binary image width (in pixels)
	 * @param oBinarizedHeight  output: binary image height (in pixels)
	 * @param paddedImageData   padded input grayscale image
	 * @param paddedImageWidth  padded width (in pixels) of the input image
	 * @param paddedImageHeight padded height (in pixels) of the input image
	 * @param directionMap      2-D vector (row-major) of discrete ridge-flow directions
	 * @param mappedImageWidth  width (in blocks) of the map
	 * @param mappedImageHeight height (in blocks) of the map
	 * @param blocksize         dimension (in pixels) of each map block
	 * @param dirBinGrids       set of rotated grid offsets used for directional binarization
	 * @return the binary image
	 */
	public int[] binarizeImageV2(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight,
			int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
			AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
			final int blocksize, final RotGrids dirBinGrids) {
		int binarizedWidth;
		int binarizedHeight;
		int binarizedXPixel;
		int binarizedYPixel;
		int mapValue;
		int[] binarizedImageData;
		int binarizedImageIndex;
		int paddedImageIndex;
		int currentPaddedImageIndex;

		/* Compute dimensions of "unpadded" binary image results. */
		binarizedWidth = paddedImageWidth - (dirBinGrids.getPad() << 1);
		binarizedHeight = paddedImageHeight - (dirBinGrids.getPad() << 1);

		binarizedImageData = new int[binarizedWidth * binarizedHeight];
		binarizedImageIndex = 0;
		currentPaddedImageIndex = 0 + (dirBinGrids.getPad() * paddedImageWidth) + dirBinGrids.getPad();
		for (int iy = 0; iy < binarizedHeight; iy++) {
			/* Set pixel pointer to start of next row in grid. */
			paddedImageIndex = currentPaddedImageIndex;
			for (int ix = 0; ix < binarizedWidth; ix++) {
				/* Compute which block the current pixel is in. */
				binarizedXPixel = (ix / blocksize);
				binarizedYPixel = (iy / blocksize);
				/* Get corresponding value in Direction Map. */
				mapValue = directionMap.get((binarizedYPixel * mappedImageWidth) + binarizedXPixel);

				/* If current block has has INVALID direction ... */
				if (mapValue == ILfs.INVALID_DIR) {
					/* Set binary pixel to white (255). */
					binarizedImageData[binarizedImageIndex] = ILfs.WHITE_PIXEL;
				}
				/* Otherwise, if block has a valid direction ... */
				else {
					/* Use directional binarization based on block's direction. */
					binarizedImageData[binarizedImageIndex] = dirbinarize(paddedImageData, paddedImageIndex, mapValue,
							dirBinGrids);
				}

				/* Bump input and output pixel pointers. */
				paddedImageIndex++;
				binarizedImageIndex++;
			}
			/* Bump pointer to the next row in padded input image. */
			currentPaddedImageIndex += paddedImageWidth;
		}

		oBinarizedWidth.set(binarizedWidth);
		oBinarizedHeight.set(binarizedHeight);
		ret.set(ILfs.FALSE);

		return binarizedImageData;
	}

	/**
	 * Determines the binary value of a grayscale pixel from a VALID IMAP ridge-flow direction (NIST
	 * {@code dirbinarize}).
	 * <p>
	 * Sums the pixels of the rotated grid for {@code imapDirection}, centered on the current pixel, and
	 * separately keeps the sum of the grid's center row. If the center-row sum, treated as an average (times
	 * the grid height), is less than the total grid sum, the pixel is black; otherwise it is white.
	 * <p>
	 * CAUTION: the image must be padded enough to cover the radius of the rotated grid; otherwise out-of-range
	 * indexes are accessed.
	 *
	 * @param paddedImageData  the padded grayscale image
	 * @param paddedImageIndex index of the current grayscale pixel in {@code paddedImageData}
	 * @param imapDirection    IMAP integer direction of the block the current pixel is in
	 * @param dirBinGrids      set of precomputed rotated grid offsets
	 * @return {@link ILfs#BLACK_PIXEL} (black pixel intensity) or {@link ILfs#WHITE_PIXEL} (white pixel intensity)
	 */
	public int dirbinarize(int[] paddedImageData, final int paddedImageIndex, final int imapDirection,
			final RotGrids dirBinGrids) {
		int gx;
		int gy;
		int gi;
		int cy;
		int rsum;
		int gsum;
		int csum = 0;
		int[] grid;
		double dcy;

		/* Assign nickname pointer. */
		grid = dirBinGrids.getGrids()[imapDirection];
		/* Calculate center (0-oriented) row in grid. */
		dcy = (dirBinGrids.getGridHeight() - 1) / 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		dcy = getDefs().truncDoublePrecision(dcy, ILfs.TRUNC_SCALE);
		cy = getDefs().sRound(dcy);
		/* Initialize grid's pixel offset index to zero. */
		gi = 0;
		/* Initialize grid's pixel accumulator to zero */
		gsum = 0;

		/* Foreach row in grid ... */
		for (gy = 0; gy < dirBinGrids.getGridHeight(); gy++) {
			/* Initialize row pixel sum to zero. */
			rsum = 0;
			/* Foreach column in grid ... */
			for (gx = 0; gx < dirBinGrids.getGridWidth(); gx++) {
				/* Accumulate next pixel along rotated row in grid. */
				rsum += paddedImageData[paddedImageIndex + grid[gi]];
				/* Bump grid's pixel offset index. */
				gi++;
			}
			/* Accumulate row sum into grid pixel sum. */
			gsum += rsum;
			/* If current row is center row, then save row sum separately. */
			if (gy == cy) {
				csum = rsum;
			}
		}

		/* If the center row sum treated as an average is less than the */
		/* total pixel sum in the rotated grid ... */
		if ((csum * dirBinGrids.getGridHeight()) < gsum) {
			/* Set the binary pixel to BLACK. */
			return (ILfs.BLACK_PIXEL);
		} else {
			/* Otherwise set the binary pixel to WHITE. */
			return (ILfs.WHITE_PIXEL);
		}
	}

	/**
	 * Determines the binary value of a grayscale pixel by comparing it with its square neighborhood (NIST
	 * {@code isobinarize}).
	 * <p>
	 * If the current pixel, treated as an average (times the number of grid pixels), is less than the sum of
	 * the pixels in the {@code isoBinGridDim x isoBinGridDim} neighborhood centered on it, the pixel is black;
	 * otherwise it is white. This technique is used when the pixel's block has no VALID IMAP direction.
	 * <p>
	 * CAUTION: the image must be padded enough to cover the radius of the neighborhood; otherwise out-of-range
	 * indexes are accessed.
	 *
	 * @param paddedImageData   the padded grayscale image
	 * @param paddedImageIndex  index of the current grayscale pixel in {@code paddedImageData}
	 * @param paddedImageWidth  padded width (in pixels) of the grayscale image
	 * @param paddedImageHeight padded height (in pixels) of the grayscale image
	 * @param isoBinGridDim     dimension (in pixels) of the square neighborhood
	 * @return {@link ILfs#BLACK_PIXEL} (black pixel intensity) or {@link ILfs#WHITE_PIXEL} (white pixel intensity)
	 */
	public int isoBinarize(int[] paddedImageData, final int paddedImageIndex, final int paddedImageWidth,
			final int paddedImageHeight, final int isoBinGridDim) {
		int currentPaddedImageIndex;
		int currentImageIndex;
		int radius;
		int bsum;
		double drad;

		/* Initialize grid pixel sum to zero. */
		bsum = 0;
		/* Compute radius from current pixel based on isoBinGridDim. */
		drad = (isoBinGridDim - 1) / 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		drad = getDefs().truncDoublePrecision(drad, ILfs.TRUNC_SCALE);
		radius = getDefs().sRound(drad);
		/* Set pointer to origin of grid centered on the current pixel. */
		currentPaddedImageIndex = (paddedImageIndex - (radius * paddedImageWidth) - radius);

		/* For each row in the grid ... */
		for (int py = 0; py < isoBinGridDim; py++) {
			/* Set pixel pointer to start of next row in grid. */
			currentImageIndex = currentPaddedImageIndex;
			/* For each column in the grid ... */
			for (int px = 0; px < isoBinGridDim; px++) {
				/* Accumulate next pixel in the grid. */
				bsum += paddedImageData[currentImageIndex];
				/* Bump pixel pointer. */
				currentImageIndex++;
			}
			/* Bump to the start of the next row in the grid. */
			currentPaddedImageIndex += paddedImageWidth;
		}

		/* If current (center) pixel when treated as an average for the */
		/* entire grid is less than the total pixel sum of the grid ... */
		if ((paddedImageData[paddedImageIndex] * isoBinGridDim * isoBinGridDim) < bsum) {
			/* Set the binary pixel to BLACK. */
			return (ILfs.BLACK_PIXEL);
		} else {
			/* Otherwise, set the binary pixel to WHITE. */
			return (ILfs.WHITE_PIXEL);
		}
	}
}
