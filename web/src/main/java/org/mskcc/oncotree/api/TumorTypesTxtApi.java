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

import java.io.InputStream;
import java.lang.Deprecated;
import java.util.HashMap;
import java.util.Map;

import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.TumorTypesUtil;
import org.mskcc.oncotree.utils.VersionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestController // shorthand for @Controller, @ResponseBody
@RequestMapping(value = "/api/tumor_types.txt", produces = {TEXT_PLAIN_VALUE})
@Api(value = "/tumor_types.txt", description = "")
public class TumorTypesTxtApi {

    @Autowired
    private VersionUtil versionUtil;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Autowired
    private CacheUtil cacheUtil;

    @Deprecated
    @ApiOperation(value = "Tumor Types in plain text format.", notes = "Return all available tumor types.", response = Void.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tumor types text file."),
        @ApiResponse(code = 404, message = "Could not find tumor types text file"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "",
        produces = {TEXT_PLAIN_VALUE},
        method = RequestMethod.GET)
    public InputStreamResource tumorTypesTxtGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {
        Map<String, TumorType> tumorTypes = new HashMap<>();
        Version v = (version == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(version);
        tumorTypes = cacheUtil.getTumorTypesByVersion(v);
        InputStream inputStream = tumorTypesUtil.getTumorTypeInputStream(tumorTypes);
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
        return inputStreamResource;
    }
}
