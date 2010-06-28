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

package org.teleal.cling.binding.staging;

import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.meta.UDAVersion;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Bauer
 */
public class MutableServiceDescriptor {

    public int udaMajorVersion;
    public int udaMinorVersion;
    public List<MutableAction> actions = new ArrayList();
    public List<MutableStateVariable> stateVariables = new ArrayList();

    public <T extends Service> T build(Class<T> serviceClass) throws Exception {
        Constructor<T> ctor = serviceClass.getConstructor(UDAVersion.class, Action[].class, StateVariable[].class);
        return ctor.newInstance(new UDAVersion(udaMajorVersion, udaMinorVersion), createActions(), createStateVariables());
    }

    public Action[] createActions() {
        Action[] array = new Action[actions.size()];
        int i = 0;
        for (MutableAction action : actions) {
            array[i++] = action.build();
        }
        return array;
    }

    public StateVariable[] createStateVariables() {
        StateVariable[] array = new StateVariable[stateVariables.size()];
        int i = 0;
        for (MutableStateVariable stateVariable : stateVariables) {
            array[i++] = stateVariable.build();
        }
        return array;
    }

}
