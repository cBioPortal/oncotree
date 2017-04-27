package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;


@ApiModel(description = "")
public class VersionsResp {

    private Meta meta = null;
    private List<Version> data = null;


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("meta")
    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("data")
    public List<Version> getData() {
        return data;
    }

    public void setData(List<Version> data) {
        this.data = data;
    }
}
