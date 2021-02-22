/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudformation.common.msgs.Output;
import com.eucalyptus.cloudformation.common.msgs.Stack;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.service.activities.RdsActivityTasks;
import com.eucalyptus.rds.service.engine.RdsEngine;
import com.eucalyptus.rds.service.persist.DBInstances;
import com.eucalyptus.rds.service.persist.RdsMetadataException;
import com.eucalyptus.rds.service.persist.entities.DBInstance;
import com.eucalyptus.rds.service.persist.entities.DBInstance.Status;
import com.eucalyptus.rds.service.persist.entities.DBInstanceRuntime;
import com.eucalyptus.rds.service.persist.entities.PersistenceDBInstances;
import com.eucalyptus.rds.service.persist.views.DBInstanceComposite;
import com.eucalyptus.rds.service.persist.views.DBInstanceRuntimeComposite;
import com.eucalyptus.rds.service.persist.views.DBInstanceRuntimeView;
import com.eucalyptus.rds.service.persist.views.DBInstanceView;
import com.eucalyptus.rds.service.persist.views.DBSubnetView;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceRuntimeComposite;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceRuntimeView;
import com.eucalyptus.rds.service.persist.views.ImmutableDBInstanceView;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Strings;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import io.vavr.Tuple3;

/**
 *
 */
@SuppressWarnings("Guava")
public class RdsWorkflow {

  private static final Logger logger = Logger.getLogger(RdsWorkflow.class);

  private final DBInstances dbInstances;

  private final RdsActivityTasks rdsActivityTasks = RdsActivityTasks.getInstance();

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add(new WorkflowTask( 10, "DBInstances.SetupVolume"   ){@Override void doWork(){ dbInstanceSetupVolume(); }})
      .add(new WorkflowTask( 10, "DBInstances.SetupInstance" ){@Override void doWork(){ dbInstanceSetupInstance(); }})
      .add(new WorkflowTask( 10, "DBInstances.SetupNetwork"  ){@Override void doWork(){ dbInstanceSetupNetwork(); }})
      .add(new WorkflowTask( 10, "DBInstances.Delete"        ){@Override void doWork(){ dbInstanceDelete(); }})
      .add(new WorkflowTask( 30, "DBInstances.FailureCleanup"){@Override void doWork(){ dbInstanceFailureCleanup( ); }})
      .add(new WorkflowTask(300, "DBInstances.Timeout"       ){@Override void doWork(){ dbInstanceTimeout( ); }})
      .build();

  public RdsWorkflow(
      final DBInstances dbInstances
  ) {
    this.dbInstances = dbInstances;
  }

  private void doWorkflow() {
    for (final WorkflowTask workflowTask : workflowTasks) {
      try {
        workflowTask.perhapsWork();
      } catch (Exception e) {
        logger.error(e, e);
      }
    }
  }

  private List<String> listDbInstanceUuids(final DBInstance.Status status) {
    List<String> dbInstanceUuids = Collections.emptyList();
    try {
      dbInstanceUuids = dbInstances.listByExample(
          DBInstance.exampleWithStatus(status),
          Predicates.alwaysTrue(),
          DBInstance::getNaturalId);
    } catch (final RdsMetadataException e) {
      logger.error("Error listing db instances", e);
    }
    return dbInstanceUuids;
  }

  private DBInstanceRuntimeComposite lookupByUuid(final String dbInstanceUuid) throws RdsMetadataException {
    return lookupByUuid( dbInstanceUuid, DBInstances.COMPOSITE_RUNTIME );
  }

  private <T> T lookupByUuid(final String dbInstanceUuid, final Function<? super DBInstance,? extends T> transform) throws RdsMetadataException {
    return dbInstances.lookupByExample(
        DBInstance.exampleWithUuid(dbInstanceUuid),
        null,
        dbInstanceUuid,
        Predicates.alwaysTrue(),
        transform );
  }

  private void dbInstanceSetupVolume() {
    for (final String dbInstanceUuid : listDbInstanceUuids(Status.creating)) {
      final DBInstanceComposite dbInstanceComposite;
      try {
        dbInstanceComposite = lookupByUuid( dbInstanceUuid, DBInstances.COMPOSITE_FULL );
      } catch (final Exception e) {
        logger.error("Error processing db instance create " + dbInstanceUuid, e);
        continue;
      }
      final DBInstanceView dbInstance = dbInstanceComposite.getInstance();

      if ( dbInstanceComposite.getRuntime().getSystemVolumeId() != null ) {
        continue; // volume creation already started
      }

      final String userAccount = dbInstance.getOwnerAccountNumber();
      final String userAvailabilityZone =
          pickAvailabilityZone(dbInstance.getAvailabilityZone(), dbInstanceComposite.getSubnets());
      if ( userAvailabilityZone == null ) {
        dbInstanceSetupFailure(dbInstance, "No zone");
        continue;
      }

      final Optional<DBSubnetView> subnetViewOptional = dbInstanceComposite.getSubnets().stream()
          .filter(subnet -> userAvailabilityZone.equals(subnet.getAvailabilityZone()))
          .findFirst();
      if (!subnetViewOptional.isPresent()) {
        dbInstanceSetupFailure(dbInstance, "No subnet");
        continue;
      }
      final String userSubnetId = subnetViewOptional.get().getSubnetId();
      final Tuple3<String,String,String> vpcInfo = RdsSystemVpcs.getSystemVpcSubnetId(userSubnetId);
      final String dbIdentifier = dbInstance.getDisplayName();
      final int dbAllocatedStorage = dbInstance.getAllocatedStorage();
      final String dbVpcId = vpcInfo._1();
      final String dbSubnetId = vpcInfo._2();
      final String dbAvailablityZone = vpcInfo._3();
      final String dbVolumeId = rdsActivityTasks.createSystemVolume(
          dbAvailablityZone,
          dbAllocatedStorage,
          Collections.singletonMap("euca:rds:dbid", userAccount + ":" + dbIdentifier)).getVolumeId();

      try {
        dbInstances.updateByView(dbInstance, instance -> {
          // zone will have been picked if not previously set
          instance.setAvailabilityZone(userAvailabilityZone);

          // update runtime details for network / volume
          final DBInstanceRuntime instanceRuntime = instance.getDbInstanceRuntime();
          instanceRuntime.setUserSubnetId(userSubnetId);
          instanceRuntime.setSystemVpcId(dbVpcId);
          instanceRuntime.setSystemSubnetId(dbSubnetId);
          instanceRuntime.setSystemVolumeId(dbVolumeId);
          return null;
        });
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict creating db instance " + dbInstanceUuid + " (will retry)");
        } else {
          logger.error("Error processing db instance create " + dbInstanceUuid, e);
        }
      }
    }
  }

  private String pickAvailabilityZone(
      final String availabilityZone,
      final List<DBSubnetView> subnets) {
    String pickedAvailabilityZone = availabilityZone;
    if ( pickedAvailabilityZone == null && !subnets.isEmpty() ) {
      final List<DBSubnetView> shuffledSubnets = Lists.newArrayList(subnets);
      Collections.shuffle(shuffledSubnets);
      pickedAvailabilityZone = shuffledSubnets.get(0).getAvailabilityZone();
    }
    return pickedAvailabilityZone;
  }

  private void dbInstanceSetupInstance() {
    for (final String dbInstanceUuid : listDbInstanceUuids(Status.creating)) {
      final DBInstanceRuntimeComposite dbInstanceRuntimeComposite;
      try {
        dbInstanceRuntimeComposite = lookupByUuid( dbInstanceUuid );
      } catch (final Exception e) {
        logger.error("Error processing db instance create " + dbInstanceUuid, e);
        continue;
      }
      final DBInstanceView dbInstance = dbInstanceRuntimeComposite.getInstance();
      final DBInstanceRuntimeView dbInstanceRuntime = dbInstanceRuntimeComposite.getRuntime();

      if ( dbInstanceRuntime.getSystemVolumeId() == null || dbInstanceRuntime.getSystemInstanceId() != null ) {
        continue; // not ready or, instance creation already started
      }

      final List<Volume> volumes =
          rdsActivityTasks.describeSystemVolumes(dbInstanceRuntime.getSystemVolumeId(), null, null);
      if (volumes == null || volumes.isEmpty() || !Sets.newHashSet("available", "in-use").contains(volumes.get(0).getStatus()) ) {
        continue; // wait until available
      }

      final String userAccount = dbInstance.getOwnerAccountNumber();
      final String dbIdentifier = dbInstance.getDisplayName();
      final String stackName = getStackName(dbInstance);
      final List<Stack> stacks = rdsActivityTasks.describeSystemStacks(stackName);
      if (stacks == null || stacks.isEmpty()) {
        final RdsEngine rdsEngine = RdsEngine.valueOf(dbInstance.getEngine());
        final String template = getTemplate(dbInstance);
        final Map<String, String> parameters = Maps.newTreeMap();
        parameters.putAll(rdsEngine.getStackParameters(dbInstance));
        parameters.put("InstanceType", Strings.trimPrefix("db.", dbInstance.getInstanceClass()));
        parameters.put("ImageId", RdsWorkerProperties.IMAGE);
        parameters.put("KeyName", RdsWorkerProperties.KEYNAME);
        //TODO:STEVE: ntp?
        parameters.put("SubnetId", dbInstanceRuntime.getSystemSubnetId());
        parameters.put("VolumeId", dbInstanceRuntime.getSystemVolumeId());
        parameters.put("VpcId", dbInstanceRuntime.getSystemVpcId());

        final String stackId = rdsActivityTasks.createSystemStack(
            stackName,
            template,
            parameters,
            Collections.singletonMap("rds:dbid", userAccount + ":" + dbIdentifier)
        );

        try {
          dbInstances.updateByView(dbInstance, instance -> {
            instance.getDbInstanceRuntime().setStackId(stackId);
            return null;
          });
        } catch (Exception e) {
          if (PersistenceExceptions.isStaleUpdate(e)) {
            logger.debug("Conflict creating db instance " + dbInstanceUuid + " (will retry)");
          } else {
            logger.error("Error processing db instance create " + dbInstanceUuid, e);
          }
        }
        continue;
      } else if ( "CREATE_FAILED".equals(stacks.get(0).getStackStatus()) ) {
        dbInstanceSetupFailure(dbInstance, "Database instance stack creation failed");
        continue;
      } else if ( !"CREATE_COMPLETE".equals(stacks.get(0).getStackStatus()) ) {
        continue;
      }

      final String instanceId = stacks.get(0).getOutputs().getMember().stream()
          .filter(output -> "InstanceId".equals(output.getOutputKey()))
          .findFirst()
          .map(Output::getOutputValue)
          .orElse(null);

      try {
        dbInstances.updateByView(dbInstance, instance -> {
          instance.getDbInstanceRuntime().setSystemInstanceId(instanceId);
          return null;
        });
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict creating db instance " + dbInstanceUuid + " (will retry)");
        } else {
          logger.error("Error processing db instance create " + dbInstanceUuid, e);
        }
      }
    }
  }

  private String getStackName(final DBInstanceView dbInstance) {
    final String userAccount = dbInstance.getOwnerAccountNumber();
    final String dbIdentifier = dbInstance.getDisplayName();
    return "rds-db-" + userAccount + "-" + dbIdentifier;
  }

  private String getTemplate(final DBInstanceView dbInstance) {
    try {
      return Resources.toString(
          Resources.getResource( RdsWorkflow.class, "rds-db-instance-" + dbInstance.getEngine() + ".yaml" ),
          Charsets.UTF_8 );
    } catch (IOException e) {
      throw Exceptions.toUndeclared(e);
    }
  }

  private void dbInstanceSetupNetwork() {
    for (final String dbInstanceUuid : listDbInstanceUuids(Status.creating)) {
      final DBInstanceRuntimeComposite dbInstanceRuntimeComposite;
      try {
        dbInstanceRuntimeComposite = lookupByUuid( dbInstanceUuid );
      } catch (final Exception e) {
        logger.error("Error processing db instance create " + dbInstanceUuid, e);
        continue;
      }
      final DBInstanceView dbInstance = dbInstanceRuntimeComposite.getInstance();
      final DBInstanceRuntimeView dbInstanceRuntime = dbInstanceRuntimeComposite.getRuntime();

      if ( dbInstanceRuntime.getSystemInstanceId() == null ) {
        continue;
      }

      final List<RunningInstancesItemType> ec2Instances =
          rdsActivityTasks.describeSystemInstances(Lists.newArrayList(dbInstanceRuntime.getSystemInstanceId()));
      if ( ec2Instances == null || ec2Instances.isEmpty() || !"running".equals(ec2Instances.get(0).getStateName()) ) {
        continue;
      }

      final AccountFullName accountFullName = AccountFullName.getInstance(dbInstance.getOwnerAccountNumber());
      final String networkInterfaceId;
      if ( dbInstanceRuntime.getUserNetworkInterfaceId() == null ) {
        final NetworkInterfaceType networkInterface =
            rdsActivityTasks.createNetworkInterface(
                accountFullName,
                dbInstanceRuntime.getUserSubnetId(),
                dbInstance.getVpcSecurityGroups() );
        networkInterfaceId = networkInterface.getNetworkInterfaceId();
        final String networkInterfacePrivateIp = networkInterface.getPrivateIpAddress();

        try {
          dbInstances.updateByView(dbInstance, instance -> {
            final DBInstanceRuntime instanceRuntime = instance.getDbInstanceRuntime();
            instanceRuntime.setUserNetworkInterfaceId(networkInterfaceId);
            instanceRuntime.setPrivateIp(networkInterfacePrivateIp);
            return null;
          });
        } catch (final Exception e) {
          logger.error("Error recording network interface id "+networkInterfaceId+" on db instance create " + dbInstanceUuid, e);
          continue;
        }

      } else {
        networkInterfaceId = dbInstanceRuntime.getUserNetworkInterfaceId();
      }

      if ( dbInstance.getPubliclyAccessible() && dbInstanceRuntime.getPublicIp()==null ) {
        // TODO:STEVE: allocate, associate, and record public ip for user eni
      }

      rdsActivityTasks.attachNetworkInterface(
          accountFullName,
          dbInstanceRuntime.getSystemInstanceId(),
          networkInterfaceId,
          1);

      try {
        dbInstances.updateByView(dbInstance, instance -> {
          instance.setState(Status.available);
          return null;
        });
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict creating db instance " + dbInstanceUuid + " (will retry)");
        } else {
          logger.error("Error processing db instance create " + dbInstanceUuid, e);
        }
      }
    }
  }

  private void dbInstanceDelete() {
    for (final String dbInstanceUuid : listDbInstanceUuids(DBInstance.Status.deleting)) {
      final DBInstanceRuntimeComposite dbInstanceRuntimeComposite;
      try {
        dbInstanceRuntimeComposite = lookupByUuid( dbInstanceUuid );
      } catch (final Exception e) {
        logger.error("Error processing db instance create " + dbInstanceUuid, e);
        continue;
      }

      if (!dbInstanceReleaseResources( dbInstanceRuntimeComposite )) {
        continue;
      }

      try {
        dbInstances.deleteByExample(DBInstance.exampleWithUuid(dbInstanceUuid));
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict deleting db instance " + dbInstanceUuid + " (will retry)");
        } else {
          logger.error("Error processing db instance delete " + dbInstanceUuid, e);
        }
      }
    }
  }

  private void dbInstanceFailureCleanup() {
    List<DBInstanceRuntimeComposite> failedDBInstances = Collections.emptyList( );
    try {
      failedDBInstances = dbInstances.list(
          null,
          Restrictions.and(
              Example.create( DBInstance.exampleWithStatus( DBInstance.Status.failed ) ),
              Restrictions.isNotNull( "dbInstanceRuntime.systemVolumeId" )
          ),
          Collections.emptyMap( ),
          Predicates.alwaysTrue( ),
          DBInstances.COMPOSITE_RUNTIME
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing failed NAT gateways for cleanup", e );
    }

    failedDBInstances.forEach( this::dbInstanceReleaseResources );
  }

  private void dbInstanceSetupFailure(
      final DBInstanceView dbInstance,
      final String reason
  ) {
    try {
      logger.info("Marking db instance " + dbInstance.getDisplayName() + " failed due to " + reason);
      dbInstances.updateByView(dbInstance, instance -> {
        instance.setState(Status.failed);
        return null;
      });
    } catch ( final Exception e ) {
      logger.error( "Error marking db instance failed", e );
    }
  }

  private boolean dbInstanceReleaseResources(
      final DBInstanceRuntimeComposite dbInstance
  ) {
    boolean releasedAll = false;

    if ( dbInstance.getRuntime().getStackId() != null ) {
      final String stackName = getStackName(dbInstance.getInstance());
      if ( rdsActivityTasks.describeSystemStacks(stackName).isEmpty() ) {
        try {
          dbInstances.updateByView(dbInstance.getInstance(), instance -> {
            final DBInstanceRuntime runtime = instance.getDbInstanceRuntime();
            runtime.setStackId(null);
            runtime.setSystemInstanceId(null);
            return null;
          });
          return dbInstanceReleaseResources(
              ImmutableDBInstanceRuntimeComposite.copyOf(dbInstance)
                  .withRuntime(ImmutableDBInstanceRuntimeView
                      .copyOf(dbInstance.getRuntime())
                      .withSystemInstanceId(null)));
        } catch ( final Exception e ) {
          logger.debug( "Error clearing system instance id after delete (will retry)", e );
        }
      } else {
        rdsActivityTasks.deleteSystemStack(stackName);
      }
    } else if ( dbInstance.getRuntime().getUserNetworkInterfaceId() != null ) {
      rdsActivityTasks.deleteNetworkInterface(
          AccountFullName.getInstance(dbInstance.getInstance().getOwnerAccountNumber()),
          dbInstance.getRuntime().getUserNetworkInterfaceId());
      try {
        dbInstances.updateByView(dbInstance.getInstance(), instance -> {
          final DBInstanceRuntime runtime = instance.getDbInstanceRuntime();
          runtime.setUserNetworkInterfaceId(null);
          runtime.setPublicIp(null);
          runtime.setPrivateIp(null);
          return null;
        });
        return dbInstanceReleaseResources(
            ImmutableDBInstanceRuntimeComposite.copyOf(dbInstance)
                .withRuntime(ImmutableDBInstanceRuntimeView
                    .copyOf(dbInstance.getRuntime())
                    .withUserNetworkInterfaceId(null)));
      } catch ( final Exception e ) {
        logger.debug( "Error clearing user network interface after delete (will retry)", e );
      }
    } else if ( dbInstance.getRuntime().getSystemVolumeId()!=null ) {
      if ( rdsActivityTasks.deleteSystemVolume(dbInstance.getRuntime().getSystemVolumeId()) ) {
        try {
          dbInstances.updateByView(dbInstance.getInstance(), instance -> {
            instance.getDbInstanceRuntime().setSystemVolumeId(null);
            return null;
          });
          return dbInstanceReleaseResources(
              ImmutableDBInstanceRuntimeComposite.copyOf(dbInstance)
                  .withRuntime(ImmutableDBInstanceRuntimeView
                      .copyOf(dbInstance.getRuntime())
                      .withSystemVolumeId(null)));
        } catch ( final Exception e ) {
          logger.debug( "Error clearing user network interface after delete (will retry)", e );
        }
      }
    } else {
      releasedAll = true;
    }

    return releasedAll;
  }

  private void dbInstanceTimeout( ) {
    List<DBInstanceView> timedOutDbInstances = Collections.emptyList( );
    try {
      timedOutDbInstances = dbInstances.list(
          null,
          Restrictions.and(
              Restrictions.or(
                  Example.create(DBInstance.exampleWithStatus(Status.creating)),
                  Example.create(DBInstance.exampleWithStatus(Status.deleting)),
                  Example.create(DBInstance.exampleWithStatus(Status.failed))
              ),
              Restrictions.lt( "lastUpdateTimestamp", new Date( System.currentTimeMillis( ) - DBInstances.EXPIRY_AGE ) )
          ),
          Collections.emptyMap( ),
          Predicates.alwaysTrue( ),
          ImmutableDBInstanceView::copyOf
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing timed out db instances", e );
    }

    for ( final DBInstanceView dbInstance : timedOutDbInstances ) {
      try {
        if ( dbInstance.getState()==Status.creating || dbInstance.getState()==Status.deleting ) {
          logger.info("Marking db instance " + dbInstance.getDisplayName() + " failed from state " + dbInstance.getState());
          dbInstances.updateByView(dbInstance, instance -> {
            instance.setState(Status.failed);
            return null;
          });
        } else {
          logger.info("Deleting db instance " + dbInstance.getDisplayName() + " with state " + dbInstance.getState());
          //TODO:STEVE: log a warning if any resources could not be cleaned up (or just keep in failed until fixed?)
          dbInstances.deleteByExample(DBInstance.exampleWithUuid(dbInstance.getNaturalId()));
        }
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict handling timeout for db instance " + dbInstance.getDisplayName() + " (will retry)");
        } else {
          logger.error("Error processing timeout for db instance " + dbInstance.getDisplayName(), e);
        }
      }
    }
  }

  private static abstract class WorkflowTask {

    private volatile int count = 0;

    private final int factor;

    private final String task;

    protected WorkflowTask(final int factor, final String task) {
      this.factor = factor;
      this.task = task;
    }

    protected final int calcFactor() {
      return factor / (int) Math.max(1, SystemClock.RATE / 1000);
    }

    protected final void perhapsWork() throws Exception {
      if (++count % calcFactor() == 0) {
        logger.trace("Running RDS workflow task: " + task);
        doWork();
        logger.trace("Completed RDS workflow task: " + task);
      }
    }

    abstract void doWork() throws Exception;
  }

  public static class RdsWorkflowEventListener implements EventListener<ClockTick> {

    private final RdsWorkflow rdsWorkflow = new RdsWorkflow(
        new PersistenceDBInstances()
    );

    public static void register() {
      Listeners.register(ClockTick.class, new RdsWorkflowEventListener());
    }

    @Override
    public void fireEvent(final ClockTick event) {
      if (Bootstrap.isOperational() &&
          Topology.isEnabledLocally(Eucalyptus.class) &&
          Topology.isEnabled(Rds.class)) {
        rdsWorkflow.doWorkflow();
      }
    }
  }
}
