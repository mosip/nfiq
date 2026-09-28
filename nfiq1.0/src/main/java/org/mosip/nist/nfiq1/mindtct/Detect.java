package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.IDetect;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LFS (Latent Fingerprint System) Version 2 minutiae detection driver: the core of MINDTCT.
 * <p>
 * Port of NIST LFS {@code detect.c} ({@code lfs_detect_minutiae_V2}). It chains together initialization
 * (lookup tables, rotated grids, padding, 6-bit rescaling), block map generation ({@code Maps}), directional
 * binarization ({@link Binarization}), minutia detection ({@link MinutiaHelper}), false-minutia removal
 * ({@link RemoveMinutia}) and neighbour ridge counting ({@link Ridges}). Called by
 * {@link GetMinutiae#getMinutiae}.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state of its
 * own.
 */
public class Detect extends MindTct implements IDetect {
	/** SLF4J logger for progress, timing and error messages. */
	private static final Logger logger = LoggerFactory.getLogger(Detect.class);

	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Detect instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Detect() {
		super();
	}

	/**
	 * Returns the shared {@code Detect} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Detect getInstance() {
		if (instance == null) {
			instance = new Detect();
		}
		return instance;
	}

	/**
	 * Returns the LFS initialization helper (padding, DIR2RAD, DFT waves, rotated grids).
	 *
	 * @return the {@link Init} singleton
	 */
	public Init getInit() {
		return Init.getInstance();
	}

	/**
	 * Returns the MINDTCT global lookup tables (e.g. DFT coefficients).
	 *
	 * @return the {@link Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the memory-release helper.
	 *
	 * @return the {@link Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the MINDTCT image utility helper (padding, bit-depth rescaling, gray/binary conversion).
	 *
	 * @return the {@link ImageUtil} singleton (MINDTCT package version)
	 */
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/**
	 * Returns the directional binarization helper.
	 *
	 * @return the {@link Binarization} singleton
	 */
	public Binarization getBinarization() {
		return Binarization.getInstance();
	}

	/**
	 * Returns the minutia allocation and detection helper.
	 *
	 * @return the {@link MinutiaHelper} singleton
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the false-minutia removal helper.
	 *
	 * @return the {@link RemoveMinutia} singleton
	 */
	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}

	/**
	 * Returns the neighbour ridge counting helper.
	 *
	 * @return the {@link Ridges} singleton
	 */
	public Ridges getRidges() {
		return Ridges.getInstance();
	}

	/**
	 * Detects minutiae in a grayscale fingerprint image of arbitrary size using LFS Version 2 (NIST
	 * {@code lfs_detect_minutiae_V2}).
	 * <p>
	 * Produces a set of image block maps, a binarized image that separates ridges from valleys, and a list of
	 * minutiae (position, reliability, type, direction, neighbours and ridge counts to neighbours). The maps
	 * are a ridge-flow direction map, a low-contrast map, a low ridge-flow map and a high-curvature map. Steps:
	 * <ol>
	 * <li>Initialization: compute the maximum padding, build the DIR2RAD table, DFT wave forms and DFT rotated
	 * grids, pad the image (or copy it if no padding is needed) and rescale it to 6 bits [0, 63];</li>
	 * <li>Maps: generate the block maps into {@code map};</li>
	 * <li>Binarization: build the directional binarization grids and binarize with
	 * {@link Binarization#binarizeV2}; the result must have the same size as the input;</li>
	 * <li>Detection: convert to a 0/1 binary image, allocate the minutia list and detect minutiae;</li>
	 * <li>Remove false minutiae;</li>
	 * <li>Count ridges between neighbouring minutiae;</li>
	 * <li>Wrap-up: convert the binary image back to 0/255 and log timings. The log messages say "secs", but the
	 * values are milliseconds.</li>
	 * </ol>
	 * On failure, the maps in {@code map} may be cleared ({@code null}) and the minutiae released.
	 *
	 * @param ret                   output: 0 ({@link ILfs#FALSE}) on success; negative on system error (e.g.
	 *                              {@link ILfs#ERROR_CODE_581} if the binary image has the wrong dimensions)
	 * @param oMinutiae             output: receives the resulting list of minutiae
	 * @param map                   input/output: receives the direction, low-contrast, low-flow and high-curve
	 *                              block maps and their dimensions
	 * @param oBinarizedImageWidth  output: width (in pixels) of the binary image
	 * @param oBinarizedImageHeight output: height (in pixels) of the binary image
	 * @param imageData             input 8-bit grayscale fingerprint image data, row-major
	 * @param imageWidth            width (in pixels) of the image
	 * @param imageHeight           height (in pixels) of the image
	 * @param lfsParams             parameters and thresholds that control LFS
	 * @return the binarized image (0 = black pixel (ridge), 255 = white pixel (valley)), or {@code null} on error
	 */
	@SuppressWarnings({ "java:S3776" })
	public int[] lfsDetectMinutiaeV2(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps map,
			AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, int[] imageData,
			final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		int[] paddedImagedata = null;
		int[] binarizedImageData = null;
		AtomicInteger paddedImageWidth = new AtomicInteger(0);
		AtomicInteger paddedImageHeight = new AtomicInteger(0);
		AtomicInteger binarizedImageWidth = new AtomicInteger(0);
		AtomicInteger binarizedImageHeight = new AtomicInteger(0);
		DirToRad dirToRad = null;
		DftWaves dftWaves = null;
		RotGrids dftGrids = null;
		RotGrids dirBinGrids = null;
		int maxPad;
		AtomicReference<Minutiae> minutiae = null;
		AtomicReferenceArray<Double> dftCoefs = null;
		long totalStartTime = System.currentTimeMillis();

		/******************/
		/* INITIALIZATION */
		/******************/

		/* Determine the maximum amount of image padding required to support */
		/* LFS processes. */
		maxPad = getInit().getMaxPaddingV2(lfsParams.getWindowSize(), lfsParams.getWindowOffset(),
				lfsParams.getDirbinGridWidth(), lfsParams.getDirbinGridHeight());

		/* Initialize lookup table for converting integer directions */
		/* to angles in radians. */
		dirToRad = new DirToRad(lfsParams.getNumDirections());
		ret.set(getInit().initDirToRad(dirToRad));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Initialize wave form lookup tables for DFT analyses. */
		/* used for direction binarization. */
		dftCoefs = new AtomicReferenceArray<>(getGlobals().getDftCoefs().length);
		for (int index = 0; index < dftCoefs.length(); index++)
			dftCoefs.set(index, getGlobals().getDftCoefs()[index]);

		dftWaves = new DftWaves(lfsParams.getNumDftWaves(), lfsParams.getWindowSize());
		ret.set(getInit().initDftWaves(dftWaves, dftCoefs));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().freeDirToRad(dirToRad);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Initialize lookup table for pixel offsets to rotated grids */
		/* used for DFT analyses. */
		dftGrids = new RotGrids(lfsParams.getStartDirAngle(), lfsParams.getNumDirections(), lfsParams.getWindowSize(),
				lfsParams.getWindowSize(), ILfs.RELATIVE_TO_ORIGIN);
		ret.set(getInit().initRotGrids(dftGrids, imageWidth, imageHeight, maxPad));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().freeDirToRad(dirToRad);
			getFree().freeDftWaves(dftWaves);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Pad input image based on max padding. */
		if (maxPad > ILfs.FALSE)// 0
		{
			// May not need to pad at all
			paddedImagedata = getImageUtil().padImage(ret, paddedImageWidth, paddedImageHeight, imageData, imageWidth,
					imageHeight, maxPad, lfsParams.getPadValue());
			if (ret.get() != ILfs.FALSE) {
				/* Free memory allocated to this point. */
				getFree().freeDirToRad(dirToRad);
				getFree().freeDftWaves(dftWaves);
				getFree().freeRotGrids(dftGrids);
				binarizedImageData = null;
				return binarizedImageData;
			}
		} else {
			/* If padding is unnecessary, then copy the input image. */
			paddedImagedata = new int[imageWidth * imageHeight];

			for (int index = 0; index < imageData.length; index++) {
				paddedImagedata[index] = imageData[index];
			}

			paddedImageWidth.set(imageWidth);
			paddedImageHeight.set(imageHeight);
		}

		/* Scale input image to 6 bits [0..63] */
		/* !!! Would like to remove this dependency eventualy !!! */
		/* But, the DFT computations will need to be changed, and */
		/* could not get this work upon first attempt. Also, if not */
		/* careful, I think accumulated power magnitudes may overflow */
		/* doubles. */
		getImageUtil().bits8To6(paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get());

		long mapStartTime = System.currentTimeMillis();

		if (isShowLogs())
			logger.debug("INITIALIZATION AND PADDING DONE");

		/******************/
		/* MAPS */
		/******************/
		/* Generate block maps from the input image. */
		ret.set(map.genImageMaps(paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get(), dirToRad, dftWaves,
				dftGrids, lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().freeDirToRad(dirToRad);
			getFree().freeDftWaves(dftWaves);
			getFree().freeRotGrids(dftGrids);
			binarizedImageData = null;
			return binarizedImageData;
		}
		/* Deallocate working memories. */
		getFree().freeDirToRad(dirToRad);
		getFree().freeDftWaves(dftWaves);
		getFree().freeRotGrids(dftGrids);

		if (isShowLogs())
			logger.debug("MAPS DONE");
		long mapEndTime = System.currentTimeMillis();

		/******************/
		/* BINARIZARION */
		/******************/
		if (isShowLogs())
			logger.debug("BINARIZATION STARTED");
		long binStartTime = System.currentTimeMillis();

		/* Initialize lookup table for pixel offsets to rotated grids */
		/* used for directional binarization. */
		dirBinGrids = new RotGrids(lfsParams.getStartDirAngle(), lfsParams.getNumDirections(),
				lfsParams.getDirbinGridWidth(), lfsParams.getDirbinGridHeight(), ILfs.RELATIVE_TO_CENTER);
		ret.set(getInit().initRotGrids(dirBinGrids, imageWidth, imageHeight, maxPad));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().freeDirToRad(dirToRad);
			getFree().freeDftWaves(dftWaves);
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Binarize input image based on NMAP information. */
		binarizedImageData = getBinarization().binarizeV2(ret, binarizedImageWidth, binarizedImageHeight,
				paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get(), map.getDirectionMap(),
				map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), dirBinGrids, lfsParams);
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			getFree().freeRotGrids(dirBinGrids);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Deallocate working memory. */
		getFree().freeRotGrids(dirBinGrids);

		/* Check dimension of binary image. If they are different from */
		/* the input image, then ERROR. */
		if ((imageWidth != binarizedImageWidth.get()) || (imageHeight != binarizedImageHeight.get())) {
			/* Free memory allocated to this point. */
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			logger.debug(
					"ERROR : lfsDetectMinutiaeV2 : binary image has bad dimensions : binarizedImageWidth = {}, binarizedImageHeight = {}",
					binarizedImageWidth, binarizedImageHeight);
			ret.set(ILfs.ERROR_CODE_581);
			binarizedImageData = null;
			return binarizedImageData;
		}

		if (isShowLogs())
			logger.debug("BINARIZATION DONE");
		long binEndTime = System.currentTimeMillis();

		/******************/
		/* DETECTION */
		/******************/
		if (isShowLogs())
			logger.debug("MINUTIA DETECTION STARTED");
		long minStartTime = System.currentTimeMillis();

		/* Convert 8-bit grayscale binary image [0,255] to */
		/* 8-bit binary image [0,1]. */
		getImageUtil().grayToBinary(1, 1, 0, binarizedImageData, imageWidth, imageHeight);

		/* Allocate initial list of minutia pointers. */
		minutiae = new AtomicReference<>();
		minutiae.set(new Minutiae());
		ret.set(getMinutiaHelper().allocMinutiae(minutiae, ILfs.MAX_MINUTIAE));
		if (ret.get() != ILfs.FALSE) {
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Detect the minutiae in the binarized image. */
		ret.set(getMinutiaHelper().detectMinutiaeV2(minutiae, binarizedImageData, imageWidth, imageHeight, map,
				lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			binarizedImageData = null;
			return binarizedImageData;
		}

		long minEndTime = System.currentTimeMillis();

		/******************/
		/* REMOVE FALSE MINUTIA */
		/******************/
		long rmStartTime = System.currentTimeMillis();
		ret.set(getRemoveMinutia().removeFalseMinutiaV2(minutiae, binarizedImageData, imageWidth, imageHeight, map,
				map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			getMinutiaHelper().freeMinutiae(minutiae);
			binarizedImageData = null;
			return binarizedImageData;
		}

		if (isShowLogs())
			logger.debug("MINUTIA DETECTION DONE");
		long rmEndTime = System.currentTimeMillis();

		/******************/
		/* RIDGE COUNTS */
		/******************/
		long ridgeStartTime = System.currentTimeMillis();
		ret.set(getRidges().countMinutiaeRidges(minutiae, binarizedImageData, imageWidth, imageHeight, lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			map.setDirectionMap(null);
			map.setLowContrastMap(null);
			map.setLowFlowMap(null);
			map.setHighCurveMap(null);
			getMinutiaHelper().freeMinutiae(minutiae);
			binarizedImageData = null;
			return binarizedImageData;
		}

		if (isShowLogs())
			logger.debug("NEIGHBOR RIDGE COUNT DONE");
		long ridgeEndTime = System.currentTimeMillis();

		/******************/
		/* WRAP-UP */
		/******************/

		/* Convert 8-bit binary image [0,1] to 8-bit */
		/* grayscale binary image [0,255]. */
		getImageUtil().grayToBinary(1, ILfs.WHITE_PIXEL, ILfs.BLACK_PIXEL, binarizedImageData, imageWidth, imageHeight);

		oBinarizedImageWidth.set(binarizedImageWidth.get());
		oBinarizedImageHeight.set(binarizedImageHeight.get());
		oMinutiae.set(minutiae.get());
		long totalEndTime = System.currentTimeMillis();

		/******************/
		/* PRINT TIMINGS */
		/******************/
		/* These Timings will print when TIMER is defined. */
		/* print MAP generation timing statistics */
		logger.debug("TIMER: MAPS time   =  {} (ms)", (float) (mapEndTime - mapStartTime));
		/* print binarization timing statistics */
		logger.debug("TIMER: Binarization time   =  {} (ms)", (float) (binEndTime - binStartTime));
		/* print minutia detection timing statistics */
		logger.debug("TIMER: Minutia Detection time   =  {} (ms)", (float) (minEndTime - minStartTime));
		/* print minutia removal timing statistics */
		logger.debug("TIMER: Minutia Removal time   =  {} (ms)", (float) (rmEndTime - rmStartTime));
		/* print neighbor ridge count timing statistics */
		logger.debug("TIMER: Neighbor Ridge Counting time   =  {} (ms)", (float) (ridgeEndTime - ridgeStartTime));
		/* print total timing statistics */
		logger.debug("TIMER: Total time   = {} (ms)", (float) (totalEndTime - totalStartTime));
		ret.set(ILfs.FALSE);

		return binarizedImageData;
	}
}