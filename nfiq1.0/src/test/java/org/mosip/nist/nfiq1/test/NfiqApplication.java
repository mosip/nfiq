package org.mosip.nist.nfiq1.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Nfiq1Helper;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.imagetools.ImageDecoder;
import org.mosip.nist.nfiq1.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NfiqApplication {
	private static final Logger logger = LoggerFactory.getLogger(NfiqApplication.class);
	private Nfiq1Helper nfiqHelper = new Nfiq1Helper();

	public NfiqApplication() {
		super();
	}

	public Nfiq1Helper getNfiqHelper() {
		return nfiqHelper;
	}

	public void setNfiqHelper(Nfiq1Helper nfiqHelper) {
		this.nfiqHelper = nfiqHelper;
	}

	public static void main(String[] args) {
		NfiqApplication nfiqApplication = new NfiqApplication();
		int ret;
		AtomicInteger retCode = new AtomicInteger(0);
		String imageFile = null;
		int[] imageData = null;
		AtomicInteger oImageType = new AtomicInteger(-1);
		AtomicInteger oImageLength = new AtomicInteger(0);
		AtomicInteger oImageWidth = new AtomicInteger(0);
		AtomicInteger oImageHeight = new AtomicInteger(0);
		AtomicInteger oImageDepth = new AtomicInteger(0);
		AtomicInteger oImagePPI = new AtomicInteger(0);
		AtomicInteger nfiq = new AtomicInteger(0);
		AtomicReference<Double> conf = new AtomicReference<Double>(0.0d);
		AtomicReference<String> ifileType = new AtomicReference<String>();
		AtomicInteger oLogsFlag = new AtomicInteger(0);

		if (args != null && args.length >= 1) {
			imageFile = args[0];
			// imgfile=info_wsq.iso/info_jp2.iso --ISO file having jp2 or wsq
			if (imageFile.contains("imgfile"))// 0
			{
				imageFile = imageFile.split("=")[1];
			}
		}

		if (args != null && args.length >= 2) {
			String logs = args[1];
			if (logs.contains("logs"))// 0
			{
				oLogsFlag.set(Integer.parseInt(logs.split("=")[1]));
			}
		}

		/* This routine will automatically detect and load: ISO FORMAT */
		/* WSQ, JP2 image formats */
		retCode.set(-1);
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageDecoder.getInstance().readAndDecodeGrayscaleImage(retCode, imageFile, oImageType,
					oImageLength, oImageWidth, oImageHeight, oImageDepth, oImagePPI, ifileType);
		} catch (Exception e) {
			logger.error("Error main ", e);
		}
		ret = retCode.get();
		if ((ret != ILfs.FALSE)) {
			if (ret == -3) // UNKNOWN_IMG
			{
				logger.error("Hint: Use -raw for raw images");
			}
			System.exit(ret);
		}

		try {
			imageData = ImageUtil.convertTo1DWithoutUsingGetRGB(bufferedImage, "jpg");
		} catch (IOException e) {
			logger.error("NFIQ={} Conf={}", nfiq.get(), conf.get());
		}
		/* Compute the NFIQ value */
		ret = nfiqApplication.getNfiqHelper().computeNfiq(nfiq, conf, imageData, oImageWidth.get(), oImageHeight.get(),
				oImageDepth.get(), oImagePPI.get(), oLogsFlag.get());
		/* If system error ... */
		if (ret < ILfs.FALSE) {
			System.exit(ret);
		}

		/* Report results to stdout */
		if (oLogsFlag.get() == ILfs.FALSE) {
			logger.info("NFIQ={} Conf={}", nfiq.get(), conf.get());
		} else if (oLogsFlag.get() == ILfs.TRUE) {
			logger.info("NFIQ={} Conf={}", nfiq.get(), conf.get());
		}

		/* Deallocate image data */
		imageData = null;

		/* Exit successfully */
		System.exit(0);
	}
}