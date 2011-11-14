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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A simple application which streams an input file, performs several binding replacements
 * on each line in turn and writes the transformed text to an output file
 */
public class PipelineAppMain2
{
    public static void main(String[] args)
    {
        try {
            BindingMap  bindings = new BindingMap();
            StringBuilder input = new StringBuilder();
            input.append("the boy threw the stick at the boy\n");
            input.append("a boy threw a stick at a window\n");

            // pipeline source is the input char sequence
            CharSequenceReader reader = new CharSequenceReader(input);
            PipelineProcessor[] pipeline = new PipelineProcessor[5];

            // pipeline stage 0 matches "the X"
            pipeline[0] = new BindingInserter("the ([A-Za-z0-9]+)", "X", bindings, reader);
            // pipeline stage 1 tees intermediate output to a trace char sequence writer
            pipeline[1] = new TeeProcessor(pipeline[0]);
            // pipeline stage 2 matches "a Y"
            pipeline[2] = new BindingInserter("a ([A-Za-z0-9]+)", "Y", bindings, pipeline[1]);
            // pipeline stage 3 tees intermediate output to a trace char sequence writer
            pipeline[3] = new TeeProcessor(pipeline[2]);
            // pipeline stage 4 matches "a Y"
            pipeline[4] = new BindingInserter("a [A-Za-z0-9]+", "Z", bindings, pipeline[3]);

            // the tees feed a char sequence writer so we can sanity check the intermediate results
            CharSequenceWriter writer = new CharSequenceWriter(pipeline[1]);
            CharSequenceWriter writer2 = new CharSequenceWriter(pipeline[3]);

            // the output is also a char sequence writer
            CharSequenceWriter writer3 = new CharSequenceWriter(pipeline[4]);

            // start all the stream processors
            reader.start();
            for(int i = 0; i <pipeline.length ;i++) {
                pipeline[i].start();
            }
            writer.start();
            writer2.start();
            writer3.start();
            //now wait for all the processors to finish
            reader.join();
            for(int i = 0; i <pipeline.length; i++) {
                pipeline[i].join();
            }
            writer.join();
            writer2.join();
            writer3.join();

            System.out.println("input:");
            System.out.println(input);
            System.out.println("1st intermediate:");
            System.out.println(writer.toString());
            System.out.println("2nd intermediate:");
            System.out.println(writer2.toString());
            System.out.println("output:");
            System.out.println(writer3.toString());

            Iterator<String> iterator = bindings.iterator();
            List<String> list = new ArrayList<String>();
            while (iterator.hasNext()) {
                String id = iterator.next();
                list.add(id);
            }
            Collections.sort(list);
            iterator = list.iterator();
            System.out.println("bindings[");
            while (iterator.hasNext()) {
                String id = iterator.next();
                System.out.println(id + " -> " + bindings.get(id));
            }
            System.out.println("]");
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
