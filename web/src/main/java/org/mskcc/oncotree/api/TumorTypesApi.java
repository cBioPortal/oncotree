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
@Api(value = "/tumorTypes", description = "the tumorTypes API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorTypesApi {

//    @ApiOperation(value = "Create a tumor type", notes = "", response = CreateTumorTypeResp.class)
//    @io.swagger.annotations.ApiResponses(value = {
//        @io.swagger.annotations.ApiResponse(code = 201, message = "Created and return the numerical id for newly created tumor type")})
//    @RequestMapping(value = "/create",
//        produces = {"application/json"},
//        consumes = {"application/json"},
//        method = RequestMethod.POST)
    public ResponseEntity<CreateTumorTypeResp> tumorTypesCreatePost(
        @ApiParam(value = "Unique identifier representing OncoTree tumor types.", required = true)
        @RequestParam(value = "code", required = true) String code,
        @ApiParam(value = "Tumor type name.", required = true)
        @RequestParam(value = "name", required = true) String name,
        @ApiParam(value = "The general tumor type id.", required = true)
        @RequestParam(value = "mainType", required = true) Integer mainType,
        @ApiParam(value = "The parent tumor type ID. If no parentId has been specified. It will be attached into root node, id is 0.", required = true, defaultValue = "0")
        @RequestParam(value = "parentId", required = true, defaultValue = "0") Integer parentId,
        @ApiParam(value = "NCI Thesaurus Code.")
        @RequestParam(value = "nci", required = false) String nci,
        @ApiParam(value = "Concept Unique Identifier.")
        @RequestParam(value = "umls", required = false) String umls
    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<CreateTumorTypeResp>(HttpStatus.OK);
    }


//    @ApiOperation(value = "Delete selected tumor type", notes = "", response = DeleteTumorTypeResp.class)
//    @io.swagger.annotations.ApiResponses(value = {
//        @io.swagger.annotations.ApiResponse(code = 200, message = "OK")})
//    @RequestMapping(value = "/delete/{id}",
//        produces = {"application/json"},
//        method = RequestMethod.POST)
    public ResponseEntity<DeleteTumorTypeResp> tumorTypesDeleteIdPost(
        @ApiParam(value = "the numerical identifier representing OncoTree tumor types.", required = true) @PathVariable("id") String id

    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<DeleteTumorTypeResp>(HttpStatus.OK);
    }


    @ApiOperation(value = "Return all available tumor types.", notes = "", response = InlineResponse200.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Nested tumor types object.")})
    @RequestMapping(value = "",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<InlineResponse200> tumorTypesGet(
        @ApiParam(value = "The version of tumor types. For example, 1, 1.1 Please see GitHub for released versions. ")
        @RequestParam(value = "version", required = false) String version,
        @ApiParam(value = "The flat list of tumor types", defaultValue = "false")
        @RequestParam(value = "falt", required = false) Boolean flat,
        @ApiParam(value = "Indicator that whether should include deprecated tumor types.", defaultValue = "false")
        @RequestParam(value = "deprecated", required = false, defaultValue = "false") Boolean deprecated,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
        @RequestParam(value = "callback", required = false) String callback)
        throws NotFoundException {
        InlineResponse200 response200 = new InlineResponse200();
        Meta meta = new Meta();
        meta.setCode(200);
        response200.setMeta(meta);

        Map<String, TumorType> tumorTypes = new HashMap<>();
        
        if (version != null) {
            Version v = VersionUtil.getVersion(version);
            tumorTypes = v != null ? CacheUtil.getOrResetTumorTypesByVersion(v) : tumorTypes;
        } else {
            tumorTypes = CacheUtil.getOrResetTumorTypesByVersion(VersionUtil.getVersion("realtime"));
        }
        if(flat) {
            response200.setData(TumorTypesUtil.flattenTumorTypes(tumorTypes));
        }else {
            response200.setData(tumorTypes);
        }
        return new ResponseEntity<InlineResponse200>(response200, HttpStatus.OK);
    }


//    @ApiOperation(value = "Return the selected tumor type children list.", notes = "Return the selected tumor type children list. By default, it will only return the children IDs.", response = ChildrenListResp.class)
//    @io.swagger.annotations.ApiResponses(value = {
//        @io.swagger.annotations.ApiResponse(code = 200, message = "OK")})
//    @RequestMapping(value = "/{id}/children",
//        produces = {"application/json"},
//        method = RequestMethod.GET)
    public ResponseEntity<ChildrenListResp> tumorTypesIdChildrenGet(
        @ApiParam(value = "The numerical ID of the desired tumor type", required = true)
        @PathVariable("id") Integer id,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
        @RequestParam(value = "callback", required = false) String callback


    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<ChildrenListResp>(HttpStatus.OK);
    }


//    @ApiOperation(value = "Get tumor type by using numerical unique ID", notes = "", response = TumorTypeResp.class)
//    @io.swagger.annotations.ApiResponses(value = {
//        @io.swagger.annotations.ApiResponse(code = 200, message = "OK")})
//    @RequestMapping(value = "/{id}",
//        produces = {"application/json"},
//        method = RequestMethod.GET)
    public ResponseEntity<TumorTypeResp> tumorTypesIdGet(
        @ApiParam(value = "The numerical ID of the desired tumor type", required = true)
        @PathVariable("id") String id,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
        @RequestParam(value = "callback", required = false) String callback


    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<TumorTypeResp>(HttpStatus.OK);
    }


    @ApiOperation(value = "Tumor Types", notes = "...", response = SearchTumorTypesResp.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search",
        produces = {"application/json"},
        method = RequestMethod.POST)
    public ResponseEntity<SearchTumorTypesPostResp> tumorTypesSearchPost(

        @ApiParam(value = "queries", required = true) @RequestBody TumorTypeQueries queries
    )
        throws NotFoundException {
        SearchTumorTypesPostResp resp = new SearchTumorTypesPostResp();
        resp.setMeta(new Meta() {{
            setCode(200);
        }});

        Version v = queries.getVersion() != null ? VersionUtil.getVersion(queries.getVersion()) : VersionUtil.getVersion("realtime");
        List<List<TumorType>> tumorTypes = new ArrayList<>();

        for (TumorTypeQuery query : queries.getQueries()) {
            List<TumorType> matchedTumorTypes = new ArrayList<>();
            CacheUtil.getOrResetTumorTypesByVersion(v);
            matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(query.getType(), query.getQuery(), query.getExactMatch(), v);
            String levels = "2,3,4,5";
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
            }
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
        @RequestParam(value = "levels", required = false, defaultValue = "1,2,3,4,5") String levels,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
        @RequestParam(value = "callback", required = false) String callback
    )
        throws NotFoundException {
        List<TumorType> matchedTumorTypes = new ArrayList<>();
        Version v = version != null ? VersionUtil.getVersion(version) : VersionUtil.getVersion("realtime");
        CacheUtil.getOrResetTumorTypesByVersion(v);
        matchedTumorTypes = v == null ? new ArrayList<TumorType>() : TumorTypesUtil.findTumorTypesByVersion(type, query, exactMatch, v);
        SearchTumorTypesResp resp = new SearchTumorTypesResp();

        if (type.toLowerCase() != "level" && levels != null) {
            List<String> ls = Arrays.asList(levels.split(","));
            List<Level> levelList = new ArrayList<>();
            for (String l : ls) {
                Level level = Level.getByLevel(l.trim());
                if (level != null) {
                    levelList.add(level);
                }
            }
            matchedTumorTypes = TumorTypesUtil.filterTumorTypesByLevel(matchedTumorTypes, levelList);
        }
        Meta meta = new Meta();
        meta.setCode(200);
        resp.setMeta(meta);

        resp.setData(matchedTumorTypes);

        return new ResponseEntity<SearchTumorTypesResp>(resp, HttpStatus.OK);
    }


//    @ApiOperation(value = "Update selected tumor type", notes = "", response = UpdateTumorTypeResp.class)
//    @io.swagger.annotations.ApiResponses(value = {
//        @io.swagger.annotations.ApiResponse(code = 200, message = "OK")})
//    @RequestMapping(value = "/update/{id}",
//        produces = {"application/json"},
//        method = RequestMethod.POST)
    public ResponseEntity<UpdateTumorTypeResp> tumorTypesUpdateIdPost(
        @ApiParam(value = "Numerical unique ID, generated by server.", required = true) @PathVariable("id") String id,
        @ApiParam(value = "OncoTree tumor types code.") @RequestParam(value = "code", required = false) String code,
        @ApiParam(value = "Tumor type name.") @RequestParam(value = "name", required = false) String name,
        @ApiParam(value = "General tumor type category.") @RequestParam(value = "mainType", required = false) String mainType,
        @ApiParam(value = "NCI Thesaurus Code.") @RequestParam(value = "nci", required = false) String nci,
        @ApiParam(value = "Concept Unique Identifier.") @RequestParam(value = "umls", required = false) String umls
    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<UpdateTumorTypeResp>(HttpStatus.OK);
    }


}
