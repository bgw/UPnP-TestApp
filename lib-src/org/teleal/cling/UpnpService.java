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

package org.teleal.cling;

import org.teleal.cling.registry.Registry;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.transport.Router;
import org.teleal.cling.controlpoint.ControlPoint;


public interface UpnpService {

    public UpnpServiceConfiguration getConfiguration();
    public ControlPoint getControlPoint();
    public ProtocolFactory getProtocolFactory();
    public Registry getRegistry();
    public Router getRouter();

    public void shutdown();

}
