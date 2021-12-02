package io.mosip.nist.nfiq1.imagetools;

import java.util.concurrent.atomic.AtomicInteger;

import io.mosip.nist.nfiq1.util.StringUtil;

public class ImageType extends ImageTools {	
	private static volatile ImageType instance;
    public static ImageType getInstance() {
        if (instance == null) {
            synchronized (ImageType.class) {
                if (instance == null) {
                    instance = new ImageType();
                }
            }
        }
        return instance;
    }    
    private ImageType()
    {
    	super();
    }
    
	public static final int UNKNOWN_IMG = -1;
	public static final int RAW_IMG = 0;
	public static final int WSQ_IMG = 1;
	public static final int JPEGL_IMG = 2;
	public static final int JPEGB_IMG = 3;
	public static final int IHEAD_IMG = 4;
	public static final int ANSI_NIST_IMG = 5;
	public static final int JP2_IMG = 6;
	public static final int PNG_IMG = 7;

	/*******************************************************************/
	/* Determine if image data is of type IHEAD, WSQ, JPEGL, JPEGB or  */
	/*    ANSI_NIST                                                    */
	/*******************************************************************/
	public int getImageType(AtomicInteger imageType, byte [] imageData, final int imageLength)
	{
		int ret = -1;
		   
		if (isWSQ(imageData, imageLength) > 0){
			imageType.set(WSQ_IMG);
		    return(0);
		}

		if (isJP2000(imageData, imageLength) > 0){
			imageType.set(JP2_IMG);
		    return(0);
		}

		/* Otherwise, image type is UNKNOWN ... */
		imageType.set(UNKNOWN_IMG);
		return ret;
	}

	public int isWSQ(byte [] imageData, final int imageLength)
	{
		int ret = 0;
        // If the first two bytes are 0xFF and 0xA0 and the last two bytes are 0xFF and 0xA1
        // then it is a WSQ file
		if ((imageData[0] == (byte) 0xFF
                && imageData[1] == (byte) 0xA0
                && imageData[imageData.length - 2] == (byte) 0xFF
                && imageData[imageData.length - 1] == (byte) 0xA1))
		{
			ret = 1;
		}

		return (ret);
	}	
	
	public int isJP2000(byte [] imageData, final int imageLength)
	{
		int ret = 0;
		int nptrIndex = 4;
		char[] buf = StringUtil.byteToCharArray (imageData, nptrIndex, nptrIndex + 4, 4);

		if (new String (buf).contains ("jP  "))
		{
			ret = 1;
		}

		return (ret);
	}		
}
