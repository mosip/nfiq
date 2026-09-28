package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IChainCode;

/**
 * 8-connected chain code utilities for feature contours in MINDTCT.
 * <p>
 * Port of NIST LFS {@code chaincod.c} ({@code chain_code_loop}, {@code is_chain_clockwise}). Minutia
 * detection uses chain codes to tell whether a traced contour loop (for example around a lake or island) runs
 * clockwise or counter-clockwise.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state.
 */
public class ChainCode extends MindTct implements IChainCode {
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static ChainCode instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private ChainCode() {
		super();
	}

	/**
	 * Returns the shared {@code ChainCode} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized ChainCode getInstance() {
		if (instance == null) {
			instance = new ChainCode();
		}
		return instance;
	}

	/**
	 * Returns the MINDTCT global lookup tables (including the 8-neighbour chain code table).
	 *
	 * @return the {@link Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Converts a feature's closed contour into an 8-connected chain code vector (NIST {@code chain_code_loop}).
	 * <p>
	 * The encoding records the direction taken between each pair of adjacent contour points, including the step
	 * from the last point back to the first, which closes the loop. Each code is looked up in
	 * {@code Globals.getChaincodesNbr8()} from the neighbour deltas {@code (dx, dy)}, which lie in [-1, 1].
	 * Chain codes have many uses, such as computing the perimeter or area of an object, and object detection
	 * and recognition. Contours with 3 or fewer points are not treated as loops: the chain length is set to 0.
	 *
	 * @param oVectorChainCodes   output: receives the chain codes; must hold at least {@code noOfPointsInContour}
	 *                            elements
	 * @param oNoOfCodesInChain   output: number of codes in the chain (equal to the number of contour points, or
	 *                            0 if the contour is too short)
	 * @param oContourX           x-coordinates of the contour points
	 * @param oContourY           y-coordinates of the contour points
	 * @param noOfPointsInContour number of points in the contour
	 * @return always 0 ({@link ILfs#FALSE}), meaning the chain code was derived successfully (the NIST original
	 *         could also return a negative system error)
	 */
	@SuppressWarnings({ "java:S3516" })
	public int chainCodeLoop(AtomicIntegerArray oVectorChainCodes, AtomicInteger oNoOfCodesInChain,
			AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, int noOfPointsInContour) {
		int index;
		int nextIndex;
		int dx;
		int dy;

		/* If we don't have at least 3 points in the contour ... */
		if (noOfPointsInContour <= 3) {
			/* Then we don't have a loop, so set chain length to 0 */
			/* and return without any allocations. */
			oNoOfCodesInChain.set(ILfs.FALSE);
			return (ILfs.FALSE);
		}

		/* Allocate chain code vector. It will be the same length as the */
		/* number of points in the contour. There will be one chain code */
		/* between each point on the contour including a code between the */
		/* last to the first point on the contour (completing the loop). */

		/* For each neighboring point in the list (with "i" pointing to the */
		/* previous neighbor and "j" pointing to the next neighbor... */
		for (index = 0, nextIndex = 1; index < noOfPointsInContour - 1; index++, nextIndex++) {
			/* Compute delta in X between neighbors. */
			dx = oContourX.get(nextIndex) - oContourX.get(index);
			/* Compute delta in Y between neighbors. */
			dy = oContourY.get(nextIndex) - oContourY.get(index);
			/* Derive chain code index from neighbor deltas. */
			/* The deltas are on the range [-1..1], so to use them as indices */
			/* into the code list, they must first be incremented by one. */
			oVectorChainCodes.set(index, getGlobals().getChaincodesNbr8()[0 + ((dy + 1) * ILfs.NBR8_DIM) + dx + 1]);
		}

		/* Now derive chain code between last and first points in the */
		/* contour list. */
		dx = oContourX.get(0) - oContourX.get(index);
		dy = oContourY.get(0) - oContourY.get(index);
		oVectorChainCodes.set(index, getGlobals().getChaincodesNbr8()[0 + ((dy + 1) * ILfs.NBR8_DIM) + dx + 1]);

		/* Store results to the output pointers. */
		oNoOfCodesInChain.set(noOfPointsInContour);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Determines whether an 8-connected chain code vector runs clockwise or counter-clockwise (NIST
	 * {@code is_chain_clockwise}).
	 * <p>
	 * Adds up the signed change in direction between consecutive codes (including the last-to-first pair),
	 * after normalizing each change to the range [-3, 4]. Left-hand turns add and right-hand turns subtract. A
	 * negative total means clockwise, a positive total counter-clockwise, and zero means the order cannot be
	 * determined. The caller supplies the return value for that undetermined case, so the default response can
	 * be application-specific.
	 *
	 * @param oVectorChainCodes the chain code vector
	 * @param noOfCodesInChain  number of codes in the chain
	 * @param defaultRetCode    value to return when the order cannot be determined
	 * @return {@link ILfs#TRUE} if the chain is clockwise; {@link ILfs#FALSE} if it is counter-clockwise;
	 *         {@code defaultRetCode} if the order could not be determined
	 */
	public int isChainClockwise(AtomicIntegerArray oVectorChainCodes, int noOfCodesInChain, int defaultRetCode) {
		int i;
		int j;
		int d;
		int sum;

		/* Initialize turn-accumulator to 0. */
		sum = 0;

		/* Foreach neighboring code in chain, compute the difference in */
		/* direction and accumulate. Left-hand turns increment, whereas */
		/* right-hand decrement. */
		for (i = 0, j = 1; i < noOfCodesInChain - 1; i++, j++) {
			/* Compute delta in neighbor direction. */
			d = oVectorChainCodes.get(j) - oVectorChainCodes.get(i);
			/* Make the delta the "inner" distance. */
			/* If delta >= 4, for example if chain_i==2 and chain_j==7 (which */
			/* means the contour went from a step up to step down-to-the-right) */
			/* then 5=(7-2) which is >=4, so -3=(5-8) which means that the */
			/* change in direction is a righ-hand turn of 3 units). */
			if (d >= 4) {
				d -= 8;
			}
			/* If delta <= -4, for example if chain_i==7 and chain_j==2 (which */
			/* means the contour went from a step down-to-the-right to step up) */
			/* then -5=(2-7) which is <=-4, so 3=(-5+8) which means that the */
			/* change in direction is a left-hand turn of 3 units). */
			else if (d <= -4) {
				d += 8;
			}

			/* The delta direction is then accumulated. */
			sum += d;
		}

		/* Now we need to add in the final delta direction between the last */
		/* and first codes in the chain. */
		d = oVectorChainCodes.get(0) - oVectorChainCodes.get(i);
		if (d >= 4) {
			d -= 8;
		} else if (d <= -4) {
			d += 8;
		}
		sum += d;

		/* If the final turn_accumulator == 0, then we CAN'T TELL the */
		/* direction of the chain code, so return the default return value. */
		if (sum == 0) {
			return (defaultRetCode);
		}
		/* Otherwise, if the final turn-accumulator is positive ... */
		else if (sum > 0) {
			/* Then we had a greater amount of left-hand turns than right-hand */
			/* turns, so the chain is in COUNTER-CLOCKWISE order, so return FALSE. */
			return (ILfs.FALSE);
		}
		/* Otherwise, the final turn-accumulator is negative ... */
		else {
			/* So we had a greater amount of right-hand turns than left-hand */
			/* turns, so the chain is in CLOCKWISE order, so return TRUE. */
			return (ILfs.TRUE);
		}
	}
}