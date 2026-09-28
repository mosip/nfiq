package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * Orientation Certainty Level (NIST {@code OCLHistogram.cpp}): eigenvalue ratio of the gradient
 * covariance of every full {@code 32 x 32} block; reported as a 10-bin histogram.
 */
public final class OCLHistogram extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "OrientationCertainty";
	/** Feature prefix. */
	public static final String PREFIX = "OCL_Bin10_";
	/** Histogram mean feature. */
	public static final String MEAN = PREFIX + CommonFunctions.MEAN_SUFFIX;
	/** Histogram standard deviation feature. */
	public static final String STDDEV = PREFIX + CommonFunctions.STDDEV_SUFFIX;
	/** Inner bin boundaries. */
	static final double[] HIST_LIMITS = { 0.337, 0.479, 0.579, 0.655, 0.716, 0.766, 0.81, 0.852, 0.898 };
	/** Block size ({@code BS_OCL}). */
	static final int BLOCK_SIZE = CommonFunctions.LOCAL_REGION_SQUARE;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi or no block has any gradient
	 */
	public OCLHistogram(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		List<Double> data = new ArrayList<>();
		for (int i = 0; i + BLOCK_SIZE <= rows; i += BLOCK_SIZE) {
			for (int j = 0; j + BLOCK_SIZE <= cols; j += BLOCK_SIZE) {
				double ocl = oclValueOfBlock(img, cols, j, i);
				if (!Double.isNaN(ocl)) {
					data.add(ocl);
				}
			}
		}
		putAll(CommonFunctions.histogramFeatures(PREFIX, HIST_LIMITS, data));
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
		return FDA.histogramIds(PREFIX);
	}

	/**
	 * {@code getOCLValueOfBlock}: {@code 1 - lambdaMin / lambdaMax} of the gradient covariance.
	 *
	 * @param img pixels
	 * @param w   image width
	 * @param x0  block left
	 * @param y0  block top
	 * @return OCL in 0 (worst) .. 1 (best), or NaN when the block has no gradient (block not used)
	 */
	static double oclValueOfBlock(byte[] img, int w, int x0, int y0) {
		double[][] g = CommonFunctions.numericalGradients(img, w, x0, y0, BLOCK_SIZE, BLOCK_SIZE);
		double[] gx = g[0];
		double[] gy = g[1];
		double a = 0;
		double b = 0;
		double c = 0;
		for (int k = 0; k < BLOCK_SIZE; k++) {
			for (int l = 0; l < BLOCK_SIZE; l++) {
				int i = l * BLOCK_SIZE + k;
				a += gx[i] * gx[i];
				b += gy[i] * gy[i];
				c += gx[i] * gy[i];
			}
		}
		int n = BLOCK_SIZE * BLOCK_SIZE;
		a /= n;
		b /= n;
		c /= n;
		double root = Math.sqrt((a - b) * (a - b) + 4 * (c * c));
		double eigvMax = ((a + b) + root) / 2.0;
		double eigvMin = ((a + b) - root) / 2.0;
		if (eigvMax == 0) {
			return Double.NaN;
		}
		return 1.0 - eigvMin / eigvMax;
	}
}
