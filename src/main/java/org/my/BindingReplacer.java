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
 * A BindingReplacer is a text line processor which searches for references to bindings in
 * each input line and replaces each binding with the corresponding bound value.
 * Bindings are looked up in a binding map provided when the binder is created. This map
 * may be populated by a Binder or a BindingInserter running as a prior pipeline stage.<p/>
 *
 * So, for example, given input text "${DET1} threw ${DET2} at ${DET1}" and bindings
 * ["DET1" -> "a boy", "DET2" -> "a stick"] the output text would be "a boy threw a
 * stick at a boy".
 */

public class BindingReplacer extends TextLineProcessor
{
    private Pattern pattern;
    private int counter;
    private BindingMap bindings;

    public BindingReplacer(BindingMap bindings, Source source) throws IOException {
        super(source);
        this.counter = 1;
        this.bindings = bindings;
        this.pattern = Pattern.compile("\\$\\{([A-Za-z]+[1-9][0-9]*)\\}");
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
            int start = matcher.start();
            int end = matcher.end();
            String matchedText = matcher.group(1);
            String value = bindings.get(matchedText);
            // copy text up to match
            while (current < start) {
                builder.append(chars[current++]);
            }
            // if there is a bound value replace it otherwise just pass the binding reference through
            if (value != null) {
                builder.append(value);
                current = end;
            } else {
                while (current < end) {
                    builder.append(chars[current++]);
                }
            }
            // restart from end of match
            isMatch = matcher.find(current);
        }
        // if we have any text left over then append it too

        while (current < chars.length) {
            builder.append(chars[current++]);
        }
        return builder.toString();
    }
}
