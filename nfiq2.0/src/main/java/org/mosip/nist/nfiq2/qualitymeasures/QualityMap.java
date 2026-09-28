package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * Region of interest coherence (NIST {@code QualityMap.cpp}): gradient coherence of every
 * {@code 32 x 32} block that belongs to the region of interest found by {@link ImgProcROI}.
 */
public final class QualityMap extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "RegionOfInterestCoherence";
	/** Sum of block coherences. */
	public static final String COHERENCE_SUM = "OrientationMap_ROIFilter_CoherenceSum";
	/** Mean block coherence over the ROI blocks. */
	public static final String COHERENCE_MEAN = "OrientationMap_ROIFilter_CoherenceRel";

	/**
	 * Computes the measures.
	 *
	 * @param image          cropped 500 ppi image
	 * @param imgProcResults region of interest of the same image
	 * @throws Nfiq2Exception when the image is not 500 ppi
	 */
	public QualityMap(FingerprintImageData image, ImgProcROI.Results imgProcResults) {
		long start = System.nanoTime();
		require500Ppi(image);
		byte[] img = image.getData();
		int cols = image.getWidth();
		int rows = image.getHeight();
		int bs = CommonFunctions.LOCAL_REGION_SQUARE;
		Set<ImgProcROI.Block> roiBlocks = new HashSet<>(imgProcResults.vecROIBlocks());
		double coherenceSum = 0.0;
		for (int i = 0; i < rows; i += bs) {
			for (int j = 0; j < cols; j += bs) {
				int bw = Math.min(bs, cols - j);
				int bh = Math.min(bs, rows - i);
				if (roiBlocks.contains(new ImgProcROI.Block(j, i, bw, bh))) {
					double coherence = coherenceOfBlock(img, cols, j, i, bw, bh);
					coherenceSum += Double.isNaN(coherence) ? 0.0 : coherence;
				}
			}
		}
		int roiCount = imgProcResults.vecROIBlocks().size();
		double coherenceRel = roiCount <= 0 ? 0.0 : coherenceSum / roiCount;
		put(COHERENCE_MEAN, coherenceRel);
		put(COHERENCE_SUM, coherenceSum);
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
		return List.of(COHERENCE_MEAN, COHERENCE_SUM);
	}

	/**
	 * Coherence part of {@code getAngleOfBlock}.
	 *
	 * @param img pixels
	 * @param w   image width
	 * @param x0  left
	 * @param y0  top
	 * @param bw  block width
	 * @param bh  block height
	 * @return coherence in [0, 1]
	 */
	static double coherenceOfBlock(byte[] img, int w, int x0, int y0, int bw, int bh) {
		double[][] g = CommonFunctions.numericalGradients(img, w, x0, y0, bw, bh);
		double sumY = 0.0;
		double sumX = 0.0;
		double cohSum2 = 0.0;
		for (int i = 0; i < bw * bh; i++) {
			double gx0 = g[0][i];
			double gy0 = g[1][i];
			double gy = 2 * gx0 * gy0;
			if (!Double.isNaN(gy)) {
				sumY += gy;
			}
			double gx = gx0 * gx0 - gy0 * gy0;
			if (!Double.isNaN(gx)) {
				sumX += gx;
			}
			cohSum2 += Math.sqrt(gy * gy + gx * gx);
		}
		double cohSum1 = Math.sqrt(sumX * sumX + sumY * sumY);
		if (Double.isNaN(cohSum1)) {
			cohSum1 = 0.0;
		}
		if (Double.isNaN(cohSum2)) {
			cohSum2 = 0.0;
		}
		return cohSum2 != 0 ? cohSum1 / cohSum2 : 0;
	}
}
