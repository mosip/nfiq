package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.Defs;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IContour;

public class Contour extends MindTct implements IContour {
	private static final Logger LOGGER = LoggerFactory.getLogger(Contour.class);	

	private static volatile Contour instance;
    public static Contour getInstance() {
        if (instance == null) {
            synchronized (Contour.class) {
                if (instance == null) {
                    instance = new Contour();
                }
            }
        }
        return instance;
    }    
    public static Contour getInstance(int noOfContour) {
        if (instance == null) {
            synchronized (Contour.class) {
                if (instance == null) {
                    instance = new Contour(noOfContour);
                }
            }
        }
        return instance;
    }    

    private Contour()
    {
    	super();
    }
    
	private AtomicIntegerArray contourX, contourY, contourEx, contourEy;
	private int noOfContour;
	private Contour(int noOfContour) {
		super();
		this.contourX = new AtomicIntegerArray (noOfContour);
		this.contourY = new AtomicIntegerArray (noOfContour);
		this.contourEx = new AtomicIntegerArray (noOfContour);
		this.contourEy = new AtomicIntegerArray (noOfContour);
		this.noOfContour = noOfContour;
	}

	public Defs getDefs() {
		return Defs.getInstance();
	}	
	public Free getFree() {
		return Free.getInstance();
	}
	public Globals getGlobals() {
		return Globals.getInstance();
	}
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/*************************************************************************
	**************************************************************************
	#cat: allocateContour - Allocates the lists needed to represent the
	#cat:            contour of a minutia feature (a ridge or valley-ending).
	#cat:            This includes two lists of coordinate pairs.  The first is
	#cat:            the 8-connected chain of points interior to the feature
	#cat:            and are called the feature's "contour points".
	#cat:            The second is a list or corresponding points each
	#cat:            adjacent to its respective feature contour point in the first
	#cat:            list and on the exterior of the feature.  These second points
	#cat:            are called the feature's "edge points".  Don't be confused,
	#cat:            both lists of points are on the "edge".  The first set is
	#cat:            guaranteed 8-connected and the color of the feature.  The
	#cat:            second set is NOT guaranteed to be 8-connected and its points
	#cat:            are opposite the color of the feature.  Remeber that "feature"
	#cat:            means either ridge-ending (black pixels) or valley-ending
	#cat:            (white pixels).
	   Input:
	      noOfContour    - number of items in each coordinate list to be allocated
	   Output:
	      ret 	-Zero      - lists were successfully allocated
	      		- Negative  - system (allocation) error
	   Return Code:
	   	Contour contains below information
	      ocontourX  - allocated x-coord list for feature's contour points
	      ocontourY  - allocated y-coord list for feature's contour points
	      ocontourEx - allocated x-coord list for feature's edge points
	      ocontourEy - allocated y-coord list for feature's edge points
	**************************************************************************/
	public Contour allocateContour(AtomicInteger ret, final int noOfContour) {
		ret.set(ILfs.FALSE);
		return new Contour(noOfContour);
	}

	/*************************************************************************
	**************************************************************************
	#cat: free_contour - Deallocates the lists used to represent the
	#cat:            contour of a minutia feature (a ridge or valley-ending).
	#cat:            This includes two lists of coordinate pairs.  The first is
	#cat:            the 8-connected chain of points interior to the feature
	#cat:            and are called the feature's "contour points".
	#cat:            The second is a list or corresponding points each
	#cat:            adjacent to its respective feature contour point in the first
	#cat:            list and on the exterior of the feature.  These second points
	#cat:            are called the feature's "edge points".
	   Input:
	   	Contour contains below information
	      contourX  - x-coord list for feature's contour points
	      contourY  - y-coord list for feature's contour points
	      contourEx - x-coord list for feature's edge points
	      contourEy - y-coord list for feature's edge points
	**************************************************************************/
	public void freeContour(Contour contour) {
		if (contour != null)
		{
			getFree().free(contour.getContourX());
			getFree().free(contour.getContourY());
			getFree().free(contour.getContourEx());
			getFree().free(contour.getContourEy());
			contour.setContourX(null);
			contour.setContourY(null);
			contour.setContourEx(null);
			contour.setContourEy(null);
			contour = null;
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: getHighCurvatureContour - Takes the pixel coordinate of a detected
	#cat:            minutia feature point and its corresponding/adjacent edge
	#cat:            pixel and attempts to extract a contour of specified length
	#cat:            of the feature's edge.  The contour is extracted by walking
	#cat:            the feature's edge a specified number of steps clockwise and
	#cat:            then counter-clockwise. If a loop is detected while
	#cat:            extracting the contour, the contour of the loop is returned
	#cat:            with a return code of (LOOP_FOUND).  If the process fails
	#cat:            to extract a contour of total specified length, then
	#cat:            the returned contour length is set to Zero, NO allocated
	#cat:            memory is returned in this case, and the return code is set
	#cat:            to Zero.  An alternative implementation would be to return
	#cat:            the incomplete contour with a return code of (INCOMPLETE).
	#cat:            For now, NO allocated contour is returned in this case.
	   Input:
	      halfContour 		- half the length of the extracted contour
	                     		(full-length non-loop contour = (half_contourX2)+1)
	      xPixelLoc  		- starting x-pixel coord of feature (interior to feature)
	      yPixelLoc  		- starting y-pixel coord of feature (interior to feature)
	      xEdgePixelLoc 	- x-pixel coord of corresponding edge pixel
	               				(exterior to feature)
	      yEdgePixelLoc 	- y-pixel coord of corresponding edge pixel
	                  		(exterior to feature)
	      binarizedImageData  	- binary image data (0==while & 1==black)
	      imageWidth     		- width (in pixels) of image
	      imageHeight     		- height (in pixels) of image
	   Output:
	      Zero       	- resulting contour was successfully extracted or is empty
	      LOOP_FOUND 	- resulting contour forms a complete loop
	      Negative   	- system error
	      oNoOfContour  - number of contour points returned
	   Return Code:
	   	Contour contains below information
	      ocontourX  - x-pixel coords of contour (interior to feature)
	      ocontourY  - y-pixel coords of contour (interior to feature)
	      ocontourEx - x-pixel coords of corresponding edge (exterior to feature)
	      ocontourEy - y-pixel coords of corresponding edge (exterior to feature)
	**************************************************************************/
	public Contour getHighCurvatureContour(AtomicInteger ret, AtomicInteger oNoOfContour, 
		final int halfContour, final int xPixelLoc, final int yPixelLoc, 
		final int xEdgePixelLoc, final int yEdgePixelLoc, 
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Contour contour = null, contourHalf1 = null, contourHalf2 = null;
		int maxContour;
		AtomicInteger nHalf1 = new AtomicInteger(0), nHalf2 = new AtomicInteger(0);
		int noOfContour, i, j;

		/* Compute maximum length of complete contour */
		/* (2 half contours + feature point).         */
		maxContour = (halfContour << 1) + 1;

		/* Initialize output contour length to 0. */
		oNoOfContour.set (0);

		/* Get 1st half contour with clockwise neighbor trace. */
		contourHalf1 = traceContour(ret, nHalf1, halfContour, 
				xPixelLoc, yPixelLoc, xPixelLoc, yPixelLoc, 
				xEdgePixelLoc, yEdgePixelLoc, ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);		
		if (ret.get() != ILfs.FALSE)
		{
			/* If trace was not possible ... */
			if (ret.get() == ILfs.IGNORE)
			{
				/* Return, with nothing allocated and contour length equal to 0. */
				ret.set(ILfs.FALSE);
				return contour;
			}

			/* If 1st half contour forms a loop ... */
			if (ret.get() == ILfs.LOOP_FOUND)
			{
				/* Need to reverse the 1st half contour so that the points are    */
				/* in consistent order.                                           */
				/* We need to add the original feature point to the list, so      */
				/* set new contour length to one plus length of 1st half contour. */
				noOfContour = nHalf1.get() + 1;
				/* Allocate new contour list. */
				contour = allocateContour(ret, noOfContour);
				if (ret.get() != ILfs.FALSE)
				{
					/* If allcation error, then deallocate memory allocated to */
					/* this point in this routine.                             */
					freeContour(contourHalf1);
					/* Return error code. */
					return (contour);
				}

				/* Otherwise, we have the new contour allocated, so store the */
				/* original feature point.                                    */
				contour.getContourX().set(0, xPixelLoc);
				contour.getContourY().set(0, yPixelLoc);
				contour.getContourEx().set(0, xEdgePixelLoc);
				contour.getContourEy().set(0, yEdgePixelLoc);

				/* Now store the first half contour in reverse order. */
				for (i = 1, j = nHalf1.get() - 1; i < noOfContour; i++, j--)
				{
					contour.getContourX().set(i, contourHalf1.getContourX().get(j));
					contour.getContourY().set(i, contourHalf1.getContourY().get(j));
					contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
					contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));
				}

				 /* Deallocate the first half contour. */
				 freeContour(contourHalf1);

				 /* Assign the output pointers. */
				 oNoOfContour.set(noOfContour);

				 /* Return LOOP_FOUND for further processing. */
				 ret.set(ILfs.LOOP_FOUND);
				 return contour;
			}

			/* Otherwise, return the system error code from the first */
			/* call to trace_contour.                                 */
			return contour;
  		}

		/* If 1st half contour not complete ... */
		if (nHalf1.get() < halfContour)
		{
			/* Deallocate the partial contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.FALSE);
			return contour;
		}

		/* Otherwise, we have a complete 1st half contour...           */
		/* Get 2nd half contour with counter-clockwise neighbor trace. */
		/* Use the last point from the first contour trace as the      */
		/* point to test for a loop when tracing the second contour.   */
		contourHalf2 = traceContour(ret, nHalf2, halfContour, 
			contourHalf1.getContourX().get(nHalf1.get() - 1), contourHalf1.getContourY().get(nHalf1.get() - 1), 
			xPixelLoc, yPixelLoc, xEdgePixelLoc, yEdgePixelLoc, 
			ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		if (ret.get() != ILfs.FALSE)
		{
			/* If 2nd trace was not possible ... */
			if (ret.get() == ILfs.IGNORE)
			{
				/* Deallocate the 1st half contour. */
				freeContour(contourHalf1);
				/* Return, with nothing allocated and contour length equal to 0. */
				ret.set(ILfs.FALSE);
				return contour;
			}

			/* If non-zero return code is NOT LOOP_FOUND, then system error ... */
			if (ret.get() != ILfs.LOOP_FOUND)
			{
				/* Deallocate the 1st half contour. */
				freeContour(contourHalf1);
				/* Return system error. */
				return contour;
			}
		}

		/* If 2nd half NOT a loop AND not complete ... */
		if ((ret.get() != ILfs.LOOP_FOUND) && 
			(nHalf2.get() < halfContour))
		{
			/* Deallocate both half contours. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.FALSE);
			return contour;
		}

		/* Otherwise we have a full 1st half contour and a 2nd half contour */
		/* that is either a loop or complete.  In either case we need to    */
		/* concatenate the two half contours into one longer contour.       */

		/* Allocate output contour list.  Go ahead and allocate the    */
		/* "max_contour" amount even though the resulting contour will */
		/* likely be shorter if it forms a loop.                       */
		contour = allocateContour(ret, maxContour);
		if (ret.get() != ILfs.FALSE)
		{
			/* If allcation error, then deallocate memory allocated to */
			/* this point in this routine.                             */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return error code. */
			return contour;
		}

		/* Set the current contour point counter to 0 */
		noOfContour = 0;

		/* Copy 1st half contour into output contour buffers.      */
		/* This contour was collected clockwise, so it's points    */
		/* are entered in reverse order of the trace.  The result  */
		/* is the first point in the output contour if farthest    */
		/* from the starting feature point.                        */
		for (i = 0, j = nHalf1.get() - 1; i < nHalf1.get(); i++, j--)
		{
			contour.getContourX().set(i, contourHalf1.getContourX().get(j));
			contour.getContourY().set(i, contourHalf1.getContourY().get(j));
			contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
			contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));

			noOfContour++;
		}

		/* Deallocate 1st half contour. */
		freeContour(contourHalf1);

		/* Next, store starting feature point into output contour buffers. */
		contour.getContourX().set(nHalf1.get(), xPixelLoc);
		contour.getContourY().set(nHalf1.get(), yPixelLoc);
		contour.getContourEx().set(nHalf1.get(), xEdgePixelLoc);
		contour.getContourEy().set(nHalf1.get(), yEdgePixelLoc);

		noOfContour++;

		/* Now, append 2nd half contour to permanent contour buffers.  */
		for (i = 0, j = nHalf1.get() + 1; i < nHalf2.get(); i++, j++)
		{
			contour.getContourX().set(j, contourHalf2.getContourX().get(i));
			contour.getContourY().set(j, contourHalf2.getContourY().get(i));
			contour.getContourEx().set(j, contourHalf2.getContourEx().get(i));
			contour.getContourEy().set(j, contourHalf2.getContourEy().get(i));

			noOfContour++;
		}

		/* Deallocate 2nd half contour. */
		freeContour(contourHalf2);

		/* Assign outputs contour to output ponters. */
		oNoOfContour.set (noOfContour);

		/* Return the resulting return code form the 2nd call to trace_contour */
		/* (the value will either be 0 or LOOP_FOUND).                         */
		return contour;
	}

	/*************************************************************************
	**************************************************************************
	#cat: getCenteredContour - Takes the pixel coordinate of a detected
	#cat:            minutia feature point and its corresponding/adjacent edge
	#cat:            pixel and attempts to extract a contour of specified length
	#cat:            of the feature's edge.  The contour is extracted by walking
	#cat:            the feature's edge a specified number of steps clockwise and
	#cat:            then counter-clockwise. If a loop is detected while
	#cat:            extracting the contour, no contour is returned with a return
	#cat:            code of (LOOP_FOUND).  If the process fails to extract a
	#cat:            a complete contour, a code of INCOMPLETE is returned.
	   Input:
	      halfContour 		- half the length of the extracted contour
	                     		(full-length non-loop contour = (half_contourX2)+1)
	      xPixelLoc  		- starting x-pixel coord of feature (interior to feature)
	      yPixelLoc  		- starting y-pixel coord of feature (interior to feature)
	      xEdgePixelLoc 	- x-pixel coord of corresponding edge pixel
	               				(exterior to feature)
	      yEdgePixelLoc 	- y-pixel coord of corresponding edge pixel
	                  		(exterior to feature)
	      binarizedImageData  - binary image data (0==while & 1==black)
	      imageWidth     	- width (in pixels) of image
	      imageHeight     	- height (in pixels) of image
	   Output:
	      ret			- Zero       - resulting contour was successfully extracted or is empty
	      				- LOOP_FOUND - resulting contour forms a complete loop
	      				- IGNORE     - contour could not be traced due to problem starting
	                   					conditions
	      				- INCOMPLETE - resulting contour was not long enough
	      				- Negative   - system error
	      oNoOfContour   - number of contour points returned
	   Return Code:
	   	Contour contains below information
	      ocontourX  - x-pixel coords of contour (interior to feature)
	      ocontourY  - y-pixel coords of contour (interior to feature)
	      ocontourEx - x-pixel coords of corresponding edge (exterior to feature)
	      ocontourEy - y-pixel coords of corresponding edge (exterior to feature)
	**************************************************************************/
	public Contour getCenteredContour(AtomicInteger ret, AtomicInteger oNoOfContour,
        final int halfContour, final int xPixelLoc, final int yPixelLoc, 
        final int xEdgePixelLoc, final int yEdgePixelLoc, 
        int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Contour contour = null, contourHalf1 = null, contourHalf2 = null;
		int maxContour;
		AtomicInteger nHalf1 = new AtomicInteger(0), nHalf2 = new AtomicInteger(0);
		int noOfContour, i, j;
			
		/* Compute maximum length of complete contour */
		/* (2 half contours + feature point).         */
		maxContour = (halfContour << 1) + 1;

		/* Initialize output contour length to 0. */
		oNoOfContour.set(0);

		/* Get 1st half contour with clockwise neighbor trace. */
		contourHalf1 = traceContour(ret, nHalf1, halfContour, 
			xPixelLoc, yPixelLoc, xPixelLoc, yPixelLoc, xEdgePixelLoc, yEdgePixelLoc, 
			ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If system error occurred ... */
		if (ret.get() < ILfs.FALSE)
		{
			/* Return error code. */
			return contour;
		}

		/* If trace was not possible ... */
		if (ret.get() == ILfs.IGNORE)
		{
			/* Return IGNORE, with nothing allocated. */
			ret.set(ILfs.IGNORE);
			return contour;
		}

		/* If 1st half contour forms a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			/* Deallocate loop's contour. */
			freeContour(contourHalf1);
			/* Return LOOP_FOUND, with nothing allocated. */
			ret.set(ILfs.LOOP_FOUND);
			return contour;
		}

		/* If 1st half contour not complete ... */
		if (nHalf1.get() < halfContour)
		{
			/* Deallocate the partial contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.INCOMPLETE);
			return contour;
		}

		/* Otherwise, we have a complete 1st half contour...           */
		/* Get 2nd half contour with counter-clockwise neighbor trace. */
		/* Use the last point from the first contour trace as the      */
		/* point to test for a loop when tracing the second contour.   */
		contourHalf2 = traceContour(ret, nHalf2, halfContour, 
			contourHalf1.getContourX().get(nHalf1.get() - 1), contourHalf1.getContourY().get(nHalf1.get() - 1), 
			xPixelLoc, yPixelLoc, xEdgePixelLoc, yEdgePixelLoc, 
			ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If system error occurred on 2nd trace ... */
		if (ret.get() < ILfs.FALSE)
		{
			/* Return error code. */
			return contour;
		}

		/* If 2nd trace was not possible ... */
		if (ret.get() == ILfs.IGNORE)
		{
			/* Deallocate the 1st half contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */
			
			ret.set(ILfs.IGNORE);
			return contour;
		}

		/* If 2nd trace forms a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND)
		{
			/* Deallocate 1st and 2nd half contours. */
			 freeContour(contourHalf1);
			 freeContour(contourHalf2);
			 /* Return LOOP_FOUND, with nothing allocated. */
			 ret.set(ILfs.LOOP_FOUND);
			return contour;
		}

		/* If 2nd half contour not complete ... */
		if (nHalf2.get() < halfContour)
		{
			/* Deallocate 1st and 2nd half contours. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.INCOMPLETE);
			return contour;
		}

		/* Otherwise we have a full 1st half contour and a 2nd half contour */
		/* that do not form a loop and are complete.  We now need to        */
		/* concatenate the two half contours into one longer contour.       */

		/* Allocate output contour list. */
		contour = allocateContour(ret, maxContour);
		if (ret.get() != ILfs.FALSE)
		{
			/* If allcation error, then deallocate memory allocated to */
			/* this point in this routine.                             */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return error code. */
			return contour;
		}

		/* Set the current contour point counter to 0 */
		noOfContour = 0;

		/* Copy 1st half contour into output contour buffers.      */
		/* This contour was collected clockwise, so it's points    */
		/* are entered in reverse order of the trace.  The result  */
		/* is the first point in the output contour if farthest    */
		/* from the starting feature point.                        */
		for (i = 0, j = nHalf1.get() - 1; i < nHalf1.get(); i++, j--)
		{
			contour.getContourX().set(i, contourHalf1.getContourX().get(j));
			contour.getContourY().set(i, contourHalf1.getContourY().get(j));
			contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
			contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));
			noOfContour++;
		}

		/* Deallocate 1st half contour. */
		freeContour(contourHalf1);

		/* Next, store starting feature point into output contour buffers. */
		contour.getContourX().set(nHalf1.get(), xPixelLoc);
		contour.getContourY().set(nHalf1.get(), yPixelLoc);
		contour.getContourEx().set(nHalf1.get(), xEdgePixelLoc);
		contour.getContourEy().set(nHalf1.get(), yEdgePixelLoc);

		noOfContour++;

		/* Now, append 2nd half contour to permanent contour buffers.  */
		for (i = 0, j = nHalf1.get() + 1; i < nHalf2.get(); i++, j++)
		{
			contour.getContourX().set(j, contourHalf2.getContourX().get(i));
			contour.getContourY().set(j, contourHalf2.getContourY().get(i));
			contour.getContourEx().set(j, contourHalf2.getContourEx().get(i));
			contour.getContourEy().set(j, contourHalf2.getContourEy().get(i));

			noOfContour++;
		}

		/* Deallocate 2nd half contour. */
		freeContour(contourHalf2);

		/* Assign outputs contour to output ponters. */
		oNoOfContour.set(noOfContour);

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return contour;
	}

	/*************************************************************************
	**************************************************************************
	#cat: traceContour - Takes the pixel coordinate of a detected minutia
	#cat:            feature point and its corresponding/adjacent edge pixel
	#cat:            and extracts a contour (up to a specified maximum length)
	#cat:            of the feature's edge in either a clockwise or counter-
	#cat:            clockwise direction.  A second point is specified, such that
	#cat:            if this point is encounted while extracting the contour,
	#cat:            it is to be assumed that a loop has been found and a code
	#cat:            of (LOOP_FOUND) is returned with the contour. By independently
	#cat:            specifying this point, successive calls can be made to
	#cat:            this routine from the same starting point, and loops across
	#cat:            successive calls can be detected.
	   Input:
	      maxLenOfContour - maximum length of contour to be extracted
	      xLoop  - x-pixel coord of point, if encountered, triggers LOOP_FOUND
	      yLoop  - y-pixel coord of point, if encountered, triggers LOOP_FOUND
	      xPixelLoc   - starting x-pixel coord of feature (interior to feature)
	      yPixelLoc   - starting y-pixel coord of feature (interior to feature)
	      xEdgePixelLoc  - x-pixel coord of corresponding edge pixel (exterior to feature)
	      yEdgePixelLoc  - y-pixel coord of corresponding edge pixel (exterior to feature)
	      scanClock - direction in which neighboring pixels are to be scanned
	                for the next contour pixel
	      binarizedImageData  - binary image data (0==while & 1==black)
	      imageWidth     - width (in pixels) of image
	      imageHeight     - height (in pixels) of image
	   Output:
	      ret		- Zero       - resulting contour was successfully allocated and extracted
	      			- LOOP_FOUND - resulting contour forms a complete loop
	      			- IGNORE     - trace is not possible due to state of inputs
	      			- Negative   - system error
	      oNoOfContour   - number of contour points returned
	   Return Code:
	   	Contour contains below information
	      ocontourX  - x-pixel coords of contour (interior to feature)
	      ocontourY  - y-pixel coords of contour (interior to feature)
	      ocontourEx - x-pixel coords of corresponding edge (exterior to feature)
	      ocontourEy - y-pixel coords of corresponding edge (exterior to feature)
	**************************************************************************/
	public Contour traceContour(AtomicInteger ret, AtomicInteger oNoOfContour, 
		final int maxLenOfContour, final int xLoop, final int yLoop, final int xPixelLoc, final int yPixelLoc, 
		final int xEdgePixelLoc, final int yEdgePixelLoc, final int scanClock, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight) {
		Contour contour = null;
		int noOfContour;
		int currentXPixelLoc, currentYPixelLoc;
		int currentXEdgePixelLoc, currentYEdgePixelLoc;
		AtomicInteger nextXPixelLoc = new AtomicInteger(0), nextYPixelLoc = new AtomicInteger(0);
		AtomicInteger nextXEdgePixelLoc = new AtomicInteger(0), nextYEdgePixelLoc = new AtomicInteger(0);
		int i;

		/* Check to make sure that the feature and edge values are opposite. */
		if (binarizedImageData[0 + (yPixelLoc * imageWidth) + xPixelLoc] == 
			binarizedImageData [0 + (yEdgePixelLoc * imageWidth) + xEdgePixelLoc])
		{
			/* If not opposite, then the trace will not work, so return IGNORE. */
			ret.set(ILfs.IGNORE);
			return (contour);
		}

		/* Allocate contour buffers. */
		contour = allocateContour(ret, maxLenOfContour);
		if (ret.get() != ILfs.FALSE)
		{
			/* If allocation error, return code. */
			return (contour);
		}

		/* Set pixel counter to 0. */
		noOfContour = 0;

		/* Set up for finding first contour pixel. */
		currentXPixelLoc = xPixelLoc;
		currentYPixelLoc = yPixelLoc;
		currentXEdgePixelLoc = xEdgePixelLoc;
		currentYEdgePixelLoc = yEdgePixelLoc;

		/* Foreach pixel to be collected on the feature's contour... */
		for (i = 0; i < maxLenOfContour; i++)
		{
			/* Find the next contour pixel. */
			if (nextContourPixel(nextXPixelLoc, nextYPixelLoc, nextXEdgePixelLoc, nextYEdgePixelLoc,
				currentXPixelLoc, currentYPixelLoc, currentXEdgePixelLoc, currentYEdgePixelLoc, 
				scanClock, binarizedImageData, imageWidth, imageHeight) == ILfs.TRUE)
			{
				/* If we trace back around to the specified starting */
				/* feature location...                               */
				if ((nextXPixelLoc.get() == xLoop) && 
					(nextYPixelLoc.get() == yLoop))
				{
					/* Then we have found a loop, so return what we */
					/* have traced to this point.                   */

					oNoOfContour.set(noOfContour);
					ret.set(ILfs.LOOP_FOUND);
					return (contour);
				 }

				 /* Otherwise, we found another point on our feature's contour, */
				 /* so store the new contour point.                             */
				contour.getContourX().set(i, nextXPixelLoc.get());
				contour.getContourY().set(i, nextYPixelLoc.get());
				contour.getContourEx().set(i, nextXEdgePixelLoc.get());
				contour.getContourEy().set(i, nextYEdgePixelLoc.get());
				
				/* Bump the number of points stored. */
				noOfContour++;

				/* Set up for finding next contour pixel. */
				currentXPixelLoc = nextXPixelLoc.get();
				currentYPixelLoc = nextYPixelLoc.get();
				currentXEdgePixelLoc = nextXEdgePixelLoc.get();
				currentYEdgePixelLoc = nextYEdgePixelLoc.get();
			}
			/* Otherwise, no new contour point found ... */
			else
			{
				/* So, stop short and return the number of pixels found */
				/* on the contour to this point.                        */
				oNoOfContour.set(noOfContour);

				/* Return normally. */
				ret.set(ILfs.FALSE);
				return (contour);
			}
		}

		/* If we get here, we successfully found the maximum points we    */
		/* were looking for on the feature contour, so assign the contour */
		/* buffers to the output pointers and return.                     */
		oNoOfContour.set(noOfContour);

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return (contour);
	}

	/*************************************************************************
	**************************************************************************
	#cat: searchContour - Walk the contour of a minutia feature starting at a
	#cat:            specified point on the feature and walking N steps in the
	#cat:            specified direction (clockwise or counter-clockwise), looking
	#cat:            for a second specified point.  In this code, "feature" is
	#cat:            consistently referring to either the black interior edge of
	#cat:            a ridge-ending or the white interior edge of a valley-ending
	#cat:            (bifurcation).  The term "edge of the feature" refers to
	#cat:            neighboring pixels on the "exterior" edge of the feature.
	#cat:            So "edge" pixels are opposite in color from the interior
	#cat:            feature pixels.
	   Input:
	      xPixelSearch   	- x-pixel coord of point being searched for
	      yPixelSearch   	- y-pixel coord of point being searched for
	      searchLen 		- number of step to walk contour in search
	      xPixelLoc      	- starting x-pixel coord of feature (interior to feature)
	      yPixelLoc      	- starting y-pixel coord of feature (interior to feature)
	      xEdgePixelLoc     - x-pixel coord of corresponding edge pixel
	                   		(exterior to feature)
	      yEdgePixelLoc     - y-pixel coord of corresponding edge pixel
	                   		(exterior to feature)
	      scanClock 		- direction in which neighbor pixels are to be scanned
	                   		(clockwise or counter-clockwise)
	      binarizedImageData  	- binary image data (0==while & 1==black)
	      imageWidth         	- width (in pixels) of image
	      imageHeight         	- height (in pixels) of image
	   Return Code:
	      NOT_FOUND  - desired pixel not found along N steps of feature's contour
	      FOUND      - desired pixel WAS found along N steps of feature's contour
	**************************************************************************/
	public int searchContour(final int xPixelSearch, final int yPixelSearch, final int searchLen, 
		final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc, 
		final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int currentXPixelLoc, currentYPixelLoc;
		int currentXEdgePixelLoc, currentYEdgePixelLoc;
		AtomicInteger nextXPixelLoc = new AtomicInteger(0), nextYPixelLoc = new AtomicInteger(0);
		AtomicInteger nextXEdgePixelLoc = new AtomicInteger(0), nextYEdgePixelLoc = new AtomicInteger(0);
		int i;

		/* Set up for finding first contour pixel. */
		currentXPixelLoc = xPixelLoc;
		currentYPixelLoc = yPixelLoc;
		currentXEdgePixelLoc = xEdgePixelLoc;
		currentYEdgePixelLoc = yEdgePixelLoc;

		/* Foreach point to be collected on the feature's contour... */
		for (i = 0; i < searchLen; i++)
		{
			/* Find the next contour pixel. */
			if (nextContourPixel(nextXPixelLoc, nextYPixelLoc, nextXEdgePixelLoc, nextYEdgePixelLoc, 
				currentXPixelLoc, currentYPixelLoc, currentXEdgePixelLoc, currentYEdgePixelLoc, 
				scanClock, binarizedImageData, imageWidth, imageHeight) == ILfs.TRUE)
			{
				/* If we find the point we are looking for on the contour... */
				if ((nextXPixelLoc.get() == xPixelSearch) && (nextYPixelLoc.get() == yPixelSearch))
				{
					/* Then return FOUND. */
					return (ILfs.FOUND);
				}

				/* Otherwise, set up for finding next contour pixel. */
				currentXPixelLoc = nextXPixelLoc.get();
				currentYPixelLoc = nextYPixelLoc.get();
				currentXEdgePixelLoc = nextXEdgePixelLoc.get();
				currentYEdgePixelLoc = nextYEdgePixelLoc.get();
			}
			/* Otherwise, no new contour point found ... */
			else
			{
				/* So, stop searching, and return NOT_FOUND. */
				return (ILfs.NOT_FOUND);
			}
		}

		/* If we get here, we successfully searched the maximum points */
		/* without finding our desired point, so return NOT_FOUND.     */
		return (ILfs.NOT_FOUND);
	}

	/*************************************************************************
	**************************************************************************
	#cat: nextContourPixel - Takes a pixel coordinate of a point determined
	#cat:            to be on the interior edge of a feature (ridge or valley-
	#cat:            ending), and attempts to locate a neighboring pixel on the
	#cat:            feature's contour.  Neighbors of the current feature pixel
	#cat:            are searched in a specified direction (clockwise or counter-
	#cat:            clockwise) and the first pair of adjacent/neigboring pixels
	#cat:            found with the first pixel having the color of the feature
	#cat:            and the second the opposite color are returned as the next
	#cat:            point on the contour.  One exception happens when the new
	#cat:            point is on an "exposed" corner.
	   Input:
	      currentXPixelLoc  	- x-pixel coord of current point on feature's
	                   			interior contour
	      currentYPixelLoc  	- y-pixel coord of current point on feature's
	                   			interior contour
	      currentXEdgePixelLoc 	- x-pixel coord of corresponding edge pixel
	                   			(exterior to feature)
	      currentYEdgePixelLoc 	- y-pixel coord of corresponding edge pixel
	                   			(exterior to feature)
	      scanClock 			- direction in which neighboring pixels are to be scanned
	                   			for the next contour pixel
	      binarizedImageData   	- binary image data (0==while & 1==black)
	      imageWidth         	- width (in pixels) of image
	      imageHeight         	- height (in pixels) of image
	   Output:
	      nextXPixelLoc  	- x-pixel coord of next point on feature's interior contour
	      nextYPixelLoc  	- y-pixel coord of next point on feature's interior contour
	      nextXEdgePixelLoc - x-pixel coord of corresponding edge (exterior to feature)
	      nextYEdgePixelLoc - y-pixel coord of corresponding edge (exterior to feature)
	   Return Code:
	      TRUE  - next contour point found and returned
	      FALSE - next contour point NOT found
	**************************************************************************/
	public int nextContourPixel(AtomicInteger nextXPixelLoc, AtomicInteger nextYPixelLoc, 
		AtomicInteger nextXEdgePixelLoc, AtomicInteger nextYEdgePixelLoc, 
		final int currentXPixelLoc, final int currentYPixelLoc,
		final int currentXEdgePixelLoc, final int currentYEdgePixelLoc, final int scanClock, 
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int featurePixel, edgePixel;
		int previousNbrPixel, previousNbrXPixel, previousNbrYPixel;
		int currentNbrPixel, currentNbrXPixel, currentNbrYPixel;
		int ni, nx, ny, npix;
		int nbrIndex, i;

		/* Get the feature's pixel value. */
		featurePixel = binarizedImageData[0 + (currentYPixelLoc * imageWidth) + currentXPixelLoc];
		/* Get the feature's edge pixel value. */
		edgePixel = binarizedImageData[0 + (currentYEdgePixelLoc * imageWidth) + currentXEdgePixelLoc];

		/* Get the nieghbor position of the feature's edge pixel in relationship */
		/* to the feature's actual position.                                     */
		/* REMEBER: The feature's position is always interior and on a ridge     */
		/* ending (black pixel) or (for bifurcations) on a valley ending (white  */
		/* pixel).  The feature's edge pixel is an adjacent pixel to the feature */
		/* pixel that is exterior to the ridge or valley ending and opposite in  */
		/* pixel value.                                                          */
		nbrIndex = 
			startScanNbr(currentXPixelLoc, currentYPixelLoc, 
				currentXEdgePixelLoc, currentYEdgePixelLoc);

		/* Set current neighbor scan pixel to the feature's edge pixel. */
		currentNbrXPixel = currentXEdgePixelLoc;
		currentNbrYPixel = currentYEdgePixelLoc;
		currentNbrPixel = edgePixel;

		/* Foreach pixel neighboring the feature pixel ... */
		for (i = 0; i < 8; i++)
		{
			/* Set current neighbor scan pixel to previous scan pixel. */
			previousNbrXPixel = currentNbrXPixel;
			previousNbrYPixel = currentNbrYPixel;
			previousNbrPixel = currentNbrPixel;

			/* Bump pixel neighbor index clockwise or counter-clockwise. */
			nbrIndex = nextScanNbr(nbrIndex, scanClock);

			/* Set current scan pixel to the new neighbor.                   */
			/* REMEMBER: the neighbors are being scanned around the original */
			/* feature point.                                                */
			currentNbrXPixel = currentXPixelLoc + getGlobals().getNbr8Dx()[nbrIndex];
			currentNbrYPixel = currentYPixelLoc + getGlobals().getNbr8Dy()[nbrIndex];

			/* If new neighbor is not within image boundaries... */
			if ((currentNbrXPixel < 0) || (currentNbrXPixel >= imageWidth) || 
				(currentNbrYPixel < 0) || (currentNbrYPixel >= imageHeight))
			{
				/* Return (FALSE==>Failure) if neighbor out of bounds. */
				return ILfs.FALSE;
	 		}

			/* Get the new neighbor's pixel value. */
			currentNbrPixel = binarizedImageData[0 + (currentNbrYPixel * imageWidth) + currentNbrXPixel];

			/* If the new neighbor's pixel value is the same as the feature's   */
			/* pixel value AND the previous neighbor's pixel value is the same  */
			/* as the features's edge, then we have "likely" found our next     */
			/* contour pixel.                                                   */
			if ((currentNbrPixel == featurePixel) && 
				(previousNbrPixel == edgePixel))
			{
				/* Check to see if current neighbor is on the corner of the */
				/* neighborhood, and if so, test to see if it is "exposed". */
				/* The neighborhood corners have odd neighbor indicies.     */
				if ((nbrIndex % 2) != ILfs.FALSE)
				{
					/* To do this, look ahead one more neighbor pixel. */
					ni = nextScanNbr(nbrIndex, scanClock);
					nx = currentXPixelLoc + getGlobals().getNbr8Dx()[ni];
					ny = currentYPixelLoc + getGlobals().getNbr8Dy()[ni];
					/* If new neighbor is not within image boundaries... */
					if ((nx < 0) || (nx >= imageWidth) || 
						(ny < 0) || (ny >= imageHeight))
					{
						/* Return (FALSE==>Failure) if neighbor out of bounds. */
						return ILfs.FALSE;
					}
					npix = binarizedImageData[0 + (ny * imageWidth) + nx];

					/* If the next neighbor's value is also the same as the */
					/* feature's pixel, then corner is NOT exposed...       */
					if (npix == featurePixel)
					{
						/* Assign the current neighbor pair to the output pointers. */
						nextXPixelLoc.set(currentNbrXPixel);
						nextYPixelLoc.set(currentNbrYPixel);
						nextXEdgePixelLoc.set(previousNbrXPixel);
						nextYEdgePixelLoc.set(previousNbrYPixel);
						/* Return TRUE==>Success. */
						return ILfs.TRUE;
					}
					/* Otherwise, corner pixel is "exposed" so skip it. */
					else
					{
						/* Skip current corner neighbor by resetting it to the      */
						/* next neighbor, which upon the iteration will immediately */
						/* become the previous neighbor.                            */
						currentNbrXPixel = nx;
						currentNbrYPixel = ny;
						currentNbrPixel = npix;
						/* Advance neighbor index. */
						nbrIndex = ni;
						/* Advance neighbor count. */
						i++;
					}
				}
				/* Otherwise, current neighbor is not a corner ... */
				else
				{
					/* Assign the current neighbor pair to the output pointers. */
					nextXPixelLoc.set(currentNbrXPixel);
					nextYPixelLoc.set(currentNbrYPixel);
					nextXEdgePixelLoc.set(previousNbrXPixel);
					nextYEdgePixelLoc.set(previousNbrYPixel);
					/* Return TRUE==>Success. */
					return ILfs.TRUE;
				}
			}
		}

		/* If we get here, then we did not find the next contour pixel */
		/* within the 8 neighbors of the current feature pixel so      */
		/* return (FALSE==>Failure).                                   */
		/* NOTE: This must mean we found a single isolated pixel.      */
		/*       Perhaps this should be filled?                        */
		return ILfs.FALSE;
	}

	/*************************************************************************
	**************************************************************************
	#cat: startScanNbr - Takes a two pixel coordinates that are either
	#cat:            aligned north-to-south or east-to-west, and returns the
	#cat:            position the second pixel is in realtionship to the first.
	#cat:            The positions returned are based on 8-connectedness.
	#cat:            NOTE, this routine does NOT account for diagonal positions.
	   Input:
	      previousXPixelLoc - x-coord of first point
	      previousYPixelLoc - y-coord of first point
	      nextXPixelLoc 	- x-coord of second point
	      nextYPixelLoc 	- y-coord of second point
	   Return Code:
	      NORTH - second pixel above first
	      SOUTH - second pixel below first
	      EAST  - second pixel right of first
	      WEST  - second pixel left of first
	**************************************************************************/
	public int startScanNbr(final int previousXPixelLoc, final int previousYPixelLoc, final int nextXPixelLoc, final int nextYPixelLoc) {
		if ((previousXPixelLoc == nextXPixelLoc) && (nextYPixelLoc > previousYPixelLoc))
		{
			return (ILfs.SOUTH);
		}
		else if ((previousXPixelLoc == nextXPixelLoc) && (nextYPixelLoc < previousYPixelLoc))
		{
			return (ILfs.NORTH);
		}
		else if ((nextXPixelLoc > previousXPixelLoc) && (previousYPixelLoc == nextYPixelLoc))
		{
			return (ILfs.EAST);
		}
		else if ((nextXPixelLoc < previousXPixelLoc) && (previousYPixelLoc == nextYPixelLoc))
		{
			return (ILfs.WEST);
		}

		/* Added by MDG on 03-16-05 */
		/* Should never reach here.  Added to remove compiler warning. */
		return (ILfs.INVALID_DIR); // -1
	}

	/*************************************************************************
	**************************************************************************
	#cat: nextScanNbr - Advances the given 8-connected neighbor index
	#cat:            on location in the specifiec direction (clockwise or
	#cat:            counter-clockwise). 
	   Input:
	      nbrIndex      - current 8-connected neighbor index
	      scanClock - direction in which the neighbor index is to be advanced
	   Return Code:
	      Next neighbor - 8-connected index of next neighbor
	**************************************************************************/
	public int nextScanNbr(final int nbrIndex, final int scanClock) {
		int nextNeighborIndex;

		/* If scanning neighbors clockwise ... */
		if (scanClock == ILfs.SCAN_CLOCKWISE)
		{
			/* Advance one neighbor clockwise. */
			nextNeighborIndex = (nbrIndex + 1) % 8;
  		}
		/* Otherwise, scanning neighbors counter-clockwise ... */
		else
		{
			/* Advance one neighbor counter-clockwise.         */
			/* There are 8 pixels in the neighborhood, so to   */
			/* decrement with wrapping from 0 around to 7, add */
			/* the nieghbor index by 7 and mod with 8.         */
			nextNeighborIndex = (nbrIndex + 7) % 8;
		}

		/* Return the new neighbor index. */
		return (nextNeighborIndex);
	}

	/*************************************************************************
	**************************************************************************
	#cat: min_contour_theta - Takes a contour list and analyzes it locating the
	#cat:            point at which the contour has highest curvature
	#cat:            (or minimum interior angle).  The angle of curvature is
	#cat:            computed by searching a majority of points on the contour.
	#cat:            At each of these points, a left and right segment (or edge)
	#cat:            are extended out N number of pixels from the center point
	#cat:            on the contour.  The angle is formed between the straight line
	#cat:            connecting the center point to the end point on the left edge
	#cat:            and the line connecting the center point to the end of the
	#cat:            right edge.  The point of highest curvature is determined
	#cat:            by locating the where the minimum of these angles occurs.
	   Input:
	      angleEdge - length of the left and right edges extending from a
	                   common/centered pixel on the contour
	      contourX  - x-coord list for contour points
	      contourY  - y-coord list for contour points
	      noOfContour   - number of points in contour
	   Output:
	      oMinContourPoint     - index of contour point where minimum occurred
	      oMinThetaAngle - minimum angle found along the contour
	   Return Code:
	      Zero       - minimum angle successfully located
	      IGNORE     - ignore the contour
	      Negative   - system error
	**************************************************************************/
	public int minContourTheta(AtomicInteger oMinContourPoint, AtomicReference<Double> oMinThetaAngle, 
		final int angleEdge, AtomicIntegerArray contourX, AtomicIntegerArray contourY,
		final int noOfContour) {
		int pointLeft, pointCenter, pointRight;
		double theta1, theta2, dtheta;
		int minContourPoint;
		double minThetaAngle;

		/* If the contour length is too short for processing... */
		if (noOfContour < (angleEdge << 1) + 1)
		{
			/* Return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Intialize running minimum values. */
		minThetaAngle = Math.PI;
		/* Need to truncate precision so that answers are consistent   */
		/* on different computer architectures when comparing doubles. */
		minThetaAngle = getDefs().truncDoublePrecision(minThetaAngle, ILfs.TRUNC_SCALE);
		minContourPoint = -1;

		/* Set left angle point to first contour point. */
		pointLeft = 0;
		/* Set center angle point to "angleEdge" points into contour. */
		pointCenter = angleEdge;
		/* Set right angle point to "angleEdge" points from pcenter. */
		pointRight = pointCenter + angleEdge;

		/* Loop until the right angle point exceeds the contour list. */
		while (pointRight < noOfContour)
		{
			/* Compute angle to first edge line (connecting pcenter to pleft). */
			theta1 = getLfsUtil().angleToLine(contourX.get(pointCenter), contourY.get(pointCenter), 
				contourX.get(pointLeft), contourY.get(pointLeft));
			/* Compute angle to second edge line (connecting pcenter to pright). */
			theta2 = getLfsUtil().angleToLine(contourX.get(pointCenter), contourY.get(pointCenter), 
				contourX.get(pointRight),contourY.get(pointRight));

			/* Compute delta between angles accounting for an inner */
			/* and outer distance between the angles.               */
			dtheta = Math.abs(theta2 - theta1);
			dtheta = Math.min(dtheta, (Math.PI * 2.0) - dtheta);
			/* Need to truncate precision so that answers are consistent   */
			/* on different computer architectures when comparing doubles. */
			dtheta = getDefs().truncDoublePrecision(dtheta, ILfs.TRUNC_SCALE);

			/* Keep track of running minimum theta. */
			if (dtheta < minThetaAngle)
			{
				minContourPoint = pointCenter;
				minThetaAngle = dtheta;
			}

			/* Bump to next points on contour. */
			pointLeft++;
			pointCenter++;
			pointRight++;
		}

		/* If no minimum found (then contour is perfectly flat) so minimum */
		/* to center point on contour.                                     */
		if (minContourPoint == ILfs.UNDEFINED)
		{
			oMinContourPoint.set(noOfContour >> 1);
			oMinThetaAngle.set(minThetaAngle);
		}
		else
		{
			/* Assign minimum theta information to output pointers. */
			oMinContourPoint.set(minContourPoint);
			oMinThetaAngle.set(minThetaAngle);
		}

		/* Return successfully. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: contourLimits - Determines the X and Y coordinate limits of the
	#cat:                  given contour list.
	   Input:
	      contourX  - x-coord list for contour points
	      contourY  - y-coord list for contour points
	      noOfContour   - number of points in contour
	   Output:
	      xMin   - left-most x-coord in contour
	      yMin   - top-most y-coord in contour
	      xMax   - right-most x-coord in contour
	      yMax   - bottom-most y-coord in contour
	**************************************************************************/
	public void contourLimits(AtomicInteger xMin, AtomicInteger yMin, 
		AtomicInteger xMax, AtomicInteger yMax, 
		AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfContour) {
		/* Find the minimum x-coord from the list of contour points. */
		xMin.set(getLfsUtil().minValue(contourX, noOfContour));
		/* Find the minimum y-coord from the list of contour points. */
		yMin.set(getLfsUtil().minValue(contourY, noOfContour));
		/* Find the maximum x-coord from the list of contour points. */
		xMax.set(getLfsUtil().maxValue(contourX, noOfContour));
		/* Find the maximum y-coord from the list of contour points. */
		yMax.set(getLfsUtil().maxValue(contourY, noOfContour));		
	}

	/*************************************************************************
	**************************************************************************
	#cat: fixEdgePixelPair - Takes a pair of pixel points with the first
	#cat:       pixel on a feature and the second adjacent and off the feature,
	#cat:       determines if the pair neighbor diagonally.  If they do, their
	#cat:       locations are adjusted so that the resulting pair retains the
	#cat:       same pixel values, but are neighboring either to the N,S,E or W.
	#cat:       This routine is needed in order to prepare the pixel pair for
	#cat:       contour tracing.
	   Input:
	      featureXPixel  	- pointer to x-pixel coord on feature
	      featureYPixel  	- pointer to y-pixel coord on feature
	      featureEdgeXPixel - pointer to x-pixel coord on edge of feature
	      featureEdgeYPixel - pointer to y-pixel coord on edge of feature
	      binarizedImageData   	- binary image data (0==while & 1==black)
	      imageWidth      		- width (in pixels) of image
	      imageHeight      		- height (in pixels) of image
	   Output:
	      featureXPixel  - pointer to resulting x-pixel coord on feature
	      featureYPixel  - pointer to resulting y-pixel coord on feature
	      featureEdgeXPixel  - pointer to resulting x-pixel coord on edge of feature
	      featureEdgeYPixel  - pointer to resulting y-pixel coord on edge of feature
	**************************************************************************/
	public void fixEdgePixelPair(AtomicInteger featureXPixel, AtomicInteger featureYPixel, 
		AtomicInteger featureEdgeXPixel, AtomicInteger featureEdgeYPixel, 
		int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int distPixelX, distPixelY, previousFeaturePixelX, previousFeaturePixelY, currentFeaturePixelX, currentFeaturePixelY;
		int featurePixel;

		/* Get the pixel value of the feature. */
		featurePixel = binarizedImageData[0 + (featureYPixel.get() * imageWidth) + featureXPixel.get()];

		/* Store the input points to current and previous points. */
		currentFeaturePixelX = featureXPixel.get();
		currentFeaturePixelY = featureYPixel.get();
		previousFeaturePixelX = featureEdgeXPixel.get();
		previousFeaturePixelY = featureEdgeYPixel.get();

		/* Compute detlas between current and previous point. */
		distPixelX = previousFeaturePixelX - currentFeaturePixelX;
		distPixelY = previousFeaturePixelY - currentFeaturePixelY;

		/* If previous point (P) is diagonal neighbor of    */
		/* current point (C)... This is a problem because   */
		/* the contour tracing routine requires that the    */
		/* "edge" pixel be north, south, east, or west of   */
		/* of the feature point.  If the previous pixel is  */
		/* diagonal neighbor, then we need to adjust either */
		/* the positon of te previous or current pixel.     */
		if ((Math.abs(distPixelX) == ILfs.TRUE) && (Math.abs(distPixelY) == ILfs.TRUE))
		{
			/* Then we have one of the 4 following conditions:  */
			  /*                                                  */
			  /*        *C                             C*         */
			  /*    1.  P*     2.  P*     3. *P     4. *P         */
			  /*                   *C        C*                   */
			  /*                                                  */
			  /*  dx =  -1         -1         1         1         */
			  /*  dy =   1         -1        -1         1         */
			  /*                                                  */
			  /* Want to test values in positions of '*':         */
			  /*  Let point P == (px, py)                         */
			  /*           p1 == '*' positon where x changes      */
			  /*           p2 == '*' positon where y changes      */
			  /*                                                  */
			  /*  p1 = px+1,py    px+1,py   px-1,py    px-1,py    */
			  /*  p2 = px,py-1    px,py+1   px,py+1    px,py-1    */
			  /*                                                  */
			  /* These can all be rewritten:                      */
			  /*  p1 = px-dx,py                                   */
			  /*  p2 = px,py-dy                                   */

			/* Check if 'p1' is NOT the value we are searching for... */
			if (binarizedImageData[0 + (previousFeaturePixelY * imageWidth) + (previousFeaturePixelX - distPixelX)] 
				!= featurePixel)
			{
				/* Then set x-coord of edge pixel to p1. */
				previousFeaturePixelX -= distPixelX;
			}
			/* Check if 'p2' is NOT the value we are searching for... */
			else if (binarizedImageData[0 + ((previousFeaturePixelY - distPixelY) * imageWidth) + previousFeaturePixelX] 
				!= featurePixel)
			{
				/* Then set y-coord of edge pixel to p2. */
				previousFeaturePixelY -= distPixelY;
			}
			/* Otherwise, the current pixel 'C' is exposed on a corner ... */
			else
			{
				/* Set pixel 'C' to 'p1', which also has the pixel */
				/* value we are searching for.                     */
				currentFeaturePixelY += distPixelY;
			}

			/* Set the pointers to the resulting values. */
			featureXPixel.set(currentFeaturePixelX);
			featureYPixel.set(currentFeaturePixelY);
			featureEdgeXPixel.set(previousFeaturePixelX);
			featureEdgeYPixel.set(previousFeaturePixelY);
		}

		/* Otherwise, nothing has changed. */
	}

	public AtomicIntegerArray getContourX() {
		return contourX;
	}
	public void setContourX(AtomicIntegerArray contourX) {
		this.contourX = contourX;
	}

	public AtomicIntegerArray getContourY() {
		return contourY;
	}
	public void setContourY(AtomicIntegerArray contourY) {
		this.contourY = contourY;
	}

	public AtomicIntegerArray getContourEx() {
		return contourEx;
	}
	public void setContourEx(AtomicIntegerArray contourEx) {
		this.contourEx = contourEx;
	}

	public AtomicIntegerArray getContourEy() {
		return contourEy;
	}
	public void setContourEy(AtomicIntegerArray contourEy) {
		this.contourEy = contourEy;
	}

	public int getNoOfContour() {
		return noOfContour;
	}
	public void setNoOfcontour(int noOfContour) {
		this.noOfContour = noOfContour;
	}
	
	@Override
	public String toString() {
		return "Contour [contourX=" + contourX + ", contourY=" + contourY + ", contourEx=" + contourEx + ", contourEy="
				+ contourEy + ", noOfContour=" + noOfContour + "]";
	}	
}