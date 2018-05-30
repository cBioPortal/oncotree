/** Copyright (c) 2017-2018 Memorial Sloan-Kettering Cancer Center.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;

import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.MainTypesUtil;
import org.mskcc.oncotree.utils.TumorTypesUtil;
import org.mskcc.oncotree.utils.VersionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RestController // shorthand for @Controller, @ResponseBody
@RequestMapping(value = "/api/mainTypes", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/mainTypes", description = "the mainTypes API")
public class MainTypesApi {

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private MainTypesUtil mainTypesUtil;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Autowired
    private VersionUtil versionUtil;

    @ApiOperation(value = "Return all available main tumor types.", notes = "")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Nested tumor types object"),
        @ApiResponse(code = 404, message = "Could not find maintypes"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public Iterable<String> mainTypesGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION  + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {
        Version v = (version == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(version);
        Map<String, TumorType> tumorTypes = cacheUtil.getTumorTypesByVersion(v);
        Set<TumorType> tumorTypesSet = tumorTypesUtil.flattenTumorTypes(tumorTypes, version);
        return mainTypesUtil.getMainTypesByTumorTypes(tumorTypesSet);
    }
}
