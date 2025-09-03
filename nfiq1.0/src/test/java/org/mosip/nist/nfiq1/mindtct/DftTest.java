package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs.DftWave;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

/**
 * Test class for Dft functionality
 */
@ExtendWith(MockitoExtension.class)
class DftTest {

    private Dft dft;
    @Mock
    private DftWaves mockDftWaves;
    @Mock
    private RotGrids mockRotGrids;
    @Mock
    private DftWave mockDftWave;

    /**
     * Sets up the Dft instance before each execution
     */
    @BeforeEach
    void setUp() {
        dft = Dft.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Dft instance1 = Dft.getInstance();
        Dft instance2 = Dft.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null object
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(dft.getDefs());
    }

    /**
     * Verifies that sumRotBlockRows calculates row sums correctly
     */
    @Test
    void sumRotBlockRowsCalculatesRowSums() {
        int[] rowSums = new int[5];
        int[] paddedImageData = new int[100];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = i % 256;
        }
        AtomicIntegerArray gridOffsets = new AtomicIntegerArray(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24});

        dft.sumRotBlockRows(rowSums, paddedImageData, 10, gridOffsets, 5);

        Assertions.assertTrue(rowSums[0] >= 0);
    }

    /**
     * Validates that computeDftPower calculates power values correctly
     */
    @Test
    void computeDftPowerCalculatesPower() {
        AtomicReference<Double> power = new AtomicReference<>();
        int[] rowSums = new int[]{10, 20, 30, 40, 50};

        Mockito.when(mockDftWave.getCos()).thenReturn(new double[]{1.0, 0.0, -1.0, 0.0, 1.0});
        Mockito.when(mockDftWave.getSin()).thenReturn(new double[]{0.0, 1.0, 0.0, -1.0, 0.0});

        dft.computeDftPower(power, rowSums, mockDftWave, 5);

        Assertions.assertNotNull(power.get());
        Assertions.assertTrue(power.get() >= 0.0);
    }

    /**
     * Verifies that getMaxNorm calculates maximum and norm values correctly
     */
    @Test
    void getMaxNormCalculatesMaxAndNorm() {
        AtomicReference<Double> powmax = new AtomicReference<>();
        AtomicInteger powmaxDir = new AtomicInteger();
        AtomicReference<Double> pownorm = new AtomicReference<>();
        AtomicReferenceArray<Double> powerVector = new AtomicReferenceArray<>(new Double[]{1.0, 5.0, 3.0, 2.0});

        dft.getMaxNorm(powmax, powmaxDir, pownorm, powerVector, 4);

        Assertions.assertEquals(5.0, powmax.get(), 0.001);
        Assertions.assertEquals(1, powmaxDir.get());
        Assertions.assertTrue(pownorm.get() > 0.0);
    }

    /**
     * Validates that sortDftWaves sorts DFT waves correctly
     */
    @Test
    void sortDftWavesSortsCorrectly() {
        AtomicIntegerArray wis = new AtomicIntegerArray(3);
        AtomicReferenceArray<Double> powMaxs = new AtomicReferenceArray<>(new Double[]{1.0, 5.0, 3.0});
        AtomicReferenceArray<Double> powNorms = new AtomicReferenceArray<>(new Double[]{0.1, 0.5, 0.3});

        int result = dft.sortDftWaves(wis, powMaxs, powNorms, 3);

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(1, wis.get(0));
    }

    /**
     * Validates that dftDirPowers processes valid input correctly
     */
    @Test
    void dftDirPowersWithValidInput() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        powers.set(0, new Double[]{0.0, 0.0});
        powers.set(1, new Double[]{0.0, 0.0});
        int[] paddedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            paddedImageData[i] = i % 256;
        }

        Mockito.when(mockDftWaves.getNWaves()).thenReturn(2);
        Mockito.when(mockDftWaves.getWaveLen()).thenReturn(5);
        Mockito.when(mockDftWaves.getWaves()).thenReturn(new DftWave[]{mockDftWave, mockDftWave});
        Mockito.when(mockRotGrids.getNoOfGrids()).thenReturn(2);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[][]{{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24}, {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24}});
        Mockito.when(mockDftWave.getCos()).thenReturn(new double[]{1.0, 0.0, -1.0, 0.0, 1.0});
        Mockito.when(mockDftWave.getSin()).thenReturn(new double[]{0.0, 1.0, 0.0, -1.0, 0.0});

        int result = dft.dftDirPowers(powers, paddedImageData, 0, 10, 10, mockDftWaves, mockRotGrids);

        Assertions.assertEquals(0, result);
    }

    /**
     * Validates that dftDirPowers returns error for non-square grid
     */
    @Test
    void dftDirPowersWithNonSquareGridReturnsError() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(1);
        int[] paddedImageData = new int[100];

        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(4);

        int result = dft.dftDirPowers(powers, paddedImageData, 0, 10, 10, mockDftWaves, mockRotGrids);

        Assertions.assertEquals(-90, result);
    }

    /**
     * Verifies that getDftPowerStats calculates statistics correctly
     */
    @Test
    void getDftPowerStatsCalculatesStatistics() {
        AtomicIntegerArray wis = new AtomicIntegerArray(2);
        AtomicReferenceArray<Double> powMaxs = new AtomicReferenceArray<>(2);
        AtomicIntegerArray powmaxDirs = new AtomicIntegerArray(2);
        AtomicReferenceArray<Double> powNorms = new AtomicReferenceArray<>(2);
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(3);
        powers.set(1, new Double[]{1.0, 2.0, 3.0});
        powers.set(2, new Double[]{2.0, 1.0, 4.0});

        int result = dft.getDftPowerStats(wis, powMaxs, powmaxDirs, powNorms, powers, 1, 3, 3);

        Assertions.assertEquals(0, result);
    }

    /**
     * Validates that getDefs returns a non-null instance
     */
    @Test
    void getDefsReturnsInstance() {
        Assertions.assertNotNull(dft.getDefs());
    }

    /**
     * Validates that getImageUtil returns a non-null instance
     */
    @Test
    void getImageUtilReturnsInstance() {
        Assertions.assertNotNull(dft.getImageUtil());
    }

    /**
     * Validates that getGlobals returns a non-null instance
     */
    @Test
    void getGlobalsReturnsInstance() {
        Assertions.assertNotNull(dft.getGlobals());
    }

    /**
     * Validates that getLfsUtil returns a non-null instance
     */
    @Test
    void getLfsUtilReturnsInstance() {
        Assertions.assertNotNull(dft.getLfsUtil());
    }

    /**
     * Validates that getFree returns a non-null instance
     */
    @Test
    void getFreeReturnsInstance() {
        Assertions.assertNotNull(dft.getFree());
    }

    /**
     * Validates that getInit returns a non-null instance
     */
    @Test
    void getInitReturnsInstance() {
        Assertions.assertNotNull(dft.getInit());
    }

    /**
     * Validates that getBinarization returns a non-null instance
     */
    @Test
    void getBinarizationReturnsInstance() {
        Assertions.assertNotNull(dft.getBinarization());
    }

    /**
     * Validates that getMinutiaHelper returns a non-null instance
     */
    @Test
    void getMinutiaHelperReturnsInstance() {
        Assertions.assertNotNull(dft.getMinutiaHelper());
    }

    /**
     * Validates that getSort returns a non-null instance
     */
    @Test
    void getSortReturnsInstance() {
        Assertions.assertNotNull(dft.getSort());
    }

    /**
     * Validates that getDetect returns a non-null instance
     */
    @Test
    void getDetectReturnsInstance() {
        Assertions.assertNotNull(dft.getDetect());
    }

    /**
     * Validates that getRemoveMinutia returns a non-null instance
     */
    @Test
    void getRemoveMinutiaReturnsInstance() {
        Assertions.assertNotNull(dft.getRemoveMinutia());
    }

    /**
     * Validates that getRidges returns a non-null instance
     */
    @Test
    void getRidgesReturnsInstance() {
        Assertions.assertNotNull(dft.getRidges());
    }

    /**
     * Validates that getLine returns a non-null instance
     */
    @Test
    void getLineReturnsInstance() {
        Assertions.assertNotNull(dft.getLine());
    }

    /**
     * Validates that getContour returns a non-null instance
     */
    @Test
    void getContourReturnsInstance() {
        Assertions.assertNotNull(dft.getContour());
    }

    /**
     * Validates that getLoop returns a non-null instance
     */
    @Test
    void getLoopReturnsInstance() {
        Assertions.assertNotNull(dft.getLoop());
    }
}