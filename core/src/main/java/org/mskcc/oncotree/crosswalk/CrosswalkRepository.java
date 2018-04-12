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

import java.util.*;

import org.mskcc.oncotree.error.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class CrosswalkRepository {

    private static final Logger logger = LoggerFactory.getLogger(CrosswalkRepository.class);

    // URI variables should be vocabularyId={vocabularyId}&conceptId={conceptId}&histologyCode={histologyCode}&siteCode={siteCode}
    @Value("${crosswalk.url}")
    private String crosswalkURL;

    public MSKConcept getByOncotreeCode(String oncotreeCode)
            throws CrosswalkException {
        return queryCVS("ONCOTREE", oncotreeCode, null, null);
    }

    public MSKConcept queryCVS(String vocabularyId, String conceptId, String histologyCode, String siteCode)
            throws CrosswalkException {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<MSKConcept> response = restTemplate.getForEntity(crosswalkURL, MSKConcept.class, vocabularyId, conceptId, histologyCode, siteCode);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("queryCVS() -- caught HttpStatusCodeException: " + e);
            String errorString = "URI: " + crosswalkURL + "\n" +
                "vocabularyId: " + vocabularyId + "\n" +
                "conceptId: " + conceptId + "\n" +
                "histologyCode: " + histologyCode + "\n" +
                "siteCode: " + siteCode + "\n";
            if (e.getStatusCode().is4xxClientError()) {
                throw new CrosswalkConceptNotFoundException("4xx Error while getting data from CVS Service with: \n" + errorString, e);
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new CrosswalkServiceUnavailableException("5xx Error while getting data from CVS", e);
            }
            throw new UnexpectedCrosswalkResponseException("Exception while getting data from CVS Service with: \n" + errorString, e);
        } catch (RestClientException e) {
            logger.error("queryCVS() -- caught RestClientErrorException: " + e);
            String errorString = "URI: " + crosswalkURL + "\n" +
                "vocabularyId: " + vocabularyId + "\n" +
                "conceptId: " + conceptId + "\n" +
                "histologyCode: " + histologyCode + "\n" +
                "siteCode: " + siteCode + "\n";
            throw new UnexpectedCrosswalkResponseException("RestClientException while getting data from CVS Service" + errorString, e);
        }
    }
}
