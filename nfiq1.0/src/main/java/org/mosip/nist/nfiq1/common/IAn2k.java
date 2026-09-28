package org.mosip.nist.nfiq1.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Constants, data structures and library contracts for ANSI/NIST-ITL (AN2K) biometric transaction files
 * (port of NIST's {@code an2k.h}).
 * <p>
 * An AN2K file is a sequence of logical records ({@link Record}); tagged records are made of fields
 * ({@link Field}), subfields ({@link SubField}) and items ({@link Item}) separated by the FS/GS/RS/US control
 * characters. The nested interfaces mirror the NIST {@code an2k} library modules (allocation, reading,
 * writing, lookup, insertion, deletion, conversion to/from FBI/IAFIS conventions, record selection, ...).
 * Within NFIQ only the constants (e.g. {@link #MM_PER_INCH}) are used at runtime.
 * <p>
 * Unless noted otherwise, functions return {@code 0} on success and a negative value on system error; record,
 * field, subfield and item indices are zero-based.
 */
@SuppressWarnings({ "java:S125" })
public interface IAn2k {
	/** Boolean true (C-style integer). */
	public static final int TRUE = 1;
	/** Boolean false (C-style integer). */
	public static final int FALSE = 0;

	/** Allocation chunk size used when growing record/field/subfield/item lists. */
	public static final int ANSI_NIST_CHUNK = 100;
	/** Marker for a value that has not been set. */
	public static final int UNSET = -1;
	/** Status: processing is done. */
	public static final int DONE = 2;
	/** Status: more data follows. */
	public static final int MORE = 3;
	/** Undefined integer value. */
	public static final int UNDEFINED_INT = -1;
	/** Return value: the item should be ignored. */
	public static final int IGNORE = 2;

	/** Record type: Type-1 transaction information record. */
	public static final int TYPE_1_ID = 1;
	/** Number of mandatory fields in a Type-1 record. */
	public static final int TYPE_1_NUM_MANDATORY_FIELDS = 9;
	/** Record type: Type-2 user-defined descriptive text record. */
	public static final int TYPE_2_ID = 2;
	/** Record type: Type-3 low-resolution grayscale fingerprint image (obsolete). */
	public static final int TYPE_3_ID = 3;
	/** Record type: Type-4 high-resolution grayscale fingerprint image. */
	public static final int TYPE_4_ID = 4;
	/** Record type: Type-5 low-resolution binary fingerprint image (obsolete). */
	public static final int TYPE_5_ID = 5;
	/** Record type: Type-6 high-resolution binary fingerprint image (obsolete). */
	public static final int TYPE_6_ID = 6;
	/** Record type: Type-7 user-defined image record. */
	public static final int TYPE_7_ID = 7;
	/** Record type: Type-8 signature image record. */
	public static final int TYPE_8_ID = 8;
	/** Record type: Type-9 minutiae data record. */
	public static final int TYPE_9_ID = 9;
	/** Record type: Type-10 facial and SMT image record. */
	public static final int TYPE_10_ID = 10;
	/** Record type: Type-11 (reserved / voice). */
	public static final int TYPE_11_ID = 11;
	/** Record type: Type-12 (reserved / dental). */
	public static final int TYPE_12_ID = 12;
	/** Record type: Type-13 variable-resolution latent image record. */
	public static final int TYPE_13_ID = 13;
	/** Record type: Type-14 variable-resolution fingerprint image record. */
	public static final int TYPE_14_ID = 14;
	/** Record type: Type-15 variable-resolution palmprint image record. */
	public static final int TYPE_15_ID = 15;
	/** Record type: Type-16 user-defined variable-resolution testing image record. */
	public static final int TYPE_16_ID = 16;
	/** Record type: Type-17 iris image record. */
	public static final int TYPE_17_ID = 17;
	/** Record type: Type-99 CBEFF biometric data record. */
	public static final int TYPE_99_ID = 99;

	/** Type-1 field number: logical record length (LEN). */
	public static final int LEN_ID = 1;
	/** Type-1 field number: version number (VER). */
	public static final int VER_ID = 2;
	/** Type-1 field number: file content (CNT). */
	public static final int CNT_ID = 3;
	/** Type-1 field number: type of transaction (TOT). */
	public static final int TOT_ID = 4;
	/** Type-1 field number: date (DAT). */
	public static final int DAT_ID = 5;
	/** Type-1 field number: priority (PRY). */
	public static final int PRY_ID = 6;
	/** Type-1 field number: destination agency identifier (DAI). */
	public static final int DAI_ID = 7;
	/** Type-1 field number: originating agency identifier (ORI). */
	public static final int ORI_ID = 8;
	/** Type-1 field number: transaction control number (TCN). */
	public static final int TCN_ID = 9;
	/** Type-1 field number: transaction control reference (TCR). */
	public static final int TCR_ID = 10;
	/** Type-1 field number: native scanning resolution (NSR). */
	public static final int NSR_ID = 11;
	/** Type-1 field number: nominal transmitting resolution (NTR). */
	public static final int NTR_ID = 12;
	/** Type-1 field number: domain name (DOM). */
	public static final int DOM_ID = 13;
	/** Type-1 field number: Greenwich mean time (GMT). */
	public static final int GMT_ID = 14;
	/** Type-1 field number: directory of character sets (DCS). */
	public static final int DCS_ID = 15;

	/** Format used to print an image designation character (IDC). */
	public static final String IDC_FMT = "%02d";
	/** Format used to print a field tag ({@code record.field:}). */
	public static final String FLD_FMT = "%d.%03d:";
	/** Character set ID for 7-bit ASCII. */
	public static final int ASCII_CSID = 0;

	/** ANSI/NIST standard version 2.00. */
	public static final int VERSION_0200 = 200;
	/** ANSI/NIST standard version 2.01. */
	public static final int VERSION_0201 = 201;
	/** ANSI/NIST standard version 3.00. */
	public static final int VERSION_0300 = 300;
	/** ANSI/NIST standard version 4.00. */
	public static final int VERSION_0400 = 400;

	/** File separator character, terminating a logical record. */
	public static final int FS_CHAR = 0x1C;
	/** Group separator character, terminating a field. */
	public static final int GS_CHAR = 0x1D;
	/** Record separator character, terminating a subfield. */
	public static final int RS_CHAR = 0x1E;
	/** Unit separator character, terminating an item. */
	public static final int US_CHAR = 0x1F;

	/** List of tagged record types (not initialized in this port). */
	public static List<Integer> TAGGED_RECORDS = null;
	/** Number of tagged record types. */
	public static final int NUM_TAGGED_RECORDS = 10;

	/** List of binary record types (not initialized in this port). */
	public static List<Integer> BINARY_RECORDS = null;
	/** Number of binary record types. */
	public static final int NUM_BINARY_RECORDS = 6;

	/** List of tagged image record types (not initialized in this port). */
	public static List<Integer> TAGGED_IMAGE_RECORDS = null;
	/** Number of tagged image record types. */
	public static final int NUM_TAGGED_IMAGE_RECORDS = 7;
	/** Field number designating the image data field of a tagged image record. */
	public static final int IMAGE_FIELD = 999;

	/** List of binary image record types (not initialized in this port). */
	public static List<Integer> BINARY_IMAGE_RECORDS = null;
	/** Number of binary image record types. */
	public static final int NUM_BINARY_IMAGE_RECORDS = 5;
	/** Byte size of the LEN field in binary image records. */
	public static final int BINARY_LEN_BYTES = 4;
	/** Byte size of the IDC field in binary image records. */
	public static final int BINARY_IDC_BYTES = 1;
	/** Byte size of the IMP field in binary image records. */
	public static final int BINARY_IMP_BYTES = 1;
	/** Byte size of the FGP field in binary image records. */
	public static final int BINARY_FGP_BYTES = 6;
	/** Byte size of the ISR field in binary image records. */
	public static final int BINARY_ISR_BYTES = 1;
	/** Byte size of the HLL field in binary image records. */
	public static final int BINARY_HLL_BYTES = 2;
	/** Byte size of the VLL field in binary image records. */
	public static final int BINARY_VLL_BYTES = 2;
	/** Byte size of the compression algorithm field in binary image records. */
	public static final int BINARY_CA_BYTES = 1;
	/** Number of fields in a binary image record. */
	public static final int NUM_BINARY_IMAGE_FIELDS = 9;

	/* Type-3,4,5,6 Field IDs */
	// public static final int LEN_ID = 1;
	/** Type-3/4/5/6 field number: image designation character (IDC). */
	public static final int IDC_ID = 2;
	/** Type-3/4/5/6 field number: impression type (IMP). */
	public static final int IMP_ID = 3;
	/** Type-3/4/5/6 field number: finger position (FGP). */
	public static final int FGP_ID = 4;
	/** Type-3/4/5/6 field number: image scanning resolution (ISR). */
	public static final int ISR_ID = 5;
	/** Type-3/4/5/6 field number: horizontal line length (HLL). */
	public static final int HLL_ID = 6;
	/** Type-3/4/5/6 field number: vertical line length (VLL). */
	public static final int VLL_ID = 7;
	/** Type-3/4/5/6 field number: compression algorithm (GCA/BCA). */
	public static final int BIN_CA_ID = 8;
	/** Type-3/4/5/6 field number: image data. */
	public static final int BIN_IMAGE_ID = 9;

	/** List of binary signature record types (not initialized in this port). */
	public static List<Integer> BINAR_SIGNATURE_RECORDS = null;
	/** Number of binary signature record types. */
	public static final int NUM_BINARY_SIGNATURE_RECORDS = 1;
	/** Byte size of the SIG field in binary signature records. */
	public static final int BINARY_SIG_BYTES = 1;
	/** Byte size of the SRT field in binary signature records. */
	public static final int BINARY_SRT_BYTES = 1;
	/** Number of fields in a binary signature record. */
	public static final int NUM_BINARY_SIGNATURE_FIELDS = 8;

	/* Type-8 Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	/** Type-8 field number: signature type (SIG). */
	public static final int SIG_ID = 3;
	/** Type-8 field number: signature representation type (SRT). */
	public static final int SRT_ID = 4;
	// public static final int ISR_ID = 5;
	// public static final int HLL_ID = 6;
	// public static final int VLL_ID = 7;

	/* Type-10,13,14,15,16 Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	// public static final int IMP_ID = 3;
	/** Tagged image record field number: source agency (SRC). */
	public static final int SRC_ID = 4;
	/** Tagged image record field number: capture date (CD). */
	public static final int CD_ID = 5;
	// public static final int HLL_ID = 6;
	// public static final int VLL_ID = 7;
	/** Tagged image record field number: scale units (SLC). */
	public static final int SLC_ID = 8;
	/** Tagged image record field number: horizontal pixel scale (HPS). */
	public static final int HPS_ID = 9;
	/** Tagged image record field number: vertical pixel scale (VPS). */
	public static final int VPS_ID = 10;
	/** Tagged image record field number: compression algorithm (CGA). */
	public static final int TAG_CA_ID = 11;
	/** Tagged image record field number: color space (CSP, Type-10). */
	public static final int CSP_ID = 12;
	/** Color space field number in Type-17 records. */
	public static final int CSP_ID_TYPE_17 = 13;
	/** Tagged image record field number: bits per pixel (BPX). */
	public static final int BPX_ID = 12;
	/** Tagged image record field number: finger/palm position (FGP). */
	public static final int FGP3_ID = 13;
	/** Identifier of the image data field of tagged image records. */
	public static final String DAT2_ID = "IMAGE_FIELD";

	/* Type-10 field IDs, in addition the the common subset above... jck */
	/** Type-10 field number: image type (IMT). */
	public static final int IMT_ID = 3;
	/** Type-10 field number: photo date (PHD). */
	public static final int PHD_ID = 5;
	/* 6 HLL, 7 VLL, 8 SLC, 9 HPS, 10 VPS, 11 CGA (TAG_CA_ID), 12CSP */
	/** Type-10 field number: subject acquisition profile (SAP). */
	public static final int SAP_ID = 13;
	/* 14 and 15 are reserved */
	/* 16 SHPS, 17 SVPS */
	/* 18 and 19 are reserved */
	/** Type-10 field number: subject pose (POS). */
	public static final int POS_ID = 20;
	/** Type-10 field number: pose offset angle (POA). */
	public static final int POA_ID = 21;
	/** Type-10 field number: photo description (PXS). */
	public static final int PXS_ID = 22;
	/** Type-10 field number: photo acquisition source (PAS). */
	public static final int PAS_ID = 23;
	/** Type-10 field number: subject quality score (SQS). */
	public static final int SQS_ID = 24;
	/** Type-10 field number: subject pose angles (SPA). */
	public static final int SPA_ID = 25;
	/** Type-10 field number: subject facial description (SXS). */
	public static final int SXS_ID = 26;
	/** Type-10 field number: subject eye color (SEC). */
	public static final int SEC_ID = 27;
	/** Type-10 field number: subject hair color (SHC). */
	public static final int SHC_ID = 28;
	/** Type-10 field number: facial feature points (FFP). */
	public static final int FFP_ID = 29;
	/** Type-10 field number: device monitoring mode (DMM). */
	public static final int DMM_ID = 30;
	/* 31 through 39 are reserved */
	/** Type-10 field number: SMT source (SMT). */
	public static final int SMT_ID = 40;
	/** Type-10 field number: SMT size (SMS). */
	public static final int SMS_ID = 41;
	/** Type-10 field number: SMT descriptors (SMD). */
	public static final int SMD_ID = 42;
	/** Type-10 field number: colors present (COL). */
	public static final int COL_ID = 43;
	/* 44 through 199 reserved */

	/* Type-13,14,15 field IDs, in addition to the common subset above... jck */
	/* Type-13,14 respecively, reserved in type 15... */
	/** Type-13 field number: search position descriptors (SPD). */
	public static final int SPD_ID = 14;
	/** Type-14 field number: print position descriptors (PPD). */
	public static final int PPD_ID = 14;

	/* Type-13,14, reserved in type 15... */
	/** Type-13/14 field number: print position coordinates (PPC). */
	public static final int PPC_ID = 15;

	/* Type-13,14,15... */
	/** Type-13/14/15 field number: scanned horizontal pixel scale (SHPS). */
	public static final int SHPS_ID = 16;
	/** Type-13/14/15 field number: scanned vertical pixel scale (SVPS). */
	public static final int SVPS_ID = 17;

	/* Type-14 only, reserved in Type-13,15... */
	/** Type-14 field number: amputated or bandaged (AMP). */
	public static final int AMP_ID = 18;

	/* 19 is reserved in Type-13,14,15. */
	/* Type-13,14,15... */
	/** Type-13/14/15 field number: comment (COM). */
	public static final int COM_ID = 20;

	/* Type-14 only, reserved in Type-13,15 */
	/** Type-14 field number: fingerprint segmentation position (SEG). */
	public static final int SEG_ID = 21;
	/** Type-14 field number: NIST quality metric (NQM). */
	public static final int NQM_ID = 22;
	/** Type-14 field number: segmentation quality metric (SQM). */
	public static final int SQM_ID = 23;

	/* Type-13,14,15 respecively... */
	/** Type-13 field number: latent quality metric (LQM). */
	public static final int LQM_ID = 24;
	/** Type-14 field number: fingerprint quality metric (FQM). */
	public static final int FQM_ID = 24;
	/** Type-15 field number: palmprint quality metric (PQM). */
	public static final int PQM_ID = 24;

	/* Type-14 only, reserved in Type-13,15... */
	/** Type-14 field number: alternate finger segment position (ASEG). */
	public static final int ASEG_ID = 25;

	/* 26 through 29 are reserved in Type-13,14,15. */

	/* Type-14,15, reserved in Type-13... */
	// public static final int DMM_ID = 30;
	/* End of Type-13,14,15 field IDs. */

	/* Type-9 Standard Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	// public static final int IMP_ID = 3;
	/** Type-9 field number: minutiae format (FMT). */
	public static final int FMT_ID = 4;
	/** Type-9 field number: originating fingerprint reading system (OFR). */
	public static final int OFR_ID = 5;
	/** Type-9 field number: finger position (FGP). */
	public static final int FGP2_ID = 6;
	/** Type-9 field number: fingerprint pattern classification (FPC). */
	public static final int FPC_ID = 7;
	/** Type-9 field number: core position (CRP). */
	public static final int CRP_ID = 8;
	/** Type-9 field number: delta position (DLT). */
	public static final int DLT_ID = 9;
	/** Type-9 field number: number of minutiae (MIN). */
	public static final int MIN_ID = 10;
	/** Type-9 field number: minutiae ridge count indicator (RDG). */
	public static final int RDG_ID = 11;
	/** Type-9 field number: minutiae and ridge count data (MRC). */
	public static final int MRC_ID = 12;
	/* Type-9 FBI/IAFIS Field IDs */
	/* EFTS Field 13 Non-standard! */
	/** FBI/IAFIS Type-9 field number: finger number (FGN). */
	public static final int FGN_ID = 14;
	/** FBI/IAFIS Type-9 field number: number of minutiae (NMN). */
	public static final int NMN_ID = 15;
	/** FBI/IAFIS Type-9 field number: fingerprint characterization process (FCP). */
	public static final int FCP_ID = 16;
	/** FBI/IAFIS Type-9 field number: AFIS/FBI pattern classification (APC). */
	public static final int APC_ID = 17;
	/** FBI/IAFIS Type-9 field number: region of value (ROV). */
	public static final int ROV_ID = 18;
	/** FBI/IAFIS Type-9 field number: coordinate offsets (COF). */
	public static final int COF_ID = 19;
	/** FBI/IAFIS Type-9 field number: orientation uncertainty (ORN). */
	public static final int ORN_ID = 20;
	/** FBI/IAFIS Type-9 field number: core attributes (CRA). */
	public static final int CRA_ID = 21;
	/** FBI/IAFIS Type-9 field number: delta attributes (DLA). */
	public static final int DLA_ID = 22;
	/** FBI/IAFIS Type-9 field number: minutiae and ridge count data (MAT). */
	public static final int MAT_ID = 23;

	/** Maximum number of minutiae in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_MINUTIAE = 254;
	/** Maximum number of pattern classes in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_PATTERN_CLASSES = 3;
	/** Maximum number of cores in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_CORES = 2;
	/** Maximum number of deltas in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_DELTAS = 2;
	/** Maximum number of items in an FBI/IAFIS minutia subfield. */
	public static final int MAX_IAFIS_MINUTIA_ITEMS = 13;
	/** Number of characters in an FBI/IAFIS method string. */
	public static final int IAFIS_METHOD_STRLEN = 3;

	/** Minimum Table 5 impression code. */
	public static final int MIN_TABLE_5_CODE = 0;
	/** Maximum Table 5 impression code. */
	public static final int MAX_TABLE_5_CODE = 29;

	/** Minimum Table 6 finger position code. */
	public static final int MIN_TABLE_6_CODE = 0;
	/** Maximum Table 6 finger position code. */
	public static final int MAX_TABLE_6_CODE = 16;

	/** Minimum Table 19 palm code. */
	public static final int MIN_TABLE_19_CODE = 20;
	/** Maximum Table 19 palm code. */
	public static final int MAX_TABLE_19_CODE = 30;

	/** Minimum minutia quality value. */
	public static final int MIN_QUALITY_VALUE = 0;
	/** Maximum minutia quality value. */
	public static final int MAX_QUALITY_VALUE = 63;

	/** Minimum scanning resolution in pixels/mm (500 dpi). */
	public static final double MIN_RESOLUTION = 19.69;
	/** Minimum scanning resolution in pixels/mm as stored in tagged field images. */
	public static final double MIN_TAGGED_RESOLUTION = 19.7;
	/** Scan resolution tolerance in millimeters. */
	public static final double MM_TOLERANCE = 0.2;

	/** Number of characters in a field number tag. */
	public static final int FIELD_NUM_LEN = 9;
	/** Character separating a field tag from its value. */
	public static final char ITEM_START = '=';
	/** Character ending an item (the US separator). */
	public static final int ITEM_END = US_CHAR;

	/** Code string: standard. */
	public static final String STD_STR = "S";
	/** Code string: user-defined. */
	public static final String USER_STR = "U";
	/** Code string: table. */
	public static final String TBL_STR = "T";
	/** Code string: automatic. */
	public static final String AUTO_STR = "A";
	/** Scale units code: pixels per inch. */
	public static final String PPI_STR = "1";
	/** Scale units code: pixels per centimeter. */
	public static final String PP_CM = "2";

	/** Edit operation code: delete. */
	public static final char DEL_OP = 'd';
	/** Edit operation code: insert. */
	public static final char INS_OP = 'i';
	/** Edit operation code: print. */
	public static final char PRN_OP = 'p';
	/** Edit operation code: substitute. */
	public static final char SUB_OP = 's';

	// #define DEFAULT_FPOUT stdout

	/** Maximum number of decimal characters of an unsigned int. */
	public static final int MAX_UINT_CHARS = 10;
	/** Maximum number of decimal characters of an unsigned short. */
	public static final int MAX_USHORT_CHARS = 5;
	/** Maximum number of decimal characters of an unsigned char. */
	public static final int MAX_UCHAR_CHARS = 3;

	/** String value for unused fields ("255"). */
	public static final String UNUSED_STR = "255";

	/** Millimeters per inch, used to convert pixels per inch to pixels per millimeter. */
	public static final double MM_PER_INCH = 25.4;

	/** Hand code: unknown. */
	public static final int UNKNOWN_HAND = 0;
	/** Hand code: right hand. */
	public static final int RIGHT_HAND = 1;
	/** Hand code: left hand. */
	public static final int LEFT_HAND = 2;

	/** Tagged compression code: none. */
	public static final String COMP_NONE = "NONE";
	/** Binary compression code: none. */
	public static final String BIN_COMP_NONE = "0";
	/** Tagged compression code: WSQ. */
	public static final String COMP_WSQ = "WSQ20";
	/** Binary compression code: WSQ. */
	public static final String BIN_COMP_WSQ = "1";
	/** Tagged compression code: baseline JPEG. */
	public static final String COMP_JPEGB = "JPEGB";
	/** Binary compression code: baseline JPEG. */
	public static final String BIN_COMP_JPEGB = "2";
	/** Tagged compression code: lossless JPEG. */
	public static final String COMP_JPEGL = "JPEGL";
	/** Binary compression code: lossless JPEG. */
	public static final String BIN_COMP_JPEGL = "3";
	/** Tagged compression code: JPEG 2000. */
	public static final String COMP_JPEG2K = "JP2";
	/** Binary compression code: JPEG 2000. */
	public static final String BIN_COMP_JPEG2K = "4";
	/** Tagged compression code: lossless JPEG 2000. */
	public static final String COMP_JPEG2KL = "JP2L";
	/** Binary compression code: lossless JPEG 2000. */
	public static final String BIN_COMP_JPEG2KL = "5";
	/** Tagged compression code: PNG. */
	public static final String COMP_PNG = "PNG";
	/** Binary compression code: PNG. */
	public static final String BIN_COMP_PNG = "6";
	/** Color space: grayscale. */
	public static final String CSP_GRAY = "GRAY";
	/** Color space: RGB. */
	public static final String CSP_RGB = "RGB";
	/** Color space: YCbCr. */
	public static final String CSP_YCC = "YCC";
	/** Color space: sRGB. */
	public static final String CSP_SRGB = "SRGB";
	/** Color space: sYCC. */
	public static final String CSP_SYCC = "SYCC";

	/** Maximum value of an unsigned char. */
	public static final short UCHAR_MAX = 255;

	/** In-memory representation of an entire ANSI/NIST file ({@code ANSI_NIST}). */
	@SuppressWarnings("unused")
	public class AnsiNist {
		/** Standard version of the file (e.g. {@link IAn2k#VERSION_0400}). */
		private int version;
		/** Total number of bytes in the file. */
		private int numOfBytes;
		/** Number of logical records. */
		private int numOfRecords;
		/** Allocated capacity of the record list. */
		private int allocRecords;
		/** Logical records. */
		private AtomicReferenceArray<Record> records;

		/** Creates an empty file structure. */
		public AnsiNist() {
			super();
		}

		/**
		 * Creates a file structure with the given attributes.
		 *
		 * @param version      standard version
		 * @param numOfBytes   total number of bytes
		 * @param numOfRecords number of logical records
		 * @param allocRecords allocated capacity of the record list
		 * @param records      logical records
		 */
		public AnsiNist(int version, int numOfBytes, int numOfRecords, int allocRecords,
				AtomicReferenceArray<Record> records) {
			this();
			this.version = version;
			this.numOfBytes = numOfBytes;
			this.numOfRecords = numOfRecords;
			this.allocRecords = allocRecords;
			this.records = records;
		}

		/**
		 * Creates a shallow copy of another file structure (the record list is shared).
		 *
		 * @param ansiNist structure to copy
		 */
		public AnsiNist(AnsiNist ansiNist) {
			this.version = ansiNist.version;
			this.numOfBytes = ansiNist.numOfBytes;
			this.numOfRecords = ansiNist.numOfRecords;
			this.allocRecords = ansiNist.allocRecords;
			this.records = ansiNist.records;
		}
	}

	/**
	 * Read/write cursor over an in-memory byte buffer ({@code AN2KBDB}), used when parsing AN2K data from memory
	 * instead of a file.
	 */
	@SuppressWarnings("unused")
	public class BasicDataBuffer {
		/** Maximum size of the buffer. */
		private int bdbSize; // Max size of the buffer
		/** Beginning read/write location. */
		private int bdbStart; // Beginning read/write location
		/** End read/write location. */
		private int bdbEnd; // End read/write location
		/** Current read/write location. */
		private int bdbCurrent; // Current read/write location

		/**
		 * Creates a buffer cursor starting at a position.
		 *
		 * @param position start (and current) position
		 * @param bdbSize  buffer size in bytes
		 */
		public BasicDataBuffer(int position, int bdbSize) {
			super();
			this.bdbSize = bdbSize;
			this.bdbCurrent = position;
			this.bdbStart = this.bdbCurrent;
			this.bdbEnd = position + bdbSize;
		}
		
		/**
		 * Creates a copy of another buffer cursor.
		 *
		 * @param basicDataBuffer cursor to copy
		 */
		public BasicDataBuffer(BasicDataBuffer basicDataBuffer) {
			this.bdbSize = basicDataBuffer.bdbSize;
			this.bdbCurrent = basicDataBuffer.bdbCurrent;
			this.bdbStart = basicDataBuffer.bdbStart;
			this.bdbEnd = basicDataBuffer.bdbEnd;
		}
	}

	/** A field of a logical record ({@code FIELD}). */
	@SuppressWarnings("unused")
	public class Field {
		/** Field tag string (e.g. {@code "1.001"}). */
		private String id;
		/** Type of the record containing the field. */
		private int recordType;
		/** Field number within the record. */
		private int fieldInfo;
		/** Number of bytes in the field, including separators. */
		private int numOfBytes;
		/** Number of subfields. */
		private int numOfSubfields;
		/** Allocated capacity of the subfield list. */
		private int allocSubfields;
		/** Subfields. */
		private AtomicReferenceArray<SubField> subfields;
		/** Trailing group separator character, if present. */
		private int gsChar;

		/** Creates an empty field. */
		public Field() {
			super();
		}

		/**
		 * Creates a field with the given attributes.
		 *
		 * @param id             field tag string
		 * @param recordType     type of the containing record
		 * @param fieldInfo      field number
		 * @param numOfBytes     number of bytes
		 * @param numOfSubfields number of subfields
		 * @param allocSubfields allocated capacity of the subfield list
		 * @param subfields      subfields
		 * @param gsChar         trailing group separator character
		 */
		@SuppressWarnings({ "java:S107" })
		public Field(String id, int recordType, int fieldInfo, int numOfBytes, int numOfSubfields, int allocSubfields,
				AtomicReferenceArray<SubField> subfields, int gsChar) {
			this();
			this.id = id;
			this.recordType = recordType;
			this.fieldInfo = fieldInfo;
			this.numOfBytes = numOfBytes;
			this.numOfSubfields = numOfSubfields;
			this.allocSubfields = allocSubfields;
			this.subfields = subfields;
			this.gsChar = gsChar;
		}

		/**
		 * Creates a shallow copy of another field (the subfield list is shared).
		 *
		 * @param fieldInfo field to copy
		 */
		public Field(Field fieldInfo) {
			this.id = fieldInfo.id;
			this.recordType = fieldInfo.recordType;
			this.fieldInfo = fieldInfo.fieldInfo;
			this.numOfBytes = fieldInfo.numOfBytes;
			this.numOfSubfields = fieldInfo.numOfSubfields;
			this.allocSubfields = fieldInfo.allocSubfields;
			this.subfields = fieldInfo.subfields;
			this.gsChar = fieldInfo.gsChar;
		}
	}

	/** An item of a subfield ({@code ITEM}). */
	@SuppressWarnings("unused")
	public class Item {
		/** Current byte size of the entire item, including any trailing US separator. */
		private int numOfBytes; // Always contains the current byte size of the entire
		/** Number of characters currently in the value, not including the NUL terminator. */
		private int numOfChars; // Number of characters currently in value, NOT
		/** Number of characters allocated for the value, including the NUL terminator. */
		private int allocChars; // Number of allocated characters for the value,
		/** Item value (must be kept NUL terminated in the C original). */
		private byte value; // Must keep NULL terminated.
		/** Trailing unit separator character, if present. */
		private int usChar;

		/** Creates an empty item. */
		public Item() {
			super();
		}

		/**
		 * Creates an item with the given attributes.
		 *
		 * @param numOfBytes byte size of the item
		 * @param numOfChars number of characters in the value
		 * @param allocChars number of allocated characters
		 * @param value      item value
		 * @param usChar     trailing unit separator character
		 */
		public Item(int numOfBytes, int numOfChars, int allocChars, byte value, int usChar) {
			this();
			this.numOfBytes = numOfBytes;
			this.numOfChars = numOfChars;
			this.allocChars = allocChars;
			this.value = value;
			this.usChar = usChar;
		}

		/**
		 * Creates a copy of another item.
		 *
		 * @param itemInfo item to copy
		 */
		public Item(Item itemInfo) {
			this.numOfBytes = itemInfo.numOfBytes;
			this.numOfChars = itemInfo.numOfChars;
			this.allocChars = itemInfo.allocChars;
			this.value = itemInfo.value;
			this.usChar = itemInfo.usChar;
		}
	}

	/** A polygon of a segmentation description ({@code POLYGON}). */
	@SuppressWarnings("unused")
	public class Polygon {
		/** Finger/palm position the polygon belongs to. */
		private int fgp;
		/** Number of vertices. */
		private int numOfPoints;
		/** X coordinates of the vertices. */
		private byte x;
		/** Y coordinates of the vertices. */
		private byte y;
	}

	/** A logical record of an ANSI/NIST file ({@code RECORD}). */
	@SuppressWarnings("unused")
	public class Record {
		/** Record type (e.g. {@link IAn2k#TYPE_14_ID}). */
		private int type;
		/** Total number of bytes of the record as stored in its LEN field. */
		private int totalBytes;
		/** Number of bytes accumulated while reading/building the record. */
		private int numOfBytes;
		/** Number of fields. */
		private int numOfFields;
		/** Allocated capacity of the field list. */
		private int allocFields;
		/** Fields. */
		private AtomicReferenceArray<Field> fields;
		/** Trailing file separator character, if present. */
		private int fsChar;

		/** Creates an empty record. */
		public Record() {
			super();
		}

		/**
		 * Creates a record with the given attributes.
		 *
		 * @param type        record type
		 * @param totalBytes  total number of bytes as stored in LEN
		 * @param numOfBytes  accumulated number of bytes
		 * @param numOfFields number of fields
		 * @param allocFields allocated capacity of the field list
		 * @param fields      fields
		 * @param fsChar      trailing file separator character
		 */
		public Record(int type, int totalBytes, int numOfBytes, int numOfFields, int allocFields,
				AtomicReferenceArray<Field> fields, int fsChar) {
			this();
			this.type = type;
			this.totalBytes = totalBytes;
			this.numOfBytes = numOfBytes;
			this.numOfFields = numOfFields;
			this.allocFields = allocFields;
			this.fields = fields;
			this.fsChar = fsChar;
		}

		/**
		 * Creates a shallow copy of another record (the field list is shared).
		 *
		 * @param recordInfo record to copy
		 */
		public Record(Record recordInfo) {
			this.type = recordInfo.type;
			this.totalBytes = recordInfo.totalBytes;
			this.numOfBytes = recordInfo.numOfBytes;
			this.numOfFields = recordInfo.numOfFields;
			this.allocFields = recordInfo.allocFields;
			this.fields = recordInfo.fields;
			this.fsChar = recordInfo.fsChar;
		}
	}

	/** A record selection criterion ({@code REC_SEL}), possibly a boolean combination of nested criteria. */
	@SuppressWarnings("unused")
	public class RecordSelected {
		/** Criterion type. */
		private RecordSelectedType type;
		/** Allocated capacity of the value list. */
		private int allocValues;
		/** Number of values. */
		private int numOfValues;
		/** Criterion value(s). */
		private RecordSelectedValue value;

		/** Creates an empty criterion. */
		public RecordSelected() {
			super();
		}

		/**
		 * Creates a criterion with the given attributes.
		 *
		 * @param type        criterion type
		 * @param allocValues allocated capacity of the value list
		 * @param numOfValues number of values
		 * @param value       criterion value(s)
		 */
		public RecordSelected(RecordSelectedType type, int allocValues, int numOfValues, RecordSelectedValue value) {
			this();
			this.type = type;
			this.allocValues = allocValues;
			this.numOfValues = numOfValues;
			this.value = value;
		}

		/**
		 * Creates a shallow copy of another criterion.
		 *
		 * @param recordSelectedInfo criterion to copy
		 */
		public RecordSelected(RecordSelected recordSelectedInfo) {
			this.type = recordSelectedInfo.type;
			this.allocValues = recordSelectedInfo.allocValues;
			this.numOfValues = recordSelectedInfo.numOfValues;
			this.value = recordSelectedInfo.value;
		}
	}

	/** Types of record selection criteria ({@code REC_SEL_TYPE}). */
	public enum RecordSelectedType {
		/** Boolean AND of nested criteria. */
		RS_AND(1000),
		/** Boolean OR of nested criteria. */
		RS_OR(1001),
		/** Logical record type. */
		RS_LRT(1002), // logical recordInfo type
		/** Finger or palm position. */
		RS_FGPLP(1003), // finger or palm position
		/** Finger position. */
		RS_FGP(1004), // finger position
		/** Palm position. */
		RS_PLP(1005), // palm position
		/** Impression type. */
		RS_IMP(1006), // impression type
		/** Image descriptor character. */
		RS_IDC(1007), // image descriptor chararacter
		/** NIST quality metric. */
		RS_NQM(1008), // NIST quality metric
		/** Image type. */
		RS_IMT(1009),
		/** Subject pose. */
		RS_POS(1010); // subject pose

		/** Size in bits of the underlying integer value. */
		public static final int SIZE = java.lang.Integer.SIZE;

		/** Numeric code of the constant. */
		private int intValue;
		/** Lazily-created map from numeric code to constant. */
		private static java.util.HashMap<Integer, RecordSelectedType> mappings;

		/**
		 * Returns the code-to-constant map, creating it if necessary.
		 *
		 * @return the mapping table
		 */
		private static java.util.HashMap<Integer, RecordSelectedType> getMappings() {
			if (mappings == null) {
				mappings = new java.util.HashMap<>();
			}
			return mappings;
		}

		/**
		 * Creates a constant and registers it in the code map.
		 *
		 * @param value numeric code
		 */
		private RecordSelectedType(int value) {
			intValue = value;
			getMappings().put(value, this);
		}

		/**
		 * Returns the numeric code of this constant.
		 *
		 * @return the numeric code
		 */
		public int getValue() {
			return intValue;
		}

		/**
		 * Returns the constant with a numeric code.
		 *
		 * @param value numeric code
		 * @return the matching constant, or {@code null} if none
		 */
		public static RecordSelectedType forValue(int value) {
			return getMappings().get(value);
		}
	}

	/** Value of a record selection criterion ({@code REC_SEL_VALUE}): a number, a string, or nested criteria. */
	@SuppressWarnings("unused")
	public class RecordSelectedValue {
		/** Numeric value. */
		private long num; /* initialization assumes a pointer is never larger than a long */
		/** String value. */
		private String str;
		/** Nested criteria. */
		private List<RecordSelected> rs = new ArrayList<>();

		/** Creates an empty value. */
		public RecordSelectedValue() {
			super();
		}

		/**
		 * Creates a numeric value.
		 *
		 * @param num numeric value
		 */
		public RecordSelectedValue(long num) {
			super();
			this.num = num;
		}

		/**
		 * Creates a value with the given attributes.
		 *
		 * @param num         numeric value
		 * @param allocValues allocated capacity (unused)
		 * @param str         string value
		 * @param rs          nested criteria
		 */
		@SuppressWarnings({ "unused" })
		public RecordSelectedValue(long num, int allocValues, String str, List<RecordSelected> rs) {
			this();
			this.num = num;
			this.str = str;
			this.rs = rs;
		}

		/**
		 * Creates a shallow copy of another value.
		 *
		 * @param recordSelectedValueInfo value to copy
		 */
		public RecordSelectedValue(RecordSelectedValue recordSelectedValueInfo) {
			this.num = recordSelectedValueInfo.num;
			this.str = recordSelectedValueInfo.str;
			this.rs = recordSelectedValueInfo.rs;
		}
	}

	/** Kinds of record selection values ({@code REC_SEL_VALUE} discriminator). */
	public enum RecordSelectedValueType {
		/** Nested selection criteria. */
		RSV_RS(2000),
		/** Numeric value. */
		RSV_NUM(2001),
		/** String value. */
		RSV_STR(2002);

		/** Size in bits of the underlying integer value. */
		public static final int SIZE = java.lang.Integer.SIZE;

		/** Numeric code of the constant. */
		private int intValue;
		/** Lazily-created map from numeric code to constant. */
		private static java.util.HashMap<Integer, RecordSelectedValueType> mappings;

		/**
		 * Returns the code-to-constant map, creating it if necessary.
		 *
		 * @return the mapping table
		 */
		private static java.util.HashMap<Integer, RecordSelectedValueType> getMappings() {
			if (mappings == null) {
				mappings = new java.util.HashMap<>();
			}
			return mappings;
		}

		/**
		 * Creates a constant and registers it in the code map.
		 *
		 * @param value numeric code
		 */
		private RecordSelectedValueType(int value) {
			intValue = value;
			getMappings().put(value, this);
		}

		/**
		 * Returns the numeric code of this constant.
		 *
		 * @return the numeric code
		 */
		public int getValue() {
			return intValue;
		}

		/**
		 * Returns the constant with a numeric code.
		 *
		 * @param value numeric code
		 * @return the matching constant, or {@code null} if none
		 */
		public static RecordSelectedValueType forValue(int value) {
			return getMappings().get(value);
		}
	}

	/** Segmentation description made of polygons ({@code SEGMENTS}). */
	@SuppressWarnings("unused")
	public class Segments {
		/** Number of polygons. */
		private int numOfPolygons;
		/** Polygons. */
		private Polygon polygons;
	}

	/** A subfield of a field ({@code SUBFIELD}). */
	@SuppressWarnings("unused")
	public class SubField {
		/** Number of bytes in the subfield, including separators. */
		private int numOfBytes;
		/** Number of items. */
		private int numOfItems;
		/** Allocated capacity of the item list. */
		private int allocItems;
		/** Items. */
		private AtomicReferenceArray<Item> items;
		/** Trailing record separator character, if present. */
		private int rsChar;

		/** Creates an empty subfield. */
		public SubField() {
			super();
		}

		/**
		 * Creates a subfield with the given attributes.
		 *
		 * @param numOfBytes number of bytes
		 * @param numOfItems number of items
		 * @param allocItems allocated capacity of the item list
		 * @param items      items
		 * @param rsChar     trailing record separator character
		 */
		public SubField(int numOfBytes, int numOfItems, int allocItems, AtomicReferenceArray<Item> items, int rsChar) {
			this();
			this.numOfBytes = numOfBytes;
			this.numOfItems = numOfItems;
			this.allocItems = allocItems;
			this.items = items;
			this.rsChar = rsChar;
		}

		/**
		 * Creates a shallow copy of another subfield (the item list is shared).
		 *
		 * @param subFieldInfo subfield to copy
		 */
		public SubField(SubField subFieldInfo) {
			this.numOfBytes = subFieldInfo.numOfBytes;
			this.numOfItems = subFieldInfo.numOfItems;
			this.allocItems = subFieldInfo.allocItems;
			this.items = subFieldInfo.items;
			this.rsChar = subFieldInfo.rsChar;
		}
	}

	/*************************************************************************/
	/* EXTERNAL FUNCTION DEFINITIONS */
	/*************************************************************************/
	/** Allocation routines (port of NIST's {@code alloc.c}). */
	public interface IAlloc {
		/**
		 * Allocates an empty ANSI/NIST structure (NIST {@code alloc_ANSI_NIST}).
		 *
		 * @param ret output return code: {@code 0} on success, negative on system error
		 * @return the new structure
		 */
		public AnsiNist allocANSIToNIST(AtomicInteger ret);

		/**
		 * Allocates a new record of a type, including its LEN and IDC fields as appropriate (NIST
		 * {@code new_ANSI_NIST_record}).
		 *
		 * @param ret        output return code: {@code 0} on success, negative on system error
		 * @param recordType record type
		 * @return the new record
		 */
		public Record newANSIToNISTRecord(AtomicInteger ret, final int recordType);

		/**
		 * Allocates an empty record (NIST {@code alloc_ANSI_NIST_record}).
		 *
		 * @param ret output return code: {@code 0} on success, negative on system error
		 * @return the new record
		 */
		public Record allocANSIToNISTRecord(AtomicInteger ret);

		/**
		 * Allocates a new field with its tag set from the record type and field number (NIST
		 * {@code new_ANSI_NIST_field}).
		 *
		 * @param ret        output return code: {@code 0} on success, negative on system error
		 * @param recordType record type
		 * @param field      field number
		 * @return the new field
		 */
		public Field newANSIToNISTField(AtomicInteger ret, final int recordType, final int field);

		/**
		 * Allocates an empty field (NIST {@code alloc_ANSI_NIST_field}).
		 *
		 * @param ret output return code: {@code 0} on success, negative on system error
		 * @return the new field
		 */
		public Field allocANSIToNISTField(AtomicInteger ret);

		/**
		 * Allocates an empty subfield (NIST {@code alloc_ANSI_NIST_subfield}).
		 *
		 * @param ret output return code: {@code 0} on success, negative on system error
		 * @return the new subfield
		 */
		public SubField allocANSIToNISTSubfield(AtomicInteger ret);

		/**
		 * Allocates an empty item (NIST {@code alloc_ANSI_NIST_item}).
		 *
		 * @param ret output return code: {@code 0} on success, negative on system error
		 * @return the new item
		 */
		public Item allocANSIToNISTItem(AtomicInteger ret);

		/**
		 * Releases an ANSI/NIST structure and all its records (NIST {@code free_ANSI_NIST}).
		 *
		 * @param ansiNist structure to release
		 */
		public void freeANSIToNIST(AnsiNist ansiNist);

		/**
		 * Releases a record and its fields (NIST {@code free_ANSI_NIST_record}).
		 *
		 * @param recordInfo record to release
		 */
		public void freeANSIToNISTRecord(Record recordInfo);

		/**
		 * Releases a field and its subfields (NIST {@code free_ANSI_NIST_field}).
		 *
		 * @param field field to release
		 */
		public void freeANSIToNISTField(Field field);

		/**
		 * Releases a subfield and its items (NIST {@code free_ANSI_NIST_subfield}).
		 *
		 * @param subField subfield to release
		 */
		public void freeANSIToNISTSubfield(SubField subField);

		/**
		 * Releases an item (NIST {@code free_ANSI_NIST_item}).
		 *
		 * @param item item to release
		 */
		public void freeANSIToNISTItem(Item item);
	}

	/** Append routines (port of NIST's {@code append.c}). */
	public interface IAppend {
		/**
		 * Appends a field to a record, updating byte counts (NIST {@code append_ANSI_NIST_record}).
		 *
		 * @param recordInfo record to extend
		 * @param field      field to append
		 * @return {@code 0} on success, negative on system error
		 */
		public int appendANSIToNISTRecord(Record recordInfo, Field field);

		/**
		 * Appends a subfield to a field, updating byte counts (NIST {@code append_ANSI_NIST_field}).
		 *
		 * @param field    field to extend
		 * @param subField subfield to append
		 * @return {@code 0} on success, negative on system error
		 */
		public int appendANSIToNISTField(Field field, SubField subField);

		/**
		 * Appends an item to a subfield, updating byte counts (NIST {@code append_ANSI_NIST_subfield}).
		 *
		 * @param subField subfield to extend
		 * @param item     item to append
		 * @return {@code 0} on success, negative on system error
		 */
		public int appendANSIToNISTSubfield(SubField subField, Item item);
	}

	/** Deep-copy routines (port of NIST's {@code copy.c}). */
	public interface ICopy {
		/**
		 * Creates a deep copy of an ANSI/NIST structure (NIST {@code copy_ANSI_NIST}).
		 *
		 * @param returnCode output return code: {@code 0} on success, negative on system error
		 * @param toAnsiNist structure to copy
		 * @return the copy
		 */
		public AnsiNist copyANSIToNIST(AtomicInteger returnCode, AnsiNist toAnsiNist);

		/**
		 * Creates a deep copy of a record (NIST {@code copy_ANSI_NIST_record}).
		 *
		 * @param returnCode output return code: {@code 0} on success, negative on system error
		 * @param toRecord   record to copy
		 * @return the copy
		 */
		public Record copyANSIToNISTRecord(AtomicInteger returnCode, Record toRecord);

		/**
		 * Creates a deep copy of a field (NIST {@code copy_ANSI_NIST_field}).
		 *
		 * @param returnCode output return code: {@code 0} on success, negative on system error
		 * @param toField    field to copy
		 * @return the copy
		 */
		public Field copyANSIToNISTField(AtomicInteger returnCode, Field toField);

		/**
		 * Creates a deep copy of a subfield (NIST {@code copy_ANSI_NIST_subfield}).
		 *
		 * @param returnCode output return code: {@code 0} on success, negative on system error
		 * @param toSubField subfield to copy
		 * @return the copy
		 */
		public SubField copyANSIToNISTSubfield(AtomicInteger returnCode, SubField toSubField);

		/**
		 * Creates a deep copy of an item (NIST {@code copy_ANSI_NIST_item}).
		 *
		 * @param returnCode output return code: {@code 0} on success, negative on system error
		 * @param toItem     item to copy
		 * @return the copy
		 */
		public Item copyANSIToNISTItem(AtomicInteger returnCode, Item toItem);
	}

	/** Date routines (port of NIST's {@code date.c}). */
	public interface IDate {
		/**
		 * Produces the current date as an ANSI/NIST date string ({@code YYYYMMDD}) (NIST {@code get_ANSI_NIST_date}).
		 *
		 * @param date date string buffer
		 * @return {@code 0} on success, negative on system error
		 */
		public int getANSIToNISTDate(String date);
	}

	/** Image decoding routines (port of NIST's {@code decode.c}). */
	public interface IDecode {
		/**
		 * Decodes the image of an image record, whatever its compression (NIST {@code decode_ANSI_NIST_image}).
		 *
		 * @param odata      output decoded pixel data
		 * @param ow         output image width in pixels
		 * @param oh         output image height in pixels
		 * @param od         output pixel depth in bits
		 * @param oppmm      output resolution in pixels per millimeter
		 * @param ansiNist   ANSI/NIST structure
		 * @param imgrecord_i index of the image record
		 * @param intrlvflag interleave flag for color images
		 * @return {@code TRUE} if decoded, {@code FALSE} if the record is not an image record, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int decodeANSIToNISTImage(byte[] odata, AtomicInteger ow, AtomicInteger oh, AtomicInteger od,
				AtomicReference<Double> oppmm, final AnsiNist ansiNist, final int imgrecord_i, final int intrlvflag);

		/**
		 * Decodes the image of a binary field image record (Type-3..6) (NIST {@code decode_binary_field_image}).
		 *
		 * @param image    output decoded pixel data
		 * @param a        output image width in pixels ({@code ow})
		 * @param b        output image height in pixels ({@code oh})
		 * @param c        output pixel depth in bits ({@code od})
		 * @param d        output resolution in pixels per millimeter ({@code oppmm})
		 * @param ansiNist ANSI/NIST structure
		 * @param e        index of the image record ({@code imgrecord_i})
		 * @return {@code 0} on success, negative on error
		 */
		public int decodeBinaryFieldImage(byte[] image, int a, int b, int c, double d, final AnsiNist ansiNist,
				final int e);

		/**
		 * Decodes the image of a tagged field image record (Type-10, 13..17) (NIST {@code decode_tagged_field_image}).
		 *
		 * @param image    output decoded pixel data
		 * @param a        output image width in pixels ({@code ow})
		 * @param b        output image height in pixels ({@code oh})
		 * @param c        output pixel depth in bits ({@code od})
		 * @param d        output resolution in pixels per millimeter ({@code oppmm})
		 * @param ansiNist ANSI/NIST structure
		 * @param e        index of the image record ({@code imgrecord_i})
		 * @param f        interleave flag for color images ({@code intrlvflag})
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int decodeTaggedFieldImage(byte[] image, int a, int b, int c, double d, final AnsiNist ansiNist,
				final int e, final int f);
	}

	/** Delete routines (port of NIST's {@code delete.c}). */
	public interface IDelete {
		/**
		 * Deletes the selected structure element and writes the result (NIST {@code do_delete}).
		 *
		 * @param a        output file name ({@code ofile})
		 * @param b        record index ({@code record_i})
		 * @param c        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param d        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param e        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int doDelete(final String a, final int b, final int c, final int d, final int e, AnsiNist ansiNist);

		/**
		 * Deletes the record, field, subfield or item selected by the given indices (NIST
		 * {@code delete_ANSI_NIST_select}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param c        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param d        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int deleteANSIToNISTSelect(final int a, final int b, final int c, final int d, AnsiNist ansiNist);

		/**
		 * Deletes a record and updates the Type-1 CNT field (NIST {@code delete_ANSI_NIST_record}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int deleteANSIToNISTRecord(final int a, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Adjusts the Type-1 CNT field and IDCs after a record deletion (NIST {@code adjust_delrec_CNT_IDCs}).
		 *
		 * @param a        index of the deleted record ({@code record_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int adjustDelrecCNTIDCs(final int a, AnsiNist ansiNist);

		/**
		 * Deletes a field from a record (NIST {@code delete_ANSI_NIST_field}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int deleteANSIToNISTField(final int a, final int b, AnsiNist ansiNist);

		/**
		 * Deletes a subfield from a field (NIST {@code delete_ANSI_NIST_subfield}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int deleteANSIToNISTSubfield(final int a, final int b, final int c, AnsiNist ansiNist);

		/**
		 * Deletes an item from a subfield (NIST {@code delete_ANSI_NIST_item}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        item index ({@code item_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int deleteANSIToNISTItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist);
	}

	/** Coordinate and direction flipping routines (port of NIST's {@code flip.c}). */
	public interface IFlip {
		/**
		 * Flips a Y coordinate string between top-left and bottom-left origin conventions (NIST {@code flip_y_coord}).
		 *
		 * @param a coordinate value string, modified in place ({@code value})
		 * @param b minutiae representation format ({@code fmt})
		 * @param c image height ({@code ih})
		 * @param d image resolution in pixels per millimeter ({@code ppmm})
		 * @return {@code 0} on success, negative on error
		 */
		public int flipYCoord(String a, final int b, final int c, final double d);

		/**
		 * Flips a minutia direction string between orientation conventions (NIST {@code flip_direction}).
		 *
		 * @param a direction value string, modified in place ({@code value})
		 * @param b minutiae representation format ({@code fmt})
		 * @return {@code 0} on success, negative on error
		 */
		public int flipDirection(String a, final int b);
	}

	/** Standard-format read, buffer-scan and write routines (port of NIST's {@code fmtstd.c}). */
	public interface IFmtStd {
		/**
		 * Reads an ANSI/NIST file into memory (NIST {@code read_ANSI_NIST_file}).
		 *
		 * @param ret      output return code: {@code 0} on success, negative on error
		 * @param fileName input file name
		 * @return the parsed structure
		 */
		public AnsiNist readANSIToNISTFile(AtomicInteger ret, String fileName);

		/**
		 * Reads an ANSI/NIST structure from an open file (NIST {@code read_ANSI_NIST}).
		 *
		 * @param file     input file
		 * @param ansiNist structure to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNIST(File file, AnsiNist ansiNist);

		/**
		 * Reads the Type-1 record and determines the standard version (NIST {@code read_Type1_record}).
		 *
		 * @param file       input file
		 * @param recordInfo record to populate
		 * @param version    output standard version
		 * @return {@code 0} on success, negative on error
		 */
		public int readType1Record(File file, Record recordInfo, AtomicInteger version);

		/**
		 * Reads all records following the Type-1 record (NIST {@code read_ANSI_NIST_remaining_records}).
		 *
		 * @param file     input file
		 * @param ansiNist structure to extend
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTRemainingRecords(File file, AnsiNist ansiNist);

		/**
		 * Reads one record of a given type (NIST {@code read_ANSI_NIST_record}).
		 *
		 * @param file       input file
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTRecord(File file, Record recordInfo, final int a);

		/**
		 * Reads a tagged record (NIST {@code read_ANSI_NIST_tagged_record}).
		 *
		 * @param file       input file
		 * @param recordInfo record to populate
		 * @param recordType record type
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTTaggedRecord(File file, Record recordInfo, int recordType);

		/**
		 * Reads the LEN field of a record (NIST {@code read_ANSI_NIST_record_length}).
		 *
		 * @param file        input file
		 * @param recordBytes output record length in bytes
		 * @param field       output LEN field
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTRecordLength(File file, AtomicInteger recordBytes, Field field);

		/**
		 * Reads the VER field of the Type-1 record (NIST {@code read_ANSI_NIST_version}).
		 *
		 * @param file     input file
		 * @param oversion output standard version
		 * @param field    output VER field
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTVersion(File file, AtomicInteger oversion, Field field);

		/**
		 * Reads a field whose value is a single integer (NIST {@code read_ANSI_NIST_integer_field}).
		 *
		 * @param file     input file
		 * @param oversion output integer value
		 * @param field    output field
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTIntegerField(File file, AtomicInteger oversion, Field field);

		/**
		 * Reads the remaining fields of a tagged record (NIST {@code read_ANSI_NIST_remaining_fields}).
		 *
		 * @param file       input file
		 * @param recordInfo record to extend
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTRemainingFields(File file, Record recordInfo);

		/**
		 * Reads one tagged field (NIST {@code read_ANSI_NIST_field}).
		 *
		 * @param file  input file
		 * @param field field to populate
		 * @param a     record type ({@code record_type})
		 * @return {@code 0} on success, {@link IAn2k#DONE} at end of record, negative on error
		 */
		public int readANSIToNISTField(File file, Field field, int a);

		/**
		 * Reads the binary image data field of a tagged image record (NIST {@code read_ANSI_NIST_image_field}).
		 *
		 * @param file  input file
		 * @param field field to populate
		 * @param a     field tag string ({@code field_id})
		 * @param b     record type ({@code record_type})
		 * @param c     field number ({@code field_int})
		 * @param d     number of bytes remaining in the record ({@code record_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTImageField(File file, Field field, String a, final int b, final int c,
				int d); /* Added by MDG 03-08-05 */

		/**
		 * Reads the value of a tagged ASCII field (NIST {@code read_ANSI_NIST_tagged_field}).
		 *
		 * @param file  input file
		 * @param field field to populate
		 * @param a     field tag string ({@code field_id})
		 * @param b     record type ({@code record_type})
		 * @param c     field number ({@code field_int})
		 * @param d     number of bytes remaining in the record ({@code record_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTTaggedField(File file, Field field, String a, final int b, final int c, int d);

		/**
		 * Reads and parses a field tag ({@code record.field:}) (NIST {@code read_ANSI_NIST_field_ID}).
		 *
		 * @param file       input file
		 * @param fieldId    output field tag string
		 * @param recordType output record type
		 * @param fieldInt   output field number
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTFieldID(File file, AtomicReference<String> fieldId, AtomicInteger recordType,
				AtomicInteger fieldInt);

		/**
		 * Parses a field tag from a byte buffer (NIST {@code parse_ANSI_NIST_field_ID}).
		 *
		 * @param buffer     input bytes
		 * @param oibufptr   in/out current buffer position
		 * @param ebufptr    end of the buffer
		 * @param fieldId    output field tag string
		 * @param recordType output record type
		 * @param fieldInt   output field number
		 * @return {@code 0} on success, negative on error
		 */
		public int parseANSIToNISTFieldID(byte[] buffer, AtomicInteger oibufptr, AtomicInteger ebufptr,
				AtomicReference<String> fieldId, AtomicInteger recordType, AtomicInteger fieldInt);

		/**
		 * Reads a subfield (NIST {@code read_ANSI_NIST_subfield}).
		 *
		 * @param file     input file
		 * @param subField subfield to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTSubfield(File file, SubField subField);

		/**
		 * Reads an item (NIST {@code read_ANSI_NIST_item}).
		 *
		 * @param file input file
		 * @param item item to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTItem(File file, Item item);

		/**
		 * Reads a binary image record (Type-3..6) (NIST {@code read_ANSI_NIST_binary_image_record}).
		 *
		 * @param file       input file
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTBinaryImageRecord(File file, Record recordInfo, final int a);

		/**
		 * Reads a binary signature record (Type-8) (NIST {@code read_ANSI_NIST_binary_signature_record}).
		 *
		 * @param file       input file
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTBinarySignatureRecord(File file, Record recordInfo, final int a);

		/**
		 * Reads a fixed-length binary field (NIST {@code read_ANSI_NIST_binary_field}).
		 *
		 * @param file  input file
		 * @param field field to populate
		 * @param a     number of bytes to read ({@code num_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int readANSIToNISTBinaryField(File file, Field field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT BUFFER SCAN ROUTINES */
		/**
		 * Scans an ANSI/NIST structure from a memory buffer (NIST {@code scan_ANSI_NIST}).
		 *
		 * @param an2kBDB  input buffer cursor
		 * @param ansiNist structure to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNIST(BasicDataBuffer an2kBDB, AnsiNist ansiNist);

		/**
		 * Scans the Type-1 record from a buffer (NIST {@code scan_Type1_record}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to populate
		 * @param a          output standard version ({@code oversion})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanType1Record(BasicDataBuffer an2kBDB, Record recordInfo, int a);

		/**
		 * Scans all records following the Type-1 record from a buffer (NIST {@code scan_ANSI_NIST_remaining_records}).
		 *
		 * @param an2kBDB  input buffer cursor
		 * @param ansiNist structure to extend
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTRemainingRecords(BasicDataBuffer an2kBDB, AnsiNist ansiNist);

		/**
		 * Scans one record of a given type from a buffer (NIST {@code scan_ANSI_NIST_record}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		/**
		 * Scans a tagged record from a buffer (NIST {@code scan_ANSI_NIST_tagged_record}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTTaggedRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		/**
		 * Scans the LEN field of a record from a buffer (NIST {@code scan_ANSI_NIST_record_length}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param a       output record length in bytes ({@code orecord_bytes})
		 * @param field   output LEN field
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTRecordLength(BasicDataBuffer an2kBDB, int a, Field field);

		/**
		 * Scans the VER field from a buffer (NIST {@code scan_ANSI_NIST_version}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param a       output standard version ({@code oversion})
		 * @param field   output VER field
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTVersion(BasicDataBuffer an2kBDB, int a, Field field);

		/**
		 * Scans a field whose value is a single integer from a buffer (NIST {@code scan_ANSI_NIST_integer_field}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param a       output integer value
		 * @param field   output field
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTIntegerField(BasicDataBuffer an2kBDB, int a, Field field);

		/**
		 * Scans the remaining fields of a tagged record from a buffer (NIST {@code scan_ANSI_NIST_remaining_fields}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to extend
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTRemainingFields(BasicDataBuffer an2kBDB, Record recordInfo);

		/**
		 * Scans one tagged field from a buffer (NIST {@code scan_ANSI_NIST_field}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param field   field to populate
		 * @param a       record type ({@code record_type})
		 * @return {@code 0} on success, {@link IAn2k#DONE} at end of record, negative on error
		 */
		public int scanANSIToNISTField(BasicDataBuffer an2kBDB, Field field, int a);

		/**
		 * Scans the binary image data field of a tagged image record from a buffer (NIST
		 * {@code scan_ANSI_NIST_image_field}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param field   field to populate
		 * @param a       field tag string ({@code field_id})
		 * @param b       record type ({@code record_type})
		 * @param c       field number ({@code field_int})
		 * @param d       number of bytes remaining in the record ({@code record_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTImageField(BasicDataBuffer an2kBDB, Field field, String a, final int b, final int c,
				int d); /* Added by MDG 03-08-05 */

		/**
		 * Scans the value of a tagged ASCII field from a buffer (NIST {@code scan_ANSI_NIST_tagged_field}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param field   field to populate
		 * @param a       field tag string ({@code field_id})
		 * @param b       record type ({@code record_type})
		 * @param c       field number ({@code field_int})
		 * @param d       number of bytes remaining in the record ({@code record_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTTaggedField(BasicDataBuffer an2kBDB, Field field, String a, final int b, final int c,
				int d);

		/**
		 * Scans and parses a field tag from a buffer (NIST {@code scan_ANSI_NIST_field_ID}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param a       output field tag string ({@code ofield_id})
		 * @param b       output record type ({@code orecord_type})
		 * @param c       output field number ({@code ofield_int})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTFieldID(BasicDataBuffer an2kBDB, String a, int b, int c);

		/**
		 * Scans a subfield from a buffer (NIST {@code scan_ANSI_NIST_subfield}).
		 *
		 * @param an2kBDB  input buffer cursor
		 * @param subField subfield to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTSubfield(BasicDataBuffer an2kBDB, SubField subField);

		/**
		 * Scans an item from a buffer (NIST {@code scan_ANSI_NIST_item}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param item    item to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTItem(BasicDataBuffer an2kBDB, Item item);

		/**
		 * Scans a binary image record (Type-3..6) from a buffer (NIST {@code scan_ANSI_NIST_binary_image_record}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTBinaryImageRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		/**
		 * Scans a binary signature record (Type-8) from a buffer (NIST {@code scan_ANSI_NIST_binary_signature_record}).
		 *
		 * @param an2kBDB    input buffer cursor
		 * @param recordInfo record to populate
		 * @param a          record type ({@code record_type})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTBinarySignatureRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		/**
		 * Scans a fixed-length binary field from a buffer (NIST {@code scan_ANSI_NIST_binary_field}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param field   field to populate
		 * @param a       number of bytes to read ({@code num_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanANSIToNISTBinaryField(BasicDataBuffer an2kBDB, Field field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT WRITE ROUTINES */
		/**
		 * Writes an ANSI/NIST structure to a file (NIST {@code write_ANSI_NIST_file}).
		 *
		 * @param a        output file name ({@code ofile})
		 * @param ansiNist structure to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTFile(final String a, final AnsiNist ansiNist);

		/**
		 * Writes an ANSI/NIST structure to an open file (NIST {@code write_ANSI_NIST}).
		 *
		 * @param file     output file
		 * @param ansiNist structure to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNIST(File file, final AnsiNist ansiNist);

		/**
		 * Writes one record (NIST {@code write_ANSI_NIST_record}).
		 *
		 * @param file       output file
		 * @param recordInfo record to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTRecord(File file, Record recordInfo);

		/**
		 * Writes a tagged field (NIST {@code write_ANSI_NIST_tagged_field}).
		 *
		 * @param file  output file
		 * @param field field to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTTaggedField(File file, final Field field);

		/**
		 * Writes a tagged subfield (NIST {@code write_ANSI_NIST_tagged_subfield}).
		 *
		 * @param file     output file
		 * @param subField subfield to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTTaggedSubfield(File file, final SubField subField);

		/**
		 * Writes a tagged item (NIST {@code write_ANSI_NIST_tagged_item}).
		 *
		 * @param file output file
		 * @param item item to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTTaggedItem(File file, final Item item);

		/**
		 * Writes a separator character (NIST {@code write_ANSI_NIST_separator}).
		 *
		 * @param file output file
		 * @param a    separator character ({@code FS}, {@code GS}, {@code RS} or {@code US})
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTSeparator(File file, final char a);

		/**
		 * Writes a binary field (NIST {@code write_ANSI_NIST_binary_field}).
		 *
		 * @param file  output file
		 * @param field field to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTBinaryField(File file, final Field field);

		/**
		 * Writes a binary subfield (NIST {@code write_ANSI_NIST_binary_subfield}).
		 *
		 * @param file     output file
		 * @param subField subfield to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTBinarySubfield(File file, final SubField subField);

		/**
		 * Writes a binary item (NIST {@code write_ANSI_NIST_binary_item}).
		 *
		 * @param file output file
		 * @param item item to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeANSIToNISTBinaryItem(File file, final Item item);
	}

	/** Formatted-text read and write routines (port of NIST's {@code fmttext.c}). */
	public interface IFmtText {
		/**
		 * Reads an ANSI/NIST structure from a formatted text file (NIST {@code read_fmttext_file}).
		 *
		 * @param fileName input file name
		 * @param ansiNist structure to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int readFormatTextFile(String fileName, AnsiNist ansiNist);

		/**
		 * Reads an ANSI/NIST structure from an open formatted text file (NIST {@code read_fmttext}).
		 *
		 * @param file     input file
		 * @param ansiNist structure to populate
		 * @return {@code 0} on success, negative on error
		 */
		public int readFormatText(File file, AnsiNist ansiNist);

		/**
		 * Reads one item line from a formatted text file (NIST {@code read_fmttext_item}).
		 *
		 * @param file input file
		 * @param a    output record index ({@code orecord_i})
		 * @param b    output field index ({@code ofield_i})
		 * @param c    output subfield index ({@code osubfield_i})
		 * @param d    output item index ({@code oitem_i})
		 * @param e    output record type ({@code orecord_type})
		 * @param f    output field number ({@code ofield_int})
		 * @param g    output item value ({@code ovalue})
		 * @return {@code 0} on success, {@code EOF} at end of file, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int readFormatTextItem(File file, int a, int b, int c, int d, int e, int f, String g);

		/**
		 * Writes an ANSI/NIST structure as a formatted text file (NIST {@code write_fmttext_file}).
		 *
		 * @param fileName output file name
		 * @param ansiNist structure to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextFile(String fileName, AnsiNist ansiNist);

		/**
		 * Writes an ANSI/NIST structure as formatted text to an open file (NIST {@code write_fmttext}).
		 *
		 * @param file     output file
		 * @param ansiNist structure to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatText(File file, final AnsiNist ansiNist);

		/**
		 * Writes one record as formatted text (NIST {@code write_fmttext_record}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param ansiNist structure containing the record
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextRecord(File file, final int a, final AnsiNist ansiNist);

		/**
		 * Writes one field as formatted text (NIST {@code write_fmttext_field}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param ansiNist structure containing the field
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextField(File file, final int a, final int b, final AnsiNist ansiNist);

		/**
		 * Writes an image field as formatted text, storing the image data in a separate file (NIST
		 * {@code write_fmttext_image_field}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param ansiNist structure containing the field
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextImageField(File file, final int a, final int b, final AnsiNist ansiNist);

		/**
		 * Writes one subfield as formatted text (NIST {@code write_fmttext_subfield}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param ansiNist structure containing the subfield
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextSubfield(File file, final int a, final int b, final int c, final AnsiNist ansiNist);

		/**
		 * Writes one item as formatted text (NIST {@code write_fmttext_item}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        item index ({@code item_i})
		 * @param ansiNist structure containing the item
		 * @return {@code 0} on success, negative on error
		 */
		public int writeFormatTextItem(File file, final int a, final int b, final int c, final int d,
				final AnsiNist ansiNist);
	}

	/** Routines that locate and return image data (port of NIST's {@code getimg.c}). */
	public interface IGetImg {
		/**
		 * Locates and decodes the first grayscale fingerprint image in a file (NIST {@code get_first_grayprint}).
		 *
		 * @param data       output decoded pixel data
		 * @param a          output image width in pixels ({@code ow})
		 * @param b          output image height in pixels ({@code oh})
		 * @param c          output pixel depth in bits ({@code od})
		 * @param d          output resolution in pixels per millimeter ({@code oppmm})
		 * @param e          output lossy-compression flag ({@code olossyflag})
		 * @param f          additional output of this port (no direct NIST counterpart)
		 * @param recordInfo output image record
		 * @param g          output index of the image record ({@code oimgrecord_i})
		 * @param ansiNist   ANSI/NIST structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int getFirstGrayprint(byte[] data, int a, int b, int c, double d, int e, int f, Record recordInfo, int g,
				final AnsiNist ansiNist);
	}

	/** Insert routines (port of NIST's {@code insert.c}). */
	public interface IInsert {
		/**
		 * Inserts a value at the selected position and writes the result (NIST {@code do_insert}).
		 *
		 * @param a        output file name ({@code ofile})
		 * @param b        record index ({@code record_i})
		 * @param c        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param d        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param e        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param f        new value ({@code newvalue})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int doInsert(final String a, final int b, final int c, final int d, final int e, final String f,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts a record, field, subfield or item selected by the given indices (NIST {@code insert_ANSI_NIST_select}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param c        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param d        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param e        new value ({@code newvalue})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTSelect(final int a, final int b, final int c, final int d, final String e,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts a record read from a formatted text file (NIST {@code insert_ANSI_NIST_record}).
		 *
		 * @param a        insertion record index ({@code record_i})
		 * @param b        name of the file containing the record ({@code fmttext_file})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTRecord(final int a, final String b, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts an in-memory record (NIST {@code insert_ANSI_NIST_record_frmem}).
		 *
		 * @param a          insertion record index ({@code record_i})
		 * @param recordInfo record to insert
		 * @param ansiNist   ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTRecordFrmem(final int a, Record recordInfo, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Core record insertion shared by the other insert routines (NIST {@code insert_ANSI_NIST_record_core}).
		 *
		 * @param a          insertion record index ({@code record_i})
		 * @param recordInfo record to insert
		 * @param b          control flag of the core insertion (e.g. whether the Type-1 CNT field is adjusted)
		 * @param ansiNist   ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTRecordCore(final int a, Record recordInfo, final int b,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts a field whose value is parsed from a string (NIST {@code insert_ANSI_NIST_field}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        insertion field index ({@code field_i})
		 * @param c        field value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTField(final int a, final int b, String c, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts an in-memory field (NIST {@code insert_ANSI_NIST_field_frmem}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        insertion field index ({@code field_i})
		 * @param field    field to insert
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTFieldFrmem(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Core field insertion shared by the other insert routines (NIST {@code insert_ANSI_NIST_field_core}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        insertion field index ({@code field_i})
		 * @param field    field to insert
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTFieldCore(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Adjusts the Type-1 CNT field and IDCs after a record insertion (NIST {@code adjust_insrec_CNT_IDCs}).
		 *
		 * @param a        index of the inserted record ({@code record_i})
		 * @param b        IDC / record type of the inserted record
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int adjustInsrecCNTIDCs(final int a, final int b, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts a subfield whose value is parsed from a string (NIST {@code insert_ANSI_NIST_subfield}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        insertion subfield index ({@code subfield_i})
		 * @param d        subfield value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTSubfield(final int a, final int b, final int c, final String d,
				AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts an in-memory subfield (NIST {@code insert_ANSI_NIST_subfield_frmem}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        insertion subfield index ({@code subfield_i})
		 * @param subField subfield to insert
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTSubfieldFrmem(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Core subfield insertion shared by the other insert routines (NIST {@code insert_ANSI_NIST_subfield_core}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        insertion subfield index ({@code subfield_i})
		 * @param subField subfield to insert
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTSubfieldCore(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);

		/**
		 * Inserts an item (NIST {@code insert_ANSI_NIST_item}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        insertion item index ({@code item_i})
		 * @param e        item value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int insertANSIToNISTItem(final int a, final int b, final int c, final int d, final String e,
				AtomicReference<AnsiNist> ansiNist);
	}

	/** AN2K format tests (port of NIST's {@code is_an2k.c}). */
	public interface IIsAn2k {
		/**
		 * Determines whether a file is in ANSI/NIST format (NIST {@code is_ANSI_NIST_file}).
		 *
		 * @param fileName file to test
		 * @return {@code TRUE} if ANSI/NIST, {@code FALSE} if not, negative on error
		 */
		public int isANSIToNISTFile(final String fileName);

		/**
		 * Determines whether a byte buffer holds ANSI/NIST data (NIST {@code is_ANSI_NIST}).
		 *
		 * @param idata data to test
		 * @param ilen  number of bytes
		 * @return {@code TRUE} if ANSI/NIST, {@code FALSE} if not, negative on error
		 */
		public int isANSIToNIST(byte[] idata, final int ilen);
	}

	/** Lookup routines (port of NIST's {@code lookup.c}). */
	public interface ILookUp {
		/**
		 * Looks up a field by number in a record (NIST {@code lookup_ANSI_NIST_field}).
		 *
		 * @param ofield     output field
		 * @param fieldI     output field index
		 * @param field      field number to look up
		 * @param recordInfo record to search
		 * @return {@code TRUE} if found, {@code FALSE} otherwise
		 */
		public int lookupANSIToNISTField(AtomicReference<Field> ofield, AtomicInteger fieldI, final int field,
				final Record recordInfo);

		/**
		 * Looks up a subfield by index in a field (NIST {@code lookup_ANSI_NIST_subfield}).
		 *
		 * @param osubfield      output subfield
		 * @param subfield_index subfield index
		 * @param field          field to search
		 * @return {@code TRUE} if found, {@code FALSE} otherwise
		 */
		public int lookupANSIToNISTSubfield(AtomicReference<SubField> osubfield, final int subfield_index,
				final Field field);

		/**
		 * Looks up an item by index in a subfield (NIST {@code lookup_ANSI_NIST_item}).
		 *
		 * @param item       output item
		 * @param item_index item index
		 * @param subfield   subfield to search
		 * @return {@code TRUE} if found, {@code FALSE} otherwise
		 */
		public int lookupANSIToNISTItem(AtomicReference<Item> item, final int item_index, final SubField subfield);

		/**
		 * Finds the next image record at or after a starting record (NIST {@code lookup_ANSI_NIST_image}).
		 *
		 * @param oimgrecord  output image record
		 * @param imgrecordI  output image record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupANSIToNISTImage(AtomicReference<Record> oimgrecord, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Determines the resolution of an image record in pixels per millimeter (NIST
		 * {@code lookup_ANSI_NIST_image_ppmm}).
		 *
		 * @param oppmm       output resolution in pixels per millimeter
		 * @param ansiNist    ANSI/NIST structure
		 * @param imgrecord_i index of the image record
		 * @return {@code 0} on success, negative on error
		 */
		public int lookupANSIToNISTImagePpmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist,
				final int imgrecord_i);

		/**
		 * Determines the resolution of a binary field image record in pixels per millimeter (NIST
		 * {@code lookup_binary_field_image_ppmm}).
		 *
		 * @param oppmm       output resolution in pixels per millimeter
		 * @param ansiNist    ANSI/NIST structure
		 * @param imgrecord_i index of the image record
		 * @return {@code 0} on success, negative on error
		 */
		public int lookupBinaryFieldImagePpmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist,
				final int imgrecord_i);

		/**
		 * Determines the resolution of a tagged field image record in pixels per millimeter (NIST
		 * {@code lookup_tagged_field_image_ppmm}).
		 *
		 * @param oppmm      output resolution in pixels per millimeter
		 * @param recordInfo image record
		 * @return {@code 0} on success, negative on error
		 */
		public int lookupTaggedFieldImagePpmm(AtomicReference<Double> oppmm, final Record recordInfo);

		/**
		 * Finds the next fingerprint image record (NIST {@code lookup_ANSI_NIST_fingerprint}).
		 *
		 * @param recordInfo  output image record
		 * @param imgrecordI  output image record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupANSIToNISTFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Finds the next grayscale fingerprint image record (NIST {@code lookup_ANSI_NIST_grayprint}).
		 *
		 * @param recordInfo  output image record
		 * @param imgrecordI  output image record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupANSIToNISTGrayprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Finds the next binary field fingerprint image record (NIST {@code lookup_binary_field_fingerprint}).
		 *
		 * @param recordInfo  output image record
		 * @param imgrecordI  output image record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupBinaryFieldFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Finds the next tagged field fingerprint image record (NIST {@code lookup_tagged_field_fingerprint}).
		 *
		 * @param recordInfo  output image record
		 * @param imgrecordI  output image record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupTaggedFieldFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Finds the next fingerprint image record with a given IDC (NIST {@code lookup_fingerprint_with_IDC}).
		 *
		 * @param recordInfo  output image record
		 * @param imgrecordI  output image record index
		 * @param idc         image designation character to match
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupFingerprintWithIDC(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI, final int idc,
				final int strt_record, final AnsiNist ansiNist);

		/**
		 * Looks up the finger position (FGP) field of an image record (NIST {@code lookup_FGP_field}).
		 *
		 * @param ofield     output field
		 * @param fieldI     output field index
		 * @param recordInfo image record
		 * @return {@code TRUE} if found, {@code FALSE} otherwise
		 */
		public int lookupFGPField(AtomicReference<Field> ofield, AtomicInteger fieldI, final Record recordInfo);

		/**
		 * Looks up the impression type (IMP) field of an image record (NIST {@code lookup_IMP_field}).
		 *
		 * @param ofield     output field
		 * @param fieldI     output field index
		 * @param recordInfo image record
		 * @return {@code TRUE} if found, {@code FALSE} otherwise
		 */
		public int lookupIMPField(AtomicReference<Field> ofield, AtomicInteger fieldI, final Record recordInfo);

		/**
		 * Determines the minutiae format (standard or user-defined) of a Type-9 record (NIST
		 * {@code lookup_minutiae_format}).
		 *
		 * @param ofmt       output format string
		 * @param recordInfo Type-9 record
		 * @return {@code 0} on success, negative on error
		 */
		public int lookupMinutiaeFormat(AtomicReference<String> ofmt, final Record recordInfo);

		/**
		 * Finds the next record matching a selection criterion (NIST {@code lookup_ANSI_NIST_record}).
		 *
		 * @param oimgrecord  output matching record
		 * @param imgrecordI  output matching record index
		 * @param strt_record index of the record where the search starts
		 * @param ansiNist    structure to search
		 * @param recSel      selection criterion
		 * @return {@code TRUE} if found, {@code FALSE} if not, negative on error
		 */
		public int lookupANSIToNISTRecord(AtomicReference<Record> oimgrecord, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist, final RecordSelected recSel);
	}

	/** Print routines (port of NIST's {@code print.c}). */
	public interface IPrint {
		/**
		 * Prints the selected structure element to a file (NIST {@code do_print}).
		 *
		 * @param a        output file name ({@code ofile})
		 * @param b        record index ({@code record_i})
		 * @param c        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param d        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param e        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param ansiNist ANSI/NIST structure
		 * @return {@code 0} on success, negative on error
		 */
		public int doPrint(final String a, final int b, final int c, final int d, final int e, AnsiNist ansiNist);

		/**
		 * Prints the record, field, subfield or item selected by the given indices (NIST {@code print_ANSI_NIST_select}).
		 *
		 * @param file     output file
		 * @param a        record index ({@code record_i})
		 * @param b        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param c        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param d        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param ansiNist ANSI/NIST structure
		 * @return {@code 0} on success, negative on error
		 */
		public int printANSIToNISTSelect(File file, final int a, final int b, final int c, final int d,
				AnsiNist ansiNist);
	}

	/** General file/buffer read and scan utilities (port of NIST's {@code read.c}). */
	public interface IRead {
		/**
		 * Reads one byte from a file or a memory buffer (NIST {@code fbgetc}).
		 *
		 * @param file    input file (used when {@code an2kBDB} is {@code null})
		 * @param an2kBDB input buffer cursor, or {@code null}
		 * @return the byte read, or {@code EOF}
		 */
		public int fbgetc(File file, BasicDataBuffer an2kBDB);

		/**
		 * Reads a block of elements from a file or a memory buffer (NIST {@code fbread}).
		 *
		 * @param a       destination ({@code ptr})
		 * @param b       size of each element in bytes ({@code size})
		 * @param c       number of elements ({@code nmemb})
		 * @param file    input file (used when {@code an2kBDB} is {@code null})
		 * @param an2kBDB input buffer cursor, or {@code null}
		 * @return number of elements read
		 */
		public long fbread(long a, long b, long c, File file, BasicDataBuffer an2kBDB);

		/**
		 * Returns the current position in a file or a memory buffer (NIST {@code fbtell}).
		 *
		 * @param file    input file (used when {@code an2kBDB} is {@code null})
		 * @param an2kBDB input buffer cursor, or {@code null}
		 * @return the current position
		 */
		public long fbtell(File file, BasicDataBuffer an2kBDB);

		/**
		 * Reads binary item data (NIST {@code read_binary_item_data}).
		 *
		 * @param file input file
		 * @param data output data
		 * @param a    number of bytes to read ({@code num_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int readBinaryItemData(File file, byte[] data, final int a);

		/**
		 * Reads a big-endian unsigned 32-bit integer (NIST {@code read_binary_uint}).
		 *
		 * @param file input file
		 * @param a    output value ({@code ouint_val})
		 * @return {@code 0} on success, negative on error
		 */
		public int readBinaryUInt(File file, int[] a);

		/**
		 * Reads a big-endian unsigned 16-bit integer (NIST {@code read_binary_ushort}).
		 *
		 * @param file input file
		 * @param a    output value ({@code oushort_val})
		 * @return {@code 0} on success, negative on error
		 */
		public int readBinaryUShort(File file, short[] a);

		/**
		 * Reads an unsigned byte (NIST {@code read_binary_uchar}).
		 *
		 * @param file input file
		 * @param data output value
		 * @return {@code 0} on success, negative on error
		 */
		public int readBinaryUChar(File file, byte[] data);

		/**
		 * Reads the whole content of a binary image file (NIST {@code read_binary_image_data}).
		 *
		 * @param fileName input file name
		 * @param data     output data
		 * @param a        output length in bytes ({@code olen})
		 * @return {@code 0} on success, negative on error
		 */
		public int readBinaryImageData(String fileName, byte[] data, int a);

		/**
		 * Reads one character and verifies it matches the expected one (NIST {@code read_char}).
		 *
		 * @param file input file
		 * @param a    expected character
		 * @return {@code TRUE} if matched, {@code FALSE} otherwise, negative on error
		 */
		public int readChar(File file, final int a);

		/**
		 * Reads a string up to a delimiter (NIST {@code read_string}).
		 *
		 * @param file input file
		 * @param a    output string ({@code ostr})
		 * @param b    delimiter character
		 * @return {@code 0} on success, negative on error
		 */
		public int readString(File file, String a, final int b);

		/**
		 * Reads an integer up to a delimiter (NIST {@code read_integer}).
		 *
		 * @param file input file
		 * @param a    output integer ({@code oint})
		 * @param b    delimiter character
		 * @return {@code 0} on success, negative on error
		 */
		public int readInteger(File file, int a, final int b);

		/**
		 * Skips white space in a file (NIST {@code skip_white_space}).
		 *
		 * @param file input file
		 * @return {@code 0} on success, {@code EOF} at end of file, negative on error
		 */
		public int skipWhiteSpace(File file);

		/**
		 * Scans binary item data from a buffer (NIST {@code scan_binary_item_data}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param data    output data
		 * @param a       number of bytes to read ({@code num_bytes})
		 * @return {@code 0} on success, negative on error
		 */
		public int scanBinaryItemData(BasicDataBuffer an2kBDB, byte[] data, final int a);

		/**
		 * Scans a big-endian unsigned 32-bit integer from a buffer (NIST {@code scan_binary_uint}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param data    output value
		 * @return {@code 0} on success, negative on error
		 */
		public int scanBinaryUInt(BasicDataBuffer an2kBDB, int[] data);

		/**
		 * Scans a big-endian unsigned 16-bit integer from a buffer (NIST {@code scan_binary_ushort}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param data    output value
		 * @return {@code 0} on success, negative on error
		 */
		public int scanBinaryUShort(BasicDataBuffer an2kBDB, short[] data);

		/**
		 * Scans an unsigned byte from a buffer (NIST {@code scan_binary_uchar}).
		 *
		 * @param an2kBDB input buffer cursor
		 * @param data    output value
		 * @return {@code 0} on success, negative on error
		 */
		public int scanBinaryUChar(BasicDataBuffer an2kBDB, byte[] data);
	}

	/** Field byte-size tables for binary records (port of NIST's {@code size.c}). */
	public interface ISize {
		/**
		 * Returns the byte size of a field of a binary image record (NIST {@code binary_image_field_bytes}).
		 *
		 * @param a field number ({@code field_int})
		 * @return the field size in bytes, or negative if the field number is invalid
		 */
		public int binaryImageFieldBytes(final int a);

		/**
		 * Returns the byte size of a field of a binary signature record (NIST {@code binary_signature_field_bytes}).
		 *
		 * @param a field number ({@code field_int})
		 * @return the field size in bytes, or negative if the field number is invalid
		 */
		public int binarySignatureFieldBytes(final int a);
	}

	/** Substitute routines (port of NIST's {@code substitute.c}). */
	public interface ISubstitute {
		/**
		 * Substitutes the value at the selected position and writes the result (NIST {@code do_substitute}).
		 *
		 * @param a        output file name ({@code ofile})
		 * @param b        record index ({@code record_i})
		 * @param c        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param d        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param e        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param f        new value ({@code newvalue})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int doSubstitute(String a, final int b, final int c, final int d, final int e, String f,
				AnsiNist ansiNist);

		/**
		 * Substitutes the record, field, subfield or item selected by the given indices (NIST
		 * {@code substitute_ANSI_NIST_select}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index, or {@link IAn2k#UNSET} ({@code field_i})
		 * @param c        subfield index, or {@link IAn2k#UNSET} ({@code subfield_i})
		 * @param d        item index, or {@link IAn2k#UNSET} ({@code item_i})
		 * @param e        new value ({@code newvalue})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int substituteANSIToNISTSelect(final int a, final int b, final int c, final int d, String e,
				AnsiNist ansiNist);

		/**
		 * Replaces a record with one read from a formatted text file (NIST {@code substitute_ANSI_NIST_record}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        name of the file containing the record ({@code fmttext_file})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int substituteANSIToNISTRecord(final int a, String b, AnsiNist ansiNist);

		/**
		 * Replaces a field's value (NIST {@code substitute_ANSI_NIST_field}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        new value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int substituteANSIToNISTField(final int a, final int b, String c, AnsiNist ansiNist);

		/**
		 * Replaces a subfield's value (NIST {@code substitute_ANSI_NIST_subfield}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        new value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int substituteANSIToNISTSubfield(final int a, final int b, final int c, String d, AnsiNist ansiNist);

		/**
		 * Replaces an item's value (NIST {@code substitute_ANSI_NIST_item}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        item index ({@code item_i})
		 * @param e        new value ({@code value})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int substituteANSIToNISTItem(final int a, final int b, final int c, final int d, String e,
				AtomicReference<AnsiNist> ansiNist);
	}

	/** ANSI/NIST 2007 to FBI/IAFIS conversion routines (port of NIST's {@code to_iafis.c}). */
	public interface IToIafis {
		/**
		 * Converts all fingerprint image records to FBI/IAFIS conventions (NIST {@code nist2iafis_fingerprints}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisFingerprints(AnsiNist ansiNist);

		/**
		 * Converts one fingerprint image record to FBI/IAFIS conventions (NIST {@code nist2iafis_fingerprint}).
		 *
		 * @param fromRecord source record
		 * @param toRecord   output converted record
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisFingerprint(Record fromRecord, Record toRecord);

		/**
		 * Converts all Type-9 minutiae records to FBI/IAFIS conventions (NIST {@code nist2iafis_type_9s}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisType9s(AnsiNist ansiNist);

		/**
		 * Determines whether a Type-9 record needs conversion to FBI/IAFIS conventions (NIST {@code nist2iafis_needed}).
		 *
		 * @param recordInfo Type-9 record
		 * @return {@code TRUE} if conversion is needed, {@code FALSE} otherwise
		 */
		public int nist2iafisNeeded(Record recordInfo);

		/**
		 * Converts one Type-9 record to FBI/IAFIS conventions (NIST {@code nist2iafis_type_9}).
		 *
		 * @param recordInfo output converted record
		 * @param ansiNist   ANSI/NIST structure
		 * @param a          index of the Type-9 record ({@code record_i})
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisType9(Record recordInfo, AnsiNist ansiNist, final int a);

		/**
		 * Converts a minutiae method code to FBI/IAFIS conventions (NIST {@code nist2iafis_method}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisMethod(String a, String b);

		/**
		 * Converts a minutia type code to FBI/IAFIS conventions (NIST {@code nist2iafis_minutia_type}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisMinutiaType(String a, String b);

		/**
		 * Converts a pattern class code to FBI/IAFIS conventions (NIST {@code nist2iafis_pattern_class}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @param c pattern class index / count
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisPatternClass(String a, String b, final int c);

		/**
		 * Converts a ridge count value to FBI/IAFIS conventions (NIST {@code nist2iafis_ridgecount}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int nist2iafisRidgecount(String a, String b);
	}

	/** FBI/IAFIS to ANSI/NIST 2007 conversion routines (port of NIST's {@code to_nist.c}). */
	public interface IToNist {
		/**
		 * Converts all fingerprint image records from FBI/IAFIS conventions (NIST {@code iafis2nist_fingerprints}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistFingerprints(AnsiNist ansiNist);

		/**
		 * Converts one fingerprint image record from FBI/IAFIS conventions (NIST {@code iafis2nist_fingerprint}).
		 *
		 * @param recordInfo output converted record
		 * @param ansiNist   ANSI/NIST structure
		 * @param a          index of the record ({@code record_i})
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistFfingerprint(Record recordInfo, AtomicReference<AnsiNist> ansiNist, final AtomicInteger a);

		/**
		 * Converts all Type-9 minutiae records from FBI/IAFIS conventions (NIST {@code iafis2nist_type_9s}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistType9s(AnsiNist ansiNist);

		/**
		 * Determines whether a Type-9 record needs conversion from FBI/IAFIS conventions (NIST {@code iafis2nist_needed}).
		 *
		 * @param recordInfo Type-9 record
		 * @return {@code TRUE} if conversion is needed, {@code FALSE} otherwise
		 */
		public int iafis2nistNeeded(Record recordInfo);

		/**
		 * Converts one Type-9 record from FBI/IAFIS conventions (NIST {@code iafis2nist_type_9}).
		 *
		 * @param recordInfo output converted record
		 * @param ansiNist   ANSI/NIST structure
		 * @param a          index of the Type-9 record ({@code record_i})
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistType9(Record recordInfo, AnsiNist ansiNist, final int a);

		/**
		 * Converts a minutiae method code from FBI/IAFIS conventions (NIST {@code iafis2nist_method}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistMethod(String a, String b);

		/**
		 * Converts a minutia type code from FBI/IAFIS conventions (NIST {@code iafis2nist_minutia_type}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistMinutiaType(String a, String b);

		/**
		 * Converts a pattern class code from FBI/IAFIS conventions (NIST {@code iafis2nist_pattern_class}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @param c pattern class index / count
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistPatternClass(String a, String b, final int c);

		/**
		 * Converts a ridge count value from FBI/IAFIS conventions (NIST {@code iafis2nist_ridgecount}).
		 *
		 * @param a output value ({@code ovalue})
		 * @param b input value ({@code value})
		 * @return {@code 0} on success, negative on error
		 */
		public int iafis2nistRidgecount(String a, String b);
	}

	/** Record and field type tests (port of NIST's {@code type.c}). */
	public interface IType {
		/**
		 * Determines whether a record type is a tagged record (NIST {@code tagged_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int taggedRecord(final int a);

		/**
		 * Determines whether a record type is a binary record (NIST {@code binary_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int binaryRecord(final int a);

		/**
		 * Determines whether a record type is a tagged image record (NIST {@code tagged_image_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int taggedImageRecord(final int a);

		/**
		 * Determines whether a record type is a binary image record (NIST {@code binary_image_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int binaryImageRecord(final int a);

		/**
		 * Determines whether a record type is any image record (NIST {@code image_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int imageRecord(final int a);

		/**
		 * Determines whether a record type is a binary signature record (NIST {@code binary_signature_record}).
		 *
		 * @param a record type
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int binarySignatureRecord(final int a);

		/**
		 * Determines whether a field is an image data field (NIST {@code image_field}).
		 *
		 * @param field field to test
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int imageField(final Field field);

		/**
		 * Determines whether a character is an ANSI/NIST separator (NIST {@code is_delimiter}).
		 *
		 * @param a character to test
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int isDelimiter(final int a);

		/**
		 * Determines the hand of a finger position code (NIST {@code which_hand}).
		 *
		 * @param a finger position code
		 * @return {@link IAn2k#RIGHT_HAND}, {@link IAn2k#LEFT_HAND} or {@link IAn2k#UNKNOWN_HAND}
		 */
		public int whichHand(final int a);
	}

	/** Record selection based on extensible criteria (port of NIST's {@code select.c}). */
	public interface ISelect {
		/**
		 * Determines whether a record satisfies a selection criterion (NIST {@code select_ANSI_NIST_record}).
		 *
		 * @param recordInfo record to test
		 * @param recSel     selection criterion
		 * @return {@code TRUE} if selected, {@code FALSE} otherwise, negative on error
		 */
		public int selectANSIToNISTRecord(AtomicReference<Record> recordInfo, final RecordSelected recSel);

		/**
		 * Creates a selection criterion from a list of values (NIST {@code new_rec_sel}).
		 *
		 * @param recSel     output criterion
		 * @param recSelType criterion type
		 * @param numValues  number of values
		 * @param args       values
		 * @return {@code 0} on success, negative on error
		 */
		public int newRecSel(AtomicReference<RecordSelected> recSel, RecordSelectedType recSelType, int numValues,
				String[] args);

		/**
		 * Allocates a selection criterion (NIST {@code alloc_rec_sel}).
		 *
		 * @param retCode     output return code: {@code 0} on success, negative on error
		 * @param type        criterion type
		 * @param allocValues initial capacity of the value list
		 * @return the criterion
		 */
		public RecordSelected allocRecSel(AtomicInteger retCode, RecordSelectedType type, int allocValues);

		/**
		 * Releases a selection criterion (NIST {@code free_rec_sel}).
		 *
		 * @param recSel criterion to release
		 */
		public void freeRecSel(RecordSelected recSel);

		/**
		 * Adds a numeric value criterion to a criterion list (NIST {@code add_rec_sel_num}).
		 *
		 * @param head  in/out head of the criterion list
		 * @param type  criterion type
		 * @param value numeric value
		 * @return {@code 0} on success, negative on error
		 */
		public int addRecSelNum(AtomicReference<RecordSelected> head, final RecordSelectedType type, final int value);

		/**
		 * Adds a string value criterion to a criterion list (NIST {@code add_rec_sel_str}).
		 *
		 * @param head  in/out head of the criterion list
		 * @param type  criterion type
		 * @param value string value
		 * @return {@code 0} on success, negative on error
		 */
		public int addRecSelStr(AtomicReference<RecordSelected> head, final RecordSelectedType type,
				final String value);

		/**
		 * Adds a criterion to a criterion list (NIST {@code add_rec_sel}).
		 *
		 * @param head   in/out head of the criterion list
		 * @param newSel criterion to add
		 * @return {@code 0} on success, negative on error
		 */
		public int addRecSel(AtomicReference<RecordSelected> head, RecordSelected newSel);

		/**
		 * Parses a command-line selection option into a criterion (NIST {@code parse_rec_sel_option}).
		 *
		 * @param recSelType criterion type
		 * @param a          option string ({@code optstr})
		 * @param b          remaining unparsed text ({@code endptr})
		 * @param recSel     output criterion
		 * @param c          verbosity flag ({@code verbose})
		 * @return {@code 0} on success, negative on error
		 */
		public int parseRecSelOption(final RecordSelectedType recSelType, final String a, final String b,
				RecordSelected recSel, final int c);

		/**
		 * Writes a selection criterion to a file (NIST {@code write_rec_sel}).
		 *
		 * @param file         output file
		 * @param recSelconst criterion to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeRecSel(File file, final RecordSelected recSelconst);

		/**
		 * Writes a selection criterion to a named file (NIST {@code write_rec_sel_file}).
		 *
		 * @param a           output file name
		 * @param recSelconst criterion to write
		 * @return {@code 0} on success, negative on error
		 */
		public int writeRecSelFile(final String a, final RecordSelected recSelconst);

		/**
		 * Reads a selection criterion from a file (NIST {@code read_rec_sel}).
		 *
		 * @param file   input file
		 * @param recSel output criterion
		 * @return {@code 0} on success, negative on error
		 */
		public int readRecSel(File file, AtomicReference<RecordSelected> recSel);

		/**
		 * Reads a selection criterion from a named file (NIST {@code read_rec_sel_file}).
		 *
		 * @param inputFile input file name
		 * @param recSel    output criterion
		 * @return {@code 0} on success, negative on error
		 */
		public int readRecSelFile(String inputFile, AtomicReference<RecordSelected> recSel);

		/**
		 * Determines whether an impression type code denotes a rolled print (NIST {@code imp_is_rolled}).
		 *
		 * @param a impression type code
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int impIsRolled(final int a);

		/**
		 * Determines whether an impression type code denotes a flat (plain) print (NIST {@code imp_is_flat}).
		 *
		 * @param a impression type code
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int impIsFlat(final int a);

		/**
		 * Determines whether an impression type code denotes a live-scan capture (NIST {@code imp_is_live_scan}).
		 *
		 * @param a impression type code
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int impIsLiveScan(final int a);

		/**
		 * Determines whether an impression type code denotes a latent print (NIST {@code imp_is_latent}).
		 *
		 * @param a impression type code
		 * @return {@code TRUE} or {@code FALSE}
		 */
		public int impIsLatent(final int a);

		/**
		 * Simplifies a selection criterion (e.g. flattening nested boolean combinations) (NIST {@code simplify_rec_sel}).
		 *
		 * @param retCode output return code: {@code 0} on success, negative on error
		 * @param rs      criterion to simplify
		 * @return the simplified criterion
		 */
		public RecordSelected simplifyRecSel(AtomicInteger retCode, RecordSelected rs);
	}

	/** Type-13 and Type-14 record construction routines (port of NIST's {@code type1314.c}). */
	public interface IType1314 {
		/**
		 * Builds a tagged field image record (Type-13 or Type-14) from fingerprint image data (NIST
		 * {@code fingerprint2tagged_field_image}).
		 *
		 * @param recordInfo output record
		 * @param data       image data
		 * @param a          image width in pixels
		 * @param b          image height in pixels
		 * @param c          pixel depth in bits
		 * @param d          additional integer attribute of the port (e.g. target record type)
		 * @param e          resolution in pixels per millimeter
		 * @param f          compression type string
		 * @param g          image designation character (IDC)
		 * @param h          impression type (IMP)
		 * @param i          source agency string
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int fingerprint2taggedFieldImage(Record recordInfo, byte[] data, final int a, final int b, final int c,
				final int d, final double e, String f, final int g, final int h, String i);

		/**
		 * Builds a Type-13 record from image data (NIST {@code image2type_13}).
		 *
		 * @param recordInfo output record
		 * @param data       image data
		 * @param a          image width in pixels
		 * @param b          image height in pixels
		 * @param c          pixel depth in bits
		 * @param d          additional integer attribute of the port
		 * @param e          resolution in pixels per millimeter
		 * @param f          compression type string
		 * @param g          image designation character (IDC)
		 * @param h          impression type (IMP)
		 * @param i          source agency string
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int image2type13(Record recordInfo, byte[] data, final int a, final int b, final int c, final int d,
				final double e, String f, final int g, final int h, String i);

		/**
		 * Builds a Type-14 record from image data (NIST {@code image2type_14}).
		 *
		 * @param recordInfo output record
		 * @param data       image data
		 * @param a          image width in pixels
		 * @param b          image height in pixels
		 * @param c          pixel depth in bits
		 * @param d          additional integer attribute of the port
		 * @param e          resolution in pixels per millimeter
		 * @param f          compression type string
		 * @param g          image designation character (IDC)
		 * @param h          impression type (IMP)
		 * @param i          source agency string
		 * @return {@code 0} on success, negative on error
		 */
		@SuppressWarnings({ "java:S107" })
		public int image2type14(Record recordInfo, byte[] data, final int a, final int b, final int c, final int d,
				final double e, String f, final int g, final int h, String i);
	}

	/** Update routines maintaining byte counts and LEN fields (port of NIST's {@code update.c}). */
	public interface IAn2kUpdate {
		/**
		 * Appends a record to an ANSI/NIST structure, updating byte counts (NIST {@code update_ANSI_NIST}).
		 *
		 * @param ansiNist   structure to extend
		 * @param recordInfo record to append
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNIST(AnsiNist ansiNist, Record recordInfo);

		/**
		 * Appends a field to a record, updating byte counts (NIST {@code update_ANSI_NIST_record}).
		 *
		 * @param recordInfo record to extend
		 * @param field      field to append
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTRecord(Record recordInfo, Field field);

		/**
		 * Appends a subfield to a field, updating byte counts (NIST {@code update_ANSI_NIST_field}).
		 *
		 * @param field    field to extend
		 * @param subField subfield to append
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTField(Field field, SubField subField);

		/**
		 * Appends an item to a subfield, updating byte counts (NIST {@code update_ANSI_NIST_subfield}).
		 *
		 * @param subField subfield to extend
		 * @param item     item to append
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTSubfield(SubField subField, Item item);

		/**
		 * Appends a character to an item's value (NIST {@code update_ANSI_NIST_item}).
		 *
		 * @param item item to extend
		 * @param a    character to append
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTItem(Item item, final int a);

		/**
		 * Recomputes the LEN field of every record (NIST {@code update_ANSI_NIST_record_LENs}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTRecordLENs(AnsiNist ansiNist);

		/**
		 * Recomputes the LEN field of one record (NIST {@code update_ANSI_NIST_record_LEN}).
		 *
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @param a        record index ({@code record_i})
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTRecordLEN(AnsiNist ansiNist, final int a);

		/**
		 * Recomputes the LEN field of a binary record (NIST {@code update_ANSI_NIST_binary_record_LEN}).
		 *
		 * @param recordInfo record, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTBinaryRecordLEN(Record recordInfo);

		/**
		 * Recomputes the LEN field of a tagged record (NIST {@code update_ANSI_NIST_tagged_record_LEN}).
		 *
		 * @param recordInfo record, modified in place
		 * @return {@code 0} on success, negative on error
		 */
		public int updateANSIToNISTTaggedRecordLEN(Record recordInfo);

		/**
		 * Sets the tag of a field from a record type and field number (NIST {@code update_ANSI_NIST_field_ID}).
		 *
		 * @param field field, modified in place
		 * @param a     record type ({@code record_type})
		 * @param b     field number ({@code field_int})
		 */
		public void updateANSIToNISTFieldID(Field field, final int a, final int b);
	}

	/** Utility routines (port of NIST's {@code util.c}). */
	public interface IAn2kUtil {
		/**
		 * Increments a numeric item value (NIST {@code increment_numeric_item}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        item index ({@code item_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @param e        format string for the new value ({@code fmt})
		 * @return {@code 0} on success, negative on error
		 */
		public int incrementNumericItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist,
				String e);

		/**
		 * Decrements a numeric item value (NIST {@code decrement_numeric_item}).
		 *
		 * @param a        record index ({@code record_i})
		 * @param b        field index ({@code field_i})
		 * @param c        subfield index ({@code subfield_i})
		 * @param d        item index ({@code item_i})
		 * @param ansiNist ANSI/NIST structure, modified in place
		 * @param e        format string for the new value ({@code fmt})
		 * @return {@code 0} on success, negative on error
		 */
		public int decrementNumericItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist,
				String e);
	}

	/** String-to-structure routines (port of NIST's {@code value2.c}). */
	public interface IValue2 {
		/**
		 * Builds a field from a string value (NIST {@code value2field}).
		 *
		 * @param field field to populate
		 * @param a     record type ({@code record_type})
		 * @param b     field number ({@code field_int})
		 * @param c     value string, with subfields/items separated by RS/US
		 * @return {@code 0} on success, negative on error
		 */
		public int value2field(Field field, final int a, final int b, final String c);

		/**
		 * Builds a subfield from a string value (NIST {@code value2subfield}).
		 *
		 * @param subField subfield to populate
		 * @param a        value string, with items separated by US
		 * @return {@code 0} on success, negative on error
		 */
		public int value2subfield(SubField subField, final String a);

		/**
		 * Builds an item from a string value (NIST {@code value2item}).
		 *
		 * @param item item to populate
		 * @param a    value string
		 * @return {@code 0} on success, negative on error
		 */
		public int value2item(Item item, final String a);
	}
}