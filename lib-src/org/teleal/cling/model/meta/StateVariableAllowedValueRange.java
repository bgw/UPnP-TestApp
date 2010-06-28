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




import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;

import java.util.List;
import java.util.ArrayList;

/**
 * TODO: The question here is: Are they crazy enough to use this for !integer (e.g. floating point) numbers?
 */
public class StateVariableAllowedValueRange implements Validatable {

    final private long minimum;
    final private long maximum;
    final private long step;

    public StateVariableAllowedValueRange(long minimum, long maximum) {
        this(minimum, maximum, 1);
    }

    public StateVariableAllowedValueRange(long minimum, long maximum, long step) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
    }

    public long getMinimum() {
        return minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public long getStep() {
        return step;
    }

    public boolean isInRange(long value) {
        return value >= getMinimum() && value <= getMaximum() && (value % step) == 0;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();
        if (getMinimum() > getMaximum()) {
            errors.add(new ValidationError(
                    getClass(),
                    "minimum",
                    "Range minimum'" + getMinimum() + "' is greater than maximum: " + getMaximum()
            ));
        }
        return errors;
    }

    @Override
    public String toString() {
        return "Range Min: " + getMinimum() + " Max: " + getMaximum() + " Step: " + getStep();
    }
}