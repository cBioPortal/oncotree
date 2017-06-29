package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import java.util.Map;
import org.mskcc.oncotree.model.MainTypeResp;
import org.mskcc.oncotree.model.MainTypesResp;
import org.mskcc.oncotree.model.Meta;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.utils.MainTypesUtil;
import org.mskcc.oncotree.utils.VersionUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping(value = "/api/mainTypes", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/mainTypes", description = "the mainTypes API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-25T21:05:12.544Z")
public class MainTypesApi {


    @ApiOperation(value = "Return all available main tumor types.", notes = "", response = MainTypesResp.class)
    @ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Nested tumor types object.")})
    @RequestMapping(value = "",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<MainTypesResp> mainTypesGet(
        @ApiParam(value = "The version of tumor types. For example, " + VersionUtil.DEFAULT_VERSION  + ". Please see the versions api documentation for released versions.")
        @RequestParam(value = "version", required = false) String version
//        , @ApiParam(value = "The callback function name. This has to be used with dataType JSONP.")
//        @RequestParam(value = "callback", required = false) String callback
    )
        throws NotFoundException {
        MainTypesResp resp = new MainTypesResp();

        Meta meta = new Meta();
        meta.setCode(200);
        resp.setMeta(meta);

        Map<String, TumorType> tumorTypes = new HashMap<>();
        Version v = (version == null) ? VersionUtil.getDefaultVersion() : VersionUtil.getVersion(version);
        tumorTypes = CacheUtil.getOrResetTumorTypesByVersion(v);
        resp.setData(MainTypesUtil.getMainTypesByTumorTypes(tumorTypes));

        return new ResponseEntity<MainTypesResp>(resp, HttpStatus.OK);
    }
}
