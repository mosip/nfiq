package org.mosip.nist.nfiq1.mindtct;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.FeaturePattern;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;

/**
 * Global tables and default parameter sets used throughout MINDTCT.
 * <p>
 * Port of NIST LFS {@code globals.c}. Holds the DFT wave coefficients, the
 * version 1 and version 2 {@link LfsParams} control structures, the
 * 8-connected neighbour offset tables, the chain-code lookup matrix, and the
 * feature pixel-pair patterns used to detect ridge endings and bifurcations.
 * <p>
 * Implemented as a lazily created singleton. {@link #getInstance()} is
 * synchronized, but the returned arrays and parameter objects are mutable and
 * shared; callers must treat them as read-only (or synchronize externally) when
 * used from multiple threads.
 */
public class Globals extends MindTct {
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Globals instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Globals() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Globals} instance
	 */
	public static synchronized Globals getInstance() {
		if (instance == null) {
			instance = new Globals();
		}
		return instance;
	}

	/**
	 * Constants (C) defining the {@link ILfs#NUM_DFT_WAVES} (4) DFT frequencies,
	 * where frequency is defined as C*(PI_FACTOR). PI_FACTOR regulates the period
	 * of the function in x, so: 1 = one period in range X; 2 = twice the
	 * frequency in range X; 3 = three times the frequency in range X; 4 = four
	 * times the frequency in range X. NIST: {@code dft_coefs[]} in
	 * {@code globals.c}.
	 */
	private double[] dftCoefs = { 1, 2, 3, 4 };// ILfs.NUM_DFT_WAVES
	/**
	 * Global (version 1) LFS parameters structure, initialized with the default
	 * NIST control values (image, map, DFT, binarization, minutiae detection,
	 * link, false-minutiae removal and ridge counting controls). NIST:
	 * {@code lfsparms} in {@code globals.c}.
	 */
	private LfsParams lfsParams = new LfsParams(ILfs.PAD_VALUE, ILfs.JOIN_LINE_RADIUS, ILfs.IMAP_BLOCKSIZE,
			ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.NUM_DIRECTIONS, ILfs.START_DIR_ANGLE, ILfs.RMV_VALID_NBR_MIN,
			ILfs.DIR_STRENGTH_MIN, ILfs.DIR_DISTANCE_MAX, ILfs.SMTH_VALID_NBR_MIN, ILfs.VORT_VALID_NBR_MIN,
			ILfs.HIGHCURV_VORTICITY_MIN, ILfs.HIGHCURV_CURVATURE_MIN, ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_INT,
			ILfs.NUM_DFT_WAVES, ILfs.POWMAX_MIN, ILfs.POWNORM_MIN, ILfs.POWMAX_MAX, ILfs.FORK_INTERVAL,
			ILfs.FORK_PCT_POWMAX, ILfs.FORK_PCT_POWNORM, ILfs.DIRBIN_GRID_W, ILfs.DIRBIN_GRID_H, ILfs.ISOBIN_GRID_DIM,
			ILfs.NUM_FILL_HOLES, ILfs.MAX_MINUTIA_DELTA, ILfs.MAX_HIGH_CURVE_THETA, ILfs.HIGH_CURVE_HALF_CONTOUR,
			ILfs.MIN_LOOP_LEN, ILfs.MIN_LOOP_ASPECT_DIST, ILfs.MIN_LOOP_ASPECT_RATIO, ILfs.LINK_TABLE_DIM,
			ILfs.MAX_LINK_DIST, ILfs.MIN_THETA_DIST, ILfs.MAXTRANS, ILfs.SCORE_THETA_NORM, ILfs.SCORE_DIST_NORM,
			ILfs.SCORE_DIST_WEIGHT, ILfs.SCORE_NUMERATOR, ILfs.MAX_RMTEST_DIST, ILfs.MAX_HOOK_LEN, ILfs.MAX_HALF_LOOP,
			ILfs.TRANS_DIR_PIX, ILfs.SMALL_LOOP_LEN, ILfs.SIDE_HALF_CONTOUR, ILfs.INV_BLOCK_MARGIN,
			ILfs.RM_VALID_NBR_MIN, ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_DBL,
			ILfs.UNUSED_INT, ILfs.PORES_TRANS_R, ILfs.PORES_PERP_STEPS, ILfs.PORES_STEPS_FWD, ILfs.PORES_STEPS_BWD,
			ILfs.PORES_MIN_DIST2, ILfs.PORES_MAX_RATIO, ILfs.MAX_NBRS, ILfs.MAX_RIDGE_STEPS);

	/**
	 * VERSION 2 global LFS parameters structure (the set used by MINDTCT/NFIQ),
	 * grouped into image, map, DFT, binarization, minutiae detection, minutiae
	 * link, false-minutiae removal and ridge counting controls. Unused slots are
	 * filled with {@link ILfs#UNUSED_INT} / {@link ILfs#UNUSED_DBL}. NIST:
	 * {@code lfsparms_V2} in {@code globals.c}.
	 */
	private LfsParams lfsParamsV2 = new LfsParams(
			/* Image Controls */
			ILfs.PAD_VALUE, ILfs.JOIN_LINE_RADIUS,

			/* Map Controls */
			ILfs.MAP_BLOCKSIZE_V2, ILfs.MAP_WINDOWSIZE_V2, ILfs.MAP_WINDOWOFFSET_V2, ILfs.NUM_DIRECTIONS,
			ILfs.START_DIR_ANGLE, ILfs.RMV_VALID_NBR_MIN, ILfs.DIR_STRENGTH_MIN, ILfs.DIR_DISTANCE_MAX,
			ILfs.SMTH_VALID_NBR_MIN, ILfs.VORT_VALID_NBR_MIN, ILfs.HIGHCURV_VORTICITY_MIN, ILfs.HIGHCURV_CURVATURE_MIN,
			ILfs.MIN_INTERPOLATE_NBRS, ILfs.PERCENTILE_MIN_MAX, ILfs.MIN_CONTRAST_DELTA,

			/* DFT Controls */
			ILfs.NUM_DFT_WAVES, ILfs.POWMAX_MIN, ILfs.POWNORM_MIN, ILfs.POWMAX_MAX, ILfs.FORK_INTERVAL,
			ILfs.FORK_PCT_POWMAX, ILfs.FORK_PCT_POWNORM,

			/* Binarization Controls */
			ILfs.DIRBIN_GRID_W, ILfs.DIRBIN_GRID_H, ILfs.UNUSED_INT, /* isobin_grid_dim */
			ILfs.NUM_FILL_HOLES,

			/* Minutiae Detection Controls */
			ILfs.MAX_MINUTIA_DELTA, ILfs.MAX_HIGH_CURVE_THETA, ILfs.HIGH_CURVE_HALF_CONTOUR, ILfs.MIN_LOOP_LEN,
			ILfs.MIN_LOOP_ASPECT_DIST, ILfs.MIN_LOOP_ASPECT_RATIO,

			/* Minutiae Link Controls */
			ILfs.UNUSED_INT, /* link_table_dim */
			ILfs.UNUSED_INT, /* max_link_dist */
			ILfs.UNUSED_INT, /* min_theta_dist */
			ILfs.MAXTRANS, /* used for removing overlaps as well */
			ILfs.UNUSED_DBL, /* score_theta_norm */
			ILfs.UNUSED_DBL, /* score_dist_norm */
			ILfs.UNUSED_DBL, /* score_dist_weight */
			ILfs.UNUSED_DBL, /* score_numerator */

			/* False Minutiae Removal Controls */
			ILfs.MAX_RMTEST_DIST_V2, ILfs.MAX_HOOK_LEN_V2, ILfs.MAX_HALF_LOOP_V2, ILfs.TRANS_DIR_PIX_V2,
			ILfs.SMALL_LOOP_LEN, ILfs.SIDE_HALF_CONTOUR, ILfs.INV_BLOCK_MARGIN_V2, ILfs.RM_VALID_NBR_MIN,
			ILfs.MAX_OVERLAP_DIST, ILfs.MAX_OVERLAP_JOIN_DIST, ILfs.MALFORMATION_STEPS_1, ILfs.MALFORMATION_STEPS_2,
			ILfs.MIN_MALFORMATION_RATIO, ILfs.MAX_MALFORMATION_DIST, ILfs.PORES_TRANS_R, ILfs.PORES_PERP_STEPS,
			ILfs.PORES_STEPS_FWD, ILfs.PORES_STEPS_BWD, ILfs.PORES_MIN_DIST2, ILfs.PORES_MAX_RATIO,

			/* Ridge Counting Controls */
			ILfs.MAX_NBRS, ILfs.MAX_RIDGE_STEPS);

	/**
	 * X offsets for conducting 8-connected neighbour analyses, indexed by
	 * neighbour number 0..7 laid out clockwise around the centre pixel C:
	 * <pre>
	 * 7 0 1
	 * 6 C 2
	 * 5 4 3
	 * </pre>
	 * NIST: {@code nbr8_dx[]} in {@code globals.c}.
	 */
	private int[] nbr8Dx = { 0, 1, 1, 1, 0, -1, -1, -1 }; /* 6 C 2 */
	/**
	 * Y offsets for conducting 8-connected neighbour analyses, indexed by
	 * neighbour number 0..7 (same layout as {@link #nbr8Dx}). NIST:
	 * {@code nbr8_dy[]} in {@code globals.c}.
	 */
	private int[] nbr8Dy = { -1, -1, 0, 1, 1, 1, 0, -1 }; /* 5 4 3 */

	/**
	 * Chain code lookup matrix for 8-connected neighbours: a 3x3 table indexed
	 * by {@code (dy + 1) * 3 + (dx + 1)} that yields the chain code (0..7, where
	 * 0 = east and codes increase counter-clockwise in image coordinates) for a
	 * unit step, or -1 for the centre:
	 * <pre>
	 * 3 2 1
	 * 4 C 0
	 * 5 6 7
	 * </pre>
	 * NIST:
	 * {@code chaincodes_nbr8[]} in {@code globals.c}.
	 */
	private int[] chaincodesNbr8 = { 3, 2, 1, 4, -1, 0, 5, 6, 7 };

	/**
	 * Global array of feature pixel-pair patterns (a..j) used when scanning the
	 * binary image for minutiae. Each pattern gives the minutia type (ridge
	 * ending or bifurcation), whether it is appearing or disappearing, and the
	 * first, middle and last pixel pairs to match. NIST:
	 * {@code feature_patterns[]} in {@code globals.c}.
	 */
	private FeaturePattern[] featurePatterns = { new FeaturePattern(ILfs.RIDGE_ENDING, /**
																						 * a. Ridge Ending (appearing)
																						 **/
			ILfs.APPEARING, new int[] { 0, 0 }, new int[] { 0, 1 }, new int[] { 0, 0 }),

			new FeaturePattern(ILfs.RIDGE_ENDING, /* b. Ridge Ending (disappearing) */
					ILfs.DISAPPEARING, new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 0 }),

			new FeaturePattern(ILfs.BIFURCATION, /* c. Bifurcation (disappearing) */
					ILfs.DISAPPEARING, new int[] { 1, 1 }, new int[] { 0, 1 }, new int[] { 1, 1 }),

			new FeaturePattern(ILfs.BIFURCATION, /* d. Bifurcation (appearing) */
					ILfs.APPEARING, new int[] { 1, 1 }, new int[] { 1, 0 }, new int[] { 1, 1 }),

			new FeaturePattern(ILfs.BIFURCATION, /* e. Bifurcation (disappearing) */
					ILfs.DISAPPEARING, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { 1, 1 }),

			new FeaturePattern(ILfs.BIFURCATION, /* f. Bifurcation (disappearing) */
					ILfs.DISAPPEARING, new int[] { 1, 1 }, new int[] { 0, 1 }, new int[] { 1, 0 }),

			new FeaturePattern(ILfs.BIFURCATION, /* g. Bifurcation (appearing) */
					ILfs.APPEARING, new int[] { 1, 1 }, new int[] { 1, 0 }, new int[] { 0, 1 }),

			new FeaturePattern(ILfs.BIFURCATION, /* h. Bifurcation (appearing) */
					ILfs.APPEARING, new int[] { 0, 1 }, new int[] { 1, 0 }, new int[] { 1, 1 }),

			new FeaturePattern(ILfs.BIFURCATION, /* i. Bifurcation (disappearing) */
					ILfs.DISAPPEARING, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { 1, 0 }),

			new FeaturePattern(ILfs.BIFURCATION, /* j. Bifurcation (appearing) */
					ILfs.APPEARING, new int[] { 0, 1 }, new int[] { 1, 0 }, new int[] { 0, 1 }) };

	/**
	 * Returns the DFT wave frequency coefficients.
	 *
	 * @return the DFT coefficients array (shared, not copied)
	 */
	public double[] getDftCoefs() {
		return dftCoefs;
	}

	/**
	 * Replaces the DFT wave frequency coefficients.
	 *
	 * @param dftCoefs new DFT coefficients array (stored by reference)
	 */
	public void setDftCoefs(double[] dftCoefs) {
		this.dftCoefs = dftCoefs;
	}

	/**
	 * Returns the version 1 global LFS parameters.
	 *
	 * @return the version 1 {@link LfsParams} (shared, not copied)
	 */
	public LfsParams getLfsParams() {
		return lfsParams;
	}

	/**
	 * Replaces the version 1 global LFS parameters.
	 *
	 * @param lfsParams new version 1 parameter set (stored by reference)
	 */
	public void setLfsParams(LfsParams lfsParams) {
		this.lfsParams = lfsParams;
	}

	/**
	 * Returns the version 2 global LFS parameters used by MINDTCT.
	 *
	 * @return the version 2 {@link LfsParams} (shared, not copied)
	 */
	public LfsParams getLfsParamsV2() {
		return lfsParamsV2;
	}

	/**
	 * Replaces the version 2 global LFS parameters.
	 *
	 * @param lfsParamsV2 new version 2 parameter set (stored by reference)
	 */
	public void setLfsParamsV2(LfsParams lfsParamsV2) {
		this.lfsParamsV2 = lfsParamsV2;
	}

	/**
	 * Returns the 8-connected neighbour X offsets.
	 *
	 * @return the neighbour X offset array (shared, not copied)
	 */
	public int[] getNbr8Dx() {
		return nbr8Dx;
	}

	/**
	 * Replaces the 8-connected neighbour X offsets.
	 *
	 * @param nbr8Dx new neighbour X offset array (stored by reference)
	 */
	public void setNbr8Dx(int[] nbr8Dx) {
		this.nbr8Dx = nbr8Dx;
	}

	/**
	 * Returns the 8-connected neighbour Y offsets.
	 *
	 * @return the neighbour Y offset array (shared, not copied)
	 */
	public int[] getNbr8Dy() {
		return nbr8Dy;
	}

	/**
	 * Replaces the 8-connected neighbour Y offsets.
	 *
	 * @param nbr8Dy new neighbour Y offset array (stored by reference)
	 */
	public void setNbr8Dy(int[] nbr8Dy) {
		this.nbr8Dy = nbr8Dy;
	}

	/**
	 * Returns the 8-connected chain code lookup matrix.
	 *
	 * @return the chain code lookup array (shared, not copied)
	 */
	public int[] getChaincodesNbr8() {
		return chaincodesNbr8;
	}

	/**
	 * Replaces the 8-connected chain code lookup matrix.
	 *
	 * @param chaincodesNbr8 new chain code lookup array (stored by reference)
	 */
	public void setChaincodesNbr8(int[] chaincodesNbr8) {
		this.chaincodesNbr8 = chaincodesNbr8;
	}

	/**
	 * Returns the feature pixel-pair patterns used for minutia detection.
	 *
	 * @return the feature pattern array (shared, not copied)
	 */
	public FeaturePattern[] getFeaturePatterns() {
		return featurePatterns;
	}

	/**
	 * Replaces the feature pixel-pair patterns used for minutia detection.
	 *
	 * @param featurePatterns new feature pattern array (stored by reference)
	 */
	public void setFeaturePatterns(FeaturePattern[] featurePatterns) {
		this.featurePatterns = featurePatterns;
	}
}