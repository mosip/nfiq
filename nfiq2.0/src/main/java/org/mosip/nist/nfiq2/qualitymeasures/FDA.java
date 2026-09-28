package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;

/**
 * Frequency Domain Analysis (NIST {@code FDA.cpp}): for every ridge block, the dominance of the main
 * ridge frequency in the spectrum of the ridge-perpendicular profile; reported as a 10-bin histogram.
 */
public final class FDA extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "FrequencyDomainAnalysis";
	/** Feature prefix. */
	public static final String PREFIX = "FDA_Bin10_";
	/** Histogram mean feature. */
	public static final String MEAN = PREFIX + CommonFunctions.MEAN_SUFFIX;
	/** Histogram standard deviation feature. */
	public static final String STDDEV = PREFIX + CommonFunctions.STDDEV_SUFFIX;
	/** Inner bin boundaries. */
	static final double[] HIST_LIMITS = { 0.268, 0.304, 0.33, 0.355, 0.38, 0.407, 0.44, 0.50, 1 };

	/** Ridge segmentation threshold. */
	private static final double THRESHOLD = 0.1;
	/** Slanted block width. */
	private static final int SLANTED_X = CommonFunctions.LOCAL_REGION_SQUARE;
	/** Slanted block height. */
	private static final int SLANTED_Y = CommonFunctions.LOCAL_REGION_SQUARE / 2;
	/** Weight of the side lobes around the dominant frequency. */
	private static final double SIDE_LOBE_WEIGHT = 0.3;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi or has fewer than 10 ridge blocks
	 */
	public FDA(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		int blk = CommonFunctions.LOCAL_REGION_SQUARE;
		boolean[] mask = CommonFunctions.ridgeSegment(img, cols, rows, blk, THRESHOLD);
		int eblksz = (int) Math.ceil(Math.sqrt((double) SLANTED_X * SLANTED_X + SLANTED_Y * SLANTED_Y));
		int offset = (int) Math.ceil((eblksz - blk) / 2.0);

		List<Double> data = new ArrayList<>();
		for (int r = offset; r < rows - (blk + offset - 1); r += blk) {
			for (int c = offset; c < cols - (blk + offset - 1); c += blk) {
				if (CommonFunctions.allfun(mask, cols, c, r, blk, blk)) {
					double[] cov = CommonFunctions.covcoef(img, cols, c, r, blk, blk);
					double orient = CommonFunctions.ridgeOrient(cov[0], cov[1], cov[2]);
					byte[] rotated = CommonFunctions.rotatedWindow(img, cols, rows, c - offset, r - offset, eblksz,
							orient + Math.PI / 2, true);
					data.add(fda(rotated, eblksz));
				}
			}
		}
		if (data.size() < CommonFunctions.BIN_COUNT) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Cannot compute Frequency Domain Analysis (FDA): Not enough data to generate histogram bins "
							+ "(is the image blank?)");
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
		return histogramIds(PREFIX);
	}

	/**
	 * Histogram feature identifiers for a prefix.
	 *
	 * @param prefix feature prefix
	 * @return {@code prefix0 .. prefix9, prefixMean, prefixStdDev}
	 */
	static List<String> histogramIds(String prefix) {
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < CommonFunctions.BIN_COUNT; i++) {
			ids.add(prefix + i);
		}
		ids.add(prefix + CommonFunctions.MEAN_SUFFIX);
		ids.add(prefix + CommonFunctions.STDDEV_SUFFIX);
		return ids;
	}

	/**
	 * {@code fda} after the rotation: averages each row of the central {@code 32 x 16} region of the window
	 * rotated so that ridges run horizontally, and measures how much the strongest non-DC frequency
	 * dominates.
	 *
	 * @param rotated rotated window
	 * @param size    window side
	 * @return FDA value; 1 when the peak is at the edge of the spectrum
	 */
	static double fda(byte[] rotated, int size) {
		int icBlock = size / 2;
		int xoff = SLANTED_X / 2;
		int yoff = SLANTED_Y / 2;
		int rowStart = icBlock - (xoff - 1) - 1;
		int rowEnd = icBlock + xoff;
		int colStart = icBlock - (yoff - 1) - 1;
		int colEnd = icBlock + yoff;

		double[] t = new double[rowEnd - rowStart];
		for (int r = rowStart; r < rowEnd; r++) {
			t[r - rowStart] = Cv.mean(rotated, size, colStart, r, colEnd - colStart, 1);
		}
		double[] mag = Cv.dftMagnitude(t);
		int n = mag.length - 1;
		double[] amp = new double[n];
		System.arraycopy(mag, 1, amp, 0, n);
		int loc = 0;
		double max = amp[0];
		for (int i = 1; i < n; i++) {
			if (amp[i] > max) {
				max = amp[i];
				loc = i;
			}
		}
		double denom = Cv.sum(amp, 0, n / 2);
		if (loc == 0 || loc + 1 >= n) {
			return 1.0;
		}
		return (max + SIDE_LOBE_WEIGHT * (amp[loc - 1] + amp[loc + 1])) / denom;
	}
}
