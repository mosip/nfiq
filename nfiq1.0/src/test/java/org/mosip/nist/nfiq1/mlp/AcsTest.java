package org.mosip.nist.nfiq1.mlp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Acs} providing comprehensive validation of activation function
 * implementations for neural network processing.
 *
 * <p>This class validates the functionality of various activation functions including
 * sinusoid, sigmoid, and linear functions used in NIST's neural network quality
 * assessment system for fingerprint image quality analysis.</p>
 */
@ExtendWith(MockitoExtension.class)
class AcsTest {

    private Acs acs;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes Acs singleton instance for testing.
     */
    @BeforeEach
    void setUp() {
        acs = Acs.getInstance();
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Acs instance1 = Acs.getInstance();
        Acs instance2 = Acs.getInstance();
        assertEquals(instance1, instance2);
    }

    /**
     * Validates acSinusoid activation function with zero input value.
     * Tests sinusoid function returning expected midpoint value and derivative.
     */
    @Test
    void acSinusoidWithZeroInputReturnsHalf() {
        AtomicReference<Float> val = new AtomicReference<>();
        AtomicReference<Float> deriv = new AtomicReference<>();

        acs.acSinusoid(0.0f, val, deriv);

        assertEquals(0.5f, val.get(), 0.001f);
        assertEquals(0.25f, deriv.get(), 0.001f);
    }

    /**
     * Validates acSinusoid activation function with positive input value.
     * Tests sinusoid function returning values within expected range bounds.
     */
    @Test
    void acSinusoidWithPositiveInputReturnsValidRange() {
        AtomicReference<Float> val = new AtomicReference<>();
        AtomicReference<Float> deriv = new AtomicReference<>();

        acs.acSinusoid(1.0f, val, deriv);

        assertTrue(val.get() >= 0.0f && val.get() <= 1.0f);
        assertTrue(deriv.get() >= 0.0f && deriv.get() <= 0.25f);
    }

    /**
     * Validates acSinusoid activation function with negative input value.
     * Tests sinusoid function handling negative inputs within expected bounds.
     */
    @Test
    void acSinusoidWithNegativeInputReturnsValidRange() {
        AtomicReference<Float> val = new AtomicReference<>();
        AtomicReference<Float> deriv = new AtomicReference<>();

        acs.acSinusoid(-1.0f, val, deriv);

        assertTrue(val.get() >= 0.0f && val.get() <= 1.0f);
        assertTrue(deriv.get() >= 0.0f && deriv.get() <= 0.25f);
    }

    /**
     * Validates acVSinusoid vector activation function modifies array values.
     * Tests in-place sinusoid transformation of array elements.
     */
    @Test
    void acVSinusoidModifiesArrayValue() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, 0.0);

        acs.acVSinusoid(p, 0);

        assertEquals(0.5, p.get(0), 0.001);
    }

    /**
     * Validates acVSinusoid vector function with positive input value.
     * Tests vector sinusoid function producing values within valid range.
     */
    @Test
    void acVSinusoidWithPositiveValueModifiesCorrectly() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, 1.0);

        acs.acVSinusoid(p, 0);

        assertTrue(p.get(0) >= 0.0 && p.get(0) <= 1.0);
    }

    /**
     * Validates acSigmoid activation function with zero input value.
     * Tests sigmoid function returning expected midpoint value and derivative.
     */
    @Test
    void acSigmoidWithZeroInputReturnsHalf() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acSigmoid(0.0, val, deriv);

        assertEquals(0.5, val.get(), 0.001);
        assertEquals(0.25, deriv.get(), 0.001);
    }

    /**
     * Validates acSigmoid activation function with large positive input.
     * Tests sigmoid function saturation behavior approaching unity.
     */
    @Test
    void acSigmoidWithLargePositiveInputReturnsOne() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acSigmoid(10.0, val, deriv);

        assertTrue(val.get() > 0.9);
        assertTrue(deriv.get() >= 0.0);
    }

    /**
     * Validates acSigmoid activation function with large negative input.
     * Tests sigmoid function saturation behavior approaching zero.
     */
    @Test
    void acSigmoidWithLargeNegativeInputReturnsZero() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acSigmoid(-100.0, val, deriv);

        assertEquals(0.0, val.get(), 0.001);
        assertEquals(0.0, deriv.get(), 0.001);
    }

    /**
     * Validates acVSigmoid vector activation function with zero input.
     * Tests in-place sigmoid transformation returning expected midpoint value.
     */
    @Test
    void acVSigmoidWithZeroInputReturnsHalf() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, 0.0);

        acs.acVSigmoid(p, 0);

        assertEquals(0.5, p.get(0), 0.001);
    }

    /**
     * Validates acVSigmoid vector function with large negative input.
     * Tests vector sigmoid function saturation behavior at lower bound.
     */
    @Test
    void acVSigmoidWithLargeNegativeInputReturnsZero() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, -100.0);

        acs.acVSigmoid(p, 0);

        assertEquals(0.0, p.get(0), 0.001);
    }

    /**
     * Validates acLinear activation function with zero input value.
     * Tests linear function returning zero value with expected derivative.
     */
    @Test
    void acLinearWithZeroInputReturnsZero() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acLinear(0.0f, val, deriv);

        assertEquals(0.0, val.get(), 0.001);
        assertEquals(0.25, deriv.get(), 0.001);
    }

    /**
     * Validates acSinusoid function with float input precision.
     * Tests sinusoid activation using single-precision floating point input.
     */
    @Test
    void acSinusoidWithFloatInputCalculatesCorrectly() {
        AtomicReference<Float> val = new AtomicReference<>();
        AtomicReference<Float> deriv = new AtomicReference<>();

        acs.acSinusoid(0.0f, val, deriv);

        assertEquals(0.5f, val.get(), 0.001f);
        assertEquals(0.25f, deriv.get(), 0.001f);
    }

    /**
     * Validates acSinusoid function with positive float input calculation.
     * Tests sinusoid activation producing values above midpoint for positive inputs.
     */
    @Test
    void acSinusoidWithPositiveFloatInputCalculatesCorrectly() {
        AtomicReference<Float> val = new AtomicReference<>();
        AtomicReference<Float> deriv = new AtomicReference<>();

        acs.acSinusoid(1.0f, val, deriv);

        assertTrue(val.get() > 0.5f && val.get() <= 1.0f);
        assertTrue(deriv.get() >= 0.0f);
    }

    /**
     * Validates acSigmoid function with double precision input.
     * Tests sigmoid activation using double-precision floating point input.
     */
    @Test
    void acSigmoidWithDoubleInputCalculatesCorrectly() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acSigmoid(0.0, val, deriv);

        assertEquals(0.5, val.get(), 0.001);
        assertEquals(0.25, deriv.get(), 0.001);
    }

    /**
     * Validates acLinear function with positive input scaling behavior.
     * Tests linear activation function scaling input to unit output range.
     */
    @Test
    void acLinearWithPositiveInputScalesCorrectly() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acLinear(4.0f, val, deriv);

        assertEquals(1.0, val.get(), 0.001);
        assertEquals(0.25, deriv.get(), 0.001);
    }

    /**
     * Validates acLinear function returning scaled value for positive input.
     * Tests linear activation producing proportional output for given input.
     */
    @Test
    void acLinearWithPositiveInputReturnsScaledValue() {
        AtomicReference<Double> val = new AtomicReference<>();
        AtomicReference<Double> deriv = new AtomicReference<>();

        acs.acLinear(4.0f, val, deriv);

        assertEquals(1.0, val.get(), 0.001);
        assertEquals(0.25, deriv.get(), 0.001);
    }

    /**
     * Validates acVLinear vector activation function modifying array values.
     * Tests in-place linear transformation of array elements.
     */
    @Test
    void acVLinearModifiesArrayValue() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, 4.0);

        acs.acVLinear(p, 0);

        assertEquals(1.0, p.get(0), 0.001);
    }

    /**
     * Validates acVLinear vector function with zero input value.
     * Tests vector linear function returning zero for zero input.
     */
    @Test
    void acVLinearWithZeroInputReturnsZero() {
        AtomicReferenceArray<Double> p = new AtomicReferenceArray<>(1);
        p.set(0, 0.0);

        acs.acVLinear(p, 0);

        assertEquals(0.0, p.get(0), 0.001);
    }
}
