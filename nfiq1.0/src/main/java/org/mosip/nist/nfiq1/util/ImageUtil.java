package org.mosip.nist.nfiq1.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

import javax.imageio.ImageIO;

import org.mosip.nist.nfiq1.Nist;

public final class ImageUtil extends Nist {
	// convert BufferedImage to byte[]
	public static byte[] toByteArray(BufferedImage image, String format) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, format, baos);
		return baos.toByteArray();
	}

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

	private static int size(int[][] values) {
		int size = 0;

		for (int index = 0; index < values.length; index++) {
			size += values[index].length;
		}
		return size;
	}

	// convert byte[] to BufferedImage
	public static BufferedImage toBufferedImage(byte[] bytes) throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		return ImageIO.read(is);
	}
}