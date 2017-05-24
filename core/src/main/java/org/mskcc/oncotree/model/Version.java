package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Created by Hongxin on 5/23/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "api_identifier",
    "graph_uri",
    "description"
})
public class Version {

    @JsonProperty("api_identifier")
    private String version;

    private String graphURI;

    @JsonProperty("description")
    private String description;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("api_identifier")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("api_identifier")
    public String getVersion() {
        return version;
    }

    @JsonProperty("graph_uri")
    public void setGraphURI(String graphURI) {
        this.graphURI = graphURI;
    }

    @JsonIgnore
    public String getGraphURI() {
        return graphURI;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
