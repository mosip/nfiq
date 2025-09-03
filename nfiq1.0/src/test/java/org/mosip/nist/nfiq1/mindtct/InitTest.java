package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for Init functionality
 */
@ExtendWith(MockitoExtension.class)
class InitTest {

    private Init init;

    /**
     * Sets up the Init instance before each execution
     */
    @BeforeEach
    void setUp() {
        init = Init.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Init instance1 = Init.getInstance();
        Init instance2 = Init.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that constructor creates Init instance successfully
     */
    @Test
    void constructorCreatesInstance() {
        Assertions.assertNotNull(init);
    }

    /**
     * Validates that getDefs returns a non-null object
     */
    @Test
    void getDefs() {
        Assertions.assertNotNull(init.getDefs());
    }

    /**
     * Verifies getMaxPadding computes non-negative value
     */
    @Test
    void getMaxPadding() {
        int iMapBlockSize = 24;
        int dirBinGridWidth = 7;
        int dirBinGridHeight = 7;
        int isobin_grid_dim = 11;
        int result = init.getMaxPadding(iMapBlockSize, dirBinGridWidth, dirBinGridHeight, isobin_grid_dim);
        Assertions.assertTrue(result >= 0);
    }

    /**
     * Verifies getMaxPaddingV2 computes non-negative value
     */
    @Test
    void getMaxPaddingV2() {
        int mapWindowSize = 24;
        int mapWindowOffset = 12;
        int dirBinGridWidth = 7;
        int dirBinGridHeight = 7;
        int result = init.getMaxPaddingV2(mapWindowSize, mapWindowOffset, dirBinGridWidth, dirBinGridHeight);
        Assertions.assertTrue(result >= 0);
    }

    /**
     * Validates direction-to-radian initialization
     */
    @Test
    void initDirToRad() {
        org.mosip.nist.nfiq1.common.ILfs.DirToRad dirToRad = new org.mosip.nist.nfiq1.common.ILfs.DirToRad(16);
        int result = init.initDirToRad(dirToRad);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertNotNull(dirToRad.getCos());
        Assertions.assertNotNull(dirToRad.getSin());
        Assertions.assertEquals(16, dirToRad.getNDirs());
        Assertions.assertTrue(dirToRad.getCos()[0] != 0.0 || dirToRad.getSin()[0] == 0.0);
    }

    /**
     * Validates DFT waves initialization
     */
    @Test
    void initDftWaves() {
        org.mosip.nist.nfiq1.common.ILfs.DftWaves dftWaves = new org.mosip.nist.nfiq1.common.ILfs.DftWaves(4, 24);
        java.util.concurrent.atomic.AtomicReferenceArray<Double> dftCoefs =
                new java.util.concurrent.atomic.AtomicReferenceArray<>(new Double[]{1.0, 2.0, 3.0, 4.0});
        int result = init.initDftWaves(dftWaves, dftCoefs);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertNotNull(dftWaves.getWaves());
        Assertions.assertEquals(4, dftWaves.getNWaves());
        Assertions.assertEquals(24, dftWaves.getWaveLen());
        for (int i = 0; i < dftWaves.getNWaves(); i++) {
            Assertions.assertNotNull(dftWaves.getWaves()[i]);
            Assertions.assertNotNull(dftWaves.getWaves()[i].getCos());
            Assertions.assertNotNull(dftWaves.getWaves()[i].getSin());
        }
    }

    /**
     * Validates initialization of RotGrids with RELATIVE_TO_CENTER
     */
    @Test
    void initRotGridsRelativeToCenter() {
        org.mosip.nist.nfiq1.common.ILfs.RotGrids rotGrids =
                new org.mosip.nist.nfiq1.common.ILfs.RotGrids(16, 7, 7, 0, org.mosip.nist.nfiq1.common.ILfs.RELATIVE_TO_CENTER);
        int imageWidth = 100;
        int imageHeight = 100;
        int nPad = org.mosip.nist.nfiq1.common.ILfs.UNDEFINED;
        int result = init.initRotGrids(rotGrids, imageWidth, imageHeight, nPad);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertNotNull(rotGrids.getGrids());
        Assertions.assertTrue(rotGrids.getPad() >= 0);
    }

    /**
     * Validates initialization of RotGrids with RELATIVE_TO_ORIGIN
     */
    @Test
    void initRotGridsRelativeToOrigin() {
        org.mosip.nist.nfiq1.common.ILfs.RotGrids rotGrids =
                new org.mosip.nist.nfiq1.common.ILfs.RotGrids(16, 7, 7, 0, org.mosip.nist.nfiq1.common.ILfs.RELATIVE_TO_ORIGIN);
        int imageWidth = 100;
        int imageHeight = 100;
        int nPad = 10;
        int result = init.initRotGrids(rotGrids, imageWidth, imageHeight, nPad);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertEquals(10, rotGrids.getPad());
    }

    /**
     * Validates RotGrids initialization fails with invalid flag
     */
    @Test
    void initRotGridsInvalidRelativeFlag() {
        org.mosip.nist.nfiq1.common.ILfs.RotGrids rotGrids =
                new org.mosip.nist.nfiq1.common.ILfs.RotGrids(16, 7, 7, 0, 999); // Invalid flag
        int imageWidth = 100;
        int imageHeight = 100;
        int nPad = 10;
        int result = init.initRotGrids(rotGrids, imageWidth, imageHeight, nPad);
        Assertions.assertEquals(-31, result);
    }

    /**
     * Validates RotGrids initialization fails with insufficient padding
     */
    @Test
    void initRotGridsInsufficientPadding() {
        org.mosip.nist.nfiq1.common.ILfs.RotGrids rotGrids =
                new org.mosip.nist.nfiq1.common.ILfs.RotGrids(16, 7, 7, 0, org.mosip.nist.nfiq1.common.ILfs.RELATIVE_TO_CENTER);
        int imageWidth = 100;
        int imageHeight = 100;
        int nPad = 1;
        int result = init.initRotGrids(rotGrids, imageWidth, imageHeight, nPad);
        Assertions.assertEquals(-32, result);
    }

    /**
     * Validates RotGrids initialization fails with null grids
     */
    @Test
    void initRotGridsNullGrids() {
        org.mosip.nist.nfiq1.common.ILfs.RotGrids rotGrids =
                new org.mosip.nist.nfiq1.common.ILfs.RotGrids(16, 7, 7, 0, org.mosip.nist.nfiq1.common.ILfs.RELATIVE_TO_CENTER) {
                    @Override
                    public int[][] getGrids() {
                        return null;
                    }
                };
        int imageWidth = 100;
        int imageHeight = 100;
        int nPad = 10;
        int result = init.initRotGrids(rotGrids, imageWidth, imageHeight, nPad);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.ERROR_CODE_33, result);
    }

    /**
     * Verifies that allocDirPowers allocates correct data structure
     */
    @Test
    void allocDirPowers() {
        java.util.concurrent.atomic.AtomicInteger ret = new java.util.concurrent.atomic.AtomicInteger();
        int nWaves = 4;
        int nDirs = 16;
        java.util.concurrent.atomic.AtomicReferenceArray<Double[]> result =
                init.allocDirPowers(ret, nWaves, nDirs);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(nWaves, result.length());
        for (int i = 0; i < nWaves; i++) {
            Assertions.assertNotNull(result.get(i));
            Assertions.assertEquals(nDirs, result.get(i).length);
        }
    }

    /**
     * Validates allocation of power statistics wis
     */
    @Test
    void allocPowerStatsWis() {
        java.util.concurrent.atomic.AtomicInteger ret = new java.util.concurrent.atomic.AtomicInteger();
        int nStats = 3;
        java.util.concurrent.atomic.AtomicIntegerArray result = init.allocPowerStatsWis(ret, nStats);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(nStats, result.length());
        for (int i = 0; i < nStats; i++) {
            Assertions.assertEquals(0, result.get(i));
        }
    }

    /**
     * Validates allocation of power statistics powmaxs
     */
    @Test
    void allocPowerStatsPowmaxs() {
        java.util.concurrent.atomic.AtomicInteger ret = new java.util.concurrent.atomic.AtomicInteger();
        int nStats = 3;
        java.util.concurrent.atomic.AtomicReferenceArray<Double> result =
                init.allocPowerStatsPowmaxs(ret, nStats);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(nStats, result.length());
        for (int i = 0; i < nStats; i++) {
            Assertions.assertEquals(0.0, result.get(i), 0.001);
        }
    }

    /**
     * Validates allocation of power statistics powmaxDirs
     */
    @Test
    void allocPowerStatsPowmaxDirs() {
        java.util.concurrent.atomic.AtomicInteger ret = new java.util.concurrent.atomic.AtomicInteger();
        int nStats = 3;
        java.util.concurrent.atomic.AtomicIntegerArray result = init.allocPowerStatsPowmaxDirs(ret, nStats);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(nStats, result.length());
        for (int i = 0; i < nStats; i++) {
            Assertions.assertEquals(0, result.get(i));
        }
    }

    /**
     * Validates allocation of power statistics pownorms
     */
    @Test
    void allocPowerStatsPownorms() {
        java.util.concurrent.atomic.AtomicInteger ret = new java.util.concurrent.atomic.AtomicInteger();
        int nStats = 3;
        java.util.concurrent.atomic.AtomicReferenceArray<Double> result =
                init.allocPowerStatsPownorms(ret, nStats);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(nStats, result.length());
        for (int i = 0; i < nStats; i++) {
            Assertions.assertEquals(0.0, result.get(i), 0.001);
        }
    }

    /**
     * Verifies getMaxPadding handles various input values
     */
    @Test
    void getMaxPaddingDifferentValues() {
        int result1 = init.getMaxPadding(10, 5, 5, 7);
        int result2 = init.getMaxPadding(20, 15, 15, 5);
        int result3 = init.getMaxPadding(15, 10, 8, 20);
        Assertions.assertTrue(result1 >= 0);
        Assertions.assertTrue(result2 >= 0);
        Assertions.assertTrue(result3 >= 0);
    }

    /**
     * Verifies getMaxPaddingV2 handles various input values
     */
    @Test
    void getMaxPaddingV2DifferentValues() {
        int result1 = init.getMaxPaddingV2(20, 5, 7, 7);
        int result2 = init.getMaxPaddingV2(30, 10, 15, 10);
        Assertions.assertTrue(result1 >= 0);
        Assertions.assertTrue(result2 >= 0);
    }

    /**
     * Validates initDirToRad with different nDirs values
     */
    @Test
    void initDirToRadDifferentNDirs() {
        org.mosip.nist.nfiq1.common.ILfs.DirToRad dirToRad = new org.mosip.nist.nfiq1.common.ILfs.DirToRad(8);
        int result = init.initDirToRad(dirToRad);
        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertEquals(8, dirToRad.getNDirs());
        for (int i = 0; i < 8; i++) {
            Assertions.assertNotNull(dirToRad.getCos()[i]);
            Assertions.assertNotNull(dirToRad.getSin()[i]);
        }
    }
}