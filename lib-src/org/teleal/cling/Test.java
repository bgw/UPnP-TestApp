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

import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.registry.Registry;


public class Test {

    public static void main(String[] args) throws Exception {

        UpnpService upnpService = new UpnpServiceImpl(
                new RegistryListener() {
                    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                        System.out.println("############ REMOTE ADD: " + device.getDisplayString());
                    }

                    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                        System.out.println("############ REMOTE UPDATE: " + device);
                    }

                    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                        System.out.println("############ REMOTE REMOVED: " + device);
                    }

                    public void localDeviceAdded(Registry registry, LocalDevice device) {
                        System.out.println("############ LOCAL ADD: " + device);
                    }

                    public void localDeviceRemoved(Registry registry, LocalDevice device) {
                        System.out.println("############ LOCAL REMOVED: " + device);
                    }
                }
        );

        upnpService.getControlPoint().search(new STAllHeader()); // Search for all devices and services
    }
}
