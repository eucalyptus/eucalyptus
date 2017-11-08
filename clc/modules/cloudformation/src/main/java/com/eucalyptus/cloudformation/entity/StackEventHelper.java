/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.UUID;

/**
 * Created by ethomas on 4/21/17.
 */
public class StackEventHelper {

  public static Logger LOG = Logger.getLogger(StackEventHelper.class);

  public static void createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int stackVersion) throws CloudFormationException {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    String stackName = stackEntity.getStackName();
    String eventId = UUID.randomUUID().toString(); //TODO: AWS has a value related to stack id. (I think)
    String logicalResourceId = stackName;
    String physicalResourceId = stackId;
    ObjectNode properties = new ObjectMapper().createObjectNode();
    for (StackEntity.Parameter parameter : StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
      properties.put(parameter.getKey(), parameter.getStringValue());
    }
    Status status = Status.valueOf(resourceStatus);
    String resourceProperties = JsonHelper.getStringFromJsonNode(properties);
    String resourceType = "AWS::CloudFormation::Stack";
    Date timestamp = new Date();
    StackEventEntityManager.addStackEvent(accountId, eventId, logicalResourceId, physicalResourceId,
      resourceProperties, status, resourceStatusReason, resourceType, stackId, stackName, timestamp);

    // Good to update the global stack too
    stackEntity.setStackStatus(status);
    stackEntity.setStackStatusReason(resourceStatusReason);
    if ((status == Status.DELETE_IN_PROGRESS) && (stackEntity.getDeleteOperationTimestamp() == null)) {
      stackEntity.setDeleteOperationTimestamp(new Date()); // AWS only records the first delete attempt timestamp
    }
    StackEntityManager.updateStack(stackEntity);
    LOG.info("Done creating global stack event: " + resourceStatus);
  }

}
