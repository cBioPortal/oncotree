package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.mskcc.oncotree.error.InvalidQueryException;
import org.mskcc.oncotree.model.*;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.TumorTypesUtil;
import org.mskcc.oncotree.utils.VersionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @ApiOperation(value = "Return all available tumor types.", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Nested tumor types object.")})
    @RequestMapping(value = "/tree",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Map<String, TumorType> tumorTypesGetTree(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {
        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);
        Map<String, TumorType> tumorTypes = CacheUtil.getTumorTypesByVersion(v);
        logger.debug("tumorTypesGetTree() -- returning " + tumorTypes.size() + " tumor types");
        return tumorTypes;
    }

    @ApiOperation(value = "Return all available tumor types as a list.", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tumor Types list.")})
    @RequestMapping(value = "",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Iterable<TumorType> tumorTypesGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
    ) {

        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);
        Map tumorTypes = CacheUtil.getTumorTypesByVersion(v);
        Set tumorTypesSet = TumorTypesUtil.flattenTumorTypes(tumorTypes, null);
        logger.debug("tumorTypesGet() -- returning " + tumorTypesSet.size() + " tumor types");
        return tumorTypesSet;
    }

    @ApiIgnore
    @ApiOperation(value = "Tumor Types", notes = "...", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.POST)
    public List<List<TumorType>> tumorTypesSearchPost(

        @ApiParam(value = "queries", required = true) @RequestBody TumorTypeQueries queries
    ) {

        Version v = queries.getVersion() != null ? VersionUtil.getVersion(queries.getVersion()) : VersionUtil.getDefaultVersion();
        List<List<TumorType>> tumorTypes = new ArrayList<>();

        // Cache in tumor types in case no data present
        CacheUtil.getTumorTypesByVersion(v);

        // each query has a list of results
        for (TumorTypeQuery query : queries.getQueries()) {
            List<TumorType> matchedTumorTypes = new ArrayList<>();
            matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(query.getType(), query.getQuery(), query.getExactMatch(), v, false);
            tumorTypes.add(matchedTumorTypes);
        }
        return tumorTypes;
    }


    @ApiOperation(value = "Tumor Types", notes = "", response = TumorType.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search/{type}/{query}",
        produces = {APPLICATION_JSON_VALUE},
        method = RequestMethod.GET)
    public Iterable<TumorType> tumorTypesSearchTypeQueryQueryGet(
        @ApiParam(value = "Query type. It could be 'id', 'code', 'name', 'mainType', 'level', 'nci', 'umls' or 'color'.", required = true)
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
        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);

        // Cache in tumor types in case no data present
        CacheUtil.getTumorTypesByVersion(v);

        matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(type, query, exactMatch, v, false);
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
            matchedTumorTypes = TumorTypesUtil.filterTumorTypesByLevel(matchedTumorTypes, levelList);
        }
        return matchedTumorTypes;
    }
}
