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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;

@RunWith(SpringRunner.class)
@Import(OncotreeTestConfig.class)
public class ApiUtilTest {

    @Autowired
    private ApiUtil apiUtil;

    public String makeMismatchMessage(String query, String gotValue, String expectedValue) {
        return "mismatch for query " + query + ". (got:" + gotValue + ") (expected:" + expectedValue + ")\n";
    }

    private boolean testValuesMatch(String value1, String value2) {
        if (value1 == null || value1.length() == 0) {
            return value2 == null || value2.length() == 0;
        }
        return value1.equals(value2);
    }

    public int failuresInReturnedStringComparison(String query, String expectedString, String returnedString, StringBuilder failureReport) throws Exception {
        int failureCount = 0;
        if (query == null) {
            query = "null";
        }
        if (!testValuesMatch(expectedString, returnedString)) {
            if (expectedString == null) {
                expectedString = "null";
            }
            if (returnedString == null) {
                returnedString = "null";
            }
            failureReport.append(makeMismatchMessage(query, returnedString, expectedString));
            failureCount = failureCount + 1;
        }
        return failureCount;
    }

    @Test
    public void testCleanArgument() throws Exception {
        Map<String, String> queryToExpectedMap = new HashMap<>();
        //no change cases
        queryToExpectedMap.put(null, null);
        queryToExpectedMap.put("", "");
        queryToExpectedMap.put("arg", "arg");
        //whitespace cases
        queryToExpectedMap.put(" ", "");
        queryToExpectedMap.put(" arg", "arg");
        queryToExpectedMap.put("a rg", "arg");
        queryToExpectedMap.put("arg ", "arg");
        queryToExpectedMap.put(" arg ", "arg");
        queryToExpectedMap.put(" a r g ", "arg");
        queryToExpectedMap.put("\targ", "arg");
        queryToExpectedMap.put("a\trg", "arg");
        queryToExpectedMap.put("\rarg", "arg");
        queryToExpectedMap.put("a\rrg", "arg");
        queryToExpectedMap.put("\narg", "arg");
        queryToExpectedMap.put("a\nrg", "arg");
        queryToExpectedMap.put("\u0000arg", "arg");
        queryToExpectedMap.put("\u0000arg", "arg");
        queryToExpectedMap.put("a\u0005rg", "arg");
        queryToExpectedMap.put("a\u0005rg", "arg");
        //prohibited character cases
        queryToExpectedMap.put("<arg", "arg");
        queryToExpectedMap.put("a<rg", "arg");
        queryToExpectedMap.put(">arg", "arg");
        queryToExpectedMap.put("a>rg", "arg");
        queryToExpectedMap.put("&arg", "arg");
        queryToExpectedMap.put("a&rg", "arg");
        queryToExpectedMap.put("\"arg", "arg");
        queryToExpectedMap.put("a\"rg", "arg");
        queryToExpectedMap.put("\'arg", "arg");
        queryToExpectedMap.put("a\'rg", "arg");
        queryToExpectedMap.put("\\arg", "arg");
        queryToExpectedMap.put("a\\rg", "arg");
        //mixed
        queryToExpectedMap.put(" a<r>\u0005g &\"\'\\\r\n ", "arg");
        Map<String, String> queryToReturnedMap = new HashMap<>();
        for (String query : queryToExpectedMap.keySet()) {
            queryToReturnedMap.put(query, apiUtil.cleanArgument(query));
        }
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        for (String query : queryToExpectedMap.keySet()) {
            String expectedString = queryToExpectedMap.get(query);
            String returnedString = queryToReturnedMap.get(query);
            failureCount = failureCount + failuresInReturnedStringComparison(query, expectedString, returnedString, failureReport);
        }
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow... " + failureReport.toString());
        }
        return;
    }

}
