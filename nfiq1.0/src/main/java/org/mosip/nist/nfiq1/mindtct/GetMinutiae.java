package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IGetMinutiae;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMinutiae extends MindTct implements IGetMinutiae {
	private static final Logger logger = LoggerFactory.getLogger(GetMinutiae.class);
	private static GetMinutiae instance;

	private GetMinutiae() {
		super();
	}

	public static synchronized GetMinutiae getInstance() {
		if (instance == null) {
			instance = new GetMinutiae();
		}
		return instance;
	}

	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	public Detect getDetect() {
		return Detect.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getMinutiae - Takes a grayscale fingerprint image, binarizes the input
	 * #cat: image, and detects minutiae points using LFS Version 2. #cat: The
	 * routine passes back the detected minutiae, the #cat: binarized image, and a
	 * set of image quality maps. Input: imageData - grayscale fingerprint image
	 * data imageWidth - width (in pixels) of the grayscale image imageHeight -
	 * height (in pixels) of the grayscale image imageDepth - pixel depth (in bits)
	 * of the grayscale image imagePPI - the scan resolution (in pixels/mm) of the
	 * grayscale image lfsParams - parameters and thresholds for controlling LFS
	 * Output: ret - Zero - successful completion - Negative - system error
	 * oMinutiae - points to a structure containing the detected minutiae
	 * oBinarizedImageWidth - width (in pixels) of binarized image
	 * oBinarizedImageHeight - height (in pixels) of binarized image
	 * oBinarizedImageDepth - pixel depth (in bits) of binarized image Return Code:
	 * binarizedImageData - points to binarized image data
	 **************************************************************************/
	public int[] getMinutiae(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps imageMap, Quality qualityMap,
			AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, AtomicInteger oBinarizedImageDepth,
			int[] imageData, final int imageWidth, final int imageHeight, final int imageDepth, final double imagePPI,
			final LfsParams lfsParams) {

		int[] binarizedImageData = null;
		/* If input image is not 8-bit grayscale ... */
		if (imageDepth != ILfs.IMAGE_DEPTH) {
			logger.info("ERROR : get_minutiae : input image pixel depth = {} != 8.", imageDepth);
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