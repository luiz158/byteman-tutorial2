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
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A PatternReplacer is a LineProcessor which transforms each line of text using a regular expression pattern
 * to match segments of the text, substituting them with a replacement string.<p/>
 *
 * The pattern is a regular expression employing the syntax expected by {@link java.util.regex.Pattern}.
 * This means it can employ bracketed groups to identify elemnts of the input line. So, for example
 * the String "(.*)[Aa]ndrew(.*)" will match the whole line "author: Andrew Dinn, JBoss" binding the first
 * bracketed group to the String "author: " and the second bracketed group to the String " Dinn, JBoss". Note that
 * the indices used to identify match groups start counting at 1.<p/>
 *
 * The replacement text is composed by substituting elements in the format \nn with the corresponding
 * matching text. So, for example the replacement String "\\1Michael\\2" could be used in
 * conjunction with the example pattern provide above to transform the input line into the text
 * "author: Michael Dinn, JBoss".<p/>
 *
 * Note that the replacement String literal requires the backslash character preceding to the matching
 * group index to be escaped with another backslash. A single backslash followed by a digit sequence
 * would be interpreted as a unicode character.<p/>
 */

class PatternReplacer extends TextLineProcessor {
    private Pattern pattern;
    private String replacement;
    int[] groupIndices;
    int maxGroupIndex;

    /**
     * create a pattern replacer
     * @param pattern a pattern which matches text to be transformed
     * @param replacement a replacement to use in place ofany matching text lines which may include match group
     * elementsformthe original line
     * @param source the source stream which provides the input text
     * @throws IOException
     */
    public PatternReplacer(String pattern, String replacement, Source source) throws IOException
    {
        super(source);
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
        computePatternGroups();
    }

    /**
     * transforms any input text line which matches the pattern
     * @param text the input text line
     * @return a transfortmed version of the line if it atches the pattern opterwise the original text line
     */
    public String transform(String text)
    {
    	Matcher matcher = pattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        int current = 0;
        int max = text.length();
	    while (matcher.find(current)) {
            int start = matcher.start();
            int end = matcher.end();
            for (int i = current; i < start ; i++) {
                builder.append(text.charAt(i));
            }
            substitutePatternGroups(matcher, builder);
            current = end;
        }
        for (int i = current; i < max ; i++) {
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }

    /**
     * called when a match is found to substitute matching groups into the replacement text line
     * @param matcher
     * @return
     */
    private void substitutePatternGroups(Matcher matcher, StringBuilder builder) {
        // reject inadequate matches
        if (matcher.groupCount() < maxGroupIndex) {
             return;
        }
        String text = replacement;
        for (int i : groupIndices) {
            text = text.replaceAll("\\\\" + i, matcher.group(i));
        }
        builder.append(text);
    }

    /**
     * parses the replacement to identifyhow many match groups need to be substituted and store
     * their indices for later use
     */
    private void  computePatternGroups()
    {
        maxGroupIndex = 0;
        List<Integer> groups = new ArrayList<Integer>();
        Matcher groupMatcher = Pattern.compile("\\\\[1-9][0-9]*").matcher(replacement);
        int start = 0;
        while(groupMatcher.find(start)) {
            String text = groupMatcher.group(0);
            int index =  Integer.valueOf(text.substring(1));
            if (!groups.contains(index)) {
                if(index > maxGroupIndex) {
                    maxGroupIndex = index;
                }
                groups.add(index);
            }
            start = groupMatcher.end();
        }
        groupIndices = new int[groups.size()];
        for (int i = 0;i < groups.size();i++) {
            groupIndices[i] = groups.get(i);
        }
    }
}
