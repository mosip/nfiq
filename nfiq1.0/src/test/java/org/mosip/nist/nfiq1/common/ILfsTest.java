package org.mosip.nist.nfiq1.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Basic tests for verifying ILfs nested data structures compile & behave.
 */
public class ILfsTest {

	@Test
	void testMinutiaCreation() {
		ILfs.Minutia minutia = new ILfs.Minutia();

		minutia.setX(10);
		minutia.setY(20);
		minutia.setDirection(5);
		minutia.setReliability(0.87);

		Assertions.assertEquals(10, minutia.getX());
		Assertions.assertEquals(20, minutia.getY());
		Assertions.assertEquals(5, minutia.getDirection());
		Assertions.assertEquals(0.87, minutia.getReliability());
	}

	@Test
	void testMinutiaeContainer() {
		ILfs.Minutiae minutiae = new ILfs.Minutiae(10, 0);

		Assertions.assertEquals(10, minutiae.getAlloc());
		Assertions.assertEquals(0, minutiae.getNum());
		Assertions.assertNotNull(minutiae.getList());
	}

	@Test
	void testDirToRadInitialization() {
		ILfs.DirToRad dirToRad = new ILfs.DirToRad(5);

		Assertions.assertEquals(5, dirToRad.getNDirs());
		Assertions.assertEquals(5, dirToRad.getCos().length);
		Assertions.assertEquals(5, dirToRad.getSin().length);
	}

	@Test
	void testDftWave() {
		ILfs.DftWave wave = new ILfs.DftWave(8);

		Assertions.assertEquals(8, wave.getCos().length);
		Assertions.assertEquals(8, wave.getSin().length);
	}

	@Test
	void testRotGrids() {
		ILfs.RotGrids grids = new ILfs.RotGrids(
				0.0,
				3,
				4,
				4,
				ILfs.RELATIVE_TO_CENTER
		);

		Assertions.assertEquals(3, grids.getNoOfGrids());
		Assertions.assertEquals(4, grids.getGridWidth());
		Assertions.assertEquals(4, grids.getGridHeight());
		Assertions.assertNotNull(grids.getGrids());
	}

	@Test
	void testLfsParamsMinimal() {
		ILfs.LfsParams params = new ILfs.LfsParams(
				ILfs.PAD_VALUE,
				ILfs.JOIN_LINE_RADIUS,
				ILfs.IMAP_BLOCKSIZE,
				ILfs.MAP_WINDOWSIZE_V2,
				ILfs.MAP_WINDOWOFFSET_V2,
				ILfs.NUM_DIRECTIONS,
				ILfs.START_DIR_ANGLE,
				ILfs.RMV_VALID_NBR_MIN,
				ILfs.DIR_STRENGTH_MIN,
				ILfs.DIR_DISTANCE_MAX,
				ILfs.SMTH_VALID_NBR_MIN,
				ILfs.VORT_VALID_NBR_MIN,
				ILfs.HIGHCURV_VORTICITY_MIN,
				ILfs.HIGHCURV_CURVATURE_MIN,
				ILfs.MIN_INTERPOLATE_NBRS,
				ILfs.PERCENTILE_MIN_MAX,
				ILfs.MIN_CONTRAST_DELTA,
				ILfs.NUM_DFT_WAVES,
				ILfs.POWMAX_MIN,
				ILfs.POWNORM_MIN,
				ILfs.POWMAX_MAX,
				ILfs.FORK_INTERVAL,
				ILfs.FORK_PCT_POWMAX,
				ILfs.FORK_PCT_POWNORM,
				ILfs.DIRBIN_GRID_W,
				ILfs.DIRBIN_GRID_H,
				ILfs.ISOBIN_GRID_DIM,
				ILfs.NUM_FILL_HOLES,
				ILfs.MAX_MINUTIA_DELTA,
				ILfs.MAX_HIGH_CURVE_THETA,
				ILfs.HIGH_CURVE_HALF_CONTOUR,
				ILfs.MIN_LOOP_LEN,
				ILfs.MIN_LOOP_ASPECT_DIST,
				ILfs.MIN_LOOP_ASPECT_RATIO,
				ILfs.LINK_TABLE_DIM,
				ILfs.MAX_LINK_DIST,
				ILfs.MIN_THETA_DIST,
				ILfs.MAXTRANS,
				ILfs.SCORE_THETA_NORM,
				ILfs.SCORE_DIST_NORM,
				ILfs.SCORE_DIST_WEIGHT,
				ILfs.SCORE_NUMERATOR,
				ILfs.MAX_RMTEST_DIST,
				ILfs.MAX_HOOK_LEN,
				ILfs.MAX_HALF_LOOP,
				ILfs.TRANS_DIR_PIX,
				ILfs.SMALL_LOOP_LEN,
				ILfs.SIDE_HALF_CONTOUR,
				ILfs.INV_BLOCK_MARGIN,
				ILfs.RM_VALID_NBR_MIN,
				ILfs.MAX_OVERLAP_DIST,
				ILfs.MAX_OVERLAP_JOIN_DIST,
				ILfs.MALFORMATION_STEPS_1,
				ILfs.MALFORMATION_STEPS_2,
				ILfs.MIN_MALFORMATION_RATIO,
				ILfs.MAX_MALFORMATION_DIST,
				ILfs.PORES_TRANS_R,
				ILfs.PORES_PERP_STEPS,
				ILfs.PORES_STEPS_FWD,
				ILfs.PORES_STEPS_BWD,
				ILfs.PORES_MIN_DIST2,
				ILfs.PORES_MAX_RATIO,
				ILfs.MAX_NBRS,
				ILfs.MAX_RIDGE_STEPS
		);

		Assertions.assertEquals(ILfs.PAD_VALUE, params.getPadValue());
		Assertions.assertEquals(ILfs.NUM_DIRECTIONS, params.getNumDirections());
	}
}