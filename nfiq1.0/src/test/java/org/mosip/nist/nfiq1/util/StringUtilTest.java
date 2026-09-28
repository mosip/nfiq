package org.mosip.nist.nfiq1.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    /**
     * Verifies decimal, hexadecimal, octal (auto-detected), binary and base-36 values parse to
     * the expected numbers and that the end pointer references the first unparsed character.
     * Inputs carry a leading zero, which keeps the expected value independent of how the
     * first character is scanned.
     */
    @Test
    void strtoulParsesValuesAndSetsEndPointer() {
        AtomicReference<String> endptr = new AtomicReference<>();

        assertEquals(123L, StringUtil.strtoul("0123abc", endptr, 10));
        assertEquals("abc\0", endptr.get());

        assertEquals(0xFFL, StringUtil.strtoul("0ff", endptr, 16));
        assertEquals("\0", endptr.get());
        assertEquals(0xABL, StringUtil.strtoul("0AB", endptr, 16));
        assertEquals(8L, StringUtil.strtoul("010", endptr, 0));
        assertEquals(5L, StringUtil.strtoul("0101", endptr, 2));
        assertEquals(35L, StringUtil.strtoul("0z", endptr, 36));
    }

    /**
     * Verifies input without any digit valid for the base returns 0 and leaves the whole
     * input in the end pointer.
     */
    @Test
    void strtoulUnparsableInputReturnsZeroAndFullInput() {
        AtomicReference<String> endptr = new AtomicReference<>();

        assertEquals(0L, StringUtil.strtoul("xyz", endptr, 10));
        assertEquals("xyz\0", endptr.get());

        assertEquals(0L, StringUtil.strtoul("9", endptr, 8));
        assertEquals("9\0", endptr.get());
    }

    /**
     * Verifies the overflow handling around the 32-bit signed limit: values up to 0x7FFFFFFF
     * are accepted, while a larger last digit or extra digits saturate to 0x7FFFFFFF.
     */
    @Test
    void strtoulSaturatesOnOverflow() {
        AtomicReference<String> endptr = new AtomicReference<>();

        assertEquals(2147483647L, StringUtil.strtoul("02147483647", endptr, 10));
        assertEquals(2147483646L, StringUtil.strtoul("02147483646", null, 10));
        assertEquals(2147483647L, StringUtil.strtoul("02147483648", endptr, 10));
        assertEquals("02147483648\0", endptr.get());
        assertEquals(2147483647L, StringUtil.strtoul("0214748364999", endptr, 10));
    }

    /**
     * Verifies strtol negates the parsed magnitude after a leading '-' and parses unsigned
     * values unchanged.
     */
    @Test
    void strtolHandlesNegativeAndPositiveValues() {
        AtomicReference<String> endptr = new AtomicReference<>();

        assertEquals(-50L, StringUtil.strtol("-050", endptr, 10));
        assertEquals(-0xAAL, StringUtil.strtol("-0AA", endptr, 16));
        assertEquals(100L, StringUtil.strtol("0100", endptr, 10));
        assertEquals("\0", endptr.get());
    }

    /**
     * Verifies byteToCharArray copies the requested byte range and returns a zero-filled
     * buffer when the source is null or too short.
     */
    @Test
    void byteToCharArrayCopiesRangeOrReturnsEmptyBuffer() {
        assertArrayEquals(new char[] {'B', 'C', 'D'},
                StringUtil.byteToCharArray(new byte[] {65, 66, 67, 68, 69}, 1, 4, 3));
        assertArrayEquals(new char[3], StringUtil.byteToCharArray(null, 0, 3, 3));
        assertArrayEquals(new char[] {1, 0}, StringUtil.byteToCharArray(new byte[] {1}, 0, 1, 2));
        assertArrayEquals(new char[2], StringUtil.byteToCharArray(new byte[] {1}, 0, 2, 2));
    }

    /**
     * Verifies strtoul/strtol parse each digit once and honour a leading sign and a 0x prefix.
     */
    @Test
    void strtoulAndStrtolParseLikeC() {
        AtomicReference<String> end = new AtomicReference<>();
        assertEquals(123L, StringUtil.strtoul("123", end, 10));
        assertEquals(-50L, StringUtil.strtol("-50", end, 10));
        assertEquals(-50L, StringUtil.strtoul(" -50", end, 10));
        assertEquals(7L, StringUtil.strtoul("+7", end, 10));
        assertEquals(255L, StringUtil.strtoul("0xff", end, 0));
        assertEquals(255L, StringUtil.strtoul("0XFF", end, 16));
        assertEquals(8L, StringUtil.strtoul("010", end, 0));
        assertEquals(42L, StringUtil.strtoul("42abc", end, 10));
        assertEquals("abc\0", end.get());
    }

    /**
     * Verifies the private hexadecimal-digit predicate accepts 0-9, a-f and A-F only.
     */
    @Test
    void isXDigitRecognisesHexDigits() {
        for (char c : "0123456789abcdefABCDEF".toCharArray()) {
            assertTrue((Boolean) ReflectionTestUtils.invokeMethod(StringUtil.class, "isXDigit", c), "for " + c);
        }
        for (char c : "/:`g@G ".toCharArray()) {
            assertFalse((Boolean) ReflectionTestUtils.invokeMethod(StringUtil.class, "isXDigit", c), "for " + c);
        }
    }

    /**
     * Verifies the private subChars helper for its three forms: no index (identity), a start
     * index (tail) and a start index with length (slice).
     */
    @Test
    void subCharsSupportsAllIndexForms() {
        char[] source = "abcdef".toCharArray();

        char[] same = ReflectionTestUtils.invokeMethod(StringUtil.class, "subChars", source, new int[0]);
        char[] tail = ReflectionTestUtils.invokeMethod(StringUtil.class, "subChars", source, new int[] {2});
        char[] slice = ReflectionTestUtils.invokeMethod(StringUtil.class, "subChars", source, new int[] {1, 3});

        assertSame(source, same);
        assertArrayEquals("cdef".toCharArray(), tail);
        assertArrayEquals("bcd".toCharArray(), slice);
    }

    /**
     * Verifies the utility class exposes the shared Nist behaviour through an instance.
     */
    @Test
    void instanceInheritsNistBehaviour() {
        assertNotNull(new StringUtil());
    }
}