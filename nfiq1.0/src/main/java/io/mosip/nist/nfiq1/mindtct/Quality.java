package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.Defs;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IQuality;
import io.mosip.nist.nfiq1.common.ILfs.Minutia;
import io.mosip.nist.nfiq1.common.ILfs.Minutiae;

public class Quality extends MindTct implements IQuality {
	private static final Logger LOGGER = LoggerFactory.getLogger(Quality.class);	
	private static volatile Quality instance;
    public static Quality getInstance() {
        if (instance == null) {
            synchronized (Quality.class) {
                if (instance == null) {
                    instance = new Quality();
                }
            }
        }
        return instance;
    }    
    public static Quality getInstance(int mappedImageWidth, int mappedImageHeight) {
        if (instance == null) {
            synchronized (Quality.class) {
                if (instance == null) {
                    instance = new Quality(mappedImageWidth, mappedImageHeight);
                }
            }
        }
        return instance;
    }    

    private Quality()
    {
    	super();
    }
    
	public Defs getDefs() {
		return Defs.getInstance();
	}	
	public Free getFree() {
		return Free.getInstance();
	}

	private AtomicIntegerArray qualityMap;
	//mappedImageWidth        - number of blocks horizontally in the padded input image
	//mh        - number of blocks vertically in the padded input image
	private int mappedImageWidth, mappedImageHeight;

	private Quality(int mappedImageWidth, int mappedImageHeight) {
		super();
		/* Compute total number of blocks in map */
		this.mappedImageWidth = mappedImageWidth;
		this.mappedImageHeight = mappedImageHeight;
		this.qualityMap = new AtomicIntegerArray(this.mappedImageWidth * this.mappedImageHeight);
	}
	
	/***********************************************************************
	************************************************************************
	#cat: generateQualityMap - Takes a direction map, low contrast map, low ridge
	#cat:              flow map, and high curvature map, and combines them
	#cat:              into a single map containing 5 levels of decreasing
	#cat:              quality.  This is done through a set of heuristics.
	   Set quality of 0(unusable)..4(good) (I call these grades A..F)
	      0/F: low contrast OR no direction
	      1/D: low flow OR high curve
	           (with low contrast OR no direction neighbor)
	           (or within NEIGHBOR_DELTA of edge)
	      2/C: low flow OR high curve
	           (or good quality with low contrast/no direction neighbor)
	      3/B: good quality with low flow / high curve neighbor
	      4/A: good quality (none of the above)
	   Generally, the features in A/B quality are useful, the C/D quality
	   ones are not.
	   Input:
	   	 map				 - contain all below 	
	      //direction_map    - map with blocks assigned dominant ridge flow direction
	      //low_contrast_map - map with blocks flagged as low contrast
	      //low_flow_map     - map with blocks flagged as low ridge flow
	      //high_curve_map   - map with blocks flagged as high curvature
	      //map_w            - width (in blocks) of the maps
	      //map_h            - height (in blocks) of the maps
	   Output:
	      //oqmap      - points to new quality map
	      // get it seperatly after call made 
	   Return Code:
	      Zero       - successful completion
	      Negative   - system error
	************************************************************************/
	public int generateQualityMap(Maps map) {
		int compX, compY;
		int arrayPos, arrayPos2;
		int qualityOffset;

		if (getQualityMap() == null)
		{
			setMappedImageWidth (map.getMappedImageWidth().get());
			setMappedImageHeight (map.getMappedImageHeight().get());
			setQualityMap(new AtomicIntegerArray(this.mappedImageWidth * this.mappedImageHeight));
		}
		/* Foreach row of blocks in maps ... */
		for (int thisY = 0; thisY < getMappedImageHeight(); thisY++)
		{
			/* Foreach block in current row ... */
			for (int thisX = 0; thisX < getMappedImageWidth(); thisX++)
			{
				/* Compute block index. */
				arrayPos = (thisY * getMappedImageWidth()) + thisX;
				/* If current block has low contrast or INVALID direction ... */
				if (map.getLowContrastMap().get(arrayPos) == ILfs.TRUE || 
					map.getDirectionMap().get(arrayPos) < ILfs.FALSE)
				{
					/* Set block's quality to 0/F. */
					getQualityMap().set(arrayPos, ILfs.FALSE);
				}
				else
				{
					/* Set baseline quality before looking at neighbors    */
					/*     (will subtract QualOffset below)                */
					/* If current block has low flow or high curvature ... */
					if (map.getLowFlowMap().get(arrayPos) == ILfs.TRUE || 
						map.getHighCurveMap().get(arrayPos) == ILfs.TRUE)
					{
						/* Set block's quality initially to 3/B. */
						getQualityMap().set(arrayPos, 3); // offset will be -1..-2
					}
					/* Otherwise, block is NOT low flow AND NOT high curvature... */
					else
					{
						/* Set block's quality to 4/A. */
						getQualityMap().set(arrayPos, 4); // offset will be 0..-2
					}

					/* If block within NEIGHBOR_DELTA of edge ... */
					if (thisY < ILfs.NEIGHBOR_DELTA || thisY > getMappedImageHeight() - 1 - ILfs.NEIGHBOR_DELTA || 
						thisX < ILfs.NEIGHBOR_DELTA || thisX > getMappedImageWidth() - 1 - ILfs.NEIGHBOR_DELTA)
					{
						/* Set block's quality to 1/E. */
						getQualityMap().set(arrayPos, 1);
					}
					/* Otherwise, test neighboring blocks ... */
					else
					{
						/* Initialize quality adjustment to 0. */
						qualityOffset = 0;
						/* Foreach row in neighborhood ... */
						for (compY = thisY - ILfs.NEIGHBOR_DELTA; compY <= thisY + ILfs.NEIGHBOR_DELTA;compY++)
						{
							/* Foreach block in neighborhood */
							/*  (including current block)... */
							for (compX = thisX - ILfs.NEIGHBOR_DELTA; compX <= thisX + ILfs.NEIGHBOR_DELTA;compX++)
							{
								/* Compute neighboring block's index. */
								arrayPos2 = (compY * getMappedImageWidth()) + compX;
								/* If neighbor block (which might be itself) has */
								/* low contrast or INVALID direction .. */
								if (map.getLowContrastMap().get(arrayPos2) == ILfs.TRUE || 
									map.getDirectionMap().get(arrayPos2) < ILfs.FALSE)
								{
									/* Set quality adjustment to -2. */
									qualityOffset = -2;
									/* Done with neighborhood row. */
									break;
								}
								/* Otherwise, if neighbor block (which might be */
								/* itself) has low flow or high curvature ... */
								else if (map.getLowFlowMap().get(arrayPos2) == ILfs.TRUE || 
									map.getHighCurveMap().get(arrayPos2) == ILfs.TRUE)
								{
									/* Set quality to -1 if not already -2. */
									qualityOffset = Math.min(qualityOffset, -1);
								}
							}
						}
						/* Decrement minutia quality by neighborhood adjustment. */
						getQualityMap().set(arrayPos, getQualityMap().get(arrayPos) + qualityOffset);
					}
				}
			}
		}

		return ILfs.FALSE;
	}

	/***********************************************************************
	************************************************************************
	#cat: combinedMinutiaQuality - Combines quality measures derived from
	#cat:              the quality map and neighboring pixel statistics to
	#cat:              infer a reliability measure on the scale [0...1].
	   Input:
	      oMinutiae    - structure contining the detected minutia
	      map				 - contain all below 	
	      //quality_map - map with blocks assigned 1 of 5 quality levels
	      //map_w       - width (in blocks) of the map
	      //map_h       - height (in blocks) of the map
	      blocksize   - size (in pixels) of each block in the map
	      imageData       - 8-bit grayscale fingerprint image
	      imageWidth          - width (in pixels) of the image
	      imageHeight          - height (in pixels) of the image
	      imageDepth          - depth (in pixels) of the image
	      imagePPI        - scan resolution of the image in pixels/mm
	   Output:
	      minutiae    - updated reliability members
	   Return Code:
	      Zero       - successful completion
	      Negative   - system error
	************************************************************************/
	public int combinedMinutiaQuality(AtomicReference<Minutiae> oMinutiae, Maps map, 
		final int blocksize, int [] imageData, 
		final int imageWidth, final int imageHeight, final int imageDepth, final double imagePPI) {
		AtomicInteger ret = new AtomicInteger(0);
		int minutiaPixelIndex = 0, radiusPixel;
		int qualityMapValue;
		double grayscaleReliability, reliability;

		/* If image is not 8-bit grayscale ... */
		if (imageDepth != ILfs.IMAGE_DEPTH)
		{
			LOGGER.error("ERROR : combined_miutia_quality : ");
			LOGGER.error("image must pixel depth = %d must be 8 " + imageDepth);
			LOGGER.error("to compute reliability\n");
			return (-2);
		}

		/* Compute pixel radius of neighborhood based on image's scan resolution. */
		radiusPixel = getDefs().sRound(ILfs.RADIUS_MM * imagePPI);

		/* Expand block map values to pixel map. */
		int mapSize = imageWidth * imageHeight;
		AtomicIntegerArray pqualityMap = new AtomicIntegerArray(mapSize);		
		ret.set(map.pixelizeMap(pqualityMap, imageWidth, imageHeight, 
			this.getQualityMap(), this.getMappedImageWidth(), this.getMappedImageHeight(), blocksize));
		if (ret.get() != ILfs.FALSE)
		{
			return ret.get();
		}

		//LOGGER.info("====================================================================\n");
		/* Foreach minutiae detected ... */
		for (int minutiaIndex = 0; minutiaIndex < oMinutiae.get().getNum(); minutiaIndex++)
		{
			/* Assign minutia pointer. */

			/* Compute reliability from stdev and mean of pixel neighborhood. */
			grayscaleReliability = 
				grayscaleReliability(oMinutiae.get().getList().get(minutiaIndex), 
					imageData, imageWidth, imageHeight, radiusPixel);

			/* Lookup quality map value. */
			/* Compute minutia pixel index. */
			minutiaPixelIndex = (oMinutiae.get().getList().get(minutiaIndex).getY() * imageWidth) + oMinutiae.get().getList().get(minutiaIndex).getX();
			/* Switch on pixel's quality value ... */
			qualityMapValue = pqualityMap.get(minutiaPixelIndex);

			/* Combine grayscale reliability and quality map value. */
			switch (qualityMapValue)
			{
				/* Quality A : [50..99]% */
				case 4 :
					reliability = 0.50 + (0.49 * grayscaleReliability);
					break;
				/* Quality B : [25..49]% */
				case 3 :
					reliability = 0.25 + (0.24 * grayscaleReliability);
					break;
				/* Quality C : [10..24]% */
				case 2 :
					reliability = 0.10 + (0.14 * grayscaleReliability);
					break;
				/* Quality D : [5..9]% */
				case 1 :
					reliability = 0.05 + (0.04 * grayscaleReliability);
					break;
				/* Quality E : 1% */
				case 0 :
					reliability = 0.01;
					break;
				/* Error if quality value not in range [0..4]. */
				default:
					LOGGER.error("ERROR : combined_miutia_quality : ");
					LOGGER.error("unexpected quality map value %d " + qualityMapValue);
					LOGGER.error("not in range [0..4]\n");
					getFree().free(pqualityMap);
					return (-3);
			}

			oMinutiae.get().getList().get(minutiaIndex).setReliability(reliability);
		}

		/* NEW 05-08-2002 */
		getFree().free(pqualityMap);
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/***********************************************************************
	************************************************************************
	#cat: grayscaleReliability - Given a minutia point, computes a reliability
	#cat:              measure from the stdev and mean of its pixel neighborhood.
	   GrayScaleReliability - reasonable reliability heuristic, returns
	   0.0 .. 1.0 based on stdev and Mean of a localized histogram where
	   "ideal" stdev is >=64; "ideal" Mean is 127.  In a 1 ridge radius
	   (11 pixels), if the bytevalue (shade of gray) in the image has a
	   stdev of >= 64 & a mean of 127,  returns 1.0 (well defined
	   light & dark areas in equal proportions).
	   Input:
	      minutia    - structure containing detected minutia
	      imageData      - 8-bit grayscale fingerprint image
	      imageWidth         - width (in pixels) of the image
	      imageHeight         - height (in pixels) of the image
	      radiusPixel - pixel radius of surrounding neighborhood
	   Return Value:
	      reliability - computed reliability measure
	************************************************************************/
	public double grayscaleReliability(Minutia minutia, int [] imageData, 
		final int imageWidth, final int imageHeight, final int radiusPixel) {
		AtomicReference<Double> mean = new AtomicReference<Double>(0.0), stdev = new AtomicReference<Double>(0.0);
		double reliability;

		getNeighborhoodStats(mean, stdev, minutia, imageData, imageWidth, imageHeight, radiusPixel);
		reliability = Math.min((stdev.get() > ILfs.IDEALSTDEV ? 1.0 : stdev.get() / (double)ILfs.IDEALSTDEV), (1.0 - (Math.abs(mean.get() - ILfs.IDEALMEAN) / (double)ILfs.IDEALMEAN)));

		return (reliability);
	}

	/***********************************************************************
	************************************************************************
	#cat: getNeighborhoodStats - Given a minutia point, computes the mean
	#cat:              and stdev of the 8-bit grayscale pixels values in a
	#cat:              surrounding neighborhood with specified radius.
	   Input:
	      minutia    - structure containing detected minutia
	      imageData      - 8-bit grayscale fingerprint image
	      imageWidth         - width (in pixels) of the image
	      imageHeight         - height (in pixels) of the image
	      radiusPixel - pixel radius of surrounding neighborhood
	   Output:
	      oMean       - mean of neighboring pixels
	      oStDev      - standard deviation of neighboring pixels
	************************************************************************/
	public void getNeighborhoodStats(AtomicReference<Double> oMean, AtomicReference<Double> oStDev, 
		Minutia minutia, int [] imageData, final int imageWidth, final int imageHeight, final int radiusPixel) {
		int i, x, y, rows, cols;
		int n = 0, sumX = 0, sumXX = 0;

		int[] histogram = new int[256];
		 /* Zero out histogram. */
		for (int index = 0; index < histogram.length; index++)
			histogram[index] = 0;
		
		/* Set minutia's coordinate variables. */
		x = minutia.getX();
		y = minutia.getY();

		/* If minutiae point is within sampleboxsize distance of image border, */
		/* a value of 0 reliability is returned. */
		if ((x < radiusPixel) || (x > imageWidth - radiusPixel - 1) || 
			(y < radiusPixel) || (y > imageHeight - radiusPixel - 1))
		{
			oMean.set(0.0);
			oStDev.set(0.0);
			return;
		}

		/* Foreach row in neighborhood ... */
		for (rows = y - radiusPixel; rows <= y + radiusPixel; rows++)
		{
			/* Foreach column in neighborhood ... */
			for (cols = x - radiusPixel; cols <= x + radiusPixel; cols++)
			{
				/* Bump neighbor's pixel value bin in histogram. */
				int histValue = imageData[(rows * imageWidth) + cols];
				histogram[histValue] = histogram[histValue] + 1;
			}
		}

		/* Foreach grayscale pixel bin ... */
		for (i = 0; i < 256; i++)
		{
			if (histogram[i] != ILfs.FALSE)
			{
				/* Accumulate Sum(X[i]) */
				sumX += (i * histogram[i]);
				/* Accumulate Sum(X[i]^2) */
				sumXX += (i * i * histogram[i]);
				/* Accumulate N samples */
				n += histogram[i];
			}
		}

		/* Mean = Sum(X[i])/N */
		oMean.set (sumX / (double)n);
		/* Stdev = sqrt((Sum(X[i]^2)/N) - Mean^2) */
		oStDev.set (Math.sqrt((sumXX / (double)n) - (oMean.get() * oMean.get())));
	}
		
	/***********************************************************************
	************************************************************************
	#cat: reliabilityFromQualityMap - Takes a set of minutiae and assigns
	#cat:              each one a reliability measure based on 1 of 5 possible
	#cat:              quality levels from its location in a quality map.
	   Input:
	      minutiae    - structure contining the detected minutia
	      map(quality Map) 	- map with blocks assigned 1 of 5 quality levels
	      //mappedImageWidth		- width (in blocks) of the map
	      //map_h       - height (in blocks) of the map
	      blocksize   - size (in pixels) of each block in the map
	   Output:
	      minutiae    - updated reliability members
	   Return Code:
	      Zero       - successful completion
	      Negative   - system error
	************************************************************************/
	public int reliabilityFromQualityMap(Minutiae minutiae, Maps map, 
		final int imageWidth, final int imageHeight, final int blocksize){
		AtomicInteger ret = new AtomicInteger (0);
		int index;

		/* Expand block map values to pixel map. */
		int mapSize = imageWidth * imageHeight;
		AtomicIntegerArray pqualityMap = new AtomicIntegerArray(mapSize);			
		ret.set(map.pixelizeMap(pqualityMap, imageWidth, imageHeight, 
				this.getQualityMap(), this.getMappedImageWidth(), this.getMappedImageHeight(), blocksize));
		if (ret.get() != ILfs.FALSE)
		{
			return ret.get();
		}

		/* Foreach minutiae detected ... */
		for (int minutiaIndex = 0; minutiaIndex < minutiae.getNum(); minutiaIndex++)
		{
			/* Assign minutia pointer. */
			/* Compute minutia pixel index. */
			index = (minutiae.getList().get(minutiaIndex).getY() * imageWidth) + minutiae.getList().get(minutiaIndex).getX();
			/* Switch on pixel's quality value ... */
			switch (pqualityMap.get(index))
			{
				case 0:
					minutiae.getList().get(minutiaIndex).setReliability(0.0);
					break;
				case 1:
					minutiae.getList().get(minutiaIndex).setReliability(0.25);
					break;
				case 2:
					minutiae.getList().get(minutiaIndex).setReliability(0.50);
					break;
				case 3:
					minutiae.getList().get(minutiaIndex).setReliability(0.75);
					break;
				case 4:
					minutiae.getList().get(minutiaIndex).setReliability(0.99);
					break;
				/* Error if quality value not in range [0..4]. */
				default:
					minutiae.getList().get(minutiaIndex).setReliability(0.0);
					LOGGER.error("ERROR : reliability_fr_quality_map :");
					LOGGER.error("unexpected quality value %d " + pqualityMap.get(index));
					LOGGER.error("not in range [0..4]\n");
					return (-2);
			}
		}

		/* Deallocate pixelized quality map. */
		getFree().free(pqualityMap);

		/* Return normally. */
		return (ILfs.FALSE);
	}
	
	public AtomicIntegerArray getQualityMap() {
		return qualityMap;
	}
	public void setQualityMap(AtomicIntegerArray qualityMap) {
		this.qualityMap = qualityMap;
	}	

	public int getMappedImageWidth() {
		return mappedImageWidth;
	}

	public void setMappedImageWidth(int mappedImageWidth) {
		this.mappedImageWidth = mappedImageWidth;
	}

	public int getMappedImageHeight() {
		return mappedImageHeight;
	}

	public void setMappedImageHeight(int mappedImageHeight) {
		this.mappedImageHeight = mappedImageHeight;
	}
}


