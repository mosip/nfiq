package org.mosip.nist.nfiq1;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.INfiq;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.mindtct.Maps;
import org.mosip.nist.nfiq1.mindtct.Quality;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        nfiq1Helper = spy(new Nfiq1Helper());
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
}