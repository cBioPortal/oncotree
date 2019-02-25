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

import static org.junit.Assert.*;


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

    @Resource(name="legacyVersion")
    private Version legacyVersion;

    @Resource(name="latestVersion")
    private Version latestVersion;
    
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

    private void assertNoHistoryInChildren(Map<String, TumorType> tumorTypes) {
        for(TumorType tumorType: tumorTypes.values()) {
            assertEquals("Node " + tumorType.getCode() + " name: " + tumorType.getName() + " has history", 0, tumorType.getHistory().size());
            assertNoHistoryInChildren(tumorType.getChildren());
        }
    }

    private void assertNoRescindsInChildren(Map<String, TumorType> tumorTypes) {
        for(TumorType tumorType: tumorTypes.values()) {
            assertEquals("Node " + tumorType.getCode() + " name: " + tumorType.getName() + " has rescinds", 0, tumorType.getRescinds().size());
            assertNoRescindsInChildren(tumorType.getChildren());
        }
    }

    private void assertNoPrecursorsInChildren(Map<String, TumorType> tumorTypes) {
        for(TumorType tumorType: tumorTypes.values()) {
            assertEquals("Node " + tumorType.getCode() + " name: " + tumorType.getName() + " has precursors", 0, tumorType.getPrecursors().size());
            assertNoPrecursorsInChildren(tumorType.getChildren());
        }
    }

    private Boolean assertHistoryInChildren(Map<String, TumorType> tumorTypes, Boolean foundHistory) {
        for(TumorType tumorType: tumorTypes.values()) {
            if ("SS".equals(tumorType.getCode())) {
                assertEquals(1, tumorType.getHistory().size());
                assertEquals("SEZS", tumorType.getHistory().get(0));
                foundHistory = Boolean.TRUE;
                // do not return because we want to check no other history
            } else {
                assertEquals(0, tumorType.getHistory().size());
            }
            foundHistory = assertHistoryInChildren(tumorType.getChildren(), foundHistory);
        }
        return foundHistory;
    }

    private Boolean assertRescindsInChildren(Map<String, TumorType> tumorTypes, Boolean foundRescinds) {
        for(TumorType tumorType: tumorTypes.values()) {
            if ("URMM".equals(tumorType.getCode())) {
                assertEquals("URMM rescinds size", 1, tumorType.getRescinds().size());
                assertEquals("GMUCM", tumorType.getRescinds().get(0));
                foundRescinds = Boolean.TRUE;
                // do not return because we want to check no other rescinds
            } else {
                assertEquals(0, tumorType.getRescinds().size());
            }
            foundRescinds = assertRescindsInChildren(tumorType.getChildren(), foundRescinds);
        }
        return foundRescinds;
    }

    private Boolean assertPrecursorsInChildren(Map<String, TumorType> tumorTypes, Boolean foundPrecursors) {
        for(TumorType tumorType: tumorTypes.values()) {
            if ("CLLSLL".equals(tumorType.getCode())) {
                assertEquals("'CLLSLL' precursors size", 2, tumorType.getPrecursors().size());
                assertTrue("Expected 'CLL' to be precursor to 'CLLSLL'", tumorType.getPrecursors().contains("CLL"));
                assertTrue("Expected 'SLL' to be precursor to 'CLLSLL'", tumorType.getPrecursors().contains("SLL"));
                foundPrecursors = Boolean.TRUE;
                // do not return because we want to check no other precursors
            } else {
                assertEquals(0, tumorType.getPrecursors().size());
            }
            foundPrecursors = assertPrecursorsInChildren(tumorType.getChildren(), foundPrecursors);
        }
        return foundPrecursors;
    }

    @Test
    public void testGetTumorTypesByVersion() {
        Map<String, TumorType> returnedTumorTypes = cacheUtil.getTumorTypesByVersion(legacyVersion);
        // there is only one node in returnedTumorTypes, and it is the TISSUE node, everything else is a child of that node
        String rootTumorTypeCode = returnedTumorTypes.keySet().iterator().next();
        TumorType rootTumorType = returnedTumorTypes.get(rootTumorTypeCode);
        assertEquals("TISSUE", rootTumorTypeCode);
        assertEquals("TISSUE", rootTumorType.getCode());
        assertEquals(32, rootTumorType.getChildren().size());

        // legacy version should have no history, no rescinds, no precursors
        assertNoHistoryInChildren(rootTumorType.getChildren());
        assertNoRescindsInChildren(rootTumorType.getChildren());
        assertNoPrecursorsInChildren(rootTumorType.getChildren());

        returnedTumorTypes = cacheUtil.getTumorTypesByVersion(latestVersion);

        // test latest version has a history in one node
        Boolean foundHistory = Boolean.FALSE;
        foundHistory = assertHistoryInChildren(returnedTumorTypes, foundHistory);
        assertTrue("Failed to find history for 'SS'", foundHistory);

        // test rescinds in latest version
        Boolean foundRescinds = Boolean.FALSE;
        foundRescinds = assertRescindsInChildren(returnedTumorTypes, foundRescinds);
        assertTrue("Failed to find rescinds for 'URMM'", foundRescinds);

        // test precursors in latest version
        Boolean foundPrecursors = Boolean.FALSE;
        foundPrecursors = assertPrecursorsInChildren(returnedTumorTypes, foundPrecursors);
        assertTrue("Failed to find precursors for 'CLLSLL'", foundPrecursors);
    }

}
