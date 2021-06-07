/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.rds.common.RdsMetadatas;
import com.eucalyptus.rds.common.msgs.*;
import com.eucalyptus.rds.service.engine.RdsEngine;
import com.eucalyptus.rds.service.persist.RdsMetadataNotFoundException;
import com.eucalyptus.rds.service.persist.entities.DBInstance.Status;
import com.eucalyptus.rds.service.persist.entities.DBSubnet;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncProxy;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
@ComponentNamed
public class RdsService {
  private static final Logger logger = Logger.getLogger( RdsService.class );

  private final com.eucalyptus.rds.service.persist.DBInstances dbInstances;
  private final com.eucalyptus.rds.service.persist.DBSubnetGroups dbSubnetGroups;

  @Inject
  public RdsService(
      final com.eucalyptus.rds.service.persist.DBInstances dbInstances,
      final com.eucalyptus.rds.service.persist.DBSubnetGroups dbSubnetGroups
  ) {
    this.dbInstances = dbInstances;
    this.dbSubnetGroups = dbSubnetGroups;
  }

  public AddRoleToDBClusterResponseType addRoleToDBCluster(final AddRoleToDBClusterType request) {
    return request.getReply();
  }

  public AddRoleToDBInstanceResponseType addRoleToDBInstance(final AddRoleToDBInstanceType request) {
    return request.getReply();
  }

  public AddSourceIdentifierToSubscriptionResponseType addSourceIdentifierToSubscription(final AddSourceIdentifierToSubscriptionType request) {
    return request.getReply();
  }

  public AddTagsToResourceResponseType addTagsToResource(final AddTagsToResourceType request) {
    return request.getReply();
  }

  public ApplyPendingMaintenanceActionResponseType applyPendingMaintenanceAction(final ApplyPendingMaintenanceActionType request) {
    return request.getReply();
  }

  public AuthorizeDBSecurityGroupIngressResponseType authorizeDBSecurityGroupIngress(final AuthorizeDBSecurityGroupIngressType request) {
    return request.getReply();
  }

  public BacktrackDBClusterResponseType backtrackDBCluster(final BacktrackDBClusterType request) {
    return request.getReply();
  }

  public CancelExportTaskResponseType cancelExportTask(final CancelExportTaskType request) {
    return request.getReply();
  }

  public CopyDBClusterParameterGroupResponseType copyDBClusterParameterGroup(final CopyDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public CopyDBClusterSnapshotResponseType copyDBClusterSnapshot(final CopyDBClusterSnapshotType request) {
    return request.getReply();
  }

  public CopyDBParameterGroupResponseType copyDBParameterGroup(final CopyDBParameterGroupType request) {
    return request.getReply();
  }

  public CopyDBSnapshotResponseType copyDBSnapshot(final CopyDBSnapshotType request) {
    return request.getReply();
  }

  public CopyOptionGroupResponseType copyOptionGroup(final CopyOptionGroupType request) {
    return request.getReply();
  }

  public CreateCustomAvailabilityZoneResponseType createCustomAvailabilityZone(final CreateCustomAvailabilityZoneType request) {
    return request.getReply();
  }

  public CreateDBClusterResponseType createDBCluster(final CreateDBClusterType request) {
    return request.getReply();
  }

  public CreateDBClusterEndpointResponseType createDBClusterEndpoint(final CreateDBClusterEndpointType request) {
    return request.getReply();
  }

  public CreateDBClusterParameterGroupResponseType createDBClusterParameterGroup(final CreateDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public CreateDBClusterSnapshotResponseType createDBClusterSnapshot(final CreateDBClusterSnapshotType request) {
    return request.getReply();
  }

  public CreateDBInstanceResponseType createDBInstance(final CreateDBInstanceType request) throws RdsException {
    final CreateDBInstanceResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String instanceName = request.getDBInstanceIdentifier();
      final String instanceClass = request.getDBInstanceClass();
      final String subnetGroupName = request.getDBSubnetGroupName();
      final RdsEngine engine = RdsEngine.valueOf(request.getEngine());
      final Set<String> vpcSecurityGroups = request.getVpcSecurityGroupIds()==null ?
          Collections.emptySet() :
          Sets.newTreeSet(request.getVpcSecurityGroupIds().getMember());

      final Supplier<com.eucalyptus.rds.service.persist.entities.DBInstance> allocator =
          new Supplier<com.eucalyptus.rds.service.persist.entities.DBInstance>( ) {
            @Override
            public com.eucalyptus.rds.service.persist.entities.DBInstance get( ) {
              try {
                final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup group =
                    subnetGroupName == null ?
                        null :
                        dbSubnetGroups.lookupByName(
                            ctx.getUserFullName( ).asAccountFullName(),
                            subnetGroupName,
                            RdsMetadatas.filterPrivileged( ),
                            Functions.identity( ));

                final com.eucalyptus.rds.service.persist.entities.DBInstance instance =
                    com.eucalyptus.rds.service.persist.entities.DBInstance.create(
                        ownerFullName,
                        instanceName,
                        MoreObjects.firstNonNull( request.getAllocatedStorage(), 20 ),
                        MoreObjects.firstNonNull( request.getCopyTagsToSnapshot(), Boolean.FALSE ),
                        MoreObjects.firstNonNull( request.getDBName(), engine.getDefaultDatabaseName()),
                        MoreObjects.firstNonNull(request.getPort(), engine.getDefaultDatabasePort()),
                         instanceClass,
                        engine.name(),
                        MoreObjects.firstNonNull(request.getEngineVersion(), engine.getDefaultDatabaseVersion()),
                        MoreObjects.firstNonNull(request.getPubliclyAccessible(), Boolean.FALSE)
                    );

                instance.setAvailabilityZone(request.getAvailabilityZone());
                instance.setMasterUsername(request.getMasterUsername());
                instance.setMasterUserPassword(request.getMasterUserPassword());
                instance.setVpcSecurityGroups(Lists.newArrayList(vpcSecurityGroups));
                instance.setDbSubnetGroup(group);
                return dbInstances.save(instance);
              } catch ( Exception ex ) {
                throw new RuntimeException( ex );
              }
            }
          };

      final com.eucalyptus.rds.service.persist.entities.DBInstance instance =
          RdsMetadatas.allocateUnitlessResource( allocator );
      reply.getCreateDBInstanceResult().setDBInstance(
          TypeMappers.transform(instance, DBInstance.class));
    } catch ( final Exception e ) {
      handleException( e, __ -> new RdsClientException("DBInstanceAlreadyExists", "Instance already exists"));
    }
    return reply;
  }

  public CreateDBInstanceReadReplicaResponseType createDBInstanceReadReplica(final CreateDBInstanceReadReplicaType request) {
    return request.getReply();
  }

  public CreateDBParameterGroupResponseType createDBParameterGroup(final CreateDBParameterGroupType request) {
    return request.getReply();
  }

  public CreateDBProxyResponseType createDBProxy(final CreateDBProxyType request) {
    return request.getReply();
  }

  public CreateDBSecurityGroupResponseType createDBSecurityGroup(final CreateDBSecurityGroupType request) {
    return request.getReply();
  }

  public CreateDBSnapshotResponseType createDBSnapshot(final CreateDBSnapshotType request) {
    return request.getReply();
  }

  public CreateDBSubnetGroupResponseType createDBSubnetGroup(final CreateDBSubnetGroupType request) throws RdsException {
    final CreateDBSubnetGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String name = request.getDBSubnetGroupName();
      final String desc = request.getDBSubnetGroupDescription();
      final Collection<String> subnetIds = request.getSubnetIds().getMember();

      if (subnetIds.isEmpty()) {
        throw new RdsClientException("DBSubnetGroupDoesNotCoverEnoughAZs", "No subnets");
      }

      final Set<String> vpcIds = Sets.newHashSet();
      final Set<Tuple2<String,String>> subnetsToZones = Sets.newHashSet();
      final ComputeApi client = AsyncProxy.client( ComputeApi.class );
      for ( final SubnetType subnetType : client.describeSubnets( subnetIds ).getSubnetSet().getItem( ) ) {
        vpcIds.add( subnetType.getVpcId( ) );
        subnetsToZones.add(Tuple.of(subnetType.getSubnetId(), subnetType.getAvailabilityZone()));
      }
      if (subnetsToZones.isEmpty()) {
        throw new RdsClientException("InvalidSubnet", "Subnet(s) not found");
      }
      if (vpcIds.size()!=1) {
        throw new RdsClientException("InvalidSubnet", "Subnets vpc invalid");
      }

      final Supplier<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup> allocator =
          new Supplier<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup>( ) {
        @Override
        public com.eucalyptus.rds.service.persist.entities.DBSubnetGroup get( ) {
          try {
            final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup group =
                com.eucalyptus.rds.service.persist.entities.DBSubnetGroup.create(
                    ownerFullName,
                    name,
                    desc
                );

            final List<DBSubnet> dbSubnets = Lists.newArrayList();
            for ( final Tuple2<String,String> subnetAndZone : subnetsToZones ) {
              dbSubnets.add( DBSubnet.create( group, subnetAndZone._1(), subnetAndZone._2() ) );
            }
            group.setVpcId(vpcIds.iterator().next());
            group.setSubnets(dbSubnets);
            return dbSubnetGroups.save(group);
          } catch ( Exception ex ) {
            throw new RuntimeException( ex );
          }
        }
      };

      final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup group =
          RdsMetadatas.allocateUnitlessResource( allocator );
      reply.getCreateDBSubnetGroupResult().setDBSubnetGroup(
          TypeMappers.transform(group, DBSubnetGroup.class));
    } catch ( final Exception e ) {
      handleException( e, __ -> new RdsClientException("DBSubnetGroupAlreadyExists", "Group already exists"));
    }
    return reply;
  }

  public CreateEventSubscriptionResponseType createEventSubscription(final CreateEventSubscriptionType request) {
    return request.getReply();
  }

  public CreateGlobalClusterResponseType createGlobalCluster(final CreateGlobalClusterType request) {
    return request.getReply();
  }

  public CreateOptionGroupResponseType createOptionGroup(final CreateOptionGroupType request) {
    return request.getReply();
  }

  public DeleteCustomAvailabilityZoneResponseType deleteCustomAvailabilityZone(final DeleteCustomAvailabilityZoneType request) {
    return request.getReply();
  }

  public DeleteDBClusterResponseType deleteDBCluster(final DeleteDBClusterType request) {
    return request.getReply();
  }

  public DeleteDBClusterEndpointResponseType deleteDBClusterEndpoint(final DeleteDBClusterEndpointType request) {
    return request.getReply();
  }

  public DeleteDBClusterParameterGroupResponseType deleteDBClusterParameterGroup(final DeleteDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public DeleteDBClusterSnapshotResponseType deleteDBClusterSnapshot(final DeleteDBClusterSnapshotType request) {
    return request.getReply();
  }

  public DeleteDBInstanceResponseType deleteDBInstance(final DeleteDBInstanceType request) throws RdsException {
    final DeleteDBInstanceResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBInstanceIdentifier();

      final DBInstance instance = dbInstances.updateByExample(
          com.eucalyptus.rds.service.persist.entities.DBInstance.exampleWithName(ownerFullName, name),
          ownerFullName,
          name,
          dbInstance -> {
            if (!RdsMetadatas.filterPrivileged().apply(dbInstance)) {
              throw new NoSuchElementException();
            }
            dbInstance.setState(Status.deleting);
            return TypeMappers.transform(dbInstance, DBInstance.class);
          });
      reply.getDeleteDBInstanceResult().setDBInstance(instance);
    } catch ( final RdsMetadataNotFoundException e ) {
      throw new RdsClientException( "DBInstanceNotFound", "DB Instance Not Found" );
    } catch ( final Exception e ) {
      handleException( e, __ -> new RdsClientException("InvalidDBInstanceState", "Invalid state for delete") );
    }
    return reply;
  }

  public DeleteDBInstanceAutomatedBackupResponseType deleteDBInstanceAutomatedBackup(final DeleteDBInstanceAutomatedBackupType request) {
    return request.getReply();
  }

  public DeleteDBParameterGroupResponseType deleteDBParameterGroup(final DeleteDBParameterGroupType request) {
    return request.getReply();
  }

  public DeleteDBProxyResponseType deleteDBProxy(final DeleteDBProxyType request) {
    return request.getReply();
  }

  public DeleteDBSecurityGroupResponseType deleteDBSecurityGroup(final DeleteDBSecurityGroupType request) {
    return request.getReply();
  }

  public DeleteDBSnapshotResponseType deleteDBSnapshot(final DeleteDBSnapshotType request) {
    return request.getReply();
  }

  public DeleteDBSubnetGroupResponseType deleteDBSubnetGroup(final DeleteDBSubnetGroupType request) throws RdsException {
    final DeleteDBSubnetGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBSubnetGroupName();

      final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup group =
          dbSubnetGroups.lookupByName( ownerFullName, name, RdsMetadatas.filterPrivileged(), Functions.identity());

      dbSubnetGroups.deleteByExample( group );
    } catch ( final RdsMetadataNotFoundException e ) {
      throw new RdsClientException( "DBSubnetGroupNotFoundFault", "DB Subnet Group Not Found" );
    } catch ( final Exception e ) {
      handleException( e, __ -> new RdsClientException("InvalidDBSubnetGroupStateFault", "Group in use") );
    }
    return reply;
  }

  public DeleteEventSubscriptionResponseType deleteEventSubscription(final DeleteEventSubscriptionType request) {
    return request.getReply();
  }

  public DeleteGlobalClusterResponseType deleteGlobalCluster(final DeleteGlobalClusterType request) {
    return request.getReply();
  }

  public DeleteInstallationMediaResponseType deleteInstallationMedia(final DeleteInstallationMediaType request) {
    return request.getReply();
  }

  public DeleteOptionGroupResponseType deleteOptionGroup(final DeleteOptionGroupType request) {
    return request.getReply();
  }

  public DeregisterDBProxyTargetsResponseType deregisterDBProxyTargets(final DeregisterDBProxyTargetsType request) {
    return request.getReply();
  }

  public DescribeAccountAttributesResponseType describeAccountAttributes(final DescribeAccountAttributesType request) {
    return request.getReply();
  }

  public DescribeCertificatesResponseType describeCertificates(final DescribeCertificatesType request) {
    return request.getReply();
  }

  public DescribeCustomAvailabilityZonesResponseType describeCustomAvailabilityZones(final DescribeCustomAvailabilityZonesType request) {
    return request.getReply();
  }

  public DescribeDBClusterBacktracksResponseType describeDBClusterBacktracks(final DescribeDBClusterBacktracksType request) {
    return request.getReply();
  }

  public DescribeDBClusterEndpointsResponseType describeDBClusterEndpoints(final DescribeDBClusterEndpointsType request) {
    return request.getReply();
  }

  public DescribeDBClusterParameterGroupsResponseType describeDBClusterParameterGroups(final DescribeDBClusterParameterGroupsType request) {
    return request.getReply();
  }

  public DescribeDBClusterParametersResponseType describeDBClusterParameters(final DescribeDBClusterParametersType request) {
    return request.getReply();
  }

  public DescribeDBClusterSnapshotAttributesResponseType describeDBClusterSnapshotAttributes(final DescribeDBClusterSnapshotAttributesType request) {
    return request.getReply();
  }

  public DescribeDBClusterSnapshotsResponseType describeDBClusterSnapshots(final DescribeDBClusterSnapshotsType request) {
    return request.getReply();
  }

  public DescribeDBClustersResponseType describeDBClusters(final DescribeDBClustersType request) {
    return request.getReply();
  }

  public DescribeDBEngineVersionsResponseType describeDBEngineVersions(final DescribeDBEngineVersionsType request) throws RdsException {
    final DescribeDBEngineVersionsResponseType reply = request.getReply();
    try {
      final DBEngineVersionList versionList = new DBEngineVersionList();
      for (final RdsEngine engine : RdsEngine.values()) {
        final DBEngineVersion engineVersion = new DBEngineVersion();
        engineVersion.setEngine(engine.toString());
        engineVersion.setDBEngineDescription(engine.getDescription());
        engineVersion.setEngineVersion(engine.getDefaultDatabaseVersion());
        engineVersion.setDBEngineVersionDescription(engine.getDefaultDatabaseVersionDescription());
        versionList.getMember().add(engineVersion);
      }
      reply.getDescribeDBEngineVersionsResult().setDBEngineVersions(versionList);
    } catch ( Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public DescribeDBInstanceAutomatedBackupsResponseType describeDBInstanceAutomatedBackups(final DescribeDBInstanceAutomatedBackupsType request) {
    return request.getReply();
  }

  public DescribeDBInstancesResponseType describeDBInstances(final DescribeDBInstancesType request) throws RdsException {
    final DescribeDBInstancesResponseType reply = request.getReply();

    final Context ctx = Contexts.lookup( );
    //TODO describe db instances filters - request.getFilters()
    final boolean showAll = "verbose".equals( request.getDBInstanceIdentifier() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<com.eucalyptus.rds.service.persist.entities.DBInstance> requestedAndAccessible =
          Predicates.and(
              RdsMetadatas.filterPrivileged( ),
              RdsMetadatas.filterById( request.getDBInstanceIdentifier() == null ?
                  Collections.emptySet() :
                  Collections.singleton( request.getDBInstanceIdentifier( )  ) )
          );

      final DBInstanceList resultDBInstances = new DBInstanceList();
      resultDBInstances.getMember().addAll( dbInstances.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( com.eucalyptus.rds.service.persist.entities.DBInstance.class, DBInstance.class ) ) );
      reply.getDescribeDBInstancesResult().setDBInstances(resultDBInstances);
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeDBLogFilesResponseType describeDBLogFiles(final DescribeDBLogFilesType request) {
    return request.getReply();
  }

  public DescribeDBParameterGroupsResponseType describeDBParameterGroups(final DescribeDBParameterGroupsType request) {
    return request.getReply();
  }

  public DescribeDBParametersResponseType describeDBParameters(final DescribeDBParametersType request) {
    return request.getReply();
  }

  public DescribeDBProxiesResponseType describeDBProxies(final DescribeDBProxiesType request) {
    return request.getReply();
  }

  public DescribeDBProxyTargetGroupsResponseType describeDBProxyTargetGroups(final DescribeDBProxyTargetGroupsType request) {
    return request.getReply();
  }

  public DescribeDBProxyTargetsResponseType describeDBProxyTargets(final DescribeDBProxyTargetsType request) {
    return request.getReply();
  }

  public DescribeDBSecurityGroupsResponseType describeDBSecurityGroups(final DescribeDBSecurityGroupsType request) {
    return request.getReply();
  }

  public DescribeDBSnapshotAttributesResponseType describeDBSnapshotAttributes(final DescribeDBSnapshotAttributesType request) {
    return request.getReply();
  }

  public DescribeDBSnapshotsResponseType describeDBSnapshots(final DescribeDBSnapshotsType request) {
    return request.getReply();
  }

  public DescribeDBSubnetGroupsResponseType describeDBSubnetGroups(final DescribeDBSubnetGroupsType request) throws RdsException {
    final DescribeDBSubnetGroupsResponseType reply = request.getReply();

    final Context ctx = Contexts.lookup( );
    final boolean showAll = "verbose".equals( request.getDBSubnetGroupName() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup> requestedAndAccessible =
          Predicates.and(
              RdsMetadatas.filterPrivileged( ),
              RdsMetadatas.filterById( request.getDBSubnetGroupName() == null ?
                  Collections.emptySet() :
                  Collections.singleton( request.getDBSubnetGroupName( )  ) )
          );

      final DBSubnetGroups resultSubnetGroups = new DBSubnetGroups();
      resultSubnetGroups.getMember().addAll( dbSubnetGroups.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( com.eucalyptus.rds.service.persist.entities.DBSubnetGroup.class, DBSubnetGroup.class ) ) );
      reply.getDescribeDBSubnetGroupsResult().setDBSubnetGroups(resultSubnetGroups);
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeEngineDefaultClusterParametersResponseType describeEngineDefaultClusterParameters(final DescribeEngineDefaultClusterParametersType request) {
    return request.getReply();
  }

  public DescribeEngineDefaultParametersResponseType describeEngineDefaultParameters(final DescribeEngineDefaultParametersType request) {
    return request.getReply();
  }

  public DescribeEventCategoriesResponseType describeEventCategories(final DescribeEventCategoriesType request) {
    return request.getReply();
  }

  public DescribeEventSubscriptionsResponseType describeEventSubscriptions(final DescribeEventSubscriptionsType request) {
    return request.getReply();
  }

  public DescribeEventsResponseType describeEvents(final DescribeEventsType request) {
    return request.getReply();
  }

  public DescribeExportTasksResponseType describeExportTasks(final DescribeExportTasksType request) {
    return request.getReply();
  }

  public DescribeGlobalClustersResponseType describeGlobalClusters(final DescribeGlobalClustersType request) {
    return request.getReply();
  }

  public DescribeInstallationMediaResponseType describeInstallationMedia(final DescribeInstallationMediaType request) {
    return request.getReply();
  }

  public DescribeOptionGroupOptionsResponseType describeOptionGroupOptions(final DescribeOptionGroupOptionsType request) {
    return request.getReply();
  }

  public DescribeOptionGroupsResponseType describeOptionGroups(final DescribeOptionGroupsType request) {
    return request.getReply();
  }

  public DescribeOrderableDBInstanceOptionsResponseType describeOrderableDBInstanceOptions(final DescribeOrderableDBInstanceOptionsType request) {
    return request.getReply();
  }

  public DescribePendingMaintenanceActionsResponseType describePendingMaintenanceActions(final DescribePendingMaintenanceActionsType request) {
    return request.getReply();
  }

  public DescribeReservedDBInstancesResponseType describeReservedDBInstances(final DescribeReservedDBInstancesType request) {
    return request.getReply();
  }

  public DescribeReservedDBInstancesOfferingsResponseType describeReservedDBInstancesOfferings(final DescribeReservedDBInstancesOfferingsType request) {
    return request.getReply();
  }

  public DescribeSourceRegionsResponseType describeSourceRegions(final DescribeSourceRegionsType request) {
    return request.getReply();
  }

  public DescribeValidDBInstanceModificationsResponseType describeValidDBInstanceModifications(final DescribeValidDBInstanceModificationsType request) {
    return request.getReply();
  }

  public DownloadDBLogFilePortionResponseType downloadDBLogFilePortion(final DownloadDBLogFilePortionType request) {
    return request.getReply();
  }

  public FailoverDBClusterResponseType failoverDBCluster(final FailoverDBClusterType request) {
    return request.getReply();
  }

  public ImportInstallationMediaResponseType importInstallationMedia(final ImportInstallationMediaType request) {
    return request.getReply();
  }

  public ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request) {
    return request.getReply();
  }

  public ModifyCertificatesResponseType modifyCertificates(final ModifyCertificatesType request) {
    return request.getReply();
  }

  public ModifyCurrentDBClusterCapacityResponseType modifyCurrentDBClusterCapacity(final ModifyCurrentDBClusterCapacityType request) {
    return request.getReply();
  }

  public ModifyDBClusterResponseType modifyDBCluster(final ModifyDBClusterType request) {
    return request.getReply();
  }

  public ModifyDBClusterEndpointResponseType modifyDBClusterEndpoint(final ModifyDBClusterEndpointType request) {
    return request.getReply();
  }

  public ModifyDBClusterParameterGroupResponseType modifyDBClusterParameterGroup(final ModifyDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public ModifyDBClusterSnapshotAttributeResponseType modifyDBClusterSnapshotAttribute(final ModifyDBClusterSnapshotAttributeType request) {
    return request.getReply();
  }

  public ModifyDBInstanceResponseType modifyDBInstance(final ModifyDBInstanceType request) {
    return request.getReply();
  }

  public ModifyDBParameterGroupResponseType modifyDBParameterGroup(final ModifyDBParameterGroupType request) {
    return request.getReply();
  }

  public ModifyDBProxyResponseType modifyDBProxy(final ModifyDBProxyType request) {
    return request.getReply();
  }

  public ModifyDBProxyTargetGroupResponseType modifyDBProxyTargetGroup(final ModifyDBProxyTargetGroupType request) {
    return request.getReply();
  }

  public ModifyDBSnapshotResponseType modifyDBSnapshot(final ModifyDBSnapshotType request) {
    return request.getReply();
  }

  public ModifyDBSnapshotAttributeResponseType modifyDBSnapshotAttribute(final ModifyDBSnapshotAttributeType request) {
    return request.getReply();
  }

  public ModifyDBSubnetGroupResponseType modifyDBSubnetGroup(final ModifyDBSubnetGroupType request) {
    return request.getReply();
  }

  public ModifyEventSubscriptionResponseType modifyEventSubscription(final ModifyEventSubscriptionType request) {
    return request.getReply();
  }

  public ModifyGlobalClusterResponseType modifyGlobalCluster(final ModifyGlobalClusterType request) {
    return request.getReply();
  }

  public ModifyOptionGroupResponseType modifyOptionGroup(final ModifyOptionGroupType request) {
    return request.getReply();
  }

  public PromoteReadReplicaResponseType promoteReadReplica(final PromoteReadReplicaType request) {
    return request.getReply();
  }

  public PromoteReadReplicaDBClusterResponseType promoteReadReplicaDBCluster(final PromoteReadReplicaDBClusterType request) {
    return request.getReply();
  }

  public PurchaseReservedDBInstancesOfferingResponseType purchaseReservedDBInstancesOffering(final PurchaseReservedDBInstancesOfferingType request) {
    return request.getReply();
  }

  public RebootDBInstanceResponseType rebootDBInstance(final RebootDBInstanceType request) {
    return request.getReply();
  }

  public RegisterDBProxyTargetsResponseType registerDBProxyTargets(final RegisterDBProxyTargetsType request) {
    return request.getReply();
  }

  public RemoveFromGlobalClusterResponseType removeFromGlobalCluster(final RemoveFromGlobalClusterType request) {
    return request.getReply();
  }

  public RemoveRoleFromDBClusterResponseType removeRoleFromDBCluster(final RemoveRoleFromDBClusterType request) {
    return request.getReply();
  }

  public RemoveRoleFromDBInstanceResponseType removeRoleFromDBInstance(final RemoveRoleFromDBInstanceType request) {
    return request.getReply();
  }

  public RemoveSourceIdentifierFromSubscriptionResponseType removeSourceIdentifierFromSubscription(final RemoveSourceIdentifierFromSubscriptionType request) {
    return request.getReply();
  }

  public RemoveTagsFromResourceResponseType removeTagsFromResource(final RemoveTagsFromResourceType request) {
    return request.getReply();
  }

  public ResetDBClusterParameterGroupResponseType resetDBClusterParameterGroup(final ResetDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public ResetDBParameterGroupResponseType resetDBParameterGroup(final ResetDBParameterGroupType request) {
    return request.getReply();
  }

  public RestoreDBClusterFromS3ResponseType restoreDBClusterFromS3(final RestoreDBClusterFromS3Type request) {
    return request.getReply();
  }

  public RestoreDBClusterFromSnapshotResponseType restoreDBClusterFromSnapshot(final RestoreDBClusterFromSnapshotType request) {
    return request.getReply();
  }

  public RestoreDBClusterToPointInTimeResponseType restoreDBClusterToPointInTime(final RestoreDBClusterToPointInTimeType request) {
    return request.getReply();
  }

  public RestoreDBInstanceFromDBSnapshotResponseType restoreDBInstanceFromDBSnapshot(final RestoreDBInstanceFromDBSnapshotType request) {
    return request.getReply();
  }

  public RestoreDBInstanceFromS3ResponseType restoreDBInstanceFromS3(final RestoreDBInstanceFromS3Type request) {
    return request.getReply();
  }

  public RestoreDBInstanceToPointInTimeResponseType restoreDBInstanceToPointInTime(final RestoreDBInstanceToPointInTimeType request) {
    return request.getReply();
  }

  public RevokeDBSecurityGroupIngressResponseType revokeDBSecurityGroupIngress(final RevokeDBSecurityGroupIngressType request) {
    return request.getReply();
  }

  public StartActivityStreamResponseType startActivityStream(final StartActivityStreamType request) {
    return request.getReply();
  }

  public StartDBClusterResponseType startDBCluster(final StartDBClusterType request) {
    return request.getReply();
  }

  public StartDBInstanceResponseType startDBInstance(final StartDBInstanceType request) {
    return request.getReply();
  }

  public StartExportTaskResponseType startExportTask(final StartExportTaskType request) {
    return request.getReply();
  }

  public StopActivityStreamResponseType stopActivityStream(final StopActivityStreamType request) {
    return request.getReply();
  }

  public StopDBClusterResponseType stopDBCluster(final StopDBClusterType request) {
    return request.getReply();
  }

  public StopDBInstanceResponseType stopDBInstance(final StopDBInstanceType request) {
    return request.getReply();
  }

  private static void handleException( final Exception e ) throws RdsException {
    handleException( e,  null );
  }

  private static void handleException(
      final Exception e,
      final Function<ConstraintViolationException, RdsException> constraintExceptionBuilder
  ) throws RdsException {
    final RdsServiceException cause = Exceptions.findCause( e, RdsServiceException.class );
    if ( cause != null ) {
      throw cause;
    }

    final ConstraintViolationException constraintViolationException =
        Exceptions.findCause( e, ConstraintViolationException.class );
    if ( constraintViolationException != null && constraintExceptionBuilder != null ) {
      throw constraintExceptionBuilder.apply( constraintViolationException );
    }

    logger.error( e, e );

    final RdsServiceException exception =
        new RdsServiceException( "InternalFailure", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
