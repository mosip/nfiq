package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWave;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.IDft;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dft extends MindTct implements IDft {
	private static final Logger logger = LoggerFactory.getLogger(Dft.class);

	private static Dft instance;

	private Dft() {
		super();
	}

	public static synchronized Dft getInstance() {
		if (instance == null) {
			instance = new Dft();
		}
		return instance;
	}

	public Defs getDefs() {
		return Defs.getInstance();
	}

	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	public Globals getGlobals() {
		return Globals.getInstance();
	}

	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	public Free getFree() {
		return Free.getInstance();
	}

	public Init getInit() {
		return Init.getInstance();
	}

	public Binarization getBinarization() {
		return Binarization.getInstance();
	}

	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	public Sort getSort() {
		return Sort.getInstance();
	}

	public Detect getDetect() {
		return Detect.getInstance();
	}

	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}

	public Ridges getRidges() {
		return Ridges.getInstance();
	}

	public Line getLine() {
		return Line.getInstance();
	}

	public Contour getContour() {
		return Contour.getInstance();
	}

	public Loop getLoop() {
		return Loop.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: dftDirPowers - Conducts the DFT analysis on a block of image data.
	 * #cat: The image block is sampled across a range of orientations #cat:
	 * (directions) and multiple wave forms of varying frequency are #cat: applied
	 * at each orientation. At each orentation, pixels are #cat: accumulated along
	 * each rotated pixel row, creating a vector #cat: of pixel row sums. Each DFT
	 * wave form is then applied #cat: individually to this vector of pixel row
	 * sums. A DFT power #cat: value is computed for each wave form (frequency0 at
	 * each #cat: orientaion within the image block. Therefore, the resulting DFT
	 * #cat: power vectors are of dimension (N Waves X M Directions). #cat: The
	 * power signatures derived form this process are used to #cat: determine
	 * dominant direction flow within the image block. Input: paddedImagedata - the
	 * padded input image. It is important that the image be properly padded, or
	 * else the sampling at various block orientations may result in accessing
	 * unkown memory. blockOffset - the pixel offset form the origin of the padded
	 * image to the origin of the current block in the image paddedImageWidth - the
	 * width (in pixels) of the padded input image paddedImageHeight - the height
	 * (in pixels) of the padded input image dftWaves - structure containing the DFT
	 * wave forms dftGrids - structure containing the rotated pixel grid offsets
	 * Output: powers - DFT power computed from each wave form frequencies at each
	 * orientation (direction) in the current image block Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
	@SuppressWarnings("unused")
	public int dftDirPowers(AtomicReferenceArray<Double[]> powers, int[] paddedImagedata, final int blockOffset,
			final int paddedImageWidth, final int paddedImageHeight, DftWaves dftWaves, RotGrids dftGrids) {
		int[] rowSums;
		int paddedImageDataIndex;

		/* Allocate line sum vector, and initialize to zeros */
		/* This routine requires square block (grid), so ERROR otherwise. */
		if (dftGrids.getGridWidth() != dftGrids.getGridHeight()) {
			logger.error("ERROR : dftDirPowers : DFT grids must be square\n");
			return (-90);
		}
		rowSums = new int[dftGrids.getGridWidth()];
		if (rowSums == null) {
			logger.error("ERROR : dftDirPowers : rowSums : Null \n");
			return (ILfs.ERROR_CODE_91);
		}

		/* Foreach direction ... */
		for (int dirIndex = 0; dirIndex < dftGrids.getNoOfGrids(); dirIndex++) {
			/* Compute vector of line sums from rotated grid */
			paddedImageDataIndex = (0 + blockOffset);
			sumRotBlockRows(rowSums, paddedImagedata, paddedImageDataIndex,
					new AtomicIntegerArray(dftGrids.getGrids()[dirIndex]), dftGrids.getGridWidth());

			/* Foreach DFT wave ... */
			for (int waveIndex = 0; waveIndex < dftWaves.getNWaves(); waveIndex++) {
				Double[] arrpowers = powers.get(waveIndex);
				AtomicReference<Double> refpowers = new AtomicReference<>(arrpowers[dirIndex]);

				computeDftPower(refpowers, rowSums, dftWaves.getWaves()[waveIndex], dftWaves.getWaveLen());

				arrpowers[dirIndex] = refpowers.get();
				powers.set(waveIndex, arrpowers);
			}
		}

		/* Deallocate working memory. */
		getFree().free(rowSums);

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: sumRotBlockRows - Computes a vector or pixel row sums by sampling #cat:
	 * the current image block at a given orientation. The #cat: sampling is
	 * conducted using a precomputed set of rotated #cat: pixel offsets (called a
	 * grid) relative to the orgin of #cat: the image block. Input: paddedImagedata
	 * - the current image block paddedImageDataIndex - the pixel address of the
	 * origin of the current image block gridOffsets - the rotated pixel offsets for
	 * a block-sized grid rotated according to a specific orientation
	 * blockOffsetSize - the width and height of the image block and thus the size
	 * of the rotated grid Output: rowSums - the resulting vector of pixel row sums
	 **************************************************************************/
	public void sumRotBlockRows(int[] rowSums, int[] paddedImagedata, final int paddedImageDataIndex,
			final AtomicIntegerArray gridOffsets, final int blockOffsetSize) {
		int gi;

		/* Initialize rotation offset index. */
		gi = 0;

		/* For each row in block ... */
		for (int iy = 0; iy < blockOffsetSize; iy++) {
			/* The sums are accumlated along the rotated rows of the grid, */
			/* so initialize row sum to 0. */
			rowSums[iy] = 0;
			/* Foreach column in block ... */
			for (int ix = 0; ix < blockOffsetSize; ix++) {
				/* Accumulate pixel value at rotated grid position in image */
				rowSums[iy] += paddedImagedata[paddedImageDataIndex + gridOffsets.get(gi)];
				gi++;
			}
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: computeDftPower - Computes the DFT power by applying a specific wave
	 * form #cat: frequency to a vector of pixel row sums computed from a #cat:
	 * specific orientation of the block image Input: rowSums - accumulated rows of
	 * pixels from within a rotated grid overlaying an input image block wave - the
	 * wave form (cosine and sine components) at a specific frequency waveLen - the
	 * length of the wave form (must match the height of the image block which is
	 * the length of the rowsum vector) Output: oPower - the computed DFT power for
	 * the given wave form at the given orientation within the image block
	 **************************************************************************/
	public void computeDftPower(AtomicReference<Double> oPower, final int[] rowSums, final DftWave dftWave,
			final int waveLen) {
		/* Initialize accumulators */
		double cospart = 0.0d;
		double sinpart = 0.0d;

		/* Accumulate cos and sin components of DFT. */
		for (int i = 0; i < waveLen; i++) {
			/* Multiply each rotated row sum by its */
			/* corresponding cos or sin point in DFT wave. */
			cospart += (rowSums[i] * dftWave.getCos()[i]);
			sinpart += (rowSums[i] * dftWave.getSin()[i]);
		}

		/* Power is the sum of the squared cos and sin components */
		oPower.set((cospart * cospart) + (sinpart * sinpart));
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getDftPowerStats - Derives statistics from a set of DFT power vectors.
	 * #cat: Statistics are computed for all but the lowest frequency #cat: wave
	 * form, including the Maximum power for each wave form, #cat: the direction at
	 * which the maximum power occured, and a #cat: normalized value for the maximum
	 * power. In addition, the #cat: statistics are ranked in descending order based
	 * on normalized #cat: squared maximum power. These statistics are fundamental
	 * #cat: to selecting a dominant direction flow for the current #cat: input
	 * image block. Input: powers - DFT power vectors (N Waves X M Directions)
	 * computed for the current image block from which the values in the statistics
	 * arrays are derived fw - the beginning of the range of wave form indices from
	 * which the statistcs are to derived tw - the ending of the range of wave form
	 * indices from which the statistcs are to derived (last index is tw-1) nDirs -
	 * number of orientations (directions) at which the DFT analysis was conducted
	 * Output: ret - Zero - successful completion - Negative - system error powMaxs
	 * - array holding the maximum DFT power for each wave form (other than the
	 * lowest frequecy) powmaxDirs - array to holding the direction corresponding to
	 * each maximum power value in powMaxs powNorms - array to holding the
	 * normalized maximum powers corresponding to each value in powMaxs Return Code:
	 * wis - list of ranked wave form indicies of the corresponding statistics based
	 * on normalized squared maximum power. These indices will be used as indirect
	 * addresses when processing the power statistics in descending order of
	 * "dominance"
	 **************************************************************************/
	public int getDftPowerStats(AtomicIntegerArray wis, AtomicReferenceArray<Double> powMaxs,
			AtomicIntegerArray powmaxDirs, AtomicReferenceArray<Double> powNorms, AtomicReferenceArray<Double[]> powers,
			final int fw, final int tw, final int nDirs) {
		int ret;

		for (int waveIndex = fw, index = 0; waveIndex < tw; waveIndex++, index++) {
			AtomicReference<Double> refpowmaxs = new AtomicReference<>(powMaxs.get(index));
			AtomicInteger refpowmaxdirs = new AtomicInteger(powmaxDirs.get(index));
			AtomicReference<Double> refpownorms = new AtomicReference<>(powNorms.get(index));
			AtomicReferenceArray<Double> refpowers = new AtomicReferenceArray<>(powers.get(waveIndex));

			getMaxNorm(refpowmaxs, refpowmaxdirs, refpownorms, refpowers, nDirs);

			powMaxs.set(index, refpowmaxs.get());
			powmaxDirs.set(index, refpowmaxdirs.get());
			powNorms.set(index, refpownorms.get());
		}

		/* Get sorted order of applied DFT waves based on normalized power */
		ret = sortDftWaves(wis, powMaxs, powNorms, tw - fw);
		if (ret != ILfs.FALSE) {
			return ret;
		}
		ret = ILfs.FALSE;
		return ret;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getMaxNorm - Analyses a DFT power vector for a specific wave form #cat:
	 * applied at different orientations (directions) to the #cat: current image
	 * block. The routine retuns the maximum #cat: power value in the vector, the
	 * direction at which the #cat: maximum occurs, and a normalized power value.
	 * The #cat: normalized power is computed as the maximum power divided #cat: by
	 * the average power across all the directions. These #cat: simple statistics
	 * are fundamental to the selection of #cat: a dominant direction flow for the
	 * image block. Input: oPowerVector - the DFT power values derived form a
	 * specific wave form applied at different directions nDirs - the number of
	 * directions to which the wave form was applied Output: powmax - the maximum
	 * power value in the DFT power vector powmaxDir - the direciton at which the
	 * maximum power value occured pownorm - the normalized power corresponding to
	 * the maximum power
	 **************************************************************************/
	public void getMaxNorm(AtomicReference<Double> powmax, AtomicInteger powmaxDir, AtomicReference<Double> pownorm,
			final AtomicReferenceArray<Double> oPowerVector, final int nDirs) {
		int nDir;
		double maxValue;
		double powSum;
		int maxIndex;
		double powMean;

		/* Find max power value and store corresponding direction */
		maxValue = oPowerVector.get(0);
		maxIndex = 0;

		/* Sum the total power in a block at a given direction */
		powSum = oPowerVector.get(0);

		/* For each direction ... */
		for (nDir = 1; nDir < nDirs; nDir++) {
			powSum += oPowerVector.get(nDir);
			if (oPowerVector.get(nDir) > maxValue) {
				maxValue = oPowerVector.get(nDir);
				maxIndex = nDir;
			}
		}

		powmax.set(maxValue);
		powmaxDir.set(maxIndex);

		/* Powmean is used as denominator for pownorm, so setting */
		/* a non-zero minimum avoids possible division by zero. */
		powMean = Math.max(powSum, ILfs.MIN_POWER_SUM) / nDirs;

		pownorm.set(powmax.get() / powMean);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortDftWaves - Creates a ranked list of DFT wave form statistics #cat:
	 * by sorting on the normalized squared maximum power. Input: powMaxs - maximum
	 * DFT power for each wave form used to derive statistics powNorms - normalized
	 * maximum power corresponding to values in powMaxs nStats - number of wave
	 * forms used to derive statistics (N Wave - 1) Output: wis - sorted list of
	 * indices corresponding to the ranked set of wave form statistics. These
	 * indices will be used as indirect addresses when processing the power
	 * statistics in descending order of "dominance" Return Code: ret - Zero -
	 * successful completion - Negative - system error
	 **************************************************************************/
	@SuppressWarnings({ "java:S3516" })
	public int sortDftWaves(AtomicIntegerArray wis, final AtomicReferenceArray<Double> powMaxs,
			final AtomicReferenceArray<Double> powNorms, final int nStats) {
		int i;
		int ret;
		AtomicReferenceArray<Double> powNorms2 = new AtomicReferenceArray<>(nStats);
		for (i = 0; i < nStats; i++) {
			/* Wis will hold the sorted statistic indices when all is done. */
			wis.set(i, i);
			/* This is normalized squared max power. */
			powNorms2.set(i, powMaxs.get(i) * powNorms.get(i));
		}

		/* Sort the statistic indices on the normalized squared power. */
		getSort().bubbleSortDoubleArrayDecremental2(powNorms2, wis, nStats);

		/* Deallocate the working memory. */
		getFree().free(powNorms2);
		ret = ILfs.FALSE;
		return ret;
	}
}