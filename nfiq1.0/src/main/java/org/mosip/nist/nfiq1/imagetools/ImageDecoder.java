package org.mosip.nist.nfiq1.imagetools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.IAn2k;
import org.mosip.nist.nfiq1.common.ILfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.biometrics.util.CommonUtil;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.finger.FingerBDIR;
import io.mosip.biometrics.util.finger.FingerDecoder;
import io.mosip.biometrics.util.finger.FingerImageCompressionType;
import io.mosip.biometrics.util.finger.FingerScaleUnitType;
import io.mosip.biometrics.util.finger.ImageData;
import io.mosip.biometrics.util.finger.RepresentationHeader;

/**
 * Reads an ISO/IEC 19794-4:2011 finger image record from disk and decodes the embedded fingerprint image
 * (JPEG 2000 lossless or WSQ) into a {@link BufferedImage} for NFIQ 1.0 processing.
 * <p>
 * This is the Java counterpart of NIST's {@code read_and_decode_grayscale_image()} /
 * {@code read_and_decode_image()} helpers (imgtools / an2k utilities). Instead of parsing the raw
 * image formats itself it delegates ISO record parsing to MOSIP's {@link FingerDecoder} and image
 * decompression to {@link CommonUtil#getBufferedImage(ConvertRequestDto)}.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is synchronized, and the decoding
 * methods keep no mutable instance state, so a single instance may be shared between threads.
 */
public class ImageDecoder extends ImageTools {
	/** SLF4J logger for decode diagnostics and errors. */
	private static final Logger logger = LoggerFactory.getLogger(ImageDecoder.class);
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static ImageDecoder instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private ImageDecoder() {
		super();
	}

	/**
	 * Returns the shared {@code ImageDecoder} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized ImageDecoder getInstance() {
		if (instance == null) {
			instance = new ImageDecoder();
		}
		return instance;
	}

	/**
	 * Reads and decodes an ISO finger image file, accepting it only if it is an 8-bit grayscale image of a
	 * known type.
	 * <p>
	 * Port of NIST {@code read_and_decode_grayscale_image()}. Calls
	 * {@link #readAndDecodeImage(AtomicInteger, String, AtomicInteger, AtomicInteger, AtomicInteger,
	 * AtomicInteger, AtomicInteger, AtomicInteger, AtomicReference)} and then rejects images whose type is
	 * {@link ImageType#UNKNOWN_IMG} or whose bit depth is not {@link ILfs#IMAGE_DEPTH} (8).
	 *
	 * @param returnCode output: 0 on success; -1 if the file could not be read/decoded; -3 if the image type
	 *                   is unknown; -4 if the pixel depth is not 8 bits
	 * @param file       path of the ISO file, absolute or relative to the current working directory
	 * @param oImageType output: detected image type, one of the {@link ImageType} constants
	 * @param oLength    output: length in bytes of the compressed image data (reset to 0 first)
	 * @param oWidth     output: image width in pixels
	 * @param oHeight    output: image height in pixels
	 * @param oDepth     output: pixel depth in bits
	 * @param oPPI       output: horizontal scan resolution in pixels per inch
	 * @param ofileType  output: {@code "JP2"}, {@code "WSQ"} or {@code "UNKNOWN"}
	 * @return the decoded grayscale image, or {@code null} on any error (see {@code returnCode})
	 * @throws Exception if reading the file or decoding the ISO record / image fails
	 */
	public BufferedImage readAndDecodeGrayscaleImage(AtomicInteger returnCode, String file, AtomicInteger oImageType,
			AtomicInteger oLength, AtomicInteger oWidth, AtomicInteger oHeight, AtomicInteger oDepth,
			AtomicInteger oPPI, AtomicReference<String> ofileType) throws Exception {

		returnCode.set(-1);
		oLength.set(0);

		/* Read in and decode image file. */
		BufferedImage bufferedImage = readAndDecodeImage(returnCode, file, oImageType, oLength, oWidth, oHeight, oDepth,
				oPPI, ofileType);
		if (returnCode.get() != ILfs.FALSE) {
			return null;
		}

		/* Image type UNKNOWN (perhaps raw), not supported */
		if (oImageType.get() == ImageType.UNKNOWN_IMG) {
			logger.error("ERROR : read_and_decode_grayscale_image : {} : image type UNKNOWN : not supported",
					oImageType.get());
			returnCode.set(-3);
			return null;
		}

		/* Only desire grayscale images ... */
		if (oDepth.get() != ILfs.IMAGE_DEPTH) {
			logger.error("ERROR : read_and_decode_grayscale_image : {} : image depth : {} != 8", file, oDepth.get());
			returnCode.set(-4);
			return null;
		}

		returnCode.set(0);
		return bufferedImage;
	}

	/**
	 * Reads an ISO/IEC 19794-4:2011 finger record from disk and decodes its image payload.
	 * <p>
	 * Port of NIST {@code read_and_decode_image()}. A relative file name is resolved against the current working
	 * directory. The record is parsed with {@link FingerDecoder#getFingerBDIR(ConvertRequestDto)}, and the image data is
	 * decompressed through {@link CommonUtil#getBufferedImage(ConvertRequestDto)}. Only JPEG 2000 lossless
	 * ({@link ImageType#JP2_IMG}) and WSQ ({@link ImageType#WSQ_IMG}) compression are recognised; any other
	 * compression type is reported as {@link ImageType#UNKNOWN_IMG}.
	 *
	 * @param returnCode output: set to -1 initially, and to {@link ILfs#FALSE} (0) on success
	 * @param iFile      path of the ISO file, absolute or relative to the current working directory
	 * @param imageType  output: detected image type, one of the {@link ImageType} constants
	 * @param oLength    output: length in bytes of the compressed image data from the ISO record
	 * @param oWidth     output: decoded image width in pixels
	 * @param oHeight    output: decoded image height in pixels
	 * @param oDepth     output: bit depth reported by the ISO record
	 * @param oPPI       output: horizontal spatial sampling rate from the representation header, in pixels per
	 *                   inch (converted when the record stores pixels per centimetre)
	 * @param ofileType  output: {@code "JP2"}, {@code "WSQ"} or {@code "UNKNOWN"}
	 * @return the decoded image; {@code null} if the file does not exist or cannot be decoded (return code
	 *         stays -1), or if the compression type is unknown (return code 0, {@code imageType} set to
	 *         {@link ImageType#UNKNOWN_IMG})
	 * @throws Exception if reading the file or decoding the ISO record / image fails
	 */
	public BufferedImage readAndDecodeImage(AtomicInteger returnCode, String iFile, AtomicInteger imageType,
			AtomicInteger oLength, AtomicInteger oWidth, AtomicInteger oHeight, AtomicInteger oDepth,
			AtomicInteger oPPI, AtomicReference<String> ofileType) throws Exception {
		BufferedImage image = null;
		returnCode.set(-1);
		ConvertRequestDto requestDto = new ConvertRequestDto();
		requestDto.setModality("Finger");
		requestDto.setVersion("ISO19794_4_2011");
		File initialFile = new File(iFile);
		if (!initialFile.isAbsolute()) {
			initialFile = new File(new File(".").getCanonicalPath(), iFile);
		}

		if (initialFile.exists()) {
			byte[] isoData = Files.readAllBytes(initialFile.toPath());

			requestDto.setInputBytes(isoData);

			FingerBDIR fingerBDIR = FingerDecoder.getFingerBDIR(requestDto);
			ImageData imageData = fingerBDIR.getRepresentation().getRepresentationBody().getImageData();
			RepresentationHeader representationHeader = fingerBDIR.getRepresentation().getRepresentationHeader();
			int ppi = representationHeader.getImageSpatialSamplingRateHorizontal();
			if (representationHeader.getScaleUnits() == FingerScaleUnitType.PIXELS_PER_CM) {
				ppi = (int) Math.round(ppi * IAn2k.MM_PER_INCH / 10.0);
			}
			int piDepth = fingerBDIR.getBitDepth();
			int fingerImageCompressionType = fingerBDIR.getCompressionType();

			oLength.set((int) imageData.getImageLength());
			requestDto.setImageType((fingerImageCompressionType == FingerImageCompressionType.JPEG_2000_LOSS_LESS ? 0
					: (fingerImageCompressionType == FingerImageCompressionType.WSQ ? 1 : -1)));

			imageType.set(ImageType.UNKNOWN_IMG);
			if (requestDto.getImageType() == 0)
				imageType.set(ImageType.JP2_IMG);
			else if (requestDto.getImageType() == 1)
				imageType.set(ImageType.WSQ_IMG);

			requestDto.setInputBytes(imageData.getImage());

			byte[] data = imageData.getImage();
			if (imageType.get() == ImageType.UNKNOWN_IMG) {
				ofileType.set("UNKNOWN");
				returnCode.set(ILfs.FALSE);
				return null;
			}
			image = CommonUtil.getBufferedImage(requestDto);
			if (image == null) {
				return null;
			}
			ofileType.set(imageType.get() == ImageType.JP2_IMG ? "JP2" : "WSQ");

			logger.debug("Image Details [Compression Type={}, Width={}, Height={}, Bit Depth={}, PPI={}, Length={}]",
					(imageType.get() == ImageType.JP2_IMG ? "JP2000" : "WSQ"), image.getWidth(), image.getHeight(),
					piDepth, ppi, data.length);

			oWidth.set(image.getWidth());
			oHeight.set(image.getHeight());
			oDepth.set(piDepth);
			oPPI.set(ppi);
			returnCode.set(ILfs.FALSE);
			return image;
		}
		return null;
	}
}