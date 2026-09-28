package org.mosip.nist.nfiq1;

import org.mosip.nist.nfiq1.common.IDefs;
import org.mosip.nist.nfiq1.common.IMlp.TDACHAR;
import org.mosip.nist.nfiq1.common.IMlp.TDAFLOAT;
import org.mosip.nist.nfiq1.common.IMlp.TDAINT;

/**
 * Java port of the numeric helper macros from NIST's {@code defs.h} (and the MLP {@code e()} indexing macro).
 * <p>
 * Implemented as a lazily-created singleton obtained through {@link #getInstance()}; the instance is
 * stateless, so it is safe to share across threads once created.
 */
public class Defs extends Nist implements IDefs {
	/** Lazily-initialized singleton instance. */
	private static Defs instance;

	/** Private constructor; use {@link #getInstance()}. */
	private Defs() {
		super();
	}

	/**
	 * Returns the process-wide {@code Defs} singleton, creating it on first use.
	 * <p>
	 * The method is {@code synchronized}, so initialization is thread-safe.
	 *
	 * @return the shared {@code Defs} instance
	 */
	public static synchronized Defs getInstance() {
		if (instance == null) {
			instance = new Defs();
		}
		return instance;
	}

	/**
	 * Returns the floating-point remainder of {@code a} divided by {@code b} (C {@code fmod}).
	 *
	 * @param a dividend
	 * @param b divisor
	 * @return {@code a % b}, with the sign of {@code a}
	 */
	public double fMod(double a, double b) {
		return a % b;
	}

	/**
	 * Converts an angle from degrees to radians.
	 *
	 * @param deg angle in degrees
	 * @return {@code deg * PI / 180}
	 */
	public double degToRad(double deg) {
		return deg * degToRad();
	}

	/**
	 * Returns the degrees-to-radians conversion factor ({@code PI / 180}), mirroring NIST's {@code deg2rad} macro.
	 *
	 * @return radians per degree
	 */
	public double degToRad() {
		return (Math.PI / 180.0);
	}

	/**
	 * Returns the larger of two values (NIST {@code max} macro).
	 *
	 * @param a first value
	 * @param b second value
	 * @return {@code Math.max(a, b)}
	 */
	public double max(double a, double b) {
		return Math.max(a, b);
	}

	/**
	 * Returns the smaller of two values (NIST {@code min} macro).
	 *
	 * @param a first value
	 * @param b second value
	 * @return {@code Math.min(a, b)}
	 */
	public double min(double a, double b) {
		return Math.min(a, b);
	}

	/**
	 * Rounds to the nearest integer with halves rounded away from zero (NIST {@code sround} macro).
	 *
	 * @param x value to round
	 * @return {@code (int)(x + 0.5)} for non-negative {@code x}, {@code (int)(x - 0.5)} otherwise
	 */
	public int sRound(double x) {
		return ((int) (((x) < 0) ? (x) - 0.5 : (x) + 0.5));
	}

	/**
	 * Rounds to the nearest {@code long} with halves rounded away from zero ({@code long} variant of
	 * {@code sround}).
	 *
	 * @param x value to round
	 * @return {@code (long)(x + 0.5)} for non-negative {@code x}, {@code (long)(x - 0.5)} otherwise
	 */
	public long sRoundLong(double x) {
		return ((long) (((x) < 0) ? (x) - 0.5 : (x) + 0.5));
	}

	/**
	 * Rounds a value up to the next multiple of 16 (NIST {@code align_to_16}).
	 *
	 * @param value value to align (expected non-negative)
	 * @return the smallest multiple of 16 that is greater than or equal to {@code value}
	 */
	public int alignTo16(int value) {
		return ((((value) + 15) >> 4) << 4);
	}

	/**
	 * Rounds a value up to the next multiple of 32 (NIST {@code align_to_32}).
	 *
	 * @param value value to align (expected non-negative)
	 * @return the smallest multiple of 32 that is greater than or equal to {@code value}
	 */
	public int alignTo32(int value) {
		return ((((value) + 31) >> 5) << 5);
	}

	/**
	 * Truncates floating-point precision by scaling, rounding and rescaling (NIST {@code trunc_dbl_precision}).
	 * <p>
	 * Used with {@code ILfs.TRUNC_SCALE} so that results are consistent across architectures.
	 *
	 * @param x     value to truncate
	 * @param scale scale factor (e.g. {@code 16384.0})
	 * @return {@code round(x * scale) / scale}, with halves rounded away from zero
	 */
	public double truncDoublePrecision(double x, double scale) {
		return (((x) < 0.0) ? ((int) (((x) * (scale)) - 0.5)) / (scale) : ((int) (((x) * (scale)) + 0.5)) / (scale));
	}

	// MLP
	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional character array (MLP {@code e()}
	 * macro), offset by the numeric value of the buffer.
	 * <p>
	 * The buffer string is parsed as an integer base offset; a {@link NumberFormatException} is thrown if it is
	 * not numeric.
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return {@code base + i * dim2 + j}, as a {@code double}
	 */
	public double e(TDACHAR tda, int i, int j) {
		return (Integer.parseInt(tda.getBuf()) + (i) * (tda.getDim2()) + (j));
	}

	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional integer array (MLP {@code e()} macro),
	 * offset by the value held in the buffer.
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return {@code base + i * dim2 + j}, as a {@code double}
	 */
	public double e(TDAINT tda, int i, int j) {
		return (tda.getBuf().intValue() + (i) * (tda.getDim2()) + (j));
	}

	/**
	 * Computes the flat index of element {@code (i, j)} of a two-dimensional float array (MLP {@code e()} macro),
	 * offset by the value held in the buffer.
	 *
	 * @param tda two-dimensional array descriptor ({@code dim2} = row length)
	 * @param i   row index
	 * @param j   column index
	 * @return {@code base + i * dim2 + j}, as a {@code double}
	 */
	public double e(TDAFLOAT tda, int i, int j) {
		return (tda.getBuf().get() + (i) * (tda.getDim2()) + (j));
	}
}