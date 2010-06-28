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

package org.teleal.cling.model.action;

import org.teleal.cling.model.types.ErrorCode;


public class ActionException extends Exception {

    private int errorCode;

    public ActionException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ActionException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ActionException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDescription());
    }

    public ActionException(ErrorCode errorCode, String message) {
        this(errorCode, message, true);
    }

    public ActionException(ErrorCode errorCode, String message, boolean concatMessages) {
        this(errorCode.getCode(), concatMessages ? (errorCode.getDescription() + ". " + message + ".") : message);
    }

    public ActionException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".", cause);
    }

    public int getErrorCode() {
        return errorCode;
    }

    /*
    private ErrorCode errorCode;
    private int customErrorCode = 800; // That range is reserved for vendors, so why not take the first one...

    public ActionException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ActionException(String s, ErrorCode errorCode) {
        super(s);
        this.errorCode = errorCode;
    }

    public ActionException(int customErrorCode) {
        this.customErrorCode = customErrorCode;
    }

    public ActionException(String s, int customErrorCode) {
        super(s);
        this.customErrorCode = customErrorCode;
    }

    public ActionException(String s, ErrorCode errorCode, Throwable throwable) {
        super(s, throwable);
        this.errorCode = errorCode;
    }

    public ActionException(String s, int customErrorCode, Throwable throwable) {
        super(s, throwable);
        this.customErrorCode = customErrorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCustomErrorCode() {
        return customErrorCode;
    }

    public String getErrorDescription() {
        StringBuilder desc = new StringBuilder();

        if (getErrorCode() != null) {

            // Standard error code, always append the standard description
            desc.append(getErrorCode().getCode()).append(" - ");
            desc.append(getErrorCode().getDescription());

            if (getMessage() != null) {

                // We have an additional message
                if (getErrorCode() != null && getMessage().startsWith(getErrorCode().getDescription())) {
                    // The additional message starts with a standard description message, remove that
                    desc.append(getMessage().substring(getErrorCode().getDescription().length()));
                } else {
                    desc.append(". "); // Separate the two strings with a periodspace
                    desc.append(getMessage());
                }

            }

        } else {

            // Custom error code, just append the message if present
            if (getMessage() != null) {
                desc.append(getCustomErrorCode()).append(" - ");
                desc.append(getMessage());
            } else if (getCause() != null){
                // If there is a cause, we could add that
                desc.append(" Cause: ");
                desc.append(getCause().toString());
            } else {
                // If there is no description at all... just add a dummy
                desc.append(getCustomErrorCode()).append(" - ");
                desc.append("No error description");
            }
        }

        // If we now don't have period at the end of the description, add it
        if (!desc.toString().endsWith(".")) {
            desc.append(".");
        }

        return desc.toString();
    }

    public int getNumericErrorCode() {
        if (getErrorCode() != null) {
            return getErrorCode().getCode();
        } else {
            return getCustomErrorCode();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getErrorDescription();
    }
    */
}