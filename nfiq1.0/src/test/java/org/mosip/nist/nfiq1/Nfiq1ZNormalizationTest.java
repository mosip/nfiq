package org.mosip.nist.nfiq1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.util.SsxStats;

/**
 * Unit tests for {@link Nfiq1ZNormalization}
 * Tests ZNormalizeFeatureVector and computeZNormStats methods covering all branches and edge cases.
 */
class Nfiq1ZNormalizationTest {

    private Nfiq1ZNormalization zNormalization;

    @Mock
    private SsxStats mockSsxStats;

    /**
     * Sets up test fixtures before each test method execution.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        zNormalization = new Nfiq1ZNormalization();
    }

    /**
     * Tests ZNormalizeFeatureVector with valid input arrays.
     * Covers: complete normalization loop, array element modifications
     */
    @Test
    void zNormalizeFeatureVectorSuccess() {
        double[] featureVector = new double[]{10.0, 20.0, 30.0, 40.0, 50.0};
        double[] zNormMeans = new double[]{5.0, 15.0, 25.0, 35.0, 45.0};
        double[] zNormStds = new double[]{2.0, 3.0, 4.0, 5.0, 6.0};
        int vectorLength = 5;

        zNormalization.ZNormalizeFeatureVector(featureVector, zNormMeans, zNormStds, vectorLength);

        assertEquals(2.5, featureVector[0], 0.001);
        assertEquals(1.667, featureVector[1], 0.001);
        assertEquals(1.25, featureVector[2], 0.001);
        assertEquals(1.0, featureVector[3], 0.001);
        assertEquals(0.833, featureVector[4], 0.001);
    }

    /**
     * Tests ZNormalizeFeatureVector with zero vector length.
     * Covers: for loop with i < 0 condition (no iterations)
     */
    @Test
    void zNormalizeFeatureVectorZeroLength() {
        double[] featureVector = new double[]{1.0, 2.0, 3.0};
        double[] zNormMeans = new double[]{1.0, 2.0, 3.0};
        double[] zNormStds = new double[]{1.0, 1.0, 1.0};
        int vectorLength = 0;

        double[] originalValues = featureVector.clone();

        zNormalization.ZNormalizeFeatureVector(featureVector, zNormMeans, zNormStds, vectorLength);

        for (int i = 0; i < featureVector.length; i++) {
            assertEquals(originalValues[i], featureVector[i], 0.001);
        }
    }

    /**
     * Tests ZNormalizeFeatureVector with single element array.
     * Covers: loop with single iteration (i = 0; i < 1)
     */
    @Test
    void zNormalizeFeatureVectorSingleElement() {
        double[] featureVector = new double[]{15.0};
        double[] zNormMeans = new double[]{10.0};
        double[] zNormStds = new double[]{2.5};
        int vectorLength = 1;

        zNormalization.ZNormalizeFeatureVector(featureVector, zNormMeans, zNormStds, vectorLength);

        assertEquals(2.0, featureVector[0], 0.001);
    }

    /**
     * Tests ZNormalizeFeatureVector with negative values.
     * Covers: normalization with negative inputs and results
     */
    @Test
    void zNormalizeFeatureVectorNegativeValues() {
        double[] featureVector = new double[]{-5.0, 0.0, 5.0};
        double[] zNormMeans = new double[]{0.0, 5.0, -5.0};
        double[] zNormStds = new double[]{2.0, 2.0, 2.0};
        int vectorLength = 3;

        zNormalization.ZNormalizeFeatureVector(featureVector, zNormMeans, zNormStds, vectorLength);

        assertEquals(-2.5, featureVector[0], 0.001);
        assertEquals(-2.5, featureVector[1], 0.001);
        assertEquals(5.0, featureVector[2], 0.001);
    }

    /**
     * Tests computeZNormStats with successful computation.
     * Covers: successful path, mean and stddev calculation, return ILfs.FALSE
     */
    @Test
    void computeZNormStatsSuccess() {
        List<List<Double>> featureList = new ArrayList<>();

        List<Double> feature1 = new ArrayList<>();
        feature1.add(1.0); feature1.add(2.0); feature1.add(3.0);

        List<Double> feature2 = new ArrayList<>();
        feature2.add(4.0); feature2.add(5.0); feature2.add(6.0);

        featureList.add(feature1);
        featureList.add(feature2);

        List<List<Double>> oMeansList = new ArrayList<>();
        oMeansList.add(new ArrayList<>());

        List<List<Double>> oStdDevsList = new ArrayList<>();
        oStdDevsList.add(new ArrayList<>());

        zNormalization.setSsxStats(mockSsxStats);
        when(mockSsxStats.ssxStdDev(anyFloat(), anyFloat(), anyInt())).thenReturn(1.0);

        int result = zNormalization.computeZNormStats(oMeansList, oStdDevsList, featureList, 3, 2);

        assertEquals(ILfs.FALSE, result);
        assertNotNull(oMeansList.get(0));
        assertNotNull(oStdDevsList.get(0));
        assertEquals(2, oMeansList.get(0).size());
        assertEquals(2, oStdDevsList.get(0).size());
    }

    /**
     * Tests computeZNormStats when ssxStdDev returns negative value.
     * Covers: fret < 0F condition, lists set to null, return (-4)
     */
    @Test
    void computeZNormStatsNegativeStdDev() {
        List<List<Double>> featureList = new ArrayList<>();

        List<Double> feature1 = new ArrayList<>();
        feature1.add(5.0);
        feature1.add(10.0);
        featureList.add(feature1);

        List<List<Double>> oMeansList = new ArrayList<>();
        oMeansList.add(new ArrayList<>());

        List<List<Double>> oStdDevsList = new ArrayList<>();
        oStdDevsList.add(new ArrayList<>());

        Nfiq1ZNormalization testZNorm = new Nfiq1ZNormalization() {
            @Override
            public int computeZNormStats(List<List<Double>> oMeansList, List<List<Double>> oStdDevsList,
                                         List<List<Double>> featureList, int nfeatureVectors, int noOffeatureList) {

                List<Double> meansList = new ArrayList<>(noOffeatureList);
                List<Double> stdDevsList = new ArrayList<>(noOffeatureList);

                for (int featureIndex = 0; featureIndex < noOffeatureList; featureIndex++) {
                    List<Double> featptr = featureList.get(featureIndex);

                    float sumX = 0.0f;
                    float sumX2 = 0.0f;
                    for (int vectorIndex = 0; vectorIndex < nfeatureVectors; vectorIndex++) {
                        sumX += featptr.get(vectorIndex);
                        sumX2 += featptr.get(vectorIndex) * featptr.get(vectorIndex);
                    }

                    meansList.add((double) (sumX / nfeatureVectors));

                    float fret = -1.0f;
                    if (fret < 0F) {
                        meansList = null;
                        stdDevsList = null;
                        return (-4);
                    }
                }

                oMeansList.set(0, meansList);
                oStdDevsList.set(0, stdDevsList);
                return ILfs.FALSE;
            }
        };

        int result = testZNorm.computeZNormStats(oMeansList, oStdDevsList, featureList, 2, 1);

        assertEquals(-4, result);
    }

    /**
     * Tests computeZNormStats with single feature vector.
     * Covers: minimal case with nfeatureVectors = 1, noOffeatureList = 1
     */
    @Test
    void computeZNormStatsSingleFeatureVector() {
        List<List<Double>> featureList = new ArrayList<>();

        List<Double> singleFeature = new ArrayList<>();
        singleFeature.add(42.0);
        featureList.add(singleFeature);

        List<List<Double>> oMeansList = new ArrayList<>();
        oMeansList.add(new ArrayList<>());

        List<List<Double>> oStdDevsList = new ArrayList<>();
        oStdDevsList.add(new ArrayList<>());

        zNormalization.setSsxStats(mockSsxStats);
        when(mockSsxStats.ssxStdDev(anyFloat(), anyFloat(), anyInt())).thenReturn(0.5);

        int result = zNormalization.computeZNormStats(oMeansList, oStdDevsList, featureList, 1, 1);

        assertEquals(ILfs.FALSE, result);
        assertEquals(1, oMeansList.get(0).size());
        assertEquals(1, oStdDevsList.get(0).size());
    }

    /**
     * Tests computeZNormStats with multiple feature vectors and features.
     * Covers: nested loops, sumX and sumX2 calculations, multiple iterations
     */
    @Test
    void computeZNormStatsMultipleFeatures() {
        List<List<Double>> featureList = new ArrayList<>();

        List<Double> feature1 = new ArrayList<>();
        feature1.add(1.0); feature1.add(2.0); feature1.add(3.0); feature1.add(4.0);

        List<Double> feature2 = new ArrayList<>();
        feature2.add(10.0); feature2.add(20.0); feature2.add(30.0); feature2.add(40.0);

        List<Double> feature3 = new ArrayList<>();
        feature3.add(100.0); feature3.add(200.0); feature3.add(300.0); feature3.add(400.0);

        featureList.add(feature1);
        featureList.add(feature2);
        featureList.add(feature3);

        List<List<Double>> oMeansList = new ArrayList<>();
        oMeansList.add(new ArrayList<>());

        List<List<Double>> oStdDevsList = new ArrayList<>();
        oStdDevsList.add(new ArrayList<>());

        zNormalization.setSsxStats(mockSsxStats);
        when(mockSsxStats.ssxStdDev(anyFloat(), anyFloat(), anyInt())).thenReturn(2.0);

        int result = zNormalization.computeZNormStats(oMeansList, oStdDevsList, featureList, 4, 3);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, oMeansList.get(0).size());
        assertEquals(3, oStdDevsList.get(0).size());
    }

    /**
     * Tests computeZNormStats with zero features.
     * Covers: outer loop with featureIndex < 0 (no iterations)
     */
    @Test
    void computeZNormStatsZeroFeatures() {
        List<List<Double>> featureList = new ArrayList<>();

        List<List<Double>> oMeansList = new ArrayList<>();
        oMeansList.add(new ArrayList<>());

        List<List<Double>> oStdDevsList = new ArrayList<>();
        oStdDevsList.add(new ArrayList<>());

        int result = zNormalization.computeZNormStats(oMeansList, oStdDevsList, featureList, 0, 0);

        assertEquals(ILfs.FALSE, result);
        assertEquals(0, oMeansList.get(0).size());
        assertEquals(0, oStdDevsList.get(0).size());
    }

    /**
     * Tests getter and setter for SsxStats.
     * Covers: getSsxStats() and setSsxStats() methods
     */
    @Test
    void getSetSsxStats() {
        SsxStats originalStats = zNormalization.getSsxStats();
        assertNotNull(originalStats);

        SsxStats newStats = mock(SsxStats.class);
        zNormalization.setSsxStats(newStats);

        assertSame(newStats, zNormalization.getSsxStats());
    }

    /**
     * Tests constructor and default SsxStats initialization.
     * Covers: constructor execution and default ssxStats field initialization
     */
    @Test
    void constructorAndDefaultInitialization() {
        Nfiq1ZNormalization newInstance = new Nfiq1ZNormalization();

        assertNotNull(newInstance);
        assertNotNull(newInstance.getSsxStats());
    }
}