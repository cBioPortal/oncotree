package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.utils.VersionUtil;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

@RestController // shorthand for @Controller, @ResponseBody
@RequestMapping(value = "/api/versions", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/versions", description = "")
public class VersionsApi {

    @ApiOperation(value = "Versions", notes = "...", response = Version.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of available versions"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Iterable<Version> versionsGet() {
        return VersionUtil.getVersions();
    }
}
