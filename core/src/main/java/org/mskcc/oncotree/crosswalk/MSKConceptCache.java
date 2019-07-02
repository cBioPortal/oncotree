/*
 * Copyright (c) 2017 - 2019 Memorial Sloan-Kettering Cancer Center.
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

    @Autowired
    private OncoTreePersistentCache oncoTreePersistentCache;

    public MSKConcept get(String oncoTreeCode) {
        // TODO we should not be checking crosswalk again for nodes we know are not there
        // the cache should have empty objects in it on a CrosswalkException
        // TODO should we also check backup? no, make oncoTreePersistentCache do that, probably not
        // TODO in OncoTreePersistentCache look at error code, if object not found, save 
        // empty MSKConcept object for oncoTreeCode, if something else, throw the exception
        // TODO this is not populating the backup cache, but is the main cache,
        // so is the backup cache getting populated with everything?
        try {
            return oncoTreePersistentCache.getMSKConceptFromPersistentCache(oncoTreeCode);
        } catch (CrosswalkException e) {
            return new MSKConcept(); // TODO in the bright future, this would be a new node with a cache problem or 500 error?
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron="0 0 3 * * SUN") // call every Sunday at 3am
    // this only fails if TopBraid is down; crosswalk being down does not affect the cache since it does not require a response from Crosswalk
    private void resetCache() throws Exception {
        logger.info("resetCache() -- attempting to refresh Crosswalk MSKConcept cache");
        HashMap<String, MSKConcept> latestOncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();
        ArrayList<Version> oncoTreeVersions = new ArrayList<Version>();

        // get versions, we are not updating them here
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

        // versions are ordered in ascending order by release date
        for (Version version : oncoTreeVersions) {
            ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
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
            for (OncoTreeNode node : oncoTreeNodes) {
                // check if already in latestOncoTreeCodesToMSKConcepts and then skip if it is
                if (!latestOncoTreeCodesToMSKConcepts.containsKey(node.getCode())) {
                    // pull from crosswalk first (b/c refreshing), then backup cache
                    MSKConcept concept = new MSKConcept();
                    try {
                        // TODO this needs to save the empty mskconcept, maybe in oncoTreePersistentCache
                        oncoTreePersistentCache.updateMSKConceptInPersistentCache(node.getCode());
                    } catch (CrosswalkException e) {
                        // TODO we don't want to throw an exception from one problem node,
                        // but what if all are always failing?
                        logger.error("Unable to update oncotree node with code " + node.getCode() + " from crosswalk...");
                    }
                    try {
                        concept = oncoTreePersistentCache.getMSKConceptFromPersistentCache(node.getCode());
                        if (concept == null) {
                            concept = new MSKConcept();
                        }
                    } catch (CrosswalkException e) {
                        try {
                            concept = oncoTreePersistentCache.getMSKConceptFromPersistentCacheBackup(node.getCode());
                        } catch (Exception e2) {
                            concept = new MSKConcept();
                        }
                    }
                    // at this point whatever we have save it?
                    latestOncoTreeCodesToMSKConcepts.put(node.getCode(), concept);
                }
            }
        }
        // now save mskconcepts to backup
        try  {
             oncoTreePersistentCache.backupMSKConceptPersistentCache(latestOncoTreeCodesToMSKConcepts);
        } catch (Exception e) {
            logger.error("Unable to backup MSK concepts in EHCache");
            // TODO throw exception?
        }
    }

}
