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

/**
 * Constants, data structures and component contracts of the NIST Latent Fingerprint System (LFS) used by
 * MINDTCT minutiae detection (port of NIST's {@code lfs.h}).
 * <p>
 * Defines the error codes, tuning thresholds (map, DFT, binarization, minutia detection/linking/removal, ridge
 * counting, quality) that make up the default {@link LfsParams}, the core data types ({@link Minutia},
 * {@link Minutiae}, {@link RotGrids}, {@link DftWaves}, {@link Shape}, ...), and one nested interface per
 * MINDTCT source module ({@link IBinarization}, {@link IMaps}, {@link IMinutia}, {@link IRemoveMinutia}, ...).
 * <p>
 * Unless noted otherwise, functions return {@code 0} ({@link #FALSE}) on success and a negative value on
 * system error; image buffers are row-major with one pixel per array element.
 */
public interface ILfs {
	/*************************************************************************/
	/* ERROR CODES */
	/*************************************************************************/
	/** Error code: input image pixel depth is not 8 bits ({@code get_minutiae}). */
	public static final int ERROR_CODE_02 = -2;// et_minutiae : input image pixel Depth != 8
	/** Error code: rotated grids allocation returned null ({@code init_rotgrids}). */
	public static final int ERROR_CODE_33 = -33;// init_rotgrids : rotgrids.grids() : Null
	/** Error code: DFT grids must be square ({@code generateInputBlockImageMap}). */
	public static final int ERROR_CODE_60 = -60;// generateInputBlockImageMap : DFT grids must be square
	/** Error code: IMAP could not be allocated ({@code initialiseInputBlockImageMap}). */
	public static final int ERROR_CODE_70 = -70;// initialiseInputBlockImageMap : imap : NULL
	/** Error code: block offsets computation failed. */
	public static final int ERROR_CODE_80 = -80;// BLOCK OFFSET
	/** Error code: row sums could not be allocated ({@code dftDirPowers}). */
	public static final int ERROR_CODE_91 = -91;// dftDirPowers : rowSums : Null
	/** Error code: normalized power array could not be allocated ({@code sort_dft_waves}). */
	public static final int ERROR_CODE_100 = -100;// sort_dft_waves : powNorms2 : NULL
	/** Error code: binarized image could not be allocated ({@code binarizeImage}). */
	public static final int ERROR_CODE_110 = -110;// binarizeImage : binarizedImageData : null
	/** Error code: input direction exceeds the range of rotated grids ({@code drawRotGrid}). */
	public static final int ERROR_CODE_140 = -140;// drawRotGrid : input direction exceeds range of rotated grids
	/** Error code: illegal neighbor direction ({@code getNbrBlockIndex}). */
	public static final int ERROR_CODE_200 = -200;// getNbrBlockIndex : illegal neighbor direction
	/** Error code: illegal neighbor direction ({@code adjustHorizontalRescan}). */
	public static final int ERROR_CODE_210 = -210;// adjustHorizontalRescan : illegal neighbor direction
	/** Error code: illegal neighbor direction ({@code adjustVerticalRescan}). */
	public static final int ERROR_CODE_220 = -220;// adjustVerticalRescan : illegal neighbor direction
	/** Error code: bad configuration of pixels ({@code isMinutiaAppearing}). */
	public static final int ERROR_CODE_240 = -240;// isMinutiaAppearing : bad configuration of pixels
	/** Error code: invalid direction ({@code removeHooksIslandsLakesOverlaps}). */
	public static final int ERROR_CODE_301 = -301;// removeHooksIslandsLakesOverlaps : INVALID direction
	/** Error code: minutia index out of range ({@code removeMinutia}). */
	public static final int ERROR_CODE_380 = -380;// removeMinutia : index out of range
	/** Error code: coordinate list overflow ({@code linePoints}). */
	public static final int ERROR_CODE_412 = -412;// linePoints : coord list overflow
	/** Error code: illegal position for a new neighbor ({@code updateNbrDists}). */
	public static final int ERROR_CODE_470 = -470;// updateNbrDists : illegal position for new neighbor
	/** Error code: inserting a neighbor failed ({@code updateNbrDists}). */
	public static final int ERROR_CODE_471 = -471;// updateNbrDists : insert neighbor failed
	/** Error code: insertion point exceeds the neighbor lists ({@code insertNeighbor}). */
	public static final int ERROR_CODE_480 = -480;// insertNeighbor : insertion point exceeds lists
	/** Error code: overflow in the neighbor lists ({@code insertNeighbor}). */
	public static final int ERROR_CODE_481 = -481;// insertNeighbor : overflow in neighbor lists
	/** Error code: minimum percentile pixel not found ({@code lowContrastBlock}). */
	public static final int ERROR_CODE_510 = -510;// lowContrastBlock : min percentile pixel not found
	/** Error code: maximum percentile pixel not found ({@code lowContrastBlock}). */
	public static final int ERROR_CODE_511 = -511;// lowContrastBlock : max percentile pixel not found
	/** Error code: DFT grids must be square ({@code genImageMaps}). */
	public static final int ERROR_CODE_540 = -540;// genImageMaps : DFT grids must be square
	/** Error code: binary image has bad dimensions ({@code lfsDetectMinutiaeV2}). */
	public static final int ERROR_CODE_581 = -581;// lfsDetectMinutiaeV2 : binary image has bad dimensions
	/** Error code: block dimensions do not match ({@code pixelizeMap}). */
	public static final int ERROR_CODE_591 = -591;// pixelizeMap : block dimensions do not match
	/** Error code: invalid direction ({@code removeIslandsAndLakes}). */
	public static final int ERROR_CODE_611 = -611;// removeIslandsAndLakes : INVALID direction
	/** Error code: margin too large for block size ({@code removeNearInvblocksV2}). */
	public static final int ERROR_CODE_620 = -620;// removeNearInvblocksV2 : margin too large for blocksize
	/** Error code: invalid direction ({@code removeHooks}). */
	public static final int ERROR_CODE_641 = -641;// removeHooks : INVALID direction
	/** Error code: invalid direction ({@code removeOverlaps}). */
	public static final int ERROR_CODE_651 = -651;// removeOverlaps : INVALID direction

	/*************************************************************************/
	/* OUTPUT FILE EXTENSIONS */
	/*************************************************************************/
	/** Output file extension for the minutiae text file. */
	public static final String MIN_TXT_EXT = "min";
	/** Output file extension for the low-contrast map. */
	public static final String LOW_CONTRAST_MAP_EXT = "lcm";
	/** Output file extension for the high-curvature map. */
	public static final String HIGH_CURVE_MAP_EXT = "hcm";
	/** Output file extension for the direction map. */
	public static final String DIRECTION_MAP_EXT = "dm";
	/** Output file extension for the low-flow map. */
	public static final String LOW_FLOW_MAP_EXT = "lfm";
	/** Output file extension for the quality map. */
	public static final String QUALITY_MAP_EXT = "qm";
	/** Output file extension for the ANSI/NIST (AN2K) minutiae output. */
	public static final String AN2K_OUT_EXT = "mdt";
	/** Output file extension for the binarized image. */
	public static final String BINARY_IMG_EXT = "brw";
	/** Output file extension for the minutiae X/Y/theta file. */
	public static final String XYT_EXT = "xyt";

	/*************************************************************************/
	/* Minutiae XYT REPRESENTATION SCHEMES */
	/*************************************************************************/
	/** Minutiae XYT representation: NIST internal convention. */
	public static final int NIST_INTERNAL_XYT_REP = 0;
	/** Minutiae XYT representation: M1 (ANSI INCITS 378) convention. */
	public static final int M1_XYT_REP = 1;

	/** The constant pi ({@link Math#PI}). */
	public static final double M_PI = Math.PI;// 3.14159265358979323846; // pi

	/**
	 * Number of feature patterns: ten 2x3 pixel-pair patterns define ridge endings and bifurcations. The second
	 * pixel pair of a pattern is permitted to repeat multiple times in a match.
	 */
	public static final int NFEATURES = 10;
	/** Minutia type: bifurcation. */
	public static final int BIFURCATION = 0;
	/** Minutia type: ridge ending. */
	public static final int RIDGE_ENDING = 1;
	/** Feature pattern direction: disappearing. */
	public static final int DISAPPEARING = 0;
	/** Feature pattern direction: appearing. */
	public static final int APPEARING = 1;

	/***** IMAGE CONSTANTS *****/
	/** Default scan resolution in pixels per inch. */
	public static final int DEFAULT_PPI = 500;
	/** Required image pixel depth in bits. */
	public static final int IMAGE_DEPTH = 8;

	/** Intensity used to fill the padded image area (medium gray at 8 bits). */
	public static final int PAD_VALUE = 128; // medium gray @ 8 bits

	/** Intensity used to draw on grayscale images (white at 8 bits). */
	public static final int DRAW_PIXEL = 255; // white in 8 bits

	/** 8-bit binary pixel intensity for white. */
	public static final int WHITE_PIXEL = 255;
	/** 8-bit binary pixel intensity for black. */
	public static final int BLACK_PIXEL = 0;

	/** {@code join_minutia} control: draw the join line without opposite perimeter pixels. */
	public static final int NO_BOUNDARY = 0;
	/** {@code join_minutia} control: draw the join line with opposite perimeter pixels. */
	public static final int WITH_BOUNDARY = 1;

	/** Radial width added to a join line, not including the boundary pixels. */
	public static final int JOIN_LINE_RADIUS = 1;

	/***** MAP CONSTANTS *****/
	/** Map value for blocks without a well-defined direction. */
	public static final int INVALID_DIR = -1;

	/** Map value assigned when the current block has no neighbors with a valid direction. */
	public static final int NO_VALID_NBRS = -3;

	/** Map value designating a block near a high-curvature area such as a core or delta. */
	public static final int HIGH_CURVATURE = -2;

	/** Pixel dimension of each block in the IMAP (version 1 maps). */
	public static final int IMAP_BLOCKSIZE = 24;

	/**
	 * Pixel dimension of image blocks (version 2 maps).
	 * <p>
	 * Together with {@link #MAP_WINDOWSIZE_V2} and {@link #MAP_WINDOWOFFSET_V2} this defines a system of 8x8
	 * adjacent, non-overlapping blocks that are assigned results from analyzing a larger 24x24 window centered
	 * about each block. Caution: if this is changed, {@link #MAP_WINDOWOFFSET_V2}, {@link #TRANS_DIR_PIX_V2} and
	 * {@link #INV_BLOCK_MARGIN_V2} likely need to change too.
	 */
	public static final int MAP_BLOCKSIZE_V2 = 8;

	/** Pixel dimension of the window surrounding a block; the window's analysis result is stored in the block. */
	public static final int MAP_WINDOWSIZE_V2 = 24;

	/** Pixel offset in X and Y from the origin of a block to the origin of its surrounding window. */
	public static final int MAP_WINDOWOFFSET_V2 = 8;

	/**
	 * Number of integer directions used in a semicircle.
	 * <p>
	 * Caution: if this is changed, {@link #HIGHCURV_VORTICITY_MIN}, {@link #HIGHCURV_CURVATURE_MIN} and
	 * {@link #FORK_INTERVAL} likely need to change too.
	 */
	public static final int NUM_DIRECTIONS = 16;

	/** Angle (radians) from which integer directions begin: pi/2 (90 degrees). */
	public static final double START_DIR_ANGLE = (M_PI / 2.0); /* 90 degrees */

	/** Minimum number of valid neighbors required for a valid block direction to avoid being removed. */
	public static final int RMV_VALID_NBR_MIN = 3;

	/** Minimum strength for a direction to be considered significant. */
	public static final double DIR_STRENGTH_MIN = 0.2;

	/**
	 * Maximum distance (in integer directions) allowed between a valid block direction and the average direction
	 * of its neighbors before the direction is removed.
	 */
	public static final int DIR_DISTANCE_MAX = 3;

	/**
	 * Minimum number of valid neighbors required for an INVALID block to receive the average direction of its
	 * neighbors.
	 */
	public static final int SMTH_VALID_NBR_MIN = 7;

	/** Minimum number of valid neighbors required for an INVALID block to be measured for vorticity. */
	public static final int VORT_VALID_NBR_MIN = 7;

	/** Minimum vorticity for which an INVALID block is considered high-curvature based on its neighbors' directions. */
	public static final int HIGHCURV_VORTICITY_MIN = 5;

	/**
	 * Minimum curvature for which a VALID block is considered high-curvature, comparing its direction with its
	 * neighbors' directions.
	 */
	public static final int HIGHCURV_CURVATURE_MIN = 5;

	/** Minimum number of neighbors with VALID direction for an INVALID block to have its direction interpolated. */
	public static final int MIN_INTERPOLATE_NBRS = 2;

	/** Low-contrast map: percentile cut-off used to choose the minimum and maximum pixel intensities in a block. */
	public static final int PERCENTILE_MIN_MAX = 10;

	/**
	 * Low-contrast map: minimum delta between the min and max percentile intensities for a block NOT to be
	 * considered low contrast (in terms of 6-bit pixels).
	 */
	public static final int MIN_CONTRAST_DELTA = 5;

	/***** DFT CONSTANTS *****/
	/** Number of DFT wave forms applied. */
	public static final int NUM_DFT_WAVES = 4;

	/**
	 * Minimum total DFT power for any block, used when computing an average power so that division by zero is
	 * avoided (value taken from HO39).
	 */
	public static final double MIN_POWER_SUM = 10.0;

	/** Minimum DFT power allowable in any one direction (HO39 threshold {@code thrhf = 1e5}). */
	public static final double POWMAX_MIN = 100000.0; // thrhf=1e5f

	/** Minimum normalized power allowable in any one direction (HO39 threshold {@code disc = 3.8}). */
	public static final double POWNORM_MIN = 3.8; // disc=3.8f

	/** Maximum power allowable at the lowest frequency DFT wave (HO39 threshold {@code thrlf = 5e7}). */
	public static final double POWMAX_MAX = 50000000.0; // thrlf=5e7f

	/**
	 * Check for a fork at plus or minus this number of integer directions from the current direction (e.g. 2
	 * directions = 2 x 11.25 degrees).
	 */
	public static final int FORK_INTERVAL = 2;

	/** Minimum DFT power allowed at fork angles, as a fraction of the block's maximum directional power. */
	public static final double FORK_PCT_POWMAX = 0.7;

	/** Minimum normalized power allowed at fork angles, as a fraction of {@link #POWNORM_MIN}. */
	public static final double FORK_PCT_POWNORM = 0.75;

	/***** BINRAIZATION CONSTANTS *****/
	/** Directional binarization grid width in pixels. */
	public static final int DIRBIN_GRID_W = 7;
	/** Directional binarization grid height in pixels. */
	public static final int DIRBIN_GRID_H = 9;

	/** Pixel dimension (square) of the grid used in isotropic binarization. */
	public static final int ISOBIN_GRID_DIM = 11;

	/** Number of passes over the binary image in which holes of length 1 in horizontal and vertical runs are filled. */
	public static final int NUM_FILL_HOLES = 3;

	/***** Minutiae DETECTION CONSTANTS *****/

	/** Maximum pixel translation in X or Y within which two potential minutia points are considered similar. */
	public static final int MAX_MINUTIA_DELTA = 10;

	/**
	 * If the angle of a contour exceeds this value (pi/3 radians), the contour is NOT considered to contain
	 * minutiae.
	 */
	public static final double MAX_HIGH_CURVE_THETA = (M_PI / 3.0);

	/** Half the length in pixels of the contour extracted for a high-curvature minutia. */
	public static final int HIGH_CURVE_HALF_CONTOUR = 14;

	/** A loop must be longer than this (in pixels) to be considered to contain minutiae. */
	public static final int MIN_LOOP_LEN = 20;

	/** If a loop's minimum distance half way across its contour is less than this, the loop is tested for minutiae. */
	public static final double MIN_LOOP_ASPECT_DIST = 1.0;

	/**
	 * If the ratio of a loop's maximum/minimum distances half way across its contour is at least this, the loop is
	 * tested for minutiae.
	 */
	public static final double MIN_LOOP_ASPECT_RATIO = 2.25;

	/** Feature ID assigned to loops: there are 10 unique feature patterns with IDs 0..9, so loops use 10. */
	public static final int LOOP_ID = 10;

	/** Minutia scan control: scan horizontally. */
	public static final int SCAN_HORIZONTAL = 0;
	/** Minutia scan control: scan vertically. */
	public static final int SCAN_VERTICAL = 1;
	/** Contour trace control: clockwise. */
	public static final int SCAN_CLOCKWISE = 0;
	/** Contour trace control: counter-clockwise. */
	public static final int SCAN_COUNTER_CLOCKWISE = 1;

	/** Dimension of the 8-neighbor chain-code lookup matrix. */
	public static final int NBR8_DIM = 3;

	/** Default minutia reliability. */
	public static final double DEFAULT_RELIABILITY = 0.99;

	/** Medium minutia reliability. */
	public static final double MEDIUM_RELIABILITY = 0.50;

	/** High minutia reliability. */
	public static final double HIGH_RELIABILITY = 0.99;

	/***** Minutiae LINKING CONSTANTS *****/
	/** Square dimension of the 2D table of potentially linked minutiae. */
	public static final int LINK_TABLE_DIM = 20;

	/** Maximum orthogonal distance (pixels) between two minutia points for them to be considered for linking. */
	public static final int MAX_LINK_DIST = 20;

	/** Minimum distance (pixels) between two minutia points for an angle computed between them to be reliable. */
	public static final int MIN_THETA_DIST = 5;

	/**
	 * Maximum number of transitions along a contiguous pixel trajectory between two minutiae for the trajectory to
	 * be considered free of obstacles.
	 */
	public static final int MAXTRANS = 2;

	/** Link score parameter: normalization factor for the angle difference. */
	public static final double SCORE_THETA_NORM = 15.0;
	/** Link score parameter: normalization factor for the distance. */
	public static final double SCORE_DIST_NORM = 10.0;
	/** Link score parameter: weight of the distance term. */
	public static final double SCORE_DIST_WEIGHT = 4.0;
	/** Link score parameter: numerator of the score. */
	public static final double SCORE_NUMERATOR = 32000.0;

	/***** FALSE Minutiae REMOVAL CONSTANTS *****/
	/**
	 * Maximum orthogonal distance (pixels) between two minutia points for them to be considered for removal as
	 * hooks, islands, lakes or overlaps (version 1).
	 */
	public static final int MAX_RMTEST_DIST = 8;
	/** Maximum orthogonal distance (pixels) between two minutia points to be considered for removal (version 2). */
	public static final int MAX_RMTEST_DIST_V2 = 16;

	/** Length of pixel contours traced and analyzed for possible hooks (version 1). */
	public static final int MAX_HOOK_LEN = 15;
	/** Length of pixel contours traced and analyzed for possible hooks (version 2). */
	public static final int MAX_HOOK_LEN_V2 = 30;

	/** Half the maximum length of pixel contours traced for possible loops (islands/lakes), version 1. */
	public static final int MAX_HALF_LOOP = 15;
	/** Half the maximum length of pixel contours traced for possible loops (islands/lakes), version 2. */
	public static final int MAX_HALF_LOOP_V2 = 30;

	/**
	 * Distance (pixels) in the direction opposite a minutia within which it is considered close to a block with
	 * invalid ridge flow (version 1).
	 */
	public static final int TRANS_DIR_PIX = 6;
	/** Distance (pixels) opposite a minutia within which it is considered close to an invalid block (version 2). */
	public static final int TRANS_DIR_PIX_V2 = 4;

	/** Maximum circumference (pixels) of small loops (islands/lakes) that qualify for removal. */
	public static final int SMALL_LOOP_LEN = 15;

	/** Half the number of pixels traced to form a complete contour when removing or adjusting side minutiae. */
	public static final int SIDE_HALF_CONTOUR = 7;

	/** Maximum orthogonal distance a minutia may be from a block with invalid ridge flow to be removed (version 1). */
	public static final int INV_BLOCK_MARGIN = 6;
	/** Maximum orthogonal distance a minutia may be from a block with invalid ridge flow to be removed (version 2). */
	public static final int INV_BLOCK_MARGIN_V2 = 4;

	/**
	 * If a sufficiently close invalid block has fewer neighbors with valid ridge flow than this, the minutia is
	 * removed.
	 */
	public static final int RM_VALID_NBR_MIN = 7;

	/** Maximum pixel distance between two points tested for overlap conditions. */
	public static final int MAX_OVERLAP_DIST = 8;

	/** Maximum pixel distance between two points on opposite sides of an overlap for them to be joined. */
	public static final int MAX_OVERLAP_JOIN_DIST = 6;

	/** Malformation removal: contour steps traced to the first measuring point. */
	public static final int MALFORMATION_STEPS_1 = 10;
	/** Malformation removal: contour steps traced to the second measuring point. */
	public static final int MALFORMATION_STEPS_2 = 20;

	/**
	 * Malformation removal: minimum ratio of the distances across the feature at the two points to be considered
	 * normal.
	 */
	public static final double MIN_MALFORMATION_RATIO = 2.0;
	/** Malformation removal: maximum distance across the feature to be considered normal. */
	public static final int MAX_MALFORMATION_DIST = 20;

	/**
	 * Pore removal: translation distance (pixels) from the minutia point in the opposite direction, to get off a
	 * valley edge and into the neighboring ridge.
	 */
	public static final int PORES_TRANS_R = 3;

	/** Pore removal: number of steps (pixels) to search for the edge of the current ridge. */
	public static final int PORES_PERP_STEPS = 12;

	/** Pore removal: number of pixels traced to find forward contour points. */
	public static final int PORES_STEPS_FWD = 10;

	/** Pore removal: number of pixels traced to find backward contour points. */
	public static final int PORES_STEPS_BWD = 8;

	/** Pore removal: minimum squared distance between points before being considered zero. */
	public static final double PORES_MIN_DIST2 = 0.5;

	/**
	 * Pore removal: maximum ratio of the distances between pairs of forward and backward contour points for the
	 * feature to be considered a pore.
	 */
	public static final double PORES_MAX_RATIO = 2.25;

	/***** RIDGE COUNTING CONSTANTS *****/
	/** Maximum number of nearest neighbors per minutia for ridge counting. */
	public static final int MAX_NBRS = 5;

	/** Maximum number of contour steps taken to validate a ridge crossing. */
	public static final int MAX_RIDGE_STEPS = 10;

	/*************************************************************************/
	/* QUALITY/RELIABILITY DEFINITIONS */
	/*************************************************************************/
	/** Number of quality map levels (0 = background/lowest to 4 = highest). */
	public static final int QMAP_LEVELS = 5;

	/** Neighborhood radius in millimeters, computed from 11 pixels scanned at 19.69 pixels/mm. */
	public static final double RADIUS_MM = (11.0 / 19.69);

	/** Ideal standard deviation of pixel values in a neighborhood. */
	public static final int IDEALSTDEV = 64;
	/** Ideal mean of pixel values in a neighborhood. */
	public static final int IDEALMEAN = 127;

	/** Look for neighbors this many blocks away when computing quality. */
	public static final int NEIGHBOR_DELTA = 2;

	/*************************************************************************/
	/* GENERAL DEFINITIONS */
	/*************************************************************************/
	/** LFS version string. */
	public static final String LFS_VERSION_STR = "NIST_LFS_VER2";

	/** Neighbor direction index: north. */
	public static final int NORTH = 0;
	/** Neighbor direction index: south. */
	public static final int SOUTH = 4;
	/** Neighbor direction index: east. */
	public static final int EAST = 2;
	/** Neighbor direction index: west. */
	public static final int WEST = 6;

	/** Boolean true (C-style integer). */
	public static final int TRUE = 1;
	/** Boolean false (C-style integer); also the generic success return code. */
	public static final int FALSE = 0;

	/** Search result: found. */
	public static final int FOUND = TRUE;
	/** Search result: not found. */
	public static final int NOT_FOUND = FALSE;

	/** Return value: a hook was found. */
	public static final int HOOK_FOUND = 1;
	/** Return value: a loop was found. */
	public static final int LOOP_FOUND = 1;
	/** Return value: the item should be ignored. */
	public static final int IGNORE = 2;
	/** Return value: the list is full. */
	public static final int LIST_FULL = 3;
	/** Return value: the operation (e.g. contour trace) is incomplete. */
	public static final int INCOMPLETE = 3;

	/** Pixel value limit of a 6-bit image. */
	public static final int IMG_6BIT_PIX_LIMIT = 64;

	/** Maximum number of minutiae (or reallocation chunk size) detected in an image. */
	public static final int MAX_MINUTIAE = 1000;

	/** If both X and Y deltas of a line are below this, the line's angle is set to 0 radians. */
	public static final double MIN_SLOPE_DELTA = 0.5;

	/** Rotated grid offsets are relative to the grid's center. */
	public static final int RELATIVE_TO_CENTER = 0;

	/** Rotated grid offsets are relative to the grid's origin. */
	public static final int RELATIVE_TO_ORIGIN = 1;

	/**
	 * Scale used to truncate floating-point precision (multiply, round, divide) for results consistent across
	 * architectures.
	 */
	public static final double TRUNC_SCALE = 16384.0;

	/** Designates an argument as undefined. */
	public static final int UNDEFINED = -1;

	/** Dummy value for unused integer LFS control parameters. */
	public static final int UNUSED_INT = 0;
	/** Dummy value for unused floating-point LFS control parameters. */
	public static final double UNUSED_DBL = 0.0;

	/**
	 * Lookup tables converting integer directions to angles in radians ({@code DIR2RAD}).
	 * <p>
	 * Entry {@code i} holds the cosine and sine of the angle of integer direction {@code i}, used when averaging
	 * and smoothing block directions.
	 */
	@Getter
	@Setter
	@Data
	public class DirToRad {
		/** Number of integer directions in the tables. */
		private int nDirs;
		/** Cosine of each integer direction's angle. */
		private double[] cos;
		/** Sine of each integer direction's angle. */
		private double[] sin;

		/**
		 * Allocates cosine and sine tables for the given number of directions (values are filled by
		 * {@code IInit.initDirToRad}).
		 *
		 * @param nDirs number of integer directions
		 */
		public DirToRad(int nDirs) {
			super();
			this.nDirs = nDirs;
			this.setCos(new double[nDirs]);
			this.setSin(new double[nDirs]);
		}
	}

	/** A DFT wave form holding both the cosine and sine components for one frequency ({@code DFTWAVE}). */
	@Getter
	@Setter
	@Data
	public class DftWave {
		/** Cosine component samples. */
		private double[] cos;
		/** Sine component samples. */
		private double[] sin;

		/**
		 * Allocates cosine and sine component arrays of the given length.
		 *
		 * @param waveLen number of samples (the DFT block size in pixels)
		 */
		public DftWave(int waveLen) {
			super();
			this.cos = new double[waveLen];
			this.sin = new double[waveLen];
		}
	}

	/** The set of DFT wave forms used in DFT analysis of image blocks ({@code DFTWAVES}). */
	@Getter
	@Setter
	@Data
	public class DftWaves {
		/** Number of wave forms. */
		private int nWaves;
		/** Length of each wave form in samples (equals the block/window size). */
		private int waveLen;// blockOffsetSize
		/** The wave forms. */
		private DftWave[] waves;

		/**
		 * Allocates an empty array for the given number of wave forms.
		 *
		 * @param nWaves  number of wave forms
		 * @param waveLen length of each wave form in samples
		 */
		public DftWaves(int nWaves, int waveLen) {
			super();
			this.nWaves = nWaves;
			this.waveLen = waveLen;
			this.waves = new DftWave[nWaves];
		}
	}

	/**
	 * Rotated pixel offsets for a grid of given dimensions at a number of orientations ({@code ROTGRIDS}).
	 * <p>
	 * Used by DFT analysis when generating the direction map and by directional/isotropic binarization. Row
	 * {@code d} of {@link #grids} holds the pixel offsets of the grid rotated to integer direction {@code d}.
	 */
	@Getter
	@Setter
	@Data
	public class RotGrids {
		/** Padding (pixels) required around the image so rotated grids stay in bounds. */
		private int pad;
		/** Offset origin flag: {@link ILfs#RELATIVE_TO_CENTER} or {@link ILfs#RELATIVE_TO_ORIGIN}. */
		private int relative2;
		/** Angle (radians) of the first orientation. */
		private double startAngle;
		/** Number of orientations (rotated grids). */
		private int noOfGrids;
		/** Grid width in pixels. */
		private int gridWidth;
		/** Grid height in pixels. */
		private int gridHeight;
		/** Pixel offsets for each rotated grid, indexed {@code [direction][gridWidth * gridHeight]}. */
		private int[][] grids;

		/**
		 * Allocates rotated grids of the given geometry (offsets are filled by {@code IInit.initRotGrids}).
		 *
		 * @param startAngle angle in radians of the first orientation
		 * @param noOfGrids  number of orientations
		 * @param gridWidth  grid width in pixels
		 * @param gridHeight grid height in pixels
		 * @param relative2  offset origin flag ({@link ILfs#RELATIVE_TO_CENTER} or {@link ILfs#RELATIVE_TO_ORIGIN})
		 */
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

	/**
	 * A single detected minutia ({@code MINUTIA}).
	 * <p>
	 * Holds the feature location, its adjacent edge pixel, direction, reliability, type and, after ridge counting,
	 * its nearest neighbors and the ridge counts to them.
	 */
	@Getter
	@Setter
	@Data
	public class Minutia {
		/** X coordinate (pixels) of the minutia point. */
		private int x;
		/** Y coordinate (pixels) of the minutia point. */
		private int y;
		/** X coordinate (pixels) of the edge pixel adjacent to the minutia point. */
		private int ex;
		/** Y coordinate (pixels) of the edge pixel adjacent to the minutia point. */
		private int ey;
		/** Minutia direction in integer direction units. */
		private int direction;
		/** Reliability (quality) of the minutia, from 0.0 to 1.0. */
		private double reliability;
		/** Minutia type: {@link ILfs#BIFURCATION} or {@link ILfs#RIDGE_ENDING}. */
		private int type;
		/** Whether the feature is {@link ILfs#APPEARING} or {@link ILfs#DISAPPEARING} along the scan direction. */
		private int appearing;
		/** ID of the feature pattern that detected this minutia ({@link ILfs#LOOP_ID} for loops). */
		private int featureId;
		/** Indices of the nearest neighbor minutiae. */
		private AtomicIntegerArray nbrs;
		/** Ridge counts to each nearest neighbor, parallel to {@link #nbrs}. */
		private AtomicIntegerArray ridgeCounts;
		/** Number of valid entries in {@link #nbrs} and {@link #ridgeCounts}. */
		private int numNbrs;

		/** Creates an empty minutia; fields are populated through setters. */
		public Minutia() {
			super();
		}

		/**
		 * Returns a string representation of this minutia listing all fields, terminated by a newline.
		 *
		 * @return a human-readable description of the minutia
		 */
		@Override
		public String toString() {
			return "Minutia [x=" + x + ", y=" + y + ", ex=" + ex + ", ey=" + ey + ", direction=" + direction
					+ ", reliability=" + reliability + ", type=" + type + ", appearing=" + appearing + ", featureId="
					+ featureId + ", nbrs=" + nbrs + ", ridgeCounts=" + ridgeCounts + ", numNbrs=" + numNbrs + "]\n";
		}
	}

	/** A list of detected minutiae ({@code MINUTIAE}). */
	@Getter
	@Setter
	@Data
	public class Minutiae {
		/** Allocated capacity of the list. */
		private int alloc;
		/** Number of minutiae currently stored. */
		private int num;
		/** The minutiae. */
		private List<Minutia> list;

		/** Creates an empty minutiae container with no backing list. */
		public Minutiae() {
			super();
		}

		/**
		 * Creates a minutiae container with a list of the given initial capacity.
		 *
		 * @param alloc initial capacity
		 * @param num   initial number of minutiae
		 */
		public Minutiae(int alloc, int num) {
			super();
			this.alloc = alloc;
			this.num = num;
			this.list = new ArrayList<>(alloc);
		}

		/**
		 * Returns a string representation of this container including all minutiae.
		 *
		 * @return a human-readable description of the list
		 */
		@Override
		public String toString() {
			return "Minutiae [alloc=" + alloc + ", num=" + num + ", list=" + list + "]";
		}
	}

	/**
	 * A 2x3 pixel-pair feature pattern used to detect ridge endings and bifurcations ({@code FEATURE_PATTERN}).
	 * <p>
	 * A pattern consists of three consecutive pixel pairs; the second pair may repeat during matching.
	 */
	@Getter
	@Setter
	@Data
	public class FeaturePattern {
		/** Number of pixels in each pair (2). */
		private int featurePatternCount = 2;
		/** Minutia type detected by the pattern ({@link ILfs#BIFURCATION} or {@link ILfs#RIDGE_ENDING}). */
		private int type;
		/** Whether the feature is appearing or disappearing along the scan direction. */
		private int appearing;
		/** First pixel pair. */
		private int[] first = new int[featurePatternCount];
		/** Second pixel pair (may repeat). */
		private int[] second = new int[featurePatternCount];
		/** Third pixel pair. */
		private int[] third = new int[featurePatternCount];

		/**
		 * Creates a feature pattern.
		 *
		 * @param type      minutia type detected by the pattern
		 * @param appearing {@link ILfs#APPEARING} or {@link ILfs#DISAPPEARING}
		 * @param first     first pixel pair
		 * @param second    second pixel pair
		 * @param third     third pixel pair
		 */
		public FeaturePattern(int type, int appearing, int[] first, int[] second, int[] third) {
			super();
			this.type = type;
			this.appearing = appearing;
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}

	/** A filled shape described by the contour points on each of its scanlines ({@code SHAPE}), used to fill loops. */
	@Getter
	@Setter
	@Data
	public class Shape {
		/** Y coordinate of the top-most scanline in the shape. */
		private int yMin; // Y-coord of top-most scanline in shape.
		/** Y coordinate of the bottom-most scanline in the shape. */
		private int yMax; // Y-coord of bottom-most scanline in shape.
		/** Rows (scanlines) comprising the shape. */
		private AtomicReferenceArray<Rows> rows; // List of row pointers comprising the shape.
		/** Number of rows allocated for the shape. */
		private int alloc; // Number of rows allocated for shape.
		/** Number of rows assigned to the shape. */
		private int nRows; // Number of rows assigned to shape.

		/** Creates an empty shape. */
		public Shape() {
			super();
		}
	}

	/** One scanline of a {@link Shape} ({@code ROW}). */
	@Getter
	@Setter
	@Data
	public class Rows {
		/** Y coordinate of this row. */
		private int y; // Y-coord of current row in shape.
		/** X coordinates of the shape contour points on this row. */
		private AtomicIntegerArray xs; // X-coords for shape contour points on current row.
		/** Number of X coordinates allocated for this row. */
		private int alloc; // Number of points allocate for x-coords on row.
		/** Number of X coordinates assigned for this row. */
		private int noOfPts; // Number of points assigned for x-coords on row.
	}

	/**
	 * Parameters used by LFS/MINDTCT to set thresholds and define test criteria ({@code LFSPARMS}).
	 * <p>
	 * Each field corresponds to one of the tuning constants declared in {@link ILfs}; default parameter sets
	 * (version 1 and version 2) are provided by {@code org.mosip.nist.nfiq1.mindtct.Globals}.
	 */
	@Getter
	@Setter
	@Data
	public class LfsParams {
		/* Image Controls */
		/** Intensity used to fill padded image areas ({@link ILfs#PAD_VALUE}). */
		private int padValue;
		/** Radial width added to join lines ({@link ILfs#JOIN_LINE_RADIUS}). */
		private int joinLineRadius;

		/* Map Controls */
		/** Pixel dimension of an image block ({@link ILfs#MAP_BLOCKSIZE_V2}). */
		private int blockOffsetSize; // Pixel dimension image block.
		/** Pixel dimension of the window surrounding a block ({@link ILfs#MAP_WINDOWSIZE_V2}). */
		private int windowSize; // Pixel dimension window surrounding block.
		/** Offset in X and Y from block to window origin ({@link ILfs#MAP_WINDOWOFFSET_V2}). */
		private int windowOffset; // Offset in X & Y from block to window origin.
		/** Number of integer directions in a semicircle ({@link ILfs#NUM_DIRECTIONS}). */
		private int numDirections;
		/** Angle in radians where integer directions begin ({@link ILfs#START_DIR_ANGLE}). */
		private double startDirAngle;
		/** Minimum valid neighbors to keep a valid block direction ({@link ILfs#RMV_VALID_NBR_MIN}). */
		private int rmvValidNbrMin;
		/** Minimum strength for a significant direction ({@link ILfs#DIR_STRENGTH_MIN}). */
		private double dirStrengthMin;
		/** Maximum distance from the neighbors' average direction ({@link ILfs#DIR_DISTANCE_MAX}). */
		private int dirDistanceMax;
		/** Minimum valid neighbors for smoothing an INVALID block ({@link ILfs#SMTH_VALID_NBR_MIN}). */
		private int smoothValidNbrMin;
		/** Minimum valid neighbors to measure vorticity ({@link ILfs#VORT_VALID_NBR_MIN}). */
		private int vortValidNbrMin;
		/** Minimum vorticity for high curvature ({@link ILfs#HIGHCURV_VORTICITY_MIN}). */
		private int highcurvVorticityMin;
		/** Minimum curvature for high curvature ({@link ILfs#HIGHCURV_CURVATURE_MIN}). */
		private int highcurvCurvatureMin;
		/** Minimum valid neighbors for direction interpolation ({@link ILfs#MIN_INTERPOLATE_NBRS}). */
		private int minInterpolateNbrs;
		/** Percentile cut-off for low-contrast analysis ({@link ILfs#PERCENTILE_MIN_MAX}). */
		private int percentileMinMax;
		/** Minimum contrast delta for a block not to be low contrast ({@link ILfs#MIN_CONTRAST_DELTA}). */
		private int minContrastDelta;

		/* DFT Controls */
		/** Number of DFT wave forms ({@link ILfs#NUM_DFT_WAVES}). */
		private int numDftWaves;
		/** Minimum DFT power in any one direction ({@link ILfs#POWMAX_MIN}). */
		private double powmaxMin;
		/** Minimum normalized DFT power in any one direction ({@link ILfs#POWNORM_MIN}). */
		private double pownormMin;
		/** Maximum power at the lowest frequency wave ({@link ILfs#POWMAX_MAX}). */
		private double powmaxMax;
		/** Fork test interval in integer directions ({@link ILfs#FORK_INTERVAL}). */
		private int forkInterval;
		/** Fork minimum power fraction ({@link ILfs#FORK_PCT_POWMAX}). */
		private double forkPctPowmax;
		/** Fork minimum normalized power fraction ({@link ILfs#FORK_PCT_POWNORM}). */
		private double forkPctPownorm;

		/* Binarization Controls */
		/** Directional binarization grid width ({@link ILfs#DIRBIN_GRID_W}). */
		private int dirbinGridWidth;
		/** Directional binarization grid height ({@link ILfs#DIRBIN_GRID_H}). */
		private int dirbinGridHeight;
		/** Isotropic binarization grid dimension ({@link ILfs#ISOBIN_GRID_DIM}). */
		private int isoBinGridDim;
		/** Number of hole-filling passes ({@link ILfs#NUM_FILL_HOLES}). */
		private int numFillHoles;

		/* Minutiae Detection Controls */
		/** Maximum translation for two minutiae to be similar ({@link ILfs#MAX_MINUTIA_DELTA}). */
		private int maxMinutiaDelta;
		/** Maximum contour angle to contain minutiae ({@link ILfs#MAX_HIGH_CURVE_THETA}). */
		private double maxHighCurveTheta;
		/** Half-length of a high-curvature contour ({@link ILfs#HIGH_CURVE_HALF_CONTOUR}). */
		private int highCurveHalfContour;
		/** Minimum loop length to contain minutiae ({@link ILfs#MIN_LOOP_LEN}). */
		private int minLoopLen;
		/** Loop aspect minimum distance threshold ({@link ILfs#MIN_LOOP_ASPECT_DIST}). */
		private double minLoopAspectDist;
		/** Loop aspect ratio threshold ({@link ILfs#MIN_LOOP_ASPECT_RATIO}). */
		private double minLoopAspectRatio;

		/* Minutiae Link Controls */
		/** Link table dimension ({@link ILfs#LINK_TABLE_DIM}). */
		private int linkTableDim;
		/** Maximum link distance ({@link ILfs#MAX_LINK_DIST}). */
		private int maxLinkDist;
		/** Minimum distance for a reliable angle ({@link ILfs#MIN_THETA_DIST}). */
		private int minThetaDist;
		/** Maximum transitions on a free path ({@link ILfs#MAXTRANS}). */
		private int maxTrans;
		/** Link score angle normalization ({@link ILfs#SCORE_THETA_NORM}). */
		private double scoreThetaNorm;
		/** Link score distance normalization ({@link ILfs#SCORE_DIST_NORM}). */
		private double scoreDistNorm;
		/** Link score distance weight ({@link ILfs#SCORE_DIST_WEIGHT}). */
		private double scoreDistWeight;
		/** Link score numerator ({@link ILfs#SCORE_NUMERATOR}). */
		private double scoreNumerator;

		/* False Minutiae Removal Controls */
		/** Maximum distance for removal tests ({@link ILfs#MAX_RMTEST_DIST_V2}). */
		private int maxRmTestDist;
		/** Maximum hook contour length ({@link ILfs#MAX_HOOK_LEN_V2}). */
		private int maxHookLen;
		/** Half the maximum loop contour length ({@link ILfs#MAX_HALF_LOOP_V2}). */
		private int maxHalfLoop;
		/** Distance opposite a minutia to test for invalid blocks ({@link ILfs#TRANS_DIR_PIX_V2}). */
		private int transDirPixel;
		/** Maximum circumference of small loops ({@link ILfs#SMALL_LOOP_LEN}). */
		private int smallLoopLen;
		/** Half-contour length for side minutiae ({@link ILfs#SIDE_HALF_CONTOUR}). */
		private int sideHalfContour;
		/** Margin to invalid blocks for removal ({@link ILfs#INV_BLOCK_MARGIN_V2}). */
		private int invBlockMargin;
		/** Minimum valid neighbors of a nearby invalid block ({@link ILfs#RM_VALID_NBR_MIN}). */
		private int rmValidNbrMin;
		/** Maximum overlap test distance ({@link ILfs#MAX_OVERLAP_DIST}). */
		private int maxOverlapDist;
		/** Maximum overlap join distance ({@link ILfs#MAX_OVERLAP_JOIN_DIST}). */
		private int maxOverlapJoinDist;
		/** Contour steps to the first malformation measuring point ({@link ILfs#MALFORMATION_STEPS_1}). */
		private int malformationSteps1;
		/** Contour steps to the second malformation measuring point ({@link ILfs#MALFORMATION_STEPS_2}). */
		private int malformationSteps2;
		/** Minimum malformation distance ratio ({@link ILfs#MIN_MALFORMATION_RATIO}). */
		private double minMalformationRatio;
		/** Maximum malformation distance ({@link ILfs#MAX_MALFORMATION_DIST}). */
		private int maxMalformationDist;
		/** Pore translation distance ({@link ILfs#PORES_TRANS_R}). */
		private int poresTransR;
		/** Pore perpendicular search steps ({@link ILfs#PORES_PERP_STEPS}). */
		private int poresPerpSteps;
		/** Pore forward trace steps ({@link ILfs#PORES_STEPS_FWD}). */
		private int poresStepsFwd;
		/** Pore backward trace steps ({@link ILfs#PORES_STEPS_BWD}). */
		private int poresStepsBwd;
		/** Pore minimum squared distance ({@link ILfs#PORES_MIN_DIST2}). */
		private double poresMinDist2;
		/** Pore maximum distance ratio ({@link ILfs#PORES_MAX_RATIO}). */
		private double poresMaxRatio;

		/* Ridge Counting Controls */
		/** Maximum nearest neighbors per minutia ({@link ILfs#MAX_NBRS}). */
		private int maxNbrs;
		/** Maximum steps to validate a ridge crossing ({@link ILfs#MAX_RIDGE_STEPS}). */
		private int maxRidgeSteps;

		/**
		 * Creates a fully specified LFS parameter set. Each argument initializes the field of the same name.
		 *
		 * @param padValue             intensity used to fill padded image areas
		 * @param joinLineRadius       radial width added to join lines
		 * @param blockOffsetSize      pixel dimension of an image block
		 * @param windowSize           pixel dimension of the window surrounding a block
		 * @param windowOffset         offset in X and Y from block to window origin
		 * @param numDirections        number of integer directions in a semicircle
		 * @param startDirAngle        angle in radians where integer directions begin
		 * @param rmvValidNbrMin       minimum valid neighbors to keep a valid block direction
		 * @param dirStrengthMin       minimum strength for a significant direction
		 * @param dirDistanceMax       maximum distance from the neighbors' average direction
		 * @param smoothValidNbrMin    minimum valid neighbors for smoothing an INVALID block
		 * @param vortValidNbrMin      minimum valid neighbors to measure vorticity
		 * @param highcurvVorticityMin minimum vorticity for high curvature
		 * @param highcurvCurvatureMin minimum curvature for high curvature
		 * @param minInterpolateNbrs   minimum valid neighbors for direction interpolation
		 * @param percentileMinMax     percentile cut-off for low-contrast analysis
		 * @param minContrastDelta     minimum contrast delta for a block not to be low contrast
		 * @param numDftWaves          number of DFT wave forms
		 * @param powmaxMin            minimum DFT power in any one direction
		 * @param pownormMin           minimum normalized DFT power in any one direction
		 * @param powmaxMax            maximum power at the lowest frequency wave
		 * @param forkInterval         fork test interval in integer directions
		 * @param forkPctPowmax        fork minimum power fraction
		 * @param forkPctPownorm       fork minimum normalized power fraction
		 * @param dirbinGridWidth      directional binarization grid width
		 * @param dirbinGridHeight     directional binarization grid height
		 * @param isoBinGridDim        isotropic binarization grid dimension
		 * @param numFillHoles         number of hole-filling passes
		 * @param maxMinutiaDelta      maximum translation for two minutiae to be similar
		 * @param maxHighCurveTheta    maximum contour angle to contain minutiae
		 * @param highCurveHalfContour half-length of a high-curvature contour
		 * @param minLoopLen           minimum loop length to contain minutiae
		 * @param minLoopAspectDist    loop aspect minimum distance threshold
		 * @param minLoopAspectRatio   loop aspect ratio threshold
		 * @param linkTableDim         link table dimension
		 * @param maxLinkDist          maximum link distance
		 * @param minThetaDist         minimum distance for a reliable angle
		 * @param maxTrans             maximum transitions on a free path
		 * @param scoreThetaNorm       link score angle normalization
		 * @param scoreDistNorm        link score distance normalization
		 * @param scoreDistWeight      link score distance weight
		 * @param scoreNumerator       link score numerator
		 * @param maxRmTestDist        maximum distance for removal tests
		 * @param maxHookLen           maximum hook contour length
		 * @param maxHalfLoop          half the maximum loop contour length
		 * @param transDirPixel        distance opposite a minutia to test for invalid blocks
		 * @param smallLoopLen         maximum circumference of small loops
		 * @param sideHalfContour      half-contour length for side minutiae
		 * @param invBlockMargin       margin to invalid blocks for removal
		 * @param rmValidNbrMin        minimum valid neighbors of a nearby invalid block
		 * @param maxOverlapDist       maximum overlap test distance
		 * @param maxOverlapJoinDist   maximum overlap join distance
		 * @param malformationSteps1   contour steps to the first malformation measuring point
		 * @param malformationSteps2   contour steps to the second malformation measuring point
		 * @param minMalformationRatio minimum malformation distance ratio
		 * @param maxMalformationDist  maximum malformation distance
		 * @param poresTransR          pore translation distance
		 * @param poresPerpSteps       pore perpendicular search steps
		 * @param poresStepsFwd        pore forward trace steps
		 * @param poresStepsBwd        pore backward trace steps
		 * @param poresMinDist2        pore minimum squared distance
		 * @param poresMaxRatio        pore maximum distance ratio
		 * @param maxNbrs              maximum nearest neighbors per minutia
		 * @param maxRidgeSteps        maximum steps to validate a ridge crossing
		 */
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
		/**
		 * Returns a string representation listing every LFS parameter and its value.
		 *
		 * @return a human-readable description of this parameter set
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

	/**
	 * Binarization routines (port of NIST's {@code binar.c}); implemented by {@code mindtct.Binarization}.
	 * <p>
	 * Converts the padded grayscale image into a binary ridge/valley image using the direction map: blocks with a
	 * valid direction are binarized with a rotated directional grid, others are handled isotropically or set to
	 * white.
	 */
	public interface IBinarization {
		/**
		 * Binarizes a padded grayscale image using a version 1 IMAP (NIST {@code binarize}).
		 *
		 * @param ret              output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth      output width of the binarized image
		 * @param oImageHeight     output height of the binarized image
		 * @param paddedImageData  padded grayscale image
		 * @param paddedImageWidth padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param mapDirectionArr  IMAP of block directions
		 * @param mappedImageWidth map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dirbingrids      rotated grids used for directional binarization
		 * @param lfsParms         LFS parameters
		 * @return the binarized image (unpadded), or {@code null} on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] binarize(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
				final RotGrids dirbingrids, final LfsParams lfsParms);

		/**
		 * Binarizes a padded grayscale image using a version 2 direction map (NIST {@code binarize_V2}).
		 *
		 * @param ret              output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth      output width of the binarized image
		 * @param oImageHeight     output height of the binarized image
		 * @param paddedImageData  padded grayscale image
		 * @param paddedImageWidth padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param directionMap     direction map (one entry per block)
		 * @param mappedImageWidth map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dirbingrids      rotated grids used for directional binarization
		 * @param lfsParms         LFS parameters
		 * @return the binarized image (unpadded), or {@code null} on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] binarizeV2(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
				final RotGrids dirbingrids, final LfsParams lfsParms);

		/**
		 * Binarizes a padded image block by block, using directional binarization where the IMAP is valid and
		 * isotropic binarization otherwise (NIST {@code binarize_image}).
		 *
		 * @param ret              output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth      output width of the binarized image
		 * @param oImageHeight     output height of the binarized image
		 * @param paddedImageData  padded grayscale image
		 * @param paddedImageWidth padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param mapDirectionArr  IMAP of block directions
		 * @param mappedImageWidth map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param imapBlockSize    IMAP block size in pixels
		 * @param dirbingrids      rotated grids used for directional binarization
		 * @param isoBinGridDim    isotropic binarization grid dimension
		 * @return the binarized image, or {@code null} on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] binarizeImage(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray mapDirectionArr, final int mappedImageWidth, final int mappedImageHeight,
				final int imapBlockSize, RotGrids dirbingrids, final int isoBinGridDim);

		/**
		 * Binarizes a padded image block by block using directional binarization where the direction map is valid;
		 * pixels in blocks without a valid direction are set to white (NIST {@code binarize_image_V2}).
		 *
		 * @param ret              output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth      output width of the binarized image
		 * @param oImageHeight     output height of the binarized image
		 * @param paddedImageData  padded grayscale image
		 * @param paddedImageWidth padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param directionMap     direction map (one entry per block)
		 * @param mappedImageWidth map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param blockOffsetSize  block size in pixels
		 * @param dirbingrids      rotated grids used for directional binarization
		 * @return the binarized image, or {@code null} on error ({@link ILfs#ERROR_CODE_110})
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] binarizeImageV2(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				AtomicIntegerArray directionMap, final int mappedImageWidth, final int mappedImageHeight,
				final int blockOffsetSize, final RotGrids dirbingrids);

		/**
		 * Binarizes one pixel by comparing the sum of the grid row through it with the average row sum of a rotated
		 * grid oriented along the ridge direction (NIST {@code dirbinarize}).
		 *
		 * @param paddedImageData  padded grayscale image
		 * @param paddedImageIndex index of the pixel in the padded image
		 * @param imapDirection    ridge direction at the pixel (integer direction)
		 * @param dirbingrids      rotated grids used for directional binarization
		 * @return {@link ILfs#WHITE_PIXEL} or {@link ILfs#BLACK_PIXEL}
		 */
		public int dirbinarize(int[] paddedImageData, final int paddedImageIndex, final int imapDirection,
				final RotGrids dirbingrids);

		/**
		 * Binarizes one pixel by comparing it with the mean of a square neighborhood (NIST {@code isobinarize}).
		 *
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageIndex  index of the pixel in the padded image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param isoBinGridDim     dimension of the square neighborhood in pixels
		 * @return {@link ILfs#WHITE_PIXEL} or {@link ILfs#BLACK_PIXEL}
		 */
		public int isoBinarize(int[] paddedImageData, final int paddedImageIndex, final int paddedImageWidth,
				final int paddedImageHeight, final int isoBinGridDim);
	}

	/** Image block utilities (port of NIST's {@code block.c}); implemented by {@code mindtct.Block}. */
	public interface IBlock {
		/**
		 * Computes the pixel offset of the origin of each block in a padded image (NIST {@code block_offsets}).
		 *
		 * @param ret             output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth     output number of blocks horizontally
		 * @param oImageHeight    output number of blocks vertically
		 * @param imageWidth      unpadded image width in pixels
		 * @param imageHeight     unpadded image height in pixels
		 * @param pad             pad size in pixels around the image
		 * @param blockOffsetSize block size in pixels
		 * @return array of block origin offsets into the padded image, or {@code null} on error
		 */
		public AtomicIntegerArray blockOffsets(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight,
				final int imageWidth, final int imageHeight, final int pad, final int blockOffsetSize);

		/**
		 * Determines whether a block (window) has low contrast, based on the spread between the min and max percentile
		 * pixel intensities (NIST {@code low_contrast_block}).
		 *
		 * @param blockOffset       offset of the block origin in the padded image
		 * @param blockOffsetSize   block (window) size in pixels
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param lfsParams         LFS parameters (percentile and contrast thresholds)
		 * @return {@link ILfs#TRUE} if low contrast, {@link ILfs#FALSE} if not, or a negative error code
		 *         ({@link ILfs#ERROR_CODE_510}, {@link ILfs#ERROR_CODE_511})
		 */
		public int lowContrastBlock(final int blockOffset, final int blockOffsetSize, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, LfsParams lfsParams);

		/**
		 * Starting at a block, walks the maps in a given direction until a block with a valid direction is found,
		 * stopping at low-contrast blocks or the map edge (NIST {@code find_valid_block}).
		 *
		 * @param nbrDir            output direction of the valid block found
		 * @param nbrX              output X (block) coordinate of the valid block found
		 * @param nbrY              output Y (block) coordinate of the valid block found
		 * @param directionMap      direction map
		 * @param lowContrastMap    low-contrast map
		 * @param startX            starting X block coordinate
		 * @param startY            starting Y block coordinate
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param xIncr             X increment per step
		 * @param yIncr             Y increment per step
		 * @return {@link ILfs#FOUND} or {@link ILfs#NOT_FOUND}
		 */
		@SuppressWarnings({ "java:S107" })
		public int findValidBlock(AtomicInteger nbrDir, AtomicInteger nbrX, AtomicInteger nbrY,
				AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap, final int startX, final int startY,
				final int mappedImageWidth, final int mappedImageHeight, final int xIncr, final int yIncr);

		/**
		 * Sets the blocks along the map margin to a given value (NIST {@code set_margin_blocks}).
		 *
		 * @param map               map to modify in place
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param marginValue       value assigned to margin blocks
		 */
		public void setMarginBlocks(AtomicIntegerArray map, final int mappedImageWidth, final int mappedImageHeight,
				final int marginValue);
	}

	/** Chain-code utilities (port of NIST's {@code chaincod.c}); implemented by {@code mindtct.ChainCode}. */
	public interface IChainCode {
		/**
		 * Converts a closed contour into an 8-neighbor chain code (NIST {@code chain_code_loop}).
		 *
		 * @param oVectorChainCodes   output chain codes
		 * @param oNoOfCodesInChain   output number of chain codes
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 * @return {@code 0} on success, negative on system error
		 */
		public int chainCodeLoop(AtomicIntegerArray oVectorChainCodes, AtomicInteger oNoOfCodesInChain,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, int noOfPointsInContour);

		/**
		 * Determines whether a chain-coded loop is traced clockwise, by accumulating direction changes (NIST
		 * {@code is_chain_clockwise}).
		 *
		 * @param oVectorChainCodes chain codes
		 * @param noOfCodesInChain  number of chain codes
		 * @param defaultRetCode    value returned if the direction cannot be determined
		 * @return {@link ILfs#TRUE} if clockwise, {@link ILfs#FALSE} if counter-clockwise, or {@code defaultRetCode}
		 */
		public int isChainClockwise(AtomicIntegerArray oVectorChainCodes, int noOfCodesInChain, int defaultRetCode);
	}

	/** Contour tracing utilities (port of NIST's {@code contour.c}); implemented by {@code mindtct.Contour}. */
	public interface IContour {
		/**
		 * Allocates a contour able to hold the given number of points (NIST {@code allocate_contour}).
		 *
		 * @param ret                 output return code: {@code 0} on success, negative on system error
		 * @param noOfPointsInContour number of contour points to allocate
		 * @return the allocated contour
		 */
		public IContour allocateContour(AtomicInteger ret, final int noOfPointsInContour);

		/**
		 * Releases a contour (NIST {@code free_contour}).
		 *
		 * @param contour contour to release
		 */
		public void freeContour(Contour contour);

		/**
		 * Extracts a contour of up to {@code 2 * halfContour + 1} points centered on a high-curvature feature,
		 * starting from the feature and its edge pixel (NIST {@code get_high_curvature_contour}).
		 *
		 * @param ret                output return code: {@code 0} on success, {@link ILfs#LOOP_FOUND} if a loop was
		 *                           encountered, {@link ILfs#IGNORE} if the contour is incomplete, negative on error
		 * @param oNoOfContour       output number of contour points
		 * @param halfContour        half the contour length in pixels
		 * @param xPixelLoc          X of the feature pixel
		 * @param yPixelLoc          Y of the feature pixel
		 * @param xEdgePixelLoc      X of the edge pixel
		 * @param yEdgePixelLoc      Y of the edge pixel
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return the extracted contour
		 */
		@SuppressWarnings({ "java:S107" })
		public IContour getHighCurvatureContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Extracts a contour of {@code 2 * halfContour + 1} points centered on a feature by tracing in both directions
		 * (NIST {@code get_centered_contour}).
		 *
		 * @param ret                output return code: {@code 0} on success, {@link ILfs#LOOP_FOUND},
		 *                           {@link ILfs#IGNORE} or {@link ILfs#INCOMPLETE}, or negative on error
		 * @param oNoOfContour       output number of contour points
		 * @param halfContour        half the contour length in pixels
		 * @param xPixelLoc          X of the feature pixel
		 * @param yPixelLoc          Y of the feature pixel
		 * @param xEdgePixelLoc      X of the edge pixel
		 * @param yEdgePixelLoc      Y of the edge pixel
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return the extracted contour
		 */
		@SuppressWarnings({ "java:S107" })
		public IContour getCenteredContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Traces a contour along the edge of a ridge or valley for up to {@code maxLenOfContour} steps (NIST
		 * {@code trace_contour}).
		 *
		 * @param ret                output return code: {@code 0} on success, {@link ILfs#LOOP_FOUND} if the trace
		 *                           returned to the loop point, {@link ILfs#IGNORE}, or negative on error
		 * @param oNoOfContour       output number of contour points
		 * @param maxLenOfContour    maximum number of points to trace
		 * @param xLoop              X of the point that signals a completed loop
		 * @param yLoop              Y of the point that signals a completed loop
		 * @param xPixelLoc          X of the starting feature pixel
		 * @param yPixelLoc          Y of the starting feature pixel
		 * @param xEdgePixelLoc      X of the starting edge pixel
		 * @param yEdgePixelLoc      Y of the starting edge pixel
		 * @param scanClock          {@link ILfs#SCAN_CLOCKWISE} or {@link ILfs#SCAN_COUNTER_CLOCKWISE}
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return the traced contour
		 */
		@SuppressWarnings({ "java:S107" })
		public IContour traceContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int maxLenOfContour,
				final int xLoop, final int yLoop, final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc, final int scanClock, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		/**
		 * Traces a contour for up to {@code searchLen} steps looking for a specific pixel (NIST
		 * {@code search_contour}).
		 *
		 * @param xPixelSearch       X of the pixel being searched for
		 * @param yPixelSearch       Y of the pixel being searched for
		 * @param searchLen          maximum number of steps
		 * @param xPixelLoc          X of the starting feature pixel
		 * @param yPixelLoc          Y of the starting feature pixel
		 * @param xEdgePixelLoc      X of the starting edge pixel
		 * @param yEdgePixelLoc      Y of the starting edge pixel
		 * @param scanClock          {@link ILfs#SCAN_CLOCKWISE} or {@link ILfs#SCAN_COUNTER_CLOCKWISE}
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@link ILfs#FOUND} or {@link ILfs#NOT_FOUND}
		 */
		@SuppressWarnings({ "java:S107" })
		public int searchContour(final int xPixelSearch, final int yPixelSearch, final int searchLen,
				final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
				final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Finds the next contour pixel and its edge pixel by scanning the 8 neighbors of the current pixel (NIST
		 * {@code next_contour_pixel}).
		 *
		 * @param nextXPixelLoc        output X of the next feature pixel
		 * @param nextYPixelLoc        output Y of the next feature pixel
		 * @param nextXEdgePixelLoc    output X of the next edge pixel
		 * @param nextYEdgePixelLoc    output Y of the next edge pixel
		 * @param cur_x_loc            X of the current feature pixel
		 * @param cur_y_loc            Y of the current feature pixel
		 * @param currentXEdgePixelLoc X of the current edge pixel
		 * @param currentYEdgePixelLoc Y of the current edge pixel
		 * @param scanClock            {@link ILfs#SCAN_CLOCKWISE} or {@link ILfs#SCAN_COUNTER_CLOCKWISE}
		 * @param binarizedImageData   binary image
		 * @param imageWidth           image width in pixels
		 * @param imageHeight          image height in pixels
		 * @return {@link ILfs#TRUE} if a next pixel was found, {@link ILfs#FALSE} otherwise
		 */
		@SuppressWarnings({ "java:S107" })
		public int nextContourPixel(AtomicInteger nextXPixelLoc, AtomicInteger nextYPixelLoc,
				AtomicInteger nextXEdgePixelLoc, AtomicInteger nextYEdgePixelLoc, final int cur_x_loc,
				final int cur_y_loc, final int currentXEdgePixelLoc, final int currentYEdgePixelLoc,
				final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Returns the 8-neighbor index of the next pixel relative to the previous pixel (NIST {@code start_scan_nbr}).
		 *
		 * @param previousXPixelLoc X of the previous (center) pixel
		 * @param previousYPixelLoc Y of the previous (center) pixel
		 * @param nextXPixelLoc     X of the neighboring pixel
		 * @param nextYPixelLoc     Y of the neighboring pixel
		 * @return neighbor index 0..7, or {@link ILfs#INVALID_DIR} if the pixels are not neighbors
		 */
		public int startScanNbr(final int previousXPixelLoc, final int previousYPixelLoc, final int nextXPixelLoc,
				final int nextYPixelLoc);

		/**
		 * Advances an 8-neighbor index one step in the given rotation (NIST {@code next_scan_nbr}).
		 *
		 * @param nbrIndex  current neighbor index (0..7)
		 * @param scanClock {@link ILfs#SCAN_CLOCKWISE} or {@link ILfs#SCAN_COUNTER_CLOCKWISE}
		 * @return the next neighbor index (0..7)
		 */
		public int nextScanNbr(final int nbrIndex, final int scanClock);

		/**
		 * Finds the contour point with the minimum angle formed with points {@code angleEdge} steps before and after
		 * it (NIST {@code min_contour_theta}).
		 *
		 * @param oMinContourPoint    output index of the point with minimum angle
		 * @param oMinThetaAngle      output minimum angle in radians
		 * @param angleEdge           number of steps to the points used to form the angle
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 * @return {@code 0} on success, {@link ILfs#IGNORE} if the contour is too short
		 */
		public int minContourTheta(AtomicInteger oMinContourPoint, AtomicReference<Double> oMinThetaAngle,
				final int angleEdge, AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour);

		/**
		 * Computes the bounding box of a contour (NIST {@code contour_limits}).
		 *
		 * @param xMin                output minimum X
		 * @param yMin                output minimum Y
		 * @param xMax                output maximum X
		 * @param yMax                output maximum Y
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 */
		public void contourLimits(AtomicInteger xMin, AtomicInteger yMin, AtomicInteger xMax, AtomicInteger yMax,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfPointsInContour);

		/**
		 * Ensures a feature/edge pixel pair is a proper pair of opposite-colored neighbors, adjusting them if needed
		 * (NIST {@code fix_edge_pixel_pair}).
		 *
		 * @param featureXPixel      in/out X of the feature pixel
		 * @param featureYPixel      in/out Y of the feature pixel
		 * @param featureEdgeXPixel  in/out X of the edge pixel
		 * @param featureEdgeYPixel  in/out Y of the edge pixel
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void fixEdgePixelPair(AtomicInteger featureXPixel, AtomicInteger featureYPixel,
				AtomicInteger featureEdgeXPixel, AtomicInteger featureEdgeYPixel, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);
	}

	/** Top-level minutiae detection (port of NIST's {@code detect.c}); implemented by {@code mindtct.Detect}. */
	public interface IDetect {
		/**
		 * Detects minutiae in a grayscale image using the version 2 algorithm (NIST {@code lfs_detect_minutiae_V2}).
		 * <p>
		 * Pads the image, generates the direction, low-contrast, low-flow and high-curvature maps via DFT analysis,
		 * binarizes the image, detects minutiae, removes false minutiae and counts ridges between neighbors.
		 *
		 * @param ret                   output return code: {@code 0} on success, negative on system error
		 * @param oMinutiae             output detected minutiae
		 * @param map                   output maps (direction, low-contrast, low-flow, high-curvature) and their size
		 * @param oBinarizedImageWidth  output width of the binarized image
		 * @param oBinarizedImageHeight output height of the binarized image
		 * @param imageData             8-bit grayscale input image
		 * @param imageWidth            image width in pixels
		 * @param imageHeight           image height in pixels
		 * @param lfsParams             LFS parameters
		 * @return the binarized image, or {@code null} on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] lfsDetectMinutiaeV2(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps map,
				AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight, int[] imageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);
	}

	/**
	 * Discrete Fourier Transform analysis of image blocks (port of NIST's {@code dft.c}); implemented by
	 * {@code mindtct.Dft}.
	 */
	public interface IDft {
		/**
		 * Computes the DFT power of a block for each wave form and each rotated-grid direction (NIST
		 * {@code dft_dir_powers}).
		 *
		 * @param powers            output power matrix indexed {@code [wave][direction]}
		 * @param paddedImageData   padded grayscale image
		 * @param blockOffset       offset of the block origin in the padded image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param dftWaves          DFT wave forms
		 * @param dftGrids          rotated DFT grids
		 * @return {@code 0} on success, negative on system error ({@link ILfs#ERROR_CODE_91})
		 */
		public int dftDirPowers(AtomicReferenceArray<Double[]> powers, int[] paddedImageData, final int blockOffset,
				final int paddedImageWidth, final int paddedImageHeight, DftWaves dftWaves, RotGrids dftGrids);

		/**
		 * Sums the pixels of each row of a rotated grid laid over a block (NIST {@code sum_rot_block_rows}).
		 *
		 * @param rowSums              output row sums (length {@code blockOffsetSize})
		 * @param paddedImageData      padded grayscale image
		 * @param paddedImageDataIndex offset of the block origin in the padded image
		 * @param gridOffsets          pixel offsets of the rotated grid
		 * @param blockOffsetSize      block size in pixels
		 */
		public void sumRotBlockRows(int[] rowSums, int[] paddedImageData, final int paddedImageDataIndex,
				final AtomicIntegerArray gridOffsets, final int blockOffsetSize);

		/**
		 * Computes the DFT power of a row-sum vector for one wave form (NIST {@code dft_power}).
		 *
		 * @param power   output power (squared magnitude of the cosine and sine correlations)
		 * @param rowSums row sums of the rotated block
		 * @param dftWave wave form (cosine and sine components)
		 * @param waveLen wave length in samples
		 */
		public void computeDftPower(AtomicReference<Double> power, final int[] rowSums, final DftWave dftWave,
				final int waveLen);

		/**
		 * Computes power statistics (maximum power, its direction and normalized power) for waves {@code fw..tw-1} and
		 * sorts them (NIST {@code dft_power_stats}).
		 *
		 * @param wis        output wave indices sorted by power statistics
		 * @param powMaxs    output maximum power per wave
		 * @param powmaxDirs output direction of maximum power per wave
		 * @param powNorms   output normalized power per wave
		 * @param powers     power matrix indexed {@code [wave][direction]}
		 * @param fw         first wave index (inclusive)
		 * @param tw         last wave index (exclusive)
		 * @param nDirs      number of directions
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int getDftPowerStats(AtomicIntegerArray wis, AtomicReferenceArray<Double> powMaxs,
				AtomicIntegerArray powmaxDirs, AtomicReferenceArray<Double> powNorms,
				AtomicReferenceArray<Double[]> powers, final int fw, final int tw, final int nDirs);

		/**
		 * Finds the maximum power, its direction, and the normalized power (max over mean) of a power vector (NIST
		 * {@code get_max_norm}).
		 *
		 * @param powmax      output maximum power
		 * @param powmaxDir   output direction of maximum power
		 * @param pownorm     output normalized power
		 * @param powerVector powers for each direction
		 * @param nDirs       number of directions
		 */
		public void getMaxNorm(AtomicReference<Double> powmax, AtomicInteger powmaxDir, AtomicReference<Double> pownorm,
				final AtomicReferenceArray<Double> powerVector, final int nDirs);

		/**
		 * Sorts wave statistics by normalized power, decreasing (NIST {@code sort_dft_waves}).
		 *
		 * @param wis      output wave indices in sorted order
		 * @param powMaxs  maximum power per wave
		 * @param powNorms normalized power per wave
		 * @param nStats   number of statistics
		 * @return {@code 0} on success, negative on system error ({@link ILfs#ERROR_CODE_100})
		 */
		public int sortDftWaves(AtomicIntegerArray wis, final AtomicReferenceArray<Double> powMaxs,
				final AtomicReferenceArray<Double> powNorms, final int nStats);
	}

	/** Resource release helpers (port of NIST's {@code free.c}); implemented by {@code mindtct.Free}. */
	public interface IFree {
		/**
		 * Releases a generic object.
		 *
		 * @param object object to release
		 */
		public void free(Object object);

		/**
		 * Releases direction-to-radian lookup tables.
		 *
		 * @param dir2Rad tables to release
		 */
		public void freeDirToRad(DirToRad dir2Rad);

		/**
		 * Releases DFT wave forms.
		 *
		 * @param dftWaves wave forms to release
		 */
		public void freeDftWaves(DftWaves dftWaves);

		/**
		 * Releases rotated grids.
		 *
		 * @param rotGrids grids to release
		 */
		public void freeRotGrids(RotGrids rotGrids);

		/**
		 * Releases a DFT power matrix.
		 *
		 * @param powers power matrix to release
		 * @param nWaves number of waves (rows)
		 */
		public void freeDirPowers(AtomicReferenceArray<Double[]> powers, final int nWaves);
	}

	/**
	 * MINDTCT minutiae extraction entry point (port of NIST's {@code getmin.c}); implemented by
	 * {@code mindtct.GetMinutiae}.
	 */
	public interface IGetMinutiae {
		/**
		 * Detects minutiae and computes the image maps, quality map and minutia reliabilities (NIST
		 * {@code get_minutiae}).
		 *
		 * @param ret                   output return code: {@code 0} on success, negative on error
		 *                              ({@link ILfs#ERROR_CODE_02} if the image is not 8 bits deep)
		 * @param oMinutiae             output detected minutiae with reliabilities
		 * @param imageMap              output direction, low-contrast, low-flow and high-curvature maps
		 * @param qualityMap            output quality map
		 * @param oBinarizedImageWidth  output width of the binarized image
		 * @param oBinarizedImageHeight output height of the binarized image
		 * @param oBinarizedImageDepth  output depth of the binarized image
		 * @param imageData             8-bit grayscale input image
		 * @param imageWidth            image width in pixels
		 * @param imageHeight           image height in pixels
		 * @param imageDepth            image depth in bits (must be 8)
		 * @param imagePPI              image resolution in pixels per millimeter
		 * @param lfsParams             LFS parameters
		 * @return the binarized image, or {@code null} on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] getMinutiae(AtomicInteger ret, AtomicReference<Minutiae> oMinutiae, Maps imageMap,
				Quality qualityMap, AtomicInteger oBinarizedImageWidth, AtomicInteger oBinarizedImageHeight,
				AtomicInteger oBinarizedImageDepth, int[] imageData, final int imageWidth, final int imageHeight,
				final int imageDepth, final double imagePPI, final LfsParams lfsParams);
	}

	/** Image utilities (port of NIST's {@code imgutil.c}); implemented by {@code mindtct.ImageUtil}. */
	public interface IImageUtil {
		/**
		 * Converts 6-bit pixel values to 8 bits in place (multiplies by 4).
		 *
		 * @param imageData   image data, modified in place
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 */
		public void bits6To8(int[] imageData, int imageWidth, int imageHeight);

		/**
		 * Converts 8-bit pixel values to 6 bits in place (divides by 4).
		 *
		 * @param imageData   image data, modified in place
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 */
		public void bits8To6(int[] imageData, int imageWidth, int imageHeight);

		/**
		 * Thresholds an image in place (NIST {@code gray2bin}).
		 *
		 * @param thresh             threshold value
		 * @param less_pix           value assigned to pixels below the threshold
		 * @param greater_pix        value assigned to pixels at or above the threshold
		 * @param binarizedImageData image data, modified in place
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void grayToBinary(final int thresh, final int less_pix, final int greater_pix, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);

		/**
		 * Copies an image into a larger buffer with a border of {@code pad} pixels filled with {@code padValue} (NIST
		 * {@code pad_uchar_image}).
		 *
		 * @param ret          output return code: {@code 0} on success, negative on system error
		 * @param oImageWidth  output padded width
		 * @param oImageHeight output padded height
		 * @param imageData    input image
		 * @param imageWidth   input width in pixels
		 * @param imageHeight  input height in pixels
		 * @param pad          pad size in pixels on each side
		 * @param padValue     intensity used to fill the border
		 * @return the padded image
		 */
		@SuppressWarnings({ "java:S107" })
		public int[] padImage(AtomicInteger ret, AtomicInteger oImageWidth, AtomicInteger oImageHeight, int[] imageData,
				final int imageWidth, final int imageHeight, final int pad, final int padValue);

		/**
		 * Fills one-pixel holes in horizontal and vertical runs of a binary image, in place (NIST {@code fill_holes}).
		 *
		 * @param binarizedImageData binary image, modified in place
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void fillHoles(int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Determines whether the straight path between two points is free of obstacles, i.e. crosses at most
		 * {@code maxTrans} pixel transitions (NIST {@code free_path}).
		 *
		 * @param x1                 X of the first point
		 * @param y1                 Y of the first point
		 * @param x2                 X of the second point
		 * @param y2                 Y of the second point
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters ({@code maxTrans})
		 * @return {@link ILfs#TRUE} if the path is free, {@link ILfs#FALSE} if not, or negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int freePath(final int x1, final int y1, final int x2, final int y2, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		/**
		 * Steps from a starting point along a direction until a pixel of the given value is found (NIST
		 * {@code search_in_direction}).
		 *
		 * @param ox                 output X of the found pixel
		 * @param oy                 output Y of the found pixel
		 * @param oex                output X of the adjacent edge pixel
		 * @param oey                output Y of the adjacent edge pixel
		 * @param pix                pixel value searched for
		 * @param strt_x             starting X
		 * @param strt_y             starting Y
		 * @param delta_x            X increment per step
		 * @param delta_y            Y increment per step
		 * @param maxsteps           maximum number of steps
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@link ILfs#TRUE} if found, {@link ILfs#FALSE} otherwise
		 */
		@SuppressWarnings({ "java:S107" })
		public int searchInDirection(AtomicInteger ox, AtomicInteger oy, AtomicInteger oex, AtomicInteger oey,
				final int pix, final int strt_x, final int strt_y, final double delta_x, final double delta_y,
				final int maxsteps, int[] binarizedImageData, final int imageWidth, final int imageHeight);
	}

	/** Initialization routines (port of NIST's {@code init.c}); implemented by {@code mindtct.Init}. */
	public interface IInit {
		/**
		 * Computes the image padding needed for version 1 maps and binarization (NIST {@code get_max_padding}).
		 *
		 * @param imapBlockSize    IMAP block size in pixels
		 * @param dirbinGridWidth  directional binarization grid width
		 * @param dirbinGridHeight directional binarization grid height
		 * @param isoBinGridDim    isotropic binarization grid dimension
		 * @return padding in pixels
		 */
		public int getMaxPadding(final int imapBlockSize, final int dirbinGridWidth, final int dirbinGridHeight,
				final int isoBinGridDim);

		/**
		 * Computes the image padding needed for version 2 maps and binarization (NIST {@code get_max_padding_V2}).
		 *
		 * @param mapWindowSize    map window size in pixels
		 * @param map_windowoffset map window offset in pixels
		 * @param dirbinGridWidth  directional binarization grid width
		 * @param dirbinGridHeight directional binarization grid height
		 * @return padding in pixels
		 */
		public int getMaxPaddingV2(final int mapWindowSize, final int map_windowoffset, final int dirbinGridWidth,
				final int dirbinGridHeight);

		/**
		 * Fills direction-to-radian lookup tables (NIST {@code init_dir2rad}).
		 *
		 * @param optr tables to initialize (size given by {@code nDirs})
		 * @return {@code 0} on success, negative on system error
		 */
		public int initDirToRad(DirToRad optr);

		/**
		 * Fills DFT wave forms from the given frequency coefficients (NIST {@code init_dftwaves}).
		 *
		 * @param optr     wave forms to initialize
		 * @param dftCoefs frequency coefficient of each wave form
		 * @return {@code 0} on success, negative on system error
		 */
		public int initDftWaves(DftWaves optr, AtomicReferenceArray<Double> dftCoefs);

		/**
		 * Computes the rotated pixel offsets of each grid orientation (NIST {@code init_rotgrids}).
		 *
		 * @param optr        rotated grids to initialize
		 * @param imageWidth  width in pixels of the (padded) image the offsets refer to
		 * @param imageHeight height in pixels of the (padded) image
		 * @param ipad        current image padding, or {@link ILfs#UNDEFINED}
		 * @return {@code 0} on success, negative on system error ({@link ILfs#ERROR_CODE_33})
		 */
		public int initRotGrids(RotGrids optr, final int imageWidth, final int imageHeight, final int ipad);

		/**
		 * Allocates a DFT power matrix (NIST {@code alloc_dir_powers}).
		 *
		 * @param ret    output return code: {@code 0} on success, negative on system error
		 * @param nWaves number of wave forms (rows)
		 * @param nDirs  number of directions (columns)
		 * @return the power matrix
		 */
		public AtomicReferenceArray<Double[]> allocDirPowers(AtomicInteger ret, final int nWaves, final int nDirs);

		/**
		 * Allocates the wave index array for DFT power statistics.
		 *
		 * @param ret    output return code: {@code 0} on success, negative on system error
		 * @param nStats number of statistics
		 * @return the allocated array
		 */
		public AtomicIntegerArray allocPowerStatsWis(AtomicInteger ret, final int nStats);

		/**
		 * Allocates the maximum-power array for DFT power statistics.
		 *
		 * @param ret    output return code: {@code 0} on success, negative on system error
		 * @param nStats number of statistics
		 * @return the allocated array
		 */
		public AtomicReferenceArray<Double> allocPowerStatsPowmaxs(AtomicInteger ret, final int nStats);

		/**
		 * Allocates the maximum-power-direction array for DFT power statistics.
		 *
		 * @param ret    output return code: {@code 0} on success, negative on system error
		 * @param nStats number of statistics
		 * @return the allocated array
		 */
		public AtomicIntegerArray allocPowerStatsPowmaxDirs(AtomicInteger ret, final int nStats);

		/**
		 * Allocates the normalized-power array for DFT power statistics.
		 *
		 * @param ret    output return code: {@code 0} on success, negative on system error
		 * @param nStats number of statistics
		 * @return the allocated array
		 */
		public AtomicReferenceArray<Double> allocPowerStatsPownorms(AtomicInteger ret, final int nStats);
	}

	/** Empty-image tests (port of NIST's {@code isempty.c}); implemented by {@code mindtct.IsEmpty}. */
	public interface IIsEmpty {
		/**
		 * Determines whether an image is empty based on its quality map.
		 *
		 * @param qualityMap quality map
		 * @param mapWidth   map width in blocks
		 * @param mapHeight  map height in blocks
		 * @return {@link ILfs#TRUE} if empty, {@link ILfs#FALSE} otherwise
		 */
		public int isImageEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight);

		/**
		 * Determines whether a quality map contains only background (level 0) blocks.
		 *
		 * @param qualityMap quality map
		 * @param mapWidth   map width in blocks
		 * @param mapHeight  map height in blocks
		 * @return {@link ILfs#TRUE} if empty, {@link ILfs#FALSE} otherwise
		 */
		public int isQualityMapEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight);
	}

	/** Line rasterization (port of NIST's {@code line.c}); implemented by {@code mindtct.Line}. */
	public interface ILine {
		/**
		 * Computes the pixel coordinates of the digital line between two points (NIST {@code line_points}).
		 *
		 * @param oxList output X coordinates
		 * @param oyList output Y coordinates
		 * @param onum   output number of points
		 * @param x1     X of the start point
		 * @param y1     Y of the start point
		 * @param x2     X of the end point
		 * @param y2     Y of the end point
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_412})
		 */
		public int linePoints(int[] oxList, int[] oyList, AtomicInteger onum, final int x1, final int y1, final int x2,
				final int y2);
	}

	/**
	 * Minutia linking (port of NIST's {@code link.c}); implemented by {@code mindtct.Link}.
	 * <p>
	 * Joins pairs of minutiae that point toward each other across a small gap, removing both as false features.
	 * Several parameters keep the generic names of the original port; their NIST meaning is given below.
	 */
	public interface ILink {
		/**
		 * Links (joins) facing minutiae across small gaps in ridges/valleys (NIST {@code link_minutiae}).
		 *
		 * @param oMinutiae list of minutiae; linked pairs are removed
		 * @param a         binary image data ({@code bdata})
		 * @param b         image width in pixels ({@code iw})
		 * @param c         image height in pixels ({@code ih})
		 * @param d         IMAP/NMAP of block values ({@code nmap})
		 * @param e         map width in blocks ({@code mw})
		 * @param f         map height in blocks ({@code mh})
		 * @param lfsParams LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int linkMinutiae(Minutiae oMinutiae, String a, final int b, final int c, AtomicInteger d, final int e,
				final int f, final LfsParams lfsParams);

		/**
		 * Builds the table of potential links between a minutia and its compatible neighbors (NIST
		 * {@code create_link_table}).
		 *
		 * @param a         output link table ({@code link_table}, {@code tbldim x tbldim} scores)
		 * @param b         output X-axis minutia indices ({@code x_axis})
		 * @param c         output Y-axis minutia indices ({@code y_axis})
		 * @param d         output number of X-axis entries ({@code nx_axis})
		 * @param e         output number of Y-axis entries ({@code ny_axis})
		 * @param f         output number of table entries ({@code n_entries})
		 * @param g         table dimension ({@code tbldim})
		 * @param h         index of the first minutia ({@code first})
		 * @param oMinutiae list of minutiae
		 * @param i         flags marking minutiae on loops ({@code onloop})
		 * @param j         IMAP/NMAP of block values ({@code nmap})
		 * @param k         map width in blocks ({@code mw})
		 * @param l         map height in blocks ({@code mh})
		 * @param m         binary image data ({@code bdata})
		 * @param n         image width in pixels ({@code iw})
		 * @param o         image height in pixels ({@code ih})
		 * @param lfsParams LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int createLinkTable(AtomicIntegerArray a, AtomicIntegerArray b, AtomicIntegerArray c, AtomicInteger d,
				AtomicInteger e, AtomicInteger f, final int g, final int h, final Minutiae oMinutiae,
				final AtomicInteger i, AtomicInteger j, final int k, final int l, String m, final int n, final int o,
				final LfsParams lfsParams);

		/**
		 * Adds a link score between two minutiae to the link table, extending the axes as needed (NIST
		 * {@code update_link_table}).
		 *
		 * @param a in/out link table ({@code link_table})
		 * @param b in/out X-axis minutia indices ({@code x_axis})
		 * @param c in/out Y-axis minutia indices ({@code y_axis})
		 * @param d in/out number of X-axis entries ({@code nx_axis})
		 * @param e in/out number of Y-axis entries ({@code ny_axis})
		 * @param f in/out number of table entries ({@code n_entries})
		 * @param g table dimension ({@code tbldim})
		 * @param h flags marking minutiae on loops ({@code onloop})
		 * @param i additional in/out table state of the port (no direct NIST counterpart)
		 * @param j additional in/out table state of the port (no direct NIST counterpart)
		 * @param k additional in/out table state of the port (no direct NIST counterpart)
		 * @param l index of the first minutia ({@code first})
		 * @param m index of the second minutia ({@code second})
		 * @param n link score ({@code score})
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int updateLinkTable(AtomicInteger a, AtomicInteger b, AtomicInteger c, AtomicInteger d, AtomicInteger e,
				AtomicInteger f, final int g, AtomicInteger h, AtomicInteger i, AtomicInteger j, AtomicInteger k,
				final int l, final int m, final int n);

		/**
		 * Sorts the link table so that the strongest links come first (NIST {@code order_link_table}).
		 *
		 * @param a         in/out link table ({@code link_table})
		 * @param b         in/out X-axis minutia indices ({@code x_axis})
		 * @param c         in/out Y-axis minutia indices ({@code y_axis})
		 * @param d         number of X-axis entries ({@code nx_axis})
		 * @param e         number of Y-axis entries ({@code ny_axis})
		 * @param f         number of table entries ({@code n_entries})
		 * @param g         table dimension ({@code tbldim})
		 * @param oMinutiae list of minutiae
		 * @param h         number of integer directions ({@code ndirs})
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int orderLinkTable(AtomicInteger a, AtomicInteger b, AtomicInteger c, final int d, final int e,
				final int f, final int g, final Minutiae oMinutiae, final int h);

		/**
		 * Processes the ordered link table, joining linked minutia pairs in the binary image and removing them (NIST
		 * {@code process_link_table}).
		 *
		 * @param a         link table ({@code link_table})
		 * @param b         X-axis minutia indices ({@code x_axis})
		 * @param c         Y-axis minutia indices ({@code y_axis})
		 * @param d         number of X-axis entries ({@code nx_axis})
		 * @param e         number of Y-axis entries ({@code ny_axis})
		 * @param f         number of table entries ({@code n_entries})
		 * @param g         table dimension ({@code tbldim})
		 * @param oMinutiae list of minutiae; linked pairs are removed
		 * @param h         flags marking minutiae on loops ({@code onloop})
		 * @param i         binary image data ({@code bdata}); join lines are drawn into it
		 * @param j         image width in pixels ({@code iw})
		 * @param k         image height in pixels ({@code ih})
		 * @param lfsParams LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processLinkTable(final AtomicInteger a, final AtomicInteger b, final AtomicInteger c, final int d,
				final int e, final int f, final int g, Minutiae oMinutiae, AtomicInteger h, String i, final int j,
				final int k, final LfsParams lfsParams);

		/**
		 * Computes the link score of two minutiae from the angle between them and their distance (NIST
		 * {@code link_score}).
		 *
		 * @param a         joining angle ({@code jointheta})
		 * @param b         joining distance ({@code joindist})
		 * @param lfsParams LFS parameters (score normalization and weight)
		 * @return the link score; larger is a stronger link
		 */
		public double linkScore(final double a, final double b, final LfsParams lfsParams);
	}

	/** General LFS utility routines (port of NIST's {@code util.c}); implemented by {@code mindtct.LfsUtil}. */
	public interface ILfsUtil {
		/**
		 * Returns the maximum value in a list.
		 *
		 * @param list values
		 * @param num  number of values
		 * @return the maximum value
		 */
		public int maxValue(final AtomicIntegerArray list, final int num);

		/**
		 * Returns the minimum value in a list.
		 *
		 * @param list values
		 * @param num  number of values
		 * @return the minimum value
		 */
		public int minValue(final AtomicIntegerArray list, final int num);

		/**
		 * Finds the local minima and maxima (and their positions) in a list of values (NIST {@code minmaxs}).
		 *
		 * @param oMinMaxValue  output extreme values
		 * @param oMinMaxType   output type of each extreme ({@code -1} minimum, {@code 1} maximum)
		 * @param oMinMaxIndex  output index of each extreme in {@code items}
		 * @param oMinMaxAlloc  output allocated size of the output lists
		 * @param oMinMaxNumber output number of extremes found
		 * @param items         input values
		 * @param num           number of input values
		 * @return {@code 0} on success, negative on system error
		 */
		public int minMaxs(AtomicIntegerArray oMinMaxValue, AtomicIntegerArray oMinMaxType,
				AtomicIntegerArray oMinMaxIndex, AtomicInteger oMinMaxAlloc, AtomicInteger oMinMaxNumber,
				AtomicIntegerArray items, final int num);

		/**
		 * Returns the Euclidean distance between two points.
		 *
		 * @param x1 X of the first point
		 * @param y1 Y of the first point
		 * @param x2 X of the second point
		 * @param y2 Y of the second point
		 * @return the distance in pixels
		 */
		public double distance(final int x1, final int y1, final int x2, final int y2);

		/**
		 * Returns the squared Euclidean distance between two points.
		 *
		 * @param x1 X of the first point
		 * @param y1 Y of the first point
		 * @param x2 X of the second point
		 * @param y2 Y of the second point
		 * @return the squared distance
		 */
		public double squaredDistance(final int x1, final int y1, final int x2, final int y2);

		/**
		 * Returns the position of a value in a list (NIST {@code in_int_list}).
		 *
		 * @param item value searched for
		 * @param list values
		 * @param len  number of values
		 * @return the index of {@code item}, or {@code -1} if not found
		 */
		public int getValueLocationInList(final int item, AtomicIntegerArray list, final int len);

		/**
		 * Removes the element at a position from a list, shifting later elements down (NIST
		 * {@code remove_from_int_list}).
		 *
		 * @param index position to remove
		 * @param list  values, modified in place
		 * @param num   number of values
		 * @return {@code 0} on success, negative if {@code index} is out of range
		 */
		public int removeValueFromLocationInList(final int index, AtomicIntegerArray list, final int num);

		/**
		 * Returns the insertion position of a value in a list sorted in increasing order (NIST
		 * {@code find_incr_position_dbl}).
		 *
		 * @param val  value to place
		 * @param list sorted values
		 * @param num  number of values
		 * @return the index at which {@code val} would be inserted
		 */
		public int findIncrementalPositionInDoubleArray(final double val, AtomicReferenceArray<Double> list,
				final int num);

		/**
		 * Returns the angle in radians of the line from one point to another (NIST {@code angle2line}).
		 *
		 * @param fx X of the from-point
		 * @param fy Y of the from-point
		 * @param tx X of the to-point
		 * @param ty Y of the to-point
		 * @return angle in radians
		 */
		public double angleToLine(final int fx, final int fy, final int tx, final int ty);

		/**
		 * Returns the integer direction of the line from one point to another (NIST {@code line2direction}).
		 *
		 * @param fx    X of the from-point
		 * @param fy    Y of the from-point
		 * @param tx    X of the to-point
		 * @param ty    Y of the to-point
		 * @param nDirs number of integer directions in a semicircle
		 * @return integer direction in {@code [0, 2 * nDirs)}
		 */
		public int lineToDirection(final int fx, final int fy, final int tx, final int ty, final int nDirs);

		/**
		 * Returns the distance between two integer directions, taking wrap-around into account (NIST
		 * {@code closest_dir_dist}).
		 *
		 * @param dir1  first direction
		 * @param dir2  second direction
		 * @param nDirs number of directions
		 * @return the closest distance, or {@link ILfs#INVALID_DIR} if either direction is invalid
		 */
		public int closestDirDistance(final int dir1, final int dir2, final int nDirs);
	}

	/** Loop (island/lake/hook) analysis (port of NIST's {@code loop.c}); implemented by {@code mindtct.Loop}. */
	public interface ILoop {
		/**
		 * Flags each minutia that lies on a small loop (NIST {@code get_loop_list}).
		 *
		 * @param onloop             output flags, one per minutia ({@link ILfs#TRUE} if on a loop)
		 * @param oMinutiae          list of minutiae
		 * @param loopLen            maximum loop length
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@code 0} on success, negative on system error
		 */
		public int getLoopList(AtomicIntegerArray onloop, AtomicReference<Minutiae> oMinutiae, final int loopLen,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Determines whether a minutia lies on a loop no longer than the given length (NIST {@code on_loop}).
		 *
		 * @param minutia            minutia to test
		 * @param max_loop_len       maximum loop length
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@link ILfs#TRUE} if on a loop, {@link ILfs#FALSE} if not, or negative on system error
		 */
		public int onLoop(final Minutia minutia, final int max_loop_len, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		/**
		 * Determines whether two minutiae lie on the same island or lake and, if so, returns its contour (NIST
		 * {@code on_island_lake}).
		 *
		 * @param ret                output: {@link ILfs#LOOP_FOUND} if on an island/lake, {@link ILfs#FALSE} if not,
		 *                           negative on system error
		 * @param oNoOfContour       output number of contour points
		 * @param minutia1           first minutia
		 * @param minutia2           second minutia
		 * @param maxHalfLoop        half the maximum loop length
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return the loop contour when found
		 */
		@SuppressWarnings({ "java:S107" })
		public IContour onIslandLake(AtomicInteger ret, AtomicInteger oNoOfContour, Minutia minutia1, Minutia minutia2,
				final int maxHalfLoop, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Determines whether two minutiae lie on the same hook (NIST {@code on_hook}).
		 *
		 * @param minutia1           first minutia
		 * @param minutia2           second minutia
		 * @param maxHookLen         maximum hook length
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@link ILfs#HOOK_FOUND} if on a hook, {@link ILfs#FALSE} if not, or negative on system error
		 */
		public int onHook(Minutia minutia1, Minutia minutia2, final int maxHookLen, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);

		/**
		 * Determines whether a closed contour is traced clockwise (NIST {@code is_loop_clockwise}).
		 *
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 * @param default_ret         value returned if the direction cannot be determined
		 * @return {@link ILfs#TRUE} if clockwise, {@link ILfs#FALSE} if not, {@code default_ret}, or negative on error
		 */
		public int isLoopClockwise(AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour, final int default_ret);

		/**
		 * Analyzes a loop contour and either adds minutiae at its extremes or fills it (NIST {@code process_loop}).
		 *
		 * @param oMinutiae           list of minutiae, possibly extended
		 * @param contourX            X coordinates of the loop contour
		 * @param contourY            Y coordinates of the loop contour
		 * @param contourEx           X coordinates of the contour edge pixels
		 * @param contourEy           Y coordinates of the contour edge pixels
		 * @param noOfPointsInContour number of contour points
		 * @param binarizedImageData  binary image, possibly modified (loop filled)
		 * @param imageWidth          image width in pixels
		 * @param imageHeight         image height in pixels
		 * @param lfsParams           LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processLoop(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray contourX,
				AtomicIntegerArray contourY, AtomicIntegerArray contourEx, AtomicIntegerArray contourEy,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				final LfsParams lfsParams);

		/**
		 * Version 2 of {@link #processLoop}, which also consults the low-flow map when assigning reliability (NIST
		 * {@code process_loop_V2}).
		 *
		 * @param oMinutiae           list of minutiae, possibly extended
		 * @param contourX            X coordinates of the loop contour
		 * @param contourY            Y coordinates of the loop contour
		 * @param contourEx           X coordinates of the contour edge pixels
		 * @param contourEy           Y coordinates of the contour edge pixels
		 * @param noOfPointsInContour number of contour points
		 * @param binarizedImageData  binary image, possibly modified (loop filled)
		 * @param imageWidth          image width in pixels
		 * @param imageHeight         image height in pixels
		 * @param plowFlowMap         pixelized low-flow map
		 * @param lfsParams           LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processLoopV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray contourX,
				AtomicIntegerArray contourY, AtomicIntegerArray contourEx, AtomicIntegerArray contourEy,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				AtomicIntegerArray plowFlowMap, final LfsParams lfsParams);

		/**
		 * Computes the minimum and maximum distances across a loop between points half way around the contour (NIST
		 * {@code get_loop_aspect}).
		 *
		 * @param ominFr              output contour index where the minimum distance starts
		 * @param ominTo              output contour index where the minimum distance ends
		 * @param ominDist            output minimum distance
		 * @param omaxFr              output contour index where the maximum distance starts
		 * @param omaxTo              output contour index where the maximum distance ends
		 * @param omaxDist            output maximum distance
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 */
		@SuppressWarnings({ "java:S107" })
		public void getLoopAspect(AtomicInteger ominFr, AtomicInteger ominTo, AtomicReference<Double> ominDist,
				AtomicInteger omaxFr, AtomicInteger omaxTo, AtomicReference<Double> omaxDist,
				AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfPointsInContour);

		/**
		 * Fills the interior of a loop in the binary image with the color of its contour (NIST {@code fill_loop}).
		 *
		 * @param a X coordinates of the loop contour ({@code contour_x})
		 * @param b Y coordinates of the loop contour ({@code contour_y})
		 * @param c number of contour points ({@code ncontour})
		 * @param d binary image data, modified in place ({@code bdata})
		 * @param e image width in pixels ({@code iw})
		 * @param f image height in pixels ({@code ih})
		 * @return {@code 0} on success, negative on system error
		 */
		public int fillLoop(final AtomicIntegerArray a, final AtomicIntegerArray b, final int c, int[] d, final int e,
				final int f);

		/**
		 * Fills a partial row of pixels between two X coordinates (NIST {@code fill_partial_row}).
		 *
		 * @param fill_pix           fill value
		 * @param frx                starting X
		 * @param tox                ending X
		 * @param y                  row
		 * @param binarizedImageData binary image, modified in place
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void fillPartialRow(final int fill_pix, final int frx, final int tox, final int y,
				int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Flood-fills the interior of a loop contour (NIST {@code flood_loop}).
		 *
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 * @param binarizedImageData  binary image, modified in place
		 * @param imageWidth          image width in pixels
		 * @param imageHeight         image height in pixels
		 */
		public void floodLoop(final AtomicIntegerArray contourX, final AtomicIntegerArray contourY,
				final int noOfPointsInContour, int[] binarizedImageData, final int imageWidth, final int imageHeight);

		/**
		 * Performs a 4-connected flood fill starting at a pixel (NIST {@code flood_fill4}).
		 *
		 * @param fill_pix           fill value
		 * @param x                  starting X
		 * @param y                  starting Y
		 * @param binarizedImageData binary image, modified in place
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void floodFill4(final int fill_pix, final int x, final int y, int[] binarizedImageData,
				final int imageWidth, final int imageHeight);
	}

	/**
	 * Image map generation (port of NIST's {@code maps.c}); implemented by {@code mindtct.Maps}.
	 * <p>
	 * Produces the direction map, low-contrast map, low-flow map and high-curvature map from DFT analysis of
	 * overlapping windows.
	 */
	public interface IMaps {
		/**
		 * Generates the direction, low-contrast, low-flow and high-curvature maps of a padded image (NIST
		 * {@code gen_image_maps}). Results are stored in the implementing {@code Maps} instance.
		 *
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param dftWaves          DFT wave forms
		 * @param dftgrids          rotated DFT grids
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_540})
		 */
		public int genImageMaps(int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight,
				DirToRad dir2Rad, DftWaves dftWaves, RotGrids dftgrids, LfsParams lfsParams);

		/**
		 * Computes the initial direction, low-contrast and low-flow maps by DFT analysis of each block's window (NIST
		 * {@code gen_initial_maps}).
		 *
		 * @param odmap             output direction map
		 * @param olcmap            output low-contrast map
		 * @param olfmap            output low-flow map
		 * @param blkoffs           block origin offsets in the padded image
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param dftWaves          DFT wave forms
		 * @param dftGrids          rotated DFT grids
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int initialiseMaps(AtomicIntegerArray odmap, AtomicIntegerArray olcmap, AtomicIntegerArray olfmap,
				AtomicIntegerArray blkoffs, final int mappedImageWidth, final int mappedImageHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves,
				final RotGrids dftGrids, final LfsParams lfsParams);

		/**
		 * Interpolates directions for INVALID blocks from their nearest valid neighbors in each direction (NIST
		 * {@code interpolate_direction_map}).
		 *
		 * @param directionMap      direction map, modified in place
		 * @param lowContrastMap    low-contrast map
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int interpolateDirectionMap(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Morphologically cleans a TRUE/FALSE map by dilation and erosion (NIST {@code morph_TF_map}).
		 *
		 * @param tfmap     TRUE/FALSE map, modified in place
		 * @param lfsParams LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int morphMapWithTF(AtomicIntegerArray tfmap, final LfsParams lfsParams);

		/**
		 * Expands a block map into a pixel map where each pixel takes its block's value (NIST {@code pixelize_map}).
		 *
		 * @param ret               output pixel map
		 * @param imageWidth        image width in pixels
		 * @param imageHeight       image height in pixels
		 * @param imap              block map
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param blockOffsetSize   block size in pixels
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_591})
		 */
		public int pixelizeMap(AtomicIntegerArray ret, int imageWidth, int imageHeight, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, final int blockOffsetSize);

		/**
		 * Smooths the direction map by replacing directions with the average of valid neighbors (NIST
		 * {@code smooth_direction_map}).
		 *
		 * @param directionMap   direction map, modified in place
		 * @param lowContrastMap low-contrast map
		 * @param dir2Rad        direction-to-radian lookup tables
		 * @param lfsParams      LFS parameters
		 */
		public void smoothDirectionMap(AtomicIntegerArray directionMap, AtomicIntegerArray lowContrastMap,
				final DirToRad dir2Rad, final LfsParams lfsParams);

		/**
		 * Generates the high-curvature map using vorticity and curvature of the direction map (NIST
		 * {@code gen_high_curve_map}).
		 *
		 * @param ohcmap            output high-curvature map
		 * @param directionMap      direction map
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int generateHighCurveMap(AtomicIntegerArray ohcmap, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Generates a version 1 IMAP (block directions) of a padded image (NIST {@code gen_imap}).
		 *
		 * @param ret               output return code: {@code 0} on success, negative on error
		 *                          ({@link ILfs#ERROR_CODE_60})
		 * @param oImageWidth       output map width in blocks
		 * @param oImageHeight      output map height in blocks
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param dftWaves          DFT wave forms
		 * @param dftGrids          rotated DFT grids
		 * @param lfsParams         LFS parameters
		 * @return the IMAP
		 */
		@SuppressWarnings({ "java:S107" })
		public AtomicIntegerArray generateInputBlockImageMap(AtomicInteger ret, AtomicInteger oImageWidth,
				AtomicInteger oImageHeight, int[] paddedImageData, final int paddedImageWidth,
				final int paddedImageHeight, final DirToRad dir2Rad, final DftWaves dftWaves, final RotGrids dftGrids,
				final LfsParams lfsParams);

		/**
		 * Computes the initial version 1 IMAP by DFT analysis of each block (NIST {@code gen_initial_imap}).
		 *
		 * @param ret               output return code: {@code 0} on success, negative on error
		 *                          ({@link ILfs#ERROR_CODE_70})
		 * @param blkoffs           block origin offsets in the padded image
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param paddedImageData   padded grayscale image
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param dftWaves          DFT wave forms
		 * @param dftGrids          rotated DFT grids
		 * @param lfsParams         LFS parameters
		 * @return the initial IMAP
		 */
		@SuppressWarnings({ "java:S107" })
		public AtomicIntegerArray initialiseInputBlockImageMap(AtomicInteger ret, AtomicIntegerArray blkoffs,
				final AtomicInteger mappedImageWidth, final AtomicInteger mappedImageHeight, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, final DftWaves dftWaves,
				final RotGrids dftGrids, final LfsParams lfsParams);

		/**
		 * Tests whether the dominant DFT wave defines a strong enough primary direction (NIST
		 * {@code primary_dir_test}).
		 *
		 * @param powers     DFT power matrix
		 * @param wis        wave indices sorted by power statistics
		 * @param powMaxs    maximum power per wave
		 * @param powmaxDirs direction of maximum power per wave
		 * @param powNorms   normalized power per wave
		 * @param nStats     number of statistics
		 * @param lfsParams  LFS parameters
		 * @return the primary direction, or {@link ILfs#INVALID_DIR}
		 */
		public int primaryDirectionTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
				final AtomicReferenceArray<Double> powMaxs, final AtomicIntegerArray powmaxDirs,
				final AtomicReferenceArray<Double> powNorms, final int nStats, final LfsParams lfsParams);

		/**
		 * Tests for a fork (two nearby strong directions) to decide the block direction when the primary test fails
		 * (NIST {@code secondary_fork_test}).
		 *
		 * @param powers     DFT power matrix
		 * @param wis        wave indices sorted by power statistics
		 * @param powMaxs    maximum power per wave
		 * @param powmaxDirs direction of maximum power per wave
		 * @param powNorms   normalized power per wave
		 * @param nStats     number of statistics
		 * @param lfsParams  LFS parameters
		 * @return the fork direction, or {@link ILfs#INVALID_DIR}
		 */
		public int secondaryForkTest(AtomicReferenceArray<Double[]> powers, final AtomicIntegerArray wis,
				final AtomicReferenceArray<Double> powMaxs, final AtomicIntegerArray powmaxDirs,
				final AtomicReferenceArray<Double> powNorms, final int nStats, final LfsParams lfsParams);

		/**
		 * Removes block directions inconsistent with their neighbors, working inward from the edges (NIST
		 * {@code remove_incon_dirs}).
		 *
		 * @param imap      IMAP, modified in place
		 * @param dir2Rad   direction-to-radian lookup tables
		 * @param lfsParams LFS parameters
		 */
		public void removeInconsistentDirs(AtomicIntegerArray imap, final DirToRad dir2Rad, final LfsParams lfsParams);

		/**
		 * Tests the top edge of a box of blocks for inconsistent directions (NIST {@code test_top_edge}).
		 *
		 * @param lbox              left block of the box
		 * @param tbox              top block of the box
		 * @param rbox              right block of the box
		 * @param bbox              bottom block of the box
		 * @param imap              IMAP, modified in place
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param lfsParams         LFS parameters
		 * @return number of directions removed
		 */
		@SuppressWarnings({ "java:S107" })
		public int testTopEdge(final int lbox, final int tbox, final int rbox, final int bbox, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad, LfsParams lfsParams);

		/**
		 * Tests the right edge of a box of blocks for inconsistent directions (NIST {@code test_right_edge}).
		 *
		 * @param lbox              left block of the box
		 * @param tbox              top block of the box
		 * @param rbox              right block of the box
		 * @param bbox              bottom block of the box
		 * @param imap              IMAP, modified in place
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param lfsParams         LFS parameters
		 * @return number of directions removed
		 */
		@SuppressWarnings({ "java:S107" })
		public int testRightEdge(final int lbox, final int tbox, final int rbox, final int bbox,
				AtomicIntegerArray imap, final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad,
				LfsParams lfsParams);

		/**
		 * Tests the bottom edge of a box of blocks for inconsistent directions (NIST {@code test_bottom_edge}).
		 *
		 * @param lbox              left block of the box
		 * @param tbox              top block of the box
		 * @param rbox              right block of the box
		 * @param bbox              bottom block of the box
		 * @param imap              IMAP, modified in place
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param lfsParams         LFS parameters
		 * @return number of directions removed
		 */
		@SuppressWarnings({ "java:S107" })
		public int testBottomEdge(final int lbox, final int tbox, final int rbox, final int bbox,
				AtomicIntegerArray imap, final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad,
				LfsParams lfsParams);

		/**
		 * Tests the left edge of a box of blocks for inconsistent directions (NIST {@code test_left_edge}).
		 *
		 * @param lbox              left block of the box
		 * @param tbox              top block of the box
		 * @param rbox              right block of the box
		 * @param bbox              bottom block of the box
		 * @param imap              IMAP, modified in place
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param lfsParams         LFS parameters
		 * @return number of directions removed
		 */
		@SuppressWarnings({ "java:S107" })
		public int testLeftEdge(final int lbox, final int tbox, final int rbox, final int bbox, AtomicIntegerArray imap,
				final int mappedImageWidth, final int mappedImageHeight, DirToRad dir2Rad, LfsParams lfsParams);

		/**
		 * Removes a block direction if it has too few valid neighbors or differs too much from their average (NIST
		 * {@code remove_dir}).
		 *
		 * @param imap              IMAP, modified in place
		 * @param mx                X of the block
		 * @param my                Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} if kept, a positive code if removed
		 */
		public int removeIMAPDirection(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final DirToRad dir2Rad, final LfsParams lfsParams);

		/**
		 * Computes the average direction and strength of the valid 8-neighbors of a block (NIST
		 * {@code average_8nbr_dir}).
		 *
		 * @param avrdir            output average direction, or {@link ILfs#INVALID_DIR}
		 * @param dirStrength       output strength of the average direction (0..1)
		 * @param nvalid            output number of valid neighbors
		 * @param imap              IMAP
		 * @param mx                X of the block
		 * @param my                Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param dir2Rad           direction-to-radian lookup tables
		 */
		@SuppressWarnings({ "java:S107" })
		public void average8NbrDir(AtomicInteger avrdir, AtomicReference<Double> dirStrength, AtomicInteger nvalid,
				AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final DirToRad dir2Rad);

		/**
		 * Counts the 8-neighbors of a block with a valid direction (NIST {@code num_valid_8nbrs}).
		 *
		 * @param imap              IMAP
		 * @param mx                X of the block
		 * @param my                Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @return number of valid neighbors (0..8)
		 */
		public int numValid8Nbrs(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight);

		/**
		 * Smooths a version 1 IMAP by averaging valid neighbor directions (NIST {@code smooth_imap}).
		 *
		 * @param imap      IMAP, modified in place
		 * @param dir2Rad   direction-to-radian lookup tables
		 * @param lfsParams LFS parameters
		 */
		public void smoothInputBlockImageMap(AtomicIntegerArray imap, final DirToRad dir2Rad,
				final LfsParams lfsParams);

		/**
		 * Generates the NMAP (IMAP with high-curvature and no-valid-neighbor markings) from an IMAP (NIST
		 * {@code gen_nmap}).
		 *
		 * @param optr              output NMAP
		 * @param imap              IMAP
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int genNMap(AtomicIntegerArray optr, AtomicIntegerArray imap, final int mappedImageWidth,
				final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Measures the vorticity (cumulative rotation of neighbor directions) around a block (NIST {@code vorticity}).
		 *
		 * @param imap              direction map
		 * @param mx                X of the block
		 * @param my                Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param nDirs             number of integer directions
		 * @return vorticity measure
		 */
		public int vorticity(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final int nDirs);

		/**
		 * Accumulates the vorticity contribution of a pair of neighbor directions (NIST {@code accum_nbr_vorticity}).
		 *
		 * @param vmeasure in/out vorticity accumulator
		 * @param dir1     first direction
		 * @param dir2     second direction
		 * @param nDirs    number of integer directions
		 */
		public void accumulateNbrVorticity(AtomicInteger vmeasure, final int dir1, final int dir2, final int nDirs);

		/**
		 * Measures the curvature of a block as the sum of direction differences to its valid neighbors (NIST
		 * {@code curvature}).
		 *
		 * @param imap              direction map
		 * @param mx                X of the block
		 * @param my                Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param nDirs             number of integer directions
		 * @return curvature measure, or {@link ILfs#INVALID_DIR}
		 */
		public int curvature(AtomicIntegerArray imap, final int mx, final int my, final int mappedImageWidth,
				final int mappedImageHeight, final int nDirs);
	}

	/**
	 * Feature-pattern matching over pixel pairs (port of NIST's {@code matchpat.c}); implemented by
	 * {@code mindtct.MatchPattern}.
	 */
	public interface IMatchPattern {
		/**
		 * Finds the feature patterns whose first pixel pair matches the given pair (NIST {@code match_1st_pair}).
		 *
		 * @param firstPixel     first pixel value of the pair
		 * @param secondPixel    second pixel value of the pair
		 * @param possible       output indices of candidate patterns
		 * @param oPossibleMatch output number of candidate patterns
		 * @return number of candidate patterns
		 */
		public int matchFirstPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		/**
		 * Narrows candidate patterns to those whose second pixel pair matches (NIST {@code match_2nd_pair}).
		 *
		 * @param firstPixel     first pixel value of the pair
		 * @param secondPixel    second pixel value of the pair
		 * @param possible       in/out indices of candidate patterns
		 * @param oPossibleMatch in/out number of candidate patterns
		 * @return number of remaining candidate patterns
		 */
		public int matchSecondPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		/**
		 * Narrows candidate patterns to those whose third pixel pair matches (NIST {@code match_3rd_pair}).
		 *
		 * @param firstPixel     first pixel value of the pair
		 * @param secondPixel    second pixel value of the pair
		 * @param possible       in/out indices of candidate patterns
		 * @param oPossibleMatch in/out number of candidate patterns
		 * @return number of remaining candidate patterns
		 */
		public int matchThirdPair(int firstPixel, int secondPixel, AtomicIntegerArray possible,
				AtomicInteger oPossibleMatch);

		/**
		 * Skips repeated identical horizontal pixel pairs along a scan (NIST {@code skip_repeated_horizontal_pair}).
		 *
		 * @param cx                 in/out current X position
		 * @param ex                 X limit of the scan
		 * @param binarizedImageData binary image
		 * @param p1ptr              in/out index of the top pixel of the pair
		 * @param p2ptr              in/out index of the bottom pixel of the pair
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 */
		public void skipRepeatedHorizontalPair(AtomicInteger cx, final int ex, int[] binarizedImageData,
				AtomicInteger p1ptr, AtomicInteger p2ptr, final int imageWidth, final int imageHeight);

		/**
		 * Skips repeated identical vertical pixel pairs along a scan (NIST {@code skip_repeated_vertical_pair}).
		 *
		 * @param currentYPixelIndex      in/out current Y position
		 * @param currentBottomYPixelIndex Y limit of the scan
		 * @param binarizedImageData      binary image
		 * @param currentLeftPixelIndex   in/out index of the left pixel of the pair
		 * @param currentRightPixelIndex  in/out index of the right pixel of the pair
		 * @param imageWidth              image width in pixels
		 * @param imageHeight             image height in pixels
		 */
		public void skipRepeatedVerticalPair(AtomicInteger currentYPixelIndex, final int currentBottomYPixelIndex,
				int[] binarizedImageData, AtomicInteger currentLeftPixelIndex, AtomicInteger currentRightPixelIndex,
				final int imageWidth, final int imageHeight);
	}

	/**
	 * Minutia detection and bookkeeping (port of NIST's {@code minutia.c}); implemented by
	 * {@code mindtct.MinutiaHelper}.
	 * <p>
	 * Scans the binary image horizontally and vertically for the ten feature patterns, creates candidate minutiae,
	 * adjusts high-curvature minutiae, and maintains/sorts/dumps the minutiae list. Some parameters keep the
	 * generic names of the original port; their NIST meaning is given below.
	 */
	public interface IMinutia {
		/**
		 * Allocates an empty minutiae list with the given capacity (NIST {@code alloc_minutiae}).
		 *
		 * @param oMinutiae    output minutiae list
		 * @param max_minutiae initial capacity
		 * @return {@code 0} on success, negative on system error
		 */
		public int allocMinutiae(AtomicReference<Minutiae> oMinutiae, final int max_minutiae);

		/**
		 * Increases the capacity of a minutiae list (NIST {@code realloc_minutiae}).
		 *
		 * @param oMinutiae    minutiae list to grow
		 * @param incr_minutiae number of entries to add
		 * @return {@code 0} on success, negative on system error
		 */
		public int reallocMinutiae(AtomicReference<Minutiae> oMinutiae, final int incr_minutiae);

		/**
		 * Detects minutiae in a binary image using the version 2 maps (NIST {@code detect_minutiae_V2}).
		 *
		 * @param oMinutiae          output list, extended with detected minutiae
		 * @param binarizedImageData binary image
		 * @param mappedImageWidth   image width in pixels
		 * @param mappedImageHeight  image height in pixels
		 * @param map                direction, low-flow and high-curvature maps
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int detectMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int mappedImageWidth, final int mappedImageHeight, Maps map, LfsParams lfsParams);

		/**
		 * Adds a minutia to the list unless a similar one is already present (NIST {@code update_minutiae}).
		 *
		 * @param oMinutiae          minutiae list
		 * @param minutia            candidate minutia
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} if added, {@link ILfs#IGNORE} if a similar minutia exists, negative on system error
		 */
		public int updateMinutiae(AtomicReference<Minutiae> oMinutiae, Minutia minutia, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		/**
		 * Version 2 of {@link #updateMinutiae}, which also considers the scan direction and block direction (NIST
		 * {@code update_minutiae_V2}).
		 *
		 * @param oMinutiae          minutiae list
		 * @param minutia            candidate minutia
		 * @param scanDir            {@link ILfs#SCAN_HORIZONTAL} or {@link ILfs#SCAN_VERTICAL}
		 * @param dmapval            direction map value at the minutia
		 * @param binarizedImageData binary image
		 * @param mappedImageWidth   image width in pixels
		 * @param mappedImageHeight  image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} if added, {@link ILfs#IGNORE} if a similar minutia exists, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int updateMinutiaeV2(AtomicReference<Minutiae> oMinutiae, Minutia minutia, final int scanDir,
				final int dmapval, int[] binarizedImageData, final int mappedImageWidth, final int mappedImageHeight,
				final LfsParams lfsParams);

		/**
		 * Sorts minutiae top-to-bottom, then left-to-right (NIST {@code sort_minutiae_y_x}).
		 *
		 * @param oMinutiae   minutiae list, sorted in place
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @return {@code 0} on success, negative on system error
		 */
		public int sortMinutiaeTopToBottomAndThenLeftToRight(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
				final int imageHeight);

		/**
		 * Sorts minutiae left-to-right, then top-to-bottom (NIST {@code sort_minutiae_x_y}).
		 *
		 * @param oMinutiae   minutiae list, sorted in place
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @return {@code 0} on success, negative on system error
		 */
		public int sortMinutiaeLeftToRightAndThenTopToBottom(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
				final int imageHeight);

		/**
		 * Removes minutiae duplicated at the same location, keeping one (NIST {@code rm_dup_minutiae}).
		 *
		 * @param oMinutiae minutiae list, sorted, modified in place
		 * @return {@code 0} on success, negative on system error
		 */
		public int removeRedundantMinutiae(AtomicReference<Minutiae> oMinutiae);

		/**
		 * Writes a detailed listing of the minutiae to a file (NIST {@code dump_minutiae}).
		 *
		 * @param file      destination file
		 * @param oMinutiae minutiae list
		 */
		public void dumpMinutiae(File file, final AtomicReference<Minutiae> oMinutiae);

		/**
		 * Writes the X/Y/direction of each minutia to a file (NIST {@code dump_minutiae_pts}).
		 *
		 * @param file      destination file
		 * @param oMinutiae minutiae list
		 */
		public void dumpMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae);

		/**
		 * Writes the points of minutiae with reliability above a threshold to a file (NIST
		 * {@code dump_reliable_minutiae_pts}).
		 *
		 * @param file        destination file
		 * @param oMinutiae   minutiae list
		 * @param reliability minimum reliability of minutiae to write
		 */
		public void dumpReliableMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae,
				final double reliability);

		/**
		 * Creates a minutia record (NIST {@code create_minutia}).
		 *
		 * @param xPixelLoc     X of the minutia point
		 * @param yPixelLoc     Y of the minutia point
		 * @param xEdgePixelLoc X of the adjacent edge pixel
		 * @param yEdgePixelLoc Y of the adjacent edge pixel
		 * @param imapDirection minutia direction (integer direction)
		 * @param reliability   minutia reliability (0..1)
		 * @param type          {@link ILfs#BIFURCATION} or {@link ILfs#RIDGE_ENDING}
		 * @param appearing     {@link ILfs#APPEARING} or {@link ILfs#DISAPPEARING}
		 * @param featureId     ID of the detecting feature pattern
		 * @return the new minutia
		 */
		@SuppressWarnings({ "java:S107" })
		public Minutia createMinutia(final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc, final int imapDirection, final double reliability, final int type,
				final int appearing, final int featureId);

		/**
		 * Releases a minutiae list (NIST {@code free_minutiae}).
		 *
		 * @param oMinutiae minutiae list to release
		 */
		public void freeMinutiae(AtomicReference<Minutiae> oMinutiae);

		/**
		 * Releases a single minutia (NIST {@code free_minutia}).
		 *
		 * @param minutia minutia to release
		 */
		public void freeMinutia(Minutia minutia);

		/**
		 * Removes the minutia at a position from the list (NIST {@code remove_minutia}).
		 *
		 * @param index     position of the minutia to remove
		 * @param ominutiae minutiae list, modified in place
		 * @return {@code 0} on success, {@link ILfs#ERROR_CODE_380} if {@code index} is out of range
		 */
		public int removeMinutia(final int index, AtomicReference<Minutiae> ominutiae);

		/**
		 * Draws a line in the binary image joining two minutiae, erasing the gap between them (NIST
		 * {@code join_minutia}).
		 *
		 * @param minutia1           first minutia
		 * @param minutia2           second minutia
		 * @param binarizedImageData binary image, modified in place
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param with_boundary      {@link ILfs#WITH_BOUNDARY} or {@link ILfs#NO_BOUNDARY}
		 * @param line_radius        radial width added to the line
		 * @return {@code 0} on success, negative on system error
		 */
		public int joinMinutia(Minutia minutia1, Minutia minutia2, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int with_boundary, final int line_radius);

		/**
		 * Determines the minutia type from the feature pixel value (NIST {@code minutia_type}).
		 *
		 * @param feature_pix feature pixel value
		 * @return {@link ILfs#RIDGE_ENDING} for a black feature pixel, {@link ILfs#BIFURCATION} otherwise
		 */
		public int getMinutiaType(final int feature_pix);

		/**
		 * Determines whether a feature appears or disappears given its feature and edge pixels (NIST
		 * {@code is_minutia_appearing}).
		 *
		 * @param xPixelLoc     X of the feature pixel
		 * @param yPixelLoc     Y of the feature pixel
		 * @param xEdgePixelLoc X of the edge pixel
		 * @param yEdgePixelLoc Y of the edge pixel
		 * @return {@link ILfs#APPEARING}, {@link ILfs#DISAPPEARING}, or {@link ILfs#ERROR_CODE_240}
		 */
		public int isMinutiaAppearing(final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
				final int yEdgePixelLoc);

		/**
		 * Chooses the scan direction (horizontal or vertical) most orthogonal to the block direction (NIST
		 * {@code choose_scan_direction}).
		 *
		 * @param imapval block direction
		 * @param nDirs   number of integer directions
		 * @return {@link ILfs#SCAN_HORIZONTAL} or {@link ILfs#SCAN_VERTICAL}
		 */
		public int chooseScanDirection(final int imapval, final int nDirs);

		/**
		 * Scans a block for minutiae in the chosen direction and rescans neighbors as needed (NIST
		 * {@code scan4minutiae}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imap               IMAP
		 * @param mapDirectionArr    NMAP
		 * @param blkX               X of the block
		 * @param blkY               Y of the block
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param scanDir            {@link ILfs#SCAN_HORIZONTAL} or {@link ILfs#SCAN_VERTICAL}
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiae(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray imap, AtomicIntegerArray mapDirectionArr, final int blkX,
				final int blkY, final int mappedImageWidth, final int mappedImageHeight, final int scanX,
				final int scanY, final int scanW, final int scanH, final int scanDir, final LfsParams lfsParams);

		/**
		 * Scans a region horizontally for feature patterns (NIST {@code scan4minutiae_horizontally}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imapval            IMAP value of the block
		 * @param nmapval            NMAP value of the block
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final int imapval, final int nmapval, final int scanX,
				final int scanY, final int scanW, final int scanH, final LfsParams lfsParams);

		/**
		 * Scans the whole binary image horizontally for feature patterns using pixelized maps (NIST
		 * {@code scan4minutiae_horizontally_V2}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param pdirectionMap      pixelized direction map
		 * @param plowFlowMap        pixelized low-flow map
		 * @param phighCurveMap      pixelized high-curvature map
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeHorizontallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray pdirectionMap,
				AtomicIntegerArray plowFlowMap, AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		/**
		 * Scans a region vertically for feature patterns (NIST {@code scan4minutiae_vertically}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imapval            IMAP value of the block
		 * @param nmapval            NMAP value of the block
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeVertically(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final int imapval, final int nmapval, final int scanX,
				final int scanY, final int scanW, final int scanH, final LfsParams lfsParams);

		/**
		 * Rescans a block horizontally, extending into neighbors with the same scan direction (NIST
		 * {@code rescan4minutiae_horizontally}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imap               IMAP
		 * @param mapDirectionArr    NMAP
		 * @param blkX               X of the block
		 * @param blkY               Y of the block
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int rescanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		/**
		 * Scans the whole binary image vertically for feature patterns using pixelized maps (NIST
		 * {@code scan4minutiae_vertically_V2}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param pdirectionMap      pixelized direction map
		 * @param plowFlowMap        pixelized low-flow map
		 * @param phighCurveMap      pixelized high-curvature map
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int scanForMinutiaeVerticallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray pdirectionMap,
				AtomicIntegerArray plowFlowMap, AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		/**
		 * Rescans a block vertically, extending into neighbors with the same scan direction (NIST
		 * {@code rescan4minutiae_vertically}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imap               IMAP
		 * @param mapDirectionArr    NMAP
		 * @param blkX               X of the block
		 * @param blkY               Y of the block
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int rescanForMinutiaeVertically(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		/**
		 * Rescans part of a block horizontally toward a neighbor (NIST {@code rescan_partial_horizontally}).
		 *
		 * @param nbrDir             neighbor direction ({@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST},
		 *                           {@link ILfs#WEST})
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imap               IMAP
		 * @param mapDirectionArr    NMAP
		 * @param blkX               X of the block
		 * @param blkY               Y of the block
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int rescanPartialHorizontally(final int nbrDir, AtomicReference<Minutiae> oMinutiae,
				int[] binarizedImageData, final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		/**
		 * Rescans part of a block vertically toward a neighbor (NIST {@code rescan_partial_vertically}).
		 *
		 * @param nbrDir             neighbor direction ({@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST},
		 *                           {@link ILfs#WEST})
		 * @param oMinutiae          minutiae list, extended
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imap               IMAP
		 * @param mapDirectionArr    NMAP
		 * @param blkX               X of the block
		 * @param blkY               Y of the block
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param scanX              X of the scan region origin
		 * @param scanY              Y of the scan region origin
		 * @param scanW              scan region width
		 * @param scanH              scan region height
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int rescanPartialVertically(final int nbrDir, AtomicReference<Minutiae> oMinutiae,
				int[] binarizedImageData, final int imageWidth, final int imageHeight, AtomicIntegerArray imap,
				AtomicIntegerArray mapDirectionArr, final int blkX, final int blkY, final int mappedImageWidth,
				final int mappedImageHeight, final int scanX, final int scanY, final int scanW, final int scanH,
				final LfsParams lfsParams);

		/**
		 * Computes the map index of a neighboring block (NIST {@code get_nbr_block_index}).
		 *
		 * @param blki              output index of the neighbor block
		 * @param nbrDir            neighbor direction ({@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST},
		 *                          {@link ILfs#WEST})
		 * @param blkx              X of the block
		 * @param blky              Y of the block
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @return {@link ILfs#FOUND}, {@link ILfs#NOT_FOUND} at the map edge, or {@link ILfs#ERROR_CODE_200}
		 */
		public int getNbrBlockIndex(AtomicInteger blki, final int nbrDir, final int blkx, final int blky,
				final int mappedImageWidth, final int mappedImageHeight);

		/**
		 * Computes the region to rescan horizontally toward a neighbor (NIST {@code adjust_horizontal_rescan}).
		 *
		 * @param a neighbor direction ({@code nbr_dir})
		 * @param b output rescan X ({@code rescan_x})
		 * @param c output rescan Y ({@code rescan_y})
		 * @param d output rescan width ({@code rescan_w})
		 * @param e output rescan height ({@code rescan_h})
		 * @param f scan region X ({@code scan_x})
		 * @param g scan region Y ({@code scan_y})
		 * @param h scan region width ({@code scan_w})
		 * @param i scan region height ({@code scan_h})
		 * @param j block size in pixels ({@code blocksize})
		 * @return {@code 0} on success, {@link ILfs#ERROR_CODE_210} for an illegal neighbor direction
		 */
		@SuppressWarnings({ "java:S107" })
		public int adjustHorizontalRescan(final int a, AtomicInteger b, AtomicInteger c, AtomicInteger d,
				AtomicInteger e, final int f, final int g, final int h, final int i, final int j);

		/**
		 * Computes the region to rescan vertically toward a neighbor (NIST {@code adjust_vertical_rescan}).
		 *
		 * @param nbrDir          neighbor direction
		 * @param rescanX         output rescan X
		 * @param rescanY         output rescan Y
		 * @param rescanW         output rescan width
		 * @param rescanH         output rescan height
		 * @param scanX           scan region X
		 * @param scanY           scan region Y
		 * @param scanW           scan region width
		 * @param scanH           scan region height
		 * @param blockOffsetSize block size in pixels
		 * @return {@code 0} on success, {@link ILfs#ERROR_CODE_220} for an illegal neighbor direction
		 */
		@SuppressWarnings({ "java:S107" })
		public int adjustVerticalRescan(final int nbrDir, AtomicInteger rescanX, AtomicInteger rescanY,
				AtomicInteger rescanW, AtomicInteger rescanH, final int scanX, final int scanY, final int scanW,
				final int scanH, final int blockOffsetSize);

		/**
		 * Processes a feature pattern matched during a horizontal scan, creating a minutia (NIST
		 * {@code process_horizontal_scan_minutia}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param cx                 X where the pattern match ended
		 * @param cy                 Y of the scan row
		 * @param x2                 X where the pattern match started
		 * @param featureId          ID of the matched feature pattern
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imapval            IMAP value of the block
		 * @param nmapval            NMAP value of the block
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE}, or negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processHorizontalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int x2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int imapval, final int nmapval, final LfsParams lfsParams);

		/**
		 * Version 2 of {@link #processHorizontalScanMinutia} using pixelized maps (NIST
		 * {@code process_horizontal_scan_minutia_V2}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param cx                 X where the pattern match ended
		 * @param cy                 Y of the scan row
		 * @param x2                 X where the pattern match started
		 * @param featureId          ID of the matched feature pattern
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param pdirectionMap      pixelized direction map
		 * @param plowFlowMap        pixelized low-flow map
		 * @param phighCurveMap      pixelized high-curvature map
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE}, or negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processHorizontalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int x2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray pdirectionMap, AtomicIntegerArray plowFlowMap,
				AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		/**
		 * Processes a feature pattern matched during a vertical scan, creating a minutia (NIST
		 * {@code process_vertical_scan_minutia}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param cx                 X of the scan column
		 * @param cy                 Y where the pattern match ended
		 * @param y2                 Y where the pattern match started
		 * @param featureId          ID of the matched feature pattern
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param imapval            IMAP value of the block
		 * @param nmapval            NMAP value of the block
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE}, or negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processVerticalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int y2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final int imapval, final int nmapval, final LfsParams lfsParams);

		/**
		 * Version 2 of {@link #processVerticalScanMinutia} using pixelized maps (NIST
		 * {@code process_vertical_scan_minutia_V2}).
		 *
		 * @param oMinutiae          minutiae list, extended
		 * @param cx                 X of the scan column
		 * @param cy                 Y where the pattern match ended
		 * @param y2                 Y where the pattern match started
		 * @param featureId          ID of the matched feature pattern
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param pdirectionMap      pixelized direction map
		 * @param plowFlowMap        pixelized low-flow map
		 * @param phighCurveMap      pixelized high-curvature map
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE}, or negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int processVerticalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
				final int y2, final int featureId, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray pdirectionMap, AtomicIntegerArray plowFlowMap,
				AtomicIntegerArray phighCurveMap, final LfsParams lfsParams);

		/**
		 * Adjusts the location and direction of a minutia in a high-curvature area using its contour (NIST
		 * {@code adjust_high_curvature_minutia}).
		 *
		 * @param oidir              output adjusted direction
		 * @param oxLoc              output adjusted X of the feature pixel
		 * @param oyLoc              output adjusted Y of the feature pixel
		 * @param oxEdge             output adjusted X of the edge pixel
		 * @param oyEdge             output adjusted Y of the edge pixel
		 * @param xPixelLoc          X of the feature pixel
		 * @param yPixelLoc          Y of the feature pixel
		 * @param xEdgePixelLoc      X of the edge pixel
		 * @param yEdgePixelLoc      Y of the edge pixel
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param oMinutiae          minutiae list (loops may add minutiae)
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE} if the minutia should be discarded, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int adjustHighCurvatureMinutia(AtomicInteger oidir, AtomicInteger oxLoc, AtomicInteger oyLoc,
				AtomicInteger oxEdge, AtomicInteger oyEdge, final int xPixelLoc, final int yPixelLoc,
				final int xEdgePixelLoc, final int yEdgePixelLoc, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicReference<Minutiae> oMinutiae, final LfsParams lfsParams);

		/**
		 * Version 2 of {@link #adjustHighCurvatureMinutia} that also consults the low-flow map (NIST
		 * {@code adjust_high_curvature_minutia_V2}).
		 *
		 * @param oidir              output adjusted direction
		 * @param oxLoc              output adjusted X of the feature pixel
		 * @param oyLoc              output adjusted Y of the feature pixel
		 * @param oxEdge             output adjusted X of the edge pixel
		 * @param oyEdge             output adjusted Y of the edge pixel
		 * @param xPixelLoc          X of the feature pixel
		 * @param yPixelLoc          Y of the feature pixel
		 * @param xEdgePixelLoc      X of the edge pixel
		 * @param yEdgePixelLoc      Y of the edge pixel
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param plowFlowMap        pixelized low-flow map
		 * @param oMinutiae          minutiae list (loops may add minutiae)
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, {@link ILfs#IGNORE} if the minutia should be discarded, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int adjustHighCurvatureMinutiaV2(AtomicInteger oidir, AtomicInteger oxLoc, AtomicInteger oyLoc,
				AtomicInteger oxEdge, AtomicInteger oyEdge, final int xPixelLoc, final int yPixelLoc,
				final int xEdgePixelLoc, final int yEdgePixelLoc, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, AtomicIntegerArray plowFlowMap, AtomicReference<Minutiae> oMinutiae,
				final LfsParams lfsParams);

		/**
		 * Determines the direction of a minutia in a low-curvature area from the block direction and the feature's
		 * appearance (NIST {@code get_low_curvature_direction}).
		 *
		 * @param a scan direction ({@code scan_dir})
		 * @param b whether the feature is appearing ({@code appearing})
		 * @param c block direction ({@code imapval})
		 * @param d number of integer directions ({@code ndirs})
		 * @return the minutia direction
		 */
		public int getLowCurvatureDirection(final int a, final int b, final int c, final int d);
	}

	/** Binary morphology (port of NIST's {@code morph.c}); implemented by {@code mindtct.Morph}. */
	public interface IMorph {
		/**
		 * Erodes a binary (0/1) image with a 4-neighbor structuring element (NIST {@code erode_charimage_2}).
		 *
		 * @param inputImageData  input image
		 * @param outputImageData output image
		 * @param imageWidth      image width in pixels
		 * @param imageHeight     image height in pixels
		 */
		public void erodeImage2(int[] inputImageData, int[] outputImageData, final int imageWidth,
				final int imageHeight);

		/**
		 * Dilates a binary (0/1) image with a 4-neighbor structuring element (NIST {@code dilate_charimage_2}).
		 *
		 * @param inputImageData  input image
		 * @param outputImageData output image
		 * @param imageWidth      image width in pixels
		 * @param imageHeight     image height in pixels
		 */
		public void dilateImage2(int[] inputImageData, int[] outputImageData, final int imageWidth,
				final int imageHeight);

		/**
		 * Returns the value of the pixel south of the given pixel (NIST {@code get_south8_2}).
		 *
		 * @param inputImageData      image data
		 * @param inputImageDataIndex index of the current pixel
		 * @param row                 row of the current pixel
		 * @param imageWidth          image width in pixels
		 * @param imageHeight         image height in pixels
		 * @param failCode            value returned at the image border
		 * @return the neighbor's value, or {@code failCode}
		 */
		public int getSouth82(int[] inputImageData, int inputImageDataIndex, final int row, final int imageWidth,
				final int imageHeight, final int failCode);

		/**
		 * Returns the value of the pixel north of the given pixel (NIST {@code get_north8_2}).
		 *
		 * @param inputImageData      image data
		 * @param inputImageDataIndex index of the current pixel
		 * @param row                 row of the current pixel
		 * @param imageWidth          image width in pixels
		 * @param failCode            value returned at the image border
		 * @return the neighbor's value, or {@code failCode}
		 */
		public int getNorth82(int[] inputImageData, int inputImageDataIndex, final int row, final int imageWidth,
				final int failCode);

		/**
		 * Returns the value of the pixel east of the given pixel (NIST {@code get_east8_2}).
		 *
		 * @param inputImageData      image data
		 * @param inputImageDataIndex index of the current pixel
		 * @param col                 column of the current pixel
		 * @param imageWidth          image width in pixels
		 * @param failCode            value returned at the image border
		 * @return the neighbor's value, or {@code failCode}
		 */
		public int getEast82(int[] inputImageData, int inputImageDataIndex, final int col, final int imageWidth,
				final int failCode);

		/**
		 * Returns the value of the pixel west of the given pixel (NIST {@code get_west8_2}).
		 *
		 * @param inputImageData      image data
		 * @param inputImageDataIndex index of the current pixel
		 * @param col                 column of the current pixel
		 * @param failCode            value returned at the image border
		 * @return the neighbor's value, or {@code failCode}
		 */
		public int getWest82(int[] inputImageData, int inputImageDataIndex, final int col, final int failCode);
	}

	/**
	 * Quality map and minutia reliability (port of NIST's {@code quality.c}); implemented by
	 * {@code mindtct.Quality}.
	 */
	public interface IQuality {
		/**
		 * Combines the direction, low-contrast, low-flow and high-curvature maps into a quality map with levels 0..4
		 * (NIST {@code gen_quality_map}).
		 *
		 * @param map maps to combine; the result is stored in the implementing {@code Quality} instance
		 * @return {@code 0} on success, negative on system error
		 */
		public int generateQualityMap(Maps map);

		/**
		 * Assigns each minutia a reliability combining the quality map with local grayscale statistics (NIST
		 * {@code combined_minutia_quality}).
		 *
		 * @param oMinutiae       minutiae list; reliabilities are updated
		 * @param map             image maps (quality map)
		 * @param blockOffsetSize block size in pixels
		 * @param imageData       grayscale image
		 * @param imageWidth      image width in pixels
		 * @param imageHeight     image height in pixels
		 * @param imageDepth      image depth in bits (must be 8)
		 * @param imagePPI        image resolution in pixels per millimeter
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int combinedMinutiaQuality(AtomicReference<Minutiae> oMinutiae, Maps map, final int blockOffsetSize,
				int[] imageData, final int imageWidth, final int imageHeight, final int imageDepth,
				final double imagePPI);

		/**
		 * Computes a reliability from the mean and standard deviation of the neighborhood of a minutia compared with
		 * {@link ILfs#IDEALMEAN} and {@link ILfs#IDEALSTDEV} (NIST {@code grayscale_reliability}).
		 *
		 * @param minutia     minutia
		 * @param imageData   grayscale image
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @param radiusPixel neighborhood radius in pixels
		 * @return reliability in {@code [0, 1]}
		 */
		double grayscaleReliability(Minutia minutia, int[] imageData, final int imageWidth, final int imageHeight,
				final int radiusPixel);

		/**
		 * Computes the mean and standard deviation of pixel values in a square neighborhood of a minutia (NIST
		 * {@code get_neighborhood_stats}).
		 *
		 * @param mean        output mean
		 * @param stdev       output standard deviation
		 * @param minutia     minutia at the neighborhood center
		 * @param imageData   grayscale image
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @param radiusPixel neighborhood radius in pixels
		 */
		public void getNeighborhoodStats(AtomicReference<Double> mean, AtomicReference<Double> stdev, Minutia minutia,
				int[] imageData, final int imageWidth, final int imageHeight, final int radiusPixel);

		/**
		 * Assigns minutia reliabilities from the quality map only (NIST {@code reliability_fr_quality_map}).
		 *
		 * @param oMinutiae       minutiae list; reliabilities are updated
		 * @param map             image maps (quality map)
		 * @param imageWidth      image width in pixels
		 * @param imageHeight     image height in pixels
		 * @param blockOffsetSize block size in pixels
		 * @return {@code 0} on success, negative on system error
		 */
		public int reliabilityFromQualityMap(Minutiae oMinutiae, Maps map, final int imageWidth, final int imageHeight,
				final int blockOffsetSize);
	}

	/** False minutia removal (port of NIST's {@code remove.c}); implemented by {@code mindtct.RemoveMinutia}. */
	public interface IRemoveMinutia {
		/**
		 * Removes false minutiae using the version 2 rules: islands/lakes, holes, pointing-to-invalid-block, near
		 * invalid blocks, side minutiae, hooks, overlaps, malformations and pores (NIST
		 * {@code remove_false_minutia_V2}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param map                direction, low-flow and high-curvature maps
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int removeFalseMinutiaV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, Maps map, final int mappedImageWidth,
				final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Removes minutiae on small holes (NIST {@code remove_holes}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int removeHoles(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		/**
		 * Removes pairs of minutiae forming hooks (NIST {@code remove_hooks}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_641})
		 */
		public int removeHooks(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		/**
		 * Removes minutiae forming hooks, islands, lakes and overlaps in a single pass (NIST
		 * {@code remove_hooks_islands_lakes_overlaps}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image, possibly modified
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_301})
		 */
		public int removeHooksIslandsLakesOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		/**
		 * Removes pairs of minutiae on islands and lakes, filling them in the binary image (NIST
		 * {@code remove_islands_and_lakes}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image, possibly modified
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_611})
		 */
		public int removeIslandsAndLakes(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, LfsParams lfsParams);

		/**
		 * Removes irregularly shaped (malformed) minutiae (NIST {@code remove_malformations}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lowFlowMap         low-flow map
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int removeMalformations(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, AtomicIntegerArray lowFlowMap, int mappedImageWidth, int mappedImageHeight,
				LfsParams lfsParams);

		/**
		 * Removes minutiae too close to blocks with invalid ridge flow (NIST {@code remove_near_invblock_V2}).
		 *
		 * @param oMinutiae         minutiae list, modified in place
		 * @param directionMap      direction map
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_620})
		 */
		public int removeNearInvblocksV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Removes minutiae pointing toward a block with invalid ridge flow (NIST {@code remove_pointing_invblock_V2}).
		 *
		 * @param oMinutiae         minutiae list, modified in place
		 * @param directionMap      direction map
		 * @param mappedImageWidth  map width in blocks
		 * @param mappedImageHeight map height in blocks
		 * @param lfsParams         LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int removePointingInvblockV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);

		/**
		 * Removes pairs of minutiae forming overlaps (NIST {@code remove_overlaps}).
		 *
		 * @param oMinutiae minutiae list, modified in place
		 * @param a         binary image data ({@code bdata})
		 * @param b         image width in pixels ({@code iw})
		 * @param c         image height in pixels ({@code ih})
		 * @param lfsParams LFS parameters
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_651})
		 */
		public int removeOverlaps(AtomicReference<Minutiae> oMinutiae, int[] a, final int b, final int c,
				final LfsParams lfsParams);

		/**
		 * Removes minutiae located on sweat pores (NIST {@code remove_pores_V2}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param directionMap       direction map
		 * @param lowFlowMap         low-flow map
		 * @param highCurveMap       high-curvature map
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int removePoresV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
				int imageHeight, AtomicIntegerArray directionMap, AtomicIntegerArray lowFlowMap,
				AtomicIntegerArray highCurveMap, int mappedImageWidth, int mappedImageHeight, LfsParams lfsParams);

		/**
		 * Removes or relocates minutiae detected on the side of a ridge or valley (NIST
		 * {@code remove_or_adjust_side_minutiae_V2}).
		 *
		 * @param oMinutiae          minutiae list, modified in place
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param directionMap       direction map
		 * @param mappedImageWidth   map width in blocks
		 * @param mappedImageHeight  map height in blocks
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int removeOrAdjustSideMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, AtomicIntegerArray directionMap,
				final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams);
	}

	/**
	 * Result output and debugging visualization (port of NIST's {@code results.c}); implemented by
	 * {@code mindtct.Results}.
	 */
	public interface IResults {
		/**
		 * Writes the minutiae and all image maps to text files (NIST {@code write_text_results}).
		 *
		 * @param file            base output file
		 * @param m1flag          non-zero to write minutiae in M1 (ANSI INCITS 378) representation
		 * @param imageWidth      image width in pixels
		 * @param imageHeight     image height in pixels
		 * @param oMinutiae       minutiae list
		 * @param oQualityMap     quality map
		 * @param oDirectionMap   direction map
		 * @param oLowContrastMap low-contrast map
		 * @param oLowFlowMap     low-flow map
		 * @param oHighCurveMap   high-curvature map
		 * @param mapWidth        map width in blocks
		 * @param mapHeight       map height in blocks
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int writeTextResults(File file, final int m1flag, final int imageWidth, final int imageHeight,
				final AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oQualityMap,
				AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowContrastMap, AtomicIntegerArray oLowFlowMap,
				AtomicIntegerArray oHighCurveMap, final int mapWidth, final int mapHeight);

		/**
		 * Writes minutiae as X/Y/theta/quality records (NIST {@code write_minutiae_XYTQ}).
		 *
		 * @param file        output file
		 * @param repType     {@link ILfs#NIST_INTERNAL_XYT_REP} or {@link ILfs#M1_XYT_REP}
		 * @param oMinutiae   minutiae list
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @return {@code 0} on success, negative on error
		 */
		public int writeMinutiaeXYTQ(File file, final int repType, final AtomicReference<Minutiae> oMinutiae,
				final int imageWidth, final int imageHeight);

		/**
		 * Writes a block map as text, one row per line (NIST {@code dump_map}).
		 *
		 * @param file      output file
		 * @param oMap      map values
		 * @param mapWidth  map width in blocks
		 * @param mapHeight map height in blocks
		 * @throws IOException if the file cannot be written
		 */
		public void dumpMap(File file, AtomicIntegerArray oMap, final int mapWidth, final int mapHeight)
				throws IOException;

		/**
		 * Draws the IMAP directions onto an image for visualization (NIST {@code drawimap}).
		 *
		 * @param oInputBlockImageMap IMAP
		 * @param mapWidth            map width in blocks
		 * @param mapHeight           map height in blocks
		 * @param imageData           image to draw on, modified in place
		 * @param imageWidth          image width in pixels
		 * @param imageHeight         image height in pixels
		 * @param rotGrids            rotated grids used to draw directions
		 * @param drawPixel           intensity used to draw
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int drawInputBlockImageMap(AtomicIntegerArray oInputBlockImageMap, final int mapWidth,
				final int mapHeight, int[] imageData, final int imageWidth, final int imageHeight,
				final RotGrids rotGrids, final int drawPixel);

		/**
		 * Draws the IMAP directions as line segments onto a padded image (NIST {@code drawimap2}).
		 *
		 * @param oInputBlockImageMap IMAP
		 * @param oBlockOffsets       block origin offsets
		 * @param mapWidth            map width in blocks
		 * @param mapHeight           map height in blocks
		 * @param paddedImageData     padded image to draw on, modified in place
		 * @param paddedImageWidth    padded image width in pixels
		 * @param paddedImageHeight   padded image height in pixels
		 * @param startAngle          angle in radians of direction 0
		 * @param nDirs               number of integer directions
		 * @param blocksize           block size in pixels
		 */
		@SuppressWarnings({ "java:S107" })
		public void drawInputBlockImageMap2(AtomicIntegerArray oInputBlockImageMap,
				final AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight, int[] paddedImageData,
				final int paddedImageWidth, final int paddedImageHeight, final double startAngle, final int nDirs,
				final int blocksize);

		/**
		 * Draws block outlines onto a padded image (NIST {@code drawblocks}).
		 *
		 * @param oBlockOffsets     block origin offsets
		 * @param mapWidth          map width in blocks
		 * @param mapHeight         map height in blocks
		 * @param paddedImageData   padded image to draw on, modified in place
		 * @param paddedImageWidth  padded image width in pixels
		 * @param paddedImageHeight padded image height in pixels
		 * @param drawPixel         intensity used to draw
		 */
		public void drawBlocks(final AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight,
				int[] paddedImageData, final int paddedImageWidth, final int paddedImageHeight, final int drawPixel);

		/**
		 * Draws one rotated grid onto an image (NIST {@code drawrotgrid}).
		 *
		 * @param rotGrids    rotated grids
		 * @param nDir        direction of the grid to draw
		 * @param imageData   image to draw on, modified in place
		 * @param blockOffset offset of the block origin
		 * @param imageWidth  image width in pixels
		 * @param imageHeight image height in pixels
		 * @param drawPixel   intensity used to draw
		 * @return {@code 0} on success, {@link ILfs#ERROR_CODE_140} if {@code nDir} is out of range
		 */
		public int drawRotGrid(final RotGrids rotGrids, final int nDir, int[] imageData, final int blockOffset,
				final int imageWidth, final int imageHeight, final int drawPixel);

		/**
		 * Writes the contents of a minutia link table (NIST {@code dump_link_table}).
		 *
		 * @param file      output file
		 * @param linkTable link table scores
		 * @param xAxis     X-axis minutia indices
		 * @param yAxis     Y-axis minutia indices
		 * @param nxAxis    number of X-axis entries
		 * @param nyAxis    number of Y-axis entries
		 * @param tblDim    table dimension
		 * @param oMinutiae minutiae list
		 */
		@SuppressWarnings({ "java:S107" })
		public void dumpLinkTable(File file, final int[] linkTable, final int[] xAxis, final int[] yAxis,
				final int nxAxis, final int nyAxis, final int tblDim, final AtomicReference<Minutiae> oMinutiae);

		/**
		 * Draws the direction map onto an image and writes it to a file (debugging aid).
		 *
		 * @param fileName      output file name
		 * @param oDirectionMap direction map
		 * @param oBlockOffsets block origin offsets
		 * @param mapWidth      map width in blocks
		 * @param mapHeight     map height in blocks
		 * @param blocksize     block size in pixels
		 * @param imageData     image to draw on
		 * @param imageWidth    image width in pixels
		 * @param imageHeight   image height in pixels
		 * @param flag          drawing option flag
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int drawDirectionMap(StringBuilder fileName, AtomicIntegerArray oDirectionMap,
				AtomicIntegerArray oBlockOffsets, final int mapWidth, final int mapHeight, final int blocksize,
				int[] imageData, final int imageWidth, final int imageHeight, final int flag);

		/**
		 * Draws a TRUE/FALSE map onto an image and writes it to a file (debugging aid).
		 *
		 * @param fileName      output file name
		 * @param oMap          TRUE/FALSE map
		 * @param oBlockOffsets block origin offsets
		 * @param mapWidth      map width in blocks
		 * @param mapHeight     map height in blocks
		 * @param blocksize     block size in pixels
		 * @param imageData     image to draw on
		 * @param imageWidth    image width in pixels
		 * @param imageHeight   image height in pixels
		 * @param flag          drawing option flag
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int drawTFMap(StringBuilder fileName, AtomicIntegerArray oMap, AtomicIntegerArray oBlockOffsets,
				final int mapWidth, final int mapHeight, final int blocksize, int[] imageData, final int imageWidth,
				final int imageHeight, final int flag);
	}

	/** Neighbor search and ridge counting (port of NIST's {@code ridges.c}); implemented by {@code mindtct.Ridges}. */
	public interface IRidges {
		/**
		 * Finds the nearest neighbors of every minutia and counts the ridges between them (NIST
		 * {@code count_minutiae_ridges}).
		 *
		 * @param oMinutiae          minutiae list; neighbor and ridge-count fields are set
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int countMinutiaeRidges(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		/**
		 * Finds the nearest neighbors of one minutia and counts the ridges to each (NIST {@code count_minutia_ridges}).
		 *
		 * @param first              index of the minutia
		 * @param oMinutiae          minutiae list
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return {@code 0} on success, negative on system error
		 */
		public int countMinutiaRidges(final int first, AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
				final int imageWidth, final int imageHeight, final LfsParams lfsParams);

		/**
		 * Finds up to {@code maxNbrs} nearest neighbors below a minutia in the sorted list (NIST
		 * {@code find_neighbors}).
		 *
		 * @param oNbrList  output neighbor indices
		 * @param oNoOfNbrs output number of neighbors
		 * @param maxNbrs   maximum number of neighbors
		 * @param first     index of the minutia
		 * @param oMinutiae minutiae list (sorted)
		 * @return {@code 0} on success, negative on system error
		 */
		public int findNeighbors(AtomicIntegerArray oNbrList, AtomicInteger oNoOfNbrs, final int maxNbrs,
				final int first, AtomicReference<Minutiae> oMinutiae);

		/**
		 * Updates the nearest-neighbor list with a candidate if it is closer than the current entries (NIST
		 * {@code update_nbr_dists}).
		 *
		 * @param nbrList     in/out neighbor indices
		 * @param nbrSqrDists in/out squared distances to the neighbors
		 * @param noOfNbrs    in/out number of neighbors
		 * @param maxNbrs     maximum number of neighbors
		 * @param first       index of the minutia
		 * @param second      index of the candidate neighbor
		 * @param oMinutiae   minutiae list
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_470}, {@link ILfs#ERROR_CODE_471})
		 */
		public int updateNbrDists(AtomicIntegerArray nbrList, AtomicReferenceArray<Double> nbrSqrDists,
				AtomicInteger noOfNbrs, final int maxNbrs, final int first, final int second,
				AtomicReference<Minutiae> oMinutiae);

		/**
		 * Inserts a neighbor into the sorted neighbor lists at a position (NIST {@code insert_neighbor}).
		 *
		 * @param nbrListPos  insertion position
		 * @param nbrIndex    index of the neighbor minutia
		 * @param nbrDist2    squared distance to the neighbor
		 * @param nbrList     in/out neighbor indices
		 * @param nbrSqrDists in/out squared distances
		 * @param noOfNbrs    in/out number of neighbors
		 * @param maxNbrs     maximum number of neighbors
		 * @return {@code 0} on success, negative on error ({@link ILfs#ERROR_CODE_480}, {@link ILfs#ERROR_CODE_481})
		 */
		public int insertNeighbor(final int nbrListPos, final int nbrIndex, final double nbrDist2,
				AtomicIntegerArray nbrList, AtomicReferenceArray<Double> nbrSqrDists, AtomicInteger noOfNbrs,
				final int maxNbrs);

		/**
		 * Sorts neighbors by angle around a minutia (NIST {@code sort_neighbors}).
		 *
		 * @param nbrList           in/out neighbor indices
		 * @param noOfNbrs          number of neighbors
		 * @param firstMinutiaIndex index of the minutia
		 * @param oMinutiae         minutiae list
		 * @return {@code 0} on success, negative on system error
		 */
		public int sortNeighbors(AtomicIntegerArray nbrList, final int noOfNbrs, final int firstMinutiaIndex,
				AtomicReference<Minutiae> oMinutiae);

		/**
		 * Counts the ridges crossed by the line between two minutiae (NIST {@code ridge_count}).
		 *
		 * @param firstMinutiaIndex  index of the first minutia
		 * @param secondMinutiaIndex index of the second minutia
		 * @param oMinutiae          minutiae list
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param lfsParams          LFS parameters
		 * @return the ridge count (non-negative), or negative on system error
		 */
		public int ridgeCount(final int firstMinutiaIndex, final int secondMinutiaIndex,
				AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
				final int imageHeight, final LfsParams lfsParams);

		/**
		 * Finds the next transition between two pixel values along a list of points (NIST {@code find_transition}).
		 *
		 * @param startPixel         in/out index in the point list where the search starts; updated past the transition
		 * @param firstPixel         pixel value before the transition
		 * @param secondPixel        pixel value after the transition
		 * @param xlist              X coordinates of the points
		 * @param ylist              Y coordinates of the points
		 * @param num                number of points
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @return {@link ILfs#TRUE} if found, {@link ILfs#FALSE} otherwise
		 */
		@SuppressWarnings({ "java:S107" })
		public int findTransition(AtomicInteger startPixel, final int firstPixel, final int secondPixel,
				final int[] xlist, final int[] ylist, final int num, int[] binarizedImageData, final int imageWidth,
				final int imageHeight);

		/**
		 * Validates that a detected ridge segment is a true crossing by tracing its contours (NIST
		 * {@code validate_ridge_crossing}).
		 *
		 * @param ridgeStart         index in the point list where the ridge starts
		 * @param ridgeEnd           index in the point list where the ridge ends
		 * @param xlist              X coordinates of the points
		 * @param ylist              Y coordinates of the points
		 * @param num                number of points
		 * @param binarizedImageData binary image
		 * @param imageWidth         image width in pixels
		 * @param imageHeight        image height in pixels
		 * @param maxRidgeSteps      maximum contour steps
		 * @return {@link ILfs#TRUE} if valid, {@link ILfs#FALSE} if not, negative on system error
		 */
		@SuppressWarnings({ "java:S107" })
		public int validateRidgeCrossing(final int ridgeStart, final int ridgeEnd, final int[] xlist, final int[] ylist,
				final int num, int[] binarizedImageData, final int imageWidth, final int imageHeight,
				final int maxRidgeSteps);
	}

	/** Shape (filled contour) utilities (port of NIST's {@code shape.c}); implemented by {@code mindtct.Shapes}. */
	public interface IShapes {
		/**
		 * Allocates a shape spanning the given bounding box (NIST {@code alloc_shape}).
		 *
		 * @param ret  output return code: {@code 0} on success, negative on system error
		 * @param xMin minimum X
		 * @param yMin minimum Y
		 * @param xMax maximum X
		 * @param yMax maximum Y
		 * @return the allocated shape
		 */
		public Shape allocShape(AtomicInteger ret, int xMin, int yMin, int xMax, int yMax);

		/**
		 * Releases a shape (NIST {@code free_shape}).
		 *
		 * @param shape shape to release
		 */
		public void freeShape(Shape shape);

		/**
		 * Writes the rows of a shape to a file (NIST {@code dump_shape}).
		 *
		 * @param file  output file
		 * @param shape shape to dump
		 */
		public void dumpShape(File file, final Shape shape);

		/**
		 * Builds a shape from a closed contour (NIST {@code shape_from_contour}).
		 *
		 * @param ret                 output return code: {@code 0} on success, negative on system error
		 * @param contourX            X coordinates of the contour
		 * @param contourY            Y coordinates of the contour
		 * @param noOfPointsInContour number of contour points
		 * @return the shape
		 */
		public Shape shapeFromContour(AtomicInteger ret, AtomicIntegerArray contourX, AtomicIntegerArray contourY,
				final int noOfPointsInContour);

		/**
		 * Sorts the X coordinates of a shape row in increasing order (NIST {@code sort_row_on_x}).
		 *
		 * @param row row to sort in place
		 */
		public void sortRowLeftToRightOnX(Rows row);
	}

	/** Sorting utilities (port of NIST's {@code sort.c}); implemented by {@code mindtct.Sort}. */
	public interface ISort {
		/**
		 * Computes the order of indices that sorts integer ranks increasingly (NIST {@code sort_indices_int_inc}).
		 *
		 * @param order output indices in sorted order
		 * @param ranks ranks to sort by
		 * @param num   number of ranks
		 * @return {@code 0} on success, negative on system error
		 */
		public int sortIndicesIntArrayIncremental(AtomicIntegerArray order, AtomicIntegerArray ranks, final int num);

		/**
		 * Computes the order of indices that sorts double ranks increasingly (NIST {@code sort_indices_double_inc}).
		 *
		 * @param order output indices in sorted order
		 * @param ranks ranks to sort by
		 * @param num   number of ranks
		 * @return {@code 0} on success, negative on system error
		 */
		public int sortIndicesDoubleArrayIncremental(AtomicIntegerArray order, AtomicReferenceArray<Double> ranks,
				final int num);

		/**
		 * Bubble-sorts integer ranks increasingly, permuting items in parallel (NIST {@code bubble_sort_int_inc_2}).
		 *
		 * @param ranks ranks, sorted in place
		 * @param items items permuted with the ranks
		 * @param len   number of entries
		 */
		public void bubbleSortIntArrayIncremental2(AtomicIntegerArray ranks, AtomicIntegerArray items, final int len);

		/**
		 * Bubble-sorts double ranks increasingly, permuting items in parallel (NIST {@code bubble_sort_double_inc_2}).
		 *
		 * @param ranks ranks, sorted in place
		 * @param items items permuted with the ranks
		 * @param len   number of entries
		 */
		public void bubbleSortDoubleArrayIncremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
				final int len);

		/**
		 * Bubble-sorts double ranks decreasingly, permuting items in parallel (NIST {@code bubble_sort_double_dec_2}).
		 *
		 * @param ranks ranks, sorted in place
		 * @param items items permuted with the ranks
		 * @param len   number of entries
		 */
		public void bubbleSortDoubleArrayDecremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
				final int len);

		/**
		 * Bubble-sorts integer ranks increasingly (NIST {@code bubble_sort_int_inc}).
		 *
		 * @param ranks ranks, sorted in place
		 * @param len   number of entries
		 */
		public void bubbleSortIntArrayIncremental(AtomicIntegerArray ranks, final int len);
	}
}