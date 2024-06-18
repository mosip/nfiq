package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.IMaps;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Maps extends MindTct implements IMaps {
	private static final Logger logger = LoggerFactory.getLogger(Maps.class);
	private static Maps instance;
	private AtomicIntegerArray directionMap;
	private AtomicIntegerArray lowContrastMap;
	private AtomicIntegerArray lowFlowMap;
	private AtomicIntegerArray highCurveMap;
	// mappedImageWidth - number of blocks horizontally in the padded input image
	// mappedImageHeight - number of blocks vertically in the padded input image
	private AtomicInteger mappedImageWidth;
	private AtomicInteger mappedImageHeight;

	private Maps() {
		super();
		setMappedImageWidth(new AtomicInteger(0));
		setMappedImageHeight(new AtomicInteger(0));
	}

	private Maps(int mappedImageWidth, int mappedImageHeight) {
		super();
		this.mappedImageWidth = new AtomicInteger(mappedImageWidth);
		this.mappedImageHeight = new AtomicInteger(mappedImageHeight);

		/* Compute total number of blocks in map */
		int mapSize = mappedImageWidth * mappedImageHeight;
		directionMap = new AtomicIntegerArray(mapSize);
		lowContrastMap = new AtomicIntegerArray(mapSize);
		lowFlowMap = new AtomicIntegerArray(mapSize);
		highCurveMap = new AtomicIntegerArray(mapSize);
	}

	private Maps(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap, AtomicIntegerArray lowFlowMap,
			AtomicIntegerArray highCurveMap) {
		super();
		this.directionMap = directionMap;
		this.lowContrastMap = lowContrastMap;
		this.lowFlowMap = lowFlowMap;
		this.highCurveMap = highCurveMap;
	}

	public static synchronized Maps getInstance() {
		if (instance == null) {
			instance = new Maps();
		}
		return instance;
	}

	public static synchronized Maps getInstance(int mappedImageWidth, int mappedImageHeight) {
		if (instance == null) {
			instance = new Maps(mappedImageWidth, mappedImageHeight);
		}
		return instance;
	}

	public static synchronized Maps getInstance(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
			AtomicIntegerArray lowFlowMap, AtomicIntegerArray highCurveMap) {
		if (instance == null) {
			instance = new Maps(directionMap, lowContrastMap, lowFlowMap, highCurveMap);
		}
		return instance;
	}

	public Defs getDefs() {
		return Defs.getInstance();
	}

	public Block getBlock() {
		return Block.getInstance();
	}

	public Init getInit() {
		return Init.getInstance();
	}

	public Free getFree() {
		return Free.getInstance();
	}

	public Dft getDft() {
		return Dft.getInstance();
	}

	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	public Morph getMorph() {
		return Morph.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: genImageMaps - Computes a set of image maps based on Version 2 #cat: of
	 * the NIST LFS System. The first map is a Direction Map #cat: which is a 2D
	 * vector of integer directions, where each #cat: direction represents the
	 * dominant ridge flow in a block of #cat: the input grayscale image. The Low
	 * Contrast Map flags #cat: blocks with insufficient contrast. The Low Flow Map
	 * flags #cat: blocks with insufficient ridge flow. The High Curve Map #cat:
	 * flags blocks containing high curvature. This routine will #cat: generate maps
	 * for an arbitrarily sized, non-square, image. Input: paddedImagedata - padded
	 * input image data (8 bits [0..256) grayscale) paddedImageWidth - padded width
	 * (in pixels) of the input image paddedImageHeight - padded height (in pixels)
	 * of the input image dirToRad - lookup table for converting integer directions
	 * dftWaves - structure containing the DFT wave forms dftGrids - structure
	 * containing the rotated pixel grid offsets lfsParams - parameters and
	 * thresholds for controlling LFS Output: //oDirectionMap - points to the
	 * created Direction Map(Take from Map Object Get Method) //oLowContrastMap -
	 * points to the created Low Contrast Map(Take from Map Object Get Method)
	 * //oLowFlowMap - points to the Low Ridge Flow Map(Take from Map Object Get
	 * Method) //oHighCurvatureMap - points to the High Curvature Map(Take from Map
	 * Object Get Method) //omw - width (in blocks) of the maps(Take from Get Map
	 * Object Method) //omh - height (in blocks) of the maps (Take from Map Object
	 * Get Method) Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int genImageMaps(int[] paddedImagedata, final int paddedImageWidth, final int paddedImageHeight,
			DirToRad dirToRad, DftWaves dftWaves, RotGrids dftGrids, LfsParams lfsParams) {
		AtomicInteger mappedImageWidth = new AtomicInteger(0);
		AtomicInteger mappedImageHeight = new AtomicInteger(0);
		int imageWidth;
		int imageHeight;
		AtomicIntegerArray blockOffsets;
		AtomicInteger ret = new AtomicInteger(0); // return code

		/* 1. Compute block offsets for the entire image, accounting for pad */
		/* Block_offsets() assumes square block (grid), so ERROR otherwise. */
		if (dftGrids.getGridWidth() != dftGrids.getGridHeight()) {
			logger.error("ERROR : genImageMaps : DFT grids must be square");
			return (ILfs.ERROR_CODE_540);
		}
		/* Compute unpadded image dimensions. */
		imageWidth = paddedImageWidth - (dftGrids.getPad() << 1);
		imageHeight = paddedImageHeight - (dftGrids.getPad() << 1);
		blockOffsets = getBlock().blockOffsets(ret, mappedImageWidth, mappedImageHeight, imageWidth, imageHeight,
				dftGrids.getPad(), lfsParams.getBlockOffsetSize());
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* Compute total number of blocks in map */
		int mapSize = mappedImageWidth.get() * mappedImageHeight.get();

		setMappedImageWidth(new AtomicInteger(mappedImageWidth.get()));
		setMappedImageHeight(new AtomicInteger(mappedImageHeight.get()));

		/* Allocate Direction Map memory */
		setDirectionMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Direction Map to INVALID (-1). */
		for (int dmIndex = 0; dmIndex < getDirectionMap().length(); dmIndex++)
			getDirectionMap().set(dmIndex, ILfs.INVALID_DIR);

		/* Allocate Low Contrast Map memory */
		setLowContrastMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Low Contrast Map to FALSE (0). */
		for (int lcmIndex = 0; lcmIndex < getLowContrastMap().length(); lcmIndex++)
			getLowContrastMap().set(lcmIndex, 0);

		/* Allocate Low Ridge Flow Map memory */
		setLowFlowMap(new AtomicIntegerArray(mapSize));
		/* Initialize the Low Flow Map to FALSE (0). */
		for (int lfmIndex = 0; lfmIndex < getLowFlowMap().length(); lfmIndex++)
			getLowFlowMap().set(lfmIndex, 0);

		/*
		 * 2. Generate initial Direction Map and Low Contrast Map and Low Ridge Flow Map
		 */
		ret.set(initialiseMaps(getDirectionMap(), getLowContrastMap(), getLowFlowMap(), blockOffsets,
				getMappedImageWidth().get(), getMappedImageHeight().get(), paddedImagedata, paddedImageWidth,
				paddedImageHeight, dftWaves, dftGrids, lfsParams));
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			getFree().free(blockOffsets);
			return ret.get();
		}

		ret.set(morphMapWithTF(getLowFlowMap(), lfsParams));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* 3. Remove directions that are inconsistent with neighbors */
		removeInconsistentDirs(getDirectionMap(), dirToRad, lfsParams);

		/* 4. Smooth Direction Map values with their neighbors */
		smoothDirectionMap(getDirectionMap(), getLowContrastMap(), dirToRad, lfsParams);

		/* 5. Interpolate INVALID direction blocks with their valid neighbors. */
		ret.set(interpolateDirectionMap(getDirectionMap(), getLowContrastMap(), getMappedImageWidth().get(),
				getMappedImageHeight().get(), lfsParams));
		if (ret.get() != 0) {
			return ret.get();
		}
		/* May be able to skip steps 6 and/or 7 if computation time */
		/* is a critical factor. */

		/* 6. Remove directions that are inconsistent with neighbors */
		removeInconsistentDirs(getDirectionMap(), dirToRad, lfsParams);

		/* 7. Smooth Direction Map values with their neighbors. */
		smoothDirectionMap(getDirectionMap(), getLowContrastMap(), dirToRad, lfsParams);

		/* 8. Set the Direction Map values in the image margin to INVALID. */
		getBlock().setMarginBlocks(getDirectionMap(), getMappedImageWidth().get(), getMappedImageHeight().get(),
				ILfs.INVALID_DIR);

		/* Allocate High Curvature Map. */
		setHighCurveMap(new AtomicIntegerArray(mapSize));
		/* Initialize High Curvature Map to FALSE (0). */
		for (int hcmIndex = 0; hcmIndex < getHighCurveMap().length(); hcmIndex++)
			getHighCurveMap().set(hcmIndex, 0);

		/* 9. Generate High Curvature Map from interpolated Direction Map. */
		ret.set(generateHighCurveMap(getHighCurveMap(), getDirectionMap(), getMappedImageWidth().get(),
				getMappedImageHeight().get(), lfsParams));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		/* Deallocate working memory. */
		getFree().free(blockOffsets);

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: initialiseMaps - Creates an initial Direction Map from the given #cat:
	 * input image. It very important that the image be properly #cat: padded so
	 * that rotated grids along the boundary of the image #cat: do not access unkown
	 * memory. The rotated grids are used by a #cat: DFT-based analysis to determine
	 * the integer directions #cat: in the map. Typically this initial vector of
	 * directions will #cat: subsequently have weak or inconsistent directions
	 * removed #cat: followed by a smoothing process. The resulting Direction #cat:
	 * Map contains valid directions >= 0 and INVALID values = -1. #cat: This
	 * routine also computes and returns 2 other image maps. #cat: The Low Contrast
	 * Map flags blocks in the image with #cat: insufficient contrast. Blocks with
	 * low contrast have a #cat: corresponding direction of INVALID in the Direction
	 * Map. #cat: The Low Flow Map flags blocks in which the DFT analyses #cat:
	 * could not determine a significant ridge flow. Blocks with #cat: low ridge
	 * flow also have a corresponding direction of #cat: INVALID in the Direction
	 * Map. Input: blockOffsets - offsets to the pixel origin of each block in the
	 * padded image mappedImageWidth - number of blocks horizontally in the padded
	 * input image mappedImageHeight - number of blocks vertically in the padded
	 * input image paddedImagedata - padded input image data (8 bits [0..256)
	 * grayscale) paddedImageWidth - width (in pixels) of the padded input image
	 * paddedImageHeight - height (in pixels) of the padded input image dftWaves -
	 * structure containing the DFT wave forms dftGrids - structure containing the
	 * rotated pixel grid offsets lfsParams - parameters and thresholds for
	 * controlling LFS Output: oDirectionMap - points to the newly created Direction
	 * Map oLowContrastMap - points to the newly created Low Contrast Map Return
	 * Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int initialiseMaps(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			AtomicIntegerArray oLowFlowMap, AtomicIntegerArray blockOffsets, final int mappedImageWidth,
			final int mappedImageHeight, int[] paddedImagedata, final int paddedImageWidth, final int paddedImageHeight,
			final DftWaves dftWaves, final RotGrids dftGrids, final LfsParams lfsParams) {
		int bi;
		int bSize;
		int blockDir;
		AtomicIntegerArray wis;
		AtomicIntegerArray powmaxDirs;
		AtomicReferenceArray<Double[]> powers;
		AtomicReferenceArray<Double> powmaxs;
		AtomicReferenceArray<Double> pownorms;
		int nStats;
		AtomicInteger ret = new AtomicInteger(0); // return code
		int dftOffset;
		int xminLimit;
		int xmaxLimit;
		int yminLimit;
		int ymaxLimit;
		int winX;
		int winY;
		int lowContrastOffset;

		if (isShowLogs())
			logger.info("INITIAL MAP");

		/* Compute total number of blocks in map */
		bSize = mappedImageWidth * mappedImageHeight;

		/* Initialize the Direction Map to INVALID (-1). */
		for (int i = 0; i < oDirectionMap.length(); i++)
			oDirectionMap.set(i, ILfs.INVALID_DIR);

		/* Initialize the Low Contrast Map to FALSE (0). */
		for (int i = 0; i < oLowContrastMap.length(); i++)
			oLowContrastMap.set(i, ILfs.FALSE);

		/* Initialize the Low Flow Map to FALSE (0). */
		for (int i = 0; i < oLowFlowMap.length(); i++)
			oLowFlowMap.set(i, ILfs.FALSE);

		/* Allocate DFT directional power vectors */
		powers = getInit().allocDirPowers(ret, dftWaves.getNWaves(), dftGrids.getNoOfGrids());
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			oDirectionMap = null;
			oLowContrastMap = null;
			oLowFlowMap = null;
			return (ret.get());
		}

		/* Allocate DFT power statistic arrays */
		/* Compute length of statistics arrays. Statistics not needed */
		/* for the first DFT wave, so the length is number of waves - 1. */
		nStats = dftWaves.getNWaves() - 1;
		wis = new AtomicIntegerArray(nStats);
		powmaxs = new AtomicReferenceArray<Double>(nStats);
		powmaxDirs = new AtomicIntegerArray(nStats);
		pownorms = new AtomicReferenceArray<Double>(nStats);

		/* Compute special window origin limits for determining low contrast. */
		/* These pixel limits avoid analyzing the padded borders of the image. */
		xminLimit = dftGrids.getPad();
		yminLimit = dftGrids.getPad();
		xmaxLimit = paddedImageWidth - dftGrids.getPad() - lfsParams.getWindowSize() - 1;
		ymaxLimit = paddedImageHeight - dftGrids.getPad() - lfsParams.getWindowSize() - 1;

		/* Foreach block in image ... */
		for (bi = 0; bi < bSize; bi++) {
			/* Adjust block offset from pointing to block origin to pointing */
			/* to surrounding window origin. */
			dftOffset = blockOffsets.get(bi) - (lfsParams.getWindowOffset() * paddedImageWidth)
					- lfsParams.getWindowOffset();

			/* Compute pixel coords of window origin. */
			winX = dftOffset % paddedImageWidth;
			winY = (int) (dftOffset / paddedImageWidth);

			/* Make sure the current window does not access padded image pixels */
			/* for analyzing low contrast. */
			winX = Math.max(xminLimit, winX);
			winX = Math.min(xmaxLimit, winX);
			winY = Math.max(yminLimit, winY);
			winY = Math.min(ymaxLimit, winY);
			lowContrastOffset = (winY * paddedImageWidth) + winX;

			if (isShowLogs())
				logger.info("   MAP BLOCK {} ({}, {}) ", bi, bi % mappedImageWidth,
						bi / mappedImageWidth);

			/* If block is low contrast ... */
			ret.set(getBlock().lowContrastBlock(lowContrastOffset, lfsParams.getWindowSize(), paddedImagedata,
					paddedImageWidth, paddedImageHeight, lfsParams));
			if (ret.get() != ILfs.FALSE) {
				/* If system error ... */
				if (ret.get() < ILfs.FALSE) {
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					wis = null;
					powmaxs = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				/* Otherwise, block is low contrast ... */
				if (isShowLogs())
					logger.info("LOW CONTRAST");
				oLowContrastMap.set(bi, ILfs.TRUE);// = 1 = true
				/* Direction Map's block is already set to INVALID. */
			}
			/* Otherwise, sufficient contrast for DFT processing ... */
			else {
				if (isShowLogs())
					logger.info("");
				/* Compute DFT powers */
				ret.set(getDft().dftDirPowers(powers, paddedImagedata, lowContrastOffset, paddedImageWidth,
						paddedImageHeight, dftWaves, dftGrids));
				if (ret.get() != ILfs.FALSE) {
					/* Free memory allocated to this point. */
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					wis = null;
					powmaxs = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				/* Compute DFT power statistics, skipping first applied DFT */
				/* wave. This is dependent on how the primary and secondary */
				/* direction tests work below. */
				ret.set(getDft().getDftPowerStats(wis, powmaxs, powmaxDirs, pownorms, powers, ILfs.TRUE,
						dftWaves.getNWaves(), dftGrids.getNoOfGrids()));
				if (ret.get() != ILfs.FALSE) {
					/* Free memory allocated to this point. */
					oDirectionMap = null;
					oLowContrastMap = null;
					oLowFlowMap = null;
					getFree().freeDirPowers(powers, dftWaves.getNWaves());
					wis = null;
					powmaxs = null;
					powmaxDirs = null;
					pownorms = null;
					return (ret.get());
				}

				if (isShowLogs()) {
					int _w;
					logger.info("      Power");
					for (_w = 0; _w < nStats; _w++) {
						/* Add 1 to wis[w] to create index to original dft_coefs[] */
						logger.info("         wis[{}] {} {} {} {} {}", _w, wis.get(_w) + 1,
								powmaxs.get(wis.get(_w)), powmaxDirs.get(wis.get(_w)), pownorms.get(wis.get(_w)),
								powers.get(0)[powmaxDirs.get(wis.get(_w))]);
					}
				}

				/* Conduct primary direction test */
				blockDir = primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
				if (blockDir != ILfs.INVALID_DIR) {
					oDirectionMap.set(bi, blockDir);
				} else {
					/* Conduct secondary (fork) direction test */
					blockDir = secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
					if (blockDir != ILfs.INVALID_DIR) {
						oDirectionMap.set(bi, blockDir);
					}
					/* Otherwise current direction in Direction Map remains INVALID */
					else {
						/* Flag the block as having LOW RIDGE FLOW. */
						oLowFlowMap.set(bi, ILfs.TRUE);
					}
				}
			} // End DFT
		} // bi

		/* Deallocate working memory */
		getFree().freeDirPowers(powers, dftWaves.getNWaves());
		wis = null;
		powmaxs = null;
		powmaxDirs = null;
		pownorms = null;

		return ILfs.FALSE;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: interpolateDirectionMap - Take a Direction Map and Low Contrast #cat:
	 * Map and attempts to fill in INVALID directions in the #cat: Direction Map
	 * based on a blocks valid neighbors. The #cat: valid neighboring directions are
	 * combined in a weighted #cat: average inversely proportional to their distance
	 * from #cat: the block being interpolated. Low Contrast blocks are #cat: used
	 * to prempt the search for a valid neighbor in a #cat: specific direction,
	 * which keeps the process from #cat: interpolating directions for blocks in the
	 * background and #cat: and perimeter of the fingerprint in the image. Input:
	 * oDirectionMap - map of blocks containing directional ridge flow
	 * oLowContrastMap - map of blocks flagged as LOW CONTRAST mappedImageWidth -
	 * number of blocks horizontally in the maps mappedImageHeight - number of
	 * blocks vertically in the maps lfsParams - parameters and thresholds for
	 * controlling LFS Output: oDirectionMap - contains the newly interpolated
	 * results Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int interpolateDirectionMap(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int newDir;
		AtomicInteger northDir = new AtomicInteger(0);
		AtomicInteger eastDir = new AtomicInteger(0);
		AtomicInteger southDir = new AtomicInteger(0);
		AtomicInteger westDir = new AtomicInteger(0);
		int northDist = 0;
		int eastDist = 0;
		int southDist = 0;
		int westDist = 0;
		int totalDist;
		int northFound;
		int eastFound;
		int southFound;
		int westFound;
		int totalFound;
		int northDelta = 0;
		int eastDelta = 0;
		int southDelta = 0;
		int westDelta = 0;
		int totalDelta;
		AtomicInteger nbrX = new AtomicInteger(0);
		AtomicInteger nbrY = new AtomicInteger(0);
		AtomicIntegerArray oMap;
		int dptrIndex = 0;
		int cptrIndex = 0;
		int mptrIndex = 0;
		double avrDir;

		if (isShowLogs())
			logger.info("INTERPOLATE DIRECTION MAP STARTED");

		/* Allocate output (interpolated) Direction Map. */
		oMap = new AtomicIntegerArray(mappedImageWidth * mappedImageHeight);

		/* Set pointers to the first block in the maps. */
		dptrIndex = 0;
		cptrIndex = 0;
		mptrIndex = 0;

		/* Foreach block in the maps ... */
		for (int y = 0; y < mappedImageHeight; y++) {
			for (int x = 0; x < mappedImageWidth; x++) {
				/* If image block is NOT LOW CONTRAST and has INVALID direction ... */
				if ((oLowContrastMap.get(cptrIndex) == 0) && (oDirectionMap.get(dptrIndex) == ILfs.INVALID_DIR)) {
					logger.info("MAP INSIDE");
					/* Set neighbor accumulators to 0. */
					totalFound = 0;
					totalDist = 0;

					/* Find north neighbor. */
					if ((northFound = getBlock().findValidBlock(northDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 0, -1)) == ILfs.FOUND) {
						/* Compute north distance. */
						northDist = y - nbrY.get();
						/* Accumulate neighbor distance. */
						totalDist += northDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find east neighbor. */
					if ((eastFound = getBlock().findValidBlock(eastDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 1, 0)) == ILfs.FOUND) {
						/* Compute east distance. */
						eastDist = nbrX.get() - x;
						/* Accumulate neighbor distance. */
						totalDist += eastDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find south neighbor. */
					if ((southFound = getBlock().findValidBlock(southDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, 0, 1)) == ILfs.FOUND) {
						/* Compute south distance. */
						southDist = nbrY.get() - y;
						/* Accumulate neighbor distance. */
						totalDist += southDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* Find west neighbor. */
					if ((westFound = getBlock().findValidBlock(westDir, nbrX, nbrY, oDirectionMap, oLowContrastMap, x,
							y, mappedImageWidth, mappedImageHeight, -1, 0)) == ILfs.FOUND) {
						/* Compute west distance. */
						westDist = x - nbrX.get();
						/* Accumulate neighbor distance. */
						totalDist += westDist;
						/* Bump number of neighbors found. */
						totalFound++;
					}

					/* If a sufficient number of neighbors found (Ex. 2) ... */
					if (totalFound >= lfsParams.getMinInterpolateNbrs()) {
						/* Accumulate weighted sum of neighboring directions */
						/* inversely related to the distance from current block. */
						totalDelta = (int) 0.0;
						/* If neighbor found to the north ... */
						if (northFound != ILfs.FALSE) {
							northDelta = totalDist - northDist;
							totalDelta += northDelta;
						}
						/* If neighbor found to the east ... */
						if (eastFound != ILfs.FALSE) {
							eastDelta = totalDist - eastDist;
							totalDelta += eastDelta;
						}
						/* If neighbor found to the south ... */
						if (southFound != ILfs.FALSE) {
							southDelta = totalDist - southDist;
							totalDelta += southDelta;
						}
						/* If neighbor found to the west ... */
						if (westFound != ILfs.FALSE) {
							westDelta = totalDist - westDist;
							totalDelta += westDelta;
						}

						avrDir = 0.0;

						if (northFound != ILfs.FALSE) {
							avrDir += (northDir.get() * (northDelta / (double) totalDelta));
						}
						if (eastFound != ILfs.FALSE) {
							avrDir += (eastDir.get() * (eastDelta / (double) totalDelta));
						}
						if (southFound != ILfs.FALSE) {
							avrDir += (southDir.get() * (southDelta / (double) totalDelta));
						}
						if (westFound != ILfs.FALSE) {
							avrDir += (westDir.get() * (westDelta / (double) totalDelta));
						}

						/* Need to truncate precision so that answers are consistent */
						/* on different computer architectures when rounding doubles. */
						avrDir = getDefs().truncDoublePrecision(avrDir, ILfs.TRUNC_SCALE);

						/* Assign interpolated direction to output Direction Map. */
						newDir = getDefs().sRound(avrDir);

						if (isShowLogs())
							logger.info("Block {},{} INTERP numnbs={} newdir={}", x, y, totalFound, newDir);

						oMap.set(mptrIndex, newDir);
					} else {
						/* Otherwise, the direction remains INVALID. */
						oMap.set(mptrIndex, oDirectionMap.get(dptrIndex));
					}
				} else {
					/* Otherwise, assign the current direction to the output block. */
					oMap.set(mptrIndex, oDirectionMap.get(dptrIndex));
				}

				/* Bump to the next block in the maps ... */
				dptrIndex++;
				cptrIndex++;
				mptrIndex++;
			}
		}

		/* Copy the interpolated directions into the input map. */
		for (int mapIndex = 0; mapIndex < oMap.length(); mapIndex++)
			oDirectionMap.set(mapIndex, oMap.get(mapIndex));

		/* Deallocate the working memory. */
		getFree().free(oMap);

		if (isShowLogs())
			logger.info("INTERPOLATE DIRECTION MAP ENDED");
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: morphMapWithTF - Takes a 2D vector of TRUE and FALSE values integers
	 * #cat: and dialates and erodes the map in an attempt to fill #cat: in voids in
	 * the map. Input: tfMap - vector of integer block values lfsParams - parameters
	 * and thresholds for controlling LFS Output: tfMap - resulting morphed map
	 **************************************************************************/
	public int morphMapWithTF(AtomicIntegerArray tfMap, final LfsParams lfsParams) {
		int[] cimage;
		int[] mimage;
		int cptrIndex;
		int mptrIndex;
		int i;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();

		if (isShowLogs())
			logger.info("morphMapWithTF Started ({}, {})", mappedImageWidth, mappedImageHeight);
		/* Convert TRUE/FALSE map into a binary byte image. */
		int mSize = mappedImageWidth * mappedImageHeight;
		cimage = new int[mSize];
		mimage = new int[mSize];

		cptrIndex = 0;
		mptrIndex = 0;
		for (i = 0; i < mSize; i++) {
			cimage[cptrIndex++] = tfMap.get(mptrIndex++);
		}

		getMorph().dilateImage2(cimage, mimage, mappedImageWidth, mappedImageHeight);
		getMorph().dilateImage2(mimage, cimage, mappedImageWidth, mappedImageHeight);
		getMorph().erodeImage2(cimage, mimage, mappedImageWidth, mappedImageHeight);
		getMorph().erodeImage2(mimage, cimage, mappedImageWidth, mappedImageHeight);

		cptrIndex = 0;
		mptrIndex = 0;
		for (i = 0; i < mSize; i++) {
			tfMap.set(mptrIndex++, cimage[cptrIndex++]);
		}

		getFree().free(cimage);
		getFree().free(mimage);

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: pixelizeMap - Takes a block image map and assigns each pixel in the
	 * #cat: image its corresponding block value. This allows block #cat: values in
	 * maps to be directly accessed via pixel addresses. Input: oMap - points to the
	 * resulting pixelized map imageWidth - the width (in pixels) of the
	 * corresponding image imageHeight - the height (in pixels) of the corresponding
	 * image inputBlockImageMap - input block image map mapWidth - the width (in
	 * blocks) of the map mapHeight - the height (in blocks) of the map blockSize -
	 * the dimension (in pixels) of each block Output: ret - value Return Code: ret
	 * - Zero - successful completion - Negative - system error
	 **************************************************************************/
	public int pixelizeMap(AtomicIntegerArray oMap, int imageWidth, int imageHeight,
			AtomicIntegerArray inputBlockImageMap, final int mapWidth, final int mapHeight, final int blockSize) {
		AtomicInteger ret = new AtomicInteger(0);
		AtomicIntegerArray blockOffsets = null;
		AtomicInteger oBlockOffsetWidth = new AtomicInteger(0);
		AtomicInteger oBlockOffsetHeight = new AtomicInteger(0);
		int mapIndex;
		int blockOffsetsIndex = 0;
		int mapCurrentIndex = 0;

		// Assigned before only // pmap

		blockOffsets = getBlock().blockOffsets(ret, oBlockOffsetWidth, oBlockOffsetHeight, imageWidth, imageHeight, 0,
				blockSize);
		if (ret.get() != ILfs.FALSE) {
			oMap = null;
			return ret.get();
		}

		if ((oBlockOffsetWidth.get() != mapWidth) || (oBlockOffsetHeight.get() != mapHeight)) {
			logger.error("ERROR : pixelizeMap : block dimensions do not match");
			blockOffsets = null;
			oMap = null;
			ret.set(ILfs.ERROR_CODE_591);
			return ret.get();
		}

		for (mapIndex = 0; mapIndex < mapWidth * mapHeight; mapIndex++) {
			blockOffsetsIndex = 0 + blockOffsets.get(mapIndex);
			for (int y = 0; y < blockSize; y++) {
				mapCurrentIndex = blockOffsetsIndex;
				for (int x = 0; x < blockSize; x++) {
					oMap.set(mapCurrentIndex++, inputBlockImageMap.get(mapIndex));
				}
				blockOffsetsIndex += imageWidth;
			}
		}

		blockOffsets = null;
		/* Assign pixelized map to output pointer. */

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return ret.get();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: smoothDirectionMap - Takes a vector of integer directions and smooths
	 * #cat: them by analyzing the direction of adjacent neighbors. Input:
	 * oDirectionMap - vector of integer block values oLowContrastMap - vector of
	 * integer block values //mappedImageWidth - width (in blocks) of the map
	 * //mappedImageHeight - height (in blocks) of the map dirToRad - lookup table
	 * for converting integer directions lfsParams - parameters and thresholds for
	 * controlling LFS Output: oDirectionMap - vector of smoothed input values
	 **************************************************************************/
	public void smoothDirectionMap(AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap,
			final DirToRad dirToRad, final LfsParams lfsParams) {
		AtomicInteger oAverageDir = new AtomicInteger(0);
		AtomicInteger oValid = new AtomicInteger(0);
		AtomicReference<Double> oDirectionStrength = new AtomicReference<>();

		if (isShowLogs())
			logger.info("SMOOTH DIRECTION MAP");
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();
		/* Assign pointers to beginning of both maps. */
		int directionMapIndex = 0;
		int lowContrastMapIndex = 0;

		/* Foreach block in maps ... */
		for (int mappedYIndex = 0; mappedYIndex < mappedImageHeight; mappedYIndex++) {
			for (int mappedXIndex = 0; mappedXIndex < mappedImageWidth; mappedXIndex++) {
				/* If the current block does NOT have LOW CONTRAST ... */
				if (oLowContrastMap.get(lowContrastMapIndex) == ILfs.FALSE) {
					/* Compute average direction from neighbors, returning the */
					/* number of valid neighbors used in the computation, and */
					/* the "strength" of the average direction. */
					average8NbrDir(oAverageDir, oDirectionStrength, oValid, oDirectionMap, mappedXIndex, mappedYIndex,
							mappedImageWidth, mappedImageHeight, dirToRad);

					/* If average direction strength is strong enough */
					/* (Ex. thresh==0.2)... */
					if (oDirectionStrength.get() >= lfsParams.getDirStrengthMin()) {
						/* If Direction Map direction is valid ... */
						if (oDirectionMap.get(directionMapIndex) != ILfs.INVALID_DIR) {
							/* Conduct valid neighbor test (Ex. thresh==3)... */
							if (oValid.get() >= lfsParams.getRmvValidNbrMin()) {
								if (isShowLogs()) {
									logger.info("   SMOOTH DIRECTION BLOCK {} ({}, {})",
											mappedXIndex + (mappedYIndex * mappedImageWidth), mappedXIndex,
											mappedYIndex);
									logger.info("      Average NBR :   {} {} {}", oAverageDir.get(),
											oDirectionStrength.get(), oValid.get());
									logger.info("      1. Valid NBR ({} >= {})", oValid.get(),
											lfsParams.getRmvValidNbrMin());
									logger.info("      Valid Direction = {}", oDirectionMap.get(directionMapIndex));
									logger.info("      Smoothed Direction = {}", oAverageDir.get());
								}
								/* Reassign valid direction with average direction. */
								oDirectionMap.set(directionMapIndex, oAverageDir.get());
							}
						}
						/* Otherwise direction is invalid ... */
						else {
							/* Even if DIRECTION_MAP value is invalid, if number of */
							/* valid neighbors is big enough (Ex. thresh==7)... */
							if (oValid.get() >= lfsParams.getSmoothValidNbrMin()) {
								if (isShowLogs()) {
									logger.info("   SMOOTH DIRECTION BLOCK {} ({}, {})",
											mappedXIndex + (mappedYIndex * mappedImageWidth), mappedXIndex,
											mappedYIndex);
									logger.info("      Average NBR :   {} {} {}", oAverageDir.get(),
											oDirectionStrength.get(), oValid.get());
									logger.info("      2. Invalid NBR ({} >= {})", oValid.get(),
											lfsParams.getSmoothValidNbrMin());
									logger.info("      Invalid Direction = {}", oDirectionMap.get(directionMapIndex));
									logger.info("      Smoothed Direction = {}", oAverageDir.get());
								}
								/* Assign invalid direction with average direction. */
								oDirectionMap.set(directionMapIndex, oAverageDir.get());
							}
						}
					}
				}
				/* Otherwise, block has LOW CONTRAST, so keep INVALID direction. */
				/* Bump to next block in maps. */
				directionMapIndex++;
				lowContrastMapIndex++;
			}
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: generateHighCurveMap - Takes a Direction Map and generates a new map
	 * #cat: that flags blocks with HIGH CURVATURE. Input: oHighCurvatureMap -
	 * pointer to the High Curvature Map oDirectionMap - map of blocks containing
	 * directional ridge flow mappedImageWidth - the width (in blocks) of the map
	 * mappedImageHeight - the height (in blocks) of the map lfsParams - parameters
	 * and thresholds for controlling LFS Output: oHighCurvatureMap - points to the
	 * created High Curvature Map Return Code: Zero - successful completion Negative
	 * - system error
	 **************************************************************************/
	public int generateHighCurveMap(AtomicIntegerArray oHighCurvatureMap, AtomicIntegerArray oDirectionMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		AtomicInteger nvalid = new AtomicInteger(0);
		int curvatureMeasure = 0;
		int vorticityMeasure = 0;

		int highCurvatureMapIndex = 0;
		int directionMapIndex = 0;
		/* Foreach row in maps ... */
		for (int mappedYIndex = 0; mappedYIndex < mappedImageHeight; mappedYIndex++) {
			for (int mappedXIndex = 0; mappedXIndex < mappedImageWidth; mappedXIndex++) {
				/* Count number of valid neighbors around current block ... */
				nvalid.set(
						numValid8Nbrs(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth, mappedImageHeight));
				/* If valid neighbors exist ... */
				if (nvalid.get() > ILfs.FALSE) {
					/* If current block's direction is INVALID ... */
					if (oDirectionMap.get(directionMapIndex) == ILfs.INVALID_DIR) {
						/* If a sufficient number of VALID neighbors exists ... */
						if (nvalid.get() >= lfsParams.getVortValidNbrMin()) {
							/* Measure vorticity of neighbors. */
							vorticityMeasure = vorticity(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth,
									mappedImageHeight, lfsParams.getNumDirections());
							/* If vorticity is sufficiently high ... */
							if (vorticityMeasure >= lfsParams.getHighcurvVorticityMin()) {
								/* Flag block as HIGH CURVATURE. */
								oHighCurvatureMap.set(highCurvatureMapIndex, ILfs.TRUE);
							}
						}
					}
					/* Otherwise block has valid direction ... */
					else {
						/* Measure curvature around the valid block. */
						curvatureMeasure = curvature(oDirectionMap, mappedXIndex, mappedYIndex, mappedImageWidth,
								mappedImageHeight, lfsParams.getNumDirections());
						/* If curvature is sufficiently high ... */
						if (curvatureMeasure >= lfsParams.getHighcurvCurvatureMin()) {
							oHighCurvatureMap.set(highCurvatureMapIndex, ILfs.TRUE);
						}
					}
				}
				/* Else (nvalid <= 0) */
				/* Bump pointers to next block in maps. */
				directionMapIndex++;
				highCurvatureMapIndex++;
			}
		}
		/* Return normally. */
		return ILfs.FALSE;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: generateInputBlockImageMap - Computes an IMAP, which is a 2D vector of
	 * integer directions, #cat: where each direction represents the dominant ridge
	 * flow in #cat: a block of the input grayscale image. This routine will #cat:
	 * generate an IMAP for arbitrarily sized, non-square, images. Input:
	 * paddedImagedata - padded input image data (8 bits [0..256) grayscale)
	 * paddedImageWidth - padded width (in pixels) of the input image
	 * paddedImageHeight - padded height (in pixels) of the input image dirToRad -
	 * lookup table for converting integer directions dftWaves - structure
	 * containing the DFT wave forms dftGrids - structure containing the rotated
	 * pixel grid offsets lfsParams - parameters and thresholds for controlling LFS
	 * Output: ret - Zero - successful completion - Negative - system error
	 * oMappedImageWidth - width (in blocks) of the IMAP oMappedImageHeight - height
	 * (in blocks) of the IMAP Return Code: optr - points to the created IMAP
	 **************************************************************************/
	public AtomicIntegerArray generateInputBlockImageMap(AtomicInteger ret, AtomicInteger oMappedImageWidth,
			AtomicInteger oMappedImageHeight, int[] paddedImagedata, final int paddedImageWidth,
			final int paddedImageHeight, final DirToRad dirToRad, final DftWaves dftWaves, final RotGrids dftGrids,
			final LfsParams lfsParams) {
		AtomicIntegerArray oInputBlockImageMap = null;
		AtomicInteger mappedImageWidth = new AtomicInteger(0);
		AtomicInteger mappedImageHeight = new AtomicInteger(0);
		int imageWidth;
		int imageHeight;
		AtomicIntegerArray blockOffsets;

		/* 1. Compute block offsets for the entire image, accounting for pad */
		/* Block_offsets() assumes square block (grid), so ERROR otherwise. */
		if (dftGrids.getGridWidth() != dftGrids.getGridHeight()) {
			logger.error("ERROR : generateInputBlockImageMap : DFT grids must be square");
			ret.set(ILfs.ERROR_CODE_60);
			return oInputBlockImageMap;
		}
		/* Compute unpadded image dimensions. */
		imageWidth = paddedImageWidth - (dftGrids.getPad() << 1);
		imageHeight = paddedImageHeight - (dftGrids.getPad() << 1);

		blockOffsets = getBlock().blockOffsets(ret, mappedImageWidth, mappedImageHeight, imageWidth, imageHeight,
				dftGrids.getPad(), dftGrids.getGridWidth());
		if (ret.get() != ILfs.FALSE) {
			return oInputBlockImageMap;
		}

		/* 2. initial imap */
		oInputBlockImageMap = initialiseInputBlockImageMap(ret, blockOffsets, mappedImageWidth, mappedImageHeight,
				paddedImagedata, paddedImageWidth, paddedImageHeight, dftWaves, dftGrids, lfsParams);
		if (ret.get() != ILfs.FALSE) {
			/* Free memory allocated to this point. */
			blockOffsets = null;
			return oInputBlockImageMap;
		}

		/* 3. Remove IMAP directions that are inconsistent with neighbors */
		removeInconsistentDirs(oInputBlockImageMap, dirToRad, lfsParams);

		/* 4. Smooth imap values with their neighbors */
		smoothInputBlockImageMap(oInputBlockImageMap, dirToRad, lfsParams);

		/* Deallocate working memory. */
		blockOffsets = null;

		oMappedImageWidth.set(mappedImageWidth.get());
		oMappedImageHeight.set(mappedImageHeight.get());
		ret.set(ILfs.FALSE);
		return oInputBlockImageMap;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: initialiseInputBlockImageMap - Creates an initial IMAP from the given
	 * input image. #cat: It very important that the image be properly padded so
	 * #cat: that rotated grids along the boudary of the image do not #cat: access
	 * unkown memory. The rotated grids are used by a #cat: DFT-based analysis to
	 * determine the integer directions #cat: in the IMAP. Typically this initial
	 * vector of directions will #cat: subsequently have weak or inconsistent
	 * directions removed #cat: followed by a smoothing process. Input: ret - return
	 * Code blockOffsets - offsets to the pixel origin of each block in the padded
	 * image mappedImageWidth - number of blocks horizontally in the padded input
	 * image mappedImageHeight - number of blocks vertically in the padded input
	 * image paddedImagedata - padded input image data (8 bits [0..256) grayscale)
	 * paddedImageWidth - width (in pixels) of the padded input image
	 * paddedImageHeight - height (in pixels) of the padded input image dftWaves -
	 * structure containing the DFT wave forms dftGrids - structure containing the
	 * rotated pixel grid offsets lfsParams - parameters and thresholds for
	 * controlling LFS Output: ret - Zero - successful completion - Negative -
	 * system error Return Code: inputBlockImageMap - points to the newly created
	 * IMAP
	 **************************************************************************/
	@SuppressWarnings("unused")
	public AtomicIntegerArray initialiseInputBlockImageMap(AtomicInteger ret, AtomicIntegerArray blockOffsets,
			final AtomicInteger mappedImageWidth, final AtomicInteger mappedImageHeight, int[] paddedImagedata,
			final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves, final RotGrids dftGrids,
			final LfsParams lfsParams) {
		AtomicIntegerArray inputBlockImageMap = null;
		int bSize;
		int blockDir;
		AtomicIntegerArray wis;
		AtomicIntegerArray powmaxDirs;
		AtomicReferenceArray<Double[]> powers;
		AtomicReferenceArray<Double> powmaxs = null;
		AtomicReferenceArray<Double> pownorms = null;
		int nStats;

		if (isShowLogs())
			logger.info("INITIAL MAP");
		/* Compute total number of blocks in IMAP */
		bSize = mappedImageWidth.get() * mappedImageHeight.get();
		inputBlockImageMap = new AtomicIntegerArray(bSize);
		if (inputBlockImageMap == null) {
			logger.error("ERROR : initialiseInputBlockImageMap : imap : NULL");
			ret.set(ILfs.ERROR_CODE_70);
			return inputBlockImageMap;
		}

		/* Allocate DFT directional power vectors */
		powers = getInit().allocDirPowers(ret, dftWaves.getNWaves(), dftGrids.getNoOfGrids());
		if (ret.get() != ILfs.FALSE) {
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		/* Allocate DFT power statistic arrays */
		/* Compute length of statistics arrays. Statistics not needed */
		/* for the first DFT wave, so the length is number of waves - 1. */
		nStats = dftWaves.getNWaves() - 1;
		wis = getInit().allocPowerStatsWis(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		powmaxs = getInit().allocPowerStatsPowmaxs(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		powmaxDirs = getInit().allocPowerStatsPowmaxDirs(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().free(powmaxs);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		pownorms = getInit().allocPowerStatsPownorms(ret, nStats);
		if (ret.get() != ILfs.FALSE) {
			getFree().free(inputBlockImageMap);
			getFree().free(wis);
			getFree().free(powmaxs);
			getFree().free(powmaxDirs);
			getFree().freeDirPowers(powers, dftWaves.getNWaves());
			inputBlockImageMap = null;
			return inputBlockImageMap;
		}

		/* Foreach block in imap ... */
		for (int blockOffsetIndex = 0; blockOffsetIndex < bSize; blockOffsetIndex++) {
			/* Compute DFT powers */
			ret.set(getDft().dftDirPowers(powers, paddedImagedata, blockOffsets.get(blockOffsetIndex), paddedImageWidth,
					paddedImageHeight, dftWaves, dftGrids));
			if (ret.get() != ILfs.FALSE) {
				getFree().free(inputBlockImageMap);
				getFree().freeDirPowers(powers, dftWaves.getNWaves());
				getFree().free(wis);
				getFree().free(powmaxs);
				getFree().free(powmaxDirs);
				getFree().free(pownorms);
				inputBlockImageMap = null;
				return inputBlockImageMap;
			}

			/* Compute DFT power statistics, skipping first applied DFT */
			/* wave. This is dependent on how the primary and secondary */
			/* direction tests work below. */
			ret.set(getDft().getDftPowerStats(wis, powmaxs, powmaxDirs, pownorms, powers, 1, dftWaves.getNWaves(),
					dftGrids.getNoOfGrids()));
			if (ret.get() != ILfs.FALSE) {
				getFree().free(inputBlockImageMap);
				getFree().freeDirPowers(powers, dftWaves.getNWaves());
				getFree().free(wis);
				getFree().free(powmaxs);
				getFree().free(powmaxDirs);
				getFree().free(pownorms);
				inputBlockImageMap = null;
				return inputBlockImageMap;
			}

			/* Conduct primary direction test */
			blockDir = primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
			if (blockDir != ILfs.INVALID_DIR) {
				inputBlockImageMap.set(blockOffsetIndex, blockDir);
			} else {
				/* Conduct secondary (fork) direction test */
				blockDir = secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, nStats, lfsParams);
				if (blockDir != ILfs.INVALID_DIR) {
					inputBlockImageMap.set(blockOffsetIndex, blockDir);
				}
			}
			/* Otherwise current block direction in IMAP remains INVALID */
		} // bi

		/* Deallocate working memory */
		getFree().freeDirPowers(powers, dftWaves.getNWaves());
		getFree().free(wis);
		getFree().free(powmaxs);
		getFree().free(powmaxDirs);
		getFree().free(pownorms);

		ret.set(ILfs.FALSE);
		return inputBlockImageMap;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: primaryDirectionTest - Applies the primary set of criteria for
	 * selecting #cat: an IMAP integer direction from a set of DFT results #cat:
	 * computed from a block of image data Input: powers - DFT power computed from
	 * each (N) wave frequencies at each rotation direction in the current image
	 * block wis - sorted order of the highest N-1 frequency power statistics
	 * powmaxs - maximum power for each of the highest N-1 frequencies powmaxDirs -
	 * directions associated with each of the N-1 maximum powers pownorms -
	 * normalized power for each of the highest N-1 frequencies nStats - N-1 wave
	 * frequencies (where N is the length of dft_coefs) lfsParams - parameters and
	 * thresholds for controlling LFS Return Code: Zero or Positive - The selected
	 * IMAP integer direction INVALID_DIR - IMAP Integer direction could not be
	 * determined
	 **************************************************************************/
	public int primaryDirectionTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
			final AtomicReferenceArray<Double> powmaxs, final AtomicIntegerArray powmaxDirs,
			final AtomicReferenceArray<Double> pownorms, final int nStats, final LfsParams lfsParams) {
		if (isShowLogs())
			logger.info("      Primary");

		/* Look at max power statistics in decreasing order ... */
		for (int statIndex = 0; statIndex < nStats; statIndex++) {
			/* 1. Test magnitude of current max power (Ex. Thresh==100000) */
			if ((powmaxs.get(wis.get(statIndex)) > lfsParams.getPowmaxMin()) &&
			/* 2. Test magnitude of normalized max power (Ex. Thresh==3.8) */
					(pownorms.get(wis.get(statIndex)) > lfsParams.getPownormMin()) &&
					/* 3. Test magnitude of power of lowest DFT frequency at current */
					/* max power direction and make sure it is not too big. */
					/* (Ex. Thresh==50000000) */
					(powers.get(0)[powmaxDirs.get(wis.get(statIndex))] <= lfsParams.getPowmaxMax())) {
				/* Add 1 to wis[w] to create index to original dft_coefs[] */
				if (isShowLogs()) {
					logger.info("         Selected Wave = {}", (wis.get(statIndex) + 1));
					logger.info("         1. Power Magnitude ({} > {})", powmaxs.get(wis.get(statIndex)),
							lfsParams.getPowmaxMin());
					logger.info("         2. Norm Power Magnitude ({} > {})", pownorms.get(wis.get(statIndex)),
							lfsParams.getPownormMin());
					logger.info("         3. Low Freq Wave Magnitude ({} <= {})",
							powers.get(0)[powmaxDirs.get(wis.get(statIndex))], lfsParams.getPowmaxMax());
					logger.info("         PASSED");
					logger.info("         Selected Direction = {}", powmaxDirs.get(wis.get(statIndex)));
				}
				/* If ALL 3 criteria met, return current max power direction. */
				return (powmaxDirs.get(wis.get(statIndex)));
			}
		}

		/* Otherwise test failed. */
		if (isShowLogs()) {
			logger.info("         1. Power Magnitude ( > {})", lfsParams.getPowmaxMin());
			logger.info("         2. Norm Power Magnitude ( > {})", lfsParams.getPownormMin());
			logger.info("         3. Low Freq Wave Magnitude ( <= {})", lfsParams.getPowmaxMax());
			logger.info("         FAILED");
		}
		return ILfs.INVALID_DIR;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: secondaryForkTest - Applies a secondary set of criteria for selecting
	 * #cat: an IMAP integer direction from a set of DFT results #cat: computed from
	 * a block of image data. This test #cat: analyzes the strongest power
	 * statistics associated #cat: with a given frequency and direction and analyses
	 * #cat: small changes in direction to the left and right to #cat: determine if
	 * the block contains a "fork". Input: powers - DFT power computed from each (N)
	 * wave frequencies at each rotation direction in the current image block wis -
	 * sorted order of the highest N-1 frequency power statistics powmaxs - maximum
	 * power for each of the highest N-1 frequencies powmaxDirs - directions
	 * associated with each of the N-1 maximum powers pownorms - normalized power
	 * for each of the highest N-1 frequencies nStats - N-1 wave frequencies (where
	 * N is the length of dft_coefs) lfsParams - parameters and thresholds for
	 * controlling LFS Return Code: Zero or Positive - The selected IMAP integer
	 * direction INVALID_DIR - IMAP Integer direction could not be determined
	 **************************************************************************/
	public int secondaryForkTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
			final AtomicReferenceArray<Double> powmaxs, final AtomicIntegerArray powmaxDirs,
			final AtomicReferenceArray<Double> pownorms, final int nStats, final LfsParams lfsParams) {
		int leftDir;
		int rightDir;
		double forkPownormMin;
		double forkPowThresh;

		int firstpart = 0; /* Flag to determine if passed 1st part ... */
		if (isShowLogs())
			logger.info("      Secondary");

		/* Relax the normalized power threshold under fork conditions. */
		forkPownormMin = lfsParams.getForkPctPownorm() * lfsParams.getPownormMin();

		/* 1. Test magnitude of largest max power (Ex. Thresh==100000) */
		if ((powmaxs.get(wis.get(0)) > lfsParams.getPowmaxMin()) &&
		/* 2. Test magnitude of corresponding normalized power */
		/* (Ex. Thresh==2.85) */
				(pownorms.get(wis.get(0)) >= forkPownormMin) &&
				/* 3. Test magnitude of power of lowest DFT frequency at largest */
				/* max power direction and make sure it is not too big. */
				/* (Ex. Thresh==50000000) */
				(powers.get(0)[powmaxDirs.get(wis.get(0))] <= lfsParams.getPowmaxMax())) {
			/* First part passed ... */
			firstpart = 1;
			if (isShowLogs()) {
				logger.info("         Selected Wave = {}", (wis.get(0) + 1));
				logger.info("         1. Power Magnitude ({} > {})", powmaxs.get(wis.get(0)), lfsParams.getPowmaxMin());
				logger.info("         2. Norm Power Magnitude ({} >= {})", pownorms.get(wis.get(0)), forkPownormMin);
				logger.info("         3. Low Freq Wave Magnitude ({} <= {})", powers.get(0)[powmaxDirs.get(wis.get(0))],
						lfsParams.getPowmaxMax());
			}

			/* Add FORK_INTERVALs to current direction modulo NDIRS */
			rightDir = (powmaxDirs.get(wis.get(0)) + lfsParams.getForkInterval()) % lfsParams.getNumDirections();

			/* Subtract FORK_INTERVALs from direction modulo NDIRS */
			/* For example, FORK_INTERVAL==2 & NDIRS==16, then */
			/* ldir = (dir - (16-2)) % 16 */
			/* which keeps result in proper modulo range. */
			leftDir = (powmaxDirs.get(wis.get(0)) + lfsParams.getNumDirections() - lfsParams.getForkInterval())
					% lfsParams.getNumDirections();

			// logger.info((" Left = {}, Current = {}, Right = {}" + ldir +
			// "----" + powmax_dirs.get(wis.get(0)) + "----" + rdir)));

			/* Set forked angle threshold to be a % of the max directional */
			/* power. (Ex. thresh==0.7*powmax) */
			forkPowThresh = powmaxs.get(wis.get(0)) * lfsParams.getForkPctPowmax();

			/* Look up and test the computed power for the left and right */
			/* fork directions.s */
			/* The power stats (and thus wis) are on the range [0..nwaves-1) */
			/* as the statistics for the first DFT wave are not included. */
			/* The original power vectors exist for ALL DFT waves, therefore */
			/* wis indices must be added by 1 before addressing the original */
			/* powers vector. */
			/* LFS permits one and only one of the fork angles to exceed */
			/* the relative power threshold. */
			if (((powers.get(wis.get(0) + 1)[leftDir] <= forkPowThresh)
					|| (powers.get(wis.get(0) + 1)[rightDir] <= forkPowThresh))
					&& ((powers.get(wis.get(0) + 1)[leftDir] > forkPowThresh)
							|| (powers.get(wis.get(0) + 1)[rightDir] > forkPowThresh))) {
				if (isShowLogs()) {
					logger.info("         4. Left Power Magnitude ({} > {})", powers.get(wis.get(0) + 1)[leftDir],
							forkPowThresh);
					logger.info("         5. Right Power Magnitude ({} > {})", powers.get(wis.get(0) + 1)[rightDir],
							forkPowThresh);
					logger.info("         PASSED");
					logger.info("         Selected Direction = {}", powmaxDirs.get(wis.get(0)));
				}
				/* If ALL the above criteria hold, then return the direction */
				/* of the largest max power. */
				return powmaxDirs.get(wis.get(0));
			}
		}

		/* Otherwise test failed. */
		return ILfs.INVALID_DIR;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: removeInconsistentDirs - Takes a vector of integer directions and
	 * removes #cat: individual directions that are too weak or inconsistent. #cat:
	 * Directions are tested from the center of the IMAP working #cat: outward in
	 * concentric squares, and the process resets to #cat: the center and continues
	 * until no changes take place during #cat: a complete pass. Input:
	 * oInputBlockImageMap - vector of IMAP integer directions //mappedImageWidth -
	 * width (in blocks) of the IMAP //mappedImageHeight - height (in blocks) of the
	 * IMAP dirToRad - lookup table for converting integer directions lfsParams -
	 * parameters and thresholds for controlling LFS Output: imap - vector of pruned
	 * input values
	 **************************************************************************/
	public void removeInconsistentDirs(AtomicIntegerArray oInputBlockImageMap, final DirToRad dirToRad,
			final LfsParams lfsParams) {
		int mappedImageXIndex;
		int mappedImageYIndex;
		int nInputBlockImageMapIndex;
		int nRemoved = -1;
		int leftBoxIndex;
		int rightBoxIndex;
		int topBoxIndex;
		int bottomBoxIndex;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();

		int numPass = 0;
		if (isShowLogs())
			logger.info("REMOVE MAP");
		/* Compute center coords of IMAP */
		mappedImageXIndex = mappedImageWidth >> 1;
		mappedImageYIndex = mappedImageHeight >> 1;

		/* Do pass, while directions have been removed in a pass ... */
		do {
			/* Count number of complete passes through IMAP */
			++numPass;
			if (isShowLogs())
				logger.info("REMOVE MAP PASS = {}, {}, {}", numPass, oInputBlockImageMap.length(), nRemoved);
			/* Reinitialize number of removed directions to 0 */
			nRemoved = 0;

			/* Start at center */
			nInputBlockImageMapIndex = 0 + (mappedImageYIndex * mappedImageWidth) + mappedImageXIndex;

			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(nInputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex, mappedImageWidth,
							mappedImageHeight, dirToRad, lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(nInputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}

			/* Initialize side indices of concentric boxes */
			leftBoxIndex = mappedImageXIndex - 1;
			topBoxIndex = mappedImageYIndex - 1;
			rightBoxIndex = mappedImageXIndex + 1;
			bottomBoxIndex = mappedImageYIndex + 1;

			/* Grow concentric boxes, until ALL edges of imap are exceeded */
			while ((leftBoxIndex >= 0) || (rightBoxIndex < mappedImageWidth) || (topBoxIndex >= 0)
					|| (bottomBoxIndex < mappedImageHeight)) {
				/* test top edge of box */
				if (topBoxIndex >= 0) {
					nRemoved += testTopEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test right edge of box */
				if (rightBoxIndex < mappedImageWidth) {
					nRemoved += testRightEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test bottom edge of box */
				if (bottomBoxIndex < mappedImageHeight) {
					nRemoved += testBottomEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* test left edge of box */
				if (leftBoxIndex >= 0) {
					nRemoved += testLeftEdge(leftBoxIndex, topBoxIndex, rightBoxIndex, bottomBoxIndex,
							oInputBlockImageMap, mappedImageWidth, mappedImageHeight, dirToRad, lfsParams);
				}

				/* Resize current box */
				leftBoxIndex--;
				topBoxIndex--;
				rightBoxIndex++;
				bottomBoxIndex++;
			}

		} while (nRemoved != ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: testTopEdge - Walks the top edge of a concentric square in the IMAP,
	 * #cat: testing directions along the way to see if they should #cat: be removed
	 * due to being too weak or inconsistent with #cat: respect to their adjacent
	 * neighbors. Input: leftBoxIndex - left edge of current concentric square
	 * topBoxIndex - top edge of current concentric square rightBoxIndex - right
	 * edge of current concentric square bottomBoxIndex - bottom edge of current
	 * concentric square oInputBlockImageMap - vector of IMAP integer directions
	 * mappedImageWidth - width (in blocks) of the IMAP mappedImageHeight - height
	 * (in blocks) of the IMAP dirToRad - lookup table for converting integer
	 * directions lfsParams - parameters and thresholds for controlling LFS Return
	 * Code: Positive - direction should be removed from IMAP Zero - direction
	 * should NOT be remove from IMAP
	 **************************************************************************/
	public int testTopEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sx, ex;
		int inputBlockImageMapIndex, inputBlockImageMapCurrentIndex, inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to top-leftmost point of box, or set it to */
		/* the leftmost point in the IMAP row (0), whichever is larger. */
		sx = Math.max(leftBoxIndex, 0);
		inputBlockImageMapCurrentIndex = 0 + (topBoxIndex * mappedImageWidth) + sx;

		/* Set end pointer to either 1 point short of the top-rightmost */
		/* point of box, or set it to the rightmost point in the IMAP */
		/* row (lastx=mappedImageWidth-1), whichever is smaller. */
		ex = Math.min(rightBoxIndex - 1, mappedImageWidth - 1);
		inputBlockImageMapEdgeIndex = 0 + (topBoxIndex * mappedImageWidth) + ex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = sx, by = topBoxIndex; inputBlockImageMapIndex <= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex++, bx++) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: testRightEdge - Walks the right edge of a concentric square in the
	 * #cat: IMAP, testing directions along the way to see if they #cat: should be
	 * removed due to being too weak or inconsistent #cat: with respect to their
	 * adjacent neighbors. Input: leftBoxIndex - left edge of current concentric
	 * square topBoxIndex - top edge of current concentric square rightBoxIndex -
	 * right edge of current concentric square bottomBoxIndex - bottom edge of
	 * current concentric square oInputBlockImageMap - vector of IMAP integer
	 * directions mappedImageWidth - width (in blocks) of the IMAP mappedImageHeight
	 * - height (in blocks) of the IMAP dirToRad - lookup table for converting
	 * integer directions lfsParams - parameters and thresholds for controlling LFS
	 * Return Code: Positive - direction should be removed from IMAP Zero -
	 * direction should NOT be remove from IMAP
	 **************************************************************************/
	public int testRightEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sy, ey;
		int inputBlockImageMapIndex, inputBlockImageMapCurrentIndex, inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to top-rightmost point of box, or set it to */
		/* the topmost point in IMAP column (0), whichever is larger. */
		sy = Math.max(topBoxIndex, 0);
		inputBlockImageMapCurrentIndex = 0 + (sy * mappedImageWidth) + rightBoxIndex;

		/* Set end pointer to either 1 point short of the bottom- */
		/* rightmost point of box, or set it to the bottommost point */
		/* in the IMAP column (lasty=mappedImageHeight-1), whichever is smaller. */
		ey = Math.min(bottomBoxIndex - 1, mappedImageHeight - 1);
		inputBlockImageMapEdgeIndex = 0 + (ey * mappedImageWidth) + rightBoxIndex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = rightBoxIndex, by = sy; inputBlockImageMapIndex <= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex += mappedImageWidth, by++) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: testBottomEdge - Walks the bottom edge of a concentric square in the
	 * #cat: IMAP, testing directions along the way to see if they #cat: should be
	 * removed due to being too weak or inconsistent #cat: with respect to their
	 * adjacent neighbors. Input: leftBoxIndex - left edge of current concentric
	 * square topBoxIndex - top edge of current concentric square rightBoxIndex -
	 * right edge of current concentric square bottomBoxIndex - bottom edge of
	 * current concentric square oInputBlockImageMap - vector of IMAP integer
	 * directions mappedImageWidth - width (in blocks) of the IMAP mappedImageHeight
	 * - height (in blocks) of the IMAP dirToRad - lookup table for converting
	 * integer directions lfsParams - parameters and thresholds for controlling LFS
	 * Return Code: Positive - direction should be removed from IMAP Zero -
	 * direction should NOT be remove from IMAP
	 **************************************************************************/
	public int testBottomEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sx, ex;
		int inputBlockImageMapIndex;
		int inputBlockImageMapCurrentIndex;
		int inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to bottom-rightmost point of box, or set it to the */
		/*
		 * rightmost point in the IMAP ROW (lastx=mappedImageWidth-1), whichever is
		 * smaller.
		 */
		sx = Math.min(rightBoxIndex, mappedImageWidth - 1);
		inputBlockImageMapCurrentIndex = 0 + (bottomBoxIndex * mappedImageWidth) + sx;

		/* Set end pointer to either 1 point short of the bottom- */
		/* lefttmost point of box, or set it to the leftmost point */
		/* in the IMAP row (x=0), whichever is larger. */
		ex = Math.max(leftBoxIndex - 1, 0);
		inputBlockImageMapEdgeIndex = 0 + (bottomBoxIndex * mappedImageWidth) + ex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = sx, by = bottomBoxIndex; inputBlockImageMapIndex >= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex--, bx--) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: testLeftEdge - Walks the left edge of a concentric square in the IMAP,
	 * #cat: testing directions along the way to see if they should #cat: be removed
	 * due to being too weak or inconsistent with #cat: respect to their adjacent
	 * neighbors. Input: leftBoxIndex - left edge of current concentric square
	 * topBoxIndex - top edge of current concentric square rightBoxIndex - right
	 * edge of current concentric square bottomBoxIndex - bottom edge of current
	 * concentric square oInputBlockImageMap - vector of IMAP integer directions
	 * mappedImageWidth - width (in blocks) of the IMAP mappedImageHeight - height
	 * (in blocks) of the IMAP dirToRad - lookup table for converting integer
	 * directions lfsParams - parameters and thresholds for controlling LFS Return
	 * Code: Positive - direction should be removed from IMAP Zero - direction
	 * should NOT be remove from IMAP
	 **************************************************************************/
	public int testLeftEdge(final int leftBoxIndex, final int topBoxIndex, final int rightBoxIndex,
			final int bottomBoxIndex, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, DirToRad dirToRad, LfsParams lfsParams) {
		int bx, by, sy, ey;
		int inputBlockImageMapIndex;
		int inputBlockImageMapCurrentIndex;
		int inputBlockImageMapEdgeIndex;
		int nRemoved;

		/* Initialize number of directions removed on edge to 0 */
		nRemoved = 0;

		/* Set start pointer to bottom-leftmost point of box, or set it to */
		/* the bottommost point in IMAP column (lasty=mappedImageHeight-1), whichever */
		/* is smaller. */
		sy = Math.min(bottomBoxIndex, mappedImageHeight - 1);
		inputBlockImageMapCurrentIndex = 0 + (sy * mappedImageWidth) + leftBoxIndex;

		/* Set end pointer to either 1 point short of the top-leftmost */
		/* point of box, or set it to the topmost point in the IMAP */
		/* column (y=0), whichever is larger. */
		ey = Math.max(topBoxIndex - 1, 0);
		inputBlockImageMapEdgeIndex = 0 + (ey * mappedImageWidth) + leftBoxIndex;

		/* For each point on box's edge ... */
		for (inputBlockImageMapIndex = inputBlockImageMapCurrentIndex, bx = leftBoxIndex, by = sy; inputBlockImageMapIndex >= inputBlockImageMapEdgeIndex; inputBlockImageMapIndex -= mappedImageWidth, by--) {
			/* If valid IMAP direction and test for removal is true ... */
			if ((oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR)
					&& (removeIMAPDirection(oInputBlockImageMap, bx, by, mappedImageWidth, mappedImageHeight, dirToRad,
							lfsParams) >= ILfs.TRUE)) {
				/* Set to INVALID */
				oInputBlockImageMap.set(inputBlockImageMapIndex, ILfs.INVALID_DIR);
				/* Bump number of removed IMAP directions */
				nRemoved++;
			}
		}

		/* Return the number of directions removed on edge */
		return (nRemoved);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: removeIMAPDirection - Determines if an IMAP direction should be removed
	 * based #cat: on analyzing its adjacent neighbors Input: oInputBlockImageMap -
	 * vector of IMAP integer directions mappedImageXIndex - IMAP X-coord of the
	 * current direction being tested mappedImageYIndex - IMPA Y-coord of the
	 * current direction being tested mappedImageWidth - width (in blocks) of the
	 * IMAP mappedImageHeight - height (in blocks) of the IMAP dirToRad - lookup
	 * table for converting integer directions lfsParams - parameters and thresholds
	 * for controlling LFS Return Code: Positive - direction should be removed from
	 * IMAP Zero - direction should NOT be remove from IMAP
	 **************************************************************************/
	@SuppressWarnings({ "java:S2629" })
	public int removeIMAPDirection(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight,
			final DirToRad dirToRad, final LfsParams lfsParams) {
		AtomicInteger oAverageDirection = new AtomicInteger(), oValid = new AtomicInteger();
		int nDistance = 0;

		AtomicReference<Double> dirStrength = new AtomicReference<Double>(0.0);
		/* Compute average direction from neighbors, returning the */
		/* number of valid neighbors used in the computation, and */
		/* the "strength" of the average direction. */
		average8NbrDir(oAverageDirection, dirStrength, oValid, oInputBlockImageMap, mappedImageXIndex,
				mappedImageYIndex, mappedImageWidth, mappedImageHeight, dirToRad);
		/* Conduct valid neighbor test (Ex. thresh==3) */
		if (oValid.get() < lfsParams.getRmvValidNbrMin()) {
			if (isShowLogs()) {
				logger.info("      BLOCK {} ({}, {})", mappedImageXIndex + (mappedImageYIndex * mappedImageWidth),
						mappedImageXIndex, mappedImageYIndex);
				logger.info("         Average NBR :   {} {} {}", oAverageDirection.get(), dirStrength.get(),
						oValid.get());
				logger.info("         1. Valid NBR ({} < {})", oValid.get(), lfsParams.getRmvValidNbrMin());
			}
			return (ILfs.TRUE);
		}

		/* If strength of average neighbor direction is large enough to */
		/* put credence in ... (Ex. threshold==0.2) */
		if (dirStrength.get() >= lfsParams.getDirStrengthMin()) {
			/* Conduct direction distance test (Ex. thresh==3) */
			/* Compute minimum absolute distance between current and */
			/* average directions accounting for wrapping from 0 to NDIRS. */
			nDistance = Math.abs(oAverageDirection.get()
					- (oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex)));
			nDistance = Math.min(nDistance, dirToRad.getNDirs() - nDistance);
			if (nDistance > lfsParams.getDirDistanceMax()) {
				if (isShowLogs()) {
					logger.info("      BLOCK {} ({}, {})", mappedImageXIndex + (mappedImageYIndex * mappedImageWidth),
							mappedImageXIndex, mappedImageYIndex);
					logger.info("         Average NBR :   {} {} {}", oAverageDirection.get(), dirStrength.get(),
							oValid.get());
					logger.info("         1. Valid NBR ({} < {})", oValid.get(), lfsParams.getRmvValidNbrMin());
					logger.info("         2. Direction Strength ({} >= {})", dirStrength.get(),
							lfsParams.getDirStrengthMin());
					logger.info("         Current Dir =  {}, Average Dir = {}",
							oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex),
							oAverageDirection.get());
					logger.info("         3. Direction Distance ({} > {})", nDistance, lfsParams.getDirDistanceMax());
				}
				return (2);
			}
		}

		/* Otherwise, the strength of the average direciton is not strong enough */
		/* to put credence in, so leave the current block's directon alone. */

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: average8NbrDir - Given an IMAP direction, computes an average #cat:
	 * direction from its adjacent 8 neighbors returning #cat: the average
	 * direction, its strength, and the #cat: number of valid direction in the
	 * neighborhood. Input: oInputBlockImageMap - vector of IMAP integer directions
	 * mapXIndex - IMAP X-coord of the current direction my - IMPA Y-coord of the
	 * current direction mappedImageWidth - width (in blocks) of the IMAP
	 * mappedImageHeight - height (in blocks) of the IMAP dirToRad - lookup table
	 * for converting integer directions Output: oAverageDir - the average direction
	 * computed from neighbors oDirStrength - the strength of the average direction
	 * oValid - the number of valid directions used to compute the average
	 **************************************************************************/
	public void average8NbrDir(AtomicInteger oAverageDir, AtomicReference<Double> oDirStrength, AtomicInteger oValid,
			AtomicIntegerArray oInputBlockImageMap, final int mapXIndex, final int mapYIndex,
			final int mappedImageWidth, final int mappedImageHeight, final DirToRad dirToRad) {
		int inputBlockImageMapIndex;
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		double cospart;
		double sinpart;
		double pi2;
		double pifactor;
		double theta;
		double avr;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mapXIndex + 1; // East
		westIndex = mapXIndex - 1; // West
		northIndex = mapYIndex - 1; // North
		southIndex = mapYIndex + 1; // South

		/* Intialize accumulators */
		int nValid = 0;
		cospart = 0.0;
		sinpart = 0.0;

		/* 1. Test NW */
		/* If NW point within IMAP boudaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 2. Test N */
		/* If N point within IMAP boudaries ... */
		if (northIndex >= 0) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + mapXIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 3. Test NE */
		/* If NE point within IMAP boudaries ... */
		if ((eastIndex < mappedImageWidth) && (northIndex >= 0)) {
			inputBlockImageMapIndex = 0 + (northIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 4. Test E */
		/* If E point within IMAP boudaries ... */
		if (eastIndex < mappedImageWidth) {
			inputBlockImageMapIndex = 0 + (mapYIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 5. Test SE */
		/* If SE point within IMAP boudaries ... */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + eastIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 6. Test S */
		/* If S point within IMAP boudaries ... */
		if (southIndex < mappedImageHeight) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + mapXIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 7. Test SW */
		/* If SW point within IMAP boudaries ... */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			inputBlockImageMapIndex = 0 + (southIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* 8. Test W */
		/* If W point within IMAP boudaries ... */
		if (westIndex >= 0) {
			inputBlockImageMapIndex = 0 + (mapYIndex * mappedImageWidth) + westIndex;
			/* If valid direction ... */
			if (oInputBlockImageMap.get(inputBlockImageMapIndex) != ILfs.INVALID_DIR) {
				/* Accumulate cosine and sine components of the direction */
				cospart += dirToRad.getCos()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				sinpart += dirToRad.getSin()[oInputBlockImageMap.get(inputBlockImageMapIndex)];
				/* Bump number of accumulated directions */
				nValid++;
			}
		}

		/* If there were no neighbors found with valid direction ... */
		if (nValid == ILfs.FALSE) {
			/* Return INVALID direction. */
			oDirStrength.set(0.0);
			oValid.set(nValid);
			oAverageDir.set(ILfs.INVALID_DIR);
			return;
		}

		oValid.set(nValid);
		/* Compute averages of accumulated cosine and sine direction components */
		cospart /= (nValid);
		sinpart /= (nValid);

		/* Compute directional strength as hypotenuse (without sqrt) of average */
		/* cosine and sine direction components. Believe this value will be on */
		/* the range of [0 .. 1]. */
		oDirStrength.set((cospart * cospart) + (sinpart * sinpart));
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when comparing doubles. */
		oDirStrength.set(getDefs().truncDoublePrecision(oDirStrength.get(), ILfs.TRUNC_SCALE));

		/* If the direction strength is not sufficiently high ... */
		if (oDirStrength.get() < ILfs.DIR_STRENGTH_MIN) {
			/* Return INVALID direction. */
			oDirStrength.set(0.0);
			oAverageDir.set(ILfs.INVALID_DIR);
			return;
		}

		/* Compute angle (in radians) from Arctan of avarage */
		/* cosine and sine direction components. I think this order */
		/* is necessary because 0 direction is vertical and positive */
		/* direction is clockwise. */
		theta = Math.atan2(sinpart, cospart);

		/* Atan2 returns theta on range [-PI..PI]. Adjust theta so that */
		/* it is on the range [0..2PI]. */
		pi2 = 2 * ILfs.M_PI;
		theta += pi2;
		theta = getDefs().fMod(theta, pi2);

		/* Pi_factor sets the period of the trig functions to NDIRS units in x. */
		/* For example, if NDIRS==16, then pi_factor = 2(PI/16) = .3926... */
		/* Dividing theta (in radians) by this factor ((1/pi_factor)==2.546...) */
		/* will produce directions on the range [0..NDIRS]. */
		pifactor = pi2 / (double) dirToRad.getNDirs(); // 2(M_PI/ndirs)

		/* Round off the direction and return it as an average direction */
		/* for the neighborhood. */
		avr = theta / pifactor;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		avr = getDefs().truncDoublePrecision(avr, ILfs.TRUNC_SCALE);
		oAverageDir.set(getDefs().sRound(avr));

		/* Really do need to map values > NDIRS back onto [0..NDIRS) range. */
		oAverageDir.set(oAverageDir.get() % dirToRad.getNDirs());
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: numValid8Nbrs - Given a block in an IMAP, counts the number of #cat:
	 * immediate neighbors that have a valid IMAP direction. Input:
	 * oInputBlockImageMap - 2-D vector of directional ridge flows mapXIndex -
	 * horizontal coord of current block in IMAP my - vertical coord of current
	 * block in IMAP mappedImageWidth - width (in blocks) of the IMAP
	 * mappedImageHeight - height (in blocks) of the IMAP Return Code: Non-negative
	 * - the number of valid IMAP neighbors
	 **************************************************************************/
	public int numValid8Nbrs(AtomicIntegerArray oInputBlockImageMap, final int mapXIndex, final int mapYIndex,
			final int mappedImageWidth, final int mappedImageHeight) {
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int nValid;

		/* Initialize VALID IMAP counter to zero. */
		nValid = 0;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mapXIndex + 1; // East index
		westIndex = mapXIndex - 1; // West index
		northIndex = mapYIndex - 1; // North index
		southIndex = mapYIndex + 1; // South index

		/* 1. Test NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries and it is VALID ... */
		if ((westIndex >= 0) && (northIndex >= 0)
				&& (oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex) >= 0)) {
			/* Bump VALID counter. */
			nValid++;
		}

		/* 2. Test N IMAP value. */
		if ((northIndex >= 0) && (oInputBlockImageMap.get((northIndex * mappedImageWidth) + mapXIndex) >= 0)) {
			nValid++;
		}

		/* 3. Test NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)
				&& (oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 4. Test E IMAP value. */
		if ((eastIndex < mappedImageWidth)
				&& (oInputBlockImageMap.get((mapYIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 5. Test SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex) >= 0)) {
			nValid++;
		}

		/* 6. Test S IMAP value. */
		if ((southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + mapXIndex) >= 0)) {
			nValid++;
		}

		/* 7. Test SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)
				&& (oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex) >= 0)) {
			nValid++;
		}

		/* 8. Test W IMAP value. */
		if ((westIndex >= 0) && (oInputBlockImageMap.get((mapYIndex * mappedImageWidth) + westIndex) >= 0)) {
			nValid++;
		}

		/* Return number of neighbors with VALID IMAP values. */
		return (nValid);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: InputBlockImageMap - Takes a vector of integer directions and smooths
	 * them #cat: by analyzing the direction of adjacent neighbors. Input:
	 * oInputBlockImageMap - vector of IMAP integer directions //mappedImageWidth -
	 * width (in blocks) of the IMAP //mappedImageHeight - height (in blocks) of the
	 * IMAP dirToRad - lookup table for converting integer directions lfsParams -
	 * parameters and thresholds for controlling LFS Output: oInputBlockImageMap -
	 * vector of smoothed input values
	 **************************************************************************/
	public void smoothInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, final DirToRad dirToRad,
			final LfsParams lfsParams) {
		int inputBlockImageMapIndex = 0;
		int inputBlockImageMapIndexValue;
		final int mappedImageWidth = getMappedImageWidth().get();
		final int mappedImageHeight = getMappedImageHeight().get();
		AtomicInteger averageDir = new AtomicInteger(0);
		AtomicInteger oValid = new AtomicInteger(0);
		AtomicReference<Double> oDirStrength = new AtomicReference<>();

		if (isShowLogs())
			logger.info("SMOOTH MAP");
		inputBlockImageMapIndexValue = oInputBlockImageMap.get(inputBlockImageMapIndex);
		for (int mapYIndex = 0; mapYIndex < mappedImageHeight; mapYIndex++) {
			for (int mapXIndex = 0; mapXIndex < mappedImageWidth; mapXIndex++) {
				/* Compute average direction from neighbors, returning the */
				/* number of valid neighbors used in the computation, and */
				/* the "strength" of the average direction. */
				average8NbrDir(averageDir, oDirStrength, oValid, oInputBlockImageMap, mapXIndex, mapYIndex,
						mappedImageWidth, mappedImageHeight, dirToRad);

				/* If average direction strength is strong enough */
				/* (Ex. thresh==0.2)... */
				if (oDirStrength.get() >= lfsParams.getDirStrengthMin()) {
					/* If IMAP direction is valid ... */
					if (inputBlockImageMapIndexValue != ILfs.INVALID_DIR) {
						/* Conduct valid neighbor test (Ex. thresh==3)... */
						if (oValid.get() >= lfsParams.getRmValidNbrMin()) {
							/* Reassign valid IMAP direction with average direction. */
							inputBlockImageMapIndexValue = averageDir.get();
						}
					}
					/* Otherwise IMAP direction is invalid ... */
					else {
						/* Even if IMAP value is invalid, if number of valid */
						/* neighbors is big enough (Ex. thresh==7)... */
						if (oValid.get() >= lfsParams.getSmoothValidNbrMin()) {
							/* Assign invalid IMAP direction with average direction. */
							inputBlockImageMapIndexValue = averageDir.get();
						}
					}
				}
				/* Bump to next IMAP direction. */
				oInputBlockImageMap.set(inputBlockImageMapIndex++, inputBlockImageMapIndexValue);
				inputBlockImageMapIndexValue = oInputBlockImageMap.get(inputBlockImageMapIndex);
			}
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: genNMap - Computes an NMAP from its associated 2D vector of integer
	 * #cat: directions (IMAP). Each value in the NMAP either represents #cat: a
	 * direction of dominant ridge flow in a block of the input #cat: grayscale
	 * image, or it contains a codes describing why such #cat: a direction was not
	 * procuded. #cat: For example, blocks near areas of high-curvature (such as
	 * #cat: with cores and deltas) will not produce reliable IMAP #cat: directions.
	 * Input: oInputBlockImageMap - associated input vector of IMAP directions
	 * mappedImageWidth - the width (in blocks) of the IMAP mappedImageHeight - the
	 * height (in blocks) of the IMAP lfsParams - parameters and thresholds for
	 * controlling LFS Output: oNMap - points to the created NMAP Return Code: Zero
	 * - successful completion Negative - system error
	 **************************************************************************/
	public int genNMap(AtomicIntegerArray oNMap, AtomicIntegerArray oInputBlockImageMap, final int mappedImageWidth,
			final int mappedImageHeight, final LfsParams lfsParams) {
		int nmapIndex;
		int inputBlockImageMapIndex;
		int nValid;
		int curvatureMeasure;
		int vorticityMeasure;

		nmapIndex = 0;
		inputBlockImageMapIndex = 0;

		/* Foreach row in IMAP ... */
		for (int mappedImageYIndex = 0; mappedImageYIndex < mappedImageHeight; mappedImageYIndex++) {
			/* Foreach column in IMAP ... */
			for (int mappedImageXIndex = 0; mappedImageXIndex < mappedImageWidth; mappedImageXIndex++) {
				/* Count number of valid neighbors around current block ... */
				nValid = numValid8Nbrs(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex, mappedImageWidth,
						mappedImageHeight);
				/* If block has no valid neighbors ... */
				if (nValid == ILfs.FALSE) {
					/* Set NMAP value to NO VALID NEIGHBORS */
					oNMap.set(nmapIndex, ILfs.NO_VALID_NBRS);
				} else {
					/* If current IMAP value is INVALID ... */
					if (oInputBlockImageMap.get(inputBlockImageMapIndex) == ILfs.INVALID_DIR) {
						/* If not enough VALID neighbors ... */
						if (nValid < lfsParams.getVortValidNbrMin()) {
							/* Set NMAP value to INVALID */
							oNMap.set(nmapIndex, ILfs.INVALID_DIR);
						} else {
							/* Otherwise measure vorticity of neighbors. */
							vorticityMeasure = vorticity(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex,
									mappedImageWidth, mappedImageHeight, lfsParams.getNumDirections());
							/* If vorticity too low ... */
							if (vorticityMeasure < lfsParams.getHighcurvVorticityMin()) {
								oNMap.set(nmapIndex, ILfs.INVALID_DIR);
							} else {
								/* Otherwise high-curvature area (Ex. core or delta). */
								oNMap.set(nmapIndex, ILfs.HIGH_CURVATURE);
							}
						}
					}
					/* Otherwise VALID IMAP value ... */
					else {
						/* Measure curvature around the VALID IMAP block. */
						curvatureMeasure = curvature(oInputBlockImageMap, mappedImageXIndex, mappedImageYIndex,
								mappedImageWidth, mappedImageHeight, lfsParams.getNumDirections());
						/* If curvature is too high ... */
						if (curvatureMeasure >= lfsParams.getHighcurvCurvatureMin()) {
							oNMap.set(nmapIndex, ILfs.HIGH_CURVATURE);
						} else {
							/* Otherwise acceptable amount of curature, so assign */
							/* VALID IMAP value to NMAP. */
							oNMap.set(nmapIndex, oInputBlockImageMap.get(inputBlockImageMapIndex));
						}
					}
				} // end else (nvalid > 0)
				/* BUMP IMAP and NMAP pointers. */
				inputBlockImageMapIndex++;
				nmapIndex++;

			} // mappedImageXIndex
		} // mappedImageYIndex

		return ILfs.FALSE;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: vorticity - Measures the amount of cummulative curvature incurred #cat:
	 * among the IMAP neighbors of the given block. Input: oInputBlockImageMap - 2D
	 * vector of ridge flow directions mappedImageXIndex - horizontal coord of
	 * current IMAP block mappedImageXIndex - vertical coord of current IMAP block
	 * mappedImageWidth - width (in blocks) of the IMAP mappedImageHeight - height
	 * (in blocks) of the IMAP nDirs - number of possible directions in the IMAP
	 * Return Code: Non-negative - the measured vorticity among the neighbors
	 **************************************************************************/
	public int vorticity(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight, final int nDirs) {
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int northwestValue;
		int northValue;
		int northeastValue;
		int eastValue;
		int southeastValue;
		int southValue;
		int southwestValue;
		int westValue;
		AtomicInteger oVorticityMeasure;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mappedImageXIndex + 1; // East index
		westIndex = mappedImageXIndex - 1; // West index
		northIndex = mappedImageYIndex - 1; // North index
		southIndex = mappedImageYIndex + 1; // South index

		/* 1. Get NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			/* Set neighbor value to IMAP value. */
			northwestValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex);
		} else {
			/* Otherwise, set the neighbor value to INVALID. */
			northwestValue = ILfs.INVALID_DIR;
		}

		/* 2. Get N IMAP value. */
		if (northIndex >= 0) {
			northValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			northValue = ILfs.INVALID_DIR;
		}

		/* 3. Get NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)) {
			northeastValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex);
		} else {
			northeastValue = ILfs.INVALID_DIR;
		}

		/* 4. Get E IMAP value. */
		if (eastIndex < mappedImageWidth) {
			eastValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + eastIndex);
		} else {
			eastValue = ILfs.INVALID_DIR;
		}

		/* 5. Get SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			southeastValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex);
		} else {
			southeastValue = ILfs.INVALID_DIR;
		}

		/* 6. Get S IMAP value. */
		if (southIndex < mappedImageHeight) {
			southValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			southValue = ILfs.INVALID_DIR;
		}

		/* 7. Get SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			southwestValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex);
		} else {
			southwestValue = ILfs.INVALID_DIR;
		}

		/* 8. Get W IMAP value. */
		if (westIndex >= 0) {
			westValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + westIndex);
		} else {
			westValue = ILfs.INVALID_DIR;
		}

		/* Now that we have all IMAP neighbors, accumulate vorticity between */
		/* the neighboring directions. */

		/* Initialize vorticity accumulator to zero. */
		oVorticityMeasure = new AtomicInteger(0);

		/* 1. NW & N */
		accumulateNbrVorticity(oVorticityMeasure, northwestValue, northValue, nDirs);

		/* 2. N & NE */
		accumulateNbrVorticity(oVorticityMeasure, northValue, northeastValue, nDirs);

		/* 3. NE & E */
		accumulateNbrVorticity(oVorticityMeasure, northeastValue, eastValue, nDirs);

		/* 4. E & SE */
		accumulateNbrVorticity(oVorticityMeasure, eastValue, southeastValue, nDirs);

		/* 5. SE & S */
		accumulateNbrVorticity(oVorticityMeasure, southeastValue, southValue, nDirs);

		/* 6. S & SW */
		accumulateNbrVorticity(oVorticityMeasure, southValue, southwestValue, nDirs);

		/* 7. SW & W */
		accumulateNbrVorticity(oVorticityMeasure, southwestValue, westValue, nDirs);

		/* 8. W & NW */
		accumulateNbrVorticity(oVorticityMeasure, westValue, northwestValue, nDirs);

		/* Return the accumulated vorticity measure. */
		return (oVorticityMeasure.get());
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: accumulateNbrVorticity - Accumlates the amount of curvature measures
	 * #cat: between neighboring IMAP blocks. Input: dir1 - first neighbor's integer
	 * IMAP direction dir2 - second neighbor's integer IMAP direction nDirs - number
	 * of possible IMAP directions Output: oVorticityMeasure - accumulated vorticity
	 * among neighbors measured so far
	 **************************************************************************/
	public void accumulateNbrVorticity(AtomicInteger oVorticityMeasure, final int dir1, final int dir2,
			final int nDirs) {
		int dist;

		/* Measure difference in direction between a pair of neighboring */
		/* directions. */
		/* If both neighbors are not equal and both are VALID ... */
		if ((dir1 != dir2) && (dir1 >= 0) && (dir2 >= 0)) {
			/* Measure the clockwise distance from the first to the second */
			/* directions. */
			dist = dir2 - dir1;
			/* If dist is negative, then clockwise distance must wrap around */
			/* the high end of the direction range. For example: */
			/* dir1 = 8 */
			/* dir2 = 3 */
			/* and ndirs = 16 */
			/* 3 - 8 = -5 */
			/* so 16 - 5 = 11 (the clockwise distance from 8 to 3) */
			if (dist < 0) {
				dist += nDirs;
			}
			/* If the change in clockwise direction is larger than 90 degrees as */
			/* in total the total number of directions covers 180 degrees. */
			if (dist > (nDirs >> 1)) {
				/* Decrement the vorticity measure. */
				oVorticityMeasure.set(oVorticityMeasure.get() - 1);
			} else {
				/* Otherwise, bump the vorticity measure. */
				oVorticityMeasure.set(oVorticityMeasure.get() + 1);
			}
		}
		/* Otherwise both directions are either equal or */
		/* one or both directions are INVALID, so ignore. */
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: curvature - Measures the largest change in direction between the #cat:
	 * current IMAP direction and its immediate neighbors. Input:
	 * oInputBlockImageMap - 2D vector of ridge flow directions mappedImageXIndex -
	 * horizontal coord of current IMAP block mappedImageYIndex - vertical coord of
	 * current IMAP block mappedImageWidth - width (in blocks) of the IMAP
	 * mappedImageHeight - height (in blocks) of the IMAP nDirs - number of possible
	 * directions in the IMAP Return Code: Non-negative - maximum change in
	 * direction found (curvature) Negative - No valid neighbor found to measure
	 * change in direction
	 **************************************************************************/
	public int curvature(AtomicIntegerArray oInputBlockImageMap, final int mappedImageXIndex,
			final int mappedImageYIndex, final int mappedImageWidth, final int mappedImageHeight, final int nDirs) {
		int nInputBlockImageMapIndexValue;
		int eastIndex;
		int westIndex;
		int northIndex;
		int southIndex;
		int northwestValue;
		int northValue;
		int northeastValue;
		int eastValue;
		int southeastValue;
		int southValue;
		int southwestValue;
		int westValue;
		int nCurvatureMeasure;
		int nDistance;

		/* Compute neighbor coordinates to current IMAP direction */
		eastIndex = mappedImageXIndex + 1; // East index
		westIndex = mappedImageXIndex - 1; // West index
		northIndex = mappedImageYIndex - 1; // North index
		southIndex = mappedImageYIndex + 1; // South index

		/* 1. Get NW IMAP value. */
		/* If neighbor indices are within IMAP boundaries ... */
		if ((westIndex >= 0) && (northIndex >= 0)) {
			/* Set neighbor value to IMAP value. */
			northwestValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + westIndex);
		} else {
			/* Otherwise, set the neighbor value to INVALID. */
			northwestValue = ILfs.INVALID_DIR;
		}

		/* 2. Get N IMAP value. */
		if (northIndex >= 0) {
			northValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			northValue = ILfs.INVALID_DIR;
		}

		/* 3. Get NE IMAP value. */
		if ((northIndex >= 0) && (eastIndex < mappedImageWidth)) {
			northeastValue = oInputBlockImageMap.get((northIndex * mappedImageWidth) + eastIndex);
		} else {
			northeastValue = ILfs.INVALID_DIR;
		}

		/* 4. Get E IMAP value. */
		if (eastIndex < mappedImageWidth) {
			eastValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + eastIndex);
		} else {
			eastValue = ILfs.INVALID_DIR;
		}

		/* 5. Get SE IMAP value. */
		if ((eastIndex < mappedImageWidth) && (southIndex < mappedImageHeight)) {
			southeastValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + eastIndex);
		} else {
			southeastValue = ILfs.INVALID_DIR;
		}

		/* 6. Get S IMAP value. */
		if (southIndex < mappedImageHeight) {
			southValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + mappedImageXIndex);
		} else {
			southValue = ILfs.INVALID_DIR;
		}

		/* 7. Get SW IMAP value. */
		if ((westIndex >= 0) && (southIndex < mappedImageHeight)) {
			southwestValue = oInputBlockImageMap.get((southIndex * mappedImageWidth) + westIndex);
		} else {
			southwestValue = ILfs.INVALID_DIR;
		}

		/* 8. Get W IMAP value. */
		if (westIndex >= 0) {
			westValue = oInputBlockImageMap.get((mappedImageYIndex * mappedImageWidth) + westIndex);
		} else {
			westValue = ILfs.INVALID_DIR;
		}

		/* Now that we have all IMAP neighbors, determine largest change in */
		/* direction from current block to each of its 8 VALID neighbors. */

		/* Initialize pointer to current IMAP value. */
		nInputBlockImageMapIndexValue = oInputBlockImageMap
				.get((mappedImageYIndex * mappedImageWidth) + mappedImageXIndex);

		/* Initialize curvature measure to negative as closest_dir_dist() */
		/* always returns -1=INVALID or a positive value. */
		nCurvatureMeasure = -1;

		/* 1. With NW */
		/* Compute closest distance between neighboring directions. */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northwestValue, nDirs);
		/* Keep track of maximum. */
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 2. With N */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 3. With NE */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, northeastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 4. With E */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, eastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 5. With SE */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southeastValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 6. With S */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 7. With SW */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, southwestValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* 8. With W */
		nDistance = getLfsUtil().closestDirDistance(nInputBlockImageMapIndexValue, westValue, nDirs);
		if (nDistance > nCurvatureMeasure) {
			nCurvatureMeasure = nDistance;
		}

		/* Return maximum difference between current block's IMAP direction */
		/* and the rest of its VALID neighbors. */
		return (nCurvatureMeasure);
	}

	public AtomicIntegerArray getDirectionMap() {
		return directionMap;
	}

	public void setDirectionMap(AtomicIntegerArray directionMap) {
		this.directionMap = directionMap;
	}

	public AtomicIntegerArray getLowContrastMap() {
		return lowContrastMap;
	}

	public void setLowContrastMap(AtomicIntegerArray lowContrastMap) {
		this.lowContrastMap = lowContrastMap;
	}

	public AtomicIntegerArray getLowFlowMap() {
		return lowFlowMap;
	}

	public void setLowFlowMap(AtomicIntegerArray lowFlowMap) {
		this.lowFlowMap = lowFlowMap;
	}

	public AtomicIntegerArray getHighCurveMap() {
		return highCurveMap;
	}

	public void setHighCurveMap(AtomicIntegerArray highCurveMap) {
		this.highCurveMap = highCurveMap;
	}

	public AtomicInteger getMappedImageWidth() {
		return mappedImageWidth;
	}

	public void setMappedImageWidth(AtomicInteger mappedImageWidth) {
		this.mappedImageWidth = mappedImageWidth;
	}

	public AtomicInteger getMappedImageHeight() {
		return mappedImageHeight;
	}

	public void setMappedImageHeight(AtomicInteger mappedImageHeight) {
		this.mappedImageHeight = mappedImageHeight;
	}
}
