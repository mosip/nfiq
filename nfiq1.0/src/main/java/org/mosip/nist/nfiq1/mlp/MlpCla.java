package org.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.IMlpCla;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BLAS/LAPACK-style linear algebra routines used by the NFIQ multi-layer perceptron.
 * <p>
 * Port of NIST's {@code mlp/mlp_cla.c}, which wraps f2c-translated Fortran BLAS routines ({@code sgemv},
 * {@code sscal}, {@code saxpy}, {@code sdot}, {@code snrm2}) and the LAPACK helpers {@code lsame} and
 * {@code xerbla}. Only {@link #sgemV} is actually implemented; it computes the matrix-vector products in
 * {@link RunMlp}. The other BLAS routines are placeholders that return 0.
 * <p>
 * Lazily created singleton. <b>Not thread-safe:</b> {@code sgemV} and {@code compareChars} keep their loop
 * counters and temporaries in {@code private static} fields (a leftover of the f2c translation), so concurrent
 * calls can interfere with each other.
 */
public class MlpCla extends Mlp implements IMlpCla {
	/** SLF4J logger, used by {@code xerbla_} to report illegal parameters. */
	private static final Logger logger = LoggerFactory.getLogger(MlpCla.class);
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static MlpCla instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private MlpCla() {
		super();
	}

	/**
	 * Returns the shared {@code MlpCla} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized MlpCla getInstance() {
		if (instance == null) {
			instance = new MlpCla();
		}
		return instance;
	}

	/** {@code sgemV} scratch: index of the first invalid parameter (0 if all are valid), as passed to xerbla. */
	private static int info;
	/** {@code sgemV} scratch: running product or dot-product accumulator. */
	private static double temp;
	/** {@code sgemV} scratch: logical length of vector x (n if TRANS = 'N', else m). */
	private static int lenx;
	/** {@code sgemV} scratch: logical length of vector y (m if TRANS = 'N', else n). */
	private static int leny;
	/** {@code sgemV} scratch: 1-based row loop counter. */
	private static int i;
	/** {@code sgemV} scratch: 1-based column loop counter. */
	private static int j;
	/** {@code sgemV} scratch: current 1-based index into x for strided access. */
	private static int ix;
	/** {@code sgemV} scratch: current 1-based index into y for strided access. */
	private static int iy;
	/** {@code sgemV} scratch: 1-based index into x for the current column. */
	private static int jx;
	/** {@code sgemV} scratch: 1-based index into y for the current column. */
	private static int jy;
	/** {@code sgemV} scratch: 1-based start index into x (adjusted for a negative INCX). */
	private static int kx;
	/** {@code sgemV} scratch: 1-based start index into y (adjusted for a negative INCY). */
	private static int ky;

	/**
	 * Level 2 BLAS {@code SGEMV}: general matrix-vector multiply.
	 * <p>
	 * Performs one of the matrix-vector operations {@code y := alpha*A*x + beta*y} (TRANS = 'N') or
	 * {@code y := alpha*A'*x + beta*y} (TRANS = 'T' or 'C'), where alpha and beta are scalars, x and y are
	 * vectors and A is an m by n matrix stored column-major with leading dimension {@code lda}. All indexing
	 * follows the Fortran 1-based convention through the {@code XGet}/{@code YGet}/{@code AGet} helpers. The
	 * input parameters are validated first; an invalid one is reported through {@code xerbla_} and the method
	 * returns without computing. It also returns early if m or n is 0, or if alpha is 0 and beta is 1. When
	 * beta is 0, y does not need to be set on input.
	 *
	 * @param trans operation to perform: 'N'/'n' for {@code y := alpha*A*x + beta*y}; 'T'/'t' or 'C'/'c' for
	 *              {@code y := alpha*A'*x + beta*y}. Unchanged on exit.
	 * @param m     number of rows of matrix A; must be at least 0
	 * @param n     number of columns of matrix A; must be at least 0
	 * @param alpha the scalar alpha. Unchanged on exit.
	 * @param a     matrix A, of dimension (lda, n), column-major; its leading m by n part holds the
	 *              coefficients. Unchanged on exit.
	 * @param lda   first (leading) dimension of A as declared by the caller; must be at least max(1, m)
	 * @param x     vector x, with at least {@code 1 + (n - 1) * |incx|} elements when TRANS = 'N', otherwise
	 *              {@code 1 + (m - 1) * |incx|}. Unchanged on exit.
	 * @param incx  increment between elements of x; must not be 0
	 * @param beta  the scalar beta. Unchanged on exit.
	 * @param y     input/output vector y, with at least {@code 1 + (m - 1) * |incy|} elements when TRANS = 'N',
	 *              otherwise {@code 1 + (n - 1) * |incy|}; overwritten with the updated y
	 * @param incy  increment between elements of y; must not be 0
	 * @return always 0. Parameter errors (info = 1 trans, 2 m, 3 n, 6 lda, 8 incx, 11 incy) are only logged.
	 */
	public int sgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha,
			AtomicReferenceArray<Double> a, int lda, AtomicReferenceArray<Double> x, int incx,
			AtomicReference<Double> beta, AtomicReferenceArray<Double> y, int incy) {
		/* System generated locals */
		/*
		 * Unused variables commented out by MDG on 03-09-05 int a_dim1, a_offset;
		 */
		int i1;
		int i2;

		info = 0;
		if (!compareChars(trans, 'N') && !compareChars(trans, 'T') && !compareChars(trans, 'C')) {
			info = 1;
		} else if (m < 0) {
			info = 2;
		} else if (n < 0) {
			info = 3;
		} else if (lda < Math.max(1, m)) {
			info = 6;
		} else if (incx == 0) {
			info = 8;
		} else if (incy == 0) {
			info = 11;
		}

		if (info != 0) {
			xerbla_("SGEMV ", info);
			return 0;
		}
		/* Quick return if possible. */
		/* Parentesis added by MDG on 03-09-05 */
		if (m == 0 || n == 0 || (alpha.get() == 0.0f && beta.get() == 1.0f)) {
			return 0;
		}

		/*
		 * Set LENX and LENY, the lengths of the vectors x and y, and set up the start
		 * points in X and Y.
		 */

		if (compareChars(trans, 'N')) {
			lenx = n;
			leny = m;
		} else {
			lenx = m;
			leny = n;
		}
		if (incx > 0) {
			kx = 1;
		} else {
			kx = 1 - (lenx - 1) * incx;
		}
		if (incy > 0) {
			ky = 1;
		} else {
			ky = 1 - (leny - 1) * incy;
		}

		/*
		 * Start the operations. In this version the elements of A are accessed
		 * sequentially with one pass through A. First form y := beta*y.
		 */
		if (beta.get() != 1.0f) {
			if (incy == 1) {
				if (beta.get() == 0.0f) {
					i1 = leny;
					for (i = 1; i <= leny; ++i) {
						YSet(y, i, 0.0f);
						/* L10: */
					}
				} else {
					i1 = leny;
					for (i = 1; i <= leny; ++i) {
						YSet(y, i, beta.get() * YGet(y, i));
						/* L20: */
					}
				}
			} else {
				iy = ky;
				if (beta.get() == 0.0f) {
					i1 = leny;
					for (i = 1; i <= leny; ++i) {
						YSet(y, iy, 0.0f);
						iy += incy;
						/* L30: */
					}
				} else {
					i1 = leny;
					for (i = 1; i <= leny; ++i) {
						YSet(y, iy, beta.get() * YGet(y, iy));
						iy += incy;
						/* L40: */
					}
				}
			}
		}

		if (alpha.get() == 0.0f) {
			return 0;
		}
		if (compareChars(trans, 'N')) {
			/* Form y := alpha*A*x + y. */
			jx = kx;
			if (incy == 1) {
				i1 = n;
				for (j = 1; j <= n; ++j) {
					if (XGet(x, jx) != 0.0f) {
						temp = alpha.get() * XGet(x, jx);
						i2 = m;
						for (i = 1; i <= m; ++i) {
							YSetPlus(y, i, temp * AGet(a, i, j, lda));
							/* L50: */
						}
					}
					jx += incx;
					/* L60: */
				}
			} else {
				i1 = n;
				for (j = 1; j <= n; ++j) {
					if (XGet(x, jx) != 0.0f) {
						temp = alpha.get() * XGet(x, jx);
						iy = ky;
						i2 = m;
						for (i = 1; i <= m; ++i) {
							YSetPlus(y, iy, temp * AGet(a, i, j, lda));
							iy += incy;
							/* L70: */
						}
					}
					jx += incx;
					/* L80: */
				}
			}
		} else {
			/* Form y := alpha*A'*x + y. */
			jy = ky;
			if (incx == 1) {
				i1 = n;
				for (j = 1; j <= n; ++j) {
					temp = 0.0f;
					i2 = m;
					for (i = 1; i <= m; ++i) {
						temp += AGet(a, i, j, lda) * XGet(x, i);
						/* L90: */
					}
					YSetPlus(y, jy, alpha.get() * temp);
					jy += incy;
					/* L100: */
				}
			} else {
				i1 = n;
				for (j = 1; j <= n; ++j) {
					temp = 0.0f;
					ix = kx;
					i2 = m;
					for (i = 1; i <= m; ++i) {
						temp += AGet(a, i, j, lda) * XGet(x, ix);
						ix += incx;
						/* L110: */
					}
					YSetPlus(y, jy, alpha.get() * temp);
					jy += incy;
					/* L120: */
				}
			}
		}

		return 0;
		/* End of SGEMV . */
	}

	/**
	 * Reads element {@code x(I)} using a Fortran-style 1-based index (C macro {@code X(I)}).
	 *
	 * @param x the vector
	 * @param I 1-based element index
	 * @return {@code x[I - 1]}
	 */
	private double XGet(AtomicReferenceArray<Double> x, int I) {
		return x.get(I - 1);
	}

	/**
	 * Adds a value to element {@code x(I)} using a Fortran-style 1-based index. Currently unused.
	 *
	 * @param x     the vector, modified in place
	 * @param I     1-based element index
	 * @param value amount to add
	 */
	private void XSetPlus(double[] x, int I, double value) {
		x[I - 1] = x[I - 1] + value;
	}

	/**
	 * Reads element {@code y(I)} using a Fortran-style 1-based index (C macro {@code Y(I)}).
	 *
	 * @param y the vector
	 * @param I 1-based element index
	 * @return {@code y[I - 1]}
	 */
	private double YGet(AtomicReferenceArray<Double> y, int I) {
		return y.get(I - 1);
	}

	/**
	 * Sets element {@code y(I)} using a Fortran-style 1-based index.
	 *
	 * @param y     the vector, modified in place
	 * @param I     1-based element index
	 * @param value new value
	 */
	private void YSet(AtomicReferenceArray<Double> y, int I, double value) {
		y.set(I - 1, value);
	}

	/**
	 * Adds a value to element {@code y(I)} using a Fortran-style 1-based index.
	 *
	 * @param y     the vector, modified in place
	 * @param I     1-based element index
	 * @param value amount to add
	 */
	private void YSetPlus(AtomicReferenceArray<Double> y, int I, double value) {
		y.set(I - 1, y.get(I - 1) + value);
	}

	/**
	 * Reads matrix element {@code A(I, J)} of a column-major matrix using Fortran-style 1-based indexes
	 * (C macro {@code A(I,J)}).
	 *
	 * @param a   the matrix storage, column-major
	 * @param I   1-based row index
	 * @param J   1-based column index
	 * @param lda leading dimension (number of stored rows) of the matrix
	 * @return {@code a[(I - 1) + (J - 1) * lda]}
	 */
	private double AGet(AtomicReferenceArray<Double> a, int I, int J, int lda) {
		return a.get((I) - 1 + (((J) - 1) * (lda)));
	}

	/**
	 * BLAS {@code SSCAL} (scale a vector by a constant, {@code x := sa*x}). Not implemented in this port.
	 *
	 * @param n    number of elements
	 * @param sa   scale factor
	 * @param sx   the vector (not modified)
	 * @param incx increment between elements
	 * @return always 0
	 */
	public int sscal(int n, double sa, AtomicReference<Double> sx, int incx) {
		return 0;
	}

	/**
	 * BLAS {@code SAXPY} ({@code y := sa*x + y}). Not implemented in this port.
	 *
	 * @param n    number of elements
	 * @param sa   scalar multiplier
	 * @param sx   vector x
	 * @param incx increment between elements of x
	 * @param sy   vector y (not modified)
	 * @param incy increment between elements of y
	 * @return always 0
	 */
	public int saxpY(int n, double sa, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		return 0;
	}

	/**
	 * BLAS {@code SDOT} (dot product of two vectors). Not implemented in this port.
	 *
	 * @param n    number of elements
	 * @param sx   vector x
	 * @param incx increment between elements of x
	 * @param sy   vector y
	 * @param incy increment between elements of y
	 * @return always 0
	 */
	public double sDot(int n, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		return 0;
	}

	/**
	 * BLAS {@code SNRM2} (Euclidean norm of a vector). Not implemented in this port.
	 *
	 * @param n    number of elements
	 * @param x    the vector
	 * @param incx increment between elements
	 * @return always 0
	 */
	public double snRm2(int n, AtomicReference<Double> x, int incx) {
		return 0;
	}

	/**
	 * Wrapper around {@link #sgemV} (NIST {@code mlp_sgemv}).
	 * <p>
	 * In C this copied the arguments into f2c {@code integer} temporaries before calling the Fortran routine.
	 * Here it unwraps {@code incx}/{@code incy} and delegates.
	 *
	 * @param trans operation: 'N' for {@code y := alpha*A*x + beta*y}; 'T' or 'C' for the transposed form
	 * @param m     number of rows of A
	 * @param n     number of columns of A
	 * @param alpha the scalar alpha
	 * @param a     matrix A, column-major
	 * @param lda   leading dimension of A
	 * @param x     vector x
	 * @param incx  increment between elements of x
	 * @param beta  the scalar beta
	 * @param y     input/output vector y, overwritten with the result
	 * @param incy  increment between elements of y
	 * @return the value returned by {@link #sgemV} (always 0)
	 */
	public int mlpSgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha,
			AtomicReferenceArray<Double> a, int lda, AtomicReferenceArray<Double> x, AtomicInteger incx,
			AtomicReference<Double> beta, AtomicReferenceArray<Double> y, AtomicInteger incy) {
		int ret;
		int t_m, t_n, t_lda, t_incx, t_incy;

		t_m = m;
		t_n = n;
		t_lda = lda;
		t_incx = incx.get();
		t_incy = incy.get();

		ret = sgemV(trans, t_m, t_n, alpha, a, t_lda, x, t_incx, beta, y, t_incy);

		return (ret);
	}

	/**
	 * Wrapper around {@link #sscal} (NIST {@code mlp_sscal}).
	 *
	 * @param n    number of elements
	 * @param sa   scale factor
	 * @param sx   the vector
	 * @param incx increment between elements
	 * @return the value returned by {@link #sscal} (always 0)
	 */
	public int mlpSScal(int n, double sa, AtomicReference<Double> sx, int incx) {
		int ret;
		int t_n, t_incx;

		t_n = n;
		t_incx = incx;

		ret = sscal(t_n, sa, sx, t_incx);

		return (ret);
	}

	/**
	 * Wrapper around {@link #saxpY} (NIST {@code mlp_saxpy}).
	 *
	 * @param n    number of elements
	 * @param sa   scalar multiplier
	 * @param sx   vector x
	 * @param incx increment between elements of x
	 * @param sy   vector y
	 * @param incy increment between elements of y
	 * @return the value returned by {@link #saxpY} (always 0)
	 */
	public int mlpSaxpY(int n, double sa, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		int ret;
		int t_n, t_incx, t_incy;

		t_n = n;
		t_incx = incx;
		t_incy = incy;

		ret = saxpY(t_n, sa, sx, t_incx, sy, t_incy);

		return (ret);
	}

	/**
	 * Wrapper around {@link #sDot} (NIST {@code mlp_sdot}).
	 *
	 * @param n    number of elements
	 * @param sx   vector x
	 * @param incx increment between elements of x
	 * @param sy   vector y
	 * @param incy increment between elements of y
	 * @return the value returned by {@link #sDot} (always 0)
	 */
	public double mlpSDot(int n, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		double dret;
		double fret;
		int t_n, t_incx, t_incy;

		t_n = n;
		t_incx = incx;
		t_incy = incy;

		dret = sDot(t_n, sx, t_incx, sy, t_incy);

		fret = (double) dret;

		return (fret);
	}

	/**
	 * Wrapper around {@link #snRm2} (NIST {@code mlp_snrm2}).
	 *
	 * @param n    number of elements
	 * @param x    the vector
	 * @param incx increment between elements
	 * @return the value returned by {@link #snRm2} (always 0)
	 */
	public double mlpSnrm2(int n, AtomicReference<Double> x, int incx) {
		double dret;
		double fret;
		int t_n, t_incx;

		t_n = n;
		t_incx = incx;

		dret = snRm2(t_n, x, t_incx);

		fret = (double) dret;

		return (fret);
	}

	/**
	 * LAPACK/BLAS error handler {@code XERBLA}: reports that a routine was called with an illegal parameter.
	 * <p>
	 * Unlike the Fortran original, which stops the program, this only logs the message.
	 *
	 * @param srName name of the routine that detected the error, e.g. {@code "SGEMV "}
	 * @param info   position of the invalid parameter in the routine's parameter list
	 * @return always {@link ILfs#FALSE} (0)
	 */
	private int xerbla_(String srName, int info) {
		/*
		 * %2ld added by MDG on 03-09-05 changed to %2d by JCK on 2009-02-02 because of
		 * change in f2c.h
		 */
		logger.error("** On entry to {}, parameter number {} had an illegal value", srName, info);
		return ILfs.FALSE;
	}

	/**
	 * {@code compareChars} scratch: {@code inta} and {@code intb} hold the (case-folded) codes of the two
	 * characters; {@code zcode} holds the code of 'Z', used to detect the character set.
	 */
	private static int inta, intb, zcode;

	/**
	 * LAPACK auxiliary routine {@code LSAME} (version 2.0, Univ. of Tennessee, Univ. of California Berkeley,
	 * NAG Ltd., Courant Institute, Argonne National Lab and Rice University, September 30, 1994).
	 * <p>
	 * Returns {@code true} if CA is the same letter as CB regardless of case. The characters are first compared
	 * directly. If they differ, both are case-folded according to the character set detected from the code of
	 * 'Z': ASCII (90/122), EBCDIC (233/169) or Prime-machine ASCII (218/250).
	 *
	 * @param ca holder for the first single character to compare
	 * @param cb the second single character to compare
	 * @return {@code true} if both are the same letter, ignoring case
	 */
	private boolean compareChars(AtomicReference<Character> ca, char cb) {
		/*
		 * -- LAPACK auxiliary routine (version 2.0) -- Univ. of Tennessee, Univ. of
		 * California Berkeley, NAG Ltd., Courant Institute, Argonne National Lab, and
		 * Rice University September 30, 1994 Purpose ======= compareChars returns
		 * .TRUE. if CA is the same letter as CB regardless of case. Arguments =========
		 * CA (input) CHARACTER*1 CB (input) CHARACTER*1 CA and CB specify the single
		 * characters to be compared.
		 * =====================================================================
		 * 
		 * Test if the characters are equal
		 */
		/* System generated locals */
		boolean ret_val = false;
		ret_val = ca.get() == cb;
		if (ret_val) {
			return ret_val;
		}

		/* Now test for equivalence if both characters are alphabetic. */

		zcode = 'Z';

		/*
		 * Use 'Z' rather than 'A' so that ASCII can be detected on Prime machines, on
		 * which ICHAR returns a value with bit 8 set. ICHAR('A') on Prime machines
		 * returns 193 which is the same as ICHAR('A') on an EBCDIC machine.
		 */

		inta = ca.get();
		intb = cb;

		if (zcode == 90 || zcode == 122) {
			/*
			 * ASCII is assumed - ZCODE is the ASCII code of either lower or upper case 'Z'.
			 */
			if (inta >= 97 && inta <= 122) {
				inta += -32;
			}
			if (intb >= 97 && intb <= 122) {
				intb += -32;
			}
		} else if (zcode == 233 || zcode == 169) {
			/*
			 * EBCDIC is assumed - ZCODE is the EBCDIC code of either lower or upper case
			 * 'Z'.
			 */

			/* Parentheses added by MDG on 03-09-05 */
			if ((inta >= 129 && inta <= 137) || (inta >= 145 && inta <= 153) || (inta >= 162 && inta <= 169)) {
				inta += 64;
			}
			/* Parentheses added by MDG on 03-09-05 */
			if ((intb >= 129 && intb <= 137) || (intb >= 145 && intb <= 153) || (intb >= 162 && intb <= 169)) {
				intb += 64;
			}
		} else if (zcode == 218 || zcode == 250) {

			/*
			 * ASCII is assumed, on Prime machines - ZCODE is the ASCII code plus 128 of
			 * either lower or upper case 'Z'.
			 */
			if (inta >= 225 && inta <= 250) {
				inta += -32;
			}
			if (intb >= 225 && intb <= 250) {
				intb += -32;
			}
		}
		ret_val = inta == intb;

		/* RETURN End of compareChars */

		return ret_val;
	} // compareChars
}