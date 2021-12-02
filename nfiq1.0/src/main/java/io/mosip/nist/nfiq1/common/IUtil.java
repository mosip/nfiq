package io.mosip.nist.nfiq1.common;

public interface IUtil {
	/* SsxStats.java */
	public interface ISsxStats {
		public double ssxStdDev(final double sumX, final double sumX2, final int count);
		public double ssxVariance(final double sumX, final double sumX2, final int count);
		public double ssx(final double sumX, final double sumX2, final int count);
	}
}
