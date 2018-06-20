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

import org.mskcc.oncotree.error.*;
import java.util.*;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.utils.VersionUtil;
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
    // use this to store and look up previous oncoTree codes
    private static HashMap<String, HashSet<String>> topBraidURIsToOncotreeCodes = new HashMap<String, HashSet<String>>();

    @Autowired
    private OncoTreeRepository oncoTreeRepository;

    @Autowired
    private CrosswalkRepository crosswalkRepository;

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
        // versions are ordered in ascending order by release date
        for (Version version : versionUtil.getVersions()) {
            List<OncoTreeNode> oncoTreeNodes = oncoTreeRepository.getOncoTree(version);
            for (OncoTreeNode node : oncoTreeNodes) {
                MSKConcept mskConcept = getFromCrosswalkAndSave(node.getCode());
                // get all codes defined so far for this topbraid uri and save in history
                if (topBraidURIsToOncotreeCodes.containsKey(node.getURI())) {
                    // do not add this code to the history, but add any others
                    HashSet<String> allButThisNode = new HashSet<String>(topBraidURIsToOncotreeCodes.get(node.getURI()));
                    allButThisNode.remove(node.getCode());
                    mskConcept.addHistory(allButThisNode);
                } else {
                    topBraidURIsToOncotreeCodes.put(node.getURI(), new HashSet<String>());
                }
                // TODO replace this hack for adding CLL to the history with something smarter
                if (node.getURI().endsWith("ONC000369")) {
                    mskConcept.addHistory("CLL");
                    topBraidURIsToOncotreeCodes.get(node.getURI()).add("CLL");
                }
                // now save this as onoctree code history for this topbraid uri
                topBraidURIsToOncotreeCodes.get(node.getURI()).add(node.getCode());
            }
        }
    }

    private MSKConcept getFromCrosswalkAndSave(String oncoTreeCode) {
        // only save if we have not seen before (UMLS/NCI info will not be different)
        if (oncoTreeCodesToMSKConcepts.containsKey(oncoTreeCode)) {
            return oncoTreeCodesToMSKConcepts.get(oncoTreeCode);
        }
        MSKConcept concept = new MSKConcept();
        try {
            concept = crosswalkRepository.getByOncotreeCode(oncoTreeCode);
        } catch (CrosswalkException e) {
            // do nothing
        }
        // save even if has no information in it
        oncoTreeCodesToMSKConcepts.put(oncoTreeCode, concept);
        return concept;
    }
}
