package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;

/**
 * Contrast (NIST {@code Mu.cpp}): mean of the {@code 32 x 32} block means (MMB) and the image mean (Mu).
 * The image standard deviation (sigma) is kept for actionable feedback.
 */
public final class Mu extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "Contrast";
	/** Image mean feature. */
	public static final String IMAGE_MEAN = "Mu";
	/** Mean of block means feature. */
	public static final String MEAN_OF_BLOCK_MEANS = "MMB";

	/** Standard deviation of the grey values. */
	private final double sigma;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi
	 */
	public Mu(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int width = image.getWidth();
		int height = image.getHeight();
		int bs = CommonFunctions.LOCAL_REGION_SQUARE;
		int count = ((height + bs - 1) / bs) * ((width + bs - 1) / bs);
		double avg = 0;
		for (int i = 0; i < height; i += bs) {
			for (int j = 0; j < width; j += bs) {
				int bw = Math.min(bs, width - j);
				int bh = Math.min(bs, height - i);
				avg += Cv.mean(img, width, j, i, bw, bh) / count;
			}
		}
		put(MEAN_OF_BLOCK_MEANS, avg);

		double[] ms = Cv.meanStdDev(img, width * height);
		sigma = ms[1];
		put(IMAGE_MEAN, ms[0]);
		setSpeedSince(start);
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns the standard deviation of the grey values.
	 *
	 * @return sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * Returns the identifiers of the measures, in NIST order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureIDs() {
		return List.of(MEAN_OF_BLOCK_MEANS, IMAGE_MEAN);
	}
}
