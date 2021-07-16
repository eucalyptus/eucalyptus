/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.rds.service.persist.DBParameterGroups;
import com.eucalyptus.rds.service.persist.DefaultDBGroupParameters;
import com.eucalyptus.rds.service.persist.entities.DBParameter;
import com.eucalyptus.rds.service.persist.views.DBParameterView;
import io.vavr.CheckedFunction0;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.entities.AbstractStatefulPersistent_;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.common.RdsMetadatas;
import com.eucalyptus.rds.common.msgs.*;
import com.eucalyptus.rds.common.policy.RdsPolicySpec;
import com.eucalyptus.rds.common.policy.RdsResourceName;
import com.eucalyptus.rds.common.policy.RdsResourceNameException;
import com.eucalyptus.rds.service.engine.RdsEngine;
import com.eucalyptus.rds.service.persist.RdsMetadataNotFoundException;
import com.eucalyptus.rds.service.persist.entities.DBInstance.Status;
import com.eucalyptus.rds.service.persist.entities.DBSubnet;
import com.eucalyptus.rds.service.persist.Taggable;
import com.eucalyptus.rds.service.persist.Tags;
import com.eucalyptus.rds.service.persist.views.TagView;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncProxy;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import io.vavr.Tuple;
import io.vavr.Tuple2;


/**
 *
 */
@ComponentNamed
@SuppressWarnings({"unused", "Convert2Lambda"})
public class RdsService {
  private static final Logger logger = Logger.getLogger( RdsService.class );

  private final com.eucalyptus.rds.service.persist.DBInstances dbInstances;
  private final com.eucalyptus.rds.service.persist.DBParameterGroups dbParameterGroups;
  private final com.eucalyptus.rds.service.persist.DBSubnetGroups dbSubnetGroups;
  private final com.eucalyptus.rds.service.persist.Tags tags;

  @Inject
  public RdsService(
      final com.eucalyptus.rds.service.persist.DBInstances dbInstances,
      final com.eucalyptus.rds.service.persist.DBParameterGroups dbParameterGroups,
      final com.eucalyptus.rds.service.persist.DBSubnetGroups dbSubnetGroups,
      final com.eucalyptus.rds.service.persist.Tags tags
  ) {
    this.dbInstances = dbInstances;
    this.dbParameterGroups = dbParameterGroups;
    this.dbSubnetGroups = dbSubnetGroups;
    this.tags = tags;
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

  public AddTagsToResourceResponseType addTagsToResource(final AddTagsToResourceType request) throws RdsException {
    final AddTagsToResourceResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    validateTags(request.getTags());
    try {
      final String resourceArn = request.getResourceName();
      final RdsResourceName arn = RdsResourceName.parse(resourceArn);
      try (final TransactionResource tx =
               Entities.transactionFor(
                   com.eucalyptus.rds.service.persist.entities.Tag.class)) {
        switch(arn.getResourceType()) {
          case "rds:db":
            addTagsForEntity(
                dbInstanceNotFound(),
                com.eucalyptus.rds.service.persist.entities.DBInstance.class,
                arn.getResourceName(),
                request.getTags());
            break;
          case "rds:pg":
            addTagsForEntity(
                dbParameterGroupNotFound(),
                com.eucalyptus.rds.service.persist.entities.DBParameterGroup.class,
                arn.getResourceName(),
                request.getTags());
            break;
          case "rds:subgrp":
            addTagsForEntity(
                dbSubnetGroupNotFound(),
                com.eucalyptus.rds.service.persist.entities.DBSubnetGroup.class,
                arn.getResourceName(),
                request.getTags());
            break;
          default:
            throw invalidArn().get();
        }
        tx.commit();
      }
    } catch ( final RdsResourceNameException ex) {
      throw invalidArn().get();
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
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

  public CopyDBParameterGroupResponseType copyDBParameterGroup(final CopyDBParameterGroupType request) throws RdsException {
    final CopyDBParameterGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String sourceIdentifierOrArn = request.getSourceDBParameterGroupIdentifier();
      final String name = request.getTargetDBParameterGroupIdentifier().toLowerCase();
      final String desc = request.getTargetDBParameterGroupDescription();

      final String sourceName = sourceIdentifierOrArn.startsWith("arn:") ?
          resourceName("pg", sourceIdentifierOrArn).getOrElseThrow(invalidArn()).getResourceName() :
          sourceIdentifierOrArn;

      if (name.equals("default") || name.startsWith("default.")) {
        throw new RdsClientException("InvalidParameterValue", "Invalid name");
      }

      validateTags(request.getTags());

      final CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBParameterGroup> allocator =
          new CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBParameterGroup>( ) {
            @Override
            public com.eucalyptus.rds.service.persist.entities.DBParameterGroup get( ) {
              try {
                try (final TransactionResource tx =
                         Entities.transactionFor(com.eucalyptus.rds.service.persist.entities.DBParameterGroup.class)) {

                  final com.eucalyptus.rds.service.persist.entities.DBParameterGroup sourceGroup =
                      dbParameterGroups.lookupByName(
                          AccountFullName.getInstance(ownerFullName.getAccountNumber()),
                          sourceName,
                          RdsMetadatas.filterPrivileged(),
                          Function.identity());

                  final com.eucalyptus.rds.service.persist.entities.DBParameterGroup group =
                      com.eucalyptus.rds.service.persist.entities.DBParameterGroup.create(
                          ownerFullName,
                          name,
                          desc,
                          sourceGroup.getFamily()
                      );
                  final com.eucalyptus.rds.service.persist.entities.DBParameterGroup persisted =
                      dbParameterGroups.save(group);
                  final List<DBParameter> parameters = Lists.newArrayList();
                  for (final DBParameter parameter : sourceGroup.getParameters()) {
                    final DBParameter dbParameter = DBParameter.create(persisted, parameter.getParameterName());
                    dbParameter.setParameterValue(parameter.getParameterValue());
                    dbParameter.setAllowedValues(parameter.getAllowedValues());
                    dbParameter.setApplyMethod(parameter.getApplyMethod());
                    dbParameter.setApplyType(parameter.getApplyType());
                    dbParameter.setDataType(parameter.getDataType());
                    dbParameter.setDescription(parameter.getDescription());
                    dbParameter.setMinimumEngineVersion(parameter.getMinimumEngineVersion());
                    dbParameter.setModifiable(parameter.isModifiable());
                    dbParameter.setSource(parameter.getSource());
                    parameters.add(dbParameter);
                  }
                  persisted.setParameters(parameters);
                  addTags(persisted, request.getTags());
                  tx.commit();
                  return persisted;
                }
              } catch ( final RdsMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared(dbParameterGroupNotFound().get());
              } catch ( Exception ex ) {
                throw new RuntimeException( ex );
              }
            }
          };

      final com.eucalyptus.rds.service.persist.entities.DBParameterGroup group =
          RdsMetadatas.allocateUnitlessResource( allocator );
      reply.getCopyDBParameterGroupResult().setDBParameterGroup(
          TypeMappers.transform(group, DBParameterGroup.class));
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("DBParameterGroupAlreadyExists", "Group already exists"));
    }
    return reply;
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
      final String availabilityZone = request.getAvailabilityZone();
      final String parameterGroupName = request.getDBParameterGroupName();
      final String subnetGroupName = request.getDBSubnetGroupName();
      final RdsEngine engine = RdsEngine.valueOf(request.getEngine());
      final Set<String> vpcSecurityGroups = request.getVpcSecurityGroupIds()==null ?
          Collections.emptySet() :
          Sets.newTreeSet(request.getVpcSecurityGroupIds().getMember());
      validateTags(request.getTags());

      final ComputeApi computeApi = AsyncProxy.client(ComputeApi.class);

      if (availabilityZone != null) {
        final List<SubnetType> subnetItems = computeApi.describeSubnets(
            ComputeApi.filter("availability-zone", availabilityZone)).getSubnetSet().getItem();
        if (subnetItems.isEmpty()) {
          throw new RdsClientException("ValidationError", "Availabilty zone not found");
        }
      }
      final List<SecurityGroupItemType> securityGroupItems = vpcSecurityGroups.isEmpty() ?
          Collections.emptyList() :
          computeApi.describeSecurityGroups(Lists.newArrayList(vpcSecurityGroups)).getSecurityGroupInfo();
      if (securityGroupItems.size() != vpcSecurityGroups.size()) {
        throw new RdsClientException("ValidationError", "Security group not found");
      }

      final CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBInstance> allocator =
          new CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBInstance>( ) {
            @Override
            public com.eucalyptus.rds.service.persist.entities.DBInstance get( ) {
              try {
                try (final TransactionResource tx = Entities.transactionFor(com.eucalyptus.rds.service.persist.entities.DBInstance.class)) {
                  final com.eucalyptus.rds.service.persist.entities.DBParameterGroup parameterGroup =
                      parameterGroupName == null ?
                          null :
                          handleNotFound(dbParameterGroupNotFound(), () -> dbParameterGroups.lookupByName(
                              ctx.getUserFullName( ).asAccountFullName(),
                              parameterGroupName,
                              RdsMetadatas.filterPrivileged( ),
                              Function.identity( )));

                  final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup subnetGroup =
                      subnetGroupName == null ?
                          null :
                          handleNotFound(dbSubnetGroupNotFound(), () -> dbSubnetGroups.lookupByName(
                              ctx.getUserFullName( ).asAccountFullName(),
                              subnetGroupName,
                              RdsMetadatas.filterPrivileged( ),
                              Function.identity( )));

                  if (availabilityZone != null && subnetGroup != null && Stream.ofAll(subnetGroup.getSubnets())
                      .filter(subnet -> availabilityZone.equals(subnet.getAvailabilityZone())).isEmpty()) {
                    throw Exceptions.toUndeclared(
                        new RdsClientException("ValidationError", "DB subnet group invalid for availability zone"));
                  }

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

                  instance.setAvailabilityZone(availabilityZone);
                  instance.setMasterUsername(request.getMasterUsername());
                  instance.setMasterUserPassword(request.getMasterUserPassword());
                  instance.setVpcSecurityGroups(Lists.newArrayList(vpcSecurityGroups));
                  instance.setDbSubnetGroup(subnetGroup);
                  instance.setDbParameterGroup(parameterGroup);
                  if (parameterGroup != null) {
                    instance.getDbInstanceRuntime().setDbParameterHandle(engine.buildHandle(
                        parameterGroup,
                        parameterGroup.getParameters()
                    ));
                  }
                  final com.eucalyptus.rds.service.persist.entities.DBInstance persisted =
                      dbInstances.save(instance);
                  addTags(persisted, request.getTags());
                  tx.commit();
                  return persisted;
                }
              } catch ( Throwable ex ) {
                throw Exceptions.toUndeclared( ex );
              }
            }
          };

      final com.eucalyptus.rds.service.persist.entities.DBInstance instance =
          RdsMetadatas.allocateUnitlessResource( allocator );
      reply.getCreateDBInstanceResult().setDBInstance(
          TypeMappers.transform(instance, DBInstance.class));
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("DBInstanceAlreadyExists", "Instance already exists"));
    }
    return reply;
  }

  public CreateDBInstanceReadReplicaResponseType createDBInstanceReadReplica(final CreateDBInstanceReadReplicaType request) {
    return request.getReply();
  }

  public CreateDBParameterGroupResponseType createDBParameterGroup(final CreateDBParameterGroupType request) throws RdsException {
    final CreateDBParameterGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName( );
    try {
      final String name = request.getDBParameterGroupName().toLowerCase();
      final String family = request.getDBParameterGroupFamily();
      final String desc = request.getDescription();

      if ("default".equals(name)) {
        throw new RdsClientException("InvalidParameterValue", "Invalid name");
      }

      final DefaultDBGroupParameters defaultParameters =
          DBParameterGroups.loadDefaultParameters(family);
      if (defaultParameters.getParameters().isEmpty()) {
        throw new RdsClientException("InvalidParameterValue", "Invalid family");
      }

      if (name.startsWith("default.") && !name.equals("default." + family)) {
        throw new RdsClientException("InvalidParameterValue", "Invalid name for family default");
      }

      validateTags(request.getTags());

      final CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBParameterGroup> allocator =
          new CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBParameterGroup>( ) {
            @Override
            public com.eucalyptus.rds.service.persist.entities.DBParameterGroup get( ) {
              try {
                final com.eucalyptus.rds.service.persist.entities.DBParameterGroup group =
                    com.eucalyptus.rds.service.persist.entities.DBParameterGroup.create(
                        ownerFullName,
                        name,
                        desc,
                        family
                    );

                try (final TransactionResource tx = Entities.transactionFor(group)) {
                  final com.eucalyptus.rds.service.persist.entities.DBParameterGroup persisted =
                      dbParameterGroups.save(group);
                  final List<DBParameter> parameters = Lists.newArrayList();
                  for (final Parameter parameter : defaultParameters.getParameters()) {
                    final DBParameter dbParameter = DBParameter.create(persisted, parameter.getParameterName());
                    dbParameter.setParameterValue(parameter.getParameterValue());
                    dbParameter.setAllowedValues(parameter.getAllowedValues());
                    dbParameter.setApplyMethod(DBParameter.ApplyMethod.fromString(parameter.getApplyMethod()));
                    dbParameter.setApplyType(parameter.getApplyType());
                    dbParameter.setDataType(parameter.getDataType());
                    dbParameter.setDescription(parameter.getDescription());
                    dbParameter.setMinimumEngineVersion(parameter.getMinimumEngineVersion());
                    dbParameter.setModifiable(parameter.getIsModifiable());
                    dbParameter.setSource(DBParameter.Source.fromString(parameter.getSource()));
                    parameters.add(dbParameter);
                  }
                  persisted.setParameters(parameters);
                  addTags(persisted, request.getTags());
                  tx.commit();
                  return persisted;
                }
              } catch ( Exception ex ) {
                throw new RuntimeException( ex );
              }
            }
          };

      final com.eucalyptus.rds.service.persist.entities.DBParameterGroup group =
          RdsMetadatas.allocateUnitlessResource( allocator );
      reply.getCreateDBParameterGroupResult().setDBParameterGroup(
          TypeMappers.transform(group, DBParameterGroup.class));
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("DBParameterGroupAlreadyExists", "Group already exists"));
    }
    return reply;
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

      if ("default".equals(name)) {
        throw new RdsClientException("InvalidParameterValue", "Invalid name");
      }
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
      validateTags(request.getTags());

      final CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup> allocator =
          new CompatSupplier<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup>( ) {
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
            try (final TransactionResource tx = Entities.transactionFor(group)) {
              final com.eucalyptus.rds.service.persist.entities.DBSubnetGroup persisted =
                  dbSubnetGroups.save(group);
              addTags(persisted, request.getTags());
              tx.commit();
              return persisted;
            }
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
      throw handleException( e, __ -> new RdsClientException("DBSubnetGroupAlreadyExists", "Group already exists"));
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
      throw dbInstanceNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("InvalidDBInstanceState", "Invalid state for delete") );
    }
    return reply;
  }

  public DeleteDBInstanceAutomatedBackupResponseType deleteDBInstanceAutomatedBackup(final DeleteDBInstanceAutomatedBackupType request) {
    return request.getReply();
  }

  public DeleteDBParameterGroupResponseType deleteDBParameterGroup(final DeleteDBParameterGroupType request) throws RdsException {
    final DeleteDBParameterGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBParameterGroupName();

      final com.eucalyptus.rds.service.persist.entities.DBParameterGroup group =
          dbParameterGroups.lookupByName( ownerFullName, name, RdsMetadatas.filterPrivileged(), Function.identity());

      dbParameterGroups.deleteByExample( group );
    } catch ( final RdsMetadataNotFoundException e ) {
      throw dbParameterGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("InvalidDBParameterGroupState", "Group in use") );
    }
    return reply;
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
          dbSubnetGroups.lookupByName( ownerFullName, name, RdsMetadatas.filterPrivileged(), Function.identity());

      dbSubnetGroups.deleteByExample( group );
    } catch ( final RdsMetadataNotFoundException e ) {
      throw dbSubnetGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("InvalidDBSubnetGroupStateFault", "Group in use") );
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
        for (final RdsEngine.Version version : engine.getVersions()) {
          final DBEngineVersion engineVersion = new DBEngineVersion();
          engineVersion.setEngine(engine.toString());
          engineVersion.setEngineVersion(version.getVersion());
          engineVersion.setDBEngineDescription(engine.getDescription());
          engineVersion.setDBEngineVersionDescription(version.getVersionDescription());
          engineVersion.setDBParameterGroupFamily(version.getFamily());
          versionList.getMember().add(engineVersion);
        }
      }
      reply.getDescribeDBEngineVersionsResult().setDBEngineVersions(versionList);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeDBInstanceAutomatedBackupsResponseType describeDBInstanceAutomatedBackups(final DescribeDBInstanceAutomatedBackupsType request) {
    return request.getReply();
  }

  public DescribeDBInstancesResponseType describeDBInstances(final DescribeDBInstancesType request) throws RdsException {
    final DescribeDBInstancesResponseType reply = request.getReply();

    final Context ctx = Contexts.lookup( );
    final boolean showAll = "verbose".equals( request.getDBInstanceIdentifier() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Conjunction conjunction = Restrictions.conjunction();
      final FilterList filters = request.getFilters();
      if (filters != null) {
        for (final Filter filter : filters.getMember()) {
          switch (Objects.toString(filter.getName(), "")) {
            case "db-instance-id":
              conjunction.add(Restrictions.in("displayName",
                  dbIdentifiers(filter.getValues().getMember(), Collections.singleton("--"))));
              break;
            case "engine":
              conjunction.add(Restrictions.in("engine", filter.getValues().getMember()));
              break;
            case "db-cluster-id":
            case "dbi-resource-id":
            case "domain":
              conjunction.add(Restrictions.eq("displayName", "")); // match nothing
              break;
            default: throw new RdsClientException("ValidationError", "Invalid filter");
          }
        }
      }

      final Predicate<com.eucalyptus.rds.service.persist.entities.DBInstance> requestedAndAccessible =
          RdsMetadatas.<com.eucalyptus.rds.service.persist.entities.DBInstance>filterPrivileged( ).and(
              RdsMetadatas.filterById( request.getDBInstanceIdentifier() == null ?
                  Collections.emptySet() :
                  Collections.singleton( request.getDBInstanceIdentifier( )  ) ) );

      final DBInstanceList resultDBInstances = new DBInstanceList();
      resultDBInstances.getMember().addAll( dbInstances.list(
          ownerFullName,
          conjunction,
          Collections.emptyMap(),
          requestedAndAccessible,
          TypeMappers.lookupF( com.eucalyptus.rds.service.persist.entities.DBInstance.class, DBInstance.class ) ) );
      reply.getDescribeDBInstancesResult().setDBInstances(resultDBInstances);
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public DescribeDBLogFilesResponseType describeDBLogFiles(final DescribeDBLogFilesType request) {
    return request.getReply();
  }

  public DescribeDBParameterGroupsResponseType describeDBParameterGroups(final DescribeDBParameterGroupsType request) throws RdsException {
    final DescribeDBParameterGroupsResponseType reply = request.getReply();

    final Context ctx = Contexts.lookup( );
    final boolean showAll = "verbose".equals( request.getDBParameterGroupName() );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<com.eucalyptus.rds.service.persist.entities.DBParameterGroup> requestedAndAccessible =
          RdsMetadatas.<com.eucalyptus.rds.service.persist.entities.DBParameterGroup>filterPrivileged( ).and(
              RdsMetadatas.filterById( request.getDBParameterGroupName() == null ?
                  Collections.emptySet() :
                  Collections.singleton( request.getDBParameterGroupName( )  ) ) );

      final DBParameterGroupList resultParameterGroups = new DBParameterGroupList();
      resultParameterGroups.getMember().addAll( dbParameterGroups.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookupF( com.eucalyptus.rds.service.persist.entities.DBParameterGroup.class, DBParameterGroup.class ) ) );
      reply.getDescribeDBParameterGroupsResult().setDBParameterGroups(resultParameterGroups);
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public DescribeDBParametersResponseType describeDBParameters(final DescribeDBParametersType request) throws RdsException {
    final DescribeDBParametersResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBParameterGroupName();
      final String source = request.getSource();
      final EnumSet<DBParameterView.Source> sources = source == null ?
          EnumSet.allOf(DBParameterView.Source.class) :
          EnumSet.of(DBParameterView.Source.fromString(source));
      final ParametersList parametersList = dbParameterGroups.lookupByName(
          ownerFullName,
          name,
          RdsMetadatas.filterPrivileged(),
          dbParameterGroup -> {
            final ParametersList parameters = new ParametersList();
            final List<Parameter> parameterCollection = parameters.getMember();
            Stream.ofAll(dbParameterGroup.getParameters())
                .filter(dbParam -> sources.contains(dbParam.getSource()))
                .map(TypeMappers.lookupF(DBParameter.class, Parameter.class))
                .forEach(parameterCollection::add);
            return parameters;
          });

      reply.getDescribeDBParametersResult().setParameters(parametersList);
    } catch ( final RdsMetadataNotFoundException e ) {
      throw dbParameterGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
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
          RdsMetadatas.<com.eucalyptus.rds.service.persist.entities.DBSubnetGroup>filterPrivileged( ).and(
              RdsMetadatas.filterById( request.getDBSubnetGroupName() == null ?
                  Collections.emptySet() :
                  Collections.singleton( request.getDBSubnetGroupName( )  ) ) );

      final DBSubnetGroups resultSubnetGroups = new DBSubnetGroups();
      resultSubnetGroups.getMember().addAll( dbSubnetGroups.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookupF( com.eucalyptus.rds.service.persist.entities.DBSubnetGroup.class, DBSubnetGroup.class ) ) );
      reply.getDescribeDBSubnetGroupsResult().setDBSubnetGroups(resultSubnetGroups);
    } catch ( Exception e ) {
      throw handleException( e );
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

  public ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request) throws RdsException {
    final ListTagsForResourceResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    try {
      final List<Tag> resourceTags = tags.list(
          ownerFullName,
          Restrictions.eq("resourceArn", request.getResourceName()),
          Collections.emptyMap(),
          RdsMetadatas.filterPrivileged(),
          TypeMappers.lookupF(TagView.class, Tag.class));
      final TagList tagList = new TagList();
      tagList.getMember().addAll(Tags.sort(resourceTags));
      reply.getListTagsForResourceResult().setTagList(tagList);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
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

  public ModifyDBParameterGroupResponseType modifyDBParameterGroup(final ModifyDBParameterGroupType request) throws RdsException {
    final ModifyDBParameterGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBParameterGroupName();

      dbParameterGroups.updateByExample(
          com.eucalyptus.rds.service.persist.entities.DBParameterGroup.exampleWithName( ownerFullName, name ),
          ownerFullName,
          name,
          dbParameterGroup -> {
            if (!RdsMetadatas.filterPrivileged().apply(dbParameterGroup)) {
              throw new NoSuchElementException();
            }

            for (final Parameter parameter : request.getParameters().getMember()) {
              for (final DBParameter dbParameter : dbParameterGroup.getParameters()) {
                if (dbParameter.getParameterName().equals(parameter.getParameterName())) {
                  dbParameter.setParameterValue(parameter.getParameterValue());
                  dbParameter.setSource(DBParameter.Source.user);
                }
              }
            }

            return dbParameterGroup;
          });

      reply.getModifyDBParameterGroupResult().setDBParameterGroupName(name);
    } catch ( final RdsMetadataNotFoundException e ) {
      throw dbParameterGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("InvalidDBParameterGroupState", "Group conflict") );
    }
    return reply;
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

  public RemoveTagsFromResourceResponseType removeTagsFromResource(final RemoveTagsFromResourceType request) throws RdsException {
    final RemoveTagsFromResourceResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    try {
      final String resourceArn = request.getResourceName();
      final Set<String> tagKeys = Sets.newHashSet(request.getTagKeys().getMember());
      try (final TransactionResource tx =
               Entities.transactionFor(com.eucalyptus.rds.service.persist.entities.Tag.class)) {
        final List<com.eucalyptus.rds.service.persist.entities.Tag<?>> tagList = tags.list(
            ownerFullName,
            Restrictions.conjunction(
              Restrictions.eq("resourceArn", resourceArn),
              Restrictions.in( "displayName", tagKeys )),
            Collections.emptyMap(),
            RdsMetadatas.filterPrivileged(),
            Function.identity());
        for (final com.eucalyptus.rds.service.persist.entities.Tag<?> tag : tagList) {
          tags.delete(tag);
        }
        tx.commit();
      }
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ResetDBClusterParameterGroupResponseType resetDBClusterParameterGroup(final ResetDBClusterParameterGroupType request) {
    return request.getReply();
  }

  public ResetDBParameterGroupResponseType resetDBParameterGroup(final ResetDBParameterGroupType request) throws RdsException {
    final ResetDBParameterGroupResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
      final String name = request.getDBParameterGroupName();

      dbParameterGroups.updateByExample(
          com.eucalyptus.rds.service.persist.entities.DBParameterGroup.exampleWithName( ownerFullName, name ),
          ownerFullName,
          name,
          dbParameterGroup -> {
            if (!RdsMetadatas.filterPrivileged().apply(dbParameterGroup)) {
              throw new NoSuchElementException();
            }

            final DefaultDBGroupParameters defaultParameters =
                DBParameterGroups.loadDefaultParameters(dbParameterGroup.getFamily());

            parameters:
            for (final Parameter parameter : request.getParameters().getMember()) {
              for (final DBParameter dbParameter : dbParameterGroup.getParameters()) {
                if (dbParameter.getParameterName().equals(parameter.getParameterName())) {
                  for (final Parameter defaultParameter : defaultParameters.getParameters()) {
                    if (defaultParameter.getParameterName().equals(parameter.getParameterName())) {
                      dbParameter.setParameterValue(defaultParameter.getParameterValue());
                      dbParameter.setSource(DBParameter.Source.fromString(defaultParameter.getSource()));
                      continue parameters;
                    }
                  }
                }
              }
            }

            return dbParameterGroup;
          });

      reply.getResetDBParameterGroupResult().setDBParameterGroupName(name);
    } catch ( final RdsMetadataNotFoundException e ) {
      throw dbParameterGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e, __ -> new RdsClientException("InvalidDBParameterGroupState", "Group conflict") );
    }
    return reply;
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

  private static Set<String> dbIdentifiers(
      final Iterable<String> values,
      final Set<String> onEmpty
  ) {
    final Set<String> ids = Sets.newHashSet();
    for (final String idOrArn : values) {
      if (idOrArn.startsWith("arn:")) {
        resourceName("db", idOrArn)
            .map(RdsResourceName::getResourceName)
            .forEach(ids::add);
      } else {
        ids.add(idOrArn);
      }
    }
    if (ids.isEmpty()) {
      return onEmpty;
    } else {
      return ids;
    }
  }

  private static io.vavr.control.Option<RdsResourceName> resourceName(
      final String type,
      final String arn
  ) {
    io.vavr.control.Option<RdsResourceName> resourceName = io.vavr.control.Option.none();
    try {
      final Ern ern = Ern.parse(arn);
      if (ern instanceof RdsResourceName) {
        final RdsResourceName rdsErn = (RdsResourceName) ern;
        if (type == null ||
            PolicySpec.qualifiedName(RdsPolicySpec.VENDOR_RDS, type).equals(rdsErn.getResourceType())) {
          resourceName = io.vavr.control.Option.of(rdsErn);
        }
      }
    } catch (final Exception ignore) {}
    return resourceName;
  }

  private static void validateTags(final TagList tagList) throws RdsClientException {
    if (tagList != null) {
      validateTags(tagList.getMember(), true,Tag::getKey, Tag::getValue);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void validateTags(
      final Taggable<?> taggable,
      final boolean checkReserved
  ) throws RdsClientException {
    validateTags(taggable.getTags(), checkReserved, TagView::getKey, TagView::getValue);
  }

  private static <T> void validateTags(
      final List<T> tagItems,
      final boolean checkReserved,
      final Function<T,String> keyGetter,
      final Function<T,String> valueGetter
  ) throws RdsClientException {
    final Map<String, String> tags = Maps.newHashMap();
    for (final T tag : tagItems) {
      if (tags.put(keyGetter.apply(tag), Objects.toString(valueGetter.apply(tag), "")) != null) {
        throw new RdsClientException("ValidationError",
            "Duplicate tag key (" + keyGetter.apply(tag) + ")");
      }
    }
    final int reservedTags = Stream.ofAll(tags.keySet())
        .filter(key -> key.startsWith("euca:") || key.startsWith("aws:")).length();
    if (reservedTags > 0 && checkReserved && !Contexts.lookup().isPrivileged()) {
      throw new RdsClientException("ValidationError",
          "Invalid tag key (reserved prefix)");
    }
    if ((tags.size() - reservedTags) > 50) {
      throw Exceptions.toUndeclared(
          new RdsClientException("ValidationError", "Tag limit exceeded"));
    }
  }

  private static <RT extends TagView, RTE extends AbstractStatefulPersistent<?> & RdsMetadata & Taggable<RT>> void addTagsForEntity(
      final CompatSupplier<RdsClientException> notFound,
      final Class<RTE> entityClass,
      final String id,
      final TagList tagList) throws RdsClientException {
    try {
      final RTE taggable = Entities.criteriaQuery(entityClass)
          .whereEqual(AbstractStatefulPersistent_.displayName, id).uniqueResult();
      if (!RdsMetadatas.filterPrivileged().test(taggable)) {
        notFound.get();
      }
      final List<RT> tags = taggable.getTags();
      final Map<String,RT> tagMap =
          CollectionUtils.putAll(tags, Maps.newHashMap(), TagView::getKey, Functions.identity());
      for (final Tag tag : tagList.getMember()) {
        if (tagMap.containsKey(tag.getKey())) {
          taggable.updateTag(tagMap.get(tag.getKey()), tag.getValue());
        } else {
          final RT resourceTag = taggable.createTag(tag.getKey(), tag.getValue());
          Entities.persist(resourceTag);
          tags.add(resourceTag);
        }
      }
      validateTags(taggable, false);
    } catch (final NoSuchElementException e) {
      throw notFound.get();
    }
  }

  private static <RT extends TagView> void addTags(final Taggable<RT> resource, final TagList tags) {
    if (tags != null) {
      final List<RT> resourceTags = Lists.newArrayList();
      for (final Tag tag : tags.getMember()) {
        final RT resourceTag =
            resource.createTag(tag.getKey(), Objects.toString(tag.getValue(), ""));
        Entities.persist(resourceTag);
      }
      resource.setTags(resourceTags);
    }
  }

  private static CompatSupplier<RdsClientException> invalidArn() {
    return () -> new RdsClientException("ValidationError", "Invalid resource ARN");
  }

  private static CompatSupplier<RdsClientException> dbInstanceNotFound() {
    return () -> new RdsClientNotFoundException("DBInstanceNotFound", "Database instance not found");
  }

  private static CompatSupplier<RdsClientException> dbParameterGroupNotFound() {
    return () -> new RdsClientNotFoundException( "DBParameterGroupNotFound", "Database parameter group not found" );
  }

  private static CompatSupplier<RdsClientException> dbSubnetGroupNotFound() {
    return () -> new RdsClientNotFoundException( "DBSubnetGroupNotFoundFault", "Database subnet group not found" );
  }

  private static RdsException handleException( final Exception e ) throws RdsException {
    return handleException( e,  null );
  }

  private static <T> T handleNotFound(
      final CompatSupplier<RdsClientException> notFoundSuppler,
      final CheckedFunction0<T> supplier
  ) throws Throwable {
    try {
      return supplier.apply();
    } catch (RdsMetadataNotFoundException e) {
      throw  notFoundSuppler.get();
    }
  }

  private static RdsException handleException(
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
