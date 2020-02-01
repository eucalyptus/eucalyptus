/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common;

import com.eucalyptus.cloudformation.common.msgs.*;
import com.eucalyptus.util.async.CheckedListenableFuture;

public interface CloudFormationApiAsync {

  CheckedListenableFuture<CancelUpdateStackResponseType> cancelUpdateStackAsync(final CancelUpdateStackType request);

  CheckedListenableFuture<ContinueUpdateRollbackResponseType> continueUpdateRollbackAsync(final ContinueUpdateRollbackType request);

  CheckedListenableFuture<CreateChangeSetResponseType> createChangeSetAsync(final CreateChangeSetType request);

  CheckedListenableFuture<CreateStackResponseType> createStackAsync(final CreateStackType request);

  CheckedListenableFuture<CreateStackInstancesResponseType> createStackInstancesAsync(final CreateStackInstancesType request);

  CheckedListenableFuture<CreateStackSetResponseType> createStackSetAsync(final CreateStackSetType request);

  CheckedListenableFuture<DeleteChangeSetResponseType> deleteChangeSetAsync(final DeleteChangeSetType request);

  CheckedListenableFuture<DeleteStackResponseType> deleteStackAsync(final DeleteStackType request);

  CheckedListenableFuture<DeleteStackInstancesResponseType> deleteStackInstancesAsync(final DeleteStackInstancesType request);

  CheckedListenableFuture<DeleteStackSetResponseType> deleteStackSetAsync(final DeleteStackSetType request);

  CheckedListenableFuture<DeregisterTypeResponseType> deregisterTypeAsync(final DeregisterTypeType request);

  default CheckedListenableFuture<DeregisterTypeResponseType> deregisterTypeAsync() {
    return deregisterTypeAsync(new DeregisterTypeType());
  }

  CheckedListenableFuture<DescribeAccountLimitsResponseType> describeAccountLimitsAsync(final DescribeAccountLimitsType request);

  default CheckedListenableFuture<DescribeAccountLimitsResponseType> describeAccountLimitsAsync() {
    return describeAccountLimitsAsync(new DescribeAccountLimitsType());
  }

  CheckedListenableFuture<DescribeChangeSetResponseType> describeChangeSetAsync(final DescribeChangeSetType request);

  CheckedListenableFuture<DescribeStackDriftDetectionStatusResponseType> describeStackDriftDetectionStatusAsync(final DescribeStackDriftDetectionStatusType request);

  CheckedListenableFuture<DescribeStackEventsResponseType> describeStackEventsAsync(final DescribeStackEventsType request);

  default CheckedListenableFuture<DescribeStackEventsResponseType> describeStackEventsAsync() {
    return describeStackEventsAsync(new DescribeStackEventsType());
  }

  CheckedListenableFuture<DescribeStackInstanceResponseType> describeStackInstanceAsync(final DescribeStackInstanceType request);

  CheckedListenableFuture<DescribeStackResourceResponseType> describeStackResourceAsync(final DescribeStackResourceType request);

  CheckedListenableFuture<DescribeStackResourceDriftsResponseType> describeStackResourceDriftsAsync(final DescribeStackResourceDriftsType request);

  CheckedListenableFuture<DescribeStackResourcesResponseType> describeStackResourcesAsync(final DescribeStackResourcesType request);

  default CheckedListenableFuture<DescribeStackResourcesResponseType> describeStackResourcesAsync() {
    return describeStackResourcesAsync(new DescribeStackResourcesType());
  }

  CheckedListenableFuture<DescribeStackSetResponseType> describeStackSetAsync(final DescribeStackSetType request);

  CheckedListenableFuture<DescribeStackSetOperationResponseType> describeStackSetOperationAsync(final DescribeStackSetOperationType request);

  CheckedListenableFuture<DescribeStacksResponseType> describeStacksAsync(final DescribeStacksType request);

  default CheckedListenableFuture<DescribeStacksResponseType> describeStacksAsync() {
    return describeStacksAsync(new DescribeStacksType());
  }

  CheckedListenableFuture<DescribeTypeResponseType> describeTypeAsync(final DescribeTypeType request);

  default CheckedListenableFuture<DescribeTypeResponseType> describeTypeAsync() {
    return describeTypeAsync(new DescribeTypeType());
  }

  CheckedListenableFuture<DescribeTypeRegistrationResponseType> describeTypeRegistrationAsync(final DescribeTypeRegistrationType request);

  CheckedListenableFuture<DetectStackDriftResponseType> detectStackDriftAsync(final DetectStackDriftType request);

  CheckedListenableFuture<DetectStackResourceDriftResponseType> detectStackResourceDriftAsync(final DetectStackResourceDriftType request);

  CheckedListenableFuture<DetectStackSetDriftResponseType> detectStackSetDriftAsync(final DetectStackSetDriftType request);

  CheckedListenableFuture<EstimateTemplateCostResponseType> estimateTemplateCostAsync(final EstimateTemplateCostType request);

  default CheckedListenableFuture<EstimateTemplateCostResponseType> estimateTemplateCostAsync() {
    return estimateTemplateCostAsync(new EstimateTemplateCostType());
  }

  CheckedListenableFuture<ExecuteChangeSetResponseType> executeChangeSetAsync(final ExecuteChangeSetType request);

  CheckedListenableFuture<GetStackPolicyResponseType> getStackPolicyAsync(final GetStackPolicyType request);

  CheckedListenableFuture<GetTemplateResponseType> getTemplateAsync(final GetTemplateType request);

  default CheckedListenableFuture<GetTemplateResponseType> getTemplateAsync() {
    return getTemplateAsync(new GetTemplateType());
  }

  CheckedListenableFuture<GetTemplateSummaryResponseType> getTemplateSummaryAsync(final GetTemplateSummaryType request);

  default CheckedListenableFuture<GetTemplateSummaryResponseType> getTemplateSummaryAsync() {
    return getTemplateSummaryAsync(new GetTemplateSummaryType());
  }

  CheckedListenableFuture<ListChangeSetsResponseType> listChangeSetsAsync(final ListChangeSetsType request);

  CheckedListenableFuture<ListExportsResponseType> listExportsAsync(final ListExportsType request);

  default CheckedListenableFuture<ListExportsResponseType> listExportsAsync() {
    return listExportsAsync(new ListExportsType());
  }

  CheckedListenableFuture<ListImportsResponseType> listImportsAsync(final ListImportsType request);

  CheckedListenableFuture<ListStackInstancesResponseType> listStackInstancesAsync(final ListStackInstancesType request);

  CheckedListenableFuture<ListStackResourcesResponseType> listStackResourcesAsync(final ListStackResourcesType request);

  CheckedListenableFuture<ListStackSetOperationResultsResponseType> listStackSetOperationResultsAsync(final ListStackSetOperationResultsType request);

  CheckedListenableFuture<ListStackSetOperationsResponseType> listStackSetOperationsAsync(final ListStackSetOperationsType request);

  CheckedListenableFuture<ListStackSetsResponseType> listStackSetsAsync(final ListStackSetsType request);

  default CheckedListenableFuture<ListStackSetsResponseType> listStackSetsAsync() {
    return listStackSetsAsync(new ListStackSetsType());
  }

  CheckedListenableFuture<ListStacksResponseType> listStacksAsync(final ListStacksType request);

  default CheckedListenableFuture<ListStacksResponseType> listStacksAsync() {
    return listStacksAsync(new ListStacksType());
  }

  CheckedListenableFuture<ListTypeRegistrationsResponseType> listTypeRegistrationsAsync(final ListTypeRegistrationsType request);

  default CheckedListenableFuture<ListTypeRegistrationsResponseType> listTypeRegistrationsAsync() {
    return listTypeRegistrationsAsync(new ListTypeRegistrationsType());
  }

  CheckedListenableFuture<ListTypeVersionsResponseType> listTypeVersionsAsync(final ListTypeVersionsType request);

  default CheckedListenableFuture<ListTypeVersionsResponseType> listTypeVersionsAsync() {
    return listTypeVersionsAsync(new ListTypeVersionsType());
  }

  CheckedListenableFuture<ListTypesResponseType> listTypesAsync(final ListTypesType request);

  default CheckedListenableFuture<ListTypesResponseType> listTypesAsync() {
    return listTypesAsync(new ListTypesType());
  }

  CheckedListenableFuture<RecordHandlerProgressResponseType> recordHandlerProgressAsync(final RecordHandlerProgressType request);

  CheckedListenableFuture<RegisterTypeResponseType> registerTypeAsync(final RegisterTypeType request);

  CheckedListenableFuture<SetStackPolicyResponseType> setStackPolicyAsync(final SetStackPolicyType request);

  CheckedListenableFuture<SetTypeDefaultVersionResponseType> setTypeDefaultVersionAsync(final SetTypeDefaultVersionType request);

  default CheckedListenableFuture<SetTypeDefaultVersionResponseType> setTypeDefaultVersionAsync() {
    return setTypeDefaultVersionAsync(new SetTypeDefaultVersionType());
  }

  CheckedListenableFuture<SignalResourceResponseType> signalResourceAsync(final SignalResourceType request);

  CheckedListenableFuture<StopStackSetOperationResponseType> stopStackSetOperationAsync(final StopStackSetOperationType request);

  CheckedListenableFuture<UpdateStackResponseType> updateStackAsync(final UpdateStackType request);

  CheckedListenableFuture<UpdateStackInstancesResponseType> updateStackInstancesAsync(final UpdateStackInstancesType request);

  CheckedListenableFuture<UpdateStackSetResponseType> updateStackSetAsync(final UpdateStackSetType request);

  CheckedListenableFuture<UpdateTerminationProtectionResponseType> updateTerminationProtectionAsync(final UpdateTerminationProtectionType request);

  CheckedListenableFuture<ValidateTemplateResponseType> validateTemplateAsync(final ValidateTemplateType request);

  default CheckedListenableFuture<ValidateTemplateResponseType> validateTemplateAsync() {
    return validateTemplateAsync(new ValidateTemplateType());
  }

}
