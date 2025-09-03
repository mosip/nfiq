package org.mosip.nist.nfiq1.mlp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link Mlp} providing validation of neural network base class functionality.
 *
 * <p>This class validates the basic instantiation and initialization of the Mlp class
 * used as the foundation for neural network operations in NIST's fingerprint quality
 * assessment system.</p>
 */
@ExtendWith(MockitoExtension.class)
class MlpTest {

    private Mlp mlp;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes a new Mlp instance for testing.
     */
    @BeforeEach
    void setUp() {
        mlp = new Mlp();
    }

    /**
     * Validates that the Mlp constructor creates a valid instance.
     * Ensures proper instantiation of the neural network base class.
     */
    @Test
    void constructorCreatesInstance() {
        assertNotNull(mlp);
    }
}
