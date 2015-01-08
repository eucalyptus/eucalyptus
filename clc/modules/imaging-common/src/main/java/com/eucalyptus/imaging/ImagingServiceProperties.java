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
package com.eucalyptus.imaging;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults.CheckException;
import com.eucalyptus.component.ServiceConfiguration;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Faults;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.imaging.common.EucalyptusActivityTasks;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;
import com.google.common.collect.Sets;

@ConfigurableClass(root = "services.imaging.worker", description = "Parameters controlling image conversion service")
public class ImagingServiceProperties {
  private static Logger LOG = Logger.getLogger(ImagingServiceProperties.class);
  // CONFIGURED is in the ImagingServiceLaunchers
  @ConfigurableField(displayName = "image", description = "EMI containing imaging worker", initial = "NULL", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = EmiChangeListener.class)
  public static String IMAGE = "NULL";

  @ConfigurableField(displayName = "instance_type", description = "instance type for imaging worker", initial = "m1.small", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = InstanceTypeChangeListener.class)
  public static String INSTANCE_TYPE = "m1.small";

  @ConfigurableField(displayName = "availability_zones", description = "availability zones for imaging worker", initial = "", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = AvailabilityZonesChangeListener.class)
  public static String AVAILABILITY_ZONES = "";

  @ConfigurableField(displayName = "keyname", description = "keyname to use when debugging imaging worker", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = KeyNameChangeListener.class)
  public static String KEYNAME = null;

  @ConfigurableField(displayName = "ntp_server", description = "address of the NTP server used by imaging worker", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = NTPServerChangeListener.class)
  public static String NTP_SERVER = null;

  @ConfigurableField(displayName = "log_server", description = "address/ip of the server that collects logs from imaging wokrers", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = LogServerAddressChangeListener.class)
  public static String LOG_SERVER = null;

  @ConfigurableField(displayName = "log_server_port", description = "UDP port that log server is listerning to", readonly = false, initial = "514", type = ConfigurableFieldType.KEYVALUE, changeListener = LogServerPortChangeListener.class)
  public static String LOG_SERVER_PORT = "514";

  @ConfigurableField(displayName = "enabled", description = "enabling imaging worker healthcheck", initial = "true", readonly = false, type = ConfigurableFieldType.BOOLEAN)
  public static Boolean HEALTHCHECK = true;

  @ConfigurableField(displayName = "expiration_days", description = "the days after which imaging work VMs expire", initial = "180", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = VmExpirationDaysChangeListener.class)
  public static String EXPIRATION_DAYS = "180";

  @ConfigurableField(displayName = "init_script", description = "bash script that will be executed before service configuration and start up", readonly = false, type = ConfigurableFieldType.KEYVALUE, changeListener = InitScriptChangeListener.class)
  public static String INIT_SCRIPT = null;

  public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-imaging-workers";

  public static String getCredentialsString() {
    final String credStr = String.format("euca-%s:%s",
        B64.standard.encString("setup-credential"),
        EXPIRATION_DAYS);
    return credStr;
  }

  @Provides(ImagingBackend.class)
  @RunDuring(Bootstrap.Stage.Final)
  @DependsLocal(ImagingBackend.class)
  public static class ImagingBackendServicePropertyBootstrapper extends
      Bootstrapper.Simple {

    private static ImagingBackendServicePropertyBootstrapper singleton;
    private static final Callable<String> imageNotConfiguredFaultRunnable = Faults
        .forComponent(ImagingBackend.class).havingId(1015).logOnFirstRun();

    public static Bootstrapper getInstance() {
      synchronized (ImagingBackendServicePropertyBootstrapper.class) {
        if (singleton == null) {
          singleton = new ImagingBackendServicePropertyBootstrapper();
          LOG.info("Creating Imaging Bootstrapper instance.");
        } else {
          LOG.debug("Returning Imaging Balancing Bootstrapper instance.");
        }
      }
      return singleton;
    }

    private static int CheckCounter = 0;
    private static boolean EmiCheckResult = true;

    @Override
    public boolean check() throws Exception {
      if (CloudMetadatas.isMachineImageIdentifier(IMAGE)) {
        if ( CheckCounter >= 3 && Topology.isEnabled( Eucalyptus.class ) ) {
          try {
            final List<ImageDetails> emis = EucalyptusActivityTasks
                .getInstance().describeImages(
                    Lists.newArrayList(IMAGE), false);
            if (IMAGE.equals(emis.get(0).getImageId()))
              EmiCheckResult = true;
            else
              EmiCheckResult = false;
          } catch (final Exception ex) {
            EmiCheckResult = false;
          }
          CheckCounter = 0;
        } else
          CheckCounter++;
        return EmiCheckResult;
      } else {
        try {
          // GRZE: do this bit in the way that it allows getting the
          // information with out needing to spelunk log files.
          final ServiceConfiguration localService = Components.lookup(
              ImagingBackend.class).getLocalServiceConfiguration();
          final CheckException ex = Faults.failure(localService,
              imageNotConfiguredFaultRunnable.call().split("\n")[1]);
          Faults.submit(localService, localService.lookupStateMachine()
              .getTransitionRecord(), ex);
        } catch (Exception e) {
          LOG.debug(e);
        }
        return false;
      }
    }

    @Override
    public boolean enable() throws Exception {
      if (!super.enable())
        return false;

      return true;
    }
  }

  public static class AvailabilityZonesChangeListener implements
      PropertyChangeListener {

    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        final String zones = (String) newValue;
        if (zones.length() == 0) {
          return;
        }

        final List<String> availabilityZones = Lists.newArrayList();
        if (zones.contains(",")) {
          final String[] tokens = zones.split(",");
          if ((tokens.length - 1) != StringUtils.countOccurrencesOf(zones, ","))
            throw new EucalyptusCloudException("Invalid availability zones");
          for (final String zone : tokens)
            availabilityZones.add(zone);
        } else {
          availabilityZones.add(zones);
        }

        try {
          final List<ClusterInfoType> clusters = EucalyptusActivityTasks
              .getInstance().describeAvailabilityZones(false);
          final List<String> clusterNames = Lists.newArrayList();
          for (final ClusterInfoType cluster : clusters) {
            clusterNames.add(cluster.getZoneName());
          }
          for (final String zone : availabilityZones) {
            if (!clusterNames.contains(zone))
              throw new ConfigurablePropertyException(zone
                  + " is not found in availability zones");
          }
        } catch (final Exception ex) {
          throw new ConfigurablePropertyException(
              "Faield to check availability zones", ex);
        }
      } catch (final ConfigurablePropertyException ex) {
        throw ex;
      } catch (final Exception ex) {
        throw new ConfigurablePropertyException(
            "Failed to check availability zones", ex);
      }
    }
  }

  public static class LogServerAddressChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        // check address
        InetAddress.getByName(newValue);
        if (t.getValue() != null && !t.getValue().equals(newValue)
            && newValue.length() > 0)
          onPropertyChange(null, null, null, null, newValue, null, null);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change log server to " + newValue, e);
      }
    }
  }

  public static class LogServerPortChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        Integer.parseInt(newValue);
        if (t.getValue() != null && !t.getValue().equals(newValue)
            && newValue.length() > 0)
          onPropertyChange(null, null, null, null, null, newValue, null);
      } catch (final NumberFormatException ex) {
        throw new ConfigurablePropertyException("Invalid number");
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change log server port to " + newValue, e);
      }
    }
  }

  public static class InitScriptChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        // init script can be empty
        if (t.getValue() != null && !t.getValue().equals(newValue))
          onPropertyChange(null, null, null, null, null, null, (String) newValue);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change init script", e);
      }
    }
  }

  public static class EmiChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        if (t.getValue() != null && !t.getValue().equals(newValue)
            && ((String) newValue).length() > 0)
          onPropertyChange((String) newValue, null, null, null, null, null, null);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change EMI ID", e);
      }
    }
  }

  public static class InstanceTypeChangeListener implements
      PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        if (newValue instanceof String) {
          if (newValue == null || ((String) newValue).equals(""))
            throw new EucalyptusCloudException("Instance type cannot be unset");
          if (t.getValue() != null && !t.getValue().equals(newValue))
            onPropertyChange(null, (String) newValue, null, null, null, null, null);
        }
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change instance type", e);
      }
    }
  }

  public static class KeyNameChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        if (newValue instanceof String) {
          if (t.getValue() != null && !t.getValue().equals(newValue))
            onPropertyChange(null, null, (String) newValue, null, null, null, null);
        }
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change key name", e);
      }
    }
  }

  public static class NTPServerChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        if (newValue instanceof String) {
          if (((String) newValue).contains(",")) {
            final String[] addresses = ((String) newValue).split(",");
            if ((addresses.length - 1) != StringUtils.countOccurrencesOf(
                (String) newValue, ","))
              throw new EucalyptusCloudException("Invalid address");

            for (final String address : addresses) {
              if (!HostSpecifier.isValid(String.format("%s.com", address)))
                throw new EucalyptusCloudException("Invalid address");
            }
          } else {
            final String address = (String) newValue;
            if (address != null && !address.equals("")) {
              if (!HostSpecifier.isValid(String.format("%s.com", address)))
                throw new EucalyptusCloudException("Invalid address");
            }
          }
        } else
          throw new EucalyptusCloudException("Address is not string type");

        onPropertyChange(null, null, null, (String) newValue, null, null, null);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change ntp server address", e);
      }
    }
  }

  public static class VmExpirationDaysChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        final int newExp = Integer.parseInt(newValue);
        if (newExp <= 0)
          throw new Exception();
      } catch (final Exception ex) {
        throw new ConfigurablePropertyException(
            "The value must be number type and bigger than 0");
      }
    }
  }

  public static String getWorkerUserData(String ntpServer, String logServer,
      String logServerPort, String initScript) {
    Map<String, String> kvMap = new HashMap<String, String>();
    if (ntpServer != null)
      kvMap.put("ntp_server", ntpServer);
    if (logServer != null)
      kvMap.put("log_server", logServer);
    if (logServerPort != null)
      kvMap.put("log_server_port", logServerPort);

    kvMap.put("imaging_service_url",
        String.format("imaging.%s", DNSProperties.DOMAIN));
    kvMap.put("euare_service_url",
        String.format("euare.%s", DNSProperties.DOMAIN));
    kvMap.put("compute_service_url",
        String.format("compute.%s", DNSProperties.DOMAIN));

    final StringBuilder sb = new StringBuilder("#!/bin/bash").append("\n");
    if (initScript != null && initScript.length()>0)
      sb.append(initScript);
    sb.append("\n#System generated Imaging worker config\n");
    sb.append("mkdir -p /etc/eucalyptus-imaging-worker/\n");
    sb.append("yum -y --disablerepo \\* --enablerepo eucalyptus-service-image install eucalyptus-imaging-worker\n");
    sb.append("echo \"");
    for (String key : kvMap.keySet()) {
      String value = kvMap.get(key);
      sb.append(String.format("\n%s=%s", key, value));
    }
    sb.append("\" > /etc/eucalyptus-imaging-worker/imaging-worker.conf");
    sb.append("\ntouch /var/lib/eucalyptus-imaging-worker/ntp.lock");
    sb.append("\nchown -R imaging-worker:imaging-worker /etc/eucalyptus-imaging-worker");
    sb.append("\nservice eucalyptus-imaging-worker start");
    return sb.toString();
  }

  private static void onPropertyChange(final String emi,
      final String instanceType, final String keyname, final String ntpServers,
      String logServer, String logServerPort, String initScript) throws EucalyptusCloudException {
    if (!(Bootstrap.isFinished() &&
    // Topology.isEnabledLocally( Imaging.class ) &&
    Topology.isEnabled(Eucalyptus.class)))
      return;

    // should validate the parameters
    if (emi != null) {
      try {
        final List<ImageDetails> images = EucalyptusActivityTasks.getInstance()
            .describeImages(Lists.newArrayList(emi), false);
        if (images == null || images.size() <= 0)
          throw new EucalyptusCloudException(
              "No such EMI is found in the system");
        if (!images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
          throw new EucalyptusCloudException(
              "No such EMI is found in the system");
      } catch (final EucalyptusCloudException ex) {
        throw ex;
      } catch (final Exception ex) {
        throw new EucalyptusCloudException("Failed to verify EMI in the system");
      }
    }

    if (instanceType != null) {
      ;
    }

    if (keyname != null && !keyname.equals("")) {
      try {
        final List<DescribeKeyPairsResponseItemType> keypairs = EucalyptusActivityTasks
            .getInstance().describeKeyPairs(Lists.newArrayList(keyname));
        if (keypairs == null || keypairs.size() <= 0)
          throw new EucalyptusCloudException(
              "No such keypair is found in the system");
        if (!keypairs.get(0).getKeyName().equals(keyname))
          throw new EucalyptusCloudException(
              "No such keypair is found in the system");
      } catch (final EucalyptusCloudException ex) {
        throw ex;
      } catch (final Exception ex) {
        throw new EucalyptusCloudException(
            "Failed to verify the keyname in the system");
      }
    }

    if (ntpServers != null) {
      ; // already sanitized
    }

    // should find the asg name using the special TAG
    // then create a new launch config and replace the old one

    if ((emi != null && emi.length() > 0)
        || (instanceType != null && instanceType.length() > 0)
        || (keyname != null && keyname.length() > 0)
        || (ntpServers != null && ntpServers.length() > 0)
        || (logServer != null && logServer.length() > 0)
        || (logServerPort != null && logServerPort.length() > 0)
        || (initScript != null)) {
      String asgName = null;
      LOG.warn("Changing launch configuration");// TODO: remove
      try {
        final List<TagDescription> tags = EucalyptusActivityTasks.getInstance()
            .describeAutoScalingTags();
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

      try {
        AutoScalingGroupType asgType = null;
        try {
          final DescribeAutoScalingGroupsResponseType resp = EucalyptusActivityTasks
              .getInstance().describeAutoScalingGroups(
                  Lists.newArrayList(asgName));
          if (resp.getDescribeAutoScalingGroupsResult() != null
              && resp.getDescribeAutoScalingGroupsResult()
                  .getAutoScalingGroups() != null
              && resp.getDescribeAutoScalingGroupsResult()
                  .getAutoScalingGroups().getMember() != null
              && resp.getDescribeAutoScalingGroupsResult()
                  .getAutoScalingGroups().getMember().size() > 0) {
            asgType = resp.getDescribeAutoScalingGroupsResult()
                .getAutoScalingGroups().getMember().get(0);
          }
        } catch (final Exception ex) {
          LOG.warn("can't find autoscaling group named " + asgName);
          return;
        }

        // / Replace the parameters but use the same launch config name;
        // otherwise "disabling" will fail
        if (asgType != null) {
          final String lcName = asgType.getLaunchConfigurationName();
          final LaunchConfigurationType lc = EucalyptusActivityTasks
              .getInstance().describeLaunchConfiguration(lcName);

          String tmpLaunchConfigName = null;
          do {
            tmpLaunchConfigName = String.format("lc-euca-internal-imaging-%s",
                UUID.randomUUID().toString().substring(0, 8));
          } while (tmpLaunchConfigName.equals(asgType
              .getLaunchConfigurationName()));

          final String newEmi = emi != null ? emi : lc.getImageId();
          final String newType = instanceType != null ? instanceType : lc
              .getInstanceType();
          String newKeyname = keyname != null ? keyname : lc.getKeyName();
          String newUserdata = lc.getUserData();
          if (ntpServers != null) {
            newUserdata = B64.standard.encString(String.format(
                "%s\n%s",
                getCredentialsString(),
                getWorkerUserData(ntpServers,
                    ImagingServiceProperties.LOG_SERVER,
                    ImagingServiceProperties.LOG_SERVER_PORT,
                    ImagingServiceProperties.INIT_SCRIPT)));
          }
          if (logServer != null) {
            newUserdata = B64.standard.encString(String.format(
                "%s\n%s",
                getCredentialsString(),
                getWorkerUserData(
                    ImagingServiceProperties.NTP_SERVER,
                    logServer,
                    ImagingServiceProperties.LOG_SERVER_PORT,
                    ImagingServiceProperties.INIT_SCRIPT)));
          }
          if (logServerPort != null) {
            newUserdata = B64.standard.encString(String.format(
                "%s\n%s",
                getCredentialsString(),
                getWorkerUserData(
                    ImagingServiceProperties.NTP_SERVER,
                    ImagingServiceProperties.LOG_SERVER,
                    logServerPort, ImagingServiceProperties.INIT_SCRIPT)));
          }
          if (initScript != null) {
            newUserdata = B64.standard.encString(String.format(
                "%s\n%s",
                getCredentialsString(),
                getWorkerUserData(
                    ImagingServiceProperties.NTP_SERVER,
                    ImagingServiceProperties.LOG_SERVER,
                    ImagingServiceProperties.LOG_SERVER_PORT,
                    initScript)));
          }
          try {
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(
                newEmi, newType, lc.getIamInstanceProfile(),
                tmpLaunchConfigName, lc.getSecurityGroups().getMember().get(0),
                newKeyname, newUserdata);
          } catch (final Exception ex) {
            LOG.warn("Failed to create temporary launch config", ex);
            throw new EucalyptusCloudException(
                "failed to create temporary launch config", ex);
          }
          try {
            EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(
                asgName, null, asgType.getDesiredCapacity(),
                tmpLaunchConfigName);
          } catch (final Exception ex) {
            LOG.warn("Failed to update the autoscaling group", ex);
            throw new EucalyptusCloudException(
                "failed to update the autoscaling group", ex);
          }
          try {
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(
                asgType.getLaunchConfigurationName());
          } catch (final Exception ex) {
            LOG.warn("unable to delete the old launch configuration", ex);
          }

          try {// new launch config with the same old name
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(
                newEmi, newType, lc.getIamInstanceProfile(),
                asgType.getLaunchConfigurationName(),
                lc.getSecurityGroups().getMember().get(0), newKeyname,
                newUserdata);
          } catch (final Exception ex) {
            throw new EucalyptusCloudException(
                "unable to create the new launch config", ex);
          }

          try {
            EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(
                asgName, null, asgType.getDesiredCapacity(),
                asgType.getLaunchConfigurationName());
          } catch (final Exception ex) {
            throw new EucalyptusCloudException(
                "failed to update the autoscaling group", ex);
          }

          try {
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(
                tmpLaunchConfigName);
          } catch (final Exception ex) {
            LOG.warn("unable to delete the temporary launch configuration", ex);
          }

          // copy all tags from image to ASG
          try {
            final List<ImageDetails> images = EucalyptusActivityTasks.getInstance().describeImages(
                  Lists.newArrayList(emi), false);
            // image should exist at this point
            for(ResourceTag tag:images.get(0).getTagSet())
              EucalyptusActivityTasks.getInstance().createOrUpdateAutoscalingTags(tag.getKey(), tag.getValue(), asgName);
          } catch (final Exception ex) {
            LOG.warn("unable to propogate tags from image to ASG", ex);
          }
          LOG.debug(String
              .format("autoscaling group '%s' was updated", asgName));
        }
      } catch (final EucalyptusCloudException ex) {
        throw ex;
      } catch (final Exception ex) {
        throw new EucalyptusCloudException(
            "Unable to update the autoscaling group", ex);
      }
    }
  }

  static final Set<String> configuredZones = Sets.newHashSet();

  public static List<String> listConfiguredZones() throws Exception {
    if (configuredZones.size() <= 0) {
      List<String> allZones = Lists.newArrayList();
      try {
        final List<ClusterInfoType> clusters = EucalyptusActivityTasks
            .getInstance().describeAvailabilityZones(false);
        for (final ClusterInfoType c : clusters)
          allZones.add(c.getZoneName());
      } catch (final Exception ex) {
        throw new Exception("failed to lookup availability zones", ex);
      }

      if (ImagingServiceProperties.AVAILABILITY_ZONES != null
          && ImagingServiceProperties.AVAILABILITY_ZONES
              .length() > 0) {
        if (ImagingServiceProperties.AVAILABILITY_ZONES
            .contains(",")) {
          final String[] tokens = ImagingServiceProperties.AVAILABILITY_ZONES
              .split(",");
          for (final String zone : tokens) {
            if (allZones.contains(zone))
              configuredZones.add(zone);
          }
        } else {
          if (allZones
              .contains(ImagingServiceProperties.AVAILABILITY_ZONES))
            configuredZones
                .add(ImagingServiceProperties.AVAILABILITY_ZONES);
        }
      } else {
        configuredZones.addAll(allZones);
      }
    }

    return Lists.newArrayList(configuredZones);
  }
}
