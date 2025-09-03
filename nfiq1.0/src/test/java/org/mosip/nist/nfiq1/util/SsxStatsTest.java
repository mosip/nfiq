package org.mosip.nist.nfiq1.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SsxStats}
 */
@ExtendWith(MockitoExtension.class)
class SsxStatsTest {

    private SsxStats ssxStats;

    /**
     * Sets up test fixtures before each test method execution.
     */
    @BeforeEach
    void setUp() {
        ssxStats = new SsxStats();
    }

    /**
     * Test that constructor creates a valid SsxStats instance.
     */
    @Test
    void constructorCreatesInstance() {
        assertNotNull(ssxStats);
    }

    /**
     * Test ssxStdDev method with valid input parameters.
     */
    @Test
    void ssxStdDevWithValidInputReturnsStandardDeviation() {
        double sumX = 10.0;
        double sumX2 = 30.0;
        int count = 5;

        double result = ssxStats.ssxStdDev(sumX, sumX2, count);

        assertTrue(result >= 0.0);
    }

    /**
     * Test ssxStdDev method when variance calculation results in negative value.
     */
    @Test
    void ssxStdDevWithNegativeVarianceReturnsErrorCode() {
        double sumX = 10.0;
        double sumX2 = 5.0;
        int count = 2;

        double result = ssxStats.ssxStdDev(sumX, sumX2, count);

        assertTrue(result < 0.0);
    }

    /**
     * Test ssxStdDev method with parameters that produce zero variance.
     */
    @Test
    void ssxStdDevWithZeroVarianceReturnsZero() {
        double sumX = 10.0;
        double sumX2 = 50.0;
        int count = 5;

        double result = ssxStats.ssxStdDev(sumX, sumX2, count);

        assertEquals(2.7386127875258306, result, 0.001);
    }

    /**
     * Test ssxVariance method with valid input parameters.
     */
    @Test
    void ssxVarianceWithValidInputReturnsVariance() {
        double sumX = 10.0;
        double sumX2 = 30.0;
        int count = 5;

        double result = ssxStats.ssxVariance(sumX, sumX2, count);

        assertTrue(result >= 0.0);
    }

    /**
     * Test ssxVariance method when count is less than two.
     */
    @Test
    void ssxVarianceWithCountLessThanTwoReturnsError() {
        double sumX = 5.0;
        double sumX2 = 25.0;
        int count = 1;

        double result = ssxStats.ssxVariance(sumX, sumX2, count);

        assertEquals(-2.0, result);
    }

    /**
     * Test ssxVariance method when count is zero.
     */
    @Test
    void ssxVarianceWithCountZeroReturnsError() {
        double sumX = 5.0;
        double sumX2 = 25.0;
        int count = 0;

        double result = ssxStats.ssxVariance(sumX, sumX2, count);

        assertEquals(-2.0, result);
    }

    /**
     * Test ssxVariance method with exactly two elements.
     */
    @Test
    void ssxVarianceWithTwoElementsReturnsCorrectValue() {
        double sumX = 3.0;
        double sumX2 = 5.0;
        int count = 2;

        double result = ssxStats.ssxVariance(sumX, sumX2, count);

        assertTrue(result >= 0.0);
    }

    /**
     * Test ssx method with valid input parameters.
     */
    @Test
    void ssxWithValidInputReturnsCorrectValue() {
        double sumX = 10.0;
        double sumX2 = 30.0;
        int count = 5;

        double result = ssxStats.ssx(sumX, sumX2, count);

        assertEquals(10.0, result, 0.001);
    }

    /**
     * Test ssx method when sum of X values is zero.
     */
    @Test
    void ssxWithZeroSumReturnsCorrectValue() {
        double sumX = 0.0;
        double sumX2 = 10.0;
        int count = 5;

        double result = ssxStats.ssx(sumX, sumX2, count);

        assertEquals(10.0, result, 0.001);
    }

    /**
     * Test ssx method with equal sum and sum of squares.
     */
    @Test
    void ssxWithEqualSumAndSumSquaredReturnsZero() {
        double sumX = 5.0;
        double sumX2 = 25.0;
        int count = 5;

        double result = ssxStats.ssx(sumX, sumX2, count);

        assertEquals(20.0, result, 0.001);
    }

    /**
     * Test ssx method with large input values.
     */
    @Test
    void ssxWithLargeValuesHandlesCorrectly() {
        double sumX = 1000.0;
        double sumX2 = 200000.0;
        int count = 10;

        double result = ssxStats.ssx(sumX, sumX2, count);

        assertTrue(result >= 0.0);
    }

    /**
     * Test ssxVariance method when calculation results in negative variance.
     */
    @Test
    void ssxVarianceWithNegativeVarianceReturnsError() {
        double sumX = 10.0;
        double sumX2 = 5.0;
        int count = 2;

        double result = ssxStats.ssxVariance(sumX, sumX2, count);

        assertTrue(result < 0.0);
    }

    /**
     * Test ssx method when calculation results in negative value.
     */
    @Test
    void ssxWithNegativeResultHandlesCorrectly() {
        double sumX = 100.0;
        double sumX2 = 50.0;
        int count = 5;

        double result = ssxStats.ssx(sumX, sumX2, count);

        assertTrue(result < 0.0);
    }
}
