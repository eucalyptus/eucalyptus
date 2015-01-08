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
@GroovyAddClassUUID
package com.eucalyptus.cloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

import java.lang.reflect.Field;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.annotation.ComponentMessage;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ResponseMetadata extends EucalyptusData {
  @JsonProperty("RequestId")
  String requestId;
  public ResponseMetadata() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
@ComponentMessage(CloudFormation.class)
@JsonIgnoreProperties(["correlationId", "userId","effectiveUserId","callerContext","_return","statusMessage","_epoch","_services","_disabledServices","_notreadyServices","_stoppedServices"] )
public class CloudFormationMessage extends BaseMessage {
  @Override
  @JsonIgnore
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
  @JsonProperty("Type")
  String type;
  @JsonProperty("Code")
  String code;
  @JsonProperty("Message")
  String message;
  public Error() {  }
  @JsonProperty("Detail")
  ErrorDetail detail = new ErrorDetail();
}
public class ResourceList extends EucalyptusData {
  public ResourceList() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class CloudFormationErrorResponse extends CloudFormationMessage {
  @JsonProperty("RequestId")
  String requestId;
  @JsonProperty("Error")
  ArrayList<Error> error = new ArrayList<Error>( );

  CloudFormationErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${error?.getAt(0)?.code}): ${error?.getAt(0)?.message}"
  }
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
  @JsonProperty("StackId")
  String stackId;
  public CreateStackResult() {  }
}
public class DeleteStackResult extends EucalyptusData {
  public DeleteStackResult() {  }
}

public class DescribeStackEventsResult extends EucalyptusData {
  @JsonProperty("NextToken")
  String nextToken
  @JsonProperty("StackEvents")
  @JsonSerialize(using = StackEventsRemoveMemberSerializer.class, as=StackEvents.class)
  StackEvents stackEvents;
  public DescribeStackEventsResult() {  }
}
public class DescribeStackResourceResult extends EucalyptusData {
  @JsonProperty("StackResourceDetail")
  StackResourceDetail stackResourceDetail;
  public DescribeStackResourceResult() {  }
}
public class DescribeStackResourcesResult extends EucalyptusData {
  @JsonProperty("StackResources")
  @JsonSerialize(using = StackResourcesRemoveMemberSerializer.class, as=StackResources.class)
  StackResources stackResources;
  public DescribeStackResourcesResult() {  }
}
public class DescribeStacksResult extends EucalyptusData {
  @JsonProperty("NextToken")
  String nextToken
  @JsonProperty("Stacks")
  @JsonSerialize(using = StacksRemoveMemberSerializer.class, as=Stacks.class)
  Stacks stacks;
  public DescribeStacksResult() {  }
}
public class EstimateTemplateCostResult extends EucalyptusData {
  @JsonProperty("Url")
  String url;
  public EstimateTemplateCostResult() {  }
}
public class GetStackPolicyResult extends EucalyptusData {
  @JsonProperty("StackPolicyBody")
  String stackPolicyBody;
  public GetStackPolicyResult() {  }
}
public class GetTemplateResult extends EucalyptusData {
  @JsonProperty("TemplateBody")
  String templateBody;
  public GetTemplateResult() {  }
}
public class ListStackResourcesResult extends EucalyptusData {
  @JsonProperty("NextToken")
  String nextToken
  @JsonProperty("StackResourceSummaries")
  @JsonSerialize(using = StackResourceSummariesRemoveMemberSerializer.class, as=StackResourceSummaries.class)
  StackResourceSummaries stackResourceSummaries;
  public ListStackResourcesResult() {  }
}
public class ListStacksResult extends EucalyptusData {
  @JsonProperty("NextToken")
  String nextToken
  @JsonProperty("StackSummaries")
  @JsonSerialize(using = StackSummariesRemoveMemberSerializer.class, as=StackSummaries.class)
  StackSummaries stackSummaries;
  public ListStacksResult() {  }
}
public class Output extends EucalyptusData {
  @JsonProperty("Description")
  String description;
  @JsonProperty("OutputKey")
  String outputKey;
  @JsonProperty("OutputValue")
  String outputValue;
  public Output() {  }
}
public class Parameter extends EucalyptusData {
  @JsonProperty("ParameterKey")
  String parameterKey;
  @JsonProperty("ParameterValue")
  String parameterValue;
  public Parameter() {  }
}
public class Stack extends EucalyptusData {
  @JsonProperty("Capabilities")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @JsonProperty("CreationTime")
  Date creationTime;
  @JsonProperty("Description")
  String description;
  @JsonProperty("DisableRollback")
  Boolean disableRollback;
  @JsonProperty("LastUpdatedTime")
  Date lastUpdatedTime;
  @JsonProperty("NotificationARNs")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList notificationARNs;
  @JsonProperty("Outputs")
  @JsonSerialize(using = OutputsRemoveMemberSerializer.class, as=Outputs.class)
  Outputs outputs;
  @JsonProperty("Parameters")
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonProperty("StackId")
  String stackId;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("StackStatus")
  String stackStatus;
  @JsonProperty("StackStatusReason")
  String stackStatusReason;
  @JsonProperty("Tags")
  @JsonSerialize(using = TagsRemoveMemberSerializer.class, as=Tags.class)
  Tags tags;
  @JsonProperty("TimeoutInMinutes")
  Integer timeoutInMinutes;
  public Stack() {  }
}
public class StackEvent extends EucalyptusData {
  @JsonProperty("EventId")
  String eventId;
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("PhysicalResourceId")
  String physicalResourceId;
  @JsonProperty("ResourceProperties")
  String resourceProperties;
  @JsonProperty("ResourceStatus")
  String resourceStatus;
  @JsonProperty("ResourceStatusReason")
  String resourceStatusReason;
  @JsonProperty("ResourceType")
  String resourceType;
  @JsonProperty("StackId")
  String stackId;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("Timestamp")
  Date timestamp;
  public StackEvent() {  }
}
public class StackResource extends EucalyptusData {
  @JsonProperty("Description")
  String description;
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("PhysicalResourceId")
  String physicalResourceId;
  @JsonProperty("ResourceStatus")
  String resourceStatus;
  @JsonProperty("ResourceStatusReason")
  String resourceStatusReason;
  @JsonProperty("ResourceType")
  String resourceType;
  @JsonProperty("StackId")
  String stackId;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("Timestamp")
  Date timestamp;
  public StackResource() {  }
}
public class StackResourceDetail extends EucalyptusData {
  @JsonProperty("Description")
  String description;
  @JsonProperty("LastUpdatedTimestamp")
  Date lastUpdatedTimestamp;
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("Metadata")
  String metadata;
  @JsonProperty("PhysicalResourceId")
  String physicalResourceId;
  @JsonProperty("ResourceStatus")
  String resourceStatus
  @JsonProperty("ResourceStatusReason")
  String resourceStatusReason;
  @JsonProperty("ResourceType")
  String resourceType;
  @JsonProperty("StackId")
  String stackId;
  @JsonProperty("StackName")
  String stackName;
  public StackResourceDetail() {  }
}
public class StackResourceSummary extends EucalyptusData {
  @JsonProperty("LastUpdatedTimestamp")
  Date lastUpdatedTimestamp;
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("PhysicalResourceId")
  String physicalResourceId;
  @JsonProperty("ResourceStatus")
  String resourceStatus;
  @JsonProperty("ResourceStatusReason")
  String resourceStatusReason;
  @JsonProperty("ResourceType")
  String resourceType;
  public StackResourceSummary() {  }
}
public class StackSummary extends EucalyptusData {
  @JsonProperty("CreationTime")
  Date creationTime;
  @JsonProperty("DeletionTime")
  Date deletionTime;
  @JsonProperty("LastUpdatedTime")
  Date lastUpdatedTime;
  @JsonProperty("StackId")
  String stackId;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("StackStatus")
  String stackStatus;
  @JsonProperty("StackStatusReason")
  String stackStatusReason;
  @JsonProperty("TemplateDescription")
  String templateDescription;
  public StackSummary() {  }
}
public class Tag extends EucalyptusData {
  @JsonProperty("Key")
  String key;
  @JsonProperty("Value")
  String value;
  public Tag() {  }
}
public class TemplateParameter extends EucalyptusData {
  @JsonProperty("DefaultValue")
  String defaultValue;
  @JsonProperty("Description")
  String description;
  @JsonProperty("NoEcho")
  Boolean noEcho;
  @JsonProperty("ParameterKey")
  String parameterKey
  public TemplateParameter() {  }
}
public class UpdateStackResult extends EucalyptusData {
  @JsonProperty("StackId")
  String stackId;
  public UpdateStackResult() {  }
}
public class ValidateTemplateResult extends EucalyptusData {
  @JsonProperty("Capabilities")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @JsonProperty("CapabilitiesReason")
  String capabilitiesReason;
  @JsonProperty("Description")
  String description;
  @JsonProperty("Parameters")
  @JsonSerialize(using = TemplateParametersRemoveMemberSerializer.class, as=TemplateParameters.class)
  TemplateParameters parameters;
  public ValidateTemplateResult() {  }
}
public class CancelUpdateStackType extends CloudFormationMessage {
  @JsonProperty("StackName")
  String stackName;
  public CancelUpdateStackType() {  }
}
public class CancelUpdateStackResponseType extends CloudFormationMessage {
  public CancelUpdateStackResponseType() {  }
  @JsonProperty("CancelUpdateStackResult")
  CancelUpdateStackResult cancelUpdateStackResult = new CancelUpdateStackResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateStackType extends CloudFormationMessage {
  @HttpEmbedded
  @JsonProperty("Capabilities")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @JsonProperty("DisableRollback")
  Boolean disableRollback;
  @HttpEmbedded
  @JsonProperty("NotificationARNs")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList notificationARNs;
  @JsonProperty("OnFailure")
  String onFailure;
  @HttpEmbedded
  @JsonProperty("Parameters")
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("StackPolicyBody")
  String stackPolicyBody;
  @JsonProperty("StackPolicyURL")
  String stackPolicyURL;
  @HttpEmbedded
  @JsonProperty("Tags")
  @JsonSerialize(using = TagsRemoveMemberSerializer.class, as=Tags.class)
  Tags tags;
  @JsonProperty("TemplateBody")
  String templateBody;
  @JsonProperty("TemplateURL")
  String templateURL;
  @JsonProperty("TimeoutInMinutes")
  Integer timeoutInMinutes;
  public CreateStackType() {  }
}
public class CreateStackResponseType extends CloudFormationMessage {
  public CreateStackResponseType() {  }
  @JsonProperty("CreateStackResult")
  CreateStackResult createStackResult = new CreateStackResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteStackType extends CloudFormationMessage {
  @JsonProperty("StackName")
  String stackName;
  public DeleteStackType() {  }
}
public class DeleteStackResponseType extends CloudFormationMessage {
  public DeleteStackResponseType() {  }
  @JsonProperty("DeleteStackResult")
  DeleteStackResult deleteStackResult = new DeleteStackResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackEventsType extends CloudFormationMessage {
  @JsonProperty("NextToken")
  String nextToken;
  @JsonProperty("StackName")
  String stackName;
  public DescribeStackEventsType() {  }
}
public class DescribeStackEventsResponseType extends CloudFormationMessage {
  public DescribeStackEventsResponseType() {  }
  @JsonProperty("DescribeStackEventsResult")
  DescribeStackEventsResult describeStackEventsResult = new DescribeStackEventsResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackResourceType extends CloudFormationMessage {
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("StackName")
  String stackName;
  public DescribeStackResourceType() {  }
}
public class DescribeStackResourceResponseType extends CloudFormationMessage {
  public DescribeStackResourceResponseType() {  }
  @JsonProperty("DescribeStackResourceResult")
  DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStackResourcesType extends CloudFormationMessage {
  @JsonProperty("LogicalResourceId")
  String logicalResourceId;
  @JsonProperty("PhysicalResourceId")
  String physicalResourceId;
  @JsonProperty("StackName")
  String stackName;
  public DescribeStackResourcesType() {  }
}
public class DescribeStackResourcesResponseType extends CloudFormationMessage {
  public DescribeStackResourcesResponseType() {  }
  @JsonProperty("DescribeStackResourcesResult")
  DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeStacksType extends CloudFormationMessage {
  @JsonProperty("NextToken")
  String nextToken;
  @JsonProperty("StackName")
  String stackName;
  public DescribeStacksType() {  }
}
public class DescribeStacksResponseType extends CloudFormationMessage {
  public DescribeStacksResponseType() {  }
  @JsonProperty("DescribeStacksResult")
  DescribeStacksResult describeStacksResult = new DescribeStacksResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class EstimateTemplateCostType extends CloudFormationMessage {
  @HttpEmbedded
  @JsonProperty("Parameters")
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonProperty("TemplateBody")
  String templateBody;
  @JsonProperty("TemplateURL")
  String templateURL;
  public EstimateTemplateCostType() {  }
}
public class EstimateTemplateCostResponseType extends CloudFormationMessage {
  public EstimateTemplateCostResponseType() {  }
  @JsonProperty("EstimateTemplateCostResult")
  EstimateTemplateCostResult estimateTemplateCostResult = new EstimateTemplateCostResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetStackPolicyType extends CloudFormationMessage {
  @JsonProperty("StackName")
  String stackName;
  public GetStackPolicyType() {  }
}
public class GetStackPolicyResponseType extends CloudFormationMessage {
  public GetStackPolicyResponseType() {  }
  @JsonProperty("GetStackPolicyResult")
  GetStackPolicyResult getStackPolicyResult = new GetStackPolicyResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetTemplateType extends CloudFormationMessage {
  @JsonProperty("StackName")
  String stackName;
  public GetTemplateType() {  }
}
public class GetTemplateResponseType extends CloudFormationMessage {
  public GetTemplateResponseType() {  }
  @JsonProperty("GetTemplateResult")
  GetTemplateResult getTemplateResult = new GetTemplateResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ListStackResourcesType extends CloudFormationMessage {
  @JsonProperty("NextToken")
  String nextToken;
  @JsonProperty("StackName")
  String stackName;
  public ListStackResourcesType() {  }
}
public class ListStackResourcesResponseType extends CloudFormationMessage {
  public ListStackResourcesResponseType() {  }
  @JsonProperty("ListStackResourcesResult")
  ListStackResourcesResult listStackResourcesResult = new ListStackResourcesResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ListStacksType extends CloudFormationMessage {
  @JsonProperty("NextToken")
  String nextToken;
  @HttpEmbedded
  @JsonProperty("StackStatusFilter")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList stackStatusFilter;
  public ListStacksType() {  }
}
public class ListStacksResponseType extends CloudFormationMessage {
  public ListStacksResponseType() {  }
  @JsonProperty("ListStacksResult")
  ListStacksResult listStacksResult = new ListStacksResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SetStackPolicyType extends CloudFormationMessage {
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("StackPolicyBody")
  String stackPolicyBody;
  @JsonProperty("StackPolicyURL")
  String stackPolicyURL;
  public SetStackPolicyType() {  }
}
public class SetStackPolicyResponseType extends CloudFormationMessage {
  public SetStackPolicyResponseType() {  }
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class UpdateStackType extends CloudFormationMessage {
  @HttpEmbedded
  @JsonProperty("Capabilities")
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @HttpEmbedded
  @JsonProperty("Parameters")
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonProperty("StackName")
  String stackName;
  @JsonProperty("StackPolicyBody")
  String stackPolicyBody;
  @JsonProperty("StackPolicyDuringUpdateBody")
  String stackPolicyDuringUpdateBody;
  @JsonProperty("StackPolicyDuringUpdateURL")
  String stackPolicyDuringUpdateURL;
  @JsonProperty("StackPolicyURL")
  String stackPolicyURL;
  @JsonProperty("TemplateBody")
  String templateBody;
  @JsonProperty("TemplateURL")
  String templateURL;
  public UpdateStackType() {  }
}
public class UpdateStackResponseType extends CloudFormationMessage {
  public UpdateStackResponseType() {  }
  @JsonProperty("UpdateStackResult")
  UpdateStackResult updateStackResult = new UpdateStackResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ValidateTemplateType extends CloudFormationMessage {
  @JsonProperty("TemplateBody")
  String templateBody;
  @JsonProperty("TemplateURL")
  String templateURL;
  public ValidateTemplateType() {  }
}
public class ValidateTemplateResponseType extends CloudFormationMessage {
  public ValidateTemplateResponseType() {  }
  @JsonProperty("ValidateTemplateResult")
  ValidateTemplateResult validateTemplateResult = new ValidateTemplateResult();
  @JsonProperty("ResponseMetadata")
  ResponseMetadata responseMetadata = new ResponseMetadata();
}

// TODO: figure out how to consolidate serializers below
public class ResourceListRemoveMemberSerializer extends JsonSerializer<ResourceList> {
  @Override
  void serialize(ResourceList resourceList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (resourceList == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: resourceList.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class OutputsRemoveMemberSerializer extends JsonSerializer<Outputs> {
  @Override
  void serialize(Outputs outputs, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (outputs == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: outputs.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class ParametersRemoveMemberSerializer extends JsonSerializer<Parameters> {
  @Override
  void serialize(Parameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (parameters == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: parameters.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class StackEventsRemoveMemberSerializer extends JsonSerializer<StackEvents> {
  @Override
  void serialize(StackEvents stackEvents, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (stackEvents == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: stackEvents.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class StackResourcesRemoveMemberSerializer extends JsonSerializer<StackResources> {
  @Override
  void serialize(StackResources stackResources, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (stackResources == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: stackResources.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class StackResourceSummariesRemoveMemberSerializer extends JsonSerializer<StackResourceSummaries> {
  @Override
  void serialize(StackResourceSummaries stackResourceSummaries, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (stackResourceSummaries == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: stackResourceSummaries.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class StackSummariesRemoveMemberSerializer extends JsonSerializer<StackSummaries> {
  @Override
  void serialize(StackSummaries stackSummaries, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (stackSummaries == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: stackSummaries.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class StacksRemoveMemberSerializer extends JsonSerializer<Stacks> {
  @Override
  void serialize(Stacks stacks, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (stacks == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: stacks.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class TagsRemoveMemberSerializer extends JsonSerializer<Tags> {
  @Override
  void serialize(Tags tags, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (tags == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: tags.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

public class TemplateParametersRemoveMemberSerializer extends JsonSerializer<TemplateParameters> {
  @Override
  void serialize(TemplateParameters templateParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (templateParameters == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: templateParameters.getMember()) {
        jsonGenerator.writeObject(item);
      }
      jsonGenerator.writeEndArray();
    }
  }
}

