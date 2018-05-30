package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;

import org.mskcc.oncotree.model.*;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.VersionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;

import springfox.documentation.annotations.ApiIgnore;

@RestController // shorthand for @Controller, @ResponseBody
@RequestMapping(value = "/api", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/", description = "API")
public class RootApi {

    private static final Logger logger = LoggerFactory.getLogger(RootApi.class);

    @Autowired
    private CacheUtil cacheUtil;

    @ApiIgnore
    @ApiOperation(value = "Refresh cache.", notes = "", response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Nested tumor types object."),
        @ApiResponse(code = 503, message = "Failed to connect to repository to refresh cache")})
    @RequestMapping(value = "/refreshCache",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Map<String, String> refreshCache() {
        cacheUtil.resetCache();
        logger.debug("refreshCache() -- refreshCache endpoint successful");
        return Collections.singletonMap("response", "Success!");
    }

}
