package org.mosip.nist.nfiq2.imagetools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.TestImages;

/**
 * Tests of {@link IsoImageDecoder}.
 */
class IsoImageDecoderTest {

	@Test
	void decodesSamples() throws IOException {
		FingerprintImageData jp2 = TestImages.jp2();
		assertEquals(500, jp2.getPpi());
		assertEquals(jp2.getWidth() * jp2.getHeight(), jp2.getData().length);
		FingerprintImageData wsq = TestImages.wsq();
		assertEquals(500, wsq.getPpi());
		assertEquals(545, wsq.getWidth());
		assertEquals(622, wsq.getHeight());
	}

	@Test
	void rejectsGarbage() {
		byte[] garbage = { 1, 2, 3, 4 };
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> IsoImageDecoder.decode(garbage));
		assertEquals(ErrorCode.BadArguments, e.getNfiq2ErrorCode());
	}

	@Test
	void greyImageIsCopied() {
		BufferedImage grey = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_GRAY);
		grey.getRaster().setSample(0, 0, 0, 10);
		grey.getRaster().setSample(1, 0, 0, 200);
		assertArrayEquals(new byte[] { 10, (byte) 200 }, IsoImageDecoder.toGrey8(grey));
	}

	@Test
	void colourImageIsConverted() {
		BufferedImage rgb = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
		rgb.setRGB(0, 0, 0xFFFFFF);
		rgb.setRGB(1, 0, 0x000000);
		assertArrayEquals(new byte[] { (byte) 255, 0 }, IsoImageDecoder.toGrey8(rgb));
	}
}
