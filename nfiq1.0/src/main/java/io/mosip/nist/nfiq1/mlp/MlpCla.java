package io.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.IMlpCla;

public class MlpCla extends Mlp implements IMlpCla {
	private static final Logger LOGGER = LoggerFactory.getLogger(MlpCla.class);	
	private static volatile MlpCla instance;
    public static MlpCla getInstance() {
        if (instance == null) {
            synchronized (MlpCla.class) {
                if (instance == null) {
                    instance = new MlpCla();
                }
            }
        }
        return instance;
    }    
    private MlpCla()
    {
    	super();
    }
    
    
    /*  Purpose   
    =======   
    SGEMV  performs one of the matrix-vector operations   
       y := alpha*A*x + beta*y,   or   y := alpha*A'*x + beta*y,   
    where alpha and beta are scalars, x and y are vectors and A is an   
    m by n matrix.   
    Parameters   
    ==========   
    TRANS  - CHARACTER*1.   
             On entry, TRANS specifies the operation to be performed as   
             follows:   
                TRANS = 'N' or 'n'   y := alpha*A*x + beta*y.   
                TRANS = 'T' or 't'   y := alpha*A'*x + beta*y.   
                TRANS = 'C' or 'c'   y := alpha*A'*x + beta*y.   
             Unchanged on exit.   
    M      - INTEGER.   
             On entry, M specifies the number of rows of the matrix A.   
             M must be at least zero.   
             Unchanged on exit.   
    N      - INTEGER.   
             On entry, N specifies the number of columns of the matrix A. 
  
             N must be at least zero.   
             Unchanged on exit.   
    ALPHA  - REAL            .   
             On entry, ALPHA specifies the scalar alpha.   
             Unchanged on exit.   
    A      - REAL             array of DIMENSION ( LDA, n ).   
             Before entry, the leading m by n part of the array A must   
             contain the matrix of coefficients.   
             Unchanged on exit.   
    LDA    - INTEGER.   
             On entry, LDA specifies the first dimension of A as declared 
  
             in the calling (sub) program. LDA must be at least   
             max( 1, m ).   
             Unchanged on exit.   
    X      - REAL             array of DIMENSION at least   
             ( 1 + ( n - 1 )*abs( INCX ) ) when TRANS = 'N' or 'n'   
             and at least   
             ( 1 + ( m - 1 )*abs( INCX ) ) otherwise.   
             Before entry, the incremented array X must contain the   
             vector x.   
             Unchanged on exit.   
    INCX   - INTEGER.   
             On entry, INCX specifies the increment for the elements of   
             X. INCX must not be zero.   
             Unchanged on exit.   
    BETA   - REAL            .   
             On entry, BETA specifies the scalar beta. When BETA is   
             supplied as zero then Y need not be set on input.   
             Unchanged on exit.   
    Y      - REAL             array of DIMENSION at least   
             ( 1 + ( m - 1 )*abs( INCY ) ) when TRANS = 'N' or 'n'   
             and at least   
             ( 1 + ( n - 1 )*abs( INCY ) ) otherwise.   
             Before entry with BETA non-zero, the incremented array Y   
             must contain the vector y. On exit, Y is overwritten by the 
  
             updated vector y.   
    INCY   - INTEGER.   
             On entry, INCY specifies the increment for the elements of   
             Y. INCY must not be zero.   
             Unchanged on exit.   
    Level 2 Blas routine.   
       Test the input parameters.   
    
   Parameter adjustments   
       Function Body */
    private static int info;
    private static double temp;
    private static int lenx, leny, i, j;
    private static int ix, iy, jx, jy, kx, ky;
	public int sgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
		AtomicReferenceArray<Double> a, int lda,
		AtomicReferenceArray<Double> x, int incx, AtomicReference<Double> beta, 
		AtomicReferenceArray<Double> y, int incy) {
		/* System generated locals */
		/*  Unused variables commented out by MDG on 03-09-05
		    int a_dim1, a_offset;
		 */
		int i1, i2;

		info = 0;
		if (!compareChars(trans, 'N') && !compareChars(trans, 'T') && !compareChars(trans, 'C'))
		{
			info = 1;
		}
		else if (m < 0)
		{
			info = 2;
		}
		else if (n < 0)
		{
			info = 3;
		}
		else if (lda < Math.max(1, m))
		{
			info = 6;
		}
		else if (incx == 0)
		{
			info = 8;
		}
		else if (incy == 0)
		{
			info = 11;
		}

		if (info != 0)
		{
			xerbla_("SGEMV ", info);
			return 0;
		}
		/*     Quick return if possible. */
		/* if (*m == 0 || *n == 0 || *alpha == 0.f && *beta == 1.f) { */
		/*  Parentesis added by MDG on 03-09-05 */
		if (m == 0 || n == 0 || (alpha.get() == 0.0f && beta.get() == 1.0f))
		{
			return 0;
		}

		/* Set  LENX  and  LENY, the lengths of the vectors x and y, and set 
		up the start points in  X  and  Y. */

		if (compareChars(trans, 'N'))
		{
			lenx = n;
			leny = m;
		}
		else
		{
			lenx = m;
			leny = n;
		}
		if (incx > 0)
		{
			kx = 1;
		}
		else
		{
			kx = 1 - (lenx - 1) * incx;
		}
		if (incy > 0)
		{
			ky = 1;
		}
		else
		{
			ky = 1 - (leny - 1) * incy;
		}

		/* Start the operations. In this version the elements of A are   
	 	accessed sequentially with one pass through A.   
	    First form  y := beta*y. */
		if (beta.get() != 1.0f)
		{
			if (incy == 1)
			{
				if (beta.get() == 0.0f)
				{
					i1 = leny;
					for (i = 1; i <= leny; ++i)
					{
						YSet(y, i, 0.0f);
						/* L10: */
					}
				}
				else
				{
					i1 = leny;
					for (i = 1; i <= leny; ++i)
					{
						YSet(y, i, beta.get() * YGet(y, i));
						/* L20: */
					}
				}
			}
			else
			{
				iy = ky;
				if (beta.get() == 0.0f)
				{
					i1 = leny;
					for (i = 1; i <= leny; ++i)
					{
						YSet(y, iy, 0.0f);
						iy += incy;
						/* L30: */
					}
				}
				else
				{
					i1 = leny;
					for (i = 1; i <= leny; ++i)
					{
						YSet(y, iy, beta.get() * YGet(y, iy));
						iy += incy;
						/* L40: */
					}
				}
			}
		}

		if (alpha.get() == 0.0f)
		{
			return 0;
		}
		if (compareChars(trans, 'N'))
		{
			/* Form  y := alpha*A*x + y. */
			jx = kx;
			if (incy == 1)
			{
				i1 = n;
				for (j = 1; j <= n; ++j)
				{
					if (XGet(x, jx) != 0.0f)
					{
						temp = alpha.get() * XGet(x, jx);
						i2 = m;
						for (i = 1; i <= m; ++i)
						{
							YSetPlus(y, i, temp * AGet(a, i, j, lda));
							/* L50: */
						}
					}
					jx += incx;
					/* L60: */
				}
			}
			else
			{
				i1 = n;
				for (j = 1; j <= n; ++j)
				{
					if (XGet(x, jx) != 0.0f)
					{
						temp = alpha.get() * XGet(x, jx);
						iy = ky;
						i2 = m;
						for (i = 1; i <= m; ++i)
						{
							YSetPlus(y, iy, temp * AGet(a, i, j, lda));
							iy += incy;
							/* L70: */
						}
					}
					jx += incx;
					/* L80: */
				}
			}
		}
		else
		{
			/* Form  y := alpha*A'*x + y. */
			jy = ky;
			if (incx == 1)
			{
				i1 = n;
				for (j = 1; j <= n; ++j)
				{
					temp = 0.0f;
					i2 = m;
					for (i = 1; i <= m; ++i)
					{
						temp += AGet(a, i, j, lda) * XGet(x, i);
						/* L90: */
					}
					YSetPlus(y, jy, alpha.get() * temp);
					jy += incy;
					/* L100: */
				}
			}
			else
			{
				i1 = n;
				for (j = 1; j <= n; ++j)
				{
					temp = 0.0f;
					ix = kx;
					i2 = m;
					for (i = 1; i <= m; ++i)
					{
						temp += AGet(a, i,j, lda) * XGet(x, ix);
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
		/*     End of SGEMV . */
	}

	private double XGet(AtomicReferenceArray<Double> x, int I) 
	{
		return x.get(I -1);
	}

	private void XSetPlus(double [] x, int I, double value) 
	{
		x [I -1] = x [I -1] + value;
	}

	private double YGet(AtomicReferenceArray<Double> y, int I) 
	{
		return y.get(I -1);
	}

	private void YSet(AtomicReferenceArray<Double> y, int I, double value) 
	{
		y.set(I -1, value);
	}

	private void YSetPlus(AtomicReferenceArray<Double> y, int I, double value) 
	{
		y.set(I -1, y.get(I -1) + value);
	}

	private double AGet(AtomicReferenceArray<Double> a, int I, int J, int lda) 
	{
		return a.get((I)-1 + (((J)-1) * (lda)));
	}

	public int sscal(int n, double sa, AtomicReference<Double> sx, int incx) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int saxpY(int n, double sa, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double sDot(int n, AtomicReference<Double> sx, int incx, AtomicReference<Double> sy, int incy) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double snRm2(int n, AtomicReference<Double> x, int incx) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int mlpSgemV(AtomicReference<Character> trans, int m, int n, AtomicReference<Double> alpha, 
		AtomicReferenceArray<Double> a, int lda, AtomicReferenceArray<Double> x,
		AtomicInteger incx, AtomicReference<Double> beta, AtomicReferenceArray<Double> y, AtomicInteger incy) {
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

	public int mlpSScal(int n, double sa, AtomicReference<Double> sx, int incx) {
		int ret;
		int t_n, t_incx;

		t_n = n;
		t_incx = incx;

		ret = sscal(t_n, sa, sx, t_incx);

		return (ret);
	}

	public int mlpSaxpY(int n, double sa, AtomicReference<Double> sx, 
		int incx, AtomicReference<Double> sy, int incy) {
		int ret;
		int t_n, t_incx, t_incy;

		t_n = n;
		t_incx = incx;
		t_incy = incy;

		ret = saxpY(t_n, sa, sx, t_incx, sy, t_incy);

		return (ret);
	}

	public double mlpSDot(int n, AtomicReference<Double> sx, int incx, 
		AtomicReference<Double> sy, int incy) {
		double dret;
		double fret;
		int t_n, t_incx, t_incy;

		t_n = n;
		t_incx = incx;
		t_incy = incy;

		dret = sDot(t_n, sx, t_incx, sy, t_incy);

		fret = (double)dret;

		return (fret);
	}

	public double mlpSnrm2(int n, AtomicReference<Double> x, int incx) {
		double dret;
		double fret;
		int t_n, t_incx;

		t_n = n;
		t_incx = incx;

		dret = snRm2(t_n, x, t_incx);

		fret = (double)dret;

		return (fret);
	}
	
	private int xerbla_(String srName, int info)
	{
		/* %2ld added by MDG on 03-09-05
	       changed to %2d by JCK on 2009-02-02 because of change in f2c.h */
		LOGGER.info(String.format("** On entry to %6s, parameter number %2d had an illegal value\n", srName, info));
	    return ILfs.FALSE;
	}
	
	private static int inta, intb, zcode;
	private boolean compareChars(AtomicReference<Character> ca, char cb)
	{
	/*  -- LAPACK auxiliary routine (version 2.0) --   
	       Univ. of Tennessee, Univ. of California Berkeley, NAG Ltd.,   
	       Courant Institute, Argonne National Lab, and Rice University   
	       September 30, 1994   
	    Purpose   
	    =======   
	    compareChars returns .TRUE. if CA is the same letter as CB regardless of   
	    case.   
	    Arguments   
	    =========   
	    CA      (input) CHARACTER*1   
	    CB      (input) CHARACTER*1   
	            CA and CB specify the single characters to be compared.   
	   ===================================================================== 
	  
	       Test if the characters are equal */
		/* System generated locals */
		boolean ret_val = false;
		ret_val = ca.get() == cb;
		if (ret_val)
		{
			return ret_val;
		}

		/*     Now test for equivalence if both characters are alphabetic. */

		zcode = 'Z';

		/*     Use 'Z' rather than 'A' so that ASCII can be detected on Prime   
	       machines, on which ICHAR returns a value with bit 8 set.   
	       ICHAR('A') on Prime machines returns 193 which is the same as   
	       ICHAR('A') on an EBCDIC machine. */

		inta = ca.get();
		intb = cb;

		if (zcode == 90 || zcode == 122)
		{
			/*        ASCII is assumed - ZCODE is the ASCII code of either lower or   
         	upper case 'Z'. */
			if (inta >= 97 && inta <= 122)
			{
				inta += -32;
			}
			if (intb >= 97 && intb <= 122)
			{
				intb += -32;
			}
		}
		else if (zcode == 233 || zcode == 169)
		{
			/*        EBCDIC is assumed - ZCODE is the EBCDIC code of either lower
	 		or   
	          upper case 'Z'. */

			/*
			if (inta >= 129 && inta <= 137 || inta >= 145 && inta <= 153 || inta >= 162 && inta <= 169) {
		 	*/
			/*      Parentheses added by MDG on 03-09-05 */
			if ((inta >= 129 && inta <= 137) || (inta >= 145 && inta <= 153) || (inta >= 162 && inta <= 169))
			{
				inta += 64;
			}
			/*
			if (intb >= 129 && intb <= 137 || intb >= 145 && intb <= 153 || intb >= 162 && intb <= 169) {
			 */
			/*      Parentheses added by MDG on 03-09-05 */
			if ((intb >= 129 && intb <= 137) || (intb >= 145 && intb <= 153) || (intb >= 162 && intb <= 169))
			{
				intb += 64;
			}
		}
		else if (zcode == 218 || zcode == 250)
		{

			/* ASCII is assumed, on Prime machines - ZCODE is the ASCII code   
	          plus 128 of either lower or upper case 'Z'. */
			if (inta >= 225 && inta <= 250)
			{
				inta += -32;
			}
			if (intb >= 225 && intb <= 250)
			{
				intb += -32;
			}
		}
		ret_val = inta == intb;

		/*     RETURN    End of compareChars */

		return ret_val;
	} // compareChars
}


