package io.mosip.nist.nfiq1.imagetools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.biometrics.util.CommonUtil;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.finger.FingerBDIR;
import io.mosip.biometrics.util.finger.FingerDecoder;
import io.mosip.biometrics.util.finger.FingerImageBitDepth;
import io.mosip.biometrics.util.finger.FingerImageCompressionType;
import io.mosip.biometrics.util.finger.ImageData;
import io.mosip.nist.nfiq1.common.ILfs;

public class ImageDecoder extends ImageTools {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageDecoder.class);	
	private static volatile ImageDecoder instance;
    public static ImageDecoder getInstance() {
        if (instance == null) {
            synchronized (ImageDecoder.class) {
                if (instance == null) {
                    instance = new ImageDecoder();
                }
            }
        }
        return instance;
    }    
    private ImageDecoder()
    {
    	super();
    }
    
	public BufferedImage readAndDecodeGrayscaleImage(AtomicInteger returnCode, String file, 
		AtomicInteger oImageType, AtomicInteger oLength, AtomicInteger oWidth, AtomicInteger oHeight, 
		AtomicInteger oDepth, AtomicInteger oPPI, AtomicReference<String> ofileType) throws Exception {
		
		returnCode.set(-1);
		oLength.set(0);

		/* Read in and decode image file. */
		BufferedImage bufferedImage = readAndDecodeImage(returnCode, file, oImageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);
		if (returnCode.get() != ILfs.FALSE)
		{
			return null;
		}

		/* Image type UNKNOWN (perhaps raw), not supported */
		if (oImageType.get() == ImageType.UNKNOWN_IMG)
		{
			LOGGER.error(String.format("ERROR : read_and_decode_grayscale_image : "));
			LOGGER.error(String.format("%s : image type UNKNOWN : not supported\n", file));
			returnCode.set(-3);
			return null;
		}

		/* Only desire grayscale images ... */
		if (oDepth.get() != ILfs.IMAGE_DEPTH)
		{
			LOGGER.error(String.format("ERROR : read_and_decode_grayscale_image : "));
			LOGGER.error(String.format("%s : image depth : %d != 8\n", file, oDepth.get()));
			returnCode.set(-4);
			return null;
		}

		returnCode.set(0);
		return bufferedImage;
	}

	public BufferedImage readAndDecodeImage(AtomicInteger returnCode, String iFile, AtomicInteger imageType, 
			AtomicInteger oLength, AtomicInteger oWidth, AtomicInteger oHeight, AtomicInteger oDepth, 
			AtomicInteger oPPI, AtomicReference<String> ofileType) throws Exception {
		BufferedImage image = null;
		returnCode.set(-1);
		ConvertRequestDto requestDto = new ConvertRequestDto();
		requestDto.setModality("Finger");
		requestDto.setVersion("ISO19794_4_2011");
		String filePath = new File (".").getCanonicalPath ();
		String fileName = filePath + File.separator + iFile;
		File initialFile = new File(fileName);
		
		if (initialFile.exists())
		{
			byte[] isoData = Files.readAllBytes(Paths.get(fileName));

			requestDto.setInputBytes(isoData);

			FingerBDIR fingerBDIR  = FingerDecoder.getFingerBDIR(requestDto);
			ImageData imageData = fingerBDIR.getRepresentation().getRepresentationBody().getImageData();
			int ppi = fingerBDIR.getRepresentation().getRepresentationHeader().getImageSpatialSamplingRateHorizontal();
			FingerImageBitDepth piDepth = fingerBDIR.getRepresentation().getRepresentationHeader().getBitDepth();
			FingerImageCompressionType fingerImageCompressionType = fingerBDIR.getRepresentation().getRepresentationHeader().getCompressionType();
			
			oLength.set(imageData.getImageLength());
			requestDto.setImageType((fingerImageCompressionType == FingerImageCompressionType.JPEG_2000_LOSS_LESS ? 0 : (fingerImageCompressionType == FingerImageCompressionType.WSQ ? 1 : -1)));

			imageType.set(ImageType.UNKNOWN_IMG);
			if (requestDto.getImageType() == 0)
				imageType.set(ImageType.JP2_IMG);
			else if (requestDto.getImageType() == 1)
				imageType.set(ImageType.WSQ_IMG);
			
			requestDto.setInputBytes(imageData.getImage());

			byte[] data = imageData.getImage();
			image = CommonUtil.getBufferedImage(requestDto);
			
			LOGGER.info("Image Details ");
			LOGGER.info(String.format("[\nCompression Type=%s\n, Width=%2d\n, Height=%2d\n, Bit Depth=%2d\n, PPI=%2d\n, Length=%2d\n]",
					(requestDto.getImageType() == 0 ? "JP2000" : "WSQ"), image.getWidth(), image.getHeight(), 
					piDepth.value(), ppi, data.length));

			oWidth.set(image.getWidth());
			oHeight.set(image.getHeight());
			oDepth.set(piDepth.value());// image.getColorModel().getPixelSize());
			oPPI.set(ppi);
			returnCode.set(ILfs.FALSE);
			return image; 
		}
		return null;
	}
}
