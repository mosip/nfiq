package io.mosip.nist.nfiq1.mindtct;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.Defs;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IImageUtil;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;

public class ImageUtil extends MindTct implements IImageUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);	

	private static volatile ImageUtil instance;
    public static ImageUtil getInstance() {
        if (instance == null) {
            synchronized (ImageUtil.class) {
                if (instance == null) {
                    instance = new ImageUtil();
                }
            }
        }
        return instance;
    }    
    private ImageUtil()
    {
    	super();
    }
    
	public Defs getDefs() {
		return Defs.getInstance();
	}	
	public Line getLine() {
		return Line.getInstance();
	}
	public Free getFree() {
		return Free.getInstance();
	}
	public Contour getContour() {
		return Contour.getInstance();
	}
	
	/*************************************************************************
	**************************************************************************
	#cat: bits6To8 - Takes an array of unsigned characters and bitwise shifts
	#cat:             each value 2 postitions to the left.  This is equivalent
	#cat:             to multiplying each value by 4.  This puts original values
	#cat:             on the range [0..64) now on the range [0..256).  Another
	#cat:             way to say this, is the original 6-bit values now fit in
	#cat:             8 bits.  This is to be used to undo the effects of bits_8to6.
	   Input:
	      imageData - input array of unsigned characters
	      imageWidth    - width (in characters) of the input array
	      imageHeight    - height (in characters) of the input array
	   Output:
	      imageData - contains the bit-shifted results
	**************************************************************************/
	public void bits6To8(int[] imageData, int imageWidth, int imageHeight) {		
		int imageSize;
		int iptrIndex;

		imageSize = imageWidth * imageHeight;
		iptrIndex = 0;
		for (int i = 0; i < imageSize; i++)
		{
			/* Multiply every pixel value by 4 so that [0..64) -> [0..255) */
			imageData[iptrIndex++] <<= 2;
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: bits8To6 - Takes an array of unsigned characters and bitwise shifts
	#cat:             each value 2 postitions to the right.  This is equivalent
	#cat:             to dividing each value by 4.  This puts original values
	#cat:             on the range [0..256) now on the range [0..64).  Another
	#cat:             way to say this, is the original 8-bit values now fit in
	#cat:             6 bits.  I would really like to make this dependency
	#cat:             go away.
	   Input:
	      imageData - input array of unsigned characters
	      imageWidth    - width (in characters) of the input array
	      imageHeight    - height (in characters) of the input array
	   Output:
	      imageData - contains the bit-shifted results
	**************************************************************************/
	public void bits8To6(int[] imageData, int imageWidth, int imageHeight) {
		int imageSize;
		int iptrIndex;

		imageSize = imageWidth * imageHeight;
		iptrIndex = 0;
		for (int i = 0; i < imageSize; i++)
		{
			/* Divide every pixel value by 4 so that [0..256) -> [0..64) */
			imageData[iptrIndex++] >>= 2;
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: grayToBinary - Takes an 8-bit threshold value and two 8-bit pixel values.
	#cat:            Those pixels in the image less than the threhsold are set
	#cat:            to the first specified pixel value, whereas those pixels
	#cat:            greater than or equal to the threshold are set to the second
	#cat:            specified pixel value.  On application for this routine is
	#cat:            to convert binary images from 8-bit pixels valued {0,255} to
	#cat:            {1,0} and vice versa.
	   Input:
	      threshold      - 8-bit pixel threshold
	      lessPixel    - pixel value used when image pixel is < threshold
	      greaterPixel - pixel value used when image pixel is >= threshold
	      binarizedmageData       - 8-bit image data
	      imageWidth          - width (in pixels) of the image
	      imageHeight          - height (in pixels) of the image
	   Output:
	      binarizedmageData       - altered 8-bit image data
	**************************************************************************/
	public void grayToBinary(final int threshold, final int lessPixel, final int greaterPixel, 
		int[] binarizedmageData, final int imageWidth, final int imageHeight) {
		int imageSize = imageWidth * imageHeight;
		for (int i = 0; i < imageSize; i++)
		{
			if (binarizedmageData[i]>= threshold)
			{
				binarizedmageData[i] = greaterPixel;
			}
			else
			{
				binarizedmageData[i] = lessPixel;
			}
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: padImage - Copies an 8-bit grayscale images into a larger
	#cat:                   output image centering the input image so as to
	#cat:                   add a specified amount of pixel padding along the
	#cat:                   entire perimeter of the input image.  The amount of
	#cat:                   pixel padding and the intensity of the pixel padding
	#cat:                   are specified.  An alternative to padding with a
	#cat:                   constant intensity would be to copy the edge pixels
	#cat:                   of the centered image into the adjacent pad area.
	   Input:
	      imageData     - input 8-bit grayscale image
	      imageWidth        - width (in pixels) of the input image
	      imageHeight        - height (in pixels) of the input image
	      pad       - size of padding (in pixels) to be added
	      padValue - intensity of the padded area
	   Output:
	      ret		- Zero     - successful completion
	      			- Negative - system error
	      ow        - width (in pixels) of the padded image
	      oh        - height (in pixels) of the padded image
	   Return Code:
	      optr      - points to the newly padded image
	**************************************************************************/
	public int[] padImage(AtomicInteger ret, AtomicInteger ow, AtomicInteger oh, 
		int[] imageData, final int imageWidth, final int imageHeight, final int pad, final int padValue) {
		int[] paddedImagedata;
		int pptrIndex, imageDataIndex, pdataIndex;
		int paddedImageWidth, paddedImageHeight;
		int pad2, paddedImageSize;

		/* Account for pad on both sides of image */
		pad2 = pad << 1;

		/* Compute new pad sizes */
		paddedImageWidth = imageWidth + pad2;
		paddedImageHeight = imageHeight + pad2;
		paddedImageSize = paddedImageWidth * paddedImageHeight;

		/* Allocate padded image */
		paddedImagedata = new int [paddedImageSize];

		/* Initialize values to a constant PAD value */
		//for (i = 0; i < paddedImageSize; i++)
		Arrays.fill(paddedImagedata, 0, paddedImageSize, padValue);

		/* Copy input image into padded image one scanline at a time */
		imageDataIndex = 0;
		pdataIndex = 0;
		pptrIndex = pdataIndex + (pad * paddedImageWidth) + pad;		

		for (int i = 0; i < imageHeight; i++)
		{
			System.arraycopy(imageData, imageDataIndex, paddedImagedata, pptrIndex, imageWidth);
			imageDataIndex += imageWidth;
			pptrIndex += paddedImageWidth;
		}

		ow.set(paddedImageWidth);
		oh.set(paddedImageHeight);
		ret.set(ILfs.FALSE);
		return paddedImagedata;
	}

	/*************************************************************************
	**************************************************************************
	#cat: fillHoles - Takes an input image and analyzes triplets of horizontal
	#cat:              pixels first and then triplets of vertical pixels, filling
	#cat:              in holes of width 1.  A hole is defined as the case where
	#cat:              the neighboring 2 pixels are equal, AND the center pixel
	#cat:              is different.  Each hole is filled with the value of its
	#cat:              immediate neighbors. This routine modifies the input image.
	   Input:
	      binarizedmageData - binary image data to be processed
	      imageWidth    - width (in pixels) of the binary input image
	      imageHeight    - height (in pixels) of the binary input image
	   Output:
	      binarizedmageData - points to the results
	**************************************************************************/
	public void fillHoles(int[] binarizedmageData, final int imageWidth, final int imageHeight) {
		int xIndex, yIndex, iw2;
		int leftPixelIndex, middlePixelIndex, rightPixelIndex, topPixelIndex, bottomPixelIndex, imagePixelIndex;

		/* 1. Fill 1-pixel wide holes in horizontal runs first ... */
		imagePixelIndex = (0 + 1);
		/* Foreach row in image ... */
		for (yIndex = 0; yIndex < imageHeight; yIndex++)
		{
			/* Initialize pointers to start of next line ... */
			leftPixelIndex = (imagePixelIndex - 1); // Left pixel
			middlePixelIndex = imagePixelIndex; // Middle pixel
			rightPixelIndex = (imagePixelIndex + 1); // Right pixel
			/* Foreach column in image (less far left and right pixels) ... */
			for (xIndex = 1; xIndex < imageWidth - 1; xIndex++)
			{
				/* Do we have a horizontal hole of length 1? */
				if ((binarizedmageData[leftPixelIndex] != binarizedmageData[middlePixelIndex]) && 
					(binarizedmageData[leftPixelIndex] == binarizedmageData[rightPixelIndex]))
				{
					/* If so, then fill it. */
					binarizedmageData[middlePixelIndex] = binarizedmageData[leftPixelIndex];
					/* Bump passed right pixel because we know it will not */
					/* be a hole.                                          */
					leftPixelIndex += 2;
					middlePixelIndex += 2;
					rightPixelIndex += 2;
					/* We bump ix once here and then the FOR bumps it again. */
					xIndex++;
				}
				else
				{
					/* Otherwise, bump to the next pixel to the right. */
					leftPixelIndex++;
					middlePixelIndex++;
					rightPixelIndex++;
				}
			}
			/* Bump to start of next row. */
			imagePixelIndex += imageWidth;
		}

		/* 2. Now, fill 1-pixel wide holes in vertical runs ... */
		iw2 = imageWidth << 1;
		/* Start processing column one row down from the top of the image. */
		imagePixelIndex = (0 + imageWidth);
		/* Foreach column in image ... */
		for (xIndex = 0; xIndex < imageWidth; xIndex++)
		{
			/* Initialize pointers to start of next column ... */
			topPixelIndex = (imagePixelIndex - imageWidth); // Top pixel
			middlePixelIndex = imagePixelIndex; // Middle pixel
			bottomPixelIndex = (imagePixelIndex + imageWidth); // Bottom pixel
			/* Foreach row in image (less top and bottom row) ... */
			for (yIndex = 1; yIndex < imageHeight - 1; yIndex++)
			{
				/* Do we have a vertical hole of length 1? */
				if ((binarizedmageData[topPixelIndex] != binarizedmageData[middlePixelIndex]) && (binarizedmageData[topPixelIndex] == binarizedmageData[bottomPixelIndex]))
				{
					/* If so, then fill it. */
					binarizedmageData[middlePixelIndex] = binarizedmageData[topPixelIndex];
					/* Bump passed bottom pixel because we know it will not */
					/* be a hole.                                           */
					topPixelIndex += iw2;
					middlePixelIndex += iw2;
					bottomPixelIndex += iw2;
					/* We bump iy once here and then the FOR bumps it again. */
					yIndex++;
				 }
				 else
				 {
					 /* Otherwise, bump to the next pixel below. */
					 topPixelIndex += imageWidth;
					 middlePixelIndex += imageWidth;
					 bottomPixelIndex += imageWidth;
				 }
			}
			/* Bump to start of next column. */
			imagePixelIndex++;
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: freePath - Traverses a straight line between 2 pixel points in an
	#cat:             image and determines if a "free path" exists between the
	#cat:             2 points by counting the number of pixel value transitions
	#cat:             between adjacent pixels along the trajectory.
	   Input:
	      x1       - x-pixel coord of first point
	      y1       - y-pixel coord of first point
	      x2       - x-pixel coord of second point
	      y2       - y-pixel coord of second point
	      binarizedmageData    - binary image data (0==while & 1==black)
	      imageWidth       - width (in pixels) of image
	      imageHeight       - height (in pixels) of image
	      lfsparms - parameters and threshold for controlling LFS
	   Return Code:
	      TRUE      - free path determined to exist
	      FALSE     - free path determined not to exist
	      Negative  - system error
	**************************************************************************/
	public int freePath(final int x1, final int y1, final int x2, final int y2, 
		int[] binarizedmageData, final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		int[] xList, yList;
		AtomicInteger num = new AtomicInteger(0); 
		int ret, i, trans, preval, nextval;
		int asize;
		/* Compute maximum number of points needed to hold line segment. */
		asize = Math.max(Math.abs(x2-x1)+2, Math.abs(y2-y1)+2);
		xList = new int[asize];
		yList = new int[asize];
		
		/* Compute points along line segment between the two points. */
		if ((ret = getLine().linePoints(xList, yList, num, x1, y1, x2, y2)) != ILfs.FALSE)
		{
			return (ret);
		}

		/* Intialize the number of transitions to 0. */
		trans = 0;
		/* Get the pixel value of first point along line segment. */
		preval = binarizedmageData[0 + (y1 * imageWidth) + x1];

		/* Foreach remaining point along line segment ... */
		for (i = 1; i < num.get(); i++)
		{
			/* Get pixel value of next point along line segment. */
			nextval = binarizedmageData[0 + (yList[i] * imageWidth) + xList[i]];

			/* If next pixel value different from previous pixel value ... */
			if (nextval != preval)
			{
				/* Then we have detected a transition, so bump counter. */
				trans++;
				/* If number of transitions seen > than threshold (ex. 2) ... */
				if (trans > lfsParams.getMaxtrans())
				{
					/* Deallocate the line segment's coordinate lists. */
					getFree().free(xList);
					getFree().free(yList);
					/* Return free path to be FALSE. */
					return (ILfs.FALSE);
				}
				/* Otherwise, maximum number of transitions not yet exceeded. */
				/* Assign the next pixel value to the previous pixel value.   */
				preval = nextval;
			}
			/* Otherwise, no transition detected this interation. */
		}

		/* If we get here we did not exceed the maximum allowable number        */
		/* of transitions.  So, deallocate the line segment's coordinate lists. */
		getFree().free(xList);
		getFree().free(yList);

		/* Return free path to be TRUE. */
		return (ILfs.TRUE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: searchInDirection - Takes a specified maximum number of steps in a
	#cat:                specified direction looking for the first occurence of
	#cat:                a pixel with specified value.  (Once found, adjustments
	#cat:                are potentially made to make sure the resulting pixel
	#cat:                and its associated edge pixel are 4-connected.)
	   Input:
	      pix       - value of pixel to be searched for
	      startX    - x-pixel coord to start search
	      startY    - y-pixel coord to start search
	      deltaX   - increment in x for each step
	      deltaY   - increment in y for each step
	      maxsteps  - maximum number of steps to conduct search
	      binarizedmageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	   Output:
	      ox        - x coord of located pixel
	      oy        - y coord of located pixel
	      oex       - x coord of associated edge pixel
	      oey       - y coord of associated edge pixel
	   Return Code:
	      TRUE      - pixel of specified value found
	      FALSE     - pixel of specified value NOT found
	**************************************************************************/
	public int searchInDirection(AtomicInteger ox, AtomicInteger oy, 
		AtomicInteger oex, AtomicInteger oey, final int pix, 
		final int startX, final int startY, final double deltaX, final double deltaY, 
		final int maxsteps, int[] binarizedmageData, final int imageWidth, final int imageHeight) {
		int i;
		AtomicInteger x = new AtomicInteger(0), y = new AtomicInteger (0), 
			px = new AtomicInteger(0), py = new AtomicInteger(0);
		double fx, fy;

		/* Set previous point to starting point. */
		px.set(startX);
		py.set(startY);
		/* Set floating point accumulators to starting point. */
		fx = (double)startX;
		fy = (double)startY;

		/* Foreach step up to the specified maximum ... */
		for (i = 0; i < maxsteps; i++)
		{
			/* Increment accumulators. */
			fx += deltaX;
			fy += deltaY;
			/* Round to get next step. */
			x.set(getDefs().sRound(fx));
			y.set(getDefs().sRound(fy));

			/* If we stepped outside the image boundaries ... */
			if ((x.get() < 0) || (x.get() >= imageWidth) || 
				(y.get() < 0) || (y.get() >= imageHeight))
			{
				/* Return FALSE (we did not find what we were looking for). */
				ox.set(-1);
				oy.set(-1);
				oex.set(-1);
				oey.set(-1);
				return (ILfs.FALSE);
			}

			/* Otherwise, test to see if we found our pixel with value 'pix'. */
			if (binarizedmageData[0 + (y.get() * imageWidth) + x.get()] == pix)
			{
				/* The previous and current pixels form a feature, edge pixel */
				/* pair, which we would like to use for edge following.  The  */
				/* previous pixel may be a diagonal neighbor however to the   */
				/* current pixel, in which case the pair could not be used by */
				/* the contour tracing (which requires the edge pixel in the  */
				/* pair neighbor to the N,S,E or W.                           */
				/* This routine adjusts the pair so that the results may be   */
				/* used by the contour tracing.                               */
				getContour().fixEdgePixelPair(x, y, px, py, binarizedmageData, imageWidth, imageHeight);

				/* Return TRUE (we found what we were looking for). */
				ox.set(x.get());
				oy.set(y.get());
				oex.set(px.get());
				oey.set(py.get());
				return (ILfs.TRUE);
			}

			/* Otherwise, still haven't found pixel with desired value, */
			/* so set current point to previous and take another step.  */
			px.set(x.get());
			py.set(y.get());
		}

		/* Return FALSE (we did not find what we were looking for). */
		ox.set(-1);
		oy.set(-1);
		oex.set(-1);
		oey.set(-1);
		return (ILfs.FALSE);
	}
}
