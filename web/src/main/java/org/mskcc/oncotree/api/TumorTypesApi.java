package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.mskcc.oncotree.model.*;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.TumorTypesUtil;
import org.mskcc.oncotree.utils.VersionUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping(value = "/api/tumorTypes", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/tumorTypes", description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorTypesApi {


    @ApiOperation(value = "Return all available tumor types.", notes = "", response = InlineResponse200.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Nested tumor types object.")})
    @RequestMapping(value = "",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<InlineResponse200> tumorTypesGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version,
        @ApiParam(value = "The flat list of tumor types", defaultValue = "false")
        @RequestParam(value = "flat", required = false, defaultValue = "false") Boolean flat,
        @ApiParam(value = "Indicator that whether should include deprecated tumor types.", defaultValue = "false")
        @RequestParam(value = "deprecated", required = false, defaultValue = "false") Boolean deprecated
//        , @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
//        @RequestParam(value = "callback", required = false) String callback
    ) {
        InlineResponse200 response200 = new InlineResponse200();
        Meta meta = new Meta();
        meta.setCode(200);
        response200.setMeta(meta);

        Map<String, TumorType> tumorTypes = new HashMap<>();

        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);

        tumorTypes = CacheUtil.getTumorTypesByVersion(v);

        if (flat) {
            response200.setData(TumorTypesUtil.flattenTumorTypes(tumorTypes, null));
        } else {
            response200.setData(tumorTypes);
        }
        return new ResponseEntity<InlineResponse200>(response200, HttpStatus.OK);
    }

    @ApiOperation(value = "Tumor Types", notes = "...", response = SearchTumorTypesResp.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search",
        produces = {"application/json"},
        method = RequestMethod.POST)
    public ResponseEntity<SearchTumorTypesPostResp> tumorTypesSearchPost(

        @ApiParam(value = "queries", required = true) @RequestBody TumorTypeQueries queries
    ) {
        SearchTumorTypesPostResp resp = new SearchTumorTypesPostResp();
        resp.setMeta(new Meta() {{
            setCode(200);
        }});

        Version v = queries.getVersion() != null ? VersionUtil.getVersion(queries.getVersion()) : VersionUtil.getDefaultVersion();
        List<List<TumorType>> tumorTypes = new ArrayList<>();

        // Cache in tumor types in case no data present
        CacheUtil.getTumorTypesByVersion(v);

        for (TumorTypeQuery query : queries.getQueries()) {
            List<TumorType> matchedTumorTypes = new ArrayList<>();
            matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(query.getType(), query.getQuery(), query.getExactMatch(), v, false);
            /*String levels = "2,3,4,5";
            if (query.getType().toLowerCase() != "level" && levels != null) {
                List<String> ls = Arrays.asList(levels.split(","));
                List<Level> levelList = new ArrayList<>();
                for (String l : ls) {
                    Level level = Level.getByLevel(l.trim());
                    if (level != null) {
                        levelList.add(level);
                    }
                }
                matchedTumorTypes = TumorTypesUtil.filterTumorTypesByLevel(matchedTumorTypes, levelList);
            }*/
            tumorTypes.add(matchedTumorTypes);
        }

        resp.setData(tumorTypes);
        return new ResponseEntity<SearchTumorTypesPostResp>(resp, HttpStatus.OK);
    }


    @ApiOperation(value = "Tumor Types", notes = "", response = SearchTumorTypesResp.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search/{type}/{query}",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<SearchTumorTypesResp> tumorTypesSearchTypeQueryQueryGet(
        @ApiParam(value = "Query type. It could be 'id', 'code', 'name', 'mainType', 'level', 'nci', 'umls' or 'color'. You can also use 'all' to search all content.", required = true)
        @PathVariable("type") String type,
        @ApiParam(value = "The query content", required = true)
        @PathVariable("query") String query,
        @ApiParam(value = "The version of tumor types. For example, 1, 1.1 Please see GitHub for released versions. ")
        @RequestParam(value = "version", required = false) String version,
        @ApiParam(value = "If it sets to true, it will only return one element array.", defaultValue = "true")
        @RequestParam(value = "exactMatch", required = false, defaultValue = "true") Boolean exactMatch,
        @ApiParam(value = "Tumor type levels. 1-5. By default, it doesn't includes tissue which is the primary level.", defaultValue = "2,3,4,5")
        @RequestParam(value = "levels", required = false, defaultValue = "1,2,3,4,5") String levels
//        , @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
//        @RequestParam(value = "callback", required = false) String callback
    ) {
        List<TumorType> matchedTumorTypes = new ArrayList<>();
        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);

        // Cache in tumor types in case no data present
        CacheUtil.getTumorTypesByVersion(v);

        matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(type, query, exactMatch, v, false);
        SearchTumorTypesResp resp = new SearchTumorTypesResp();

        /*if (type.toLowerCase() != "level" && levels != null) {
            List<String> ls = Arrays.asList(levels.split(","));
            List<Level> levelList = new ArrayList<>();
            for (String l : ls) {
                Level level = Level.getByLevel(l.trim());
                if (level != null) {
                    levelList.add(level);
                }
            }
            matchedTumorTypes = TumorTypesUtil.filterTumorTypesByLevel(matchedTumorTypes, levelList);
        }*/
        Meta meta = new Meta();
        meta.setCode(200);
        resp.setMeta(meta);

        resp.setData(matchedTumorTypes);

        return new ResponseEntity<SearchTumorTypesResp>(resp, HttpStatus.OK);
    }


}
