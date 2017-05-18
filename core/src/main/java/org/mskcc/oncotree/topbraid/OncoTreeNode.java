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

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "code",
    "name",
    "mainType",
    "color",
    "nci",
    "umls",
    "nccn",
    "parentCode"
})
public class OncoTreeNode {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mainType")
    private String mainType;

    @JsonProperty("color")
    private String color;

    @JsonProperty("nci")
    private String nci;

    @JsonProperty("umls")
    private String umls;

    @JsonProperty("nccn")
    private String nccn;

    @JsonProperty("parentCode")
    private String parentCode;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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

    @JsonProperty("nci")
    public String getNci() {
        return nci;
    }

    @JsonProperty("nci")
    public void setNci(String nci) {
        this.nci = nci;
    }

    @JsonProperty("umls")
    public String getUmls() {
        return umls;
    }

    @JsonProperty("umls")
    public void setUmls(String umls) {
        this.umls = umls;
    }

    @JsonProperty("nccn")
    public String getNccn() {
        return nccn;
    }

    @JsonProperty("nccn")
    public void setNccn(String nccn) {
        this.nccn = nccn;
    }

    @JsonProperty("parentCode")
    public String getParentCode() {
    return parentCode;
    }

    @JsonProperty("parentCode")
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
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
