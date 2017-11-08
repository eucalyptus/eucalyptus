/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources

import com.eucalyptus.cloudformation.CloudFormationException
import com.fasterxml.jackson.databind.JsonNode

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class ResourceInfo {
  Boolean ready = false;
  String propertiesJson;
  String metadataJson;
  String creationPolicyJson;
  String updatePolicyJson;
  String deletionPolicy = "Delete";

  public boolean supportsTags() {
    return false;
  }

  public boolean supportsSnapshot() {
    return false;
  }

  public boolean supportsSignals() {
    return false;
  }

  public boolean isAttributeAllowed(String attributeName) {
    return ResourceAttributeResolver.resourceHasAttribute(this, attributeName);
  }

  public Collection<String> getRequiredCapabilities(JsonNode propertiesJson) {
    return new ArrayList<String>();
  }
  String accountId;
  String effectiveUserId;
  String type;
  String logicalResourceId;
  String physicalResourceId;
  Boolean createdEnoughToDelete = false;
  Boolean allowedByCondition;
  String referenceValueJson;
  String description;

  public String getResourceAttributeJson(String attributeName)
          throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeJson(this, attributeName);
  }

  public void setResourceAttributeJson(String attributeName, String attributeValueJson)
          throws CloudFormationException {
    ResourceAttributeResolver.setResourceAttributeJson(this, attributeName, attributeValueJson);
  }

  public Collection<String> getAttributeNames() throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeNames(this);
  }

}