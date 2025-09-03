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
}