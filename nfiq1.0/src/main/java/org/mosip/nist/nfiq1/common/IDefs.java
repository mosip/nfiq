package org.mosip.nist.nfiq1.common;

import org.mosip.nist.nfiq1.common.IMlp.TDACHAR;
import org.mosip.nist.nfiq1.common.IMlp.TDAFLOAT;
import org.mosip.nist.nfiq1.common.IMlp.TDAINT;

/**
 * Contract for the numeric helper macros ported from NIST's {@code defs.h} plus the MLP two-dimensional array
 * indexing macro {@code e()}; implemented by {@code org.mosip.nist.nfiq1.Defs}.
 */
public interface IDefs {
	/**
	 * Returns the floating-point remainder of {@code a} divided by {@code b} (C {@code fmod}).
	 *
	 * @param a dividend
	 * @param b divisor
	 * @return the remainder of {@code a / b}, with the sign of {@code a}
	 */
	public double fMod(double a, double b);

	/**
	 * Converts an angle from degrees to radians.
	 *
	 * @param deg angle in degrees
	 * @return {@code deg * PI / 180}
	 */
	public double degToRad(double deg);
	/**
	 * Returns the degrees-to-radians conversion factor ({@code PI / 180}).
	 *
	 * @return radians per degree
	 */
	public double degToRad();

	/**
	 * Returns the larger of two values.
	 *
	 * @param a first value
	 * @param b second value
	 * @return the maximum of {@code a} and {@code b}
	 */
	public double max(double a, double b);

	/**
	 * Returns the smaller of two values.
	 *
	 * @param a first value
	 * @param b second value
	 * @return the minimum of {@code a} and {@code b}
	 */
	public double min(double a, double b);
	
	/**
	 * Rounds to the nearest {@code int}, with halves rounded away from zero (NIST {@code sround}).
	 *
	 * @param x value to round
	 * @return the rounded value
	 */
	public int sRound(double x);

	/**
	 * Rounds to the nearest {@code long}, with halves rounded away from zero.
	 *
	 * @param x value to round
	 * @return the rounded value
	 */
	public long sRoundLong(double x);
	
	/**
	 * Rounds a value up to the next multiple of 16.
	 *
	 * @param value value to align (expected non-negative)
	 * @return the smallest multiple of 16 greater than or equal to {@code value}
	 */
	public int alignTo16(int value);

	/**
	 * Rounds a value up to the next multiple of 32.
	 *
	 * @param value value to align (expected non-negative)
	 * @return the smallest multiple of 32 greater than or equal to {@code value}
	 */
	public int alignTo32(int value) ;
	
	/**
	 * Truncates floating-point precision by scaling, rounding and rescaling (NIST {@code trunc_dbl_precision}).
	 *
	 * @param x     value to truncate
	 * @param scale scale factor (e.g. {@code ILfs.TRUNC_SCALE})
	 * @return {@code round(x * scale) / scale}
	 */
	public double truncDoublePrecision(double x, double scale);

	//MLP
	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional character array (MLP {@code e()}).
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return the buffer base offset plus {@code i * dim2 + j}
	 */
	public double e(TDACHAR tda, int i, int j);
	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional integer array (MLP {@code e()}).
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return the buffer base offset plus {@code i * dim2 + j}
	 */
	public double e(TDAINT tda, int i, int j);
	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional float array (MLP {@code e()}).
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return the buffer base offset plus {@code i * dim2 + j}
	 */
	public double e(TDAFLOAT tda, int i, int j);

}
