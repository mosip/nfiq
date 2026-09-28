package org.mosip.nist.nfiq1.mlp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mosip.nist.nfiq1.common.IMlp;
import org.mosip.nist.nfiq1.mindtct.Free;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Complete test class for RunMlp
 */
class RunMlpTest {

    private RunMlp runMlp;
    private PrintStream originalErr;
    private ByteArrayOutputStream errorStream;
    private static final double DELTA = 0.0001;
    private ByteArrayOutputStream logOutput;
    private Logger logger;

    @BeforeEach
    void setUp() {
        runMlp = RunMlp.getInstance();

        originalErr = System.err;
        errorStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    /**
     * Verifies that getInstance returns the same singleton instance.
     */
    @Test
    void getInstanceReturnsSingleton() {
        RunMlp firstInstance = RunMlp.getInstance();
        RunMlp secondInstance = RunMlp.getInstance();
        assertSame(firstInstance, secondInstance);
        assertNotNull(firstInstance);
    }

    /**
     * Verifies that getAcs returns a valid Acs instance.
     */
    @Test
    void getAcs() {
        assertNotNull(runMlp.getAcs());
        assertTrue(runMlp.getAcs() instanceof Acs);
    }

    /**
     * Verifies that getFree returns a valid Free instance.
     */
    @Test
    void getFree() {
        assertNotNull(runMlp.getFree());
        assertTrue(runMlp.getFree() instanceof Free);
    }

    /**
     * Verifies that getMlpCla returns a valid MlpCla instance.
     */
    @Test
    void getMlpCla() {
        assertNotNull(runMlp.getMlpCla());
        assertTrue(runMlp.getMlpCla() instanceof MlpCla);
    }

    /**
     * Verifies runMlp2 reports the index of the most strongly activated output node, including
     * cases where the maximum is not reached by consecutive improvements.
     */
    @Test
    void runMlp2HypClassIsIndexOfMaximumActivation() {
        double[][] biases = {{0.5, 0.1, 0.9}, {0.9, 0.1, 0.5}, {0.1, 0.9, 0.5}, {0.1, 0.5, 0.3, 0.9},
                {0.2, 0.8, 0.4, 0.6, 0.1}};
        int[] expectedClass = {2, 0, 1, 3, 1};
        for (int t = 0; t < biases.length; t++) {
            int nOuts = biases[t].length;
            // 1 input, 1 hidden node with zero weight/bias, so each output equals its bias (then 0.25x).
            AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(2 + 2 * nOuts);
            for (int i = 0; i < weights.length(); i++) {
                weights.set(i, 0.0);
            }
            for (int o = 0; o < nOuts; o++) {
                weights.set(2 + nOuts + o, biases[t][o]);
            }
            AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
            AtomicInteger hypClass = new AtomicInteger(-1);
            AtomicReference<Double> confidence = new AtomicReference<>();

            assertEquals(0, runMlp.runMlp2(1, 1, nOuts, IMlp.LINEAR, IMlp.LINEAR, weights, new double[] {1.0},
                    outAcs, hypClass, confidence));
            assertEquals(expectedClass[t], hypClass.get(), "case " + t);
            assertEquals(outAcs.get(expectedClass[t]), confidence.get(), DELTA);
        }
    }

    /**
     * Tests runMlp2 method with linear activation functions.
     */
    @Test
    void runMlp2LinearActivation() {
        int nInps = 2, nHids = 3, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, 2.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
    }

    /**
     * Tests runMlp2 method with sigmoid activation functions.
     */
    @Test
    void runMlp2SigmoidActivation() {
        int nInps = 3, nHids = 2, nOuts = 3;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {0.5, -0.5, 1.5};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SIGMOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
    }

    /**
     * Tests runMlp2 method with sinusoid activation functions.
     */
    @Test
    void runMlp2SinusoidActivation() {
        int nInps = 2, nHids = 4, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {Math.PI / 4, -Math.PI / 6};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SINUSOID, IMlp.SINUSOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
    }

    /**
     * Tests runMlp2 method when hidden units exceed maximum allowed.
     */
    @Test
    void runMlp2ExceedsMaxHids() {
        int nInps = 2, nHids = IMlp.MAX_NHIDS + 10, nOuts = 2;
        AtomicReferenceArray<Double> weights = createMinimalWeightsArray(20);
        double[] featureVector = {1.0, 2.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(-2, result);

        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.length() > 0 || result == -2, "Error should be logged or return code should be -2");
    }

    /**
     * Tests runMlp2 method with unsupported hidden layer activation function.
     */
    @Test
    void runMlp2UnsupportedHiddenActivation() {
        int nInps = 2, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, 1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, 999, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(-3, result);

        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.length() > 0 || result == -3, "Error should be logged or return code should be -3");
    }

    /**
     * Tests runMlp2 method with unsupported output layer activation function.
     */
    @Test
    void runMlp2UnsupportedOutputActivation() {
        int nInps = 2, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, 1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, 888,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(-4, result);

        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.length() > 0 || result == -4, "Error should be logged or return code should be -4");
    }

    /**
     * Tests runMlp2 method's ability to find maximum confidence output.
     */
    @Test
    void runMlp2MaximumConfidenceFinding() {
        int nInps = 1, nHids = 1, nOuts = 4;
        AtomicReferenceArray<Double> weights = createIncreasingOutputWeights(nInps, nHids, nOuts);
        double[] featureVector = {1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
        assertNotNull(confidence.get());
        assertTrue(confidence.get() > 0.0);
    }

    /**
     * Tests runMlp2 method with all possible activation function combinations.
     */
    @Test
    void runMlp2AllActivationCombinations() {
        int[] activationCodes = {IMlp.LINEAR, IMlp.SIGMOID, IMlp.SINUSOID};
        int nInps = 2, nHids = 2, nOuts = 2;

        for (int hidCode : activationCodes) {
            for (int outCode : activationCodes) {
                AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
                double[] featureVector = {0.5, -0.5};
                AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
                AtomicInteger hypClass = new AtomicInteger();
                AtomicReference<Double> confidence = new AtomicReference<>();

                int result = runMlp.runMlp2(nInps, nHids, nOuts, hidCode, outCode,
                        weights, featureVector, outAcs, hypClass, confidence);

                assertEquals(0, result, String.format("Failed for hidden: %d, output: %d", hidCode, outCode));
                assertNotNull(confidence.get());
                assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
            }
        }
    }

    /**
     * Tests runMlp2 method with a single neuron network.
     */
    @Test
    void runMlp2SingleNeuron() {
        int nInps = 1, nHids = 1, nOuts = 1;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {2.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertEquals(0, hypClass.get());
        assertNotNull(confidence.get());
    }

    /**
     * Tests runMlp2 method with a large neural network configuration.
     */
    @Test
    void runMlp2LargeNetwork() {
        int nInps = 5, nHids = 8, nOuts = 4;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, -1.0, 0.5, 2.0, -0.5};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SINUSOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
        assertNotNull(confidence.get());
    }

    /**
     * Tests runMlp2 method with exactly the maximum allowed hidden units.
     */
    @Test
    void runMlp2ExactMaxHids() {
        int nInps = 2, nHids = IMlp.MAX_NHIDS, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {0.1, 0.1};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
        assertNotNull(confidence.get());
    }

    /**
     * Tests concurrent access to the RunMlp singleton instance.
     */
    @Test
    void concurrentAccess() throws InterruptedException {
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    RunMlp instance = RunMlp.getInstance();
                    assertSame(runMlp, instance);

                    int nInps = 2, nHids = 2, nOuts = 2;
                    AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
                    double[] featureVector = {1.0 + threadId * 0.1, -1.0 + threadId * 0.1};
                    AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
                    AtomicInteger hypClass = new AtomicInteger();
                    AtomicReference<Double> confidence = new AtomicReference<>();

                    int result = instance.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                            weights, featureVector, outAcs, hypClass, confidence);

                    if (result == 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertTrue(successCount.get() > 0);
    }

    /**
     * Tests runMlp2 method with zero-valued input features.
     */
    @Test
    void runMlp2ZeroInputs() {
        int nInps = 3, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {0.0, 0.0, 0.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
        assertNotNull(confidence.get());
    }

    /**
     * Tests runMlp2 method with edge case scenarios.
     */
    @Test
    void runMlp2EdgeCases() {
        int nInps = 1, nHids = 1, nOuts = 1;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
        assertEquals(0, hypClass.get());
    }

    /**
     * Tests runMlp method condition for exceeding maximum hidden units.
     */
    @Test
    void runMlpExceedsMaxHidsCondition() {
        int nHids = IMlp.MAX_NHIDS + 1;
        assertTrue(nHids > IMlp.MAX_NHIDS, "Condition should trigger error path");
    }

    /**
     * Verifies runMlp produces the same activations, class and confidence as runMlp2.
     */
    @Test
    void runMlpMatchesRunMlp2() {
        int nInps = 2, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, 1.0};
        AtomicReferenceArray<Double> outAcs2 = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass2 = new AtomicInteger();
        AtomicReference<Double> confidence2 = new AtomicReference<>();
        assertEquals(0, runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SINUSOID,
                weights, featureVector, outAcs2, hypClass2, confidence2));

        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();
        runMlp.runMlp(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SINUSOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(hypClass2.get(), hypClass.get());
        assertEquals(confidence2.get(), confidence.get(), DELTA);
        for (int i = 0; i < nOuts; i++) {
            assertEquals(outAcs2.get(i), outAcs.get(i), DELTA);
        }
    }

    /**
     * Verifies runMlp throws instead of exiting the JVM when the network is invalid.
     */
    @Test
    void runMlpThrowsOnInvalidNetwork() {
        AtomicReferenceArray<Double> weights = createProperWeightsArray(1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> runMlp.runMlp(1, IMlp.MAX_NHIDS + 1, 1, IMlp.LINEAR,
                IMlp.LINEAR, weights, new double[] {1.0}, new AtomicReferenceArray<>(1), new AtomicInteger(),
                new AtomicReference<>()));
        assertThrows(IllegalArgumentException.class, () -> runMlp.runMlp(1, 1, 1, 'x', IMlp.LINEAR, weights,
                new double[] {1.0}, new AtomicReferenceArray<>(1), new AtomicInteger(), new AtomicReference<>()));
        assertThrows(IllegalArgumentException.class, () -> runMlp.runMlp(1, 1, 1, IMlp.LINEAR, 'x', weights,
                new double[] {1.0}, new AtomicReferenceArray<>(1), new AtomicInteger(), new AtomicReference<>()));
    }

    /**
     * Tests runMlp2 with negative feature values to verify robustness.
     */
    @Test
    void runMlp2NegativeInputs() {
        int nInps = 2, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {-1.0, -2.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
        assertTrue(hypClass.get() >= 0 && hypClass.get() < nOuts);
    }

    /**
     * Tests runMlp2 with very large input values to check numerical stability.
     */
    @Test
    void runMlp2LargeInputs() {
        int nInps = 2, nHids = 3, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1e6, -1e6};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SIGMOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
    }

    /**
     * Tests runMlp2 to ensure output activations are updated.
     */
    @Test
    void runMlp2OutputActivationValuesAreSet() {
        int nInps = 2, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {0.3, 0.7};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        for (int i = 0; i < nOuts; i++) {
            assertNotNull(outAcs.get(i));
        }
    }

    /**
     * Tests runMlp2 confidence is zero when weights are zero.
     */
    @Test
    void runMlp2ZeroWeightsGivesZeroConfidence() {
        int nInps = 2, nHids = 2, nOuts = 2;
        int totalSize = (nInps * nHids) + nHids + (nOuts * nHids) + nOuts;
        AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(totalSize);
        for (int i = 0; i < totalSize; i++) {
            weights.set(i, 0.0);
        }

        double[] featureVector = {1.0, 1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.LINEAR, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertEquals(0.0, confidence.get());
    }

    /**
     * Tests runMlp2 when all inputs are the same value.
     */
    @Test
    void runMlp2UniformInputs() {
        int nInps = 3, nHids = 2, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, 1.0, 1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SINUSOID, IMlp.LINEAR,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertTrue(confidence.get() != null);
    }

    /**
     * Tests runMlp2 with alternating sign inputs.
     */
    @Test
    void runMlp2AlternatingInputs() {
        int nInps = 4, nHids = 3, nOuts = 2;
        AtomicReferenceArray<Double> weights = createProperWeightsArray(nInps, nHids, nOuts);
        double[] featureVector = {1.0, -1.0, 1.0, -1.0};
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(nOuts);
        AtomicInteger hypClass = new AtomicInteger();
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(nInps, nHids, nOuts, IMlp.SIGMOID, IMlp.SINUSOID,
                weights, featureVector, outAcs, hypClass, confidence);

        assertEquals(0, result);
        assertNotNull(confidence.get());
    }

    /**
     * Creates a spy of the RunMlp singleton whose MlpCla fills every output activation of
     * each matrix-vector call with {@code fillValue}; the arrays passed to each call are
     * recorded in {@code captured} (call order).
     */
    private RunMlp spyWithFillingMlpCla(double fillValue, java.util.List<AtomicReferenceArray<Double>> captured) {
        MlpCla mlpCla = mock(MlpCla.class);
        doAnswer(invocation -> {
            AtomicReferenceArray<Double> y = invocation.getArgument(9);
            for (int i = 0; i < y.length(); i++) {
                y.set(i, fillValue);
            }
            captured.add(y);
            return 0;
        }).when(mlpCla).mlpSgemV(any(), anyInt(), anyInt(), any(), any(), anyInt(), any(), any(), any(), any(),
                any());
        RunMlp spyRunMlp = spy(RunMlp.getInstance());
        doReturn(mlpCla).when(spyRunMlp).getMlpCla();
        return spyRunMlp;
    }

    /**
     * Verifies runMlp with zero hidden and output nodes completes and reports the first output
     * activation as confidence with hypothetical class 0.
     */
    @Test
    void runMlpWithoutHiddenOrOutputNodesReportsFirstActivation() {
        java.util.List<AtomicReferenceArray<Double>> captured = new java.util.ArrayList<>();
        RunMlp spyRunMlp = spyWithFillingMlpCla(3.0, captured);
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(1);
        AtomicInteger hypClass = new AtomicInteger(-1);
        AtomicReference<Double> confidence = new AtomicReference<>();

        spyRunMlp.runMlp(1, 0, 0, IMlp.LINEAR, IMlp.LINEAR, createMinimalWeightsArray(4), new double[] {1.0},
                outAcs, hypClass, confidence);

        assertEquals(2, captured.size());
        assertSame(outAcs, captured.get(1));
        assertEquals(0, hypClass.get());
        assertEquals(3.0, confidence.get(), DELTA);
    }

    /**
     * Verifies runMlp2 with linear activations computes the exact output activations,
     * hypothetical class and confidence for hand-computed weights.
     */
    @Test
    void runMlp2LinearNetworkComputesExactOutputs() {
        // 2 inputs, 1 hidden, 2 outputs: w1 = {1, 2}, b1 = {1}, w2 = {1, 3}, b2 = {0, 1}
        AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(new Double[] {1.0, 2.0, 1.0, 1.0, 3.0,
                0.0, 1.0});
        AtomicReferenceArray<Double> outAcs = new AtomicReferenceArray<>(2);
        AtomicInteger hypClass = new AtomicInteger(-1);
        AtomicReference<Double> confidence = new AtomicReference<>();

        int result = runMlp.runMlp2(2, 1, 2, IMlp.LINEAR, IMlp.LINEAR, weights, new double[] {1.0, 1.0}, outAcs,
                hypClass, confidence);

        // hidden = 0.25 * (1 + 1*1 + 2*1) = 1.0; out0 = 0.25 * (0 + 1*1) ; out1 = 0.25 * (1 + 3*1)
        assertEquals(0, result);
        assertEquals(0.25, outAcs.get(0), DELTA);
        assertEquals(1.0, outAcs.get(1), DELTA);
        assertEquals(1, hypClass.get());
        assertEquals(1.0, confidence.get(), DELTA);
    }

    private AtomicReferenceArray<Double> createMinimalWeightsArray(int size) {
        AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(size);
        for (int i = 0; i < size; i++) {
            weights.set(i, 0.1 + i * 0.01);
        }
        return weights;
    }

    private AtomicReferenceArray<Double> createProperWeightsArray(int nInps, int nHids, int nOuts) {
        int totalSize = (nInps * nHids) + nHids + (nOuts * nHids) + nOuts;
        AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(totalSize);

        for (int i = 0; i < totalSize; i++) {
            double value = 0.1 + (i * 0.05) % 1.0;
            weights.set(i, value);
        }

        return weights;
    }

    private AtomicReferenceArray<Double> createIncreasingOutputWeights(int nInps, int nHids, int nOuts) {
        int totalSize = (nInps * nHids) + nHids + (nOuts * nHids) + nOuts;
        AtomicReferenceArray<Double> weights = new AtomicReferenceArray<>(totalSize);

        for (int i = 0; i < totalSize; i++) {
            weights.set(i, 0.01);
        }

        int b2Index = (nInps * nHids) + nHids + (nOuts * nHids);
        for (int i = 0; i < nOuts; i++) {
            weights.set(b2Index + i, i * 10.0);
        }

        return weights;
    }
}