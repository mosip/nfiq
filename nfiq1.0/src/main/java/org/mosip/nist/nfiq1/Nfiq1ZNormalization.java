package org.mosip.nist.nfiq1;

import java.util.ArrayList;
import java.util.List;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.INfiq.INfiq1ZNormalization;
import org.mosip.nist.nfiq1.util.SsxStats;

/**
 * Z-normalization routines for NFIQ feature vectors (port of NIST's {@code znorm.c}).
 * <p>
 * Z-normalization rescales each feature to zero mean and unit variance using global statistics, so that the
 * MLP classifier sees features on comparable scales. Instances hold only a stateless {@link SsxStats} helper.
 */
public class Nfiq1ZNormalization implements INfiq1ZNormalization
{
	/** Helper for computing standard deviation from running sums. */
	private SsxStats ssxStats = new SsxStats();
	
	/**
	 * Z-normalizes an NFIQ feature vector in place (NIST {@code znorm_fniq_featvctr}).
	 * <p>
	 * For each coefficient {@code i}: {@code featureVector[i] = (featureVector[i] - mean[i]) / stddev[i]}.
	 *
	 * @param featureVector  NFIQ feature vector; overwritten with the normalized values
	 * @param zNormmeansList global mean for each coefficient of the feature vector
	 * @param zNormStds      global standard deviation for each coefficient of the feature vector
	 * @param vectorLength   number of coefficients to normalize
	 */
	public void ZNormalizeFeatureVector(double[] featureVector, double[] zNormmeansList, double[] zNormStds, final int vectorLength)
	{
		int i;
		for (i = 0; i < vectorLength; i++)
		{
			featureVector [i] = (double)((double)((double)featureVector [i] - (double)zNormmeansList[i]) / (double)zNormStds[i]);
		}
	}

	/**
	 * Computes the per-column mean and standard deviation of a matrix of feature vectors (NIST
	 * {@code comp_znorm_stats}).
	 * <p>
	 * {@code featureList} is indexed as {@code featureList.get(featureIndex).get(vectorIndex)}. Side effect: each
	 * visited entry of {@code featureList} is incremented by {@code noOffeatureList} while summing.
	 *
	 * @param oMeansList      output; element 0 is set to the list of coefficient means
	 * @param oStdDevsList    output; element 0 is set to the list of coefficient standard deviations
	 * @param featureList     input feature matrix, one inner list per feature coefficient
	 * @param nfeatureVectors number of feature vectors (entries per inner list)
	 * @param noOffeatureList number of coefficients in each feature vector
	 * @return {@code 0} ({@code ILfs.FALSE}) on successful completion, or {@code -4} if a standard deviation could
	 *         not be computed (system error)
	 */
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

	/**
	 * Returns the helper used to compute standard deviations.
	 *
	 * @return the {@link SsxStats} helper
	 */
	public SsxStats getSsxStats() {
		return ssxStats;
	}

	/**
	 * Sets the helper used to compute standard deviations.
	 *
	 * @param ssxStats the {@link SsxStats} helper
	 */
	public void setSsxStats(SsxStats ssxStats) {
		this.ssxStats = ssxStats;
	}
}
