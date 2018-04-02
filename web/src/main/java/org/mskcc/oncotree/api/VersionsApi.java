package org.mskcc.oncotree.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.mskcc.oncotree.model.Meta;
import org.mskcc.oncotree.model.VersionsResp;
import org.mskcc.oncotree.utils.VersionUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping(value = "/api/versions", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/versions", description = "")
public class VersionsApi {
    @ApiOperation(value = "Versions", notes = "...", response = VersionsResp.class)
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "List of available versions")})
    @RequestMapping(value = "",
        produces = {"application/json"},
        method = RequestMethod.GET)
    public ResponseEntity<VersionsResp> versionsGet() {
        VersionsResp resp = new VersionsResp();
        resp.setMeta(new Meta() {{
            setCode(200);
        }});

        resp.setData(new ArrayList<>(VersionUtil.getVersions()));
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }
}
