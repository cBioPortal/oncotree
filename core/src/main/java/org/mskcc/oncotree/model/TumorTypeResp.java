package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorTypeResp {

    private Meta meta = null;
    private TumorType data = null;


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
    public TumorType getData() {
        return data;
    }

    public void setData(TumorType data) {
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
        TumorTypeResp tumorTypeResp = (TumorTypeResp) o;
        return Objects.equals(meta, tumorTypeResp.meta) &&
            Objects.equals(data, tumorTypeResp.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meta, data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TumorTypeResp {\n");

        sb.append("  meta: ").append(meta).append("\n");
        sb.append("  data: ").append(data).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
