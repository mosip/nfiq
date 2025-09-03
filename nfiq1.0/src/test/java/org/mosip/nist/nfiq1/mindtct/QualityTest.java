package org.mosip.nist.nfiq1.mindtct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.any;

/**
 * Test class for {@link Quality} providing comprehensive validation of quality map
 * generation and minutiae quality assessment functionality.
 *
 * <p>This class validates the functionality of quality assessment algorithms used
 * in NIST's Mindtct fingerprint analysis system for evaluating minutiae reliability
 * and generating quality maps based on various image characteristics.</p>
 */
class QualityTest {

    private Quality quality;
    private Defs mockDefs;
    private Free mockFree;
    private Maps mockMaps;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes Quality instance with proper singleton reset and mock dependencies.
     */
    @BeforeEach
    void setUp() throws Exception {
        resetQualitySingleton();
        quality = spy(Quality.getInstance());
        mockDefs = mock(Defs.class);
        mockFree = mock(Free.class);
        mockMaps = mock(Maps.class);

        doReturn(mockDefs).when(quality).getDefs();
        doReturn(mockFree).when(quality).getFree();
        doNothing().when(mockFree).free(any());
    }

    /**
     * Helper method to reset singleton using reflection for clean test state.
     */
    private void resetQualitySingleton() throws Exception {
        Field instanceField = Quality.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    /**
     * Validates default singleton pattern implementation.
     * Ensures getInstance returns the same instance across multiple calls.
     */
    @Test
    void getInstanceDefaultSingleton() {
        Quality instance1 = Quality.getInstance();
        Quality instance2 = Quality.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    /**
     * Validates parameterized singleton pattern implementation.
     * Ensures getInstance with parameters maintains singleton behavior.
     */
    @Test
    void getInstanceParameterizedSingleton() throws Exception {
        resetQualitySingleton();
        Quality instance1 = Quality.getInstance(100, 80);
        Quality instance2 = Quality.getInstance(100, 80);

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    /**
     * Validates dependency getter methods return non-null instances.
     * Tests proper initialization of required dependencies.
     */
    @Test
    void dependencyGetters() {
        assertNotNull(quality.getDefs());
        assertNotNull(quality.getFree());
    }

    /**
     * Validates generateQualityMap method with comprehensive scenarios.
     * Tests quality map generation with various block configurations.
     */
    @Test
    void generateQualityMapComprehensive() {
        Maps map = createTestMaps(5, 5);

        int result = quality.generateQualityMap(map);

        assertEquals(ILfs.FALSE, result);
        assertNotNull(quality.getQualityMap());
        assertEquals(25, quality.getQualityMap().length());
        assertEquals(5, quality.getMappedImageWidth());
        assertEquals(5, quality.getMappedImageHeight());
    }

    /**
     * Validates generateQualityMap method with low contrast blocks.
     * Tests quality assignment for blocks identified as having low contrast.
     */
    @Test
    void generateQualityMapLowContrast() {
        Maps map = createTestMaps(3, 3);

        map.getLowContrastMap().set(0, ILfs.TRUE);
        map.getLowContrastMap().set(4, ILfs.TRUE);

        int result = quality.generateQualityMap(map);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.FALSE, quality.getQualityMap().get(0));
        assertEquals(ILfs.FALSE, quality.getQualityMap().get(4));
    }

    /**
     * Validates generateQualityMap method with invalid direction blocks.
     * Tests quality assignment for blocks with invalid direction values.
     */
    @Test
    void generateQualityMapInvalidDirection() {
        Maps map = createTestMaps(3, 3);

        map.getDirectionMap().set(1, -1);
        map.getDirectionMap().set(5, -2);

        int result = quality.generateQualityMap(map);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.FALSE, quality.getQualityMap().get(1));
        assertEquals(ILfs.FALSE, quality.getQualityMap().get(5));
    }

    /**
     * Validates generateQualityMap method with edge blocks.
     * Tests quality assignment for blocks located at image boundaries.
     */
    @Test
    void generateQualityMapEdgeBlocks() {
        Maps map = createTestMaps(10, 10);

        int result = quality.generateQualityMap(map);

        assertEquals(ILfs.FALSE, result);
        assertEquals(1, quality.getQualityMap().get(0));
        assertEquals(1, quality.getQualityMap().get(9));
        assertEquals(1, quality.getQualityMap().get(90));
    }

    /**
     * Validates generateQualityMap method with low flow and high curve blocks.
     * Tests quality computation considering ridge flow and curvature characteristics.
     */
    @Test
    void generateQualityMapLowFlowHighCurve() {
        Maps map = createTestMaps(7, 7);

        int centerIndex = 3 * 7 + 3;
        map.getLowFlowMap().set(centerIndex, ILfs.TRUE);

        int result = quality.generateQualityMap(map);

        assertEquals(ILfs.FALSE, result);
        assertEquals(2, quality.getQualityMap().get(centerIndex));
    }

    /**
     * Validates combinedMinutiaQuality method with valid input parameters.
     * Tests combined quality assessment for minutiae using multiple quality measures.
     */
    @Test
    void combinedMinutiaQualityValid() {
        Minutiae minutiae = createTestMinutiae();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(minutiae);
        Maps map = createTestMaps(10, 10);

        quality.generateQualityMap(map);

        when(mockMaps.pixelizeMap(any(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(ILfs.FALSE);
        when(mockDefs.sRound(anyDouble())).thenReturn(5);

        int[] imageData = new int[100 * 100];
        for (int i = 0; i < imageData.length; i++) {
            imageData[i] = (i % 256);
        }

        int result = quality.combinedMinutiaQuality(oMinutiae, mockMaps, 8, imageData, 100, 100, 8, 300.0);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates combinedMinutiaQuality method with invalid image depth.
     * Tests error handling for unsupported image bit depths.
     */
    @Test
    void combinedMinutiaQualityInvalidDepth() {
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createTestMinutiae());

        int result = quality.combinedMinutiaQuality(oMinutiae, mockMaps, 8, new int[100], 10, 10, 16, 300.0);

        assertEquals(-2, result);
    }

    /**
     * Validates combinedMinutiaQuality method with pixelizeMap error.
     * Tests error propagation when map pixelization fails.
     */
    @Test
    void combinedMinutiaQualityPixelizeError() {
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createTestMinutiae());
        Maps map = createTestMaps(5, 5);
        quality.generateQualityMap(map);

        when(mockMaps.pixelizeMap(any(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(-100);

        int result = quality.combinedMinutiaQuality(oMinutiae, mockMaps, 8, new int[100], 10, 10, 8, 300.0);

        assertEquals(-100, result);
    }

    /**
     * Validates combinedMinutiaQuality method with invalid quality value.
     * Tests error handling when quality values exceed valid ranges.
     */
    @Test
    void combinedMinutiaQualityInvalidQualityValue() {
        Minutiae minutiae = createTestMinutiaeWithCorrectPositions();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(minutiae);
        Maps map = createTestMaps(10, 10);
        quality.generateQualityMap(map);

        when(mockDefs.sRound(anyDouble())).thenReturn(5);

        doAnswer(invocation -> {
            AtomicIntegerArray pmap = invocation.getArgument(0);
            pmap.set(0, 999);
            return ILfs.FALSE;
        }).when(mockMaps).pixelizeMap(any(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt());

        int result = quality.combinedMinutiaQuality(oMinutiae, mockMaps, 8, new int[10000], 100, 100, 8, 300.0);

        assertEquals(-3, result);
    }

    /**
     * Validates grayscaleReliability method with normal minutia conditions.
     * Tests reliability calculation based on grayscale intensity patterns.
     */
    @Test
    void grayscaleReliabilityNormal() {
        Minutia minutia = createTestMinutia(50, 50);

        int[] imageData = new int[100 * 100];
        for (int i = 0; i < imageData.length; i++) {
            imageData[i] = (i % 256);
        }

        double reliability = quality.grayscaleReliability(minutia, imageData, 100, 100, 5);

        assertTrue(reliability >= 0.0);
        assertTrue(reliability <= 1.0);
    }

    /**
     * Validates getNeighborhoodStats method with normal conditions.
     * Tests statistical analysis of pixel neighborhoods around minutiae.
     */
    @Test
    void getNeighborhoodStatsNormal() {
        Minutia minutia = createTestMinutia(50, 50);
        AtomicReference<Double> mean = new AtomicReference<>();
        AtomicReference<Double> stdev = new AtomicReference<>();

        int[] imageData = new int[100 * 100];
        for (int i = 0; i < imageData.length; i++) {
            imageData[i] = 128;
        }

        quality.getNeighborhoodStats(mean, stdev, minutia, imageData, 100, 100, 5);

        assertEquals(128.0, mean.get(), 0.01);
        assertEquals(0.0, stdev.get(), 0.01);
    }

    /**
     * Validates getNeighborhoodStats method with edge minutia positioning.
     * Tests statistical computation for minutiae near image boundaries.
     */
    @Test
    void getNeighborhoodStatsEdgeMinutia() {
        Minutia minutia = createTestMinutia(2, 2);
        AtomicReference<Double> mean = new AtomicReference<>();
        AtomicReference<Double> stdev = new AtomicReference<>();

        quality.getNeighborhoodStats(mean, stdev, minutia, new int[100], 10, 10, 5);

        assertEquals(0.0, mean.get());
        assertEquals(0.0, stdev.get());
    }

    /**
     * Validates getNeighborhoodStats method with empty neighborhood scenario.
     * Tests handling when no valid neighbors are available for analysis.
     */
    @Test
    void getNeighborhoodStatsEmptyNeighborhood() {
        Minutia minutia = createTestMinutia(50, 50);
        AtomicReference<Double> mean = new AtomicReference<>();
        AtomicReference<Double> stdev = new AtomicReference<>();

        int[] imageData = new int[100 * 100];

        quality.getNeighborhoodStats(mean, stdev, minutia, imageData, 100, 100, 5);

        assertEquals(0.0, mean.get());
        assertEquals(0.0, stdev.get());
    }

    /**
     * Validates reliabilityFromQualityMap method with all quality levels.
     * Tests reliability assignment based on complete range of quality values.
     */
    @Test
    void reliabilityFromQualityMapAllQualityLevels() {
        Minutiae minutiae = createTestMinutiaeWithCorrectPositions();
        Maps map = createTestMaps(10, 10);
        quality.generateQualityMap(map);

        doAnswer(invocation -> {
            AtomicIntegerArray pmap = invocation.getArgument(0);
            pmap.set(0, 0); pmap.set(1, 1); pmap.set(2, 2); pmap.set(3, 3); pmap.set(4, 4);
            return ILfs.FALSE;
        }).when(mockMaps).pixelizeMap(any(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt());

        int result = quality.reliabilityFromQualityMap(minutiae, mockMaps, 10, 10, 8);

        assertEquals(ILfs.FALSE, result);
        assertEquals(0.0, minutiae.getList().get(0).getReliability());
        assertEquals(0.25, minutiae.getList().get(1).getReliability());
        assertEquals(0.50, minutiae.getList().get(2).getReliability());
        assertEquals(0.75, minutiae.getList().get(3).getReliability());
        assertEquals(0.99, minutiae.getList().get(4).getReliability());
    }

    /**
     * Validates reliabilityFromQualityMap method with invalid quality value.
     * Tests error handling when quality values are outside acceptable range.
     */
    @Test
    void reliabilityFromQualityMapInvalidQuality() {
        Minutiae minutiae = createTestMinutiaeWithCorrectPositions();
        Maps map = createTestMaps(5, 5);
        quality.generateQualityMap(map);

        doAnswer(invocation -> {
            AtomicIntegerArray pmap = invocation.getArgument(0);
            pmap.set(0, 999);
            return ILfs.FALSE;
        }).when(mockMaps).pixelizeMap(any(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt());

        int result = quality.reliabilityFromQualityMap(minutiae, mockMaps, 100, 100, 8);

        assertEquals(-2, result);
        assertEquals(0.0, minutiae.getList().get(0).getReliability());
    }

    /**
     * Validates getter and setter methods for quality map dimensions and data.
     * Tests property access methods with fresh instance to avoid state pollution.
     */
    @Test
    void gettersAndSetters() throws Exception {
        resetQualitySingleton();
        Quality testQuality = Quality.getInstance(50, 40);

        assertEquals(50, testQuality.getMappedImageWidth());
        assertEquals(40, testQuality.getMappedImageHeight());

        testQuality.setMappedImageWidth(60);
        testQuality.setMappedImageHeight(45);

        assertEquals(60, testQuality.getMappedImageWidth());
        assertEquals(45, testQuality.getMappedImageHeight());

        AtomicIntegerArray newMap = new AtomicIntegerArray(100);
        testQuality.setQualityMap(newMap);
        assertSame(newMap, testQuality.getQualityMap());
    }

    /**
     * Creates test Maps instance with specified dimensions and proper array sizing.
     */
    private Maps createTestMaps(int width, int height) {
        Maps map = mock(Maps.class);

        int size = Math.max(width * height, 500);

        AtomicIntegerArray directionMap = new AtomicIntegerArray(size);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(size);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(size);
        AtomicIntegerArray highCurveMap = new AtomicIntegerArray(size);
        AtomicInteger mapWidth = new AtomicInteger(width);
        AtomicInteger mapHeight = new AtomicInteger(height);

        for (int i = 0; i < size; i++) {
            directionMap.set(i, 0);
            lowContrastMap.set(i, ILfs.FALSE);
            lowFlowMap.set(i, ILfs.FALSE);
            highCurveMap.set(i, ILfs.FALSE);
        }

        when(map.getDirectionMap()).thenReturn(directionMap);
        when(map.getLowContrastMap()).thenReturn(lowContrastMap);
        when(map.getLowFlowMap()).thenReturn(lowFlowMap);
        when(map.getHighCurveMap()).thenReturn(highCurveMap);
        when(map.getMappedImageWidth()).thenReturn(mapWidth);
        when(map.getMappedImageHeight()).thenReturn(mapHeight);

        return map;
    }

    /**
     * Creates test Minutiae instance with sample minutiae data.
     */
    private Minutiae createTestMinutiae() {
        Minutiae minutiae = new Minutiae();
        List<Minutia> minutiaList = new ArrayList<>();

        minutiaList.add(createTestMinutia(25, 25));
        minutiaList.add(createTestMinutia(75, 75));

        minutiae.setList(minutiaList);
        minutiae.setNum(2);

        return minutiae;
    }

    /**
     * Creates test Minutiae instance with positions that prevent array bounds issues.
     */
    private Minutiae createTestMinutiaeWithCorrectPositions() {
        Minutiae minutiae = new Minutiae();
        List<Minutia> minutiaList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            minutiaList.add(createTestMinutia(i, 0));
        }

        minutiae.setList(minutiaList);
        minutiae.setNum(5);

        return minutiae;
    }

    /**
     * Creates test Minutia instance with specified coordinates and default attributes.
     */
    private Minutia createTestMinutia(int x, int y) {
        Minutia minutia = new Minutia();
        minutia.setX(x);
        minutia.setY(y);
        minutia.setEx(x);
        minutia.setEy(y);
        minutia.setDirection(0);
        minutia.setReliability(0.0);
        minutia.setType(1);
        minutia.setAppearing(1);
        minutia.setFeatureId(1);

        return minutia;
    }
}
