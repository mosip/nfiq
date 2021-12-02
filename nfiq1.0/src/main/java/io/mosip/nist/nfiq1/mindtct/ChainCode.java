package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IChainCode;

public class ChainCode extends MindTct implements IChainCode {
	private static volatile ChainCode instance;
    public static ChainCode getInstance() {
        if (instance == null) {
            synchronized (ChainCode.class) {
                if (instance == null) {
                    instance = new ChainCode();
                }
            }
        }
        return instance;
    }    
    private ChainCode()
    {
    	super();
    }
    
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/*************************************************************************
	**************************************************************************
	#cat: chainCodeLoop - Converts a feature's contour points into an
	#cat:            8-connected chain code vector.  This encoding represents
	#cat:            the direction taken between each adjacent point in the
	#cat:            contour.  Chain codes may be used for many purposes, such
	#cat:            as computing the perimeter or area of an object, and they
	#cat:            may be used in object detection and recognition.
	   Input:
	      oContourX - x-coord list for feature's contour points
	      oContourY - y-coord list for feature's contour points
	      noOfPointsInContour  - number of points in contour
	   Output:
	      oVectorChainCodes    - resulting vector of chain codes
	      oNoOfCodesInChain   - number of codes in chain
	                  (same as number of points in contour)
	   Return Code:
	      Zero      - chain code successful derived
	      Negative  - system error
	**************************************************************************/
	public int chainCodeLoop(AtomicIntegerArray oVectorChainCodes, AtomicInteger oNoOfCodesInChain, 
		AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, int noOfPointsInContour) {
		int index, nextIndex, dx, dy;

		/* If we don't have at least 3 points in the contour ... */
		if (noOfPointsInContour <= 3)
		{
			/* Then we don't have a loop, so set chain length to 0 */
			/* and return without any allocations.                 */
			oNoOfCodesInChain.set (ILfs.FALSE);
			return (ILfs.FALSE);
		}

		/* Allocate chain code vector.  It will be the same length as the */
		/* number of points in the contour.  There will be one chain code */
		/* between each point on the contour including a code between the */
		/* last to the first point on the contour (completing the loop).  */
		// Do in calling fucntion init chain 
		//chain = (int)malloc(noOfPointsInContour);

		/* For each neighboring point in the list (with "i" pointing to the */
		/* previous neighbor and "j" pointing to the next neighbor...       */
		for (index = 0, nextIndex = 1; index < noOfPointsInContour - 1; index++, nextIndex++)
		{
			/* Compute delta in X between neighbors. */
			dx = oContourX.get(nextIndex) - oContourX.get(index);
			/* Compute delta in Y between neighbors. */
			dy = oContourY.get(nextIndex) - oContourY.get(index);
			/* Derive chain code index from neighbor deltas.                  */
			/* The deltas are on the range [-1..1], so to use them as indices */
			/* into the code list, they must first be incremented by one.     */
			oVectorChainCodes.set(index, getGlobals().getChaincodesNbr8()[0 + ((dy + 1) * ILfs.NBR8_DIM) + dx + 1]);
		}

		/* Now derive chain code between last and first points in the */
		/* contour list.                                              */
		dx = oContourX.get(0) - oContourX.get(index);
		dy = oContourY.get(0) - oContourY.get(index);
		oVectorChainCodes.set(index, getGlobals().getChaincodesNbr8()[0 + ((dy + 1) * ILfs.NBR8_DIM) + dx + 1]);

		/* Store results to the output pointers. */
		oNoOfCodesInChain.set(noOfPointsInContour);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: isChainClockwise - Takes an 8-connected chain code vector and
	#cat:            determines if the codes are ordered clockwise or
	#cat:            counter-clockwise.
	#cat:            The routine also requires a default return value be
	#cat:            specified in the case the the routine is not able to
	#cat:            definitively determine the chains direction.  This allows
	#cat:            the default response to be application-specific.
	   Input:
	      oVectorChainCodes       	- chain code vector
	      noOfCodesInChain      	- number of codes in chain
	      defaultRetCode			- default return code (used when we can't tell the order)
	   Return Code:
	      TRUE      - chain determined to be ordered clockwise
	      FALSE     - chain determined to be ordered counter-clockwise
	      Default   - could not determine the order of the chain
	**************************************************************************/
	public int isChainClockwise(AtomicIntegerArray oVectorChainCodes, int noOfCodesInChain, int defaultRetCode) {
		 int i, j, d, sum;

		 /* Initialize turn-accumulator to 0. */
		 sum = 0;

		 /* Foreach neighboring code in chain, compute the difference in  */
		 /* direction and accumulate.  Left-hand turns increment, whereas */
		 /* right-hand decrement.                                         */
		 for (i = 0, j = 1; i < noOfCodesInChain - 1; i++, j++)
		 {
			 /* Compute delta in neighbor direction. */
			 d = oVectorChainCodes.get(j) - oVectorChainCodes.get(i);
			 /* Make the delta the "inner" distance. */
			 /* If delta >= 4, for example if chain_i==2 and chain_j==7 (which   */
			 /* means the contour went from a step up to step down-to-the-right) */
			 /* then 5=(7-2) which is >=4, so -3=(5-8) which means that the      */
			 /* change in direction is a righ-hand turn of 3 units).             */
			 if (d >= 4)
			 {
				 d -= 8;
			 }
			 /* If delta <= -4, for example if chain_i==7 and chain_j==2 (which  */
			 /* means the contour went from a step down-to-the-right to step up) */
			 /* then -5=(2-7) which is <=-4, so 3=(-5+8) which means that the    */
			 /* change in direction is a left-hand turn of 3 units).             */
			 else if (d <= -4)
			 {
				 d += 8;
			 }

			 /* The delta direction is then accumulated. */
			 sum += d;
		 }

		 /* Now we need to add in the final delta direction between the last */
		 /* and first codes in the chain.                                    */
		 d = oVectorChainCodes.get(0) - oVectorChainCodes.get(i);
		 if (d >= 4)
		 {
			d -= 8;
		 }
		 else if (d <= -4)
		 {
			d += 8;
		 }
		 sum += d;

		 /* If the final turn_accumulator == 0, then we CAN'T TELL the       */
		 /* direction of the chain code, so return the default return value. */
		 if (sum == 0)
		 {
			return (defaultRetCode);
		 }
		 /* Otherwise, if the final turn-accumulator is positive ... */
		 else if (sum > 0)
		 {
			 /* Then we had a greater amount of left-hand turns than right-hand     */
			 /* turns, so the chain is in COUNTER-CLOCKWISE order, so return FALSE. */
			 return (ILfs.FALSE);
		 }
		 /* Otherwise, the final turn-accumulator is negative ... */
		 else
		 {
			/* So we had a greater amount of right-hand turns than left-hand  */
			/* turns, so the chain is in CLOCKWISE order, so return TRUE.     */
			return (ILfs.TRUE);
		 }
	}
}


