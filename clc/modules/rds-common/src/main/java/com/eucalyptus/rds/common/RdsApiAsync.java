/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.rds.common.msgs.*;
import com.eucalyptus.util.async.CheckedListenableFuture;

@ComponentPart(Rds.class)
public interface RdsApiAsync {

  CheckedListenableFuture<AddRoleToDBClusterResponseType> addRoleToDBClusterAsync(final AddRoleToDBClusterType request);

  CheckedListenableFuture<AddRoleToDBInstanceResponseType> addRoleToDBInstanceAsync(final AddRoleToDBInstanceType request);

  CheckedListenableFuture<AddSourceIdentifierToSubscriptionResponseType> addSourceIdentifierToSubscriptionAsync(final AddSourceIdentifierToSubscriptionType request);

  CheckedListenableFuture<AddTagsToResourceResponseType> addTagsToResourceAsync(final AddTagsToResourceType request);

  CheckedListenableFuture<ApplyPendingMaintenanceActionResponseType> applyPendingMaintenanceActionAsync(final ApplyPendingMaintenanceActionType request);

  CheckedListenableFuture<AuthorizeDBSecurityGroupIngressResponseType> authorizeDBSecurityGroupIngressAsync(final AuthorizeDBSecurityGroupIngressType request);

  CheckedListenableFuture<BacktrackDBClusterResponseType> backtrackDBClusterAsync(final BacktrackDBClusterType request);

  CheckedListenableFuture<CancelExportTaskResponseType> cancelExportTaskAsync(final CancelExportTaskType request);

  CheckedListenableFuture<CopyDBClusterParameterGroupResponseType> copyDBClusterParameterGroupAsync(final CopyDBClusterParameterGroupType request);

  CheckedListenableFuture<CopyDBClusterSnapshotResponseType> copyDBClusterSnapshotAsync(final CopyDBClusterSnapshotType request);

  CheckedListenableFuture<CopyDBParameterGroupResponseType> copyDBParameterGroupAsync(final CopyDBParameterGroupType request);

  CheckedListenableFuture<CopyDBSnapshotResponseType> copyDBSnapshotAsync(final CopyDBSnapshotType request);

  CheckedListenableFuture<CopyOptionGroupResponseType> copyOptionGroupAsync(final CopyOptionGroupType request);

  CheckedListenableFuture<CreateCustomAvailabilityZoneResponseType> createCustomAvailabilityZoneAsync(final CreateCustomAvailabilityZoneType request);

  CheckedListenableFuture<CreateDBClusterResponseType> createDBClusterAsync(final CreateDBClusterType request);

  CheckedListenableFuture<CreateDBClusterEndpointResponseType> createDBClusterEndpointAsync(final CreateDBClusterEndpointType request);

  CheckedListenableFuture<CreateDBClusterParameterGroupResponseType> createDBClusterParameterGroupAsync(final CreateDBClusterParameterGroupType request);

  CheckedListenableFuture<CreateDBClusterSnapshotResponseType> createDBClusterSnapshotAsync(final CreateDBClusterSnapshotType request);

  CheckedListenableFuture<CreateDBInstanceResponseType> createDBInstanceAsync(final CreateDBInstanceType request);

  CheckedListenableFuture<CreateDBInstanceReadReplicaResponseType> createDBInstanceReadReplicaAsync(final CreateDBInstanceReadReplicaType request);

  CheckedListenableFuture<CreateDBParameterGroupResponseType> createDBParameterGroupAsync(final CreateDBParameterGroupType request);

  CheckedListenableFuture<CreateDBProxyResponseType> createDBProxyAsync(final CreateDBProxyType request);

  CheckedListenableFuture<CreateDBSecurityGroupResponseType> createDBSecurityGroupAsync(final CreateDBSecurityGroupType request);

  CheckedListenableFuture<CreateDBSnapshotResponseType> createDBSnapshotAsync(final CreateDBSnapshotType request);

  CheckedListenableFuture<CreateDBSubnetGroupResponseType> createDBSubnetGroupAsync(final CreateDBSubnetGroupType request);

  CheckedListenableFuture<CreateEventSubscriptionResponseType> createEventSubscriptionAsync(final CreateEventSubscriptionType request);

  CheckedListenableFuture<CreateGlobalClusterResponseType> createGlobalClusterAsync(final CreateGlobalClusterType request);

  default CheckedListenableFuture<CreateGlobalClusterResponseType> createGlobalClusterAsync() {
    return createGlobalClusterAsync(new CreateGlobalClusterType());
  }

  CheckedListenableFuture<CreateOptionGroupResponseType> createOptionGroupAsync(final CreateOptionGroupType request);

  CheckedListenableFuture<DeleteCustomAvailabilityZoneResponseType> deleteCustomAvailabilityZoneAsync(final DeleteCustomAvailabilityZoneType request);

  CheckedListenableFuture<DeleteDBClusterResponseType> deleteDBClusterAsync(final DeleteDBClusterType request);

  CheckedListenableFuture<DeleteDBClusterEndpointResponseType> deleteDBClusterEndpointAsync(final DeleteDBClusterEndpointType request);

  CheckedListenableFuture<DeleteDBClusterParameterGroupResponseType> deleteDBClusterParameterGroupAsync(final DeleteDBClusterParameterGroupType request);

  CheckedListenableFuture<DeleteDBClusterSnapshotResponseType> deleteDBClusterSnapshotAsync(final DeleteDBClusterSnapshotType request);

  CheckedListenableFuture<DeleteDBInstanceResponseType> deleteDBInstanceAsync(final DeleteDBInstanceType request);

  CheckedListenableFuture<DeleteDBInstanceAutomatedBackupResponseType> deleteDBInstanceAutomatedBackupAsync(final DeleteDBInstanceAutomatedBackupType request);

  CheckedListenableFuture<DeleteDBParameterGroupResponseType> deleteDBParameterGroupAsync(final DeleteDBParameterGroupType request);

  CheckedListenableFuture<DeleteDBProxyResponseType> deleteDBProxyAsync(final DeleteDBProxyType request);

  CheckedListenableFuture<DeleteDBSecurityGroupResponseType> deleteDBSecurityGroupAsync(final DeleteDBSecurityGroupType request);

  CheckedListenableFuture<DeleteDBSnapshotResponseType> deleteDBSnapshotAsync(final DeleteDBSnapshotType request);

  CheckedListenableFuture<DeleteDBSubnetGroupResponseType> deleteDBSubnetGroupAsync(final DeleteDBSubnetGroupType request);

  CheckedListenableFuture<DeleteEventSubscriptionResponseType> deleteEventSubscriptionAsync(final DeleteEventSubscriptionType request);

  CheckedListenableFuture<DeleteGlobalClusterResponseType> deleteGlobalClusterAsync(final DeleteGlobalClusterType request);

  CheckedListenableFuture<DeleteInstallationMediaResponseType> deleteInstallationMediaAsync(final DeleteInstallationMediaType request);

  CheckedListenableFuture<DeleteOptionGroupResponseType> deleteOptionGroupAsync(final DeleteOptionGroupType request);

  CheckedListenableFuture<DeregisterDBProxyTargetsResponseType> deregisterDBProxyTargetsAsync(final DeregisterDBProxyTargetsType request);

  CheckedListenableFuture<DescribeAccountAttributesResponseType> describeAccountAttributesAsync(final DescribeAccountAttributesType request);

  default CheckedListenableFuture<DescribeAccountAttributesResponseType> describeAccountAttributesAsync() {
    return describeAccountAttributesAsync(new DescribeAccountAttributesType());
  }

  CheckedListenableFuture<DescribeCertificatesResponseType> describeCertificatesAsync(final DescribeCertificatesType request);

  default CheckedListenableFuture<DescribeCertificatesResponseType> describeCertificatesAsync() {
    return describeCertificatesAsync(new DescribeCertificatesType());
  }

  CheckedListenableFuture<DescribeCustomAvailabilityZonesResponseType> describeCustomAvailabilityZonesAsync(final DescribeCustomAvailabilityZonesType request);

  default CheckedListenableFuture<DescribeCustomAvailabilityZonesResponseType> describeCustomAvailabilityZonesAsync() {
    return describeCustomAvailabilityZonesAsync(new DescribeCustomAvailabilityZonesType());
  }

  CheckedListenableFuture<DescribeDBClusterBacktracksResponseType> describeDBClusterBacktracksAsync(final DescribeDBClusterBacktracksType request);

  CheckedListenableFuture<DescribeDBClusterEndpointsResponseType> describeDBClusterEndpointsAsync(final DescribeDBClusterEndpointsType request);

  default CheckedListenableFuture<DescribeDBClusterEndpointsResponseType> describeDBClusterEndpointsAsync() {
    return describeDBClusterEndpointsAsync(new DescribeDBClusterEndpointsType());
  }

  CheckedListenableFuture<DescribeDBClusterParameterGroupsResponseType> describeDBClusterParameterGroupsAsync(final DescribeDBClusterParameterGroupsType request);

  default CheckedListenableFuture<DescribeDBClusterParameterGroupsResponseType> describeDBClusterParameterGroupsAsync() {
    return describeDBClusterParameterGroupsAsync(new DescribeDBClusterParameterGroupsType());
  }

  CheckedListenableFuture<DescribeDBClusterParametersResponseType> describeDBClusterParametersAsync(final DescribeDBClusterParametersType request);

  CheckedListenableFuture<DescribeDBClusterSnapshotAttributesResponseType> describeDBClusterSnapshotAttributesAsync(final DescribeDBClusterSnapshotAttributesType request);

  CheckedListenableFuture<DescribeDBClusterSnapshotsResponseType> describeDBClusterSnapshotsAsync(final DescribeDBClusterSnapshotsType request);

  default CheckedListenableFuture<DescribeDBClusterSnapshotsResponseType> describeDBClusterSnapshotsAsync() {
    return describeDBClusterSnapshotsAsync(new DescribeDBClusterSnapshotsType());
  }

  CheckedListenableFuture<DescribeDBClustersResponseType> describeDBClustersAsync(final DescribeDBClustersType request);

  default CheckedListenableFuture<DescribeDBClustersResponseType> describeDBClustersAsync() {
    return describeDBClustersAsync(new DescribeDBClustersType());
  }

  CheckedListenableFuture<DescribeDBEngineVersionsResponseType> describeDBEngineVersionsAsync(final DescribeDBEngineVersionsType request);

  default CheckedListenableFuture<DescribeDBEngineVersionsResponseType> describeDBEngineVersionsAsync() {
    return describeDBEngineVersionsAsync(new DescribeDBEngineVersionsType());
  }

  CheckedListenableFuture<DescribeDBInstanceAutomatedBackupsResponseType> describeDBInstanceAutomatedBackupsAsync(final DescribeDBInstanceAutomatedBackupsType request);

  default CheckedListenableFuture<DescribeDBInstanceAutomatedBackupsResponseType> describeDBInstanceAutomatedBackupsAsync() {
    return describeDBInstanceAutomatedBackupsAsync(new DescribeDBInstanceAutomatedBackupsType());
  }

  CheckedListenableFuture<DescribeDBInstancesResponseType> describeDBInstancesAsync(final DescribeDBInstancesType request);

  default CheckedListenableFuture<DescribeDBInstancesResponseType> describeDBInstancesAsync() {
    return describeDBInstancesAsync(new DescribeDBInstancesType());
  }

  CheckedListenableFuture<DescribeDBLogFilesResponseType> describeDBLogFilesAsync(final DescribeDBLogFilesType request);

  CheckedListenableFuture<DescribeDBParameterGroupsResponseType> describeDBParameterGroupsAsync(final DescribeDBParameterGroupsType request);

  default CheckedListenableFuture<DescribeDBParameterGroupsResponseType> describeDBParameterGroupsAsync() {
    return describeDBParameterGroupsAsync(new DescribeDBParameterGroupsType());
  }

  CheckedListenableFuture<DescribeDBParametersResponseType> describeDBParametersAsync(final DescribeDBParametersType request);

  CheckedListenableFuture<DescribeDBProxiesResponseType> describeDBProxiesAsync(final DescribeDBProxiesType request);

  default CheckedListenableFuture<DescribeDBProxiesResponseType> describeDBProxiesAsync() {
    return describeDBProxiesAsync(new DescribeDBProxiesType());
  }

  CheckedListenableFuture<DescribeDBProxyTargetGroupsResponseType> describeDBProxyTargetGroupsAsync(final DescribeDBProxyTargetGroupsType request);

  CheckedListenableFuture<DescribeDBProxyTargetsResponseType> describeDBProxyTargetsAsync(final DescribeDBProxyTargetsType request);

  CheckedListenableFuture<DescribeDBSecurityGroupsResponseType> describeDBSecurityGroupsAsync(final DescribeDBSecurityGroupsType request);

  default CheckedListenableFuture<DescribeDBSecurityGroupsResponseType> describeDBSecurityGroupsAsync() {
    return describeDBSecurityGroupsAsync(new DescribeDBSecurityGroupsType());
  }

  CheckedListenableFuture<DescribeDBSnapshotAttributesResponseType> describeDBSnapshotAttributesAsync(final DescribeDBSnapshotAttributesType request);

  CheckedListenableFuture<DescribeDBSnapshotsResponseType> describeDBSnapshotsAsync(final DescribeDBSnapshotsType request);

  default CheckedListenableFuture<DescribeDBSnapshotsResponseType> describeDBSnapshotsAsync() {
    return describeDBSnapshotsAsync(new DescribeDBSnapshotsType());
  }

  CheckedListenableFuture<DescribeDBSubnetGroupsResponseType> describeDBSubnetGroupsAsync(final DescribeDBSubnetGroupsType request);

  default CheckedListenableFuture<DescribeDBSubnetGroupsResponseType> describeDBSubnetGroupsAsync() {
    return describeDBSubnetGroupsAsync(new DescribeDBSubnetGroupsType());
  }

  CheckedListenableFuture<DescribeEngineDefaultClusterParametersResponseType> describeEngineDefaultClusterParametersAsync(final DescribeEngineDefaultClusterParametersType request);

  CheckedListenableFuture<DescribeEngineDefaultParametersResponseType> describeEngineDefaultParametersAsync(final DescribeEngineDefaultParametersType request);

  CheckedListenableFuture<DescribeEventCategoriesResponseType> describeEventCategoriesAsync(final DescribeEventCategoriesType request);

  default CheckedListenableFuture<DescribeEventCategoriesResponseType> describeEventCategoriesAsync() {
    return describeEventCategoriesAsync(new DescribeEventCategoriesType());
  }

  CheckedListenableFuture<DescribeEventSubscriptionsResponseType> describeEventSubscriptionsAsync(final DescribeEventSubscriptionsType request);

  default CheckedListenableFuture<DescribeEventSubscriptionsResponseType> describeEventSubscriptionsAsync() {
    return describeEventSubscriptionsAsync(new DescribeEventSubscriptionsType());
  }

  CheckedListenableFuture<DescribeEventsResponseType> describeEventsAsync(final DescribeEventsType request);

  default CheckedListenableFuture<DescribeEventsResponseType> describeEventsAsync() {
    return describeEventsAsync(new DescribeEventsType());
  }

  CheckedListenableFuture<DescribeExportTasksResponseType> describeExportTasksAsync(final DescribeExportTasksType request);

  default CheckedListenableFuture<DescribeExportTasksResponseType> describeExportTasksAsync() {
    return describeExportTasksAsync(new DescribeExportTasksType());
  }

  CheckedListenableFuture<DescribeGlobalClustersResponseType> describeGlobalClustersAsync(final DescribeGlobalClustersType request);

  default CheckedListenableFuture<DescribeGlobalClustersResponseType> describeGlobalClustersAsync() {
    return describeGlobalClustersAsync(new DescribeGlobalClustersType());
  }

  CheckedListenableFuture<DescribeInstallationMediaResponseType> describeInstallationMediaAsync(final DescribeInstallationMediaType request);

  default CheckedListenableFuture<DescribeInstallationMediaResponseType> describeInstallationMediaAsync() {
    return describeInstallationMediaAsync(new DescribeInstallationMediaType());
  }

  CheckedListenableFuture<DescribeOptionGroupOptionsResponseType> describeOptionGroupOptionsAsync(final DescribeOptionGroupOptionsType request);

  CheckedListenableFuture<DescribeOptionGroupsResponseType> describeOptionGroupsAsync(final DescribeOptionGroupsType request);

  default CheckedListenableFuture<DescribeOptionGroupsResponseType> describeOptionGroupsAsync() {
    return describeOptionGroupsAsync(new DescribeOptionGroupsType());
  }

  CheckedListenableFuture<DescribeOrderableDBInstanceOptionsResponseType> describeOrderableDBInstanceOptionsAsync(final DescribeOrderableDBInstanceOptionsType request);

  CheckedListenableFuture<DescribePendingMaintenanceActionsResponseType> describePendingMaintenanceActionsAsync(final DescribePendingMaintenanceActionsType request);

  default CheckedListenableFuture<DescribePendingMaintenanceActionsResponseType> describePendingMaintenanceActionsAsync() {
    return describePendingMaintenanceActionsAsync(new DescribePendingMaintenanceActionsType());
  }

  CheckedListenableFuture<DescribeReservedDBInstancesResponseType> describeReservedDBInstancesAsync(final DescribeReservedDBInstancesType request);

  default CheckedListenableFuture<DescribeReservedDBInstancesResponseType> describeReservedDBInstancesAsync() {
    return describeReservedDBInstancesAsync(new DescribeReservedDBInstancesType());
  }

  CheckedListenableFuture<DescribeReservedDBInstancesOfferingsResponseType> describeReservedDBInstancesOfferingsAsync(final DescribeReservedDBInstancesOfferingsType request);

  default CheckedListenableFuture<DescribeReservedDBInstancesOfferingsResponseType> describeReservedDBInstancesOfferingsAsync() {
    return describeReservedDBInstancesOfferingsAsync(new DescribeReservedDBInstancesOfferingsType());
  }

  CheckedListenableFuture<DescribeSourceRegionsResponseType> describeSourceRegionsAsync(final DescribeSourceRegionsType request);

  default CheckedListenableFuture<DescribeSourceRegionsResponseType> describeSourceRegionsAsync() {
    return describeSourceRegionsAsync(new DescribeSourceRegionsType());
  }

  CheckedListenableFuture<DescribeValidDBInstanceModificationsResponseType> describeValidDBInstanceModificationsAsync(final DescribeValidDBInstanceModificationsType request);

  CheckedListenableFuture<DownloadDBLogFilePortionResponseType> downloadDBLogFilePortionAsync(final DownloadDBLogFilePortionType request);

  CheckedListenableFuture<FailoverDBClusterResponseType> failoverDBClusterAsync(final FailoverDBClusterType request);

  CheckedListenableFuture<ImportInstallationMediaResponseType> importInstallationMediaAsync(final ImportInstallationMediaType request);

  CheckedListenableFuture<ListTagsForResourceResponseType> listTagsForResourceAsync(final ListTagsForResourceType request);

  CheckedListenableFuture<ModifyCertificatesResponseType> modifyCertificatesAsync(final ModifyCertificatesType request);

  default CheckedListenableFuture<ModifyCertificatesResponseType> modifyCertificatesAsync() {
    return modifyCertificatesAsync(new ModifyCertificatesType());
  }

  CheckedListenableFuture<ModifyCurrentDBClusterCapacityResponseType> modifyCurrentDBClusterCapacityAsync(final ModifyCurrentDBClusterCapacityType request);

  CheckedListenableFuture<ModifyDBClusterResponseType> modifyDBClusterAsync(final ModifyDBClusterType request);

  CheckedListenableFuture<ModifyDBClusterEndpointResponseType> modifyDBClusterEndpointAsync(final ModifyDBClusterEndpointType request);

  CheckedListenableFuture<ModifyDBClusterParameterGroupResponseType> modifyDBClusterParameterGroupAsync(final ModifyDBClusterParameterGroupType request);

  CheckedListenableFuture<ModifyDBClusterSnapshotAttributeResponseType> modifyDBClusterSnapshotAttributeAsync(final ModifyDBClusterSnapshotAttributeType request);

  CheckedListenableFuture<ModifyDBInstanceResponseType> modifyDBInstanceAsync(final ModifyDBInstanceType request);

  CheckedListenableFuture<ModifyDBParameterGroupResponseType> modifyDBParameterGroupAsync(final ModifyDBParameterGroupType request);

  CheckedListenableFuture<ModifyDBProxyResponseType> modifyDBProxyAsync(final ModifyDBProxyType request);

  CheckedListenableFuture<ModifyDBProxyTargetGroupResponseType> modifyDBProxyTargetGroupAsync(final ModifyDBProxyTargetGroupType request);

  CheckedListenableFuture<ModifyDBSnapshotResponseType> modifyDBSnapshotAsync(final ModifyDBSnapshotType request);

  CheckedListenableFuture<ModifyDBSnapshotAttributeResponseType> modifyDBSnapshotAttributeAsync(final ModifyDBSnapshotAttributeType request);

  CheckedListenableFuture<ModifyDBSubnetGroupResponseType> modifyDBSubnetGroupAsync(final ModifyDBSubnetGroupType request);

  CheckedListenableFuture<ModifyEventSubscriptionResponseType> modifyEventSubscriptionAsync(final ModifyEventSubscriptionType request);

  CheckedListenableFuture<ModifyGlobalClusterResponseType> modifyGlobalClusterAsync(final ModifyGlobalClusterType request);

  default CheckedListenableFuture<ModifyGlobalClusterResponseType> modifyGlobalClusterAsync() {
    return modifyGlobalClusterAsync(new ModifyGlobalClusterType());
  }

  CheckedListenableFuture<ModifyOptionGroupResponseType> modifyOptionGroupAsync(final ModifyOptionGroupType request);

  CheckedListenableFuture<PromoteReadReplicaResponseType> promoteReadReplicaAsync(final PromoteReadReplicaType request);

  CheckedListenableFuture<PromoteReadReplicaDBClusterResponseType> promoteReadReplicaDBClusterAsync(final PromoteReadReplicaDBClusterType request);

  CheckedListenableFuture<PurchaseReservedDBInstancesOfferingResponseType> purchaseReservedDBInstancesOfferingAsync(final PurchaseReservedDBInstancesOfferingType request);

  CheckedListenableFuture<RebootDBInstanceResponseType> rebootDBInstanceAsync(final RebootDBInstanceType request);

  CheckedListenableFuture<RegisterDBProxyTargetsResponseType> registerDBProxyTargetsAsync(final RegisterDBProxyTargetsType request);

  CheckedListenableFuture<RemoveFromGlobalClusterResponseType> removeFromGlobalClusterAsync(final RemoveFromGlobalClusterType request);

  default CheckedListenableFuture<RemoveFromGlobalClusterResponseType> removeFromGlobalClusterAsync() {
    return removeFromGlobalClusterAsync(new RemoveFromGlobalClusterType());
  }

  CheckedListenableFuture<RemoveRoleFromDBClusterResponseType> removeRoleFromDBClusterAsync(final RemoveRoleFromDBClusterType request);

  CheckedListenableFuture<RemoveRoleFromDBInstanceResponseType> removeRoleFromDBInstanceAsync(final RemoveRoleFromDBInstanceType request);

  CheckedListenableFuture<RemoveSourceIdentifierFromSubscriptionResponseType> removeSourceIdentifierFromSubscriptionAsync(final RemoveSourceIdentifierFromSubscriptionType request);

  CheckedListenableFuture<RemoveTagsFromResourceResponseType> removeTagsFromResourceAsync(final RemoveTagsFromResourceType request);

  CheckedListenableFuture<ResetDBClusterParameterGroupResponseType> resetDBClusterParameterGroupAsync(final ResetDBClusterParameterGroupType request);

  CheckedListenableFuture<ResetDBParameterGroupResponseType> resetDBParameterGroupAsync(final ResetDBParameterGroupType request);

  CheckedListenableFuture<RestoreDBClusterFromS3ResponseType> restoreDBClusterFromS3Async(final RestoreDBClusterFromS3Type request);

  CheckedListenableFuture<RestoreDBClusterFromSnapshotResponseType> restoreDBClusterFromSnapshotAsync(final RestoreDBClusterFromSnapshotType request);

  CheckedListenableFuture<RestoreDBClusterToPointInTimeResponseType> restoreDBClusterToPointInTimeAsync(final RestoreDBClusterToPointInTimeType request);

  CheckedListenableFuture<RestoreDBInstanceFromDBSnapshotResponseType> restoreDBInstanceFromDBSnapshotAsync(final RestoreDBInstanceFromDBSnapshotType request);

  CheckedListenableFuture<RestoreDBInstanceFromS3ResponseType> restoreDBInstanceFromS3Async(final RestoreDBInstanceFromS3Type request);

  CheckedListenableFuture<RestoreDBInstanceToPointInTimeResponseType> restoreDBInstanceToPointInTimeAsync(final RestoreDBInstanceToPointInTimeType request);

  CheckedListenableFuture<RevokeDBSecurityGroupIngressResponseType> revokeDBSecurityGroupIngressAsync(final RevokeDBSecurityGroupIngressType request);

  CheckedListenableFuture<StartActivityStreamResponseType> startActivityStreamAsync(final StartActivityStreamType request);

  CheckedListenableFuture<StartDBClusterResponseType> startDBClusterAsync(final StartDBClusterType request);

  CheckedListenableFuture<StartDBInstanceResponseType> startDBInstanceAsync(final StartDBInstanceType request);

  CheckedListenableFuture<StartExportTaskResponseType> startExportTaskAsync(final StartExportTaskType request);

  CheckedListenableFuture<StopActivityStreamResponseType> stopActivityStreamAsync(final StopActivityStreamType request);

  CheckedListenableFuture<StopDBClusterResponseType> stopDBClusterAsync(final StopDBClusterType request);

  CheckedListenableFuture<StopDBInstanceResponseType> stopDBInstanceAsync(final StopDBInstanceType request);

}
