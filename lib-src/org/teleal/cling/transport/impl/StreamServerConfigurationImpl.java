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

import org.teleal.cling.model.ServerClientTokens;
import org.teleal.cling.transport.spi.StreamServerConfiguration;


public class StreamServerConfigurationImpl implements StreamServerConfiguration {

    private int listenPort;

    // This is the maximum number of queued incoming connections to allow on the listening socket.
    // Queued TCP connections exceeding this limit may be rejected by the TCP implementation.
    private int tcpConnectionBacklog;

    public StreamServerConfigurationImpl(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getTcpConnectionBacklog() {
        return tcpConnectionBacklog;
    }

    public void setTcpConnectionBacklog(int tcpConnectionBacklog) {
        this.tcpConnectionBacklog = tcpConnectionBacklog;
    }

    public String getServerOriginOS() {
        return new ServerClientTokens().getOsToken();
    }

    public String getServerOriginProduct() {
        return new ServerClientTokens().getProductToken();
    }



}