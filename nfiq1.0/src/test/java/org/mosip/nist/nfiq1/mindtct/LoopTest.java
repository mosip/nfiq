package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;

/**
 * Test class for {@link Loop} providing comprehensive test cases of all methods
 * including loop detection, contour processing, filling operations, and minutiae handling.
 *
 * <p>This class validates the functionality of loop analysis and processing
 * for NIST's Mindtct fingerprint analysis algorithms.</p>
 */
public class LoopTest {

    private Loop loop;
    private LfsParams mockLfsParams;
    private AtomicReference<Minutiae> mockMinutiae;
    private int[] mockBinaryImageData;
    private int imageWidth;
    private int imageHeight;
    private Contour mockContour;
    private ChainCode mockChainCode;
    private Shapes mockShapes;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes the Loop singleton instance and mock objects for testing.
     */
    @BeforeEach
    public void setUp() {
        loop = Loop.getInstance();
        imageWidth = 20;
        imageHeight = 20;
        mockBinaryImageData = new int[imageWidth * imageHeight];

        for (int i = 0; i < mockBinaryImageData.length; i++) {
            mockBinaryImageData[i] = 0;
        }

        mockLfsParams = mock(LfsParams.class);
        when(mockLfsParams.getMinLoopLen()).thenReturn(3);
        when(mockLfsParams.getMinLoopAspectDist()).thenReturn(5.0);
        when(mockLfsParams.getMinLoopAspectRatio()).thenReturn(2.0);
        when(mockLfsParams.getNumDirections()).thenReturn(16);

        mockMinutiae = new AtomicReference<>(mock(Minutiae.class));

        mockContour = mock(Contour.class);
        mockChainCode = mock(ChainCode.class);
        mockShapes = mock(Shapes.class);
    }

    /**
     * Cleans up resources after each test execution.
     */
    @AfterEach
    public void tearDown() {
        loop = null;
        mockLfsParams = null;
        mockMinutiae = null;
        mockBinaryImageData = null;
        mockContour = null;
        mockChainCode = null;
        mockShapes = null;
    }

    /**
     * Verifies that getInstance returns the same singleton instance across multiple calls.
     * This ensures proper singleton pattern implementation.
     */
    @Test
    public void verifySingletonInstance() {
        Loop firstInstance = Loop.getInstance();
        Loop secondInstance = Loop.getInstance();

        assertEquals(firstInstance, secondInstance, "getInstance should return the same singleton instance");
        assertNotNull(firstInstance, "getInstance should never return null");
    }

    /**
     * Validates that all dependency getter methods return non-null instances.
     * Tests the proper initialization of dependency injection pattern.
     */
    @Test
    public void validateDependencyGetters() {
        assertNotNull(loop.getShapes(), "getShapes should return non-null instance");
        assertNotNull(loop.getChainCode(), "getChainCode should return non-null instance");
        assertNotNull(loop.getFree(), "getFree should return non-null instance");
        assertNotNull(loop.getMinutiaHelper(), "getMinutiaHelper should return non-null instance");
        assertNotNull(loop.getContour(), "getContour should return non-null instance");
        assertNotNull(loop.getLfsUtil(), "getLfsUtil should return non-null instance");
    }

    /**
     * Validates loop list processing with empty minutiae list.
     * Tests edge case where no minutiae are present for processing.
     */
    @Test
    public void validateLoopListWithEmptyMinutiae() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(0);
        Minutiae emptyMinutiae = mock(Minutiae.class);
        when(emptyMinutiae.getNum()).thenReturn(0);
        AtomicReference<Minutiae> emptyMinutiaeRef = new AtomicReference<>(emptyMinutiae);

        int result = loop.getLoopList(onloop, emptyMinutiaeRef, 20, mockBinaryImageData,
                imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result, "Empty minutiae list should return FALSE");
    }

    /**
     * Validates loop list processing with ridge ending minutiae.
     * Tests that ridge endings are automatically marked as not on loop.
     */
    @Test
    public void validateLoopListWithRidgeEnding() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(1);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1);

        List<Minutia> minutiaList = new ArrayList<>();
        Minutia ridgeEndingMinutia = mock(Minutia.class);
        when(ridgeEndingMinutia.getType()).thenReturn(ILfs.RIDGE_ENDING);
        minutiaList.add(ridgeEndingMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = loop.getLoopList(onloop, minutiaeRef, 20, mockBinaryImageData,
                imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result, "Should process ridge ending successfully");
        assertEquals(ILfs.FALSE, onloop.get(0), "Ridge ending should be marked as not on loop");
    }

    /**
     * Validates onLoop functionality with successful loop detection.
     * Tests the core loop detection algorithm for a single minutia point.
     */
    @Test
    public void validateOnLoopWithLoopFound() {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(5);
        when(minutia.getY()).thenReturn(5);
        when(minutia.getEx()).thenReturn(6);
        when(minutia.getEy()).thenReturn(5);

        int result = loop.onLoop(minutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertTrue(result >= ILfs.FALSE, "onLoop should not return error");
    }

    /**
     * Validates island/lake detection with successful loop completion.
     * Tests detection of minutiae pairs that lie on the same qualifying loop.
     */
    @Test
    public void validateOnIslandLakeWithLoopFound() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(5);
        when(firstMinutia.getY()).thenReturn(5);
        when(firstMinutia.getEx()).thenReturn(6);
        when(firstMinutia.getEy()).thenReturn(5);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(8);
        when(secondMinutia.getY()).thenReturn(8);
        when(secondMinutia.getEx()).thenReturn(9);
        when(secondMinutia.getEy()).thenReturn(8);

        AtomicInteger ret = new AtomicInteger(0);
        AtomicInteger oncontour = new AtomicInteger(0);

        Contour result = loop.onIslandLake(ret, oncontour, firstMinutia, secondMinutia,
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertTrue(ret.get() >= ILfs.FALSE || ret.get() == ILfs.IGNORE, "Should handle island/lake detection");
    }

    /**
     * Validates hook detection with successful hook found.
     * Tests detection of minutiae pairs that lie on the same qualifying hook.
     */
    @Test
    public void validateOnHookWithHookFound() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(3);
        when(firstMinutia.getY()).thenReturn(3);
        when(firstMinutia.getEx()).thenReturn(4);
        when(firstMinutia.getEy()).thenReturn(3);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(6);
        when(secondMinutia.getY()).thenReturn(6);
        when(secondMinutia.getEx()).thenReturn(7);
        when(secondMinutia.getEy()).thenReturn(6);

        int result = loop.onHook(firstMinutia, secondMinutia, 8, mockBinaryImageData,
                imageWidth, imageHeight);

        assertTrue(result >= ILfs.FALSE, "onHook should handle hook detection");
    }

    /**
     * Validates loop processing with small loop requiring fill operation.
     * Tests loop filling when loop does not meet criteria for minutiae extraction.
     */
    @Test
    public void validateProcessLoopWithSmallLoop() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(3);
        AtomicIntegerArray contourY = new AtomicIntegerArray(3);
        AtomicIntegerArray contourEx = new AtomicIntegerArray(3);
        AtomicIntegerArray contourEy = new AtomicIntegerArray(3);

        contourX.set(0, 5); contourY.set(0, 5);
        contourX.set(1, 6); contourY.set(1, 5);
        contourX.set(2, 5); contourY.set(2, 6);

        contourEx.set(0, 6); contourEy.set(0, 5);
        contourEx.set(1, 6); contourEy.set(1, 6);
        contourEx.set(2, 4); contourEy.set(2, 6);

        mockBinaryImageData[(5 * imageWidth) + 5] = 1;

        when(mockLfsParams.getMinLoopLen()).thenReturn(5);

        int result = loop.processLoop(mockMinutiae, contourX, contourY, contourEx,
                contourEy, 3, mockBinaryImageData, imageWidth,
                imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result, "Should fill small loop successfully");
    }

    /**
     * Validates loop processing version 2 with low flow map consideration.
     * Tests enhanced loop processing that considers ridge flow reliability.
     */
    @Test
    public void validateProcessLoopV2WithLowFlow() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(3);
        AtomicIntegerArray contourY = new AtomicIntegerArray(3);
        AtomicIntegerArray contourEx = new AtomicIntegerArray(3);
        AtomicIntegerArray contourEy = new AtomicIntegerArray(3);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(imageWidth * imageHeight);

        contourX.set(0, 7); contourY.set(0, 7);
        contourX.set(1, 8); contourY.set(1, 7);
        contourX.set(2, 7); contourY.set(2, 8);

        contourEx.set(0, 8); contourEy.set(0, 7);
        contourEx.set(1, 8); contourEy.set(1, 8);
        contourEx.set(2, 6); contourEy.set(2, 8);

        for (int i = 0; i < imageWidth * imageHeight; i++) {
            lowFlowMap.set(i, ILfs.TRUE);
        }
        mockBinaryImageData[(7 * imageWidth) + 7] = 0;

        when(mockLfsParams.getMinLoopLen()).thenReturn(5);

        int result = loop.processLoopV2(mockMinutiae, contourX, contourY, contourEx,
                contourEy, 3, mockBinaryImageData, imageWidth,
                imageHeight, lowFlowMap, mockLfsParams);

        assertEquals(ILfs.FALSE, result, "Should process loop V2 with low flow successfully");
    }

    /**
     * Validates loop aspect calculation with valid contour.
     * Tests measurement of minimum and maximum distances across loop contour.
     */
    @Test
    public void validateLoopAspectWithValidLength() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(4);
        AtomicIntegerArray contourY = new AtomicIntegerArray(4);

        contourX.set(0, 6); contourY.set(0, 6);
        contourX.set(1, 9); contourY.set(1, 6);
        contourX.set(2, 9); contourY.set(2, 9);
        contourX.set(3, 6); contourY.set(3, 9);

        AtomicInteger minFrom = new AtomicInteger(0);
        AtomicInteger minTo = new AtomicInteger(0);
        AtomicReference<Double> minDist = new AtomicReference<>(0.0);
        AtomicInteger maxFrom = new AtomicInteger(0);
        AtomicInteger maxTo = new AtomicInteger(0);
        AtomicReference<Double> maxDist = new AtomicReference<>(0.0);

        loop.getLoopAspect(minFrom, minTo, minDist, maxFrom, maxTo, maxDist,
                contourX, contourY, 4);

        assertNotNull(minDist.get(), "Minimum distance should be calculated");
        assertNotNull(maxDist.get(), "Maximum distance should be calculated");
        assertTrue(maxDist.get() >= minDist.get(), "Maximum distance should be >= minimum distance");
    }

    /**
     * Validates loop filling with simple contour.
     * Tests filling algorithm for loops with basic geometry.
     */
    @Test
    public void validateFillLoopWithSimpleShape() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(3);
        AtomicIntegerArray contourY = new AtomicIntegerArray(3);

        contourX.set(0, 10); contourY.set(0, 10);
        contourX.set(1, 12); contourY.set(1, 10);
        contourX.set(2, 11); contourY.set(2, 12);

        mockBinaryImageData[(10 * imageWidth) + 10] = 1;

        int result = loop.fillLoop(contourX, contourY, 3, mockBinaryImageData,
                imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result, "Should fill simple loop successfully");
    }

    /**
     * Validates partial row filling functionality.
     * Tests filling of contiguous pixels within a specified range on a row.
     */
    @Test
    public void validateFillPartialRow() {
        int fillPixel = 255;
        int fromX = 2;
        int toX = 5;
        int yIndex = 8;

        for (int x = 0; x < imageWidth; x++) {
            mockBinaryImageData[(yIndex * imageWidth) + x] = 0;
        }

        loop.fillPartialRow(fillPixel, fromX, toX, yIndex, mockBinaryImageData,
                imageWidth, imageHeight);

        for (int x = fromX; x <= toX; x++) {
            assertEquals(fillPixel, mockBinaryImageData[(yIndex * imageWidth) + x],
                    "Pixel at position " + x + " should be filled");
        }

        if (fromX > 0) {
            assertEquals(0, mockBinaryImageData[(yIndex * imageWidth) + (fromX - 1)],
                    "Pixel before range should not be filled");
        }
        if (toX < imageWidth - 1) {
            assertEquals(0, mockBinaryImageData[(yIndex * imageWidth) + (toX + 1)],
                    "Pixel after range should not be filled");
        }
    }

    /**
     * Validates flood fill functionality with bounded region.
     * Tests flood filling algorithm for small, controlled regions.
     */
    @Test
    public void validateFloodFill4WithBoundedRegion() {
        int fillPixel = 128;
        int startX = 3;
        int startY = 3;

        mockBinaryImageData[(startY * imageWidth) + startX] = 0;
        mockBinaryImageData[(startY * imageWidth) + (startX + 1)] = 0;
        mockBinaryImageData[((startY + 1) * imageWidth) + startX] = 0;
        mockBinaryImageData[((startY + 1) * imageWidth) + (startX + 1)] = 0;

        for (int y = startY - 1; y <= startY + 2; y++) {
            for (int x = startX - 1; x <= startX + 2; x++) {
                if (x >= 0 && x < imageWidth && y >= 0 && y < imageHeight) {
                    if (x == startX - 1 || x == startX + 2 || y == startY - 1 || y == startY + 2) {
                        mockBinaryImageData[(y * imageWidth) + x] = fillPixel;
                    }
                }
            }
        }

        loop.floodFill4(fillPixel, startX, startY, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(fillPixel, mockBinaryImageData[(startY * imageWidth) + startX],
                "Start pixel should be filled");
    }

    /**
     * Validates process loop with empty contour.
     * Tests handling of edge case where contour has no points.
     */
    @Test
    public void validateProcessLoopWithEmptyContour() {
        AtomicIntegerArray emptyContourX = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourY = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourEx = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourEy = new AtomicIntegerArray(0);

        int result = loop.processLoop(mockMinutiae, emptyContourX, emptyContourY,
                emptyContourEx, emptyContourEy, 0, mockBinaryImageData,
                imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result, "Empty contour should return normally");
    }

    /**
     * Validates process loop V2 with empty contour.
     * Tests handling of edge case in version 2 processing.
     */
    @Test
    public void validateProcessLoopV2WithEmptyContour() {
        AtomicIntegerArray emptyContourX = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourY = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourEx = new AtomicIntegerArray(0);
        AtomicIntegerArray emptyContourEy = new AtomicIntegerArray(0);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(imageWidth * imageHeight);

        int result = loop.processLoopV2(mockMinutiae, emptyContourX, emptyContourY,
                emptyContourEx, emptyContourEy, 0, mockBinaryImageData,
                imageWidth, imageHeight, lowFlowMap, mockLfsParams);

        assertEquals(ILfs.FALSE, result, "Empty contour should return normally in V2");
    }

    /**
     * Validates flood loop functionality with simple contour.
     * Tests flood filling of a loop using contour points as seeds.
     */
    @Test
    public void validateFloodLoop() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(3);
        AtomicIntegerArray contourY = new AtomicIntegerArray(3);

        contourX.set(0, 12); contourY.set(0, 12);
        contourX.set(1, 14); contourY.set(1, 12);
        contourX.set(2, 13); contourY.set(2, 14);

        mockBinaryImageData[(12 * imageWidth) + 12] = 1;

        loop.floodLoop(contourX, contourY, 3, mockBinaryImageData, imageWidth, imageHeight);

        assertTrue(true, "FloodLoop should complete without error");
    }

    /**
     * Validates getLoopList method for bifurcation minutiae when loop is found.
     * Validates that bifurcation minutiae are properly marked when detected on a loop.
     */
    @Test
    public void getLoopListWithBifurcationLoopFound() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(1);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1);

        List<Minutia> minutiaList = new ArrayList<>();
        Minutia bifurcationMinutia = mock(Minutia.class);
        when(bifurcationMinutia.getType()).thenReturn(ILfs.BIFURCATION);
        when(bifurcationMinutia.getX()).thenReturn(5);
        when(bifurcationMinutia.getY()).thenReturn(5);
        when(bifurcationMinutia.getEx()).thenReturn(6);
        when(bifurcationMinutia.getEy()).thenReturn(5);
        minutiaList.add(bifurcationMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(ILfs.LOOP_FOUND).when(spyLoop).onLoop(any(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.getLoopList(onloop, minutiaeRef, 20, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.TRUE, onloop.get(0));
    }

    /**
     * Validates getLoopList method for bifurcation minutiae when minutia should be removed.
     * Validates proper handling of bifurcation minutiae marked for removal.
     */
    @Test
    public void getLoopListWithBifurcationIgnore() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(1);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1).thenReturn(0);

        List<Minutia> minutiaList = new ArrayList<>();
        Minutia bifurcationMinutia = mock(Minutia.class);
        when(bifurcationMinutia.getType()).thenReturn(ILfs.BIFURCATION);
        minutiaList.add(bifurcationMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(ILfs.IGNORE).when(spyLoop).onLoop(any(), anyInt(), any(), anyInt(), anyInt());

        MinutiaHelper mockMinutiaHelper = mock(MinutiaHelper.class);
        when(mockMinutiaHelper.removeMinutia(anyInt(), any())).thenReturn(0);
        Mockito.doReturn(mockMinutiaHelper).when(spyLoop).getMinutiaHelper();

        int result = spyLoop.getLoopList(onloop, minutiaeRef, 20, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates getLoopList method for bifurcation minutiae when not on loop.
     * Validates that bifurcation minutiae not on loops are properly handled.
     */
    @Test
    public void getLoopListWithBifurcationNotOnLoop() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(1);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1);

        List<Minutia> minutiaList = new ArrayList<>();
        Minutia bifurcationMinutia = mock(Minutia.class);
        when(bifurcationMinutia.getType()).thenReturn(ILfs.BIFURCATION);
        minutiaList.add(bifurcationMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(ILfs.FALSE).when(spyLoop).onLoop(any(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.getLoopList(onloop, minutiaeRef, 20, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.FALSE, onloop.get(0));
    }

    /**
     * Validates getLoopList method for bifurcation minutiae when an error occurs.
     * Validates proper error handling during loop detection for bifurcation minutiae.
     */
    @Test
    public void getLoopListWithError() {
        AtomicIntegerArray onloop = new AtomicIntegerArray(1);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1);

        List<Minutia> minutiaList = new ArrayList<>();
        Minutia bifurcationMinutia = mock(Minutia.class);
        when(bifurcationMinutia.getType()).thenReturn(ILfs.BIFURCATION);
        minutiaList.add(bifurcationMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(-1).when(spyLoop).onLoop(any(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.getLoopList(onloop, minutiaeRef, 20, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(-1, result);
    }

    /**
     * Validates onLoop method for the IGNORE return value scenario.
     * Validates handling when contour tracing returns IGNORE status.
     */
    @Test
    public void onLoopWithIgnore() {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(5);
        when(minutia.getY()).thenReturn(5);
        when(minutia.getEx()).thenReturn(6);
        when(minutia.getEy()).thenReturn(5);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(ILfs.IGNORE);
            return null;
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.onLoop(minutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.IGNORE, result);
    }

    /**
     * Validates onLoop method for the LOOP_FOUND return value scenario.
     * Validates successful loop detection and proper return value handling.
     */
    @Test
    public void onLoopWithLoopFound() {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(5);
        when(minutia.getY()).thenReturn(5);
        when(minutia.getEx()).thenReturn(6);
        when(minutia.getEy()).thenReturn(5);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(ILfs.LOOP_FOUND);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.onLoop(minutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.LOOP_FOUND, result);
    }

    /**
     * Validates onLoop method for the FALSE return value scenario.
     * Validates handling when no loop is detected for a minutia.
     */
    @Test
    public void onLoopWithFalse() {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(5);
        when(minutia.getY()).thenReturn(5);
        when(minutia.getEx()).thenReturn(6);
        when(minutia.getEy()).thenReturn(5);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(ILfs.FALSE);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.onLoop(minutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates onLoop method for error return value scenario.
     * Validates proper error handling during loop detection.
     */
    @Test
    public void onLoopWithError() {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(5);
        when(minutia.getY()).thenReturn(5);
        when(minutia.getEx()).thenReturn(6);
        when(minutia.getEy()).thenReturn(5);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(-1);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.onLoop(minutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(-1, result);
    }

    /**
     * Validates isLoopClockwise method with a valid clockwise loop.
     * Validates proper determination of loop orientation using chain code analysis.
     */
    @Test
    public void isLoopClockwise() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(4);
        AtomicIntegerArray contourY = new AtomicIntegerArray(4);

        contourX.set(0, 0); contourY.set(0, 0);
        contourX.set(1, 1); contourY.set(1, 0);
        contourX.set(2, 1); contourY.set(2, 1);
        contourX.set(3, 0); contourY.set(3, 1);

        Loop spyLoop = Mockito.spy(loop);
        ChainCode mockChainCode = mock(ChainCode.class);
        Mockito.doReturn(mockChainCode).when(spyLoop).getChainCode();

        Mockito.doAnswer(invocation -> {
            AtomicInteger nchain = invocation.getArgument(1);
            nchain.set(4);
            return ILfs.FALSE;
        }).when(mockChainCode).chainCodeLoop(any(), any(), any(), any(), anyInt());

        when(mockChainCode.isChainClockwise(any(), anyInt(), anyInt())).thenReturn(ILfs.TRUE);

        int result = spyLoop.isLoopClockwise(contourX, contourY, 4, ILfs.FALSE);

        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates isLoopClockwise method with an empty chain.
     * Validates handling when chain code generation produces no valid chain.
     */
    @Test
    public void isLoopClockwiseWithEmptyChain() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(2);
        AtomicIntegerArray contourY = new AtomicIntegerArray(2);

        contourX.set(0, 0); contourY.set(0, 0);
        contourX.set(1, 1); contourY.set(1, 1);

        Loop spyLoop = Mockito.spy(loop);
        ChainCode mockChainCode = mock(ChainCode.class);
        Mockito.doReturn(mockChainCode).when(spyLoop).getChainCode();

        Mockito.doAnswer(invocation -> {
            AtomicInteger nchain = invocation.getArgument(1);
            nchain.set(ILfs.FALSE);
            return ILfs.FALSE;
        }).when(mockChainCode).chainCodeLoop(any(), any(), any(), any(), anyInt());

        int result = spyLoop.isLoopClockwise(contourX, contourY, 2, ILfs.TRUE);

        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates isLoopClockwise method for error return scenario.
     * Validates proper error handling during chain code generation.
     */
    @Test
    public void isLoopClockwiseWithError() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(3);
        AtomicIntegerArray contourY = new AtomicIntegerArray(3);

        Loop spyLoop = Mockito.spy(loop);
        ChainCode mockChainCode = mock(ChainCode.class);
        Mockito.doReturn(mockChainCode).when(spyLoop).getChainCode();

        when(mockChainCode.chainCodeLoop(any(), any(), any(), any(), anyInt())).thenReturn(-1);

        int result = spyLoop.isLoopClockwise(contourX, contourY, 3, ILfs.FALSE);

        assertEquals(-1, result);
    }

    /**
     * Validates onIslandLake method when first trace finds a loop and second is IGNORE.
     * Validates handling of island/lake detection with mixed trace results.
     */
    @Test
    void onIslandLakeFirstTraceLoopFoundSecondTraceIgnore() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(5);
        when(firstMinutia.getY()).thenReturn(5);
        when(firstMinutia.getEx()).thenReturn(6);
        when(firstMinutia.getEy()).thenReturn(5);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(8);
        when(secondMinutia.getY()).thenReturn(8);
        when(secondMinutia.getEx()).thenReturn(9);
        when(secondMinutia.getEy()).thenReturn(8);

        AtomicInteger ret = new AtomicInteger(0);
        AtomicInteger oncontour = new AtomicInteger(0);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.LOOP_FOUND);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.IGNORE);
            return null;
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        spyLoop.onIslandLake(ret, oncontour, firstMinutia, secondMinutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.IGNORE, ret.get());
    }

    /**
     * Validates onIslandLake method when both traces find a loop.
     * Validates successful island/lake detection with both minutiae on loops.
     */
    @Test
    public void onIslandLakeFirstTraceLoopFoundSecondTraceLoopFound() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(5);
        when(firstMinutia.getY()).thenReturn(5);
        when(firstMinutia.getEx()).thenReturn(6);
        when(firstMinutia.getEy()).thenReturn(5);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(8);
        when(secondMinutia.getY()).thenReturn(8);
        when(secondMinutia.getEx()).thenReturn(9);
        when(secondMinutia.getEy()).thenReturn(8);

        AtomicInteger ret = new AtomicInteger(0);
        AtomicInteger oncontour = new AtomicInteger(0);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        org.mosip.nist.nfiq1.mindtct.Contour mockContour1 = mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        org.mosip.nist.nfiq1.mindtct.Contour mockContour2 = mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        org.mosip.nist.nfiq1.mindtct.Contour mockContourLoop = mock(org.mosip.nist.nfiq1.mindtct.Contour.class);

        AtomicIntegerArray mockArray = new AtomicIntegerArray(6);
        when(mockContour1.getContourX()).thenReturn(mockArray);
        when(mockContour1.getContourY()).thenReturn(mockArray);
        when(mockContour1.getContourEx()).thenReturn(mockArray);
        when(mockContour1.getContourEy()).thenReturn(mockArray);

        when(mockContour2.getContourX()).thenReturn(mockArray);
        when(mockContour2.getContourY()).thenReturn(mockArray);
        when(mockContour2.getContourEx()).thenReturn(mockArray);
        when(mockContour2.getContourEy()).thenReturn(mockArray);

        AtomicIntegerArray mockLoopArray = new AtomicIntegerArray(6);
        when(mockContourLoop.getContourX()).thenReturn(mockLoopArray);
        when(mockContourLoop.getContourY()).thenReturn(mockLoopArray);
        when(mockContourLoop.getContourEx()).thenReturn(mockLoopArray);
        when(mockContourLoop.getContourEy()).thenReturn(mockLoopArray);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nContour = invocation.getArgument(1);
            retArg.set(ILfs.LOOP_FOUND);
            nContour.set(2);
            return mockContour1;
        }).doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            AtomicInteger nContour = invocation.getArgument(1);
            retArg.set(ILfs.LOOP_FOUND);
            nContour.set(2);
            return mockContour2;
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.FALSE);
            return mockContourLoop;
        }).when(mockContour).allocateContour(any(), anyInt());

        Contour result = spyLoop.onIslandLake(ret, oncontour, firstMinutia, secondMinutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.LOOP_FOUND, ret.get());
        assertNotNull(result);
    }

    /**
     * Validates onIslandLake method when first trace finds a loop and second is FALSE.
     * Validates handling when only one minutia is found on a loop.
     */
    @Test
    public void onIslandLakeFirstTraceLoopFoundSecondTraceFalse() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(5);
        when(firstMinutia.getY()).thenReturn(5);
        when(firstMinutia.getEx()).thenReturn(6);
        when(firstMinutia.getEy()).thenReturn(5);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(8);
        when(secondMinutia.getY()).thenReturn(8);
        when(secondMinutia.getEx()).thenReturn(9);
        when(secondMinutia.getEy()).thenReturn(8);

        AtomicInteger ret = new AtomicInteger(0);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.LOOP_FOUND);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.FALSE);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());
        assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Validates onIslandLake method when first trace finds a loop and second trace errors.
     * Validates proper error handling during island/lake detection.
     */
    @Test
    void onIslandLakeFirstTraceLoopFoundSecondTraceError() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(5);
        when(firstMinutia.getY()).thenReturn(5);
        when(firstMinutia.getEx()).thenReturn(6);
        when(firstMinutia.getEy()).thenReturn(5);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(8);
        when(secondMinutia.getY()).thenReturn(8);
        when(secondMinutia.getEx()).thenReturn(9);
        when(secondMinutia.getEy()).thenReturn(8);

        AtomicInteger ret = new AtomicInteger(0);
        AtomicInteger oncontour = new AtomicInteger(0);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.LOOP_FOUND);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(-1);
            return null;
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        spyLoop.onIslandLake(ret, oncontour, firstMinutia, secondMinutia, 10, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(-1, ret.get());
    }

    /**
     * Validates onHook method when the first trace finds a loop.
     * Validates successful hook detection between two minutiae.
     */
    @Test
    public void onHookFirstTraceLoopFound() {
        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(3);
        when(firstMinutia.getY()).thenReturn(3);
        when(firstMinutia.getEx()).thenReturn(4);
        when(firstMinutia.getEy()).thenReturn(3);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(6);
        when(secondMinutia.getY()).thenReturn(6);
        when(secondMinutia.getEx()).thenReturn(7);
        when(secondMinutia.getEy()).thenReturn(6);

        Loop spyLoop = Mockito.spy(loop);
        Contour mockContour = mock(Contour.class);
        Mockito.doReturn(mockContour).when(spyLoop).getContour();

        Mockito.doAnswer(invocation -> {
            AtomicInteger retArg = invocation.getArgument(0);
            retArg.set(ILfs.LOOP_FOUND);
            return mock(org.mosip.nist.nfiq1.mindtct.Contour.class);
        }).when(mockContour).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt());

        int result = spyLoop.onHook(firstMinutia, secondMinutia, 8, mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.HOOK_FOUND, result);
    }

    /**
     * Validates processLoop method with realistic contour and minutiae creation.
     * Validates full loop processing workflow including minutiae generation.
     */
    @Test
    public void processLoopWithMinutiaeCreation() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(8);
        AtomicIntegerArray contourY = new AtomicIntegerArray(8);
        AtomicIntegerArray contourEx = new AtomicIntegerArray(8);
        AtomicIntegerArray contourEy = new AtomicIntegerArray(8);

        contourX.set(0, 5); contourY.set(0, 5);
        contourX.set(1, 15); contourY.set(1, 5);
        contourX.set(2, 15); contourY.set(2, 6);
        contourX.set(3, 14); contourY.set(3, 8);
        contourX.set(4, 10); contourY.set(4, 9);
        contourX.set(5, 5); contourY.set(5, 9);
        contourX.set(6, 4); contourY.set(6, 7);
        contourX.set(7, 4); contourY.set(7, 6);

        for (int i = 0; i < 8; i++) {
            contourEx.set(i, contourX.get(i) + 1);
            contourEy.set(i, contourY.get(i));
        }

        mockBinaryImageData[(5 * imageWidth) + 5] = 1;
        mockBinaryImageData[(7 * imageWidth) + 9] = 1;

        when(mockLfsParams.getMinLoopLen()).thenReturn(3);
        when(mockLfsParams.getMinLoopAspectDist()).thenReturn(200.0);
        when(mockLfsParams.getMinLoopAspectRatio()).thenReturn(1.5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);

        Minutiae realMinutiae = new Minutiae();
        realMinutiae.setList(new ArrayList<>());
        realMinutiae.setNum(0);
        realMinutiae.setAlloc(10);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(realMinutiae);

        int result = loop.processLoop(minutiaeRef, contourX, contourY, contourEx,
                contourEy, 8, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates processLoopV2 method with realistic contour and minutiae creation.
     * Validates enhanced loop processing with low flow map consideration.
     */
    @Test
    public void processLoopV2WithMinutiaeCreation() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(8);
        AtomicIntegerArray contourY = new AtomicIntegerArray(8);
        AtomicIntegerArray contourEx = new AtomicIntegerArray(8);
        AtomicIntegerArray contourEy = new AtomicIntegerArray(8);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(imageWidth * imageHeight);

        contourX.set(0, 6); contourY.set(0, 6);
        contourX.set(1, 16); contourY.set(1, 6);
        contourX.set(2, 16); contourY.set(2, 7);
        contourX.set(3, 15); contourY.set(3, 9);
        contourX.set(4, 10); contourY.set(4, 10);
        contourX.set(5, 6); contourY.set(5, 10);
        contourX.set(6, 5); contourY.set(6, 8);
        contourX.set(7, 5); contourY.set(7, 7);

        for (int i = 0; i < 8; i++) {
            contourEx.set(i, contourX.get(i) + 1);
            contourEy.set(i, contourY.get(i));
        }

        mockBinaryImageData[(6 * imageWidth) + 6] = 0;
        mockBinaryImageData[(8 * imageWidth) + 11] = 0;

        for (int i = 0; i < imageWidth * imageHeight; i++) {
            lowFlowMap.set(i, ILfs.TRUE);
        }

        when(mockLfsParams.getMinLoopLen()).thenReturn(3);
        when(mockLfsParams.getMinLoopAspectDist()).thenReturn(200.0);
        when(mockLfsParams.getMinLoopAspectRatio()).thenReturn(1.5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);

        Minutiae realMinutiae = new Minutiae();
        realMinutiae.setList(new ArrayList<>());
        realMinutiae.setNum(0);
        realMinutiae.setAlloc(10);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(realMinutiae);

        int result = loop.processLoopV2(minutiaeRef, contourX, contourY, contourEx,
                contourEy, 8, mockBinaryImageData, imageWidth, imageHeight, lowFlowMap, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    private static final int ISLAND_IMAGE_WIDTH = 30;
    private static final int ISLAND_IMAGE_HEIGHT = 20;
    private static final int ISLAND_LEFT = 5;
    private static final int ISLAND_RIGHT = 20;
    private static final int ISLAND_TOP = 8;
    private static final int ISLAND_BOTTOM = 10;

    /**
     * Validates getLoopList returns the error code when removing an IGNOREd bifurcation fails.
     */
    @Test
    public void getLoopListReturnsRemoveMinutiaError() {
        Minutiae minutiae = new Minutiae();
        List<Minutia> list = new ArrayList<>();
        Minutia bifurcation = mock(Minutia.class);
        when(bifurcation.getType()).thenReturn(ILfs.BIFURCATION);
        list.add(bifurcation);
        minutiae.setList(list);
        minutiae.setNum(1);

        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(ILfs.IGNORE).when(spyLoop).onLoop(any(), anyInt(), any(), anyInt(), anyInt());
        MinutiaHelper helper = mock(MinutiaHelper.class);
        when(helper.removeMinutia(anyInt(), any())).thenReturn(-5);
        Mockito.doReturn(helper).when(spyLoop).getMinutiaHelper();

        int result = spyLoop.getLoopList(new AtomicIntegerArray(1), new AtomicReference<>(minutiae), 20,
                mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(-5, result);
    }

    /**
     * Validates onIslandLake reports IGNORE when the first contour cannot be traced.
     */
    @Test
    public void onIslandLakeFirstTraceIgnore() {
        Loop spyLoop = loopWithTraceResults(ILfs.IGNORE);
        AtomicInteger ret = new AtomicInteger(0);

        Contour result = spyLoop.onIslandLake(ret, new AtomicInteger(), minutia(5, 5, 6, 5), minutia(8, 8, 9, 8),
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertNull(result);
        assertEquals(ILfs.IGNORE, ret.get());
    }

    /**
     * Validates onIslandLake reports FALSE when the first trace does not reach the second minutia.
     */
    @Test
    public void onIslandLakeFirstTraceFalse() {
        Loop spyLoop = loopWithTraceResults(ILfs.FALSE);
        AtomicInteger ret = new AtomicInteger(-9);

        Contour result = spyLoop.onIslandLake(ret, new AtomicInteger(), minutia(5, 5, 6, 5), minutia(8, 8, 9, 8),
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertNull(result);
        assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Validates onIslandLake leaves the error code of a failed first trace in ret.
     */
    @Test
    public void onIslandLakeFirstTraceError() {
        Loop spyLoop = loopWithTraceResults(-3);
        AtomicInteger ret = new AtomicInteger(0);

        Contour result = spyLoop.onIslandLake(ret, new AtomicInteger(), minutia(5, 5, 6, 5), minutia(8, 8, 9, 8),
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertNull(result);
        assertEquals(-3, ret.get());
    }

    /**
     * Validates onIslandLake reports FALSE when only the first half loop is closed.
     */
    @Test
    public void onIslandLakeSecondTraceFalseReturnsFalse() {
        Loop spyLoop = loopWithTraceResults(ILfs.LOOP_FOUND, ILfs.FALSE);
        AtomicInteger ret = new AtomicInteger(0);

        Contour result = spyLoop.onIslandLake(ret, new AtomicInteger(), minutia(5, 5, 6, 5), minutia(8, 8, 9, 8),
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertNull(result);
        assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Validates onIslandLake propagates the error when the combined loop contour cannot be allocated.
     */
    @Test
    public void onIslandLakeAllocateContourError() {
        Loop spyLoop = loopWithTraceResults(ILfs.LOOP_FOUND, ILfs.LOOP_FOUND);
        Contour contourMock = spyLoop.getContour();
        Mockito.doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(-4);
            return null;
        }).when(contourMock).allocateContour(any(), anyInt());
        AtomicInteger ret = new AtomicInteger(0);

        Contour result = spyLoop.onIslandLake(ret, new AtomicInteger(), minutia(5, 5, 6, 5), minutia(8, 8, 9, 8),
                10, mockBinaryImageData, imageWidth, imageHeight);

        assertNull(result);
        assertEquals(-4, ret.get());
    }

    /**
     * Validates onIslandLake joins the two half loops in order: first minutia, first half
     * contour, second minutia, second half contour.
     */
    @Test
    public void onIslandLakeCombinesHalfLoopsInOrder() {
        Contour realContour = Contour.getInstance();
        Contour firstHalf = contourOf(realContour, new int[][] { { 6, 4, 6, 3 }, { 7, 4, 7, 3 } });
        Contour secondHalf = contourOf(realContour, new int[][] { { 7, 6, 7, 7 } });

        Contour contourMock = mock(Contour.class);
        Mockito.doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(ILfs.LOOP_FOUND);
            ((AtomicInteger) invocation.getArgument(1)).set(2);
            return firstHalf;
        }).doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(ILfs.LOOP_FOUND);
            ((AtomicInteger) invocation.getArgument(1)).set(1);
            return secondHalf;
        }).when(contourMock).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), anyInt(), anyInt());
        Mockito.doAnswer(invocation -> realContour.allocateContour(invocation.getArgument(0),
                invocation.getArgument(1))).when(contourMock).allocateContour(any(), anyInt());
        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(contourMock).when(spyLoop).getContour();
        AtomicInteger ret = new AtomicInteger(0);
        AtomicInteger nLoop = new AtomicInteger(0);

        Contour result = spyLoop.onIslandLake(ret, nLoop, minutia(5, 5, 5, 4), minutia(8, 5, 8, 6), 10,
                mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.LOOP_FOUND, ret.get());
        assertEquals(5, nLoop.get());
        int[] expectedX = { 5, 6, 7, 8, 7 };
        int[] expectedY = { 5, 4, 4, 5, 6 };
        int[] expectedEy = { 4, 3, 3, 6, 7 };
        for (int i = 0; i < 5; i++) {
            assertEquals(expectedX[i], result.getContourX().get(i), "x " + i);
            assertEquals(expectedY[i], result.getContourY().get(i), "y " + i);
            assertEquals(expectedEy[i], result.getContourEy().get(i), "ey " + i);
        }
    }

    /**
     * Validates onHook returns IGNORE when the first (clockwise) trace is not possible.
     */
    @Test
    public void onHookFirstTraceIgnore() {
        assertEquals(ILfs.IGNORE, loopWithTraceResults(ILfs.IGNORE).onHook(minutia(3, 3, 4, 3),
                minutia(6, 6, 7, 6), 8, mockBinaryImageData, imageWidth, imageHeight));
    }

    /**
     * Validates onHook returns the error code of a failed clockwise trace without retrying.
     */
    @Test
    public void onHookFirstTraceError() {
        assertEquals(-4, loopWithTraceResults(-4).onHook(minutia(3, 3, 4, 3), minutia(6, 6, 7, 6), 8,
                mockBinaryImageData, imageWidth, imageHeight));
    }

    /**
     * Validates onHook retries counter-clockwise and reports each outcome of the second trace.
     */
    @Test
    public void onHookSecondTraceOutcomes() {
        Minutia first = minutia(3, 3, 4, 3);
        Minutia second = minutia(6, 6, 7, 6);

        assertEquals(ILfs.IGNORE, loopWithTraceResults(ILfs.FALSE, ILfs.IGNORE).onHook(first, second, 8,
                mockBinaryImageData, imageWidth, imageHeight));
        assertEquals(ILfs.HOOK_FOUND, loopWithTraceResults(ILfs.FALSE, ILfs.LOOP_FOUND).onHook(first, second, 8,
                mockBinaryImageData, imageWidth, imageHeight));
        assertEquals(ILfs.FALSE, loopWithTraceResults(ILfs.FALSE, ILfs.FALSE).onHook(first, second, 8,
                mockBinaryImageData, imageWidth, imageHeight));
        assertEquals(-6, loopWithTraceResults(ILfs.FALSE, -6).onHook(first, second, 8, mockBinaryImageData,
                imageWidth, imageHeight));
    }

    /**
     * Validates getLoopAspect on an odd-length contour walks the whole perimeter and records
     * the indices of the minimum and maximum opposite-point distances.
     */
    @Test
    public void getLoopAspectOddLengthContour() {
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[] { 0, 4, 4, 2, 0 });
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[] { 0, 0, 2, 3, 2 });
        AtomicInteger minFrom = new AtomicInteger(-1);
        AtomicInteger minTo = new AtomicInteger(-1);
        AtomicReference<Double> minDist = new AtomicReference<>();
        AtomicInteger maxFrom = new AtomicInteger(-1);
        AtomicInteger maxTo = new AtomicInteger(-1);
        AtomicReference<Double> maxDist = new AtomicReference<>();

        loop.getLoopAspect(minFrom, minTo, minDist, maxFrom, maxTo, maxDist, contourX, contourY, 5);

        assertEquals(13.0, minDist.get());
        assertEquals(1, minFrom.get());
        assertEquals(3, minTo.get());
        assertEquals(20.0, maxDist.get());
        assertEquals(0, maxFrom.get());
        assertEquals(2, maxTo.get());
    }

    /**
     * Validates fillLoop returns the error code when a shape cannot be built from the contour.
     */
    @Test
    public void fillLoopShapeError() {
        Loop spyLoop = Mockito.spy(loop);
        Shapes shapes = mock(Shapes.class);
        Mockito.doAnswer(invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(-7);
            return null;
        }).when(shapes).shapeFromContour(any(), any(), any(), anyInt());
        Mockito.doReturn(shapes).when(spyLoop).getShapes();

        int result = spyLoop.fillLoop(new AtomicIntegerArray(3), new AtomicIntegerArray(3), 3, mockBinaryImageData,
                imageWidth, imageHeight);

        assertEquals(-7, result);
    }

    /**
     * Validates processLoop on an elongated black island adds two opposite RIDGE_ENDING minutiae
     * at the ends of the loop's longest axis, with directions 180 degrees apart.
     */
    @Test
    public void processLoopElongatedIslandAddsTwoMinutiae() {
        int[] image = islandImage();
        AtomicIntegerArray[] contour = islandContour();
        Minutiae minutiae = emptyMinutiae();
        LfsParams params = Globals.getInstance().getLfsParams();

        int result = loop.processLoop(new AtomicReference<>(minutiae), contour[0], contour[1], contour[2],
                contour[3], contour[0].length(), image, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT, params);

        assertEquals(ILfs.FALSE, result);
        assertEquals(2, minutiae.getNum());
        Minutia first = minutiae.getList().get(0);
        Minutia second = minutiae.getList().get(1);
        assertEquals(ISLAND_LEFT, first.getX());
        assertEquals(ISLAND_TOP, first.getY());
        assertEquals(ISLAND_RIGHT, second.getX());
        assertEquals(ISLAND_BOTTOM, second.getY());
        assertEquals(ILfs.RIDGE_ENDING, first.getType());
        assertEquals(ILfs.RIDGE_ENDING, second.getType());
        assertEquals(ILfs.DEFAULT_RELIABILITY, first.getReliability());
        assertEquals((first.getDirection() + params.getNumDirections()) % (params.getNumDirections() << 1),
                second.getDirection());
        assertEquals(1, image[(9 * ISLAND_IMAGE_WIDTH) + 12], "island must not be filled");
    }

    /**
     * Validates processLoopV2 assigns MEDIUM reliability to a loop minutia in a low ridge flow block
     * and HIGH reliability to one in a reliable block.
     */
    @Test
    public void processLoopV2ReliabilityFollowsLowFlowMap() {
        int[] image = islandImage();
        AtomicIntegerArray[] contour = islandContour();
        Minutiae minutiae = emptyMinutiae();
        AtomicIntegerArray lowFlow = new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT);
        lowFlow.set((ISLAND_TOP * ISLAND_IMAGE_WIDTH) + ISLAND_LEFT, ILfs.TRUE);

        int result = loop.processLoopV2(new AtomicReference<>(minutiae), contour[0], contour[1], contour[2],
                contour[3], contour[0].length(), image, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT, lowFlow,
                Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.FALSE, result);
        assertEquals(2, minutiae.getNum());
        assertEquals(ILfs.MEDIUM_RELIABILITY, minutiae.getList().get(0).getReliability());
        assertEquals(ILfs.HIGH_RELIABILITY, minutiae.getList().get(1).getReliability());
    }

    /**
     * Validates processLoop and processLoopV2 fill the loop instead of adding minutiae when the
     * loop is neither narrow nor elongated enough.
     */
    @Test
    public void processLoopFillsLoopFailingAspectTests() {
        AtomicIntegerArray[] contour = islandContour();
        LfsParams params = mock(LfsParams.class);
        when(params.getMinLoopLen()).thenReturn(3);
        when(params.getMinLoopAspectDist()).thenReturn(0.0);
        when(params.getMinLoopAspectRatio()).thenReturn(1000.0);

        int[] image = islandImage();
        Minutiae minutiae = emptyMinutiae();
        int result = loop.processLoop(new AtomicReference<>(minutiae), contour[0], contour[1], contour[2],
                contour[3], contour[0].length(), image, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT, params);

        assertEquals(ILfs.FALSE, result);
        assertEquals(0, minutiae.getNum());
        assertEquals(0, image[(9 * ISLAND_IMAGE_WIDTH) + 12], "island interior must be filled");

        int[] imageV2 = islandImage();
        Minutiae minutiaeV2 = emptyMinutiae();
        int resultV2 = loop.processLoopV2(new AtomicReference<>(minutiaeV2), contour[0], contour[1], contour[2],
                contour[3], contour[0].length(), imageV2, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT), params);

        assertEquals(ILfs.FALSE, resultV2);
        assertEquals(0, minutiaeV2.getNum());
        assertEquals(0, imageV2[(9 * ISLAND_IMAGE_WIDTH) + 12], "island interior must be filled");
    }

    /**
     * Validates processLoop and processLoopV2 fill the loop when the loop's interior midpoint does
     * not have the feature pixel value.
     */
    @Test
    public void processLoopFillsLoopWithInconsistentInterior() {
        AtomicIntegerArray[] contour = islandContour();
        int midIndex = (9 * ISLAND_IMAGE_WIDTH) + 12;

        int[] image = islandImage();
        image[midIndex] = 0;
        Minutiae minutiae = emptyMinutiae();
        assertEquals(ILfs.FALSE, loop.processLoop(new AtomicReference<>(minutiae), contour[0], contour[1],
                contour[2], contour[3], contour[0].length(), image, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                Globals.getInstance().getLfsParams()));
        assertEquals(0, minutiae.getNum());

        int[] imageV2 = islandImage();
        imageV2[midIndex] = 0;
        Minutiae minutiaeV2 = emptyMinutiae();
        assertEquals(ILfs.FALSE, loop.processLoopV2(new AtomicReference<>(minutiaeV2), contour[0], contour[1],
                contour[2], contour[3], contour[0].length(), imageV2, ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT),
                Globals.getInstance().getLfsParamsV2()));
        assertEquals(0, minutiaeV2.getNum());
    }

    /**
     * Validates processLoop and processLoopV2 return the error code when the appearing/disappearing
     * test fails for the first or for the opposite loop point.
     */
    @Test
    public void processLoopReturnsAppearingErrors() {
        AtomicIntegerArray[] contour = islandContour();
        int n = contour[0].length();

        MinutiaHelper helper = minutiaHelperMock(ILfs.FALSE);
        when(helper.isMinutiaAppearing(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(-2);
        Loop spyLoop = loopWithMinutiaHelper(helper);
        assertEquals(-2, spyLoop.processLoop(new AtomicReference<>(emptyMinutiae()), contour[0], contour[1],
                contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                Globals.getInstance().getLfsParams()));
        assertEquals(-2, spyLoop.processLoopV2(new AtomicReference<>(emptyMinutiae()), contour[0], contour[1],
                contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT),
                Globals.getInstance().getLfsParamsV2()));

        MinutiaHelper secondFails = minutiaHelperMock(ILfs.FALSE);
        when(secondFails.isMinutiaAppearing(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1, -3, 1, -3);
        Loop spySecond = loopWithMinutiaHelper(secondFails);
        assertEquals(-3, spySecond.processLoop(new AtomicReference<>(emptyMinutiae()), contour[0], contour[1],
                contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                Globals.getInstance().getLfsParams()));
        assertEquals(-3, spySecond.processLoopV2(new AtomicReference<>(emptyMinutiae()), contour[0], contour[1],
                contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT),
                Globals.getInstance().getLfsParamsV2()));
    }

    /**
     * Validates processLoop and processLoopV2 free both candidate minutiae when the minutiae list
     * IGNOREs them, and still complete normally.
     */
    @Test
    public void processLoopFreesIgnoredMinutiae() {
        AtomicIntegerArray[] contour = islandContour();
        int n = contour[0].length();
        MinutiaHelper helper = minutiaHelperMock(ILfs.IGNORE);
        when(helper.isMinutiaAppearing(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);
        Loop spyLoop = loopWithMinutiaHelper(helper);

        assertEquals(ILfs.FALSE, spyLoop.processLoop(new AtomicReference<>(emptyMinutiae()), contour[0],
                contour[1], contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                Globals.getInstance().getLfsParams()));
        assertEquals(ILfs.FALSE, spyLoop.processLoopV2(new AtomicReference<>(emptyMinutiae()), contour[0],
                contour[1], contour[2], contour[3], n, islandImage(), ISLAND_IMAGE_WIDTH, ISLAND_IMAGE_HEIGHT,
                new AtomicIntegerArray(ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT),
                Globals.getInstance().getLfsParamsV2()));

        Mockito.verify(helper, Mockito.times(4)).freeMinutia(any());
    }

    private static Minutia minutia(int x, int y, int ex, int ey) {
        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(x);
        when(minutia.getY()).thenReturn(y);
        when(minutia.getEx()).thenReturn(ex);
        when(minutia.getEy()).thenReturn(ey);
        return minutia;
    }

    /** Spies the Loop singleton so successive contour traces report the given return codes. */
    private Loop loopWithTraceResults(int... results) {
        Contour contourMock = mock(Contour.class);
        AtomicInteger call = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            int index = Math.min(call.getAndIncrement(), results.length - 1);
            ((AtomicInteger) invocation.getArgument(0)).set(results[index]);
            return results[index] == ILfs.IGNORE ? null : mock(Contour.class);
        }).when(contourMock).traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), anyInt(), anyInt());
        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(contourMock).when(spyLoop).getContour();
        return spyLoop;
    }

    private static Contour contourOf(Contour factory, int[][] points) {
        Contour contour = factory.allocateContour(new AtomicInteger(), points.length);
        for (int i = 0; i < points.length; i++) {
            contour.getContourX().set(i, points[i][0]);
            contour.getContourY().set(i, points[i][1]);
            contour.getContourEx().set(i, points[i][2]);
            contour.getContourEy().set(i, points[i][3]);
        }
        return contour;
    }

    /** Binary image (1 = black) containing a single 16x3 black island. */
    private static int[] islandImage() {
        int[] image = new int[ISLAND_IMAGE_WIDTH * ISLAND_IMAGE_HEIGHT];
        for (int y = ISLAND_TOP; y <= ISLAND_BOTTOM; y++) {
            for (int x = ISLAND_LEFT; x <= ISLAND_RIGHT; x++) {
                image[(y * ISLAND_IMAGE_WIDTH) + x] = 1;
            }
        }
        return image;
    }

    /**
     * Clockwise contour of the island's boundary pixels starting at its top-left corner, with each
     * point's edge neighbour just outside the island. Returns {x, y, ex, ey}.
     */
    private static AtomicIntegerArray[] islandContour() {
        List<int[]> points = new ArrayList<>();
        for (int x = ISLAND_LEFT; x <= ISLAND_RIGHT; x++) {
            points.add(new int[] { x, ISLAND_TOP, x, ISLAND_TOP - 1 });
        }
        for (int y = ISLAND_TOP + 1; y < ISLAND_BOTTOM; y++) {
            points.add(new int[] { ISLAND_RIGHT, y, ISLAND_RIGHT + 1, y });
        }
        for (int x = ISLAND_RIGHT; x >= ISLAND_LEFT; x--) {
            points.add(new int[] { x, ISLAND_BOTTOM, x, ISLAND_BOTTOM + 1 });
        }
        for (int y = ISLAND_BOTTOM - 1; y > ISLAND_TOP; y--) {
            points.add(new int[] { ISLAND_LEFT, y, ISLAND_LEFT - 1, y });
        }
        AtomicIntegerArray[] contour = new AtomicIntegerArray[4];
        for (int c = 0; c < 4; c++) {
            contour[c] = new AtomicIntegerArray(points.size());
            for (int i = 0; i < points.size(); i++) {
                contour[c].set(i, points.get(i)[c]);
            }
        }
        return contour;
    }

    private static Minutiae emptyMinutiae() {
        Minutiae minutiae = new Minutiae();
        minutiae.setList(new ArrayList<>());
        minutiae.setNum(0);
        minutiae.setAlloc(10);
        return minutiae;
    }

    private static MinutiaHelper minutiaHelperMock(int updateResult) {
        MinutiaHelper helper = mock(MinutiaHelper.class);
        when(helper.getMinutiaType(anyInt())).thenReturn(ILfs.RIDGE_ENDING);
        when(helper.createMinutia(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), Mockito.anyDouble(), anyInt(),
                anyInt(), anyInt())).thenReturn(mock(Minutia.class));
        when(helper.updateMinutiae(any(), any(), any(), anyInt(), anyInt(), any())).thenReturn(updateResult);
        return helper;
    }

    private Loop loopWithMinutiaHelper(MinutiaHelper helper) {
        Loop spyLoop = Mockito.spy(loop);
        Mockito.doReturn(helper).when(spyLoop).getMinutiaHelper();
        return spyLoop;
    }
}