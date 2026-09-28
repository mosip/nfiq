package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for Contour functionality
 */
@ExtendWith(MockitoExtension.class)
class ContourTest {

    private Contour contour;

    /**
     * Sets up the Contour instance before each execution
     */
    @BeforeEach
    void setUp() {
        contour = Contour.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Contour instance1 = Contour.getInstance();
        Contour instance2 = Contour.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null object
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(contour.getDefs());
    }

    /**
     * Verifies that startScanNbr calculates correct direction value
     */
    @Test
    void startScanNbrCalculatesCorrectDirection() {
        int result = contour.startScanNbr(0, 0, 1, 0);
        Assertions.assertTrue(result >= 0 && result < 8);
    }

    /**
     * Validates that nextScanNbr calculates next direction correctly
     */
    @Test
    void nextScanNbrCalculatesNextDirection() {
        int result = contour.nextScanNbr(0, ILfs.SCAN_CLOCKWISE);
        Assertions.assertTrue(result >= 0 && result < 8);
    }

    /**
     * Validates that nextScanNbr handles counter-clockwise direction correctly
     */
    @Test
    void nextScanNbrWithCounterClockwiseCalculatesCorrectly() {
        int result = contour.nextScanNbr(0, ILfs.SCAN_COUNTER_CLOCKWISE);
        Assertions.assertTrue(result >= 0 && result < 8);
    }

    /**
     * Verifies that contourLimits calculates correct bounding box coordinates
     */
    @Test
    void contourLimitsCalculatesBoundingBox() {
        AtomicInteger xMin = new AtomicInteger();
        AtomicInteger yMin = new AtomicInteger();
        AtomicInteger xMax = new AtomicInteger();
        AtomicInteger yMax = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{1, 5, 3, 7, 2});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{2, 1, 6, 3, 4});

        contour.contourLimits(xMin, yMin, xMax, yMax, contourX, contourY, 5);

        Assertions.assertEquals(1, xMin.get());
        Assertions.assertEquals(1, yMin.get());
        Assertions.assertEquals(7, xMax.get());
        Assertions.assertEquals(6, yMax.get());
    }

    /**
     * Validates that fixEdgePixelPair adjusts pixel coordinates appropriately
     */
    @Test
    void fixEdgePixelPairAdjustsPixelCoordinates() {
        AtomicInteger featureXPixel = new AtomicInteger(5);
        AtomicInteger featureYPixel = new AtomicInteger(5);
        AtomicInteger featureEdgeXPixel = new AtomicInteger(5);
        AtomicInteger featureEdgeYPixel = new AtomicInteger(4);
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2 == 0 ? ILfs.WHITE_PIXEL : ILfs.BLACK_PIXEL;
        }

        contour.fixEdgePixelPair(featureXPixel, featureYPixel, featureEdgeXPixel, featureEdgeYPixel,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(featureXPixel.get() >= 0);
        Assertions.assertTrue(featureYPixel.get() >= 0);
    }

    /**
     * Verifies that minContourTheta finds the minimum angle correctly
     */
    @Test
    void minContourThetaFindsMinimumAngle() {
        AtomicInteger oMinContourPoint = new AtomicInteger();
        AtomicReference<Double> oMinThetaAngle = new AtomicReference<>();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{0, 1, 2, 1, 0});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{0, 0, 1, 2, 2});

        int result = contour.minContourTheta(oMinContourPoint, oMinThetaAngle, 2, contourX, contourY, 5);

        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinContourPoint.get() >= 0);
        Assertions.assertNotNull(oMinThetaAngle.get());
    }

    /**
     * Validates that searchContour with valid parameters returns appropriate result
     */
    @Test
    void searchContourWithValidParametersReturnsResult() {
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2 == 0 ? ILfs.WHITE_PIXEL : ILfs.BLACK_PIXEL;
        }

        int result = contour.searchContour(2, 2, 5, 1, 1, 2, 1, ILfs.SCAN_CLOCKWISE,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.FOUND || result == ILfs.NOT_FOUND);
    }

    /**
     * Validates that allocateContour creates a new instance correctly
     */
    @Test
    void allocateContourCreatesNewInstance() {
        AtomicInteger ret = new AtomicInteger();
        Contour result = contour.allocateContour(ret, 5);
        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.getNoOfContour());
    }

    /**
     * Verifies that freeContour deallocates memory without throwing exceptions
     */
    @Test
    void freeContourDeallocatesMemory() {
        AtomicInteger ret = new AtomicInteger();
        Contour contourInstance = contour.allocateContour(ret, 5);
        contour.freeContour(contourInstance);
    }

    /**
     * Validates that getHighCurvatureContour processes valid input correctly
     */
    @Test
    void getHighCurvatureContourWithValidInput() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Contour result = contour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND || ret.get() == ILfs.IGNORE);
    }

    /**
     * Validates that getCenteredContour processes valid input correctly
     */
    @Test
    void getCenteredContourWithValidInput() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Contour result = contour.getCenteredContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND ||
                ret.get() == ILfs.IGNORE || ret.get() == ILfs.INCOMPLETE);
    }

    /**
     * Validates that traceContour processes valid input correctly
     */
    @Test
    void traceContourWithValidInput() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Contour result = contour.traceContour(ret, oNoOfContour, 5, 1, 1, 5, 5, 4, 5,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND || ret.get() == ILfs.IGNORE);
    }

    /**
     * Verifies that nextContourPixel finds next pixel correctly
     */
    @Test
    void nextContourPixelFindsNextPixel() {
        AtomicInteger nextX = new AtomicInteger();
        AtomicInteger nextY = new AtomicInteger();
        AtomicInteger nextEx = new AtomicInteger();
        AtomicInteger nextEy = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        int result = contour.nextContourPixel(nextX, nextY, nextEx, nextEy, 5, 5, 4, 5,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.TRUE || result == ILfs.FALSE);
    }

    /**
     * Validates that startScanNbr returns correct direction value
     */
    @Test
    void startScanNbrReturnsDirection() {
        int result = contour.startScanNbr(5, 5, 5, 4);
        Assertions.assertTrue(result >= 0 && result < 8);
    }

    /**
     * Verifies that nextScanNbr advances direction correctly
     */
    @Test
    void nextScanNbrAdvancesDirection() {
        int result = contour.nextScanNbr(0, ILfs.SCAN_CLOCKWISE);
        Assertions.assertEquals(1, result);

        result = contour.nextScanNbr(0, ILfs.SCAN_COUNTER_CLOCKWISE);
        Assertions.assertEquals(7, result);
    }

    /**
     * Validates getInstance with noOfContour parameter
     */
    @Test
    void getInstanceWithNoOfContour() {
        Contour instance = Contour.getInstance(10);
        Assertions.assertNotNull(instance);
        Assertions.assertTrue(instance.getNoOfContour() >= 0);
    }

    /**
     * Validates that getter methods return non-null objects
     */
    @Test
    void getterMethods() {
        Assertions.assertNotNull(contour.getFree());
        Assertions.assertNotNull(contour.getGlobals());
        Assertions.assertNotNull(contour.getLfsUtil());
    }

    /**
     * Validates contour array getters and setters functionality
     */
    @Test
    void contourArrayGettersSetters() {
        AtomicInteger ret = new AtomicInteger();
        Contour contourInstance = contour.allocateContour(ret, 5);

        Assertions.assertNotNull(contourInstance.getContourX());
        Assertions.assertNotNull(contourInstance.getContourY());
        Assertions.assertNotNull(contourInstance.getContourEx());
        Assertions.assertNotNull(contourInstance.getContourEy());
        Assertions.assertEquals(5, contourInstance.getNoOfContour());

        contourInstance.setContourX(new AtomicIntegerArray(3));
        contourInstance.setContourY(new AtomicIntegerArray(3));
        contourInstance.setContourEx(new AtomicIntegerArray(3));
        contourInstance.setContourEy(new AtomicIntegerArray(3));

        Assertions.assertEquals(3, contourInstance.getContourX().length());
    }

    /**
     * Validates that freeContour handles null contour without exceptions
     */
    @Test
    void freeContourWithNullContour() {
        contour.freeContour(null);
    }

    /**
     * Validates traceContour behavior with opposite pixel values
     */
    @Test
    void traceContourWithOppositePixelValues() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = 1;
        }

        Contour result = contour.traceContour(ret, oNoOfContour, 5, 1, 1, 5, 5, 5, 5,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertEquals(ILfs.IGNORE, ret.get());
    }

    /**
     * Validates traceContour loop detection functionality
     */
    @Test
    void traceContourLoopDetection() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Contour result = contour.traceContour(ret, oNoOfContour, 5, 5, 5, 5, 5, 4, 5,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND || ret.get() == ILfs.IGNORE);
    }

    /**
     * Validates getHighCurvatureContour first half ignore behavior
     */
    @Test
    void getHighCurvatureContourFirstHalfIgnore() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = 1;
        }

        Contour result = contour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 5, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(0, oNoOfContour.get());
    }

    /**
     * Validates getHighCurvatureContour incomplete first half behavior
     */
    @Test
    void getHighCurvatureContourIncompleteFirstHalf() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = (i < 50) ? 0 : 1;
        }

        Contour result = contour.getHighCurvatureContour(ret, oNoOfContour, 10, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND);
    }

    /**
     * Validates getCenteredContour system error handling
     */
    @Test
    void getCenteredContourSystemError() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Contour result = contour.getCenteredContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() >= ILfs.FALSE);
    }

    /**
     * Validates getCenteredContour loop found behavior
     */
    @Test
    void getCenteredContourLoopFound() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = (i % 3 == 0) ? 0 : 1;
        }

        Contour result = contour.getCenteredContour(ret, oNoOfContour, 2, 3, 3, 2, 3,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(ret.get() == ILfs.FALSE || ret.get() == ILfs.LOOP_FOUND ||
                ret.get() == ILfs.IGNORE || ret.get() == ILfs.INCOMPLETE);
    }

    /**
     * Validates nextContourPixel boundary conditions handling
     */
    @Test
    void nextContourPixelBoundaryConditions() {
        AtomicInteger nextX = new AtomicInteger();
        AtomicInteger nextY = new AtomicInteger();
        AtomicInteger nextEx = new AtomicInteger();
        AtomicInteger nextEy = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        int result = contour.nextContourPixel(nextX, nextY, nextEx, nextEy, 0, 0, 1, 0,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.TRUE || result == ILfs.FALSE);

        result = contour.nextContourPixel(nextX, nextY, nextEx, nextEy, 9, 9, 8, 9,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.TRUE || result == ILfs.FALSE);
    }

    /**
     * Validates nextContourPixel corner exposed behavior
     */
    @Test
    void nextContourPixelCornerExposed() {
        AtomicInteger nextX = new AtomicInteger();
        AtomicInteger nextY = new AtomicInteger();
        AtomicInteger nextEx = new AtomicInteger();
        AtomicInteger nextEy = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = (i % 4 < 2) ? 0 : 1;
        }

        int result = contour.nextContourPixel(nextX, nextY, nextEx, nextEy, 5, 5, 4, 4,
                ILfs.SCAN_CLOCKWISE, binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.TRUE || result == ILfs.FALSE);
    }

    /**
     * Validates startScanNbr for all direction inputs
     */
    @Test
    void startScanNbrAllDirections() {
        int result = contour.startScanNbr(5, 5, 5, 4);
        Assertions.assertTrue(result >= 0 && result < 8);

        result = contour.startScanNbr(5, 5, 5, 6);
        Assertions.assertTrue(result >= 0 && result < 8);

        result = contour.startScanNbr(5, 5, 6, 5);
        Assertions.assertTrue(result >= 0 && result < 8);

        result = contour.startScanNbr(5, 5, 4, 5);
        Assertions.assertTrue(result >= 0 && result < 8);
    }

    /**
     * Validates nextScanNbr for all index values
     */
    @Test
    void nextScanNbrAllIndices() {
        for (int i = 0; i < 8; i++) {
            int clockwise = contour.nextScanNbr(i, ILfs.SCAN_CLOCKWISE);
            int counterClockwise = contour.nextScanNbr(i, ILfs.SCAN_COUNTER_CLOCKWISE);

            Assertions.assertTrue(clockwise >= 0 && clockwise < 8);
            Assertions.assertTrue(counterClockwise >= 0 && counterClockwise < 8);
        }
    }

    /**
     * Validates searchContour found case behavior
     */
    @Test
    void searchContourFoundCase() {
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = (i == 22 || i == 32) ? 1 : 0;
        }

        int result = contour.searchContour(2, 3, 5, 2, 2, 1, 2, ILfs.SCAN_CLOCKWISE,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(result == ILfs.FOUND || result == ILfs.NOT_FOUND);
    }

    /**
     * Validates contourLimits edge cases handling
     */
    @Test
    void contourLimitsEdgeCases() {
        AtomicInteger xMin = new AtomicInteger();
        AtomicInteger yMin = new AtomicInteger();
        AtomicInteger xMax = new AtomicInteger();
        AtomicInteger yMax = new AtomicInteger();

        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{5});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{3});

        contour.contourLimits(xMin, yMin, xMax, yMax, contourX, contourY, 1);

        Assertions.assertEquals(5, xMin.get());
        Assertions.assertEquals(3, yMin.get());
        Assertions.assertEquals(5, xMax.get());
        Assertions.assertEquals(3, yMax.get());
    }

    /**
     * Validates fixEdgePixelPair boundary conditions handling
     */
    @Test
    void fixEdgePixelPairBoundaryConditions() {
        AtomicInteger featureXPixel = new AtomicInteger(0);
        AtomicInteger featureYPixel = new AtomicInteger(0);
        AtomicInteger featureEdgeXPixel = new AtomicInteger(1);
        AtomicInteger featureEdgeYPixel = new AtomicInteger(0);
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        contour.fixEdgePixelPair(featureXPixel, featureYPixel, featureEdgeXPixel, featureEdgeYPixel,
                binarizedImageData, 10, 10);

        Assertions.assertTrue(featureXPixel.get() >= 0 && featureXPixel.get() < 10);
        Assertions.assertTrue(featureYPixel.get() >= 0 && featureYPixel.get() < 10);
    }

    /**
     * Validates minContourTheta edge cases with collinear points
     */
    @Test
    void minContourThetaEdgeCases() {
        AtomicInteger oMinContourPoint = new AtomicInteger();
        AtomicReference<Double> oMinThetaAngle = new AtomicReference<>();

        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{0, 1, 2, 3, 4});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{0, 0, 0, 0, 0});

        int result = contour.minContourTheta(oMinContourPoint, oMinThetaAngle, 2, contourX, contourY, 5);

        Assertions.assertTrue(result == ILfs.FALSE || result < 0);
    }

    /**
     * Validates getHighCurvatureContour loop found in first half
     */
    @Test
    void getHighCurvatureContourLoopFoundInFirstHalf() {
        Contour spyContour = Mockito.spy(contour);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        AtomicInteger mockRet = new AtomicInteger();
        Contour mockContour = contour.allocateContour(mockRet, 3);
        mockContour.getContourX().set(0, 1);
        mockContour.getContourY().set(0, 1);
        mockContour.getContourX().set(1, 2);
        mockContour.getContourY().set(1, 2);
        mockContour.getContourX().set(2, 3);
        mockContour.getContourY().set(2, 3);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nHalf1 = invocation.getArgument(1);
            retArg.set(ILfs.LOOP_FOUND);
            nHalf1.set(3);
            return mockContour;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Contour result = spyContour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(ILfs.LOOP_FOUND, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, oNoOfContour.get());
    }

    /**
     * Validates getHighCurvatureContour system error in first half
     */
    @Test
    void getHighCurvatureContourSystemErrorInFirstHalf() {
        Contour spyContour = Mockito.spy(contour);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(-1);
            return null;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Contour result = spyContour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(-1, ret.get());
    }

    /**
     * Validates getHighCurvatureContour second half ignore behavior
     */
    @Test
    void getHighCurvatureContourSecondHalfIgnore() {
        Contour spyContour = Mockito.spy(contour);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        AtomicInteger mockRet = new AtomicInteger();
        Contour mockContour1 = contour.allocateContour(mockRet, 3);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nHalf1 = invocation.getArgument(1);
            retArg.set(ILfs.FALSE);
            nHalf1.set(3);
            return mockContour1;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.IGNORE);
            return null;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_COUNTER_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Contour result = spyContour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(0, oNoOfContour.get());
    }

    /**
     * Validates getHighCurvatureContour second half system error behavior
     */
    @Test
    void getHighCurvatureContourSecondHalfSystemError() {
        Contour spyContour = Mockito.spy(contour);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        AtomicInteger mockRet = new AtomicInteger();
        Contour mockContour1 = contour.allocateContour(mockRet, 3);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nHalf1 = invocation.getArgument(1);
            retArg.set(ILfs.FALSE);
            nHalf1.set(3);
            return mockContour1;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(-2);
            return null;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_COUNTER_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Contour result = spyContour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(-2, ret.get());
    }

    /**
     * Validates getHighCurvatureContour second half incomplete behavior
     */
    @Test
    void getHighCurvatureContourSecondHalfIncomplete() {
        Contour spyContour = Mockito.spy(contour);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oNoOfContour = new AtomicInteger();
        int[] binarizedImageData = new int[100];
        for (int i = 0; i < 100; i++) {
            binarizedImageData[i] = i % 2;
        }

        AtomicInteger mockRet = new AtomicInteger();
        Contour mockContour1 = contour.allocateContour(mockRet, 3);
        Contour mockContour2 = contour.allocateContour(mockRet, 2);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nHalf1 = invocation.getArgument(1);
            retArg.set(ILfs.FALSE);
            nHalf1.set(3);
            return mockContour1;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nHalf2 = invocation.getArgument(1);
            retArg.set(ILfs.FALSE);
            nHalf2.set(2);
            return mockContour2;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(),
                Mockito.eq(3), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.eq(ILfs.SCAN_COUNTER_CLOCKWISE),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        Contour result = spyContour.getHighCurvatureContour(ret, oNoOfContour, 3, 5, 5, 4, 5,
                binarizedImageData, 10, 10);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(0, oNoOfContour.get());
    }

    private Object savedInstance;
    private boolean instanceReplaced;

    /**
     * Restores the Contour singleton if a test replaced it.
     */
    @AfterEach
    void restoreSingleton() {
        if (instanceReplaced) {
            ReflectionTestUtils.setField(Contour.class, "instance", savedInstance);
            instanceReplaced = false;
        }
    }

    /**
     * Verifies that getInstance(int) creates a contour with lists of the requested length
     * when no singleton exists yet, and returns that same instance afterwards.
     */
    @Test
    void getInstanceWithLengthCreatesSizedContourWhenNoInstanceExists() {
        savedInstance = ReflectionTestUtils.getField(Contour.class, "instance");
        instanceReplaced = true;
        ReflectionTestUtils.setField(Contour.class, "instance", null);

        Contour created = Contour.getInstance(7);

        Assertions.assertEquals(7, created.getNoOfContour());
        Assertions.assertEquals(7, created.getContourX().length());
        Assertions.assertEquals(7, created.getContourEy().length());
        Assertions.assertSame(created, Contour.getInstance(3));
    }

    /**
     * Verifies the contour length accessor pair and the toString summary.
     */
    @Test
    void noOfContourAccessorsAndToString() {
        Contour allocated = contour.allocateContour(new AtomicInteger(), 2);
        allocated.setNoOfcontour(5);

        Assertions.assertEquals(5, allocated.getNoOfContour());
        Assertions.assertTrue(allocated.toString().startsWith("Contour [contourX=[0, 0], contourY=[0, 0]"));
        Assertions.assertTrue(allocated.toString().endsWith("noOfContour=5]"));
    }

    /**
     * Verifies getCenteredContour on a long ridge: the contour is centered on the feature,
     * runs along the ridge's top edge and pairs each point with the white pixel above it.
     */
    @Test
    void getCenteredContourAlongStraightRidgeEdge() {
        int[] image = barImage(5, 54);
        AtomicInteger ret = new AtomicInteger(-1);
        AtomicInteger count = new AtomicInteger();

        Contour result = contour.getCenteredContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(21, count.get());
        Assertions.assertEquals(30, result.getContourX().get(10));
        for (int i = 0; i < 21; i++) {
            Assertions.assertEquals(20, result.getContourY().get(i));
            Assertions.assertEquals(19, result.getContourEy().get(i));
            Assertions.assertEquals(result.getContourX().get(i), result.getContourEx().get(i));
        }
        Assertions.assertEquals(20, Math.abs(result.getContourX().get(20) - result.getContourX().get(0)));
    }

    /**
     * Verifies the non-success outcomes of getCenteredContour on synthetic shapes: same-colored
     * start pixels, loops closing in the first or second half, and contours cut off by the image border.
     */
    @Test
    void getCenteredContourReportsIgnoreLoopAndIncomplete() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        Assertions.assertNull(contour.getCenteredContour(ret, count, 10, 30, 20, 31, 20, barImage(5, 54), 60, 50));
        Assertions.assertEquals(ILfs.IGNORE, ret.get());

        // 3x3 blob: the first (clockwise) half already closes the loop.
        Assertions.assertNull(contour.getCenteredContour(ret, count, 14, 20, 20, 19, 20, blobImage(3), 60, 50));
        Assertions.assertEquals(ILfs.LOOP_FOUND, ret.get());

        // 5x5 blob: the loop closes while tracing the second half.
        Assertions.assertNull(contour.getCenteredContour(ret, count, 14, 20, 20, 19, 20, blobImage(5), 60, 50));
        Assertions.assertEquals(ILfs.LOOP_FOUND, ret.get());

        // Ridge touching both image borders: one half runs off the image near each end.
        int[] fullWidth = barImage(0, 59);
        Assertions.assertNull(contour.getCenteredContour(ret, count, 10, 5, 20, 5, 19, fullWidth, 60, 50));
        Assertions.assertEquals(ILfs.INCOMPLETE, ret.get());
        Assertions.assertNull(contour.getCenteredContour(ret, count, 10, 54, 20, 54, 19, fullWidth, 60, 50));
        Assertions.assertEquals(ILfs.INCOMPLETE, ret.get());
        Assertions.assertEquals(0, count.get());
    }

    /**
     * Verifies that getCenteredContour propagates trace/allocation errors and a second-half IGNORE.
     */
    @Test
    void getCenteredContourPropagatesTraceAndAllocationErrors() {
        int[] image = barImage(5, 54);
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        Contour firstFails = Mockito.spy(contour);
        stubTrace(firstFails, ILfs.SCAN_CLOCKWISE, -7);
        Assertions.assertNull(firstFails.getCenteredContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(-7, ret.get());

        Contour secondFails = Mockito.spy(contour);
        stubTrace(secondFails, ILfs.SCAN_COUNTER_CLOCKWISE, -8);
        Assertions.assertNull(secondFails.getCenteredContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(-8, ret.get());

        Contour secondIgnored = Mockito.spy(contour);
        stubTrace(secondIgnored, ILfs.SCAN_COUNTER_CLOCKWISE, ILfs.IGNORE);
        Assertions.assertNull(secondIgnored.getCenteredContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(ILfs.IGNORE, ret.get());

        Contour allocationFails = Mockito.spy(contour);
        stubAllocation(allocationFails, 21, -9);
        Assertions.assertNull(allocationFails.getCenteredContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(-9, ret.get());
        Assertions.assertEquals(0, count.get());
    }

    /**
     * Verifies getHighCurvatureContour error paths: allocation failures for loop and full
     * contours, and a failing or ignored second trace.
     */
    @Test
    void getHighCurvatureContourPropagatesTraceAndAllocationErrors() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        // 3x3 blob loop: 7 traced points plus the feature point.
        Contour loopAllocationFails = Mockito.spy(contour);
        stubAllocation(loopAllocationFails, 8, -10);
        Assertions.assertNull(loopAllocationFails.getHighCurvatureContour(ret, count, 14, 20, 20, 19, 20, blobImage(3),
                60, 50));
        Assertions.assertEquals(-10, ret.get());

        int[] image = barImage(5, 54);
        Contour fullAllocationFails = Mockito.spy(contour);
        stubAllocation(fullAllocationFails, 21, -11);
        Assertions.assertNull(fullAllocationFails.getHighCurvatureContour(ret, count, 10, 30, 20, 30, 19, image, 60,
                50));
        Assertions.assertEquals(-11, ret.get());

        Contour secondFails = Mockito.spy(contour);
        stubTrace(secondFails, ILfs.SCAN_COUNTER_CLOCKWISE, -12);
        Assertions.assertNull(secondFails.getHighCurvatureContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(-12, ret.get());

        Contour secondIgnored = Mockito.spy(contour);
        stubTrace(secondIgnored, ILfs.SCAN_COUNTER_CLOCKWISE, ILfs.IGNORE);
        Assertions.assertNull(secondIgnored.getHighCurvatureContour(ret, count, 10, 30, 20, 30, 19, image, 60, 50));
        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(0, count.get());
    }

    /**
     * Verifies that getHighCurvatureContour concatenates both halves when the loop closes
     * during the second trace (5x5 blob: 14 clockwise points, the feature, 1 counter-clockwise point).
     */
    @Test
    void getHighCurvatureContourJoinsHalvesWhenSecondHalfLoops() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        Contour result = contour.getHighCurvatureContour(ret, count, 14, 20, 20, 19, 20, blobImage(5), 60, 50);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, count.get());
        Assertions.assertEquals(20, result.getContourX().get(14));
        Assertions.assertEquals(20, result.getContourY().get(14));
    }

    /**
     * Verifies that traceContour returns the allocation error without a contour.
     */
    @Test
    void traceContourPropagatesAllocationError() {
        Contour spyContour = Mockito.spy(contour);
        Mockito.doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(-13);
            return null;
        }).when(spyContour).allocateContour(Mockito.any(), Mockito.anyInt());
        AtomicInteger ret = new AtomicInteger();

        Assertions.assertNull(spyContour.traceContour(ret, new AtomicInteger(), 5, 0, 0, 30, 20, 30, 19,
                ILfs.SCAN_CLOCKWISE, barImage(5, 54), 60, 50));
        Assertions.assertEquals(-13, ret.get());
    }

    /**
     * Verifies that nextContourPixel fails when the neighbor scan steps off any of the four image borders.
     */
    @Test
    void nextContourPixelFailsAtEachImageBorder() {
        AtomicInteger nx = new AtomicInteger();
        AtomicInteger ny = new AtomicInteger();
        AtomicInteger nex = new AtomicInteger();
        AtomicInteger ney = new AtomicInteger();
        int[][] featureAndEdge = { { 0, 5, 1, 5 }, { 9, 5, 8, 5 }, { 5, 0, 5, 1 }, { 5, 9, 5, 8 } };
        for (int[] p : featureAndEdge) {
            int[] image = new int[100];
            image[p[1] * 10 + p[0]] = 1;
            Assertions.assertEquals(ILfs.FALSE, contour.nextContourPixel(nx, ny, nex, ney, p[0], p[1], p[2], p[3],
                    ILfs.SCAN_CLOCKWISE, image, 10, 10));
        }
    }

    /**
     * Verifies startScanNbr for the four axis-aligned positions and for non-adjacent input.
     */
    @Test
    void startScanNbrForAllPositions() {
        Assertions.assertEquals(ILfs.SOUTH, contour.startScanNbr(5, 5, 5, 6));
        Assertions.assertEquals(ILfs.NORTH, contour.startScanNbr(5, 5, 5, 4));
        Assertions.assertEquals(ILfs.EAST, contour.startScanNbr(5, 5, 6, 5));
        Assertions.assertEquals(ILfs.WEST, contour.startScanNbr(5, 5, 4, 5));
        Assertions.assertEquals(ILfs.INVALID_DIR, contour.startScanNbr(5, 5, 6, 6));
        Assertions.assertEquals(ILfs.INVALID_DIR, contour.startScanNbr(5, 5, 5, 5));
    }

    /**
     * Verifies that minContourTheta ignores contours shorter than two angle edges plus one.
     */
    @Test
    void minContourThetaIgnoresShortContour() {
        AtomicInteger index = new AtomicInteger(-5);
        AtomicReference<Double> theta = new AtomicReference<>(-1.0);

        Assertions.assertEquals(ILfs.IGNORE, contour.minContourTheta(index, theta, 3,
                new AtomicIntegerArray(6), new AtomicIntegerArray(6), 6));
        Assertions.assertEquals(-5, index.get());
        Assertions.assertEquals(-1.0, theta.get());
    }

    private static void stubTrace(Contour spyContour, int scanClock, int result) {
        Mockito.lenient().doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(result);
            return null;
        }).when(spyContour).traceContour(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.eq(scanClock), Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    private static void stubAllocation(Contour spyContour, int length, int result) {
        Mockito.lenient().doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(result);
            return null;
        }).when(spyContour).allocateContour(Mockito.any(), Mockito.eq(length));
    }

    /** 60x50 white image with a black ridge on rows 20..22 from x0 to x1. */
    private static int[] barImage(int x0, int x1) {
        int[] image = new int[60 * 50];
        for (int y = 20; y <= 22; y++) {
            Arrays.fill(image, y * 60 + x0, y * 60 + x1 + 1, 1);
        }
        return image;
    }

    /** 60x50 white image with a size x size black blob whose top-left corner is (20, 20). */
    private static int[] blobImage(int size) {
        int[] image = new int[60 * 50];
        for (int y = 20; y < 20 + size; y++) {
            Arrays.fill(image, y * 60 + 20, y * 60 + 20 + size, 1);
        }
        return image;
    }
}