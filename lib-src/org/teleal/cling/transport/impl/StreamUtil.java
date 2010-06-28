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

package org.teleal.cling.transport.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;


public class StreamUtil {

    // Will return null if there is a problem with the input stream!
    public static String readBufferedString(InputStream inputStream) throws IOException {
        assert inputStream != null;

        BufferedReader inputReader = null;
        try {
            inputReader = new BufferedReader(
                    new InputStreamReader(inputStream)
            );

            StringBuilder input = new StringBuilder();
            String inputLine;
            while ((inputLine = inputReader.readLine()) != null) {
                input.append(inputLine).append("\n");
            }
            if (input.length() > 0) {
                input.deleteCharAt(input.length() - 1);
            }

            return input.length() > 0 ? input.toString() : null;
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException ex) {
                    // Ignore this, it's thrown for example if the stream is already closed!
                }
            }
        }
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        assert inputStream != null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            int b;
            while ( (b = inputStream.read()) != -1) {
                bos.write(b);
            }
        } finally {
            inputStream.close();
        }
        return bos.toByteArray();
    }

    public static void writeUTF8(OutputStream outputStream, String data) throws IOException {
        assert outputStream != null;

        OutputStreamWriter outputWriter = null;
        try {
            outputWriter = new OutputStreamWriter(outputStream, "UTF-8");
            outputWriter.write(data);
        } finally {
            if (outputWriter != null) {
                outputWriter.close();
            }
        }
    }

    public static void writeBytes(OutputStream outputStream, byte[] data) throws IOException {
        assert outputStream != null;
        try {
            outputStream.write(data);
        } finally {
            outputStream.close();
        }
    }
}
