/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/
package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.io.Serializable;
/**
 * Created by Hongxin on 5/23/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "api_identifier",
    "graph_uri",
    "description"
})
public class Version implements Serializable {

    @JsonProperty("api_identifier")
    private String version;

    private String graphURI;

    @JsonProperty("description")
    private String description;

    @JsonProperty("visible")
    private boolean visible;

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

    @JsonProperty("visible")
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @JsonProperty("visible")
    public boolean getVisible() {
        return visible;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Version otherVersion = (Version) o;
        return Objects.equals(version, otherVersion.version) &&
            Objects.equals(description, otherVersion.description) &&
            Objects.equals(graphURI, otherVersion.graphURI) &&
            Objects.equals(visible, otherVersion.visible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, description, graphURI, visible);
    }

}
