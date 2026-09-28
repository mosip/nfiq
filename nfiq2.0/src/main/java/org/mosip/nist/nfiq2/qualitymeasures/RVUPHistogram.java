package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * Ridge Valley Uniformity (NIST {@code RVUPHistogram.cpp}): ratios of consecutive ridge and valley widths
 * in every ridge block; reported as a 10-bin histogram.
 */
public final class RVUPHistogram extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "RidgeValleyUniformity";
	/** Feature prefix. */
	public static final String PREFIX = "RVUP_Bin10_";
	/** Histogram mean feature. */
	public static final String MEAN = PREFIX + CommonFunctions.MEAN_SUFFIX;
	/** Histogram standard deviation feature. */
	public static final String STDDEV = PREFIX + CommonFunctions.STDDEV_SUFFIX;
	/** Inner bin boundaries. */
	static final double[] HIST_LIMITS = { 0.5, 0.667, 0.8, 1, 1.25, 1.5, 2, 24, 30 };

	/** Ridge segmentation threshold. */
	private static final double THRESHOLD = 0.1;
	/** Slanted block width. */
	private static final int SLANTED_X = CommonFunctions.LOCAL_REGION_SQUARE;
	/** Slanted block height. */
	private static final int SLANTED_Y = CommonFunctions.LOCAL_REGION_SQUARE / 2;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi or no ratio could be computed
	 */
	public RVUPHistogram(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		int blk = CommonFunctions.LOCAL_REGION_SQUARE;
		boolean[] mask = CommonFunctions.ridgeSegment(img, cols, rows, blk, THRESHOLD);
		int eblksz = (int) Math.ceil(Math.sqrt((double) SLANTED_X * SLANTED_X + SLANTED_Y * SLANTED_Y));
		int offset = (int) Math.ceil((eblksz - blk) / 2.0);

		List<Double> ratios = new ArrayList<>();
		for (int r = offset; r < rows - (blk + offset - 1); r += blk) {
			for (int c = offset; c < cols - (blk + offset - 1); c += blk) {
				if (CommonFunctions.allfun(mask, cols, c, r, blk, blk)) {
					double[] cov = CommonFunctions.covcoef(img, cols, c, r, blk, blk);
					double orient = CommonFunctions.ridgeOrient(cov[0], cov[1], cov[2]);
					byte[] rotated = CommonFunctions.rotatedWindow(img, cols, rows, c - offset, r - offset, eblksz,
							orient, true);
					rvuhist(rotated, eblksz, ratios);
				}
			}
		}
		putAll(CommonFunctions.histogramFeatures(PREFIX, HIST_LIMITS, ratios));
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
	 * {@code rvuhist} after the rotation: appends the ridge/valley width ratios of one block.
	 *
	 * @param rotated window rotated so that ridges run vertically
	 * @param size    window side
	 * @param out     ratio list to append to
	 */
	static void rvuhist(byte[] rotated, int size, List<Double> out) {
		int icBlock = size / 2;
		int xoff = SLANTED_X / 2;
		int yoff = SLANTED_Y / 2;
		int rowStart = icBlock - (yoff - 1) - 1;
		int colStart = icBlock - (xoff - 1) - 1;
		int rowsV2 = icBlock + yoff - rowStart;
		int colsV2 = icBlock + xoff - colStart;
		int[] ridval = CommonFunctions.ridgeValleyStructure(rotated, size, colStart, rowStart, colsV2, rowsV2)
				.ridval();

		int n = ridval.length;
		List<Integer> changeIndex = new ArrayList<>();
		for (int i = 1; i < n - 1; i++) {
			if (ridval[i] != ridval[i - 1]) {
				changeIndex.add(i - 1);
			}
		}
		if (changeIndex.isEmpty()) {
			return;
		}
		int first = changeIndex.get(0);
		int last = changeIndex.get(changeIndex.size() - 1);
		if (first + 1 >= last) {
			return;
		}
		int begrid = ridval[first + 1];

		int changes = changeIndex.size() - 1;
		if (changes <= 1) {
			return;
		}
		int[] complete = new int[changes];
		for (int i = 1; i <= changes; i++) {
			complete[i - 1] = (changeIndex.get(i) - first) & 0xFF;
		}
		int[] widths = new int[changes - 1];
		for (int i = changes - 1, k = 0; i > 0; i--, k++) {
			widths[k] = (complete[i] - complete[i - 1]) & 0xFF;
		}
		double[] ratios = new double[widths.length - 1];
		for (int m = 0; m < ratios.length; m++) {
			ratios[m] = (double) widths[m] / widths[m + 1];
		}
		for (int i = begrid; i < ratios.length; i += 2) {
			ratios[i] = 1 / ratios[i];
		}
		for (double r : ratios) {
			out.add(r);
		}
	}
}
