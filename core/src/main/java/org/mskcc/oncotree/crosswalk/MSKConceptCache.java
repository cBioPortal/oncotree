/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

import org.apache.log4j.Logger;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.utils.VersionUtil;
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

    private final static Logger logger = Logger.getLogger(MSKConceptCache.class);
    private static HashMap<String, MSKConcept> oncoTreeCodesToMSKConcepts = new HashMap<String, MSKConcept>();

    @Autowired
    private OncoTreeRepository oncoTreeRepository;

    @Autowired
    private CrosswalkRepository crosswalkRepository;

    // note we only @Autowire this so that it isn't
    // null in the @PostConstruct call to resetCache()
    @Autowired
    private VersionUtil versionUtil;

    public MSKConcept get(String oncoTreeCode) {
        if (oncoTreeCodesToMSKConcepts.containsKey(oncoTreeCode)) {
            logger.debug("get(" + oncoTreeCode + ") -- in cache");
            return oncoTreeCodesToMSKConcepts.get(oncoTreeCode);
        }
        logger.debug("get(" + oncoTreeCode + ") -- NOT in cache, query crosswalk");
        return getFromCrosswalkAndSave(oncoTreeCode);
    }

    @PostConstruct // call when constructed
    @Scheduled(cron="0 0 3 * * SUN") // call every Sunday at 3am
    private void resetCache() {
        logger.info("resetCache() -- clearing Crosswalk MSKConcept cache and refilling");
        oncoTreeCodesToMSKConcepts.clear();
        List<OncoTreeNode> oncoTreeNodes = oncoTreeRepository.getOncoTree(VersionUtil.getDefaultVersion());
        for (OncoTreeNode node : oncoTreeNodes) {
            getFromCrosswalkAndSave(node.getCode());
        } 
    }

    private MSKConcept getFromCrosswalkAndSave(String oncoTreeCode) {
        MSKConcept concept = null;
        try {
            concept = crosswalkRepository.getByOncotreeCode(oncoTreeCode);
        } catch (CrosswalkException e) {
            // do nothing
        }
        // save even if null
        oncoTreeCodesToMSKConcepts.put(oncoTreeCode, concept);
        return concept;
    }
}
