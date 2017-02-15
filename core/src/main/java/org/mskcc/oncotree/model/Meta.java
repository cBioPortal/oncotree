package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class Meta  {
  
  private String errorType = null;
  private Integer code = null;
  private String erroMessage = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("error_type")
  public String getErrorType() {
    return errorType;
  }
  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("code")
  public Integer getCode() {
    return code;
  }
  public void setCode(Integer code) {
    this.code = code;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("erro_message")
  public String getErroMessage() {
    return erroMessage;
  }
  public void setErroMessage(String erroMessage) {
    this.erroMessage = erroMessage;
  }

  

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Meta meta = (Meta) o;
    return Objects.equals(errorType, meta.errorType) &&
        Objects.equals(code, meta.code) &&
        Objects.equals(erroMessage, meta.erroMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorType, code, erroMessage);
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Meta {\n");
    
    sb.append("  errorType: ").append(errorType).append("\n");
    sb.append("  code: ").append(code).append("\n");
    sb.append("  erroMessage: ").append(erroMessage).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
