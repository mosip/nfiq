package org.mosip.nist.nfiq1.common;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.mindtct.Quality;

/**
 * Constants and contracts for the NFIQ 1.0 layer (port of NIST's {@code nfiq.h}).
 * <p>
 * NFIQ maps a fingerprint image to a quality value from 1 (best) to 5 (worst) by extracting an 11-element
 * feature vector from MINDTCT output and classifying it with a multi-layer perceptron.
 */
public interface INfiq {
	/** Default scan resolution in pixels per inch assumed when the image resolution is unknown. */
	public static final int DEFAULT_PPI = 500;

	/** Length of the NFIQ feature vector (number of MLP inputs). */
	public static final int NFIQ_VCTRLEN = 11;
	/** Number of NFIQ quality classes (number of MLP outputs); NFIQ values range from 1 to 5. */
	public static final int NFIQ_NUM_CLASSES = 5;
	/** Return code signalling that an empty image (no foreground blocks) was detected. */
	public static final int EMPTY_IMG = 1;
	/** NFIQ value assigned to an empty image (5, worst quality). */
	public static final int EMPTY_IMG_QUAL = 5;
	/** Return code signalling that too few minutiae were detected to compute a meaningful NFIQ. */
	public static final int TOO_FEW_MINUTIAE = 2;
	/** Minimum minutiae count; images with this many minutiae or fewer are treated as too few. */
	public static final int MIN_MINUTIAE = 5;
	/** NFIQ value assigned when too few minutiae are detected (5, worst quality). */
	public static final int MIN_MINUTIAE_QUAL = 5;

	/** NFIQ supporting routines (implemented by {@code org.mosip.nist.nfiq1.Nfiq1Helper}). */
	public interface INfiq1Helper {
		/**
		 * Builds the 11-element NFIQ feature vector from MINDTCT minutiae and quality map (NIST
		 * {@code comp_nfiq_featvctr}).
		 *
		 * @param featvctr   output array receiving the feature values (zeroed for an empty image)
		 * @param vctrlen    allocated length of {@code featvctr}
		 * @param minutiae   minutiae detected by MINDTCT
		 * @param qualityMap quality map computed by MINDTCT
		 * @param map_w      width of the quality map in blocks
		 * @param map_h      height of the quality map in blocks
		 * @return {@code 0} on success, or {@link #EMPTY_IMG} if an empty image was detected
		 */
		public int computeNfiqFeatureVector(double[] featvctr, int vctrlen, 
			AtomicReference<Minutiae> minutiae, Quality qualityMap, int map_w, int map_h);
		/**
		 * Computes the NFIQ value of an image using the default normalization statistics and MLP weights (NIST
		 * {@code comp_nfiq}).
		 *
		 * @param onfiq   output receiving the NFIQ value, 1 (best) to 5 (worst)
		 * @param oconf   output receiving the confidence (maximum MLP output activation)
		 * @param idata   grayscale image data, one pixel per element, row-major
		 * @param iw      image width in pixels
		 * @param ih      image height in pixels
		 * @param id      image depth in bits (should be 8)
		 * @param ippi    scan resolution in pixels per inch, or {@code -1} if unknown (500 ppi assumed)
		 * @param logflag {@code 1} to enable detailed logging
		 * @return {@code 0} on success, {@link #EMPTY_IMG}, {@link #TOO_FEW_MINUTIAE}, or a negative system error code
		 */
		public int computeNfiq(AtomicInteger onfiq, AtomicReference<Double> oconf, int [] idata, 
			final int iw, final int ih, final int id, final int ippi, int logflag);
		/**
		 * Computes the NFIQ value of an image using caller-supplied normalization statistics and MLP weights (NIST
		 * {@code comp_nfiq_flex}).
		 *
		 * @param onfiq       output receiving the NFIQ value, 1 (best) to 5 (worst)
		 * @param oconf       output receiving the confidence (maximum MLP output activation)
		 * @param idata       grayscale image data, one pixel per element, row-major
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @param imageDepth  image depth in bits (should be 8)
		 * @param imagePPI    scan resolution in pixels per inch, or {@code -1} if unknown (500 ppi assumed)
		 * @param znorm_means global mean of each feature coefficient for Z-normalization
		 * @param znorm_stds  global standard deviation of each feature coefficient for Z-normalization
		 * @param nInps       number of MLP inputs (feature vector length)
		 * @param nHids       number of MLP hidden nodes
		 * @param nOuts       number of MLP outputs (NFIQ classes)
		 * @param acfunc_hids hidden-layer activation function code
		 * @param acfunc_outs output-layer activation function code
		 * @param wts         MLP weights
		 * @return {@code 0} on success, {@link #EMPTY_IMG}, {@link #TOO_FEW_MINUTIAE}, or a non-zero/negative error
		 *         code
		 */
		public int computeNfiqFlex(AtomicInteger onfiq, AtomicReference<Double> oconf, int [] idata, 
			final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI,
			double[] znorm_means, double[] znorm_stds, 
			int nInps, int nHids, int nOuts, final int acfunc_hids, final int acfunc_outs,
			double[] wts);
	}	
	/**
	 * Routines supporting Z-normalization of NFIQ feature vectors (implemented by
	 * {@code org.mosip.nist.nfiq1.Nfiq1ZNormalization}).
	 */
	public interface INfiq1ZNormalization {
		/**
		 * Z-normalizes a feature vector in place: {@code v[i] = (v[i] - mean[i]) / stddev[i]}.
		 *
		 * @param featvctr    feature vector; overwritten with normalized values
		 * @param znorm_means global mean of each coefficient
		 * @param znorm_stds  global standard deviation of each coefficient
		 * @param vctrlen     number of coefficients to normalize
		 */
		public void ZNormalizeFeatureVector(double[] featvctr, double[] znorm_means, double[] znorm_stds, final int vctrlen);
		/**
		 * Computes per-coefficient mean and standard deviation over a set of feature vectors (NIST
		 * {@code comp_znorm_stats}).
		 *
		 * @param omeans    output; element 0 receives the list of coefficient means
		 * @param ostddevs  output; element 0 receives the list of coefficient standard deviations
		 * @param feats     feature matrix, one inner list per coefficient
		 * @param nfeatvctrs number of feature vectors
		 * @param nfeats    number of coefficients per feature vector
		 * @return {@code 0} on success, negative on system error
		 */
		public int computeZNormStats(List<List<Double>> omeans, List<List<Double>> ostddevs, 
			List<List<Double>> feats, final int nfeatvctrs, final int nfeats);
	}

	/**
	 * Global variables supporting NFIQ (NIST {@code nfiqgbls.c}); implemented by
	 * {@code org.mosip.nist.nfiq1.Nfiq1Globals}.
	 * <p>
	 * The constants declared here are placeholders only (null/sentinel values); the real defaults live in the
	 * implementing class's instance fields.
	 */
	public interface INfiq1Globals {
		/** Placeholder for the default Z-normalization means (always {@code null}). */
		public float dfltZnormMeans[] = null;
		/** Placeholder for the default Z-normalization standard deviations (always {@code null}). */
		public float dfltZnormStds[] = null;
		/** Placeholder for the default MLP purpose (NUL character). */
		public char  dfltPurpose = '\0';
		/** Placeholder for the default number of MLP inputs ({@code -1}). */
		public int   dfltNInps = -1;
		/** Placeholder for the default number of MLP hidden nodes ({@code -1}). */
		public int   dfltNHids = -1;
		/** Placeholder for the default number of MLP outputs ({@code -1}). */
		public int   dfltNOuts = -1;
		/** Placeholder for the default hidden-layer activation function code (NUL character). */
		public char  dfltAcFuncHids = '\0';
		/** Placeholder for the default output-layer activation function code (NUL character). */
		public char  dfltAcFuncOuts = '\0';
		/** Placeholder for the default MLP weights (always {@code null}). */
		public float dfltWts[] = null;		
	}
}


