/** Copyright (c) 2017, 2024 Memorial Sloan-Kettering Cancer Center.
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
import javax.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.graphite.OncoTreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.fail;

/**
 *
 * @author heinsz
 */

@RunWith(SpringRunner.class)
@Import(OncotreeTestConfig.class)
public class MainTypesUtilTest {
    @Autowired
    private OncoTreeRepository mockRepository;
    @Autowired
    private MainTypesUtil mainTypesUtil;
    @Resource(name="expectedTumorTypeMap")
    private Map<String, TumorType> expectedTumorTypeMap;
    @Resource(name="expectedMainTypeList")
    private List<String> expectedMainTypeList;

    //TODO: move towards a more fine-grained test definition --- each condition being tested should be a single test function, and needed expected and actual data structures should be set up using the Before or BeforeClass annotation

    @Test
    public void testFullMainTypesListFromExpectedTumorTypes() {
        List<String> returnedMainTypeList = mainTypesUtil.getMainTypesByTumorTypes(new HashSet<>(expectedTumorTypeMap.values()));

        StringBuilder failureReport = new StringBuilder();
        int failureCount = 0;
        if (returnedMainTypeList.size() != expectedMainTypeList.size()) {
            failureCount++;
            failureReport.append("Returned MainType list is of different length than the expected MainType list." +
            "\n\tReturned list length: " + String.valueOf(returnedMainTypeList.size()) +
            "\n\tExpected list length: " + String.valueOf(expectedMainTypeList.size()) +
            "\n\tDifference is: " + StringUtils.join(CollectionUtils.disjunction(returnedMainTypeList, expectedMainTypeList), ", ") + "\n");
        }
        for (String mainType : expectedMainTypeList) {
            if (!returnedMainTypeList.contains(mainType)) {
                failureCount++;
                failureReport.append("MainType " + mainType + " missing in returned MainType list\n");
            }
        }
        for (String mainType : returnedMainTypeList) {
            if (!expectedMainTypeList.contains(mainType)) {
                failureCount++;
                failureReport.append("MainType " + mainType + " from returned MainType list not found in expected MainType list\n");
            }
        }
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow...\n" + failureReport.toString());
        }
    }
}
