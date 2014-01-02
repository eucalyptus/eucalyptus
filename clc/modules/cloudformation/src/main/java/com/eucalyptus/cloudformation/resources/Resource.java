package com.eucalyptus.cloudformation.resources;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class Resource {

  private JSONObject propertiesJSON;
  private JSONObject metadataJSON;
  private String type;
  private String logicalResourceId;
  private String physicalResourceId;
  private String ownerUserId;

  public JSONObject getMetadataJSON() {
    return metadataJSON;
  }

  public void setMetadataJSON(JSONObject metadataJSON) {
    this.metadataJSON = metadataJSON;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public JSONObject getPropertiesJSON() {
    return propertiesJSON;
  }

  public void setPropertiesJSON(JSONObject propertiesJSON) {
    this.propertiesJSON = propertiesJSON;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getPhysicalResourceId() {
    return physicalResourceId;
  }

  public void setPhysicalResourceId(String physicalResourceId) {
    this.physicalResourceId = physicalResourceId;
  }

  public abstract void create() throws Exception;
  public abstract void delete() throws Exception;
  public abstract void rollback() throws Exception;

  public abstract Object referenceValue();
}
