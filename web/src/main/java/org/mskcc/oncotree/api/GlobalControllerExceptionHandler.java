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

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mskcc.oncotree.error.*;
import org.mskcc.oncotree.topbraid.TopBraidException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author Manda Wilson
 **/
@ControllerAdvice
class GlobalControllerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "Failed to connect to TopBraid")
    @ExceptionHandler(TopBraidException.class)
    public void handleTopBraidException() {}

    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE, reason = "Failed to connect to CVS")
    @ExceptionHandler(CrosswalkServiceUnavailableException.class)
    public void handleCrosswalkServiceUnavailableException() {}

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Failed to build OncoTree")
    @ExceptionHandler(InvalidOncoTreeDataException.class)
    public void handleInvalidOncoTreeDataException() {}

    @ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Your query parameters: vocabularyId, conceptId, histologyCode, siteCode are not valid. Please refer to the documentation")
    @ExceptionHandler(InvalidOncotreeMappingsParameters.class)
    public void handleInvalidOncotreeMappingsParameters() {}

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Unexpected response returned from related system - this may be interpreted as having no oncotree code available for your request")
    @ExceptionHandler(UnexpectedCrosswalkResponseException.class)
    public void handleUnexpectedCrosswalkResponseException() {}

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No oncotree codes were mapped to your query")
    @ExceptionHandler({CrosswalkConceptNotFoundException.class, OncotreeMappingsNotFound.class})
    public void handleOncotreeMappingsNotFound() {}

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No tumor types were mapped to your query")
    @ExceptionHandler(TumorTypesNotFoundException.class)
    public void handleTumorTypesNotFoundException() {}

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Supplied version not found")
    @ExceptionHandler(InvalidVersionException.class)
    public void handleInvalidVersionException() {}

    @ExceptionHandler
    public void handleInvalidQueryException(InvalidQueryException e, HttpServletResponse response) 
        throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "An unknown error occured")
    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    public void defaultErrorHandler(Exception e) {
        // note our custom exceptions above already log errors
        // an unknown exception might not log anything so log it here
        logger.error(ExceptionUtils.getStackTrace(e));
    }
}
