package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorTypeQuery {

    private String type = null;
    private String query = null;
    private Boolean exactMatch = true;

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("query")
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("exactMatch")
    public Boolean getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TumorTypeQuery tumorTypeQuery = (TumorTypeQuery) o;
        return Objects.equals(type, tumorTypeQuery.type) &&
            Objects.equals(query, tumorTypeQuery.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, query);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TumorTypeQuery {\n");
        sb.append("  type: ").append(type).append("\n");
        sb.append("  query: ").append(query).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
