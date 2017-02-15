package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-25T21:05:12.544Z")
public class MainTypeResp {

    private Meta meta = null;
    private MainType data = null;


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
    public MainType getData() {
        return data;
    }

    public void setData(MainType data) {
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
        MainTypeResp mainTypeResp = (MainTypeResp) o;
        return Objects.equals(meta, mainTypeResp.meta) &&
            Objects.equals(data, mainTypeResp.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meta, data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MainTypeResp {\n");

        sb.append("  meta: ").append(meta).append("\n");
        sb.append("  data: ").append(data).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
