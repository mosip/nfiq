package io.mosip.nist.nfiq1.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public interface IMlpCla {
	/* Cblas library routines used by MLP library codes */
	public int sgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
			AtomicReferenceArray<Double> a, int lda,
			AtomicReferenceArray<Double> x, int incx, AtomicReference<Double> beta, 
			AtomicReferenceArray<Double> y, int incy);
	public int sscal(int n, double sa, AtomicReference<Double> sx, int incx);
	public int saxpY(int n, double sa, AtomicReference<Double> sx, 
			int incx, AtomicReference<Double> sy, int incy);
	public double sDot(int n, AtomicReference<Double> sx, int incx, 
			AtomicReference<Double> sy, int incy);
	public double snRm2(int n, AtomicReference<Double> x, int incx);
	
	public int mlpSgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
			AtomicReferenceArray<Double> a, int lda, AtomicReferenceArray<Double> x,
			AtomicInteger incx, AtomicReference<Double> beta, AtomicReferenceArray<Double> y, AtomicInteger incy);
	public int mlpSScal(int n, double sa, AtomicReference<Double> sx, int incx);
	public int mlpSaxpY(int n, double sa, AtomicReference<Double> sx, 
		int incx, AtomicReference<Double> sy, int incy);
	public double mlpSDot(int n, AtomicReference<Double> sx, int incx, 
		AtomicReference<Double> sy, int incy);
	public double mlpSnrm2(int n, AtomicReference<Double> x, int incx);
}