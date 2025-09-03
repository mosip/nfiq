package org.mosip.nist.nfiq1.mindtct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

/**
 * Test class for {@link Results}
 *
 * <p>This class validates the functionality of result processing and output generation
 * for NIST's Mindtct fingerprint analysis algorithms.</p>
 *
 */
public class ResultsTest {

    private Results results;

    @TempDir
    Path tempDir;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes the Results singleton instance for testing.
     */
    @BeforeEach
    public void setUp() {
        results = Results.getInstance();
    }

    /**
     * Cleans up resources after each test execution.
     */
    @AfterEach
    public void tearDown() {
        results = null;
    }

    /**
     * Verifies that getInstance returns the same singleton instance across multiple calls.
     * This ensures proper singleton pattern implementation.
     */
    @Test
    public void verifySingletonInstance() {
        Results firstInstance = Results.getInstance();
        Results secondInstance = Results.getInstance();

        assertEquals(firstInstance, secondInstance, "getInstance should return the same singleton instance");
        assertNotNull(firstInstance, "getInstance should never return null");
    }

    /**
     * Validates successful map dumping with a small 2x2 integer map.
     * Tests basic functionality of writing integer array data to file.
     */
    @Test
    public void validateMapDumpingWithSmallMap() throws IOException {
        File outputFile = tempDir.resolve("small_map.txt").toFile();
        int mapWidth = 2;
        int mapHeight = 2;
        AtomicIntegerArray testMap = new AtomicIntegerArray(mapWidth * mapHeight);

        testMap.set(0, 5);
        testMap.set(1, 10);
        testMap.set(2, 15);
        testMap.set(3, 20);

        results.dumpMap(outputFile, testMap, mapWidth, mapHeight);

        assertTrue(outputFile.exists(), "Output file should be created");
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains(" 5"), "File should contain first map value");
        assertTrue(content.contains("10"), "File should contain second map value");
        assertTrue(content.contains("15"), "File should contain third map value");
        assertTrue(content.contains("20"), "File should contain fourth map value");
    }

    /**
     * Validates map dumping with a larger 3x4 integer map containing various values.
     * Tests scalability and proper formatting of larger datasets.
     */
    @Test
    public void validateMapDumpingWithLargerMap() throws IOException {
        File outputFile = tempDir.resolve("large_map.txt").toFile();
        int mapWidth = 3;
        int mapHeight = 4;
        AtomicIntegerArray testMap = new AtomicIntegerArray(mapWidth * mapHeight);

        for (int i = 0; i < testMap.length(); i++) {
            testMap.set(i, i * 2);
        }

        results.dumpMap(outputFile, testMap, mapWidth, mapHeight);

        assertTrue(outputFile.exists(), "Output file should be created");
        String content = Files.readString(outputFile.toPath());
        String[] lines = content.split("\n");
        assertEquals(mapHeight, lines.length, "Number of lines should match map height");
    }

    /**
     * Validates map dumping with negative values to ensure proper handling
     * of signed integers in the output format.
     */
    @Test
    public void validateMapDumpingWithNegativeValues() throws IOException {
        File outputFile = tempDir.resolve("negative_map.txt").toFile();
        int mapWidth = 2;
        int mapHeight = 2;
        AtomicIntegerArray testMap = new AtomicIntegerArray(mapWidth * mapHeight);

        testMap.set(0, -1);
        testMap.set(1, -5);
        testMap.set(2, 3);
        testMap.set(3, -10);

        results.dumpMap(outputFile, testMap, mapWidth, mapHeight);

        assertTrue(outputFile.exists(), "Output file should be created");
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("-1"), "File should contain negative values");
        assertTrue(content.contains("-5"), "File should contain negative values");
        assertTrue(content.contains("-10"), "File should contain negative values");
    }

    /**
     * Validates map dumping with single element map (1x1).
     * Tests boundary condition with minimum possible map size.
     */
    @Test
    public void validateMapDumpingWithSingleElement() throws IOException {
        File outputFile = tempDir.resolve("single_element.txt").toFile();
        int mapWidth = 1;
        int mapHeight = 1;
        AtomicIntegerArray testMap = new AtomicIntegerArray(1);

        testMap.set(0, 42);

        results.dumpMap(outputFile, testMap, mapWidth, mapHeight);

        assertTrue(outputFile.exists(), "Output file should be created");
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("42"), "File should contain the single map value");
    }

    /**
     * Validates map dumping with zero values to ensure proper formatting
     * and handling of zero-valued maps.
     */
    @Test
    public void validateMapDumpingWithZeroValues() throws IOException {
        File outputFile = tempDir.resolve("zero_map.txt").toFile();
        int mapWidth = 2;
        int mapHeight = 3;
        AtomicIntegerArray testMap = new AtomicIntegerArray(mapWidth * mapHeight);

        results.dumpMap(outputFile, testMap, mapWidth, mapHeight);

        assertTrue(outputFile.exists(), "Output file should be created");
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains(" 0"), "File should contain zero values");
    }

    /**
     * Validates block drawing with edge case offsets at image boundaries.
     * Tests handling of block offsets near image edges.
     */
    @Test
    public void validateBlockDrawingWithBoundaryOffsets() {
        int mapWidth = 1;
        int mapHeight = 1;
        int paddedImageWidth = 5;
        int paddedImageHeight = 5;
        int drawPixel = 128;

        AtomicIntegerArray blockOffsets = new AtomicIntegerArray(paddedImageWidth * paddedImageHeight);
        int[] paddedImageData = new int[paddedImageWidth * paddedImageHeight];

        // Set boundary offsets
        blockOffsets.set(0, 0); // First pixel
        blockOffsets.set(1, paddedImageWidth * paddedImageHeight - 1); // Last pixel

        results.drawBlocks(blockOffsets, mapWidth, mapHeight, paddedImageData,
                paddedImageWidth, paddedImageHeight, drawPixel);

        assertEquals(drawPixel, paddedImageData[0], "First pixel should be drawn");
        assertEquals(drawPixel, paddedImageData[paddedImageWidth * paddedImageHeight - 1],
                "Last pixel should be drawn");
    }

    /**
     * Validates rotated grid drawing with valid direction parameter.
     * Tests successful annotation of rotated grids within image blocks.
     */
    @Test
    public void validateRotatedGridDrawingWithValidDirection() {
        RotGrids mockRotGrids = Mockito.mock(RotGrids.class);
        Mockito.when(mockRotGrids.getNoOfGrids()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(3);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(3);

        int[][] mockGridsArray = new int[5][9];
        for (int dir = 0; dir < 5; dir++) {
            for (int i = 0; i < 9; i++) {
                mockGridsArray[dir][i] = i;
            }
        }
        Mockito.when(mockRotGrids.getGrids()).thenReturn(mockGridsArray);

        int nDir = 0;
        int[] imageData = new int[100];
        int blockOffset = 10;
        int imageWidth = 10;
        int imageHeight = 10;
        int drawPixel = 200;

        int result = results.drawRotGrid(mockRotGrids, nDir, imageData, blockOffset,
                imageWidth, imageHeight, drawPixel);

        assertEquals(ILfs.FALSE, result, "Method should return FALSE for successful completion");
    }

    /**
     * Validates rotated grid drawing with invalid direction parameter.
     * Tests error handling when direction exceeds available grid range.
     */
    @Test
    public void validateRotatedGridDrawingWithInvalidDirection() {
        RotGrids mockRotGrids = Mockito.mock(RotGrids.class);
        Mockito.when(mockRotGrids.getNoOfGrids()).thenReturn(3);

        int nDir = 5; // Exceeds available grids
        int[] imageData = new int[100];
        int blockOffset = 10;
        int imageWidth = 10;
        int imageHeight = 10;
        int drawPixel = 200;

        int result = results.drawRotGrid(mockRotGrids, nDir, imageData, blockOffset,
                imageWidth, imageHeight, drawPixel);

        assertEquals(ILfs.ERROR_CODE_140, result, "Method should return error code for invalid direction");
    }

    /**
     * Validates writeTextResults method returns expected default value.
     * Tests the stub implementation of text results writing functionality.
     */
    @Test
    public void validateWriteTextResultsStubBehavior() {
        File mockFile = Mockito.mock(File.class);
        AtomicReference<Minutiae> mockMinutiae = new AtomicReference<>();
        AtomicIntegerArray mockQualityMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockDirectionMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockLowContrastMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockLowFlowMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockHighCurveMap = new AtomicIntegerArray(10);

        int result = results.writeTextResults(mockFile, 1, 100, 100, mockMinutiae,
                mockQualityMap, mockDirectionMap, mockLowContrastMap,
                mockLowFlowMap, mockHighCurveMap, 10, 10);

        assertEquals(0, result, "Stub method should return 0");
    }

    /**
     * Validates writeMinutiaeXYTQ method returns expected default value.
     * Tests the stub implementation of minutiae writing functionality.
     */
    @Test
    public void validateWriteMinutiaeXYTQStubBehavior() {
        File mockFile = Mockito.mock(File.class);
        AtomicReference<Minutiae> mockMinutiae = new AtomicReference<>();

        int result = results.writeMinutiaeXYTQ(mockFile, 1, mockMinutiae, 100, 100);

        assertEquals(0, result, "Stub method should return 0");
    }

    /**
     * Validates drawInputBlockImageMap method returns expected default value.
     * Tests the stub implementation of input block image map drawing functionality.
     */
    @Test
    public void validateDrawInputBlockImageMapStubBehavior() {
        AtomicIntegerArray mockMap = new AtomicIntegerArray(10);
        int[] mockImageData = new int[100];
        RotGrids mockRotGrids = Mockito.mock(RotGrids.class);

        int result = results.drawInputBlockImageMap(mockMap, 5, 2, mockImageData,
                10, 10, mockRotGrids, 255);

        assertEquals(0, result, "Stub method should return 0");
    }

    /**
     * Validates drawDirectionMap method returns expected default value.
     * Tests the stub implementation of direction map drawing functionality.
     */
    @Test
    public void validateDrawDirectionMapStubBehavior() {
        StringBuilder mockFileName = new StringBuilder("test.txt");
        AtomicIntegerArray mockDirectionMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockBlockOffsets = new AtomicIntegerArray(10);
        int[] mockImageData = new int[100];

        int result = results.drawDirectionMap(mockFileName, mockDirectionMap, mockBlockOffsets,
                5, 2, 10, mockImageData, 10, 10, 1);

        assertEquals(0, result, "Stub method should return 0");
    }

    /**
     * Validates drawTFMap method returns expected default value.
     * Tests the stub implementation of TF map drawing functionality.
     */
    @Test
    public void validateDrawTFMapStubBehavior() {
        StringBuilder mockFileName = new StringBuilder("tfmap.txt");
        AtomicIntegerArray mockMap = new AtomicIntegerArray(10);
        AtomicIntegerArray mockBlockOffsets = new AtomicIntegerArray(10);
        int[] mockImageData = new int[100];

        int result = results.drawTFMap(mockFileName, mockMap, mockBlockOffsets,
                5, 2, 10, mockImageData, 10, 10, 1);

        assertEquals(0, result, "Stub method should return 0");
    }
}