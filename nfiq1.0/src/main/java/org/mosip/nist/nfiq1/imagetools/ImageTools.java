package org.mosip.nist.nfiq1.imagetools;

import org.mosip.nist.nfiq1.Nist;

/**
 * Common base class for the image-handling helpers of the NFIQ 1.0 port (the Java analogue of NIST's
 * {@code imgtools} library).
 * <p>
 * It adds no behaviour of its own; it exists to group {@link ImageDecoder} and {@link ImageType} under a
 * shared supertype derived from {@link Nist}.
 */
public class ImageTools extends Nist{

}
