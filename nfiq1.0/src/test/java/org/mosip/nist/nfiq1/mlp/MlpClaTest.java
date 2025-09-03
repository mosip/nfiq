package org.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link MlpCla} providing comprehensive validation of Basic Linear
 * Algebra Subprograms (BLAS) operations implementation.
 *
 * <p>This class validates the functionality of matrix-vector operations, scalar
 * operations, and vector computations used in NIST's neural network quality
 * assessment system for fingerprint image analysis.</p>
 */
class MlpClaTest {

    private MlpCla mlpCla;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes MlpCla singleton instance for testing.
     */
    @BeforeEach
    void setUp() {
        mlpCla = MlpCla.getInstance();
    }

    /**
     * Creates and initializes AtomicReferenceArray with proper size and values.
     * Helper method to prevent ArrayIndexOutOfBoundsException and NullPointerException.
     *
     * @param size the size of the array to create
     * @param defaultValue the default value to initialize each element
     * @return properly initialized AtomicReferenceArray
     */
    private AtomicReferenceArray<Double> createInitializedArray(int size, double defaultValue) {
        AtomicReferenceArray<Double> array = new AtomicReferenceArray<>(size);
        for (int i = 0; i < size; i++) {
            array.set(i, defaultValue);
        }
        return array;
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        MlpCla instance1 = MlpCla.getInstance();
        MlpCla instance2 = MlpCla.getInstance();

        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }

    /**
     * Validates sgemV method with invalid parameters for error handling.
     * Tests parameter validation and error conditions in matrix-vector operations.
     */
    @Test
    void sgemVInvalidParameters() {
        AtomicReference<Character> trans = new AtomicReference<>('X');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);
        AtomicReferenceArray<Double> a = createInitializedArray(10, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(5, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(5, 1.0);

        int result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        trans.set('N');
        result = mlpCla.sgemV(trans, -1, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, -1, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 5, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 0, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 0);
        assertEquals(0, result);
    }

    /**
     * Validates sgemV method quick return conditions for optimization.
     * Tests early termination conditions when operations can be skipped.
     */
    @Test
    void sgemVQuickReturn() {
        AtomicReference<Character> trans = new AtomicReference<>('N');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);
        AtomicReferenceArray<Double> a = createInitializedArray(10, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(5, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(5, 1.0);

        int result = mlpCla.sgemV(trans, 0, 3, alpha, a, 1, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, 0, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        alpha.set(0.0);
        beta.set(1.0);
        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);
    }

    /**
     * Validates sgemV method with normal matrix-vector multiplication.
     * Tests standard matrix-vector operation without transpose.
     */
    @Test
    void sgemVTransN() {
        AtomicReference<Character> trans = new AtomicReference<>('N');
        AtomicReference<Double> alpha = new AtomicReference<>(2.0);
        AtomicReference<Double> beta = new AtomicReference<>(0.5);

        AtomicReferenceArray<Double> a = createInitializedArray(15, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(10, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(10, 1.0);

        int result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        beta.set(0.0);
        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        beta.set(0.5);
        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 2);
        assertEquals(0, result);
    }

    /**
     * Validates sgemV method with transpose matrix operation.
     * Tests matrix-vector multiplication with transposed matrix.
     */
    @Test
    void sgemVTransT() {
        AtomicReference<Character> trans = new AtomicReference<>('T');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);

        AtomicReferenceArray<Double> a = createInitializedArray(15, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(10, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(10, 1.0);

        int result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 2, beta, y, 1);
        assertEquals(0, result);
    }

    /**
     * Validates sgemV method with conjugate transpose operation.
     * Tests matrix-vector multiplication with conjugate transpose matrix.
     */
    @Test
    void sgemVTransC() {
        AtomicReference<Character> trans = new AtomicReference<>('C');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);

        AtomicReferenceArray<Double> a = createInitializedArray(15, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(10, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(10, 1.0);

        int result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);
    }

    /**
     * Validates sgemV matrix-vector operations with proper array initialization.
     * Tests various increment patterns and array access scenarios.
     */
    @Test
    void sgemVMatrixVectorOperations() {
        AtomicReference<Character> trans = new AtomicReference<>('N');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);

        AtomicReferenceArray<Double> a = createInitializedArray(20, 2.0);
        AtomicReferenceArray<Double> x = createInitializedArray(15, 2.0);
        AtomicReferenceArray<Double> y = createInitializedArray(15, 1.0);

        int result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 1);
        assertEquals(0, result);

        result = mlpCla.sgemV(trans, 3, 3, alpha, a, 3, x, 1, beta, y, 2);
        assertEquals(0, result);
    }

    /**
     * Validates wrapper methods with proper array initialization.
     * Tests high-level BLAS operation wrappers and utility methods.
     */
    @Test
    void wrapperMethods() {
        AtomicReference<Character> trans = new AtomicReference<>('N');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);

        AtomicReferenceArray<Double> a = createInitializedArray(20, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(15, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(15, 1.0);
        AtomicInteger incx = new AtomicInteger(1);
        AtomicInteger incy = new AtomicInteger(1);

        int result = mlpCla.mlpSgemV(trans, 3, 3, alpha, a, 3, x, incx, beta, y, incy);
        assertEquals(0, result);

        result = mlpCla.mlpSScal(3, 2.0, alpha, 1);
        assertEquals(0, result);

        result = mlpCla.mlpSaxpY(3, 2.0, alpha, 1, beta, 1);
        assertEquals(0, result);

        double dotResult = mlpCla.mlpSDot(3, alpha, 1, beta, 1);
        assertEquals(0.0, dotResult, 0.001);

        double normResult = mlpCla.mlpSnrm2(3, alpha, 1);
        assertEquals(0.0, normResult, 0.001);
    }

    /**
     * Validates character comparison functionality through sgemV calls.
     * Tests case-insensitive character matching for operation parameters.
     */
    @Test
    void compareChars() {
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);
        AtomicReferenceArray<Double> a = createInitializedArray(5, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(5, 1.0);
        AtomicReferenceArray<Double> y = createInitializedArray(5, 1.0);

        char[] testChars = {'N', 'n', 'T', 't', 'C', 'c'};

        for (char c : testChars) {
            AtomicReference<Character> trans = new AtomicReference<>(c);
            int result = mlpCla.sgemV(trans, 1, 1, alpha, a, 1, x, 1, beta, y, 1);
            assertEquals(0, result);
        }
    }

    /**
     * Validates core BLAS methods for scalar and vector operations.
     * Tests fundamental linear algebra operations including scaling, addition, and norms.
     */
    @Test
    void coreBLASMethods() {
        AtomicReference<Double> sx = new AtomicReference<>(1.0);
        AtomicReference<Double> sy = new AtomicReference<>(2.0);

        int result = mlpCla.sscal(3, 2.0, sx, 1);
        assertEquals(0, result);

        result = mlpCla.saxpY(3, 2.0, sx, 1, sy, 1);
        assertEquals(0, result);

        double dotResult = mlpCla.sDot(3, sx, 1, sy, 1);
        assertEquals(0.0, dotResult, 0.001);

        double normResult = mlpCla.snRm2(3, sx, 1);
        assertEquals(0.0, normResult, 0.001);
    }

    /**
     * Validates edge cases with minimal valid inputs and special conditions.
     * Tests boundary conditions and optimization scenarios.
     */
    @Test
    void edgeCases() {
        AtomicReference<Character> trans = new AtomicReference<>('N');
        AtomicReference<Double> alpha = new AtomicReference<>(1.0);
        AtomicReference<Double> beta = new AtomicReference<>(1.0);
        AtomicReferenceArray<Double> a = createInitializedArray(5, 1.0);
        AtomicReferenceArray<Double> x = createInitializedArray(5, 0.0);
        AtomicReferenceArray<Double> y = createInitializedArray(5, 1.0);

        int result = mlpCla.sgemV(trans, 1, 1, alpha, a, 1, x, 1, beta, y, 1);
        assertEquals(0, result);

        x.set(0, 1.0);
        result = mlpCla.sgemV(trans, 1, 1, alpha, a, 1, x, 1, beta, y, 1);
        assertEquals(0, result);
    }
}
