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

package org.teleal.cling.model.message;

import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.MANHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.common.http.Headers;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpnpHeaders implements Map<UpnpHeader.Type, List<UpnpHeader>> {

    private static Logger log = Logger.getLogger(UpnpHeaders.class.getName());

    // TODO: I forgot why I did this... obviously it was necessary to have namespaces here, but why?
    private Map<UpnpHeader.Type, String> prefixes = new HashMap();

    private Map<UpnpHeader.Type, List<UpnpHeader>> map;

    public UpnpHeaders() {
        this.map = new LinkedHashMap<UpnpHeader.Type, List<UpnpHeader>>();
    }

    public UpnpHeaders(Map<UpnpHeader.Type, List<UpnpHeader>> map) {
        this.map = map;
    }

    public UpnpHeaders(Headers headers) {
        this.map = toUpnpHeaders(headers);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return key != null && key instanceof UpnpHeader.Type && map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public List<UpnpHeader> get(Object o) {
        return map.get(o);
    }

    public List<UpnpHeader> put(UpnpHeader.Type key, List<UpnpHeader> value) {
        return map.put(key, value);
    }

    public List<UpnpHeader> remove(Object key) {
        prefixes.remove(key);
        return map.remove(key);
    }

    public void putAll(Map<? extends UpnpHeader.Type, ? extends List<UpnpHeader>> map) {
        this.map.putAll(map);
    }

    public void clear() {
        prefixes.clear();
        map.clear();
    }

    public Set<UpnpHeader.Type> keySet() {
        return map.keySet();
    }

    public Collection<List<UpnpHeader>> values() {
        return map.values();
    }

    public Set<Entry<UpnpHeader.Type, List<UpnpHeader>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public void add(UpnpHeader.Type key, UpnpHeader value) {
        List<UpnpHeader> valuesForHeader = get(key);
        if (valuesForHeader == null) {
            valuesForHeader = new ArrayList<UpnpHeader>();
            put(key, valuesForHeader);
        }
        valuesForHeader.add(value);
    }

    public UpnpHeader[] getAsArray(UpnpHeader.Type type) {
        return get(type) != null
                ? get(type).toArray(new UpnpHeader[get(type).size()])
                : new UpnpHeader[0];
    }

    public UpnpHeader getFirstHeader(UpnpHeader.Type type) {
        return getAsArray(type).length > 0
                ? getAsArray(type)[0]
                : null;
    }

    public <H extends UpnpHeader> H getFirstHeader(UpnpHeader.Type type, Class<H> subtype) {
        UpnpHeader[] headers = getAsArray(type);
        if (headers.length == 0) return null;

        for (UpnpHeader header : headers) {
            if (header.getClass().isAssignableFrom(subtype)) {
                return (H)header;
            }
        }
        return null;
    }

    public void setPrefix(UpnpHeader.Type key, String prefix) {
        prefixes.put(key, prefix);
    }

    public String getPrefix(UpnpHeader.Type key) {
        return prefixes.get(key);
    }

    public String getPrefixedHttpName(UpnpHeader.Type key) {
        return getPrefix(key) != null  ? prefixes.get(key) + "-" + key.getHttpName() : key.getHttpName();
    }

    public boolean containsTextContentType() {
        List<UpnpHeader> contentTypeHeaders = get(UpnpHeader.Type.CONTENT_TYPE);
        if (contentTypeHeaders == null) return true;
        for (UpnpHeader upnpHeader : contentTypeHeaders) {
            if (upnpHeader instanceof ContentTypeHeader) {
                ContentTypeHeader contentTypeHeader = (ContentTypeHeader) upnpHeader;
                if (contentTypeHeader.isText()) return true;
            }
        }
        return false;
    }

    public Headers toHttpHeaders() {
        Headers httpHeaders = new Headers();
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : entrySet()) {
            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                httpHeaders.add(
                        getPrefixedHttpName(headerEntry.getKey()),
                        upnpHeader.getString()
                );
            }
        }
        return httpHeaders;
    }

    public void applyTo(HttpURLConnection urlConnection) {
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : entrySet()) {
            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                urlConnection.setRequestProperty(getPrefixedHttpName(headerEntry.getKey()), upnpHeader.getString());
            }
        }
    }

    public String toString() {
        StringBuilder headerString = new StringBuilder();
        for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> headerEntry : entrySet()) {
            StringBuilder headerLine = new StringBuilder();

            headerLine.append(getPrefixedHttpName(headerEntry.getKey())).append(": ");

            for (UpnpHeader upnpHeader : headerEntry.getValue()) {
                headerLine.append(upnpHeader.getString()).append(",");
            }
            headerLine.delete(headerLine.length()-1, headerLine.length());
            headerString.append(headerLine).append("\r\n");
        }
        return headerString.toString();
    }

    protected UpnpHeaders toUpnpHeaders(Headers rawHeaders) {

        UpnpHeaders headers = new UpnpHeaders();

        log.fine("Converting HTTP message headers into UPnP message headers: " + rawHeaders.size());

        Headers knownHeaders = new Headers();
        Headers unknownHeaders = new Headers();

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
            UpnpHeader upnpHeader = UpnpHeader.newInstance(upnpHeaderType, headerValue);

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
                log.fine("Ignoring unknown header: '"+ headerName + "': " + headerValue);
                continue;
            }

            String prefix = headerName.substring(0, 2);
            headerName = headerName.substring(3);

            if (!headerNamespaces.containsKey(prefix)) {
                log.fine("Ignoring extension header with unknown namespace prefix: '"+ headerName + "': " + headerValue);
                continue;
            }

            UpnpHeader.Type upnpHeaderType = UpnpHeader.Type.getByHttpName(headerName.toUpperCase());
            if (upnpHeaderType == null) {
                log.fine("Unknown namespaced request header: " + headerName + ", value: " + headerValue);
                continue;
            }

            UpnpHeader upnpHeader = UpnpHeader.newInstance(upnpHeaderType, headerValue);

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

    public void logHeaders() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("########################## HEADERS ##########################");
            for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> entry : entrySet()) {
                log.fine("=== TYPE: " + entry.getKey());
                for (UpnpHeader upnpHeader : entry.getValue()) {
                    log.fine("HEADER: " + upnpHeader);
                }
            }
            log.fine("#############################################################");
        }
    }

}
