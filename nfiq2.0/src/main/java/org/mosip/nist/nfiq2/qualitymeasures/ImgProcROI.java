package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.cv.Cv;

/**
 * Region of interest (NIST {@code ImgProcROI.cpp}): segments the finger area (erode, two rounds of
 * Gaussian blur and Otsu binarisation, hole filling, largest region only) and reports the mean grey value
 * inside it.
 */
public final class ImgProcROI extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "RegionOfInterestMean";
	/** Mean grey value of the region of interest. */
	public static final String MEAN = "ImgProcROIArea_Mean";

	/** Erosion window. */
	private static final int ERODE_SIZE = 5;
	/** First Gaussian kernel size. */
	private static final int BLUR1_SIZE = 41;
	/** Second Gaussian kernel size. */
	private static final int BLUR2_SIZE = 91;

	/** Segmentation results. */
	private final Results results;

	/**
	 * A block of the ROI block grid.
	 *
	 * @param x      left
	 * @param y      top
	 * @param width  width
	 * @param height height
	 */
	public record Block(int x, int y, int width, int height) {
	}

	/**
	 * Results of {@code computeROI} ({@code ImgProcROIResults}).
	 *
	 * @param chosenBlockSize    block size
	 * @param noOfAllBlocks      number of grid blocks
	 * @param noOfCompleteBlocks number of full-size grid blocks
	 * @param noOfImagePixels    number of image pixels
	 * @param noOfROIPixels      number of ROI pixels
	 * @param meanOfROIPixels    mean grey value of ROI pixels (255 when there are none)
	 * @param stdDevOfROIPixels  sample standard deviation of ROI pixels
	 * @param vecROIBlocks       grid blocks containing at least one ROI pixel, row by row
	 */
	public record Results(int chosenBlockSize, int noOfAllBlocks, int noOfCompleteBlocks, int noOfImagePixels,
			int noOfROIPixels, double meanOfROIPixels, double stdDevOfROIPixels, List<Block> vecROIBlocks) {
	}

	/**
	 * Computes the measure.
	 *
	 * @param image cropped 500 ppi image
	 * @throws Nfiq2Exception when the image is not 500 ppi
	 */
	public ImgProcROI(FingerprintImageData image) {
		long start = System.nanoTime();
		require500Ppi(image);
		results = computeROI(image.getData(), image.getWidth(), image.getHeight(),
				CommonFunctions.LOCAL_REGION_SQUARE);
		put(MEAN, results.meanOfROIPixels());
		setSpeedSince(start);
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns the segmentation results.
	 *
	 * @return results
	 */
	public Results getImgProcResults() {
		return results;
	}

	/**
	 * Returns the identifiers of the measures, in NIST order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureIDs() {
		return List.of(MEAN);
	}

	/**
	 * {@code computeROI}.
	 *
	 * @param img pixels
	 * @param w   width
	 * @param h   height
	 * @param bs  block size
	 * @return segmentation results
	 */
	static Results computeROI(byte[] img, int w, int h, int bs) {
		int n = w * h;
		byte[] eroded = Cv.erode(img, w, h, ERODE_SIZE);
		byte[] thresh1 = Cv.thresholdOtsu(Cv.gaussianBlur(eroded, w, h, BLUR1_SIZE), n);
		byte[] roi = Cv.thresholdOtsu(Cv.gaussianBlur(thresh1, w, h, BLUR2_SIZE), n);
		Cv.fillHoles(roi, w, h);
		keepLargestRegion(roi, w, h);

		int roiPixels = 0;
		double sum = 0;
		for (int i = 0; i < n; i++) {
			if (roi[i] == 0) {
				roiPixels++;
				sum += img[i] & 0xFF;
			}
		}
		double mean = roiPixels <= 0 ? 255.0 : sum / roiPixels;
		double sumSquare = 0;
		for (int i = 0; i < n; i++) {
			if (roi[i] == 0) {
				double d = (img[i] & 0xFF) - mean;
				sumSquare += d * d;
			}
		}
		sumSquare = 1.0 / (roiPixels - 1.0) * sumSquare;
		double std = sumSquare >= 0 ? Math.sqrt(sumSquare) : 0.0;

		List<Block> blocks = new ArrayList<>();
		int all = 0;
		int complete = 0;
		for (int i = 0; i < h; i += bs) {
			for (int j = 0; j < w; j += bs) {
				int bw = Math.min(bs, w - j);
				int bh = Math.min(bs, h - i);
				all++;
				if (bw == bs && bh == bs) {
					complete++;
				}
				if (Cv.mean(roi, w, j, i, bw, bh) < 255) {
					blocks.add(new Block(j, i, bw, bh));
				}
			}
		}
		return new Results(bs, all, complete, n, roiPixels, mean, std, Collections.unmodifiableList(blocks));
	}

	/**
	 * Keeps only the black region with the largest bounding box; all other black regions become white.
	 *
	 * @param roi binary image, modified in place
	 * @param w   width
	 * @param h   height
	 */
	private static void keepLargestRegion(byte[] roi, int w, int h) {
		byte[] ff = roi.clone();
		List<int[]> seeds = new ArrayList<>();
		int maxIdx = 0;
		int maxSize = 0;
		for (int p = 0; p < w * h; p++) {
			if (ff[p] == 0) {
				int[] rect = Cv.floodFill(ff, w, h, p % w, p / w, 255);
				int size = rect[2] * rect[3];
				if (size > maxSize) {
					maxSize = size;
					maxIdx = seeds.size();
				}
				seeds.add(new int[] { p % w, p / w });
			}
		}
		for (int i = 0; i < seeds.size(); i++) {
			if (i != maxIdx) {
				Cv.floodFill(roi, w, h, seeds.get(i)[0], seeds.get(i)[1], 255);
			}
		}
	}
}
