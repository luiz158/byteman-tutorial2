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
 * A PipelineProcessor is a stream processor which transforms an input stream of bytes into an
 * output stream. It implements Sink allowing it to be connected to a Source which provides its
 * input. It also implements Source allowing a Sink to consume its output.
 *
 * PipelineProcessor is an abstract class. Implementations define the transformation by
 * implementing method processPipeline.
 */
public abstract class PipelineProcessor extends Thread implements Sink, Source {
    /**
     * the stream feeding this processor
     */
    protected PipedReader input;

    /**
     * the stream fed by this processor
     */
    protected PipedWriter output;

    /**
     * construct a PipelineProcessor by setting up its input stream from the supplied Source
     * @param source
     * @throws IOException
     */
    public PipelineProcessor(Source source) throws IOException {
        this.output = null;
        this.input = null;
        source.feed(this);
    }

    public void feed(Sink sink) throws IOException
    {
        if (output != null) {
            throw new IOException("output already connected");
        }
        output = new PipedWriter();
        sink.setInput(new PipedReader(output));
    }

    public void setInput(PipedReader input) throws IOException {
        if (this.input != null) {
            throw new IOException("input already connected");
        }
        this.input = input;
    }

    /**
     * Calls {@link #processPipeline()}.
     * @throws RuntimeException if either an input or an output has not been configured
     */
    public void run() {
        boolean excepted = false;

        if (input == null || output == null) {
            throw new RuntimeException("unconnected pipeline");
        }
        try {
            processPipeline();
        } catch (IOException ioe) {
            //ioe.printStackTrace();
            // when we got an exception then we need try to close our input as well as our ouptut
            // otherwise we may leave our feeder thread sitting on a write to a full pipeline.
            // so remember that this happened
            excepted = true;
        } finally {
            try {
                output.close();
            } catch (IOException ioe) {
                // only print this if we have not already printed another one. if we already excepted
                // it might have been caused by the same stream
                if (!excepted) {
                    // ioe.printStackTrace();
                }
            }
            if (excepted) {
                try {
                    input.close();
                } catch (IOException ioe2) {
                    // the input may be the source of the original exception so don't bother to print this
                }
            }
        }
    }

    /**
     * Called by the run method to process the input stream and write the output stream.
     * @throws IOException
     */
    public abstract void processPipeline() throws IOException;
}
