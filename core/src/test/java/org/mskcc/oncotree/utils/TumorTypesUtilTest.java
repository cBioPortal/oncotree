/** Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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
import java.util.*;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mockito;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.config.OncoTreeAppConfig;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;

@RunWith(SpringRunner.class)
@Import(OncotreeTestConfig.class)
public class TumorTypesUtilTest {
    @Resource(name="legacyVersion")
    private Version legacyVersion;
    @Autowired
    private OncoTreeRepository mockRepository;
    @Autowired
    private TumorTypesUtil tumorTypesUtil;
    @Autowired
    private CacheUtil cacheUtil;
    @Resource(name="expectedTumorTypeMap")
    private Map<String, TumorType> expectedTumorTypeMap;

    //TODO: move towards a more fine-grained test definition --- each condition being tested should be a single test function, and needed expected and actual data structures should be set up using the Before or BeforeClass annotation
    //TODO: as an example, in this class, the various conditions being tested might be:
    //          fullOncoTreeCodesMatchTest, fullOncoTreeTumorTypesMatchTest, fullOncoTreeColorsMatchTest, fullOncoTreeMainTypesMatchTest, fullOncoTreeChildrenMatchTest, ...
    //          fullOncoTreeTumorTypesTxtHeaderPresentTest, fullOncoTreeTumorTypesTxtColumnCountTest, fullOncoTreeTumorTypesTxtRowCountTest, ...

    @Before
    public void setupMockRepository() throws Exception {
        OncotreeTestConfig config = new OncotreeTestConfig();
        config.resetWorkingRepository(mockRepository);
    }

    public String makeMismatchMessage(String oncoTreeCode, String fieldName, String gotValue, String expectedValue) {
        return oncoTreeCode + " : mismatch in " + fieldName + " (got:" + gotValue + ") (expected:" + expectedValue + ")\n";
    }

    public Set<TumorType> simulateFullTumorTypeApiResponse() throws Exception {
        ArrayList<OncoTreeNode> mockOncoTreeNodes = mockRepository.getOncoTree(legacyVersion);
        Map<String, TumorType> tumorTypeMap = tumorTypesUtil.getAllTumorTypesFromOncoTreeNodes(mockOncoTreeNodes, legacyVersion, new HashMap<String, ArrayList<String>>());
        Set<TumorType> tumorTypeSet = tumorTypesUtil.flattenTumorTypes(tumorTypeMap, null);
        return tumorTypeSet;
    }

    private Map<String, TumorType> getReturnedFullTumorTypeMap() throws Exception {
        Set<TumorType> tumorTypesAsPreparedForTumorTypesEndpoint = simulateFullTumorTypeApiResponse();
        HashMap<String, TumorType> returnedTumorTypeMap = new HashMap<>(tumorTypesAsPreparedForTumorTypesEndpoint.size());
        for (TumorType tumorType : tumorTypesAsPreparedForTumorTypesEndpoint) {
            returnedTumorTypeMap.put(tumorType.getCode(), tumorType);
        }
        return returnedTumorTypeMap;
    }

    private boolean testValuesMatch(String value1, String value2) {
        if (value1 == null || value1.trim().length() == 0) {
            return value2 == null || value2.trim().length() == 0;
        }
        return value1.equals(value2);
    }

    @Test
    public void testFullOncoTreeCodeListByCode() throws Exception {
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        Map<String,TumorType> returnedTumorTypeMap = getReturnedFullTumorTypeMap();
        for (String oncoTreeCode : expectedTumorTypeMap.keySet()) {
            TumorType expectedTumorType = expectedTumorTypeMap.get(oncoTreeCode);
            TumorType returnedTumorType = returnedTumorTypeMap.get(oncoTreeCode);
            if (expectedTumorType == null || returnedTumorType == null) {
                if (expectedTumorType == null) {
                    failureReport.append(oncoTreeCode + " : no values in expected tumor type hash (internal error)\n");
                    failureCount = failureCount + 1;
                }
                if (returnedTumorType == null) {
                    failureReport.append(oncoTreeCode + " : tumor type expected but not returned from tumorTypesUtil\n");
                    failureCount = failureCount + 1;
                }
            } else {
                String expectedCode = expectedTumorType.getCode();
                String returnedCode = returnedTumorType.getCode();
                if (!testValuesMatch(expectedCode, returnedCode)) {
                    failureReport.append(makeMismatchMessage(oncoTreeCode, "code", returnedCode, expectedCode));
                    failureCount = failureCount + 1;
                }
                String expectedName = expectedTumorType.getName();
                String returnedName = returnedTumorType.getName();
                if (!testValuesMatch(expectedName, returnedName)) {
                    failureReport.append(makeMismatchMessage(oncoTreeCode, "name", returnedName, expectedName));
                    failureCount = failureCount + 1;
                }
                String expectedMainType = expectedTumorType.getMainType();
                String returnedMainType = returnedTumorType.getMainType();
                if (expectedMainType == null || returnedMainType == null) {
                    if (expectedMainType == null && returnedMainType != null) {
                        if (returnedMainType != null && returnedMainType.trim().length() != 0) {
                            failureReport.append(oncoTreeCode + " : expected MainType is null, and returned MainType is non-null\n");
                            failureCount = failureCount + 1;
                        }
                    }
                    if (returnedMainType == null && expectedMainType != null) {
                        if (expectedMainType != null && expectedMainType.trim().length() != 0) {
                            failureReport.append(oncoTreeCode + " : retunred MainType is null, and expected MainType is non-null\n");
                            failureCount = failureCount + 1;
                        }
                    }
                } else {
                    if (!testValuesMatch(expectedMainType, returnedMainType)) {
                        failureReport.append(makeMismatchMessage(oncoTreeCode, "mainType", returnedMainType, expectedMainType));
                        failureCount = failureCount + 1;
                    }
                }
                String expectedColor = expectedTumorType.getColor();
                String returnedColor = returnedTumorType.getColor();
                if (!testValuesMatch(expectedColor, returnedColor)) {
                    failureReport.append(makeMismatchMessage(oncoTreeCode, "color", returnedColor, expectedColor));
                    failureCount = failureCount + 1;
                }
                String expectedParent = expectedTumorType.getParent();
                String returnedParent = returnedTumorType.getParent();
                if (!testValuesMatch(expectedParent, returnedParent)) {
                    failureReport.append(makeMismatchMessage(oncoTreeCode, "parent", returnedParent, expectedParent));
                    failureCount = failureCount + 1;
                }
                //TODO: add checks for children, level, tissue
            }
        }
        //TODO: scan for differences between sets of TumorTypes
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow... " + failureReport.toString());
        }
        return;
    }
    //TODO: add negative tests

    public int countNonRootNodes(Map<String, TumorType> tumorTypeMap, boolean isSubtree) {
        if (tumorTypeMap == null) {
            return 0;
        }
        int count = 0;
        for (String code : tumorTypeMap.keySet()) {
            if (isSubtree) {
                count = count + 1; //count current node if within subtree .. otherwise ignore root(s)
            }
            Map<String, TumorType> children = tumorTypeMap.get(code).getChildren();
            if (children != null && children.keySet().size() > 0) {
                count = count + countNonRootNodes(children, true);
            }
        }
        return count;
    }

    @Test
    public void testFullTumorTypesTxt() throws Exception {
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        ArrayList<OncoTreeNode> mockOncoTreeNodes = mockRepository.getOncoTree(legacyVersion);
        Map<String, TumorType> returnedTumorTypeMap = tumorTypesUtil.getAllTumorTypesFromOncoTreeNodes(mockOncoTreeNodes, legacyVersion, new HashMap<String, ArrayList<String>>());
        InputStream tumorTypeTxtInputStream = tumorTypesUtil.getTumorTypeInputStream(returnedTumorTypeMap);
        BufferedReader returnedSheetReader = new BufferedReader(new InputStreamReader(tumorTypeTxtInputStream));
        //check that header is present and matches
        String header = returnedSheetReader.readLine();
        if (!header.equals(TumorTypesUtil.TSV_HEADER)) {
            failureReport.append("Header does not match what was expected, received : " + header + "\n\texpected:" + TumorTypesUtil.TSV_HEADER);
            failureCount = failureCount + 1;
        }
        //check that each line is proper width and that line count matches the count of all nodes, minus root(s)
        int expectedDataLineCount = countNonRootNodes(returnedTumorTypeMap, false);
        int headerColumnCount = header.split("\t", -1).length;
        int linesRead = 0;
        while (returnedSheetReader.ready()) {
            String nextLine = returnedSheetReader.readLine();
            if (nextLine == null) {
                break;
            }
            linesRead = linesRead + 1;
            int lineColumnCount = nextLine.split("\t", -1).length;
            if (lineColumnCount != headerColumnCount) {
                failureReport.append("Line in TSV table has incorrect number of columns. expected: " + Integer.toString(headerColumnCount) +
                        " seen: " + Integer.toString(lineColumnCount) + " on line:\n\t:" + nextLine);
                failureCount = failureCount + 1;
            }
        }
        if (expectedDataLineCount != linesRead) {
            failureReport.append("Incorrect number of data lines. Expected: " + Integer.toString(expectedDataLineCount) + " received: " + Integer.toString(linesRead));
            failureCount = failureCount + 1;
        }
        //TODO: add test for alphabetized order and the proper filling in of parents in primary, secondary, tertiary...; also meta fields
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow... " + failureReport.toString());
        }
    }

    @Test
    public void validateCacheTest() throws Exception {
        Calendar currentDate = Calendar.getInstance();

        // test condition where cache is not stale
        // set cache age to a valid age (current date - MAXIMUM_CACHE_AGE_IN_DAYS + 1)
        // cache is not stale so resetCache should not be called -- cache age should remain the smae
        currentDate.add(Calendar.DATE, -(CacheUtil.MAXIMUM_CACHE_AGE_IN_DAYS - 1));
        Date validDateOfLastCacheRefresh = currentDate.getTime();
        cacheUtil.setDateOfLastCacheRefresh(validDateOfLastCacheRefresh);
        if (cacheUtil.cacheIsStale()) {
            cacheUtil.resetCache();
        }
        assertThat(cacheUtil.getDateOfLastCacheRefresh().toString(), equalTo(validDateOfLastCacheRefresh.toString()));

        // test condition where cache is stale
        // set cache age to a valid age (current date - MAXIMUM_CACHE_AGE_IN_DAYS - 3)
        // cache is stale so resetCache should be called -- cache age should change
        // NOTE: we have already set the currentDate back to one day before the cache should expire
        //   so we can just take this date and subtract 1 day
        currentDate.add(Calendar.DATE, -1);
        Date expiredDateOfLastCacheRefresh = currentDate.getTime();
        cacheUtil.setDateOfLastCacheRefresh(expiredDateOfLastCacheRefresh);
        if (cacheUtil.cacheIsStale()) {
            cacheUtil.resetCache();
        }
        assertThat(cacheUtil.getDateOfLastCacheRefresh().toString(), not(equalTo(validDateOfLastCacheRefresh.toString())));
    }

    @Test(expected = FailedCacheRefreshException.class)
    public void failedCacheRefreshTest() throws Exception {
        // resetting cache with a broken repository should throw a FailedCacheRefreshException
        OncotreeTestConfig config = new OncotreeTestConfig();
        config.resetNotWorkingRepository(mockRepository);
        cacheUtil.resetCache();
    }
}
