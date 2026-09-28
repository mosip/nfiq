package org.mosip.nist.nfiq1.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * BLAS-style linear-algebra routines used by the MLP classifier (port of NIST's {@code mlp/cla} CBLAS subset:
 * {@code sgemv}, {@code sscal}, {@code saxpy}, {@code sdot}, {@code snrm2} and their {@code mlp_*} wrappers).
 * <p>
 * All routines operate on single/double-precision vectors with stride ({@code inc*}) semantics as in the
 * reference BLAS.
 */
public interface IMlpCla {
	/**
	 * Matrix-vector product {@code y = alpha * op(A) * x + beta * y} (BLAS {@code sgemv}).
	 *
	 * @param trans {@code 'N'} for {@code op(A) = A}, {@code 'T'} or {@code 'C'} for {@code op(A) = A}-transpose
	 * @param m     number of rows of {@code A}
	 * @param n     number of columns of {@code A}
	 * @param alpha scalar multiplier for {@code op(A) * x}
	 * @param a     matrix {@code A} stored column-major with leading dimension {@code lda}
	 * @param lda   leading dimension of {@code A} (at least {@code max(1, m)})
	 * @param x     input vector {@code x}
	 * @param incx  stride between elements of {@code x}
	 * @param beta  scalar multiplier for the input {@code y}
	 * @param y     input/output vector {@code y}; overwritten with the result
	 * @param incy  stride between elements of {@code y}
	 * @return {@code 0} on success, non-zero if an argument is invalid
	 */
	public int sgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
			AtomicReferenceArray<Double> a, int lda,
			AtomicReferenceArray<Double> x, int incx, AtomicReference<Double> beta, 
			AtomicReferenceArray<Double> y, int incy);
	/**
	 * Scales a vector by a constant: {@code sx = sa * sx} (BLAS {@code sscal}).
	 *
	 * @param n    number of elements
	 * @param sa   scale factor
	 * @param sx   vector to scale; updated in place
	 * @param incx stride between elements of {@code sx}
	 * @return {@code 0} on success
	 */
	public int sscal(int n, double sa, AtomicReference<Double> sx, int incx);
	/**
	 * Constant times a vector plus a vector: {@code sy = sa * sx + sy} (BLAS {@code saxpy}).
	 *
	 * @param n    number of elements
	 * @param sa   scalar multiplier for {@code sx}
	 * @param sx   input vector
	 * @param incx stride between elements of {@code sx}
	 * @param sy   input/output vector; updated in place
	 * @param incy stride between elements of {@code sy}
	 * @return {@code 0} on success
	 */
	public int saxpY(int n, double sa, AtomicReference<Double> sx, 
			int incx, AtomicReference<Double> sy, int incy);
	/**
	 * Dot product of two vectors (BLAS {@code sdot}).
	 *
	 * @param n    number of elements
	 * @param sx   first vector
	 * @param incx stride between elements of {@code sx}
	 * @param sy   second vector
	 * @param incy stride between elements of {@code sy}
	 * @return the dot product {@code sum(sx[i] * sy[i])}
	 */
	public double sDot(int n, AtomicReference<Double> sx, int incx, 
			AtomicReference<Double> sy, int incy);
	/**
	 * Euclidean norm of a vector (BLAS {@code snrm2}).
	 *
	 * @param n    number of elements
	 * @param x    input vector
	 * @param incx stride between elements of {@code x}
	 * @return {@code sqrt(sum(x[i]^2))}
	 */
	public double snRm2(int n, AtomicReference<Double> x, int incx);
	
	/**
	 * MLP wrapper for {@link #sgemV}: {@code y = alpha * op(A) * x + beta * y}.
	 *
	 * @param trans {@code 'N'} for {@code op(A) = A}, {@code 'T'} or {@code 'C'} for {@code op(A) = A}-transpose
	 * @param m     number of rows of {@code A}
	 * @param n     number of columns of {@code A}
	 * @param alpha scalar multiplier for {@code op(A) * x}
	 * @param a     matrix {@code A} with leading dimension {@code lda}
	 * @param lda   leading dimension of {@code A}
	 * @param x     input vector {@code x}
	 * @param incx  stride between elements of {@code x}
	 * @param beta  scalar multiplier for the input {@code y}
	 * @param y     input/output vector {@code y}; overwritten with the result
	 * @param incy  stride between elements of {@code y}
	 * @return {@code 0} on success, non-zero if an argument is invalid
	 */
	public int mlpSgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
			AtomicReferenceArray<Double> a, int lda, AtomicReferenceArray<Double> x,
			AtomicInteger incx, AtomicReference<Double> beta, AtomicReferenceArray<Double> y, AtomicInteger incy);
	/**
	 * MLP wrapper for {@link #sscal}: {@code sx = sa * sx}.
	 *
	 * @param n    number of elements
	 * @param sa   scale factor
	 * @param sx   vector to scale; updated in place
	 * @param incx stride between elements of {@code sx}
	 * @return {@code 0} on success
	 */
	public int mlpSScal(int n, double sa, AtomicReference<Double> sx, int incx);
	/**
	 * MLP wrapper for {@link #saxpY}: {@code sy = sa * sx + sy}.
	 *
	 * @param n    number of elements
	 * @param sa   scalar multiplier for {@code sx}
	 * @param sx   input vector
	 * @param incx stride between elements of {@code sx}
	 * @param sy   input/output vector; updated in place
	 * @param incy stride between elements of {@code sy}
	 * @return {@code 0} on success
	 */
	public int mlpSaxpY(int n, double sa, AtomicReference<Double> sx, 
		int incx, AtomicReference<Double> sy, int incy);
	/**
	 * MLP wrapper for {@link #sDot}: dot product of two vectors.
	 *
	 * @param n    number of elements
	 * @param sx   first vector
	 * @param incx stride between elements of {@code sx}
	 * @param sy   second vector
	 * @param incy stride between elements of {@code sy}
	 * @return the dot product
	 */
	public double mlpSDot(int n, AtomicReference<Double> sx, int incx, 
		AtomicReference<Double> sy, int incy);
	/**
	 * MLP wrapper for {@link #snRm2}: Euclidean norm of a vector.
	 *
	 * @param n    number of elements
	 * @param x    input vector
	 * @param incx stride between elements of {@code x}
	 * @return the Euclidean norm
	 */
	public double mlpSnrm2(int n, AtomicReference<Double> x, int incx);
}