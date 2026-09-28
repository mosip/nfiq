package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Nist;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;
import org.mosip.nist.nfiq1.imagetools.ImageDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;

/**
 * Test class for {@link Maps} map generation,
 * processing, and analysis functionality for fingerprint image processing.
 *
 * <p>This class validates the functionality of direction maps, contrast maps,
 * flow maps, and various image processing algorithms used in NIST's Mindtct
 * fingerprint analysis system.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MapsTest {

    private static final int NUM_DIRS = ILfs.NUM_DIRECTIONS;

    private static int[] fingerprintImage;
    private static int fingerprintWidth;
    private static int fingerprintHeight;

    private Maps maps;

    private AtomicInteger savedMappedWidth;
    private AtomicInteger savedMappedHeight;
    private AtomicIntegerArray savedDirectionMap;
    private AtomicIntegerArray savedLowContrastMap;
    private AtomicIntegerArray savedLowFlowMap;
    private AtomicIntegerArray savedHighCurveMap;
    private boolean savedShowLogs;

    @Mock
    private LfsParams mockLfsParams;

    @Mock
    private DftWaves mockDftWaves;

    @Mock
    private RotGrids mockDftGrids;

    @Mock
    private DirToRad mockDirToRad;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes the Maps singleton instance for testing.
     */
    @BeforeEach
    void setUp() {
        maps = Maps.getInstance();
        savedMappedWidth = maps.getMappedImageWidth();
        savedMappedHeight = maps.getMappedImageHeight();
        savedDirectionMap = maps.getDirectionMap();
        savedLowContrastMap = maps.getLowContrastMap();
        savedLowFlowMap = maps.getLowFlowMap();
        savedHighCurveMap = maps.getHighCurveMap();
        savedShowLogs = Nist.isShowLogs();
    }

    /**
     * Restores the state of the shared Maps singleton and the static show-logs flag
     * so that tests in this and other classes are not affected by mutations made here.
     */
    @AfterEach
    void restoreSharedState() {
        maps.setMappedImageWidth(savedMappedWidth);
        maps.setMappedImageHeight(savedMappedHeight);
        maps.setDirectionMap(savedDirectionMap);
        maps.setLowContrastMap(savedLowContrastMap);
        maps.setLowFlowMap(savedLowFlowMap);
        maps.setHighCurveMap(savedHighCurveMap);
        Nist.setShowLogs(savedShowLogs);
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Maps instance1 = Maps.getInstance();
        Maps instance2 = Maps.getInstance();
        assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null instance.
     * Ensures proper initialization of the Defs dependency.
     */
    @Test
    void getDefsReturnsNotNull() {
        assertNotNull(maps.getDefs());
    }

    /**
     * Validates that getBlock returns a non-null instance.
     * Ensures proper initialization of the Block dependency.
     */
    @Test
    void getBlockReturnsNotNull() {
        assertNotNull(maps.getBlock());
    }

    /**
     * Validates that getInit returns a non-null instance.
     * Ensures proper initialization of the Init dependency.
     */
    @Test
    void getInitReturnsNotNull() {
        assertNotNull(maps.getInit());
    }

    /**
     * Validates that getFree returns a non-null instance.
     * Ensures proper initialization of the Free dependency.
     */
    @Test
    void getFreeReturnsNotNull() {
        assertNotNull(maps.getFree());
    }

    /**
     * Validates that getDft returns a non-null instance.
     * Ensures proper initialization of the Dft dependency.
     */
    @Test
    void getDftReturnsNotNull() {
        assertNotNull(maps.getDft());
    }

    /**
     * Validates that getLfsUtil returns a non-null instance.
     * Ensures proper initialization of the LfsUtil dependency.
     */
    @Test
    void getLfsUtilReturnsNotNull() {
        assertNotNull(maps.getLfsUtil());
    }

    /**
     * Validates that getMorph returns a non-null instance.
     * Ensures proper initialization of the Morph dependency.
     */
    @Test
    void getMorphReturnsNotNull() {
        assertNotNull(maps.getMorph());
    }

    /**
     * Validates numValid8Nbrs method with all valid neighbors.
     * Tests counting of valid 8-connected neighbors around a pixel.
     */
    @Test
    void numValid8NbrsWithValidNeighborsReturnsCount() {
        AtomicIntegerArray testMap = new AtomicIntegerArray(9);
        for (int i = 0; i < 9; i++) {
            testMap.set(i, 1);
        }

        int result = maps.numValid8Nbrs(testMap, 1, 1, 3, 3);

        assertEquals(8, result);
    }

    /**
     * Validates numValid8Nbrs method with all invalid neighbors.
     * Tests behavior when all neighboring pixels are marked as invalid.
     */
    @Test
    void numValid8NbrsWithInvalidNeighborsReturnsZero() {
        AtomicIntegerArray testMap = new AtomicIntegerArray(9);
        for (int i = 0; i < 9; i++) {
            testMap.set(i, -1);
        }

        int result = maps.numValid8Nbrs(testMap, 1, 1, 3, 3);

        assertEquals(0, result);
    }

    /**
     * Validates accumulateNbrVorticity method with valid direction difference.
     * Tests vorticity accumulation for neighboring direction vectors.
     */
    @Test
    void accumulateNbrVorticityWithValidDirectionsAccumulates() {
        AtomicInteger vorticity = new AtomicInteger(0);

        maps.accumulateNbrVorticity(vorticity, 1, 3, 16);

        assertEquals(1, vorticity.get());
    }

    /**
     * Validates accumulateNbrVorticity method with large direction distance.
     * Tests behavior when direction difference exceeds half the direction range.
     */
    @Test
    void accumulateNbrVorticityWithLargeDistanceDecrements() {
        AtomicInteger vorticity = new AtomicInteger(0);

        maps.accumulateNbrVorticity(vorticity, 1, 10, 16);

        assertEquals(-1, vorticity.get());
    }

    /**
     * Validates curvature method with valid neighboring directions.
     * Tests calculation of maximum direction distance for curvature measurement.
     */
    @Test
    void curvatureWithValidNeighborsReturnsMaxDistance() {
        AtomicIntegerArray testMap = new AtomicIntegerArray(9);
        testMap.set(4, 0);
        testMap.set(1, 8);

        int result = maps.curvature(testMap, 1, 1, 3, 3, 16);

        assertEquals(8, result);
    }

    /**
     * Validates vorticity method with valid neighboring directions.
     * Tests calculation of rotational flow measure around a pixel.
     */
    @Test
    void vorticityWithValidNeighborsReturnsMeasure() {
        AtomicIntegerArray testMap = new AtomicIntegerArray(9);
        for (int i = 0; i < 9; i++) {
            testMap.set(i, i % 8);
        }

        int result = maps.vorticity(testMap, 1, 1, 3, 3, 16);

        assertTrue(result >= 0);
    }

    /**
     * Validates morphMapWithTF method for morphological processing.
     * Tests map morphology operations with a true/false map.
     */
    @Test
    void morphMapWithTF() {
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));

        AtomicIntegerArray tfMap = new AtomicIntegerArray(9);
        tfMap.set(4, 1);

        int result = maps.morphMapWithTF(tfMap, mockLfsParams);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates pixelizeMap method for successful block-to-pixel mapping.
     * Tests conversion of block-level data to pixel-level representation.
     */
    @Test
    void pixelizeMapSuccess() {
        AtomicIntegerArray oMap = new AtomicIntegerArray(16);
        AtomicIntegerArray blockMap = new AtomicIntegerArray(4);

        for (int i = 0; i < 4; i++) {
            blockMap.set(i, i + 1);
        }

        int result = maps.pixelizeMap(oMap, 4, 4, blockMap, 2, 2, 2);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates interpolateDirectionMap method for direction interpolation.
     * Tests interpolation of invalid directions using valid neighboring directions.
     */
    @Test
    void interpolateDirectionMap() {
        AtomicIntegerArray dirMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        dirMap.set(4, ILfs.INVALID_DIR);
        dirMap.set(1, 0);
        dirMap.set(5, 4);

        when(mockLfsParams.getMinInterpolateNbrs()).thenReturn(2);

        int result = maps.interpolateDirectionMap(dirMap, lowContrastMap, 3, 3, mockLfsParams);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates generateHighCurveMap method for high curvature detection.
     * Tests identification of areas with high ridge curvature.
     */
    @Test
    void generateHighCurveMap() {
        AtomicIntegerArray highCurveMap = new AtomicIntegerArray(9);
        AtomicIntegerArray dirMap = new AtomicIntegerArray(9);

        dirMap.set(4, 0);
        dirMap.set(1, 8);

        when(mockLfsParams.getHighcurvCurvatureMin()).thenReturn(5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);

        int result = maps.generateHighCurveMap(highCurveMap, dirMap, 3, 3, mockLfsParams);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates primaryDirectionTest method for direction quality assessment.
     * Tests primary direction validation based on power spectrum analysis.
     */
    @Test
    void primaryDirectionPassed() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        Double[] powerArray0 = new Double[16];
        Double[] powerArray1 = new Double[16];
        for (int i = 0; i < 16; i++) {
            powerArray0[i] = 1.0;
            powerArray1[i] = 2.0;
        }
        powers.set(0, powerArray0);
        powers.set(1, powerArray1);

        AtomicIntegerArray wis = new AtomicIntegerArray(1);
        wis.set(0, 0);

        AtomicReferenceArray<Double> powmaxs = new AtomicReferenceArray<>(1);
        powmaxs.set(0, 10.0);

        AtomicIntegerArray powmaxDirs = new AtomicIntegerArray(1);
        powmaxDirs.set(0, 4);

        AtomicReferenceArray<Double> pownorms = new AtomicReferenceArray<>(1);
        pownorms.set(0, 5.0);

        when(mockLfsParams.getPowmaxMin()).thenReturn(5.0);
        when(mockLfsParams.getPownormMin()).thenReturn(3.0);
        when(mockLfsParams.getPowmaxMax()).thenReturn(15.0);

        int result = maps.primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, 1, mockLfsParams);

        assertEquals(4, result);
    }

    /**
     * Validates secondaryForkTest method for fork detection.
     * Tests detection of secondary ridges or bifurcations in power spectrum.
     */
    @Test
    void secondaryForkInvalid() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        Double[] powerArray0 = new Double[16];
        Double[] powerArray1 = new Double[16];
        for (int i = 0; i < 16; i++) {
            powerArray0[i] = 1.0;
            powerArray1[i] = 2.0;
        }
        powers.set(0, powerArray0);
        powers.set(1, powerArray1);

        AtomicIntegerArray wis = new AtomicIntegerArray(1);
        wis.set(0, 0);

        AtomicReferenceArray<Double> powmaxs = new AtomicReferenceArray<>(1);
        powmaxs.set(0, 8.0);

        AtomicIntegerArray powmaxDirs = new AtomicIntegerArray(1);
        powmaxDirs.set(0, 6);

        AtomicReferenceArray<Double> pownorms = new AtomicReferenceArray<>(1);
        pownorms.set(0, 4.0);

        when(mockLfsParams.getPowmaxMin()).thenReturn(5.0);

        int result = maps.secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, 1, mockLfsParams);

        assertEquals(ILfs.INVALID_DIR, result);
    }

    /**
     * Validates genImageMaps method with invalid grid dimensions.
     * Tests error handling when DFT grid dimensions don't match image dimensions.
     */
    @Test
    void genImageMapsInvalidGrids() {
        int[] imageData = new int[100];

        when(mockDftGrids.getGridWidth()).thenReturn(8);
        when(mockDftGrids.getGridHeight()).thenReturn(10);

        int result = maps.genImageMaps(imageData, 10, 10, mockDirToRad, mockDftWaves, mockDftGrids, mockLfsParams);

        assertEquals(ILfs.ERROR_CODE_540, result);
    }

    /**
     * Validates pixelizeMap method with dimension mismatch.
     * Tests error handling when output and block map dimensions are incompatible.
     */
    @Test
    void pixelizeMapDimensionMismatch() {
        AtomicIntegerArray oMap = new AtomicIntegerArray(16);
        AtomicIntegerArray blockMap = new AtomicIntegerArray(4);

        int result = maps.pixelizeMap(oMap, 6, 6, blockMap, 2, 2, 2);

        assertEquals(ILfs.ERROR_CODE_591, result);
    }

    /**
     * Validates getInstance method with parameter initialization.
     * Tests singleton instantiation with pre-configured map arrays.
     */
    @Test
    void getInstanceWithParameters() {
        AtomicIntegerArray dirMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(4);
        AtomicIntegerArray highCurveMap = new AtomicIntegerArray(4);

        Maps instance = Maps.getInstance(dirMap, lowContrastMap, lowFlowMap, highCurveMap);

        assertNotNull(instance);
    }

    /**
     * Validates initialiseMaps method with low contrast error condition.
     * Tests error propagation during map initialization when low contrast detection fails.
     */
    @Test
    void initialiseMapsWithLowContrastError() {
        AtomicIntegerArray dirMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(4);
        AtomicIntegerArray blockOffsets = new AtomicIntegerArray(4);
        int[] imageData = new int[100];

        when(mockDftWaves.getNWaves()).thenReturn(4);
        when(mockDftGrids.getNoOfGrids()).thenReturn(8);
        when(mockLfsParams.getWindowSize()).thenReturn(32);
        when(mockLfsParams.getWindowOffset()).thenReturn(16);

        Block mockBlock = mock(Block.class);
        when(mockBlock.lowContrastBlock(anyInt(), anyInt(), any(), anyInt(), anyInt(), any()))
                .thenReturn(ILfs.ERROR_CODE_301);

        Maps spyMaps = spy(maps);
        doReturn(mockBlock).when(spyMaps).getBlock();

        int result = spyMaps.initialiseMaps(dirMap, lowContrastMap, lowFlowMap, blockOffsets, 2, 2, imageData, 10, 10, mockDftWaves, mockDftGrids, mockLfsParams);

        assertEquals(ILfs.ERROR_CODE_301, result);
    }

    /**
     * Validates initialiseMaps method with successful low contrast detection.
     * Tests proper handling when a block is identified as having low contrast.
     */
    @Test
    void initialiseMapsWithLowContrast() {
        AtomicIntegerArray dirMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(4);
        AtomicIntegerArray lowFlowMap = new AtomicIntegerArray(4);
        AtomicIntegerArray blockOffsets = new AtomicIntegerArray(4);
        int[] imageData = new int[100];

        when(mockDftWaves.getNWaves()).thenReturn(4);
        when(mockDftGrids.getNoOfGrids()).thenReturn(8);
        when(mockLfsParams.getWindowSize()).thenReturn(32);
        when(mockLfsParams.getWindowOffset()).thenReturn(16);

        Block mockBlock = mock(Block.class);
        when(mockBlock.lowContrastBlock(anyInt(), anyInt(), any(), anyInt(), anyInt(), any()))
                .thenReturn(ILfs.TRUE);

        Maps spyMaps = spy(maps);
        doReturn(mockBlock).when(spyMaps).getBlock();

        int result = spyMaps.initialiseMaps(dirMap, lowContrastMap, lowFlowMap, blockOffsets, 2, 2, imageData, 10, 10, mockDftWaves, mockDftGrids, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.TRUE, lowContrastMap.get(0));
    }

    /**
     * Validates smoothDirectionMap method with valid neighbors.
     * Tests direction smoothing using 8-connected neighbor averaging.
     */
    @Test
    void smoothDirectionMapWithValidNeighbors() {
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));

        AtomicIntegerArray dirMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        dirMap.set(4, 0);
        lowContrastMap.set(4, ILfs.FALSE);

        when(mockLfsParams.getDirStrengthMin()).thenReturn(0.2);
        when(mockLfsParams.getRmvValidNbrMin()).thenReturn(3);


        Maps spyMaps = spy(maps);
        doAnswer(invocation -> {
            AtomicInteger avgDir = invocation.getArgument(0);
            AtomicReference<Double> dirStrength = invocation.getArgument(1);
            AtomicInteger valid = invocation.getArgument(2);
            avgDir.set(4);
            dirStrength.set(0.5);
            valid.set(5);
            return null;
        }).when(spyMaps).average8NbrDir(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any());

        spyMaps.smoothDirectionMap(dirMap, lowContrastMap, mockDirToRad, mockLfsParams);

        assertEquals(4, dirMap.get(4));
    }

    /**
     * Validates smoothDirectionMap method with invalid direction.
     * Tests direction assignment for pixels with initially invalid directions.
     */
    @Test
    void smoothDirectionMapWithInvalidDirection() {
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));

        AtomicIntegerArray dirMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        dirMap.set(4, ILfs.INVALID_DIR);
        lowContrastMap.set(4, ILfs.FALSE);

        when(mockLfsParams.getDirStrengthMin()).thenReturn(0.2);
        when(mockLfsParams.getSmoothValidNbrMin()).thenReturn(3);

        Maps spyMaps = spy(maps);
        doAnswer(invocation -> {
            AtomicInteger avgDir = invocation.getArgument(0);
            AtomicReference<Double> dirStrength = invocation.getArgument(1);
            AtomicInteger valid = invocation.getArgument(2);
            avgDir.set(8);
            dirStrength.set(0.5);
            valid.set(5);
            return null;
        }).when(spyMaps).average8NbrDir(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any());

        spyMaps.smoothDirectionMap(dirMap, lowContrastMap, mockDirToRad, mockLfsParams);

        assertEquals(8, dirMap.get(4));
    }

    /**
     * Validates generateHighCurveMap method with vorticity calculation.
     * Tests high curvature detection using vorticity analysis of direction fields.
     */
    @Test
    void generateHighCurveMapWithVorticity() {
        AtomicIntegerArray highCurveMap = new AtomicIntegerArray(9);
        AtomicIntegerArray dirMap = new AtomicIntegerArray(9);

        dirMap.set(4, ILfs.INVALID_DIR);
        dirMap.set(1, 0);
        dirMap.set(3, 4);
        dirMap.set(5, 8);
        dirMap.set(7, 12);

        when(mockLfsParams.getVortValidNbrMin()).thenReturn(3);
        when(mockLfsParams.getHighcurvVorticityMin()).thenReturn(5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);

        Maps spyMaps = spy(maps);
        doReturn(4).when(spyMaps).numValid8Nbrs(any(), anyInt(), anyInt(), anyInt(), anyInt());
        doReturn(8).when(spyMaps).vorticity(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        int result = spyMaps.generateHighCurveMap(highCurveMap, dirMap, 3, 3, mockLfsParams);

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.TRUE, highCurveMap.get(4));
    }

    /**
     * Validates primaryDirectionTest method with failed criteria.
     * Tests rejection of primary direction when power values don't meet thresholds.
     */
    @Test
    void primaryDirectionFailed() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        Double[] powerArray0 = new Double[16];
        Double[] powerArray1 = new Double[16];
        for (int i = 0; i < 16; i++) {
            powerArray0[i] = 1.0;
            powerArray1[i] = 2.0;
        }
        powers.set(0, powerArray0);
        powers.set(1, powerArray1);

        AtomicIntegerArray wis = new AtomicIntegerArray(1);
        wis.set(0, 0);

        AtomicReferenceArray<Double> powmaxs = new AtomicReferenceArray<>(1);
        powmaxs.set(0, 3.0);

        AtomicIntegerArray powmaxDirs = new AtomicIntegerArray(1);
        powmaxDirs.set(0, 4);

        AtomicReferenceArray<Double> pownorms = new AtomicReferenceArray<>(1);
        pownorms.set(0, 2.0);

        lenient().when(mockLfsParams.getPowmaxMin()).thenReturn(5.0);
        lenient().when(mockLfsParams.getPownormMin()).thenReturn(3.0);
        lenient().when(mockLfsParams.getPowmaxMax()).thenReturn(15.0);

        int result = maps.primaryDirectionTest(powers, wis, powmaxs, powmaxDirs, pownorms, 1, mockLfsParams);

        assertEquals(ILfs.INVALID_DIR, result);
    }

    /**
     * Validates secondaryForkTest method with successful fork detection.
     * Tests identification of valid secondary fork in power spectrum analysis.
     */
    @Test
    void secondaryForkPassed() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        Double[] powerArray0 = new Double[16];
        Double[] powerArray1 = new Double[16];
        for (int i = 0; i < 16; i++) {
            powerArray0[i] = 1.0;
            powerArray1[i] = 2.0;
        }
        powerArray1[6] = 1.0;
        powerArray1[10] = 6.0;
        powers.set(0, powerArray0);
        powers.set(1, powerArray1);

        AtomicIntegerArray wis = new AtomicIntegerArray(1);
        wis.set(0, 0);

        AtomicReferenceArray<Double> powmaxs = new AtomicReferenceArray<>(1);
        powmaxs.set(0, 8.0);

        AtomicIntegerArray powmaxDirs = new AtomicIntegerArray(1);
        powmaxDirs.set(0, 8);

        AtomicReferenceArray<Double> pownorms = new AtomicReferenceArray<>(1);
        pownorms.set(0, 4.0);

        when(mockLfsParams.getPowmaxMin()).thenReturn(5.0);
        when(mockLfsParams.getPownormMin()).thenReturn(3.5);
        when(mockLfsParams.getPowmaxMax()).thenReturn(15.0);
        when(mockLfsParams.getForkInterval()).thenReturn(2);
        when(mockLfsParams.getNumDirections()).thenReturn(16);
        when(mockLfsParams.getForkPctPowmax()).thenReturn(0.7);

        int result = maps.secondaryForkTest(powers, wis, powmaxs, powmaxDirs, pownorms, 1, mockLfsParams);

        assertEquals(8, result);
    }

    /**
     * Validates generateInputBlockImageMap method with grid validation error.
     * Tests error handling during input block image map generation.
     */
    @Test
    void generateInputBlockImageMapError() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger mappedWidth = new AtomicInteger();
        AtomicInteger mappedHeight = new AtomicInteger();
        int[] imageData = new int[100];

        when(mockDftGrids.getGridWidth()).thenReturn(8);
        when(mockDftGrids.getGridHeight()).thenReturn(10);

        AtomicIntegerArray result = maps.generateInputBlockImageMap(ret, mappedWidth, mappedHeight, imageData, 10, 10, mockDirToRad, mockDftWaves, mockDftGrids, mockLfsParams);

        assertNull(result);
        assertEquals(ILfs.ERROR_CODE_60, ret.get());
    }

    /**
     * Validates generateInputBlockImageMap method with block offsets error.
     * Tests error handling when block offset calculation fails during map generation.
     */
    @Test
    void generateInputBlockImageMapBlockOffsetsError() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger mappedWidth = new AtomicInteger();
        AtomicInteger mappedHeight = new AtomicInteger();
        int[] imageData = new int[100];

        when(mockDftGrids.getGridWidth()).thenReturn(8);
        when(mockDftGrids.getGridHeight()).thenReturn(8);
        when(mockDftGrids.getPad()).thenReturn(1);

        Block mockBlock = mock(Block.class);
        when(mockBlock.blockOffsets(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    AtomicInteger retArg = invocation.getArgument(0);
                    retArg.set(ILfs.ERROR_CODE_301);
                    return null;
                });

        Maps spyMaps = spy(maps);
        doReturn(mockBlock).when(spyMaps).getBlock();

        AtomicIntegerArray result = spyMaps.generateInputBlockImageMap(ret, mappedWidth, mappedHeight, imageData, 10, 10, mockDirToRad, mockDftWaves, mockDftGrids, mockLfsParams);

        assertNull(result);
        assertEquals(ILfs.ERROR_CODE_301, ret.get());
    }

    @Test
    void smoothInputBlockImageMapValidDirSufficientNeighbors() {
        maps.setMappedImageWidth(new AtomicInteger(2));
        maps.setMappedImageHeight(new AtomicInteger(2));
        
        AtomicIntegerArray inputMap = new AtomicIntegerArray(5);
        inputMap.set(0, 5);
        
        when(mockLfsParams.getDirStrengthMin()).thenReturn(0.2);
        when(mockLfsParams.getRmValidNbrMin()).thenReturn(3);
        
        Maps spyMaps = spy(maps);
        doAnswer(invocation -> {
            AtomicInteger avgDir = invocation.getArgument(0);
            AtomicReference<Double> dirStrength = invocation.getArgument(1);
            AtomicInteger valid = invocation.getArgument(2);
            avgDir.set(8);
            dirStrength.set(0.5);
            valid.set(5);
            return null;
        }).when(spyMaps).average8NbrDir(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any());
        
        spyMaps.smoothInputBlockImageMap(inputMap, mockDirToRad, mockLfsParams);
        
        assertEquals(8, inputMap.get(0));
    }

    @Test
    void smoothInputBlockImageMapInvalidDirSufficientNeighbors() {
        maps.setMappedImageWidth(new AtomicInteger(2));
        maps.setMappedImageHeight(new AtomicInteger(2));
        
        AtomicIntegerArray inputMap = new AtomicIntegerArray(5);
        inputMap.set(0, ILfs.INVALID_DIR);
        
        when(mockLfsParams.getDirStrengthMin()).thenReturn(0.2);
        when(mockLfsParams.getSmoothValidNbrMin()).thenReturn(3);
        
        Maps spyMaps = spy(maps);
        doAnswer(invocation -> {
            AtomicInteger avgDir = invocation.getArgument(0);
            AtomicReference<Double> dirStrength = invocation.getArgument(1);
            AtomicInteger valid = invocation.getArgument(2);
            avgDir.set(12);
            dirStrength.set(0.6);
            valid.set(5);
            return null;
        }).when(spyMaps).average8NbrDir(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any());
        
        spyMaps.smoothInputBlockImageMap(inputMap, mockDirToRad, mockLfsParams);
        
        assertEquals(12, inputMap.get(0));
    }

    @Test
    void smoothInputBlockImageMapWeakDirection() {
        maps.setMappedImageWidth(new AtomicInteger(2));
        maps.setMappedImageHeight(new AtomicInteger(2));
        
        AtomicIntegerArray inputMap = new AtomicIntegerArray(5);
        inputMap.set(0, 3);
        
        when(mockLfsParams.getDirStrengthMin()).thenReturn(0.5);
        
        Maps spyMaps = spy(maps);
        doAnswer(invocation -> {
            AtomicReference<Double> dirStrength = invocation.getArgument(1);
            dirStrength.set(0.1);
            return null;
        }).when(spyMaps).average8NbrDir(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any());
        
        spyMaps.smoothInputBlockImageMap(inputMap, mockDirToRad, mockLfsParams);
        
        assertEquals(3, inputMap.get(0));
    }

    @Test
    void genNMapNoValidNeighbors() {
        AtomicIntegerArray nMap = new AtomicIntegerArray(4);
        AtomicIntegerArray inputMap = new AtomicIntegerArray(4);
        
        Maps spyMaps = spy(maps);
        doReturn(0).when(spyMaps).numValid8Nbrs(any(), anyInt(), anyInt(), anyInt(), anyInt());
        
        int result = spyMaps.genNMap(nMap, inputMap, 2, 2, mockLfsParams);
        
        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.NO_VALID_NBRS, nMap.get(0));
    }

    @Test
    void genNMapInvalidDirHighVorticity() {
        AtomicIntegerArray nMap = new AtomicIntegerArray(4);
        AtomicIntegerArray inputMap = new AtomicIntegerArray(4);
        inputMap.set(0, ILfs.INVALID_DIR);
        
        when(mockLfsParams.getVortValidNbrMin()).thenReturn(3);
        when(mockLfsParams.getHighcurvVorticityMin()).thenReturn(5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);
        
        Maps spyMaps = spy(maps);
        doReturn(5).when(spyMaps).numValid8Nbrs(any(), anyInt(), anyInt(), anyInt(), anyInt());
        doReturn(8).when(spyMaps).vorticity(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        
        int result = spyMaps.genNMap(nMap, inputMap, 2, 2, mockLfsParams);
        
        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.HIGH_CURVATURE, nMap.get(0));
    }

    @Test
    void genNMapValidDirHighCurvature() {
        AtomicIntegerArray nMap = new AtomicIntegerArray(4);
        AtomicIntegerArray inputMap = new AtomicIntegerArray(4);
        inputMap.set(0, 8);
        
        when(mockLfsParams.getHighcurvCurvatureMin()).thenReturn(5);
        when(mockLfsParams.getNumDirections()).thenReturn(16);
        
        Maps spyMaps = spy(maps);
        doReturn(6).when(spyMaps).numValid8Nbrs(any(), anyInt(), anyInt(), anyInt(), anyInt());
        doReturn(10).when(spyMaps).curvature(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        
        int result = spyMaps.genNMap(nMap, inputMap, 2, 2, mockLfsParams);
        
        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.HIGH_CURVATURE, nMap.get(0));
    }

    @Test
    void genNMapValidDirAcceptableCurvature() {
        AtomicIntegerArray nMap = new AtomicIntegerArray(4);
        AtomicIntegerArray inputMap = new AtomicIntegerArray(4);
        inputMap.set(0, 6);
        
        when(mockLfsParams.getHighcurvCurvatureMin()).thenReturn(10);
        when(mockLfsParams.getNumDirections()).thenReturn(16);
        
        Maps spyMaps = spy(maps);
        doReturn(4).when(spyMaps).numValid8Nbrs(any(), anyInt(), anyInt(), anyInt(), anyInt());
        doReturn(3).when(spyMaps).curvature(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        
        int result = spyMaps.genNMap(nMap, inputMap, 2, 2, mockLfsParams);
        
        assertEquals(ILfs.FALSE, result);
        assertEquals(6, nMap.get(0));
    }

    /**
     * Validates genImageMaps end-to-end on a real fingerprint region using the Version 2
     * LFS parameters, exactly as Detect.lfsDetectMinutiaeV2 prepares its inputs.
     * All four maps must be consistent: margins and low contrast blocks invalid, flags binary.
     */
    @Test
    void genImageMapsOnRealFingerprintProducesConsistentMaps() throws Exception {
        Nist.setShowLogs(false);
        int size = 160;
        MapInputs in = prepareV2(cropFingerprint(size), size, size);

        int result = maps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad, in.dftWaves,
                in.dftGrids, in.lfsParams);

        assertEquals(ILfs.FALSE, result);
        int mw = maps.getMappedImageWidth().get();
        int mh = maps.getMappedImageHeight().get();
        assertEquals(size / ILfs.MAP_BLOCKSIZE_V2, mw);
        assertEquals(size / ILfs.MAP_BLOCKSIZE_V2, mh);
        assertMapInvariants(mw, mh);
        assertTrue(countValid(maps.getDirectionMap()) > (mw * mh) / 2,
                "most interior blocks of a fingerprint centre should have a ridge direction");
    }

    /**
     * Validates genImageMaps with logging enabled on a fingerprint patch framed by a white
     * background, so both low contrast blocks and DFT-analysed blocks are produced.
     */
    @Test
    void genImageMapsWithLogsOnFramedFingerprintFlagsLowContrastBackground() throws Exception {
        Nist.setShowLogs(true);
        int size = 96;
        int patch = 48;
        int[] image = new int[size * size];
        java.util.Arrays.fill(image, 255);
        int[] crop = cropFingerprint(patch);
        int offset = (size - patch) / 2;
        for (int y = 0; y < patch; y++) {
            System.arraycopy(crop, y * patch, image, (y + offset) * size + offset, patch);
        }
        MapInputs in = prepareV2(image, size, size);

        int result = maps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad, in.dftWaves,
                in.dftGrids, in.lfsParams);

        assertEquals(ILfs.FALSE, result);
        int mw = maps.getMappedImageWidth().get();
        int mh = maps.getMappedImageHeight().get();
        assertMapInvariants(mw, mh);
        assertTrue(countValue(maps.getLowContrastMap(), ILfs.TRUE) > 0, "white frame must be low contrast");
        assertTrue(countValid(maps.getDirectionMap()) > 0, "fingerprint patch must yield directions");
    }

    /**
     * Validates genImageMaps on a uniformly white image: every block is low contrast,
     * so no direction, low flow or high curvature is reported anywhere.
     */
    @Test
    void genImageMapsOnBlankImageMarksEveryBlockLowContrast() {
        Nist.setShowLogs(false);
        int size = 64;
        int[] image = new int[size * size];
        java.util.Arrays.fill(image, 255);
        MapInputs in = prepareV2(image, size, size);

        int result = maps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad, in.dftWaves,
                in.dftGrids, in.lfsParams);

        assertEquals(ILfs.FALSE, result);
        int blocks = maps.getMappedImageWidth().get() * maps.getMappedImageHeight().get();
        assertEquals(64, blocks);
        assertEquals(blocks, countValue(maps.getLowContrastMap(), ILfs.TRUE));
        assertEquals(blocks, countValue(maps.getDirectionMap(), ILfs.INVALID_DIR));
        assertEquals(0, countValue(maps.getLowFlowMap(), ILfs.TRUE));
        assertEquals(0, countValue(maps.getHighCurveMap(), ILfs.TRUE));
    }

    /**
     * Validates genImageMaps propagates the block offset error when the unpadded image
     * is smaller than a single map block.
     */
    @Test
    void genImageMapsImageSmallerThanBlockReturnsBlockOffsetError() {
        int size = 64;
        MapInputs in = prepareV2(new int[size * size], size, size);
        int tinyPadded = (in.dftGrids.getPad() << 1) + ILfs.MAP_BLOCKSIZE_V2 - 1;

        int result = maps.genImageMaps(in.padded, tinyPadded, tinyPadded, in.dirToRad, in.dftWaves, in.dftGrids,
                in.lfsParams);

        assertEquals(ILfs.ERROR_CODE_80, result);
    }

    /**
     * Validates genImageMaps stops and returns the error code when initial map generation fails.
     */
    @Test
    void genImageMapsPropagatesInitialiseMapsError() {
        MapInputs in = prepareV2(new int[48 * 48], 48, 48);
        Maps spyMaps = spy(maps);
        doReturn(-11).when(spyMaps).initialiseMaps(any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt(),
                anyInt(), any(), any(), any());

        assertEquals(-11, spyMaps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad,
                in.dftWaves, in.dftGrids, in.lfsParams));
    }

    /**
     * Validates genImageMaps stops and returns the error code when low flow map morphing fails.
     */
    @Test
    void genImageMapsPropagatesMorphError() {
        Nist.setShowLogs(false);
        MapInputs in = prepareV2(new int[48 * 48], 48, 48);
        Maps spyMaps = spy(maps);
        doReturn(-12).when(spyMaps).morphMapWithTF(any(), any());

        assertEquals(-12, spyMaps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad,
                in.dftWaves, in.dftGrids, in.lfsParams));
    }

    /**
     * Validates genImageMaps stops and returns the error code when direction interpolation fails.
     */
    @Test
    void genImageMapsPropagatesInterpolationError() {
        Nist.setShowLogs(false);
        MapInputs in = prepareV2(new int[48 * 48], 48, 48);
        Maps spyMaps = spy(maps);
        doReturn(-13).when(spyMaps).interpolateDirectionMap(any(), any(), anyInt(), anyInt(), any());

        assertEquals(-13, spyMaps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad,
                in.dftWaves, in.dftGrids, in.lfsParams));
    }

    /**
     * Validates genImageMaps returns the error code when high curvature map generation fails.
     */
    @Test
    void genImageMapsPropagatesHighCurveError() {
        Nist.setShowLogs(false);
        MapInputs in = prepareV2(new int[48 * 48], 48, 48);
        Maps spyMaps = spy(maps);
        doReturn(-14).when(spyMaps).generateHighCurveMap(any(), any(), anyInt(), anyInt(), any());

        assertEquals(-14, spyMaps.genImageMaps(in.padded, in.paddedWidth, in.paddedHeight, in.dirToRad,
                in.dftWaves, in.dftGrids, in.lfsParams));
    }

    /**
     * Validates initialiseMaps returns the allocation error and leaves the maps initialised
     * to INVALID / FALSE when DFT power vectors cannot be allocated.
     */
    @Test
    void initialiseMapsReturnsAllocDirPowersError() {
        Init initMock = mock(Init.class);
        doAnswer(failWith(-21)).when(initMock).allocDirPowers(any(), anyInt(), anyInt());
        Maps spyMaps = spy(maps);
        doReturn(initMock).when(spyMaps).getInit();
        AtomicIntegerArray dirMap = new AtomicIntegerArray(new int[] { 3, 3 });
        AtomicIntegerArray lcMap = new AtomicIntegerArray(new int[] { 1, 1 });
        AtomicIntegerArray lfMap = new AtomicIntegerArray(new int[] { 1, 1 });

        int result = spyMaps.initialiseMaps(dirMap, lcMap, lfMap, new AtomicIntegerArray(2), 2, 1, new int[100], 10,
                10, mockDftWaves, mockDftGrids, mockLfsParams);

        assertEquals(-21, result);
        assertEquals(ILfs.INVALID_DIR, dirMap.get(0));
        assertEquals(ILfs.FALSE, lcMap.get(1));
        assertEquals(ILfs.FALSE, lfMap.get(0));
    }

    /**
     * Validates initialiseMaps returns the error code produced by DFT power computation.
     */
    @Test
    void initialiseMapsReturnsDftDirPowersError() {
        Dft dftMock = mock(Dft.class);
        when(dftMock.dftDirPowers(any(), any(), anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(-22);
        Maps spyMaps = spyWithContrastingBlocks(dftMock);

        int result = spyMaps.initialiseMaps(new AtomicIntegerArray(1), new AtomicIntegerArray(1),
                new AtomicIntegerArray(1), new AtomicIntegerArray(1), 1, 1, new int[100], 10, 10, mockDftWaves,
                mockDftGrids, Globals.getInstance().getLfsParamsV2());

        assertEquals(-22, result);
    }

    /**
     * Validates initialiseMaps returns the error code produced by DFT power statistics.
     */
    @Test
    void initialiseMapsReturnsDftPowerStatsError() {
        Dft dftMock = mock(Dft.class);
        when(dftMock.dftDirPowers(any(), any(), anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(0);
        when(dftMock.getDftPowerStats(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(-23);
        Maps spyMaps = spyWithContrastingBlocks(dftMock);

        int result = spyMaps.initialiseMaps(new AtomicIntegerArray(1), new AtomicIntegerArray(1),
                new AtomicIntegerArray(1), new AtomicIntegerArray(1), 1, 1, new int[100], 10, 10, mockDftWaves,
                mockDftGrids, Globals.getInstance().getLfsParamsV2());

        assertEquals(-23, result);
    }

    /**
     * Validates initialiseMaps falls back to the secondary fork test: the first block passes
     * the fork test and receives its direction, the second fails and is flagged LOW FLOW.
     */
    @Test
    void initialiseMapsUsesForkTestAndFlagsLowFlow() {
        Nist.setShowLogs(false);
        Maps spyMaps = spyWithContrastingBlocks(forkDftMock());
        AtomicIntegerArray dirMap = new AtomicIntegerArray(2);
        AtomicIntegerArray lcMap = new AtomicIntegerArray(2);
        AtomicIntegerArray lfMap = new AtomicIntegerArray(2);

        int result = spyMaps.initialiseMaps(dirMap, lcMap, lfMap, new AtomicIntegerArray(2), 2, 1, new int[100], 10,
                10, mockDftWaves, mockDftGrids, Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.FALSE, result);
        assertEquals(4, dirMap.get(0));
        assertEquals(ILfs.FALSE, lfMap.get(0));
        assertEquals(ILfs.INVALID_DIR, dirMap.get(1));
        assertEquals(ILfs.TRUE, lfMap.get(1));
        assertEquals(ILfs.FALSE, lcMap.get(1));
    }

    /**
     * Validates initialiseInputBlockImageMap (Version 1 IMAP) on a real fingerprint region:
     * one direction per 24x24 block, all within the valid direction range.
     */
    @Test
    void initialiseInputBlockImageMapOnRealFingerprint() throws Exception {
        Nist.setShowLogs(false);
        int size = 144;
        MapInputs in = prepareV1(cropFingerprint(size), size, size);
        AtomicInteger ret = new AtomicInteger(-1);
        AtomicInteger mw = new AtomicInteger();
        AtomicInteger mh = new AtomicInteger();
        AtomicIntegerArray offsets = Block.getInstance().blockOffsets(ret, mw, mh, size, size, in.dftGrids.getPad(),
                in.dftGrids.getGridWidth());
        assertEquals(ILfs.FALSE, ret.get());

        AtomicIntegerArray imap = maps.initialiseInputBlockImageMap(ret, offsets, mw, mh, in.padded,
                in.paddedWidth, in.paddedHeight, in.dftWaves, in.dftGrids, in.lfsParams);

        assertEquals(ILfs.FALSE, ret.get());
        assertEquals(6, mw.get());
        assertEquals(6, mh.get());
        assertEquals(36, imap.length());
        assertDirectionsInRange(imap);
    }

    /**
     * Validates generateInputBlockImageMap on a real fingerprint region returns the IMAP
     * together with its block dimensions. Smoothing is stubbed out because it reads one
     * element past the end of an exactly sized map.
     */
    @Test
    void generateInputBlockImageMapOnRealFingerprint() throws Exception {
        Nist.setShowLogs(false);
        int size = 144;
        MapInputs in = prepareV1(cropFingerprint(size), size, size);
        Maps spyMaps = spy(maps);
        spyMaps.setMappedImageWidth(new AtomicInteger(6));
        spyMaps.setMappedImageHeight(new AtomicInteger(6));
        doNothing().when(spyMaps).smoothInputBlockImageMap(any(), any(), any());
        AtomicInteger ret = new AtomicInteger(-1);
        AtomicInteger mw = new AtomicInteger();
        AtomicInteger mh = new AtomicInteger();

        AtomicIntegerArray imap = spyMaps.generateInputBlockImageMap(ret, mw, mh, in.padded, in.paddedWidth,
                in.paddedHeight, in.dirToRad, in.dftWaves, in.dftGrids, in.lfsParams);

        assertEquals(ILfs.FALSE, ret.get());
        assertEquals(6, mw.get());
        assertEquals(6, mh.get());
        assertEquals(36, imap.length());
        assertDirectionsInRange(imap);
    }

    /**
     * Validates generateInputBlockImageMap returns null and propagates the error when
     * initial IMAP generation fails.
     */
    @Test
    void generateInputBlockImageMapPropagatesInitialiseError() {
        when(mockDftGrids.getGridWidth()).thenReturn(8);
        when(mockDftGrids.getGridHeight()).thenReturn(8);
        when(mockDftGrids.getPad()).thenReturn(0);
        Maps spyMaps = spy(maps);
        doAnswer(failWith(-41)).when(spyMaps).initialiseInputBlockImageMap(any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any(), any(), any());
        AtomicInteger ret = new AtomicInteger();

        AtomicIntegerArray result = spyMaps.generateInputBlockImageMap(ret, new AtomicInteger(), new AtomicInteger(),
                new int[256], 16, 16, mockDirToRad, mockDftWaves, mockDftGrids, mockLfsParams);

        assertNull(result);
        assertEquals(-41, ret.get());
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when DFT power vectors cannot be allocated.
     */
    @Test
    void initialiseInputBlockImageMapAllocDirPowersError() {
        assertInitialiseInputBlockImageMapFails(
                init -> doAnswer(failWith(-31)).when(init).allocDirPowers(any(), anyInt(), anyInt()), -31);
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when the wave index vector cannot be allocated.
     */
    @Test
    void initialiseInputBlockImageMapAllocWisError() {
        assertInitialiseInputBlockImageMapFails(
                init -> doAnswer(failWith(-32)).when(init).allocPowerStatsWis(any(), anyInt()), -32);
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when the max power vector cannot be allocated.
     */
    @Test
    void initialiseInputBlockImageMapAllocPowmaxsError() {
        assertInitialiseInputBlockImageMapFails(
                init -> doAnswer(failWith(-33)).when(init).allocPowerStatsPowmaxs(any(), anyInt()), -33);
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when the max power direction vector
     * cannot be allocated.
     */
    @Test
    void initialiseInputBlockImageMapAllocPowmaxDirsError() {
        assertInitialiseInputBlockImageMapFails(
                init -> doAnswer(failWith(-34)).when(init).allocPowerStatsPowmaxDirs(any(), anyInt()), -34);
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when the normalized power vector
     * cannot be allocated.
     */
    @Test
    void initialiseInputBlockImageMapAllocPownormsError() {
        assertInitialiseInputBlockImageMapFails(
                init -> doAnswer(failWith(-35)).when(init).allocPowerStatsPownorms(any(), anyInt()), -35);
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when DFT power computation fails.
     */
    @Test
    void initialiseInputBlockImageMapDftDirPowersError() {
        Dft dftMock = mock(Dft.class);
        when(dftMock.dftDirPowers(any(), any(), anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(-36);
        Maps spyMaps = spy(maps);
        doReturn(dftMock).when(spyMaps).getDft();
        AtomicInteger ret = new AtomicInteger();

        AtomicIntegerArray result = spyMaps.initialiseInputBlockImageMap(ret, new AtomicIntegerArray(1),
                new AtomicInteger(1), new AtomicInteger(1), new int[64], 8, 8, fourWaves(), sixteenGrids(),
                mockLfsParams);

        assertNull(result);
        assertEquals(-36, ret.get());
    }

    /**
     * Validates initialiseInputBlockImageMap returns null when DFT power statistics fail.
     */
    @Test
    void initialiseInputBlockImageMapDftPowerStatsError() {
        Dft dftMock = mock(Dft.class);
        when(dftMock.dftDirPowers(any(), any(), anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(0);
        when(dftMock.getDftPowerStats(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(-37);
        Maps spyMaps = spy(maps);
        doReturn(dftMock).when(spyMaps).getDft();
        AtomicInteger ret = new AtomicInteger();

        AtomicIntegerArray result = spyMaps.initialiseInputBlockImageMap(ret, new AtomicIntegerArray(1),
                new AtomicInteger(1), new AtomicInteger(1), new int[64], 8, 8, fourWaves(), sixteenGrids(),
                mockLfsParams);

        assertNull(result);
        assertEquals(-37, ret.get());
    }

    /**
     * Validates initialiseInputBlockImageMap assigns the fork test direction to a block whose
     * primary test fails but whose secondary fork test passes.
     */
    @Test
    void initialiseInputBlockImageMapUsesForkTest() {
        Nist.setShowLogs(false);
        Maps spyMaps = spy(maps);
        doReturn(forkDftMock()).when(spyMaps).getDft();
        AtomicInteger ret = new AtomicInteger(-1);

        AtomicIntegerArray imap = spyMaps.initialiseInputBlockImageMap(ret, new AtomicIntegerArray(2),
                new AtomicInteger(2), new AtomicInteger(1), new int[64], 8, 8, fourWaves(), sixteenGrids(),
                Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.FALSE, ret.get());
        assertEquals(4, imap.get(0));
    }

    /**
     * Validates the sized getInstance and map-based getInstance factories create a new
     * singleton only when none exists; the original singleton is restored afterwards.
     */
    @Test
    void getInstanceFactoriesCreateInstanceOnlyWhenAbsent() {
        Maps original = maps;
        try {
            Maps.resetInstance();
            Maps sized = Maps.getInstance(3, 4);
            assertNotSame(original, sized);
            assertEquals(3, sized.getMappedImageWidth().get());
            assertEquals(4, sized.getMappedImageHeight().get());
            assertEquals(12, sized.getDirectionMap().length());
            assertEquals(12, sized.getLowContrastMap().length());
            assertEquals(12, sized.getLowFlowMap().length());
            assertEquals(12, sized.getHighCurveMap().length());
            assertSame(sized, Maps.getInstance(7, 7));

            Maps.resetInstance();
            AtomicIntegerArray dirMap = new AtomicIntegerArray(2);
            AtomicIntegerArray lcMap = new AtomicIntegerArray(2);
            AtomicIntegerArray lfMap = new AtomicIntegerArray(2);
            AtomicIntegerArray hcMap = new AtomicIntegerArray(2);
            Maps fromMaps = Maps.getInstance(dirMap, lcMap, lfMap, hcMap);
            assertSame(dirMap, fromMaps.getDirectionMap());
            assertSame(lcMap, fromMaps.getLowContrastMap());
            assertSame(lfMap, fromMaps.getLowFlowMap());
            assertSame(hcMap, fromMaps.getHighCurveMap());
        } finally {
            ReflectionTestUtils.setField(Maps.class, "instance", original);
        }
        assertSame(original, Maps.getInstance());
    }

    /**
     * Validates pixelizeMap copies each block value to every pixel of that block.
     */
    @Test
    void pixelizeMapAssignsBlockValueToEveryPixel() {
        AtomicIntegerArray pixelMap = new AtomicIntegerArray(16);
        AtomicIntegerArray blockMap = new AtomicIntegerArray(new int[] { 1, 2, 3, 4 });

        assertEquals(ILfs.FALSE, maps.pixelizeMap(pixelMap, 4, 4, blockMap, 2, 2, 2));

        int[] expected = { 1, 1, 2, 2, 1, 1, 2, 2, 3, 3, 4, 4, 3, 3, 4, 4 };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], pixelMap.get(i), "pixel " + i);
        }
    }

    /**
     * Validates pixelizeMap propagates the block offset error for an image smaller than a block.
     */
    @Test
    void pixelizeMapImageSmallerThanBlockReturnsError() {
        assertEquals(ILfs.ERROR_CODE_80,
                maps.pixelizeMap(new AtomicIntegerArray(4), 2, 2, new AtomicIntegerArray(1), 1, 1, 4));
    }

    /**
     * Validates pixelizeMap rejects a block map whose width matches but whose height does not.
     */
    @Test
    void pixelizeMapHeightMismatchReturnsError() {
        assertEquals(ILfs.ERROR_CODE_591,
                maps.pixelizeMap(new AtomicIntegerArray(24), 4, 6, new AtomicIntegerArray(4), 2, 2, 2));
    }

    /**
     * Validates morphMapWithTF closes a single-block hole inside a region of TRUE blocks.
     */
    @Test
    void morphMapWithTFFillsInteriorHole() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(7));
        maps.setMappedImageHeight(new AtomicInteger(7));
        AtomicIntegerArray tfMap = new AtomicIntegerArray(49);
        for (int i = 0; i < 49; i++) {
            tfMap.set(i, ILfs.TRUE);
        }
        tfMap.set(24, ILfs.FALSE);

        assertEquals(ILfs.FALSE, maps.morphMapWithTF(tfMap, mockLfsParams));

        assertEquals(ILfs.TRUE, tfMap.get(24));
    }

    /**
     * Validates interpolateDirectionMap weights neighbours inversely to their distance:
     * nearer neighbours dominate the interpolated direction.
     */
    @Test
    void interpolateDirectionMapWeightsNeighboursByDistance() {
        Nist.setShowLogs(false);
        AtomicIntegerArray dirMap = new AtomicIntegerArray(new int[] { 4, ILfs.INVALID_DIR, ILfs.INVALID_DIR, 10 });
        AtomicIntegerArray lcMap = new AtomicIntegerArray(4);

        int result = maps.interpolateDirectionMap(dirMap, lcMap, 1, 4, Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.FALSE, result);
        assertEquals(4, dirMap.get(0));
        assertEquals(6, dirMap.get(1));
        assertEquals(8, dirMap.get(2));
        assertEquals(10, dirMap.get(3));
    }

    /**
     * Validates interpolateDirectionMap does not search through low contrast blocks and leaves
     * blocks with too few valid neighbours INVALID.
     */
    @Test
    void interpolateDirectionMapStopsAtLowContrastBlocks() {
        Nist.setShowLogs(true);
        AtomicIntegerArray dirMap = new AtomicIntegerArray(
                new int[] { 4, ILfs.INVALID_DIR, ILfs.INVALID_DIR, 10 });
        AtomicIntegerArray lcMap = new AtomicIntegerArray(new int[] { 0, 0, 1, 0 });

        int result = maps.interpolateDirectionMap(dirMap, lcMap, 1, 4, Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.FALSE, result);
        assertEquals(ILfs.INVALID_DIR, dirMap.get(1));
        assertEquals(ILfs.INVALID_DIR, dirMap.get(2));
    }

    /**
     * Validates interpolateDirectionMap combines neighbours found in all four directions.
     */
    @Test
    void interpolateDirectionMapUsesAllFourNeighbours() {
        Nist.setShowLogs(false);
        AtomicIntegerArray dirMap = filledMap(9, 5);
        dirMap.set(4, ILfs.INVALID_DIR);

        maps.interpolateDirectionMap(dirMap, new AtomicIntegerArray(9), 3, 3, Globals.getInstance().getLfsParamsV2());

        assertEquals(5, dirMap.get(4));
    }

    /**
     * Validates removeInconsistentDirs removes directions that disagree with their neighbours,
     * at the centre, at a corner and on the bottom edge of the map, while keeping consistent ones.
     */
    @Test
    void removeInconsistentDirsRemovesOutliers() {
        Nist.setShowLogs(true);
        maps.setMappedImageWidth(new AtomicInteger(5));
        maps.setMappedImageHeight(new AtomicInteger(5));
        AtomicIntegerArray dirMap = filledMap(25, 4);
        dirMap.set(12, 12);
        dirMap.set(0, 12);
        dirMap.set(22, 12);

        maps.removeInconsistentDirs(dirMap, realDirToRad(), Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.INVALID_DIR, dirMap.get(12));
        assertEquals(ILfs.INVALID_DIR, dirMap.get(0));
        assertEquals(ILfs.INVALID_DIR, dirMap.get(22));
        assertEquals(22, countValue(dirMap, 4));
    }

    /**
     * Validates removeInconsistentDirs on a wide, non-square map where the concentric boxes
     * outgrow the top and bottom edges before the left and right ones. Removing the two edge
     * outliers leaves the four corners with too few valid neighbours, so later passes remove them.
     */
    @Test
    void removeInconsistentDirsOnWideMapCascadesToCorners() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(7));
        maps.setMappedImageHeight(new AtomicInteger(3));
        AtomicIntegerArray dirMap = filledMap(21, 4);
        dirMap.set(13, 12);
        dirMap.set(7, 12);

        maps.removeInconsistentDirs(dirMap, realDirToRad(), Globals.getInstance().getLfsParamsV2());

        for (int index : new int[] { 7, 13, 0, 6, 14, 20 }) {
            assertEquals(ILfs.INVALID_DIR, dirMap.get(index), "block " + index);
        }
        assertEquals(15, countValue(dirMap, 4));
    }

    /**
     * Validates removeInconsistentDirs removes an isolated direction lacking enough valid neighbours.
     */
    @Test
    void removeInconsistentDirsRemovesIsolatedDirection() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(5));
        AtomicIntegerArray dirMap = filledMap(15, ILfs.INVALID_DIR);
        dirMap.set(7, 6);

        maps.removeInconsistentDirs(dirMap, realDirToRad(), Globals.getInstance().getLfsParamsV2());

        assertEquals(15, countValue(dirMap, ILfs.INVALID_DIR));
    }

    /**
     * Validates removeIMAPDirection return codes: TRUE for too few neighbours, 2 for a direction
     * too far from the neighbour average, FALSE for a consistent direction.
     */
    @Test
    void removeIMAPDirectionReturnCodes() {
        Nist.setShowLogs(true);
        DirToRad dirToRad = realDirToRad();
        LfsParams params = Globals.getInstance().getLfsParamsV2();

        AtomicIntegerArray sparse = filledMap(9, ILfs.INVALID_DIR);
        sparse.set(4, 4);
        sparse.set(0, 4);
        assertEquals(ILfs.TRUE, maps.removeIMAPDirection(sparse, 1, 1, 3, 3, dirToRad, params));

        AtomicIntegerArray inconsistent = filledMap(9, 4);
        inconsistent.set(4, 12);
        assertEquals(2, maps.removeIMAPDirection(inconsistent, 1, 1, 3, 3, dirToRad, params));

        Nist.setShowLogs(false);
        assertEquals(2, maps.removeIMAPDirection(inconsistent, 1, 1, 3, 3, dirToRad, params));
        assertEquals(ILfs.TRUE, maps.removeIMAPDirection(sparse, 1, 1, 3, 3, dirToRad, params));
        assertEquals(ILfs.FALSE, maps.removeIMAPDirection(filledMap(9, 4), 1, 1, 3, 3, dirToRad, params));
    }

    /**
     * Validates removeIMAPDirection keeps a direction when the neighbour average is too weak
     * (opposing neighbour directions cancel out).
     */
    @Test
    void removeIMAPDirectionKeepsDirectionWhenAverageIsWeak() {
        AtomicIntegerArray map = opposingNeighbourMap();

        assertEquals(ILfs.FALSE, maps.removeIMAPDirection(map, 1, 1, 3, 3, realDirToRad(),
                Globals.getInstance().getLfsParamsV2()));
    }

    /**
     * Validates average8NbrDir averages all eight neighbours of an interior block.
     */
    @Test
    void average8NbrDirInteriorBlock() {
        AtomicInteger avg = new AtomicInteger();
        AtomicReference<Double> strength = new AtomicReference<>();
        AtomicInteger valid = new AtomicInteger();

        maps.average8NbrDir(avg, strength, valid, filledMap(9, 6), 1, 1, 3, 3, realDirToRad());

        assertEquals(6, avg.get());
        assertEquals(8, valid.get());
        assertEquals(1.0, strength.get(), 1e-3);
    }

    /**
     * Validates average8NbrDir only considers in-bounds neighbours for corner blocks.
     */
    @Test
    void average8NbrDirCornerBlocks() {
        AtomicInteger avg = new AtomicInteger();
        AtomicReference<Double> strength = new AtomicReference<>();
        AtomicInteger valid = new AtomicInteger();
        DirToRad dirToRad = realDirToRad();

        maps.average8NbrDir(avg, strength, valid, filledMap(9, 2), 0, 0, 3, 3, dirToRad);
        assertEquals(3, valid.get());
        assertEquals(2, avg.get());

        maps.average8NbrDir(avg, strength, valid, filledMap(9, 2), 2, 2, 3, 3, dirToRad);
        assertEquals(3, valid.get());
        assertEquals(2, avg.get());
    }

    /**
     * Validates average8NbrDir reports INVALID with zero strength when no neighbour is valid.
     */
    @Test
    void average8NbrDirNoValidNeighbours() {
        AtomicInteger avg = new AtomicInteger();
        AtomicReference<Double> strength = new AtomicReference<>();
        AtomicInteger valid = new AtomicInteger(99);
        AtomicIntegerArray map = filledMap(9, ILfs.INVALID_DIR);
        map.set(4, 3);

        maps.average8NbrDir(avg, strength, valid, map, 1, 1, 3, 3, realDirToRad());

        assertEquals(ILfs.INVALID_DIR, avg.get());
        assertEquals(0, valid.get());
        assertEquals(0.0, strength.get());
    }

    /**
     * Validates average8NbrDir reports INVALID when opposing neighbour directions cancel out,
     * while still reporting how many neighbours were valid.
     */
    @Test
    void average8NbrDirOpposingNeighboursAreTooWeak() {
        AtomicInteger avg = new AtomicInteger();
        AtomicReference<Double> strength = new AtomicReference<>();
        AtomicInteger valid = new AtomicInteger();

        maps.average8NbrDir(avg, strength, valid, opposingNeighbourMap(), 1, 1, 3, 3, realDirToRad());

        assertEquals(ILfs.INVALID_DIR, avg.get());
        assertEquals(8, valid.get());
        assertEquals(0.0, strength.get());
    }

    /**
     * Validates smoothDirectionMap without logging replaces valid and invalid directions with
     * the neighbour average when enough valid neighbours exist.
     */
    @Test
    void smoothDirectionMapReplacesWithNeighbourAverage() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));
        AtomicIntegerArray dirMap = filledMap(9, 4);
        dirMap.set(4, 6);

        maps.smoothDirectionMap(dirMap, onlyCentreHasContrast(), realDirToRad(),
                Globals.getInstance().getLfsParamsV2());
        assertEquals(4, dirMap.get(4));

        dirMap.set(4, ILfs.INVALID_DIR);
        maps.smoothDirectionMap(dirMap, onlyCentreHasContrast(), realDirToRad(),
                Globals.getInstance().getLfsParamsV2());
        assertEquals(4, dirMap.get(4));
    }

    /**
     * Validates smoothDirectionMap leaves blocks unchanged when they are low contrast, when the
     * neighbour average is weak, or when too few valid neighbours exist.
     */
    @Test
    void smoothDirectionMapKeepsDirectionWhenConditionsNotMet() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));
        DirToRad dirToRad = realDirToRad();
        LfsParams params = Globals.getInstance().getLfsParamsV2();

        AtomicIntegerArray lowContrast = filledMap(9, 4);
        lowContrast.set(4, ILfs.INVALID_DIR);
        AtomicIntegerArray lcMap = new AtomicIntegerArray(9);
        lcMap.set(4, ILfs.TRUE);
        maps.smoothDirectionMap(lowContrast, lcMap, dirToRad, params);
        assertEquals(ILfs.INVALID_DIR, lowContrast.get(4));

        AtomicIntegerArray weak = opposingNeighbourMap();
        maps.smoothDirectionMap(weak, onlyCentreHasContrast(), dirToRad, params);
        assertEquals(2, weak.get(4));

        AtomicIntegerArray sparseValid = filledMap(9, ILfs.INVALID_DIR);
        sparseValid.set(4, 7);
        sparseValid.set(1, 4);
        sparseValid.set(3, 4);
        maps.smoothDirectionMap(sparseValid, onlyCentreHasContrast(), dirToRad, params);
        assertEquals(7, sparseValid.get(4));

        AtomicIntegerArray sparseInvalid = filledMap(9, 4);
        sparseInvalid.set(4, ILfs.INVALID_DIR);
        sparseInvalid.set(0, ILfs.INVALID_DIR);
        sparseInvalid.set(2, ILfs.INVALID_DIR);
        maps.smoothDirectionMap(sparseInvalid, onlyCentreHasContrast(), dirToRad, params);
        assertEquals(ILfs.INVALID_DIR, sparseInvalid.get(4));
    }

    /** 3x3 low contrast map in which only the centre block has sufficient contrast. */
    private static AtomicIntegerArray onlyCentreHasContrast() {
        AtomicIntegerArray lcMap = filledMap(9, ILfs.TRUE);
        lcMap.set(4, ILfs.FALSE);
        return lcMap;
    }

    /**
     * Validates smoothInputBlockImageMap without logging keeps a valid direction when there are too
     * few valid neighbours and fills an invalid one only when enough neighbours exist.
     * The map is one element longer than the block grid because the routine reads one element ahead.
     */
    @Test
    void smoothInputBlockImageMapNeighbourThresholds() {
        Nist.setShowLogs(false);
        maps.setMappedImageWidth(new AtomicInteger(3));
        maps.setMappedImageHeight(new AtomicInteger(3));
        DirToRad dirToRad = realDirToRad();
        LfsParams params = Globals.getInstance().getLfsParams();

        AtomicIntegerArray sparseValid = filledMap(10, ILfs.INVALID_DIR);
        sparseValid.set(4, 7);
        sparseValid.set(1, 4);
        sparseValid.set(3, 4);
        maps.smoothInputBlockImageMap(sparseValid, dirToRad, params);
        assertEquals(7, sparseValid.get(4));

        AtomicIntegerArray fullInvalidCentre = filledMap(10, 4);
        fullInvalidCentre.set(4, ILfs.INVALID_DIR);
        maps.smoothInputBlockImageMap(fullInvalidCentre, dirToRad, params);
        assertEquals(4, fullInvalidCentre.get(4));

        AtomicIntegerArray sparseInvalid = filledMap(10, ILfs.INVALID_DIR);
        sparseInvalid.set(1, 4);
        sparseInvalid.set(3, 4);
        maps.smoothInputBlockImageMap(sparseInvalid, dirToRad, params);
        assertEquals(ILfs.INVALID_DIR, sparseInvalid.get(4));
    }

    /**
     * Validates generateHighCurveMap leaves blocks unflagged when there are no valid neighbours,
     * too few valid neighbours for vorticity, or when vorticity is low.
     */
    @Test
    void generateHighCurveMapLowVorticityCasesAreNotFlagged() {
        LfsParams params = Globals.getInstance().getLfsParamsV2();

        AtomicIntegerArray allInvalid = filledMap(9, ILfs.INVALID_DIR);
        AtomicIntegerArray hc1 = new AtomicIntegerArray(9);
        assertEquals(ILfs.FALSE, maps.generateHighCurveMap(hc1, allInvalid, 3, 3, params));
        assertEquals(0, countValue(hc1, ILfs.TRUE));

        AtomicIntegerArray fewValid = filledMap(9, ILfs.INVALID_DIR);
        fewValid.set(0, 4);
        fewValid.set(1, 4);
        AtomicIntegerArray hc2 = new AtomicIntegerArray(9);
        maps.generateHighCurveMap(hc2, fewValid, 3, 3, params);
        assertEquals(ILfs.FALSE, hc2.get(4));

        AtomicIntegerArray uniform = filledMap(9, 4);
        uniform.set(4, ILfs.INVALID_DIR);
        AtomicIntegerArray hc3 = new AtomicIntegerArray(9);
        maps.generateHighCurveMap(hc3, uniform, 3, 3, params);
        assertEquals(0, countValue(hc3, ILfs.TRUE));
    }

    /**
     * Validates generateHighCurveMap flags an invalid block surrounded by a full rotation of
     * neighbour directions (a core-like vortex).
     */
    @Test
    void generateHighCurveMapFlagsVortex() {
        AtomicIntegerArray hc = new AtomicIntegerArray(9);

        maps.generateHighCurveMap(hc, vortexMap(ILfs.INVALID_DIR), 3, 3, Globals.getInstance().getLfsParamsV2());

        assertEquals(ILfs.TRUE, hc.get(4));
    }

    /**
     * Validates genNMap marks an invalid block INVALID when it has too few valid neighbours or
     * when their vorticity is low, and HIGH_CURVATURE for a vortex.
     */
    @Test
    void genNMapInvalidBlockClassification() {
        LfsParams params = Globals.getInstance().getLfsParamsV2();

        AtomicIntegerArray fewValid = filledMap(9, ILfs.INVALID_DIR);
        fewValid.set(0, 4);
        AtomicIntegerArray nMap1 = new AtomicIntegerArray(9);
        assertEquals(ILfs.FALSE, maps.genNMap(nMap1, fewValid, 3, 3, params));
        assertEquals(ILfs.INVALID_DIR, nMap1.get(4));
        assertEquals(ILfs.NO_VALID_NBRS, nMap1.get(8));

        AtomicIntegerArray uniform = filledMap(9, 4);
        uniform.set(4, ILfs.INVALID_DIR);
        AtomicIntegerArray nMap2 = new AtomicIntegerArray(9);
        maps.genNMap(nMap2, uniform, 3, 3, params);
        assertEquals(ILfs.INVALID_DIR, nMap2.get(4));
        assertEquals(4, nMap2.get(0));

        AtomicIntegerArray nMap3 = new AtomicIntegerArray(9);
        maps.genNMap(nMap3, vortexMap(ILfs.INVALID_DIR), 3, 3, params);
        assertEquals(ILfs.HIGH_CURVATURE, nMap3.get(4));
    }

    /**
     * Validates vorticity sums clockwise (+1) and counter-clockwise (-1) turns between adjacent
     * neighbours, for the interior block and for corner and edge blocks where neighbours fall
     * outside the map.
     */
    @Test
    void vorticityInteriorEdgeAndCornerBlocks() {
        AtomicIntegerArray map = vortexMap(0);

        assertEquals(8, maps.vorticity(map, 1, 1, 3, 3, NUM_DIRS));
        assertEquals(-2, maps.vorticity(map, 0, 0, 3, 3, NUM_DIRS));
        assertEquals(2, maps.vorticity(map, 2, 2, 3, 3, NUM_DIRS));
        assertEquals(0, maps.vorticity(map, 1, 0, 3, 3, NUM_DIRS));
        assertEquals(4, maps.vorticity(map, 1, 2, 3, 3, NUM_DIRS));
    }

    /**
     * Validates accumulateNbrVorticity ignores equal directions and pairs with an invalid direction.
     */
    @Test
    void accumulateNbrVorticityIgnoresEqualAndInvalidPairs() {
        AtomicInteger measure = new AtomicInteger(5);

        maps.accumulateNbrVorticity(measure, 3, 3, NUM_DIRS);
        maps.accumulateNbrVorticity(measure, ILfs.INVALID_DIR, 3, NUM_DIRS);
        maps.accumulateNbrVorticity(measure, 3, ILfs.INVALID_DIR, NUM_DIRS);

        assertEquals(5, measure.get());
    }

    /**
     * Validates curvature picks up the largest change even when it comes from the last
     * neighbours examined (south-west), and handles corner blocks.
     */
    @Test
    void curvatureFindsMaximumAtSouthWestAndCorners() {
        AtomicIntegerArray map = filledMap(9, 0);
        map.set(6, 8);

        assertEquals(8, maps.curvature(map, 1, 1, 3, 3, NUM_DIRS));
        assertEquals(0, maps.curvature(map, 0, 0, 3, 3, NUM_DIRS));
        assertEquals(0, maps.curvature(map, 2, 2, 3, 3, NUM_DIRS));

        AtomicIntegerArray westMap = filledMap(9, 0);
        westMap.set(3, 6);
        assertEquals(6, maps.curvature(westMap, 1, 1, 3, 3, NUM_DIRS));
    }

    /**
     * Validates primaryDirectionTest without logging accepts the first statistic meeting all three
     * criteria and rejects low normalized power or an excessive low-frequency power.
     */
    @Test
    void primaryDirectionTestCriteria() {
        Nist.setShowLogs(false);
        LfsParams params = Globals.getInstance().getLfsParamsV2();
        double powmax = params.getPowmaxMin() * 2;

        assertEquals(5, maps.primaryDirectionTest(powers(0.0, 0.0, 0.0), wis(0), doubles(powmax), ints(5),
                doubles(params.getPownormMin() + 1), 1, params));
        assertEquals(ILfs.INVALID_DIR, maps.primaryDirectionTest(powers(0.0, 0.0, 0.0), wis(0), doubles(powmax),
                ints(5), doubles(params.getPownormMin() / 2), 1, params));
        assertEquals(ILfs.INVALID_DIR, maps.primaryDirectionTest(powers(params.getPowmaxMax() * 2, 0.0, 0.0),
                wis(0), doubles(powmax), ints(5), doubles(params.getPownormMin() + 1), 1, params));
    }

    /**
     * Validates secondaryForkTest rejects blocks failing any of the first-part criteria.
     */
    @Test
    void secondaryForkTestFirstPartFailures() {
        Nist.setShowLogs(false);
        LfsParams params = Globals.getInstance().getLfsParamsV2();
        double powmax = params.getPowmaxMin() * 2;
        double forkNorm = forkPownorm(params);

        assertEquals(ILfs.INVALID_DIR, maps.secondaryForkTest(powers(0.0, 0.0, 0.0), wis(0),
                doubles(params.getPowmaxMin() / 2), ints(4), doubles(forkNorm), 1, params));
        assertEquals(ILfs.INVALID_DIR, maps.secondaryForkTest(powers(0.0, 0.0, 0.0), wis(0), doubles(powmax),
                ints(4), doubles(forkNorm / 2), 1, params));
        assertEquals(ILfs.INVALID_DIR, maps.secondaryForkTest(powers(params.getPowmaxMax() * 2, 0.0, 0.0), wis(0),
                doubles(powmax), ints(4), doubles(forkNorm), 1, params));
    }

    /**
     * Validates secondaryForkTest requires exactly one of the left/right fork directions to exceed
     * the fork power threshold.
     */
    @Test
    void secondaryForkTestRequiresExactlyOneStrongForkSide() {
        LfsParams params = Globals.getInstance().getLfsParamsV2();
        double powmax = params.getPowmaxMin() * 2;
        double strong = powmax;
        double forkNorm = forkPownorm(params);

        Nist.setShowLogs(false);
        assertEquals(ILfs.INVALID_DIR, maps.secondaryForkTest(powers(0.0, 0.0, 0.0), wis(0), doubles(powmax),
                ints(4), doubles(forkNorm), 1, params));
        assertEquals(ILfs.INVALID_DIR, maps.secondaryForkTest(powers(0.0, strong, strong), wis(0), doubles(powmax),
                ints(4), doubles(forkNorm), 1, params));
        assertEquals(4, maps.secondaryForkTest(powers(0.0, strong, 0.0), wis(0), doubles(powmax), ints(4),
                doubles(forkNorm), 1, params));

        Nist.setShowLogs(true);
        assertEquals(4, maps.secondaryForkTest(powers(0.0, 0.0, strong), wis(0), doubles(powmax), ints(4),
                doubles(forkNorm), 1, params));
    }

    private static final class MapInputs {
        int[] padded;
        int paddedWidth;
        int paddedHeight;
        DirToRad dirToRad;
        DftWaves dftWaves;
        RotGrids dftGrids;
        LfsParams lfsParams;
    }

    private static synchronized void loadFingerprint() throws Exception {
        if (fingerprintImage != null) {
            return;
        }
        String isoFile = "src/test/resources/info_wsq.iso";
        AtomicInteger ret = new AtomicInteger(-1);
        AtomicInteger width = new AtomicInteger();
        AtomicInteger height = new AtomicInteger();
        BufferedImage image = ImageDecoder.getInstance().readAndDecodeGrayscaleImage(ret, isoFile,
                new AtomicInteger(-1), new AtomicInteger(), width, height, new AtomicInteger(), new AtomicInteger(),
                new AtomicReference<>());
        assertEquals(ILfs.FALSE, ret.get());
        fingerprintImage = org.mosip.nist.nfiq1.util.ImageUtil.convertTo1DWithoutUsingGetRGB(image, "jpg");
        fingerprintWidth = width.get();
        fingerprintHeight = height.get();
    }

    private static int[] cropFingerprint(int size) throws Exception {
        loadFingerprint();
        assertTrue(fingerprintWidth >= size && fingerprintHeight >= size);
        int x0 = (fingerprintWidth - size) / 2;
        int y0 = (fingerprintHeight - size) / 2;
        int[] crop = new int[size * size];
        for (int y = 0; y < size; y++) {
            System.arraycopy(fingerprintImage, (y0 + y) * fingerprintWidth + x0, crop, y * size, size);
        }
        return crop;
    }

    private static AtomicReferenceArray<Double> dftCoefs() {
        double[] coefs = Globals.getInstance().getDftCoefs();
        AtomicReferenceArray<Double> result = new AtomicReferenceArray<>(coefs.length);
        for (int i = 0; i < coefs.length; i++) {
            result.set(i, coefs[i]);
        }
        return result;
    }

    private static DirToRad realDirToRad() {
        DirToRad dirToRad = new DirToRad(NUM_DIRS);
        assertEquals(ILfs.FALSE, Init.getInstance().initDirToRad(dirToRad));
        return dirToRad;
    }

    private static MapInputs prepareV2(int[] image, int width, int height) {
        LfsParams params = Globals.getInstance().getLfsParamsV2();
        int maxPad = Init.getInstance().getMaxPaddingV2(params.getWindowSize(), params.getWindowOffset(),
                params.getDirbinGridWidth(), params.getDirbinGridHeight());
        return prepare(image, width, height, params, params.getWindowSize(), maxPad);
    }

    private static MapInputs prepareV1(int[] image, int width, int height) {
        LfsParams params = Globals.getInstance().getLfsParams();
        int maxPad = Init.getInstance().getMaxPadding(params.getBlockOffsetSize(), params.getDirbinGridWidth(),
                params.getDirbinGridHeight(), params.getIsoBinGridDim());
        return prepare(image, width, height, params, params.getBlockOffsetSize(), maxPad);
    }

    private static MapInputs prepare(int[] image, int width, int height, LfsParams params, int gridSize,
            int maxPad) {
        Init init = Init.getInstance();
        MapInputs in = new MapInputs();
        in.lfsParams = params;
        in.dirToRad = realDirToRad();
        in.dftWaves = new DftWaves(params.getNumDftWaves(), gridSize);
        assertEquals(ILfs.FALSE, init.initDftWaves(in.dftWaves, dftCoefs()));
        in.dftGrids = new RotGrids(params.getStartDirAngle(), params.getNumDirections(), gridSize, gridSize,
                ILfs.RELATIVE_TO_ORIGIN);
        assertEquals(ILfs.FALSE, init.initRotGrids(in.dftGrids, width, height, maxPad));
        AtomicInteger ret = new AtomicInteger(-1);
        AtomicInteger paddedWidth = new AtomicInteger();
        AtomicInteger paddedHeight = new AtomicInteger();
        in.padded = ImageUtil.getInstance().padImage(ret, paddedWidth, paddedHeight, image.clone(), width, height,
                in.dftGrids.getPad(), params.getPadValue());
        assertEquals(ILfs.FALSE, ret.get());
        in.paddedWidth = paddedWidth.get();
        in.paddedHeight = paddedHeight.get();
        ImageUtil.getInstance().bits8To6(in.padded, in.paddedWidth, in.paddedHeight);
        return in;
    }

    private void assertMapInvariants(int mw, int mh) {
        AtomicIntegerArray dirMap = maps.getDirectionMap();
        AtomicIntegerArray lcMap = maps.getLowContrastMap();
        AtomicIntegerArray lfMap = maps.getLowFlowMap();
        AtomicIntegerArray hcMap = maps.getHighCurveMap();
        assertEquals(mw * mh, dirMap.length());
        assertEquals(mw * mh, lcMap.length());
        assertEquals(mw * mh, lfMap.length());
        assertEquals(mw * mh, hcMap.length());
        assertDirectionsInRange(dirMap);
        for (int i = 0; i < mw * mh; i++) {
            assertTrue(lcMap.get(i) == 0 || lcMap.get(i) == 1);
            assertTrue(lfMap.get(i) == 0 || lfMap.get(i) == 1);
            assertTrue(hcMap.get(i) == 0 || hcMap.get(i) == 1);
            if (lcMap.get(i) == ILfs.TRUE) {
                assertEquals(ILfs.INVALID_DIR, dirMap.get(i), "low contrast block " + i + " must be INVALID");
            }
            int x = i % mw;
            int y = i / mw;
            if (x == 0 || y == 0 || x == mw - 1 || y == mh - 1) {
                assertEquals(ILfs.INVALID_DIR, dirMap.get(i), "margin block " + i + " must be INVALID");
            }
        }
    }

    private static void assertDirectionsInRange(AtomicIntegerArray dirMap) {
        for (int i = 0; i < dirMap.length(); i++) {
            assertTrue(dirMap.get(i) >= ILfs.INVALID_DIR && dirMap.get(i) < NUM_DIRS,
                    "direction out of range at " + i + ": " + dirMap.get(i));
        }
        assertTrue(countValid(dirMap) > 0);
    }

    private static int countValid(AtomicIntegerArray map) {
        int count = 0;
        for (int i = 0; i < map.length(); i++) {
            if (map.get(i) != ILfs.INVALID_DIR) {
                count++;
            }
        }
        return count;
    }

    private static int countValue(AtomicIntegerArray map, int value) {
        int count = 0;
        for (int i = 0; i < map.length(); i++) {
            if (map.get(i) == value) {
                count++;
            }
        }
        return count;
    }

    private static AtomicIntegerArray filledMap(int length, int value) {
        AtomicIntegerArray map = new AtomicIntegerArray(length);
        for (int i = 0; i < length; i++) {
            map.set(i, value);
        }
        return map;
    }

    /** 3x3 map whose centre is 2 and whose neighbours alternate between opposite directions 0 and 8. */
    private static AtomicIntegerArray opposingNeighbourMap() {
        return new AtomicIntegerArray(new int[] { 0, 8, 0, 8, 2, 8, 0, 8, 0 });
    }

    /** 3x3 map whose neighbours rotate clockwise NW=0, N=2, ... W=14 around the given centre value. */
    private static AtomicIntegerArray vortexMap(int centre) {
        return new AtomicIntegerArray(new int[] { 0, 2, 4, 14, centre, 6, 12, 10, 8 });
    }

    private static org.mockito.stubbing.Answer<Object> failWith(int code) {
        return invocation -> {
            ((AtomicInteger) invocation.getArgument(0)).set(code);
            return null;
        };
    }

    private void assertInitialiseInputBlockImageMapFails(java.util.function.Consumer<Init> failingStub,
            int expected) {
        Init initSpy = spy(Init.getInstance());
        failingStub.accept(initSpy);
        Maps spyMaps = spy(maps);
        doReturn(initSpy).when(spyMaps).getInit();
        AtomicInteger ret = new AtomicInteger();

        AtomicIntegerArray result = spyMaps.initialiseInputBlockImageMap(ret, new AtomicIntegerArray(1),
                new AtomicInteger(1), new AtomicInteger(1), new int[64], 8, 8, fourWaves(), sixteenGrids(),
                mockLfsParams);

        assertNull(result);
        assertEquals(expected, ret.get());
    }

    private DftWaves fourWaves() {
        when(mockDftWaves.getNWaves()).thenReturn(4);
        return mockDftWaves;
    }

    private RotGrids sixteenGrids() {
        when(mockDftGrids.getNoOfGrids()).thenReturn(NUM_DIRS);
        return mockDftGrids;
    }

    /** Spies the Maps singleton so every block has sufficient contrast and DFT results come from the given mock. */
    private Maps spyWithContrastingBlocks(Dft dftMock) {
        fourWaves();
        sixteenGrids();
        Block blockMock = mock(Block.class);
        when(blockMock.lowContrastBlock(anyInt(), anyInt(), any(), anyInt(), anyInt(), any())).thenReturn(ILfs.FALSE);
        Maps spyMaps = spy(maps);
        doReturn(blockMock).when(spyMaps).getBlock();
        doReturn(dftMock).when(spyMaps).getDft();
        return spyMaps;
    }

    /**
     * Dft mock whose statistics fail the primary test but pass the first part of the fork test
     * with direction 4. The first analysed block has one strong fork side (passes), later blocks
     * have none (fail).
     */
    private static Dft forkDftMock() {
        LfsParams params = Globals.getInstance().getLfsParamsV2();
        double powmax = params.getPowmaxMin() * 2;
        double pownorm = forkPownorm(params);
        int leftDir = (4 + NUM_DIRS - params.getForkInterval()) % NUM_DIRS;
        int rightDir = (4 + params.getForkInterval()) % NUM_DIRS;
        AtomicInteger calls = new AtomicInteger();
        Dft dftMock = mock(Dft.class);
        when(dftMock.dftDirPowers(any(), any(), anyInt(), anyInt(), anyInt(), any(), any())).thenAnswer(inv -> {
            AtomicReferenceArray<Double[]> powers = inv.getArgument(0);
            powers.get(1)[leftDir] = calls.getAndIncrement() == 0 ? powmax : 0.0;
            powers.get(1)[rightDir] = 0.0;
            return ILfs.FALSE;
        });
        when(dftMock.getDftPowerStats(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt()))
                .thenAnswer(inv -> {
                    AtomicIntegerArray wis = inv.getArgument(0);
                    AtomicReferenceArray<Double> powmaxs = inv.getArgument(1);
                    AtomicIntegerArray powmaxDirs = inv.getArgument(2);
                    AtomicReferenceArray<Double> pownorms = inv.getArgument(3);
                    for (int i = 0; i < wis.length(); i++) {
                        wis.set(i, i);
                        powmaxs.set(i, powmax);
                        powmaxDirs.set(i, 4);
                        pownorms.set(i, pownorm);
                    }
                    return ILfs.FALSE;
                });
        return dftMock;
    }

    /** A normalized power that fails the primary test but passes the relaxed fork threshold. */
    private static double forkPownorm(LfsParams params) {
        double forkMin = params.getForkPctPownorm() * params.getPownormMin();
        return (forkMin + params.getPownormMin()) / 2;
    }

    /**
     * Builds DFT powers for two waves: wave 0 has the given value at direction 4/5 (the
     * low-frequency check), wave 1 has the given left (dir 2) and right (dir 6) fork powers.
     */
    private static AtomicReferenceArray<Double[]> powers(double lowFreqAtDir, double left, double right) {
        Double[] wave0 = new Double[NUM_DIRS];
        Double[] wave1 = new Double[NUM_DIRS];
        java.util.Arrays.fill(wave0, 0.0);
        java.util.Arrays.fill(wave1, 0.0);
        wave0[4] = lowFreqAtDir;
        wave0[5] = lowFreqAtDir;
        wave1[2] = left;
        wave1[6] = right;
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        powers.set(0, wave0);
        powers.set(1, wave1);
        return powers;
    }

    private static AtomicIntegerArray wis(int... values) {
        return new AtomicIntegerArray(values);
    }

    private static AtomicIntegerArray ints(int... values) {
        return new AtomicIntegerArray(values);
    }

    private static AtomicReferenceArray<Double> doubles(double... values) {
        AtomicReferenceArray<Double> result = new AtomicReferenceArray<>(values.length);
        for (int i = 0; i < values.length; i++) {
            result.set(i, values[i]);
        }
        return result;
    }
}