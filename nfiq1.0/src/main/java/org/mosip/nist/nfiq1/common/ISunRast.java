package org.mosip.nist.nfiq1.common;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Constants, header structure and I/O contract for Sun Rasterfile images (port of NIST's {@code sunrast.h}).
 * <p>
 * Note: each line of a bitmap image is padded to a multiple of 16 bits.
 */
public interface ISunRast {
	
	/** Magic number identifying a Sun Rasterfile. */
	public static final int SUN_MAGIC = 0x59a66a95;
	/** Sun {@code ras_type}: raw pixrect image in 68000 byte order. */
	public static final int SUN_STANDARD = 1; // Raw pixrect image in 68000 byte order
	/** Sun {@code ras_type}: run-length compression of bytes. */
	public static final int SUN_RUN_LENGTH = 2; // Run-length compression of bytes
	/** Sun {@code ras_type}: XRGB or RGB instead of XBGR or BGR. */
	public static final int SUN_FORMAT_RGB = 3; // XRGB or RGB instead of XBGR or BGR
	/** Sun {@code ras_type}: TIFF converted to/from a standard rasterfile. */
	public static final int SUN_FORMAT_TIFF = 4; // tiff <-> standard rasterfile
	/** Sun {@code ras_type}: IFF (TAAC format) converted to/from a standard rasterfile. */
	public static final int SUN_FORMAT_IFF = 5; // iff (TAAC format) <-> standard rasterfile
	
	/** Length in entries of the grayscale RGB colormap (3 x 256). */
	public static final int COLORMAP_LEN = 768;

	/** Grayscale RGB colormap: three consecutive identity ramps {@code 0..255} (red, green, blue planes). */
	public static int colormap[] = {
		0,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10,  11,  12,  13,  14,  15,
		16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,
		32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,
		48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,
		64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,
		80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,
		96,  97,  98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
		112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
		128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
		144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159,
		160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
		176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
		192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
		208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
		224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
		240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255,
		0,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10,  11,  12,  13,  14,  15,
		16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,
		32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,
		48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,
		64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,
		80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,
		96,  97,  98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
		112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
		128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
		144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159,
		160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
		176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
		192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
		208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
		224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
		240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255,
		0,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10,  11,  12,  13,  14,  15,
		16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,
		32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,
		48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,
		64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,
		80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,
		96,  97,  98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
		112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
		128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
		144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159,
		160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
		176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
		192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
		208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
		224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
		240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255
	};
	
	/**
	 * Header information of a Sun Rasterfile image.
	 * <p>
	 * The color map (if any) follows the header for {@code mapLength} bytes, followed by the image data.
	 */
	public class SunRasterHeader
	{
		/** Magic number ({@link ISunRast#SUN_MAGIC}). */
		private int magic; // magic number
		/** Image width in pixels. */
		private int width; // width (in pixels) of image
		/** Image height in pixels. */
		private int height; // height (in pixels) of image
		/** Pixel depth in bits (1, 8 or 24). */
		private int depth; // depth (1, 8, or 24 bits) of pixel
		/** Length of the image data in bytes. */
		private int rasLength; // length (in bytes) of image
		/** Raster file type (one of the {@code SUN_*} constants). */
		private int rasType; // type of file; see SUN_* below
		/** Colormap type. */
		private int mapType; // type of colormap; see MAP_* below
		/** Length of the colormap that follows the header, in bytes. */
		private int mapLength; // length (bytes) of following map
		/**
		 * Returns the magic number.
		 *
		 * @return the magic number
		 */
		public int getMagic() {
			return magic;
		}
		/**
		 * Sets the magic number.
		 *
		 * @param magic the magic number
		 */
		public void setMagic(int magic) {
			this.magic = magic;
		}
		/**
		 * Returns the image width.
		 *
		 * @return width in pixels
		 */
		public int getWidth() {
			return width;
		}
		/**
		 * Sets the image width.
		 *
		 * @param width width in pixels
		 */
		public void setWidth(int width) {
			this.width = width;
		}
		/**
		 * Returns the image height.
		 *
		 * @return height in pixels
		 */
		public int getHeight() {
			return height;
		}
		/**
		 * Sets the image height.
		 *
		 * @param height height in pixels
		 */
		public void setHeight(int height) {
			this.height = height;
		}
		/**
		 * Returns the pixel depth.
		 *
		 * @return depth in bits (1, 8 or 24)
		 */
		public int getDepth() {
			return depth;
		}
		/**
		 * Sets the pixel depth.
		 *
		 * @param depth depth in bits (1, 8 or 24)
		 */
		public void setDepth(int depth) {
			this.depth = depth;
		}
		/**
		 * Returns the image data length.
		 *
		 * @return length in bytes
		 */
		public int getRasLength() {
			return rasLength;
		}
		/**
		 * Sets the image data length.
		 *
		 * @param rasLength length in bytes
		 */
		public void setRasLength(int rasLength) {
			this.rasLength = rasLength;
		}
		/**
		 * Returns the raster file type.
		 *
		 * @return one of the {@code SUN_*} type constants
		 */
		public int getRasType() {
			return rasType;
		}
		/**
		 * Sets the raster file type.
		 *
		 * @param rasType one of the {@code SUN_*} type constants
		 */
		public void setRasType(int rasType) {
			this.rasType = rasType;
		}
		/**
		 * Returns the colormap type.
		 *
		 * @return the colormap type
		 */
		public int getMapType() {
			return mapType;
		}
		/**
		 * Sets the colormap type.
		 *
		 * @param mapType the colormap type
		 */
		public void setMapType(int mapType) {
			this.mapType = mapType;
		}
		/**
		 * Returns the colormap length.
		 *
		 * @return colormap length in bytes
		 */
		public int getMapLength() {
			return mapLength;
		}
		/**
		 * Sets the colormap length.
		 *
		 * @param mapLength colormap length in bytes
		 */
		public void setMapLength(int mapLength) {
			this.mapLength = mapLength;
		}			
	}
	
	/**
	 * Reads a Sun Rasterfile image (NIST {@code ReadSunRaster}).
	 *
	 * @param file            file to read
	 * @param sunRasterHeader output header populated from the file
	 * @param oColorMap       output colormap entries
	 * @param oMapLen         output colormap length in bytes
	 * @param oData           output pixel data
	 * @param oScanWidth      output scan-line width in bytes (padded to a multiple of 16 bits)
	 * @param oImgageWidth    output image width in pixels
	 * @param oImgageHeight   output image height in pixels
	 * @param oImgageDepth    output pixel depth in bits
	 * @return {@code 0} on success, negative on error
	 */
	public int readSunRaster(File file, SunRasterHeader sunRasterHeader, AtomicIntegerArray oColorMap, 
		AtomicInteger oMapLen, int [] oData, AtomicInteger oScanWidth, AtomicInteger oImgageWidth, 
		AtomicInteger oImgageHeight, AtomicInteger oImgageDepth);
	/**
	 * Writes image data as a Sun Rasterfile (NIST {@code WriteSunRaster}).
	 *
	 * @param file   destination file
	 * @param oData  pixel data to write
	 * @param width  image width in pixels
	 * @param height image height in pixels
	 * @param depth  pixel depth in bits
	 * @return {@code 0} on success, negative on error
	 */
	public int writeSunRaster(File file, int [] oData, final int width, final int height, final int depth);
}

