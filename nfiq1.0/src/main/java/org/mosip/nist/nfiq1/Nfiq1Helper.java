package org.mosip.nist.nfiq1;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.IAn2k;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.INfiq;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.common.INfiq.INfiq1Helper;
import org.mosip.nist.nfiq1.mindtct.Maps;
import org.mosip.nist.nfiq1.mindtct.Quality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for computing an NFIQ 1.0 quality score from a grayscale fingerprint image (port of NIST's
 * {@code nfiq.c}).
 * <p>
 * Pipeline: MINDTCT minutiae detection and quality map generation, extraction of an 11-element feature vector
 * ({@link #computeNfiqFeatureVector}), Z-normalization, and classification by a feed-forward multi-layer
 * perceptron. The result is an NFIQ value from 1 (best) to 5 (worst) plus a confidence equal to the winning
 * MLP output activation.
 * <p>
 * Thread-safety: not thread-safe. The class relies on shared MINDTCT singletons ({@code Maps},
 * {@code Quality}) and on the JVM-wide logging flag in {@link Nist}, so concurrent scoring from multiple
 * threads is not supported.
 */
public class Nfiq1Helper extends Nfiq1 implements INfiq1Helper{
	/** SLF4J logger used for detailed diagnostic output when logging is enabled. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Nfiq1Helper.class);
	/**
	 * Serialises scoring across all instances: MINDTCT maps, the quality map, the MLP helpers and the log
	 * flag live in JVM-wide singletons/statics, so concurrent computations would overwrite each other.
	 */
	private static final Object NFIQ_LOCK = new Object();
	/**
	 * Builds the NFIQ feature vector from MINDTCT results (NIST {@code comp_nfiq_featvctr}).
	 * <p>
	 * Computes a histogram of the quality map, the foreground block count (all blocks with quality level
	 * {@code > 0}) and cumulative minutia reliability counts, and stores the following 11 features in order:
	 * <ol>
	 * <li>quality-map foreground block count;</li>
	 * <li>total number of minutiae;</li>
	 * <li>number of minutiae with reliability {@code > 0.5};</li>
	 * <li>number of minutiae with reliability {@code > 0.6};</li>
	 * <li>number of minutiae with reliability {@code > 0.7};</li>
	 * <li>number of minutiae with reliability {@code > 0.8};</li>
	 * <li>number of minutiae with reliability {@code > 0.9};</li>
	 * <li>fraction of foreground blocks with quality level 1;</li>
	 * <li>fraction of foreground blocks with quality level 2;</li>
	 * <li>fraction of foreground blocks with quality level 3;</li>
	 * <li>fraction of foreground blocks with quality level 4.</li>
	 * </ol>
	 * The computed values are also logged (or printed to stdout when detailed logging is disabled).
	 *
	 * @param featureVector output array receiving the feature values; must hold at least 11 elements. When the
	 *                      image is empty, the first {@code vectorLength} entries are set to {@code 0.0}.
	 * @param vectorLength  allocated length of {@code featureVector}
	 * @param oMinutiae     minutiae detected by MINDTCT
	 * @param qualityMap    quality map computed by MINDTCT (levels {@code 0..QMAP_LEVELS-1})
	 * @param mapWidth      width of the quality map in blocks
	 * @param mapHeight     height of the quality map in blocks
	 * @return {@code 0} ({@code ILfs.FALSE}) on successful completion, or {@code INfiq.EMPTY_IMG} if no foreground
	 *         blocks were found (feature vector zeroed)
	 */
	public int computeNfiqFeatureVector (double[] featureVector, int vectorLength, 
		AtomicReference<Minutiae> oMinutiae, Quality qualityMap, int mapWidth, int mapHeight) {
		int i, t;
		int foreground;
		int featureVectorIndex;
		int qualityMapHist [] = new int[ILfs.QMAP_LEVELS];
		AtomicIntegerArray qptr = null;
		int qualityMapLength;
		int num_rel_bins = INfiq.NFIQ_NUM_CLASSES;
		double[] rel_threshs = {0.5, 0.6, 0.7, 0.8, 0.9};
		int rel_bins [] = new int [INfiq.NFIQ_NUM_CLASSES];
		int passed_thresh;
		qualityMapLength = mapWidth * mapHeight;
		
		/* Generate qmap histogram */
		qptr = qualityMap.getQualityMap();
		int qptrIndex = 0;
		for (i = 0; i < qualityMapLength; i++)
		{
			qualityMapHist[qptr.get(qptrIndex++)]++;
		}

		/* Compute pixel foreground */
		foreground = qualityMapLength - qualityMapHist[0];

		if (foreground == ILfs.FALSE)
		{
			for (i = 0; i < vectorLength; i++)
			{
				featureVector [i] = 0.0f;
			}
			return INfiq.EMPTY_IMG;
		}

		/* Compute reliability bins */
		for (i = 0; i < oMinutiae.get().getNum(); i++)
		{
			passed_thresh = 1;
			Minutia minutia = oMinutiae.get().getList().get(i);
			for (t = 0; (t < num_rel_bins) && (passed_thresh == 1); t++)
			{
				if (minutia.getReliability() > rel_threshs[t])
				{
					rel_bins[t]++;
				}
				else
				{
					passed_thresh = 0;
				}
			}
		}

		featureVectorIndex = 0;
		/* Load feature vector */
		/* 1. qmap foreground count */
		featureVector [featureVectorIndex++] = (double)foreground;
		/* 2. number of minutiae */
		featureVector [featureVectorIndex++] = (double)oMinutiae.get().getNum();
		/* 3. reliability count > 0.5 */
		t = 0;
		featureVector [featureVectorIndex++] = (double)rel_bins[t++];
		/* 4. reliability count > 0.6 */
		featureVector [featureVectorIndex++] = (double)rel_bins[t++];
		/* 5. reliability count > 0.7 */
		featureVector [featureVectorIndex++] = (double)rel_bins[t++];
		/* 6. reliability count > 0.8 */
		featureVector [featureVectorIndex++] = (double)rel_bins[t++];
		/* 7. reliability count > 0.9 */
		featureVector [featureVectorIndex++] = (double)rel_bins[t++];
		/* 8. qmap count == 1 */
		i = 1;
		featureVector [featureVectorIndex++] = qualityMapHist[i++] / (double)foreground;
		/* 9. qmap count == 2 */
		featureVector [featureVectorIndex++] = qualityMapHist[i++] / (double)foreground;
		/* 10. qmap count == 3 */
		featureVector [featureVectorIndex++] = qualityMapHist[i++] / (double)foreground;
		/* 11. qmap count == 4 */
		featureVector [featureVectorIndex++] = qualityMapHist[i++] / (double)foreground;

		if (isShowLogs())
		{
			LOGGER.info(String.format(" \nCOMPUTED NFIQ1.0 VALUES\n[\n Quality Map Foreground Count=%d,\n number of minutiae=%d,\n (reliability count > 0.5) = %d,\n (reliability count > 0.6) = %d,\n (reliability count > 0.7) = %d,\n (reliability count > 0.8) = %d,\n (reliability count > 0.9) = %d,\n (qmap count == 1) = %2f,\n (qmap count == 2) = %2f,\n (qmap count == 3) = %2f,\n (qmap count == 4) = %2f\n]\n\n", foreground, 
					oMinutiae.get().getNum(), rel_bins [0], rel_bins [1], rel_bins [2], rel_bins [3], rel_bins [4], 
					(double)(qualityMapHist [1] / (float)foreground), (double)(qualityMapHist[2] / (float)foreground), (double)(qualityMapHist[3] / (float)foreground), (double)(qualityMapHist[4] / (float)foreground)));
		}
		/* return normally */
		return (ILfs.FALSE);
	}

	/**
	 * Computes the NFIQ value of a fingerprint image using the built-in defaults (NIST {@code comp_nfiq}).
	 * <p>
	 * Uses the default Z-normalization statistics and MLP weights/topology from {@link Nfiq1Globals} and
	 * delegates to {@link #computeNfiqFlex}. Side effect: sets the JVM-wide logging flag via
	 * {@code setShowLogs(logflag == 1)}.
	 *
	 * @param oNfiq       output receiving the NFIQ value, 1 (best) to 5 (worst)
	 * @param oConf       output receiving the confidence, i.e. the maximum MLP output-class activation
	 * @param imageData   grayscale fingerprint image data, one pixel per element, row-major
	 * @param imageWidth  image width in pixels
	 * @param imageHeight image height in pixels
	 * @param imageDepth  image pixel depth in bits (should always be 8)
	 * @param imagePPI    scan resolution in pixels per inch; pass {@code -1} ({@code ILfs.UNDEFINED}) if unknown,
	 *                    in which case 500 ppi is assumed
	 * @param logflag     {@code 1} to enable detailed logging, any other value to disable it
	 * @return {@code 0} on successful completion; {@code INfiq.EMPTY_IMG} if an empty image was detected (NFIQ set
	 *         to 5, confidence 1.0); {@code INfiq.TOO_FEW_MINUTIAE} if too few minutiae were detected, indicating a
	 *         poor-quality print (NFIQ set to 5, confidence 1.0); or a negative value on system error
	 */
	public int computeNfiq(AtomicInteger oNfiq, AtomicReference<Double> oConf, int [] imageData, 
		final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI, 
		int logflag) {		
		synchronized (NFIQ_LOCK) {
			setShowLogs (logflag == 1);
			return computeNfiqFlex(oNfiq, oConf, imageData, 
					imageWidth, imageHeight, imageDepth, imagePPI, 
					getNfiqGlobals().getDfltZnormMeans(), 
					getNfiqGlobals().getDfltZnormStds(), 
					getNfiqGlobals().getDfltNInps(), 
					getNfiqGlobals().getDfltNHids(), 
					getNfiqGlobals().getDfltNOuts(), 
					getNfiqGlobals().getDfltAcFuncHids(), 
					getNfiqGlobals().getDfltAcFuncOuts(), 
					getNfiqGlobals().getDfltWts());
		}
	}

	/**
	 * Computes the NFIQ value of a fingerprint image using caller-supplied statistics and MLP weights (NIST
	 * {@code comp_nfiq_flex}).
	 * <p>
	 * Steps: detect minutiae with MINDTCT using the V2 LFS parameters; if {@code INfiq.MIN_MINUTIAE} or fewer
	 * minutiae are found, return early; build the feature vector; Z-normalize it with {@code zNormMeans} and
	 * {@code zNormStds}; classify it with a feed-forward MLP; and report {@code class + 1} as the NFIQ value.
	 *
	 * @param oNfiq       output receiving the NFIQ value, 1 (best) to 5 (worst)
	 * @param oConf       output receiving the confidence, i.e. the maximum MLP output-class activation
	 * @param imageData   grayscale fingerprint image data, one pixel per element, row-major
	 * @param imageWidth  image width in pixels
	 * @param imageHeight image height in pixels
	 * @param imageDepth  image pixel depth in bits (should always be 8)
	 * @param imagePPI    scan resolution in pixels per inch; pass {@code -1} ({@code ILfs.UNDEFINED}) if unknown,
	 *                    in which case 500 ppi is assumed
	 * @param zNormMeans  global mean of each feature-vector coefficient, used for Z-normalization
	 * @param zNormStds   global standard deviation of each feature-vector coefficient, used for Z-normalization
	 * @param nInps       feature vector length (number of MLP inputs)
	 * @param nHids       number of hidden-layer nodes in the MLP
	 * @param nOuts       number of NFIQ levels (number of MLP output classes)
	 * @param acFuncHids  activation function code for the hidden layer (e.g. {@code IMlp.SINUSOID})
	 * @param acFuncOuts  activation function code for the output layer (e.g. {@code IMlp.SINUSOID})
	 * @param weights     MLP classification weights
	 * @return {@code 0} on successful completion; {@code INfiq.EMPTY_IMG} if an empty image was detected (NFIQ set
	 *         to 5, confidence 1.0); {@code INfiq.TOO_FEW_MINUTIAE} if too few minutiae were detected (NFIQ set to 5,
	 *         confidence 1.0); or a non-zero/negative error code propagated from minutiae detection or the MLP
	 */
	public int computeNfiqFlex(AtomicInteger oNfiq, AtomicReference<Double> oConf, int [] imageData, 
		final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI,
		double[] zNormMeans, double[] zNormStds, 
		int nInps, int nHids, int nOuts, final int acFuncHids, final int acFuncOuts,
		double[] weights) {
		synchronized (NFIQ_LOCK) {
			return computeNfiqFlexLocked(oNfiq, oConf, imageData, imageWidth, imageHeight, imageDepth, imagePPI,
					zNormMeans, zNormStds, nInps, nHids, nOuts, acFuncHids, acFuncOuts, weights);
		}
	}

	/**
	 * Body of {@link #computeNfiqFlex}; the caller must hold {@link #NFIQ_LOCK}.
	 *
	 * @param oNfiq       output receiving the NFIQ value, 1 (best) to 5 (worst)
	 * @param oConf       output receiving the confidence
	 * @param imageData   grayscale fingerprint image data, row-major
	 * @param imageWidth  image width in pixels
	 * @param imageHeight image height in pixels
	 * @param imageDepth  image pixel depth in bits
	 * @param imagePPI    scan resolution in pixels per inch, or {@code ILfs.UNDEFINED}
	 * @param zNormMeans  Z-normalization means
	 * @param zNormStds   Z-normalization standard deviations
	 * @param nInps       number of MLP inputs
	 * @param nHids       number of MLP hidden nodes
	 * @param nOuts       number of MLP output classes
	 * @param acFuncHids  hidden-layer activation function code
	 * @param acFuncOuts  output-layer activation function code
	 * @param weights     MLP classification weights
	 * @return same as {@link #computeNfiqFlex}
	 */
	private int computeNfiqFlexLocked(AtomicInteger oNfiq, AtomicReference<Double> oConf, int [] imageData, 
		final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI,
		double[] zNormMeans, double[] zNormStds, 
		int nInps, int nHids, int nOuts, final int acFuncHids, final int acFuncOuts,
		double[] weights) {

		AtomicInteger ret = new AtomicInteger(0);

		double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
		double[] outacsarr = new double[INfiq.NFIQ_NUM_CLASSES];
		AtomicInteger binarizedImageWidth = new AtomicInteger(0), 
			binarizedImageHeight = new AtomicInteger(0), 
			binarizedImageDepth = new AtomicInteger(0);
		double binarizedImageWidthPPMM = 0.0d;
		int[] binarizedImageData = null;

		AtomicReferenceArray<Double> wts = new AtomicReferenceArray<Double>(weights.length);
		for (int index = 0; index < weights.length; index++)
		{
			wts.set(index, weights[index]);
		}

		AtomicReference<Minutiae> minutiae = new AtomicReference<Minutiae>();
		minutiae.set(new Minutiae());
		//AtomicInteger quality_map = new AtomicInteger ();

		AtomicInteger class_i = new AtomicInteger();
		AtomicReference<Double> maxact = new AtomicReference<Double>(0.0d);

		/* If image ppi not defined, then assume 500 */
		if(imagePPI == ILfs.UNDEFINED)
			binarizedImageWidthPPMM  = INfiq.DEFAULT_PPI / (double)IAn2k.MM_PER_INCH;
		else 
			binarizedImageWidthPPMM  = imagePPI / (double)IAn2k.MM_PER_INCH;

		Maps imageMap = Maps.getInstance();
		Quality imageQualityMap = Quality.getInstance(); 
		
		/* Detect minutiae */
		binarizedImageData = getGetMinutiae().getMinutiae(ret, minutiae, 
			imageMap, imageQualityMap, binarizedImageWidth, binarizedImageHeight, binarizedImageDepth, 
			imageData, imageWidth, imageHeight, imageDepth, binarizedImageWidthPPMM, 
			getGlobals().getLfsParamsV2());
		if (ret.get() != ILfs.FALSE)
		{
			return (ret.get());
		}
		
		binarizedImageData = null;
		/* Catch case where too few minutiae detected */
		if (minutiae.get().getNum() <= INfiq.MIN_MINUTIAE)
		{
			getMinutiaHelper().freeMinutiae (minutiae);
			imageQualityMap = null;
			oNfiq.set(INfiq.MIN_MINUTIAE_QUAL);
			oConf.set(1.0d);
			return (INfiq.TOO_FEW_MINUTIAE);
		}

		/* Compute feature vector */
		ret.set(computeNfiqFeatureVector (featureVector, INfiq.NFIQ_VCTRLEN, minutiae, 
			imageQualityMap, imageMap.getMappedImageWidth().get(), imageMap.getMappedImageHeight().get()));
		if (ret.get() == INfiq.EMPTY_IMG)
		{
		   getMinutiaHelper().freeMinutiae (minutiae);
		   imageQualityMap = null;
		   oNfiq.set(INfiq.EMPTY_IMG_QUAL);
		   oConf.set(1.0d);
		   return (ret.get());
		}

		getMinutiaHelper().freeMinutiae (minutiae);
		imageQualityMap = null;

		/* ZNormalize feature vector */
		getZNorm().ZNormalizeFeatureVector(featureVector, zNormMeans, zNormStds, INfiq.NFIQ_VCTRLEN);

		AtomicReferenceArray<Double> outacs = new AtomicReferenceArray<Double>(outacsarr.length);
		for (int index = 0; index < outacsarr.length; index++)
		{
			outacs.set(index, outacsarr[index]);
		}
		
		/* Classify feature vector with feedforward MLP */
		ret.set(getRunMlp().runMlp2(nInps, nHids, nOuts, acFuncHids, acFuncOuts, wts, 
			featureVector, outacs, class_i, maxact));
		if (ret.get() != ILfs.FALSE)
		{
			return (ret.get());
		}

		for (int index = 0; index < outacs.length(); index++)
		{
			outacsarr[index] = outacs.get(index);
		}

		oNfiq.set(class_i.get() + 1);
		oConf.set(maxact.get());

		ret.set(ILfs.FALSE);
		return ret.get();
	}
}


