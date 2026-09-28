package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;

/**
 * Local Clarity Score (NIST {@code LCS.cpp}): for every ridge block, how cleanly the pixels separate into
 * ridges and valleys of plausible width; reported as a 10-bin histogram.
 */
public final class LCS extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "LocalClarity";
	/** Feature prefix. */
	public static final String PREFIX = "LCS_Bin10_";
	/** Histogram mean feature. */
	public static final String MEAN = PREFIX + CommonFunctions.MEAN_SUFFIX;
	/** Histogram standard deviation feature. */
	public static final String STDDEV = PREFIX + CommonFunctions.STDDEV_SUFFIX;
	/** Inner bin boundaries. */
	static final double[] HIST_LIMITS = { 0, 0.70, 0.74, 0.77, 0.79, 0.81, 0.83, 0.85, 0.87 };

	/** Ridge segmentation threshold. */
	private static final double THRESHOLD = 0.1;
	/** Scanner resolution the width limits refer to. */
	private static final int SCANNER_RES = 500;
	/** Maximum ridge width at 125 ppi. */
	private static final double WR_MAX_125 = 5.0;
	/** Maximum valley width at 125 ppi. */
	private static final double WV_MAX_125 = 5.0;
	/** Minimum ridge width in pixels. */
	private static final double WR_MIN = 3.0;
	/** Maximum ridge width in pixels. */
	private static final double WR_MAX = 10.0;
	/** Minimum valley width in pixels. */
	private static final double WV_MIN = 2.0;
	/** Maximum valley width in pixels. */
	private static final double WV_MAX = 10.0;

	/**
	 * Computes the measures.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi or has no ridge block
	 */
	public LCS(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		int blk = CommonFunctions.LOCAL_REGION_SQUARE;
		int v1x = blk;
		int v1y = blk / 2;
		boolean[] mask = CommonFunctions.ridgeSegment(img, cols, rows, blk, THRESHOLD);
		int eblksz = (int) Math.ceil(Math.sqrt((double) v1x * v1x + v1y * v1y));
		int offset = (int) Math.ceil((eblksz - blk) / 2.0);

		List<Double> data = new ArrayList<>();
		for (int r = offset; r < rows - (blk + offset - 1); r += blk) {
			for (int c = offset; c < cols - (blk + offset - 1); c += blk) {
				if (CommonFunctions.allfun(mask, cols, c, r, blk, blk)) {
					double[] cov = CommonFunctions.covcoef(img, cols, c, r, blk, blk);
					double orient = CommonFunctions.ridgeOrient(cov[0], cov[1], cov[2]);
					byte[] rotated = CommonFunctions.rotatedWindow(img, cols, rows, c - offset, r - offset, eblksz,
							orient, false);
					data.add(loclar(rotated, eblksz, v1x, v1y));
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
	 * {@code loclar} after the rotation: local clarity of one block, 0 (worst) to 1 (best).
	 *
	 * @param rotated window rotated so that ridges run vertically
	 * @param size    window side
	 * @param v1x     slanted block width
	 * @param v1y     slanted block height
	 * @return clarity score
	 */
	static double loclar(byte[] rotated, int size, int v1x, int v1y) {
		int icBlock = size / 2;
		int xoff = v1x / 2;
		int yoff = v1y / 2;
		int rowStart = icBlock - (yoff - 1) - 1;
		int colStart = icBlock - (xoff - 1) - 1;
		int v2Rows = icBlock + yoff - rowStart;
		int v2Cols = icBlock + xoff - colStart;

		CommonFunctions.RidgeValley rv = CommonFunctions.ridgeValleyStructure(rotated, size, colStart, rowStart,
				v2Cols, v2Rows);
		int[] ridval = rv.ridval();
		double[] dt = rv.dt();
		boolean begrid = ridval[0] != 0;

		int n = ridval.length;
		List<Integer> changeIndex = new ArrayList<>();
		for (int i = 1; i < n; i++) {
			if (ridval[i] != ridval[i - 1]) {
				changeIndex.add((i - 1) & 0xFF);
			}
		}
		if (changeIndex.isEmpty()) {
			return 0.0;
		}
		int[] wrv = new int[changeIndex.size()];
		wrv[0] = changeIndex.get(0);
		for (int i = 1; i < wrv.length; i++) {
			wrv[i] = (changeIndex.get(i) - changeIndex.get(i - 1)) & 0xFF;
		}

		double sc = SCANNER_RES;
		double rScaleNorm = (sc / 125.0) * WR_MAX_125;
		double vScaleNorm = (sc / 125.0) * WV_MAX_125;
		double nwrMin = WR_MIN / rScaleNorm;
		double nwrMax = WR_MAX / rScaleNorm;
		double nwvMin = WV_MIN / rScaleNorm;
		double nwvMax = WV_MAX / rScaleNorm;

		double[] nwr = new double[wrv.length];
		double[] nwv = new double[wrv.length];
		int nr = 0;
		int nv = 0;
		for (int i = 0; i < wrv.length; i += 2) {
			if (begrid) {
				nwr[nr++] = wrv[i] / rScaleNorm;
			} else {
				nwv[nv++] = wrv[i] / vScaleNorm;
			}
		}
		for (int i = 0; i < wrv.length - 1; i += 2) {
			if (begrid) {
				nwv[nv++] = wrv[i + 1] / vScaleNorm;
			} else {
				nwr[nr++] = wrv[i + 1] / rScaleNorm;
			}
		}
		double muNwr = nr > 0 ? Cv.mean(nwr, 0, nr) : 0;
		double muNwv = nv > 0 ? Cv.mean(nwv, 0, nv) : 0;

		if (!(muNwr >= nwrMin && muNwr <= nwrMax && muNwv >= nwvMin && muNwv <= nwvMax)) {
			return 0.0;
		}
		int ridgeGood = 0;
		int valleyGood = 0;
		int ridgeCount = 0;
		int valleyCount = 0;
		for (int i = 0; i < v2Cols; i++) {
			long threshold = scalarThreshold(dt[i]);
			for (int r = 0; r < v2Rows; r++) {
				int v = CommonFunctions.px(rotated, size, colStart + i, rowStart + r);
				if (ridval[i] == 1) {
					ridgeGood += v >= threshold ? 1 : 0;
				} else {
					valleyGood += v < threshold ? 1 : 0;
				}
			}
			if (ridval[i] == 1) {
				ridgeCount += v2Rows;
			} else {
				valleyCount += v2Rows;
			}
		}
		double alpha = (double) valleyGood / valleyCount;
		double beta = (double) ridgeGood / ridgeCount;
		return 1.0 - (alpha + beta) / 2.0;
	}

	/**
	 * Integer threshold OpenCV uses when comparing an 8-bit matrix with a double scalar using
	 * {@code >=} or {@code <}: the ceiling of a non-integral value. It is not saturated: a threshold above
	 * 255 (or below 0) makes every comparison false (or true), as in OpenCV.
	 *
	 * @param value double threshold
	 * @return integer threshold
	 */
	static long scalarThreshold(double value) {
		long ival = (long) Math.rint(value);
		if (value != ival) {
			ival = (long) Math.ceil(value);
		}
		return ival;
	}
}
