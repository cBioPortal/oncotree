/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/

package org.mskcc.oncotree.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ApiUtil {

    public static Pattern non_graphable_pattern = Pattern.compile("[^\\p{Graph}]");
    public static Pattern prohibited_pattern = Pattern.compile("[\\<\\>\\&\\\"\\'\\\\\\x00]");

    /**
     * removes non-printing characters, and &lt; &gt; &amp; &quot; apostrophe, and backslash
     */
    public String cleanArgument(String arg) {
        if (arg == null) {
            return null;
        }
        Matcher graphable_matcher = non_graphable_pattern.matcher(arg);
        String filtered_of_non_graphing = graphable_matcher.replaceAll("");
        Matcher prohibited_matcher = prohibited_pattern.matcher(filtered_of_non_graphing);
        return prohibited_matcher.replaceAll("");
    }
}
