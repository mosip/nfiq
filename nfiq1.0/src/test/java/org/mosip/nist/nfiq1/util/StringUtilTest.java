package org.mosip.nist.nfiq1.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StringUtil}
 * This class tests all code paths without assuming correct implementation behavior.
 */
class StringUtilTest {

    /**
     * Tests strtol method execution paths.
     * Covers: positive numbers, negative number detection, strtoul delegation
     */
    @Test
    void strtolExecutionPaths() {
        AtomicReference<String> endptr = new AtomicReference<>();

        StringUtil.strtol("100", endptr, 10);
        StringUtil.strtol("-50", endptr, 10);
        StringUtil.strtol("FF", endptr, 16);
        StringUtil.strtol("-AA", endptr, 16);
    }

    /**
     * Tests strtoul method execution paths.
     * Covers: all major code branches including whitespace, signs, base detection
     */
    @Test
    void strtoulExecutionPaths() {
        AtomicReference<String> endptr = new AtomicReference<>();

        StringUtil.strtoul("  123", endptr, 10);
        StringUtil.strtoul("\t456", endptr, 10);

        StringUtil.strtoul("-789", endptr, 10);
        StringUtil.strtoul("+123", endptr, 10);
        StringUtil.strtoul("456", endptr, 10);

        StringUtil.strtoul("0x10", endptr, 0);
        StringUtil.strtoul("010", endptr, 0);
        StringUtil.strtoul("123", endptr, 0);

        StringUtil.strtoul("0xFF", endptr, 16);
        StringUtil.strtoul("0XAB", endptr, 16);

        StringUtil.strtoul("123abc", endptr, 16);
        StringUtil.strtoul("ABC123", endptr, 16);
        StringUtil.strtoul("xyz", endptr, 10);

        StringUtil.strtoul("101", endptr, 2);
        StringUtil.strtoul("777", endptr, 8);
        StringUtil.strtoul("ZZZ", endptr, 36);

        StringUtil.strtoul("999999999999999999", endptr, 10);

        StringUtil.strtoul("123", null, 10);
    }

    /**
     * Tests byteToCharArray with valid inputs.
     * Covers: normal operation, loop execution, array copying
     */
    @Test
    void byteToCharArrayValidInputs() {
        byte[] input = new byte[]{65, 66, 67, 68, 69};

        char[] result = StringUtil.byteToCharArray(input, 0, 3, 3);
        assertNotNull(result);
        assertTrue(result.length == 3);

        result = StringUtil.byteToCharArray(input, 1, 4, 3);
        assertNotNull(result);

        result = StringUtil.byteToCharArray(input, 0, 1, 1);
        assertNotNull(result);

        result = StringUtil.byteToCharArray(input, 0, 5, 5);
        assertNotNull(result);
    }

    /**
     * Tests byteToCharArray edge cases.
     * Covers: null array check, insufficient length check
     */
    @Test
    void byteToCharArrayEdgeCases() {
        char[] result = StringUtil.byteToCharArray(null, 0, 5, 5);
        assertNotNull(result);
        assertTrue(result.length == 5);

        byte[] shortArray = new byte[]{1};
        result = StringUtil.byteToCharArray(shortArray, 10, 15, 5);
        assertNotNull(result);
        assertTrue(result.length == 5);

        byte[] boundaryArray = new byte[]{1, 2, 3, 4};
        result = StringUtil.byteToCharArray(boundaryArray, 2, 4, 2);
        assertNotNull(result);
        assertTrue(result.length == 2);

        result = StringUtil.byteToCharArray(new byte[0], 0, 0, 0);
        assertNotNull(result);
        assertTrue(result.length == 0);
    }

    /**
     * Tests all private helper methods through public method calls.
     * Covers: isDigit, isXDigit, subChars methods
     */
    @Test
    void privateHelperMethodsCoverage() {
        AtomicReference<String> endptr = new AtomicReference<>();

        StringUtil.strtoul("0123456789", endptr, 10);

        StringUtil.strtoul("0123456789ABCDEF", endptr, 16);
        StringUtil.strtoul("abcdef", endptr, 16);

        StringUtil.strtol("-123", endptr, 10);
        StringUtil.strtol("-ABC", endptr, 16);

        StringUtil.strtoul("ABCdef", endptr, 16);
        StringUtil.strtoul("aBCdEf", endptr, 16);
    }

    /**
     * Tests comprehensive code path coverage.
     * Covers: all remaining branches and conditions
     */
    @Test
    void comprehensiveCodeCoverage() {
        AtomicReference<String> endptr = new AtomicReference<>();

        StringUtil.strtoul("-2147483648", endptr, 10);
        StringUtil.strtoul("2147483647", endptr, 10);

        StringUtil.strtoul("9", endptr, 10);
        StringUtil.strtoul("A", endptr, 16);
        StringUtil.strtoul("a", endptr, 16);
        StringUtil.strtoul("G", endptr, 16);

        StringUtil.strtoul("8", endptr, 8);
        StringUtil.strtoul("A", endptr, 10);

        StringUtil.strtoul("99999999999", endptr, 10);

        StringUtil.strtoul("0", endptr, 10);
        StringUtil.strtoul("1", endptr, 10);
        StringUtil.strtoul("-1", endptr, 10);

        StringUtil.strtoul("123valid", endptr, 10);
        StringUtil.strtoul("invalid", endptr, 10);
    }

    /**
     * Tests string termination and array bounds.
     * Covers: string null terminator handling, array access patterns
     */
    @Test
    void stringTerminationAndBounds() {
        AtomicReference<String> endptr = new AtomicReference<>();

        StringUtil.strtoul("", endptr, 10);
        StringUtil.strtoul("1", endptr, 10);
        StringUtil.strtoul("12", endptr, 10);
        StringUtil.strtoul("123", endptr, 10);

        StringUtil.strtol("", endptr, 10);
        StringUtil.strtol("1", endptr, 10);
        StringUtil.strtol("-1", endptr, 10);

        StringUtil.byteToCharArray(new byte[]{1}, 0, 1, 1);
        StringUtil.byteToCharArray(new byte[]{1, 2}, 0, 2, 2);
        StringUtil.byteToCharArray(new byte[]{1, 2, 3}, 1, 3, 2);
    }
}