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

import org.apache.log4j.Logger;

import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.error.InvalidOncotreeMappingsParameters;
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

    private final static Logger logger = Logger.getLogger(GlobalControllerExceptionHandler.class);

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Failed to connect to TopBraid")
    @ExceptionHandler(TopBraidException.class)
    public void handleTopBraidException() {
        // nothing to do
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Failed to build OncoTree")
    @ExceptionHandler(InvalidOncoTreeDataException.class)
    public void handleInvalidOncoTreeDataException() {
        // nothing to do
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Your query parameters: vocabularyId, conceptId, histologyCode, siteCode are not valid. Please refer to the documentation")
    @ExceptionHandler(InvalidOncotreeMappingsParameters.class)
    public void handleInvalidOncotreeMappingsParameters() {
        // nothing to do
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "An unknown error occured")
    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    public void defaultErrorHandler(Exception e) {
        // note our custom exceptions above already log errors
        // an unknown exception might not log anything so log it here
        logger.error(e);
    }
}
