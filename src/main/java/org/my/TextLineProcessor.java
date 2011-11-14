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
import java.io.Reader;

/**
 * A TextLineProcessor is a PipelineProcessor which transforms its input line by line, using
 * either LF or CR/LF as a line separator.
 *
 * Subclasses of TextLineProcessor must provide an implementation of method transform which accepts a line
 * of text as a String and returns a transformed version of the line as a String
 */

public abstract class TextLineProcessor extends PipelineProcessor {

    public TextLineProcessor(Source source) throws IOException
    {
        super(source);
    }

    /**
     * reads successive lines of text from its input up to a CR/LF or LF separator (or to EOF),
     * calling {@link #transform(String)} to allow the text line to be substituted and then
     * writes the test line followedby the same line terminator.
     * @throws IOException
     */
    public void processPipeline() throws IOException
    {
        TextLine lineBuffer = new TextLine(input);
        String text = lineBuffer.readText();
        while (text != null) {
            text = transform(text);
            output.write(text);
            if (lineBuffer.isCrLf()) {
                output.write('\r');
                output.write('\n');
            } else if (lineBuffer.isLf()){
                output.write('\n');
            }
            text = lineBuffer.readText();
        }
    }

    /**
     * abstract method provided to allow subclasses to define how each text line istobe transformed
     * @param line a line of text from th efile omitting any line terminator
     * @return
     */
    public abstract String transform(String line);

    /**
     * private class used to read sucessive text lines from an input stream up to CR/LF,LF or EOF.
     * The text is returned as a String and methods are provided to check the line termination.
     */

    private static class TextLine
    {
        private Reader input;
        private boolean isCrLf;
        private boolean isLf;

        public TextLine(Reader input) throws IOException
        {
            this.input = input;
            this.isCrLf = false;
            this.isLf = false;
        }

        /**
         * reads and returns the next line of text from the input stream
         * @return the text as a String or null if the input is at EOF
         * @throws IOException
         */
        public String readText() throws IOException
        {
            StringBuilder builder = new StringBuilder();
            isCrLf = false;
            isLf = false;
            int next = input.read();

            while (next >=  0) {
                char c =(char)next;
                if (c == '\r') {

                    next = input.read();
                    if (next >= 0 && ((char)next) == '\n') {
                        isCrLf = true;
                        return builder.toString();
                    } else {
                        builder.append(c);
                        c = (char)next;
                    }
                } else  if (c == '\n') {
                    isLf = true;
                    return builder.toString();
                }
                // add this character to the text
                builder.append(c);
                next = input.read();
            }
            String text = builder.toString();

            /**
             * we may have been called at EOF in which case we need to return null
             */
            if (text.length() == 0) {
                return null;
            }
            // this can happen if we have a final line with no LF at end
            return text;
        }

        /**
         * true if the current line is terminated by CR/LF otherwise false
         * @return
         */
        public boolean isCrLf()
        {
            return isCrLf;
        }

        /**
         * true if the current line is terminated by LF with no preceding CR otherwise false
         * @return
         */
        public boolean isLf()
        {
            return isLf;
        }
    }
}
