package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class UpdateTumorTypeResp {

    private Meta meta = null;
    private CreateTumorTypeRespData data = null;


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
    public CreateTumorTypeRespData getData() {
        return data;
    }

    public void setData(CreateTumorTypeRespData data) {
        this.data = data;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateTumorTypeResp updateTumorTypeResp = (UpdateTumorTypeResp) o;
        return Objects.equals(meta, updateTumorTypeResp.meta) &&
            Objects.equals(data, updateTumorTypeResp.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meta, data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UpdateTumorTypeResp {\n");

        sb.append("  meta: ").append(meta).append("\n");
        sb.append("  data: ").append(data).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
