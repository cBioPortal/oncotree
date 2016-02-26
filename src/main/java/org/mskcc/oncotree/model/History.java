package org.mskcc.oncotree.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.User;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringMVCServerCodegen", date = "2016-04-04T17:16:11.368Z")
public class History  {
  
  private String dateTime = null;
  private String operationType = null;
  private User user = null;
  private TumorType oldValue = null;
  private TumorType newValue = null;

  
  /**
   * Operation time. Please RFC3339 for details.
   **/
  @ApiModelProperty(value = "Operation time. Please RFC3339 for details.")
  @JsonProperty("dateTime")
  public String getDateTime() {
    return dateTime;
  }
  public void setDateTime(String dateTime) {
    this.dateTime = dateTime;
  }

  
  /**
   * C, D, U - Create, Delete, Update
   **/
  @ApiModelProperty(value = "C, D, U - Create, Delete, Update")
  @JsonProperty("operationType")
  public String getOperationType() {
    return operationType;
  }
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("user")
  public User getUser() {
    return user;
  }
  public void setUser(User user) {
    this.user = user;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("oldValue")
  public TumorType getOldValue() {
    return oldValue;
  }
  public void setOldValue(TumorType oldValue) {
    this.oldValue = oldValue;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("newValue")
  public TumorType getNewValue() {
    return newValue;
  }
  public void setNewValue(TumorType newValue) {
    this.newValue = newValue;
  }

  

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    History history = (History) o;
    return Objects.equals(dateTime, history.dateTime) &&
        Objects.equals(operationType, history.operationType) &&
        Objects.equals(user, history.user) &&
        Objects.equals(oldValue, history.oldValue) &&
        Objects.equals(newValue, history.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTime, operationType, user, oldValue, newValue);
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class History {\n");
    
    sb.append("  dateTime: ").append(dateTime).append("\n");
    sb.append("  operationType: ").append(operationType).append("\n");
    sb.append("  user: ").append(user).append("\n");
    sb.append("  oldValue: ").append(oldValue).append("\n");
    sb.append("  newValue: ").append(newValue).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
