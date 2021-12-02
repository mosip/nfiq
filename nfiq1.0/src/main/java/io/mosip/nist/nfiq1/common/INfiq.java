package io.mosip.nist.nfiq1.common;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.mosip.nist.nfiq1.common.ILfs.Minutiae;
import io.mosip.nist.nfiq1.mindtct.Quality;

public interface INfiq {
	public static final int DEFAULT_PPI = 500;

	public static final int NFIQ_VCTRLEN = 11;
	public static final int NFIQ_NUM_CLASSES = 5;
	public static final int EMPTY_IMG = 1;
	public static final int EMPTY_IMG_QUAL = 5;
	public static final int TOO_FEW_MINUTIAE = 2;
	public static final int MIN_MINUTIAE = 5;
	public static final int MIN_MINUTIAE_QUAL = 5;

	/***********************************************************************/
	/* NFIQ1.java : NFIQ supporting routines */
	public interface INfiq1Helper {
		public int computeNfiqFeatureVector(double[] featvctr, int vctrlen, 
			AtomicReference<Minutiae> minutiae, Quality qualityMap, int map_w, int map_h);
		public int computeNfiq(AtomicInteger onfiq, AtomicReference<Double> oconf, int [] idata, 
			final int iw, final int ih, final int id, final int ippi, int logflag);
		public int computeNfiqFlex(AtomicInteger onfiq, AtomicReference<Double> oconf, int [] idata, 
			final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI,
			double[] znorm_means, double[] znorm_stds, 
			int nInps, int nHids, int nOuts, final int acfunc_hids, final int acfunc_outs,
			double[] wts);
	}	
	/***********************************************************************/
	/* IZNormalization.java : Routines supporting Z-Normalization */
	public interface INfiq1ZNormalization {
		public void ZNormalizeFeatureVector(double[] featvctr, double[] znorm_means, double[] znorm_stds, final int vctrlen);
		public int computeZNormStats(List<List<Double>> omeans, List<List<Double>> ostddevs, 
			List<List<Double>> feats, final int nfeatvctrs, final int nfeats);
	}

	/***********************************************************************/
	/* NfiqGlobals.java : Global variables supporting NFIQ */
	public interface INfiq1Globals {
		public float dfltZnormMeans[] = null;
		public float dfltZnormStds[] = null;
		public char  dfltPurpose = '\0';
		public int   dfltNInps = -1;
		public int   dfltNHids = -1;
		public int   dfltNOuts = -1;
		public char  dfltAcFuncHids = '\0';
		public char  dfltAcFuncOuts = '\0';
		public float dfltWts[] = null;		
	}
}


