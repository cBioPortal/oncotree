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
import org.mskcc.oncotree.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static HashMap<String, MSKConcept> oncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();

    @Autowired
    private OncoTreeRepository oncoTreeRepository;

    @Autowired
    private CrosswalkRepository crosswalkRepository;

    @Autowired
    private OncoTreeVersionRepository oncoTreeVersionRepository;

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

    @PostConstruct // call when constructed
    @Scheduled(cron="0 0 3 * * SUN") // call every Sunday at 3am
    private void resetCache() {
        logger.info("resetCache() -- attempting to refresh  Crosswalk MSKConcept cache");
        HashMap<String, MSKConcept> latestOncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();
        List<Version> versions = new ArrayList<Version>();
        try {
            versions = oncoTreeVersionRepository.getOncoTreeVersions();
        } catch (TopBraidException e) {
            logger.error("resetCache() -- failed to pull versions from repository");
            throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache");
        }
        // versions are ordered in ascending order by release date
        for (Version version : versions) {
            List<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
            try {
                oncoTreeNodes = oncoTreeRepository.getOncoTree(version);
            } catch (TopBraidException e) {
                logger.error("resetCache() -- failed to pull a versioned OncoTree");
                throw new FailedCacheRefreshException("Failed to refresh MSKConceptCache");
            }
            for (OncoTreeNode node : oncoTreeNodes) {
                MSKConcept mskConcept = getFromCrosswalk(node.getCode());
                latestOncoTreeCodesToMSKConcepts.put(node.getCode(), mskConcept);
            }
        }
        oncoTreeCodesToMSKConcepts = latestOncoTreeCodesToMSKConcepts;
    }

    private MSKConcept getFromCrosswalk(String oncoTreeCode) {
        MSKConcept concept = new MSKConcept();
        try {
            concept = crosswalkRepository.getByOncotreeCode(oncoTreeCode);
        } catch (CrosswalkException e) {
            // do nothing
        }
        return concept;
    }
}
