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

import org.teleal.cling.transport.spi.MulticastReceiverConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class MulticastReceiverConfigurationImpl implements MulticastReceiverConfiguration {

    private InetAddress group;
    private int port;

    public MulticastReceiverConfigurationImpl(InetAddress group, int port) {
        this.group = group;
        this.port = port;
    }

    public MulticastReceiverConfigurationImpl(String group, int port) throws UnknownHostException {
        this.group = InetAddress.getByName(group);
        this.port = port;
    }

    public InetAddress getGroup() {
        return group;
    }

    public void setGroup(InetAddress group) {
        this.group = group;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
