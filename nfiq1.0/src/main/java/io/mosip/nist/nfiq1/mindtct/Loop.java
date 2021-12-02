package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.ILoop;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;
import io.mosip.nist.nfiq1.common.ILfs.Minutia;
import io.mosip.nist.nfiq1.common.ILfs.Minutiae;
import io.mosip.nist.nfiq1.common.ILfs.Shape;

public class Loop extends MindTct implements ILoop {
	private static final Logger LOGGER = LoggerFactory.getLogger(Loop.class);	
	private static volatile Loop instance;
    public static Loop getInstance() {
        if (instance == null) {
            synchronized (Loop.class) {
                if (instance == null) {
                    instance = new Loop();
                }
            }
        }
        return instance;
    }    
    private Loop()
    {
    	super();
    }
    
	public Shapes getShapes() {
		return Shapes.getInstance();
	}
	public ChainCode getChainCode() {
		return ChainCode.getInstance();
	}
	public Free getFree() {
		return Free.getInstance();
	}
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}
	public Contour getContour() {
		return Contour.getInstance();
	}
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/*************************************************************************
	**************************************************************************
	#cat: getLoopList - Takes a list of minutia points and determines which
	#cat:                ones lie on loops around valleys (lakes) of a specified
	#cat:                maximum circumference.  The routine returns a list of
	#cat:                flags, one for each minutia in the input list, and if
	#cat:                the minutia is on a qualifying loop, the corresponding
	#cat:                flag is set to TRUE, otherwise it is set to FALSE.
	#cat:                If for some reason it was not possible to trace the
	#cat:                minutia's contour, then it is removed from the list.
	#cat:                This can occur due to edits dynamically taking place
	#cat:                in the image by other routines.
	   Input:
	      oMinutiae  - list of true and false oMinutiae
	      loopLen  - maximum size of loop searched for
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	   Output:
	      onloop   - loop flags: TRUE == loop, FALSE == no loop
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int getLoopList(AtomicIntegerArray onloop, AtomicReference<Minutiae> oMinutiae, final int loopLen, 
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int i;
		int ret;
		Minutia minutia;

		/* Allocate a list of onloop flags (one for each minutia in list). */
		//Allocate from calling function

		i = 0;
		/* Foreach minutia remaining in list ... */
		while (i < oMinutiae.get().getNum())
		{
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(i);
			/* If current minutia is a bifurcation ... */
			if (minutia.getType() == ILfs.BIFURCATION)
			{
				/* Check to see if it is on a loop of specified length. */
				ret = onLoop(minutia, loopLen, binarizedImageData, imageWidth, imageHeight);
				/* If minutia is on a loop... */
				if (ret == ILfs.LOOP_FOUND)
				{
					/* Then set the onloop flag to TRUE. */
					onloop.set(i, ILfs.TRUE);
					/* Advance to next minutia in the list. */
					i++;
				}
				/* If on loop test IGNORED ... */
				else if (ret == ILfs.IGNORE)
				{
					/* Remove the current minutia from the list. */
					if ((ret = getMinutiaHelper().removeMinutia(i, oMinutiae)) != 0)
					{
						/* Return error code. */
						return (ret);
					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'.                   */
				}
				/* If the minutia is NOT on a loop... */
				else if (ret == ILfs.FALSE)
				{
					/* Then set the onloop flag to FALSE. */
					onloop.set(i, ILfs.FALSE);
					/* Advance to next minutia in the list. */
					i++;
				}
				/* Otherwise, an ERROR occurred while looking for loop. */
				else
				{
					/* Return error code. */
					return (ret);
				}
			}
			/* Otherwise, the current minutia is a ridge-ending... */
			else
			{
				/* Ridge-endings will never be on a loop, so set flag to FALSE. */
				onloop.set(i, ILfs.FALSE);
				/* Advance to next minutia in the list. */
				i++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: onLoop - Determines if a minutia point lies on a loop (island or lake)
	#cat:           of specified maximum circumference.
	   Input:
	      minutia      - list of true and false minutia
	      maxLoopLen  - maximum size of loop searched for
	      binarizedImageData         - binary image data (0==while & 1==black)
	      imageWidth            - width (in pixels) of image
	      imageHeight            - height (in pixels) of image
	   Return Code:
	      IGNORE     - minutia contour could not be traced
	      LOOP_FOUND - minutia determined to lie on qualifying loop
	      FALSE      - minutia determined not to lie on qualifying loop
	      Negative   - system error
	**************************************************************************/
	public int onLoop(final Minutia minutia, final int maxLoopLen, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight) {
		AtomicInteger ret = new AtomicInteger(0), oNoOfContour = new AtomicInteger(0);

		/* Trace the contour of the feature starting at the minutia point  */
		/* and stepping along up to the specified maximum number of steps. */
		Contour contour = getContour().traceContour(ret, oNoOfContour, maxLoopLen, 
			minutia.getX(), minutia.getY(), minutia.getX(), minutia.getY(), 
			minutia.getEx(), minutia.getEy(), 
			ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible ... */
		if (ret.get() == ILfs.IGNORE)
		{
			getFree().free(contour);
			return (ret.get());
		}

		/* If the trace completed a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			getFree().free(contour);
			return (ILfs.LOOP_FOUND);
		}

		/* If the trace successfully followed the minutia's contour, but did */
		/* not complete a loop within the specified number of steps ...      */
		if (ret.get() == ILfs.FALSE)
		{
			getFree().free(contour);
			return (ILfs.FALSE);
  		}

		getFree().free(contour);
		/* Otherwise, the trace had an error in following the contour ... */
		return (ret.get());
	}

	/*************************************************************************
	**************************************************************************
	#cat: onIslandLake - Determines if two minutia points lie on the same loop
	#cat:                 (island or lake).  If a loop is detected, the contour
	#cat:                 points of the loop are returned.
	   Input:
	      firstMinutia      - first minutia point
	      secondMinutia      - second minutia point
	      maxHalfLoop - maximum size of half the loop circumference searched for
	      binarizedImageData         - binary image data (0==while & 1==black)
	      imageWidth            - width (in pixels) of image
	      imageHeight            - height (in pixels) of image
	   Output:
	      ret		- IGNORE     - contour could not be traced
	      			- LOOP_FOUND - oMinutiae determined to lie on same qualifying loop
	      			- FALSE      - oMinutiae determined not to lie on same qualifying loop
	      			- Negative   - system error
	      oncontour   - number of points in the contour.
	   Return Code:
	   	  Contour	-- contians below information
	      	ocontourX  - x-pixel coords of loop contour
	      	ocontourY  - y-pixel coords of loop contour
	      	ocontourX  - x coord of each contour point's edge pixel
	      	ocontourY  - y coord of each contour point's edge pixel
	**************************************************************************/
	public Contour onIslandLake(AtomicInteger ret, AtomicInteger oncontour, 
		Minutia firstMinutia, Minutia secondMinutia, final int maxHalfLoop, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight) {
		int i, l;
		Contour contour1 = null, contour2 = null, contourLoop = null;
		AtomicInteger nContour1 = new AtomicInteger(0), nContour2 = new AtomicInteger(0), 
			nLoop = new AtomicInteger(0);

		/* Trace the contour of the feature starting at the 1st minutia point  */
		/* and stepping along up to the specified maximum number of steps or   */
		/* until 2nd mintuia point is encountered.                             */
		contour1 = getContour().traceContour(ret, nContour1, maxHalfLoop, 
			secondMinutia.getX(), secondMinutia.getY(), firstMinutia.getX(), firstMinutia.getY(), 
			firstMinutia.getEx(), firstMinutia.getEy(), 
			ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE)
		{			
			return contourLoop;
		}

		/* If the trace encounters 2nd minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			/* Now, trace the contour of the feature starting at the 2nd minutia, */
			/* continuing to search for edge neighbors clockwise, and stepping    */
			/* along up to the specified maximum number of steps or until 1st     */
			/*  mintuia point is encountered.                                     */
			contour2 = getContour().traceContour(ret, nContour2, maxHalfLoop, 
				firstMinutia.getX(), firstMinutia.getY(), secondMinutia.getX(), secondMinutia.getY(), 
				secondMinutia.getEx(), secondMinutia.getEy(), 
				ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
			/* If trace was not possible, return IGNORE. */
			if (ret.get() == ILfs.IGNORE)
			{
				getFree().free(contour1);
				return contourLoop;
			}

			/* If the 2nd trace encounters 1st minutia point ... */
			if (ret.get() == ILfs.LOOP_FOUND)
			{
				/* Combine the 2 half loop contours into one full loop. */
				/* Compute loop length (including the minutia pair). */
				nLoop.set(nContour1.get() + nContour2.get() + 2);

				/* Allocate loop contour. */
				contourLoop = getContour().allocateContour(ret, nLoop.get());
				if (ret.get() != ILfs.FALSE)
				{
					getFree().free(contour1) ;
					getFree().free(contour2) ;
					return contourLoop;
				}

				/* Store 1st minutia. */
				l = 0;
				contourLoop.getContourX().set(l, firstMinutia.getX());
				contourLoop.getContourY().set(l, firstMinutia.getY());
				contourLoop.getContourEx().set(l, firstMinutia.getEx());
				contourLoop.getContourEy().set(l++, firstMinutia.getEy());

				/* Store first contour. */
				for (i = 0; i < nContour1.get(); i++)
				{
					contourLoop.getContourX().set(l, contour1.getContourX().get(i));
					contourLoop.getContourY().set(l, contour1.getContourY().get(i));
					contourLoop.getContourEx().set(l, contour1.getContourEx().get(i));
					contourLoop.getContourEy().set(l++, contour1.getContourEy().get(i));
				}
				/* Store 2nd minutia. */
				contourLoop.getContourX().set(l, secondMinutia.getX());
				contourLoop.getContourY().set(l, secondMinutia.getY());
				contourLoop.getContourEx().set(l, secondMinutia.getEx());
				contourLoop.getContourEy().set(l++, secondMinutia.getEy());

				/* Store 2nd contour. */
				for (i = 0; i < nContour2.get(); i++)
				{
					contourLoop.getContourX().set(l, contour2.getContourX().get(i));
					contourLoop.getContourY().set(l, contour2.getContourY().get(i));
					contourLoop.getContourEx().set(l, contour2.getContourEx().get(i));
					contourLoop.getContourEy().set(l++, contour2.getContourEy().get(i));
				}

				/* Deallocate the half loop contours. */
				getFree().free(contour1) ;
				getFree().free(contour2) ;

				/* Assign loop contour to return pointers. */
				oncontour.set(nLoop.get());

				/* Then return that an island/lake WAS found (LOOP_FOUND). */
				ret.set(ILfs.LOOP_FOUND);
				return contourLoop;
			}

			/* If the trace successfully followed 2nd minutia's contour, but   */
			/* did not encounter 1st minutia point within the specified number */
			/* of steps ...                                                    */
			if (ret.get() == ILfs.FALSE)
			{
				/* Deallocate the two contours. */
				getFree().free(contour1) ;
				getFree().free(contour2) ;
				/* Then return that an island/lake was NOT found (FALSE). */
				ret.set(ILfs.FALSE);
				return contourLoop;
			}

			/* Otherwise, the 2nd trace had an error in following the contour ... */
			getFree().free(contour1) ;
			return contourLoop;
  		}

		/* If the 1st trace successfully followed 1st minutia's contour, but   */
		/* did not encounter the 2nd minutia point within the specified number */
		/* of steps ...                                                        */
		if (ret.get() == ILfs.FALSE)
		{
			getFree().free(contour1) ;
			/* Then return that an island/lake was NOT found (FALSE). */
			ret.set(ILfs.FALSE);
			return contourLoop;
		}

		/* Otherwise, the 1st trace had an error in following the contour ... */
		return contourLoop;
	}

	/*************************************************************************
	**************************************************************************
	#cat: onHook - Determines if two minutia points lie on a hook on the side
	#cat:           of a ridge or valley.
	   Input:
	      firstMinutia      - first minutia point
	      secondMinutia      - second minutia point
	      maxHookLen  - maximum length of contour searched along for a hook
	      binarizedImageData         - binary image data (0==while & 1==black)
	      imageWidth            - width (in pixels) of image
	      imageHeight            - height (in pixels) of image
	   Return Code:
	      IGNORE     - contour could not be traced
	      HOOK_FOUND - oMinutiae determined to lie on same qualifying hook
	      FALSE      - oMinutiae determined not to lie on same qualifying hook
	      Negative   - system error
	**************************************************************************/
	public int onHook(Minutia firstMinutia, Minutia secondMinutia, final int maxHookLen, 
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		AtomicInteger ret = new AtomicInteger (0);
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);

		/* NOTE: This routine should only be called when the 2 minutia points */
		/*       are of "opposite" type.                                      */

		/* Trace the contour of the feature starting at the 1st minutia's     */
		/* "edge" point and stepping along up to the specified maximum number */
		/* of steps or until the 2nd minutia point is encountered.            */
		/* First search for edge neighbors clockwise.                         */
		contour = getContour().traceContour(ret, oNoOfContour, maxHookLen, 
			secondMinutia.getX(), secondMinutia.getY(), firstMinutia.getEx(), firstMinutia.getEy(), 
			firstMinutia.getX(), firstMinutia.getY(), 
			ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE)
		{
			return (ret.get());
		}

		/* If the trace encountered the second minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			getFree().free(contour);
			return (ILfs.HOOK_FOUND);
		}

		/* If trace had an error in following the contour ... */
		if (ret.get() != ILfs.FALSE)
		{
			return (ret.get());
		}

		/* Otherwise, the trace successfully followed the contour, but did */
		/* not encounter the 2nd minutia point within the specified number */
		/* of steps.                                                       */

		/* Deallocate previously extracted contour. */
		getFree().free(contour);

		/* Try searching contour from 1st minutia "edge" searching for */
		/* edge neighbors counter-clockwise.                           */
		contour = getContour().traceContour(ret, oNoOfContour, maxHookLen, 
			secondMinutia.getX(), secondMinutia.getY(), firstMinutia.getEx(), firstMinutia.getEy(), 
			firstMinutia.getX(), firstMinutia.getY(), 
			ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE)
		{
			return (ret.get());
		}

		/* If the trace encountered the second minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			getFree().free(contour);
			return (ILfs.HOOK_FOUND);
		}

		/* If the trace successfully followed the 1st minutia's contour, but   */
		/* did not encounter the 2nd minutia point within the specified number */
		/* of steps ...      */
		if (ret.get() == ILfs.FALSE)
		{
			getFree().free(contour);
			/* Then return hook NOT found (FALSE). */
			return (ILfs.FALSE);
		}

		/* Otherwise, the 2nd trace had an error in following the contour ... */
		return (ret.get());
	}

	/*************************************************************************
	**************************************************************************
	#cat: isLoopClockwise - Takes a feature's contour points and determines if
	#cat:            the points are ordered clockwise or counter-clockwise about
	#cat:            the feature.  The routine also requires a default return
	#cat:            value be specified in the case the the routine is not able
	#cat:            to definitively determine the contour's order.  This allows
	#cat:            the default response to be application-specific.
	   Input:
	      oContourX   - x-coord list for feature's contour points
	      oContourY   - y-coord list for feature's contour points
	      noOfContour    - number of points in contour
	      defaultRet - default return code (used when we can't tell the order)
	   Return Code:
	      TRUE      - contour determined to be ordered clockwise
	      FALSE     - contour determined to be ordered counter-clockwise
	      Default   - could not determine the order of the contour
	      Negative  - system error
	**************************************************************************/
	public int isLoopClockwise(AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, 
		final int noOfContour, final int defaultRet) {
		int ret;
		AtomicInteger nchain = new AtomicInteger(0);
		AtomicIntegerArray chain = new AtomicIntegerArray(noOfContour);
		  
		/* Derive chain code from contour points. */
		ret = getChainCode().chainCodeLoop(chain, nchain, oContourX, oContourY, noOfContour);
		if (ret != ILfs.FALSE)
		{
			/* If there is a system error, return the error code. */
			return (ret);
		}

		/* If chain is empty... */
		if (nchain.get() == ILfs.FALSE)
		{
			/* There wasn't enough contour points to tell, so return the   */
			/* the default return value.  No chain needs to be deallocated */
			/* in this case.                                               */
			return (defaultRet);
		}

		/* If the chain code for contour is clockwise ... pass default return   */
		/* value on to this routine to correctly handle the case where we can't */
		/* tell the direction of the chain code.                                */
		ret = getChainCode().isChainClockwise(chain, nchain.get(), defaultRet);

		/* Free the chain code and return result. */
		getFree().free(chain);
		return (ret);
	}

	/*************************************************************************
	**************************************************************************
	#cat: processLoop - Takes a contour list that has been determined to form
	#cat:            a complete loop, and processes it. If the loop is sufficiently
	#cat:            large and elongated, then two minutia points are calculated
	#cat:            along the loop's longest aspect axis.  If it is determined
	#cat:            that the loop does not contain oMinutiae, it is filled in the
	#cat:            binary image.
	   Input:
	      oContourX  - x-coord list for loop's contour points
	      oContourY  - y-coord list for loop's contour points
	      oContourEx - x-coord list for loop's edge points
	      oContourEy - y-coord list for loop's edge points
	      noOfContour   - number of points in contour
	      binarizedImageData      - binary image data (0==while & 1==black)
	      imageWidth         - width (in pixels) of image
	      imageHeight         - height (in pixels) of image
	      lfsParams   - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae    - points to a list of detected minutia structures
	        OR
	      binarizedImageData      - binary image data with loop filled
	   Return Code:
	      Zero      - loop processed successfully
	      Negative  - system error
	**************************************************************************/
	public int processLoop(AtomicReference<Minutiae> oMinutiae, 
		AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, 
		AtomicIntegerArray oContourEx, AtomicIntegerArray oContourEy,
		final int noOfContour, int[] binarizedImageData, final int imageWidth, final int imageHeight, 
		final LfsParams lfsParams) {
		int iDir, type, appearing;
		int halfway;
		AtomicReference<Double> minDistance = new AtomicReference<Double>(0.0), 
			maxDistance = new AtomicReference<Double>(0.0);
		AtomicInteger minFrom = new AtomicInteger(0), maxFrom = new AtomicInteger(0), 
			minTo = new AtomicInteger(0), maxTo = new AtomicInteger(0);
		int midPointX, midPointY, midPixel;
		int featurePixel;
		int ret;
		Minutia minutia;

		/* If contour is empty, then just return. */
		if (noOfContour <= ILfs.FALSE)
		{
			return (ILfs.FALSE);
		}

		/* If loop is large enough ... */
		if (noOfContour > lfsParams.getMinLoopLen())
		{
			/* Get pixel value of feature's interior. */
			featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

			/* Compute half the perimeter of the loop. */
			halfway = noOfContour >> 1;

			/* Get the aspect dimensions of the loop in units of */
			/* squared distance.                                 */
			getLoopAspect(minFrom, minTo, minDistance, maxFrom, maxTo, maxDistance, oContourX, oContourY, noOfContour);

			/* If loop passes aspect ratio tests ... loop is sufficiently  */
			/* narrow or elongated ...                                     */
			if ((minDistance.get() < lfsParams.getMinLoopAspectDist()) || ((maxDistance.get() / minDistance.get()) >= lfsParams.getMinLoopAspectRatio()))
			{
				/* Update oMinutiae list with opposite points of max distance */
				/* on the loop.                                             */
				/* First, check if interior point has proper pixel value. */
				midPointX = (oContourX.get(maxFrom.get()) + oContourX.get(maxTo.get())) >> 1;
				midPointY = (oContourY.get(maxFrom.get()) + oContourY.get(maxTo.get())) >> 1;
				midPixel = binarizedImageData[0 + (midPointY * imageWidth) + midPointX];
				/* If interior point is the same as the feature... */
				if (midPixel == featurePixel)
				{
					/* 1. Treat maximum distance point as a potential minutia. */
					/* Compute direction from maximum loop point to its */
					/* opposite point.                                  */
					iDir = getLfsUtil().lineToDirection(oContourX.get(maxFrom.get()), 
						oContourY.get(maxFrom.get()), oContourX.get(maxTo.get()), oContourY.get(maxTo.get()), 
						lfsParams.getNumDirections());
					/* Get type of minutia: BIFURCATION or RIDGE_ENDING. */
					type = getMinutiaHelper().getMinutiaType(featurePixel);
					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = 
						getMinutiaHelper().isMinutiaAppearing(oContourX.get(maxFrom.get()), 
						oContourY.get(maxFrom.get()), oContourEx.get(maxFrom.get()), 
						oContourEy.get(maxFrom.get()))) < 0)
					{
						/* Return system error code. */
						return (appearing);
					}
					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(maxFrom.get()), 
						oContourY.get(maxFrom.get()), oContourEx.get(maxFrom.get()), 
						oContourEy.get(maxFrom.get()), iDir, 
						ILfs.DEFAULT_RELIABILITY, type, appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, 
						binarizedImageData, imageWidth, imageHeight, lfsParams);
					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE)
					{
				  		/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* 2. Treat point opposite of maximum distance point as */
					/*    a potential minutia.                              */

					/* Flip the direction 180 degrees. Make sure new direction */
					/* is on the range [0..(ndirsX2)].                         */
					iDir += lfsParams.getNumDirections();
					iDir %= (lfsParams.getNumDirections() << 1);

					/* The type of minutia will stay the same. */

					/* Determine if minutia is appearing or disappearing. */
					if ((appearing 
						= getMinutiaHelper().isMinutiaAppearing(oContourX.get(maxTo.get()), 
							oContourY.get(maxTo.get()), oContourEx.get(maxTo.get()), 
							oContourEy.get(maxTo.get()))) < 0)
					{
					   /* Return system error code. */
					   return (appearing);
					}
					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(maxTo.get()), 
						oContourY.get(maxTo.get()), oContourEx.get(maxTo.get()), oContourEy.get(maxTo.get()), 
						iDir, ILfs.DEFAULT_RELIABILITY, type, appearing, ILfs.LOOP_ID);
					/* Update the oMinutiae list with potential new minutia. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, 
						binarizedImageData, imageWidth, imageHeight, lfsParams);
					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE)
					{
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* Done successfully processing this loop, so return normally. */
					return (ILfs.FALSE);
				} // Otherwise, loop interior has problems.
			} // Otherwise, loop is not the right shape for oMinutiae.
		} // Otherwise, loop's perimeter is too small for oMinutiae.

		/* If we get here, we have a loop that is assumed to not contain */
		/* oMinutiae, so remove the loop from the image.                   */
		ret = fillLoop(oContourX, oContourY, noOfContour, binarizedImageData, imageWidth, imageHeight);

		/* Return either an error code from fill_loop or return normally. */
		return (ret);
	}

	/*************************************************************************
	**************************************************************************
	#cat: processLoopV2 - Takes a contour list that has been determined to form
	#cat:            a complete loop, and processes it. If the loop is sufficiently
	#cat:            large and elongated, then two minutia points are calculated
	#cat:            along the loop's longest aspect axis.  If it is determined
	#cat:            that the loop does not contain oMinutiae, it is filled in the
	#cat:            binary image.
	   Input:
	      oContourX  - x-coord list for loop's contour points
	      oContourY  - y-coord list for loop's contour points
	      oContourEx - x-coord list for loop's edge points
	      oContourEy - y-coord list for loop's edge points
	      noOfContour   - number of points in contour
	      binarizedImageData      - binary image data (0==while & 1==black)
	      imageWidth         - width (in pixels) of image
	      imageHeight         - height (in pixels) of image
	      oLowFlowMap  - pixelized Low Ridge Flow Map
	      lfsParams   - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae    - points to a list of detected minutia structures
	        OR
	      binarizedImageData      - binary image data with loop filled
	   Return Code:
	      Zero      - loop processed successfully
	      Negative  - system error
	**************************************************************************/
	public int processLoopV2(AtomicReference<Minutiae> oMinutiae, 
		AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, 
		AtomicIntegerArray oContourEx, AtomicIntegerArray oContourEy,
		final int noOfContour, int[] binarizedImageData, final int imageWidth, final int imageHeight, 
		AtomicIntegerArray oLowFlowMap, final LfsParams lfsParams) {
		int halfway;
		int idir, type, appearing;
		AtomicReference<Double> oMinDistance = new AtomicReference<Double>(0.0), 
			oMaxDistance = new AtomicReference<Double>(0.0);
		AtomicInteger oMinFrom = new AtomicInteger(0), oMaxFrom = new AtomicInteger(0), 
			oMinTo = new AtomicInteger(0), oMaxTo = new AtomicInteger(0);
		int midX, midY, midPixel;
		int featurePixel;
		int ret;
		Minutia minutia;
		int fmapval;
		double reliability;

		/* If contour is empty, then just return. */
		if (noOfContour <= ILfs.FALSE)
		{
			return (ILfs.FALSE);
		}

		/* If loop is large enough ... */
		if (noOfContour > lfsParams.getMinLoopLen())
		{
			/* Get pixel value of feature's interior. */
			featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

			/* Compute half the perimeter of the loop. */
			halfway = noOfContour >> 1;

			/* Get the aspect dimensions of the loop in units of */
			/* squared distance.                                 */
			getLoopAspect(oMinFrom, oMinTo, oMinDistance, oMaxFrom, oMaxTo, oMaxDistance, oContourX, oContourY, noOfContour);

			/* If loop passes aspect ratio tests ... loop is sufficiently  */
			/* narrow or elongated ...                                     */
			if ((oMinDistance.get() < lfsParams.getMinLoopAspectDist()) || 
				((oMaxDistance.get() / oMinDistance.get()) >= lfsParams.getMinLoopAspectRatio()))
			{
				/* Update oMinutiae list with opposite points of max distance */
				/* on the loop.                                             */

				/* First, check if interior point has proper pixel value. */
				midX = (oContourX.get(oMaxFrom.get()) + oContourX.get(oMaxTo.get())) >> 1;
				midY = (oContourY.get(oMaxFrom.get()) + oContourY.get(oMaxTo.get())) >> 1;
				midPixel = binarizedImageData[0 + (midY * imageWidth) + midX];
				/* If interior point is the same as the feature... */
				if (midPixel == featurePixel)
				{
					/* 1. Treat maximum distance point as a potential minutia. */
					/* Compute direction from maximum loop point to its */
					/* opposite point.                                  */
					idir = getLfsUtil().lineToDirection(oContourX.get(oMaxFrom.get()), oContourY.get(oMaxFrom.get()), oContourX.get(oMaxTo.get()), oContourY.get(oMaxTo.get()), lfsParams.getNumDirections());
					/* Get type of minutia: BIFURCATION or RIDGE_ENDING. */
					type = getMinutiaHelper().getMinutiaType(featurePixel);
					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = 
							getMinutiaHelper().isMinutiaAppearing(oContourX.get(oMaxFrom.get()), oContourY.get(oMaxFrom.get()), 
							oContourEx.get(oMaxFrom.get()), oContourEy.get(oMaxFrom.get()))) < ILfs.FALSE)
					{
						/* Return system error code. */
						return (appearing);
					}

					/* Is the new point in a LOW RIDGE FLOW block? */
					fmapval = oLowFlowMap.get(0 + (oContourY.get(oMaxFrom.get()) * imageWidth) + oContourX.get(oMaxFrom.get()));

					/* If current minutia is in a LOW RIDGE FLOW block ... */
					if (fmapval >= ILfs.TRUE)
					{
						reliability = ILfs.MEDIUM_RELIABILITY;
					}
					else
					{
						/* Otherwise, minutia is in a reliable block. */
						reliability = ILfs.HIGH_RELIABILITY;
					}

					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(oMaxFrom.get()), oContourY.get(oMaxFrom.get()), 
						oContourEx.get(oMaxFrom.get()), oContourEy.get(oMaxFrom.get()), idir, 
						reliability, type, appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia.  */
					/* NOTE: Deliberately using version one of this routine. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth, imageHeight, lfsParams);

					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE)
					{
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* 2. Treat point opposite of maximum distance point as */
					/*    a potential minutia.                              */

					/* Flip the direction 180 degrees. Make sure new direction */
					/* is on the range [0..(ndirsX2)].                         */
					idir += lfsParams.getNumDirections();
					idir %= (lfsParams.getNumDirections() << 1);

					/* The type of minutia will stay the same. */

					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = 
						getMinutiaHelper().isMinutiaAppearing(oContourX.get(oMaxTo.get()), oContourY.get(oMaxTo.get()), 
						oContourEx.get(oMaxTo.get()), oContourEy.get(oMaxTo.get()))) < ILfs.FALSE)
					{
						/* Return system error code. */
						return (appearing);
					}

					/* Is the new point in a LOW RIDGE FLOW block? */
					fmapval = oLowFlowMap.get(0 + (oContourY.get(oMaxTo.get()) * imageWidth) + oContourX.get(oMaxTo.get()));

					/* If current minutia is in a LOW RIDGE FLOW block ... */
					if (fmapval >= ILfs.TRUE)
					{
						reliability = ILfs.MEDIUM_RELIABILITY;
					}
					else
					{
						/* Otherwise, minutia is in a reliable block. */
						reliability = ILfs.HIGH_RELIABILITY;
					}

					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(oMaxTo.get()), oContourY.get(oMaxTo.get()), 
						oContourEx.get(oMaxTo.get()), oContourEy.get(oMaxTo.get()), idir, 
						reliability, type, appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia. */
					/* NOTE: Deliberately using version one of this routine. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth, imageHeight, lfsParams);

					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE)
					{
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* Done successfully processing this loop, so return normally. */
					return (ILfs.FALSE);
				} // Otherwise, loop interior has problems.
			} // Otherwise, loop is not the right shape for oMinutiae.
		} // Otherwise, loop's perimeter is too small for oMinutiae.

		/* If we get here, we have a loop that is assumed to not contain */
		/* oMinutiae, so remove the loop from the image.                   */
		ret = fillLoop(oContourX, oContourY, noOfContour, binarizedImageData, imageWidth, imageHeight);

		/* Return either an error code from fill_loop or return normally. */
		return (ret);
	}

	/*************************************************************************
	**************************************************************************
	#cat: getLoopAspect - Takes a contour list (determined to form a complete
	#cat:            loop) and measures the loop's aspect (the largest and smallest
	#cat:            distances across the loop) and returns the points on the
	#cat:            loop where these distances occur.
	   Input:
	      oContourX - x-coord list for loop's contour points
	      oContourY - y-coord list for loop's contour points
	      noOfContour  - number of points in contour
	   Output:
	      oMinFrom   - contour point index where minimum aspect occurs
	      oMinTo   - opposite contour point index where minimum aspect occurs
	      oMinDist - the minimum distance across the loop
	      oMaxFrom   - contour point index where maximum aspect occurs
	      oMaxTo   - contour point index where maximum aspect occurs
	      oMaxDist - the maximum distance across the loop
	**************************************************************************/
	public void getLoopAspect(AtomicInteger oMinFrom, AtomicInteger oMinTo, AtomicReference<Double> oMinDist, 
		AtomicInteger oMaxFrom, AtomicInteger oMaxTo, AtomicReference<Double> oMaxDist, 
		AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, final int noOfContour) {
		int halfway, limit;
		int i, j;
		double dist, minDistance, maxDistance;
		int minI, maxI;
		int minJ, maxJ;

		/* Compute half the perimeter of the loop. */
		halfway = noOfContour >> 1;

		/* Take opposite points on the contour and walk half way    */
		/* around the loop.                                         */
		i = 0;
		j = halfway;
		/* Compute squared distance between opposite points on loop. */
		dist = getLfsUtil().squaredDistance(oContourX.get(i), oContourY.get(i), 
			oContourX.get(j), oContourY.get(j));

		/* Initialize running minimum and maximum distances along loop. */
		minDistance = dist;
		minI = i;
		minJ = j;
		maxDistance = dist;
		maxI = i;
		maxJ = j;
		/* Bump to next pair of opposite points. */
		i++;
		/* Make sure j wraps around end of list. */
		j++;
		j %= noOfContour;

		/* If the loop is of even length, then we only need to walk half */
		/* way around as the other half will be exactly redundant.  If   */
		/* the loop is of odd length, then the second half will not be   */
		/* be exactly redundant and the difference "may" be meaningful.  */
		/* If execution speed is an issue, then probably get away with   */
		/* walking only the fist half of the loop under ALL conditions.  */

		/* If loop has odd length ... */
		if ((noOfContour % 2) == ILfs.TRUE)//odd
		{
			/* Walk the loop's entire perimeter. */
			limit = noOfContour;
  		}
		/* Otherwise the loop has even length ... */
		else
		{
			/* Only walk half the perimeter. */
			limit = halfway;
		}

		/* While we have not reached our perimeter limit ... */
		while (i < limit)
		{
			/* Compute squared distance between opposite points on loop. */
			dist = getLfsUtil().squaredDistance(oContourX.get(i), oContourY.get(i), 
				oContourX.get(j), oContourY.get(j));
			/* Check the running minimum and maximum distances. */
			if (dist < minDistance)
			{
				minDistance = dist;
				minI = i;
				minJ = j;
			}
			if (dist > maxDistance)
			{
				maxDistance = dist;
				maxI = i;
				maxJ = j;
			}
			/* Bump to next pair of opposite points. */
			i++;
			/* Make sure j wraps around end of list. */
			j++;
			j %= noOfContour;
		}

		/* Assign minimum and maximum distances to output pointers. */
		oMinFrom.set(minI);
		oMinTo.set(minJ);
		oMinDist.set(minDistance);
		oMaxFrom.set(maxI);
		oMaxTo.set(maxJ);
		oMaxDist.set(maxDistance);
	}

	/*************************************************************************
	**************************************************************************
	#cat: fillLoop - Takes a contour list that has been determined to form
	#cat:            a complete loop, and fills the loop accounting for
	#cat:            complex/concaved shapes.
	#cat:            NOTE, I tried using a flood-fill in place of this routine,
	#cat:            but the contour (although 8-connected) is NOT guaranteed to
	#cat:            be "complete" surrounded (in an 8-connected sense) by pixels
	#cat:            of opposite color.  Therefore, the flood would occasionally
	#cat:            escape the loop and corrupt the binary image!
	   Input:
	      oContourX  - x-coord list for loop's contour points
	      oContourY  - y-coord list for loop's contour points
	      noOfContour   - number of points in contour
	      binarizedImageData      - binary image data (0==while & 1==black)
	      imageWidth         - width (in pixels) of image
	      imageHeight         - height (in pixels) of image
	   Output:
	      binarizedImageData      - binary image data with loop filled
	   Return Code:
	      Zero      - loop filled successfully
	      Negative  - system error
	**************************************************************************/
	public int fillLoop(AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, final int noOfContour,
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Shape shape;
		int ret, i, j, x, nx, y, lastj, nextPixel, featurePixel, edgePixel;

		/* Create a shape structure from loop's contour. */
		AtomicInteger returnCode = new AtomicInteger(0);
		shape = getShapes().shapeFromContour(returnCode, oContourX, oContourY, noOfContour);
		ret = returnCode.get();
		if (ret != ILfs.FALSE)
		{
		   /* If system error, then return error code. */
		   return (ret);
		}

		/* Get feature pixel value (the value on the interior of the loop */
		/* to be filled).                                                 */
		//feature_pix = *(binarizedImageData.argValue + (oContourY[0] * imageWidth) + oContourX[0]);
		featurePixel = binarizedImageData [(oContourY.get(0) * imageWidth) + oContourX.get(0)];
		/* Now get edge pixel value (the value on the exterior of the loop    */
		/* to be used to filled the loop).  We can get this value by flipping */
		/* the feature pixel value.                                           */
		if (featurePixel == ILfs.TRUE)
		{
		   edgePixel = 0;
		}
		else
		{
		   edgePixel = 1;
		}

		/* Foreach row in shape... */
		for (i = 0; i < shape.getNRows(); i++)
		{
			/* Get y-coord of current row in shape. */
			y = shape.getRows().get(i).getY();

			/* There should always be at least 1 contour points in the row.    */
			/* If there isn't, then something is wrong, so post a warning and  */
			/* just return.  This is mostly for debug purposes.                */
			if (shape.getRows().get(i).getNoOfPts() < ILfs.TRUE)
			{
				/* Deallocate the shape. */
				getShapes().freeShape(shape);
				LOGGER.warn(String.format("WARNING : fill_loop : unexpected shape, preempting loop fill\n"));
				/* This is unexpected, but not fatal, so return normally. */
				return (ILfs.FALSE);
			}

			/* Reset x index on row to the left-most contour point in the row. */
			j = 0;
			/* Get first x-coord corresponding to the first contour point on row. */
			x = shape.getRows().get(i).getXs().get(j);
			/* Fill the first contour point on the row. */
			//*(binarizedImageData.argValue + (y * imageWidth) + x) = edgePixel;
			binarizedImageData[(y * imageWidth) + x] = edgePixel;
			/* Set the index of last contour point on row. */
			lastj = shape.getRows().get(i).getNoOfPts() - 1;
			
			/* While last contour point on row has not been processed... */
			while (j < lastj)
			{
				/* On each interation, we have filled up to the current   */
				/* contour point on the row pointed to by "j", and now we */
				/* need to determine if we need to skip some edge pixels  */
				/* caused by a concavity in the shape or not.             */

				/* Get the next pixel value on the row just right of the     */
				/* last contour point filled.  We know there are more points */
				/* on the row because we haven't processed the last contour  */
				/* point on the row yet.                                     */
				x++;
				//nextPixel = *(binarizedImageData.argValue + (y * imageWidth) + x);
				nextPixel = binarizedImageData[(y * imageWidth) + x];

				/* If the next pixel is the same value as loop's edge pixels ... */
				if (nextPixel == edgePixel)
				{
					/* Then assume we have found a concavity and skip to next */
					/* contour point on row.                                  */
					j++;
					/* Fill the new contour point because we know it is on the */
					/* feature's contour.                                      */
					x = shape.getRows().get(i).getXs().get(j);
					//*(binarizedImageData.argValue + (y * imageWidth) + x) = edgePixel;
					binarizedImageData[(y * imageWidth) + x] = edgePixel;

					/* Now we are ready to loop again. */
				}
				/* Otherwise, fill from current pixel up through the next contour */
				/* point to the right on the row.                                 */
				else
				{
					/* Bump to the next contour point to the right on row. */
					j++;
					/* Set the destination x-coord to the next contour point   */
					/* to the right on row.  Realize that this could be the    */
					/* same pixel as the current x-coord if contour points are */
					/* adjacent.                                               */
					nx = shape.getRows().get(i).getXs().get(j);

					/* Fill between current x-coord and next contour point to the */
					/* right on the row (including the new contour point).*/
					fillPartialRow(edgePixel, x, nx, y, binarizedImageData, imageWidth, imageHeight);
				}

				/* Once we are here we have filled the row up to (and including) */
				/* the contour point currently pointed to by "j".                */
				/* We are now ready to loop again.                               */
			} // End WHILE
		} // End FOR

		getShapes().freeShape(shape);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: fillPartialRow - Fills a specified range of contiguous pixels on
	#cat:            a specified row of an 8-bit pixel image with a specified
	#cat:            pixel value.  NOTE, the pixel coordinates are assumed to
	#cat:            be within the image boundaries.
	   Input:
	      fillPixel - pixel value to fill with (should be on range [0..255]
	      fromX      - x-pixel coord where fill should begin
	      toX      - x-pixel coord where fill should end (inclusive)
	      yIndex        - y-pixel coord of current row being filled
	      binarizedImageData    - 8-bit image data
	      imageWidth       - width (in pixels) of image
	      imageHeight       - height (in pixels) of image
	   Output:
	      binarizedImageData    - 8-bit image data with partial row filled.
	**************************************************************************/
	public void fillPartialRow(final int fillPixel, final int fromX, final int toX,
		final int yIndex, int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int binarizedImageDataIndex;

		/* Set pixel pointer to starting x-coord on current row. */
		binarizedImageDataIndex = 0 + (yIndex * imageWidth) + fromX;

		/* Foreach pixel between starting and ending x-coord on row */
		/* (including the end points) ...                           */
		for (int x = fromX; x <= toX; x++)
		{
			/* Set current pixel with fill pixel value. */
			binarizedImageData [binarizedImageDataIndex] = fillPixel;
			/* Bump to next pixel in the row. */
			binarizedImageDataIndex++;
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: floodLoop - Fills a given contour (determined to form a complete loop)
	#cat:            with a specified pixel value using a recursive flood-fill
	#cat:            technique.
	#cat:            NOTE, this fill approach will NOT always work with the
	#cat:            contours generated in this application because they
	#cat:            are NOT guaranteed to be ENTIRELY surrounded by 8-connected
	#cat:            pixels not equal to the fill pixel value.  This is unfortunate
	#cat:            because the flood-fill is a simple algorithm that will handle
	#cat:            complex/concaved shapes.
	   Input:
	      oContourX  - x-coord list for loop's contour points
	      oContourY  - y-coord list for loop's contour points
	      noOfContour   - number of points in contour
	      binarizedImageData      - binary image data (0==while & 1==black)
	      imageWidth         - width (in pixels) of image
	      imageHeight         - height (in pixels) of image
	   Output:
	      binarizedImageData      - binary image data with loop filled
	**************************************************************************/
	public void floodLoop(final AtomicIntegerArray oContourX, final AtomicIntegerArray oContourY,
		final int noOfContour, int [] binarizedImageData, final int imageWidth, final int imageHeight) {
		int featurePixel;
		int fillPixel;

		/* Get the pixel value of the minutia feauture.  This is */
		/* the pixel value we wish to replace with the flood.    */
		featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

		/* Flip the feature pixel value to the value we want to */
		/* fill with and send this value to the flood routine.  */
		fillPixel = ~featurePixel & 0xFF;

		/* Flood-fill interior of contour using a 4-neighbor fill.  */
		/* We are using a 4-neighbor fill because the contour was   */
		/* collected using 8-neighbors, and the 4-neighbor fill     */
		/* will NOT escape the 8-neighbor based contour.            */
		/* The contour passed must be guarenteed to be complete for */
		/* the flood-fill to work properly.                         */
		/* We are initiating a flood-fill from each point on the    */
		/* contour to make sure complex patterns get filled in.     */
		/* The complex patterns we are concerned about are those    */
		/* that "pinch" the interior of the feature off due to      */
		/* skipping "exposed" corners along the contour.            */
		/* Simple shapes will fill upon invoking the first contour  */
		/* pixel, and the subsequent calls will immediately return  */
		/* as their seed pixel will have already been flipped.      */
		for (int i = 0; i < noOfContour; i++)
		{
			/* Start the recursive flooding. */
			floodFill4(fillPixel, oContourX.get(i), oContourY.get(i), 
				binarizedImageData, imageWidth, imageHeight);
  		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: floodFill4 - Recursively floods a region of an 8-bit pixel image with a
	#cat:               specified pixel value given a starting (seed) point.  The
	#cat:               recursion is based neighbors being 4-connected.
	   Input:
	      fillPixel - 8-bit pixel value to be filled with (on range [0..255]
	      xIndex        - starting x-pixel coord
	      yIndex        - starting y-pixel coord
	      binarizedImageData    - 8-bit pixel image data
	      imageWidth       - width (in pixels) of image
	      imageHeight       - height (in pixels) of image
	   Output:
	      binarizedImageData    - 8-bit pixel image data with region filled
	**************************************************************************/
	public void floodFill4(final int fillPixel, final int xIndex, final int yIndex,
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int binarizedImageDataIndex;
		int yNorthPixel, ySouthPixel, xEastPixel, xWestPixel;

		/* Get address of current pixel. */
		binarizedImageDataIndex = (0 + (yIndex * imageWidth) + xIndex);
		/* If pixel needs to be filled ... */
		if (binarizedImageData[binarizedImageDataIndex] != fillPixel)
		{
			/* Fill the current pixel. */
			binarizedImageData[binarizedImageDataIndex] = fillPixel;

			/* Recursively invoke flood on the pixel's 4 neighbors.   */
			/* Test to make sure neighbors are within image boudaries */
			/* before invoking each flood.                            */
			yNorthPixel = yIndex - 1;
			ySouthPixel = yIndex + 1;
			xWestPixel = xIndex - 1;
			xEastPixel = xIndex + 1;

			/* Invoke North */
			if (yNorthPixel >= 0)
			{
				floodFill4(fillPixel, xIndex, yNorthPixel, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke East */
			if (xEastPixel < imageWidth)
			{
				floodFill4(fillPixel, xEastPixel, yIndex, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke South */
			if (ySouthPixel < imageHeight)
			{
				floodFill4(fillPixel, xIndex, ySouthPixel, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke West */
			if (xWestPixel >= 0)
			{
				floodFill4(fillPixel, xWestPixel, yIndex, binarizedImageData, imageWidth, imageHeight);
			}
		}
		/* Otherwise, there is nothing to be done. */
	}
}


