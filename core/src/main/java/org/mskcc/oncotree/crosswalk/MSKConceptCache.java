/*
 * Copyright (c) 2017 - 2020, 2024 Memorial Sloan-Kettering Cancer Center.
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

import java.util.*;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mskcc.oncotree.error.*;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.graphite.OncoTreeNode;
import org.mskcc.oncotree.graphite.OncoTreeRepository;
import org.mskcc.oncotree.graphite.OncoTreeVersionRepository;
import org.mskcc.oncotree.graphite.GraphiteException;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.mskcc.oncotree.utils.OncoTreePersistentCache;
import org.mskcc.oncotree.utils.SlackUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Autowired
    private SlackUtil slackUtil;

    // Only called when tumorTypes cache is reset
    // will always return an MSKConcept (even if empty)
    public MSKConcept get(String oncoTreeCode) {
        return oncoTreePersistentCache.getMSKConceptFromPersistentCache(oncoTreeCode);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron="0 0 3 * * SUN") // call every Sunday at 3am
    // this only fails if Graphite + EHCache is unavailable (and only breaks webapp if it occurs on startup)
    // the actual returned MSKConcept is not necessary for webapp deployment
    private void resetCache() throws Exception {
        logger.info("resetCache() -- attempting to refresh Crosswalk MSKConcept cache");
        HashMap<String, MSKConcept> latestOncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();
        ArrayList<Version> oncoTreeVersions = new ArrayList<Version>();

        try {
            oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCache();
        } catch (RuntimeException e) {
            throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache, unable to load verisons...");
        }

        for (Version version : oncoTreeVersions) {
            ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
            try {
                oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCache(version);
            } catch (RuntimeException e) {
                throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache : " + e.toString());
            }
            for (OncoTreeNode node : oncoTreeNodes) {
                // skip querying repeated nodes/MSKConcepts
                if (!latestOncoTreeCodesToMSKConcepts.containsKey(node.getCode())) {
                    // pull from crosswalk first
                    oncoTreePersistentCache.updateMSKConceptInPersistentCache(node.getCode());
                    MSKConcept concept = oncoTreePersistentCache.getMSKConceptFromPersistentCache(node.getCode());
                    latestOncoTreeCodesToMSKConcepts.put(node.getCode(), concept);
                }
            }
        }
        // save all MSKConcepts at once
        try {
            oncoTreePersistentCache.backupMSKConceptPersistentCache(latestOncoTreeCodesToMSKConcepts);
        } catch (Exception e) {
            logger.error("Unable to backup MSKConcepts in EHCache");
            slackUtil.sendSlackNotification("*OncoTree Error* - MSKConceptCache backup failed." + e.getMessage());
        }
    }

}
