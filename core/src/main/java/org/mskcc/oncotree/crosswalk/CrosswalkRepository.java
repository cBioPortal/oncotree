/*
 * Copyright (c) 2017 - 2020 Memorial Sloan-Kettering Cancer Center.
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

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.mskcc.oncotree.error.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpServerErrorException;
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

    @Value("${crosswalk.disable_cvs_querying:false}")
    private Boolean DISABLE_CVS_QUERYING;

    private static final String STATIC_CROSSWALK_FILENAME = "staticCrosswalkOncotreeMappings.txt";
    private Map<String, MSKConcept> parsedStaticResource = null;

    public MSKConcept getByOncotreeCode(String oncotreeCode)
            throws CrosswalkException {
        return queryCVS("ONCOTREE", oncotreeCode, null, null);
    }

    public MSKConcept queryCVS(String vocabularyId, String conceptId, String histologyCode, String siteCode)
            throws CrosswalkException {
        if (DISABLE_CVS_QUERYING) {
            return respondToQueryFromStaticResource(vocabularyId, conceptId, histologyCode, siteCode);
        }
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

    private MSKConcept respondToQueryFromStaticResource(String vocabularyId, String conceptId, String histologyCode, String siteCode) {
        if (vocabularyId == null || !vocabularyId.equals("ONCOTREE")) {
            // static resource only handles oncotree queries
            throw new CrosswalkServiceUnavailableException("CVS server queries have been disabled (crosswalk.disable_cvs_querying set to true)", new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        }
        parseCrosswalkResourceFileIfNeeded();
        MSKConcept concept = parsedStaticResource.get(conceptId);
        return concept;
    }

    private void parseCrosswalkResourceFileIfNeeded() {
        if (parsedStaticResource == null) {
            parsedStaticResource = new HashMap<String, MSKConcept>();
            parseCrosswalkResourceFile();
        }
    }

    private void parseCrosswalkResourceFile() {
        try {
            Resource resource = new ClassPathResource(STATIC_CROSSWALK_FILENAME);
            InputStream inputStream = resource.getInputStream();
            InputStreamReader isreader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(isreader);
            while (reader.ready()) {
                String line = reader.readLine();
                String columns[] = line.split("\t");
                if (columns.length != 3) {
                    throw new RuntimeException("error : could not parse static file with crosswalk mappings - wrong column count");
                }
                String code = columns[0];
                String nci[] = columns[1].split(",");
                String umln[] = columns[2].split(",");
                MSKConcept concept = new MSKConcept();
                List<String> oncotreeCodes = new ArrayList<>();
                oncotreeCodes.add(code);
                concept.setOncotreeCodes(oncotreeCodes);
                concept.setConceptIds(Arrays.asList(umln));
                HashMap<String, List<String>> crosswalks = new HashMap<String, List<String>>();
                crosswalks.put("NCI", Arrays.asList(nci));
                concept.setCrosswalks(crosswalks);
                parsedStaticResource.put(code, concept);
            }
        } catch (IOException e) {
            throw new RuntimeException("error : could not parse static file with crosswalk mappings", e);
        }

    }

}
