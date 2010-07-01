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

package org.teleal.cling.transport.impl.apache;

import org.teleal.cling.model.ServerClientTokens;
import org.teleal.cling.transport.spi.StreamServerConfiguration;

/**
 * @author Christian Bauer
 */
public class StreamServerConfigurationImpl implements StreamServerConfiguration {

    private int listenPort;

    // Defines the socket timeout (SO_TIMEOUT) in seconds, which is the timeout for waiting for data.
    private int dataWaitTimeoutSeconds = 5;

    // Determines the size of the internal socket buffer used to buffer data while receiving / transmitting HTTP messages.
    private int bufferSizeKilobytes = 8;

    // TODO: This seems to be only relevant for HTTP clients, no?
    // Determines whether stale connection check is to be used. Disabling stale connection check may result in
    // slight performance improvement at the risk of getting an I/O error when executing a request over a
    // connection that has been closed at the server side.
    private boolean staleConnectionCheck = true;

    // Determines whether Nagle's algorithm is to be used.
    private boolean tcpNoDelay = true;

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

    public int getDataWaitTimeoutSeconds() {
        return dataWaitTimeoutSeconds;
    }

    public void setDataWaitTimeoutSeconds(int dataWaitTimeoutSeconds) {
        this.dataWaitTimeoutSeconds = dataWaitTimeoutSeconds;
    }

    public int getBufferSizeKilobytes() {
        return bufferSizeKilobytes;
    }

    public void setBufferSizeKilobytes(int bufferSizeKilobytes) {
        this.bufferSizeKilobytes = bufferSizeKilobytes;
    }

    public boolean isStaleConnectionCheck() {
        return staleConnectionCheck;
    }

    public void setStaleConnectionCheck(boolean staleConnectionCheck) {
        this.staleConnectionCheck = staleConnectionCheck;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
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
