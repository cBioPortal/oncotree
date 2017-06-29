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
import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.fail;

/**
 *
 * @author heinsz
 */

@RunWith(SpringRunner.class)
@Import(OncotreeUtilTestConfig.class)
public class MainTypesUtilTest {
    @Autowired
    private OncoTreeRepository mockRepository;
    @Autowired
    private MainTypesUtil mainTypesUtil;
    @Autowired
    private Map<String, TumorType> expectedTumorTypeMap;
    @Autowired
    private List<MainType> expectedMainTypeList;
    
    @Test
    public void testFullMainTypesListFromExpectedTumorTypes() {
        List<MainType> returnedMainTypeList = mainTypesUtil.getMainTypesByTumorTypes(new HashSet<>(expectedTumorTypeMap.values()));
        
        StringBuilder failureReport = new StringBuilder();
        int failureCount = 0;        
        if (returnedMainTypeList.size() != expectedMainTypeList.size()) {
            failureCount++;
            failureReport.append("Returned MainType list is of different length than the expected MainType list." +
            "\n\tReturned list length: " + String.valueOf(returnedMainTypeList.size()) + 
            "\n\tExpected list length: " + String.valueOf(expectedMainTypeList.size()) + "\n");
        }
        for (MainType mainType : expectedMainTypeList) {
            if (!returnedMainTypeList.contains(mainType)) {
                failureCount++;
                failureReport.append("MainType " + mainType.getName() + " missing in returned MainType list\n");
            }
        }        
        for (MainType mainType : returnedMainTypeList) {
            if (!expectedMainTypeList.contains(mainType)) {
                failureCount++;
                failureReport.append("MainType " + mainType.getName() + " from returned MainType list not found in expected MainType list\n");
            }
        }
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow...\n" + failureReport.toString());
        }
    }
}
