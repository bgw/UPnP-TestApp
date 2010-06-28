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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unifies HttpExchange.getRequestHeaders(), HttpExchange.setResponseHeaders(), and HttpURLConnection.getHeaderFields().
 */
public class HttpHeaders implements Map<String, List<String>> {

    final Map<String, List<String>> map = new HashMap<String, List<String>>(32);

    public HttpHeaders(com.sun.net.httpserver.Headers headers) {
        putAll(headers);
    }

    public HttpHeaders(Map<String, List<String>> map) {
        putAll(map);
    }

    public HttpHeaders() {
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return key != null && key instanceof String && map.containsKey(normalize((String) key));
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public List<String> get(Object key) {
        return map.get(normalize((String) key));
    }

    public List<String> put(String key, List<String> value) {
        return map.put(normalize(key), value);
    }

    public List<String> remove(Object key) {
        return map.remove(normalize((String) key));
    }

    public void putAll(Map<? extends String, ? extends List<String>> t) {
        // Enforce key normalization!
        for (Entry<? extends String, ? extends List<String>> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<List<String>> values() {
        return map.values();
    }

    public Set<Map.Entry<String, List<String>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public String getFirstHeader(String key) {
        List<String> l = map.get(normalize(key));
        if (l == null) {
            return null;
        }
        return l.get(0);
    }

    public void add(String key, String value) {
        String k = normalize(key);
        List<String> l = map.get(k);
        if (l == null) {
            l = new LinkedList<String>();
            map.put(k, l);
        }
        l.add(value);
    }

    public void set(String key, String value) {
        LinkedList<String> l = new LinkedList<String>();
        l.add(value);
        put(key, l);
    }

    private String normalize(String key) {
        if (key == null) return null;
        if (key.length() == 0) return key;
        char[] b;
        String s;
        b = key.toCharArray();
        if (b[0] >= 'a' && b[0] <= 'z') {
            b[0] = (char) (b[0] - ('a' - 'A'));
        }
        for (int i = 1; i < key.length(); i++) {
            if (b[i] >= 'A' && b[i] <= 'Z') {
                b[i] = (char) (b[i] + ('a' - 'A'));
            }
        }
        return new String(b);
    }
    
}