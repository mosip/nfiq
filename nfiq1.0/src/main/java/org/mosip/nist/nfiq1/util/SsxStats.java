package org.mosip.nist.nfiq1.util;

import org.mosip.nist.nfiq1.Nist;
import org.mosip.nist.nfiq1.common.IUtil.ISsxStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsxStats extends Nist implements ISsxStats {
	private static final Logger logger = LoggerFactory.getLogger(SsxStats.class);

	/*****************************************************
	 * ssxStdDev accepts the sum of the values, the sum of the squares of the
	 * values, and the number of values contained in the sum and returns the
	 * standard deviation of the data. double sumX; # sum of the x values double
	 * sumX2; # sum of the squares of the x values int count; # number of items
	 * sumed
	 ******************************************************/
	public double ssxStdDev(final double sumX, final double sumX2, final int count) {
		double varKey = 0;

		varKey = ssxVariance(sumX, sumX2, count);
		if (varKey >= 0.0) {
			return (Math.sqrt(varKey));
		} else {
			/* otherwise error code */
			return (varKey);
		}
	}

	/*****************************************************
	 * ssxVariance accepts the sum of the values, the sum of the squares of the
	 * values, and the number of values contained in the sum and returns the
	 * variance of the data. double sumX; # sum of the x values double sumX2; # sum
	 * of the squares of the values x int count; # number of items that were sumed
	 ******************************************************/
	public double ssxVariance(final double sumX, final double sumX2, final int count) {
		double ssxval; // holds value from SSx()
		double variance;

		if (count < 2) {
			logger.error("ERROR : ssx_variance : invalid count : {} < 2\n", count);
			return (-2.0);
		}
		ssxval = ssx(sumX, sumX2, count);
		variance = ssxval / (count - 1);

		return (variance);
	}

	/*****************************************************
	 * ssx accepts the sum of the values, sumX, the sum of the squares of the
	 * values, sumX2 and the number of values contained in the sums, count and
	 * returns the value of the sum of the squares calculation, SS(x). double sumX;
	 * # sum of x values double sumX2; # sum of the squares of the x values int
	 * count; # number of values sumed
	 ******************************************************/
	public double ssx(final double sumX, final double sumX2, final int count) {
		// SS(x) SS(y)
		/* SS(x) = (sumX2 - ((sumX * sumX)/count)) */
		double ssx;

		ssx = sumX * sumX;
		ssx = ssx / count;
		ssx = sumX2 - ssx;

		return (ssx);
	}
}