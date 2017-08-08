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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class CrosswalkRepository {

    private final static Logger logger = Logger.getLogger(CrosswalkRepository.class);

    @Value("${crosswalk.url}")
    private String crosswalkURL;

    protected MSKConcept getByOncotreeCode(String oncotreeCode)
            throws CrosswalkException {
        RestTemplate restTemplate = new RestTemplate();
        String url = crosswalkURL + oncotreeCode;
        try {
            ResponseEntity<MSKConcept> response = restTemplate.getForEntity(url, MSKConcept.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("getByOncotreeCode() -- caught RestClientException: " + e);
            throw new CrosswalkException("Exception querying url '" + url + "'", e);
        }
    }
}
