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

/**
 * Discrete Fourier Transform (DFT) analysis of image blocks, used to estimate the dominant ridge-flow
 * direction of each block in MINDTCT's direction map.
 * <p>
 * Port of NIST LFS {@code dft.c} ({@code dft_dir_powers}, {@code sum_rot_block_rows}, {@code dft_power},
 * {@code dft_power_stats}, {@code get_max_norm}, {@code sort_dft_waves}). Each block is sampled along rotated
 * pixel rows for every direction, the row sums are correlated with several sine/cosine wave forms, and the
 * resulting power statistics are ranked so the map-building code can choose a dominant direction.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state. Many
 * of the accessor methods below are not used by this class; they give access to the shared MINDTCT helper
 * singletons.
 */
public class Dft extends MindTct implements IDft {
	/** SLF4J logger for input errors. */
	private static final Logger logger = LoggerFactory.getLogger(Dft.class);

	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Dft instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Dft() {
		super();
	}

	/**
	 * Returns the shared {@code Dft} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Dft getInstance() {
		if (instance == null) {
			instance = new Dft();
		}
		return instance;
	}

	/**
	 * Returns the shared numeric-definitions helper (rounding and precision truncation).
	 *
	 * @return the {@link Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the MINDTCT image utility helper.
	 *
	 * @return the {@link ImageUtil} singleton (MINDTCT package version)
	 */
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/**
	 * Returns the MINDTCT global lookup tables.
	 *
	 * @return the {@link Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the general LFS utility helper.
	 *
	 * @return the {@link LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Returns the memory-release helper.
	 *
	 * @return the {@link Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the LFS initialization helper.
	 *
	 * @return the {@link Init} singleton
	 */
	public Init getInit() {
		return Init.getInstance();
	}

	/**
	 * Returns the directional binarization helper.
	 *
	 * @return the {@link Binarization} singleton
	 */
	public Binarization getBinarization() {
		return Binarization.getInstance();
	}

	/**
	 * Returns the minutia allocation and detection helper.
	 *
	 * @return the {@link MinutiaHelper} singleton
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the sorting helper, used to rank DFT wave statistics.
	 *
	 * @return the {@link Sort} singleton
	 */
	public Sort getSort() {
		return Sort.getInstance();
	}

	/**
	 * Returns the LFS minutiae detection driver.
	 *
	 * @return the {@link Detect} singleton
	 */
	public Detect getDetect() {
		return Detect.getInstance();
	}

	/**
	 * Returns the false-minutia removal helper.
	 *
	 * @return the {@link RemoveMinutia} singleton
	 */
	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}

	/**
	 * Returns the neighbour ridge counting helper.
	 *
	 * @return the {@link Ridges} singleton
	 */
	public Ridges getRidges() {
		return Ridges.getInstance();
	}

	/**
	 * Returns the line-tracing helper.
	 *
	 * @return the {@link Line} singleton
	 */
	public Line getLine() {
		return Line.getInstance();
	}

	/**
	 * Returns the contour-tracing helper.
	 *
	 * @return the {@link Contour} singleton
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the loop (lake/island) processing helper.
	 *
	 * @return the {@link Loop} singleton
	 */
	public Loop getLoop() {
		return Loop.getInstance();
	}

	/**
	 * Runs the DFT analysis on one block of image data (NIST {@code dft_dir_powers}).
	 * <p>
	 * The block is sampled across a range of orientations (directions), and several wave forms of different
	 * frequencies are applied at each orientation. At each orientation, pixels are summed along each rotated
	 * pixel row to form a vector of row sums ({@link #sumRotBlockRows}). Each DFT wave form is then applied to
	 * that vector separately ({@link #computeDftPower}), giving one power value per wave form (frequency) per
	 * orientation. The resulting power vectors therefore have dimension (N waves x M directions). These power
	 * signatures are used to determine the dominant ridge-flow direction within the block.
	 *
	 * @param powers            input/output: DFT power vectors indexed {@code [wave][direction]}, pre-allocated by
	 *                          the caller and filled in place with the power of each wave form at each direction
	 * @param paddedImagedata   the padded input image. It must be padded properly, or sampling at some block
	 *                          orientations may read out-of-range indexes.
	 * @param blockOffset       pixel offset from the origin of the padded image to the origin of the current block
	 * @param paddedImageWidth  width (in pixels) of the padded input image
	 * @param paddedImageHeight height (in pixels) of the padded input image
	 * @param dftWaves          structure containing the DFT wave forms
	 * @param dftGrids          structure containing the rotated pixel grid offsets (must be square)
	 * @return 0 ({@link ILfs#FALSE}) on success; -90 if the DFT grids are not square; {@link ILfs#ERROR_CODE_91}
	 *         if the row-sum buffer cannot be allocated
	 */
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

	/**
	 * Computes a vector of pixel row sums by sampling the current image block at one orientation (NIST
	 * {@code sum_rot_block_rows}).
	 * <p>
	 * The sampling uses a precomputed set of rotated pixel offsets (a "grid") relative to the origin of the
	 * image block.
	 *
	 * @param rowSums              output: receives one sum per rotated row; must hold at least
	 *                             {@code blockOffsetSize} elements
	 * @param paddedImagedata      the padded image containing the current block
	 * @param paddedImageDataIndex pixel index of the origin of the current image block
	 * @param gridOffsets          rotated pixel offsets for a block-sized grid rotated to one orientation
	 * @param blockOffsetSize      width and height of the image block, and so the size of the rotated grid
	 */
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

	/**
	 * Computes the DFT power of one wave form applied to a vector of row sums from one block orientation (NIST
	 * {@code dft_power}).
	 * <p>
	 * power = (sum of rowSums[i] * cos[i])^2 + (sum of rowSums[i] * sin[i])^2.
	 *
	 * @param oPower   output: the DFT power for this wave form at this orientation within the block
	 * @param rowSums  summed rows of pixels from a rotated grid laid over the image block
	 * @param dftWave  the wave form (cosine and sine components) at one frequency
	 * @param waveLen  length of the wave form; must equal the block height, which is the length of
	 *                 {@code rowSums}
	 */
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

	/**
	 * Derives ranked statistics from a set of DFT power vectors (NIST {@code dft_power_stats}).
	 * <p>
	 * For each wave form in {@code [fw, tw)} (normally every wave except the lowest frequency), computes the
	 * maximum power, the direction where it occurs, and a normalized maximum power ({@link #getMaxNorm}). The
	 * statistics are then ranked in descending order of normalized squared maximum power
	 * ({@link #sortDftWaves}). These statistics are central to choosing the dominant ridge-flow direction of the
	 * block.
	 *
	 * @param wis        output: wave form indexes of the statistics, ranked by normalized squared maximum
	 *                   power; used as indirect addresses when processing the statistics in descending order of
	 *                   "dominance"
	 * @param powMaxs    output: maximum DFT power for each wave form in the range
	 * @param powmaxDirs output: direction at which each maximum in {@code powMaxs} occurs
	 * @param powNorms   output: normalized maximum power for each value in {@code powMaxs}
	 * @param powers     DFT power vectors (N waves x M directions) computed for the current block
	 * @param fw         first wave form index of the range (inclusive)
	 * @param tw         end wave form index of the range (exclusive; the last index used is {@code tw - 1})
	 * @param nDirs      number of orientations (directions) the DFT analysis was run at
	 * @return 0 ({@link ILfs#FALSE}) on success; negative on system error
	 */
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

	/**
	 * Analyses the DFT power vector of one wave form applied at different orientations to the current block
	 * (NIST {@code get_max_norm}).
	 * <p>
	 * Returns the maximum power in the vector, the direction where it occurs, and a normalized power: the
	 * maximum power divided by the average power across all directions. The power sum is clamped to at least
	 * {@link ILfs#MIN_POWER_SUM} to avoid dividing by zero. These simple statistics are central to choosing
	 * the block's dominant ridge-flow direction.
	 *
	 * @param powmax       output: the maximum value in the DFT power vector
	 * @param powmaxDir    output: the direction at which the maximum occurs
	 * @param pownorm      output: the normalized power corresponding to the maximum
	 * @param oPowerVector the DFT power values of one wave form at the different directions
	 * @param nDirs        number of directions the wave form was applied at
	 */
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

	/**
	 * Ranks DFT wave form statistics by normalized squared maximum power, {@code powMaxs[i] * powNorms[i]}
	 * (NIST {@code sort_dft_waves}).
	 * <p>
	 * Sorts in descending order with {@code Sort.bubbleSortDoubleArrayDecremental2}.
	 *
	 * @param wis      output: sorted indexes of the ranked wave form statistics; used as indirect addresses when
	 *                 processing the power statistics in descending order of "dominance"
	 * @param powMaxs  maximum DFT power for each wave form used to derive the statistics
	 * @param powNorms normalized maximum power for each value in {@code powMaxs}
	 * @param nStats   number of wave forms used to derive the statistics (N waves - 1)
	 * @return always 0 ({@link ILfs#FALSE}), meaning success
	 */
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