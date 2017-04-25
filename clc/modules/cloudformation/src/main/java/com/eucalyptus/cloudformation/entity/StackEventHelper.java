/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
