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

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

/**
 * A TeeProcessor is a PipelineProcessor which copies its input stream of bytes into two output streams.
 * This class implements Sink allowing it to be connected to a Source which provides its input. It also
 * implements Source allowing a Sink to consume its output. It expects and will only accept connections
 * from exactly two sinks
 */
public class TeeProcessor extends PipelineProcessor {
    protected PipedWriter output2;

    public TeeProcessor(Source source) throws IOException {
        super(source);
        this.output2 = null;
    }

    /**
     * TeeProcessor expects and will only accept two feed requests.
     * @param sink
     * @throws IOException
     */
    public void feed(Sink sink) throws IOException
    {
        if (output == null) {
            super.feed(sink);
        } else if (output2 == null) {
            output2 = new PipedWriter();
            sink.setInput(new PipedReader(output2));
        } else {
            throw new IOException("output already connected");
        }
    }

    public void setInput(PipedReader input) throws IOException {
        if (this.input != null) {
            throw new IOException("input already connected");
        }
        this.input = input;
    }

    /**
     * Copies the input stream to both output streams
     * @throws RuntimeException if a second output has not been configured
     */
    public void processPipeline() throws IOException
    {
        if (output2 == null) {
            throw new RuntimeException("unconnected tee");
        }

        try {
            int next = input.read();
            while (next != -1) {
                output.write(next);
                output2.write(next);
                next = input.read();
            }
        } finally {
            output2.close();
        }
    }
}
