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

package org.teleal.cling.protocol;

import org.teleal.cling.UpnpService;

import java.net.InetAddress;


public abstract class SendingAsync implements Runnable {

    private final UpnpService upnpService;

    private InetAddress destinationAddress;
    private int destinationPort;

    protected SendingAsync(UpnpService upnpService, InetAddress destinationAddress, int destinationPort) {
        this.upnpService = upnpService;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }

    protected SendingAsync(UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public InetAddress getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void run() {
        execute();
    }

    protected abstract void execute();

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }

}