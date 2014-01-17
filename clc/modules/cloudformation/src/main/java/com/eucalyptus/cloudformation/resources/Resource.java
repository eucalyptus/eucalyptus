/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources;

import net.sf.json.JSONObject;

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class Resource {
  private String accountId;
  private String effectiveUserId;

  public String getEffectiveUserId() {
    return effectiveUserId;
  }

  public void setEffectiveUserId(String effectiveUserId) {
    this.effectiveUserId = effectiveUserId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  private JSONObject propertiesJSON;
  private JSONObject metadataJSON;
  private String type;
  private String logicalResourceId;
  private String physicalResourceId;

  public JSONObject getMetadataJSON() {
    return metadataJSON;
  }

  public void setMetadataJSON(JSONObject metadataJSON) {
    this.metadataJSON = metadataJSON;
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
