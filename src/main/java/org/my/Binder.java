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
 * A Binder is a text line processor which searches for occurences of a specific pattern in
 * each input line and associates the matching text with a binding name, a unique identifier.
 * The binding is recorded in a binding map provided when the binder is created. If the same
 * text is matched more than oncethen the existign bindingis reused. Binding names
 * are created by appending a number to a prefix supplied when the binder is created<p/>
 *
 * The pattern is a regular expression which should contain at most one match group. If it
 * contains no match groups then the whole of the matching text is used as the value to be bound.
 * If it contains one match group then the text for this groupis used as the value to be bound.<p/>
 *
 * So, for example, given pattern "the [A-Za-z]+", prefix "DET" and input text "the boy threw the
 * stick at the boy" the bindings would be ["DET1" -> "the boy", "DET2" -> "the stick"]. By contrast,
 * with pattern "the \([A-Za-z]+\)" and prefix N the bindings would be ["N1" -> "boy", "N2" -> "stick"].
 */

public class Binder extends TextLineProcessor
{
    private Pattern pattern;
    private String prefix;
    private int counter;
    private BindingMap bindings;
    public Binder(String regex, String prefix, BindingMap bindings, Source source) throws IOException {
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
        Matcher matcher = pattern.matcher(line);
        int current = 0;
        boolean isMatch = matcher.find(current);
        while (isMatch) {
            int end;
            String matchedText;
            if (matcher.groupCount() == 1) {
                matchedText = matcher.group(1);
                end = matcher.end(1);
            } else {
                matchedText = matcher.group();
                end = matcher.end();
            }
            findOrCreateBinding(matchedText);
            current = end;
            isMatch = matcher.find(current);
        }
        // return the original line unchanged

        return line;
    }

    private String findOrCreateBinding(String matchedText) {
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
