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

package org.teleal.cling.model.message.header;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class CallbackHeader extends UpnpHeader<List<URL>> {

    public CallbackHeader() {
        setValue(new ArrayList());
    }

    public CallbackHeader(URL url) {
        this();
        getValue().add(url);
    }

    public void setString(String s) throws InvalidHeaderException {

        if (!s.contains("<") || !s.contains(">")) {
            throw new InvalidHeaderException("URLs not in brackets: " + s);
        }

        s = s.replaceAll("<", "");
        String[] split = s.split(">");
        try {
            List<URL> urls = new ArrayList();
            for (String sp : split) {
                sp = sp.trim();
                if (!sp.startsWith("http://")) {
                    throw new InvalidHeaderException("Can't parse non-http callback URL: " + sp);
                }
                urls.add(new URL(sp));
            }
            setValue(urls);
        } catch (MalformedURLException ex) {
            throw new InvalidHeaderException("Can't parse callback URLs from '" + s + "': " + ex);
        }
    }

    public String getString() {
        StringBuilder s = new StringBuilder();
        for (URL url : getValue()) {
            s.append("<").append(url.toString()).append(">");
        }
        return s.toString();
    }
}
