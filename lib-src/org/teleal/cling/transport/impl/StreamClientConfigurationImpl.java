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

import org.teleal.cling.transport.spi.StreamClientConfiguration;
import org.teleal.cling.model.ServerClientTokens;


public class StreamClientConfigurationImpl implements StreamClientConfiguration {

    // By default we don't need to optimize for HTTP performance, rather prevent obscure bugs
    private boolean usePersistentConnections = false;

    // Timeout until connection is established
    private int connectionTimeoutSeconds = 10;

    // Timeout waiting for data during read
    private int dataReadTimeoutSeconds = 10;

    public boolean isUsePersistentConnections() {
        return usePersistentConnections;
    }

    public void setUsePersistentConnections(boolean usePersistentConnections) {
        this.usePersistentConnections = usePersistentConnections;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getDataReadTimeoutSeconds() {
        return dataReadTimeoutSeconds;
    }

    public void setDataReadTimeoutSeconds(int dataReadTimeoutSeconds) {
        this.dataReadTimeoutSeconds = dataReadTimeoutSeconds;
    }

    public String getUserAgentOS() {
        return new ServerClientTokens().getOsToken();
    }

    public String getUserAgentProduct() {
        return new ServerClientTokens().getProductToken();
    }
    
}
