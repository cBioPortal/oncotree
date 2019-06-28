/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

package org.mskcc.oncotree.topbraid;

/**
 *
 * @author Manda Wilson
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "uri",
    "code",
    "name",
    "mainType",
    "color",
    "parentCode",
    "revocations",
    "precursors"
})
public class OncoTreeNode implements Serializable {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mainType")
    private String mainType;

    @JsonProperty("color")
    private String color;

    @JsonProperty("parentCode")
    private String parentCode;

    @JsonProperty("revocations")
    private List<String> revocations = new ArrayList<String>();

    @JsonProperty("precursors")
    private List<String> precursors = new ArrayList<String>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public OncoTreeNode() {}

    public OncoTreeNode(OncoTreeNode otherOncoTreeNode) {
        this.uri = otherOncoTreeNode.uri;
        this.code = otherOncoTreeNode.code;
        this.name = otherOncoTreeNode.name;
        this.mainType = otherOncoTreeNode.mainType;
        this.color = otherOncoTreeNode.color;
        this.parentCode = otherOncoTreeNode.parentCode;
        // shallow copies
        this.revocations = new ArrayList<String>(otherOncoTreeNode.revocations);
        this.precursors = new ArrayList<String>(otherOncoTreeNode.precursors);
        this.additionalProperties = new HashMap<String, Object>(otherOncoTreeNode.additionalProperties);
    }

    @JsonProperty("uri")
    public String getURI() {
        return uri;
    }

    @JsonProperty("uri")
    public void setURI(String uri) {
        this.uri = uri;
    }

    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    @JsonProperty("code")
    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("mainType")
    public String getMainType() {
        return mainType;
    }

    @JsonProperty("mainType")
    public void setMainType(String mainType) {
        this.mainType = mainType;
    }

    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty("parentCode")
    public String getParentCode() {
    return parentCode;
    }

    @JsonProperty("parentCode")
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    @JsonProperty("revocations")
    public List<String> getRevocations() {
        return revocations;
    }

    @JsonProperty("revocations")
    public void setRevocations(String revocations) {
        if (revocations != null) {
            this.revocations = Arrays.asList(revocations.split("\\s+"));
        }
    }

    @JsonProperty("precursors")
    public List<String> getPrecursors() {
        return precursors;
    }

    @JsonProperty("precursors")
    public void setPrecursors(String precursors) {
        if (precursors != null) {
            this.precursors = Arrays.asList(precursors.split("\\s+"));
        }
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
