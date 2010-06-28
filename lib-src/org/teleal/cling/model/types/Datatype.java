/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model.types;

import java.util.Date;


public interface Datatype<V> {

    public static enum Default {

        BOOLEAN(Boolean.class, Builtin.BOOLEAN),
        BOOLEAN_PRIMITIVE(Boolean.TYPE, Builtin.BOOLEAN),
        SHORT(Short.class, Builtin.I2),
        SHORT_PRIMITIVE(Short.TYPE, Builtin.BOOLEAN),
        INTEGER(Integer.class, Builtin.I4),
        INTEGER_PRIMITIVE(Integer.TYPE, Builtin.I4),
        UNSIGNED_INTEGER_ONE_BYTE(UnsignedIntegerOneByte.class, Builtin.UI1),
        UNSIGNED_INTEGER_TWO_BYTES(UnsignedIntegerTwoBytes.class, Builtin.UI2),
        UNSIGNED_INTEGER_FOUR_BYTES(UnsignedIntegerFourBytes.class, Builtin.UI4),
        FLOAT(Float.class, Builtin.R4),
        FLOAT_PRIMITIVE(Float.TYPE, Builtin.R4),
        DOUBLE(Double.class, Builtin.FLOAT),
        DOUBLE_PRIMTIIVE(Double.TYPE, Builtin.FLOAT),
        CHAR(Character.class, Builtin.CHAR),
        CHAR_PRIMITIVE(Character.TYPE, Builtin.CHAR),
        STRING(String.class, Builtin.STRING),
        DATE(Date.class, Builtin.DATETIME),
        BYTES(Byte[].class, Builtin.BIN_BASE64),
        URI(java.net.URI.class, Builtin.URI);

        private Class javaType;
        private Builtin builtinType;

        Default(Class javaType, Builtin builtinType) {
            this.javaType = javaType;
            this.builtinType = builtinType;
        }

        public Class getJavaType() {
            return javaType;
        }

        public Builtin getBuiltinType() {
            return builtinType;
        }

        public static Default getByJavaType(Class javaType) {
            for (Default d : Default.values()) {
                if (d.getJavaType().equals(javaType)) {
                    return d;
                }
            }
            return null;
        }
    }

    public static enum Builtin {

        UI1("ui1", UnsignedIntegerOneByte.class, new UnsignedIntegerOneByteDatatype()),
        UI2("ui2", UnsignedIntegerTwoBytes.class, new UnsignedIntegerTwoBytesDatatype()),
        UI4("ui4", UnsignedIntegerFourBytes.class, new UnsignedIntegerFourBytesDatatype()),
        I1("i1", Integer.class, new IntegerDatatype(1)),
        I2("i2", Integer.class, new IntegerDatatype(2)),
        I4("i4", Integer.class, new IntegerDatatype(4)),
        INT("int", Integer.class, new IntegerDatatype(4)),
        R4("r4", Float.class, new FloatDatatype()),
        R8("r8", Double.class, new DoubleDatatype()),
        NUMBER("number", Double.class, new DoubleDatatype()),
        FIXED144("fixed.14.4", Double.class, new DoubleDatatype()),
        FLOAT("float", Double.class, new DoubleDatatype()), // TODO: Is that Double or Float?
        CHAR("char", Character.class, new CharacterDatatype()),
        STRING("string", String.class, new StringDatatype()),
        DATE("date", Date.class, new DateTimeDatatype(
                new String[]{"yyyy-MM-dd"},
                "yyyy-MM-dd"
        )),
        DATETIME("dateTime", Date.class, new DateTimeDatatype(
                new String[]{"yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss"},
                "yyyy-MM-dd'T'HH:mm:ss"
        )),
        DATETIME_TZ("dateTime.tz", Date.class, new DateTimeDatatype(
                new String[]{"yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssZ"},
                "yyyy-MM-dd'T'HH:mm:ssZ"
        )),
        TIME("time", Long.class, new TimeDatatype(
                new String[]{"HH:mm:ss"},
                "HH:mm:ss"
        )),
        TIME_TZ("time.tz", Long.class, new TimeDatatype(
                new String[]{"HH:mm:ssZ", "HH:mm:ss"},
                "HH:mm:ssZ"
        )),
        BOOLEAN("boolean", Boolean.class, new BooleanDatatype()),
        BIN_BASE64("bin.base64", Byte[].class, new Base64Datatype()),
        BIN_HEX("bin.hex", Byte[].class, new BinHexDatatype()),
        URI("uri", java.net.URI.class, new URIDatatype()),
        UUID("uuid", String.class, new StringDatatype());

        private String descriptorName;
        private Class valuetype;
        private Datatype datatype;

        <VT> Builtin(String descriptorName, Class<VT> valuetype, Datatype<VT> datatype) {
            this.descriptorName = descriptorName;
            this.valuetype = valuetype;
            this.datatype = datatype;
            this.datatype.setBuiltin(this);
        }

        public String getDescriptorName() {
            return descriptorName;
        }

        public Class getValuetype() {
            return valuetype;
        }

        public Datatype getDatatype() {
            return datatype;
        }

        public static Builtin getByDescriptorName(String descriptorName) {
            for (Builtin t : Builtin.values()) {
                if (t.getDescriptorName().equals(descriptorName)) {
                    return t;
                }
            }
            return null;
        }

        public static boolean isNumeric(Builtin builtin) {
            return builtin != null &&
                    (builtin.equals(UI1) ||
                            builtin.equals(UI2) ||
                            builtin.equals(UI4) ||
                            builtin.equals(I1) ||
                            builtin.equals(I2) ||
                            builtin.equals(I4));
        }
    }

    public Class<V> getValueType();

    public void setBuiltin(Builtin builtin);

    public Builtin getBuiltin();

    public boolean isValid(V value);

    // Must return "" empty string when value is null, should call isValid() before it transforms
    public String getString(V value) throws InvalidValueException;
    // Must return null when String is empty ""
    public V fromString(String s) throws InvalidValueException;

    public String getDisplayString();

    public V getDefaultValue();


}
