/*
 * Copyright (c) 2016-2019 Memorial Sloan-Kettering Cancer Center.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorType {

    private String uri = null;
    private String code = null;
    private String color = null;
    private String name = null;
    private String mainType = null;
    private Map<String, List<String>> externalReferences = new HashMap<String, List<String>>();
    private String tissue = null;
    private Map<String, TumorType> children = new HashMap<String, TumorType>();
    private String parent = null;
    private List<String> history = new ArrayList<String>();
    private Integer level = null;
    private List<String> revocations = new ArrayList<String>();
    private List<String> precursors = new ArrayList<String>();
    private static final String UNKNOWN_ONCOTREE_NODE_LEVEL = "-1";

    public TumorType() {}

    public TumorType(TumorType otherTumorType) {
        this.uri = otherTumorType.uri;
        this.code = otherTumorType.code;
        this.color = otherTumorType.color;
        this.name = otherTumorType.name;
        this.mainType = otherTumorType.mainType;
        // shallow copy
        this.externalReferences = new HashMap<String, List<String>>(otherTumorType.externalReferences);
        this.tissue = otherTumorType.tissue;
        // shallow copy
        this.children = new HashMap<String, TumorType>(otherTumorType.children);
        this.parent = otherTumorType.parent;
        this.history = new ArrayList<String>(otherTumorType.history);
        this.revocations = new ArrayList<String>(otherTumorType.revocations);
        this.precursors = new ArrayList<String>(otherTumorType.precursors);
        this.level = otherTumorType.level;
    }

    public TumorType deepCopy() {
        TumorType newTumorType = new TumorType();
        newTumorType.uri = this.uri;
        newTumorType.code = this.code;
        newTumorType.color = this.color;
        newTumorType.name = this.name;
        newTumorType.mainType = this.mainType;
        newTumorType.externalReferences = new HashMap<String, List<String>>(this.externalReferences);
        newTumorType.tissue = this.tissue;
        newTumorType.children = new HashMap<String, TumorType>(this.children.size());
        for (Map.Entry<String, TumorType> entry : this.children.entrySet()) {
            newTumorType.children.put(entry.getKey(), entry.getValue().deepCopy());
        }
        newTumorType.parent = this.parent;
        newTumorType.history = new ArrayList<String>(this.history);
        newTumorType.revocations = new ArrayList<String>(this.revocations);
        newTumorType.precursors = new ArrayList<String>(this.precursors);
        newTumorType.level = this.level;
        return newTumorType;
    }

    @JsonIgnore
    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Unique identifier representing OncoTree tumor types.
     **/
    @ApiModelProperty(value = "Unique identifier representing OncoTree tumor types.")
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    /**
     * Tumor type color.
     **/
    @ApiModelProperty(value = "Tumor type color.")
    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }


    /**
     * Tumor type name.
     **/
    @ApiModelProperty(value = "Tumor type name.")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("mainType")
    public String getMainType() {
        return mainType;
    }

    public void setMainType(String mainType) {
        this.mainType = mainType;
    }


    /**
     * External references (e.g. NCI and UMLS)
     **/
    @ApiModelProperty(value = "External references e.g. NCI Thesaurus or UMLS code(s).")
    @JsonProperty("externalReferences")
    public Map<String, List<String>> getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(Map<String, List<String>> externalReferences) {
        this.externalReferences = externalReferences;
    }

    public void setExternalReference(String type, List<String> codes) {
        if (codes == null) {
            this.externalReferences.put(type, new ArrayList<String>());
        } else {
            this.externalReferences.put(type, codes);
        }
    }

    public void addExternalReference(String type, String code) {
        if (!this.externalReferences.containsKey(type)) {
            this.externalReferences.put(type, new ArrayList<String>());
        }
        this.externalReferences.get(type).add(code);
    }

    /**
     * The tissue this tumor type belongs to.
     **/
    @ApiModelProperty(value = "The tissue this tumor type belongs to.")
    @JsonProperty("tissue")
    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }


    /**
     * List of all available children tumor types.
     **/
    @ApiModelProperty(value = "List of all available children tumor types.")
    @JsonProperty("children")
    public Map<String, TumorType> getChildren() {
        return children;
    }

    public void setChildren(Map<String, TumorType> children) {
        if (children == null) {
            this.children = new HashMap<String, TumorType>();
        } else {
            this.children = children;
        }
    }

    public void addChild(TumorType child) {
        if (children == null) {
            this.children = new HashMap<String, TumorType>();
        }
        this.children.put(child.getCode(), child);
    }

    /**
     * The parent node code.
     **/
    @ApiModelProperty(value = "The parent node code.")
    @JsonProperty("parent")
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("history")
    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("revocations")
    public List<String> getRevocations() {
        return revocations;
    }

    /**
    *
    * @param oncotreeCode
    */
    public void addRevocations(String oncotreeCode) {
        this.revocations.add(oncotreeCode);
    }

    public void setRevocations(List<String> revocations) {
        this.revocations = revocations;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("precursors")
    public List<String> getPrecursors() {
        return precursors;
    }

    /**
    *
    * @param oncotreeCode
    */
    public void addPrecursors(String oncotreeCode) {
        this.precursors.add(oncotreeCode);
    }

    public void setPrecursors(List<String> precursors) {
        this.precursors = precursors;
    }

    /**
     **/
    @ApiModelProperty(value = UNKNOWN_ONCOTREE_NODE_LEVEL)
    @JsonProperty("level")
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TumorType tumorType = (TumorType) o;
        return Objects.equals(code, tumorType.code) &&
            Objects.equals(color, tumorType.color) &&
            Objects.equals(name, tumorType.name) &&
            Objects.equals(mainType, tumorType.mainType) &&
            Objects.equals(externalReferences, tumorType.externalReferences) &&
            Objects.equals(tissue, tumorType.tissue) &&
            Objects.equals(children, tumorType.children) &&
            Objects.equals(parent, tumorType.parent) &&
            Objects.equals(history, tumorType.history) &&
            Objects.equals(revocations, tumorType.revocations) &&
            Objects.equals(precursors, tumorType.precursors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, color, name, mainType, externalReferences, tissue, children, parent, history, revocations, precursors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TumorType {\n");
        sb.append("  code: ").append(code).append("\n");
        sb.append("  color: ").append(color).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  mainType: ").append(mainType).append("\n");
        for (String type : externalReferences.keySet()) {
            sb.append("  ").append(type).append(": ").append(StringUtils.join(externalReferences.get(type), ",")).append("\n");
        }
        sb.append("  tissue: ").append(tissue).append("\n");
        sb.append("  children: ").append(children).append("\n");
        sb.append("  parent: ").append(parent).append("\n");
        sb.append("  history: ").append(history).append("\n");
        sb.append("  revocations: ").append(revocations).append("\n");
        sb.append("  precursors: ").append(precursors).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
