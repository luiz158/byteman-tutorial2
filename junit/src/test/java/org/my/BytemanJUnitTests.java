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

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class showing how to use Byteman BMUnit package with JUnit
 */
@RunWith(BMUnitRunner.class)
@BMScript(value="trace", dir="target/test-classes")
public class BytemanJUnitTests
{
    /**
     * a simple test of the pattern replacer functionality. we feed a String into the pipeline via a
     * CharSequenceReader, transform it via a PatternReplacer and then retrieve the transformed String
     * using a CharSequenceWriter. This test does not use any Byteman rules.
     * @throws Exception
     */
    @Test
    public void testPipeline() throws Exception
    {
        System.out.println("testPipeLine:");
        StringBuffer buffer = new StringBuffer("hello world!");
        buffer.append(" goodbye cruel world, goodbye!\n");
        CharSequenceReader reader = new CharSequenceReader(buffer);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceWriter writer = new CharSequenceWriter(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("hello mum! goodbye cruel mum, goodbye!\n"));
    }

    /**
     * this is the same code as the previous test but it uses a Byteman rule to throw an IOException in
     * the pattern replacer before it can processes any text lines. The pattern replacer thread should catch
     * the exception, close its input and output streams and exit, causing the writer to finish
     * with an empty output. All the pipeline threads shoudl exit cleanly.
     * @throws Exception
     */
    @Test
    @BMRule(name="throw IOException at 1st transform",
            targetClass = "TextLineProcessor",
            targetMethod = "processPipeline",
            action = "throw new java.io.IOException()")
    public void testErrorInPipeline() throws Exception
    {
        System.out.println("testErrorInPipeline:");
        StringBuffer buffer = new StringBuffer("hello world!");
        buffer.append(" goodbye cruel world, goodbye!\n");
        CharSequenceReader reader = new CharSequenceReader(buffer);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceWriter writer = new CharSequenceWriter(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals(""));
    }

    /**
     * this is a similar test to the previous one but it differs in two small respects. Firstly  the
     * reader passes in many lines of text, enough to fill the input pipeline to the pattern replacer.
     * Secondly, there are two rules, the first of which creates a countDown used to control firing
     * of the second. The first rule is injected into the constructor for TextLineProcessor. It
     * creates a countDown with count 2 using the newly constructed instance as an identifying key.<p/>
     *
     * The second rule throws an IOException in method processPipeline as in the previous test. However
     * this time it specifies a target location and condition (note that these values were defaulted
     * to "AT ENTRY" and "TRUE" in the previous example). The target location is inside the loop body,
     * just before a call is made to method transform(String). This means the rule is triggered
     * each time a line of text is about to be processed and written to the output.<p/>
     *
     * The condition calls countDown passing $0 as the identifying key. This ensures that the countdown
     * used to perform the test is the one with initial counter 2 created when the text processor was
     * constructed and that this same countDown is used on each successive firing. At the first
     * triggering method countDown decrements the counter from 2 to 1 and returns false. At the second
     * triggering it decrements it from 1 to 0 and again returns false. At the third firing it finds
     * that the counter is zero so it deletes the countDown instance returning true. So, at this
     * triggering the rule fires and throws an exception.<p/>
     *
     * The pattern replacer should process two lines of text before the exception is thrown. It should
     * catch and print the exception, close its input and output streams and exit, causing the writer to
     * finish with two lines of transformed output. The upstream thread should also print an exception.
     * It will either still be writing text to its output or, more likely, it will be sitting on a full
     * pipeline waiting for the pattern replacer to clear the pipe. In either case a close on the stream
     * it is feeding will cause it to suffer a Pipe closed IOException.
     * @throws Exception
     */
    @Test
    @BMRules(rules={@BMRule(name="create countDown for TextLineProcessor",
                    targetClass = "TextLineProcessor",
                    targetMethod = "<init>",
                    action = "createCountDown($0, 2)"),
                    @BMRule(name="throw IOException at 3rd transform",
                    targetClass = "TextLineProcessor",
                    targetMethod = "processPipeline",
                    targetLocation = "CALL transform(String)",
                    condition = "countDown($0)",
                    action = "throw new java.io.IOException()")})
    public void testErrorInStuffedPipeline() throws Exception
    {
        System.out.println("testErrorInStuffedPipeline:");
        StringBuffer buffer = new StringBuffer("hello world!\n");
        buffer.append("goodbye cruel world, goodbye!\n");
        for (int i = 0; i < 40; i++) {
            buffer.append("goodbye! goodbye! goodbye!\n");
        }
        CharSequenceReader reader = new CharSequenceReader(buffer);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceWriter writer = new CharSequenceWriter(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("hello mum!\ngoodbye cruel mum, goodbye!\n"));
    }
}
