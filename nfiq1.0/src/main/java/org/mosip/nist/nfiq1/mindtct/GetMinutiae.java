package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IGetMinutiae;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level MINDTCT entry point: detects minutiae in a grayscale fingerprint image and builds the image
 * quality maps that NFIQ 1.0 feature extraction uses.
 * <p>
 * Port of NIST {@code get_minutiae()} ({@code getmin.c}). It checks the pixel depth, runs LFS V2 detection
 * through {@link Detect#lfsDetectMinutiaeV2}, builds the integrated quality map, and assigns each minutia a
 * reliability from the quality map.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state
 * (results are returned through the caller-supplied output holders and map objects).
 */
public class GetMinutiae extends MindTct implements IGetMinutiae {
	/** SLF4J logger for input validation errors. */
	private static final Logger logger = LoggerFactory.getLogger(GetMinutiae.class);
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static GetMinutiae instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private GetMinutiae() {
		super();
	}

	/**
	 * Returns the shared {@code GetMinutiae} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized GetMinutiae getInstance() {
		if (instance == null) {
			instance = new GetMinutiae();
		}
		return instance;
	}

	/**
	 * Returns the minutia list helper, used to release minutiae on error.
	 *
	 * @return the {@link MinutiaHelper} singleton
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the LFS minutiae detection driver.
	 *
	 * @return the {@link Detect} singleton
	 */
	public Detect getDetect() {
		return Detect.getInstance();
	}

	/**
	 * Binarizes a grayscale fingerprint image and detects minutiae using LFS Version 2 (NIST
	 * {@code get_minutiae}).
	 * <p>
	 * Returns the detected minutiae, the binarized image and a set of image quality maps. Steps:
	 * <ol>
	 * <li>reject images whose depth is not {@link ILfs#IMAGE_DEPTH} (8 bits);</li>
	 * <li>detect minutiae and fill the image maps with {@link Detect#lfsDetectMinutiaeV2};</li>
	 * <li>build the integrated quality map with {@code Quality.generateQualityMap(Maps)};</li>
	 * <li>assign each minutia a reliability with {@code Quality.combinedMinutiaQuality(...)}.</li>
	 * </ol>
	 * If step 3 or 4 fails, the minutiae in {@code oMinutiae} are released.
	 *
	 * @param ret                   output: 0 ({@link ILfs#FALSE}) on success; {@link ILfs#ERROR_CODE_02} if the
	 *                              pixel depth is not 8; otherwise a negative system error code
	 * @param oMinutiae             output: holder that receives the detected minutiae
	 * @param imageMap              input/output: filled with the direction, low-contrast, low-flow, high-curve
	 *                              and quality maps
	 * @param qualityMap            quality helper used to build the quality map and minutia reliabilities
	 * @param oBinarizedImageWidth  output: width (in pixels) of the binarized image
	 * @param oBinarizedImageHeight output: height (in pixels) of the binarized image
	 * @param oBinarizedImageDepth  output: pixel depth (in bits) of the binarized image
	 * @param imageData             grayscale fingerprint image data, row-major
	 * @param imageWidth            width (in pixels) of the grayscale image
	 * @param imageHeight           height (in pixels) of the grayscale image
	 * @param imageDepth            pixel depth (in bits) of the grayscale image; must be 8
	 * @param imagePPI              scan resolution of the grayscale image (the NIST documentation gives this in
	 *                              pixels/mm)
	 * @param lfsParams             parameters and thresholds that control LFS
	 * @return the binarized image data, or {@code null} on error
	 */
	public int[] getMinutiae(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps imageMap, Quality qualityMap,
			AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, AtomicInteger oBinarizedImageDepth,
			int[] imageData, final int imageWidth, final int imageHeight, final int imageDepth, final double imagePPI,
			final LfsParams lfsParams) {

		int[] binarizedImageData = null;
		/* If input image is not 8-bit grayscale ... */
		if (imageDepth != ILfs.IMAGE_DEPTH) {
			logger.error("ERROR : get_minutiae : input image pixel depth = {} != 8.", imageDepth);
			ret.set(ILfs.ERROR_CODE_02);
			return binarizedImageData;
		}

		/* Detect minutiae in grayscale fingerpeint image. */
		binarizedImageData = getDetect().lfsDetectMinutiaeV2(ret, oMinutiae, imageMap, oBinarizedImageWidth,
				oBinarizedImageHeight, imageData, imageWidth, imageHeight, lfsParams);
		if (ret.get() != ILfs.FALSE) {
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Build integrated quality map. */
		ret.set(qualityMap.generateQualityMap(imageMap));
		if (ret.get() != ILfs.FALSE) {
			getMinutiaHelper().freeMinutiae(oMinutiae);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Assign reliability from quality map. */
		ret.set(qualityMap.combinedMinutiaQuality(oMinutiae, imageMap, lfsParams.getBlockOffsetSize(), imageData, imageWidth,
				imageHeight, imageDepth, imagePPI));
		if (ret.get() != ILfs.FALSE) {
			getMinutiaHelper().freeMinutiae(oMinutiae);
			binarizedImageData = null;
			return binarizedImageData;
		}

		/* Set output pointers. */
		oBinarizedImageDepth.set(imageDepth);

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return binarizedImageData;
	}
}