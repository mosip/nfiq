package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.IMaps;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block-level image maps for the MINDTCT minutiae detector (Direction, Low Contrast, Low Flow,
 * High Curvature, IMAP and NMAP).
 *
 * This class is the Java port of NIST's {@code maps.c} from the LFS (Latent Fingerprint System)
 * library used by MINDTCT / NFIQ 1.0. The fingerprint image is partitioned into blocks and,
 * using DFT-based analysis of rotated pixel grids ({@link Dft}), each block is assigned:
 * <ul>
 * <li>a <b>Direction Map</b> value: the dominant ridge-flow direction as an integer on the range
 * {@code [0..nDirs)}, or {@link ILfs#INVALID_DIR} ({@code -1}) when none could be determined;</li>
 * <li>a <b>Low Contrast Map</b> flag: {@link ILfs#TRUE} when the block has insufficient
 * contrast (typically background);</li>
 * <li>a <b>Low Flow Map</b> flag: {@link ILfs#TRUE} when no significant ridge flow was found;</li>
 * <li>a <b>High Curvature Map</b> flag: {@link ILfs#TRUE} when the block lies in an area of high
 * curvature (such as cores and deltas).</li>
 * </ul>
 * {@link #genImageMaps} (LFS version 2) produces these four maps and stores them in this
 * object's fields. The version-1 routines {@link #generateInputBlockImageMap} (IMAP) and
 * {@link #genNMap} (NMAP) are also provided. The maps are later used to drive minutia detection
 * and to assign minutia reliability, and ultimately feed the NFIQ quality features.
 *
 * All maps are stored row-major as {@link AtomicIntegerArray}s of
 * {@code mappedImageWidth * mappedImageHeight} entries (block index
 * {@code = y * mappedImageWidth + x}).
 *
 * The class is a lazily created singleton ({@link #getInstance()} and overloads). Unlike most
 * MINDTCT helpers it carries mutable per-image state (the generated maps and their block
 * dimensions), and several methods read the block dimensions from that state rather than from
 * parameters. The singleton accessors are {@code synchronized}, but the instance itself is not
 * thread-safe: concurrent map generation for different images on the same instance will
 * interfere, so callers must serialize access externally.
 */
public class Maps extends MindTct implements IMaps {
	/**
	 * SLF4J logger for this class; used for error messages and, when {@code isShowLogs()} is
	 * enabled, detailed per-block trace output.
	 */
	private static final Logger logger = LoggerFactory.getLogger(Maps.class);
	/**
	 * Lazily created singleton instance, returned by the {@code getInstance} methods and cleared by
	 * {@link #resetInstance()}; guarded by the class monitor.
	 */
	private static Maps instance;
	/**
	 * Direction Map: per-block dominant ridge-flow direction on the range {@code [0..nDirs)}, or
	 * {@link ILfs#INVALID_DIR} ({@code -1}) where no reliable direction exists. Row-major, one entry
	 * per block.
	 */
	private AtomicIntegerArray directionMap;
	/**
	 * Low Contrast Map: per-block flag, {@link ILfs#TRUE} ({@code 1}) for blocks with insufficient
	 * contrast, {@link ILfs#FALSE} ({@code 0}) otherwise. Row-major, one entry per block.
	 */
	private AtomicIntegerArray lowContrastMap;
	/**
	 * Low Ridge Flow Map: per-block flag, {@link ILfs#TRUE} ({@code 1}) for blocks in which DFT
	 * analysis found no significant ridge flow, {@link ILfs#FALSE} ({@code 0}) otherwise. Row-major,
	 * one entry per block.
	 */
	private AtomicIntegerArray lowFlowMap;
	/**
	 * High Curvature Map: per-block flag, {@link ILfs#TRUE} ({@code 1}) for blocks with high
	 * curvature (vorticity or direction change above threshold), {@link ILfs#FALSE} ({@code 0})
	 * otherwise. Row-major, one entry per block.
	 */
	private AtomicIntegerArray highCurveMap;
	/**
	 * Number of blocks horizontally in the (padded) input image, i.e. the width of the maps in
	 * blocks.
	 */
	private AtomicInteger mappedImageWidth;
	/**
	 * Number of blocks vertically in the (padded) input image, i.e. the height of the maps in
	 * blocks.
	 */
	private AtomicInteger mappedImageHeight;

	/**
	 * Creates an empty instance with block dimensions of zero and no maps allocated. Private to
	 * enforce the singleton pattern; use {@link #getInstance()}.
	 */
	private Maps() {
		super();
		setMappedImageWidth(new AtomicInteger(0));
		setMappedImageHeight(new AtomicInteger(0));
	}

	/**
	 * Creates an instance with the given block dimensions and allocates all four maps
	 * ({@code mappedImageWidth * mappedImageHeight} entries each, initialized to zero).
	 *
	 * @param mappedImageWidth  width of the maps, in blocks
	 * @param mappedImageHeight height of the maps, in blocks
	 */
	private Maps(int mappedImageWidth, int mappedImageHeight) {
		super();
		this.mappedImageWidth = new AtomicInteger(mappedImageWidth);
		this.mappedImageHeight = new AtomicInteger(mappedImageHeight);

		/* Compute total number of blocks in map */
		int mapSize = mappedImageWidth * mappedImageHeight;
		directionMap = new AtomicIntegerArray(mapSize);
		lowContrastMap = new AtomicIntegerArray(mapSize);
		lowFlowMap = new AtomicIntegerArray(mapSize);
		highCurveMap = new AtomicIntegerArray(mapSize);
	}

	/**
	 * Creates an instance wrapping existing maps. The block dimensions are left unset
	 * ({@code null}) and must be assigned with the setters before calling methods that rely on them.
	 *
	 * @param directionMap   the Direction Map
	 * @param lowContrastMap the Low Contrast Map
	 * @param lowFlowMap     the Low Ridge Flow Map
	 * @param highCurveMap   the High Curvature Map
	 */
	private Maps(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap, AtomicIntegerArray lowFlowMap,
			AtomicIntegerArray highCurveMap) {
		super();
		this.directionMap = directionMap;
		this.lowContrastMap = lowContrastMap;
		this.lowFlowMap = lowFlowMap;
		this.highCurveMap = highCurveMap;
	}

	/**
	 * Returns the shared singleton instance, creating an empty one on first use.
	 *
	 * @return the singleton {@code Maps} instance, never {@code null}
	 */
	public static synchronized Maps getInstance() {
		if (instance == null) {
			instance = new Maps();
		}
		return instance;
	}

	/**
	 * Discards the current singleton so that the next {@code getInstance} call creates a fresh
	 * instance. Package-private; intended mainly for tests.
	 */
	static synchronized void resetInstance() {
		instance = null;
	}

	/**
	 * Returns the shared singleton instance, creating it with the given block dimensions (and
	 * freshly allocated maps) if it does not exist yet. If the singleton already exists, the
	 * arguments are ignored.
	 *
	 * @param mappedImageWidth  width of the maps, in blocks (used only on first creation)
	 * @param mappedImageHeight height of the maps, in blocks (used only on first creation)
	 * @return the singleton {@code Maps} instance, never {@code null}
	 */
	public static synchronized Maps getInstance(int mappedImageWidth, int mappedImageHeight) {
		if (instance == null) {
			instance = new Maps(mappedImageWidth, mappedImageHeight);
		}
		return instance;
	}

	/**
	 * Returns the shared singleton instance, creating it around the given maps if it does not
	 * exist yet. If the singleton already exists, the arguments are ignored.
	 *
	 * @param directionMap   the Direction Map (used only on first creation)
	 * @param lowContrastMap the Low Contrast Map (used only on first creation)
	 * @param lowFlowMap     the Low Ridge Flow Map (used only on first creation)
	 * @param highCurveMap   the High Curvature Map (used only on first creation)
	 * @return the singleton {@code Maps} instance, never {@code null}
	 */
	public static synchronized Maps getInstance(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
			AtomicIntegerArray lowFlowMap, AtomicIntegerArray highCurveMap) {
		if (instance == null) {
			instance = new Maps(directionMap, lowContrastMap, lowFlowMap, highCurveMap);
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Defs} helper providing numeric utilities (rounding, precision
	 * truncation, floating-point modulo) matching NIST's {@code defs.h} macros.
	 *
	 * @return the {@link Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the shared {@link Block} helper (NIST {@code block.c}) used to compute block offsets,
	 * test low-contrast blocks, find valid neighbor blocks and set margin blocks.
	 *
	 * @return the {@link Block} singleton
	 */
	public Block getBlock() {
		return Block.getInstance();
	}

	/**
	 * Returns the shared {@link Init} helper (NIST {@code init.c}) used to allocate DFT power and
	 * power-statistic arrays.
	 *
	 * @return the {@link Init} singleton
	 */
	public Init getInit() {
		return Init.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper, the Java counterpart of NIST's {@code free.c}
	 * deallocation routines (largely no-ops under garbage collection, kept for parity).
	 *
	 * @return the {@link Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link Dft} helper (NIST {@code dft.c}) used to compute directional DFT
	 * powers and power statistics for each block.
	 *
	 * @return the {@link Dft} singleton
	 */
	public Dft getDft() {
		return Dft.getInstance();
	}

	/**
	 * Returns the shared {@link LfsUtil} helper (NIST {@code util.c}), used here for the closest
	 * direction distance between two integer directions.
	 *
	 * @return the {@link LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Returns the shared {@link Morph} helper (NIST {@code morph.c}) providing binary dilation and
	 * erosion used to morph TRUE/FALSE maps.
	 *
	 * @return the {@link Morph} singleton
	 */
	public Morph getMorph() {
		return Morph.getInstance();
	}

	/**
	 * Computes the Direction, Low Contrast, Low Flow and High Curvature maps for an image, based on
	 * Version 2 of the NIST LFS system.
	 *
	 * Port of NIST {@code gen_image_maps()}. Works for arbitrarily sized, non-square images. Steps:
	 * <ol>
	 * <li>Compute block offsets for the unpadded image (the DFT grids must be square).</li>
	 * <li>Generate the initial Direction, Low Contrast and Low Flow maps
	 * ({@link #initialiseMaps}), then morph the Low Flow Map ({@link #morphMapWithTF}).</li>
	 * <li>Remove inconsistent directions ({@link #removeInconsistentDirs}).</li>
	 * <li>Smooth directions with their neighbors ({@link #smoothDirectionMap}).</li>
	 * <li>Interpolate INVALID blocks from valid neighbors ({@link #interpolateDirectionMap}).</li>
	 * <li>Remove inconsistent directions again.</li>
	 * <li>Smooth directions again.</li>
	 * <li>Set the Direction Map in the image margin to {@link ILfs#INVALID_DIR}.</li>
	 * <li>Generate the High Curvature Map ({@link #generateHighCurveMap}).</li>
	 * </ol>
	 * Unlike the C original, which returns the maps through output pointers, the results are stored
	 * in this instance and must be read back with {@link #getDirectionMap()},
	 * {@link #getLowContrastMap()}, {@link #getLowFlowMap()}, {@link #getHighCurveMap()},
	 * {@link #getMappedImageWidth()} and {@link #getMappedImageHeight()} (map dimensions in blocks).
	 *
	 * @param paddedImagedata   padded input image data (8-bit grayscale, values [0..256)),
	 *                          row-major
	 * @param paddedImageWidth  width of the padded input image, in pixels
	 * @param paddedImageHeight height of the padded input image, in pixels
	 * @param dirToRad          lookup table for converting integer directions to radians
	 *                          (cosine/sine components)
	 * @param dftWaves          structure containing the DFT wave forms
	 * @param dftGrids          structure containing the rotated pixel grid offsets and pad size
	 * @param lfsParams         LFS parameters and thresholds
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion;
	 *         {@link ILfs#ERROR_CODE_540} ({@code -540}) if the DFT grids are not square; another
	 *         negative system error code propagated from a sub-step otherwise
	 */
	public int genImageMaps(int[] paddedImagedata, final int paddedImageWidth, final int paddedImageHeight,
			DirToRad dirToRad, DftWaves dftWaves, RotGrids dftGrids, LfsParams lfsParams) {
		AtomicInteger mappedImageWidth = new AtomicInteger(0);
		AtomicInteger mappedImageHeight = new AtomicInteger(0);
		int imageWidth;
		int imageHeight;
		AtomicIntegerArray blockOffsets;
		AtomicInteger ret = new AtomicInteger(0); // return code

		/* 1. Compute block offsets for the entire image, accounting for pad */
		/* Block_offsets() assumes square block (grid), so ERROR otherwise. */
		if (dftGrids.getGridWidth() != dftGrids.getGridHeight()) {
			logger.error("ERROR : genImageMaps : DFT grids must be square");
			return (ILfs.ERROR_CODE_540);
		}
		/* Compute unpadded image dimensions. */
		imageWidth = paddedImageWidth - (dftGrids.getPad() << 1);
		imageHeight = paddedImageHeight - (dftGrids.getPad() << 1);
		blockOffsets = getBlock().blockOffsets(ret, mappedImageWidth, mappedImageHeight, imageWidth, imageHeight,
				dftGrids.getPad(), lfsParams.getBlockOffsetSize());
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* Compute total number of blocks in map */
		int mapSize = mappedImageWidth.get() * mappedImageHeight.get();

		setMappedImageWidth(new AtomicInteger(mappedImageWidth.get()));
		setMappedImageHeight(new AtomicInteger(mappedImageHeight.get()));

		/* Allocate Direction Map memory */
		setDirectionMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Direction Map to INVALID (-1). */
		for (int dmIndex = 0; dmIndex < getDirectionMap().length(); dmIndex++)
			getDirectionMap().set(dmIndex, ILfs.INVALID_DIR);

		/* Allocate Low Contrast Map memory */
		setLowContrastMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Low Contrast Map to FALSE (0). */
		for (int lcmIndex = 0; lcmIndex < getLowContrastMap().length(); lcmIndex++)
			getLowContrastMap().set(lcmIndex, 0);

		/* Allocate Low Ridge Flow Map memory */
		setLowFlowMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Low Flow Map to FALSE (0). */
		for (int lfmIndex = 0; lfmIndex < getLowFlowMap().length(); lfmIndex++)
			getLowFlowMap().set(lfmIndex, 0);

		/*
		 * 2. Generate initial Direction Map and Low Contrast Map and Low Ridge Flow Map
		 */
		ret.set(initialiseMaps(getDirectionMap(), getLowContrastMap(), getLowFlowMap(), blockOffsets,
				getMappedImageWidth().get(), getMappedImageHeight().get(), paddedImagedata, paddedImageWidth,
				paddedImageHeight, dftWaves, dftGrids, lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().free(blockOffsets);
			return ret.get();
		}

		ret.set(morphMapWithTF(getLowFlowMap(), lfsParams));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* 3. Remove directions that are inconsistent with neighbors */
		removeInconsistentDirs(getDirectionMap(), dirToRad, lfsParams);

		/* 4. Smooth Direction Map values with their neighbors */
		smoothDirectionMap(getDirectionMap(), getLowContrastMap(), dirToRad, lfsParams);

		/* 5. Interpolate INVALID direction blocks with their valid neighbors. */
		ret.set(interpolateDirectionMap(getDirectionMap(), getLowContrastMap(), getMappedImageWidth().get(),
				getMappedImageHeight().get(), lfsParams));
		if (ret.get() != 0) {
			return ret.get();
		}
		/* May be able to skip steps 6 and/or 7 if computation time */
		/* is a critical factor. */

		/* 6. Remove directions that are inconsistent with neighbors */
		removeInconsistentDirs(getDirectionMap(), dirToRad, lfsParams);

		/* 7. Smooth Direction Map values with their neighbors. */
		smoothDirectionMap(getDirectionMap(), getLowContrastMap(), dirToRad, lfsParams);

		/* 8. Set the Direction Map values in the image margin to INVALID. */
		getBlock().setMarginBlocks(getDirectionMap(), getMappedImageWidth().get(), getMappedImageHeight().get(),
				ILfs.INVALID_DIR);

		/* Allocate High Curvature Map. */
		setHighCurveMap(new AtomicIntegerArray(mapSize));
		/* Initialize High Curvature Map to FALSE (0). */
		for (int hcmIndex = 0; hcmIndex < getHighCurveMap().length(); hcmIndex++)
			getHighCurveMap().set(hcmIndex, 0);

		/* 9. Generate High Curvature Map from interpolated Direction Map. */
		ret.set(generateHighCurveMap(getHighCurveMap(), getDirectionMap(), getMappedImageWidth().get(),
				getMappedImageHeight().get(), lfsParams));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* Deallocate working memory. */
		getFree().free(blockOffsets);

		return (ILfs.FALSE);
	}

	/**
	 * Creates the initial Direction Map, Low Contrast Map and Low Flow Map from the padded input
	 * image.
	 *
	 * Port of NIST {@code gen_initial_maps()}. It is very important that the image be properly padded
	 * so that rotated grids along the image boundary do not access unknown memory. For each block,
	 * a window around it (clamped to stay out of the padded border) is first tested for low
	 * contrast; low-contrast blocks are flagged in the Low Contrast Map and keep an INVALID
	 * direction. Otherwise, DFT directional powers and power statistics are computed (skipping the
	 * first DFT wave) and the {@link #primaryDirectionTest} is applied, falling back to the
	 * {@link #secondaryForkTest}. If neither yields a direction, the block is flagged in the Low
	 * Flow Map and its direction stays {@link ILfs#INVALID_DIR}. Typically this initial map will
	 * subsequently have weak or inconsistent directions removed, followed by smoothing.
	 *
	 * The resulting Direction Map contains valid directions {@code >= 0} and INVALID values
	 * {@code = -1}.
	 *
	 * @param oDirectionMap     output Direction Map (pre-allocated, one entry per block); reset to
	 *                          {@link ILfs#INVALID_DIR} and then filled in place
	 * @param oLowContrastMap   output Low Contrast Map (pre-allocated); reset to {@link ILfs#FALSE}
	 *                          and then filled in place
	 * @param oLowFlowMap       output Low Ridge Flow Map (pre-allocated); reset to
	 *                          {@link ILfs#FALSE} and then filled in place
	 * @param blockOffsets      offsets to the pixel origin of each block in the padded image
	 * @param mappedImageWidth  number of blocks horizontally in the padded input image
	 * @param mappedImageHeight number of blocks vertically in the padded input image
	 * @param paddedImagedata   padded input image data (8-bit grayscale, values [0..256)),
	 *                          row-major
	 * @param paddedImageWidth  width of the padded input image, in pixels
	 * @param paddedImageHeight height of the padded input image, in pixels
	 * @param dftWaves          structure containing the DFT wave forms
	 * @param dftGrids          structure containing the rotated pixel grid offsets
	 * @param lfsParams         LFS parameters and thresholds (window size/offset, DFT thresholds)
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion; a negative system error code
	 *         otherwise (the partially filled maps should then be discarded)
	 */
	public int initialiseMaps(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			AtomicIntegerArray oLowFlowMap, AtomicIntegerArray blockOffsets, final int mappedImageWidth,
			final int mappedImageHeight, int[] paddedImagedata, final int paddedImageWidth, final int paddedImageHeight,
			final DftWaves dftWaves, final RotGrids dftGrids, final LfsParams lfsParams) {
		int bi;
		int bSize;
		int blockDir;
		AtomicIntegerArray wis;
		AtomicIntegerArray powmaxDirs;
		AtomicReferenceArray<Double[]> powers;
		AtomicReferenceArray<Double> powmaxs;
		AtomicReferenceArray<Double> pownorms;
		int nStats;
		AtomicInteger ret = new AtomicInteger(0); // return code
		int dftOffset;
		int xminLimit;
		int xmaxLimit;
		int yminLimit;
		int ymaxLimit;
		int winX;
		int winY;
		int lowContrastOffset;

		if (isShowLogs())
			logger.debug("INITIAL MAP");

		/* Compute total number of blocks in map */
		bSize = mappedImageWidth * mappedImageHeight;

		/* Initialize the Direction Map to INVALID (-1). */
		for (int i = 0; i < oDirectionMap.length(); i++)
			oDirectionMap.set(i, ILfs.INVALID_DIR);

		/* Initialize the Low Contrast Map to FALSE (0). */
		for (int i = 0; i < oLowContrastMap.length(); i++)
			oLowContrastMap.set(i, ILfs.FALSE);

		/* Initialize the Low Flow Map to FALSE (0). */
		for (int i = 0; i < oLowFlowMap.length(); i++)
			oLowFlowMap.set(i, ILfs.FALSE);

		/* Allocate DFT directional power vectors */
		powers = getInit().allocDirPowers(ret, dftWaves.getNWaves(), dftGrids.getNoOfGrids());
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			oDirectionMap = null;
			oLowContrastMap = null;
			oLowFlowMap = null;
			return (ret.get());
		}

		/* Allocate DFT power statistic arrays */
		/* Compute length of statistics arrays. Statistics not needed */
		/* for the first DFT wave, so the length is number of waves - 1. */
		nStats = dftWaves.getNWaves() - 1;
		wis = new AtomicIntegerArray(nStats);
		powmaxs = new AtomicReferenceArray<Double>(nStats);
		powmaxDirs = new AtomicIntegerArray(nStats);
		pownorms = new AtomicReferenceArray<Double>(nStats);

		/* Compute special window origin limits for determining low contrast. */
		/* These pixel limits avoid analyzing the padded borders of the image. */
		xminLimit = dftGrids.getPad();
		yminLimit = dftGrids.getPad();
		xmaxLimit = paddedImageWidth - dftGrids.getPad() - lfsParams.getWindowSize() - 1;
		ymaxLimit = paddedImageHeight - dftGrids.getPad() - lfsParams.getWindowSize() - 1;

		/* Foreach block in image ... */
		for (bi = 0; bi < bSize; bi++) {
			/* Adjust block offset from pointing to block origin to pointing */
			/* to surrounding window origin. */
			dftOffset = blockOffsets.get(bi) - (lfsParams.getWindowOffset() * paddedImageWidth)
					- lfsParams.getWindowOffset();

			/* Compute pixel coords of window origin. */
			winX = dftOffset % paddedImageWidth;
			winY = (int) (dftOffset / paddedImageWidth);

			/* Make sure the current window does not access padded image pixels */
			/* for analyzing low contrast. */
			winX = Math.max(xminLimit, winX);
			winX = Math.min(xmaxLimit, winX);
			winY = Math.max(yminLimit, winY);
			winY = Math.min(ymaxLimit, winY);
			lowContrastOffset = (winY * paddedImageWidth) + winX;

			if (isShowLogs())
				logger.debug("   MAP BLOCK {} ({}, {}) ", bi, bi % mappedImageWidth,
						bi / mappedImageWidth);

			/* If block is low contrast ... */
			ret.set(getBlock().lowContrastBlock(lowContrastOffset, lfsParams.getWindowSize(), paddedImagedata,
					paddedImageWidth, paddedImageHeight, lfsParams));
			if (ret.get() != ILfs.FALSE) {
				/* If system error ... */
				if (ret.get() < ILfs.FALSE) {
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					wis = null;
					powmaxs = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				/* Otherwise, block is low contrast ... */
				if (isShowLogs())
					logger.debug("LOW CONTRAST");
				oLowContrastMap.set(bi, ILfs.TRUE);// = 1 = true
				/* Direction Map's block is already set to INVALID. */
			}
			/* Otherwise, sufficient contrast for DFT processing ... */
			else {
				if (isShowLogs())
					logger.debug("");
				/* Compute DFT powers */
				ret.set(getDft().dftDirPowers(powers, paddedImagedata, lowContrastOffset, paddedImageWidth,
						paddedImageHeight, dftWaves, dftGrids));
				if (ret.get() != ILfs.FALSE) {
					/* Free memory allocated to this point. */
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					wis = null;
					powmaxs = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				/* Compute DFT power statistics, skipping first applied DFT */
				/* wave. This is dependent on how the primary and secondary */
				/* direction tests work below. */
				ret.set(getDft().getDftPowerStats(wis, powmaxs, powmaxDirs, pownorms, powers, ILfs.TRUE,
						dftWaves.getNWaves(), dftGrids.getNoOfGrids()));
				if (ret.get() != ILfs.FALSE) {
					/* Free memory allocated to this point. */
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					wis = null;
					powmaxs = null;
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				if (isShowLogs()) {
					int _w;
					logger.debug("      Power");
					for (_w = 0; _w < nStats; _w++) {
						/* Add 1 to wis[w] to create index to original dft_coefs[] */
						logger.debug("         wis[{}] {} {} {} {} {}", _w, wis.get(_w) + 1,
								powmaxs.get(wis.get(_w)), powmaxDirs.get(wis.get(_w)), pownorms.get(wis.get(_w)),
								powers.get(0)[powmaxDirs.get(wis.get(_w))]);
					}
				}

				/* Conduct primary direction test */
				blockDir = primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
				if (blockDir != ILfs.INVALID_DIR) {
					oDirectionMap.set(bi, blockDir);
				} else {
					/* Conduct secondary (fork) direction test */
					blockDir = secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
					if (blockDir != ILfs.INVALID_DIR) {
						oDirectionMap.set(bi, blockDir);
					}
					/* Otherwise current direction in Direction Map remains INVALID */
					else {
						/* Flag the block as having LOW RIDGE FLOW. */
						oLowFlowMap.set(bi, ILfs.TRUE);
					}
				}
			} // End DFT
		} // bi

		/* Deallocate working memory */
		getFree().freeDirPowers(powers, dftWaves.getNWaves());
		wis = null;
		powmaxs = null;
		powmaxDirs = null;
		pownorms = null;

		return ILfs.FALSE;
	}

	/**
	 * Fills in INVALID directions in the Direction Map from each block's valid neighbors.
	 *
	 * Port of NIST {@code interpolate_direction_map()}. For every block that is not LOW CONTRAST
	 * and has an INVALID direction, the nearest valid block is searched for in each of the four
	 * compass directions ({@link Block#findValidBlock}); low-contrast blocks stop the search, which
	 * keeps the process from interpolating directions in the background and along the perimeter of
	 * the fingerprint. If at least {@link LfsParams#getMinInterpolateNbrs()} neighbors are found,
	 * their directions are combined in a weighted average inversely related to their distance
	 * (in blocks) from the block, truncated to {@link ILfs#TRUNC_SCALE} precision for
	 * cross-platform consistency and rounded. Results are computed into a working map and then
	 * copied back, so interpolation uses only the original values.
	 *
	 * @param oDirectionMap     Direction Map; updated in place with the interpolated results
	 * @param oLowContrastMap   Low Contrast Map of blocks flagged as LOW CONTRAST
	 * @param mappedImageWidth  number of blocks horizontally in the maps
	 * @param mappedImageHeight number of blocks vertically in the maps
	 * @param lfsParams         LFS parameters and thresholds
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion (no error path exists in this
	 *         implementation)
	 */
	public int interpolateDirectionMap(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int newDir;
		AtomicInteger northDir = new AtomicInteger(0);
		AtomicInteger eastDir = new AtomicInteger(0);
		AtomicInteger southDir = new AtomicInteger(0);
		AtomicInteger westDir = new AtomicInteger(0);
		int northDist = 0;
		int eastDist = 0;
		int southDist = 0;
		int westDist = 0;
		int totalDist;
		int northFound;
		int eastFound;
		int southFound;
		int westFound;
		int totalFound;
		int northDelta = 0;
		int eastDelta = 0;
		int southDelta = 0;
		int westDelta = 0;
		int totalDelta;
		AtomicInteger nbrX = new AtomicInteger(0);
		AtomicInteger nbrY = new AtomicInteger(0);
		AtomicIntegerArray oMap;
		int dptrIndex = 0;
		int cptrIndex = 0;
		int mptrIndex = 0;
		double avrDir;

		if (isShowLogs())
			logger.debug("INTERPOLATE DIRECTION MAP STARTED");

		/* Allocate output (interpolated) Direction Map. */
		oMap = new AtomicIntegerArray(mappedImageWidth * mappedImageHeight);

		/* Set pointers to the first block in the maps. */
		dptrIndex = 0;
		cptrIndex = 0;
		mptrIndex = 0;

		/* Foreach block in the maps ... */
		for (int y = 0; y < mappedImageHeight; y++) {
			for (int x = 0; x < mappedImageWidth; x++) {
				/* If image block is NOT LOW CONTRAST and has INVALID direction ... */
				if ((oLowContrastMap.get(cptrIndex) == 0) && (oDirectionMap.get(dptrIndex) == ILfs.INVALID_DIR)) {
					/* Set neighbor accumulators to 0. */
					totalFound = 0;
					totalDist = 0;

					/* Find north neighbor. */
					if ((northFound = getBlock().findValidBlock(northDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 0, -1)) == ILfs.FOUND) {
						/* Compute north distance. */
						northDist = y - nbrY.get();
						/* Accumulate neighbor distance. */
						totalDist += northDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find east neighbor. */
					if ((eastFound = getBlock().findValidBlock(eastDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 1, 0)) == ILfs.FOUND) {
						/* Compute east distance. */
						eastDist = nbrX.get() - x;
						/* Accumulate neighbor distance. */
						totalDist += eastDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find south neighbor. */
					if ((southFound = getBlock().findValidBlock(southDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 0, 1)) == ILfs.FOUND) {
						/* Compute south distance. */
						southDist = nbrY.get() - y;
						/* Accumulate neighbor distance. */
						totalDist += southDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find west neighbor. */
					if ((westFound = getBlock().findValidBlock(westDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, -1, 0)) == ILfs.FOUND) {
						/* Compute west distance. */
						westDist = x - nbrX.get();
						/* Accumulate neighbor distance. */
						totalDist += westDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* If a sufficient number of neighbors found (Ex. 2) ... */
					if (totalFound >= lfsParams.getMinInterpolateNbrs()) {
						/* Accumulate weighted sum of neighboring directions */
						/* inversely related to the distance from current block. */
						totalDelta = (int) 0.0;
						/* If neighbor found to the north ... */
						if (northFound != ILfs.FALSE) {
							northDelta = totalDist - northDist;
							totalDelta += northDelta;
						}
						/* If neighbor found to the east ... */
						if (eastFound != ILfs.FALSE) {
							eastDelta = totalDist - eastDist;
							totalDelta += eastDelta;
						}
						/* If neighbor found to the south ... */
						if (southFound != ILfs.FALSE) {
							southDelta = totalDist - southDist;
							totalDelta += southDelta;
						}
						/* If neighbor found to the west ... */
						if (westFound != ILfs.FALSE) {
							westDelta = totalDist - westDist;
							totalDelta += westDelta;
						}

						avrDir = 0.0;

						if (northFound != ILfs.FALSE) {
							avrDir += (northDir.get() * (northDelta / (double) totalDelta));
						}
						if (eastFound != ILfs.FALSE) {
							avrDir += (eastDir.get() * (eastDelta / (double) totalDelta));
						}
						if (southFound != ILfs.FALSE) {
							avrDir += (southDir.get() * (southDelta / (double) totalDelta));
						}
						if (westFound != ILfs.FALSE) {
							avrDir += (westDir.get() * (westDelta / (double) totalDelta));
						}

						/* Need to truncate precision so that answers are consistent */
						/* on different computer architectures when rounding doubles. */
						avrDir = getDefs().truncDoublePrecision(avrDir, ILfs.TRUNC_SCALE);

						/* Assign interpolated direction to output Direction Map. */
						newDir = getDefs().sRound(avrDir);

						if (isShowLogs())
							logger.debug("Block {},{} INTERP numnbs={} newdir={}", x, y, totalFound, newDir);

						oMap.set(mptrIndex, newDir);
					} else {
						/* Otherwise, the direction remains INVALID. */
						oMap.set(mptrIndex, oDirectionMap.get(dptrIndex));
					}
				} else {
					/* Otherwise, assign the current direction to the output block. */
					oMap.set(mptrIndex, oDirectionMap.get(dptrIndex));
				}

				/* Bump to the next block in the maps ... */
				dptrIndex++;
				cptrIndex++;
				mptrIndex++;
			}
		}

		/* Copy the interpolated directions into the input map. */
		for (int mapIndex = 0; mapIndex < oMap.length(); mapIndex++)
			oDirectionMap.set(mapIndex, oMap.get(mapIndex));

		/* Deallocate the working memory. */
		getFree().free(oMap);

		if (isShowLogs())
			logger.debug("INTERPOLATE DIRECTION MAP ENDED");
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Dilates and erodes a TRUE/FALSE block map in an attempt to fill voids in it.
	 *
	 * Port of NIST {@code morph_TF_map()}. The map is copied into a working binary image, dilated
	 * twice and then eroded twice ({@link Morph#dilateImage2}, {@link Morph#erodeImage2}), and the
	 * result is copied back. The map dimensions are taken from this instance's
	 * {@link #getMappedImageWidth()} and {@link #getMappedImageHeight()}.
	 *
	 * @param tfMap     TRUE/FALSE map of block values; replaced in place by the morphed map
	 * @param lfsParams LFS parameters and thresholds (unused, kept for parity with NIST)
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion
	 */
	public int morphMapWithTF(AtomicIntegerArray tfMap, final LfsParams lfsParams) {
		int[] cimage;
		int[] mimage;
		int cptrIndex;
		int mptrIndex;
		int i;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();

		if (isShowLogs())
			logger.debug("morphMapWithTF Started ({}, {})", mappedImageWidth, mappedImageHeight);
		/* Convert TRUE/FALSE map into a binary byte image. */
		int mSize = mappedImageWidth * mappedImageHeight;
		cimage = new int[mSize];
		mimage = new int[mSize];

		cptrIndex = 0;
		mptrIndex = 0;
		for (i = 0; i < mSize; i++) {
			cimage[cptrIndex++] = tfMap.get(mptrIndex++);
		}

		getMorph().dilateImage2(cimage, mimage, mappedImageWidth, mappedImageHeight);
		getMorph().dilateImage2(mimage, cimage, mappedImageWidth, mappedImageHeight);
		getMorph().erodeImage2(cimage, mimage, mappedImageWidth, mappedImageHeight);
		getMorph().erodeImage2(mimage, cimage, mappedImageWidth, mappedImageHeight);

		cptrIndex = 0;
		mptrIndex = 0;
		for (i = 0; i < mSize; i++) {
			tfMap.set(mptrIndex++, cimage[cptrIndex++]);
		}

		getFree().free(cimage);
		getFree().free(mimage);

		return (ILfs.FALSE);
	}

	/**
	 * Expands a block map to pixel resolution, assigning each pixel its block's value.
	 *
	 * Port of NIST {@code pixelize_map()}. This allows block values in maps to be directly accessed
	 * via pixel addresses. Block offsets are computed for an unpadded image of the given size with
	 * the given block size; they must yield exactly {@code mapWidth x mapHeight} blocks.
	 *
	 * @param oMap               output pixelized map; must be pre-allocated by the caller with
	 *                           {@code imageWidth * imageHeight} entries, and is filled in place
	 * @param imageWidth         width of the corresponding image, in pixels
	 * @param imageHeight        height of the corresponding image, in pixels
	 * @param inputBlockImageMap input block map (one entry per block)
	 * @param mapWidth           width of the block map, in blocks
	 * @param mapHeight          height of the block map, in blocks
	 * @param blockSize          dimension of each (square) block, in pixels
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion;
	 *         {@link ILfs#ERROR_CODE_591} ({@code -591}) if the computed block dimensions do not
	 *         match {@code mapWidth}/{@code mapHeight}; another negative system error code from
	 *         block offset computation otherwise
	 */
	public int pixelizeMap(AtomicIntegerArray oMap, int imageWidth, int imageHeight,
			AtomicIntegerArray inputBlockImageMap, final int mapWidth, final int mapHeight, final int blockSize) {
		AtomicInteger ret = new AtomicInteger(0);
		AtomicIntegerArray blockOffsets = null;
		AtomicInteger oBlockOffsetWidth = new AtomicInteger(0);
		AtomicInteger oBlockOffsetHeight = new AtomicInteger(0);
		int mapIndex;
		int blockOffsetsIndex = 0;
		int mapCurrentIndex = 0;

		// Assigned before only // pmap

		blockOffsets = getBlock().blockOffsets(ret, oBlockOffsetWidth, oBlockOffsetHeight, imageWidth, imageHeight, 0,
				blockSize);
		if (ret.get() != ILfs.FALSE) {
			oMap = null;
			return ret.get();
		}

		if ((oBlockOffsetWidth.get() != mapWidth) || (oBlockOffsetHeight.get() != mapHeight)) {
			logger.error("ERROR : pixelizeMap : block dimensions do not match");
			blockOffsets = null;
			oMap = null;
			ret.set(ILfs.ERROR_CODE_591);
			return ret.get();
		}

		for (mapIndex = 0; mapIndex < mapWidth * mapHeight; mapIndex++) {
			blockOffsetsIndex = 0 + blockOffsets.get(mapIndex);
			for (int y = 0; y < blockSize; y++) {
				mapCurrentIndex = blockOffsetsIndex;
				for (int x = 0; x < blockSize; x++) {
					oMap.set(mapCurrentIndex++, inputBlockImageMap.get(mapIndex));
				}
				blockOffsetsIndex += imageWidth;
			}
		}

		blockOffsets = null;
		/* Assign pixelized map to output pointer. */

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return ret.get();
	}

	/**
	 * Smooths the Direction Map by analyzing the directions of each block's 8 neighbors.
	 *
	 * Port of NIST {@code smooth_direction_map()}. For every block that is not LOW CONTRAST, the
	 * average neighbor direction, its strength and the number of valid neighbors are computed
	 * ({@link #average8NbrDir}). If the strength is at least
	 * {@link LfsParams#getDirStrengthMin()} (e.g. 0.2), then a valid block is replaced by the
	 * average when it has at least {@link LfsParams#getRmvValidNbrMin()} (e.g. 3) valid neighbors,
	 * and an INVALID block is assigned the average when it has at least
	 * {@link LfsParams#getSmoothValidNbrMin()} (e.g. 7) valid neighbors. Updates are applied in
	 * place in raster order, so later blocks see already-smoothed neighbors, as in NIST.
	 *
	 * The map dimensions are taken from this instance's {@link #getMappedImageWidth()} and
	 * {@link #getMappedImageHeight()}.
	 *
	 * @param oDirectionMap   Direction Map; smoothed in place
	 * @param oLowContrastMap Low Contrast Map; LOW CONTRAST blocks are left unchanged
	 * @param dirToRad        lookup table for converting integer directions to radians
	 * @param lfsParams       LFS parameters and thresholds
	 */
	public void smoothDirectionMap(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			final DirToRad dirToRad, final LfsParams lfsParams) {
		AtomicInteger oAverageDir = new AtomicInteger(0);
		AtomicInteger oValid = new AtomicInteger(0);
		AtomicReference<Double> oDirectionStrength = new AtomicReference<>();

		if (isShowLogs())
			logger.debug("SMOOTH DIRECTION MAP");
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();
		/* Assign pointers to beginning of both maps. */
		int directionMapIndex = 0;
		int lowContrastMapIndex = 0;

		/* Foreach block in maps ... */
		for (int mappedYIndex = 0; mappedYIndex < mappedImageHeight; mappedYIndex++) {
			for (int mappedXIndex = 0; mappedXIndex < mappedImageWidth; mappedXIndex++) {
				/* If the current block does NOT have LOW CONTRAST ... */
				if (oLowContrastMap.get(lowContrastMapIndex) == ILfs.FALSE) {
					/* Compute average direction from neighbors, returning the */
					/* number of valid neighbors used in the computation, and */
					/* the "strength" of the average direction. */
					average8NbrDir(oAverageDir, oDirectionStrength, oValid, oDirectionMap, mappedXIndex, mappedYIndex,
							mappedImageWidth, mappedImageHeight, dirToRad);

					/* If average direction strength is strong enough */
					/* (Ex. thresh==0.2)... */
					if (oDirectionStrength.get() >= lfsParams.getDirStrengthMin()) {
						/* If Direction Map direction is valid ... */
						if (oDirectionMap.get(directionMapIndex) != ILfs.INVALID_DIR) {
							/* Conduct valid neighbor test (Ex. thresh==3)... */
							if (oValid.get() >= lfsParams.getRmvValidNbrMin()) {
								if (isShowLogs()) {
									logger.debug("   SMOOTH DIRECTION BLOCK {} ({}, {})",
											mappedXIndex + (mappedYIndex * mappedImageWidth), mappedXIndex,
											mappedYIndex);
									logger.debug("      Average NBR :   {} {} {}", oAverageDir.get(),
											oDirectionStrength.get(), oValid.get());
									logger.debug("      1. Valid NBR ({} >= {})", oValid.get(),
											lfsParams.getRmvValidNbrMin());
									logger.debug("      Valid Direction = {}", oDirectionMap.get(directionMapIndex));
									logger.debug("      Smoothed Direction = {}", oAverageDir.get());
								}
								/* Reassign valid direction with average direction. */
								oDirectionMap.set(directionMapIndex, oAverageDir.get());
							}
						}
						/* Otherwise direction is invalid ... */
						else {
							/* Even if DIRECTION_MAP value is invalid, if number of */
							/* valid neighbors is big enough (Ex. thresh==7)... */
							if (oValid.get() >= lfsParams.getSmoothValidNbrMin()) {
								if (isShowLogs()) {
									logger.debug("   SMOOTH DIRECTION BLOCK {} ({}, {})",
											mappedXIndex + (mappedYIndex * mappedImageWidth), mappedXIndex,
											mappedYIndex);
									logger.debug("      Average NBR :   {} {} {}", oAverageDir.get(),
											oDirectionStrength.get(), oValid.get());
									logger.debug("      2. Invalid NBR ({} >= {})", oValid.get(),
											lfsParams.getSmoothValidNbrMin());
									logger.debug("      Invalid Direction = {}", oDirectionMap.get(directionMapIndex));
									logger.debug("      Smoothed Direction = {}", oAverageDir.get());
								}
								/* Assign invalid direction with average direction. */
								oDirectionMap.set(directionMapIndex, oAverageDir.get());
							}
						}
					}
				}
				/* Otherwise, block has LOW CONTRAST, so keep INVALID direction. */
				/* Bump to next block in maps. */
				directionMapIndex++;
				lowContrastMapIndex++;
			}
		}
	}

	/**
	 * Generates the High Curvature Map from a Direction Map.
	 *
	 * Port of NIST {@code gen_high_curve_map()}. For each block with at least one valid neighbor:
	 * if its direction is INVALID and it has at least {@link LfsParams#getVortValidNbrMin()} valid
	 * neighbors, it is flagged when its neighbors' {@link #vorticity} reaches
	 * {@link LfsParams#getHighcurvVorticityMin()}; if its direction is valid, it is flagged when its
	 * {@link #curvature} reaches {@link LfsParams#getHighcurvCurvatureMin()}.
	 *
	 * @param oHighCurvatureMap output High Curvature Map; must be pre-allocated with one entry per
	 *                          block and initialized to {@link ILfs#FALSE}. Flagged blocks are set to
	 *                          {@link ILfs#TRUE} in place
	 * @param oDirectionMap     Direction Map of blocks containing directional ridge flow
	 * @param mappedImageWidth  width of the maps, in blocks
	 * @param mappedImageHeight height of the maps, in blocks
	 * @param lfsParams         LFS parameters and thresholds
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion
	 */
	public int generateHighCurveMap(AtomicIntegerArray oHighCurvatureMap, AtomicIntegerArray oDirectionMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		AtomicInteger nvalid = new AtomicInteger(0);
		int curvatureMeasure = 0;
		int vorticityMeasure = 0;

		int highCurvatureMapIndex = 0;
		int directionMapIndex = 0;
		/* Foreach row in maps ... */
		for (int mappedYIndex = 0; mappedYIndex < mappedImageHeight; mappedYIndex++) {
			for (int mappedXIndex = 0; mappedXIndex < mappedImageWidth; mappedXIndex++) {
				/* Count number of valid neighbors around current block ... */
				nvalid.set(
						numValid8Nbrs(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth, mappedImageHeight));
				/* If valid neighbors exist ... */
				if (nvalid.get() > ILfs.FALSE) {
					/* If current block's direction is INVALID ... */
					if (oDirectionMap.get(directionMapIndex) == ILfs.INVALID_DIR) {
						/* If a sufficient number of VALID neighbors exists ... */
						if (nvalid.get() >= lfsParams.getVortValidNbrMin()) {
							/* Measure vorticity of neighbors. */
							vorticityMeasure = vorticity(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth,
									mappedImageHeight, lfsParams.getNumDirections());
							/* If vorticity is sufficiently high ... */
							if (vorticityMeasure >= lfsParams.getHighcurvVorticityMin()) {
								/* Flag block as HIGH CURVATURE. */
								oHighCurvatureMap.set(highCurvatureMapIndex, ILfs.TRUE);
							}
						}
					}
					/* Otherwise block has valid direction ... */
					else {
						/* Measure curvature around the valid block. */
						curvatureMeasure = curvature(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth,
								mappedImageHeight, lfsParams.getNumDirections());
						/* If curvature is sufficiently high ... */
						if (curvatureMeasure >= lfsParams.getHighcurvCurvatureMin()) {
							oHighCurvatureMap.set(highCurvatureMapIndex, ILfs.TRUE);
						}
					}
				}
				/* Else (nvalid <= 0) */
				/* Bump pointers to next block in maps. */
				directionMapIndex++;
				highCurvatureMapIndex++;
			}
		}
		/* Return normally. */
		return ILfs.FALSE;
	}

	/**
	 * Computes an IMAP: a 2D vector of integer directions, each representing the dominant ridge flow
	 * in a block of the input grayscale image (LFS version 1).
	 *
	 * Port of NIST {@code gen_imap()}. Works for arbitrarily sized, non-square images. Steps: compute
	 * block offsets (the DFT grids must be square and define the block size), build the initial IMAP
	 * ({@link #initialiseInputBlockImageMap}), remove inconsistent directions
	 * ({@link #removeInconsistentDirs}) and smooth the result ({@link #smoothInputBlockImageMap}).
	 *
	 * Note: the removal and smoothing steps read the map dimensions from this instance's
	 * {@link #getMappedImageWidth()}/{@link #getMappedImageHeight()} rather than from the
	 * dimensions computed here, so those fields must already match the IMAP size.
	 *
	 * @param ret                output return code: {@link ILfs#FALSE} ({@code 0}) on successful
	 *                           completion; {@link ILfs#ERROR_CODE_60} ({@code -60}) if the DFT grids
	 *                           are not square; another negative system error code otherwise
	 * @param oMappedImageWidth  output width of the IMAP, in blocks (set on success)
	 * @param oMappedImageHeight output height of the IMAP, in blocks (set on success)
	 * @param paddedImagedata    padded input image data (8-bit grayscale, values [0..256)),
	 *                           row-major
	 * @param paddedImageWidth   width of the padded input image, in pixels
	 * @param paddedImageHeight  height of the padded input image, in pixels
	 * @param dirToRad           lookup table for converting integer directions to radians
	 * @param dftWaves           structure containing the DFT wave forms
	 * @param dftGrids           structure containing the rotated pixel grid offsets
	 * @param lfsParams          LFS parameters and thresholds
	 * @return the created IMAP (one direction or {@link ILfs#INVALID_DIR} per block), or
	 *         {@code null} if an error occurred (see {@code ret})
	 */
	public AtomicIntegerArray generateInputBlockImageMap(AtomicInteger ret, AtomicInteger oMappedImageWidth,
			AtomicInteger oMappedImageHeight, int[] paddedImagedata, final int paddedImageWidth,
			final int paddedImageHeight, final DirToRad dirToRad, final DftWaves dftWaves, final RotGrids dftGrids,
			final LfsParams lfsParams) {
		AtomicIntegerArray oInputBlockImageMap = null;
		AtomicInteger mappedImageWidth = new AtomicInteger(0);
		AtomicInteger mappedImageHeight = new AtomicInteger(0);
		int imageWidth;
		int imageHeight;
		AtomicIntegerArray blockOffsets;

		/* 1. Compute block offsets for the entire image, accounting for pad */
		/* Block_offsets() assumes square block (grid), so ERROR otherwise. */
		if (dftGrids.getGridWidth() != dftGrids.getGridHeight()) {
			logger.error("ERROR : generateInputBlockImageMap : DFT grids must be square");
			ret.set(ILfs.ERROR_CODE_60);
			return oInputBlockImageMap;
		}
		/* Compute unpadded image dimensions. */
		imageWidth = paddedImageWidth - (dftGrids.getPad() << 1);
		imageHeight = paddedImageHeight - (dftGrids.getPad() << 1);

		blockOffsets = getBlock().blockOffsets(ret, mappedImageWidth, mappedImageHeight, imageWidth, imageHeight,
				dftGrids.getPad(), dftGrids.getGridWidth());
		if (ret.get() != ILfs.FALSE) {
			return oInputBlockImageMap;
		}

		/* 2. initial imap */
		oInputBlockImageMap = initialiseInputBlockImageMap(ret, blockOffsets, mappedImageWidth, mappedImageHeight,
				paddedImagedata, paddedImageWidth, paddedImageHeight, dftWaves, dftGrids, lfsParams);
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			blockOffsets = null;
			return oInputBlockImageMap;
		}

		/* Steps 3 and 4 read the map dimensions from this instance. */
		setMappedImageWidth(new AtomicInteger(mappedImageWidth.get()));
		setMappedImageHeight(new AtomicInteger(mappedImageHeight.get()));

		/* 3. Remove IMAP directions that are inconsistent with neighbors */
		removeInconsistentDirs(oInputBlockImageMap, dirToRad, lfsParams);

		/* 4. Smooth imap values with their neighbors */
		smoothInputBlockImageMap(oInputBlockImageMap, dirToRad, lfsParams);

		/* Deallocate working memory. */
		blockOffsets = null;

		oMappedImageWidth.set(mappedImageWidth.get());
		oMappedImageHeight.set(mappedImageHeight.get());
		ret.set(ILfs.FALSE);
		return oInputBlockImageMap;
	}

	/**
	 * Creates an initial IMAP from the padded input image.
	 *
	 * Port of NIST {@code gen_initial_imap()}. It is very important that the image be properly padded
	 * so that rotated grids along the image boundary do not access unknown memory. For each block,
	 * DFT directional powers and power statistics (skipping the first DFT wave) are computed and the
	 * {@link #primaryDirectionTest} is applied, falling back to the {@link #secondaryForkTest};
	 * blocks for which neither yields a direction remain {@link ILfs#INVALID_DIR}. Typically this
	 * initial vector of directions will subsequently have weak or inconsistent directions removed,
	 * followed by smoothing.
	 *
	 * @param ret               output return code: {@link ILfs#FALSE} ({@code 0}) on successful
	 *                          completion; {@link ILfs#ERROR_CODE_70} ({@code -70}) if the IMAP could
	 *                          not be allocated; another negative system error code otherwise
	 * @param blockOffsets      offsets to the pixel origin of each block in the padded image
	 * @param mappedImageWidth  number of blocks horizontally in the padded input image
	 * @param mappedImageHeight number of blocks vertically in the padded input image
	 * @param paddedImagedata   padded input image data (8-bit grayscale, values [0..256)),
	 *                          row-major
	 * @param paddedImageWidth  width of the padded input image, in pixels
	 * @param paddedImageHeight height of the padded input image, in pixels
	 * @param dftWaves          structure containing the DFT wave forms
	 * @param dftGrids          structure containing the rotated pixel grid offsets
	 * @param lfsParams         LFS parameters and thresholds
	 * @return the newly created IMAP, or {@code null} if an error occurred (see {@code ret})
	 */
	@SuppressWarnings("unused")
	public AtomicIntegerArray initialiseInputBlockImageMap(AtomicInteger ret, AtomicIntegerArray blockOffsets,
			final AtomicInteger mappedImageWidth, final AtomicInteger mappedImageHeight, int[] paddedImagedata,
			final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves, final RotGrids dftGrids,
			final LfsParams lfsParams) {
		AtomicIntegerArray inputBlockImageMap = null;
		int bSize;
		int blockDir;
		AtomicIntegerArray wis;
		AtomicIntegerArray powmaxDirs;
		AtomicReferenceArray<Double[]> powers;
		AtomicReferenceArray<Double> powmaxs = null;
		AtomicReferenceArray<Double> pownorms = null;
		int nStats;

		if (isShowLogs())
			logger.debug("INITIAL MAP");
		/* Compute total number of blocks in IMAP */
		bSize = mappedImageWidth.get() * mappedImageHeight.get();
		inputBlockImageMap = new AtomicIntegerArray(bSize);
		if (inputBlockImageMap == null) {
			logger.error("ERROR : initialiseInputBlockImageMap : imap : NULL");
			ret.set(ILfs.ERROR_CODE_70);
			return inputBlockImageMap;
		}
		/* Initialize IMAP to INVALID_DIR */
		for (int blockIndex = 0; blockIndex < bSize; blockIndex++) {
			inputBlockImageMap.set(blockIndex, ILfs.INVALID_DIR);
		}

		/* Allocate DFT directional power vectors */
		powers = getInit().allocDirPowers(ret, dftWaves.getNWaves(), dftGrids.getNoOfGrids());
		if (ret.get() != ILfs.FALSE) {
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		/* Allocate DFT power statistic arrays */
		/* Compute length of statistics arrays. Statistics not needed */
		/* for the first DFT wave, so the length is number of waves - 1. */
		nStats = dftWaves.getNWaves() - 1;
		wis = getInit().allocPowerStatsWis(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		powmaxs = getInit().allocPowerStatsPowmaxs(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		powmaxDirs = getInit().allocPowerStatsPowmaxDirs(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().free(powmaxs);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		pownorms = getInit().allocPowerStatsPownorms(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().free(powmaxs);
			getFree().free(powmaxDirs);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		/* Foreach block in imap ... */
		for (int blockOffsetIndex = 0; blockOffsetIndex < bSize; blockOffsetIndex++) {
			/* Compute DFT powers */
			ret.set(getDft().dftDirPowers(powers, paddedImagedata, blockOffsets.get(blockOffsetIndex), paddedImageWidth,
					paddedImageHeight, dftWaves, dftGrids));
			if (ret.get() != ILfs.FALSE) {
				getFree().free(inputBlockImageMap);
				getFree().freeDirPowers(powers, dftWaves.getNWaves());
				getFree().free(wis);
				getFree().free(powmaxs);
				getFree().free(powmaxDirs);
				getFree().free(pownorms);
				inputBlockImageMap = null;
				return inputBlockImageMap;
			}

			/* Compute DFT power statistics, skipping first applied DFT */
			/* wave. This is dependent on how the primary and secondary */
			/* direction tests work below. */
			ret.set(getDft().getDftPowerStats(wis, powmaxs, powmaxDirs, pownorms, powers, 1, dftWaves.getNWaves(),
					dftGrids.getNoOfGrids()));
			if (ret.get() != ILfs.FALSE) {
				getFree().free(inputBlockImageMap);
				getFree().freeDirPowers(powers, dftWaves.getNWaves());
				getFree().free(wis);
				getFree().free(powmaxs);
				getFree().free(powmaxDirs);
				getFree().free(pownorms);
				inputBlockImageMap = null;
				return inputBlockImageMap;
			}

			/* Conduct primary direction test */
			blockDir = primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
			if (blockDir != ILfs.INVALID_DIR) {
				inputBlockImageMap.set(blockOffsetIndex, blockDir);
			} else {
				/* Conduct secondary (fork) direction test */
				blockDir = secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
				if (blockDir != ILfs.INVALID_DIR) {
					inputBlockImageMap.set(blockOffsetIndex, blockDir);
				}
			}
			/* Otherwise current block direction in IMAP remains INVALID */
		} // bi

		/* Deallocate working memory */
		getFree().freeDirPowers(powers, dftWaves.getNWaves());
		getFree().free(wis);
		getFree().free(powmaxs);
		getFree().free(powmaxDirs);
		getFree().free(pownorms);

		ret.set(ILfs.FALSE);
		return inputBlockImageMap;
	}

	/**
	 * Applies the primary set of criteria for selecting a block's integer direction from its DFT
	 * results.
	 *
	 * Port of NIST {@code primary_dir_test()}. The power statistics are examined in decreasing order
	 * of strength; the first one that satisfies all three criteria determines the direction:
	 * <ol>
	 * <li>its maximum power exceeds {@link LfsParams#getPowmaxMin()} (e.g. 100000);</li>
	 * <li>its normalized power exceeds {@link LfsParams#getPownormMin()} (e.g. 3.8);</li>
	 * <li>the power of the lowest DFT frequency at that direction does not exceed
	 * {@link LfsParams#getPowmaxMax()} (e.g. 50000000).</li>
	 * </ol>
	 *
	 * @param powers     DFT power computed for each of the N wave frequencies at each rotation
	 *                   direction in the current image block (index 0 is the lowest frequency)
	 * @param wis        sorted order (strongest first) of the highest N-1 frequency power statistics
	 * @param powmaxs    maximum power for each of the highest N-1 frequencies
	 * @param powmaxDirs directions associated with each of the N-1 maximum powers
	 * @param pownorms   normalized power for each of the highest N-1 frequencies
	 * @param nStats     number of statistics, N-1 (where N is the number of DFT waves)
	 * @param lfsParams  LFS parameters and thresholds
	 * @return the selected integer direction (zero or positive), or {@link ILfs#INVALID_DIR} if no
	 *         direction could be determined
	 */
	public int primaryDirectionTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
			final AtomicReferenceArray<Double> powmaxs, final AtomicIntegerArray powmaxDirs,
			final AtomicReferenceArray<Double> pownorms, final int nStats, final LfsParams lfsParams) {
		if (isShowLogs())
			logger.debug("      Primary");

		/* Look at max power statistics in decreasing order ... */
		for (int statIndex = 0; statIndex < nStats; statIndex++) {
			/* 1. Test magnitude of current max power (Ex. Thresh==100000) */
			if ((powmaxs.get(wis.get(statIndex)) > lfsParams.getPowmaxMin()) &&
			/* 2. Test magnitude of normalized max power (Ex. Thresh==3.8) */
					(pownorms.get(wis.get(statIndex)) > lfsParams.getPownormMin()) &&
					/* 3. Test magnitude of power of lowest DFT frequency at current */
					/* max power direction and make sure it is not too big. */
					/* (Ex. Thresh==50000000) */
					(powers.get(0)[powmaxDirs.get(wis.get(statIndex))] <= lfsParams.getPowmaxMax())) {
				/* Add 1 to wis[w] to create index to original dft_coefs[] */
				if (isShowLogs()) {
					logger.debug("         Selected Wave = {}", (wis.get(statIndex) + 1));
					logger.debug("         1. Power Magnitude ({} > {})", powmaxs.get(wis.get(statIndex)),
							lfsParams.getPowmaxMin());
					logger.debug("         2. Norm Power Magnitude ({} > {})", pownorms.get(wis.get(statIndex)),
							lfsParams.getPownormMin());
					logger.debug("         3. Low Freq Wave Magnitude ({} <= {})",
							powers.get(0)[powmaxDirs.get(wis.get(statIndex))], lfsParams.getPowmaxMax());
					logger.debug("         PASSED");
					logger.debug("         Selected Direction = {}", powmaxDirs.get(wis.get(statIndex)));
				}
				/* If ALL 3 criteria met, return current max power direction. */
				return (powmaxDirs.get(wis.get(statIndex)));
			}
		}

		/* Otherwise test failed. */
		if (isShowLogs()) {
			logger.debug("         1. Power Magnitude ( > {})", lfsParams.getPowmaxMin());
			logger.debug("         2. Norm Power Magnitude ( > {})", lfsParams.getPownormMin());
			logger.debug("         3. Low Freq Wave Magnitude ( <= {})", lfsParams.getPowmaxMax());
			logger.debug("         FAILED");
		}
		return ILfs.INVALID_DIR;
	}

	/**
	 * Applies a secondary set of criteria for selecting a block's integer direction, designed to
	 * detect blocks containing a ridge "fork".
	 *
	 * Port of NIST {@code secondary_fork_test()}. Only the strongest power statistic is considered.
	 * It must have maximum power above {@link LfsParams#getPowmaxMin()}, normalized power at least
	 * {@code getForkPctPownorm() * getPownormMin()} (a relaxed threshold, e.g. 2.85), and
	 * lowest-frequency power at that direction not above {@link LfsParams#getPowmaxMax()}. Then the
	 * powers at the directions {@link LfsParams#getForkInterval()} steps to the left and right
	 * (modulo the number of directions) are compared against
	 * {@code getForkPctPowmax() * powmax} (e.g. 0.7 of the maximum power): exactly one of the two
	 * fork angles must exceed that threshold.
	 *
	 * @param powers     DFT power computed for each of the N wave frequencies at each rotation
	 *                   direction in the current image block (index 0 is the lowest frequency)
	 * @param wis        sorted order (strongest first) of the highest N-1 frequency power statistics
	 * @param powmaxs    maximum power for each of the highest N-1 frequencies
	 * @param powmaxDirs directions associated with each of the N-1 maximum powers
	 * @param pownorms   normalized power for each of the highest N-1 frequencies
	 * @param nStats     number of statistics, N-1 (where N is the number of DFT waves)
	 * @param lfsParams  LFS parameters and thresholds
	 * @return the direction of the strongest power statistic if the fork criteria hold, otherwise
	 *         {@link ILfs#INVALID_DIR}
	 */
	public int secondaryForkTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
			final AtomicReferenceArray<Double> powmaxs, final AtomicIntegerArray powmaxDirs,
			final AtomicReferenceArray<Double> pownorms, final int nStats, final LfsParams lfsParams) {
		int leftDir;
		int rightDir;
		double forkPownormMin;
		double forkPowThresh;

		int firstpart = 0; /* Flag to determine if passed 1st part ... */
		if (isShowLogs())
			logger.debug("      Secondary");

		/* Relax the normalized power threshold under fork conditions. */
		forkPownormMin = lfsParams.getForkPctPownorm() * lfsParams.getPownormMin();

		/* 1. Test magnitude of largest max power (Ex. Thresh==100000) */
		if ((powmaxs.get(wis.get(0)) > lfsParams.getPowmaxMin()) &&
		/* 2. Test magnitude of corresponding normalized power */
		/* (Ex. Thresh==2.85) */
				(pownorms.get(wis.get(0)) >= forkPownormMin) &&
				/* 3. Test magnitude of power of lowest DFT frequency at largest */
				/* max power direction and make sure it is not too big. */
				/* (Ex. Thresh==50000000) */
				(powers.get(0)[powmaxDirs.get(wis.get(0))] <= lfsParams.getPowmaxMax())) {
			/* First part passed ... */
			firstpart = 1;
			if (isShowLogs()) {
				logger.debug("         Selected Wave = {}", (wis.get(0) + 1));
				logger.debug("         1. Power Magnitude ({} > {})", powmaxs.get(wis.get(0)), lfsParams.getPowmaxMin());
				logger.debug("         2. Norm Power Magnitude ({} >= {})", pownorms.get(wis.get(0)), forkPownormMin);
				logger.debug("         3. Low Freq Wave Magnitude ({} <= {})", powers.get(0)[powmaxDirs.get(wis.get(0))],
						lfsParams.getPowmaxMax());
			}

			/* Add FORK_INTERVALs to current direction modulo NDIRS */
			rightDir = (powmaxDirs.get(wis.get(0)) + lfsParams.getForkInterval()) % lfsParams.getNumDirections();

			/* Subtract FORK_INTERVALs from direction modulo NDIRS */
			/* For example, FORK_INTERVAL==2 & NDIRS==16, then */
			/* ldir = (dir - (16-2)) % 16 */
			/* which keeps result in proper modulo range. */
			leftDir = (powmaxDirs.get(wis.get(0)) + lfsParams.getNumDirections() - lfsParams.getForkInterval())
					% lfsParams.getNumDirections();

			// logger.info((" Left = {}, Current = {}, Right = {}" + ldir +
			// "----" + powmax_dirs.get(wis.get(0)) + "----" + rdir)));

			/* Set forked angle threshold to be a % of the max directional */
			/* power. (Ex. thresh==0.7*powmax) */
			forkPowThresh = powmaxs.get(wis.get(0)) * lfsParams.getForkPctPowmax();

			/* Look up and test the computed power for the left and right */
			/* fork directions.s */
			/* The power stats (and thus wis) are on the range [0..nwaves-1) */
			/* as the statistics for the first DFT wave are not included. */
			/* The original power vectors exist for ALL DFT waves, therefore */
			/* wis indices must be added by 1 before addressing the original */
			/* powers vector. */
			/* LFS permits one and only one of the fork angles to exceed */
			/* the relative power threshold. */
			if (((powers.get(wis.get(0) + 1)[leftDir] <= forkPowThresh)
					|| (powers.get(wis.get(0) + 1)[rightDir] <= forkPowThresh))
					&& ((powers.get(wis.get(0) + 1)[leftDir] > forkPowThresh)
							|| (powers.get(wis.get(0) + 1)[rightDir] > forkPowThresh))) {
				if (isShowLogs()) {
					logger.debug("         4. Left Power Magnitude ({} > {})", powers.get(wis.get(0) + 1)[leftDir],
							forkPowThresh);
					logger.debug("         5. Right Power Magnitude ({} > {})", powers.get(wis.get(0) + 1)[rightDir],
							forkPowThresh);
					logger.debug("         PASSED");
					logger.debug("         Selected Direction = {}", powmaxDirs.get(wis.get(0)));
				}
				/* If ALL the above criteria hold, then return the direction */
				/* of the largest max power. */
				return powmaxDirs.get(wis.get(0));
			}
		}

		/* Otherwise test failed. */
		return ILfs.INVALID_DIR;
	}

	/**
	 * Removes individual directions that are too weak or inconsistent with their neighbors.
	 *
	 * Port of NIST {@code remove_incon_dirs()}. Directions are tested with
	 * {@link #removeIMAPDirection}, starting at the center of the map and working outward in
	 * concentric squares (top, right, bottom, then left edge of each square). Removed directions are
	 * set to {@link ILfs#INVALID_DIR}. Complete passes are repeated, restarting at the center, until
	 * a pass removes nothing.
	 *
	 * The map dimensions are taken from this instance's {@link #getMappedImageWidth()} and
	 * {@link #getMappedImageHeight()}.
	 *
	 * @param oInputBlockImageMap Direction Map / IMAP of integer directions; pruned in place
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 */
	public void removeInconsistentDirs(AtomicIntegerArray oInputBlockImageMap, final DirToRad dirToRad,
			final LfsParams lfsParams) {
		int mappedImageXIndex;
		int mappedImageYIndex;
		int nInputBlockImageMapIndex;
		int nRemoved = -1;
		int leftBoxIndex;
		int rightBoxIndex;
		int topBoxIndex;
		int bottomBoxIndex;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();

		int numPass = 0;
		if (isShowLogs())
			logger.debug("REMOVE MAP");
		/* Compute center coords of IMAP */
		mappedImageXIndex = mappedImageWidth >> 1;
		mappedImageYIndex = mappedImageHeight >> 1;

		/* Do pass, while directions have been removed in a pass ... */
		do {
			/* Count number of complete passes through IMAP */
			++numPass;
			if (isShowLogs())
				logger.debug("REMOVE MAP PASS = {}, {}, {}", numPass, oInputBlockImageMap.length(), nRemoved);
			/* Reinitialize number of removed directions to 0 */
			nRemoved = 0;

			/* Start at center */
			nInputBlockImageMapIndex = 0 + (mappedImageYIndex * mappedImageWidth) + mappedImageXIndex;

			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(nInputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex, mappedImageWidth,
							mappedImageHeight, dirToRad, lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(nInputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}

			/* Initialize side indices of concentric boxes */
			leftBoxIndex = mappedImageXIndex - 1;
			topBoxIndex = mappedImageYIndex - 1;
			rightBoxIndex = mappedImageXIndex + 1;
			bottomBoxIndex = mappedImageYIndex + 1;

			/* Grow concentric boxes, until ALL edges of imap are exceeded */
			while ((leftBoxIndex >= 0) || (rightBoxIndex < mappedImageWidth) || (topBoxIndex >= 0)
					|| (bottomBoxIndex < mappedImageHeight)) {
				/* test top edge of box */
				if (topBoxIndex >= 0) {
					nRemoved += testTopEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test right edge of box */
				if (rightBoxIndex < mappedImageWidth) {
					nRemoved += testRightEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test bottom edge of box */
				if (bottomBoxIndex < mappedImageHeight) {
					nRemoved += testBottomEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test left edge of box */
				if (leftBoxIndex >= 0) {
					nRemoved += testLeftEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* Resize current box */
				leftBoxIndex--;
				topBoxIndex--;
				rightBoxIndex++;
				bottomBoxIndex++;
			}

		} while (nRemoved != ILfs.FALSE);
	}

	/**
	 * Walks one edge of a concentric square in the map, testing directions along the way to see
	 * if they should be removed for being too weak or inconsistent with their neighbors.
	 *
	 * Port of NIST {@code test_top_edge()}. Walks the
	 * top edge, left to right, from the top-left corner (clamped to column 0)
	 * up to one block short of the top-right corner (clamped to the last column),
	 * applying {@link #removeIMAPDirection} to every valid direction.
	 *
	 * @param leftBoxIndex        left edge (block x) of the current concentric square
	 * @param topBoxIndex         top edge (block y) of the current concentric square
	 * @param rightBoxIndex       right edge (block x) of the current concentric square
	 * @param bottomBoxIndex      bottom edge (block y) of the current concentric square
	 * @param oInputBlockImageMap map of integer directions; removed directions are set to
	 *                            {@link ILfs#INVALID_DIR} in place
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 * @return the number of directions removed along this edge (zero or positive)
	 */
	public int testTopEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sx, ex;
		int inputBlockImageMapIndex, inputBlockImageMapCurrentIndex, inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to top-leftmost point of box, or set it to */
		/* the leftmost point in the IMAP row (0), whichever is larger. */
		sx = Math.max(leftBoxIndex, 0);
		inputBlockImageMapCurrentIndex = 0 + (topBoxIndex * mappedImageWidth) + sx;

		/* Set end pointer to either 1 point short of the top-rightmost */
		/* point of box, or set it to the rightmost point in the IMAP */
		/* row (lastx=mappedImageWidth-1), whichever is smaller. */
		ex = Math.min(rightBoxIndex - 1, mappedImageWidth - 1);
		inputBlockImageMapEdgeIndex = 0 + (topBoxIndex * mappedImageWidth) + ex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = sx, by = topBoxIndex; inputBlockImageMapIndex <= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex++, bx++) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/**
	 * Walks one edge of a concentric square in the map, testing directions along the way to see
	 * if they should be removed for being too weak or inconsistent with their neighbors.
	 *
	 * Port of NIST {@code test_right_edge()}. Walks the
	 * right edge, top to bottom, from the top-right corner (clamped to row
	 * 0) up to one block short of the bottom-right corner (clamped to the last row),
	 * applying {@link #removeIMAPDirection} to every valid direction.
	 *
	 * @param leftBoxIndex        left edge (block x) of the current concentric square
	 * @param topBoxIndex         top edge (block y) of the current concentric square
	 * @param rightBoxIndex       right edge (block x) of the current concentric square
	 * @param bottomBoxIndex      bottom edge (block y) of the current concentric square
	 * @param oInputBlockImageMap map of integer directions; removed directions are set to
	 *                            {@link ILfs#INVALID_DIR} in place
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 * @return the number of directions removed along this edge (zero or positive)
	 */
	public int testRightEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sy, ey;
		int inputBlockImageMapIndex, inputBlockImageMapCurrentIndex, inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to top-rightmost point of box, or set it to */
		/* the topmost point in IMAP column (0), whichever is larger. */
		sy = Math.max(topBoxIndex, 0);
		inputBlockImageMapCurrentIndex = 0 + (sy * mappedImageWidth) + rightBoxIndex;

		/* Set end pointer to either 1 point short of the bottom- */
		/* rightmost point of box, or set it to the bottommost point */
		/* in the IMAP column (lasty=mappedImageHeight-1), whichever is smaller. */
		ey = Math.min(bottomBoxIndex - 1, mappedImageHeight - 1);
		inputBlockImageMapEdgeIndex = 0 + (ey * mappedImageWidth) + rightBoxIndex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = rightBoxIndex, by = sy; inputBlockImageMapIndex <= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex += mappedImageWidth, by++) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/**
	 * Walks one edge of a concentric square in the map, testing directions along the way to see
	 * if they should be removed for being too weak or inconsistent with their neighbors.
	 *
	 * Port of NIST {@code test_bottom_edge()}. Walks the
	 * bottom edge, right to left, from the bottom-right corner (clamped to
	 * the last column) up to one block short of the bottom-left corner (clamped to column 0),
	 * applying {@link #removeIMAPDirection} to every valid direction.
	 *
	 * @param leftBoxIndex        left edge (block x) of the current concentric square
	 * @param topBoxIndex         top edge (block y) of the current concentric square
	 * @param rightBoxIndex       right edge (block x) of the current concentric square
	 * @param bottomBoxIndex      bottom edge (block y) of the current concentric square
	 * @param oInputBlockImageMap map of integer directions; removed directions are set to
	 *                            {@link ILfs#INVALID_DIR} in place
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 * @return the number of directions removed along this edge (zero or positive)
	 */
	public int testBottomEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sx, ex;
		int inputBlockImageMapIndex;
		int inputBlockImageMapCurrentIndex;
		int inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to bottom-rightmost point of box, or set it to the */
		/*
		 * rightmost point in the IMAP ROW (lastx=mappedImageWidth-1), whichever is
		 * smaller.
		 */
		sx = Math.min(rightBoxIndex, mappedImageWidth - 1);
		inputBlockImageMapCurrentIndex = 0 + (bottomBoxIndex * mappedImageWidth) + sx;

		/* Set end pointer to either 1 point short of the bottom- */
		/* lefttmost point of box, or set it to the leftmost point */
		/* in the IMAP row (x=0), whichever is larger. */
		ex = Math.max(leftBoxIndex - 1, 0);
		inputBlockImageMapEdgeIndex = 0 + (bottomBoxIndex * mappedImageWidth) + ex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = sx, by = bottomBoxIndex; inputBlockImageMapIndex >= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex--, bx--) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/**
	 * Walks one edge of a concentric square in the map, testing directions along the way to see
	 * if they should be removed for being too weak or inconsistent with their neighbors.
	 *
	 * Port of NIST {@code test_left_edge()}. Walks the
	 * left edge, bottom to top, from the bottom-left corner (clamped to the
	 * last row) up to one block short of the top-left corner (clamped to row 0),
	 * applying {@link #removeIMAPDirection} to every valid direction.
	 *
	 * @param leftBoxIndex        left edge (block x) of the current concentric square
	 * @param topBoxIndex         top edge (block y) of the current concentric square
	 * @param rightBoxIndex       right edge (block x) of the current concentric square
	 * @param bottomBoxIndex      bottom edge (block y) of the current concentric square
	 * @param oInputBlockImageMap map of integer directions; removed directions are set to
	 *                            {@link ILfs#INVALID_DIR} in place
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 * @return the number of directions removed along this edge (zero or positive)
	 */
	public int testLeftEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sy, ey;
		int inputBlockImageMapIndex;
		int inputBlockImageMapCurrentIndex;
		int inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to bottom-leftmost point of box, or set it to */
		/* the bottommost point in IMAP column (lasty=mappedImageHeight-1), whichever */
		/* is smaller. */
		sy = Math.min(bottomBoxIndex, mappedImageHeight - 1);
		inputBlockImageMapCurrentIndex = 0 + (sy * mappedImageWidth) + leftBoxIndex;

		/* Set end pointer to either 1 point short of the top-leftmost */
		/* point of box, or set it to the topmost point in the IMAP */
		/* column (y=0), whichever is larger. */
		ey = Math.max(topBoxIndex - 1, 0);
		inputBlockImageMapEdgeIndex = 0 + (ey * mappedImageWidth) + leftBoxIndex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = leftBoxIndex, by = sy; inputBlockImageMapIndex >= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex -= mappedImageWidth, by--) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/**
	 * Determines whether a block's direction should be removed, based on its 8 adjacent neighbors.
	 *
	 * Port of NIST {@code remove_dir()}. The average neighbor direction, its strength and the number
	 * of valid neighbors are computed ({@link #average8NbrDir}). The direction is removed if:
	 * <ol>
	 * <li>fewer than {@link LfsParams#getRmvValidNbrMin()} (e.g. 3) neighbors are valid; or</li>
	 * <li>the average direction is strong enough to put credence in (strength at least
	 * {@link LfsParams#getDirStrengthMin()}, e.g. 0.2) and the minimum circular distance between the
	 * block's direction and the average exceeds {@link LfsParams#getDirDistanceMax()} (e.g. 3).</li>
	 * </ol>
	 *
	 * @param oInputBlockImageMap map of integer directions (not modified)
	 * @param mappedImageXIndex   block x-coordinate of the direction being tested
	 * @param mappedImageYIndex   block y-coordinate of the direction being tested
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 * @return {@code 1} ({@link ILfs#TRUE}) if the direction should be removed because of too few
	 *         valid neighbors; {@code 2} if it should be removed because it differs too much from the
	 *         average neighbor direction; {@link ILfs#FALSE} ({@code 0}) if it should NOT be removed
	 */
	@SuppressWarnings({ "java:S2629" })
	public int removeIMAPDirection(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight,
			final DirToRad dirToRad, final LfsParams lfsParams) {
		AtomicInteger oAverageDirection = new AtomicInteger(), oValid = new AtomicInteger();
		int nDistance = 0;

		AtomicReference<Double> dirStrength = new AtomicReference<Double>(0.0);
		/* Compute average direction from neighbors, returning the */
		/* number of valid neighbors used in the computation, and */
		/* the "strength" of the average direction. */
		average8NbrDir(oAverageDirection, dirStrength, oValid, oInputBlockImageMap, mappedImageXIndex,
				mappedImageYIndex, mappedImageWidth, mappedImageHeight, dirToRad);
		/* Conduct valid neighbor test (Ex. thresh==3) */
		if (oValid.get() < lfsParams.getRmvValidNbrMin()) {
			if (isShowLogs()) {
				logger.debug("      BLOCK {} ({}, {})", mappedImageXIndex + (mappedImageYIndex * mappedImageWidth),
						mappedImageXIndex, mappedImageYIndex);
				logger.debug("         Average NBR :   {} {} {}", oAverageDirection.get(), dirStrength.get(),
						oValid.get());
				logger.debug("         1. Valid NBR ({} < {})", oValid.get(), lfsParams.getRmvValidNbrMin());
			}
			return (ILfs.TRUE);
		}

		/* If strength of average neighbor direction is large enough to */
		/* put credence in ... (Ex. threshold==0.2) */
		if (dirStrength.get() >= lfsParams.getDirStrengthMin()) {
			/* Conduct direction distance test (Ex. thresh==3) */
			/* Compute minimum absolute distance between current and */
			/* average directions accounting for wrapping from 0 to NDIRS. */
			nDistance = Math.abs(oAverageDirection.get()
					- (oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex)));
			nDistance = Math.min(nDistance, dirToRad.getNDirs() - nDistance);
			if (nDistance > lfsParams.getDirDistanceMax()) {
				if (isShowLogs()) {
					logger.debug("      BLOCK {} ({}, {})", mappedImageXIndex + (mappedImageYIndex * mappedImageWidth),
							mappedImageXIndex, mappedImageYIndex);
					logger.debug("         Average NBR :   {} {} {}", oAverageDirection.get(), dirStrength.get(),
							oValid.get());
					logger.debug("         1. Valid NBR ({} < {})", oValid.get(), lfsParams.getRmvValidNbrMin());
					logger.debug("         2. Direction Strength ({} >= {})", dirStrength.get(),
							lfsParams.getDirStrengthMin());
					logger.debug("         Current Dir =  {}, Average Dir = {}",
							oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex),
							oAverageDirection.get());
					logger.debug("         3. Direction Distance ({} > {})", nDistance, lfsParams.getDirDistanceMax());
				}
				return (2);
			}
		}

		/* Otherwise, the strength of the average direciton is not strong enough */
		/* to put credence in, so leave the current block's directon alone. */

		return (ILfs.FALSE);
	}

	/**
	 * Computes the average direction of a block's 8 adjacent neighbors, together with its strength
	 * and the number of valid neighbors used.
	 *
	 * Port of NIST {@code average_8nbr_dir()}. The cosine and sine components of each valid
	 * neighbor direction (via {@code dirToRad}) are averaged. The strength is the squared magnitude
	 * of the averaged vector (on the range [0..1]), truncated to {@link ILfs#TRUNC_SCALE}
	 * precision. If no neighbor is valid, or the strength is below {@link ILfs#DIR_STRENGTH_MIN},
	 * the average is {@link ILfs#INVALID_DIR} with strength {@code 0}. Otherwise the angle
	 * {@code atan2(sin, cos)} is mapped onto {@code [0..2PI)}, converted to integer direction units
	 * ({@code nDirs} per full period), truncated, rounded and reduced modulo {@code nDirs}.
	 *
	 * @param oAverageDir         output average direction, or {@link ILfs#INVALID_DIR}
	 * @param oDirStrength        output strength of the average direction (range [0..1])
	 * @param oValid              output number of valid neighbor directions used
	 * @param oInputBlockImageMap map of integer directions
	 * @param mapXIndex           block x-coordinate of the current block
	 * @param mapYIndex           block y-coordinate of the current block
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param dirToRad            lookup table for converting integer directions to radians
	 */
	public void average8NbrDir(AtomicInteger oAverageDir, AtomicReference<Double> oDirStrength, AtomicInteger oValid,
			AtomicIntegerArray oInputBlockImageMap, final int mapXIndex, final int mapYIndex,
			final int mappedImageWidth, final int mappedImageHeight, final DirToRad dirToRad) {
		int inputBlockImageMapIndex;
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		double cospart;
		double sinpart;
		double pi2;
		double pifactor;
		double theta;
		double avr;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mapXIndex + 1; // East
		westIndex = mapXIndex - 1; // West
		northIndex = mapYIndex - 1; // North
		southIndex = mapYIndex + 1; // South

		/* Intialize accumulators */
		int nValid = 0;
		cospart = 0.0;
		sinpart = 0.0;

		/* 1. Test NW */
		/* If NW point within IMAP boudaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 2. Test N */
		/* If N point within IMAP boudaries ... */
		if (northIndex >= 0) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + mapXIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 3. Test NE */
		/* If NE point within IMAP boudaries ... */
		if ((eastIndex < mappedImageWidth) && (northIndex >= 0)) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 4. Test E */
		/* If E point within IMAP boudaries ... */
		if (eastIndex < mappedImageWidth) {
			inputBlockImageMapIndex = 0 + (mapYIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 5. Test SE */
		/* If SE point within IMAP boudaries ... */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 6. Test S */
		/* If S point within IMAP boudaries ... */
		if (southIndex < mappedImageHeight) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + mapXIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 7. Test SW */
		/* If SW point within IMAP boudaries ... */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 8. Test W */
		/* If W point within IMAP boudaries ... */
		if (westIndex >= 0) {
			inputBlockImageMapIndex = 0 + (mapYIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* If there were no neighbors found with valid direction ... */
		if (nValid == ILfs.FALSE) {
			/* Return INVALID direction. */
			oDirStrength.set(0.0);
			oValid.set(nValid);
			oAverageDir.set(ILfs.INVALID_DIR);
			return;
		}

		oValid.set(nValid);
		/* Compute averages of accumulated cosine and sine direction components */
		cospart /= (nValid);
		sinpart /= (nValid);

		/* Compute directional strength as hypotenuse (without sqrt) of average */
		/* cosine and sine direction components. Believe this value will be on */
		/* the range of [0 .. 1]. */
		oDirStrength.set((cospart * cospart) + (sinpart * sinpart));
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when comparing doubles. */
		oDirStrength.set(getDefs().truncDoublePrecision(oDirStrength.get(), ILfs.TRUNC_SCALE));

		/* If the direction strength is not sufficiently high ... */
		if (oDirStrength.get() < ILfs.DIR_STRENGTH_MIN) {
			/* Return INVALID direction. */
			oDirStrength.set(0.0);
			oAverageDir.set(ILfs.INVALID_DIR);
			return;
		}

		/* Compute angle (in radians) from Arctan of avarage */
		/* cosine and sine direction components. I think this order */
		/* is necessary because 0 direction is vertical and positive */
		/* direction is clockwise. */
		theta = Math.atan2(sinpart, cospart);

		/* Atan2 returns theta on range [-PI..PI]. Adjust theta so that */
		/* it is on the range [0..2PI]. */
		pi2 = 2 * ILfs.M_PI;
		theta += pi2;
		theta = getDefs().fMod(theta, pi2);

		/* Pi_factor sets the period of the trig functions to NDIRS units in x. */
		/* For example, if NDIRS==16, then pi_factor = 2(PI/16) = .3926... */
		/* Dividing theta (in radians) by this factor ((1/pi_factor)==2.546...) */
		/* will produce directions on the range [0..NDIRS]. */
		pifactor = pi2 / (double) dirToRad.getNDirs(); // 2(M_PI/ndirs)

		/* Round off the direction and return it as an average direction */
		/* for the neighborhood. */
		avr = theta / pifactor;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		avr = getDefs().truncDoublePrecision(avr, ILfs.TRUNC_SCALE);
		oAverageDir.set(getDefs().sRound(avr));

		/* Really do need to map values > NDIRS back onto [0..NDIRS) range. */
		oAverageDir.set(oAverageDir.get() % dirToRad.getNDirs());
	}

	/**
	 * Counts the immediate (8-connected) neighbors of a block that have a valid direction.
	 *
	 * Port of NIST {@code num_valid_8nbrs()}. A neighbor is valid when it lies within the map and
	 * its value is {@code >= 0}.
	 *
	 * @param oInputBlockImageMap 2D map of directional ridge flows
	 * @param mapXIndex           block x-coordinate of the current block
	 * @param mapYIndex           block y-coordinate of the current block
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @return the number of valid neighbors (0 to 8)
	 */
	public int numValid8Nbrs(AtomicIntegerArray oInputBlockImageMap, final int mapXIndex, final int mapYIndex,
			final int mappedImageWidth, final int mappedImageHeight) {
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int nValid;

		/* Initialize VALID IMAP counter to zero. */
		nValid = 0;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mapXIndex + 1; // East index
		westIndex = mapXIndex - 1; // West index
		northIndex = mapYIndex - 1; // North index
		southIndex = mapYIndex + 1; // South index

		/* 1. Test NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries and it is VALID ... */
		if ((westIndex >= 0) && (northIndex >= 0)
				&& (oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex) >= 0)) {
			/* Bump VALID counter. */
			nValid++;
		}

		/* 2. Test N IMAP value. */
		if ((northIndex >= 0) && (oInputBlockImageMap.get((northIndex * mappedImageWidth) + mapXIndex) >= 0)) {
			nValid++;
		}

		/* 3. Test NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)
				&& (oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 4. Test E IMAP value. */
		if ((eastIndex < mappedImageWidth)
				&& (oInputBlockImageMap.get((mapYIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 5. Test SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 6. Test S IMAP value. */
		if ((southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + mapXIndex) >= 0)) {
			nValid++;
		}

		/* 7. Test SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex) >= 0)) {
			nValid++;
		}

		/* 8. Test W IMAP value. */
		if ((westIndex >= 0) && (oInputBlockImageMap.get((mapYIndex * mappedImageWidth) + westIndex) >= 0)) {
			nValid++;
		}

		/* Return number of neighbors with VALID IMAP values. */
		return (nValid);
	}

	/**
	 * Smooths an IMAP by analyzing the directions of each block's 8 neighbors.
	 *
	 * Port of NIST {@code smooth_imap()}. For every block, the average neighbor direction, its
	 * strength and the number of valid neighbors are computed ({@link #average8NbrDir}). If the
	 * strength is at least {@link LfsParams#getDirStrengthMin()} (e.g. 0.2), a valid direction is
	 * replaced by the average when there are at least {@link LfsParams#getRmvValidNbrMin()} (e.g. 3)
	 * valid neighbors, and an INVALID direction is assigned the average when there are at least
	 * {@link LfsParams#getSmoothValidNbrMin()} (e.g. 7) valid neighbors. Updates are applied in
	 * place in raster order.
	 *
	 * The map dimensions are taken from this instance's {@link #getMappedImageWidth()} and
	 * {@link #getMappedImageHeight()}.
	 *
	 * @param oInputBlockImageMap IMAP of integer directions; smoothed in place
	 * @param dirToRad            lookup table for converting integer directions to radians
	 * @param lfsParams           LFS parameters and thresholds
	 */
	public void smoothInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, final DirToRad dirToRad,
			final LfsParams lfsParams) {
		int inputBlockImageMapIndex = 0;
		int inputBlockImageMapIndexValue;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();
		AtomicInteger averageDir = new AtomicInteger(0);
		AtomicInteger oValid = new AtomicInteger(0);
		AtomicReference<Double> oDirStrength = new AtomicReference<>();

		if (isShowLogs())
			logger.debug("SMOOTH MAP");
		for (int mapYIndex = 0; mapYIndex < mappedImageHeight; mapYIndex++) {
			for (int mapXIndex = 0; mapXIndex < mappedImageWidth; mapXIndex++) {
				inputBlockImageMapIndexValue = oInputBlockImageMap.get(inputBlockImageMapIndex);
				/* Compute average direction from neighbors, returning the */
				/* number of valid neighbors used in the computation, and */
				/* the "strength" of the average direction. */
				average8NbrDir(averageDir, oDirStrength, oValid, oInputBlockImageMap, mapXIndex, mapYIndex,
						mappedImageWidth, mappedImageHeight, dirToRad);

				/* If average direction strength is strong enough */
				/* (Ex. thresh==0.2)... */
				if (oDirStrength.get() >= lfsParams.getDirStrengthMin()) {
					/* If IMAP direction is valid ... */
					if (inputBlockImageMapIndexValue != ILfs.INVALID_DIR) {
						/* Conduct valid neighbor test (Ex. thresh==3)... */
						if (oValid.get() >= lfsParams.getRmvValidNbrMin()) {
							/* Reassign valid IMAP direction with average direction. */
							inputBlockImageMapIndexValue = averageDir.get();
						}
					}
					/* Otherwise IMAP direction is invalid ... */
					else {
						/* Even if IMAP value is invalid, if number of valid */
						/* neighbors is big enough (Ex. thresh==7)... */
						if (oValid.get() >= lfsParams.getSmoothValidNbrMin()) {
							/* Assign invalid IMAP direction with average direction. */
							inputBlockImageMapIndexValue = averageDir.get();
						}
					}
				}
				/* Bump to next IMAP direction. */
				oInputBlockImageMap.set(inputBlockImageMapIndex++, inputBlockImageMapIndexValue);
			}
		}
	}

	/**
	 * Computes an NMAP from its associated IMAP.
	 *
	 * Port of NIST {@code gen_nmap()}. Each NMAP value either holds the direction of dominant ridge
	 * flow in a block, or a code describing why such a direction was not produced. For example,
	 * blocks near areas of high curvature (such as cores and deltas) will not produce reliable IMAP
	 * directions. Per block:
	 * <ul>
	 * <li>no valid neighbors: {@link ILfs#NO_VALID_NBRS} ({@code -3});</li>
	 * <li>INVALID IMAP value with fewer than {@link LfsParams#getVortValidNbrMin()} valid
	 * neighbors, or with {@link #vorticity} below {@link LfsParams#getHighcurvVorticityMin()}:
	 * {@link ILfs#INVALID_DIR} ({@code -1}); otherwise {@link ILfs#HIGH_CURVATURE}
	 * ({@code -2});</li>
	 * <li>valid IMAP value with {@link #curvature} at least
	 * {@link LfsParams#getHighcurvCurvatureMin()}: {@link ILfs#HIGH_CURVATURE}; otherwise the IMAP
	 * direction itself.</li>
	 * </ul>
	 *
	 * @param oNMap               output NMAP; must be pre-allocated with one entry per block and is
	 *                            filled in place
	 * @param oInputBlockImageMap associated IMAP of directions
	 * @param mappedImageWidth    width of the IMAP, in blocks
	 * @param mappedImageHeight   height of the IMAP, in blocks
	 * @param lfsParams           LFS parameters and thresholds
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion
	 */
	public int genNMap(AtomicIntegerArray oNMap, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, final LfsParams lfsParams) {
		int nmapIndex;
		int inputBlockImageMapIndex;
		int nValid;
		int curvatureMeasure;
		int vorticityMeasure;

		nmapIndex = 0;
		inputBlockImageMapIndex = 0;

		/* Foreach row in IMAP ... */
		for (int mappedImageYIndex = 0; mappedImageYIndex < mappedImageHeight; mappedImageYIndex++) {
			/* Foreach column in IMAP ... */
			for (int mappedImageXIndex = 0; mappedImageXIndex < mappedImageWidth; mappedImageXIndex++) {
				/* Count number of valid neighbors around current block ... */
				nValid = numValid8Nbrs(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex, mappedImageWidth,
						mappedImageHeight);
				/* If block has no valid neighbors ... */
				if (nValid == ILfs.FALSE) {
					/* Set NMAP value to NO VALID NEIGHBORS */
					oNMap.set(nmapIndex, ILfs.NO_VALID_NBRS);
				} else {
					/* If current IMAP value is INVALID ... */
					if (oInputBlockImageMap.get(inputBlockImageMapIndex) == ILfs.INVALID_DIR) {
						/* If not enough VALID neighbors ... */
						if (nValid < lfsParams.getVortValidNbrMin()) {
							/* Set NMAP value to INVALID */
							oNMap.set(nmapIndex, ILfs.INVALID_DIR);
						} else {
							/* Otherwise measure vorticity of neighbors. */
							vorticityMeasure = vorticity(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex,
									mappedImageWidth, mappedImageHeight, lfsParams.getNumDirections());
							/* If vorticity too low ... */
							if (vorticityMeasure < lfsParams.getHighcurvVorticityMin()) {
								oNMap.set(nmapIndex, ILfs.INVALID_DIR);
							} else {
								/* Otherwise high-curvature area (Ex. core or delta). */
								oNMap.set(nmapIndex, ILfs.HIGH_CURVATURE);
							}
						}
					}
					/* Otherwise VALID IMAP value ... */
					else {
						/* Measure curvature around the VALID IMAP block. */
						curvatureMeasure = curvature(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex,
								mappedImageWidth, mappedImageHeight, lfsParams.getNumDirections());
						/* If curvature is too high ... */
						if (curvatureMeasure >= lfsParams.getHighcurvCurvatureMin()) {
							oNMap.set(nmapIndex, ILfs.HIGH_CURVATURE);
						} else {
							/* Otherwise acceptable amount of curature, so assign */
							/* VALID IMAP value to NMAP. */
							oNMap.set(nmapIndex, oInputBlockImageMap.get(inputBlockImageMapIndex));
						}
					}
				} // end else (nvalid > 0)
				/* BUMP IMAP and NMAP pointers. */
				inputBlockImageMapIndex++;
				nmapIndex++;

			} // mappedImageXIndex
		} // mappedImageYIndex

		return ILfs.FALSE;
	}

	/**
	 * Measures the cumulative curvature (vorticity) among the 8 neighbors of a block.
	 *
	 * Port of NIST {@code vorticity()}. The 8 neighbors are visited in circular order (NW, N, NE, E,
	 * SE, S, SW, W and back to NW); for each adjacent pair the vorticity is accumulated with
	 * {@link #accumulateNbrVorticity}. Neighbors outside the map count as INVALID and are ignored.
	 *
	 * @param oInputBlockImageMap 2D map of ridge-flow directions
	 * @param mappedImageXIndex   block x-coordinate of the current block
	 * @param mappedImageYIndex   block y-coordinate of the current block
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param nDirs               number of possible directions in the map (covering 180 degrees)
	 * @return the accumulated vorticity measure among the neighbors (may be negative, as clockwise
	 *         turns larger than 90 degrees decrement it)
	 */
	public int vorticity(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight, final int nDirs) {
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int northwestValue;
		int northValue;
		int northeastValue;
		int eastValue;
		int southeastValue;
		int southValue;
		int southwestValue;
		int westValue;
		AtomicInteger oVorticityMeasure;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mappedImageXIndex + 1; // East index
		westIndex = mappedImageXIndex - 1; // West index
		northIndex = mappedImageYIndex - 1; // North index
		southIndex = mappedImageYIndex + 1; // South index

		/* 1. Get NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			/* Set neighbor value to IMAP value. */
			northwestValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex);
		} else {
			/* Otherwise, set the neighbor value to INVALID. */
			northwestValue = ILfs.INVALID_DIR;
		}

		/* 2. Get N IMAP value. */
		if (northIndex >= 0) {
			northValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			northValue = ILfs.INVALID_DIR;
		}

		/* 3. Get NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)) {
			northeastValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex);
		} else {
			northeastValue = ILfs.INVALID_DIR;
		}

		/* 4. Get E IMAP value. */
		if (eastIndex < mappedImageWidth) {
			eastValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + eastIndex);
		} else {
			eastValue = ILfs.INVALID_DIR;
		}

		/* 5. Get SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			southeastValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex);
		} else {
			southeastValue = ILfs.INVALID_DIR;
		}

		/* 6. Get S IMAP value. */
		if (southIndex < mappedImageHeight) {
			southValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			southValue = ILfs.INVALID_DIR;
		}

		/* 7. Get SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			southwestValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex);
		} else {
			southwestValue = ILfs.INVALID_DIR;
		}

		/* 8. Get W IMAP value. */
		if (westIndex >= 0) {
			westValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + westIndex);
		} else {
			westValue = ILfs.INVALID_DIR;
		}

		/* Now that we have all IMAP neighbors, accumulate vorticity between */
		/* the neighboring directions. */

		/* Initialize vorticity accumulator to zero. */
		oVorticityMeasure = new AtomicInteger(0);

		/* 1. NW & N */
		accumulateNbrVorticity(oVorticityMeasure, northwestValue, northValue, nDirs);

		/* 2. N & NE */
		accumulateNbrVorticity(oVorticityMeasure, northValue, northeastValue, nDirs);

		/* 3. NE & E */
		accumulateNbrVorticity(oVorticityMeasure, northeastValue, eastValue, nDirs);

		/* 4. E & SE */
		accumulateNbrVorticity(oVorticityMeasure, eastValue, southeastValue, nDirs);

		/* 5. SE & S */
		accumulateNbrVorticity(oVorticityMeasure, southeastValue, southValue, nDirs);

		/* 6. S & SW */
		accumulateNbrVorticity(oVorticityMeasure, southValue, southwestValue, nDirs);

		/* 7. SW & W */
		accumulateNbrVorticity(oVorticityMeasure, southwestValue, westValue, nDirs);

		/* 8. W & NW */
		accumulateNbrVorticity(oVorticityMeasure, westValue, northwestValue, nDirs);

		/* Return the accumulated vorticity measure. */
		return (oVorticityMeasure.get());
	}

	/**
	 * Accumulates the curvature contribution between two neighboring block directions.
	 *
	 * Port of NIST {@code accum_nbr_vorticity()}. If both directions are valid and different, the
	 * clockwise distance from {@code dir1} to {@code dir2} is computed (wrapping modulo
	 * {@code nDirs}). If it is larger than {@code nDirs / 2} (more than 90 degrees, since all
	 * directions cover 180 degrees) the measure is decremented, otherwise it is incremented. Equal
	 * or INVALID directions are ignored.
	 *
	 * @param oVorticityMeasure accumulated vorticity measure; updated in place
	 * @param dir1              first neighbor's integer direction
	 * @param dir2              second neighbor's integer direction
	 * @param nDirs             number of possible directions
	 */
	public void accumulateNbrVorticity(AtomicInteger oVorticityMeasure, final int dir1, final int dir2,
			final int nDirs) {
		int dist;

		/* Measure difference in direction between a pair of neighboring */
		/* directions. */
		/* If both neighbors are not equal and both are VALID ... */
		if ((dir1 != dir2) && (dir1 >= 0) && (dir2 >= 0)) {
			/* Measure the clockwise distance from the first to the second */
			/* directions. */
			dist = dir2 - dir1;
			/* If dist is negative, then clockwise distance must wrap around */
			/* the high end of the direction range. For example: */
			/* dir1 = 8 */
			/* dir2 = 3 */
			/* and ndirs = 16 */
			/* 3 - 8 = -5 */
			/* so 16 - 5 = 11 (the clockwise distance from 8 to 3) */
			if (dist < 0) {
				dist += nDirs;
			}
			/* If the change in clockwise direction is larger than 90 degrees as */
			/* in total the total number of directions covers 180 degrees. */
			if (dist > (nDirs >> 1)) {
				/* Decrement the vorticity measure. */
				oVorticityMeasure.set(oVorticityMeasure.get() - 1);
			} else {
				/* Otherwise, bump the vorticity measure. */
				oVorticityMeasure.set(oVorticityMeasure.get() + 1);
			}
		}
		/* Otherwise both directions are either equal or */
		/* one or both directions are INVALID, so ignore. */
	}

	/**
	 * Measures the largest change in direction between a block and its 8 immediate neighbors.
	 *
	 * Port of NIST {@code curvature()}. The closest circular distance
	 * ({@link LfsUtil#closestDirDistance}) between the block's direction and each neighbor's is
	 * computed; neighbors outside the map count as INVALID. The maximum distance is returned.
	 *
	 * @param oInputBlockImageMap 2D map of ridge-flow directions
	 * @param mappedImageXIndex   block x-coordinate of the current block
	 * @param mappedImageYIndex   block y-coordinate of the current block
	 * @param mappedImageWidth    width of the map, in blocks
	 * @param mappedImageHeight   height of the map, in blocks
	 * @param nDirs               number of possible directions in the map (covering 180 degrees)
	 * @return the maximum change in direction found (curvature, zero or positive), or a negative
	 *         value ({@code -1}) if no valid neighbor was found to measure a change against
	 */
	public int curvature(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight, final int nDirs) {
		int nInputBlockImageMapIndexValue;
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int northwestValue;
		int northValue;
		int northeastValue;
		int eastValue;
		int southeastValue;
		int southValue;
		int southwestValue;
		int westValue;
		int nCurvatureMeasure;
		int nDistance;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mappedImageXIndex + 1; // East index
		westIndex = mappedImageXIndex - 1; // West index
		northIndex = mappedImageYIndex - 1; // North index
		southIndex = mappedImageYIndex + 1; // South index

		/* 1. Get NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			/* Set neighbor value to IMAP value. */
			northwestValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex);
		} else {
			/* Otherwise, set the neighbor value to INVALID. */
			northwestValue = ILfs.INVALID_DIR;
		}

		/* 2. Get N IMAP value. */
		if (northIndex >= 0) {
			northValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			northValue = ILfs.INVALID_DIR;
		}

		/* 3. Get NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)) {
			northeastValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex);
		} else {
			northeastValue = ILfs.INVALID_DIR;
		}

		/* 4. Get E IMAP value. */
		if (eastIndex < mappedImageWidth) {
			eastValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + eastIndex);
		} else {
			eastValue = ILfs.INVALID_DIR;
		}

		/* 5. Get SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			southeastValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex);
		} else {
			southeastValue = ILfs.INVALID_DIR;
		}

		/* 6. Get S IMAP value. */
		if (southIndex < mappedImageHeight) {
			southValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			southValue = ILfs.INVALID_DIR;
		}

		/* 7. Get SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			southwestValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex);
		} else {
			southwestValue = ILfs.INVALID_DIR;
		}

		/* 8. Get W IMAP value. */
		if (westIndex >= 0) {
			westValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + westIndex);
		} else {
			westValue = ILfs.INVALID_DIR;
		}

		/* Now that we have all IMAP neighbors, determine largest change in */
		/* direction from current block to each of its 8 VALID neighbors. */

		/* Initialize pointer to current IMAP value. */
		nInputBlockImageMapIndexValue = oInputBlockImageMap
				.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex);

		/* Initialize curvature measure to negative as closest_dir_dist() */
		/* always returns -1=INVALID or a positive value. */
		nCurvatureMeasure = -1;

		/* 1. With NW */
		/* Compute closest distance between neighboring directions. */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northwestValue, nDirs);
		/* Keep track of maximum. */
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 2. With N */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 3. With NE */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northeastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 4. With E */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, eastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 5. With SE */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southeastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 6. With S */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 7. With SW */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southwestValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 8. With W */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, westValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* Return maximum difference between current block's IMAP direction */
		/* and the rest of its VALID neighbors. */
		return (nCurvatureMeasure);
	}

	/**
	 * Returns the Direction Map (per-block direction or {@link ILfs#INVALID_DIR}).
	 *
	 * @return the Direction Map; may be {@code null} if not yet set
	 */
	public AtomicIntegerArray getDirectionMap() {
		return directionMap;
	}

	/**
	 * Sets the Direction Map (per-block direction or {@link ILfs#INVALID_DIR}).
	 *
	 * @param directionMap the new Direction Map
	 */
	public void setDirectionMap(AtomicIntegerArray directionMap) {
		this.directionMap = directionMap;
	}

	/**
	 * Returns the Low Contrast Map (per-block TRUE/FALSE flags).
	 *
	 * @return the Low Contrast Map; may be {@code null} if not yet set
	 */
	public AtomicIntegerArray getLowContrastMap() {
		return lowContrastMap;
	}

	/**
	 * Sets the Low Contrast Map (per-block TRUE/FALSE flags).
	 *
	 * @param lowContrastMap the new Low Contrast Map
	 */
	public void setLowContrastMap(AtomicIntegerArray lowContrastMap) {
		this.lowContrastMap = lowContrastMap;
	}

	/**
	 * Returns the Low Ridge Flow Map (per-block TRUE/FALSE flags).
	 *
	 * @return the Low Ridge Flow Map; may be {@code null} if not yet set
	 */
	public AtomicIntegerArray getLowFlowMap() {
		return lowFlowMap;
	}

	/**
	 * Sets the Low Ridge Flow Map (per-block TRUE/FALSE flags).
	 *
	 * @param lowFlowMap the new Low Ridge Flow Map
	 */
	public void setLowFlowMap(AtomicIntegerArray lowFlowMap) {
		this.lowFlowMap = lowFlowMap;
	}

	/**
	 * Returns the High Curvature Map (per-block TRUE/FALSE flags).
	 *
	 * @return the High Curvature Map; may be {@code null} if not yet set
	 */
	public AtomicIntegerArray getHighCurveMap() {
		return highCurveMap;
	}

	/**
	 * Sets the High Curvature Map (per-block TRUE/FALSE flags).
	 *
	 * @param highCurveMap the new High Curvature Map
	 */
	public void setHighCurveMap(AtomicIntegerArray highCurveMap) {
		this.highCurveMap = highCurveMap;
	}

	/**
	 * Returns the width of the maps, in blocks.
	 *
	 * @return the width of the maps, in blocks; may be {@code null} if not yet set
	 */
	public AtomicInteger getMappedImageWidth() {
		return mappedImageWidth;
	}

	/**
	 * Sets the width of the maps, in blocks.
	 *
	 * @param mappedImageWidth the new width of the maps, in blocks
	 */
	public void setMappedImageWidth(AtomicInteger mappedImageWidth) {
		this.mappedImageWidth = mappedImageWidth;
	}

	/**
	 * Returns the height of the maps, in blocks.
	 *
	 * @return the height of the maps, in blocks; may be {@code null} if not yet set
	 */
	public AtomicInteger getMappedImageHeight() {
		return mappedImageHeight;
	}

	/**
	 * Sets the height of the maps, in blocks.
	 *
	 * @param mappedImageHeight the new height of the maps, in blocks
	 */
	public void setMappedImageHeight(AtomicInteger mappedImageHeight) {
		this.mappedImageHeight = mappedImageHeight;
	}
}
