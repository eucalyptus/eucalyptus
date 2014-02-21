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


import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.resources.propertytypes.ResourceAttributes;
import com.eucalyptus.cloudformation.resources.propertytypes.ResourceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class Resource {

  public abstract void populateResourceProperties(JsonNode jsonNode) throws CloudFormationException;
  public abstract ResourceProperties getResourceProperties();
  public abstract ResourceAttributes getResourceAttributes();

  public boolean supportsSnapshots() {
    return false;
  }
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

  private JsonNode propertiesJsonNode;
  private JsonNode metadataJsonNode;
  private JsonNode updatePolicyJsonNode;
  private String deletionPolicy = "Delete";
  private boolean allowedByCondition = true;

  public JsonNode getUpdatePolicyJsonNode() {
    return updatePolicyJsonNode;
  }

  public void setUpdatePolicyJsonNode(JsonNode updatePolicyJsonNode) {
    this.updatePolicyJsonNode = updatePolicyJsonNode;
  }

  public String getDeletionPolicy() {
    return deletionPolicy;
  }

  public void setDeletionPolicy(String deletionPolicy) {
    this.deletionPolicy = deletionPolicy;
  }

  public boolean isAllowedByCondition() {
    return allowedByCondition;
  }

  public void setAllowedByCondition(boolean allowedByCondition) {
    this.allowedByCondition = allowedByCondition;
  }

  private String type;
  private String logicalResourceId;
  private String physicalResourceId;

  public JsonNode getMetadataJsonNode() {
    return metadataJsonNode;
  }

  public void setMetadataJsonNode(JsonNode metadataJSON) {
    this.metadataJsonNode = metadataJSON;
  }

  public JsonNode getPropertiesJsonNode() {
    return propertiesJsonNode;
  }

  public void setPropertiesJsonNode(JsonNode propertiesJsonNode) {
    this.propertiesJsonNode = propertiesJsonNode;
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

  public abstract JsonNode referenceValue();
}
