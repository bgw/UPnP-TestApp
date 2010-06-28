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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class TimeDatatype extends AbstractDatatype<Long> {

    protected String[] readFormats;
    protected String writeFormat;

    public TimeDatatype(String[] readFormats, String writeFormat) {
        this.readFormats = readFormats;
        this.writeFormat = writeFormat;
    }

    public Long fromString(String s) throws InvalidValueException {
        if (s.equals("")) return null;

        Date d = getDateValue(s, readFormats);
        if (d == null) {
            throw new InvalidValueException("Can't parse date/time from: " + s);
        }

        Calendar c = Calendar.getInstance(getTimeZone());
        c.setTime(d);

        if (readFormats[0].equals("HH:mm:ssZ") && (getTimeZone().inDaylightTime(d)))
            c.add(Calendar.MILLISECOND, 3600000);

        return (long) c.get(Calendar.HOUR_OF_DAY) * 3600000 + c.get(Calendar.MINUTE) * 60000 + c.get(Calendar.SECOND) * 1000;
    }

    @Override
    public boolean isValid(Long value) {
        return value > 0;
    }

    @Override
    public String getString(Long value) throws InvalidValueException {
        if (value == null) return "";

        if (!isValid(value)) {
            throw new InvalidValueException("Not valid timestamp: " + value);
        }

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, (int) (value / 3600000));
        int x = (int) (value % 3600000);
        c.set(Calendar.MINUTE, x / 60000);
        c.set(Calendar.SECOND, (x % 60000) / 1000);

        SimpleDateFormat sdt = new SimpleDateFormat(writeFormat);
        sdt.setTimeZone(getTimeZone());
        return sdt.format(c.getTime());
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

    protected Date getDateValue(String value, String[] formats) {

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
    }

    protected TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public Long getDefaultValue() {
        return new Date().getTime();
    }
}
