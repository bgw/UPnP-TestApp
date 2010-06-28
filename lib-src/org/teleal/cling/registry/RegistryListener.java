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

import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;

public interface RegistryListener {

    public void remoteDeviceAdded(Registry registry, RemoteDevice device);
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device);
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device);
    public void localDeviceAdded(Registry registry, LocalDevice device);
    public void localDeviceRemoved(Registry registry, LocalDevice device);

}
