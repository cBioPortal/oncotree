/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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

import java.time.ZonedDateTime;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.mskcc.oncotree.utils.VersionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.mockito.Matchers.*;
import static org.junit.Assert.fail;


/**
 * Author: Avery Wang.
 */
@RunWith(SpringRunner.class)
@Import(OncotreeTestConfig.class)
public class CacheUtilTest {

    @Resource(name="oncoTreeVersionRepositoryMockResponse")
    private List<Version> oncoTreeVersionRepositoryMockResponse;
    
    @Resource(name="oncoTreeAdditionalVersionRepositoryMockResponse")
    private List<Version> oncoTreeAdditionalVersionRepositoryMockResponse;
    
    @Autowired
    private OncoTreeVersionRepository oncoTreeVersionRepository;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private VersionUtil versionUtil;

    @Before
    public void resetOncoTreeVersionRepository() throws Exception {
        OncotreeTestConfig config = new OncotreeTestConfig();
        config.resetVersionRepository(oncoTreeVersionRepository);
        cacheUtil.resetCache();
    }

    public boolean versionListsAreEqual(List<Version> expectedVersionList, List<Version> returnedVersionList) {
        boolean versionListsAreEqual = true;
        if (expectedVersionList.size() != returnedVersionList.size()) {
            versionListsAreEqual = false;
        }
        if (!(returnedVersionList.containsAll(expectedVersionList) && expectedVersionList.containsAll(returnedVersionList))) {
            versionListsAreEqual = false;
        } 
        return versionListsAreEqual;
    }

    public String getVersionListNames(List<Version> versionList) {
        List<String> versionNames = new ArrayList<String>();
        for (Version version : versionList) {
            versionNames.add(version.getVersion());
        }
        return String.join(",", versionNames);
    }

    /*
     * Tests getCachedVersions is dependent on OncoTreeVersionRepository
     * Returned versions (keyset of tumor types cache) should only be updated after cache refresh
     */
    @Test
    public void testGetCachedVersions() {
        OncotreeTestConfig config = new OncotreeTestConfig();
        List<Version> returnedCachedVersions = cacheUtil.getCachedVersions();
        String expectedVersionNames = "";
        String returnedVersionNames = "";
        if (!versionListsAreEqual(oncoTreeVersionRepositoryMockResponse, returnedCachedVersions)) {
            expectedVersionNames = getVersionListNames(oncoTreeVersionRepositoryMockResponse);
            returnedVersionNames = getVersionListNames(returnedCachedVersions);
            fail("Expected and returned cached versions differ. Expected: " + expectedVersionNames + ", Returned: " + returnedVersionNames);
        } 
        config.resetAdditionalVersionRepository(oncoTreeVersionRepository);
        cacheUtil.resetCache();
        returnedCachedVersions = cacheUtil.getCachedVersions();
        if (!versionListsAreEqual(oncoTreeAdditionalVersionRepositoryMockResponse, returnedCachedVersions)) {
            expectedVersionNames = getVersionListNames(oncoTreeVersionRepositoryMockResponse);
            returnedVersionNames = getVersionListNames(returnedCachedVersions);
            fail("Expected and returned cached versions differ. Expected: " + expectedVersionNames + ", Returned: " + returnedVersionNames);
        } 
    }
    
    @Test(expected = FailedCacheRefreshException.class)
    public void testRecacheWithBrokenOncoTreeVersionRepository() {
        OncotreeTestConfig config = new OncotreeTestConfig();
        config.resetNotWorkingVersionRepository(oncoTreeVersionRepository);
        cacheUtil.resetCache();
    }   
}
