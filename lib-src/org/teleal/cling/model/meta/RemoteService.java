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

package org.teleal.cling.model.meta;

import org.teleal.cling.model.ValidationException;

import java.lang.reflect.Constructor;



public class RemoteService extends Service {

    public static Constructor<RemoteService> getConstructor() {
        try {
            return RemoteService.class.getConstructor(
                    Action[].class, StateVariable[].class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteService(UDAVersion version, Action[] actions, StateVariable[] stateVariables) throws ValidationException {
        super(version, actions, stateVariables);
    }

    public RemoteService(Action[] actions, StateVariable[] stateVariables) throws ValidationException {
        super(actions, stateVariables);
    }

    @Override
    public Action getQueryStateVariableAction() {
        return new QueryStateVariableAction(this);
    }
}