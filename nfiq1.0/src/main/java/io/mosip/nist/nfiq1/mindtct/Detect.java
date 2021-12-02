package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.DftWaves;
import io.mosip.nist.nfiq1.common.ILfs.DirToRad;
import io.mosip.nist.nfiq1.common.ILfs.IDetect;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;
import io.mosip.nist.nfiq1.common.ILfs.Minutiae;
import io.mosip.nist.nfiq1.common.ILfs.RotGrids;

public class Detect extends MindTct implements IDetect {
	private static final Logger LOGGER = LoggerFactory.getLogger(Detect.class);	

	private static volatile Detect instance;
    public static Detect getInstance() {
        if (instance == null) {
            synchronized (Detect.class) {
                if (instance == null) {
                    instance = new Detect();
                }
            }
        }
        return instance;
    }    
    private Detect()
    {
    	super();
    }
    
	public Init getInit() {
		return Init.getInstance();
	}
	public Globals getGlobals() {
		return Globals.getInstance();
	}
	public Free getFree() {
		return Free.getInstance();
	}
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}
	public Binarization getBinarization() {
		return Binarization.getInstance();
	}
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}
	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}
	public Ridges getRidges() {
		return Ridges.getInstance();
	}
	
	/*************************************************************************
	#cat: lfsDetectMinutiaeV2 - Takes a grayscale fingerprint image (of
	#cat:          arbitrary size), and returns a set of image block maps,
	#cat:          a binarized image designating ridges from valleys,
	#cat:          and a list of minutiae (including position, reliability,
	#cat:          type, direction, neighbors, and ridge counts to neighbors).
	#cat:          The image maps include a ridge flow directional map,
	#cat:          a map of low contrast blocks, a map of low ridge flow blocks.
	#cat:          and a map of high-curvature blocks.
	   Input:
	      imageData     - input 8-bit grayscale fingerprint image data
	      imageWidth        - width (in pixels) of the image
	      imageHeight        - height (in pixels) of the image
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      ret		- Zero      - successful completion
	      			- Negative  - system error
	      oMinutiae - resulting list of minutiae
	      map 		-- contains above details	
	      oBinarizedImageWidth       - width (in pixels) of the binary image
	      oBinarizedImageHeight       - height (in pixels) of the binary image
	   Return Code:
	      binarizedImageData    - resulting binarized image
	                  {0 = black pixel (ridge) and 255 = white pixel (valley)}
	**************************************************************************/
	public int [] lfsDetectMinutiaeV2(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps map, 
		AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, int [] imageData, 
		final int imageWidth, final int imageHeight, final LfsParams lfsParams) {

		int [] paddedImagedata = null;
		int [] binarizedImageData = null;
		AtomicInteger paddedImageWidth = new AtomicInteger(0), paddedImageHeight = new AtomicInteger(0);
		AtomicInteger binarizedImageWidth = new AtomicInteger(0), binarizedImageHeight = new AtomicInteger(0);
		DirToRad dirToRad = null;
		DftWaves dftWaves = null;
		RotGrids dftGrids = null;
		RotGrids dirBinGrids = null;
	   	//int mw, mh;
	   	int maxPad;
	   	AtomicReference<Minutiae> minutiae = null;
	   	AtomicReferenceArray<Double> dftCoefs = null;
	    long totalStartTime = System.currentTimeMillis();
	   	
	   	/******************/
	    /* INITIALIZATION */
	    /******************/
	   	
	   	/* Determine the maximum amount of image padding required to support */
	    /* LFS processes.                                                    */
	   	maxPad = getInit().getMaxPaddingV2(lfsParams.getWindowSize(), lfsParams.getWindowOffset(), 
	   			lfsParams.getDirbinGridWidth(), lfsParams.getDirbinGridHeight());

	   	/* Initialize lookup table for converting integer directions */
	   	/* to angles in radians.                                     */
	   	dirToRad = new DirToRad (lfsParams.getNumDirections());
	   	ret.set(getInit().initDirToRad(dirToRad));
	   	if (ret.get() != ILfs.FALSE)
	    {
	   		/* Free memory allocated to this point. */
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	   	/* Initialize wave form lookup tables for DFT analyses. */
	    /* used for direction binarization.                             */
	   	dftCoefs = new AtomicReferenceArray<Double>(getGlobals().getDftCoefs().length);
		for (int index = 0; index < dftCoefs.length(); index++)
			dftCoefs.set(index, getGlobals().getDftCoefs()[index]);
		
	   	dftWaves = new DftWaves (lfsParams.getNumDftWaves(), lfsParams.getWindowSize());
		ret.set(getInit().initDftWaves(dftWaves, dftCoefs));				
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	getFree().freeDirToRad(dirToRad);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

		/* Initialize lookup table for pixel offsets to rotated grids */
	    /* used for DFT analyses.                                     */
	    dftGrids = new RotGrids (lfsParams.getStartDirAngle(), lfsParams.getNumDirections(), lfsParams.getWindowSize(), lfsParams.getWindowSize(), ILfs.RELATIVE_TO_ORIGIN); 
		ret.set(getInit().initRotGrids(dftGrids, imageWidth, imageHeight, maxPad));
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	getFree().freeDirToRad(dirToRad);
	    	getFree().freeDftWaves(dftWaves);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }
	    
	    /* Pad input image based on max padding. */
	    if (maxPad > ILfs.FALSE)//0
	    { 
	    	// May not need to pad at all
	    	paddedImagedata = getImageUtil().padImage(ret, paddedImageWidth, paddedImageHeight, 
    			imageData, imageWidth, imageHeight, maxPad, lfsParams.getPadValue());
	    	if (ret.get() != ILfs.FALSE)
	    	{
	    		/* Free memory allocated to this point. */
	    		getFree().freeDirToRad(dirToRad);
	    		getFree().freeDftWaves(dftWaves);
	    		getFree().freeRotGrids(dftGrids);
		   		binarizedImageData = null;
		   		return binarizedImageData;
	    	}
    	}
	    else
	    {
	    	/* If padding is unnecessary, then copy the input image. */
	    	paddedImagedata = new int [imageWidth * imageHeight];
	    	
	    	for (int index = 0; index < imageData.length; index++)
	    	{
	    		paddedImagedata[index] = imageData[index];
	    	}
	
	    	paddedImageWidth.set(imageWidth);
	    	paddedImageHeight.set(imageHeight);
	    }

	    /* Scale input image to 6 bits [0..63] */
	    /* !!! Would like to remove this dependency eventualy !!!     */
	    /* But, the DFT computations will need to be changed, and     */
	    /* could not get this work upon first attempt. Also, if not   */
	    /* careful, I think accumulated power magnitudes may overflow */
	    /* doubles.                                                   */
	    getImageUtil().bits8To6(paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get());

	    long mapStartTime = System.currentTimeMillis();
	
	    if (isShowLogs())
	    	LOGGER.info("\nINITIALIZATION AND PADDING DONE\n");

	    /******************/
	    /*      MAPS      */
	    /******************/
	    /* Generate block maps from the input image. */
	    ret.set(map.genImageMaps(paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get(), 
    		dirToRad, dftWaves, dftGrids, lfsParams));
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	getFree().freeDirToRad(dirToRad);
	    	getFree().freeDftWaves(dftWaves);
	    	getFree().freeRotGrids(dftGrids);
	    	paddedImagedata = null;
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }
	    /* Deallocate working memories. */
	    getFree().freeDirToRad(dirToRad);
	    getFree().freeDftWaves(dftWaves);
	    getFree().freeRotGrids(dftGrids);

	    if (isShowLogs())	    
	    	LOGGER.info("\nMAPS DONE\n");
	    long mapEndTime = System.currentTimeMillis();
	    
	    /******************/
	    /* BINARIZARION   */
	    /******************/
	    if (isShowLogs())
	    	LOGGER.info("BINARIZATION STARTED");
	    long binStartTime = System.currentTimeMillis();
	    
	    /* Initialize lookup table for pixel offsets to rotated grids */
	    /* used for directional binarization.                         */
	    dirBinGrids = new RotGrids (lfsParams.getStartDirAngle(), lfsParams.getNumDirections(), 
	    		lfsParams.getDirbinGridWidth(), lfsParams.getDirbinGridHeight(), ILfs.RELATIVE_TO_CENTER); 
	    ret.set(getInit().initRotGrids(dirBinGrids, imageWidth, imageHeight, maxPad));
		if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
			paddedImagedata = null;
	    	getFree().freeDirToRad(dirToRad);
	    	getFree().freeDftWaves(dftWaves);
		 	map.setDirectionMap(null);
		 	map.setLowContrastMap(null);
		 	map.setLowFlowMap(null);
		 	map.setHighCurveMap(null);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	    /* Binarize input image based on NMAP information. */
		binarizedImageData = 
			getBinarization().binarizeV2(ret, binarizedImageWidth, binarizedImageHeight, 
			paddedImagedata, paddedImageWidth.get(), paddedImageHeight.get(), 
			map.getDirectionMap(), map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), 
			dirBinGrids, lfsParams);
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	paddedImagedata = null;
	    	map.setDirectionMap(null);
	    	map.setLowContrastMap(null);
	    	map.setLowFlowMap(null);
	    	map.setHighCurveMap(null);
	    	getFree().freeRotGrids(dirBinGrids);
	   		binarizedImageData = null;
	   		return binarizedImageData;
   		}
	
	    /* Deallocate working memory. */
	    getFree().freeRotGrids(dirBinGrids);

	    /* Check dimension of binary image.  If they are different from */
	    /* the input image, then ERROR.                                 */
	    if ((imageWidth != binarizedImageWidth.get()) || 
    		(imageHeight != binarizedImageHeight.get()))
	    {
	    	/* Free memory allocated to this point. */
	    	paddedImagedata = null;
	    	map.setDirectionMap(null);
	    	map.setLowContrastMap(null);
	    	map.setLowFlowMap(null);
	    	map.setHighCurveMap(null);
	    	binarizedImageData = null;
	    	LOGGER.error("ERROR : lfsDetectMinutiaeV2 :");
	    	LOGGER.error(String.format("binary image has bad dimensions : binarizedImageWidth = %d, binarizedImageHeight = %d\n" +  binarizedImageWidth, binarizedImageHeight));
	    	ret.set(ILfs.ERROR_CODE_581);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	    if (isShowLogs())
	    	LOGGER.info("BINARIZATION DONE\n");
	    long binEndTime = System.currentTimeMillis();

	    /******************/
	    /*   DETECTION    */
	    /******************/
	    if (isShowLogs())
	    	LOGGER.info("MINUTIA DETECTION STARTED\n");
	    long minStartTime = System.currentTimeMillis();
	    
	    /* Convert 8-bit grayscale binary image [0,255] to */
	    /* 8-bit binary image [0,1].                       */
	    getImageUtil().grayToBinary(1, 1, 0, binarizedImageData, imageWidth, imageHeight);

	    /* Allocate initial list of minutia pointers. */
	    minutiae = new AtomicReference<Minutiae>();	  
	    minutiae.set(new Minutiae());
	    ret.set(getMinutiaHelper().allocMinutiae(minutiae, ILfs.MAX_MINUTIAE));
	    if (ret.get() != ILfs.FALSE)
	    {
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	    /* Detect the minutiae in the binarized image. */
	    ret.set(getMinutiaHelper().detectMinutiaeV2(minutiae, binarizedImageData, imageWidth, imageHeight, 
	    		map, lfsParams));
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	paddedImagedata = null;
	    	map.setDirectionMap(null);
	    	map.setLowContrastMap(null);
	    	map.setLowFlowMap(null);
	    	map.setHighCurveMap(null);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }
	    
	    long minEndTime = System.currentTimeMillis();

	    /******************/
	    /*   REMOVE FALSE MINUTIA */
	    /******************/
	    long rmStartTime = System.currentTimeMillis();
	    ret.set(getRemoveMinutia().removeFalseMinutiaV2(minutiae, binarizedImageData, imageWidth, 
	    		imageHeight, map, map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), lfsParams));
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	paddedImagedata = null;
	    	map.setDirectionMap(null);
	    	map.setLowContrastMap(null);
	    	map.setLowFlowMap(null);
	    	map.setHighCurveMap(null);
	    	getMinutiaHelper().freeMinutiae(minutiae);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	    if (isShowLogs())
	    	LOGGER.info("\nMINUTIA DETECTION DONE\n");
	    long rmEndTime = System.currentTimeMillis();

	    /******************/
	    /*  RIDGE COUNTS  */
	    /******************/
	    long ridgeStartTime = System.currentTimeMillis();
	    ret.set(getRidges().countMinutiaeRidges(minutiae, 
    		binarizedImageData, imageWidth, imageHeight, lfsParams));
	    if (ret.get() != ILfs.FALSE)
	    {
	    	/* Free memory allocated to this point. */
	    	paddedImagedata = null;
	    	map.setDirectionMap(null);
	    	map.setLowContrastMap(null);
	    	map.setLowFlowMap(null);
	    	map.setHighCurveMap(null);
	    	getMinutiaHelper().freeMinutiae(minutiae);
	   		binarizedImageData = null;
	   		return binarizedImageData;
	    }

	    //LOGGER.info(minutiae.toString());

	    if (isShowLogs())
	    	LOGGER.info("\nNEIGHBOR RIDGE COUNT DONE\n");
	    long ridgeEndTime = System.currentTimeMillis();

	    /******************/
	    /*    WRAP-UP     */
	    /******************/

	    /* Convert 8-bit binary image [0,1] to 8-bit */
	    /* grayscale binary image [0,255].           */
	    getImageUtil().grayToBinary(1, ILfs.WHITE_PIXEL, ILfs.BLACK_PIXEL, 
    		binarizedImageData, imageWidth, imageHeight);

	    oBinarizedImageWidth.set(binarizedImageWidth.get());
	    oBinarizedImageHeight.set(binarizedImageHeight.get());
	    oMinutiae.set(minutiae.get());
	    long totalEndTime = System.currentTimeMillis();

	    /******************/
	    /* PRINT TIMINGS  */
	    /******************/
	    /* These Timings will print when TIMER is defined. */
	    /* print MAP generation timing statistics */
	    System.out.println(String.format("TIMER: MAPS time   = %f (secs)\n", (float)(mapEndTime - mapStartTime)));
	    /* print binarization timing statistics */
	    System.out.println(String.format("TIMER: Binarization time   = %f (secs)\n", (float)(binEndTime - binStartTime)));
	    /* print minutia detection timing statistics */
	    System.out.println(String.format("TIMER: Minutia Detection time   = %f (secs)\n", (float)(minEndTime - minStartTime)));
	    /* print minutia removal timing statistics */
	    System.out.println(String.format("TIMER: Minutia Removal time   = %f (secs)\n", (float)(rmEndTime - rmStartTime)));
	    /* print neighbor ridge count timing statistics */
	    System.out.println(String.format("TIMER: Neighbor Ridge Counting time   = %f (secs)\n", (float)(ridgeEndTime - ridgeStartTime)));
	    /* print total timing statistics */
	    System.out.println(String.format("TIMER: Total time   = %f (secs)\n", (float)(totalEndTime - totalStartTime)));
	    ret.set(ILfs.FALSE);
	    
	    return binarizedImageData;
	}
	
}


