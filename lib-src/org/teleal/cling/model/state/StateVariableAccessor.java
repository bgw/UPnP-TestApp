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

package org.teleal.cling.model.state;

import org.teleal.cling.model.Command;
import org.teleal.cling.model.ServiceManager;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.StateVariable;


/**
 * @author Christian Bauer
 */
public abstract class StateVariableAccessor {

    public StateVariableValue read(final StateVariable<LocalService> stateVariable, final Object serviceImpl) throws Exception {

        class AccessCommand implements Command {
            Object result;
            public void execute(ServiceManager serviceManager) throws Exception {
                result = read(serviceImpl);
                if (stateVariable.getService().isStringConvertibleType(result)) {
                    result = result.toString();
                }
            }
        }

        AccessCommand cmd = new AccessCommand();
        stateVariable.getService().getManager().execute(cmd);
        return new StateVariableValue(stateVariable, cmd.result);
    }

    public abstract Class<?> getReturnType();

    // This is public on purpose!
    public abstract Object read(Object serviceImpl) throws Exception;

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
