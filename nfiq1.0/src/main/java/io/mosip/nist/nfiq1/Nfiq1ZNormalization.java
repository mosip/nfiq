package io.mosip.nist.nfiq1;

import java.util.ArrayList;
import java.util.List;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.INfiq.INfiq1ZNormalization;
import io.mosip.nist.nfiq1.util.SsxStats;

public class Nfiq1ZNormalization implements INfiq1ZNormalization
{
	private SsxStats ssxStats = new SsxStats();
	
	/***********************************************************************
	************************************************************************
	#cat: ZNormalizeFeatureVector - Routine Z-Normalized an NFIQ feature vector

	   Input:
	      featureVector    - NFIQ feature vector
	      zNormmeansList - global meansList for each coef in the feature vector
	      zNormStds  - global stddev for each coef in the feature vector
	      vectorLength     - allocated length of feature vector
	   Output:
	      featureVector    - resulting normalized feature vector values
	************************************************************************/
	public void ZNormalizeFeatureVector(double[] featureVector, double[] zNormmeansList, double[] zNormStds, final int vectorLength)
	{
		int i;
		for (i = 0; i < vectorLength; i++)
		{
			featureVector [i] = (double)((double)((double)featureVector [i] - (double)zNormmeansList[i]) / (double)zNormStds[i]);
		}
	}

	/***********************************************************************
	************************************************************************
	#cat: computeZNormStats - Routine takes a list of feature vectors
	#cat:             (a matrix) and computes the mean and stddev for each
	#cat:             column of features coefs in the matrix.

	   Input:
	      featureList       - list of input feature vectors
	      nfeatureVectors  	- number of vectors in the list
	      noOffeatureList 	- number of coefs in each vector
	   Output:
	      oMeansList      	- resulting allocated list of coef meansList
	      oStdDevsList    	- resulting allocated list of coef stdDevsList
	   Return Code:
	      Zero        		- successful completion
	      Negative    		- system error
	************************************************************************/
	public int computeZNormStats(List<List<Double>> oMeansList, List<List<Double>> oStdDevsList, 
			List<List<Double>> featureList, final int nfeatureVectors, final int noOffeatureList) {
	   double fret;
	   List<Double> meansList, stdDevsList;
	   List<Double> mptr, sdptr;
	   List<List<Double>> sfeatptr;
	   List<Double> featptr;
	   float sumX, sumX2;

	   //int capacity = nfeatureList;
	   meansList = new ArrayList<Double>(noOffeatureList);
	   stdDevsList = new ArrayList<Double>(noOffeatureList);

	   /* foreach column of feature vector matrix */
	   sfeatptr = featureList;
	   mptr = meansList;
	   sdptr = stdDevsList;

	   for (int featureIndex = 0; featureIndex < noOffeatureList; featureIndex++)
	   {
		   featptr = sfeatptr.get(featureIndex);
		   /* sum_x column of features */
		   sumX = 0.0f;
		   sumX2 = 0.0f;
		   for (int vectorIndex = 0; vectorIndex < nfeatureVectors; vectorIndex++)
		   {
			   sumX += featptr.get(vectorIndex);
			   sumX2 += featptr.get(vectorIndex) * featptr.get(vectorIndex);
			   featptr.set(vectorIndex, featptr.get(vectorIndex) + noOffeatureList);
		   }
		   /* compute mean of column features */
		   mptr.add((double) (sumX / nfeatureVectors));
		   fret = (float)getSsxStats().ssxStdDev(sumX, sumX2, nfeatureVectors);
		   if (fret < 0F)
		   {
			   meansList = null;
			   stdDevsList = null;
			   return (-4);
		  }
		  sdptr.add(fret);

		  /* bump to next column in feature vector matrix */
		  //sfeatptr++;
	   }
	   
	   oMeansList.set(0, meansList);
	   oStdDevsList.set(0, stdDevsList);

	   return ILfs.FALSE;
	}		

	public SsxStats getSsxStats() {
		return ssxStats;
	}

	public void setSsxStats(SsxStats ssxStats) {
		this.ssxStats = ssxStats;
	}
}
