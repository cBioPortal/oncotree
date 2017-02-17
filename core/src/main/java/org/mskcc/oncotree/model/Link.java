package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class Link  {
  
  private String href = null;
  private String rel = null;
  private String method = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("href")
  public String getHref() {
    return href;
  }
  public void setHref(String href) {
    this.href = href;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("rel")
  public String getRel() {
    return rel;
  }
  public void setRel(String rel) {
    this.rel = rel;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("method")
  public String getMethod() {
    return method;
  }
  public void setMethod(String method) {
    this.method = method;
  }

  

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Link link = (Link) o;
    return Objects.equals(href, link.href) &&
        Objects.equals(rel, link.rel) &&
        Objects.equals(method, link.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(href, rel, method);
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Link {\n");
    
    sb.append("  href: ").append(href).append("\n");
    sb.append("  rel: ").append(rel).append("\n");
    sb.append("  method: ").append(method).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
