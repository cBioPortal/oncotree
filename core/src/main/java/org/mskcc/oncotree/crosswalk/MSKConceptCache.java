/*
 * Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.crosswalk;

import org.mskcc.oncotree.error.*;
import java.util.*;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.mskcc.oncotree.utils.OncoTreePersistentCache;
import org.mskcc.oncotree.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 *
 * @author Manda Wilson
 **/
@Component
@EnableScheduling
public class MSKConceptCache {

    private static final Logger logger = LoggerFactory.getLogger(MSKConceptCache.class);
    private static HashMap<String, MSKConcept> oncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();

    @Autowired
    private OncoTreePersistentCache oncoTreePersistentCache;

    public MSKConcept get(String oncoTreeCode) {
        if (oncoTreeCodesToMSKConcepts.containsKey(oncoTreeCode)) {
            logger.debug("get(" + oncoTreeCode + ") -- in cache");
            return oncoTreeCodesToMSKConcepts.get(oncoTreeCode);
        }
        logger.debug("get(" + oncoTreeCode + ") -- NOT in cache, query crosswalk");
        MSKConcept concept = getFromCrosswalk(oncoTreeCode);
        // save even if has no information in it
        oncoTreeCodesToMSKConcepts.put(oncoTreeCode, concept);
        return concept;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron="0 0 3 * * SUN") // call every Sunday at 3am
    // this only fails if TopBraid is down; crosswalk being down does not affect the cache since it does not require a response from Crosswalk 
    private void resetCache() throws Exception {
        logger.info("resetCache() -- attempting to refresh  Crosswalk MSKConcept cache");
        HashMap<String, MSKConcept> latestOncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();
        ArrayList<Version> oncoTreeVersions = new ArrayList<Version>();
       
        // update verisons in EHCache, extract from EHCache, backup if update was successful 
        boolean failedOncoTreeVersionsCacheRefresh = false;
        try {
            oncoTreePersistentCache.updateOncoTreeVersionsInPersistentCache();
        } catch (TopBraidException exception) {
            logger.error("resetCache() -- failed to pull versions from repository");
            failedOncoTreeVersionsCacheRefresh = true;
        }
        
        try {
            oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCache();
        } catch (TopBraidException e) {
            // unable to get value from persistentCache - attempt to extract from backup
            try {
                logger.error("Unable to load versions from default EHCache... attempting to read from backup.");
                oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCacheBackup();
                if (oncoTreeVersions == null) {
                    throw new FailedCacheRefreshException("No data found in specified backup cache location...");
                }
            } catch (Exception e2) {
                logger.error("Unable to load versions from backup EHCache..." + e2.getMessage());
                throw new FailedCacheRefreshException("Unable to load versions from backup cache...");
            }
        }
        if (!failedOncoTreeVersionsCacheRefresh) {
            try {
                oncoTreePersistentCache.backupOncoTreeVersionsPersistentCache(oncoTreeVersions);
            } catch (Exception e) {
                logger.error("Unable to backup versions EHCache");
            }
        }

        // versions are ordered in ascending order by release date
        // for every version, update oncotree nodes in EHCache, extract from EHCache, and backup if successful
        for (Version version : oncoTreeVersions) {
            boolean failedVersionedOncoTreeNodesCacheRefresh = false;       
            ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
            try {
                ;//oncoTreePersistentCache.updateOncoTreeNodesInPersistentCache(version);
            } catch (TopBraidException e) {
                logger.error("resetCache() -- failed to pull tumor types for version '" + version.getVersion() + "' from repository");
                failedVersionedOncoTreeNodesCacheRefresh = true;
            }
            try {
                oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCache(version);
            } catch (TopBraidException e) {
                try {
                    logger.error("Unable to load versions from default EHCache... attempting to read from backup.");
                    oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCacheBackup(version);
                    if (oncoTreeNodes == null) {
                        logger.error("No data found for version " + version.getVersion() + " in backup EHCache");
                        throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache");
                    }
                } catch (Exception e2) {
                    logger.error("Unable to load oncotree nodes for version " + version.getVersion() + " from backup cache...");
                    throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache");
                } 
            }
            if (!failedVersionedOncoTreeNodesCacheRefresh) {
                try {
                    oncoTreePersistentCache.backupOncoTreeNodesPersistentCache(oncoTreeNodes, version);
                } catch (Exception e) {
                    logger.error("Unable to backup oncotree nodes in EHCche");
                }
            }
            for (OncoTreeNode node : oncoTreeNodes) {
                MSKConcept mskConcept = getFromCrosswalk(node.getCode());
                latestOncoTreeCodesToMSKConcepts.put(node.getCode(), mskConcept);
            }
        }
        oncoTreeCodesToMSKConcepts = latestOncoTreeCodesToMSKConcepts;
    }

    // returns default MSKConcept when unable to fetch from crosswalk (existing behavior)
    private MSKConcept getFromCrosswalk(String oncoTreeCode) {
        MSKConcept concept = new MSKConcept();
        boolean failedUpdateMskConceptInPersistentCache = false;
        try {
            oncoTreePersistentCache.updateMSKConceptInPersistentCache(oncoTreeCode);
        } catch (CrosswalkException e) {
            failedUpdateMskConceptInPersistentCache = true;
        }
        try {
            concept = oncoTreePersistentCache.getMSKConceptFromPersistentCache(oncoTreeCode); 
        } catch (CrosswalkException e) {
            try {
                concept = oncoTreePersistentCache.getMSKConceptFromPersistentCacheBackup(oncoTreeCode);
                if (concept == null) {
                    return new MSKConcept();
                }
            } catch (Exception e2) {
                return new MSKConcept();
            }                       
        }
        if (!failedUpdateMskConceptInPersistentCache) {
            try {
                oncoTreePersistentCache.backupMSKConceptPersistentCache(concept, oncoTreeCode);
            } catch (Exception e) {
                logger.error("Unable to backup MSKConcpet EHCache");
            }
        }
        return concept;
    }
}
