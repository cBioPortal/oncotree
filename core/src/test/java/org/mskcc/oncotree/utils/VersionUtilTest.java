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
import java.nio.file.*;
import java.util.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


@RunWith(SpringRunner.class)
@Import(VersionUtilTestConfig.class)
public class VersionUtilTest {
    private List<Version> oncoTreeVersionRepositoryMockResponse = null;
    @Autowired
    private OncoTreeVersionRepository mockVersionRepository = null;
    @Autowired
    private VersionUtil versionUtil = null;
    private HashMap<String, Version> expectedVersionMap = null;

    private void setupOncoTreeVersionRepositoryMockResponse() throws Exception {
        if (oncoTreeVersionRepositoryMockResponse == null) {
            oncoTreeVersionRepositoryMockResponse = new ArrayList<Version>();
            Version nextVersion = new Version();
            nextVersion.setVersion("oncotree_latest_stable");
            nextVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
            nextVersion.setDescription("This is an alias for whatever OncoTree version is the latest stable (timestamped) release.");
            oncoTreeVersionRepositoryMockResponse.add(nextVersion);
            nextVersion = new Version();
            nextVersion.setVersion("oncotree_development");
            nextVersion.setGraphURI("urn:x-evn-master:oncotree_current");
            nextVersion.setDescription("Latest OncoTree under development (subject to change without notice)");
            oncoTreeVersionRepositoryMockResponse.add(nextVersion);
            nextVersion = new Version();
            nextVersion.setVersion("oncotree_2017_06_21");
            nextVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
            nextVersion.setDescription("Stable OncoTree released on date 2017-06-21");
            oncoTreeVersionRepositoryMockResponse.add(nextVersion);
            nextVersion = new Version();
            nextVersion.setVersion("oncotree_legacy_1.1");
            nextVersion.setGraphURI("urn:x-evn-master:oncotree_legacy_1_1");
            nextVersion.setDescription("This is the closest match in TopBraid for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)");
            oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        }
    }

    private void setupMockVersionRepository() throws Exception {
        setupOncoTreeVersionRepositoryMockResponse();
        Mockito.when(mockVersionRepository.getOncoTreeVersions()).thenReturn(oncoTreeVersionRepositoryMockResponse);
    }

    private void setupExpectedVersionMap() throws Exception {
        setupOncoTreeVersionRepositoryMockResponse();
        if (expectedVersionMap == null) {
            expectedVersionMap = new HashMap<>(oncoTreeVersionRepositoryMockResponse.size());
            for (Version version : oncoTreeVersionRepositoryMockResponse) {
                expectedVersionMap.put(version.getVersion(), version);
            }
        }
    }

    @Before
    public void setupForTests() throws Exception {
        setupMockVersionRepository();
        setupExpectedVersionMap();
    }

    public String makeMismatchMessage(String versionName, String fieldName, String gotValue, String expectedValue) {
        return versionName + " : mismatch in " + fieldName + " (got:" + gotValue + ") (expected:" + expectedValue + ")\n";
    }


    private boolean testValuesMatch(String value1, String value2) {
        if (value1 == null || value1.trim().length() == 0) {
            return value2 == null || value2.trim().length() == 0;
        }
        return value1.equals(value2);
    }

    public int failuresInReturnedVersionComparison(String versionName, Version expectedVersion, Version returnedVersion, StringBuilder failureReport) throws Exception {
        int failureCount = 0;
        if (expectedVersion == null || returnedVersion == null) {
            if (expectedVersion == null) {
                failureReport.append(versionName + " : no values in expected version hash (internal error)\n");
                failureCount = failureCount + 1;
            }
            if (returnedVersion == null) {
                failureReport.append(versionName + " : version expected but not returned from versionUtil\n");
                failureCount = failureCount + 1;
            }
        } else {
            String expectedVersionName = expectedVersion.getVersion();
            String returnedVersionName = returnedVersion.getVersion();
            if (!testValuesMatch(expectedVersionName, returnedVersionName)) {
                failureReport.append(makeMismatchMessage(versionName, "version", returnedVersionName, expectedVersionName));
                failureCount = failureCount + 1;
            }
            String expectedGraphURI = expectedVersion.getGraphURI();
            String returnedGraphURI = returnedVersion.getGraphURI();
            if (!testValuesMatch(expectedGraphURI, returnedGraphURI)) {
                failureReport.append(makeMismatchMessage(versionName, "graphURI", returnedGraphURI, expectedGraphURI));
                failureCount = failureCount + 1;
            }
            String expectedDescription = expectedVersion.getDescription();
            String returnedDescription = returnedVersion.getDescription();
            if (!testValuesMatch(expectedDescription, returnedDescription)) {
                failureReport.append(makeMismatchMessage(versionName, "description", returnedDescription, expectedDescription));
                failureCount = failureCount + 1;
            }
        }
        return failureCount;
    }

    @Test
    public void testFullVersionList() throws Exception {
        List<Version> versionsFromUtil = versionUtil.getVersions();
        Map<String, Version> returnedVersionMap = new HashMap<>(versionsFromUtil.size());
        for (Version version : versionsFromUtil) {
            returnedVersionMap.put(version.getVersion(), version);
        }
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        for (String versionName : expectedVersionMap.keySet()) {
            Version expectedVersion = expectedVersionMap.get(versionName);
            Version returnedVersion = returnedVersionMap.get(versionName);
            failureCount = failureCount + failuresInReturnedVersionComparison(versionName, expectedVersion, returnedVersion, failureReport);
        }
        if (expectedVersionMap.size() != returnedVersionMap.size()) {
            failureReport.append("mismatch between returned version list (size:" + Integer.toString(returnedVersionMap.size()) +
                    ") and expected version list (size:" + Integer.toString(expectedVersionMap.size()) + ")");
            failureCount = failureCount + 1;
        }
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow... " + failureReport.toString());
        }
        return;
    }

    @Test
    public void testSuccessfulVersionQuery() throws Exception {
        String testVersionName = expectedVersionMap.keySet().iterator().next();
        Version expectedVersion = expectedVersionMap.get(testVersionName);
        Version returnedVersion = versionUtil.getVersion(testVersionName);
        int failureCount = 0;
        StringBuilder failureReport  = new StringBuilder();
        failureCount = failureCount + failuresInReturnedVersionComparison(testVersionName, expectedVersion, returnedVersion, failureReport);
        if (failureCount > 0) {
            fail(Integer.toString(failureCount) + " failed test conditions. Details follow... " + failureReport.toString());
        }
        return;
    }

    @Test(expected = InvalidVersionException.class)
    public void testUnmatchedVersionQuery() throws Exception {
        String testVersionName = "THIS_STRING_IS_VERY_UNLIKELY_TO_EVER_BE_USED_FOR_THE_NAME_OF_AN_ACTUAL_ONCOTREE_VERSION";
        Version returnedVersion = versionUtil.getVersion(testVersionName);
        fail("expected exception (InvalidVersionException) did not get thrown");
    }

    @Test(expected = InvalidVersionException.class)
    public void testNullVersionQuery() throws Exception {
        String testVersionName = null;
        Version returnedVersion = versionUtil.getVersion(testVersionName);
        fail("expected exception (InvalidVersionException) did not get thrown");
    }

    @Test(expected = InvalidVersionException.class)
    public void testEmptyVersionQuery() throws Exception {
        String testVersionName = "";
        Version returnedVersion = versionUtil.getVersion(testVersionName);
        fail("expected exception (InvalidVersionException) did not get thrown");
    }

    @Test
    public void testGetDefaultVersion() throws Exception {
        Version returnedVersion = versionUtil.getDefaultVersion();
        if (returnedVersion == null || returnedVersion.getVersion() == null || returnedVersion.getVersion().trim().length() < 1) {
            fail("returned value from getDefaultVersion() was empty");
        }
        if (!returnedVersion.getVersion().equals(VersionUtil.DEFAULT_VERSION)) {
            fail("returned value from getDefaultVersion() did have the correct version identifier");
        }
    }
}
