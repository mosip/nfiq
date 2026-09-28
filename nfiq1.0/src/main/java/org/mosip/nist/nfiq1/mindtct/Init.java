package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWave;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.IInit;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialization routines for the lookup tables and work buffers used by
 * MINDTCT.
 * <p>
 * Port of NIST LFS {@code init.c}. Computes the image padding required by the
 * LFS processes, and initializes the direction-to-radian table, the DFT wave
 * forms, the rotated pixel grids used by DFT analysis and binarization, and the
 * buffers that hold DFT power vectors and their statistics.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class Init extends MindTct implements IInit {
	/** SLF4J logger for error reporting in this class. */
	private static final Logger logger = LoggerFactory.getLogger(Init.class);
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Init instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Init() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Init} instance
	 */
	public static synchronized Init getInstance() {
		if (instance == null) {
			instance = new Init();
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Defs} helper (rounding and precision
	 * truncation).
	 *
	 * @return the {@code Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Determines the maximum amount of image pixel padding required by all LFS
	 * (version 1) processes.
	 * <p>
	 * NIST: {@code get_max_padding()}. Padding is currently required by the
	 * rotated grids used in DFT analyses, the rotated grids used in directional
	 * binarization, and the grid used for isotropic binarization. The NIST
	 * generalized code enables the parameters governing these processes to be
	 * redefined, so a check at runtime is required to determine which process
	 * requires the most padding. By using the maximum as the padding factor, all
	 * processes will run safely with a single padding of the input image,
	 * avoiding the need to repad for further processes. Intermediate values are
	 * truncated to {@link ILfs#TRUNC_SCALE} for cross-platform consistency.
	 *
	 * @param iMapBlockSize    the size (in pixels) of each IMAP block in the
	 *                         image
	 * @param dirBinGridWidth  the width (in pixels) of the rotated grids used in
	 *                         directional binarization
	 * @param dirBinGridHeight the height (in pixels) of the rotated grids used in
	 *                         directional binarization
	 * @param isobin_grid_dim  the dimension (in pixels) of the square grid used
	 *                         in isotropic binarization
	 * @return non-negative: the maximum padding (in pixels) required for all
	 *         processes
	 */
	public int getMaxPadding(final int iMapBlockSize, final int dirBinGridWidth, final int dirBinGridHeight,
			final int isobin_grid_dim) {
		int dftPad;
		int dirBinPad;
		int isoBinPad;
		int maxPad;
		double diag, pad;

		/* Compute pad required for rotated blocks used in DFT analyses. */
		diag = Math.sqrt((2.0 * iMapBlockSize * iMapBlockSize));
		/* Compute pad as difference between the IMAP blocksize */
		/* and the diagonal distance of the block. */
		/* Assumption: all block origins reside in valid/allocated memory. */
		/* DFT grids are computed with pixel offsets RELATIVE2ORIGIN. */
		pad = (diag - iMapBlockSize) / 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
		dftPad = getDefs().sRound(pad);

		/* Compute pad required for rotated blocks used in directional */
		/* binarization. */
		diag = Math.sqrt((double) ((dirBinGridWidth * dirBinGridWidth) + (dirBinGridHeight * dirBinGridHeight)));
		/* Assumption: all grid centers reside in valid/allocated memory. */
		/* dirbin grids are computed with pixel offsets RELATIVE2CENTER. */
		pad = (diag - 1) / (double) 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
		dirBinPad = getDefs().sRound(pad);

		/* Compute pad required for grids used in isotropic binarization. */
		pad = (isobin_grid_dim - 1) / (double) 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
		isoBinPad = getDefs().sRound((isobin_grid_dim - 1) / (double) 2.0);

		maxPad = Math.max(dftPad, dirBinPad);
		maxPad = Math.max(maxPad, isoBinPad);

		/* Return the maximum of the three required paddings. This padding will */
		/* be sufficiently large for all three purposes, so that padding of the */
		/* input image will only be required once. */
		return (maxPad);
	}

	/**
	 * Determines the maximum amount of image pixel padding required by all LFS
	 * (version 2) processes.
	 * <p>
	 * NIST: {@code get_max_padding_V2()}. Padding is currently required by the
	 * rotated grids used in DFT analyses and in directional binarization. The
	 * NIST generalized code enables the parameters governing these processes to
	 * be redefined, so a check at runtime is required to determine which process
	 * requires the most padding. By using the maximum as the padding factor, all
	 * processes will run safely with a single padding of the input image,
	 * avoiding the need to repad for further processes. The DFT pad is the
	 * rotational pad of the window plus {@code mapWindowOffset}.
	 *
	 * @param mapWindowSize    the size (in pixels) of each window centered about
	 *                         each block in the image used in DFT analyses
	 * @param mapWindowOffset  the offset (in pixels) from the origin of the
	 *                         surrounding window to the origin of the block
	 * @param dirBinGridWidth  the width (in pixels) of the rotated grids used in
	 *                         directional binarization
	 * @param dirBinGridHeight the height (in pixels) of the rotated grids used in
	 *                         directional binarization
	 * @return non-negative: the maximum padding (in pixels) required for all
	 *         processes
	 */
	public int getMaxPaddingV2(final int mapWindowSize, final int mapWindowOffset, final int dirBinGridWidth,
			final int dirBinGridHeight) {
		int dftPad;
		int dirBinPad;
		int maxPad;
		double diag;
		double pad;

		/* 1. Compute pad required for rotated windows used in DFT analyses. */

		/*
		 * Explanation of DFT padding: B--------------------- | window | | | | | |
		 * A.......______|__________ | : : | |<-C-->: block: | <--|--D-->: : | image |
		 * ........ | | | | | | | | | | ---------------------- | | | Pixel A = Origin of
		 * entire fingerprint image = Also origin of first block in image. Each pixel in
		 * this block gets the same DFT results computed from the surrounding window.
		 * Note that in general blocks are adjacent and non-overlapping. Pixel B =
		 * Origin of surrounding window in which DFT analysis is conducted. Note that
		 * this window is not completely contained in the image but extends to the top
		 * and to the right. Distance C = Number of pixels in which the window extends
		 * beyond the image (mapWindowOffset). Distance D = Amount of padding required
		 * to hold the entire rotated window in memory.
		 */

		/* Compute pad as difference between the MAP windowsize */
		/* and the diagonal distance of the window. */
		/* (DFT grids are computed with pixel offsets RELATIVE2ORIGIN.) */
		diag = Math.sqrt((2.0 * mapWindowSize * mapWindowSize));
		pad = (diag - mapWindowSize) / 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
		/* Must add the window offset to the rotational padding. */
		dftPad = getDefs().sRound(pad) + mapWindowOffset;

		/* 2. Compute pad required for rotated blocks used in directional */
		/* binarization. Binarization blocks are applied to each pixel */
		/* in the input image. */
		diag = Math.sqrt(((dirBinGridWidth * dirBinGridWidth) + (dirBinGridHeight * dirBinGridHeight)));
		/* Assumption: all grid centers reside in valid/allocated memory. */
		/* (Dirbin grids are computed with pixel offsets RELATIVE2CENTER.) */
		pad = (diag - 1) / 2.0;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
		dirBinPad = getDefs().sRound(pad);

		maxPad = Math.max(dftPad, dirBinPad);

		/* Return the maximum of the two required paddings. This padding will */
		/* be sufficiently large for all purposes, so that padding of the */
		/* input image will only be required once. */
		return (maxPad);
	}

	/**
	 * Initializes a lookup table containing cosine and sine values needed to
	 * convert integer IMAP directions to angles in radians.
	 * <p>
	 * NIST: {@code init_dir2rad()}. Unlike the C original, the {@link DirToRad}
	 * object (with its number of directions and cos/sin arrays) is allocated by
	 * the caller; this method only fills it. Direction {@code i} maps to angle
	 * {@code i * 2 * PI / nDirs}; values are truncated to
	 * {@link ILfs#TRUNC_SCALE} for cross-platform consistency.
	 *
	 * @param dirToRad input/output: pre-allocated DIR2RAD structure whose cos and
	 *                 sin arrays are filled in
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int initDirToRad(DirToRad dirToRad) {
		double theta;
		double piFactor;
		double cos;
		double sin;

		/*
		 * create at the call of above function and initialize here Allocate structure
		 */
		/* Pi_factor sets the period of the trig functions to NDIRS units in x. */
		/* For example, if NDIRS==16, then piFactor = 2(PI/16) = .3926... */
		piFactor = 2.0 * ILfs.M_PI / (double) dirToRad.getNDirs();

		/* Now compute cos and sin values for each direction. */
		for (int i = 0; i < dirToRad.getNDirs(); ++i) {
			theta = (i * piFactor);
			cos = Math.cos(theta);
			sin = Math.sin(theta);
			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures. */
			cos = getDefs().truncDoublePrecision(cos, ILfs.TRUNC_SCALE);
			sin = getDefs().truncDoublePrecision(sin, ILfs.TRUNC_SCALE);
			dirToRad.getCos()[i] = cos;
			dirToRad.getSin()[i] = sin;
		}

		return (ILfs.FALSE);
	}

	/**
	 * Initializes a set of wave forms needed to conduct DFT analysis on blocks
	 * of the input image.
	 * <p>
	 * NIST: {@code init_dftwaves()}. The {@link DftWaves} container is allocated
	 * by the caller; this method allocates one {@link DftWave} per wave and fills
	 * its cos/sin sample arrays. The base frequency sets the period of the trig
	 * functions to {@code waveLen} units (e.g. {@code 2 * PI / 24} for a block
	 * size of 24), multiplied by the corresponding coefficient.
	 *
	 * @param dftWaves input/output: pre-allocated DFTWAVES structure (number of
	 *                 waves and wave length set) whose waves are created and
	 *                 filled in
	 * @param dftCoefs array of multipliers used to define the frequency for each
	 *                 wave form to be computed
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int initDftWaves(DftWaves dftWaves, AtomicReferenceArray<Double> dftCoefs) {
		double piFactor;
		double freq;
		double x;

		/* Pi_factor sets the period of the trig functions to BLOCKSIZE units */
		/* in x. For example, if BLOCKSIZE==24, then */
		/* piFactor = 2(PI/24) = .26179... */
		piFactor = 2.0 * Math.PI / dftWaves.getWaveLen();

		/* Foreach of 4 DFT frequency coef ... */
		for (int i = 0; i < dftWaves.getNWaves(); ++i) {
			/* Allocate wave structure */
			dftWaves.getWaves()[i] = new DftWave(dftWaves.getWaveLen());

			/* Compute actual frequency */
			freq = piFactor * dftCoefs.get(i);

			/* Used as a 1D DFT on a 24 long vector of pixel sums */
			for (int j = 0; j < dftWaves.getWaveLen(); ++j) {
				/* Compute sample points from frequency */
				x = freq * j;
				/* Store cos and sin components of sample point */
				dftWaves.getWaves()[i].getCos()[j] = Math.cos(x);
				dftWaves.getWaves()[i].getSin()[j] = Math.sin(x);
			}
		}
		return (ILfs.FALSE);
	}

	/**
	 * Initializes a set of offsets that address individual rotated pixels within
	 * a grid.
	 * <p>
	 * NIST: {@code init_rotgrids()}. These rotated grids are used to conduct DFT
	 * analyses on blocks of input image data, and in directional/isotropic
	 * binarization. For each of the {@code noOfGrids} directions (starting at the
	 * grid's start angle and incrementing by {@code PI / noOfGrids}), each grid
	 * pixel is rotated about the grid centre and stored as a pixel offset into
	 * the padded image (offsets are relative to the grid centre or origin
	 * according to {@link RotGrids#getRelative2()}). The {@link RotGrids} object
	 * and its grid arrays are allocated by the caller; this method sets its pad
	 * and fills the offsets.
	 *
	 * @param rotGrids    input/output: pre-allocated ROTGRIDS structure
	 * @param imageWidth  width (in pixels) of the input image
	 * @param imageHeight height (in pixels) of the input image
	 * @param nPad        number of pixels to be padded to the perimeter of the
	 *                    input image; may be {@link ILfs#UNDEFINED}, in which
	 *                    case the specific padding required by the rotated grids
	 *                    is computed and stored in {@code rotGrids}
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error: -31 illegal relative flag, -32 pad passed is too
	 *         small, {@link ILfs#ERROR_CODE_33} grid list not allocated
	 */
	public int initRotGrids(RotGrids rotGrids, final int imageWidth, final int imageHeight, final int nPad) {
		double piOffset;
		double piIncrement;
		int dir;
		int ix;
		int iy; // gridSize,
		int pw;
		int gridPad;
		int minDim;
		double diag;
		double theta;
		double cos;
		double sin;
		double cx;
		double cy;
		double fxm;
		double fym;
		double fx;
		double fy;
		int ixt;
		int iyt;
		double pad;

		/* Compute pad based on diagonal of the grid */
		diag = Math.sqrt((double) ((rotGrids.getGridWidth() * rotGrids.getGridWidth())
				+ (rotGrids.getGridHeight() * rotGrids.getGridHeight())));
		switch (rotGrids.getRelative2()) {
		case ILfs.RELATIVE_TO_CENTER:
			/* Assumption: all grid centers reside in valid/allocated memory. */
			pad = (diag - 1) / (double) 2.0;
			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures when rounding doubles. */
			pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
			gridPad = getDefs().sRound(pad);
			break;
		case ILfs.RELATIVE_TO_ORIGIN:
			/* Assumption: all grid origins reside in valid/allocated memory. */
			minDim = Math.min(rotGrids.getGridWidth(), rotGrids.getGridHeight());
			/* Compute pad as difference between the smallest grid dimension */
			/* and the diagonal distance of the grid. */
			pad = (diag - minDim) / (double) 2.0;
			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures when rounding doubles. */
			pad = getDefs().truncDoublePrecision(pad, ILfs.TRUNC_SCALE);
			gridPad = getDefs().sRound(pad);
			break;
		default:
			logger.error("ERROR : init_rotgrids : Illegal relative flag : %d" + rotGrids.getRelative2());
			rotGrids = null;
			return (-31);
		}

		/* If input padding is UNDEFINED ... */
		if (nPad == ILfs.UNDEFINED) {
			/* Use the padding specifically required by the rotated grids herein. */
			rotGrids.setPad(gridPad);
		} else {
			/* Otherwise, input pad was specified, so check to make sure it is */
			/* sufficiently large to handle the rotated grids herein. */
			if (nPad < gridPad) {
				/* If input pad is NOT large enough, then ERROR. */
				logger.error("ERROR : init_rotgrids : Pad passed is too small");
				rotGrids = null;
				return (-32);
			}
			/* Otherwise, use the specified input pad in computing grid offsets. */
			rotGrids.setPad(nPad);
		}

		/* Total number of points in grid */
		// Assigned before entering function

		/* Compute width of "padded" image */
		pw = imageWidth + (rotGrids.getPad() << 1);

		/* Center coord of grid (0-oriented). */
		cx = (rotGrids.getGridWidth() - 1) / 2.0;
		cy = (rotGrids.getGridHeight() - 1) / 2.0;

		/* Allocate list of rotgrid pointers */
		// Done in constructor
		if (rotGrids.getGrids() == null) {
			/* Free memory allocated to this point. */
			rotGrids = null;
			logger.error("ERROR : init_rotgrids : rotgrids.grids() : Null");
			return (ILfs.ERROR_CODE_33);
		}

		/* Pi_offset is the offset in radians from which angles are to begin. */
		piOffset = rotGrids.getStartAngle();
		piIncrement = ILfs.M_PI / rotGrids.getNoOfGrids(); // if nDirs == 16, incr = 11.25 degrees

		/* For each direction to rotate a grid ... */
		for (dir = 0, theta = piOffset; dir < rotGrids.getNoOfGrids(); dir++, theta += piIncrement) {
			/* Allocate a rotgrid */
			// done in Constructor

			/* Set pointer to current grid */
			int gridptrIndex = 0;
			/* Compute cos and sin of current angle */
			cos = Math.cos(theta);
			sin = Math.sin(theta);

			/* This next section of nested FOR loops precomputes a */
			/* rotated grid. The rotation is set up to rotate a GRID_W X */
			/* GRID_H grid on its center point at C=(Cx,Cy). The current */
			/* pixel being rotated is P=(Ix,Iy). Therefore, we have a */
			/* rotation transformation of point P about pivot point C. */
			/* The rotation transformation about a pivot point in matrix */
			/* form is: */
			/*
			 * +- -+ | cos(T) sin(T) 0 | [Ix Iy 1] | -sin(T) cos(T) 0 | | (1-cos(T))*Cx +
			 * Cy*sin(T) (1-cos(T))*Cy - Cx*sin(T) 1 | +- -+
			 */
			/* Multiplying the 2 matrices and combining terms yeilds the */
			/* equations for rotated coordinates (Rx, Ry): */
			/* Rx = Cx + (Ix - Cx)*cos(T) - (Iy - Cy)*sin(T) */
			/* Ry = Cy + (Ix - Cx)*sin(T) + (Iy - Cy)*cos(T) */
			/*                                                           */
			/* Care has been taken to ensure that (for example) when */
			/* BLOCKSIZE==24 the rotated indices stay within a centered */
			/* 34X34 area. */
			/* This is important for computing an accurate padding of */
			/* the input image. The rotation occurs "in-place" so that */
			/* outer pixels in the grid are mapped at times from */
			/* adjoining blocks. As a result, to keep from accessing */
			/* "unknown" memory or pixels wrapped from the other side of */
			/* the image, the input image should first be padded by */
			/* PAD=round((DIAG - BLOCKSIZE)/2.0) where DIAG is the */
			/* diagonal distance of the grid. */
			/* For example, when BLOCKSIZE==24, Dx=34, so PAD=5. */

			/* Foreach each y coord in block ... */
			for (iy = 0; iy < rotGrids.getGridHeight(); ++iy) {
				/* Compute rotation factors dependent on Iy (include constant) */
				fxm = -1.0 * ((iy - cy) * sin);
				fym = ((iy - cy) * cos);

				/* If offsets are to be relative to the grids origin, then */
				/* we need to subtract CX and CY. */
				if (rotGrids.getRelative2() == ILfs.RELATIVE_TO_ORIGIN) {
					fxm += cx;
					fym += cy;
				}

				/* foreach each x coord in block ... */
				for (ix = 0; ix < rotGrids.getGridWidth(); ++ix) {
					/* Now combine factors dependent on Iy with those of Ix */
					fx = fxm + ((ix - cx) * cos);
					fy = fym + ((ix - cx) * sin);
					/* Need to truncate precision so that answers are consistent */
					/* on different computer architectures when rounding doubles. */
					fx = getDefs().truncDoublePrecision(fx, ILfs.TRUNC_SCALE);
					fy = getDefs().truncDoublePrecision(fy, ILfs.TRUNC_SCALE);
					ixt = getDefs().sRound(fx);
					iyt = getDefs().sRound(fy);

					/* Store the current pixels relative */
					/* rotated offset. Make sure to */
					/* multiply the y-component of the */
					/* offset by the "padded" image width! */

					rotGrids.getGrids()[dir][gridptrIndex++] = ixt + (iyt * pw);
				} // ix
			} // iy
		} // dir

		return (ILfs.FALSE);
	}

	/**
	 * Allocates the memory associated with DFT power vectors.
	 * <p>
	 * NIST: {@code alloc_dir_powers()}. The DFT analysis is conducted block by
	 * block in the input image, and within each block, N wave forms are applied
	 * at M different directions. All powers are initialized to 0.0.
	 *
	 * @param ret    output: zero ({@link ILfs#FALSE}) on successful completion
	 *               (negative would indicate a system error)
	 * @param nWaves number of DFT wave forms
	 * @param nDirs  number of orientations (directions) used in DFT analysis
	 * @return the allocated power vectors, indexed {@code [wave][direction]}
	 */
	public AtomicReferenceArray<Double[]> allocDirPowers(AtomicInteger ret, final int nWaves, final int nDirs) {
		ret.set(ILfs.UNDEFINED);
		/* Allocate list of double pointers to hold power vectors */
		Double[][] oPowers = new Double[nWaves][nDirs];

		/* Foreach DFT wave ... */
		for (int w = 0; w < nWaves; w++) {
			/* Allocate power vector for all directions */
			for (int d = 0; d < nDirs; d++) {
				oPowers[w][d] = 0.0d;
			}
		}
		ret.set(ILfs.FALSE);
		return new AtomicReferenceArray<>(oPowers);
	}

	/**
	 * Allocates the wave-form index array of the statistics derived from DFT
	 * power vectors computed in a block of the input image.
	 * <p>
	 * NIST: part of {@code alloc_power_stats()}. Statistics are not computed for
	 * the lowest DFT wave form, so the length of the statistics arrays is one
	 * less than the number of DFT wave forms used. The statistics include the
	 * maximum power for each wave form, the direction at which the maximum power
	 * occurred, and a normalized value for the maximum power. In addition, the
	 * statistics are ranked in descending order based on normalized squared
	 * maximum power. All entries are initialized to 0.
	 *
	 * @param ret    output: zero ({@link ILfs#FALSE}) on successful completion
	 *               (negative would indicate a system error)
	 * @param nStats the number of wave forms from which statistics are to be
	 *               derived (N waves - 1)
	 * @return an array to hold the ranked wave form indices of the corresponding
	 *         statistics
	 */
	public AtomicIntegerArray allocPowerStatsWis(AtomicInteger ret, final int nStats) {
		/* Allocate DFT wave index vector */
		int[] wis = new int[nStats];
		/* Allocate power vector for all directions */
		for (int i = 0; i < nStats; i++) {
			wis[i] = 0;
		}
		ret.set(ILfs.FALSE);
		return new AtomicIntegerArray(wis);
	}

	/**
	 * Allocates the maximum-power array of the statistics derived from DFT power
	 * vectors computed in a block of the input image.
	 * <p>
	 * NIST: part of {@code alloc_power_stats()}. Statistics are not computed for
	 * the lowest DFT wave form, so the length of the statistics arrays is one
	 * less than the number of DFT wave forms used (see
	 * {@link #allocPowerStatsWis(AtomicInteger, int)}). All entries are
	 * initialized to 0.0.
	 *
	 * @param ret    output: zero ({@link ILfs#FALSE}) on successful completion
	 *               (negative would indicate a system error)
	 * @param nStats the number of wave forms from which statistics are to be
	 *               derived (N waves - 1)
	 * @return an array to hold the maximum DFT power for each wave form
	 */
	public AtomicReferenceArray<Double> allocPowerStatsPowmaxs(AtomicInteger ret, final int nStats) {
		/* Allocate max power vector */
		Double[] powmaxs = new Double[nStats];
		for (int i = 0; i < nStats; i++) {
			powmaxs[i] = 0.0;
		}
		ret.set(ILfs.FALSE);
		return new AtomicReferenceArray<>(powmaxs);
	}

	/**
	 * Allocates the maximum-power direction array of the statistics derived from
	 * DFT power vectors computed in a block of the input image.
	 * <p>
	 * NIST: part of {@code alloc_power_stats()}. Statistics are not computed for
	 * the lowest DFT wave form, so the length of the statistics arrays is one
	 * less than the number of DFT wave forms used (see
	 * {@link #allocPowerStatsWis(AtomicInteger, int)}). All entries are
	 * initialized to 0.
	 *
	 * @param ret    output: zero ({@link ILfs#FALSE}) on successful completion
	 *               (negative would indicate a system error)
	 * @param nStats the number of wave forms from which statistics are to be
	 *               derived (N waves - 1)
	 * @return an array to hold the direction corresponding to each maximum power
	 *         value
	 */
	public AtomicIntegerArray allocPowerStatsPowmaxDirs(AtomicInteger ret, final int nStats) {
		/* Allocate max power direction vector */
		int[] powmaxDirs = new int[nStats];
		for (int i = 0; i < nStats; i++) {
			powmaxDirs[i] = 0;
		}
		ret.set(ILfs.FALSE);
		return new AtomicIntegerArray(powmaxDirs);
	}

	/**
	 * Allocates the normalized maximum-power array of the statistics derived
	 * from DFT power vectors computed in a block of the input image.
	 * <p>
	 * NIST: part of {@code alloc_power_stats()}. Statistics are not computed for
	 * the lowest DFT wave form, so the length of the statistics arrays is one
	 * less than the number of DFT wave forms used (see
	 * {@link #allocPowerStatsWis(AtomicInteger, int)}). All entries are
	 * initialized to 0.0.
	 *
	 * @param ret    output: zero ({@link ILfs#FALSE}) on successful completion
	 *               (negative would indicate a system error)
	 * @param nStats the number of wave forms from which statistics are to be
	 *               derived (N waves - 1)
	 * @return an array to hold the normalized maximum power for each wave form
	 */
	public AtomicReferenceArray<Double> allocPowerStatsPownorms(AtomicInteger ret, final int nStats) {
		/* Allocate normalized power vector */
		Double[] pownorms = new Double[nStats];
		for (int i = 0; i < nStats; i++) {
			pownorms[i] = 0.0d;
		}
		ret.set(ILfs.FALSE);
		return new AtomicReferenceArray<>(pownorms);
	}
}