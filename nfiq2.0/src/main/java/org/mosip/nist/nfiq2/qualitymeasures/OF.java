package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * Orientation Flow (NIST {@code OF.cpp}): mean absolute orientation difference between each ridge block
 * and its eight neighbours, above a 4 degree tolerance; reported as a 10-bin histogram.
 */
public final class OF extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "OrientationFlow";
	/** Feature prefix. */
	public static final String PREFIX = "OF_Bin10_";
	/** Histogram mean feature. */
	public static final String MEAN = PREFIX + CommonFunctions.MEAN_SUFFIX;
	/** Histogram standard deviation feature. */
	public static final String STDDEV = PREFIX + CommonFunctions.STDDEV_SUFFIX;
	/** Inner bin boundaries. */
	static final double[] HIST_LIMITS = { 1.715e-2, 3.5e-2, 5.57e-2, 8.1e-2, 1.15e-1, 1.718e-1, 2.569e-1, 4.758e-1,
			7.48e-1 };
	/** Orientation differences up to this angle (degrees) are ignored. */
	public static final double ANGLE_MIN = 4.0;

	/** Ridge segmentation threshold. */
	private static final double THRESHOLD = 0.1;
	/** Slanted block width. */
	private static final int SLANTED_X = CommonFunctions.LOCAL_REGION_SQUARE;
	/** Slanted block height. */
	private static final int SLANTED_Y = CommonFunctions.LOCAL_REGION_SQUARE / 2;
	/** Degrees to radians. */
	private static final double DEG2RAD = Math.PI / 180.0;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi or no block qualifies
	 */
	public OF(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		int blk = CommonFunctions.LOCAL_REGION_SQUARE;
		boolean[] mask = CommonFunctions.ridgeSegment(img, cols, rows, blk, THRESHOLD);
		int eblksz = (int) Math.ceil(Math.sqrt((double) SLANTED_X * SLANTED_X + SLANTED_Y * SLANTED_Y));
		int offset = (int) Math.ceil((eblksz - blk) / 2.0);

		int mapRows = 0;
		for (int r = offset; r < rows - (blk + offset - 1); r += blk) {
			mapRows++;
		}
		int mapCols = 0;
		for (int c = offset; c < cols - (blk + offset - 1); c += blk) {
			mapCols++;
		}
		double[] orient = new double[mapRows * mapCols];
		boolean[] maskB = new boolean[mapRows * mapCols];
		for (int br = 0; br < mapRows; br++) {
			int r = offset + br * blk;
			for (int bc = 0; bc < mapCols; bc++) {
				int c = offset + bc * blk;
				maskB[br * mapCols + bc] = CommonFunctions.allfun(mask, cols, c, r, blk, blk);
				double[] cov = CommonFunctions.covcoef(img, cols, c, r, blk, blk);
				orient[br * mapCols + bc] = CommonFunctions.ridgeOrient(cov[0], cov[1], cov[2]);
			}
		}

		double threeSixtyRad = DEG2RAD * 360.0;
		double angdiff = (90.0 - ANGLE_MIN) * DEG2RAD;
		double angmin = ANGLE_MIN * DEG2RAD;
		List<Double> data = new ArrayList<>();
		for (int i = 0; i < mapRows; i++) {
			for (int j = 0; j < mapCols; j++) {
				double center = orient[i * mapCols + j];
				double sum = 0;
				boolean allFg = true;
				for (int di = -1; di <= 1; di++) {
					for (int dj = -1; dj <= 1; dj++) {
						int y = i + di;
						int x = j + dj;
						boolean inside = y >= 0 && y < mapRows && x >= 0 && x < mapCols;
						double v = inside ? orient[y * mapCols + x] : 0;
						double d = Math.abs(center - v);
						sum += Math.min(d, threeSixtyRad - d);
						allFg &= inside && maskB[y * mapCols + x];
					}
				}
				double loq = sum / 8.0;
				if (loq > angmin && allFg) {
					data.add((loq - angmin) / angdiff);
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
}
