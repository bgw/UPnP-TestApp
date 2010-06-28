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

package org.teleal.cling.transport.impl;

import java.util.logging.Logger;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.InvalidHeaderException;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.MANHeader;
import org.teleal.cling.model.message.UpnpHeaders;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Although we only read the "first" header value for a given header name, we write all of them.
 *
 */
public class HttpHeaderConverter {

    private static Logger log = Logger.getLogger(HttpHeaderConverter.class.getName());

    public static UpnpHeaders convertHeaders(HttpHeaders rawHeaders)
            throws InstantiationException, IllegalAccessException, InvalidHeaderException {

        UpnpHeaders headers = new UpnpHeaders();

        log.fine("Converting HTTP message headers into UPnP message headers: " + rawHeaders.size());

        HttpHeaders knownHeaders = new HttpHeaders();
        HttpHeaders unknownHeaders = new HttpHeaders();

        for (Map.Entry<String, List<String>> entry : rawHeaders.entrySet()) {

            log.fine("Converting raw HTTP header: " + entry.getKey());

            String rawHeaderName = entry.getKey();

            // Fantastic, the JDK HTTPURLConnection returns "null" headers
            if (rawHeaderName == null) {
                log.finer("Skipping null...");
                continue;
            }

            String rawHeaderValue = rawHeaders.getFirstHeader(rawHeaderName);

            UpnpHeader.Type upnpHeaderType = UpnpHeader.Type.getByHttpName(rawHeaderName.toUpperCase());

            if (upnpHeaderType == null) {
                log.finer("Will try later to parse unknown request header '" + rawHeaderName + "' with value '" + rawHeaderValue + "'");
                unknownHeaders.put(rawHeaderName, rawHeaders.get(rawHeaderName));
            } else {
                knownHeaders.put(rawHeaderName, rawHeaders.get(rawHeaderName));
            }
        }

        for (Map.Entry<String, List<String>> entry : knownHeaders.entrySet()) {

            String headerName = entry.getKey();
            String headerValue = knownHeaders.getFirstHeader(headerName);

            UpnpHeader.Type upnpHeaderType = UpnpHeader.Type.getByHttpName(headerName.toUpperCase());
            UpnpHeader upnpHeader = createUpnpHeader(upnpHeaderType, headerValue);

            if (upnpHeader == null || upnpHeader.getValue() == null) {
                log.fine("Ignoring known but non-parsable header (value violates the UDA specification?) '"+ headerName + "': " + headerValue);
            } else {
                log.fine("Adding parsed header to UPnP message: " + upnpHeader);
                headers.add(upnpHeaderType, upnpHeader);
            }
        }

        Map<String, String> headerNamespaces = new HashMap();
        if (headers.get(UpnpHeader.Type.MAN) != null) {
            for (UpnpHeader h : headers.get(UpnpHeader.Type.MAN)) {
                MANHeader manHeader = (MANHeader)h;
                if (manHeader.getNamespace() != null) {
                    headerNamespaces.put(manHeader.getNamespace(), manHeader.getValue());
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : unknownHeaders.entrySet()) {

            String headerName = entry.getKey();
            String headerValue = unknownHeaders.getFirstHeader(headerName);

            if (!headerName.matches("[0-9]{2}-.+")) {
                log.fine("Ignorning unknown header: '"+ headerName + "': " + headerValue);
                continue;
            }

            String prefix = headerName.substring(0, 2);
            headerName = headerName.substring(3);

            if (!headerNamespaces.containsKey(prefix)) {
                log.fine("Ignorning extension header with unknown namespace prefix: '"+ headerName + "': " + headerValue);
                continue;
            }

            UpnpHeader.Type upnpHeaderType = UpnpHeader.Type.getByHttpName(headerName.toUpperCase());
            if (upnpHeaderType == null) {
                log.fine("Unknown namespaced request header: " + headerName + ", value: " + headerValue);
                continue;
            }

            UpnpHeader upnpHeader = createUpnpHeader(upnpHeaderType, headerValue);

            if (upnpHeader == null || upnpHeader.getValue() == null) {
                log.fine("Ignoring known but non-parsable namespaced header (value violates the UDA specification?) '"+ headerName + "': " + headerValue);
            } else {
                log.fine("Adding namespaced parsed header to UPnP message with prefix '"+prefix+"': " + upnpHeader);
                headers.add(upnpHeaderType, upnpHeader);
                headers.setPrefix(upnpHeaderType, prefix);
            }
        }

        return headers;
    }

    protected static UpnpHeader createUpnpHeader(UpnpHeader.Type upnpHeaderType, String headerValue)
            throws InstantiationException, IllegalAccessException, InvalidHeaderException {

        // Try all the UPnP headers and see if one matches our value parsers
        UpnpHeader upnpHeader = null;
        for (int i = 0; i < upnpHeaderType.getHeaderClasses().length && upnpHeader == null; i++) {
            Class<? extends UpnpHeader> headerClass = upnpHeaderType.getHeaderClasses()[i];
            try {
                log.finer("Trying to parse header '" + upnpHeaderType + "' with header class: " + headerClass.getSimpleName());
                upnpHeader = headerClass.newInstance();
                if (headerValue != null) {
                    upnpHeader.setString(headerValue);
                }
            } catch (InvalidHeaderException ex) {
                log.finer("Invalid header value for tested type: " + headerClass.getSimpleName() + " - " + ex.getMessage());
                upnpHeader = null;
            }
        }
        return upnpHeader;
    }

    public static void writeHeaders(UpnpHeaders upnpHeaders, HttpHeaders rawHeaders) {
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : upnpHeaders.entrySet()) {
            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                log.fine("Writing HTTP header '" + headerEntry.getKey().getHttpName() + ": " + upnpHeader.getString() + "'");
                rawHeaders.add(upnpHeaders.getPrefixedHttpName(headerEntry.getKey()), upnpHeader.getString());
            }
        }
    }

    public static void writeHeaders(UpnpHeaders upnpHeaders, HttpURLConnection urlConnection) {
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : upnpHeaders.entrySet()) {
            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                log.fine("Writing HTTP header '" + headerEntry.getKey().getHttpName() + ": " + upnpHeader.getString() + "'");
                urlConnection.setRequestProperty(upnpHeaders.getPrefixedHttpName(headerEntry.getKey()), upnpHeader.getString());
            }
        }
    }

    public static boolean containsTextContentType(Map<UpnpHeader.Type, List<UpnpHeader>> headers) {
        List<UpnpHeader> contentTypeHeaders = headers.get(UpnpHeader.Type.CONTENT_TYPE);
        if (contentTypeHeaders == null) return true;
        for (UpnpHeader upnpHeader : contentTypeHeaders) {
            if (upnpHeader instanceof ContentTypeHeader) {
                ContentTypeHeader contentTypeHeader = (ContentTypeHeader) upnpHeader;
                if (contentTypeHeader.isText()) return true;
            }
        }
        return false;
    }

    public static String convertHeaders(UpnpHeaders headers) {
        StringBuilder headerString = new StringBuilder();
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : headers.entrySet()) {
            StringBuilder headerLine = new StringBuilder();

            headerLine.append(headers.getPrefixedHttpName(headerEntry.getKey())).append(": ");

            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                headerLine.append(upnpHeader.getString()).append(",");
            }
            headerLine.delete(headerLine.length()-1, headerLine.length());
            headerString.append(headerLine).append("\r\n");
        }
        return headerString.toString();
    }

}
