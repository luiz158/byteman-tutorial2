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

import org.jboss.byteman.contrib.bmunit.BMNGRunner;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.testng.annotations.Test;

/**
 * Test class showing how to use Byteman BMUnit package with TestNG
 */
/*
 * This script annotation installs some tracing rules which write information to System.out. When using
 * TestNG the output gets written to the test ouptut file. The script is located in the resources
 * subdirectory of the test JVM's working directory.
 */
@BMScript(value="trace", dir="target/test-classes")
public class BytemanNGTests extends BMNGRunner
{
    /**
     * this test exercises the binding and binding replacement pipeline processors but it only
     * works because of a timing dependency. The pipeline is a CharSequenceReader feeding a
     * Binder, feeding a BindingReplacer which finally feeds a CharSequenceWriter. The Binder
     * matches certain patterns in the input text and binds identifiers to the matched values.
     * So, for example, when the Binder matches the pattern "the \(A-Xa-z]+)" against the input
     * text "the boy threw the stick at the window" it installs the bindings [X1-> "boy",
     * X2 -> "stick", X3 -> "window"] into its BindingMap.<p/>
     *
     * The BindingReplacer employs the same BindingMap as the Binder as a source for the values
     * it uses to replace variable references. So, for example, when the BindingReplacer sees
     * the input text "a ${X1} threw a ${X2} at a ${X1}" it transforms it to the ouptut text
     * "a boy threw a stick at a boy".<p/>
     *
     * The timing issue arises because the pipeline stages run as independent threads. That means
     * the Binder can get several lines ahead of the BindingReplacer when it is processing its
     * input. If the input text references a variable ${Xk} at line N and that variable only gets
     * matched and bound at line M where N > M then the output text is indeterminate. If the Binder
     * gets far enough ahead to process line M before the BindingReplacer processes line N then
     * the reference ${Xk} will be bound and hence will be substituted. If the Binder arrives at
     * line M after the BindingReplacer has processed line N then ${Xk} will be unbound and hence
     * the variable reference will passthrough unsubstituted. Which outcome occursdepends upon
     * the sizeof the PipelineStream buffers and the vaaries ofthe Java thread scheduler.<p/>
     *
     * We can use Byteman to display this timing dependency by injecting synchronization operations
     * into the relevant threads so they arrive at the relevant lines in the desired order. The rules
     * are in script timing.btm. The rules injected into the Binder cause it to enter a 2-way
     * rendezvous just before it processes the 3rd line and just after it finishes processing that
     * line. The rules injected into the BindingReplacer cause it to enter a 2-way rendezvous just
     * before it processes the 2nd line and just after it finishes processing that line.<p/>
     *
     * Neither of the pipeline threads can process their line until they have passed their first
     * rendezvous. Once they have passed the second rendezvous then the line has definitely been
     * processed. So, the test thread can choose to rendezvous with either the Binder or the
     * BindingReplacer thread and in doing so decidewhich line gets processed first and which
     * one second.<p/>
     *
     * Of course, the test thread cannot directly call the Byteman built-in operations but it
     * doesn't need to. The test code calls a dummy method triggerRendezvous when it wants to
     * rendezvous with either the Binder or the BindingReplacer. A Byteman rule injects a call
     * to rendezvous into this method. The argument to this method is the Binder or the
     * BindingReplacer which are also used as the keys to identify the corresponding rendezvous.<p/>
     */
    @BMScript(value="timing", dir="target/test-classes")
    @Test
    public void testForwardPublishing() throws Exception
    {
        BindingMap bindings = new BindingMap();
        // this first line binds [X1 ->"boy", X2 -> "stick", X3 -> "dog"]
        StringBuffer buffer = new StringBuffer("the boy threw the stick for the dog to catch\n");
        // this second line needs the bindings [X1 ->"boy", X2 -> "stick", X4 -> "dog"]
        buffer.append("a ${X1} broke a ${X4} with a ${X2}\n");
        // this third line reuses bindings [X1 ->"boy", X2 -> "stick"] and add binding [X4 -> "window"]
        buffer.append("the boy threw the stick at the window\n");
        CharSequenceReader reader = new CharSequenceReader(buffer);
        Binder binder = new Binder("the ([A-Za-z]+)", "X", bindings, reader);
        BindingReplacer replacer = new BindingReplacer(bindings, binder);
        CharSequenceWriter writer = new CharSequenceWriter(replacer);
        reader.start();
        binder.start();
        replacer.start();
        writer.start();
        // first we rendezvous with the binder to allow it to pass the trigger point where it
        // is just about to process the third line
        triggerRendezvous(binder);
        // now we rendezvous again with the binder ensuring that it has completed processing
        // the third line. this means the binding for X4 should have been installed
        triggerRendezvous(binder);
        String value = bindings.get("X4");
        assert("window".equals(value));
        // next we rendezvous with the replacer to allow it to pass the trigger point where it
        // is just about to process the second line
        triggerRendezvous(replacer);
        // now we rendezvous again with the replacer ensuring that it has completed processing
        // the second line. this means the reference to X4 should have been replaced
        triggerRendezvous(replacer);
        reader.join();
        binder.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("the boy threw the stick for the dog to catch\n" +
                            "a boy broke a window with a stick\n" +
                            "the boy threw the stick at the window\n"));

        // ok, now we do the whole thing again but this time we allow the replacer to
        // process line 2 before allowing the binder to process line 3

        bindings = new BindingMap();
        // this first line binds [X1 ->"boy", X2 -> "stick", X3 -> "dog"]
        buffer = new StringBuffer("the boy threw the stick for the dog to catch\n");
        // this second line needs the bindings [X1 ->"boy", X2 -> "stick", X4 -> "dog"]
        buffer.append("a ${X1} broke a ${X4} with a ${X2}\n");
        // this third line reuses bindings [X1 ->"boy", X2 -> "stick"] and add binding [X4 -> "window"]
        buffer.append("the boy threw the stick at the window\n");
        reader = new CharSequenceReader(buffer);
        binder = new Binder("the ([A-Za-z]+)", "X", bindings, reader);
        replacer = new BindingReplacer(bindings, binder);
        writer = new CharSequenceWriter(replacer);
        reader.start();
        binder.start();
        replacer.start();
        writer.start();
        // first we rendezvous with the replacer to allow it to pass the trigger point where it
        // is just about to process the second line
        triggerRendezvous(replacer);
        // now we rendezvous again with the replacer ensuring that it has completed processing
        // the second line. this means the binding for X4 will not be found so the reference
        // will not get replaced
        value = bindings.get("X4");
        assert(value == null);
        triggerRendezvous(replacer);
        // next we rendezvous with the binder to allow it to pass the trigger point where it
        // is just about to process the third line
        triggerRendezvous(binder);
        // now we rendezvous again with the binder ensuring that it has completed processing
        // the third line. this means the binding for X4 will have been installed too late
        triggerRendezvous(binder);
        reader.join();
        binder.join();
        replacer.join();
        writer.join();
        output = writer.toString();
        assert(output.equals("the boy threw the stick for the dog to catch\n" +
                            "a boy broke a ${X4} with a stick\n" +
                            "the boy threw the stick at the window\n"));
    }

    private void triggerRendezvous(PipelineProcessor processor)
    {
        // nothing to do here as Byteman will inject the relevant code into this method
    }
}
