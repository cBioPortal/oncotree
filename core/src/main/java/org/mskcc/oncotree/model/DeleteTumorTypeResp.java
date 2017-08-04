package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.mskcc.oncotree.model.CreateTumorTypeRespData;
import org.mskcc.oncotree.model.Meta;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class DeleteTumorTypeResp  {
  
  private Meta meta = null;
  private CreateTumorTypeRespData data = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("meta")
  public Meta getMeta() {
    return meta;
  }
  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("data")
  public CreateTumorTypeRespData getData() {
    return data;
  }
  public void setData(CreateTumorTypeRespData data) {
    this.data = data;
  }

  

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteTumorTypeResp deleteTumorTypeResp = (DeleteTumorTypeResp) o;
    return Objects.equals(meta, deleteTumorTypeResp.meta) &&
        Objects.equals(data, deleteTumorTypeResp.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meta, data);
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteTumorTypeResp {\n");
    
    sb.append("  meta: ").append(meta).append("\n");
    sb.append("  data: ").append(data).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
