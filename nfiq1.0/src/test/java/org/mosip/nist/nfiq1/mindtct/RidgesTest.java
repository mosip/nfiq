package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link Ridges} providing comprehensive validation of ridge counting
 * and neighbor analysis functionality.
 *
 * <p>This class validates the functionality of ridge counting algorithms used in
 * NIST's Mindtct fingerprint analysis system for minutiae neighbor analysis,
 * ridge traversal, and crossing validation.</p>
 */
public class RidgesTest {

    private Ridges ridges;
    private LfsParams mockLfsParams;
    private AtomicReference<Minutiae> mockMinutiae;
    private int[] mockBinaryImageData;
    private int imageWidth;
    private int imageHeight;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes Ridges singleton instance and test data structures.
     */
    @BeforeEach
    public void setUp() {
        ridges = Ridges.getInstance();
        imageWidth = 100;
        imageHeight = 100;
        mockBinaryImageData = new int[imageWidth * imageHeight];

        mockLfsParams = mock(LfsParams.class);
        when(mockLfsParams.getMaxNbrs()).thenReturn(5);
        when(mockLfsParams.getMaxRidgeSteps()).thenReturn(10);

        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia minutia1 = mock(Minutia.class);
        when(minutia1.getX()).thenReturn(10);
        when(minutia1.getY()).thenReturn(10);
        minutiaList.add(minutia1);

        Minutia minutia2 = mock(Minutia.class);
        when(minutia2.getX()).thenReturn(20);
        when(minutia2.getY()).thenReturn(20);
        minutiaList.add(minutia2);

        when(minutiae.getList()).thenReturn(minutiaList);
        mockMinutiae = new AtomicReference<>(minutiae);
    }

    /**
     * Cleans up resources after each test execution.
     * Releases references to test objects and data structures.
     */
    @AfterEach
    public void tearDown() {
        ridges = null;
        mockLfsParams = null;
        mockMinutiae = null;
        mockBinaryImageData = null;
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    public void verifySingletonInstance() {
        Ridges firstInstance = Ridges.getInstance();
        Ridges secondInstance = Ridges.getInstance();

        assertEquals(firstInstance, secondInstance);
        assertNotNull(firstInstance);
    }

    /**
     * Validates that all dependency getter methods return non-null instances.
     * Tests proper initialization of required dependencies.
     */
    @Test
    public void validateDependencyGetters() {
        assertNotNull(ridges.getDefs());
        assertNotNull(ridges.getImageUtil());
        assertNotNull(ridges.getGlobals());
        assertNotNull(ridges.getLfsUtil());
        assertNotNull(ridges.getFree());
        assertNotNull(ridges.getInit());
        assertNotNull(ridges.getBinarization());
        assertNotNull(ridges.getMinutiaHelper());
        assertNotNull(ridges.getSort());
        assertNotNull(ridges.getDetect());
        assertNotNull(ridges.getRemoveMinutia());
        assertNotNull(ridges.getLine());
        assertNotNull(ridges.getContour());
        assertNotNull(ridges.getMap());
        assertNotNull(ridges.getLoop());
    }

    /**
     * Validates ridge counting functionality with empty minutiae list.
     * Tests handling when no minutiae are available for ridge counting.
     */
    @Test
    public void validateRidgeCountingWithEmptyMinutiae() {
        Minutiae emptyMinutiae = mock(Minutiae.class);
        when(emptyMinutiae.getNum()).thenReturn(0);
        AtomicReference<Minutiae> emptyMinutiaeRef = new AtomicReference<>(emptyMinutiae);

        int result = ridges.countMinutiaeRidges(emptyMinutiaeRef, mockBinaryImageData,
                imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates countMinutiaRidges method when sorting encounters error.
     * Tests error handling during minutiae sorting process.
     */
    @Test
    public void countMinutiaRidgesWithSortError() {
        try (MockedStatic<MinutiaHelper> mockedMinutiaHelper = Mockito.mockStatic(MinutiaHelper.class)) {
            MinutiaHelper mockMinutiaHelper = mock(MinutiaHelper.class);
            mockedMinutiaHelper.when(MinutiaHelper::getInstance).thenReturn(mockMinutiaHelper);

            when(mockMinutiaHelper.sortMinutiaeLeftToRightAndThenTopToBottom(any(), anyInt(), anyInt()))
                    .thenReturn(ILfs.ERROR_CODE_301);

            int result = ridges.countMinutiaeRidges(mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.ERROR_CODE_301, result);
        }
    }

    /**
     * Validates countMinutiaRidges method when removing redundant minutiae fails.
     * Tests error propagation during redundant minutiae removal process.
     */
    @Test
    public void countMinutiaRidgesWithRemoveRedundantError() {
        try (MockedStatic<MinutiaHelper> mockedMinutiaHelper = Mockito.mockStatic(MinutiaHelper.class)) {
            MinutiaHelper mockMinutiaHelper = mock(MinutiaHelper.class);
            mockedMinutiaHelper.when(MinutiaHelper::getInstance).thenReturn(mockMinutiaHelper);

            when(mockMinutiaHelper.sortMinutiaeLeftToRightAndThenTopToBottom(any(), anyInt(), anyInt()))
                    .thenReturn(ILfs.FALSE);
            when(mockMinutiaHelper.removeRedundantMinutiae(any()))
                    .thenReturn(ILfs.ERROR_CODE_301);

            int result = ridges.countMinutiaeRidges(mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.ERROR_CODE_301, result);
        }
    }

    /**
     * Validates countMinutiaRidges method when finding neighbors encounters error.
     * Tests error handling during neighbor discovery process.
     */
    @Test
    public void countMinutiaRidgesWithFindNeighborsError() {
        Ridges spyRidges = Mockito.spy(ridges);
        Mockito.doReturn(ILfs.ERROR_CODE_301).when(spyRidges)
                .findNeighbors(any(), any(), anyInt(), anyInt(), any());

        int result = spyRidges.countMinutiaRidges(0, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.ERROR_CODE_301, result);
    }

    /**
     * Validates updateNbrDists method with illegal position parameter.
     * Tests error handling when position index is out of valid range.
     */
    @Test
    public void updateNbrDistsWithIllegalPosition() {
        try (MockedStatic<LfsUtil> mockedLfsUtil = Mockito.mockStatic(LfsUtil.class)) {
            LfsUtil mockLfsUtil = mock(LfsUtil.class);
            mockedLfsUtil.when(LfsUtil::getInstance).thenReturn(mockLfsUtil);

            when(mockLfsUtil.squaredDistance(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(100.0);
            when(mockLfsUtil.findIncrementalPositionInDoubleArray(anyDouble(), any(), anyInt())).thenReturn(10);

            Minutiae minutiae = mock(Minutiae.class);
            List<Minutia> minutiaList = new ArrayList<>();

            Minutia minutia1 = mock(Minutia.class);
            when(minutia1.getX()).thenReturn(10);
            when(minutia1.getY()).thenReturn(10);
            minutiaList.add(minutia1);

            Minutia minutia2 = mock(Minutia.class);
            when(minutia2.getX()).thenReturn(20);
            when(minutia2.getY()).thenReturn(20);
            minutiaList.add(minutia2);

            when(minutiae.getList()).thenReturn(minutiaList);
            AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

            AtomicIntegerArray nbrList = new AtomicIntegerArray(5);
            AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(5);
            AtomicInteger noOfNbrs = new AtomicInteger(0);

            int result = ridges.updateNbrDists(nbrList, nbrSqrDists, noOfNbrs, 5, 0, 1, minutiaeRef);

            assertEquals(ILfs.ERROR_CODE_470, result);
        }
    }

    /**
     * Validates ridgeCount method when line point generation fails.
     * Tests error handling during trajectory line point creation.
     */
    @Test
    public void ridgeCountWithLinePointsError() {
        try (MockedStatic<Line> mockedLine = Mockito.mockStatic(Line.class)) {
            Line mockLine = mock(Line.class);
            mockedLine.when(Line::getInstance).thenReturn(mockLine);

            Minutiae minutiae = mock(Minutiae.class);
            List<Minutia> minutiaList = new ArrayList<>();

            Minutia minutia1 = mock(Minutia.class);
            when(minutia1.getX()).thenReturn(10);
            when(minutia1.getY()).thenReturn(10);
            minutiaList.add(minutia1);

            Minutia minutia2 = mock(Minutia.class);
            when(minutia2.getX()).thenReturn(20);
            when(minutia2.getY()).thenReturn(20);
            minutiaList.add(minutia2);

            when(minutiae.getList()).thenReturn(minutiaList);
            AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

            when(mockLine.linePoints(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(ILfs.ERROR_CODE_301);

            int result = ridges.ridgeCount(0, 1, minutiaeRef, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.ERROR_CODE_301, result);
        }
    }

    /**
     * Validates validateRidgeCrossing method when contour tracing fails.
     * Tests error handling during ridge crossing validation with contour analysis.
     */
    @Test
    public void validateRidgeCrossingWithTraceContourError() {
        try (MockedStatic<Contour> mockedContour = Mockito.mockStatic(Contour.class)) {
            Contour mockContour = mock(Contour.class);
            mockedContour.when(Contour::getInstance).thenReturn(mockContour);

            Mockito.doNothing().when(mockContour)
                    .fixEdgePixelPair(any(), any(), any(), any(), any(), anyInt(), anyInt());
            when(mockContour.traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        AtomicInteger ret = invocation.getArgument(0);
                        ret.set(ILfs.ERROR_CODE_301);
                        return null;
                    });

            int[] xlist = {10, 11, 12, 13, 14};
            int[] ylist = {10, 10, 10, 10, 10};

            int result = ridges.validateRidgeCrossing(1, 3, xlist, ylist, 5, mockBinaryImageData, imageWidth, imageHeight, 10);

            assertEquals(ILfs.ERROR_CODE_301, result);
        }
    }

    /**
     * Validates validateRidgeCrossing method with successful crossing detection.
     * Tests ridge crossing validation when a valid crossing is found.
     */
    @Test
    public void validateRidgeCrossingWithValidCrossing() {
        try (MockedStatic<Contour> mockedContour = Mockito.mockStatic(Contour.class)) {
            Contour mockContour = mock(Contour.class);
            mockedContour.when(Contour::getInstance).thenReturn(mockContour);

            Mockito.doNothing().when(mockContour)
                    .fixEdgePixelPair(any(), any(), any(), any(), any(), anyInt(), anyInt());
            when(mockContour.traceContour(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        AtomicInteger ret = invocation.getArgument(0);
                        ret.set(ILfs.FALSE);
                        return mock(Contour.class);
                    });
            Mockito.doNothing().when(mockContour).freeContour(any());

            int[] xlist = {10, 11, 12, 13, 14};
            int[] ylist = {10, 10, 10, 10, 10};

            int result = ridges.validateRidgeCrossing(1, 3, xlist, ylist, 5, mockBinaryImageData, imageWidth, imageHeight, 10);

            assertEquals(ILfs.TRUE, result);
        }
    }

    /**
     * Validates findNeighbors method when no neighbors are found.
     * Tests neighbor discovery when only one minutia exists.
     */
    @Test
    public void validateFindNeighborsWithNoNeighbors() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(5);
        AtomicInteger noOfNbrs = new AtomicInteger(0);

        Minutiae minutiae = mock(Minutiae.class);
        when(minutiae.getNum()).thenReturn(1);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.findNeighbors(nbrList, noOfNbrs, 5, 0, minutiaeRef);

        assertEquals(ILfs.FALSE, result);
        assertEquals(0, noOfNbrs.get());
    }

    /**
     * Validates updateNeighborDistances method with valid minutiae pair.
     * Tests distance calculation and neighbor list updating functionality.
     */
    @Test
    public void validateUpdateNeighborDistances() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(3);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(3);
        AtomicInteger noOfNbrs = new AtomicInteger(0);

        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia firstMinutia = mock(Minutia.class);
        when(firstMinutia.getX()).thenReturn(10);
        when(firstMinutia.getY()).thenReturn(10);
        minutiaList.add(firstMinutia);

        Minutia secondMinutia = mock(Minutia.class);
        when(secondMinutia.getX()).thenReturn(20);
        when(secondMinutia.getY()).thenReturn(20);
        minutiaList.add(secondMinutia);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.updateNbrDists(nbrList, nbrSqrDists, noOfNbrs, 3, 0, 1, minutiaeRef);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates neighbor insertion functionality with middle position insertion.
     * Tests insertion of neighbor at intermediate position in sorted list.
     */
    @Test
    public void validateNeighborInsertion() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(5);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(5);
        AtomicInteger noOfNbrs = new AtomicInteger(2);

        nbrList.set(0, 10);
        nbrList.set(1, 20);
        nbrSqrDists.set(0, 100.0);
        nbrSqrDists.set(1, 400.0);

        int result = ridges.insertNeighbor(1, 15, 225.0, nbrList, nbrSqrDists, noOfNbrs, 5);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, noOfNbrs.get());
        assertEquals(15, nbrList.get(1));
    }

    /**
     * Validates neighbor insertion when capacity is reached.
     * Tests insertion behavior when neighbor list is at maximum capacity.
     */
    @Test
    public void validateNeighborInsertionAtCapacity() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(3);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(3);
        AtomicInteger noOfNbrs = new AtomicInteger(3);

        for (int i = 0; i < 3; i++) {
            nbrList.set(i, i * 10);
            nbrSqrDists.set(i, (double)(i * 100));
        }

        int result = ridges.insertNeighbor(1, 15, 125.0, nbrList, nbrSqrDists, noOfNbrs, 3);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, noOfNbrs.get());
    }

    /**
     * Validates neighbor insertion error handling with invalid position.
     * Tests error handling when insertion position is out of bounds.
     */
    @Test
    public void validateNeighborInsertionErrorHandling() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(3);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(3);
        AtomicInteger noOfNbrs = new AtomicInteger(2);

        int result = ridges.insertNeighbor(5, 15, 225.0, nbrList, nbrSqrDists, noOfNbrs, 3);

        assertEquals(ILfs.ERROR_CODE_480, result);
    }

    /**
     * Validates ridge counting with identical minutiae coordinates.
     * Tests ridge counting when two minutiae have the same position.
     */
    @Test
    public void validateRidgeCountWithIdenticalCoordinates() {
        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia minutia1 = mock(Minutia.class);
        when(minutia1.getX()).thenReturn(50);
        when(minutia1.getY()).thenReturn(50);
        minutiaList.add(minutia1);

        Minutia minutia2 = mock(Minutia.class);
        when(minutia2.getX()).thenReturn(50);
        when(minutia2.getY()).thenReturn(50);
        minutiaList.add(minutia2);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.ridgeCount(0, 1, minutiaeRef, mockBinaryImageData,
                imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates transition finding with successful detection.
     * Tests pixel value transition detection along trajectory line.
     */
    @Test
    public void validateTransitionFindingSuccess() {
        int[] xlist = {0, 1, 2, 3, 4};
        int[] ylist = {0, 0, 0, 0, 0};

        for (int i = 0; i < imageWidth * imageHeight; i++) {
            mockBinaryImageData[i] = 0;
        }

        mockBinaryImageData[(0 * imageWidth) + 0] = 0;
        mockBinaryImageData[(0 * imageWidth) + 1] = 0;
        mockBinaryImageData[(0 * imageWidth) + 2] = 1;
        mockBinaryImageData[(0 * imageWidth) + 3] = 1;
        mockBinaryImageData[(0 * imageWidth) + 4] = 1;

        AtomicInteger startPixel = new AtomicInteger(0);
        int result = ridges.findTransition(startPixel, 0, 1, xlist, ylist, 5,
                mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.TRUE, result);
        assertEquals(2, startPixel.get());
    }

    /**
     * Validates transition finding when no transition exists.
     * Tests behavior when all pixels have the same value along trajectory.
     */
    @Test
    public void validateTransitionFindingFailure() {
        int[] xlist = {0, 1, 2, 3, 4};
        int[] ylist = {0, 0, 0, 0, 0};

        for (int i = 0; i < imageWidth * imageHeight; i++) {
            mockBinaryImageData[i] = 1;
        }

        AtomicInteger startPixel = new AtomicInteger(0);
        int result = ridges.findTransition(startPixel, 1, 0, xlist, ylist, 5,
                mockBinaryImageData, imageWidth, imageHeight);

        assertEquals(ILfs.FALSE, result);
        assertEquals(5, startPixel.get());
    }

    /**
     * Validates ridge crossing validation with alternating pixel pattern.
     * Tests ridge crossing detection with predefined pixel value pattern.
     */
    @Test
    public void validateRidgeCrossingValidation() {
        int[] xlist = {10, 11, 12, 13, 14, 15};
        int[] ylist = {10, 10, 10, 10, 10, 10};

        for (int i = 10; i < 16; i++) {
            int index = (10 * imageWidth) + i;
            if (index < mockBinaryImageData.length) {
                mockBinaryImageData[index] = i % 2;
            }
        }

        int result = ridges.validateRidgeCrossing(2, 4, xlist, ylist, 6,
                mockBinaryImageData, imageWidth, imageHeight, 10);

        assertTrue(result >= ILfs.FALSE);
    }

    /**
     * Validates neighbor sorting functionality with angular ordering.
     * Tests sorting of neighbors based on their angular position relative to primary minutia.
     */
    @Test
    public void validateNeighborSorting() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(3);
        nbrList.set(0, 1);
        nbrList.set(1, 2);
        nbrList.set(2, 3);

        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia primary = mock(Minutia.class);
        when(primary.getX()).thenReturn(50);
        when(primary.getY()).thenReturn(50);
        minutiaList.add(primary);

        for (int i = 1; i <= 3; i++) {
            Minutia neighbor = mock(Minutia.class);
            when(neighbor.getX()).thenReturn(50 + (i * 10));
            when(neighbor.getY()).thenReturn(50 + (i * 5));
            minutiaList.add(neighbor);
        }

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.sortNeighbors(nbrList, 3, 0, minutiaeRef);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates updateNbrDists error handling with extreme coordinates.
     * Tests distance calculation with coordinates at maximum integer values.
     */
    @Test
    public void validateUpdateNbrDistsErrorHandling() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(2);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(2);
        AtomicInteger noOfNbrs = new AtomicInteger(0);

        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia minutia1 = mock(Minutia.class);
        when(minutia1.getX()).thenReturn(0);
        when(minutia1.getY()).thenReturn(0);
        minutiaList.add(minutia1);

        Minutia minutia2 = mock(Minutia.class);
        when(minutia2.getX()).thenReturn(Integer.MAX_VALUE);
        when(minutia2.getY()).thenReturn(Integer.MAX_VALUE);
        minutiaList.add(minutia2);

        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.updateNbrDists(nbrList, nbrSqrDists, noOfNbrs, 2, 0, 1, minutiaeRef);

        assertTrue(result >= ILfs.FALSE);
    }

    /**
     * Validates neighbor insertion overflow error handling.
     * Tests error detection when neighbor count exceeds capacity limits.
     */
    @Test
    public void validateNeighborInsertionOverflowError() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(3);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(3);
        AtomicInteger noOfNbrs = new AtomicInteger(5);

        int result = ridges.insertNeighbor(1, 15, 225.0, nbrList, nbrSqrDists, noOfNbrs, 3);

        assertEquals(ILfs.ERROR_CODE_481, result);
    }

    /**
     * Validates countMinutiaRidges method with single minutia.
     * Tests ridge counting when only one minutia is available.
     */
    @Test
    public void countMinutiaRidgesWithSingleMinutia() {
        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia minutia = mock(Minutia.class);
        when(minutia.getX()).thenReturn(50);
        when(minutia.getY()).thenReturn(50);
        minutiaList.add(minutia);

        when(minutiae.getNum()).thenReturn(1);
        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.countMinutiaeRidges(minutiaeRef, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates findNeighbors method with multiple potential neighbors.
     * Tests neighbor discovery when multiple minutiae are available as neighbors.
     */
    @Test
    public void findNeighborsWithMultipleNeighbors() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(5);
        AtomicInteger noOfNbrs = new AtomicInteger(0);

        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> minutiaList = new ArrayList<>();

        Minutia primary = mock(Minutia.class);
        when(primary.getX()).thenReturn(50);
        when(primary.getY()).thenReturn(50);
        minutiaList.add(primary);

        for (int i = 1; i <= 4; i++) {
            Minutia neighbor = mock(Minutia.class);
            when(neighbor.getX()).thenReturn(50 + i * 5);
            when(neighbor.getY()).thenReturn(50 + i * 5);
            minutiaList.add(neighbor);
        }

        when(minutiae.getNum()).thenReturn(5);
        when(minutiae.getList()).thenReturn(minutiaList);
        AtomicReference<Minutiae> minutiaeRef = new AtomicReference<>(minutiae);

        int result = ridges.findNeighbors(nbrList, noOfNbrs, 5, 0, minutiaeRef);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates neighbor insertion at the beginning of the list.
     * Tests insertion of neighbor with smallest distance at list start.
     */
    @Test
    public void insertNeighborAtBeginning() {
        AtomicIntegerArray nbrList = new AtomicIntegerArray(5);
        AtomicReferenceArray<Double> nbrSqrDists = new AtomicReferenceArray<>(5);
        AtomicInteger noOfNbrs = new AtomicInteger(2);

        nbrList.set(0, 20);
        nbrList.set(1, 30);
        nbrSqrDists.set(0, 400.0);
        nbrSqrDists.set(1, 900.0);

        int result = ridges.insertNeighbor(0, 10, 100.0, nbrList, nbrSqrDists, noOfNbrs, 5);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, noOfNbrs.get());
        assertEquals(10, nbrList.get(0));
    }

    /**
     * Validates ridge crossing validation with edge case coordinates.
     * Tests ridge crossing detection at image boundary conditions.
     */
    @Test
    public void validateRidgeCrossingEdgeCase() {
        int[] xlist = {5, 6, 7, 8, 9};
        int[] ylist = {5, 5, 5, 5, 5};

        for (int i = 5; i < 10; i++) {
            int index = (5 * imageWidth) + i;
            if (index < mockBinaryImageData.length) {
                mockBinaryImageData[index] = i % 2;
            }
        }

        int result = ridges.validateRidgeCrossing(1, 3, xlist, ylist, 5, mockBinaryImageData, imageWidth, imageHeight, 5);

        assertTrue(result >= ILfs.FALSE);
    }

    /**
     * Validates that Ridges class inherits from MindTct base class.
     * Tests proper class inheritance structure.
     */
    @Test
    public void mindTctInheritance() {
        assertTrue(ridges instanceof MindTct);
    }

    /**
     * Validates countMinutiaRidges method when neighbors are found and sorted.
     * Tests complete ridge counting workflow with neighbor discovery and sorting.
     */
    @Test
    public void countMinutiaRidgesWithNeighborsFound() {
        Ridges spyRidges = Mockito.spy(ridges);
        AtomicInteger mockNoOfNbrs = new AtomicInteger(1);

        Mockito.doReturn(ILfs.FALSE).when(spyRidges)
                .findNeighbors(any(), any(), anyInt(), anyInt(), any());
        Mockito.doAnswer(invocation -> {
            AtomicInteger noOfNbrs = invocation.getArgument(1);
            noOfNbrs.set(1);
            return ILfs.FALSE;
        }).when(spyRidges).findNeighbors(any(), any(), anyInt(), anyInt(), any());

        Mockito.doReturn(ILfs.FALSE).when(spyRidges)
                .sortNeighbors(any(), anyInt(), anyInt(), any());
        Mockito.doReturn(2).when(spyRidges)
                .ridgeCount(anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), any());

        int result = spyRidges.countMinutiaRidges(0, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates ridgeCount method when no points exist on trajectory.
     * Tests ridge counting when line generation produces empty point set.
     */
    @Test
    public void ridgeCountWithNoPointsOnTrajectory() {
        try (MockedStatic<Line> mockedLine = Mockito.mockStatic(Line.class)) {
            Line mockLine = mock(Line.class);
            mockedLine.when(Line::getInstance).thenReturn(mockLine);

            when(mockLine.linePoints(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        AtomicInteger num = invocation.getArgument(2);
                        num.set(0);
                        return ILfs.FALSE;
                    });

            int result = ridges.ridgeCount(0, 1, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.FALSE, result);
        }
    }

    /**
     * Validates ridgeCount method when no opposite pixel is found.
     * Tests ridge counting when transition detection fails along trajectory.
     */
    @Test
    public void ridgeCountWithNoOppositePixelFound() {
        try (MockedStatic<Line> mockedLine = Mockito.mockStatic(Line.class)) {
            Line mockLine = mock(Line.class);
            mockedLine.when(Line::getInstance).thenReturn(mockLine);

            when(mockLine.linePoints(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        int[] xlist = invocation.getArgument(0);
                        int[] ylist = invocation.getArgument(1);
                        AtomicInteger num = invocation.getArgument(2);
                        xlist[0] = 0; xlist[1] = 1; xlist[2] = 2;
                        ylist[0] = 0; ylist[1] = 0; ylist[2] = 0;
                        num.set(3);
                        return ILfs.FALSE;
                    });

            for (int i = 0; i < 3; i++) {
                mockBinaryImageData[i] = 1;
            }

            int result = ridges.ridgeCount(0, 1, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.FALSE, result);
        }
    }

    /**
     * Validates ridgeCount method with valid ridge crossing detection.
     * Tests successful ridge counting when valid crossings are found.
     */
    @Test
    public void ridgeCountWithValidRidgeCrossing() {
        try (MockedStatic<Line> mockedLine = Mockito.mockStatic(Line.class)) {
            Line mockLine = mock(Line.class);
            mockedLine.when(Line::getInstance).thenReturn(mockLine);

            when(mockLine.linePoints(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        int[] xlist = invocation.getArgument(0);
                        int[] ylist = invocation.getArgument(1);
                        AtomicInteger num = invocation.getArgument(2);
                        for (int i = 0; i < 10; i++) {
                            xlist[i] = i;
                            ylist[i] = 0;
                        }
                        num.set(10);
                        return ILfs.FALSE;
                    });

            for (int i = 0; i < 10; i++) {
                mockBinaryImageData[i] = i % 2;
            }

            Ridges spyRidges = Mockito.spy(ridges);
            Mockito.doReturn(ILfs.TRUE).when(spyRidges)
                    .validateRidgeCrossing(anyInt(), anyInt(), any(), any(), anyInt(), any(), anyInt(), anyInt(), anyInt());

            int result = spyRidges.ridgeCount(0, 1, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertTrue(result >= 0);
        }
    }

    /**
     * Validates ridgeCount method when ridge crossing validation fails.
     * Tests error propagation when ridge crossing validation encounters error.
     */
    @Test
    public void ridgeCountWithValidateRidgeCrossingError() {
        try (MockedStatic<Line> mockedLine = Mockito.mockStatic(Line.class)) {
            Line mockLine = mock(Line.class);
            mockedLine.when(Line::getInstance).thenReturn(mockLine);

            when(mockLine.linePoints(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        int[] xlist = invocation.getArgument(0);
                        int[] ylist = invocation.getArgument(1);
                        AtomicInteger num = invocation.getArgument(2);
                        for (int i = 0; i < 10; i++) {
                            xlist[i] = i;
                            ylist[i] = 0;
                        }
                        num.set(10);
                        return ILfs.FALSE;
                    });

            for (int i = 0; i < 10; i++) {
                mockBinaryImageData[i] = i % 2;
            }

            Ridges spyRidges = Mockito.spy(ridges);
            Mockito.doReturn(ILfs.ERROR_CODE_301).when(spyRidges)
                    .validateRidgeCrossing(anyInt(), anyInt(), any(), any(), anyInt(), any(), anyInt(), anyInt(), anyInt());

            int result = spyRidges.ridgeCount(0, 1, mockMinutiae, mockBinaryImageData, imageWidth, imageHeight, mockLfsParams);

            assertEquals(ILfs.ERROR_CODE_301, result);
        }
    }
}