package org.mosip.nist.nfiq2.imagetools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.biometrics.util.CommonUtil;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.finger.FingerBDIR;
import io.mosip.biometrics.util.finger.FingerDecoder;
import io.mosip.biometrics.util.finger.FingerImageCompressionType;
import io.mosip.biometrics.util.finger.FingerScaleUnitType;
import io.mosip.biometrics.util.finger.RepresentationHeader;

/**
 * Decodes an ISO/IEC 19794-4:2011 finger image record (JPEG 2000 lossless or WSQ payload) with MOSIP
 * biometrics-util into 8-bit greyscale {@link FingerprintImageData}.
 * <p>
 * Stateless and thread-safe.
 */
public final class IsoImageDecoder {
	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(IsoImageDecoder.class);
	/** Millimetres per inch. */
	private static final double MM_PER_INCH = 25.4;
	/** {@link ConvertRequestDto} image type of JPEG 2000. */
	private static final int IMAGE_TYPE_JP2 = 0;
	/** {@link ConvertRequestDto} image type of WSQ. */
	private static final int IMAGE_TYPE_WSQ = 1;

	/** Static helpers only. */
	private IsoImageDecoder() {
	}

	/**
	 * Reads and decodes an ISO record file.
	 *
	 * @param file ISO file
	 * @return greyscale image with the finger position and resolution of the record
	 * @throws IOException    when the file cannot be read
	 * @throws Nfiq2Exception {@link ErrorCode#BadArguments} when the record cannot be decoded
	 */
	public static FingerprintImageData decode(Path file) throws IOException {
		return decode(Files.readAllBytes(file));
	}

	/**
	 * Decodes an ISO record.
	 *
	 * @param isoData ISO/IEC 19794-4:2011 record
	 * @return greyscale image with the finger position and resolution of the record
	 * @throws Nfiq2Exception {@link ErrorCode#BadArguments} when the record cannot be decoded
	 */
	public static FingerprintImageData decode(byte[] isoData) {
		ConvertRequestDto request = new ConvertRequestDto();
		request.setModality("Finger");
		request.setVersion("ISO19794_4_2011");
		request.setInputBytes(isoData);
		BufferedImage image;
		RepresentationHeader header;
		try {
			FingerBDIR bdir = FingerDecoder.getFingerBDIR(request);
			header = bdir.getRepresentation().getRepresentationHeader();
			int compression = bdir.getCompressionType();
			if (compression == FingerImageCompressionType.JPEG_2000_LOSS_LESS) {
				request.setImageType(IMAGE_TYPE_JP2);
			} else if (compression == FingerImageCompressionType.WSQ) {
				request.setImageType(IMAGE_TYPE_WSQ);
			} else {
				throw new Nfiq2Exception(ErrorCode.BadArguments,
						"Unsupported ISO image compression type " + compression + " (JPEG 2000 lossless or WSQ only)");
			}
			request.setInputBytes(bdir.getRepresentation().getRepresentationBody().getImageData().getImage());
			image = CommonUtil.getBufferedImage(request);
		} catch (Nfiq2Exception e) {
			throw e;
		} catch (Exception e) {
			throw new Nfiq2Exception(ErrorCode.BadArguments, "Cannot decode ISO finger record: " + e.getMessage(), e);
		}
		if (image == null) {
			throw new Nfiq2Exception(ErrorCode.BadArguments, "Cannot decode ISO finger image");
		}
		int ppi = header.getImageSpatialSamplingRateHorizontal();
		if (header.getScaleUnits() == FingerScaleUnitType.PIXELS_PER_CM) {
			ppi = (int) Math.round(ppi * MM_PER_INCH / 10.0);
		}
		LOGGER.debug("ISO finger image [type={}, width={}, height={}, ppi={}, position={}]",
				request.getImageType() == IMAGE_TYPE_JP2 ? "JP2000" : "WSQ", image.getWidth(), image.getHeight(), ppi,
				header.getFingerPosition());
		return new FingerprintImageData(toGrey8(image), image.getWidth(), image.getHeight(),
				header.getFingerPosition(), ppi);
	}

	/**
	 * Row-major 8-bit greyscale pixels of an image; single-band 8-bit rasters are copied as is, anything else
	 * is converted through {@link BufferedImage#TYPE_BYTE_GRAY}.
	 *
	 * @param image image
	 * @return pixels
	 */
	public static byte[] toGrey8(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		Raster raster = image.getRaster();
		if (raster.getNumBands() != 1 || raster.getSampleModel().getSampleSize(0) != 8) {
			BufferedImage grey = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g = grey.createGraphics();
			try {
				g.drawImage(image, 0, 0, null);
			} finally {
				g.dispose();
			}
			raster = grey.getRaster();
		}
		int[] samples = raster.getSamples(0, 0, w, h, 0, (int[]) null);
		byte[] out = new byte[w * h];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) samples[i];
		}
		return out;
	}
}
