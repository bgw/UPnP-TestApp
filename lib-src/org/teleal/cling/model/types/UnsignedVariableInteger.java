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

/**
 * TODO: A temporary solution for unsigned "non-negative" types in UPnP, hopefully not used for any arithmetic
 */
public abstract class UnsignedVariableInteger {

    public enum Bits {
        EIGHT(0xffL),
        SIXTEEN(0xffffL),
        TWENTYFOUR(0xffffffL),
        THIRTYTWO(0xffffffffL);

        private long maxValue;

        Bits(long maxValue) {
            this.maxValue = maxValue;
        }

        public long getMaxValue() {
            return maxValue;
        }
    }

    protected long value;

    protected UnsignedVariableInteger() {
    }

    public UnsignedVariableInteger(long value) throws NumberFormatException {
        setValue(value);
    }

    public UnsignedVariableInteger(String s) throws NumberFormatException {
        if (s.startsWith("-")) {
            throw new NumberFormatException("Can't convert String with signed value to integer");
        }
        setValue(Long.parseLong(s));
    }

    protected UnsignedVariableInteger setValue(long value) {
        isInRange(value);
        this.value = value;
        return this;
    }

    public Long getValue() {
        return value;
    }

    public void isInRange(long value) throws NumberFormatException {
        if (value < getMinValue() || value > getBits().getMaxValue()) {
            throw new NumberFormatException("Value must be between " + getMinValue() + " and " + getBits().getMaxValue() + ": " + value);
        }
    }

    public int getMinValue() {
        return 0;
    }

    public abstract Bits getBits();

    public UnsignedVariableInteger increment(boolean rolloverToOne) {
        if (value + 1 > getBits().getMaxValue()) {
            value = rolloverToOne ? 1 : 0;
        } else {
            value++;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnsignedVariableInteger that = (UnsignedVariableInteger) o;

        if (value != that.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

}
