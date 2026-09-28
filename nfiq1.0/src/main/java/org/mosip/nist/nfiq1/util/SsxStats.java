package org.mosip.nist.nfiq1.util;

import org.mosip.nist.nfiq1.Nist;
import org.mosip.nist.nfiq1.common.IUtil.ISsxStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sum-of-squares statistics (standard deviation, variance and SS(x)) computed from running sums.
 * <p>
 * Port of NIST's {@code ssx.c} utilities ({@code ssx_stddev}, {@code ssx_variance}, {@code ssx}), used by
 * NFIQ to compute feature statistics without keeping every sample. Stateless and thread-safe.
 */
public class SsxStats extends Nist implements ISsxStats {
	/** SLF4J logger for invalid-input errors. */
	private static final Logger logger = LoggerFactory.getLogger(SsxStats.class);

	/**
	 * Computes the sample standard deviation from a sum, a sum of squares and a count.
	 * <p>
	 * Port of NIST {@code ssx_stddev()}: returns {@code sqrt(ssxVariance(sumX, sumX2, count))}.
	 *
	 * @param sumX  sum of the x values
	 * @param sumX2 sum of the squares of the x values
	 * @param count number of values summed
	 * @return the standard deviation, or the negative error code from
	 *         {@link #ssxVariance(double, double, int)} (-2.0 if {@code count < 2})
	 */
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

	/**
	 * Computes the sample variance from a sum, a sum of squares and a count.
	 * <p>
	 * Port of NIST {@code ssx_variance()}: variance = SS(x) / (count - 1).
	 *
	 * @param sumX  sum of the x values
	 * @param sumX2 sum of the squares of the x values
	 * @param count number of values summed; must be at least 2
	 * @return the sample variance, or -2.0 if {@code count < 2}
	 */
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

	/**
	 * Computes the corrected sum of squares SS(x) from a sum, a sum of squares and a count.
	 * <p>
	 * Port of NIST {@code ssx()}: SS(x) = sumX2 - (sumX * sumX) / count.
	 *
	 * @param sumX  sum of the x values
	 * @param sumX2 sum of the squares of the x values
	 * @param count number of values summed (must be non-zero)
	 * @return the SS(x) value
	 */
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