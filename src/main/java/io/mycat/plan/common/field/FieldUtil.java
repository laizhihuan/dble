package io.mycat.plan.common.field;

import java.util.List;

import io.mycat.plan.common.item.FieldTypes;

public class FieldUtil {
	public static final int NOT_NULL_FLAG = 1; /* Field can't be NULL */
	public static final int PRI_KEY_FLAG = 2; /*
												 * Field is part of a primary key
												 */
	public static final int UNIQUE_KEY_FLAG = 4; /*
													 * Field is part of a unique
													 * key
													 */
	public static final int MULTIPLE_KEY_FLAG = 8; /* Field is part of a key */
	public static final int BLOB_FLAG = 16; /* Field is a blob */
	public static final int UNSIGNED_FLAG = 32; /* Field is unsigned */
	public static final int ZEROFILL_FLAG = 64; /* Field is zerofill */
	public static final int BINARY_FLAG = 128; /* Field is binary */

	/**
	 * b1,b2代表的是整数型数字，进行比较,b1,b2非null
	 * 
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static int compareIntUsingStringBytes(byte[] b1, byte[] b2) {
		char b1c0 = (char) b1[0];
		char b2c0 = (char) b2[0];
		if (b1c0 == '-') {// b1为负数
			if (b2c0 == '-') { // b2为负数
				return -compareUnIntUsingStringBytes(b1, 1, b2, 1);
			} else {
				return -1;
			}
		} else {// b1为正数
			if (b2c0 == '-') {
				return 1;
			} else {
				return compareUnIntUsingStringBytes(b1, 0, b2, 0);
			}
		}
	}

	/* 不考虑符号的b1,b2的比较 , b1,b2代表整数 */
	private static int compareUnIntUsingStringBytes(byte[] b1, int startb1, byte[] b2, int startb2) {
		int b1len = b1.length - startb1;
		int b2len = b2.length - startb2;
		if (b1len < b2len)
			return -1;
		else if (b1len > b2len)
			return 1;
		else {
			// 长度相等
			for (int i = 0; i < b1len; i++) {
				byte bb1 = b1[startb1 + i];
				byte bb2 = b2[startb2 + i];
				if (bb1 > bb2)
					return 1;
				else if (bb1 < bb2)
					return -1;
				else
					continue;
			}
			return 0;
		}
	}

	public static void main(String[] args) {
		String s1 = "-1234";
		String s2 = "1234";
		byte[] b1 = s1.getBytes();
		byte[] b2 = s2.getBytes();
		System.out.println(compareIntUsingStringBytes(b1, b2));
	}

	public int get_enum_pack_length(int elements) {
		return elements < 256 ? 1 : 2;
	}

	public int get_set_pack_length(int elements) {
		int len = (elements + 7) / 8;
		return len > 4 ? 8 : len;
	}

	public static void initFields(List<Field> fields, List<byte[]> bs) {
		int size = fields.size();
		for (int index = 0; index < size; index++) {
			fields.get(index).setPtr(bs.get(index));
		}
	}

	public static boolean is_temporal_type(FieldTypes valuetype) {
		switch (valuetype) {
		case MYSQL_TYPE_DATE:
		case MYSQL_TYPE_DATETIME:
		case MYSQL_TYPE_TIMESTAMP:
		case MYSQL_TYPE_TIME:
		case MYSQL_TYPE_NEWDATE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Tests if field real type is temporal, i.e. represents all existing
	 * implementations of DATE, TIME, DATETIME or TIMESTAMP types in SQL.
	 * 
	 * @param type
	 *            Field real type, as returned by field->real_type()
	 * @retval true If field real type is temporal
	 * @retval false If field real type is not temporal
	 */
	public static boolean is_temporal_real_type(FieldTypes type) {
		switch (type) {
		case MYSQL_TYPE_TIME2:
		case MYSQL_TYPE_TIMESTAMP2:
		case MYSQL_TYPE_DATETIME2:
			return true;
		default:
			return FieldUtil.is_temporal_type(type);
		}
	}

	public static boolean is_temporal_type_with_time(FieldTypes type) {
		switch (type) {
		case MYSQL_TYPE_TIME:
		case MYSQL_TYPE_DATETIME:
		case MYSQL_TYPE_TIMESTAMP:
			return true;
		default:
			return false;
		}
	}

	public static boolean is_temporal_type_with_date(FieldTypes valuetype) {
		switch (valuetype) {
		case MYSQL_TYPE_DATE:
		case MYSQL_TYPE_DATETIME:
		case MYSQL_TYPE_TIMESTAMP:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Tests if field type is temporal and has date and time parts, i.e.
	 * represents DATETIME or TIMESTAMP types in SQL.
	 * 
	 * @param type
	 *            Field type, as returned by field->type().
	 * @retval true If field type is temporal type with date and time parts.
	 * @retval false If field type is not temporal type with date and time
	 *         parts.
	 */
	public static boolean is_temporal_type_with_date_and_time(FieldTypes type) {
		switch (type) {
		case MYSQL_TYPE_DATETIME:
		case MYSQL_TYPE_TIMESTAMP:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Recognizer for concrete data type (called real_type for some reason),
	 * returning true if it is one of the TIMESTAMP types.
	 */
	public static boolean is_timestamp_type(FieldTypes type) {
		return type == FieldTypes.MYSQL_TYPE_TIMESTAMP || type == FieldTypes.MYSQL_TYPE_TIMESTAMP2;
	}

	/**
	 * Convert temporal real types as retuned by field->real_type() to field
	 * type as returned by field->type().
	 * 
	 * @param real_type
	 *            Real type.
	 * @retval Field type.
	 */
	public static FieldTypes real_type_to_type(FieldTypes real_type) {
		switch (real_type) {
		case MYSQL_TYPE_TIME2:
			return FieldTypes.MYSQL_TYPE_TIME;
		case MYSQL_TYPE_DATETIME2:
			return FieldTypes.MYSQL_TYPE_DATETIME;
		case MYSQL_TYPE_TIMESTAMP2:
			return FieldTypes.MYSQL_TYPE_TIMESTAMP;
		case MYSQL_TYPE_NEWDATE:
			return FieldTypes.MYSQL_TYPE_DATE;
		/* Note: NEWDECIMAL is a type, not only a real_type */
		default:
			return real_type;
		}
	}

	/*
	 * Rules for merging different types of fields in UNION
	 * 
	 * NOTE: to avoid 256*256 table, gap in table types numeration is skiped
	 * following #defines describe that gap and how to canculate number of
	 * fields and index of field in thia array.
	 */
	private static int FIELDTYPE_TEAR_FROM = (FieldTypes.MYSQL_TYPE_BIT.numberValue() + 1);
	private static int FIELDTYPE_TEAR_TO = (FieldTypes.MYSQL_TYPE_NEWDECIMAL.numberValue() - 1);

	// private static int FIELDTYPE_NUM = (FIELDTYPE_TEAR_FROM + (255 -
	// FIELDTYPE_TEAR_TO));

	public static int field_type2index(FieldTypes field_type) {
		field_type = real_type_to_type(field_type);
		assert (field_type.numberValue() < FIELDTYPE_TEAR_FROM || field_type.numberValue() > FIELDTYPE_TEAR_TO);
		return (field_type.numberValue() < FIELDTYPE_TEAR_FROM ? field_type.numberValue()
				: ((int) FIELDTYPE_TEAR_FROM) + (field_type.numberValue() - FIELDTYPE_TEAR_TO) - 1);
	}

	public static FieldTypes field_type_merge(FieldTypes a, FieldTypes b) {
		return field_types_merge_rules[field_type2index(a)][field_type2index(b)];
	}

	private static FieldTypes[][] field_types_merge_rules = new FieldTypes[][] {
			/* enum_field_types.MYSQL_TYPE_DECIMAL -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_DECIMAL, FieldTypes.MYSQL_TYPE_DECIMAL,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_TINY -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_TINY,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_SHORT, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_TINY, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_SHORT -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_SHORT,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_SHORT, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_SHORT, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_SHORT,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_LONG -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_LONG, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_LONG, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_FLOAT -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_FLOAT,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_FLOAT,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_FLOAT,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_DOUBLE -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_NULL -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_TINY,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_SHORT, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_NULL, FieldTypes.MYSQL_TYPE_TIMESTAMP,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_LONGLONG,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_TIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_YEAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_BIT,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_ENUM,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_SET, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_GEOMETRY },
			/* enum_field_types.MYSQL_TYPE_TIMESTAMP -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_TIMESTAMP, FieldTypes.MYSQL_TYPE_TIMESTAMP,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_LONGLONG -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_LONGLONG,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_LONGLONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_LONGLONG,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_INT24 -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_INT24, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_INT24, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_DATE -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_TIME -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_TIME, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_TIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_DATETIME -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_YEAR -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_DECIMAL, FieldTypes.MYSQL_TYPE_TINY,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_SHORT, FieldTypes.MYSQL_TYPE_LONG,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_FLOAT, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_YEAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONGLONG, FieldTypes.MYSQL_TYPE_INT24,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_YEAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_NEWDATE -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_DATETIME,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_DATETIME, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_NEWDATE, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_VARCHAR -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_BIT -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_BIT, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_BIT,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_NEWDECIMAL -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_DOUBLE, FieldTypes.MYSQL_TYPE_DOUBLE,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_NEWDECIMAL,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_NEWDECIMAL, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_ENUM -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_ENUM, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_SET -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_SET, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_TINY_BLOB -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_TINY_BLOB, FieldTypes.MYSQL_TYPE_TINY_BLOB },
			/* enum_field_types.MYSQL_TYPE_MEDIUM_BLOB -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_MEDIUM_BLOB },
			/* enum_field_types.MYSQL_TYPE_LONG_BLOB -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_LONG_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB },
			/* enum_field_types.MYSQL_TYPE_BLOB -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_BLOB },
			/* enum_field_types.MYSQL_TYPE_VAR_STRING -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR },
			/* enum_field_types.MYSQL_TYPE_STRING -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_STRING },
			/* enum_field_types.MYSQL_TYPE_GEOMETRY -> */
			{
					// enum_field_types.MYSQL_TYPE_DECIMAL
					// enum_field_types.MYSQL_TYPE_TINY
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SHORT
					// enum_field_types.MYSQL_TYPE_LONG
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_FLOAT
					// enum_field_types.MYSQL_TYPE_DOUBLE
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NULL
					// enum_field_types.MYSQL_TYPE_TIMESTAMP
					FieldTypes.MYSQL_TYPE_GEOMETRY, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_LONGLONG
					// enum_field_types.MYSQL_TYPE_INT24
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATE
					// enum_field_types.MYSQL_TYPE_TIME
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_DATETIME
					// enum_field_types.MYSQL_TYPE_YEAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDATE
					// enum_field_types.MYSQL_TYPE_VARCHAR
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_BIT <16>-<245>
					FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_NEWDECIMAL
					// enum_field_types.MYSQL_TYPE_ENUM
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_SET
					// enum_field_types.MYSQL_TYPE_TINY_BLOB
					FieldTypes.MYSQL_TYPE_VARCHAR, FieldTypes.MYSQL_TYPE_TINY_BLOB,
					// enum_field_types.MYSQL_TYPE_MEDIUM_BLOB
					// enum_field_types.MYSQL_TYPE_LONG_BLOB
					FieldTypes.MYSQL_TYPE_MEDIUM_BLOB, FieldTypes.MYSQL_TYPE_LONG_BLOB,
					// enum_field_types.MYSQL_TYPE_BLOB
					// enum_field_types.MYSQL_TYPE_VAR_STRING
					FieldTypes.MYSQL_TYPE_BLOB, FieldTypes.MYSQL_TYPE_VARCHAR,
					// enum_field_types.MYSQL_TYPE_STRING
					// enum_field_types.MYSQL_TYPE_GEOMETRY
					FieldTypes.MYSQL_TYPE_STRING, FieldTypes.MYSQL_TYPE_GEOMETRY } };
}