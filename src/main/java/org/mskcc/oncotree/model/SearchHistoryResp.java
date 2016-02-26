package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class SearchHistoryResp {

    private Meta meta = null;
    private List<History> data = new ArrayList<History>();


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
    public List<History> getData() {
        return data;
    }

    public void setData(List<History> data) {
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
        SearchHistoryResp searchHistoryResp = (SearchHistoryResp) o;
        return Objects.equals(meta, searchHistoryResp.meta) &&
            Objects.equals(data, searchHistoryResp.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meta, data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SearchHistoryResp {\n");

        sb.append("  meta: ").append(meta).append("\n");
        sb.append("  data: ").append(data).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
