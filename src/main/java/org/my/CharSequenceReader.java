/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat and individual contributors as identified
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @authors Andrew Dinn
 */

package org.my;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

/**
 * CharSequenceReader is a data Source which populates its output stream with characters read from a CharSequence
 */

public class CharSequenceReader extends Thread implements Source
{
    CharSequence input;
    private PipedWriter output;

    public CharSequenceReader(CharSequence input) throws IOException
    {
        this.input = input;
        output = null;
    }

    public void feed(Sink sink) throws IOException {
        if (output != null) {
            throw new IOException("output already connected");
        }
        output = new PipedWriter();
        sink.setInput(new PipedReader(output));
    }

    public void run()
    {
        if (input==null || output==null) {
            //nothing to do
            return;
        }
        try {
            if (input instanceof String) {
                output.write((String) input);
            } else {
                int l = input.length();
                for (int i = 0; i < l; i++) {
                    output.write(input.charAt(i));
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
