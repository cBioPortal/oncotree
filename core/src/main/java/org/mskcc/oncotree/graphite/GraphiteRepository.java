/*
 * Copyright (c) 2017, 2024 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.graphite;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public abstract class GraphiteRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(GraphiteRepository.class);

    @Value("${graphite.url}")
    private String graphiteURL;

    @Value("${graphite.username}")
    private String graphiteUsername;

    @Value("${graphite.password}")
    private String graphitePassword;

    protected T query(String query, ParameterizedTypeReference<T> parameterizedType)
            throws GraphiteException {
        return query(query, parameterizedType, true);
    }

    private T query(String query, ParameterizedTypeReference<T> parameterizedType, boolean attemptAgainOnFailure)
            throws GraphiteException {
        logger.debug("query() -- graphite url: " + graphiteURL);
        String encodedCredentials = Base64.getEncoder().encodeToString((graphiteUsername + ":" + graphitePassword).getBytes(StandardCharsets.UTF_8));
        RestTemplate restTemplate = new RestTemplate();

        // the default supported types for MappingJackson2HttpMessageConverter are:
        //   application/json and application/*+json
        // our response content type is application/sparql-results+json-simple
        // NOTE: if the response content type was one of the default types we
        //   would not have to add the message converter to the rest template
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setSupportedMediaTypes(Collections.singletonList(
            new MediaType("application","sparql-results+json")));
        restTemplate.getMessageConverters().add(messageConverter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Basic " + encodedCredentials);

        String requestBody = "query=" + query;
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // NOTE ParameterizedTypeReference cannot be made generic, that is why child class passes it
        // See: http://stackoverflow.com/questions/21987295/using-spring-resttemplate-in-generic-method-with-generic-parameter
        try {
            ResponseEntity<T> response = restTemplate.exchange(graphiteURL,
                    HttpMethod.POST,
                    entity,
                    parameterizedType);
            return response.getBody();
        } catch (RestClientException e) {
            logger.debug("query() -- caught RestClientException");
            // see if we should try again
            if (attemptAgainOnFailure == true) {
                return query(query, parameterizedType, false); // do not make a second attempt
            }
            throw new GraphiteException("Failed to connect to Graphite", e);
        }
    }
}
