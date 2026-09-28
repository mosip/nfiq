package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IQuality;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block quality map generation and minutia reliability assignment for MINDTCT.
 * <p>
 * Port of NIST LFS {@code quality.c}. Combines the direction, low contrast,
 * low ridge flow and high curvature block maps into a single quality map with
 * five levels (0 = unusable .. 4 = good), and then assigns each detected
 * minutia a reliability value derived from that map and, optionally, from the
 * grayscale statistics of the minutia's pixel neighbourhood. NFIQ 1.0 uses
 * the quality map and minutia reliabilities as inputs to its feature vector.
 * <p>
 * Implemented as a lazily created singleton, but unlike most MINDTCT helpers it
 * holds mutable state (the current quality map and its dimensions) that is
 * overwritten by {@link #generateQualityMap(Maps)}. It is therefore <b>not</b>
 * thread-safe: concurrent quality computations must be serialized externally.
 */
public class Quality extends MindTct implements IQuality {
	/** SLF4J logger for error reporting in this class. */
	private static final Logger logger = LoggerFactory.getLogger(Quality.class);
	/**
	 * Lazily created singleton instance, see {@link #getInstance()} and
	 * {@link #getInstance(int, int)}.
	 */
	private static Quality instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 * The quality map is left unallocated until
	 * {@link #generateQualityMap(Maps)} is called.
	 */
	private Quality() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Quality} instance
	 */
	public static synchronized Quality getInstance() {
		if (instance == null) {
			instance = new Quality();
		}
		return instance;
	}

	/**
	 * Returns the shared singleton instance, creating it on first use with a
	 * pre-allocated quality map of the given dimensions.
	 * <p>
	 * If the singleton already exists, the arguments are ignored and the
	 * existing instance is returned unchanged.
	 *
	 * @param mappedImageWidth  number of blocks horizontally in the padded input
	 *                          image
	 * @param mappedImageHeight number of blocks vertically in the padded input
	 *                          image
	 * @return the singleton {@code Quality} instance
	 */
	public static synchronized Quality getInstance(int mappedImageWidth, int mappedImageHeight) {
		if (instance == null) {
			instance = new Quality(mappedImageWidth, mappedImageHeight);
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Defs} helper (rounding utilities).
	 *
	 * @return the {@code Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper used to release buffers.
	 *
	 * @return the {@code Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Current block quality map (row-major, {@code mappedImageWidth *
	 * mappedImageHeight} entries, values 0..4), produced by
	 * {@link #generateQualityMap(Maps)}.
	 */
	private AtomicIntegerArray qualityMap;
	// mappedImageWidth - number of blocks horizontally in the padded input image
	// mh - number of blocks vertically in the padded input image
	/** Width of the quality map, in blocks (number of blocks horizontally). */
	private int mappedImageWidth;
	/** Height of the quality map, in blocks (number of blocks vertically). */
	private int mappedImageHeight;

	/**
	 * Private constructor that pre-allocates a zeroed quality map of the given
	 * dimensions; use {@link #getInstance(int, int)} to obtain the singleton.
	 *
	 * @param mappedImageWidth  number of blocks horizontally in the padded input
	 *                          image
	 * @param mappedImageHeight number of blocks vertically in the padded input
	 *                          image
	 */
	private Quality(int mappedImageWidth, int mappedImageHeight) {
		super();
		/* Compute total number of blocks in map */
		this.mappedImageWidth = mappedImageWidth;
		this.mappedImageHeight = mappedImageHeight;
		this.qualityMap = new AtomicIntegerArray(this.mappedImageWidth * this.mappedImageHeight);
	}

	/**
	 * Takes a direction map, low contrast map, low ridge flow map and high
	 * curvature map, and combines them into a single map containing 5 levels of
	 * decreasing quality.
	 * <p>
	 * NIST: {@code gen_quality_map()}. This is done through a set of heuristics
	 * that set the quality of each block to 0 (unusable) .. 4 (good), also
	 * referred to as grades A..F:
	 * <ul>
	 * <li>0/F: low contrast OR no direction</li>
	 * <li>1/D: low flow OR high curve (with low contrast OR no direction
	 * neighbour), or within {@link ILfs#NEIGHBOR_DELTA} of the edge</li>
	 * <li>2/C: low flow OR high curve (or good quality with low contrast/no
	 * direction neighbour)</li>
	 * <li>3/B: good quality with low flow / high curve neighbour</li>
	 * <li>4/A: good quality (none of the above)</li>
	 * </ul>
	 * Generally, the features in A/B quality are useful, the C/D quality ones are
	 * not.
	 * <p>
	 * Side effects: resets this instance's map dimensions from {@code map} and
	 * allocates a new quality map, retrievable afterwards via
	 * {@link #getQualityMap()}.
	 *
	 * @param map container holding the direction map (blocks assigned dominant
	 *            ridge flow direction), low contrast map, low ridge flow map,
	 *            high curvature map, and the width/height (in blocks) of the maps
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int generateQualityMap(Maps map) {
		int compX;
		int compY;
		int arrayPos;
		int arrayPos2;
		int qualityOffset;

		//if (getQualityMap() == null) {
			setMappedImageWidth(map.getMappedImageWidth().get());
			setMappedImageHeight(map.getMappedImageHeight().get());
			setQualityMap(new AtomicIntegerArray(this.mappedImageWidth * this.mappedImageHeight));
		//}
		/* Foreach row of blocks in maps ... */
		for (int thisY = 0; thisY < getMappedImageHeight(); thisY++) {
			/* Foreach block in current row ... */
			for (int thisX = 0; thisX < getMappedImageWidth(); thisX++) {
				/* Compute block index. */
				arrayPos = (thisY * getMappedImageWidth()) + thisX;
				/* If current block has low contrast or INVALID direction ... */
				if (map.getLowContrastMap().get(arrayPos) == ILfs.TRUE
						|| map.getDirectionMap().get(arrayPos) < ILfs.FALSE) {
					/* Set block's quality to 0/F. */
					getQualityMap().set(arrayPos, ILfs.FALSE);
				} else {
					/* Set baseline quality before looking at neighbors */
					/* (will subtract QualOffset below) */
					/* If current block has low flow or high curvature ... */
					if (map.getLowFlowMap().get(arrayPos) == ILfs.TRUE
							|| map.getHighCurveMap().get(arrayPos) == ILfs.TRUE) {
						/* Set block's quality initially to 3/B. */
						getQualityMap().set(arrayPos, 3); // offset will be -1..-2
					}
					/* Otherwise, block is NOT low flow AND NOT high curvature... */
					else {
						/* Set block's quality to 4/A. */
						getQualityMap().set(arrayPos, 4); // offset will be 0..-2
					}

					/* If block within NEIGHBOR_DELTA of edge ... */
					if (thisY < ILfs.NEIGHBOR_DELTA || thisY > getMappedImageHeight() - 1 - ILfs.NEIGHBOR_DELTA
							|| thisX < ILfs.NEIGHBOR_DELTA || thisX > getMappedImageWidth() - 1 - ILfs.NEIGHBOR_DELTA) {
						/* Set block's quality to 1/E. */
						getQualityMap().set(arrayPos, 1);
					}
					/* Otherwise, test neighboring blocks ... */
					else {
						/* Initialize quality adjustment to 0. */
						qualityOffset = 0;
						/* Foreach row in neighborhood ... */
						for (compY = thisY - ILfs.NEIGHBOR_DELTA; compY <= thisY + ILfs.NEIGHBOR_DELTA; compY++) {
							/* Foreach block in neighborhood */
							/* (including current block)... */
							for (compX = thisX - ILfs.NEIGHBOR_DELTA; compX <= thisX + ILfs.NEIGHBOR_DELTA; compX++) {
								/* Compute neighboring block's index. */
								arrayPos2 = (compY * getMappedImageWidth()) + compX;
								/* If neighbor block (which might be itself) has */
								/* low contrast or INVALID direction .. */
								if (map.getLowContrastMap().get(arrayPos2) == ILfs.TRUE
										|| map.getDirectionMap().get(arrayPos2) < ILfs.FALSE) {
									/* Set quality adjustment to -2. */
									qualityOffset = -2;
									/* Done with neighborhood row. */
									break;
								}
								/* Otherwise, if neighbor block (which might be */
								/* itself) has low flow or high curvature ... */
								else if (map.getLowFlowMap().get(arrayPos2) == ILfs.TRUE
										|| map.getHighCurveMap().get(arrayPos2) == ILfs.TRUE) {
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

	/**
	 * Combines quality measures derived from the quality map and neighbouring
	 * pixel statistics to infer a reliability measure on the scale [0...1].
	 * <p>
	 * NIST: {@code combined_minutia_quality()}. The block quality map held by
	 * this instance (see {@link #generateQualityMap(Maps)}) is expanded to a
	 * pixel map; for each minutia the grayscale reliability of its neighbourhood
	 * (radius {@link ILfs#RADIUS_MM} times {@code imagePPI}) is combined with its
	 * quality level: A (4) maps to [50..99]%, B (3) to [25..49]%, C (2) to
	 * [10..24]%, D (1) to [5..9]% and E (0) to 1%.
	 *
	 * @param oMinutiae   input/output: structure containing the detected
	 *                    minutiae; each minutia's reliability member is updated
	 * @param map         map container used to pixelize this instance's quality
	 *                    map
	 * @param blocksize   size (in pixels) of each block in the map
	 * @param imageData   8-bit grayscale fingerprint image
	 * @param imageWidth  width (in pixels) of the image
	 * @param imageHeight height (in pixels) of the image
	 * @param imageDepth  depth (in bits per pixel) of the image; must be
	 *                    {@link ILfs#IMAGE_DEPTH} (8)
	 * @param imagePPI    scan resolution of the image in pixels/mm
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error: -2 image is not 8-bit, -3 unexpected quality map
	 *         value, or the error code returned by map pixelization
	 */
	public int combinedMinutiaQuality(AtomicReference<Minutiae> oMinutiae, Maps map, final int blocksize,
			int[] imageData, final int imageWidth, final int imageHeight, final int imageDepth, final double imagePPI) {
		AtomicInteger ret = new AtomicInteger(0);
		int minutiaPixelIndex = 0, radiusPixel;
		int qualityMapValue;
		double grayscaleReliability, reliability;

		/* If image is not 8-bit grayscale ... */
		if (imageDepth != ILfs.IMAGE_DEPTH) {
			logger.error("ERROR : combined_miutia_quality : ");
			logger.error("image must pixel depth = {} must be 8 ", imageDepth);
			logger.error("to compute reliability\n");
			return (-2);
		}

		/* Compute pixel radius of neighborhood based on image's scan resolution. */
		radiusPixel = getDefs().sRound(ILfs.RADIUS_MM * imagePPI);

		/* Expand block map values to pixel map. */
		int mapSize = imageWidth * imageHeight;
		AtomicIntegerArray pqualityMap = new AtomicIntegerArray(mapSize);
		ret.set(map.pixelizeMap(pqualityMap, imageWidth, imageHeight, this.getQualityMap(), this.getMappedImageWidth(),
				this.getMappedImageHeight(), blocksize));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		// logger.info("====================================================================\n");
		/* Foreach minutiae detected ... */
		for (int minutiaIndex = 0; minutiaIndex < oMinutiae.get().getNum(); minutiaIndex++) {
			/* Assign minutia pointer. */

			/* Compute reliability from stdev and mean of pixel neighborhood. */
			grayscaleReliability = grayscaleReliability(oMinutiae.get().getList().get(minutiaIndex), imageData,
					imageWidth, imageHeight, radiusPixel);

			/* Lookup quality map value. */
			/* Compute minutia pixel index. */
			minutiaPixelIndex = (oMinutiae.get().getList().get(minutiaIndex).getY() * imageWidth)
					+ oMinutiae.get().getList().get(minutiaIndex).getX();
			/* Switch on pixel's quality value ... */
			qualityMapValue = pqualityMap.get(minutiaPixelIndex);

			/* Combine grayscale reliability and quality map value. */
			switch (qualityMapValue) {
			/* Quality A : [50..99]% */
			case 4:
				reliability = 0.50 + (0.49 * grayscaleReliability);
				break;
			/* Quality B : [25..49]% */
			case 3:
				reliability = 0.25 + (0.24 * grayscaleReliability);
				break;
			/* Quality C : [10..24]% */
			case 2:
				reliability = 0.10 + (0.14 * grayscaleReliability);
				break;
			/* Quality D : [5..9]% */
			case 1:
				reliability = 0.05 + (0.04 * grayscaleReliability);
				break;
			/* Quality E : 1% */
			case 0:
				reliability = 0.01;
				break;
			/* Error if quality value not in range [0..4]. */
			default:
				logger.error("ERROR : combined_miutia_quality : ");
				logger.error("unexpected quality map value {} ", qualityMapValue);
				logger.error("not in range [0..4]\n");
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

	/**
	 * Given a minutia point, computes a reliability measure from the standard
	 * deviation and mean of its pixel neighbourhood.
	 * <p>
	 * NIST: {@code grayscale_reliability()}. A reasonable reliability heuristic
	 * returning 0.0 .. 1.0 based on the stdev and mean of a localized histogram,
	 * where the "ideal" stdev is {@code >= 64} ({@link ILfs#IDEALSTDEV}) and the
	 * "ideal" mean is 127 ({@link ILfs#IDEALMEAN}). In a one ridge radius (11
	 * pixels), if the byte value (shade of gray) in the image has a stdev of
	 * {@code >= 64} and a mean of 127, returns 1.0 (well defined light and dark
	 * areas in equal proportions).
	 *
	 * @param minutia     structure containing the detected minutia
	 * @param imageData   8-bit grayscale fingerprint image
	 * @param imageWidth  width (in pixels) of the image
	 * @param imageHeight height (in pixels) of the image
	 * @param radiusPixel pixel radius of the surrounding neighbourhood
	 * @return the computed reliability measure (0.0 .. 1.0)
	 */
	public double grayscaleReliability(Minutia minutia, int[] imageData, final int imageWidth, final int imageHeight,
			final int radiusPixel) {
		AtomicReference<Double> mean = new AtomicReference<>(0.0), stdev = new AtomicReference<>(0.0);
		double reliability;

		getNeighborhoodStats(mean, stdev, minutia, imageData, imageWidth, imageHeight, radiusPixel);
		reliability = Math.min((stdev.get() > ILfs.IDEALSTDEV ? 1.0 : stdev.get() / (double) ILfs.IDEALSTDEV),
				(1.0 - (Math.abs(mean.get() - ILfs.IDEALMEAN) / ILfs.IDEALMEAN)));

		return (reliability);
	}

	/**
	 * Given a minutia point, computes the mean and standard deviation of the
	 * 8-bit grayscale pixel values in a surrounding square neighbourhood with the
	 * specified radius.
	 * <p>
	 * NIST: {@code get_neighborhood_stats()}. If the minutia lies within
	 * {@code radiusPixel} of the image border (or the neighbourhood is empty),
	 * both outputs are set to 0.0, which yields zero reliability.
	 *
	 * @param oMean       output: mean of neighbouring pixels
	 * @param oStDev      output: standard deviation of neighbouring pixels
	 * @param minutia     structure containing the detected minutia
	 * @param imageData   8-bit grayscale fingerprint image
	 * @param imageWidth  width (in pixels) of the image
	 * @param imageHeight height (in pixels) of the image
	 * @param radiusPixel pixel radius of the surrounding neighbourhood
	 */
	public void getNeighborhoodStats(AtomicReference<Double> oMean, AtomicReference<Double> oStDev, Minutia minutia,
			int[] imageData, final int imageWidth, final int imageHeight, final int radiusPixel) {
		int i;
		int x;
		int y;
		int rows;
		int cols;
		int n = 0;
		int sumX = 0;
		int sumXX = 0;

		int[] histogram = new int[256];
		/* Zero out histogram. */
		for (int index = 0; index < histogram.length; index++)
			histogram[index] = 0;

		/* Set minutia's coordinate variables. */
		x = minutia.getX();
		y = minutia.getY();

		/* If minutiae point is within sampleboxsize distance of image border, */
		/* a value of 0 reliability is returned. */
		if ((x < radiusPixel) || (x > imageWidth - radiusPixel - 1) || (y < radiusPixel)
				|| (y > imageHeight - radiusPixel - 1)) {
			oMean.set(0.0);
			oStDev.set(0.0);
			return;
		}

		/* Foreach row in neighborhood ... */
		for (rows = y - radiusPixel; rows <= y + radiusPixel; rows++) {
			/* Foreach column in neighborhood ... */
			for (cols = x - radiusPixel; cols <= x + radiusPixel; cols++) {
				/* Bump neighbor's pixel value bin in histogram. */
				int histValue = imageData[(rows * imageWidth) + cols];
				histogram[histValue] = histogram[histValue] + 1;
			}
		}

		/* Foreach grayscale pixel bin ... */
		for (i = 0; i < 256; i++) {
			if (histogram[i] != ILfs.FALSE) {
				/* Accumulate Sum(X[i]) */
				sumX += (i * histogram[i]);
				/* Accumulate Sum(X[i]^2) */
				sumXX += (i * i * histogram[i]);
				/* Accumulate N samples */
				n += histogram[i];
			}
		}

		if (n == 0) {
		    oMean.set(0.0);
		    oStDev.set(0.0);
		    return;
		}
		/* Mean = Sum(X[i])/N */
		oMean.set(sumX / (double) n);
		/* Stdev = sqrt((Sum(X[i]^2)/N) - Mean^2) */
		oStDev.set(Math.sqrt((sumXX / (double) n) - (oMean.get() * oMean.get())));
	}

	/**
	 * Takes a set of minutiae and assigns each one a reliability measure based on
	 * one of 5 possible quality levels from its location in the quality map.
	 * <p>
	 * NIST: {@code reliability_fr_quality_map()}. The block quality map held by
	 * this instance is expanded to a pixel map and quality levels 0..4 are mapped
	 * to reliabilities 0.0, 0.25, 0.50, 0.75 and 0.99 respectively.
	 *
	 * @param minutiae    input/output: structure containing the detected
	 *                    minutiae; each minutia's reliability member is updated
	 * @param map         map container used to pixelize this instance's quality
	 *                    map (blocks assigned one of 5 quality levels)
	 * @param imageWidth  width (in pixels) of the image
	 * @param imageHeight height (in pixels) of the image
	 * @param blocksize   size (in pixels) of each block in the map
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error: -2 unexpected quality value, or the error code
	 *         returned by map pixelization
	 */
	public int reliabilityFromQualityMap(Minutiae minutiae, Maps map, final int imageWidth, final int imageHeight,
			final int blocksize) {
		AtomicInteger ret = new AtomicInteger(0);
		int index;

		/* Expand block map values to pixel map. */
		int mapSize = imageWidth * imageHeight;
		AtomicIntegerArray pqualityMap = new AtomicIntegerArray(mapSize);
		ret.set(map.pixelizeMap(pqualityMap, imageWidth, imageHeight, this.getQualityMap(), this.getMappedImageWidth(),
				this.getMappedImageHeight(), blocksize));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* Foreach minutiae detected ... */
		for (int minutiaIndex = 0; minutiaIndex < minutiae.getNum(); minutiaIndex++) {
			/* Assign minutia pointer. */
			/* Compute minutia pixel index. */
			index = (minutiae.getList().get(minutiaIndex).getY() * imageWidth)
					+ minutiae.getList().get(minutiaIndex).getX();
			/* Switch on pixel's quality value ... */
			switch (pqualityMap.get(index)) {
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
				logger.error("ERROR : reliability_fr_quality_map :");
				logger.error("unexpected quality value {} ", pqualityMap.get(index));
				logger.error("not in range [0..4]\n");
				return (-2);
			}
		}

		/* Deallocate pixelized quality map. */
		getFree().free(pqualityMap);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Returns the current block quality map.
	 *
	 * @return the quality map (values 0..4, row-major), or {@code null} if not
	 *         yet generated
	 */
	public AtomicIntegerArray getQualityMap() {
		return qualityMap;
	}

	/**
	 * Replaces the current block quality map.
	 *
	 * @param qualityMap new quality map (stored by reference)
	 */
	public void setQualityMap(AtomicIntegerArray qualityMap) {
		this.qualityMap = qualityMap;
	}

	/**
	 * Returns the width of the quality map.
	 *
	 * @return the map width, in blocks
	 */
	public int getMappedImageWidth() {
		return mappedImageWidth;
	}

	/**
	 * Sets the width of the quality map.
	 *
	 * @param mappedImageWidth the map width, in blocks
	 */
	public void setMappedImageWidth(int mappedImageWidth) {
		this.mappedImageWidth = mappedImageWidth;
	}

	/**
	 * Returns the height of the quality map.
	 *
	 * @return the map height, in blocks
	 */
	public int getMappedImageHeight() {
		return mappedImageHeight;
	}

	/**
	 * Sets the height of the quality map.
	 *
	 * @param mappedImageHeight the map height, in blocks
	 */
	public void setMappedImageHeight(int mappedImageHeight) {
		this.mappedImageHeight = mappedImageHeight;
	}
}