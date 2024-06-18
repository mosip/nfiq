package org.mosip.nist.nfiq1.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.mindtct.Contour;
import org.mosip.nist.nfiq1.mindtct.Maps;
import org.mosip.nist.nfiq1.mindtct.Quality;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public interface ILfs {
	/*************************************************************************/
	/* ERROR CODES */
	/*************************************************************************/
	public static final int ERROR_CODE_02 = -2;// et_minutiae : input image pixel Depth != 8
	public static final int ERROR_CODE_33 = -33;// init_rotgrids : rotgrids.grids() : Null
	public static final int ERROR_CODE_60 = -60;// generateInputBlockImageMap : DFT grids must be square
	public static final int ERROR_CODE_70 = -70;// initialiseInputBlockImageMap : imap : NULL
	public static final int ERROR_CODE_80 = -80;// BLOCK OFFSET
	public static final int ERROR_CODE_91 = -91;// dftDirPowers : rowSums : Null
	public static final int ERROR_CODE_100 = -100;// sort_dft_waves : powNorms2 : NULL
	public static final int ERROR_CODE_110 = -110;// binarizeImage : binarizedImageData : null
	public static final int ERROR_CODE_140 = -140;// drawRotGrid : input direction exceeds range of rotated grids
	public static final int ERROR_CODE_200 = -200;// getNbrBlockIndex : illegal neighbor direction
	public static final int ERROR_CODE_210 = -210;// adjustHorizontalRescan : illegal neighbor direction
	public static final int ERROR_CODE_220 = -220;// adjustVerticalRescan : illegal neighbor direction
	public static final int ERROR_CODE_240 = -240;// isMinutiaAppearing : bad configuration of pixels
	public static final int ERROR_CODE_301 = -301;// removeHooksIslandsLakesOverlaps : INVALID direction
	public static final int ERROR_CODE_380 = -380;// removeMinutia : index out of range
	public static final int ERROR_CODE_412 = -412;// linePoints : coord list overflow
	public static final int ERROR_CODE_470 = -470;// updateNbrDists : illegal position for new neighbor
	public static final int ERROR_CODE_471 = -471;// updateNbrDists : insert neighbor failed
	public static final int ERROR_CODE_480 = -480;// insertNeighbor : insertion point exceeds lists
	public static final int ERROR_CODE_481 = -481;// insertNeighbor : overflow in neighbor lists
	public static final int ERROR_CODE_510 = -510;// lowContrastBlock : min percentile pixel not found
	public static final int ERROR_CODE_511 = -511;// lowContrastBlock : max percentile pixel not found
	public static final int ERROR_CODE_540 = -540;// genImageMaps : DFT grids must be square
	public static final int ERROR_CODE_581 = -581;// lfsDetectMinutiaeV2 : binary image has bad dimensions
	public static final int ERROR_CODE_591 = -591;// pixelizeMap : block dimensions do not match
	public static final int ERROR_CODE_611 = -611;// removeIslandsAndLakes : INVALID direction
	public static final int ERROR_CODE_620 = -620;// removeNearInvblocksV2 : margin too large for blocksize
	public static final int ERROR_CODE_641 = -641;// removeHooks : INVALID direction
	public static final int ERROR_CODE_651 = -651;// removeOverlaps : INVALID direction

	/*************************************************************************/
	/* OUTPUT FILE EXTENSIONS */
	/*************************************************************************/
	public static final String MIN_TXT_EXT = "min";
	public static final String LOW_CONTRAST_MAP_EXT = "lcm";
	public static final String HIGH_CURVE_MAP_EXT = "hcm";
	public static final String DIRECTION_MAP_EXT = "dm";
	public static final String LOW_FLOW_MAP_EXT = "lfm";
	public static final String QUALITY_MAP_EXT = "qm";
	public static final String AN2K_OUT_EXT = "mdt";
	public static final String BINARY_IMG_EXT = "brw";
	public static final String XYT_EXT = "xyt";

	/*************************************************************************/
	/* Minutiae XYT REPRESENTATION SCHEMES */
	/*************************************************************************/
	public static final int NIST_INTERNAL_XYT_REP = 0;
	public static final int M1_XYT_REP = 1;

	public static final double M_PI = Math.PI;// 3.14159265358979323846; // pi

	/*************************************************************************/
	/* 10, 2X3 pixel pair feature patterns used to define ridge endings */
	/* and bifurcations. */
	/* 2nd pixel pair is permitted to repeat multiple times in match. */
	public static final int NFEATURES = 10;
	public static final int BIFURCATION = 0;
	public static final int RIDGE_ENDING = 1;
	public static final int DISAPPEARING = 0;
	public static final int APPEARING = 1;

	/***** IMAGE CONSTANTS *****/
	public static final int DEFAULT_PPI = 500;
	public static final int IMAGE_DEPTH = 8;

	/* Intensity used to fill padded image area */
	public static final int PAD_VALUE = 128; // medium gray @ 8 bits

	/* Intensity used to draw on grayscale images */
	public static final int DRAW_PIXEL = 255; // white in 8 bits

	/* Definitions for 8-bit binary pixel intensities. */
	public static final int WHITE_PIXEL = 255;
	public static final int BLACK_PIXEL = 0;

	/* Definitions for controlling join_miutia(). */
	public static final int NO_BOUNDARY = 0;
	/* Draw without opposite perimeter pixels. */

	/* Draw with opposite perimeter pixels. */
	public static final int WITH_BOUNDARY = 1;

	/* Radial width added to join line (not including the boundary pixels). */
	public static final int JOIN_LINE_RADIUS = 1;

	/***** MAP CONSTANTS *****/
	/* Map value for not well-defined directions */
	public static final int INVALID_DIR = -1;

	/* Map value assigned when the current block has no neighbors */
	/* with valid direction. */
	public static final int NO_VALID_NBRS = -3;

	/* Map value designating a block is near a high-curvature */
	/* area such as a core or delta. */
	public static final int HIGH_CURVATURE = -2;

	/* This specifies the pixel dimensions of each block in the IMAP */
	public static final int IMAP_BLOCKSIZE = 24;

	/* Pixel dimension of image blocks. The following three constants work */
	/* together to define a system of 8X8 adjacent and non-overlapping */
	/* blocks that are assigned results from analyzing a larger 24X24 */
	/* window centered about each of the 8X8 blocks. */
	/* CAUTION: If MAP_BLOCKSIZE_V2 is changed, then the following will */
	/* likely need to be changed: MAP_WINDOWOFFSET_V2, */
	/* TRANS_DIR_PIX_V2, */
	/* INV_BLOCK_MARGIN_V2 */
	public static final int MAP_BLOCKSIZE_V2 = 8;

	/* Pixel dimension of window that surrounds the block. The result from */
	/* analyzing the content of the window is stored in the interior block. */
	public static final int MAP_WINDOWSIZE_V2 = 24;

	/* Pixel offset in X & Y from the origin of the block to the origin of */
	/* the surrounding window. */
	public static final int MAP_WINDOWOFFSET_V2 = 8;

	/* This is the number of integer directions to be used in semicircle. */
	/* CAUTION: If NUM_DIRECTIONS is changed, then the following will */
	/* likely need to be changed: HIGHCURV_VORTICITY_MIN, */
	/* HIGHCURV_CURVATURE_MIN, */
	/* FORK_INTERVAL */
	public static final int NUM_DIRECTIONS = 16;

	/* This is the theta from which integer directions */
	/* are to begin. */
	public static final double START_DIR_ANGLE = (M_PI / 2.0); /* 90 degrees */

	/* Minimum number of valid neighbors required for a */
	/* valid block value to keep from being removed. */
	public static final int RMV_VALID_NBR_MIN = 3;

	/* Minimum strength for a direction to be considered significant. */
	public static final double DIR_STRENGTH_MIN = 0.2;

	/* Maximum distance allowable between valid block direction */
	/* and the average direction of its neighbors before the */
	/* direction is removed. */
	public static final int DIR_DISTANCE_MAX = 3;

	/* Minimum number of valid neighbors required for an */
	/* INVALID block direction to receive its direction from */
	/* the average of its neighbors. */
	public static final int SMTH_VALID_NBR_MIN = 7;

	/* Minimum number of valid neighbors required for a block */
	/* with an INVALID block direction to be measured for */
	/* vorticity. */
	public static final int VORT_VALID_NBR_MIN = 7;

	/* The minimum vorticity value whereby an INVALID block */
	/* is determined to be high-curvature based on the directions */
	/* of it neighbors. */
	public static final int HIGHCURV_VORTICITY_MIN = 5;

	/* The minimum curvature value whereby a VALID direction block is */
	/* determined to be high-curvature based on it value compared with */
	/* its neighbors' directions. */
	public static final int HIGHCURV_CURVATURE_MIN = 5;

	/* Minimum number of neighbors with VALID direction for an INVALID */
	/* directon block to have its direction interpolated from those neighbors. */
	public static final int MIN_INTERPOLATE_NBRS = 2;

	/* Definitions for creating a low contrast map. */
	/* Percentile cut off for choosing min and max pixel intensities */
	/* in a block. */
	public static final int PERCENTILE_MIN_MAX = 10;

	/* The minimum delta between min and max percentile pixel intensities */
	/* in block for block NOT to be considered low contrast. (Note that */
	/* this value is in terms of 6-bit pixels.) */
	public static final int MIN_CONTRAST_DELTA = 5;

	/***** DFT CONSTANTS *****/
	/* This specifies the number of DFT wave forms to be applied */
	public static final int NUM_DFT_WAVES = 4;

	/* Minimum total DFT power for any given block */
	/* which is used to compute an average power. */
	/* By setting a non-zero minimum total,possible */
	/* division by zero is avoided. This value was */
	/* taken from HO39. */
	public static final double MIN_POWER_SUM = 10.0;

	/* Thresholds and factors used by HO39. Renamed */
	/* here to give more meaning. */
	/* HO39 Name=Value */
	/* Minimum DFT power allowable in any one direction. */
	public static final double POWMAX_MIN = 100000.0; // thrhf=1e5f

	/* Minimum normalized power allowable in any one */
	/* direction. */
	public static final double POWNORM_MIN = 3.8; // disc=3.8f

	/* Maximum power allowable at the lowest frequency */
	/* DFT wave. */
	public static final double POWMAX_MAX = 50000000.0; // thrlf=5e7f

	/* Check for a fork at +- this number of units from */
	/* current integer direction. For example, */
	/* 2 dir ==> 11.25 X 2 degrees. */
	public static final int FORK_INTERVAL = 2;

	/* Minimum DFT power allowable at fork angles is */
	/* FORK_PCT_POWMAX X block's max directional power. */
	public static final double FORK_PCT_POWMAX = 0.7;

	/* Minimum normalized power allowable at fork angles */
	/* is FORK_PCT_POWNORM X POWNORM_MIN */
	public static final double FORK_PCT_POWNORM = 0.75;

	/***** BINRAIZATION CONSTANTS *****/
	/* Directional binarization grid dimensions. */
	public static final int DIRBIN_GRID_W = 7;
	public static final int DIRBIN_GRID_H = 9;

	/* The pixel dimension (square) of the grid used in isotropic */
	/* binarization. */
	public static final int ISOBIN_GRID_DIM = 11;

	/* Number of passes through the resulting binary image where holes */
	/* of pixel length 1 in horizontal and vertical runs are filled. */
	public static final int NUM_FILL_HOLES = 3;

	/***** Minutiae DETECTION CONSTANTS *****/

	/* The maximum pixel translation distance in X or Y within which */
	/* two potential Minutia points are to be considered similar. */
	public static final int MAX_MINUTIA_DELTA = 10;

	/* If the angle of a contour exceeds this angle, then it is NOT */
	/* to be considered to contain Minutiae. */
	public static final double MAX_HIGH_CURVE_THETA = (M_PI / 3.0);

	/* Half the length in pixels to be extracted for a high-curvature contour. */
	public static final int HIGH_CURVE_HALF_CONTOUR = 14;

	/* Loop must be larger than this threshold (in pixels) to be considered */
	/* to contain Minutiae. */
	public static final int MIN_LOOP_LEN = 20;

	/* If loop's minimum distance half way across its contour is less than */
	/* this threshold, then loop is tested for Minutiae. */
	public static final double MIN_LOOP_ASPECT_DIST = 1.0;

	/* If ratio of loop's maximum/minimum distances half way across its */
	/* contour is >= to this threshold, then loop is tested for Minutiae. */
	public static final double MIN_LOOP_ASPECT_RATIO = 2.25;

	/* There are 10 unique feature patterns with ID = [0..9] , */
	/* so set LOOP ID to 10 (one more than max pattern ID). */
	public static final int LOOP_ID = 10;

	/* Definitions for controlling the scanning of Minutiae. */
	public static final int SCAN_HORIZONTAL = 0;
	public static final int SCAN_VERTICAL = 1;
	public static final int SCAN_CLOCKWISE = 0;
	public static final int SCAN_COUNTER_CLOCKWISE = 1;

	/* The dimension of the chaincode loopkup matrix. */
	public static final int NBR8_DIM = 3;

	/* Default Minutiae reliability. */
	public static final double DEFAULT_RELIABILITY = 0.99;

	/* Medium Minutia reliability. */
	public static final double MEDIUM_RELIABILITY = 0.50;

	/* High Minutia reliability. */
	public static final double HIGH_RELIABILITY = 0.99;

	/***** Minutiae LINKING CONSTANTS *****/
	/* Definitions for controlling the linking of Minutiae. */
	/* Square dimensions of 2D table of potentially linked Minutiae. */
	public static final int LINK_TABLE_DIM = 20;

	/* Distance (in pixels) used to determine if the orthogonal distance */
	/* between the coordinates of 2 Minutia points are sufficiently close */
	/* to be considered for linking. */
	public static final int MAX_LINK_DIST = 20;

	/* Minimum distance (in pixels) between 2 Minutia points that an angle */
	/* computed between the points may be considered reliable. */
	public static final int MIN_THETA_DIST = 5;

	/* Maximum number of transitions along a contiguous pixel trajectory */
	/* between 2 Minutia points for that trajectory to be considered "free" */
	/* of obstacles. */
	public static final int MAXTRANS = 2;

	/* Parameters used to compute a link score between 2 Minutiae. */
	public static final double SCORE_THETA_NORM = 15.0;
	public static final double SCORE_DIST_NORM = 10.0;
	public static final double SCORE_DIST_WEIGHT = 4.0;
	public static final double SCORE_NUMERATOR = 32000.0;

	/***** FALSE Minutiae REMOVAL CONSTANTS *****/
	/* Definitions for removing hooks, islands, lakes, and overlaps. */
	/* Distance (in pixels) used to determine if the orthogonal distance */
	/* between the coordinates of 2 Minutia points are sufficiently close */
	/* to be considered for removal. */
	public static final int MAX_RMTEST_DIST = 8;
	public static final int MAX_RMTEST_DIST_V2 = 16;

	/* Length of pixel contours to be traced and analyzed for possible hooks. */
	public static final int MAX_HOOK_LEN = 15;
	public static final int MAX_HOOK_LEN_V2 = 30;

	/* Half the maximum length of pixel contours to be traced and analyzed */
	/* for possible loops (islands/lakes). */
	public static final int MAX_HALF_LOOP = 15;
	public static final int MAX_HALF_LOOP_V2 = 30;

	/* Definitions for removing Minutiae that are sufficiently close and */
	/* point to a block with invalid ridge flow. */
	/* Distance (in pixels) in direction opposite the Minutia to be */
	/* considered sufficiently close to an invalid block. */
	public static final int TRANS_DIR_PIX = 6;
	public static final int TRANS_DIR_PIX_V2 = 4;

	/* Definitions for removing small holes (islands/lakes). */
	/* Maximum circumference (in pixels) of qualifying loops. */
	public static final int SMALL_LOOP_LEN = 15;

	/* Definitions for removing or adusting side Minutiae. */
	/* Half the number of pixels to be traced to form a complete contour. */
	public static final int SIDE_HALF_CONTOUR = 7;

	/* Definitions for removing Minutiae near invalid blocks. */
	/* Maximum orthogonal distance a Minutia can be neighboring a block with */
	/* invalid ridge flow in order to be removed. */
	public static final int INV_BLOCK_MARGIN = 6;
	public static final int INV_BLOCK_MARGIN_V2 = 4;

	/* Given a sufficiently close, neighboring invalid block, if that invalid */
	/* block has a total number of neighboring blocks with valid ridge flow */
	/* less than this threshold, then the Minutia point is removed. */
	public static final int RM_VALID_NBR_MIN = 7;

	/* Definitions for removing overlaps. */
	/* Maximum pixel distance between 2 points to be tested for overlapping */
	/* conditions. */
	public static final int MAX_OVERLAP_DIST = 8;

	/* Maximum pixel distance between 2 points on opposite sides of an overlap */
	/* will be joined. */
	public static final int MAX_OVERLAP_JOIN_DIST = 6;

	/* Definitions for removing "irregularly-shaped" Minutiae. */
	/* Contour steps to be traced to 1st measuring point. */
	public static final int MALFORMATION_STEPS_1 = 10;
	/* Contour steps to be traced to 2nd measuring point. */
	public static final int MALFORMATION_STEPS_2 = 20;

	/* Minimum ratio of distances across feature at the two point to be */
	/* considered normal. */
	public static final double MIN_MALFORMATION_RATIO = 2.0;
	/* Maximum distance permitted across feature to be considered normal. */
	public static final int MAX_MALFORMATION_DIST = 20;

	/* Definitions for removing Minutiae on pores. */
	/* Translation distance (in pixels) from Minutia point in opposite direction */
	/* in order to get off a valley edge and into the neighboring ridge. */
	public static final int PORES_TRANS_R = 3;

	/* Number of steps (in pixels) to search for edge of current ridge. */
	public static final int PORES_PERP_STEPS = 12;

	/* Number of pixels to be traced to find forward contour points. */
	public static final int PORES_STEPS_FWD = 10;

	/* Number of pixels to be traced to find backward contour points. */
	public static final int PORES_STEPS_BWD = 8;

	/* Minimum squared distance between points before being considered zero. */
	public static final double PORES_MIN_DIST2 = 0.5;

	/* Max ratio of computed distances between pairs of forward and backward */
	/* contour points to be considered a pore. */
	public static final double PORES_MAX_RATIO = 2.25;

	/***** RIDGE COUNTING CONSTANTS *****/
	/* Definitions for detecting nearest neighbors and counting ridges. */
	/* Maximum number of nearest neighbors per Minutia. */
	public static final int MAX_NBRS = 5;

	/* Maximum number of contour steps taken to validate a ridge crossing. */
	public static final int MAX_RIDGE_STEPS = 10;

	/*************************************************************************/
	/* QUALITY/RELIABILITY DEFINITIONS */
	/*************************************************************************/
	/* Quality map levels */
	public static final int QMAP_LEVELS = 5;

	/* Neighborhood radius in millimeters computed from 11 pixles */
	/* scanned at 19.69 pixels/mm. */
	public static final double RADIUS_MM = (11.0 / 19.69);

	/* Ideal Standard Deviation of pixel values in a neighborhood. */
	public static final int IDEALSTDEV = 64;
	/* Ideal Mean of pixel values in a neighborhood. */
	public static final int IDEALMEAN = 127;

	/* Look for neighbors this many blocks away. */
	public static final int NEIGHBOR_DELTA = 2;

	/*************************************************************************/
	/* GENERAL DEFINITIONS */
	/*************************************************************************/
	public static final String LFS_VERSION_STR = "NIST_LFS_VER2";

	public static final int NORTH = 0;
	public static final int SOUTH = 4;
	public static final int EAST = 2;
	public static final int WEST = 6;

	public static final int TRUE = 1;
	public static final int FALSE = 0;

	public static final int FOUND = TRUE;
	public static final int NOT_FOUND = FALSE;

	public static final int HOOK_FOUND = 1;
	public static final int LOOP_FOUND = 1;
	public static final int IGNORE = 2;
	public static final int LIST_FULL = 3;
	public static final int INCOMPLETE = 3;

	/* Pixel value limit in 6-bit image. */
	public static final int IMG_6BIT_PIX_LIMIT = 64;

	/* Maximum number (or reallocated chunks) of Minutia to be detected */
	/* in an image. */
	public static final int MAX_MINUTIAE = 1000;

	/* If both deltas in X and Y for a line of specified slope is less than */
	/* this threshold, then the angle for the line is set to 0 radians. */
	public static final double MIN_SLOPE_DELTA = 0.5;

	/* Designates that rotated grid offsets should be relative */
	/* to the grid's center. */
	public static final int RELATIVE_TO_CENTER = 0;

	/* Designates that rotated grid offsets should be relative */
	/* to the grid's origin. */
	public static final int RELATIVE_TO_ORIGIN = 1;

	/* Truncate floating point precision by multiply, rounding, and then */
	/* dividing by this value. This enables consistant results across */
	/* different computer architectures. */
	public static final double TRUNC_SCALE = 16384.0;

	/* Designates passed argument as undefined. */
	public static final int UNDEFINED = -1;

	/* Dummy values for unused LFS control parameters. */
	public static final int UNUSED_INT = 0;
	public static final double UNUSED_DBL = 0.0;

	/* Lookup tables for converting from integer directions */
	/* to angles in radians. */
	@Getter
	@Setter
	@Data
	public class DirToRad {
		private int nDirs;
		private double[] cos;
		private double[] sin;

		public DirToRad(int nDirs) {
			super();
			this.nDirs = nDirs;
			this.setCos(new double[nDirs]);
			this.setSin(new double[nDirs]);
		}
	}

	/* DFT wave form structure containing both cosine and */
	/* sine components for a specific frequency. */
	@Getter
	@Setter
	@Data
	public class DftWave {
		private double[] cos;
		private double[] sin;

		public DftWave(int waveLen) {
			super();
			this.cos = new double[waveLen];
			this.sin = new double[waveLen];
		}
	}

	/* DFT wave forms structure containing all wave forms */
	/* to be used in DFT analysis. */
	@Getter
	@Setter
	@Data
	public class DftWaves {
		private int nWaves;
		private int waveLen;// blockOffsetSize
		private DftWave[] waves;

		public DftWaves(int nWaves, int waveLen) {
			super();
			this.nWaves = nWaves;
			this.waveLen = waveLen;
			this.waves = new DftWave[nWaves];
		}
	}

	/* Rotated pixel offsets for a grid of specified dimensions */
	/* rotated at a specified number of different orientations */
	/* (directions). This structure used by the DFT analysis */
	/* when generating a Direction Map and also for conducting */
	/* isotropic binarization. */
	@Getter
	@Setter
	@Data
	public class RotGrids {
		private int pad;
		private int relative2;
		private double startAngle;
		private int noOfGrids;
		private int gridWidth;
		private int gridHeight;
		private int[][] grids;

		public RotGrids(double startAngle, int noOfGrids, int gridWidth, int gridHeight, int relative2) {
			super();
			this.startAngle = startAngle;
			this.noOfGrids = noOfGrids;
			this.gridWidth = gridWidth;
			this.gridHeight = gridHeight;
			this.relative2 = relative2;
			this.grids = new int[noOfGrids][gridWidth * gridHeight];
		}
	}

	@Getter
	@Setter
	@Data
	public class Minutia {
		private int x;
		private int y;
		private int ex;
		private int ey;
		private int direction;
		private double reliability;
		private int type;
		private int appearing;
		private int featureId;
		private AtomicIntegerArray nbrs;
		private AtomicIntegerArray ridgeCounts;
		private int numNbrs;

		public Minutia() {
			super();
		}

		@Override
		public String toString() {
			return "Minutia [x=" + x + ", y=" + y + ", ex=" + ex + ", ey=" + ey + ", direction=" + direction
					+ ", reliability=" + reliability + ", type=" + type + ", appearing=" + appearing + ", featureId="
					+ featureId + ", nbrs=" + nbrs + ", ridgeCounts=" + ridgeCounts + ", numNbrs=" + numNbrs + "]\n";
		}
	}

	@Getter
	@Setter
	@Data
	public class Minutiae {
		private int alloc;
		private int num;
		private List<Minutia> list;

		public Minutiae() {
			super();
		}

		public Minutiae(int alloc, int num) {
			super();
			this.alloc = alloc;
			this.num = num;
			this.list = new ArrayList<>(alloc);
		}

		@Override
		public String toString() {
			return "Minutiae [alloc=" + alloc + ", num=" + num + ", list=" + list + "]";
		}
	}

	@Getter
	@Setter
	@Data
	public class FeaturePattern {
		private int featurePatternCount = 2;
		private int type;
		private int appearing;
		private int[] first = new int[featurePatternCount];
		private int[] second = new int[featurePatternCount];
		private int[] third = new int[featurePatternCount];

		public FeaturePattern(int type, int appearing, int[] first, int[] second, int[] third) {
			super();
			this.type = type;
			this.appearing = appearing;
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}

	/* SHAPE structure definitions. */
	@Getter
	@Setter
	@Data
	public class Shape {
		private int yMin; // Y-coord of top-most scanline in shape.
		private int yMax; // Y-coord of bottom-most scanline in shape.
		private AtomicReferenceArray<Rows> rows; // List of row pointers comprising the shape.
		private int alloc; // Number of rows allocated for shape.
		private int nRows; // Number of rows assigned to shape.

		public Shape() {
			super();
		}
	}

	@Getter
	@Setter
	@Data
	public class Rows {
		private int y; // Y-coord of current row in shape.
		private AtomicIntegerArray xs; // X-coords for shape contour points on current row.
		private int alloc; // Number of points allocate for x-coords on row.
		private int noOfPts; // Number of points assigned for x-coords on row.
	}

	/* Parameters used by LFS for setting thresholds and */
	/* defining testing criterion. */
	@Getter
	@Setter
	@Data
	public class LfsParams {
		/* Image Controls */
		private int padValue;
		private int joinLineRadius;

		/* Map Controls */
		private int blockOffsetSize; // Pixel dimension image block.
		private int windowSize; // Pixel dimension window surrounding block.
		private int windowOffset; // Offset in X & Y from block to window origin.
		private int numDirections;
		private double startDirAngle;
		private int rmvValidNbrMin;
		private double dirStrengthMin;
		private int dirDistanceMax;
		private int smoothValidNbrMin;
		private int vortValidNbrMin;
		private int highcurvVorticityMin;
		private int highcurvCurvatureMin;
		private int minInterpolateNbrs;
		private int percentileMinMax;
		private int minContrastDelta;

		/* DFT Controls */
		private int numDftWaves;
		private double powmaxMin;
		private double pownormMin;
		private double powmaxMax;
		private int forkInterval;
		private double forkPctPowmax;
		private double forkPctPownorm;

		/* Binarization Controls */
		private int dirbinGridWidth;
		private int dirbinGridHeight;
		private int isoBinGridDim;
		private int numFillHoles;

		/* Minutiae Detection Controls */
		private int maxMinutiaDelta;
		private double maxHighCurveTheta;
		private int highCurveHalfContour;
		private int minLoopLen;
		private double minLoopAspectDist;
		private double minLoopAspectRatio;

		/* Minutiae Link Controls */
		private int linkTableDim;
		private int maxLinkDist;
		private int minThetaDist;
		private int maxTrans;
		private double scoreThetaNorm;
		private double scoreDistNorm;
		private double scoreDistWeight;
		private double scoreNumerator;

		/* False Minutiae Removal Controls */
		private int maxRmTestDist;
		private int maxHookLen;
		private int maxHalfLoop;
		private int transDirPixel;
		private int smallLoopLen;
		private int sideHalfContour;
		private int invBlockMargin;
		private int rmValidNbrMin;
		private int maxOverlapDist;
		private int maxOverlapJoinDist;
		private int malformationSteps1;
		private int malformationSteps2;
		private double minMalformationRatio;
		private int maxMalformationDist;
		private int poresTransR;
		private int poresPerpSteps;
		private int poresStepsFwd;
		private int poresStepsBwd;
		private double poresMinDist2;
		private double poresMaxRatio;

		/* Ridge Counting Controls */
		private int maxNbrs;
		private int maxRidgeSteps;

		public LfsParams(int padValue, int joinLineRadius, int blockOffsetSize, int windowSize, int windowOffset,
				int numDirections, double startDirAngle, int rmvValidNbrMin, double dirStrengthMin, int dirDistanceMax,
				int smoothValidNbrMin, int vortValidNbrMin, int highcurvVorticityMin, int highcurvCurvatureMin,
				int minInterpolateNbrs, int percentileMinMax, int minContrastDelta, int numDftWaves, double powmaxMin,
				double pownormMin, double powmaxMax, int forkInterval, double forkPctPowmax, double forkPctPownorm,
				int dirbinGridWidth, int dirbinGridHeight, int isoBinGridDim, int numFillHoles, int maxMinutiaDelta,
				double maxHighCurveTheta, int highCurveHalfContour, int minLoopLen, double minLoopAspectDist,
				double minLoopAspectRatio, int linkTableDim, int maxLinkDist, int minThetaDist, int maxTrans,
				double scoreThetaNorm, double scoreDistNorm, double scoreDistWeight, double scoreNumerator,
				int maxRmTestDist, int maxHookLen, int maxHalfLoop, int transDirPixel, int smallLoopLen,
				int sideHalfContour, int invBlockMargin, int rmValidNbrMin, int maxOverlapDist, int maxOverlapJoinDist,
				int malformationSteps1, int malformationSteps2, double minMalformationRatio, int maxMalformationDist,
				int poresTransR, int poresPerpSteps, int poresStepsFwd, int poresStepsBwd, double poresMinDist2,
				double poresMaxRatio, int maxNbrs, int maxRidgeSteps) {
			super();
			this.padValue = padValue;
			this.joinLineRadius = joinLineRadius;
			this.blockOffsetSize = blockOffsetSize;
			this.windowSize = windowSize;
			this.windowOffset = windowOffset;
			this.numDirections = numDirections;
			this.startDirAngle = startDirAngle;
			this.rmvValidNbrMin = rmvValidNbrMin;
			this.dirStrengthMin = dirStrengthMin;
			this.dirDistanceMax = dirDistanceMax;
			this.smoothValidNbrMin = smoothValidNbrMin;
			this.vortValidNbrMin = vortValidNbrMin;
			this.highcurvVorticityMin = highcurvVorticityMin;
			this.highcurvCurvatureMin = highcurvCurvatureMin;
			this.minInterpolateNbrs = minInterpolateNbrs;
			this.percentileMinMax = percentileMinMax;
			this.minContrastDelta = minContrastDelta;
			this.numDftWaves = numDftWaves;
			this.powmaxMin = powmaxMin;
			this.pownormMin = pownormMin;
			this.powmaxMax = powmaxMax;
			this.forkInterval = forkInterval;
			this.forkPctPowmax = forkPctPowmax;
			this.forkPctPownorm = forkPctPownorm;
			this.dirbinGridWidth = dirbinGridWidth;
			this.dirbinGridHeight = dirbinGridHeight;
			this.isoBinGridDim = isoBinGridDim;
			this.numFillHoles = numFillHoles;
			this.maxMinutiaDelta = maxMinutiaDelta;
			this.maxHighCurveTheta = maxHighCurveTheta;
			this.highCurveHalfContour = highCurveHalfContour;
			this.minLoopLen = minLoopLen;
			this.minLoopAspectDist = minLoopAspectDist;
			this.minLoopAspectRatio = minLoopAspectRatio;
			this.linkTableDim = linkTableDim;
			this.maxLinkDist = maxLinkDist;
			this.minThetaDist = minThetaDist;
			this.maxTrans = maxTrans;
			this.scoreThetaNorm = scoreThetaNorm;
			this.scoreDistNorm = scoreDistNorm;
			this.scoreDistWeight = scoreDistWeight;
			this.scoreNumerator = scoreNumerator;
			this.maxRmTestDist = maxRmTestDist;
			this.maxHookLen = maxHookLen;
			this.maxHalfLoop = maxHalfLoop;
			this.transDirPixel = transDirPixel;
			this.smallLoopLen = smallLoopLen;
			this.sideHalfContour = sideHalfContour;
			this.invBlockMargin = invBlockMargin;
			this.rmValidNbrMin = rmValidNbrMin;
			this.maxOverlapDist = maxOverlapDist;
			this.maxOverlapJoinDist = maxOverlapJoinDist;
			this.malformationSteps1 = malformationSteps1;
			this.malformationSteps2 = malformationSteps2;
			this.minMalformationRatio = minMalformationRatio;
			this.maxMalformationDist = maxMalformationDist;
			this.poresTransR = poresTransR;
			this.poresPerpSteps = poresPerpSteps;
			this.poresStepsFwd = poresStepsFwd;
			this.poresStepsBwd = poresStepsBwd;
			this.poresMinDist2 = poresMinDist2;
			this.poresMaxRatio = poresMaxRatio;
			this.maxNbrs = maxNbrs;
			this.maxRidgeSteps = maxRidgeSteps;
		}
/*
		public int getPadValue() {
			return padValue;
		}

		public void setPadValue(int padValue) {
			this.padValue = padValue;
		}

		public int getJoinLineRadius() {
			return joinLineRadius;
		}

		public void setJoinLineRadius(int joinLineRadius) {
			this.joinLineRadius = joinLineRadius;
		}

		public int getBlockSize() {
			return blockOffsetSize;
		}

		public void setBlockSize(int blockOffsetSize) {
			this.blockOffsetSize = blockOffsetSize;
		}

		public int getWindowSize() {
			return windowSize;
		}

		public void setWindowSize(int windowSize) {
			this.windowSize = windowSize;
		}

		public int getWindowOffset() {
			return windowOffset;
		}

		public void setWindowOffset(int windowOffset) {
			this.windowOffset = windowOffset;
		}

		public int getNumDirections() {
			return numDirections;
		}

		public void setNumDirections(int numDirections) {
			this.numDirections = numDirections;
		}

		public double getStartDirAngle() {
			return startDirAngle;
		}

		public void setStartDirAngle(double startDirAngle) {
			this.startDirAngle = startDirAngle;
		}

		public int getRmvValidNbrMin() {
			return rmvValidNbrMin;
		}

		public void setRmvValidNbrMin(int rmvValidNbrMin) {
			this.rmvValidNbrMin = rmvValidNbrMin;
		}

		public double getDirStrengthMin() {
			return dirStrengthMin;
		}

		public void setDirStrengthMin(double dirStrengthMin) {
			this.dirStrengthMin = dirStrengthMin;
		}

		public int getDirDistanceMax() {
			return dirDistanceMax;
		}

		public void setDirDistanceMax(int dirDistanceMax) {
			this.dirDistanceMax = dirDistanceMax;
		}

		public int getSmoothValidNbrMin() {
			return smoothValidNbrMin;
		}

		public void setSmoothValidNbrMin(int smoothValidNbrMin) {
			this.smoothValidNbrMin = smoothValidNbrMin;
		}

		public int getVortValidNbrMin() {
			return vortValidNbrMin;
		}

		public void setVortValidNbrMin(int vortValidNbrMin) {
			this.vortValidNbrMin = vortValidNbrMin;
		}

		public int getHighcurvVorticityMin() {
			return highcurvVorticityMin;
		}

		public void setHighcurvVorticityMin(int highcurvVorticityMin) {
			this.highcurvVorticityMin = highcurvVorticityMin;
		}

		public int getHighcurvCurvatureMin() {
			return highcurvCurvatureMin;
		}

		public void setHighcurvCurvatureMin(int highcurvCurvatureMin) {
			this.highcurvCurvatureMin = highcurvCurvatureMin;
		}

		public int getMinInterpolateNbrs() {
			return minInterpolateNbrs;
		}

		public void setMinInterpolateNbrs(int minInterpolateNbrs) {
			this.minInterpolateNbrs = minInterpolateNbrs;
		}

		public int getPercentileMinMax() {
			return percentileMinMax;
		}

		public void setPercentileMinMax(int percentileMinMax) {
			this.percentileMinMax = percentileMinMax;
		}

		public int getMinContrastDelta() {
			return minContrastDelta;
		}

		public void setMinContrastDelta(int minContrastDelta) {
			this.minContrastDelta = minContrastDelta;
		}

		public int getNumDftWaves() {
			return numDftWaves;
		}

		public void setNumDftWaves(int numDftWaves) {
			this.numDftWaves = numDftWaves;
		}

		public double getPowmaxMin() {
			return powmaxMin;
		}

		public void setPowmaxMin(double powmaxMin) {
			this.powmaxMin = powmaxMin;
		}

		public double getPownormMin() {
			return pownormMin;
		}

		public void setPownormMin(double pownormMin) {
			this.pownormMin = pownormMin;
		}

		public double getPowmaxMax() {
			return powmaxMax;
		}

		public void setPowmaxMax(double powmaxMax) {
			this.powmaxMax = powmaxMax;
		}

		public int getForkInterval() {
			return forkInterval;
		}

		public void setForkInterval(int forkInterval) {
			this.forkInterval = forkInterval;
		}

		public double getForkPctPowmax() {
			return forkPctPowmax;
		}

		public void setForkPctPowmax(double forkPctPowmax) {
			this.forkPctPowmax = forkPctPowmax;
		}

		public double getForkPctPownorm() {
			return forkPctPownorm;
		}

		public void setForkPctPownorm(double forkPctPownorm) {
			this.forkPctPownorm = forkPctPownorm;
		}

		public int getDirbinGridWidth() {
			return dirbinGridWidth;
		}

		public void setDirbinGridWidth(int dirbinGridWidth) {
			this.dirbinGridWidth = dirbinGridWidth;
		}

		public int getDirbinGridHeight() {
			return dirbinGridHeight;
		}

		public void setDirbinGridHeight(int dirbinGridHeight) {
			this.dirbinGridHeight = dirbinGridHeight;
		}

		public int getIsoBinGridDim() {
			return isoBinGridDim;
		}

		public void setIsobin_grid_dim(int isoBinGridDim) {
			this.isoBinGridDim = isoBinGridDim;
		}

		public int getNumFillHoles() {
			return numFillHoles;
		}

		public void setNumFillHoles(int numFillHoles) {
			this.numFillHoles = numFillHoles;
		}

		public int getMaxMinutiaDelta() {
			return maxMinutiaDelta;
		}

		public void setMaxMinutiaDelta(int maxMinutiaDelta) {
			this.maxMinutiaDelta = maxMinutiaDelta;
		}

		public double getMaxHighCurveTheta() {
			return maxHighCurveTheta;
		}

		public void setMaxHighCurveTheta(double maxHighCurveTheta) {
			this.maxHighCurveTheta = maxHighCurveTheta;
		}

		public int getHighCurveHalfContour() {
			return highCurveHalfContour;
		}

		public void setHighCurveHalfContour(int highCurveHalfContour) {
			this.highCurveHalfContour = highCurveHalfContour;
		}

		public int getMinLoopLen() {
			return minLoopLen;
		}

		public void setMinLoopLen(int minLoopLen) {
			this.minLoopLen = minLoopLen;
		}

		public double getMinLoopAspectDist() {
			return minLoopAspectDist;
		}

		public void setMinLoopAspectDist(double minLoopAspectDist) {
			this.minLoopAspectDist = minLoopAspectDist;
		}

		public double getMinLoopAspectRatio() {
			return minLoopAspectRatio;
		}

		public void setMinLoopAspectRatio(double minLoopAspectRatio) {
			this.minLoopAspectRatio = minLoopAspectRatio;
		}

		public int getLinkTableDim() {
			return linkTableDim;
		}

		public void setLinkTableDim(int linkTableDim) {
			this.linkTableDim = linkTableDim;
		}

		public int getMaxLinkDist() {
			return maxLinkDist;
		}

		public void setMaxLinkDist(int maxLinkDist) {
			this.maxLinkDist = maxLinkDist;
		}

		public int getMinThetaDist() {
			return minThetaDist;
		}

		public void setMinThetaDist(int minThetaDist) {
			this.minThetaDist = minThetaDist;
		}

		public int getMaxtrans() {
			return maxTrans;
		}

		public void setMaxTrans(int maxTrans) {
			this.maxTrans = maxTrans;
		}

		public double getScoreThetaNorm() {
			return scoreThetaNorm;
		}

		public void setScoreThetaNorm(double scoreThetaNorm) {
			this.scoreThetaNorm = scoreThetaNorm;
		}

		public double getScoreDistNorm() {
			return scoreDistNorm;
		}

		public void setScoreDistNorm(double scoreDistNorm) {
			this.scoreDistNorm = scoreDistNorm;
		}

		public double getScoreDistWeight() {
			return scoreDistWeight;
		}

		public void setScoreDistWeight(double scoreDistWeight) {
			this.scoreDistWeight = scoreDistWeight;
		}

		public double getScoreNumerator() {
			return scoreNumerator;
		}

		public void setScoreNumerator(double scoreNumerator) {
			this.scoreNumerator = scoreNumerator;
		}

		public int getMaxRmTestDist() {
			return maxRmTestDist;
		}

		public void setMaxRmTestDist(int maxRmTestDist) {
			this.maxRmTestDist = maxRmTestDist;
		}

		public int getMaxHookLen() {
			return maxHookLen;
		}

		public void setMaxHookLen(int maxHookLen) {
			this.maxHookLen = maxHookLen;
		}

		public int getMaxHalfLoop() {
			return maxHalfLoop;
		}

		public void setMaxHalfLoop(int maxHalfLoop) {
			this.maxHalfLoop = maxHalfLoop;
		}

		public int getTransDirPixel() {
			return transDirPixel;
		}

		public void setTransDirPixel(int transDirPixel) {
			this.transDirPixel = transDirPixel;
		}

		public int getSmallLoopLen() {
			return smallLoopLen;
		}

		public void setSmallLloopLen(int smallLoopLen) {
			this.smallLoopLen = smallLoopLen;
		}

		public int getSideHalfContour() {
			return sideHalfContour;
		}

		public void setSideHalfContour(int sideHalfContour) {
			this.sideHalfContour = sideHalfContour;
		}

		public int getInvBlockMargin() {
			return invBlockMargin;
		}

		public void setInvBlockMargin(int invBlockMargin) {
			this.invBlockMargin = invBlockMargin;
		}

		public int getRmValidNbrMin() {
			return rmValidNbrMin;
		}

		public void setRmValidNbrMin(int rmValidNbrMin) {
			this.rmValidNbrMin = rmValidNbrMin;
		}

		public int getMaxOverlapDist() {
			return maxOverlapDist;
		}

		public void setMaxOverlapDist(int maxOverlapDist) {
			this.maxOverlapDist = maxOverlapDist;
		}

		public int getMaxOverlapJoinDist() {
			return maxOverlapJoinDist;
		}

		public void setMaxOverlapJoinDist(int maxOverlapJoinDist) {
			this.maxOverlapJoinDist = maxOverlapJoinDist;
		}

		public int getMalformationSteps1() {
			return malformationSteps1;
		}

		public void setMalformationSteps1(int malformationSteps1) {
			this.malformationSteps1 = malformationSteps1;
		}

		public int getMalformationSteps2() {
			return malformationSteps2;
		}

		public void setMalformationSteps2(int malformationSteps2) {
			this.malformationSteps2 = malformationSteps2;
		}

		public double getMinMalformationRatio() {
			return minMalformationRatio;
		}

		public void setMinMalformationRatio(double minMalformationRatio) {
			this.minMalformationRatio = minMalformationRatio;
		}

		public int getMaxMalformationDist() {
			return maxMalformationDist;
		}

		public void setMaxMalformationDist(int maxMalformationDist) {
			this.maxMalformationDist = maxMalformationDist;
		}

		public int getPoresTransR() {
			return poresTransR;
		}

		public void setPoresTransR(int poresTransR) {
			this.poresTransR = poresTransR;
		}

		public int getPoresPerpSteps() {
			return poresPerpSteps;
		}

		public void setPoresPerpSteps(int poresPerpSteps) {
			this.poresPerpSteps = poresPerpSteps;
		}

		public int getPoresStepsFwd() {
			return poresStepsFwd;
		}

		public void setPoresStepsFwd(int poresStepsFwd) {
			this.poresStepsFwd = poresStepsFwd;
		}

		public int getPoresStepsBwd() {
			return poresStepsBwd;
		}

		public void setPoresStepsBwd(int poresStepsBwd) {
			this.poresStepsBwd = poresStepsBwd;
		}

		public double getPoresMinDist2() {
			return poresMinDist2;
		}

		public void setPoresMinDist2(double poresMinDist2) {
			this.poresMinDist2 = poresMinDist2;
		}

		public double getPoresMaxRatio() {
			return poresMaxRatio;
		}

		public void setPoresMaxRatio(double poresMaxRatio) {
			this.poresMaxRatio = poresMaxRatio;
		}

		public int getMaxNbrs() {
			return maxNbrs;
		}

		public void setMaxNbrs(int maxNbrs) {
			this.maxNbrs = maxNbrs;
		}

		public int getMaxRidgeSteps() {
			return maxRidgeSteps;
		}

		public void setMaxRidgeSteps(int maxRidgeSteps) {
			this.maxRidgeSteps = maxRidgeSteps;
		}
*/
		@Override
		public String toString() {
			return "LfsParams [padValue=" + padValue + ", joinLineRadius=" + joinLineRadius + ", blockOffsetSize="
					+ blockOffsetSize + ", windowSize=" + windowSize + ", windowOffset=" + windowOffset
					+ ", numDirections=" + numDirections + ", startDirAngle=" + startDirAngle + ", rmvValidNbrMin="
					+ rmvValidNbrMin + ", dirStrengthMin=" + dirStrengthMin + ", dirDistanceMax=" + dirDistanceMax
					+ ", smoothValidNbrMin=" + smoothValidNbrMin + ", vortValidNbrMin=" + vortValidNbrMin
					+ ", highcurvVorticityMin=" + highcurvVorticityMin + ", highcurvCurvatureMin="
					+ highcurvCurvatureMin + ", minInterpolateNbrs=" + minInterpolateNbrs + ", percentileMinMax="
					+ percentileMinMax + ", minContrastDelta=" + minContrastDelta + ", numDftWaves=" + numDftWaves
					+ ", powmaxMin=" + powmaxMin + ", pownormMin=" + pownormMin + ", powmaxMax=" + powmaxMax
					+ ", forkInterval=" + forkInterval + ", forkPctPowmax=" + forkPctPowmax + ", forkPctPownorm="
					+ forkPctPownorm + ", dirbinGridWidth=" + dirbinGridWidth + ", dirbinGridHeight=" + dirbinGridHeight
					+ ", isoBinGridDim=" + isoBinGridDim + ", numFillHoles=" + numFillHoles + ", maxMinutiaDelta="
					+ maxMinutiaDelta + ", maxHighCurveTheta=" + maxHighCurveTheta + ", highCurveHalfContour="
					+ highCurveHalfContour + ", minLoopLen=" + minLoopLen + ", minLoopAspectDist=" + minLoopAspectDist
					+ ", minLoopAspectRatio=" + minLoopAspectRatio + ", linkTableDim=" + linkTableDim + ", maxLinkDist="
					+ maxLinkDist + ", minThetaDist=" + minThetaDist + ", maxTrans=" + maxTrans + ", scoreThetaNorm="
					+ scoreThetaNorm + ", scoreDistNorm=" + scoreDistNorm + ", scoreDistWeight=" + scoreDistWeight
					+ ", scoreNumerator=" + scoreNumerator + ", maxRmTestDist=" + maxRmTestDist + ", maxHookLen="
					+ maxHookLen + ", maxHalfLoop=" + maxHalfLoop + ", transDirPixel=" + transDirPixel
					+ ", smallLoopLen=" + smallLoopLen + ", sideHalfContour=" + sideHalfContour + ", invBlockMargin="
					+ invBlockMargin + ", rmValidNbrMin=" + rmValidNbrMin + ", maxOverlapDist=" + maxOverlapDist
					+ ", maxOverlapJoinDist=" + maxOverlapJoinDist + ", malformationSteps1=" + malformationSteps1
					+ ", malformationSteps2=" + malformationSteps2 + ", minMalformationRatio=" + minMalformationRatio
					+ ", maxMalformationDist=" + maxMalformationDist + ", poresTransR=" + poresTransR
					+ ", poresPerpSteps=" + poresPerpSteps + ", poresStepsFwd=" + poresStepsFwd + ", poresStepsBwd="
					+ poresStepsBwd + ", poresMinDist2=" + poresMinDist2 + ", poresMaxRatio=" + poresMaxRatio
					+ ", maxNbrs=" + maxNbrs + ", maxRidgeSteps=" + maxRidgeSteps + "]";
		}

	}

	/*************************************************************************/
	/* publicAL FUNCTION DEFINITIONS */
	/*************************************************************************/

	/* Binarization.java */
	public interface IBinarization {
		@SuppressWarnings({ "java:S107" })
		public int[] binarize(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
				final RotGrids dirbingrids, final LfsParams lfsParms);

		@SuppressWarnings({ "java:S107" })
		public int[] binarizeV2(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
				final RotGrids dirbingrids, final LfsParams lfsParms);

		@SuppressWarnings({ "java:S107" })
		public int[] binarizeImage(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
				final int imapBlockSize, RotGrids dirbingrids, final int isoBinGridDim);

		@SuppressWarnings({ "java:S107" })
		public int[] binarizeImageV2(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
				final int blockOffsetSize, final RotGrids dirbingrids);

		public int dirbinarize(int[] paddedImageData, final int paddedImageIndex, final int imapDirection,
				final RotGrids dirbingrids);

		public int isoBinarize(int[] paddedImageData, final int paddedImageIndex, final int paddedImageWidth,
				final int paddedImageHeight, final int isoBinGridDim);
	}

	/* Block.java */
	public interface IBlock {
		public AtomicIntegerArray blockOffsets(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				final int imageWidth, final int imageHeight, final int pad, final int blockOffsetSize);

		public int lowContrastBlock(final int blockOffset, final int blockOffsetSize, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int findValidBlock(AtomicInteger nbrDir, AtomicInteger nbrX, AtomicInteger nbrY,
				AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap, final int startX, final int startY,
				final int mappedImageWidth, final int mappedImageHeight, final int xIncr, final int yIncr);

		public void setMarginBlocks(AtomicIntegerArray map, final int mappedImageWidth, final int mappedImageHeight,
				final int marginValue);
	}

	/* ChainCode.java */
	public interface IChainCode {
		public int chainCodeLoop(AtomicIntegerArray oVectorChainCodes, AtomicInteger oNoOfCodesInChain,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, int noOfPointsInContour);

		public int isChainClockwise(AtomicIntegerArray oVectorChainCodes, int noOfCodesInChain, int defaultRetCode);
	}

	/* Contour.java */
	public interface IContour {
		public IContour allocateContour(AtomicInteger ret, final int noOfPointsInContour);

		public void freeContour(Contour contour);

		@SuppressWarnings({ "java:S107" })
		public IContour getHighCurvatureContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public IContour getCenteredContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public IContour traceContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int maxLenOfContour,
				final int xLoop, final int yLoop, final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc, final int scanClock, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public int searchContour(final int xPixelSearch, final int yPixelSearch, final int searchLen,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public int nextContourPixel(AtomicInteger nextXPixelLoc, AtomicInteger nextYPixelLoc,
				AtomicInteger nextXEdgePixelLoc, AtomicInteger nextYEdgePixelLoc, final int cur_x_loc,
				final int cur_y_loc, final int currentXEdgePixelLoc, final int currentYEdgePixelLoc,
				final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		public int startScanNbr(final int previousXPixelLoc, final int previousYPixelLoc, final int nextXPixelLoc,
				final int nextYPixelLoc);

		public int nextScanNbr(final int nbrIndex, final int scanClock);

		public int minContourTheta(AtomicInteger oMinContourPoint, AtomicReference<Double> oMinThetaAngle,
				final int angleEdge, AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour);

		public void contourLimits(AtomicInteger xMin, AtomicInteger yMin, AtomicInteger xMax, AtomicInteger yMax,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfPointsInContour);

		public void fixEdgePixelPair(AtomicInteger featureXPixel, AtomicInteger featureYPixel,
				AtomicInteger featureEdgeXPixel, AtomicInteger featureEdgeYPixel, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);
	}

	/* Detect.java */
	public interface IDetect {
		@SuppressWarnings({ "java:S107" })
		public int[] lfsDetectMinutiaeV2(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps map,
				AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, int[] imageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);
	}

	/* Dft.java */
	public interface IDft {
		public int dftDirPowers(AtomicReferenceArray<Double[]> powers, int[] paddedImageData, final int blockOffset,
				final int paddedImageWidth, final int paddedImageHeight, DftWaves dftWaves, RotGrids dftGrids);

		public void sumRotBlockRows(int[] rowSums, int[] paddedImageData, final int paddedImageDataIndex,
				final AtomicIntegerArray gridOffsets, final int blockOffsetSize);

		public void computeDftPower(AtomicReference<Double> power, final int[] rowSums, final DftWave dftWave,
				final int waveLen);

		@SuppressWarnings({ "java:S107" })
		public int getDftPowerStats(AtomicIntegerArray wis, AtomicReferenceArray<Double> powMaxs,
				AtomicIntegerArray powmaxDirs, AtomicReferenceArray<Double> powNorms,
				AtomicReferenceArray<Double[]> powers, final int fw, final int tw, final int nDirs);

		public void getMaxNorm(AtomicReference<Double> powmax, AtomicInteger powmaxDir, AtomicReference<Double> pownorm,
				final AtomicReferenceArray<Double> powerVector, final int nDirs);

		public int sortDftWaves(AtomicIntegerArray wis, final AtomicReferenceArray<Double> powMaxs,
				final AtomicReferenceArray<Double> powNorms, final int nStats);
	}

	/* Free.java */
	public interface IFree {
		public void free(Object object);

		public void freeDirToRad(DirToRad dir2Rad);

		public void freeDftWaves(DftWaves dftWaves);

		public void freeRotGrids(RotGrids rotGrids);

		public void freeDirPowers(AtomicReferenceArray<Double[]> powers, final int nWaves);
	}

	/* GetMinutiae.java */
	public interface IGetMinutiae {
		@SuppressWarnings({ "java:S107" })
		public int[] getMinutiae(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps imageMap,
				Quality qualityMap, AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight,
				AtomicInteger oBinarizedImageDepth, int[] imageData, final int imageWidth, final int imageHeight,
				final int imageDepth, final double imagePPI, final LfsParams lfsParams);
	}

	/* ImageUtil.java */
	public interface IImageUtil {
		public void bits6To8(int[] imageData, int imageWidth, int imageHeight);

		public void bits8To6(int[] imageData, int imageWidth, int imageHeight);

		public void grayToBinary(final int thresh, final int less_pix, final int greater_pix, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public int[] padImage(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight, int[] imageData,
				final int imageWidth, final int imageHeight, final int pad, final int padValue);

		public void fillHoles(int[] binarizedImageData, final int imageWidth, final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public int freePath(final int x1, final int y1, final int x2, final int y2, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int searchInDirection(AtomicInteger ox, AtomicInteger oy, AtomicInteger oex, AtomicInteger oey,
				final int pix, final int strt_x, final int strt_y, final double delta_x, final double delta_y,
				final int maxsteps, int[] binarizedImageData, final int imageWidth, final int imageHeight);
	}

	/* init.c */
	public interface IInit {
		public int getMaxPadding(final int imapBlockSize, final int dirbinGridWidth, final int dirbinGridHeight,
				final int isoBinGridDim);

		public int getMaxPaddingV2(final int mapWindowSize, final int map_windowoffset, final int dirbinGridWidth,
				final int dirbinGridHeight);

		public int initDirToRad(DirToRad optr);

		public int initDftWaves(DftWaves optr, AtomicReferenceArray<Double> dftCoefs);

		public int initRotGrids(RotGrids optr, final int imageWidth, final int imageHeight, final int ipad);

		public AtomicReferenceArray<Double[]> allocDirPowers(AtomicInteger ret, final int nWaves, final int nDirs);

		public AtomicIntegerArray allocPowerStatsWis(AtomicInteger ret, final int nStats);

		public AtomicReferenceArray<Double> allocPowerStatsPowmaxs(AtomicInteger ret, final int nStats);

		public AtomicIntegerArray allocPowerStatsPowmaxDirs(AtomicInteger ret, final int nStats);

		public AtomicReferenceArray<Double> allocPowerStatsPownorms(AtomicInteger ret, final int nStats);
	}

	/* IsEmpty.java */
	public interface IIsEmpty {
		public int isImageEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight);

		public int isQualityMapEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight);
	}

	/* Line.java */
	public interface ILine {
		public int linePoints(int[] oxList, int[] oyList, AtomicInteger onum, final int x1, final int y1, final int x2,
				final int y2);
	}

	/* Link.java */
	public interface ILink {
		@SuppressWarnings({ "java:S107" })
		public int linkMinutiae(Minutiae oMinutiae, String a, final int b, final int c, AtomicInteger d, final int e,
				final int f, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int createLinkTable(AtomicIntegerArray a, AtomicIntegerArray b, AtomicIntegerArray c, AtomicInteger d,
				AtomicInteger e, AtomicInteger f, final int g, final int h, final Minutiae oMinutiae,
				final AtomicInteger i, AtomicInteger j, final int k, final int l, String m, final int n, final int o,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int updateLinkTable(AtomicInteger a, AtomicInteger b, AtomicInteger c, AtomicInteger d, AtomicInteger e,
				AtomicInteger f, final int g, AtomicInteger h, AtomicInteger i, AtomicInteger j, AtomicInteger k,
				final int l, final int m, final int n);

		@SuppressWarnings({ "java:S107" })
		public int orderLinkTable(AtomicInteger a, AtomicInteger b, AtomicInteger c, final int d, final int e,
				final int f, final int g, final Minutiae oMinutiae, final int h);

		@SuppressWarnings({ "java:S107" })
		public int processLinkTable(final AtomicInteger a, final AtomicInteger b, final AtomicInteger c, final int d,
				final int e, final int f, final int g, Minutiae oMinutiae, AtomicInteger h, String i, final int j,
				final int k, final LfsParams lfsParams);

		public double linkScore(final double a, final double b, final LfsParams lfsParams);
	}

	/* LfsUtil.java */
	public interface ILfsUtil {
		public int maxValue(final AtomicIntegerArray list, final int num);

		public int minValue(final AtomicIntegerArray list, final int num);

		public int minMaxs(AtomicIntegerArray oMinMaxValue, AtomicIntegerArray oMinMaxType,
				AtomicIntegerArray oMinMaxIndex, AtomicInteger oMinMaxAlloc, AtomicInteger oMinMaxNumber,
				AtomicIntegerArray items, final int num);

		public double distance(final int x1, final int y1, final int x2, final int y2);

		public double squaredDistance(final int x1, final int y1, final int x2, final int y2);

		public int getValueLocationInList(final int item, AtomicIntegerArray list, final int len);

		public int removeValueFromLocationInList(final int index, AtomicIntegerArray list, final int num);

		public int findIncrementalPositionInDoubleArray(final double val, AtomicReferenceArray<Double> list,
				final int num);

		public double angleToLine(final int fx, final int fy, final int tx, final int ty);

		public int lineToDirection(final int fx, final int fy, final int tx, final int ty, final int nDirs);

		public int closestDirDistance(final int dir1, final int dir2, final int nDirs);
	}

	/* Loop.java */
	public interface ILoop {
		public int getLoopList(AtomicIntegerArray onloop, AtomicReference<Minutiae> oMinutiae, final int loopLen,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		public int onLoop(final Minutia minutia, final int max_loop_len, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public IContour onIslandLake(AtomicInteger ret, AtomicInteger oNoOfContour, Minutia minutia1, Minutia minutia2,
				final int maxHalfLoop, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		public int onHook(Minutia minutia1, Minutia minutia2, final int maxHookLen, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);

		public int isLoopClockwise(AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour, final int default_ret);

		@SuppressWarnings({ "java:S107" })
		public int processLoop(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray contourX,
				AtomicIntegerArray contourY, AtomicIntegerArray contourEx, AtomicIntegerArray contourEy,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int processLoopV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray contourX,
				AtomicIntegerArray contourY, AtomicIntegerArray contourEx, AtomicIntegerArray contourEy,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				AtomicIntegerArray plowFlowMap, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public void getLoopAspect(AtomicInteger ominFr, AtomicInteger ominTo, AtomicReference<Double> ominDist,
				AtomicInteger omaxFr, AtomicInteger omaxTo, AtomicReference<Double> omaxDist,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfPointsInContour);

		public int fillLoop(final AtomicIntegerArray a, final AtomicIntegerArray b, final int c, int[] d, final int e,
				final int f);

		public void fillPartialRow(final int fill_pix, final int frx, final int tox, final int y,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		public void floodLoop(final AtomicIntegerArray contourX, final AtomicIntegerArray contourY,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		public void floodFill4(final int fill_pix, final int x, final int y, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);
	}

	/* Maps.java */
	public interface IMaps {
		public int genImageMaps(int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				DirToRad dir2Rad, DftWaves dftWaves, RotGrids dftgrids, LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int initialiseMaps(AtomicIntegerArray odmap, AtomicIntegerArray olcmap, AtomicIntegerArray olfmap,
				AtomicIntegerArray blkoffs, final int mappedImageWidth, final int mappedImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves,
				final RotGrids dftGrids, final LfsParams lfsParams);

		public int interpolateDirectionMap(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		public int morphMapWithTF(AtomicIntegerArray tfmap, final LfsParams lfsParams);

		public int pixelizeMap(AtomicIntegerArray ret, int imageWidth, int imageHeight, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, final int blockOffsetSize);

		public void smoothDirectionMap(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
				final DirToRad dir2Rad, final LfsParams lfsParams);

		public int generateHighCurveMap(AtomicIntegerArray ohcmap, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public AtomicIntegerArray generateInputBlockImageMap(AtomicInteger ret, AtomicInteger oImageWidth,
				AtomicInteger oImageHeight, int[] paddedImageData, final int paddedImageWidth,
				final int paddedImageHeight, final DirToRad dir2Rad, final DftWaves dftWaves, final RotGrids dftGrids,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public AtomicIntegerArray initialiseInputBlockImageMap(AtomicInteger ret, AtomicIntegerArray blkoffs,
				final AtomicInteger mappedImageWidth, final AtomicInteger mappedImageHeight, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves,
				final RotGrids dftGrids, final LfsParams lfsParams);

		public int primaryDirectionTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
				final AtomicReferenceArray<Double> powMaxs, final AtomicIntegerArray powmaxDirs,
				final AtomicReferenceArray<Double> powNorms, final int nStats, final LfsParams lfsParams);

		public int secondaryForkTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
				final AtomicReferenceArray<Double> powMaxs, final AtomicIntegerArray powmaxDirs,
				final AtomicReferenceArray<Double> powNorms, final int nStats, final LfsParams lfsParams);

		public void removeInconsistentDirs(AtomicIntegerArray imap, final DirToRad dir2Rad, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int testTopEdge(final int lbox, final int tbox, final int rbox, final int bbox, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad, LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int testRightEdge(final int lbox, final int tbox, final int rbox, final int bbox,
				AtomicIntegerArray imap, final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad,
				LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int testBottomEdge(final int lbox, final int tbox, final int rbox, final int bbox,
				AtomicIntegerArray imap, final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad,
				LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int testLeftEdge(final int lbox, final int tbox, final int rbox, final int bbox, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad, LfsParams lfsParams);

		public int removeIMAPDirection(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final DirToRad dir2Rad, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public void average8NbrDir(AtomicInteger avrdir, AtomicReference<Double> dirStrength, AtomicInteger nvalid,
				AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final DirToRad dir2Rad);

		public int numValid8Nbrs(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight);

		public void smoothInputBlockImageMap(AtomicIntegerArray imap, final DirToRad dir2Rad,
				final LfsParams lfsParams);

		public int genNMap(AtomicIntegerArray optr, AtomicIntegerArray imap, final int mappedImageWidth,
				final int mappedImageHeight, final LfsParams lfsParams);

		public int vorticity(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final int nDirs);

		public void accumulateNbrVorticity(AtomicInteger vmeasure, final int dir1, final int dir2, final int nDirs);

		public int curvature(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final int nDirs);
	}

	/* MatchPattern.java */
	public interface IMatchPattern {
		public int matchFirstPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		public int matchSecondPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		public int matchThirdPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		public void skipRepeatedHorizontalPair(AtomicInteger cx, final int ex, int[] binarizedImageData,
				AtomicInteger p1ptr, AtomicInteger p2ptr, final int imageWidth, final int imageHeight);

		public void skipRepeatedVerticalPair(AtomicInteger currentYPixelIndex, final int currentBottomYPixelIndex,
				int[] binarizedImageData, AtomicInteger currentLeftPixelIndex, AtomicInteger currentRightPixelIndex,
				final int imageWidth, final int imageHeight);
	}

	/* MinutiaHelper.java */
	public interface IMinutia {
		public int allocMinutiae(AtomicReference<Minutiae> oMinutiae, final int max_minutiae);

		public int reallocMinutiae(AtomicReference<Minutiae> oMinutiae, final int incr_minutiae);

		public int detectMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int mappedImageWidth, final int mappedImageHeight, Maps map, LfsParams lfsParams);

		public int updateMinutiae(AtomicReference<Minutiae> oMinutiae, Minutia minutia, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int updateMinutiaeV2(AtomicReference<Minutiae> oMinutiae, Minutia minutia, final int scanDir,
				final int dmapval, int[] binarizedImageData, final int mappedImageWidth, final int mappedImageHeight,
				final LfsParams lfsParams);

		public int sortMinutiaeTopToBottomAndThenLeftToRight(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
				final int imageHeight);

		public int sortMinutiaeLeftToRightAndThenTopToBottom(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
				final int imageHeight);

		public int removeRedundantMinutiae(AtomicReference<Minutiae> oMinutiae);

		public void dumpMinutiae(File file, final AtomicReference<Minutiae> oMinutiae);

		public void dumpMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae);

		public void dumpReliableMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae,
				final double reliability);

		@SuppressWarnings({ "java:S107" })
		public Minutia createMinutia(final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc, final int imapDirection, final double reliability, final int type,
				final int appearing, final int featureId);

		public void freeMinutiae(AtomicReference<Minutiae> oMinutiae);

		public void freeMinutia(Minutia minutia);

		public int removeMinutia(final int index, AtomicReference<Minutiae> ominutiae);

		public int joinMinutia(Minutia minutia1, Minutia minutia2, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int with_boundary, final int line_radius);

		public int getMinutiaType(final int feature_pix);

		public int isMinutiaAppearing(final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc);

		public int chooseScanDirection(final int imapval, final int nDirs);

		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiae(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray imap, AtomicIntegerArray mapDirectionArr, final int blkX,
				final int blkY, final int mappedImageWidth, final int mappedImageHeight, final int scanX,
				final int scanY, final int scanW, final int scanH, final int scanDir, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final int imapval, final int nmapval, final int scanX,
				final int scanY, final int scanW, final int scanH, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeHorizontallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray pdirectionMap,
				AtomicIntegerArray plowFlowMap, AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeVertically(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final int imapval, final int nmapval, final int scanX,
				final int scanY, final int scanW, final int scanH, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int rescanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeVerticallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray pdirectionMap,
				AtomicIntegerArray plowFlowMap, AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int rescanForMinutiaeVertically(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int rescanPartialHorizontally(final int nbrDir, AtomicReference<Minutiae> oMinutiae,
				int[] binarizedImageData, final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int rescanPartialVertically(final int nbrDir, AtomicReference<Minutiae> oMinutiae,
				int[] binarizedImageData, final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		public int getNbrBlockIndex(AtomicInteger blki, final int nbrDir, final int blkx, final int blky,
				final int mappedImageWidth, final int mappedImageHeight);

		@SuppressWarnings({ "java:S107" })
		public int adjustHorizontalRescan(final int a, AtomicInteger b, AtomicInteger c, AtomicInteger d,
				AtomicInteger e, final int f, final int g, final int h, final int i, final int j);

		@SuppressWarnings({ "java:S107" })
		public int adjustVerticalRescan(final int nbrDir, AtomicInteger rescanX, AtomicInteger rescanY,
				AtomicInteger rescanW, AtomicInteger rescanH, final int scanX, final int scanY, final int scanW,
				final int scanH, final int blockOffsetSize);

		@SuppressWarnings({ "java:S107" })
		public int processHorizontalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int x2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int imapval, final int nmapval, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int processHorizontalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int x2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray pdirectionMap, AtomicIntegerArray plowFlowMap,
				AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int processVerticalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int y2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int imapval, final int nmapval, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int processVerticalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int y2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray pdirectionMap, AtomicIntegerArray plowFlowMap,
				AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int adjustHighCurvatureMinutia(AtomicInteger oidir, AtomicInteger oxLoc, AtomicInteger oyLoc,
				AtomicInteger oxEdge, AtomicInteger oyEdge, final int xPixelLoc, final int yPixelLoc,
				final int xEdgePixelLoc, final int yEdgePixelLoc, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicReference<Minutiae> oMinutiae, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int adjustHighCurvatureMinutiaV2(AtomicInteger oidir, AtomicInteger oxLoc, AtomicInteger oyLoc,
				AtomicInteger oxEdge, AtomicInteger oyEdge, final int xPixelLoc, final int yPixelLoc,
				final int xEdgePixelLoc, final int yEdgePixelLoc, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray plowFlowMap, AtomicReference<Minutiae> oMinutiae,
				final LfsParams lfsParams);

		public int getLowCurvatureDirection(final int a, final int b, final int c, final int d);
	}

	/* Morph.java */
	public interface IMorph {
		public void erodeImage2(int[] inputImageData, int[] outputImageData, final int imageWidth,
				final int imageHeight);

		public void dilateImage2(int[] inputImageData, int[] outputImageData, final int imageWidth,
				final int imageHeight);

		public int getSouth82(int[] inputImageData, int inputImageDataIndex, final int row, final int imageWidth,
				final int imageHeight, final int failCode);

		public int getNorth82(int[] inputImageData, int inputImageDataIndex, final int row, final int imageWidth,
				final int failCode);

		public int getEast82(int[] inputImageData, int inputImageDataIndex, final int col, final int imageWidth,
				final int failCode);

		public int getWest82(int[] inputImageData, int inputImageDataIndex, final int col, final int failCode);
	}

	/* Quality.java */
	public interface IQuality {
		public int generateQualityMap(Maps map);

		@SuppressWarnings({ "java:S107" })
		public int combinedMinutiaQuality(AtomicReference<Minutiae> oMinutiae, Maps map, final int blockOffsetSize,
				int[] imageData, final int imageWidth, final int imageHeight, final int imageDepth,
				final double imagePPI);

		double grayscaleReliability(Minutia minutia, int[] imageData, final int imageWidth, final int imageHeight,
				final int radiusPixel);

		public void getNeighborhoodStats(AtomicReference<Double> mean, AtomicReference<Double> stdev, Minutia minutia,
				int[] imageData, final int imageWidth, final int imageHeight, final int radiusPixel);

		public int reliabilityFromQualityMap(Minutiae oMinutiae, Maps map, final int imageWidth, final int imageHeight,
				final int blockOffsetSize);
	}

	/* RemoveMinutia.java */
	public interface IRemoveMinutia {
		@SuppressWarnings({ "java:S107" })
		public int removeFalseMinutiaV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, Maps map, final int mappedImageWidth,
				final int mappedImageHeight, final LfsParams lfsParams);

		public int removeHoles(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		public int removeHooks(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		public int removeHooksIslandsLakesOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		public int removeIslandsAndLakes(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int removeMalformations(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, AtomicIntegerArray lowFlowMap, int mappedImageWidth, int mappedImageHeight,
				LfsParams lfsParams);

		public int removeNearInvblocksV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		public int removePointingInvblockV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		public int removeOverlaps(AtomicReference<Minutiae> oMinutiae, int[] a, final int b, final int c,
				final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int removePoresV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, AtomicIntegerArray directionMap, AtomicIntegerArray lowFlowMap,
				AtomicIntegerArray highCurveMap, int mappedImageWidth, int mappedImageHeight, LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int removeOrAdjustSideMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);
	}

	/* Results.java */
	public interface IResults {
		@SuppressWarnings({ "java:S107" })
		public int writeTextResults(File file, final int m1flag, final int imageWidth, final int imageHeight,
				final AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oQualityMap,
				AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap, AtomicIntegerArray oLowFlowMap,
				AtomicIntegerArray oHighCurveMap, final int mapWidth, final int mapHeight);

		public int writeMinutiaeXYTQ(File file, final int repType, final AtomicReference<Minutiae> oMinutiae,
				final int imageWidth, final int imageHeight);

		public void dumpMap(File file, AtomicIntegerArray oMap, final int mapWidth, final int mapHeight)
				throws IOException;

		@SuppressWarnings({ "java:S107" })
		public int drawInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, final int mapWidth,
				final int mapHeight, int[] imageData, final int imageWidth, final int imageHeight,
				final RotGrids rotGrids, final int drawPixel);

		@SuppressWarnings({ "java:S107" })
		public void drawInputBlockImageMap2(AtomicIntegerArray oInputBlockImageMap,
				final AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, final double startAngle, final int nDirs,
				final int blocksize);

		public void drawBlocks(final AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, final int drawPixel);

		public int drawRotGrid(final RotGrids rotGrids, final int nDir, int[] imageData, final int blockOffset,
				final int imageWidth, final int imageHeight, final int drawPixel);

		@SuppressWarnings({ "java:S107" })
		public void dumpLinkTable(File file, final int[] linkTable, final int[] xAxis, final int[] yAxis,
				final int nxAxis, final int nyAxis, final int tblDim, final AtomicReference<Minutiae> oMinutiae);

		@SuppressWarnings({ "java:S107" })
		public int drawDirectionMap(StringBuilder fileName, AtomicIntegerArray oDirectionMap,
				AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight, final int blocksize,
				int[] imageData, final int imageWidth, final int imageHeight, final int flag);

		@SuppressWarnings({ "java:S107" })
		public int drawTFMap(StringBuilder fileName, AtomicIntegerArray oMap, AtomicIntegerArray oBlockOffsets,
				final int mapWidth, final int mapHeight, final int blocksize, int[] imageData, final int imageWidth,
				final int imageHeight, final int flag);
	}

	/* Ridges.java */
	public interface IRidges {
		public int countMinutiaeRidges(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		public int countMinutiaRidges(final int first, AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		public int findNeighbors(AtomicIntegerArray oNbrList, AtomicInteger oNoOfNbrs, final int maxNbrs,
				final int first, AtomicReference<Minutiae> oMinutiae);

		public int updateNbrDists(AtomicIntegerArray nbrList, AtomicReferenceArray<Double> nbrSqrDists,
				AtomicInteger noOfNbrs, final int maxNbrs, final int first, final int second,
				AtomicReference<Minutiae> oMinutiae);

		public int insertNeighbor(final int nbrListPos, final int nbrIndex, final double nbrDist2,
				AtomicIntegerArray nbrList, AtomicReferenceArray<Double> nbrSqrDists, AtomicInteger noOfNbrs,
				final int maxNbrs);

		public int sortNeighbors(AtomicIntegerArray nbrList, final int noOfNbrs, final int firstMinutiaIndex,
				AtomicReference<Minutiae> oMinutiae);

		public int ridgeCount(final int firstMinutiaIndex, final int secondMinutiaIndex,
				AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		@SuppressWarnings({ "java:S107" })
		public int findTransition(AtomicInteger startPixel, final int firstPixel, final int secondPixel,
				final int[] xlist, final int[] ylist, final int num, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		@SuppressWarnings({ "java:S107" })
		public int validateRidgeCrossing(final int ridgeStart, final int ridgeEnd, final int[] xlist, final int[] ylist,
				final int num, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				final int maxRidgeSteps);
	}

	/* Shapes.java */
	public interface IShapes {
		public Shape allocShape(AtomicInteger ret, int xMin, int yMin, int xMax, int yMax);

		public void freeShape(Shape shape);

		public void dumpShape(File file, final Shape shape);

		public Shape shapeFromContour(AtomicInteger ret, AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour);

		public void sortRowLeftToRightOnX(Rows row);
	}

	/* sort.c */
	public interface ISort {
		public int sortIndicesIntArrayIncremental(AtomicIntegerArray order, AtomicIntegerArray ranks, final int num);

		public int sortIndicesDoubleArrayIncremental(AtomicIntegerArray order, AtomicReferenceArray<Double> ranks,
				final int num);

		public void bubbleSortIntArrayIncremental2(AtomicIntegerArray ranks, AtomicIntegerArray items, final int len);

		public void bubbleSortDoubleArrayIncremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
				final int len);

		public void bubbleSortDoubleArrayDecremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
				final int len);

		public void bubbleSortIntArrayIncremental(AtomicIntegerArray ranks, final int len);
	}
}