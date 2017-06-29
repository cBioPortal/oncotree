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

import java.util.*;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mockito;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.Matchers.*;
import static org.junit.Assert.fail;
import org.junit.Before;

@RunWith(SpringRunner.class)
@Import(OncotreeUtilTestConfig.class)
public class TumorTypesUtilTest {
    @Autowired
    private ArrayList<OncoTreeNode> oncoTreeRepositoryMockResponse;
    @Autowired
    private Version mockVersion;
    @Autowired
    private OncoTreeRepository mockRepository;
    @Autowired
    private TumorTypesUtil tumorTypesUtil;
    @Autowired
    private Map<String, TumorType> expectedTumorTypeMap;
//TODO: add negative tests, tests based on a copy of data_clinical
//TODO: add mock implementation of OncoTree Connection manager ... so that true connection is not needed for unit tests (integration tests use true connection)

    @Before
    public void setupMockRepository() throws Exception {
        Mockito.when(mockRepository.getOncoTree(any(Version.class))).thenReturn(oncoTreeRepositoryMockResponse);
    }
    
    public String makeMismatchMessage(String oncoTreeCode, String fieldName, String gotValue, String expectedValue) {
        return oncoTreeCode + " : mismatch in " + fieldName + " (got:" + gotValue + ") (expected:" + expectedValue + ")\n";
    }

    public Set<TumorType> simulateTumorTypeApiResponse() throws Exception {
        Map<String, TumorType> tumorTypeMap = tumorTypesUtil.getTumorTypesByVersionFromRaw(mockVersion);
        Set<TumorType> tumorTypeSet = tumorTypesUtil.flattenTumorTypes(tumorTypeMap, null);
        return tumorTypeSet;
    }

    private boolean testValuesMatch(String value1, String value2) {
        if (value1 == null || value1.trim().length() == 0) {
            return value2 == null || value2.trim().length() == 0;
        }
        return value1.equals(value2);
    }

    @Test
    public void testFullOncoTreeCodeListByCode() throws Exception {
        Set<TumorType> tumorTypesAsPreparedForTumorTypesEndpoint = simulateTumorTypeApiResponse();
        HashMap<String,TumorType> returnedTumorTypeMap = new HashMap<>(tumorTypesAsPreparedForTumorTypesEndpoint.size());
        for (TumorType tumorType : tumorTypesAsPreparedForTumorTypesEndpoint) {
            returnedTumorTypeMap.put(tumorType.getCode(), tumorType);
        }
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        for (String oncoTreeCode : expectedTumorTypeMap.keySet()) {
            TumorType expectedTumorType = expectedTumorTypeMap.get(oncoTreeCode);
            TumorType returnedTumorType = returnedTumorTypeMap.get(oncoTreeCode);
            if (expectedTumorType == null || returnedTumorType == null) {
                if (expectedTumorType == null) {
                    failureReport.append(oncoTreeCode + " : no values in expected tumor type hash (internal error)\n");
                }
                if (returnedTumorType == null) {
                    failureReport.append(oncoTreeCode + " : tumor type expected but not returned from tumorTypesUtil\n");
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
}
