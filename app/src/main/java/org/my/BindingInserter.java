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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A BindingInserter is text line processor similar to {@link Binder}. However, as well as binding
 * matched text to identifiers it also inserts a reference to the binding into the output in place
 * of the matched text.<p/>
 *
 * The pattern and prefix are as expected by {@link Binder}.<p/>
 *
 * So, for example, given pattern "the [A-Za-z]+", prefix "DET" and input text "the boy threw the
 * stick at the boy" the output text would be "${DET1} threw ${DET2} at ${DET1}" and the  * bindings would
 * be ["DET1" -> "the boy", "DET2" -> "the stick"]. By contrast, with pattern "the \([A-Za-z]+\)"
 * and prefix N the output text would be "the ${N1} threw the ${N2} at the ${N1}" and the bindings would be
 * ["N1" -> "boy", "N2" -> "stick"].
 */

public class BindingInserter extends TextLineProcessor
{
    private Pattern pattern;
    private String prefix;
    private int counter;
    private BindingMap bindings;
    public BindingInserter(String regex, String prefix, BindingMap bindings, Source source) throws IOException {
        super(source);
        createMatcher(regex);
        this.prefix = prefix;
        this.counter = 1;
        this.bindings = bindings;
    }

    private void createMatcher(String regex) throws IOException {
        pattern = Pattern.compile(regex);
    }

    @Override
    public String transform(String line) {
        // seach for successive matches
        // look up previous bindings or bind them if they are new
        // replace them
        StringBuilder builder = new StringBuilder();
        Matcher matcher = pattern.matcher(line);
        char[] chars = line.toCharArray();
        int current = 0;
        boolean isMatch = matcher.find(current);
        while (isMatch) {
            int start;
            int end;
            String matchedText;
            if (matcher.groupCount() == 1) {
                matchedText = matcher.group(1);
                start = matcher.start(1);
                end = matcher.end(1);
            } else {
                matchedText = matcher.group();
                start = matcher.start();
                end = matcher.end();
            }
            String binding = getBinding(matchedText);
            // copy text up to match and bound name and then restart from end of match
            while (current < start) {
                builder.append(chars[current++]);
            }
            builder.append(binding);
            current = end;
            isMatch = matcher.find(current);
        }
        // if we have any text left over then append it too

        while (current < chars.length) {
            builder.append(chars[current++]);
        }
        return builder.toString();
    }

    private String getBinding(String matchedText) {
        String next = prefix + counter;
        String identifier = bindings.putIfAbsent(next, matchedText);
        if (identifier == null) {
            // this was a new binding
            counter++;
            return next;
        } else {
            // this was an existing binding
            return identifier;
        }
    }
}
