package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.Defs;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IBinarization;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;
import io.mosip.nist.nfiq1.common.ILfs.RotGrids;

public class Binarization extends MindTct implements IBinarization {
	private static final Logger LOGGER = LoggerFactory.getLogger(Binarization.class);	

	private static volatile Binarization instance;
    public static Binarization getInstance() {
        if (instance == null) {
            synchronized (Binarization.class) {
                if (instance == null) {
                    instance = new Binarization();
                }
            }
        }
        return instance;
    }    
    private Binarization()
    {
    	super();
    }
    
	public Defs getDefs() {
		return Defs.getInstance();
	}	
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/*************************************************************************
	**************************************************************************
	#cat: binarize - Takes a padded grayscale input image and its associated ridge
	#cat:              direction flow NMAP and produces a binarized version of the
	#cat:              image.  It then fills horizontal and vertical "holes" in the
	#cat:              binary image results.
	   Input:
	      paddedImageData   - padded input grayscale image
	      paddedImageWidth  - padded width (in pixels) of input image
	      paddedImageHeight - padded height (in pixels) of input image
	      mapDirectionArr     			- 2-D vector of IMAP directions and other codes
	      mappedImageWidth	- width (in blocks) of the NMAP
	      mappedImageHeight	- height (in blocks) of the NMAP
	      dirBinGrids 		- set of rotated grid offsets used for directional
	              			binarization
	      lfsParms 			- parameters and thresholds for controlling LFS
	   Output:
	      ret				- Zero     - successful completion
      						-Negative - system error
	      oBinarizedWidth   - width of binary image
	      oBinarizedHeight  - height of binary image
	   Return Code:
	      binarizedImageData  			- points to created (unpadded) binary image
	**************************************************************************/
	public int[] binarize(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight, 
		int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, 
		AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight, 
		final RotGrids dirBinGrids, final LfsParams lfsParms) {
		int[] binarizedImageData;
		int i;
		AtomicInteger binarizedWidth = new AtomicInteger (0), binarizedHeight = new AtomicInteger(0);
		// return code

		/* 1. Binarize the padded input image using NMAP information. */
		binarizedImageData = binarizeImage(ret, binarizedWidth, binarizedHeight, paddedImageData, paddedImageWidth, paddedImageHeight, mapDirectionArr, mappedImageWidth, mappedImageHeight, lfsParms.getBlockSize(), dirBinGrids, lfsParms.getIsoBinGridDim());
		if (ret.get() != ILfs.FALSE)
		{
			return null;
		}

		/* 2. Fill black and white holes in binary image. */
		/* LFS scans the binary image, filling holes, 3 times. */
		for (i = 0; i < lfsParms.getNumFillHoles(); i++)
		{
			getImageUtil().fillHoles(binarizedImageData, binarizedWidth.get(), binarizedHeight.get());
		}

		/* Return binarized input image. */
		oBinarizedWidth.set(binarizedWidth.get());
		oBinarizedHeight.set(binarizedHeight.get());
		return binarizedImageData;
	}

	/*************************************************************************
	**************************************************************************
	#cat: binarizeV2 - Takes a padded grayscale input image and its associated
	#cat:              Direction Map and produces a binarized version of the
	#cat:              image.  It then fills horizontal and vertical "holes" in
	#cat:              the binary image results.  Note that the input image must
	#cat:              be padded sufficiently to contain in memory rotated
	#cat:              directional binarization grids applied to pixels along the
	#cat:              perimeter of the input image.
	   Input:
	      paddedImageData       - padded input grayscale image
	      paddedImageWidth		- padded width (in pixels) of input image
	      paddedImageHeight     - padded height (in pixels) of input image
	      directionMap 		- 2-D vector of discrete ridge flow directions
	      mappedImageWidth      - width (in blocks) of the map
	      mappedImageHeight     - height (in blocks) of the map
	      dirBinGrids 			- set of rotated grid offsets used for directional
	                    		binarization
	      lfsParms    			- parameters and thresholds for controlling LFS
	   Output:
	      ret					- Zero     - successful completion
      							-Negative - system error
	      oBinarizedWidth    	- width of binary image
	      oBinarizedHeight    	- height of binary image
	   Return Code:
	      binarizedImageData 	- points to created (unpadded) binary image
	**************************************************************************/
	public int[] binarizeV2(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight, int[] paddedImageData, 
		final int paddedImageWidth, final int paddedImageHeight, AtomicIntegerArray directionMap, final int mappedImageWidth,
		final int mappedImageHeight, final RotGrids dirBinGrids, final LfsParams lfsParms) {
		int[] binarizeImagedata;
		AtomicInteger binarizedWidth = new AtomicInteger (0), binarizedHeight = new AtomicInteger(0);
		// return code
		/* 1. Binarize the padded input image using NMAP information. */
		binarizeImagedata = binarizeImageV2(ret, binarizedWidth, binarizedHeight, paddedImageData, paddedImageWidth, paddedImageHeight, directionMap, mappedImageWidth, mappedImageHeight, 
				lfsParms.getBlockSize(), dirBinGrids);
		if (ret.get() != ILfs.FALSE)
		{
			return null;
		}

		/* 2. Fill black and white holes in binary image. */
		/* LFS scans the binary image, filling holes, 3 times. */
		for (int i = 0; i < lfsParms.getNumFillHoles(); i++)
		{
			getImageUtil().fillHoles(binarizeImagedata, binarizedWidth.get(), binarizedHeight.get());
		}

		/* Return binarized input image. */
		oBinarizedWidth.set(binarizedWidth.get());
		oBinarizedHeight.set(binarizedHeight.get());

		return binarizeImagedata;
	}

	/*************************************************************************
	**************************************************************************
	#cat: binarize_image - Takes a grayscale input image and its associated
	#cat:                  NMAP and generates a binarized version of the image.
	   Input:
	      paddedImageData 		- padded input grayscale image
	      paddedImageWidth    	- padded width (in pixels) of input image
	      paddedImageHeight    	- padded height (in pixels) of input image
	      mapDirectionArr  		- 2-D vector of IMAP directions and other codes
	      mappedImageWidth    	- width (in blocks) of the NMAP
	      mappedImageHeight    	- height (in blocks) of the NMAP
	      imapBlockSize 		- dimension (in pixels) of each NMAP block
	      dirBinGrids 			- set of rotated grid offsets used for directional
	              				binarization
	      isoBinGridDim 		- dimension (in pixels) of grid used for isotropic
	              				binarization
	   Output:
	      ret					- Zero     - successful completion
	      						- Negative - system error
	      oBinarizedWidth		- points to binary image width
	      oBinarizedHeight    	- points to binary image height
	   Return Code:
	      binarizedImageData		- points to binary image results
	**************************************************************************/	
	@SuppressWarnings("unused")
	public int[] binarizeImage(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight, 
		int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, AtomicIntegerArray mapDirectionArr,
		final int mappedImageWidth, final int mappedImageHeight, final int imapBlockSize, RotGrids dirBinGrids, final int isoBinGridDim) {
		int binarizedWidth, binarizedHeight, binarizedXPixel, binarizedYPixel, nMapValue;
		int [] binarizedImageData;
		int binarizedImageIndex, paddedImageIndex, currentPaddedImageIndex;

		/* Compute dimensions of "unpadded" binary image results. */
		binarizedWidth = paddedImageWidth - (dirBinGrids.getPad() << 1);
		binarizedHeight = paddedImageHeight - (dirBinGrids.getPad() << 1);

		binarizedImageData = new int [binarizedWidth * binarizedHeight];
		if (binarizedImageData == null)
		{
			LOGGER.error(String.format("ERROR : binarizeImage : binarizedImageData : null\n"));
			ret.set(ILfs.ERROR_CODE_110);
			return null;
  		}

		binarizedImageIndex = 0;
		currentPaddedImageIndex = 0 + (dirBinGrids.getPad() * paddedImageWidth) + dirBinGrids.getPad();
		for (int iy = 0; iy < binarizedHeight; iy++)
		{
			/* Set pixel pointer to start of next row in grid. */
			paddedImageIndex = currentPaddedImageIndex;
			for (int ix = 0; ix < binarizedWidth; ix++)
			{
				/* Compute which block the current pixel is in. */
				binarizedXPixel = (int)(ix / imapBlockSize);
				binarizedYPixel = (int)(iy / imapBlockSize);
				/* Get corresponding value in NMAP */
				nMapValue = mapDirectionArr.get((binarizedYPixel * mappedImageWidth) + binarizedXPixel);
				/* If current block has no neighboring blocks with */
				/* VALID directions ...                            */
				if (nMapValue == ILfs.NO_VALID_NBRS)
				{
					/* Set binary pixel to white (255). */
					binarizedImageData[binarizedImageIndex] = ILfs.WHITE_PIXEL;
				}
				/* Otherwise, if block's NMAP has a valid direction ... */
				else if (nMapValue >= 0)
				{
					/* Use directional binarization based on NMAP direction. */
					binarizedImageData[binarizedImageIndex] = dirbinarize(paddedImageData, paddedImageIndex, nMapValue, dirBinGrids);
				}
				else
				{
					/* Otherwise, the block's NMAP is either INVALID or */
					/* HIGH-CURVATURE, so use isotropic binarization.    */
					binarizedImageData[binarizedImageIndex] = isoBinarize(paddedImageData, paddedImageIndex, paddedImageWidth, paddedImageHeight, isoBinGridDim);
				}
				/* Bump input and output pixel pointers. */
				paddedImageIndex++;
				binarizedImageIndex++;
			}
			/* Bump pointer to the next row in padded input image. */
			currentPaddedImageIndex += paddedImageWidth;
		}

		oBinarizedWidth.set(binarizedWidth);
		oBinarizedHeight.set(binarizedHeight);
		ret.set(ILfs.FALSE);
		return binarizedImageData;
	}

	/*************************************************************************
	**************************************************************************
	#cat: binarizeImageV2 - Takes a grayscale input image and its associated
	#cat:              Direction Map and generates a binarized version of the
	#cat:              image.  Note that there is no "Isotropic" binarization
	#cat:              used in this version.
	   Input:
	      paddedImageData       	- padded input grayscale image
	      paddedImageWidth          - padded width (in pixels) of input image
	      paddedImageHeight         - padded height (in pixels) of input image
	      directionMap 				- 2-D vector of discrete ridge flow directions
	      mappedImageWidth          - width (in blocks) of the map
	      mappedImageHeight         - height (in blocks) of the map
	      blocksize   				- dimension (in pixels) of each NMAP block
	      dirBinGrids 				- set of rotated grid offsets used for directional
	                    			binarization
	   Output:
	      ret						- Zero     - successful completion
	      							- Negative - system error
	      oBinarizedWidth     		- points to binary image width
	      oBinarizedHeight     		- points to binary image height
	   Return Code:
	      binarizedImageData  	- points to binary image results
	**************************************************************************/
	public int[] binarizeImageV2(AtomicInteger ret, AtomicInteger oBinarizedWidth, AtomicInteger oBinarizedHeight, 
		int[] paddedImageData,  final int paddedImageWidth, final int paddedImageHeight, 
		AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight, 
		final int blocksize, final RotGrids dirBinGrids) {
		int binarizedWidth, binarizedHeight, binarizedXPixel, binarizedYPixel;
		int mapValue;
		int[] binarizedImageData;
		int binarizedImageIndex, paddedImageIndex, currentPaddedImageIndex;

		/* Compute dimensions of "unpadded" binary image results. */
		binarizedWidth = paddedImageWidth - (dirBinGrids.getPad() << 1);
		binarizedHeight = paddedImageHeight - (dirBinGrids.getPad() << 1);

		binarizedImageData = new int [binarizedWidth * binarizedHeight];
		binarizedImageIndex = 0;
		currentPaddedImageIndex = 0 + (dirBinGrids.getPad() * paddedImageWidth) + dirBinGrids.getPad();
		for (int iy = 0; iy < binarizedHeight; iy++)
		{
			/* Set pixel pointer to start of next row in grid. */
			paddedImageIndex = currentPaddedImageIndex;
			for (int ix = 0; ix < binarizedWidth; ix++)
			{
				/* Compute which block the current pixel is in. */
				binarizedXPixel = (int)(ix / blocksize);
				binarizedYPixel = (int)(iy / blocksize);
				/* Get corresponding value in Direction Map. */
				mapValue = directionMap.get((binarizedYPixel * mappedImageWidth) + binarizedXPixel);

				/* If current block has has INVALID direction ... */
				if (mapValue == ILfs.INVALID_DIR)
				{
					/* Set binary pixel to white (255). */
					binarizedImageData[binarizedImageIndex] = ILfs.WHITE_PIXEL;
				 }
				 /* Otherwise, if block has a valid direction ... */
				 else //if(mapValue >= 0)
				 {
					 /* Use directional binarization based on block's direction. */
					 binarizedImageData[binarizedImageIndex] = dirbinarize(paddedImageData, paddedImageIndex, mapValue, dirBinGrids);
				 }

				 /* Bump input and output pixel pointers. */
				 paddedImageIndex++;
				 binarizedImageIndex++;
			  }
			  /* Bump pointer to the next row in padded input image. */
			  currentPaddedImageIndex += paddedImageWidth;
		   }

		   oBinarizedWidth.set(binarizedWidth);
		   oBinarizedHeight.set(binarizedHeight);
		   ret.set(ILfs.FALSE);
		   
		   return binarizedImageData;
	}

	/*************************************************************************
	**************************************************************************
	#cat: dirbinarize - Determines the binary value of a grayscale pixel based
	#cat:               on a VALID IMAP ridge flow direction.
	   CAUTION: The image to which the input pixel points must be appropriately
	            padded to account for the radius of the rotated grid.  Otherwise,
	            this routine may access "unkown" memory.
	   Input:
	      paddedImageData       - pointer to current grayscale pixel
      	  paddedImageIndex        	- pointer to current grayscale pixel
	      imapDirection        			- IMAP integer direction associated with the block the
	                    current is in
	      dirBinGrids - set of precomputed rotated grid offsets
	   Return Code:
	      BLACK_PIXEL - pixel intensity for BLACK
	      WHITE_PIXEL - pixel intensity of WHITE
	**************************************************************************/
	public int dirbinarize(int[] paddedImageData, final int paddedImageIndex, final int imapDirection, final RotGrids dirBinGrids) {
		int gx, gy, gi, cy;
		int rsum, gsum, csum = 0;
		int []grid ;
		double dcy;

		/* Assign nickname pointer. */
		grid = dirBinGrids.getGrids()[imapDirection];
		/* Calculate center (0-oriented) row in grid. */
		dcy = (dirBinGrids.getGridHeight() - 1) / (double)2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		dcy = getDefs().truncDoublePrecision(dcy, ILfs.TRUNC_SCALE);
		cy = getDefs().sRound(dcy);
		/* Initialize grid's pixel offset index to zero. */
		gi = 0;
		/* Initialize grid's pixel accumulator to zero */
		gsum = 0;

		/* Foreach row in grid ... */
		for (gy = 0; gy < dirBinGrids.getGridHeight(); gy++)
		{
			/* Initialize row pixel sum to zero. */
			rsum = 0;
			/* Foreach column in grid ... */
			for (gx = 0; gx < dirBinGrids.getGridWidth(); gx++)
			{
				/* Accumulate next pixel along rotated row in grid. */
				rsum += paddedImageData [paddedImageIndex + grid[gi]];
				/* Bump grid's pixel offset index. */
				gi++;
			}
			/* Accumulate row sum into grid pixel sum. */
			gsum += rsum;
			/* If current row is center row, then save row sum separately. */
			if (gy == cy)
			{
				csum = rsum;
			}
		}

		/* If the center row sum treated as an average is less than the */
		/* total pixel sum in the rotated grid ...                      */
		if ((csum * dirBinGrids.getGridHeight()) < gsum)
		{
			/* Set the binary pixel to BLACK. */
			return (ILfs.BLACK_PIXEL);
		}
		else
		{
			/* Otherwise set the binary pixel to WHITE. */
			return (ILfs.WHITE_PIXEL);
		}
	}

	/*************************************************************************
	**************************************************************************
	#cat: isoBinarize - Determines the binary value of a grayscale pixel based
	#cat:               on comparing the grayscale value with a surrounding
	#cat:               neighborhood grid of pixels.  If the current pixel (treated
	#cat:               as an average) is less than the sum of the pixels in
	#cat:               the neighborhood, then the binary value is set to BLACK,
	#cat:               otherwise it is set to WHITE.  This binarization technique
	#cat:               is used when there is no VALID IMAP direction for the
	#cat:               block in which the current pixel resides.
	   CAUTION: The image to which the input pixel points must be appropriately
	            padded to account for the radius of the neighborhood.  Otherwise,
	            this routine may access "unkown" memory.
	   Input:
	      paddedImageData 		- pointer to curent grayscale pixel
		  paddedImageIndex      - pointer to current grayscale pixel
	      paddedImageWidth   	- padded width (in pixels) of the grayscale image
	      paddedImageHeight   	- padded height (in pixels) of the grayscale image
	      isoBinGridDim 		- dimension (in pixels) of the neighborhood
	   Return Code:
	      BLACK_PIXEL 		- pixel intensity for BLACK
	      WHITE_PIXEL 		- pixel intensity of WHITE
	**************************************************************************/
	public int isoBinarize(int[] paddedImageData, final int paddedImageIndex, final int paddedImageWidth, 
			final int paddedImageHeight, final int isoBinGridDim) {
		int currentPaddedImageIndex, currentImageIndex;
		int radius, bsum;
		double drad;

		/* Initialize grid pixel sum to zero. */
		bsum = 0;
		/* Compute radius from current pixel based on isoBinGridDim. */
		drad = (isoBinGridDim - 1) / (double)2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		drad = getDefs().truncDoublePrecision(drad, ILfs.TRUNC_SCALE);
		radius = getDefs().sRound(drad);
		/* Set pointer to origin of grid centered on the current pixel. */
		currentPaddedImageIndex = (paddedImageIndex - (radius * paddedImageWidth) - radius);

		/* For each row in the grid ... */
		for (int py = 0; py < isoBinGridDim; py++)
		{
			/* Set pixel pointer to start of next row in grid. */
			currentImageIndex = currentPaddedImageIndex;
			/* For each column in the grid ... */
			for (int px = 0; px < isoBinGridDim; px++)
			{
				/* Accumulate next pixel in the grid. */
				bsum += paddedImageData[currentImageIndex];
				/* Bump pixel pointer. */
				currentImageIndex++;
			}
			/* Bump to the start of the next row in the grid. */
			currentPaddedImageIndex += paddedImageWidth;
		}

		/* If current (center) pixel when treated as an average for the   */
		/* entire grid is less than the total pixel sum of the grid ... */
		if ((paddedImageData[paddedImageIndex] * isoBinGridDim * isoBinGridDim) < bsum)
		{
			/* Set the binary pixel to BLACK. */
			return (ILfs.BLACK_PIXEL);
		}
		else
		{
			/* Otherwise, set the binary pixel to WHITE. */
			return (ILfs.WHITE_PIXEL);
		}
	}
}


