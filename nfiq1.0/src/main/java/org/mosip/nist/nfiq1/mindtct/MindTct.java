package org.mosip.nist.nfiq1.mindtct;

import org.mosip.nist.nfiq1.Nist;

/**
 * Common base class for all MINDTCT (NIST minutiae detection) components in
 * this Java port of NFIQ 1.0.
 * <p>
 * MINDTCT is the NIST Latent Fingerprint System (LFS) minutiae detector; the
 * classes in the {@code org.mosip.nist.nfiq1.mindtct} package mirror the
 * original NIST C modules (e.g. {@code globals.c}, {@code imgutil.c},
 * {@code init.c}, {@code contour.c}, {@code ridges.c}, {@code quality.c}). This
 * class carries no state or behaviour of its own; it exists only to give the
 * MINDTCT helpers a shared supertype derived from {@link Nist}.
 */
public class MindTct extends Nist{
}
