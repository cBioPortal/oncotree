package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import org.mskcc.oncotree.model.SearchHistoryResp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping(value = "/api/history", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/history", description = "the history API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class HistoryApi {


    @ApiOperation(value = "Search all operations.", notes = "...", response = SearchHistoryResp.class)
    @ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<SearchHistoryResp> historySearchGet(
        @ApiParam(value = "The start date") @RequestParam(value = "start", required = false) Date start,
        @ApiParam(value = "The start date") @RequestParam(value = "end", required = false) Date end,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.") @RequestParam(value = "callback", required = false) String callback
    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<SearchHistoryResp>(HttpStatus.OK);
    }


    @ApiOperation(value = "The history of data manipulating.", notes = "...", response = SearchHistoryResp.class)
    @ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of tumor types")})
    @RequestMapping(value = "/search/{type}",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<SearchHistoryResp> historySearchTypeGet(
        @ApiParam(value = "Operation type. It could be 'create', 'delete', 'update' or 'all'.", required = true) @PathVariable("type") String type,
        @ApiParam(value = "The start date") @RequestParam(value = "start", required = false) Date start,
        @ApiParam(value = "The start date") @RequestParam(value = "end", required = false) Date end,
        @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.") @RequestParam(value = "callback", required = false) String callback
    )
        throws NotFoundException {
        // do some magic!
        return new ResponseEntity<SearchHistoryResp>(HttpStatus.OK);
    }


}
