package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
class MapsTest {

    private Maps maps;

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
}