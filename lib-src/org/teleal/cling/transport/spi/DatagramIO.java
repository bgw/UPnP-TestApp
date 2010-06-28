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

package org.teleal.cling.transport.spi;

import org.teleal.cling.transport.Router;
import org.teleal.cling.model.message.OutgoingDatagramMessage;

import java.net.InetAddress;
import java.net.DatagramPacket;


public interface DatagramIO<C extends DatagramIOConfiguration> extends Runnable {

    public void init(InetAddress bindAddress, Router router, DatagramProcessor datagramProcessor) throws InitializationException;
    public void stop();
    public C getConfiguration();
    
    public void send(OutgoingDatagramMessage message);
    public void send(DatagramPacket datagram);
}
