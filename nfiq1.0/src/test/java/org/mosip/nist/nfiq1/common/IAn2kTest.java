package org.mosip.nist.nfiq1.common;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.Test;

public class IAn2kTest {

	/** Utility: Read private field value using reflection */
	private Object getPrivateField(Object obj, String fieldName) {
		try {
			Field f = obj.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			return f.get(obj);
		} catch (Exception e) {
			return null;
		}
	}

	@Test
	public void testBasicDataBuffer() {
		IAn2k.BasicDataBuffer buffer = new IAn2k.BasicDataBuffer(5, 100);

		assertEquals(100, getPrivateField(buffer, "bdbSize"));
		assertEquals(5, getPrivateField(buffer, "bdbStart"));
		assertEquals(5, getPrivateField(buffer, "bdbCurrent"));
		assertEquals(105, getPrivateField(buffer, "bdbEnd"));

		// Copy constructor
		IAn2k.BasicDataBuffer copy = new IAn2k.BasicDataBuffer(buffer);
		assertEquals(getPrivateField(buffer, "bdbSize"),
				getPrivateField(copy, "bdbSize"));
	}

	@Test
	public void testFieldConstructor() {
		AtomicReferenceArray<IAn2k.SubField> subfields =
				new AtomicReferenceArray<>(1);

		IAn2k.Field field = new IAn2k.Field(
				"10.003",
				10, 3, 120, 1, 1,
				subfields,
				IAn2k.GS_CHAR
		);

		assertEquals("10.003", getPrivateField(field, "id"));
		assertEquals(10, getPrivateField(field, "recordType"));
		assertEquals(3, getPrivateField(field, "fieldInfo"));
		assertEquals(120, getPrivateField(field, "numOfBytes"));
		assertEquals(subfields, getPrivateField(field, "subfields"));

		// Copy constructor
		IAn2k.Field copy = new IAn2k.Field(field);
		assertEquals(getPrivateField(field, "id"),
				getPrivateField(copy, "id"));
	}

	@Test
	public void testRecordConstructor() {
		AtomicReferenceArray<IAn2k.Field> fields =
				new AtomicReferenceArray<>(1);

		IAn2k.Record record = new IAn2k.Record(
				10, 512, 500, 3, 5,
				fields,
				IAn2k.FS_CHAR
		);

		assertEquals(10, getPrivateField(record, "type"));
		assertEquals(512, getPrivateField(record, "totalBytes"));
		assertEquals(fields, getPrivateField(record, "fields"));

		// Copy constructor
		IAn2k.Record copy = new IAn2k.Record(record);
		assertEquals(getPrivateField(record, "type"),
				getPrivateField(copy, "type"));
	}

	@Test
	public void testRecordSelected() {
		IAn2k.RecordSelectedValue value = new IAn2k.RecordSelectedValue(1234L);

		IAn2k.RecordSelected rs = new IAn2k.RecordSelected(
				IAn2k.RecordSelectedType.RS_IDC,
				10, 1, value
		);

		assertEquals(IAn2k.RecordSelectedType.RS_IDC,
				getPrivateField(rs, "type"));

		// Copy constructor
		IAn2k.RecordSelected copy = new IAn2k.RecordSelected(rs);
		assertEquals(getPrivateField(rs, "type"),
				getPrivateField(copy, "type"));
	}

	@Test
	public void testConstants() {
		assertEquals(1, IAn2k.TRUE);
		assertEquals(0, IAn2k.FALSE);
		assertEquals(1, IAn2k.TYPE_1_ID);
		assertEquals(99, IAn2k.TYPE_99_ID);
	}
}