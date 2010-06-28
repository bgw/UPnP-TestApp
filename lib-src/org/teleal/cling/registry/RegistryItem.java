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

package org.teleal.cling.registry;

import java.util.Date;


class RegistryItem<K, I> {

    private K key;
    private I item;
    private int maxAgeSeconds;
    private long lastRefreshTimestampSeconds = getCurrentTimestampSeconds();

    RegistryItem(K key) {
        this.key = key;
    }

    RegistryItem(K key, I item, int maxAgeSeconds) {
        this.key = key;
        this.item = item;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public K getKey() {
        return key;
    }

    public I getItem() {
        return item;
    }

    public int getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public long getLastRefreshTimestampSeconds() {
        return lastRefreshTimestampSeconds;
    }

    public void setLastRefreshTimestampSeconds(long lastRefreshTimestampSeconds) {
        this.lastRefreshTimestampSeconds = lastRefreshTimestampSeconds;
    }

    public void stampLastRefresh() {
        setLastRefreshTimestampSeconds(getCurrentTimestampSeconds());
    }

    public boolean hasExpired(boolean halfTime) {
        return (getLastRefreshTimestampSeconds() + (getMaxAgeSeconds()/(halfTime ? 2 : 1))) < getCurrentTimestampSeconds();
    }

    public long getSecondsUntilExpiration() {
        return (getLastRefreshTimestampSeconds() + getMaxAgeSeconds()) - getCurrentTimestampSeconds();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistryItem that = (RegistryItem) o;

        return key.equals(that.key);
    }

    public int hashCode() {
        return key.hashCode();
    }

    protected long getCurrentTimestampSeconds() {
        return new Date().getTime()/1000;
    }

    @Override
    public String toString() {
        return "("+getClass().getSimpleName()+") MAX AGE: " + getMaxAgeSeconds() + " KEY: " + getKey() + " ITEM: " + getItem();
    }
}
