package org.mosip.nist.nfiq1.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

import javax.imageio.ImageIO;

import org.mosip.nist.nfiq1.Nist;

/**
 * Static helpers for converting between {@link BufferedImage}, encoded byte arrays and flat pixel arrays.
 * <p>
 * MOSIP-specific utility (no NIST C counterpart). It is used to turn a decoded 8-bit grayscale fingerprint
 * image into the row-major {@code int[]} pixel buffer the MINDTCT/NFIQ routines work on. Stateless and
 * thread-safe.
 */
public final class ImageUtil extends Nist {
	/**
	 * Encodes a {@link BufferedImage} into a byte array in the given format.
	 *
	 * @param image  the image to encode
	 * @param format the informal ImageIO format name, e.g. {@code "png"} or {@code "bmp"}
	 * @return the encoded image bytes (empty if no ImageIO writer supports {@code format})
	 * @throws IOException if encoding fails
	 */
	public static byte[] toByteArray(BufferedImage image, String format) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, format, baos);
		return baos.toByteArray();
	}

	/**
	 * Extracts the pixels of an 8-bit image as a flat, row-major array of unsigned values (0 to 255).
	 * <p>
	 * Reads the raster's {@link DataBufferByte} directly (much faster than {@code getRGB}). Signed Java bytes
	 * are converted to unsigned ints, placed into a {@code height x width} matrix, and then flattened with
	 * {@link #twoDConvert(int[][])}. The image must be backed by a {@code DataBufferByte} with one byte per
	 * pixel (e.g. {@code TYPE_BYTE_GRAY}).
	 *
	 * @param image  the source image; must use a {@code DataBufferByte}
	 * @param format image format name (not used)
	 * @return array of {@code width * height} grayscale values in row-major order
	 * @throws IOException declared for API compatibility; not thrown by the current implementation
	 */
	public static int[] convertTo1DWithoutUsingGetRGB(BufferedImage image, String format) throws IOException {
		// get pixel value as single array from buffered Image
		final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		// get image width value
		final int width = image.getWidth();
		// get image height value
		final int height = image.getHeight();

		int[][] result = new int[height][width]; // Initialize the array with height and width

		// this loop allocates pixels value to two dimensional array
		for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel++) {
			int argb = 0;
			argb = pixels[pixel];
			if (argb < 0) { // if pixel value is negative, change to positive //still weird to me
				argb += 256;
			}
			result[row][col] = argb;
			col++;
			if (col == width) {
				col = 0;
				row++;
			}
		}

		return twoDConvert(result);
	}

	/**
	 * Flattens a (possibly jagged) 2-D int array into a 1-D array in row-major order.
	 *
	 * @param nums the 2-D array to flatten
	 * @return a new array holding all elements of {@code nums}, row by row (empty if there are none)
	 */
	public static int[] twoDConvert(int[][] nums) {
		int[] combined = new int[size(nums)];

		if (combined.length <= 0) {
			return combined;
		}
		int index = 0;

		for (int row = 0; row < nums.length; row++) {
			for (int column = 0; column < nums[row].length; column++) {
				combined[index++] = nums[row][column];
			}
		}
		return combined;
	}

	/**
	 * Counts the total number of elements in a (possibly jagged) 2-D int array.
	 *
	 * @param values the 2-D array
	 * @return the sum of the lengths of all rows
	 */
	private static int size(int[][] values) {
		int size = 0;

		for (int index = 0; index < values.length; index++) {
			size += values[index].length;
		}
		return size;
	}

	/**
	 * Decodes an encoded image byte array into a {@link BufferedImage} using the registered ImageIO readers.
	 *
	 * @param bytes the encoded image bytes
	 * @return the decoded image, or {@code null} if no registered reader recognises the data
	 * @throws IOException if reading or decoding fails
	 */
	public static BufferedImage toBufferedImage(byte[] bytes) throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		return ImageIO.read(is);
	}
}