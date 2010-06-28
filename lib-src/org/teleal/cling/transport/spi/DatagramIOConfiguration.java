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

import java.net.InetAddress;


public interface DatagramIOConfiguration {

    // This is not really an MTU, it's just the fixed size buffer we use, according to the
    // UPnP DA 1.1 spec, recommended limit of 512 bytes message length + 128 byte UDP header.
    public static final int MTU = 640;

    public InetAddress getGroup();
    public int getPort();

}
