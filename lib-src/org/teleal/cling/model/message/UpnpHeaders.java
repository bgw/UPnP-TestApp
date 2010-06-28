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

import org.teleal.cling.model.message.header.UpnpHeader;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;


public class UpnpHeaders implements Map<UpnpHeader.Type, List<UpnpHeader>> {

    private Map<UpnpHeader.Type, String> prefixes = new HashMap();

    private Map<UpnpHeader.Type, List<UpnpHeader>> map;

    public UpnpHeaders() {
        this.map = new LinkedHashMap<UpnpHeader.Type, List<UpnpHeader>>();
    }

    public UpnpHeaders(Map<UpnpHeader.Type, List<UpnpHeader>> map) {
        this.map = map;
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
}
