/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.common;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.RestrictedType;

/**
 *
 */
@PolicyVendor( SimpleWorkflowMetadata.VENDOR )
public interface SimpleWorkflowMetadata extends RestrictedType {

  String VENDOR = "swf";

  @PolicyResourceType( "domain" )
  interface DomainMetadata extends SimpleWorkflowMetadata {}

  @PolicyResourceType( "activity-task" ) // Not an AWS/SWF type
  interface ActivityTaskMetadata extends SimpleWorkflowMetadata {}

  @PolicyResourceType( "activity-type" ) // Not an AWS/SWF type
  interface ActivityTypeMetadata extends SimpleWorkflowMetadata {}

  @PolicyResourceType( "workflow-type" ) // Not an AWS/SWF type
  interface WorkflowTypeMetadata extends SimpleWorkflowMetadata {}

  @PolicyResourceType( "workflow-execution" ) // Not an AWS/SWF type
  interface WorkflowExecutionMetadata extends SimpleWorkflowMetadata {}

  @PolicyResourceType( "timer" ) // Not an AWS/SWF type
  interface TimerMetadata extends SimpleWorkflowMetadata {}

}
