/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.rds.common.msgs.*;


@ComponentPart(Rds.class)
public interface RdsApi {

  AddRoleToDBClusterResponseType addRoleToDBCluster(final AddRoleToDBClusterType request);

  AddRoleToDBInstanceResponseType addRoleToDBInstance(final AddRoleToDBInstanceType request);

  AddSourceIdentifierToSubscriptionResponseType addSourceIdentifierToSubscription(final AddSourceIdentifierToSubscriptionType request);

  AddTagsToResourceResponseType addTagsToResource(final AddTagsToResourceType request);

  ApplyPendingMaintenanceActionResponseType applyPendingMaintenanceAction(final ApplyPendingMaintenanceActionType request);

  AuthorizeDBSecurityGroupIngressResponseType authorizeDBSecurityGroupIngress(final AuthorizeDBSecurityGroupIngressType request);

  BacktrackDBClusterResponseType backtrackDBCluster(final BacktrackDBClusterType request);

  CancelExportTaskResponseType cancelExportTask(final CancelExportTaskType request);

  CopyDBClusterParameterGroupResponseType copyDBClusterParameterGroup(final CopyDBClusterParameterGroupType request);

  CopyDBClusterSnapshotResponseType copyDBClusterSnapshot(final CopyDBClusterSnapshotType request);

  CopyDBParameterGroupResponseType copyDBParameterGroup(final CopyDBParameterGroupType request);

  CopyDBSnapshotResponseType copyDBSnapshot(final CopyDBSnapshotType request);

  CopyOptionGroupResponseType copyOptionGroup(final CopyOptionGroupType request);

  CreateCustomAvailabilityZoneResponseType createCustomAvailabilityZone(final CreateCustomAvailabilityZoneType request);

  CreateDBClusterResponseType createDBCluster(final CreateDBClusterType request);

  CreateDBClusterEndpointResponseType createDBClusterEndpoint(final CreateDBClusterEndpointType request);

  CreateDBClusterParameterGroupResponseType createDBClusterParameterGroup(final CreateDBClusterParameterGroupType request);

  CreateDBClusterSnapshotResponseType createDBClusterSnapshot(final CreateDBClusterSnapshotType request);

  CreateDBInstanceResponseType createDBInstance(final CreateDBInstanceType request);

  CreateDBInstanceReadReplicaResponseType createDBInstanceReadReplica(final CreateDBInstanceReadReplicaType request);

  CreateDBParameterGroupResponseType createDBParameterGroup(final CreateDBParameterGroupType request);

  CreateDBProxyResponseType createDBProxy(final CreateDBProxyType request);

  CreateDBSecurityGroupResponseType createDBSecurityGroup(final CreateDBSecurityGroupType request);

  CreateDBSnapshotResponseType createDBSnapshot(final CreateDBSnapshotType request);

  CreateDBSubnetGroupResponseType createDBSubnetGroup(final CreateDBSubnetGroupType request);

  CreateEventSubscriptionResponseType createEventSubscription(final CreateEventSubscriptionType request);

  CreateGlobalClusterResponseType createGlobalCluster(final CreateGlobalClusterType request);

  default CreateGlobalClusterResponseType createGlobalCluster() {
    return createGlobalCluster(new CreateGlobalClusterType());
  }

  CreateOptionGroupResponseType createOptionGroup(final CreateOptionGroupType request);

  DeleteCustomAvailabilityZoneResponseType deleteCustomAvailabilityZone(final DeleteCustomAvailabilityZoneType request);

  DeleteDBClusterResponseType deleteDBCluster(final DeleteDBClusterType request);

  DeleteDBClusterEndpointResponseType deleteDBClusterEndpoint(final DeleteDBClusterEndpointType request);

  DeleteDBClusterParameterGroupResponseType deleteDBClusterParameterGroup(final DeleteDBClusterParameterGroupType request);

  DeleteDBClusterSnapshotResponseType deleteDBClusterSnapshot(final DeleteDBClusterSnapshotType request);

  DeleteDBInstanceResponseType deleteDBInstance(final DeleteDBInstanceType request);

  DeleteDBInstanceAutomatedBackupResponseType deleteDBInstanceAutomatedBackup(final DeleteDBInstanceAutomatedBackupType request);

  DeleteDBParameterGroupResponseType deleteDBParameterGroup(final DeleteDBParameterGroupType request);

  DeleteDBProxyResponseType deleteDBProxy(final DeleteDBProxyType request);

  DeleteDBSecurityGroupResponseType deleteDBSecurityGroup(final DeleteDBSecurityGroupType request);

  DeleteDBSnapshotResponseType deleteDBSnapshot(final DeleteDBSnapshotType request);

  DeleteDBSubnetGroupResponseType deleteDBSubnetGroup(final DeleteDBSubnetGroupType request);

  DeleteEventSubscriptionResponseType deleteEventSubscription(final DeleteEventSubscriptionType request);

  DeleteGlobalClusterResponseType deleteGlobalCluster(final DeleteGlobalClusterType request);

  DeleteInstallationMediaResponseType deleteInstallationMedia(final DeleteInstallationMediaType request);

  DeleteOptionGroupResponseType deleteOptionGroup(final DeleteOptionGroupType request);

  DeregisterDBProxyTargetsResponseType deregisterDBProxyTargets(final DeregisterDBProxyTargetsType request);

  DescribeAccountAttributesResponseType describeAccountAttributes(final DescribeAccountAttributesType request);

  default DescribeAccountAttributesResponseType describeAccountAttributes() {
    return describeAccountAttributes(new DescribeAccountAttributesType());
  }

  DescribeCertificatesResponseType describeCertificates(final DescribeCertificatesType request);

  default DescribeCertificatesResponseType describeCertificates() {
    return describeCertificates(new DescribeCertificatesType());
  }

  DescribeCustomAvailabilityZonesResponseType describeCustomAvailabilityZones(final DescribeCustomAvailabilityZonesType request);

  default DescribeCustomAvailabilityZonesResponseType describeCustomAvailabilityZones() {
    return describeCustomAvailabilityZones(new DescribeCustomAvailabilityZonesType());
  }

  DescribeDBClusterBacktracksResponseType describeDBClusterBacktracks(final DescribeDBClusterBacktracksType request);

  DescribeDBClusterEndpointsResponseType describeDBClusterEndpoints(final DescribeDBClusterEndpointsType request);

  default DescribeDBClusterEndpointsResponseType describeDBClusterEndpoints() {
    return describeDBClusterEndpoints(new DescribeDBClusterEndpointsType());
  }

  DescribeDBClusterParameterGroupsResponseType describeDBClusterParameterGroups(final DescribeDBClusterParameterGroupsType request);

  default DescribeDBClusterParameterGroupsResponseType describeDBClusterParameterGroups() {
    return describeDBClusterParameterGroups(new DescribeDBClusterParameterGroupsType());
  }

  DescribeDBClusterParametersResponseType describeDBClusterParameters(final DescribeDBClusterParametersType request);

  DescribeDBClusterSnapshotAttributesResponseType describeDBClusterSnapshotAttributes(final DescribeDBClusterSnapshotAttributesType request);

  DescribeDBClusterSnapshotsResponseType describeDBClusterSnapshots(final DescribeDBClusterSnapshotsType request);

  default DescribeDBClusterSnapshotsResponseType describeDBClusterSnapshots() {
    return describeDBClusterSnapshots(new DescribeDBClusterSnapshotsType());
  }

  DescribeDBClustersResponseType describeDBClusters(final DescribeDBClustersType request);

  default DescribeDBClustersResponseType describeDBClusters() {
    return describeDBClusters(new DescribeDBClustersType());
  }

  DescribeDBEngineVersionsResponseType describeDBEngineVersions(final DescribeDBEngineVersionsType request);

  default DescribeDBEngineVersionsResponseType describeDBEngineVersions() {
    return describeDBEngineVersions(new DescribeDBEngineVersionsType());
  }

  DescribeDBInstanceAutomatedBackupsResponseType describeDBInstanceAutomatedBackups(final DescribeDBInstanceAutomatedBackupsType request);

  default DescribeDBInstanceAutomatedBackupsResponseType describeDBInstanceAutomatedBackups() {
    return describeDBInstanceAutomatedBackups(new DescribeDBInstanceAutomatedBackupsType());
  }

  DescribeDBInstancesResponseType describeDBInstances(final DescribeDBInstancesType request);

  default DescribeDBInstancesResponseType describeDBInstances() {
    return describeDBInstances(new DescribeDBInstancesType());
  }

  DescribeDBLogFilesResponseType describeDBLogFiles(final DescribeDBLogFilesType request);

  DescribeDBParameterGroupsResponseType describeDBParameterGroups(final DescribeDBParameterGroupsType request);

  default DescribeDBParameterGroupsResponseType describeDBParameterGroups() {
    return describeDBParameterGroups(new DescribeDBParameterGroupsType());
  }

  DescribeDBParametersResponseType describeDBParameters(final DescribeDBParametersType request);

  DescribeDBProxiesResponseType describeDBProxies(final DescribeDBProxiesType request);

  default DescribeDBProxiesResponseType describeDBProxies() {
    return describeDBProxies(new DescribeDBProxiesType());
  }

  DescribeDBProxyTargetGroupsResponseType describeDBProxyTargetGroups(final DescribeDBProxyTargetGroupsType request);

  DescribeDBProxyTargetsResponseType describeDBProxyTargets(final DescribeDBProxyTargetsType request);

  DescribeDBSecurityGroupsResponseType describeDBSecurityGroups(final DescribeDBSecurityGroupsType request);

  default DescribeDBSecurityGroupsResponseType describeDBSecurityGroups() {
    return describeDBSecurityGroups(new DescribeDBSecurityGroupsType());
  }

  DescribeDBSnapshotAttributesResponseType describeDBSnapshotAttributes(final DescribeDBSnapshotAttributesType request);

  DescribeDBSnapshotsResponseType describeDBSnapshots(final DescribeDBSnapshotsType request);

  default DescribeDBSnapshotsResponseType describeDBSnapshots() {
    return describeDBSnapshots(new DescribeDBSnapshotsType());
  }

  DescribeDBSubnetGroupsResponseType describeDBSubnetGroups(final DescribeDBSubnetGroupsType request);

  default DescribeDBSubnetGroupsResponseType describeDBSubnetGroups() {
    return describeDBSubnetGroups(new DescribeDBSubnetGroupsType());
  }

  DescribeEngineDefaultClusterParametersResponseType describeEngineDefaultClusterParameters(final DescribeEngineDefaultClusterParametersType request);

  DescribeEngineDefaultParametersResponseType describeEngineDefaultParameters(final DescribeEngineDefaultParametersType request);

  DescribeEventCategoriesResponseType describeEventCategories(final DescribeEventCategoriesType request);

  default DescribeEventCategoriesResponseType describeEventCategories() {
    return describeEventCategories(new DescribeEventCategoriesType());
  }

  DescribeEventSubscriptionsResponseType describeEventSubscriptions(final DescribeEventSubscriptionsType request);

  default DescribeEventSubscriptionsResponseType describeEventSubscriptions() {
    return describeEventSubscriptions(new DescribeEventSubscriptionsType());
  }

  DescribeEventsResponseType describeEvents(final DescribeEventsType request);

  default DescribeEventsResponseType describeEvents() {
    return describeEvents(new DescribeEventsType());
  }

  DescribeExportTasksResponseType describeExportTasks(final DescribeExportTasksType request);

  default DescribeExportTasksResponseType describeExportTasks() {
    return describeExportTasks(new DescribeExportTasksType());
  }

  DescribeGlobalClustersResponseType describeGlobalClusters(final DescribeGlobalClustersType request);

  default DescribeGlobalClustersResponseType describeGlobalClusters() {
    return describeGlobalClusters(new DescribeGlobalClustersType());
  }

  DescribeInstallationMediaResponseType describeInstallationMedia(final DescribeInstallationMediaType request);

  default DescribeInstallationMediaResponseType describeInstallationMedia() {
    return describeInstallationMedia(new DescribeInstallationMediaType());
  }

  DescribeOptionGroupOptionsResponseType describeOptionGroupOptions(final DescribeOptionGroupOptionsType request);

  DescribeOptionGroupsResponseType describeOptionGroups(final DescribeOptionGroupsType request);

  default DescribeOptionGroupsResponseType describeOptionGroups() {
    return describeOptionGroups(new DescribeOptionGroupsType());
  }

  DescribeOrderableDBInstanceOptionsResponseType describeOrderableDBInstanceOptions(final DescribeOrderableDBInstanceOptionsType request);

  DescribePendingMaintenanceActionsResponseType describePendingMaintenanceActions(final DescribePendingMaintenanceActionsType request);

  default DescribePendingMaintenanceActionsResponseType describePendingMaintenanceActions() {
    return describePendingMaintenanceActions(new DescribePendingMaintenanceActionsType());
  }

  DescribeReservedDBInstancesResponseType describeReservedDBInstances(final DescribeReservedDBInstancesType request);

  default DescribeReservedDBInstancesResponseType describeReservedDBInstances() {
    return describeReservedDBInstances(new DescribeReservedDBInstancesType());
  }

  DescribeReservedDBInstancesOfferingsResponseType describeReservedDBInstancesOfferings(final DescribeReservedDBInstancesOfferingsType request);

  default DescribeReservedDBInstancesOfferingsResponseType describeReservedDBInstancesOfferings() {
    return describeReservedDBInstancesOfferings(new DescribeReservedDBInstancesOfferingsType());
  }

  DescribeSourceRegionsResponseType describeSourceRegions(final DescribeSourceRegionsType request);

  default DescribeSourceRegionsResponseType describeSourceRegions() {
    return describeSourceRegions(new DescribeSourceRegionsType());
  }

  DescribeValidDBInstanceModificationsResponseType describeValidDBInstanceModifications(final DescribeValidDBInstanceModificationsType request);

  DownloadDBLogFilePortionResponseType downloadDBLogFilePortion(final DownloadDBLogFilePortionType request);

  FailoverDBClusterResponseType failoverDBCluster(final FailoverDBClusterType request);

  ImportInstallationMediaResponseType importInstallationMedia(final ImportInstallationMediaType request);

  ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request);

  ModifyCertificatesResponseType modifyCertificates(final ModifyCertificatesType request);

  default ModifyCertificatesResponseType modifyCertificates() {
    return modifyCertificates(new ModifyCertificatesType());
  }

  ModifyCurrentDBClusterCapacityResponseType modifyCurrentDBClusterCapacity(final ModifyCurrentDBClusterCapacityType request);

  ModifyDBClusterResponseType modifyDBCluster(final ModifyDBClusterType request);

  ModifyDBClusterEndpointResponseType modifyDBClusterEndpoint(final ModifyDBClusterEndpointType request);

  ModifyDBClusterParameterGroupResponseType modifyDBClusterParameterGroup(final ModifyDBClusterParameterGroupType request);

  ModifyDBClusterSnapshotAttributeResponseType modifyDBClusterSnapshotAttribute(final ModifyDBClusterSnapshotAttributeType request);

  ModifyDBInstanceResponseType modifyDBInstance(final ModifyDBInstanceType request);

  ModifyDBParameterGroupResponseType modifyDBParameterGroup(final ModifyDBParameterGroupType request);

  ModifyDBProxyResponseType modifyDBProxy(final ModifyDBProxyType request);

  ModifyDBProxyTargetGroupResponseType modifyDBProxyTargetGroup(final ModifyDBProxyTargetGroupType request);

  ModifyDBSnapshotResponseType modifyDBSnapshot(final ModifyDBSnapshotType request);

  ModifyDBSnapshotAttributeResponseType modifyDBSnapshotAttribute(final ModifyDBSnapshotAttributeType request);

  ModifyDBSubnetGroupResponseType modifyDBSubnetGroup(final ModifyDBSubnetGroupType request);

  ModifyEventSubscriptionResponseType modifyEventSubscription(final ModifyEventSubscriptionType request);

  ModifyGlobalClusterResponseType modifyGlobalCluster(final ModifyGlobalClusterType request);

  default ModifyGlobalClusterResponseType modifyGlobalCluster() {
    return modifyGlobalCluster(new ModifyGlobalClusterType());
  }

  ModifyOptionGroupResponseType modifyOptionGroup(final ModifyOptionGroupType request);

  PromoteReadReplicaResponseType promoteReadReplica(final PromoteReadReplicaType request);

  PromoteReadReplicaDBClusterResponseType promoteReadReplicaDBCluster(final PromoteReadReplicaDBClusterType request);

  PurchaseReservedDBInstancesOfferingResponseType purchaseReservedDBInstancesOffering(final PurchaseReservedDBInstancesOfferingType request);

  RebootDBInstanceResponseType rebootDBInstance(final RebootDBInstanceType request);

  RegisterDBProxyTargetsResponseType registerDBProxyTargets(final RegisterDBProxyTargetsType request);

  RemoveFromGlobalClusterResponseType removeFromGlobalCluster(final RemoveFromGlobalClusterType request);

  default RemoveFromGlobalClusterResponseType removeFromGlobalCluster() {
    return removeFromGlobalCluster(new RemoveFromGlobalClusterType());
  }

  RemoveRoleFromDBClusterResponseType removeRoleFromDBCluster(final RemoveRoleFromDBClusterType request);

  RemoveRoleFromDBInstanceResponseType removeRoleFromDBInstance(final RemoveRoleFromDBInstanceType request);

  RemoveSourceIdentifierFromSubscriptionResponseType removeSourceIdentifierFromSubscription(final RemoveSourceIdentifierFromSubscriptionType request);

  RemoveTagsFromResourceResponseType removeTagsFromResource(final RemoveTagsFromResourceType request);

  ResetDBClusterParameterGroupResponseType resetDBClusterParameterGroup(final ResetDBClusterParameterGroupType request);

  ResetDBParameterGroupResponseType resetDBParameterGroup(final ResetDBParameterGroupType request);

  RestoreDBClusterFromS3ResponseType restoreDBClusterFromS3(final RestoreDBClusterFromS3Type request);

  RestoreDBClusterFromSnapshotResponseType restoreDBClusterFromSnapshot(final RestoreDBClusterFromSnapshotType request);

  RestoreDBClusterToPointInTimeResponseType restoreDBClusterToPointInTime(final RestoreDBClusterToPointInTimeType request);

  RestoreDBInstanceFromDBSnapshotResponseType restoreDBInstanceFromDBSnapshot(final RestoreDBInstanceFromDBSnapshotType request);

  RestoreDBInstanceFromS3ResponseType restoreDBInstanceFromS3(final RestoreDBInstanceFromS3Type request);

  RestoreDBInstanceToPointInTimeResponseType restoreDBInstanceToPointInTime(final RestoreDBInstanceToPointInTimeType request);

  RevokeDBSecurityGroupIngressResponseType revokeDBSecurityGroupIngress(final RevokeDBSecurityGroupIngressType request);

  StartActivityStreamResponseType startActivityStream(final StartActivityStreamType request);

  StartDBClusterResponseType startDBCluster(final StartDBClusterType request);

  StartDBInstanceResponseType startDBInstance(final StartDBInstanceType request);

  StartExportTaskResponseType startExportTask(final StartExportTaskType request);

  StopActivityStreamResponseType stopActivityStream(final StopActivityStreamType request);

  StopDBClusterResponseType stopDBCluster(final StopDBClusterType request);

  StopDBInstanceResponseType stopDBInstance(final StopDBInstanceType request);

}
