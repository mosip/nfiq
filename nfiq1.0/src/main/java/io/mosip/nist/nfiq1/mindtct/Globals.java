package io.mosip.nist.nfiq1.mindtct;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.FeaturePattern;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;

public class Globals extends MindTct {
	private static volatile Globals instance;
    public static Globals getInstance() {
        if (instance == null) {
            synchronized (Globals.class) {
                if (instance == null) {
                    instance = new Globals();
                }
            }
        }
        return instance;
    }    
    private Globals()
    {
    	super();
    }
    
	/* Constants (C) for defining 4 DFT frequencies, where  */
	/* frequency is defined as C*(PI_FACTOR).  PI_FACTOR    */
	/* regulates the period of the function in x, so:       */
	/*      1 = one period in range X.                      */
	/*      2 = twice the frequency in range X.             */
	/*      3 = three times the frequency in reange X.      */
	/*      4 = four times the frequency in ranage X.       */
	
	private double dftCoefs[] = { 1,2,3,4 };//ILfs.NUM_DFT_WAVES
	/* Allocate and initialize a global LFS parameters structure. */
	private LfsParams lfsParams = new LfsParams(
			ILfs.PAD_VALUE, ILfs.JOIN_LINE_RADIUS, ILfs.IMAP_BLOCKSIZE, ILfs.UNUSED_INT, 
			ILfs.UNUSED_INT, ILfs.NUM_DIRECTIONS, ILfs.START_DIR_ANGLE, ILfs.RMV_VALID_NBR_MIN, 
			ILfs.DIR_STRENGTH_MIN, ILfs.DIR_DISTANCE_MAX, ILfs.SMTH_VALID_NBR_MIN, ILfs.VORT_VALID_NBR_MIN, 
			ILfs.HIGHCURV_VORTICITY_MIN, ILfs.HIGHCURV_CURVATURE_MIN, ILfs.UNUSED_INT, 
			ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.NUM_DFT_WAVES, ILfs.POWMAX_MIN, ILfs.POWNORM_MIN, 
			ILfs.POWMAX_MAX, ILfs.FORK_INTERVAL, ILfs.FORK_PCT_POWMAX, ILfs.FORK_PCT_POWNORM, 
			ILfs.DIRBIN_GRID_W, ILfs.DIRBIN_GRID_H, ILfs.ISOBIN_GRID_DIM, ILfs.NUM_FILL_HOLES, 
			ILfs.MAX_MINUTIA_DELTA, ILfs.MAX_HIGH_CURVE_THETA, ILfs.HIGH_CURVE_HALF_CONTOUR, 
			ILfs.MIN_LOOP_LEN, ILfs.MIN_LOOP_ASPECT_DIST, ILfs.MIN_LOOP_ASPECT_RATIO, ILfs.LINK_TABLE_DIM, 
			ILfs.MAX_LINK_DIST, ILfs.MIN_THETA_DIST, ILfs.MAXTRANS, ILfs.SCORE_THETA_NORM, 
			ILfs.SCORE_DIST_NORM, ILfs.SCORE_DIST_WEIGHT, ILfs.SCORE_NUMERATOR, ILfs.MAX_RMTEST_DIST, 
			ILfs.MAX_HOOK_LEN, ILfs.MAX_HALF_LOOP, ILfs.TRANS_DIR_PIX, ILfs.SMALL_LOOP_LEN, 
			ILfs.SIDE_HALF_CONTOUR, ILfs.INV_BLOCK_MARGIN, ILfs.RM_VALID_NBR_MIN, ILfs.UNUSED_INT, 
			ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_INT, ILfs.UNUSED_DBL, ILfs.UNUSED_INT, 
			ILfs.PORES_TRANS_R, ILfs.PORES_PERP_STEPS, ILfs.PORES_STEPS_FWD, ILfs.PORES_STEPS_BWD, 
			ILfs.PORES_MIN_DIST2, ILfs.PORES_MAX_RATIO, ILfs.MAX_NBRS, ILfs.MAX_RIDGE_STEPS
		);
	
	/* Allocate and initialize VERSION 2 global LFS parameters structure. */
	private LfsParams lfsParamsV2 = new LfsParams(  
		/* Image Controls */
			ILfs.PAD_VALUE, ILfs.JOIN_LINE_RADIUS,

		/* Map Controls */
			ILfs.MAP_BLOCKSIZE_V2, ILfs.MAP_WINDOWSIZE_V2, ILfs.MAP_WINDOWOFFSET_V2,
			ILfs.NUM_DIRECTIONS, ILfs.START_DIR_ANGLE, ILfs.RMV_VALID_NBR_MIN, ILfs.DIR_STRENGTH_MIN,
			ILfs.DIR_DISTANCE_MAX, ILfs.SMTH_VALID_NBR_MIN, ILfs.VORT_VALID_NBR_MIN, ILfs.HIGHCURV_VORTICITY_MIN,
			ILfs.HIGHCURV_CURVATURE_MIN, ILfs.MIN_INTERPOLATE_NBRS, ILfs.PERCENTILE_MIN_MAX, ILfs.MIN_CONTRAST_DELTA,

		/* DFT Controls */
			ILfs.NUM_DFT_WAVES, ILfs.POWMAX_MIN, ILfs.POWNORM_MIN, ILfs.POWMAX_MAX, ILfs.FORK_INTERVAL,
			ILfs.FORK_PCT_POWMAX, ILfs.FORK_PCT_POWNORM,

		/* Binarization Controls */
			ILfs.DIRBIN_GRID_W, ILfs.DIRBIN_GRID_H,
			ILfs.UNUSED_INT,          /* isobin_grid_dim */
			ILfs.NUM_FILL_HOLES,

		/* Minutiae Detection Controls */
			ILfs.MAX_MINUTIA_DELTA, ILfs.MAX_HIGH_CURVE_THETA, ILfs.HIGH_CURVE_HALF_CONTOUR,
			ILfs.MIN_LOOP_LEN, ILfs.MIN_LOOP_ASPECT_DIST, ILfs.MIN_LOOP_ASPECT_RATIO,

		/* Minutiae Link Controls */
			ILfs.UNUSED_INT,          /* link_table_dim     */
			ILfs.UNUSED_INT,          /* max_link_dist      */
			ILfs.UNUSED_INT,          /* min_theta_dist     */
			ILfs.MAXTRANS,            /* used for removing overlaps as well */
			ILfs.UNUSED_DBL,          /* score_theta_norm   */
			ILfs.UNUSED_DBL,          /* score_dist_norm    */
			ILfs.UNUSED_DBL,          /* score_dist_weight  */
			ILfs.UNUSED_DBL,          /* score_numerator    */

		/* False Minutiae Removal Controls */
			ILfs.MAX_RMTEST_DIST_V2, ILfs.MAX_HOOK_LEN_V2, ILfs.MAX_HALF_LOOP_V2, ILfs.TRANS_DIR_PIX_V2,
			ILfs.SMALL_LOOP_LEN, ILfs.SIDE_HALF_CONTOUR, ILfs.INV_BLOCK_MARGIN_V2, ILfs.RM_VALID_NBR_MIN,
			ILfs.MAX_OVERLAP_DIST, ILfs.MAX_OVERLAP_JOIN_DIST, ILfs.MALFORMATION_STEPS_1, ILfs.MALFORMATION_STEPS_2,
			ILfs.MIN_MALFORMATION_RATIO, ILfs.MAX_MALFORMATION_DIST, ILfs.PORES_TRANS_R, ILfs.PORES_PERP_STEPS,
			ILfs.PORES_STEPS_FWD, ILfs.PORES_STEPS_BWD, ILfs.PORES_MIN_DIST2, ILfs.PORES_MAX_RATIO,

		/* Ridge Counting Controls */
			ILfs.MAX_NBRS, ILfs.MAX_RIDGE_STEPS
	);
	
	/* Variables for conducting 8-connected neighbor analyses. */
	/* Pixel neighbor offsets:  0  1  2  3  4  5  6  7  */     /* 7 0 1 */
	private int nbr8Dx[] =          {  0, 1, 1, 1, 0,-1,-1,-1 };      /* 6 C 2 */
	private int nbr8Dy[] =          { -1,-1, 0, 1, 1, 1, 0,-1 };      /* 5 4 3 */

	/* The chain code lookup matrix for 8-connected neighbors. */
	/* Should put this in globals.                             */
	private int chaincodesNbr8[]={ 3, 2, 1,
	                        4,-1, 0,
	                        5, 6, 7};
	
	/* Global array of feature pixel pairs. */
	private FeaturePattern featurePatterns[] = {
				new FeaturePattern (
					ILfs.RIDGE_ENDING,  /** a. Ridge Ending (appearing) **/
					ILfs.APPEARING,
					new int []{0,0},
					new int []{0,1},
					new int []{0,0}
				),

				new FeaturePattern (
					ILfs.RIDGE_ENDING,  /* b. Ridge Ending (disappearing) */
					ILfs.DISAPPEARING,
					new int []{0,0},
					new int []{1,0},
					new int []{0,0}
				),

				new FeaturePattern (
					ILfs.BIFURCATION,   /* c. Bifurcation (disappearing) */
                    ILfs.DISAPPEARING,
                    new int []{1,1},
                    new int []{0,1},
                    new int []{1,1}
				),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* d. Bifurcation (appearing) */
	                ILfs.APPEARING,
	                new int []{1,1},
	                new int []{1,0},
	                new int []{1,1}
				),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* e. Bifurcation (disappearing) */
                    ILfs.DISAPPEARING,
	                new int []{1,0},
	                new int []{0,1},
	                new int []{1,1}
                ),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* f. Bifurcation (disappearing) */
	                ILfs.DISAPPEARING,
	                new int []{1,1},
	                new int []{0,1},
	                new int []{1,0}
                ),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* g. Bifurcation (appearing) */
	                ILfs.APPEARING,
	                new int []{1,1},
	                new int []{1,0},
	                new int []{0,1}
				),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* h. Bifurcation (appearing) */
	                ILfs.APPEARING,
	                new int []{0,1},
	                new int []{1,0},
	                new int []{1,1}
                ),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* i. Bifurcation (disappearing) */
	                ILfs.DISAPPEARING,
	                new int []{1,0},
	                new int []{0,1},
	                new int []{1,0}
                ),

				new FeaturePattern (
                    ILfs.BIFURCATION,   /* j. Bifurcation (appearing) */
	                ILfs.APPEARING,
	                new int []{0,1},
	                new int []{1,0},
	                new int []{0,1}
                )
        	 };

	public double[] getDftCoefs() {
		return dftCoefs;
	}

	public void setDftCoefs(double[] DftCoefs) {
		this.dftCoefs = DftCoefs;
	}

	public LfsParams getLfsParams() {
		return lfsParams;
	}

	public void setLfsParams(LfsParams lfsParams) {
		this.lfsParams = lfsParams;
	}

	public LfsParams getLfsParamsV2() {
		return lfsParamsV2;
	}

	public void setLfsParamsV2(LfsParams lfsParamsV2) {
		this.lfsParamsV2 = lfsParamsV2;
	}

	public int[] getNbr8Dx() {
		return nbr8Dx;
	}

	public void setNbr8Dx(int[] nbr8Dx) {
		this.nbr8Dx = nbr8Dx;
	}

	public int[] getNbr8Dy() {
		return nbr8Dy;
	}

	public void setNbr8Dy(int[] nbr8Dy) {
		this.nbr8Dy = nbr8Dy;
	}

	public int[] getChaincodesNbr8() {
		return chaincodesNbr8;
	}

	public void setChaincodesNbr8(int[] chaincodesNbr8) {
		this.chaincodesNbr8 = chaincodesNbr8;
	}

	public FeaturePattern[] getFeaturePatterns() {
		return featurePatterns;
	}

	public void setFeaturePatterns(FeaturePattern[] featurePatterns) {
		this.featurePatterns = featurePatterns;
	}
	
}