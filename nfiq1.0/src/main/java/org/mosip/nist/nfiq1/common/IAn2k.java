package org.mosip.nist.nfiq1.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

@SuppressWarnings({ "java:S125" })
public interface IAn2k {
	public static final int TRUE = 1;
	public static final int FALSE = 0;

	public static final int ANSI_NIST_CHUNK = 100;
	public static final int UNSET = -1;
	public static final int DONE = 2;
	public static final int MORE = 3;
	public static final int UNDEFINED_INT = -1;
	public static final int IGNORE = 2;

	public static final int TYPE_1_ID = 1;
	public static final int TYPE_1_NUM_MANDATORY_FIELDS = 9;
	public static final int TYPE_2_ID = 2;
	public static final int TYPE_3_ID = 3;
	public static final int TYPE_4_ID = 4;
	public static final int TYPE_5_ID = 5;
	public static final int TYPE_6_ID = 6;
	public static final int TYPE_7_ID = 7;
	public static final int TYPE_8_ID = 8;
	public static final int TYPE_9_ID = 9;
	public static final int TYPE_10_ID = 10;
	public static final int TYPE_11_ID = 11;
	public static final int TYPE_12_ID = 12;
	public static final int TYPE_13_ID = 13;
	public static final int TYPE_14_ID = 14;
	public static final int TYPE_15_ID = 15;
	public static final int TYPE_16_ID = 16;
	public static final int TYPE_17_ID = 17;
	public static final int TYPE_99_ID = 99;

	public static final int LEN_ID = 1;
	public static final int VER_ID = 2;
	public static final int CNT_ID = 3;
	public static final int TOT_ID = 4;
	public static final int DAT_ID = 5;
	public static final int PRY_ID = 6;
	public static final int DAI_ID = 7;
	public static final int ORI_ID = 8;
	public static final int TCN_ID = 9;
	public static final int TCR_ID = 10;
	public static final int NSR_ID = 11;
	public static final int NTR_ID = 12;
	public static final int DOM_ID = 13;
	public static final int GMT_ID = 14;
	public static final int DCS_ID = 15;

	public static final String IDC_FMT = "%02d";
	public static final String FLD_FMT = "%d.%03d:";
	public static final int ASCII_CSID = 0;

	public static final int VERSION_0200 = 200;
	public static final int VERSION_0201 = 201;
	public static final int VERSION_0300 = 300;
	public static final int VERSION_0400 = 400;

	public static final int FS_CHAR = 0x1C;
	public static final int GS_CHAR = 0x1D;
	public static final int RS_CHAR = 0x1E;
	public static final int US_CHAR = 0x1F;

	public static List<Integer> TAGGED_RECORDS = null;
	public static final int NUM_TAGGED_RECORDS = 10;

	public static List<Integer> BINARY_RECORDS = null;
	public static final int NUM_BINARY_RECORDS = 6;

	public static List<Integer> TAGGED_IMAGE_RECORDS = null;
	public static final int NUM_TAGGED_IMAGE_RECORDS = 7;
	public static final int IMAGE_FIELD = 999;

	public static List<Integer> BINARY_IMAGE_RECORDS = null;
	public static final int NUM_BINARY_IMAGE_RECORDS = 5;
	public static final int BINARY_LEN_BYTES = 4;
	public static final int BINARY_IDC_BYTES = 1;
	public static final int BINARY_IMP_BYTES = 1;
	public static final int BINARY_FGP_BYTES = 6;
	public static final int BINARY_ISR_BYTES = 1;
	public static final int BINARY_HLL_BYTES = 2;
	public static final int BINARY_VLL_BYTES = 2;
	public static final int BINARY_CA_BYTES = 1;
	public static final int NUM_BINARY_IMAGE_FIELDS = 9;

	/* Type-3,4,5,6 Field IDs */
	// public static final int LEN_ID = 1;
	public static final int IDC_ID = 2;
	public static final int IMP_ID = 3;
	public static final int FGP_ID = 4;
	public static final int ISR_ID = 5;
	public static final int HLL_ID = 6;
	public static final int VLL_ID = 7;
	public static final int BIN_CA_ID = 8;
	public static final int BIN_IMAGE_ID = 9;

	public static List<Integer> BINAR_SIGNATURE_RECORDS = null;
	public static final int NUM_BINARY_SIGNATURE_RECORDS = 1;
	public static final int BINARY_SIG_BYTES = 1;
	public static final int BINARY_SRT_BYTES = 1;
	public static final int NUM_BINARY_SIGNATURE_FIELDS = 8;

	/* Type-8 Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	public static final int SIG_ID = 3;
	public static final int SRT_ID = 4;
	// public static final int ISR_ID = 5;
	// public static final int HLL_ID = 6;
	// public static final int VLL_ID = 7;

	/* Type-10,13,14,15,16 Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	// public static final int IMP_ID = 3;
	public static final int SRC_ID = 4;
	public static final int CD_ID = 5;
	// public static final int HLL_ID = 6;
	// public static final int VLL_ID = 7;
	public static final int SLC_ID = 8;
	public static final int HPS_ID = 9;
	public static final int VPS_ID = 10;
	public static final int TAG_CA_ID = 11;
	public static final int CSP_ID = 12;
	public static final int CSP_ID_TYPE_17 = 13;
	public static final int BPX_ID = 12;
	public static final int FGP3_ID = 13;
	public static final String DAT2_ID = "IMAGE_FIELD";

	/* Type-10 field IDs, in addition the the common subset above... jck */
	public static final int IMT_ID = 3;
	public static final int PHD_ID = 5;
	/* 6 HLL, 7 VLL, 8 SLC, 9 HPS, 10 VPS, 11 CGA (TAG_CA_ID), 12CSP */
	public static final int SAP_ID = 13;
	/* 14 and 15 are reserved */
	/* 16 SHPS, 17 SVPS */
	/* 18 and 19 are reserved */
	public static final int POS_ID = 20;
	public static final int POA_ID = 21;
	public static final int PXS_ID = 22;
	public static final int PAS_ID = 23;
	public static final int SQS_ID = 24;
	public static final int SPA_ID = 25;
	public static final int SXS_ID = 26;
	public static final int SEC_ID = 27;
	public static final int SHC_ID = 28;
	public static final int FFP_ID = 29;
	public static final int DMM_ID = 30;
	/* 31 through 39 are reserved */
	public static final int SMT_ID = 40;
	public static final int SMS_ID = 41;
	public static final int SMD_ID = 42;
	public static final int COL_ID = 43;
	/* 44 through 199 reserved */

	/* Type-13,14,15 field IDs, in addition to the common subset above... jck */
	/* Type-13,14 respecively, reserved in type 15... */
	public static final int SPD_ID = 14;
	public static final int PPD_ID = 14;

	/* Type-13,14, reserved in type 15... */
	public static final int PPC_ID = 15;

	/* Type-13,14,15... */
	public static final int SHPS_ID = 16;
	public static final int SVPS_ID = 17;

	/* Type-14 only, reserved in Type-13,15... */
	public static final int AMP_ID = 18;

	/* 19 is reserved in Type-13,14,15. */
	/* Type-13,14,15... */
	public static final int COM_ID = 20;

	/* Type-14 only, reserved in Type-13,15 */
	public static final int SEG_ID = 21;
	public static final int NQM_ID = 22;
	public static final int SQM_ID = 23;

	/* Type-13,14,15 respecively... */
	public static final int LQM_ID = 24;
	public static final int FQM_ID = 24;
	public static final int PQM_ID = 24;

	/* Type-14 only, reserved in Type-13,15... */
	public static final int ASEG_ID = 25;

	/* 26 through 29 are reserved in Type-13,14,15. */

	/* Type-14,15, reserved in Type-13... */
	// public static final int DMM_ID = 30;
	/* End of Type-13,14,15 field IDs. */

	/* Type-9 Standard Field IDs */
	// public static final int LEN_ID = 1;
	// public static final int IDC_ID = 2;
	// public static final int IMP_ID = 3;
	public static final int FMT_ID = 4;
	public static final int OFR_ID = 5;
	public static final int FGP2_ID = 6;
	public static final int FPC_ID = 7;
	public static final int CRP_ID = 8;
	public static final int DLT_ID = 9;
	public static final int MIN_ID = 10;
	public static final int RDG_ID = 11;
	public static final int MRC_ID = 12;
	/* Type-9 FBI/IAFIS Field IDs */
	/* EFTS Field 13 Non-standard! */
	public static final int FGN_ID = 14;
	public static final int NMN_ID = 15;
	public static final int FCP_ID = 16;
	public static final int APC_ID = 17;
	public static final int ROV_ID = 18;
	public static final int COF_ID = 19;
	public static final int ORN_ID = 20;
	public static final int CRA_ID = 21;
	public static final int DLA_ID = 22;
	public static final int MAT_ID = 23;

	/* Maximum number of minutiae in an FBI/IAFIS Type-9 recordInfo. */
	public static final int MAX_IAFIS_MINUTIAE = 254;
	/* Maximum number of pattern classes in an FBI/IAFIS Type-9 recordInfo. */
	public static final int MAX_IAFIS_PATTERN_CLASSES = 3;
	/* Maximum number of cores in an FBI/IAFIS Type-9 recordInfo. */
	public static final int MAX_IAFIS_CORES = 2;
	/* Maximum number of deltas in an FBI/IAFIS Type-9 recordInfo. */
	public static final int MAX_IAFIS_DELTAS = 2;
	/* Maximum number of items in FBI/IAFIS minutia subfield. */
	public static final int MAX_IAFIS_MINUTIA_ITEMS = 13;
	/* Number of characters in an FBI/IAFIS method string. */
	public static final int IAFIS_METHOD_STRLEN = 3;

	/* Minimum Table 5 Impression Code. */
	public static final int MIN_TABLE_5_CODE = 0;
	/* Maximum Table 5 Impression Code. */
	public static final int MAX_TABLE_5_CODE = 29;

	/* Minimum Table 6 Finger Position Code. */
	public static final int MIN_TABLE_6_CODE = 0;
	/* Maximum Table 6 Finger Position Code. */
	public static final int MAX_TABLE_6_CODE = 16;

	/* Minimum Table 19 Palm Code. */
	public static final int MIN_TABLE_19_CODE = 20;
	/* Maximum Table 19 Palm Code. */
	public static final int MAX_TABLE_19_CODE = 30;

	/* Minimum Minutia Quality value. */
	public static final int MIN_QUALITY_VALUE = 0;
	/* Maximum Minutia Quality value. */
	public static final int MAX_QUALITY_VALUE = 63;

	/* Minimum scanning resolution in pixels/mm (500 dpi). */
	public static final double MIN_RESOLUTION = 19.69;
	/* Minimum scanning resolution in as stored in tagged field images. */
	public static final double MIN_TAGGED_RESOLUTION = 19.7;
	/* Scan resolution tolerance in mm's. */
	public static final double MM_TOLERANCE = 0.2;

	public static final int FIELD_NUM_LEN = 9;
	public static final char ITEM_START = '=';
	public static final int ITEM_END = US_CHAR;

	public static final String STD_STR = "S";
	public static final String USER_STR = "U";
	public static final String TBL_STR = "T";
	public static final String AUTO_STR = "A";
	public static final String PPI_STR = "1";
	public static final String PP_CM = "2";

	public static final char DEL_OP = 'd';
	public static final char INS_OP = 'i';
	public static final char PRN_OP = 'p';
	public static final char SUB_OP = 's';

	// #define DEFAULT_FPOUT stdout

	public static final int MAX_UINT_CHARS = 10;
	public static final int MAX_USHORT_CHARS = 5;
	public static final int MAX_UCHAR_CHARS = 3;

	public static final String UNUSED_STR = "255";

	public static final double MM_PER_INCH = 25.4;

	public static final int UNKNOWN_HAND = 0;
	public static final int RIGHT_HAND = 1;
	public static final int LEFT_HAND = 2;

	public static final String COMP_NONE = "NONE";
	public static final String BIN_COMP_NONE = "0";
	public static final String COMP_WSQ = "WSQ20";
	public static final String BIN_COMP_WSQ = "1";
	public static final String COMP_JPEGB = "JPEGB";
	public static final String BIN_COMP_JPEGB = "2";
	public static final String COMP_JPEGL = "JPEGL";
	public static final String BIN_COMP_JPEGL = "3";
	public static final String COMP_JPEG2K = "JP2";
	public static final String BIN_COMP_JPEG2K = "4";
	public static final String COMP_JPEG2KL = "JP2L";
	public static final String BIN_COMP_JPEG2KL = "5";
	public static final String COMP_PNG = "PNG";
	public static final String BIN_COMP_PNG = "6";
	public static final String CSP_GRAY = "GRAY";
	public static final String CSP_RGB = "RGB";
	public static final String CSP_YCC = "YCC";
	public static final String CSP_SRGB = "SRGB";
	public static final String CSP_SYCC = "SYCC";

	public static final short UCHAR_MAX = 255;

	@SuppressWarnings("unused")
	public class AnsiNist {
		private int version;
		private int numOfBytes;
		private int numOfRecords;
		private int allocRecords;
		private AtomicReferenceArray<Record> records;

		public AnsiNist() {
			super();
		}

		public AnsiNist(int version, int numOfBytes, int numOfRecords, int allocRecords,
				AtomicReferenceArray<Record> records) {
			this();
			this.version = version;
			this.numOfBytes = numOfBytes;
			this.numOfRecords = numOfRecords;
			this.allocRecords = allocRecords;
			this.records = records;
		}

		public AnsiNist(AnsiNist ansiNist) {
			this.version = ansiNist.version;
			this.numOfBytes = ansiNist.numOfBytes;
			this.numOfRecords = ansiNist.numOfRecords;
			this.allocRecords = ansiNist.allocRecords;
			this.records = ansiNist.records;
		}
	}

	@SuppressWarnings("unused")
	public class BasicDataBuffer {
		private int bdbSize; // Max size of the buffer
		private int bdbStart; // Beginning read/write location
		private int bdbEnd; // End read/write location
		private int bdbCurrent; // Current read/write location

		public BasicDataBuffer(int position, int bdbSize) {
			super();
			this.bdbSize = bdbSize;
			this.bdbCurrent = position;
			this.bdbStart = this.bdbCurrent;
			this.bdbEnd = position + bdbSize;
		}
		
		public BasicDataBuffer(BasicDataBuffer basicDataBuffer) {
			this.bdbSize = basicDataBuffer.bdbSize;
			this.bdbCurrent = basicDataBuffer.bdbCurrent;
			this.bdbStart = basicDataBuffer.bdbStart;
			this.bdbEnd = basicDataBuffer.bdbEnd;
		}
	}

	@SuppressWarnings("unused")
	public class Field {
		private String id;
		private int recordType;
		private int fieldInfo;
		private int numOfBytes;
		private int numOfSubfields;
		private int allocSubfields;
		private AtomicReferenceArray<SubField> subfields;
		private int gsChar;

		public Field() {
			super();
		}

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

	@SuppressWarnings("unused")
	public class Item {
		private int numOfBytes; // Always contains the current byte size of the entire
		/* item including any trailing US separator. */
		private int numOfChars; // Number of characters currently in value, NOT
		/* including the NULL terminator. */
		private int allocChars; // Number of allocated characters for the value,
		/* including the NULL terminator. */
		private byte value; // Must keep NULL terminated.
		private int usChar;

		public Item() {
			super();
		}

		public Item(int numOfBytes, int numOfChars, int allocChars, byte value, int usChar) {
			this();
			this.numOfBytes = numOfBytes;
			this.numOfChars = numOfChars;
			this.allocChars = allocChars;
			this.value = value;
			this.usChar = usChar;
		}

		public Item(Item itemInfo) {
			this.numOfBytes = itemInfo.numOfBytes;
			this.numOfChars = itemInfo.numOfChars;
			this.allocChars = itemInfo.allocChars;
			this.value = itemInfo.value;
			this.usChar = itemInfo.usChar;
		}
	}

	@SuppressWarnings("unused")
	public class Polygon {
		private int fgp;
		private int numOfPoints;
		private byte x;
		private byte y;
	}

	@SuppressWarnings("unused")
	public class Record {
		private int type;
		private int totalBytes;
		private int numOfBytes;
		private int numOfFields;
		private int allocFields;
		private AtomicReferenceArray<Field> fields;
		private int fsChar;

		public Record() {
			super();
		}

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

	@SuppressWarnings("unused")
	public class RecordSelected {
		private RecordSelectedType type;
		private int allocValues;
		private int numOfValues;
		private RecordSelectedValue value;

		public RecordSelected() {
			super();
		}

		public RecordSelected(RecordSelectedType type, int allocValues, int numOfValues, RecordSelectedValue value) {
			this();
			this.type = type;
			this.allocValues = allocValues;
			this.numOfValues = numOfValues;
			this.value = value;
		}

		public RecordSelected(RecordSelected recordSelectedInfo) {
			this.type = recordSelectedInfo.type;
			this.allocValues = recordSelectedInfo.allocValues;
			this.numOfValues = recordSelectedInfo.numOfValues;
			this.value = recordSelectedInfo.value;
		}
	}

	public enum RecordSelectedType {
		RS_AND(1000), RS_OR(1001), RS_LRT(1002), // logical recordInfo type
		RS_FGPLP(1003), // finger or palm position
		RS_FGP(1004), // finger position
		RS_PLP(1005), // palm position
		RS_IMP(1006), // impression type
		RS_IDC(1007), // image descriptor chararacter
		RS_NQM(1008), // NIST quality metric
		RS_IMT(1009), RS_POS(1010); // subject pose

		public static final int SIZE = java.lang.Integer.SIZE;

		private int intValue;
		private static java.util.HashMap<Integer, RecordSelectedType> mappings;

		private static java.util.HashMap<Integer, RecordSelectedType> getMappings() {
			if (mappings == null) {
				mappings = new java.util.HashMap<>();
			}
			return mappings;
		}

		private RecordSelectedType(int value) {
			intValue = value;
			getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static RecordSelectedType forValue(int value) {
			return getMappings().get(value);
		}
	}

	@SuppressWarnings("unused")
	public class RecordSelectedValue {
		private long num; /* initialization assumes a pointer is never larger than a long */
		private String str;
		private List<RecordSelected> rs = new ArrayList<>();

		public RecordSelectedValue() {
			super();
		}

		public RecordSelectedValue(long num) {
			super();
			this.num = num;
		}

		@SuppressWarnings({ "unused" })
		public RecordSelectedValue(long num, int allocValues, String str, List<RecordSelected> rs) {
			this();
			this.num = num;
			this.str = str;
			this.rs = rs;
		}

		public RecordSelectedValue(RecordSelectedValue recordSelectedValueInfo) {
			this.num = recordSelectedValueInfo.num;
			this.str = recordSelectedValueInfo.str;
			this.rs = recordSelectedValueInfo.rs;
		}
	}

	public enum RecordSelectedValueType {
		RSV_RS(2000), RSV_NUM(2001), RSV_STR(2002);

		public static final int SIZE = java.lang.Integer.SIZE;

		private int intValue;
		private static java.util.HashMap<Integer, RecordSelectedValueType> mappings;

		private static java.util.HashMap<Integer, RecordSelectedValueType> getMappings() {
			if (mappings == null) {
				mappings = new java.util.HashMap<>();
			}
			return mappings;
		}

		private RecordSelectedValueType(int value) {
			intValue = value;
			getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static RecordSelectedValueType forValue(int value) {
			return getMappings().get(value);
		}
	}

	@SuppressWarnings("unused")
	public class Segments {
		private int numOfPolygons;
		private Polygon polygons;
	}

	@SuppressWarnings("unused")
	public class SubField {
		private int numOfBytes;
		private int numOfItems;
		private int allocItems;
		private AtomicReferenceArray<Item> items;
		private int rsChar;

		public SubField() {
			super();
		}

		public SubField(int numOfBytes, int numOfItems, int allocItems, AtomicReferenceArray<Item> items, int rsChar) {
			this();
			this.numOfBytes = numOfBytes;
			this.numOfItems = numOfItems;
			this.allocItems = allocItems;
			this.items = items;
			this.rsChar = rsChar;
		}

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
	/* Alloc.java : ALLOCATION ROUTINES */
	public interface IAlloc {
		public AnsiNist allocANSIToNIST(AtomicInteger ret);

		public Record newANSIToNISTRecord(AtomicInteger ret, final int recordType);

		public Record allocANSIToNISTRecord(AtomicInteger ret);

		public Field newANSIToNISTField(AtomicInteger ret, final int recordType, final int field);

		public Field allocANSIToNISTField(AtomicInteger ret);

		public SubField allocANSIToNISTSubfield(AtomicInteger ret);

		public Item allocANSIToNISTItem(AtomicInteger ret);

		public void freeANSIToNIST(AnsiNist ansiNist);

		public void freeANSIToNISTRecord(Record recordInfo);

		public void freeANSIToNISTField(Field field);

		public void freeANSIToNISTSubfield(SubField subField);

		public void freeANSIToNISTItem(Item item);
	}

	/* Append.java : APPEND ROUTINES */
	public interface IAppend {
		public int appendANSIToNISTRecord(Record recordInfo, Field field);

		public int appendANSIToNISTField(Field field, SubField subField);

		public int appendANSIToNISTSubfield(SubField subField, Item item);
	}

	/* Copy.java : COPY ROUTINES */
	public interface ICopy {
		public AnsiNist copyANSIToNIST(AtomicInteger returnCode, AnsiNist toAnsiNist);

		public Record copyANSIToNISTRecord(AtomicInteger returnCode, Record toRecord);

		public Field copyANSIToNISTField(AtomicInteger returnCode, Field toField);

		public SubField copyANSIToNISTSubfield(AtomicInteger returnCode, SubField toSubField);

		public Item copyANSIToNISTItem(AtomicInteger returnCode, Item toItem);
	}

	/* Date.java : DATE ROUTINES */
	public interface IDate {
		public int getANSIToNISTDate(String date);
	}

	/* Decode.java : DECODE ROUTINES */
	public interface IDecode {
		@SuppressWarnings({ "java:S107" })
		public int decodeANSIToNISTImage(byte[] odata, AtomicInteger ow, AtomicInteger oh, AtomicInteger od,
				AtomicReference<Double> oppmm, final AnsiNist ansiNist, final int imgrecord_i, final int intrlvflag);

		public int decodeBinaryFieldImage(byte[] image, int a, int b, int c, double d, final AnsiNist ansiNist,
				final int e);

		@SuppressWarnings({ "java:S107" })
		public int decodeTaggedFieldImage(byte[] image, int a, int b, int c, double d, final AnsiNist ansiNist,
				final int e, final int f);
	}

	/* Delete.java : DELETE ROUTINES */
	public interface IDelete {
		public int doDelete(final String a, final int b, final int c, final int d, final int e, AnsiNist ansiNist);

		public int deleteANSIToNISTSelect(final int a, final int b, final int c, final int d, AnsiNist ansiNist);

		public int deleteANSIToNISTRecord(final int a, AtomicReference<AnsiNist> ansiNist);

		public int adjustDelrecCNTIDCs(final int a, AnsiNist ansiNist);

		public int deleteANSIToNISTField(final int a, final int b, AnsiNist ansiNist);

		public int deleteANSIToNISTSubfield(final int a, final int b, final int c, AnsiNist ansiNist);

		public int deleteANSIToNISTItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist);
	}

	/* Flip.java : FLIP COORDS & DIRECTION ROUTINES */
	public interface IFlip {
		public int flipYCoord(String a, final int b, final int c, final double d);

		public int flipDirection(String a, final int b);
	}

	/* FmtStd.C : ANSI_NIST FORMAT READ ROUTINES */
	public interface IFmtStd {
		public AnsiNist readANSIToNISTFile(AtomicInteger ret, String fileName);

		public int readANSIToNIST(File file, AnsiNist ansiNist);

		public int readType1Record(File file, Record recordInfo, AtomicInteger version);

		public int readANSIToNISTRemainingRecords(File file, AnsiNist ansiNist);

		public int readANSIToNISTRecord(File file, Record recordInfo, final int a);

		public int readANSIToNISTTaggedRecord(File file, Record recordInfo, int recordType);

		public int readANSIToNISTRecordLength(File file, AtomicInteger recordBytes, Field field);

		public int readANSIToNISTVersion(File file, AtomicInteger oversion, Field field);

		public int readANSIToNISTIntegerField(File file, AtomicInteger oversion, Field field);

		public int readANSIToNISTRemainingFields(File file, Record recordInfo);

		public int readANSIToNISTField(File file, Field field, int a);

		public int readANSIToNISTImageField(File file, Field field, String a, final int b, final int c,
				int d); /* Added by MDG 03-08-05 */

		public int readANSIToNISTTaggedField(File file, Field field, String a, final int b, final int c, int d);

		public int readANSIToNISTFieldID(File file, AtomicReference<String> fieldId, AtomicInteger recordType,
				AtomicInteger fieldInt);

		public int parseANSIToNISTFieldID(byte[] buffer, AtomicInteger oibufptr, AtomicInteger ebufptr,
				AtomicReference<String> fieldId, AtomicInteger recordType, AtomicInteger fieldInt);

		public int readANSIToNISTSubfield(File file, SubField subField);

		public int readANSIToNISTItem(File file, Item item);

		public int readANSIToNISTBinaryImageRecord(File file, Record recordInfo, final int a);

		public int readANSIToNISTBinarySignatureRecord(File file, Record recordInfo, final int a);

		public int readANSIToNISTBinaryField(File file, Field field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT BUFFER SCAN ROUTINES */
		public int scanANSIToNIST(BasicDataBuffer an2kBDB, AnsiNist ansiNist);

		public int scanType1Record(BasicDataBuffer an2kBDB, Record recordInfo, int a);

		public int scanANSIToNISTRemainingRecords(BasicDataBuffer an2kBDB, AnsiNist ansiNist);

		public int scanANSIToNISTRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		public int scanANSIToNISTTaggedRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		public int scanANSIToNISTRecordLength(BasicDataBuffer an2kBDB, int a, Field field);

		public int scanANSIToNISTVersion(BasicDataBuffer an2kBDB, int a, Field field);

		public int scanANSIToNISTIntegerField(BasicDataBuffer an2kBDB, int a, Field field);

		public int scanANSIToNISTRemainingFields(BasicDataBuffer an2kBDB, Record recordInfo);

		public int scanANSIToNISTField(BasicDataBuffer an2kBDB, Field field, int a);

		public int scanANSIToNISTImageField(BasicDataBuffer an2kBDB, Field field, String a, final int b, final int c,
				int d); /* Added by MDG 03-08-05 */

		public int scanANSIToNISTTaggedField(BasicDataBuffer an2kBDB, Field field, String a, final int b, final int c,
				int d);

		public int scanANSIToNISTFieldID(BasicDataBuffer an2kBDB, String a, int b, int c);

		public int scanANSIToNISTSubfield(BasicDataBuffer an2kBDB, SubField subField);

		public int scanANSIToNISTItem(BasicDataBuffer an2kBDB, Item item);

		public int scanANSIToNISTBinaryImageRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		public int scanANSIToNISTBinarySignatureRecord(BasicDataBuffer an2kBDB, Record recordInfo, final int a);

		public int scanANSIToNISTBinaryField(BasicDataBuffer an2kBDB, Field field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT WRITE ROUTINES */
		public int writeANSIToNISTFile(final String a, final AnsiNist ansiNist);

		public int writeANSIToNIST(File file, final AnsiNist ansiNist);

		public int writeANSIToNISTRecord(File file, Record recordInfo);

		public int writeANSIToNISTTaggedField(File file, final Field field);

		public int writeANSIToNISTTaggedSubfield(File file, final SubField subField);

		public int writeANSIToNISTTaggedItem(File file, final Item item);

		public int writeANSIToNISTSeparator(File file, final char a);

		public int writeANSIToNISTBinaryField(File file, final Field field);

		public int writeANSIToNISTBinarySubfield(File file, final SubField subField);

		public int writeANSIToNISTBinaryItem(File file, final Item item);
	}

	/* FmtText.java : READ FORMATTED TEXT ROUTINES */
	/* FmtText.java : WRITE FORMATTED TEXT ROUTINES */
	public interface IFmtText {
		public int readFormatTextFile(String fileName, AnsiNist ansiNist);

		public int readFormatText(File file, AnsiNist ansiNist);

		@SuppressWarnings({ "java:S107" })
		public int readFormatTextItem(File file, int a, int b, int c, int d, int e, int f, String g);

		public int writeFormatTextFile(String fileName, AnsiNist ansiNist);

		public int writeFormatText(File file, final AnsiNist ansiNist);

		public int writeFormatTextRecord(File file, final int a, final AnsiNist ansiNist);

		public int writeFormatTextField(File file, final int a, final int b, final AnsiNist ansiNist);

		public int writeFormatTextImageField(File file, final int a, final int b, final AnsiNist ansiNist);

		public int writeFormatTextSubfield(File file, final int a, final int b, final int c, final AnsiNist ansiNist);

		public int writeFormatTextItem(File file, final int a, final int b, final int c, final int d,
				final AnsiNist ansiNist);
	}

	/* GetImg.java : LOCATE & RETURN IMAGE DATA ROUTINES */
	public interface IGetImg {
		@SuppressWarnings({ "java:S107" })
		public int getFirstGrayprint(byte[] data, int a, int b, int c, double d, int e, int f, Record recordInfo, int g,
				final AnsiNist ansiNist);
	}

	/* Insert.java : INSERT ROUTINES */
	public interface IInsert {
		public int doInsert(final String a, final int b, final int c, final int d, final int e, final String f,
				AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTSelect(final int a, final int b, final int c, final int d, final String e,
				AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTRecord(final int a, final String b, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTRecordFrmem(final int a, Record recordInfo, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTRecordCore(final int a, Record recordInfo, final int b,
				AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTField(final int a, final int b, String c, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTFieldFrmem(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTFieldCore(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);

		public int adjustInsrecCNTIDCs(final int a, final int b, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTSubfield(final int a, final int b, final int c, final String d,
				AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTSubfieldFrmem(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTSubfieldCore(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);

		public int insertANSIToNISTItem(final int a, final int b, final int c, final int d, final String e,
				AtomicReference<AnsiNist> ansiNist);
	}

	/* IsAn2k.java : AN2K FORMAT TESTS */
	public interface IIsAn2k {
		public int isANSIToNISTFile(final String fileName);

		public int isANSIToNIST(byte[] idata, final int ilen);
	}

	/* LookUp.java : LOOKUP ROUTINES */
	public interface ILookUp {
		public int lookupANSIToNISTField(AtomicReference<Field> ofield, AtomicInteger fieldI, final int field,
				final Record recordInfo);

		public int lookupANSIToNISTSubfield(AtomicReference<SubField> osubfield, final int subfield_index,
				final Field field);

		public int lookupANSIToNISTItem(AtomicReference<Item> item, final int item_index, final SubField subfield);

		public int lookupANSIToNISTImage(AtomicReference<Record> oimgrecord, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupANSIToNISTImagePpmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist,
				final int imgrecord_i);

		public int lookupBinaryFieldImagePpmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist,
				final int imgrecord_i);

		public int lookupTaggedFieldImagePpmm(AtomicReference<Double> oppmm, final Record recordInfo);

		public int lookupANSIToNISTFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupANSIToNISTGrayprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupBinaryFieldFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupTaggedFieldFingerprint(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupFingerprintWithIDC(AtomicReference<Record> recordInfo, AtomicInteger imgrecordI, final int idc,
				final int strt_record, final AnsiNist ansiNist);

		public int lookupFGPField(AtomicReference<Field> ofield, AtomicInteger fieldI, final Record recordInfo);

		public int lookupIMPField(AtomicReference<Field> ofield, AtomicInteger fieldI, final Record recordInfo);

		public int lookupMinutiaeFormat(AtomicReference<String> ofmt, final Record recordInfo);

		public int lookupANSIToNISTRecord(AtomicReference<Record> oimgrecord, AtomicInteger imgrecordI,
				final int strt_record, final AnsiNist ansiNist, final RecordSelected recSel);
	}

	/* Print.java : PRINT ROUTINES */
	public interface IPrint {
		public int doPrint(final String a, final int b, final int c, final int d, final int e, AnsiNist ansiNist);

		public int printANSIToNISTSelect(File file, final int a, final int b, final int c, final int d,
				AnsiNist ansiNist);
	}

	/* Read.java : GENERAL FILE AND BUFFER UTILITIES */
	/* Read.java : GENERAL READ UTILITIES */
	/* Read.java : GENERAL BUFFER SCAN UTILITIES */
	public interface IRead {
		public int fbgetc(File file, BasicDataBuffer an2kBDB);

		public long fbread(long a, long b, long c, File file, BasicDataBuffer an2kBDB);

		public long fbtell(File file, BasicDataBuffer an2kBDB);

		public int readBinaryItemData(File file, byte[] data, final int a);

		public int readBinaryUInt(File file, int[] a);

		public int readBinaryUShort(File file, short[] a);

		public int readBinaryUChar(File file, byte[] data);

		public int readBinaryImageData(String fileName, byte[] data, int a);

		public int readChar(File file, final int a);

		public int readString(File file, String a, final int b);

		public int readInteger(File file, int a, final int b);

		public int skipWhiteSpace(File file);

		public int scanBinaryItemData(BasicDataBuffer an2kBDB, byte[] data, final int a);

		public int scanBinaryUInt(BasicDataBuffer an2kBDB, int[] data);

		public int scanBinaryUShort(BasicDataBuffer an2kBDB, short[] data);

		public int scanBinaryUChar(BasicDataBuffer an2kBDB, byte[] data);
	}

	/* Size.java : FIELD BYTE SIZES */
	public interface ISize {
		public int binaryImageFieldBytes(final int a);

		public int binarySignatureFieldBytes(final int a);
	}

	/* Substitute.java : SUBSTITUTE ROUTINES */
	public interface ISubstitute {
		public int doSubstitute(String a, final int b, final int c, final int d, final int e, String f,
				AnsiNist ansiNist);

		public int substituteANSIToNISTSelect(final int a, final int b, final int c, final int d, String e,
				AnsiNist ansiNist);

		public int substituteANSIToNISTRecord(final int a, String b, AnsiNist ansiNist);

		public int substituteANSIToNISTField(final int a, final int b, String c, AnsiNist ansiNist);

		public int substituteANSIToNISTSubfield(final int a, final int b, final int c, String d, AnsiNist ansiNist);

		public int substituteANSIToNISTItem(final int a, final int b, final int c, final int d, String e,
				AtomicReference<AnsiNist> ansiNist);
	}

	/* TO_IAFIS.C : ANSI/NIST 2007 TO FBI/IAFIS CONVERSION ROUTINES */
	public interface IToIafis {
		public int nist2iafisFingerprints(AnsiNist ansiNist);

		public int nist2iafisFingerprint(Record fromRecord, Record toRecord);

		public int nist2iafisType9s(AnsiNist ansiNist);

		public int nist2iafisNeeded(Record recordInfo);

		public int nist2iafisType9(Record recordInfo, AnsiNist ansiNist, final int a);

		public int nist2iafisMethod(String a, String b);

		public int nist2iafisMinutiaType(String a, String b);

		public int nist2iafisPatternClass(String a, String b, final int c);

		public int nist2iafisRidgecount(String a, String b);
	}

	/* ToNist.java : FBI/IAFIS TO ANSI/NIST 2007 CONVERSION ROUTINES */
	public interface IToNist {
		public int iafis2nistFingerprints(AnsiNist ansiNist);

		public int iafis2nistFfingerprint(Record recordInfo, AtomicReference<AnsiNist> ansiNist, final AtomicInteger a);

		public int iafis2nistType9s(AnsiNist ansiNist);

		public int iafis2nistNeeded(Record recordInfo);

		public int iafis2nistType9(Record recordInfo, AnsiNist ansiNist, final int a);

		public int iafis2nistMethod(String a, String b);

		public int iafis2nistMinutiaType(String a, String b);

		public int iafis2nistPatternClass(String a, String b, final int c);

		public int iafis2nistRidgecount(String a, String b);
	}

	/* Type.java : RECORD & FIELD TYPE TESTS */
	public interface IType {
		public int taggedRecord(final int a);

		public int binaryRecord(final int a);

		public int taggedImageRecord(final int a);

		public int binaryImageRecord(final int a);

		public int imageRecord(final int a);

		public int binarySignatureRecord(final int a);

		public int imageField(final Field field);

		public int isDelimiter(final int a);

		public int whichHand(final int a);
	}

	/* Select.java : RECORD SELECTION BASED ON VARIOUS EXTENSIBLE CRITERIA */
	public interface ISelect {
		public int selectANSIToNISTRecord(AtomicReference<Record> recordInfo, final RecordSelected recSel);

		public int newRecSel(AtomicReference<RecordSelected> recSel, RecordSelectedType recSelType, int numValues,
				String[] args);

		public RecordSelected allocRecSel(AtomicInteger retCode, RecordSelectedType type, int allocValues);

		public void freeRecSel(RecordSelected recSel);

		public int addRecSelNum(AtomicReference<RecordSelected> head, final RecordSelectedType type, final int value);

		public int addRecSelStr(AtomicReference<RecordSelected> head, final RecordSelectedType type,
				final String value);

		public int addRecSel(AtomicReference<RecordSelected> head, RecordSelected newSel);

		public int parseRecSelOption(final RecordSelectedType recSelType, final String a, final String b,
				RecordSelected recSel, final int c);

		public int writeRecSel(File file, final RecordSelected recSelconst);

		public int writeRecSelFile(final String a, final RecordSelected recSelconst);

		public int readRecSel(File file, AtomicReference<RecordSelected> recSel);

		public int readRecSelFile(String inputFile, AtomicReference<RecordSelected> recSel);

		public int impIsRolled(final int a);

		public int impIsFlat(final int a);

		public int impIsLiveScan(final int a);

		public int impIsLatent(final int a);

		public RecordSelected simplifyRecSel(AtomicInteger retCode, RecordSelected rs);
	}

	/* Type1314.java : Type-13 and Type-14 ROUTINES */
	public interface IType1314 {
		@SuppressWarnings({ "java:S107" })
		public int fingerprint2taggedFieldImage(Record recordInfo, byte[] data, final int a, final int b, final int c,
				final int d, final double e, String f, final int g, final int h, String i);

		@SuppressWarnings({ "java:S107" })
		public int image2type13(Record recordInfo, byte[] data, final int a, final int b, final int c, final int d,
				final double e, String f, final int g, final int h, String i);

		@SuppressWarnings({ "java:S107" })
		public int image2type14(Record recordInfo, byte[] data, final int a, final int b, final int c, final int d,
				final double e, String f, final int g, final int h, String i);
	}

	/* An2kUpdate.java : UPDATE ROUTINES */
	public interface IAn2kUpdate {
		public int updateANSIToNIST(AnsiNist ansiNist, Record recordInfo);

		public int updateANSIToNISTRecord(Record recordInfo, Field field);

		public int updateANSIToNISTField(Field field, SubField subField);

		public int updateANSIToNISTSubfield(SubField subField, Item item);

		public int updateANSIToNISTItem(Item item, final int a);

		public int updateANSIToNISTRecordLENs(AnsiNist ansiNist);

		public int updateANSIToNISTRecordLEN(AnsiNist ansiNist, final int a);

		public int updateANSIToNISTBinaryRecordLEN(Record recordInfo);

		public int updateANSIToNISTTaggedRecordLEN(Record recordInfo);

		public void updateANSIToNISTFieldID(Field field, final int a, final int b);
	}

	/* An2kUtil.java : UTILITY ROUTINES */
	public interface IAn2kUtil {
		public int incrementNumericItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist,
				String e);

		public int decrementNumericItem(final int a, final int b, final int c, final int d, AnsiNist ansiNist,
				String e);
	}

	/* Value2.C : STRING TO STRUCTURE ROUTINES */
	public interface IValue2 {
		public int value2field(Field field, final int a, final int b, final String c);

		public int value2subfield(SubField subField, final String a);

		public int value2item(Item item, final String a);
	}
}