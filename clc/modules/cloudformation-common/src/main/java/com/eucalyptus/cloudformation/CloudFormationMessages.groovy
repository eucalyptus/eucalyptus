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
package com.eucalyptus.cloudformation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.annotation.ComponentMessage;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
@ComponentMessage(CloudFormation.class)
public class CloudFormationMessage extends BaseMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    try {
      Field responseMetadataField = type.class.getDeclaredField("responseMetadata")
      responseMetadataField.setAccessible( true ) 
      ((ResponseMetadata) responseMetadataField.get( type )).requestId = getCorrelationId()
    } catch ( Exception e ) {       
    }
    return type
  }
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  public Error() {  }
  ErrorDetail detail = new ErrorDetail();
}
public class ResourceList extends EucalyptusData {
  public ResourceList() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class CloudFormationErrorResponse extends CloudFormationMessage {
  String requestId;
  public CloudFormationErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>();
}
public class Outputs extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Output> member = new ArrayList<Output>();
  public Outputs() {  }
  public Outputs( Output output ) {
    member.add( output  )
  }
  @Override
  public String toString() {
    return "Outputs [member=" + member + "]";
  }
}
public class Parameters extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Parameter> member = new ArrayList<Parameter>();
  public Parameters() {  }
  public Parameters( Parameter parameter ) {
    member.add( parameter  )
  }
  @Override
  public String toString() {
    return "Parameters [member=" + member + "]";
  }
}
public class StackEvents extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<StackEvent> member = new ArrayList<StackEvent>();
  public StackEvents() {  }
  public StackEvents( StackEvent stackEvent ) {
    member.add( stackEvent  )
  }
  @Override
  public String toString() {
    return "StackEvents [member=" + member + "]";
  }
}
public class StackResources extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<StackResource> member = new ArrayList<StackResource>();
  public StackResources() {  }
  public StackResources( StackResource stackResource ) {
    member.add( stackResource  )
  }
  @Override
  public String toString() {
    return "StackResources [member=" + member + "]";
  }
}
public class StackResourceSummaries extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<StackResourceSummary> member = new ArrayList<StackResourceSummary>();
  public StackResourceSummaries() {  }
  public StackResourceSummaries( StackResourceSummary stackResourceSummary ) {
    member.add( stackResourceSummary  )
  }
  @Override
  public String toString() {
    return "StackResourceSummaries [member=" + member + "]";
  }
}

public class StackSummaries extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<StackSummary> member = new ArrayList<StackSummary>();
  public StackSummaries() {  }
  public StackSummaries( StackSummary stackSummary ) {
    member.add( stackSummary  )
  }
  @Override
  public String toString() {
    return "StackSummaries [member=" + member + "]";
  }
}

public class Stacks extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Stack> member = new ArrayList<Stack>();
  public Stacks() {  }
  public Stacks( Stack stack ) {
    member.add( stack  )
  }
  @Override
  public String toString() {
    return "Stacks [member=" + member + "]";
  }
}
public class Tags extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Tag> member = new ArrayList<Tag>();
  public Tags() {  }
  public Tags( Tag tag ) {
    member.add( tag  )
  }
  @Override
  public String toString() {
    return "Tags [member=" + member + "]";
  }
}
public class TemplateParameters extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<TemplateParameter> member = new ArrayList<TemplateParameter>();
  public TemplateParameters() {  }
  public TemplateParameters( TemplateParameter templateParameter ) {
    member.add( templateParameter  )
  }
  @Override
  public String toString() {
    return "TemplateParameters [member=" + member + "]";
  }
}
public class CancelUpdateStackResult extends EucalyptusData {
  public CancelUpdateStackResult() {  }
}
public class CreateStackResult extends EucalyptusData {
  String stackId;
  public CreateStackResult() {  }
}
public class DeleteStackResult extends EucalyptusData {
  public DeleteStackResult() {  }
}

public class DescribeStackEventsResult extends EucalyptusData {
  String nextToken
  StackEvents stackEvents;
  public DescribeStackEventsResult() {  }
}
public class DescribeStackResourceResult extends EucalyptusData {
  StackResourceDetail stackResourceDetail;
  public DescribeStackResourceResult() {  }
}
public class DescribeStackResourcesResult extends EucalyptusData {
  StackResources stackResources;
  public DescribeStackResourcesResult() {  }
}
public class DescribeStacksResult extends EucalyptusData {
  String nextToken
  Stacks stacks;
  public DescribeStacksResult() {  }
}
public class EstimateTemplateCostResult extends EucalyptusData {
  String url;
  public EstimateTemplateCostResult() {  }
}
public class GetStackPolicyResult extends EucalyptusData {
  String stackPolicyBody;
  public GetStackPolicyResult() {  }
}
public class GetTemplateResult extends EucalyptusData {
  String templateBody;
  public GetTemplateResult() {  }
}
public class ListStackResourcesResult extends EucalyptusData {
  String nextToken
  StackResourceSummaries stackResourceSummaries;
  public ListStackResourcesResult() {  }
}
public class ListStacksResult extends EucalyptusData {
  String nextToken
  StackSummaries stackSummaries;
  public ListStacksResult() {  }
}
public class Output extends EucalyptusData {
  String description;
  String outputKey;
  String outputValue;
  public Output() {  }
}
public class Parameter extends EucalyptusData {
  String parameterKey;
  String parameterValue;
  public Parameter() {  }
}
public class Stack extends EucalyptusData {
  ResourceList capabilities;
  Date creationTime;
  String description;
  Boolean disableRollback;
  Date lastUpdatedTime;
  ResourceList notificationARNs;
  Outputs outputs;
  Parameters parameters;
  String stackId;
  String stackName;
  String stackStatus;
  String stackStatusReason;
  Tags tags;
  Integer timeoutInMinutes;
  public Stack() {  }
}
public class StackEvent extends EucalyptusData {
  String eventId;
  String logicalResourceId;
  String physicalResourceId;
  String resourceProperties;
  String resourceStatus;
  String resourceStatusReason;
  String resourceType;
  String stackId;
  String stackName;
  Date timestamp;
  public StackEvent() {  }
}
public class StackResource extends EucalyptusData {
  String description;
  String logicalResourceId;
  String physicalResourceId;
  String resourceStatus;
  String resourceStatusReason;
  String resourceType;
  String stackId;
  String stackName;
  Date timestamp;
  public StackResource() {  }
}
public class StackResourceDetail extends EucalyptusData {
  String description;
  Date lastUpdatedTimestamp;
  String logicalResourceId;
  String metadata;
  String physicalResourceId;
  String resourceStatus
  String resourceStatusReason;
  String resourceType;
  String stackId;
  String stackName;
  public StackResourceDetail() {  }
}
public class StackResourceSummary extends EucalyptusData {
  Date lastUpdatedTimestamp;
  String logicalResourceId;
  String physicalResourceId;
  String resourceStatus;
  String resourceStatusReason;
  String resourceType;
  public StackResourceSummary() {  }
}
public class StackSummary extends EucalyptusData {
  Date creationTime;
  Date deletionTime;
  Date lastUpdatedTime;
  String stackId;
  String stackName;
  String stackStatus;
  String stackStatusReason;
  String templateDescription;
  public StackSummary() {  }
}
public class Tag extends EucalyptusData {
  String key;
  String value;
  public Tag() {  }
}
public class TemplateParameter extends EucalyptusData {
  String defaultValue;
  String description;
  Boolean noEcho;
  String parameterKey
  public TemplateParameter() {  }
}
public class UpdateStackResult extends EucalyptusData {
  String stackId;
  public UpdateStackResult() {  }
}
public class ValidateTemplateResult extends EucalyptusData {
  ResourceList capabilities;
  String capabilitiesReason;
  String description;
  TemplateParameters parameters;
  public ValidateTemplateResult() {  }
}
public class CancelUpdateStackType extends CloudFormationMessage {
  String stackName;
  public CancelUpdateStackType() {  }
}
public class CancelUpdateStackResponseType extends CloudFormationMessage {
  public CancelUpdateStackResponseType() {  }
  CancelUpdateStackResult cancelUpdateStackResult = new CancelUpdateStackResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateStackType extends CloudFormationMessage {
  @HttpEmbedded
  ResourceList capabilities;
  Boolean disableRollback;
  @HttpEmbedded
  ResourceList notificationARNs;
  String onFailure;
  @HttpEmbedded
  Parameters parameters;
  String stackName;
  String stackPolicyBody;
  String stackPolicyURL;
  @HttpEmbedded
  Tags tags;
  String templateBody;
  String templateURL;
  Integer timeoutInMinutes;
  public CreateStackType() {  }
}
public class CreateStackResponseType extends CloudFormationMessage {
  public CreateStackResponseType() {  }
  CreateStackResult createStackResult = new CreateStackResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteStackType extends CloudFormationMessage {
  String stackName;
  public DeleteStackType() {  }
}
public class DeleteStackResponseType extends CloudFormationMessage {
  public DeleteStackResponseType() {  }
  DeleteStackResult deleteStackResult = new DeleteStackResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackEventsType extends CloudFormationMessage {
  String nextToken;
  String stackName;
  public DescribeStackEventsType() {  }
}
public class DescribeStackEventsResponseType extends CloudFormationMessage {
  public DescribeStackEventsResponseType() {  }
  DescribeStackEventsResult describeStackEventsResult = new DescribeStackEventsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackResourceType extends CloudFormationMessage {
  String logicalResourceId;
  String stackName;
  public DescribeStackResourceType() {  }
}
public class DescribeStackResourceResponseType extends CloudFormationMessage {
  public DescribeStackResourceResponseType() {  }
  DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackResourcesType extends CloudFormationMessage {
  String logicalResourceId;
  String physicalResourceId;
  String stackName;
  public DescribeStackResourcesType() {  }
}
public class DescribeStackResourcesResponseType extends CloudFormationMessage {
  public DescribeStackResourcesResponseType() {  }
  DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStacksType extends CloudFormationMessage {
  String nextToken;
  String stackName;
  public DescribeStacksType() {  }
}
public class DescribeStacksResponseType extends CloudFormationMessage {
  public DescribeStacksResponseType() {  }
  DescribeStacksResult describeStacksResult = new DescribeStacksResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class EstimateTemplateCostType extends CloudFormationMessage {
  @HttpEmbedded
  Parameters parameters;
  String templateBody;
  String templateURL;
  public EstimateTemplateCostType() {  }
}
public class EstimateTemplateCostResponseType extends CloudFormationMessage {
  public EstimateTemplateCostResponseType() {  }
  EstimateTemplateCostResult estimateTemplateCostResult = new EstimateTemplateCostResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetStackPolicyType extends CloudFormationMessage {
  String stackName;
  public GetStackPolicyType() {  }
}
public class GetStackPolicyResponseType extends CloudFormationMessage {
  public GetStackPolicyResponseType() {  }
  GetStackPolicyResult getStackPolicyResult = new GetStackPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetTemplateType extends CloudFormationMessage {
  String stackName;
  public GetTemplateType() {  }
}
public class GetTemplateResponseType extends CloudFormationMessage {
  public GetTemplateResponseType() {  }
  GetTemplateResult getTemplateResult = new GetTemplateResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ListStackResourcesType extends CloudFormationMessage {
  String nextToken;
  String stackName;
  public ListStackResourcesType() {  }
}
public class ListStackResourcesResponseType extends CloudFormationMessage {
  public ListStackResourcesResponseType() {  }
  ListStackResourcesResult listStackResourcesResult = new ListStackResourcesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ListStacksType extends CloudFormationMessage {
  String nextToken;
  @HttpEmbedded
  ResourceList stackStatusFilter;
  public ListStacksType() {  }
}
public class ListStacksResponseType extends CloudFormationMessage {
  public ListStacksResponseType() {  }
  ListStacksResult listStacksResult = new ListStacksResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SetStackPolicyType extends CloudFormationMessage {
  String stackName;
  String stackPolicyBody;
  String stackPolicyURL;
  public SetStackPolicyType() {  }
}
public class SetStackPolicyResponseType extends CloudFormationMessage {
  public SetStackPolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class UpdateStackType extends CloudFormationMessage {
  @HttpEmbedded
  ResourceList capabilities;
  @HttpEmbedded
  Parameters parameters;
  String stackName;
  String stackPolicyBody;
  String stackPolicyDuringUpdateBody;
  String stackPolicyDuringUpdateURL;
  String stackPolicyURL;
  String templateBody;
  String templateURL;
  public UpdateStackType() {  }
}
public class UpdateStackResponseType extends CloudFormationMessage {
  public UpdateStackResponseType() {  }
  UpdateStackResult updateStackResult = new UpdateStackResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ValidateTemplateType extends CloudFormationMessage {
  String templateBody;
  String templateURL;
  public ValidateTemplateType() {  }
}
public class ValidateTemplateResponseType extends CloudFormationMessage {
  public ValidateTemplateResponseType() {  }
  ValidateTemplateResult validateTemplateResult = new ValidateTemplateResult(); 
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
