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

import java.util.logging.Logger;


public class RegistryMaintainer implements Runnable {

    private static Logger log = Logger.getLogger(RegistryMaintainer.class.getName());

    final private RegistryImpl registry;

    private volatile boolean stopped = false;

    public RegistryMaintainer(RegistryImpl registry) {
        this.registry = registry;
    }

    public int getSleepIntervalMillis() {
        return 1000;
    }

    public void stop() {
        log.fine("Setting stopped status on thread");
        stopped = true;
    }

    public void run() {
        stopped = false;
        log.fine("Running registry maintenance loop every milliseconds: " + getSleepIntervalMillis());
        while (!stopped) {

            try {
                registry.maintain();
                Thread.sleep(getSleepIntervalMillis());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
        log.fine("Stopped status on thread received, ending maintenance loop");
    }

}