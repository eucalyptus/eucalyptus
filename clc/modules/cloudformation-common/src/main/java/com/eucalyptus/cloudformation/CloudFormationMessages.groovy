/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.cloudformation

import com.eucalyptus.ws.WebServiceError
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
  String requestId;
  public ResponseMetadata() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
@ComponentMessage(CloudFormation.class)
@JsonIgnoreProperties(["CorrelationId", "UserId","EffectiveUserId", "CallerContext","Reply","_return","StatusMessage","_epoch","_services","_disabledServices","_notreadyServices","_stoppedServices"] )
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
  String type;
  String code;
  String message;
  public Error() {  }
  ErrorDetail detail
}
public class ResourceList extends EucalyptusData {
  public ResourceList() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class CloudFormationErrorResponse extends CloudFormationMessage implements WebServiceError {
  String requestId
  Error error

  CloudFormationErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @JsonIgnore
  @Override
  String getWebServiceErrorCode( ) {
    error?.code
  }

  @JsonIgnore
  @Override
  String getWebServiceErrorMessage() {
    error?.message
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
public class ParameterDeclarations extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<ParameterDeclaration> member = new ArrayList<ParameterDeclaration>();
  public ParameterDeclarations() {  }
  public ParameterDeclarations( ParameterDeclaration parameterDeclaration ) {
    member.add( parameterDeclaration  )
  }
  @Override
  public String toString() {
    return "ParameterDeclarations [member=" + member + "]";
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
public class ContinueUpdateRollbackResult extends EucalyptusData {
  public ContinueUpdateRollbackResult() {  }
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
  @JsonSerialize(using = StackEventsRemoveMemberSerializer.class, as=StackEvents.class)
  StackEvents stackEvents;
  public DescribeStackEventsResult() {  }
}
public class DescribeStackResourceResult extends EucalyptusData {
  StackResourceDetail stackResourceDetail;
  public DescribeStackResourceResult() {  }
}
public class DescribeStackResourcesResult extends EucalyptusData {
  @JsonSerialize(using = StackResourcesRemoveMemberSerializer.class, as=StackResources.class)
  StackResources stackResources;
  public DescribeStackResourcesResult() {  }
}
public class DescribeStacksResult extends EucalyptusData {
  String nextToken
  @JsonSerialize(using = StacksRemoveMemberSerializer.class, as=Stacks.class)
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
public class GetTemplateSummaryResult extends EucalyptusData {
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList resourceTypes;
  String capabilitiesReason;
  String description;
  String metadata;
  @JsonSerialize(using = ParameterDeclarationsRemoveMemberSerializer.class, as=ParameterDeclarations.class)
  ParameterDeclarations parameters;
  String version;
  public GetTemplateSummaryResult() {  }
}


public class ListStackResourcesResult extends EucalyptusData {
  String nextToken
  @JsonSerialize(using = StackResourceSummariesRemoveMemberSerializer.class, as=StackResourceSummaries.class)
  StackResourceSummaries stackResourceSummaries;
  public ListStackResourcesResult() {  }
}
public class ListStacksResult extends EucalyptusData {
  String nextToken
  @JsonSerialize(using = StackSummariesRemoveMemberSerializer.class, as=StackSummaries.class)
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
  Boolean usePreviousValue;

  public Parameter() {  }
  public Parameter(String key, String value) {
    this.parameterKey = key;
    this.parameterValue = value;
  }
}
public class ParameterConstraints extends EucalyptusData {
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList allowedValues;
  public ParameterConstraints() {  }
}

public class ParameterDeclaration extends EucalyptusData {
  String defaultValue;
  String description;
  Boolean noEcho;
  ParameterConstraints parameterConstraints;
  String parameterKey;
  String parameterType;
  public ParameterDeclaration() {  }
}

public class SignalResourceResult extends EucalyptusData {
  public SignalResourceResult() {  }
}
public class Stack extends EucalyptusData {
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  Date creationTime;
  String description;
  Boolean disableRollback;
  Date lastUpdatedTime;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList notificationARNs;
  @JsonSerialize(using = OutputsRemoveMemberSerializer.class, as=Outputs.class)
  Outputs outputs;
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  String stackId;
  String stackName;
  String stackStatus;
  String stackStatusReason;
  @JsonSerialize(using = TagsRemoveMemberSerializer.class, as=Tags.class)
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
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  String capabilitiesReason;
  String description;
  @JsonSerialize(using = TemplateParametersRemoveMemberSerializer.class, as=TemplateParameters.class)
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
public class ContinueUpdateRollbackType extends CloudFormationMessage {
  String stackName;
  public ContinueUpdateRollbackType() {  }
}
public class ContinueUpdateRollbackResponseType extends CloudFormationMessage {
  public ContinueUpdateRollbackResponseType() {  }
  ContinueUpdateRollbackResult continueUpdateRollbackResult = new ContinueUpdateRollbackResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateStackType extends CloudFormationMessage {
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  Boolean disableRollback;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList notificationARNs;
  String onFailure;
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList resourceTypes;
  String stackName;
  String stackPolicyBody;
  String stackPolicyURL;
  @JsonSerialize(using = TagsRemoveMemberSerializer.class, as=Tags.class)
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
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
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
public class GetTemplateSummaryType extends CloudFormationMessage {
  public GetTemplateSummaryType() {  }
  String stackName;
  String templateBody;
  String templateURL;
}
public class GetTemplateSummaryResponseType extends CloudFormationMessage {
  public GetTemplateSummaryResponseType() {  }
  GetTemplateSummaryResult getTemplateSummaryResult = new GetTemplateSummaryResult();
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
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
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
public class SignalResourceType extends CloudFormationMessage {
  String logicalResourceId;
  String stackName;
  String status;
  String uniqueId;
  public SignalResourceType() {  }
}
public class SignalResourceResponseType extends CloudFormationMessage {
  public SignalResourceResponseType() {  }
  SignalResourceResult signalResourceResult = new SignalResourceResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class UpdateStackType extends CloudFormationMessage {
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList capabilities;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList notificationARNs;
  @JsonSerialize(using = ParametersRemoveMemberSerializer.class, as=Parameters.class)
  Parameters parameters;
  @JsonSerialize(using = ResourceListRemoveMemberSerializer.class, as=ResourceList.class)
  ResourceList resourceTypes;
  String stackName;
  String stackPolicyBody;
  String stackPolicyDuringUpdateBody;
  String stackPolicyDuringUpdateURL;
  String stackPolicyURL;
  @JsonSerialize(using = TagsRemoveMemberSerializer.class, as=Tags.class)
  Tags tags;
  String templateBody;
  String templateURL;
  Boolean usePreviousTemplate;
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

public class ParameterDeclarationsRemoveMemberSerializer extends JsonSerializer<ParameterDeclarations> {
  @Override
  void serialize(ParameterDeclarations parameterDeclarations, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
    if (parameterDeclarations == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeStartArray();
      for (String item: parameterDeclarations.getMember()) {
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

