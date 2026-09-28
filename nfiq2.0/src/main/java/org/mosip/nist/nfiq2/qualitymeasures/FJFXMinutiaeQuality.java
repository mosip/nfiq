package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.List;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;
import org.mosip.nist.nfiq2.frfxll.FrfxllMinutia;

/**
 * Minutiae quality (NIST {@code FJFXMinutiaeQuality.cpp}): share of minutiae whose surrounding block is
 * slightly darker than the image mean, and share whose surrounding block has an orientation certainty
 * above 80.
 */
public final class FJFXMinutiaeQuality extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "MinutiaeQuality";
	/** Share of minutiae with a normalised block mean in {@code (0, 0.5]}. */
	public static final String PERCENT_IMAGE_MEAN_50 = "FJFXPos_Mu_MinutiaeQuality_2";
	/** Share of minutiae with a block OCL quality above 80. */
	public static final String PERCENT_ORIENTATION_CERTAINTY_80 = "FJFXPos_OCL_MinutiaeQuality_80";

	/** Block size around each minutia. */
	private static final int BLOCK_SIZE = CommonFunctions.LOCAL_REGION_SQUARE;
	/** OCL quality threshold. */
	private static final int OCL_THRESHOLD = 80;

	/**
	 * Computes the measures.
	 *
	 * @param image       cropped image
	 * @param minutiaData minutiae found by {@link FingerJetFX}
	 * @throws Nfiq2Exception when the image is smaller than one block
	 */
	public FJFXMinutiaeQuality(FingerprintImageData image, List<FrfxllMinutia> minutiaData) {
		long start = System.nanoTime();
		byte[] img = image.getData();
		int width = image.getWidth();
		int height = image.getHeight();
		double size = minutiaData.size();

		double[] ms = Cv.meanStdDev(img, width * height);
		int muBin2 = 0;
		for (FrfxllMinutia m : minutiaData) {
			double q = muQuality(img, width, height, m, ms[0], ms[1]);
			if (q > 0.0 && q <= 0.5) {
				muBin2++;
			}
		}
		put(PERCENT_IMAGE_MEAN_50, muBin2 / size);

		int oclHigh = 0;
		for (FrfxllMinutia m : minutiaData) {
			if (oclQuality(img, width, height, m) > OCL_THRESHOLD) {
				oclHigh++;
			}
		}
		put(PERCENT_ORIENTATION_CERTAINTY_80, oclHigh / size);
		setSpeedSince(start);
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns the identifiers of the measures, in NIST order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureIDs() {
		return List.of(PERCENT_IMAGE_MEAN_50, PERCENT_ORIENTATION_CERTAINTY_80);
	}

	/**
	 * {@code computeMuMinQuality} for one minutia: {@code (imageMean - blockMean) / imageStdDev}.
	 *
	 * @param img    pixels
	 * @param width  width
	 * @param height height
	 * @param m      minutia
	 * @param mean   image mean
	 * @param std    image standard deviation
	 * @return quality
	 */
	static double muQuality(byte[] img, int width, int height, FrfxllMinutia m, double mean, double std) {
		int leftX = Math.max(m.x() - BLOCK_SIZE / 2, 0);
		int topY = Math.max(m.y() - BLOCK_SIZE / 2, 0);
		int bw = leftX + BLOCK_SIZE > width ? width - leftX : BLOCK_SIZE;
		int bh = topY + BLOCK_SIZE > height ? height - topY : BLOCK_SIZE;
		return (mean - Cv.mean(img, width, leftX, topY, bw, bh)) / std;
	}

	/**
	 * {@code computeOCLMinQuality} for one minutia: OCL of the full block closest to the minutia, as
	 * {@code 0..100}.
	 *
	 * @param img    pixels
	 * @param width  width
	 * @param height height
	 * @param m      minutia
	 * @return quality
	 * @throws Nfiq2Exception when the image is smaller than one block
	 */
	static int oclQuality(byte[] img, int width, int height, FrfxllMinutia m) {
		int leftX = Math.max(m.x() - BLOCK_SIZE / 2, 0);
		int topY = Math.max(m.y() - BLOCK_SIZE / 2, 0);
		if (leftX + BLOCK_SIZE > width) {
			leftX = width - BLOCK_SIZE;
		}
		if (topY + BLOCK_SIZE > height) {
			topY = height - BLOCK_SIZE;
		}
		if (leftX < 0 || topY < 0) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Cannot compute FJFX based minutiae quality features: image smaller than " + BLOCK_SIZE
							+ " pixels");
		}
		double ocl = OCLHistogram.oclValueOfBlock(img, width, leftX, topY);
		if (Double.isNaN(ocl)) {
			ocl = 0.0;
		}
		return (int) ((ocl * 100) + 0.5);
	}
}
