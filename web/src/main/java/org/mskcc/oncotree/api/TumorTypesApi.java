package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.mskcc.oncotree.error.InvalidQueryException;
import org.mskcc.oncotree.error.TumorTypesNotFoundException;
import org.mskcc.oncotree.model.*;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.TumorTypesUtil;
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
@RequestMapping(value = "/api/tumorTypes", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/tumorTypes", description = "")
public class TumorTypesApi {

    private static final Logger logger = LoggerFactory.getLogger(TumorTypesApi.class);

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Autowired
    private VersionUtil versionUtil;

    @ApiOperation(value = "Return all available tumor types.", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Nested tumor types object."),
        @ApiResponse(code = 404, message = "Could not find tumor types"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "/tree",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Map<String, TumorType> tumorTypesGetTree(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {
        Version v = (version == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(version);
        Map<String, TumorType> tumorTypes = cacheUtil.getTumorTypesByVersion(v);
        logger.debug("tumorTypesGetTree() -- returning " + tumorTypes.size() + " tumor types");
        return tumorTypes;
    }

    @ApiOperation(value = "Return all available tumor types as a list.", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tumor Types list."),
        @ApiResponse(code = 404, message = "Could not find tumor types"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Iterable<TumorType> tumorTypesGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {

        Version v = (version == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(version);
        Map<String, TumorType> tumorTypes = cacheUtil.getTumorTypesByVersion(v);
        Set<TumorType> tumorTypesSet = tumorTypesUtil.flattenTumorTypes(tumorTypes, null);
        logger.debug("tumorTypesGet() -- returning " + tumorTypesSet.size() + " tumor types");
        return tumorTypesSet;
    }

    // TODO add this @ApiIgnore
    @ApiOperation(value = "Translates the source version code into the equivalent code in target version.", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tumor Type."),
        @ApiResponse(code = 400, message = "Bad request for versions and/or source code"),
        @ApiResponse(code = 404, message = "Could not find source code in target version"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "/translate",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public List<TumorType> translateGet(
        @ApiParam(value = "The source version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "sourceVersion", required = true) String sourceVersion,
        @ApiParam(value = "The target version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "targetVersion", required = false) String targetVersion,
        @ApiParam(value = "The code of the source tumor type.")
        @RequestParam(value = "sourceCode", required = true) String sourceCode
    ) {
        if (StringUtils.isEmpty(sourceCode)) {
            throw new InvalidQueryException("'sourceCode' is a required parameter");
        }
        Version sourceV = versionUtil.getVersion(sourceVersion);
        Version targetV = (targetVersion == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(targetVersion);
        // TODO put in TumorTypesUtil
        Map<String, TumorType> allSourceTumorTypes = cacheUtil.getTumorTypesByVersion(sourceV);
        Map<String, TumorType> allTargetTumorTypes = cacheUtil.getTumorTypesByVersion(targetV);
        Set<TumorType> allSourceTumorTypesSet = tumorTypesUtil.flattenTumorTypes(allSourceTumorTypes, null);
        Set<TumorType> allTargetTumorTypesSet = tumorTypesUtil.flattenTumorTypes(allTargetTumorTypes, null);

        // find tumor type by code
        List<TumorType> targetTumorTypes = new ArrayList<TumorType>();
        TumorType sourceTumorType = null;
        for (TumorType source : allSourceTumorTypesSet) {
            if (source.getCode().equals(sourceCode)) {
                sourceTumorType = source;
                break;
            }
        }
        if (sourceTumorType == null) {
            throw new InvalidQueryException("No tumor type matching '" + sourceCode + "' in version '" + sourceV.getVersion() + "' found");
        }

        // if source and target version are the same don't bother with lookup
        if (sourceV.equals(targetV)) {
            targetTumorTypes.add(sourceTumorType);
        } else {
            for (TumorType target : allTargetTumorTypesSet) {
                if (target.getUri().equals(sourceTumorType.getUri())) {
                    targetTumorTypes.add(target);
                    // TODO remove? break;
                } else {

                }
            }
            if (targetTumorTypes.size() == 0) {
                throw new TumorTypesNotFoundException("No tumor type matching '" + sourceCode + "' in '" + sourceV.getVersion() + "' found in version '" + targetV.getVersion() + "'");
            }
        }
        logger.debug("translateGet() -- returning '" + targetTumorTypes.size() + "' tumor types");
        return targetTumorTypes;
    }

    @ApiIgnore
    @ApiOperation(value = "Tumor Types", notes = "...", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "An array of tumor types")
        }
    )
    @RequestMapping(value = "/search",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.POST)
    public List<List<TumorType>> tumorTypesSearchPost(

        @ApiParam(value = "queries", required = true) @RequestBody TumorTypeQueries queries
    ) {

        Version v = queries.getVersion() != null ? versionUtil.getVersion(queries.getVersion()) : versionUtil.getDefaultVersion();
        List<List<TumorType>> tumorTypes = new ArrayList<>();

        // Cache in tumor types in case no data present
        cacheUtil.getTumorTypesByVersion(v);

        // each query has a list of results
        for (TumorTypeQuery query : queries.getQueries()) {
            List<TumorType> matchedTumorTypes = new ArrayList<>();
            matchedTumorTypes = v == null ? new ArrayList<TumorType>() : tumorTypesUtil.findTumorTypesByVersion(query.getType(), query.getQuery(), query.getExactMatch(), v, false);
            tumorTypes.add(matchedTumorTypes);
        }
        return tumorTypes;
    }


    @ApiOperation(value = "Tumor Types", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "An array of tumor types"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "Could not find tumor types"),
        @ApiResponse(code = 503, message = "Required data source unavailable")
        }
    )
    @RequestMapping(value = "/search/{type}/{query}",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Iterable<TumorType> tumorTypesSearchTypeQueryQueryGet(
        @ApiParam(value = "Query type. It could be 'code', 'name', 'mainType', 'level', 'nci', 'umls' or 'color'.", required = true)
        @PathVariable("type") String type,
        @ApiParam(value = "The query content", required = true)
        @PathVariable("query") String query,
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version,
        @ApiParam(value = "If it sets to true, it will only return one element array.", defaultValue = "true")
        @RequestParam(value = "exactMatch", required = false, defaultValue = "true") Boolean exactMatch,
        @ApiParam(value = "Tumor type levels. 1-5. By default, it doesn't includes tissue which is the primary level.", defaultValue = "2,3,4,5")
        @RequestParam(value = "levels", required = false, defaultValue = "1,2,3,4,5") String levels
    ) {
        List<TumorType> matchedTumorTypes = new ArrayList<>();
        Version v = (version == null) ? versionUtil.getDefaultVersion() : versionUtil.getVersion(version);

        // Cache in tumor types in case no data present
        cacheUtil.getTumorTypesByVersion(v);

        matchedTumorTypes = v == null ? new ArrayList<TumorType>() : tumorTypesUtil.findTumorTypesByVersion(type, query, exactMatch, v, false);
        // check that user did not do a "level" query, but is doing a "levels" filter
        if (!type.toLowerCase().equals("level") && !StringUtils.isBlank(levels)) {
            List<String> ls = Arrays.asList(levels.split(","));
            List<Integer> levelList = new ArrayList<>();
            for (String l : ls) {
                try {
                    Integer level = new Integer(Integer.parseInt(l.trim()));
                    levelList.add(level);
                } catch (NumberFormatException e) {
                    throw new InvalidQueryException("'" + l + "' is not a valid level.  Level must be an integer.");
                }
            }
            matchedTumorTypes = tumorTypesUtil.filterTumorTypesByLevel(matchedTumorTypes, levelList);
        }
        if (matchedTumorTypes.isEmpty()) {
            throw new TumorTypesNotFoundException("No tumor types found matching supplied query");
        }
        return matchedTumorTypes;
    }
}
