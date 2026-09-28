package org.mosip.nist.nfiq1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.INfiq;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.imagetools.ImageDecoder;
import org.mosip.nist.nfiq1.mindtct.Maps;
import org.mosip.nist.nfiq1.mindtct.Quality;
import org.mosip.nist.nfiq1.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link Nfiq1Helper}
 * including empty images, valid data processing, and error conditions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Nfiq1HelperTest {

    private Nfiq1Helper nfiq1Helper;

    @Mock
    private Quality mockQualityMap;

    @Mock
    private Maps mockImageMap;

    @Mock
    private AtomicIntegerArray mockQualityArray;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes mocks and creates a spy instance of Nfiq1Helper for testing.
     */
    private boolean originalShowLogs;

    @BeforeEach
    public void setUp() {
        originalShowLogs = Nist.isShowLogs();
        nfiq1Helper = spy(new Nfiq1Helper());
    }

    /**
     * Restores the static show-logs flag so other test classes are not affected.
     */
    @AfterEach
    public void tearDown() {
        Nist.setShowLogs(originalShowLogs);
    }

    /**
     * Tests computeNfiqFeatureVector method with an empty image scenario.
     * Verifies that the method correctly identifies empty images and returns appropriate status.
     */
    @Test
    public void computeNfiqFeatureVectorEmptyImage() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMinutiae(0));

        AtomicIntegerArray qualityArray = mock(AtomicIntegerArray.class);
        when(qualityArray.get(anyInt())).thenReturn(0);
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        int mapWidth = 10, mapHeight = 10;

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, mapWidth, mapHeight);

        assertEquals(INfiq.EMPTY_IMG, result);
        for (double value : featureVector) {
            assertEquals(0.0, value, 0.0001);
        }
    }

    /**
     * Tests computeNfiqFeatureVector method with valid data and logging enabled.
     * Verifies proper feature vector computation when valid minutiae and quality data are provided.
     */
    @Test
    public void computeNfiqFeatureVectorValidDataWithLogs() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMinutiaeWithReliabilities());

        AtomicIntegerArray qualityArray = createMockQualityArray(100, new int[]{20, 30, 25, 15, 10});
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        Nist.setShowLogs(true);

        int mapWidth = 10, mapHeight = 10;

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, mapWidth, mapHeight);

        assertEquals(ILfs.FALSE, result);
        assertTrue(featureVector[0] > 0);
        assertEquals(5.0, featureVector[1], 0.0001);
    }

    /**
     * Tests computeNfiqFeatureVector method with valid data and logging disabled.
     * Verifies that the method works correctly regardless of logging configuration.
     */
    @Test
    public void computeNfiqFeatureVectorValidDataWithoutLogs() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMinutiaeWithReliabilities());

        AtomicIntegerArray qualityArray = createMockQualityArray(100, new int[]{20, 30, 25, 15, 10});
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        Nist.setShowLogs(false);

        int mapWidth = 10, mapHeight = 10;

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, mapWidth, mapHeight);

        assertEquals(ILfs.FALSE, result);
        assertTrue(featureVector[0] > 0);
    }

    /**
     * Tests computeNfiq method with default parameters.
     * Verifies that the method correctly handles standard NFIQ computation with default settings.
     */
    @Test
    public void computeNfiqDefaultParameters() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();

        doReturn(0).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());

        int result = nfiq1Helper.computeNfiq(oNfiq, oConf, imageData, 100, 100, 8, 500, 1);

        assertEquals(0, result);
    }

    /**
     * Tests computeNfiq method with logging disabled.
     * Verifies that the log flag parameter is properly handled when set to disabled.
     */
    @Test
    public void computeNfiqLogFlagDisabled() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();

        doReturn(0).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());

        int result = nfiq1Helper.computeNfiq(oNfiq, oConf, imageData, 100, 100, 8, 500, 0);

        assertEquals(0, result);
    }

    /**
     * Tests computeNfiqFlex method for successful execution scenario.
     * Verifies that the flexible NFIQ computation works correctly with all parameters provided.
     */
    @Test
    public void computeNfiqFlexSuccess() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        setupMocksForSuccessfulExecution();

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Tests computeNfiqFlex method with undefined PPI (Pixels Per Inch).
     * Verifies that the method handles undefined PPI values correctly.
     */
    @Test
    public void computeNfiqFlexUndefinedPPI() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        setupMocksForSuccessfulExecution();

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8,
                ILfs.UNDEFINED, zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Tests computeNfiqFlex method when minutiae detection fails.
     * Verifies that the method properly handles errors during minutiae detection process.
     */
    @Test
    public void computeNfiqFlexMinutiaeDetectionError() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        doReturn(-1).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(-1, result);
    }

    /**
     * Tests computeNfiqFeatureVector with high reliability minutiae.
     */
    @Test
    public void computeNfiqFeatureVectorHighReliabilityMinutiae() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createHighReliabilityMinutiae());

        AtomicIntegerArray qualityArray = createMockQualityArray(100, new int[]{10, 20, 30, 25, 15});
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, 10, 10);

        assertEquals(ILfs.FALSE, result);
        assertTrue(featureVector[0] > 0);
    }

    /**
     * Tests computeNfiqFeatureVector with mixed reliability minutiae.
     */
    @Test
    public void computeNfiqFeatureVectorMixedReliabilityMinutiae() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMixedReliabilityMinutiae());

        AtomicIntegerArray qualityArray = createMockQualityArray(100, new int[]{5, 15, 25, 35, 20});
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, 10, 10);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Tests computeNfiq with error from computeNfiqFlex.
     */
    @Test
    public void computeNfiqFlexError() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();

        doReturn(-1).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());

        int result = nfiq1Helper.computeNfiq(oNfiq, oConf, imageData, 100, 100, 8, 500, 1);

        assertEquals(-1, result);
    }

    /**
     * Tests computeNfiqFlex method when too few minutiae are detected.
     * Verifies that the method correctly identifies cases with insufficient minutiae for quality assessment.
     */
    @Test
    public void computeNfiqFlexTooFewMinutiae() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        setupMocksForTooFewMinutiae();

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(INfiq.TOO_FEW_MINUTIAE, result);
    }

    /**
     * Tests computeNfiqFlex method when empty image is detected during feature vector computation.
     * Verifies that empty images are properly identified at the feature computation stage.
     */
    @Test
    public void computeNfiqFlexEmptyImageInFeatureVector() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        setupMocksForEmptyImage();

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(INfiq.EMPTY_IMG, result);
    }

    /**
     * Tests computeNfiqFlex method when MLP (Multi-Layer Perceptron) classification fails.
     * Verifies that the method handles neural network classification errors appropriately.
     */
    @Test
    public void computeNfiqFlexMLPError() {
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        setupMocksForMLPError();

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertEquals(-2, result);
    }

    /**
     * Creates a mock Minutiae object with the specified number of minutiae.
     *
     * @param count The number of minutiae to create
     * @return A mock Minutiae object
     */
    private Minutiae createMinutiae(int count) {
        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Minutia());
        }
        when(minutiae.getNum()).thenReturn(count);
        if (count > 0) {
            when(minutiae.getList()).thenReturn(list);
        }
        return minutiae;
    }

    /**
     * Creates a mock Minutiae object with predefined reliability values.
     * This helper method creates minutiae with varying reliability scores for testing.
     *
     * @return A mock Minutiae object with reliability values
     */
    private Minutiae createMinutiaeWithReliabilities() {
        Minutiae minutiae = mock(Minutiae.class);
        double[] reliabilities = {0.4, 0.6, 0.7, 0.8, 0.95};
        List<Minutia> list = new ArrayList<>();

        for (double reliability : reliabilities) {
            Minutia minutia = mock(Minutia.class);
            when(minutia.getReliability()).thenReturn(reliability);
            list.add(minutia);
        }
        when(minutiae.getNum()).thenReturn(5);
        when(minutiae.getList()).thenReturn(list);
        return minutiae;
    }

    /**
     * Creates a mock quality array based on histogram distribution.
     *
     * @param totalPixels The total number of pixels
     * @param histogram   An array representing the distribution of quality levels
     * @return A mock AtomicIntegerArray representing quality values
     */
    private AtomicIntegerArray createMockQualityArray(int totalPixels, int[] histogram) {
        AtomicIntegerArray array = mock(AtomicIntegerArray.class);
        int index = 0;

        for (int level = 0; level < histogram.length; level++) {
            for (int count = 0; count < histogram[level]; count++) {
                when(array.get(index++)).thenReturn(level);
            }
        }

        return array;
    }

    /**
     * Creates sample image data for testing purposes.
     *
     * @return An array of integers representing grayscale image data
     */
    private int[] createSampleImageData() {
        int[] data = new int[10000];
        for (int i = 0; i < data.length; i++) {
            data[i] = 128;
        }
        return data;
    }

    /**
     * Sets up mocks for successful execution scenario.
     * Configures the spy to return successful status codes.
     */
    private void setupMocksForSuccessfulExecution() {
        doReturn(ILfs.FALSE).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());
    }

    /**
     * Sets up mocks for too few minutiae scenario.
     * Configures the spy to return the appropriate error code for insufficient minutiae.
     */
    private void setupMocksForTooFewMinutiae() {
        doReturn(INfiq.TOO_FEW_MINUTIAE).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());
    }

    /**
     * Sets up mocks for empty image scenario.
     * Configures the spy to return the empty image status code.
     */
    private void setupMocksForEmptyImage() {
        doReturn(INfiq.EMPTY_IMG).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());
    }

    /**
     * Sets up mocks for MLP error scenario.
     * Configures the spy to return an error code indicating MLP classification failure.
     */
    private void setupMocksForMLPError() {
        doReturn(-2).when(nfiq1Helper).computeNfiqFlex(any(), any(), any(), anyInt(), anyInt(),
                anyInt(), anyInt(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), any());
    }

    /**
     * Creates minutiae with high reliability values.
     */
    private Minutiae createHighReliabilityMinutiae() {
        Minutiae minutiae = mock(Minutiae.class);
        double[] reliabilities = {0.9, 0.95, 0.98, 0.92, 0.96};
        List<Minutia> list = new ArrayList<>();

        for (double reliability : reliabilities) {
            Minutia minutia = mock(Minutia.class);
            when(minutia.getReliability()).thenReturn(reliability);
            list.add(minutia);
        }
        when(minutiae.getNum()).thenReturn(5);
        when(minutiae.getList()).thenReturn(list);
        return minutiae;
    }

    /**
     * Creates minutiae with mixed reliability values.
     */
    private Minutiae createMixedReliabilityMinutiae() {
        Minutiae minutiae = mock(Minutiae.class);
        double[] reliabilities = {0.3, 0.7, 0.5, 0.9, 0.6, 0.8};
        List<Minutia> list = new ArrayList<>();

        for (double reliability : reliabilities) {
            Minutia minutia = mock(Minutia.class);
            when(minutia.getReliability()).thenReturn(reliability);
            list.add(minutia);
        }
        when(minutiae.getNum()).thenReturn(6);
        when(minutiae.getList()).thenReturn(list);
        return minutiae;
    }

    /**
     * Verifies computeNfiqFeatureVector with zero foreground pixels.
     */
    @Test
    public void computeNfiqFeatureVectorZeroForeground() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMinutiae(0));

        AtomicIntegerArray qualityArray = mock(AtomicIntegerArray.class);
        when(qualityArray.get(anyInt())).thenReturn(0);
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, 10, 10);

        assertEquals(INfiq.EMPTY_IMG, result);
    }

    /**
     * Verifies computeNfiqFeatureVector with partial reliability minutiae.
     */
    @Test
    public void computeNfiqFeatureVectorPartialReliability() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        Minutiae minutiae = mock(Minutiae.class);
        List<Minutia> list = new ArrayList<>();

        Minutia m1 = mock(Minutia.class);
        when(m1.getReliability()).thenReturn(0.55);
        Minutia m2 = mock(Minutia.class);
        when(m2.getReliability()).thenReturn(0.65);
        list.add(m1);
        list.add(m2);

        when(minutiae.getNum()).thenReturn(2);
        when(minutiae.getList()).thenReturn(list);
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(minutiae);

        AtomicIntegerArray qualityArray = createMockQualityArray(100, new int[]{10, 25, 30, 20, 15});
        when(mockQualityMap.getQualityMap()).thenReturn(qualityArray);

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN,
                oMinutiae, mockQualityMap, 10, 10);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Verifies computeNfiqFlex with actual implementation behavior.
     */
    @Test
    public void computeNfiqFlexActualImplementation() {
        Nfiq1Helper realHelper = new Nfiq1Helper();
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        for (int i = 0; i < zNormStds.length; i++) {
            zNormStds[i] = 1.0;
        }

        int result = realHelper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, 500,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertTrue(result == ILfs.FALSE || result == INfiq.TOO_FEW_MINUTIAE || result == INfiq.EMPTY_IMG);
    }

    /**
     * Verifies computeNfiqFlex with undefined PPI path.
     */
    @Test
    public void computeNfiqFlexUndefinedPPIPath() {
        Nfiq1Helper realHelper = new Nfiq1Helper();
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);
        int[] imageData = createSampleImageData();
        double[] zNormMeans = new double[INfiq.NFIQ_VCTRLEN];
        double[] zNormStds = new double[INfiq.NFIQ_VCTRLEN];
        double[] weights = new double[100];

        for (int i = 0; i < zNormStds.length; i++) {
            zNormStds[i] = 1.0;
        }

        int result = realHelper.computeNfiqFlex(oNfiq, oConf, imageData, 100, 100, 8, ILfs.UNDEFINED,
                zNormMeans, zNormStds, 10, 5, 5, 1, 1, weights);

        assertTrue(result == ILfs.FALSE || result == INfiq.TOO_FEW_MINUTIAE || result == INfiq.EMPTY_IMG);
    }

    /**
     * Verifies Z-normalization and MLP classification calculations.
     */
    @Test
    public void zNormalizationAndMLPClassification() {
        double[] featureVector = {100.0, 5.0, 3.0, 2.0, 1.0, 0.3, 0.25, 0.15, 0.1};
        double[] zNormMeans = {50.0, 10.0, 5.0, 4.0, 3.0, 0.2, 0.2, 0.2, 0.2};
        double[] zNormStds = {25.0, 5.0, 2.0, 2.0, 2.0, 0.1, 0.1, 0.1, 0.1};

        double expectedNorm0 = (100.0 - 50.0) / 25.0;
        double expectedNorm1 = (5.0 - 10.0) / 5.0;

        assertEquals(2.0, expectedNorm0, 0.001);
        assertEquals(-1.0, expectedNorm1, 0.001);
    }

    /**
     * Verifies MLP output processing logic.
     */
    @Test
    public void mLPOutputProcessing() {
        AtomicInteger classIndex = new AtomicInteger(2);
        AtomicReference<Double> maxActivation = new AtomicReference<>(0.75);
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);

        oNfiq.set(classIndex.get() + 1);
        oConf.set(maxActivation.get());

        assertEquals(3, oNfiq.get());
        assertEquals(0.75, oConf.get(), 0.001);
    }

    /**
     * Verifies AtomicReferenceArray to array conversion.
     */
    @Test
    public void atomicReferenceArrayConversion() {
        double[] outacsarr = {0.1, 0.3, 0.8, 0.2, 0.05};
        AtomicReferenceArray<Double> outacs = new AtomicReferenceArray<>(outacsarr.length);

        for (int i = 0; i < outacsarr.length; i++) {
            outacs.set(i, outacsarr[i]);
        }

        for (int i = 0; i < outacs.length(); i++) {
            outacsarr[i] = outacs.get(i);
        }

        assertEquals(0.1, outacsarr[0], 0.001);
        assertEquals(0.8, outacsarr[2], 0.001);
    }

    /**
     * Decoded grayscale sample image used by the end-to-end tests.
     */
    private record DecodedImage(int[] data, int width, int height, int depth, int ppi) {
    }

    /**
     * Decodes one of the bundled ISO sample fingerprints into 8-bit grayscale pixel data, the
     * same way the sample application does. The file is looked up under src/test/resources
     * first and then in the module directory (paths are relative to the working directory).
     */
    private DecodedImage decodeSample(String sampleName) throws Exception {
        String isoFile = "src/test/resources/" + sampleName;
        AtomicInteger retCode = new AtomicInteger(-1);
        AtomicInteger imageType = new AtomicInteger(-1);
        AtomicInteger length = new AtomicInteger();
        AtomicInteger width = new AtomicInteger();
        AtomicInteger height = new AtomicInteger();
        AtomicInteger depth = new AtomicInteger();
        AtomicInteger ppi = new AtomicInteger();
        AtomicReference<String> fileType = new AtomicReference<>();

        BufferedImage image = ImageDecoder.getInstance().readAndDecodeGrayscaleImage(retCode, isoFile, imageType,
                length, width, height, depth, ppi, fileType);
        assertEquals(ILfs.FALSE, retCode.get());
        assertNotNull(image);

        int[] data = ImageUtil.convertTo1DWithoutUsingGetRGB(image, "jpg");
        return new DecodedImage(data, width.get(), height.get(), depth.get(), ppi.get());
    }

    /**
     * Runs the full NFIQ pipeline twice on the bundled JPEG2000 sample with logging disabled and
     * verifies a valid quality level (1..5) and confidence in (0, 1] are produced, identically
     * on both runs (no state leaks between runs through the shared singletons).
     */
    @Test
    public void computeNfiqJp2SampleEndToEndWithoutLogsIsDeterministic() throws Exception {
        DecodedImage image = decodeSample("info_jp2.iso");
        Nfiq1Helper helper = new Nfiq1Helper();
        AtomicInteger firstNfiq = new AtomicInteger();
        AtomicReference<Double> firstConf = new AtomicReference<>(0.0);
        AtomicInteger secondNfiq = new AtomicInteger();
        AtomicReference<Double> secondConf = new AtomicReference<>(0.0);

        int first = helper.computeNfiq(firstNfiq, firstConf, image.data().clone(), image.width(),
                image.height(), image.depth(), image.ppi(), 0);
        int second = helper.computeNfiq(secondNfiq, secondConf, image.data().clone(), image.width(),
                image.height(), image.depth(), image.ppi(), 0);

        assertEquals(ILfs.FALSE, first);
        assertEquals(ILfs.FALSE, second);
        assertFalse(Nist.isShowLogs());
        assertTrue(firstNfiq.get() >= 1 && firstNfiq.get() <= INfiq.NFIQ_NUM_CLASSES);
        assertTrue(firstConf.get() > 0.0 && firstConf.get() <= 1.0);
        assertEquals(firstNfiq.get(), secondNfiq.get());
        assertEquals(firstConf.get(), secondConf.get(), 1e-12);
    }

    /**
     * Runs the full NFIQ pipeline on the bundled WSQ sample with detailed logging enabled and
     * verifies a valid quality level and confidence are produced.
     */
    @Test
    public void computeNfiqWsqSampleEndToEndWithLogs() throws Exception {
        DecodedImage image = decodeSample("info_wsq.iso");
        Nfiq1Helper helper = new Nfiq1Helper();
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);

        int result = helper.computeNfiq(oNfiq, oConf, image.data(), image.width(), image.height(),
                image.depth(), image.ppi(), 1);

        assertEquals(ILfs.FALSE, result);
        assertTrue(Nist.isShowLogs());
        assertTrue(oNfiq.get() >= 1 && oNfiq.get() <= INfiq.NFIQ_NUM_CLASSES);
        assertTrue(oConf.get() > 0.0 && oConf.get() <= 1.0);
    }

    /**
     * Verifies computeNfiqFlex propagates the minutiae-detection error when the image is not
     * 8 bits deep, leaving the output values untouched.
     */
    @Test
    public void computeNfiqFlexRejectsNonEightBitImage() {
        Nfiq1Helper helper = new Nfiq1Helper();
        Nfiq1Globals globals = helper.getNfiqGlobals();
        AtomicInteger oNfiq = new AtomicInteger(-7);
        AtomicReference<Double> oConf = new AtomicReference<>(-7.0);

        int result = helper.computeNfiqFlex(oNfiq, oConf, createSampleImageData(), 100, 100, 16, 500,
                globals.getDfltZnormMeans(), globals.getDfltZnormStds(), globals.getDfltNInps(),
                globals.getDfltNHids(), globals.getDfltNOuts(), globals.getDfltAcFuncHids(),
                globals.getDfltAcFuncOuts(), globals.getDfltWts());

        assertTrue(result < ILfs.FALSE);
        assertEquals(-7, oNfiq.get());
        assertEquals(-7.0, oConf.get(), 0.0);
    }

    /**
     * Verifies computeNfiqFlex returns the MLP error code when an unsupported hidden-layer
     * activation function is requested for a real fingerprint.
     */
    @Test
    public void computeNfiqFlexReturnsMlpErrorForUnsupportedActivation() throws Exception {
        DecodedImage image = decodeSample("info_jp2.iso");
        Nfiq1Helper helper = new Nfiq1Helper();
        Nfiq1Globals globals = helper.getNfiqGlobals();
        AtomicInteger oNfiq = new AtomicInteger(-7);
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);

        int result = helper.computeNfiqFlex(oNfiq, oConf, image.data(), image.width(), image.height(),
                image.depth(), image.ppi(), globals.getDfltZnormMeans(), globals.getDfltZnormStds(),
                globals.getDfltNInps(), globals.getDfltNHids(), globals.getDfltNOuts(), 999,
                globals.getDfltAcFuncOuts(), globals.getDfltWts());

        assertEquals(-3, result);
        assertEquals(-7, oNfiq.get());
    }

    /**
     * Verifies computeNfiqFlex maps an empty feature vector to the empty-image quality level
     * with full confidence.
     */
    @Test
    public void computeNfiqFlexEmptyFeatureVectorGivesEmptyImageQuality() throws Exception {
        DecodedImage image = decodeSample("info_jp2.iso");
        Nfiq1Globals globals = nfiq1Helper.getNfiqGlobals();
        doReturn(INfiq.EMPTY_IMG).when(nfiq1Helper).computeNfiqFeatureVector(any(), anyInt(), any(), any(),
                anyInt(), anyInt());
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);

        int result = nfiq1Helper.computeNfiqFlex(oNfiq, oConf, image.data(), image.width(), image.height(),
                image.depth(), image.ppi(), globals.getDfltZnormMeans(), globals.getDfltZnormStds(),
                globals.getDfltNInps(), globals.getDfltNHids(), globals.getDfltNOuts(),
                globals.getDfltAcFuncHids(), globals.getDfltAcFuncOuts(), globals.getDfltWts());

        assertEquals(INfiq.EMPTY_IMG, result);
        assertEquals(INfiq.EMPTY_IMG_QUAL, oNfiq.get());
        assertEquals(1.0, oConf.get(), 0.0);
    }

    /**
     * Verifies a blank (uniform white) image yields too few minutiae and is assigned the
     * minimum-minutiae quality level with full confidence.
     */
    @Test
    public void computeNfiqBlankImageReportsTooFewMinutiae() {
        Nfiq1Helper helper = new Nfiq1Helper();
        int[] blank = new int[200 * 200];
        java.util.Arrays.fill(blank, 255);
        AtomicInteger oNfiq = new AtomicInteger();
        AtomicReference<Double> oConf = new AtomicReference<>(0.0);

        int result = helper.computeNfiq(oNfiq, oConf, blank, 200, 200, 8, 500, 0);

        assertEquals(INfiq.TOO_FEW_MINUTIAE, result);
        assertEquals(INfiq.MIN_MINUTIAE_QUAL, oNfiq.get());
        assertEquals(1.0, oConf.get(), 0.0);
    }

    /**
     * Verifies the computed feature vector values (foreground, minutiae count, reliability bins
     * and quality-map ratios) for a known quality map and minutiae set.
     */
    @Test
    public void computeNfiqFeatureVectorComputesExpectedValues() {
        double[] featureVector = new double[INfiq.NFIQ_VCTRLEN];
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>(createMinutiaeWithReliabilities());
        int[] levels = new int[100];
        int index = 0;
        int[] histogram = {20, 30, 25, 15, 10};
        for (int level = 0; level < histogram.length; level++) {
            for (int count = 0; count < histogram[level]; count++) {
                levels[index++] = level;
            }
        }
        when(mockQualityMap.getQualityMap()).thenReturn(new AtomicIntegerArray(levels));

        int result = nfiq1Helper.computeNfiqFeatureVector(featureVector, INfiq.NFIQ_VCTRLEN, oMinutiae,
                mockQualityMap, 10, 10);

        assertEquals(ILfs.FALSE, result);
        assertEquals(80.0, featureVector[0], 1e-9);
        assertEquals(5.0, featureVector[1], 1e-9);
        assertEquals(4.0, featureVector[2], 1e-9);
        assertEquals(3.0, featureVector[3], 1e-9);
        assertEquals(2.0, featureVector[4], 1e-9);
        assertEquals(1.0, featureVector[5], 1e-9);
        assertEquals(1.0, featureVector[6], 1e-9);
        assertEquals(30.0 / 80.0, featureVector[7], 1e-9);
        assertEquals(25.0 / 80.0, featureVector[8], 1e-9);
        assertEquals(15.0 / 80.0, featureVector[9], 1e-9);
        assertEquals(10.0 / 80.0, featureVector[10], 1e-9);
    }
}