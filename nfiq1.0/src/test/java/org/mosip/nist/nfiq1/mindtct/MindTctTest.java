package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link MindTct} providing validation of base functionality
 * for the NIST fingerprint analysis system.
 *
 * <p>This class validates the core MindTct class instantiation and basic
 * functionality used as the foundation for fingerprint image processing
 * and minutiae extraction algorithms.</p>
 */
@ExtendWith(MockitoExtension.class)
class MindTctTest {

    private MindTct mindTct;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes a new MindTct instance for testing.
     */
    @BeforeEach
    void setUp() {
        mindTct = new MindTct();
    }

    /**
     * Validates that the MindTct constructor creates a valid instance.
     * Ensures proper instantiation of the base MindTct class.
     */
    @Test
    void constructorCreatesInstance() {
        assertNotNull(mindTct);
    }
}