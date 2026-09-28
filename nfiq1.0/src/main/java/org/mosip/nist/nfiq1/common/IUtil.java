package org.mosip.nist.nfiq1.common;

/** Container for contracts of generic NIST utility routines ({@code util} package). */
public interface IUtil {
	/**
	 * Statistics from running sums (port of NIST's {@code ssxstats.c}); implemented by
	 * {@code org.mosip.nist.nfiq1.util.SsxStats}.
	 */
	public interface ISsxStats {
		/**
		 * Computes the sample standard deviation from a running sum and sum of squares (NIST {@code ssx_stddev}).
		 *
		 * @param sumX  sum of the samples
		 * @param sumX2 sum of the squared samples
		 * @param count number of samples
		 * @return the standard deviation, or a negative value on error (e.g. too few samples)
		 */
		public double ssxStdDev(final double sumX, final double sumX2, final int count);
		/**
		 * Computes the sample variance from a running sum and sum of squares (NIST {@code ssx_variance}).
		 *
		 * @param sumX  sum of the samples
		 * @param sumX2 sum of the squared samples
		 * @param count number of samples
		 * @return the variance {@code ssx / (count - 1)}, or a negative value on error
		 */
		public double ssxVariance(final double sumX, final double sumX2, final int count);
		/**
		 * Computes the sum of squared deviations from the mean, {@code sumX2 - sumX^2 / count} (NIST {@code ssx}).
		 *
		 * @param sumX  sum of the samples
		 * @param sumX2 sum of the squared samples
		 * @param count number of samples
		 * @return the sum of squared deviations
		 */
		public double ssx(final double sumX, final double sumX2, final int count);
	}
}
