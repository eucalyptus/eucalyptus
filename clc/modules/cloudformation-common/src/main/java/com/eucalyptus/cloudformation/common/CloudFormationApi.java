/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common;

import com.eucalyptus.cloudformation.common.msgs.*;


public interface CloudFormationApi {

  CancelUpdateStackResponseType cancelUpdateStack(final CancelUpdateStackType request);

  ContinueUpdateRollbackResponseType continueUpdateRollback(final ContinueUpdateRollbackType request);

  CreateChangeSetResponseType createChangeSet(final CreateChangeSetType request);

  CreateStackResponseType createStack(final CreateStackType request);

  CreateStackInstancesResponseType createStackInstances(final CreateStackInstancesType request);

  CreateStackSetResponseType createStackSet(final CreateStackSetType request);

  DeleteChangeSetResponseType deleteChangeSet(final DeleteChangeSetType request);

  DeleteStackResponseType deleteStack(final DeleteStackType request);

  DeleteStackInstancesResponseType deleteStackInstances(final DeleteStackInstancesType request);

  DeleteStackSetResponseType deleteStackSet(final DeleteStackSetType request);

  DeregisterTypeResponseType deregisterType(final DeregisterTypeType request);

  default DeregisterTypeResponseType deregisterType() {
    return deregisterType(new DeregisterTypeType());
  }

  DescribeAccountLimitsResponseType describeAccountLimits(final DescribeAccountLimitsType request);

  default DescribeAccountLimitsResponseType describeAccountLimits() {
    return describeAccountLimits(new DescribeAccountLimitsType());
  }

  DescribeChangeSetResponseType describeChangeSet(final DescribeChangeSetType request);

  DescribeStackDriftDetectionStatusResponseType describeStackDriftDetectionStatus(final DescribeStackDriftDetectionStatusType request);

  DescribeStackEventsResponseType describeStackEvents(final DescribeStackEventsType request);

  default DescribeStackEventsResponseType describeStackEvents() {
    return describeStackEvents(new DescribeStackEventsType());
  }

  DescribeStackInstanceResponseType describeStackInstance(final DescribeStackInstanceType request);

  DescribeStackResourceResponseType describeStackResource(final DescribeStackResourceType request);

  DescribeStackResourceDriftsResponseType describeStackResourceDrifts(final DescribeStackResourceDriftsType request);

  DescribeStackResourcesResponseType describeStackResources(final DescribeStackResourcesType request);

  default DescribeStackResourcesResponseType describeStackResources() {
    return describeStackResources(new DescribeStackResourcesType());
  }

  DescribeStackSetResponseType describeStackSet(final DescribeStackSetType request);

  DescribeStackSetOperationResponseType describeStackSetOperation(final DescribeStackSetOperationType request);

  DescribeStacksResponseType describeStacks(final DescribeStacksType request);

  default DescribeStacksResponseType describeStacks() {
    return describeStacks(new DescribeStacksType());
  }

  DescribeTypeResponseType describeType(final DescribeTypeType request);

  default DescribeTypeResponseType describeType() {
    return describeType(new DescribeTypeType());
  }

  DescribeTypeRegistrationResponseType describeTypeRegistration(final DescribeTypeRegistrationType request);

  DetectStackDriftResponseType detectStackDrift(final DetectStackDriftType request);

  DetectStackResourceDriftResponseType detectStackResourceDrift(final DetectStackResourceDriftType request);

  DetectStackSetDriftResponseType detectStackSetDrift(final DetectStackSetDriftType request);

  EstimateTemplateCostResponseType estimateTemplateCost(final EstimateTemplateCostType request);

  default EstimateTemplateCostResponseType estimateTemplateCost() {
    return estimateTemplateCost(new EstimateTemplateCostType());
  }

  ExecuteChangeSetResponseType executeChangeSet(final ExecuteChangeSetType request);

  GetStackPolicyResponseType getStackPolicy(final GetStackPolicyType request);

  GetTemplateResponseType getTemplate(final GetTemplateType request);

  default GetTemplateResponseType getTemplate() {
    return getTemplate(new GetTemplateType());
  }

  GetTemplateSummaryResponseType getTemplateSummary(final GetTemplateSummaryType request);

  default GetTemplateSummaryResponseType getTemplateSummary() {
    return getTemplateSummary(new GetTemplateSummaryType());
  }

  ListChangeSetsResponseType listChangeSets(final ListChangeSetsType request);

  ListExportsResponseType listExports(final ListExportsType request);

  default ListExportsResponseType listExports() {
    return listExports(new ListExportsType());
  }

  ListImportsResponseType listImports(final ListImportsType request);

  ListStackInstancesResponseType listStackInstances(final ListStackInstancesType request);

  ListStackResourcesResponseType listStackResources(final ListStackResourcesType request);

  ListStackSetOperationResultsResponseType listStackSetOperationResults(final ListStackSetOperationResultsType request);

  ListStackSetOperationsResponseType listStackSetOperations(final ListStackSetOperationsType request);

  ListStackSetsResponseType listStackSets(final ListStackSetsType request);

  default ListStackSetsResponseType listStackSets() {
    return listStackSets(new ListStackSetsType());
  }

  ListStacksResponseType listStacks(final ListStacksType request);

  default ListStacksResponseType listStacks() {
    return listStacks(new ListStacksType());
  }

  ListTypeRegistrationsResponseType listTypeRegistrations(final ListTypeRegistrationsType request);

  default ListTypeRegistrationsResponseType listTypeRegistrations() {
    return listTypeRegistrations(new ListTypeRegistrationsType());
  }

  ListTypeVersionsResponseType listTypeVersions(final ListTypeVersionsType request);

  default ListTypeVersionsResponseType listTypeVersions() {
    return listTypeVersions(new ListTypeVersionsType());
  }

  ListTypesResponseType listTypes(final ListTypesType request);

  default ListTypesResponseType listTypes() {
    return listTypes(new ListTypesType());
  }

  RecordHandlerProgressResponseType recordHandlerProgress(final RecordHandlerProgressType request);

  RegisterTypeResponseType registerType(final RegisterTypeType request);

  SetStackPolicyResponseType setStackPolicy(final SetStackPolicyType request);

  SetTypeDefaultVersionResponseType setTypeDefaultVersion(final SetTypeDefaultVersionType request);

  default SetTypeDefaultVersionResponseType setTypeDefaultVersion() {
    return setTypeDefaultVersion(new SetTypeDefaultVersionType());
  }

  SignalResourceResponseType signalResource(final SignalResourceType request);

  StopStackSetOperationResponseType stopStackSetOperation(final StopStackSetOperationType request);

  UpdateStackResponseType updateStack(final UpdateStackType request);

  UpdateStackInstancesResponseType updateStackInstances(final UpdateStackInstancesType request);

  UpdateStackSetResponseType updateStackSet(final UpdateStackSetType request);

  UpdateTerminationProtectionResponseType updateTerminationProtection(final UpdateTerminationProtectionType request);

  ValidateTemplateResponseType validateTemplate(final ValidateTemplateType request);

  default ValidateTemplateResponseType validateTemplate() {
    return validateTemplate(new ValidateTemplateType());
  }

}
