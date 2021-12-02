package io.mosip.nist.nfiq1;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.common.IAn2k;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.INfiq;
import io.mosip.nist.nfiq1.common.ILfs.Minutia;
import io.mosip.nist.nfiq1.common.ILfs.Minutiae;
import io.mosip.nist.nfiq1.common.INfiq.INfiq1Helper;
import io.mosip.nist.nfiq1.mindtct.Maps;
import io.mosip.nist.nfiq1.mindtct.Quality;

public class Nfiq1Helper extends Nfiq1 implements INfiq1Helper{
	private static final Logger LOGGER = LoggerFactory.getLogger(Nfiq1Helper.class);	
	/***********************************************************************
	************************************************************************
	#cat: computeNfiqFeatureVector - Routine takes results from NIST's Mindtct and
	#cat:                      computes a feature vector for computing NFIQ

	   Input:
	      vectorLength     - allocated length of feature vector
	      oMinutiae    - list of minutiae from NIST's Mindtct
	      qualityMap - quality map computed by NIST's Mindtct
	      mapWidth       - width of maforegroundp
	      mapHeight       - height of map
	   Output:
	      featureVector    - resulting feature vector values
	   Return Code:
	      Zero        - successful completion
	      EMPTY_IMG   - empty image detected (feature vector set to 0's)
	************************************************************************/
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
		else if (!isShowLogs())
		{		
			System.out.println(String.format(" \nCOMPUTED NFIQ1.0 VALUES\n[\n Quality Map Foreground Count=%d,\n number of minutiae=%d,\n (reliability count > 0.5) = %d,\n (reliability count > 0.6) = %d,\n (reliability count > 0.7) = %d,\n (reliability count > 0.8) = %d,\n (reliability count > 0.9) = %d,\n (qmap count == 1) = %2f,\n (qmap count == 2) = %2f,\n (qmap count == 3) = %2f,\n (qmap count == 4) = %2f\n]\n\n", foreground, 
				oMinutiae.get().getNum(), rel_bins [0], rel_bins [1], rel_bins [2], rel_bins [3], rel_bins [4], 
				(double)(qualityMapHist [1] / (float)foreground), (double)(qualityMapHist[2] / (float)foreground), (double)(qualityMapHist[3] / (float)foreground), (double)(qualityMapHist[4] / (float)foreground)));
		}
		/* return normally */
		return (ILfs.FALSE);
	}

	/***********************************************************************
	************************************************************************
	#cat: computeNfiq - Routine computes NFIQ given an input image.
	#cat:             This routine uses default statistics for Z-Normalization
	#cat:             and default weights for MLP classification.
	   Input:
	      imageData       - grayscale fingerprint image data
	      imageWidth          - image pixel width
	      imageHeight          - image pixel height
	      imageDepth          - image pixel depth (should always be 8)
	      imagePPI        - image scan density in pix/inch
	                    If scan density is unknown (pass in -1),
	                    then default density of 500ppi is used.
	   Output:
	      oNfiq       - resulting NFIQ value
	      oConf       - max output class MLP activation
	   Return Code:
	      Zero        - successful completion
	      EMPTY_IMG   - empty image detected (feature vector set to 0's)
	      TOO_FEW_MINUTIAE - too few minutiae detected from fingerprint image,
	                    indicating poor quality fingerprint
	      Negative    - system error
	************************************************************************/
	public int computeNfiq(AtomicInteger oNfiq, AtomicReference<Double> oConf, int [] imageData, 
		final int imageWidth, final int imageHeight, final int imageDepth, final int imagePPI, 
		int logflag) {		
		setShowLogs (logflag == 1);
		int ret = computeNfiqFlex(oNfiq, oConf, imageData, 
				imageWidth, imageHeight, imageDepth, imagePPI, 
				getNfiqGlobals().getDfltZnormMeans(), 
				getNfiqGlobals().getDfltZnormStds(), 
				getNfiqGlobals().getDfltNInps(), 
				getNfiqGlobals().getDfltNHids(), 
				getNfiqGlobals().getDfltNOuts(), 
				getNfiqGlobals().getDfltAcFuncHids(), 
				getNfiqGlobals().getDfltAcFuncOuts(), 
				getNfiqGlobals().getDfltWts());
		
		return (ret);
	}

	/***********************************************************************
	************************************************************************
	#cat: computeNfiqFlex - Routine computes NFIQ given an input image.
	#cat:             This routine requires statistics for Z-Normalization
	#cat:             and weights for MLP classification.
	   Input:
	      imageData     - grayscale fingerprint image data
	      imageWidth    - image pixel width
	      imageHeight   - image pixel height
	      imageDepth    - image pixel depth (should always be 8)
	      imagePPI		- image scan density in pix/inch
	                    If scan density is unknown (pass in -1),
	                    then default density of 500ppi is used.
	      zNormMeans 	- global mean for each feature vector coef used for Z-Norm
	      zNormStds  	- global stddev for each feature vector coef used for Z-Norm
	      nInps       	- feature vector length (number of MLP inputs)
	      nHids       	- number of hidden layer neurodes in MLP
	      nOuts       	- number of NFIQ levels (number of MLP output classes)
	      acFuncHids 	- type of MLP activiation function used at MLP hidden layer
	      acFuncOuts 	- type of MLP activiation function used at MLP output layer
	      weights         	- MLP classification weights
	   Output:
	      oNfiq       	- resulting NFIQ value
	      oConf       	- max output class MLP activation
	   Return Code:
	      Zero        	- successful completion
	      EMPTY_IMG   	- empty image detected (feature vector set to 0's)
	      TOO_FEW_MINUTIAE - too few minutiae detected from fingerprint image,
	                    indicating poor quality fingerprint
	      EMPTY_IMG   	- empty image detected (feature vector set to 0's)
	      Negative    	- system error
	************************************************************************/
	public int computeNfiqFlex(AtomicInteger oNfiq, AtomicReference<Double> oConf, int [] imageData, 
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


