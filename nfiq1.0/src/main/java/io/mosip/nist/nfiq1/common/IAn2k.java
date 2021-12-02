package io.mosip.nist.nfiq1.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public interface IAn2k {
	public static final int True = 1;
	public static final int False = 0;

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

	public static final List<Integer> tagged_records = null;
	public static final int NUM_TAGGED_RECORDS = 10;

	public static final List<Integer> binary_records = null;
	public static final int NUM_BINARY_RECORDS = 6;
	
	public static final List<Integer> tagged_image_records = null;
	public static final int NUM_TAGGED_IMAGE_RECORDS = 7;
	public static final int IMAGE_FIELD = 999;

	public static final List<Integer> binary_image_records = null;
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
	//public static final int LEN_ID = 1;
	public static final int IDC_ID = 2;
	public static final int IMP_ID = 3;
	public static final int FGP_ID = 4;
	public static final int ISR_ID = 5;
	public static final int HLL_ID = 6;
	public static final int VLL_ID = 7;
	public static final int BIN_CA_ID = 8;
	public static final int BIN_IMAGE_ID = 9;
	
	public static final List<Integer> binary_signature_records = null;
	public static final int NUM_BINARY_SIGNATURE_RECORDS = 1;
	public static final int BINARY_SIG_BYTES = 1;
	public static final int BINARY_SRT_BYTES = 1;
	public static final int NUM_BINARY_SIGNATURE_FIELDS = 8;
	
	
	/* Type-8 Field IDs */
	//public static final int LEN_ID = 1;
	//public static final int IDC_ID = 2;
	public static final int SIG_ID = 3;
	public static final int SRT_ID = 4;
	//public static final int ISR_ID = 5;
	//public static final int HLL_ID = 6;
	//public static final int VLL_ID = 7;

	/* Type-10,13,14,15,16 Field IDs */
	//public static final int LEN_ID = 1;
	//public static final int IDC_ID = 2;
	//public static final int IMP_ID = 3;
	public static final int SRC_ID = 4;
	public static final int CD_ID = 5;
	//public static final int HLL_ID = 6;
	//public static final int VLL_ID = 7;
	public static final int SLC_ID = 8;
	public static final int HPS_ID = 9;
	public static final int VPS_ID = 10;
	public static final int TAG_CA_ID = 11;
	public static final int CSP_ID = 12;
	public static final int CSP_ID_Type_17 = 13;
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
	/* Type-13,14,15...*/
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
	//public static final int DMM_ID = 30;
	/* End of Type-13,14,15 field IDs. */

	/* Type-9 Standard Field IDs */
	//public static final int LEN_ID = 1;
	//public static final int IDC_ID = 2;
	//public static final int IMP_ID = 3;
	public static final int FMT_ID = 4;
	public static final int OFR_ID = 5;
	public static final int FGP2_ID = 6;
	public static final int FPC_ID = 7;
	public static final int CRP_ID = 8;
	public static final int DLT_ID = 9;
	public static final int MIN_ID = 10;
	public static final int RDG_ID = 11;
	public static final int MRC_ID = 12;
	/* Type-9 FBI/IAFIS Field IDs  */
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
	
	/* Maximum number of minutiae in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_MINUTIAE = 254;
	/* Maximum number of pattern classes in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_PATTERN_CLASSES = 3;
	/* Maximum number of cores in an FBI/IAFIS Type-9 record. */
	public static final int MAX_IAFIS_CORES = 2;
	/* Maximum number of deltas in an FBI/IAFIS Type-9 record. */
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

	//#define DEFAULT_FPOUT              stdout

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

	public class AnsiNist implements Cloneable {
		public int version;
		public int num_bytes;
		public int num_records;
		public int alloc_records;
		public AtomicReferenceArray<Record> records;
		
		public AnsiNist() {
			super();
		}

		public AnsiNist(int version, int num_bytes, int num_records, int alloc_records, AtomicReferenceArray<Record> records) {
			this();
			this.version = version;
			this.num_bytes = num_bytes;
			this.num_records = num_records;
			this.alloc_records = alloc_records;
			this.records = records;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public class BasicDataBuffer {
		public int bdb_size; // Max size of the buffer
		public int bdb_start; // Beginning read/write location
		public int bdb_end; // End read/write location
		public int bdb_current; // Current read/write location
		
		public BasicDataBuffer(int position, int bdb_size) {
			super();
			this.bdb_size = bdb_size;
			this.bdb_current = position;
			this.bdb_start = this.bdb_current;
			this.bdb_end = position + bdb_size;
		}
	}

	public class Field implements Cloneable {
		public String id;
		public int record_type;
		public int field_int;
		public int num_bytes;
		public int num_subfields;
		public int alloc_subfields;
		public AtomicReferenceArray<SubField> subfields;
		public int gs_char;
		
		public Field() {
			super();
		}

		public Field(String id, int record_type, int field_int, int num_bytes, int num_subfields, int alloc_subfields, AtomicReferenceArray<SubField> subfields, int gs_char) {
			this();
			this.id = id;
			this.record_type = record_type;
			this.field_int = field_int;
			this.num_bytes = num_bytes;
			this.num_subfields = num_subfields;
			this.alloc_subfields = alloc_subfields;
			this.subfields = subfields;
			this.gs_char = gs_char;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

	}

	public class Item implements Cloneable {
		public int num_bytes; // Always contains the current byte size of the entire
		/* item including any trailing US separator.           */
		public int num_chars; // Number of characters currently in value, NOT
		/* including the NULL terminator.                */
		public int alloc_chars; // Number of allocated characters for the value,
		/* including the NULL terminator.                */
		public byte value; // Must keep NULL terminated.
		public int us_char;
		
		public Item() {
			super();
		}

		public Item(int num_bytes, int num_chars, int alloc_chars, byte value, int us_char) {
			this();
			this.num_bytes = num_bytes;
			this.num_chars = num_chars;
			this.alloc_chars = alloc_chars;
			this.value = value;
			this.us_char = us_char;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public class Polygon {
		public int fgp;
		public int num_points;
		public byte x;
		public byte y;
	}

	public class Record implements Cloneable {
		public int type;
		public int total_bytes;
		public int num_bytes;
		public int num_fields;
		public int alloc_fields;
		public AtomicReferenceArray<Field> fields;
		public int fs_char;
		
		public Record() {
			super();
		}

		public Record(int type, int total_bytes, int num_bytes, int num_fields, int alloc_fields, AtomicReferenceArray<Field> fields, int fs_char) {
			this();
			this.type = type;
			this.total_bytes = total_bytes;
			this.num_bytes = num_bytes;
			this.num_fields = num_fields;
			this.alloc_fields = alloc_fields;
			this.fields = fields;
			this.fs_char = fs_char;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public class RecordSelected implements Cloneable {
		public RecordSelectedType type;
		public int alloc_values;
		public int num_values;
	   	public RecordSelectedValue value;
	   	
		public RecordSelected() {
			super();
		}

		public RecordSelected(RecordSelectedType type, int alloc_values, int num_values, RecordSelectedValue value) {
			this();
			this.type = type;
			this.alloc_values = alloc_values;
			this.num_values = num_values;
			this.value = value;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public enum RecordSelectedType {
		rs_and(1000),
		rs_or(1001),
		rs_lrt(1002), // logical record type
		rs_fgplp(1003), // finger or palm position
		rs_fgp(1004), // finger position
		rs_plp(1005), // palm position
		rs_imp(1006), // impression type
		rs_idc(1007), // image descriptor chararacter
		rs_nqm(1008), // NIST quality metric
		rs_imt(1009),
		rs_pos(1010); // subject pose

		public static final int SIZE = java.lang.Integer.SIZE;

		private int intValue;
		private static java.util.HashMap<Integer, RecordSelectedType> mappings;
		private static java.util.HashMap<Integer, RecordSelectedType> getMappings()
		{
			if (mappings == null)
			{
				synchronized (RecordSelectedType.class)
				{
					if (mappings == null)
					{
						mappings = new java.util.HashMap<Integer, RecordSelectedType>();
					}
				}
			}
			return mappings;
		}

		private RecordSelectedType(int value)
		{
			intValue = value;
			getMappings().put(value, this);
		}

		public int getValue()
		{
			return intValue;
		}

		public static RecordSelectedType forValue(int value)
		{
			return getMappings().get(value);
		}
	}

	public class RecordSelectedValue implements Cloneable  {
		public long num;  /* initialization assumes a pointer is never larger than a long */
		public String str;
		public ArrayList<RecordSelected> rs = new ArrayList<RecordSelected>();

		public RecordSelectedValue() {
			super();
		}

		public RecordSelectedValue(long num) {
			super();
			this.num = num;
		}

		public RecordSelectedValue(long num, int alloc_values, String str, ArrayList<RecordSelected> rs) {
			this();
			this.num = num;
			this.str = str;
			this.rs = rs;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public enum RecordSelectedValueType {
		rsv_rs(2000),
		rsv_num(2001),
		rsv_str(2002);

		public static final int SIZE = java.lang.Integer.SIZE;

		private int intValue;
		private static java.util.HashMap<Integer, RecordSelectedValueType> mappings;
		private static java.util.HashMap<Integer, RecordSelectedValueType> getMappings()
		{
			if (mappings == null)
			{
				synchronized (RecordSelectedValueType.class)
				{
					if (mappings == null)
					{
						mappings = new java.util.HashMap<Integer, RecordSelectedValueType>();
					}
				}
			}
			return mappings;
		}

		private RecordSelectedValueType(int value)
		{
			intValue = value;
			getMappings().put(value, this);
		}

		public int getValue()
		{
			return intValue;
		}

		public static RecordSelectedValueType forValue(int value)
		{
			return getMappings().get(value);
		}
	}

	public class Segments {
		public int num_polygons;
		Polygon polygons;
	}

	public class SubField implements Cloneable {
	 	public int num_bytes;
		public int num_items;
		public int alloc_items;
		public AtomicReferenceArray<Item> items;
		public int rs_char;
		
		public SubField() {
			super();
		}

		public SubField(int num_bytes, int num_items, int alloc_items, AtomicReferenceArray<Item> items, int rs_char) {
			this();
			this.num_bytes = num_bytes;
			this.num_items = num_items;
			this.alloc_items = alloc_items;
			this.items = items;
			this.rs_char = rs_char;
		}

		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	/*************************************************************************/
	/*        EXTERNAL FUNCTION DEFINITIONS                                  */
	/*************************************************************************/
	/* Alloc.java : ALLOCATION ROUTINES */
	public interface IAlloc {
		public AnsiNist alloc_ANSI_NIST(AtomicInteger ret);
		public Record new_ANSI_NIST_record(AtomicInteger ret, final int record_type);
		public Record alloc_ANSI_NIST_record(AtomicInteger ret);
		public Field new_ANSI_NIST_field(AtomicInteger ret, final int record_type, final int field_int);
		public Field alloc_ANSI_NIST_field(AtomicInteger ret);
		public SubField alloc_ANSI_NIST_subfield(AtomicInteger ret);
		public Item alloc_ANSI_NIST_item(AtomicInteger ret);
		public void free_ANSI_NIST(AnsiNist ansiNist);
		public void free_ANSI_NIST_record(Record record);
		public void free_ANSI_NIST_field(Field field);
		public void free_ANSI_NIST_subfield(SubField subField);
		public void free_ANSI_NIST_item(Item item);
	}

	/* Append.java : APPEND ROUTINES */
	public interface IAppend {
		public int append_ANSI_NIST_record (Record record, Field field);
		public int append_ANSI_NIST_field (Field field, SubField subField);
		public int append_ANSI_NIST_subfield (SubField subField, Item item);
	}

	/* Copy.java : COPY ROUTINES */
	public interface ICopy {
		public AnsiNist copy_ANSI_NIST(AtomicInteger returnCode, AnsiNist toAnsiNist);
		public Record copy_ANSI_NIST_record(AtomicInteger returnCode, Record toRecord);
		public Field copy_ANSI_NIST_field(AtomicInteger returnCode, Field toField);
		public SubField copy_ANSI_NIST_subfield(AtomicInteger returnCode, SubField toSubField);
		public Item copy_ANSI_NIST_item(AtomicInteger returnCode, Item toItem);
	}

	/* Date.java : DATE ROUTINES */
	public interface IDate {
		public int get_ANSI_NIST_date(String date);
	}

	/* Decode.java : DECODE ROUTINES */
	public interface IDecode {
		public int decode_ANSI_NIST_image(byte[] odata, AtomicInteger ow, AtomicInteger oh, AtomicInteger od, AtomicReference<Double> oppmm, final AnsiNist ansiNist, final int imgrecord_i, final int intrlvflag);
		public int decode_binary_field_image(byte [] image, int a, int b, int c,
	        double d, final AnsiNist ansiNist, final int e);
		public int decode_tagged_field_image(byte [] image, int a, int b, int c,
	        double d, final AnsiNist ansiNist, final int e, final int f);
	}

	/* Delete.java : DELETE ROUTINES */
	public interface IDelete {
		public int do_delete(final String a, final int b, final int c, final int d,
				final int e, AnsiNist ansiNist);
		public int delete_ANSI_NIST_select(final int a, final int b, final int c,
				final int d, AnsiNist ansiNist);
		public int delete_ANSI_NIST_record(final int a, AtomicReference<AnsiNist> ansiNist);
		public int adjust_delrec_CNT_IDCs(final int a, AnsiNist ansiNist);
		public int delete_ANSI_NIST_field(final int a, final int b, AnsiNist ansiNist);
		public int delete_ANSI_NIST_subfield(final int a, final int b, final int c, AnsiNist ansiNist);
		public int delete_ANSI_NIST_item(final int a, final int b, final int c, final int d, AnsiNist ansiNist);
	}

	/* Flip.java : FLIP COORDS & DIRECTION ROUTINES */
	public interface IFlip {
		public int flip_y_coord(String a, final int b, final int c, final double d);
		public int flip_direction(String a, final int b);
	}
	
	/* FmtStd.C : ANSI_NIST FORMAT READ ROUTINES */
	public interface IFmtStd {
		public AnsiNist read_ANSI_NIST_file(AtomicInteger ret, String fileName);
		public int read_ANSI_NIST(File file, AnsiNist ansiNist);
		public int read_Type1_record(File file, Record record, AtomicInteger version);
		public int read_ANSI_NIST_remaining_records(File file, AnsiNist ansiNist);
		public int read_ANSI_NIST_record(File file, Record record, final int a);
		public int read_ANSI_NIST_tagged_record(File file, Record record, int record_type);
		public int read_ANSI_NIST_record_length(File file, AtomicInteger orecord_bytes, Field field);
		public int read_ANSI_NIST_version(File file, AtomicInteger oversion, Field field);
		public int read_ANSI_NIST_integer_field(File file, AtomicInteger oversion, Field field);
		public int read_ANSI_NIST_remaining_fields(File file, Record record);
		public int read_ANSI_NIST_field(File file, Field field, int a);
		public int read_ANSI_NIST_image_field(File file, Field field, String a, final int b,
	              final int c, int d); /* Added by MDG 03-08-05 */
		public int read_ANSI_NIST_tagged_field(File file, Field field, String a, final int b,
	              final int c, int d);
		public int read_ANSI_NIST_field_ID(File file, AtomicReference<String> ofield_id, AtomicInteger orecord_type, AtomicInteger ofield_int);
		public int parse_ANSI_NIST_field_ID(byte[] buffer, AtomicInteger oibufptr, AtomicInteger ebufptr, 
			AtomicReference<String> ofield_id, AtomicInteger orecord_type, AtomicInteger ofield_int);
		public int read_ANSI_NIST_subfield(File file, SubField subField);
		public int read_ANSI_NIST_item(File file, Item item);
		public int read_ANSI_NIST_binary_image_record(File file, Record record,
	              final int a);
		public int read_ANSI_NIST_binary_signature_record(File file, Record record,
	              final int a);
		public int read_ANSI_NIST_binary_field(File file, Field field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT BUFFER SCAN ROUTINES */
		public int scan_ANSI_NIST(BasicDataBuffer an2kBDB, AnsiNist ansiNist);
		public int scan_Type1_record(BasicDataBuffer an2kBDB, Record record, int a);
		public int scan_ANSI_NIST_remaining_records(BasicDataBuffer an2kBDB, AnsiNist ansiNist);
		public int scan_ANSI_NIST_record(BasicDataBuffer an2kBDB, Record record, final int a);
		public int scan_ANSI_NIST_tagged_record(BasicDataBuffer an2kBDB, Record record,
		              final int a);
		public int scan_ANSI_NIST_record_length(BasicDataBuffer an2kBDB, int a, Field field);
		public int scan_ANSI_NIST_version(BasicDataBuffer an2kBDB, int a, Field field);
		public int scan_ANSI_NIST_integer_field(BasicDataBuffer an2kBDB, int a, Field field);
		public int scan_ANSI_NIST_remaining_fields(BasicDataBuffer an2kBDB, Record record);
		public int scan_ANSI_NIST_field(BasicDataBuffer an2kBDB, Field field, int a);
		public int scan_ANSI_NIST_image_field(BasicDataBuffer an2kBDB, Field field, String a, final int b,
		              final int c, int d); /* Added by MDG 03-08-05 */
		public int scan_ANSI_NIST_tagged_field(BasicDataBuffer an2kBDB, Field field, String a, final int b,
		              final int c, int d);
		public int scan_ANSI_NIST_field_ID(BasicDataBuffer an2kBDB, String a, int b,
		              int c);
		public int scan_ANSI_NIST_subfield(BasicDataBuffer an2kBDB, SubField subField);
		public int scan_ANSI_NIST_item(BasicDataBuffer an2kBDB, Item item);
		public int scan_ANSI_NIST_binary_image_record(BasicDataBuffer an2kBDB, Record record,
				final int a);
		public int scan_ANSI_NIST_binary_signature_record(BasicDataBuffer an2kBDB, Record record,
		              final int a);
		public int scan_ANSI_NIST_binary_field(BasicDataBuffer an2kBDB, Field Field, final int a);

		/* FMTSTD.C : ANSI_NIST FORMAT WRITE ROUTINES */
		public int write_ANSI_NIST_file(final String a, final AnsiNist ansiNist);
		public int write_ANSI_NIST(File file, final AnsiNist ansiNist);
		public int write_ANSI_NIST_record(File file, Record record);
		public int write_ANSI_NIST_tagged_field(File file, final Field field);
		public int write_ANSI_NIST_tagged_subfield(File file, final SubField subField);
		public int write_ANSI_NIST_tagged_item(File file, final Item item);
		public int write_ANSI_NIST_separator(File file, final char a);
		public int write_ANSI_NIST_binary_field(File file, final Field field);
		public int write_ANSI_NIST_binary_subfield(File file, final SubField subField);
		public int write_ANSI_NIST_binary_item(File file, final Item item);
	}

	/* FmtText.java : READ FORMATTED TEXT ROUTINES */
	/* FmtText.java : WRITE FORMATTED TEXT ROUTINES */
	public interface IFmtText {
		public int read_fmttext_file(String fileName, AnsiNist ansiNist);
		public int read_fmttext(File file, AnsiNist ansiNist);
		public int read_fmttext_item(File file, int a, int b, int c, int d, int e,
		              int f, String g);

		public int write_fmttext_file(String fileName, AnsiNist ansiNist);
		public int write_fmttext(File file, final AnsiNist ansiNist);
		public int write_fmttext_record(File file, final int a, final AnsiNist ansiNist);
		public int write_fmttext_field(File file, final int a, final int b,
		              final AnsiNist ansiNist);
		public int write_fmttext_image_field(File file, final int a, final int b,
		                      final AnsiNist ansiNist);
		public int write_fmttext_subfield(File file, final int a, final int b, final int c,
		              final AnsiNist ansiNist);
		public int write_fmttext_item(File file, final int a, final int b, final int c,
				final int d, final AnsiNist ansiNist);
	}

	/* GetImg.java : LOCATE & RETURN IMAGE DATA ROUTINES */
	public interface IGetImg {
		public int get_first_grayprint(byte [] data, int a, int b, int c,
			double d, int e, int f,
	       	Record record, int g, final AnsiNist ansiNist);
	}

	/* Insert.java : INSERT ROUTINES */
	public interface IInsert {
		public int do_insert(final String a, final int b, final int c, final int d,
			final int e, final String f, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_select(final int a, final int b, final int c,
			final int d, final String e, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_record(final int a, final String b, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_record_frmem(final int a, Record record, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_record_core(final int a, Record record, final int b, 
				AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_field(final int a, final int b, String c,
				AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_field_frmem(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_field_core(final int a, final int b, AtomicReference<Field> field,
				AtomicReference<AnsiNist> ansiNist);
		public int adjust_insrec_CNT_IDCs(final int a, final int b, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_subfield(final int a, final int b, final int c,
			final String d, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_subfield_frmem(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_subfield_core(final int a, final int b, final int c,
				AtomicReference<SubField> subField, AtomicReference<AnsiNist> ansiNist);
		public int insert_ANSI_NIST_item(final int a, final int b, final int c, final int d,
			final String e, AtomicReference<AnsiNist> ansiNist);
	}

	/* IsAn2k.java : AN2K FORMAT TESTS */
	public interface IIsAn2k {
		public int is_ANSI_NIST_file(final String fileName);
		public int is_ANSI_NIST(byte[] idata, final int ilen);
	}

	/* LookUp.java : LOOKUP ROUTINES */
	public interface ILookUp {
		public int lookup_ANSI_NIST_field(AtomicReference<Field> ofield, AtomicInteger ofield_i, final int field_int, final Record record);
		public int lookup_ANSI_NIST_subfield(AtomicReference<SubField> osubfield, final int subfield_index, final Field field);
		public int lookup_ANSI_NIST_item(AtomicReference<Item> item, final int item_index, final SubField subfield);
		public int lookup_ANSI_NIST_image(AtomicReference<Record> oimgrecord, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist);
		public int lookup_ANSI_NIST_image_ppmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist, final int imgrecord_i);
		public int lookup_binary_field_image_ppmm(AtomicReference<Double> oppmm, final AnsiNist ansiNist, final int imgrecord_i);
		public int lookup_tagged_field_image_ppmm(AtomicReference<Double> oppmm, final Record record);
		public int lookup_ANSI_NIST_fingerprint(AtomicReference<Record> record, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist);
		public int lookup_ANSI_NIST_grayprint(AtomicReference<Record> record, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist);
		public int lookup_binary_field_fingerprint(AtomicReference<Record> record, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist);
		public int lookup_tagged_field_fingerprint(AtomicReference<Record> record, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist);
		public int lookup_fingerprint_with_IDC(AtomicReference<Record> record, AtomicInteger oimgrecord_i, final int idc, final int strt_record, final AnsiNist ansiNist);
		public int lookup_FGP_field(AtomicReference<Field> ofield, AtomicInteger ofield_i, final Record record);
		public int lookup_IMP_field(AtomicReference<Field> ofield, AtomicInteger ofield_i, final Record record);
		public int lookup_minutiae_format(AtomicReference<String> ofmt, final Record record);
		public int lookup_ANSI_NIST_record(AtomicReference<Record> oimgrecord, AtomicInteger oimgrecord_i, final int strt_record, final AnsiNist ansiNist, final RecordSelected recSel);
	}

	/* Print.java : PRINT ROUTINES */
	public interface IPrint{
		public int do_print(final String a, final int b, final int c, final int d,
			final int e, AnsiNist ansiNist);
		public int print_ANSI_NIST_select(File file, final int a, final int b, final int c,
			final int d, AnsiNist ansiNist);
	}

	/* Read.java : GENERAL FILE AND BUFFER UTILITIES */
	/* Read.java : GENERAL READ UTILITIES */
	/* Read.java : GENERAL BUFFER SCAN UTILITIES */
	public interface IRead{
		public int fbgetc(File file, BasicDataBuffer an2kBDB);
		public long fbread(long a, long b, long c, File file, BasicDataBuffer an2kBDB);
		public long fbtell(File file, BasicDataBuffer an2kBDB);

		public int read_binary_item_data(File file, byte[] data, final int a);
		public int read_binary_uint(File file, int [] a);
		public int read_binary_ushort(File file, short [] a);
		public int read_binary_uchar(File file, byte[] data);
		public int read_binary_image_data(String fileName, byte[] data, int a);
		public int read_char(File file, final int a);
		public int read_string(File file, String a, final int b);
		public int read_integer(File file, int a, final int b);
		public int skip_white_space(File file);

		public int scan_binary_item_data(BasicDataBuffer an2kBDB, byte[] data, final int a);
		public int scan_binary_uint(BasicDataBuffer an2kBDB, int [] data);
		public int scan_binary_ushort(BasicDataBuffer an2kBDB, short [] data);
		public int scan_binary_uchar(BasicDataBuffer an2kBDB, byte[] data);
	}

	/* Size.java : FIELD BYTE SIZES */
	public interface ISize{
		public int binary_image_field_bytes(final int a);
		public int binary_signature_field_bytes(final int a);
	}

	/* Substitute.java : SUBSTITUTE ROUTINES */
	public interface ISubstitute {
		public int do_substitute(String a, final int b, final int c, final int d,
				final int e, String f, AnsiNist ansiNist);
		public int substitute_ANSI_NIST_select(final int a, final int b, final int c,
				final int d, String e, AnsiNist ansiNist);
		public int substitute_ANSI_NIST_record(final int a, String b, AnsiNist ansiNist);
		public int substitute_ANSI_NIST_field(final int a, final int b, String c,
		              AnsiNist ansiNist);
		public int substitute_ANSI_NIST_subfield(final int a, final int b, final int c,
				String d, AnsiNist ansiNist);
		public int substitute_ANSI_NIST_item(final int a, final int b, final int c,
				final int d, String e, AtomicReference<AnsiNist> ansiNist);
	}

	/* TO_IAFIS.C : ANSI/NIST 2007 TO FBI/IAFIS CONVERSION ROUTINES */
	public interface IToIafis {
		public int nist2iafis_fingerprints(AnsiNist ansiNist);
		public int nist2iafis_fingerprint(Record fromRecord, Record toRecord);
		public int nist2iafis_type_9s(AnsiNist ansiNist);
		public int nist2iafis_needed(Record record);
		public int nist2iafis_type_9(Record record, AnsiNist ansiNist, final int a);
		public int nist2iafis_method(String a, String b);
		public int nist2iafis_minutia_type(String a, String b);
		public int nist2iafis_pattern_class(String a, String b, final int c);
		public int nist2iafis_ridgecount(String a, String b);
	}

	/* ToNist.java : FBI/IAFIS TO ANSI/NIST 2007 CONVERSION ROUTINES */
	public interface IToNist {
		public int iafis2nist_fingerprints(AnsiNist ansiNist);
		public int iafis2nist_fingerprint(Record record, AtomicReference<AnsiNist> ansiNist, final AtomicInteger a);
		public int iafis2nist_type_9s(AnsiNist ansiNist);
		public int iafis2nist_needed(Record record);
		public int iafis2nist_type_9(Record record, AnsiNist ansiNist, final int a);
		public int iafis2nist_method(String a, String b);
		public int iafis2nist_minutia_type(String a, String b);
		public int iafis2nist_pattern_class(String a, String b, final int c);
		public int iafis2nist_ridgecount(String a, String b);
	}

	/* Type.java : RECORD & FIELD TYPE TESTS */
	public interface IType {
		public int tagged_record(final int a);
		public int binary_record(final int a);
		public int tagged_image_record(final int a);
		public int binary_image_record(final int a);
		public int image_record(final int a);
		public int binary_signature_record(final int a);
		public int image_field(final Field field);
		public int is_delimiter(final int a);
		public int which_hand(final int a);
	}

	/* Select.java : RECORD SELECTION BASED ON VARIOUS EXTENSIBLE CRITERIA */
	public interface ISelect {
		public int select_ANSI_NIST_record(AtomicReference<Record> record, final RecordSelected recSel);
		//public int new_rec_sel(RecordSelected recSel, final RecordSelectedType recSelType, final int a, ...);
		public int new_rec_sel(AtomicReference<RecordSelected> recSel, RecordSelectedType recSelType, int num_values, String[] args);
		public RecordSelected alloc_rec_sel(AtomicInteger retCode, RecordSelectedType type, int alloc_values);
		public void free_rec_sel(RecordSelected recSel);
		public int add_rec_sel_num(AtomicReference<RecordSelected> head, final RecordSelectedType type, final int value);
		public int add_rec_sel_str(AtomicReference<RecordSelected> head, final RecordSelectedType type, final String value);
		public int add_rec_sel(AtomicReference<RecordSelected> head, RecordSelected new_sel);
		public int parse_rec_sel_option(final RecordSelectedType recSelType, final String a,
						final String b, RecordSelected recSel, final int c);
		public int write_rec_sel(File file, final RecordSelected recSelconst);
		public int write_rec_sel_file(final String a, final RecordSelected recSelconst);
		public int read_rec_sel(File file, AtomicReference<RecordSelected> recSel);
		public int read_rec_sel_file(String input_file, AtomicReference<RecordSelected> recSel);
		public int imp_is_rolled(final int a);
		public int imp_is_flat(final int a);
		public int imp_is_live_scan(final int a);
		public int imp_is_latent(final int a);
		public RecordSelected simplify_rec_sel(AtomicInteger retCode, RecordSelected rs);
	}

	/* Type1314.java : Type-13 and Type-14 ROUTINES */
	public interface IType1314 {
		public int fingerprint2tagged_field_image(Record record, byte[] data,
				final int a, final int b, final int c, final int d, final double e,
				String f, final int g, final int h, String i);
		public int image2type_13(Record record, byte[] data, final int a, final int b,
				final int c, final int d, final double e, String f, final int g,
				final int h, String i);
		public int image2type_14(Record record, byte[] data, final int a, final int b,
				final int c, final int d, final double e, String f, final int g,
				final int h, String i);
	}

	/* An2kUpdate.java : UPDATE ROUTINES */
	public interface IAn2kUpdate {
		public int update_ANSI_NIST(AnsiNist ansiNist, Record record);
		public int update_ANSI_NIST_record(Record record, Field field);
		public int update_ANSI_NIST_field(Field field, SubField subField);
		public int update_ANSI_NIST_subfield(SubField subField, Item item);
		public int update_ANSI_NIST_item(Item item, final int a);
		public int update_ANSI_NIST_record_LENs(AnsiNist ansiNist);
		public int update_ANSI_NIST_record_LEN(AnsiNist ansiNist, final int a);
		public int update_ANSI_NIST_binary_record_LEN(Record record);
		public int update_ANSI_NIST_tagged_record_LEN(Record record);
		public void update_ANSI_NIST_field_ID(Field field, final int a, final int b);
	}

	/* An2kUtil.java : UTILITY ROUTINES */
	public interface IAn2kUtil {
		public int increment_numeric_item(final int a, final int b, final int c,
				final int d, AnsiNist ansiNist, String e);
		public int decrement_numeric_item(final int a, final int b, final int c,
				final int d, AnsiNist ansiNist, String e);
	}

	/* Value2.C : STRING TO STRUCTURE ROUTINES */
	public interface IValue2 {
		public int value2field(Field Field, final int a, final int b, final String c);
		public int value2subfield(SubField subField, final String a);
		public int value2item(Item item, final String a);
	}
}

