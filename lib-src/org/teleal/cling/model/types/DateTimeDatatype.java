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

/*
 * This file is licensed to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package org.teleal.cling.model.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class DateTimeDatatype extends AbstractDatatype<Date> {

    protected String[] readFormats;
    protected String writeFormat;

    public DateTimeDatatype(String[] readFormats, String writeFormat) {
        this.readFormats = readFormats;
        this.writeFormat = writeFormat;
    }

    public Date fromString(String s) throws InvalidValueException {
        if (s.equals("")) return null;
        Date d = getDateValue(s, readFormats);
        if (d == null) {
            throw new InvalidValueException("Can't parse date/time from: " + s);
        }
        return d;
    }

    @Override
    public String getString(Date value) throws InvalidValueException {
        if (value == null) return "";
        SimpleDateFormat sdt = new SimpleDateFormat(writeFormat);
        sdt.setTimeZone(getTimeZone());
        return sdt.format(value);
    }

    protected String normalizeTimeZone(String value) {
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1) + "+0000";
        } else if ((value.length() > 7)
                && (value.charAt(value.length() - 3) == ':')
                && ((value.charAt(value.length() - 6) == '-') || (value.charAt(value.length() - 6) == '+'))) {

            value = value.substring(0, value.length() - 3) + value.substring(value.length() - 2);
        }
        return value;
    }

    protected Date getDateValue(String value, String[] formats)  {

        value = normalizeTimeZone(value);

        Date d = null;
        for (String format : formats) {
            SimpleDateFormat sdt = new SimpleDateFormat(format);
            sdt.setTimeZone(getTimeZone());
            try {
                d = sdt.parse(value);
            } catch (ParseException ex) {
                // Just continue
            }
        }
        return d;

        /* I have no idea what this code does... and I don't want to know!
        ParsePosition position = null;
        Date d;
        value = normalizeTimeZone(value);
        for (String timeFormat : formats) {
            position = new ParsePosition(0);
            SimpleDateFormat sdt = new SimpleDateFormat(timeFormat);
            d = sdt.parse(value, position);
            if (d != null) {
                if (position.getIndex() >= value.length()) {
                    choosedIndex[0] = timeFormat;
                    return d;
                }
            }
        }
        throw new ParseException("Error parsing date/time: " + value);
        */
    }

    protected TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public Date getDefaultValue() {
        return new Date();
    }
}
