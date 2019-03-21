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

package org.mskcc.oncotree.api;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mskcc.oncotree.crosswalk.CrosswalkRepository;
import org.mskcc.oncotree.crosswalk.MSKConcept;
import org.mskcc.oncotree.error.InvalidOncotreeMappingsParameters;
import org.mskcc.oncotree.error.OncotreeMappingsNotFound;
import org.mskcc.oncotree.utils.ApiUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OncotreeMappingsApi {

    @Autowired
    private CrosswalkRepository crosswalkRepository;

    @Autowired
    private ApiUtil apiUtil;

    private final static int MAX_VOCABULARY_ID_ARG_LENGTH = 24;
    private final static int MAX_CONCEPT_ID_ARG_LENGTH = 36;
    private final static int MAX_HISTOLOGY_CODE_ARG_LENGTH = 36;
    private final static int MAX_SITE_CODE_ARG_LENGTH = 36;
    private static final Logger logger = LoggerFactory.getLogger(OncotreeMappingsApi.class);

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "An array of mapped OncoTree codes"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "Could not find OncoTree mapping"), 
        @ApiResponse(code = 503, message = "Required data source unavailable") 
        }   
    )
    @RequestMapping(value = "api/crosswalk", method = RequestMethod.GET)
    public Iterable<String> getMappings(
            @RequestParam(value="vocabularyId", required=false) String vocabularyId,
            @RequestParam(value="conceptId", required=false) String conceptId,
            @RequestParam(value="histologyCode", required=false) String histologyCode,
            @RequestParam(value="siteCode", required=false) String siteCode) {
            String cleanVocabularyId = apiUtil.cleanArgument(vocabularyId);
            String cleanConceptId = apiUtil.cleanArgument(conceptId);
            String cleanHistologyCode = apiUtil.cleanArgument(histologyCode);
            String cleanSiteCode = apiUtil.cleanArgument(siteCode);
            MSKConcept mskConcept = new MSKConcept();
        if (!mappingParametersAreValid(cleanVocabularyId, cleanConceptId, cleanHistologyCode, cleanSiteCode)) {
            throw new InvalidOncotreeMappingsParameters("Your query parameters, vocabularyId: " + cleanVocabularyId +
                    ", conceptId: " + cleanConceptId + ", histologyCode: " + cleanHistologyCode +
                    ", siteCode: " + cleanSiteCode + " are not valid. Please refer to the documentation");
        }
        mskConcept = crosswalkRepository.queryCVS(cleanVocabularyId, cleanConceptId, cleanHistologyCode, cleanSiteCode);
        return extractOncotreeMappings(mskConcept);
    }

    private static boolean mappingParametersAreValid (
            String vocabularyId,
            String conceptId,
            String histologyCode,
            String siteCode) {
        // vocabularyId is required for all queries
        if (!requiredArgumentIsValid(vocabularyId, MAX_VOCABULARY_ID_ARG_LENGTH)) {
            return false;
        }
        if (conceptId != null && conceptId.length() > 0) {
            // Query by Concept Id 
            if (conceptId.length() > MAX_CONCEPT_ID_ARG_LENGTH) {
                return false;
            }
            // if there's a conceptId, histology and site must both be empty
            if (!prohibitedArgumentIsEmpty(histologyCode)) {
                return false;
            }
            if (!prohibitedArgumentIsEmpty(siteCode)) {
                return false;
            }
        } else {
            // Query by Histology and Site
            if (!requiredArgumentIsValid(histologyCode, MAX_HISTOLOGY_CODE_ARG_LENGTH )) {
                return false;
            }
            if (!requiredArgumentIsValid(siteCode, MAX_SITE_CODE_ARG_LENGTH)) {
                return false;
            }
        }
        return true;
    }

    private static boolean requiredArgumentIsValid(String arg, int max_length) {
        return arg != null && arg.length() > 0 && arg.length() <= max_length;
    }

    private static boolean prohibitedArgumentIsEmpty(String arg) {
        return arg == null || arg.length() == 0;
    }

    private Iterable<String> extractOncotreeMappings(MSKConcept mskConcept) {
        List<String> oncotreeCodes = null;
        if (mskConcept != null) {
            if (mskConcept.getCrosswalks() != null && mskConcept.getCrosswalks().size() > 0) {
                if (mskConcept.getCrosswalks().containsKey("ONCOTREE")) {
                    logger.info("OncoTree mskConcept found for concept id " + mskConcept.getConceptIds().get(0));
                    oncotreeCodes = mskConcept.getCrosswalks().get("ONCOTREE");
                }
            } else if (mskConcept.getOncotreeCodes() != null && mskConcept.getOncotreeCodes().size() > 0) {
                logger.info("OncoTree mskConcept found for concept id " + mskConcept.getOncotreeCodes().toString());
                oncotreeCodes = mskConcept.getOncotreeCodes();
            }
        }
        if (oncotreeCodes == null || oncotreeCodes.isEmpty()) {
            throw new OncotreeMappingsNotFound("There is no OncoTree code mapped to the query");
        }
        return oncotreeCodes;
    }

}
