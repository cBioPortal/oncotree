package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorType {

    private Integer id = null;
    private String code = null;
    private String color = null;
    private String name = null;
    private MainType mainType = null;
    private List<String> NCI = new ArrayList<String>();
    private List<String> UMLS = new ArrayList<String>();
    private String tissue = null;
    private Map<String, TumorType> children = new HashMap<String, TumorType>();
    private String parent = null;
    private Boolean deprecated = false;
    private List<String> history = new ArrayList<String>();
    private Links links = null;
    private Integer level = null;
    private static final String UNKNOWN_ONCOTREE_NODE_LEVEL = "-1";

    /**
     * the numarical identifier of tumor type.
     **/
    @ApiModelProperty(value = "the numarical identifier of tumor type.")
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
    public MainType getMainType() {
        return mainType;
    }

    public void setMainType(MainType mainType) {
        this.mainType = mainType;
    }


    /**
     * NCI Thesaurus Code(s).
     **/
    @ApiModelProperty(value = "NCI Thesaurus Code(s).")
    @JsonProperty("NCI")
    public List<String> getNCI() {
        return NCI;
    }

    public void setNCI(List<String> NCI) {
        this.NCI = NCI;
    }

    public void addNCI(String NCI) {
        this.NCI.add(NCI);
    }

    /**
     * Concept Unique Identifier(s).
     **/
    @ApiModelProperty(value = "Concept Unique Identifier(s).")
    @JsonProperty("UMLS")
    public List<String> getUMLS() {
        return UMLS;
    }

    public void setUMLS(List<String> UMLS) {
        this.UMLS = UMLS;
    }

    public void addUMLS(String UMLS) {
        this.UMLS.add(UMLS);
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
        this.children = children;
    }

    public void addChild(TumorType child) {
        if (children == null) {
            this.children = new HashMap<String, TumorType>();
        }
        this.children.put(child.getCode(), child);
    }

    /**
     * The parent node id.
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
     * The indicater whether this code has been deprecated.
     **/
    @ApiModelProperty(value = "The indicater whether this code has been deprecated.")
    @JsonProperty("deprecated")
    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
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
    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
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
        return Objects.equals(id, tumorType.id) &&
            Objects.equals(code, tumorType.code) &&
            Objects.equals(color, tumorType.color) &&
            Objects.equals(name, tumorType.name) &&
            Objects.equals(mainType, tumorType.mainType) &&
            Objects.equals(NCI, tumorType.NCI) &&
            Objects.equals(UMLS, tumorType.UMLS) &&
            Objects.equals(tissue, tumorType.tissue) &&
            Objects.equals(children, tumorType.children) &&
            Objects.equals(parent, tumorType.parent) &&
            Objects.equals(deprecated, tumorType.deprecated) &&
            Objects.equals(history, tumorType.history) &&
            Objects.equals(links, tumorType.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, color, name, mainType, NCI, UMLS, tissue, children, parent, deprecated, history, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TumorType {\n");

        sb.append("  id: ").append(id).append("\n");
        sb.append("  code: ").append(code).append("\n");
        sb.append("  color: ").append(color).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  mainType: ").append(mainType).append("\n");
        sb.append("  NCI: ").append(StringUtils.join(NCI, ",")).append("\n");
        sb.append("  UMLS: ").append(StringUtils.join(UMLS, ",")).append("\n");
        sb.append("  tissue: ").append(tissue).append("\n");
        sb.append("  children: ").append(children).append("\n");
        sb.append("  parent: ").append(parent).append("\n");
        sb.append("  deprecated: ").append(deprecated).append("\n");
        sb.append("  history: ").append(history).append("\n");
        sb.append("  links: ").append(links).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
