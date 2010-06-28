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

package org.teleal.cling.model;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

public class ModelUtil {

    public static boolean isStringConvertibleType(Set<Class> stringConvertibleTypes, Class clazz) {
        if (clazz.isEnum()) return true;
        for (Class toStringOutputType : stringConvertibleTypes) {
            if (toStringOutputType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidUDAName(String name) {
        return (name != null && name.length() != 0 && !name.toLowerCase().startsWith("XML") && name.matches(Constants.REGEX_UDA_NAME));
    }

    public static InetAddress getInetAddressByName(String name) {
        try {
            return InetAddress.getByName(name);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toCommaSeparatedList(Object[] o) {
        return toCommaSeparatedList(o, true, false);
    }

    public static String toCommaSeparatedList(Object[] o, boolean escapeCommas, boolean escapeDoubleQuotes) {
        if (o == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object obj : o) {
            String objString = obj.toString();
            objString = objString.replaceAll("\\\\", "\\\\\\\\"); // Replace one backslash with two (nice, eh?)
            if (escapeCommas) {
                objString = objString.replaceAll(",", "\\\\,");
            }
            if (escapeDoubleQuotes) {
                objString = objString.replaceAll("\"", "\\\"");
            }
            sb.append(objString).append(",");
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();

    }

    public static String[] fromCommaSeparatedList(String s) {
        return fromCommaSeparatedList(s, true);
    }

    public static String[] fromCommaSeparatedList(String s, boolean unescapeCommas) {
        if (s == null || s.length() == 0) {
            return null;
        }

        final String QUOTED_COMMA_PLACEHOLDER = "XXX1122334455XXX";
        if (unescapeCommas) {
            s = s.replaceAll("\\\\,", QUOTED_COMMA_PLACEHOLDER);
        }

        String[] split = s.split(",");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].replaceAll(QUOTED_COMMA_PLACEHOLDER, ",");
            split[i] = split[i].replaceAll("\\\\\\\\", "\\\\");
        }
        return split;
    }

    public static String toTimeString(long seconds) {
        long hours = seconds / 3600,
                remainder = seconds % 3600,
                minutes = remainder / 60,
                secs = remainder % 60;

        return ((hours < 10 ? "0" : "") + hours
                + ":" + (minutes < 10 ? "0" : "") + minutes
                + ":" + (secs < 10 ? "0" : "") + secs);
    }

    public static long fromTimeString(String s) {
        String[] split = s.split(":");
        return (Long.parseLong(split[0]) * 3600) +
                (Long.parseLong(split[1]) * 60) +
                (Long.parseLong(split[2]));
    }

    public static String commaToNewline(String s) {
        StringBuilder sb = new StringBuilder();
        String[] split = s.split(",");
        for (String splitString : split) {
            sb.append(splitString).append(",").append("\n");
        }
        if (sb.length() > 2) {
            sb.deleteCharAt(sb.length() - 2);
        }
        return sb.toString();
    }

    public static String getLocalHostName(boolean includeDomain) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return includeDomain
                    ? hostname
                    : hostname.indexOf(".") != -1 ? hostname.substring(0, hostname.indexOf(".")) : hostname;

        } catch (Exception ex) {
            // Return a dummy String
            return "UNKNOWN HOST";
        }
    }

    public static byte[] getFirstNetworkInterfaceHardwareAddress() {
        try {
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
                if (!iface.isLoopback() && iface.isUp() && iface.getHardwareAddress() != null) {
                    return iface.getHardwareAddress();
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not discover first network interface hardware address");
        }
        throw new RuntimeException("Could not discover first network interface hardware address");
    }




}
