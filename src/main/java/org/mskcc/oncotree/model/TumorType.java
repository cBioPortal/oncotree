package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.*;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class TumorType {

    private Integer id = null;
    private String code = null;
    private String color = null;
    private String name = null;
    private MainType mainType = null;
    private String NCI = null;
    private String UMLS = null;
    private String tissue = null;
    private Map<String, TumorType> children = new HashMap<>();
    private TumorType parent = null;
    private Boolean deprecated = false;
    private List<History> history = new ArrayList<History>();
    private Links links = null;
    private Level level = null;


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
     * NCI Thesaurus Code.
     **/
    @ApiModelProperty(value = "NCI Thesaurus Code.")
    @JsonProperty("NCI")
    public String getNCI() {
        return NCI;
    }

    public void setNCI(String NCI) {
        this.NCI = NCI;
    }


    /**
     * Concept Unique Identifier.
     **/
    @ApiModelProperty(value = "Concept Unique Identifier.")
    @JsonProperty("UMLS")
    public String getUMLS() {
        return UMLS;
    }

    public void setUMLS(String UMLS) {
        this.UMLS = UMLS;
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

    public void setChildren(Map children) {
        this.children = children;
    }


    /**
     * The parent node id.
     **/
    @ApiModelProperty(value = "The parent node id.")
    @JsonProperty("parent")
    public TumorType getParent() {
        return parent;
    }

    public void setParent(TumorType parent) {
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
    public List<History> getHistory() {
        return history;
    }

    public void setHistory(List<History> history) {
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
    @ApiModelProperty(value = "")
    @JsonProperty("level")
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
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
        sb.append("  NCI: ").append(NCI).append("\n");
        sb.append("  UMLS: ").append(UMLS).append("\n");
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
