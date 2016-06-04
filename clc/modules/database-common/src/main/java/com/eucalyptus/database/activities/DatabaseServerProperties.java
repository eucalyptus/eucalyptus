/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.database.activities;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DatabaseInfo;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.resources.PropertyChangeListeners;
import com.eucalyptus.resources.client.AutoScalingClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

// Configuration properties disabled for EUCA-12016
//@ConfigurableClass(root = "services.database.worker", description = "Parameters controlling database information.", singleton = true)
public class DatabaseServerProperties {
  private static Logger LOG = Logger.getLogger(DatabaseServerProperties.class);
  @ConfigurableField(displayName = "configured", description = "Configure DB service so a VM can be launched."
      + "If something goes south with the service there is a chance that setting it to false and back to true would solve issues",
      initial = "false", readonly = false, type = ConfigurableFieldType.BOOLEAN,
      changeListener = EnabledChangeListener.class)
  public static boolean CONFIGURED = false;

  @ConfigurableField(displayName = "image", description = "EMI containing database server",
      initial = "NULL", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.EmiChangeListener.class)
  public static String IMAGE = "NULL";

  @ConfigurableField(displayName = "volume", description = "volume containing database files",
      initial = "NULL", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = VolumeChangeListener.class)
  public static String VOLUME = "NULL";

  @ConfigurableField(displayName = "instance_type", description = "instance type for database server",
      initial = "m1.small", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.InstanceTypeChangeListener.class)
  public static String INSTANCE_TYPE = "m1.small";

  @ConfigurableField(displayName = "availability_zones", description = "availability zones for database server",
      initial = "", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.AvailabilityZonesChangeListener.class)
  public static String AVAILABILITY_ZONES = "";

  @ConfigurableField(displayName = "keyname", description = "keyname to use when debugging database server",
      readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = KeyNameChangeListener.class)
  public static String KEYNAME = null;

  @ConfigurableField(displayName = "ntp_server", description = "address of the NTP server used by database server",
      readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.NTPServerChangeListener.class)
  public static String NTP_SERVER = null;

  @ConfigurableField(displayName = "expiration_days", description = "days after which the VMs expire",
      readonly = false, initial = "180", type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.PositiveNumberChangeListener.class)
  public static String EXPIRATION_DAYS = "180";

  @ConfigurableField(displayName = "init_script", description = "bash script that will be executed before service"
      + "configuration and start up", readonly = false, type = ConfigurableFieldType.KEYVALUE)
  public static String INIT_SCRIPT = null;

  @Provides(Reporting.class)
  @RunDuring(Bootstrap.Stage.Final)
  @DependsLocal(Reporting.class)
  public static class ReportingPropertyBootstrapper extends
      DatabaseServerPropertyBootstrapper {
  }

  @Provides(CloudWatchBackend.class)
  @RunDuring(Bootstrap.Stage.Final)
  @DependsLocal(CloudWatchBackend.class)
  public static class CloudWatchPropertyBootstrapper extends
      DatabaseServerPropertyBootstrapper {
  }

  public abstract static class DatabaseServerPropertyBootstrapper extends
      Bootstrapper.Simple {
    @Override
    public boolean check() throws Exception {
      if ("localhost"
          .equals(DatabaseInfo.getDatabaseInfo().getAppendOnlyHost()))
        return true;

      String vmHost = null;
      try {
        final DatabaseInfo dbInfo = DatabaseInfo.getDatabaseInfo();
        // get the host addr
        vmHost = dbInfo.getAppendOnlyHost();
        // get the port
        final String port = dbInfo.getAppendOnlyPort();
        final String userName = dbInfo.getAppendOnlyUser();
        final String password = dbInfo.getAppendOnlyPassword();

        // get jdbc url and ping
        final String jdbcUrl = EventHandlerChainEnableVmDatabase.WaitOnDb
            .getJdbcUrlWithSsl(vmHost, Integer.parseInt(port));

        if (EventHandlerChainEnableVmDatabase.WaitOnDb.pingDatabase(jdbcUrl,
            userName, password))
          return true;
        else
          return false;
      } catch (final Exception ex) {
        LOG.warn("Error pinging append-only database at " + vmHost);
        return false;
      }
    }

    @Override
    public boolean enable() throws Exception {
      synchronized (DatabaseServerPropertyBootstrapper.class) {
        if (PersistenceContexts.remoteConnected())
          return true;
        try {
          Groovyness.run("setup_persistence_remote.groovy");
          LOG.info("Remote persistence contexts are initialized");
        } catch (final Exception ex) {
          LOG.error("Failed to setup remote persistence contexts", ex);
          return false;
        }
        return true;
      }
    }
  }

  public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-db-servers";
  public static final String REPORTING_DB_NAME = "euca-internal-remote-reporting";
  private static AtomicBoolean launchLock = new AtomicBoolean();
  private static final String DB_INSTANCE_IDENTIFIER = "postgresql";
  private static final int DB_PORT = 5432;

  private static class CreateDBRunner implements Callable<Boolean> {
    @Override
    public Boolean call() throws Exception {
      try {
        String masterPassword = DatabaseInfo.getDatabaseInfo()
            .getAppendOnlyPassword();
        if (masterPassword == null || masterPassword.length() <= 0)
          masterPassword = Crypto.generateAlphanumericId(25).toLowerCase();
        final String masterUserName = "eucalyptus";
        boolean vmCreated = false;
        try {
          // creates a random password for master db user
          final NewDBInstanceEvent evt = new NewDBInstanceEvent(Accounts
              .lookupSystemAccountByAlias(AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT).getUserId());
          evt.setMasterUserName(masterUserName);
          evt.setMasterUserPassword(masterPassword);
          evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
          evt.setPort(DB_PORT);
          evt.setDbName(REPORTING_DB_NAME);
          DatabaseEventListeners.getInstance().fire(evt);
          vmCreated = true;
        } catch (final Exception e) {
          LOG.error("failed to create a database vm", e);
          vmCreated = false;
        }

        if (!vmCreated)
          return false;

        try {
          final EnableDBInstanceEvent evt = new EnableDBInstanceEvent(Accounts
              .lookupSystemAccountByAlias(AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT).getUserId());
          evt.setMasterUserName(masterUserName);
          evt.setMasterUserPassword(masterPassword);
          evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
          evt.setPort(DB_PORT);
          DatabaseEventListeners.getInstance().fire(evt);
        } catch (final Exception e) {
          LOG.error("failed to enable remote database", e);
          throw e;
        }
      } catch (final Exception ex) {
        return false;
      } finally {
        launchLock.set(false);
      }
      LOG.info("New remote database is created");
      return true;
    }
  }

  private static class DeleteDBRunner implements Callable<Boolean> {
    @Override
    public Boolean call() throws Exception {
      try {
        final DisableDBInstanceEvent evt = new DisableDBInstanceEvent(Accounts
            .lookupSystemAccountByAlias(AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT).getUserId());
        evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
        DatabaseEventListeners.getInstance().fire(evt);
      } catch (final Exception e) {
        launchLock.set(false);
        return false;
      }
      LOG.info("Remote database is disabled");
      try {
        final DeleteDBInstanceEvent evt = new DeleteDBInstanceEvent(Accounts
            .lookupSystemAccountByAlias(AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT).getUserId());
        evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
        DatabaseEventListeners.getInstance().fire(evt);
      } catch (final Exception e) {
        LOG.error("failed to handle DeleteDbInstanceEvent", e);
        return false;
      } finally {
        launchLock.set(false);
      }
      LOG.info("Database worker stack is destroyed");
      return true;
    }
  }

  public static class KeyNameChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String keyname)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(keyname))
        return;
      if (keyname == null  || keyname.isEmpty())
        return;
      try {
        Ec2Client.getInstance().describeKeyPairs(Accounts.lookupSystemAccountByAlias(
            AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT ).getUserId( ),
            Lists.newArrayList(keyname));
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change key name due to: "
            + e.getMessage() + ". Are you using keypair that belongs to "
            + AccountIdentifiers.DATABASE_SYSTEM_ACCOUNT + " account?");
      }
    }
  }

  public static class EnabledChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        if ("false".equalsIgnoreCase(newValue)
            && "true".equalsIgnoreCase(t.getValue())) {
          // / disable vm-based database
          if (!launchLock.compareAndSet(false, true))
            throw new ConfigurablePropertyException(
                "the property is currently being updated");

          try {
            Callable<Boolean> disableDbRun = new DeleteDBRunner();
            Threads.enqueue(Eucalyptus.class, DatabaseServerProperties.class,
                disableDbRun);
          } catch (final Exception ex) {
            throw ex;
          }
        } else if ("true".equalsIgnoreCase(newValue)
            && "false".equalsIgnoreCase(t.getValue())) {
          // / enable vm-based database
          if (!launchLock.compareAndSet(false, true))
            throw new ConfigurablePropertyException(
                "the property is currently being updated");
          try {
            Callable<Boolean> newDbRun = new CreateDBRunner();
            Threads.enqueue(Eucalyptus.class, DatabaseServerProperties.class,
                newDbRun);
          } catch (final Exception ex) {
            throw ex;
          }
        } else
          ; // do nothing
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not toggle database server", e);
      }
    }
  }

  public static class VolumeChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String volumeId)
        throws ConfigurablePropertyException {
      if (t.getValue() != null && t.getValue().equals(volumeId))
        return;
      try {
        if (!volumeId.toLowerCase().startsWith("vol-"))
          throw new EucalyptusCloudException("Invalid volume id");
        final List<Volume> volumes = Ec2Client.getInstance().describeVolumes(
            null, Lists.newArrayList(volumeId));
        if (volumes == null
            || volumes.size() != 1
            || !(volumeId.equals(volumes.get(0).getVolumeId()) && "available"
                .equals(volumes.get(0).getStatus()))) {
          throw new EucalyptusCloudException("There is no volume with id "
              + volumeId + " in available status");
        }
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change VOLUME ID due to: " + e.getMessage());
      }
    }
  }

  static final Set<String> configuredZones = Sets.newHashSet();

  public static List<String> listConfiguredZones() throws Exception {
    if (configuredZones.size() <= 0) {
      List<String> allZones = Lists.newArrayList();
      try {
        final List<ClusterInfoType> clusters = Ec2Client.getInstance()
            .describeAvailabilityZones(null, false);
        for (final ClusterInfoType c : clusters)
          allZones.add(c.getZoneName());
      } catch (final Exception ex) {
        throw new Exception("failed to lookup availability zones", ex);
      }

      if (AVAILABILITY_ZONES != null && AVAILABILITY_ZONES.length() > 0) {
        if (AVAILABILITY_ZONES.contains(",")) {
          final String[] tokens = AVAILABILITY_ZONES.split(",");
          for (final String zone : tokens) {
            if (allZones.contains(zone))
              configuredZones.add(zone);
          }
        } else {
          if (allZones.contains(AVAILABILITY_ZONES))
            configuredZones.add(AVAILABILITY_ZONES);
        }
      } else {
        configuredZones.addAll(allZones);
      }
    }

    return Lists.newArrayList(configuredZones);
  }

  public static class RemoteDatabaseChecker implements EventListener<ClockTick> {
    static final int CHECK_EVERY_SECONDS = 60;
    static Date lastChecked = null;

    public static void register() {
      Listeners.register(ClockTick.class, new RemoteDatabaseChecker());
    }

    @Override
    public void fireEvent(ClockTick event) {
      if (!(Bootstrap.isOperational() && Topology
          .isEnabledLocally(Eucalyptus.class)))
        return;
      if (Topology.isEnabled(Reporting.class))
        return;

      if (lastChecked == null) {
        lastChecked = new Date();
      } else {
        int elapsedSec = (int) (((new Date()).getTime() - lastChecked.getTime()) / 1000.0);
        if (elapsedSec < CHECK_EVERY_SECONDS) {
          return;
        }
        lastChecked = new Date();
      }

      try {
        final ConfigurableProperty hostProp = PropertyDirectory
            .getPropertyEntry("services.database.appendonlyhost");
        if ("localhost".equals(hostProp.getValue()))
          return;
      } catch (final Exception ex) {
        return;
      }

      // describe autoscaling group and finds the instances
      final List<String> instances = Lists.newArrayList();
      String asgName = null;
      try {
        final List<TagDescription> tags = AutoScalingClient.getInstance()
            .describeAutoScalingTags(null);
        for (final TagDescription tag : tags) {
          if (DEFAULT_LAUNCHER_TAG.equals(tag.getValue())) {
            asgName = tag.getResourceId();
            break;
          }
        }
      } catch (final Exception ex) {
        return; // ASG not created yet; do nothing.
      }
      if (asgName == null)
        return;

      AutoScalingGroupType asgType = null;
      try {
        final DescribeAutoScalingGroupsResponseType resp = AutoScalingClient
            .getInstance().describeAutoScalingGroups(null,
                Lists.newArrayList(asgName));
        if (resp.getDescribeAutoScalingGroupsResult() != null
            && resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() != null
            && resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()
                .getMember() != null
            && resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()
                .getMember().size() > 0) {
          asgType = resp.getDescribeAutoScalingGroupsResult()
              .getAutoScalingGroups().getMember().get(0);
        }

        if (asgType.getInstances() != null
            && asgType.getInstances().getMember() != null)
          instances.addAll(Collections2.transform(asgType.getInstances()
              .getMember(), new Function<Instance, String>() {
            @Override
            public String apply(Instance arg0) {
              return arg0.getInstanceId();
            }
          }));
      } catch (final Exception ex) {
        LOG.warn("Can't find autoscaling group named " + asgName);
        return;
      }

      // get the ip address of the running instance
      final List<String> runningIps = Lists.newArrayList();
      try {
        final List<RunningInstancesItemType> ec2Instances = Ec2Client
            .getInstance().describeInstances(null, instances);
        for (final RunningInstancesItemType inst : ec2Instances) {
          if ("running".equals(inst.getStateName())) {
            runningIps.add(inst.getIpAddress());
          }
        }
      } catch (final Exception ex) {
        LOG.warn("Can't get the ip address of the running instance", ex);
        return;
      }
      if (runningIps.size() > 1) {
        LOG.warn("There are more than 1 instances running remote databases.");
      } else if (runningIps.size() == 0) {
        return;
      }

      final String instanceIp = runningIps.get(0);
      if (instanceIp == null || instanceIp.length() <= 0) {
        LOG.warn("Invalid IP address for the instance running remote databases.");
        return;
      }

      // see if the ip address matches with the property
      // if not update the property
      try {
        final ConfigurableProperty hostProp = PropertyDirectory
            .getPropertyEntry("services.database.appendonlyhost");
        final String curHost = hostProp.getValue();
        if ("localhost".equals(curHost))
          return;
        else if (!instanceIp.equals(curHost)) {
          hostProp.setValue(instanceIp);
          LOG.info("Updated the property services.database.appendonlyhost to "
              + instanceIp);
        }
      } catch (final Exception ex) {
        LOG.error(
            "Failed to update the property: services.database.appendonlyhost",
            ex);
      }
    }
  }
}