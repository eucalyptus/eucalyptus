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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.eucalyptus.resources.PropertyChangeListeners;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.Ec2Client;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.Faults.CheckException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ConfigurableClass(root = "services.imaging.worker", description = "Parameters controlling image conversion service")
public class ImagingServiceProperties {
  private static Logger LOG = Logger.getLogger(ImagingServiceProperties.class);
  // CONFIGURED is in the ImagingServiceLaunchers
  @ConfigurableField(displayName = "image",
      description = "EMI containing imaging worker",
      initial = "NULL", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.EmiChangeListener.class)
  public static String IMAGE = "NULL";

  @ConfigurableField(displayName = "instance_type",
      description = "instance type for imaging worker",
      initial = "m1.small", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.InstanceTypeChangeListener.class)
  public static String INSTANCE_TYPE = "m1.small";

  @ConfigurableField(displayName = "availability_zones",
      description = "availability zones for imaging worker",
      initial = "", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.AvailabilityZonesChangeListener.class)
  public static String AVAILABILITY_ZONES = "";

  @ConfigurableField(displayName = "keyname",
      description = "keyname to use when debugging imaging worker",
      readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.KeyNameChangeListener.class)
  public static String KEYNAME = null;

  @ConfigurableField(displayName = "ntp_server",
      description = "address of the NTP server used by imaging worker",
      readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.NTPServerChangeListener.class)
  public static String NTP_SERVER = null;

  @ConfigurableField(displayName = "log_server",
      description = "address/ip of the server that collects logs from imaging wokrers",
      readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = LogServerAddressChangeListener.class)
  public static String LOG_SERVER = null;

  @ConfigurableField(displayName = "log_server_port",
      description = "UDP port that log server is listerning to",
      readonly = false, initial = "514", type = ConfigurableFieldType.KEYVALUE,
      changeListener = LogServerPortChangeListener.class)
  public static String LOG_SERVER_PORT = "514";

  @ConfigurableField(displayName = "enabled",
      description = "enabling imaging worker healthcheck", initial = "true",
      readonly = false, type = ConfigurableFieldType.BOOLEAN)
  public static Boolean HEALTHCHECK = true;

  @ConfigurableField(displayName = "expiration_days",
      description = "the days after which imaging work VMs expire",
      initial = "180", readonly = false, type = ConfigurableFieldType.KEYVALUE,
      changeListener = PropertyChangeListeners.PositiveNumberChangeListener.class)
  public static String EXPIRATION_DAYS = "180";

  @ConfigurableField(displayName = "init_script",
      description = "bash script that will be executed before service configuration and start up",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE)
  public static String INIT_SCRIPT = null;

  public static String getCredentialsString() {
    final String credStr = String.format("euca-%s:%s",
        B64.standard.encString("setup-credential"),
        EXPIRATION_DAYS);
    return credStr;
  }

  public static final String IMAGING_WORKER_STACK_NAME = "euca-internal-imaging-service";

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
    private static boolean StackCheckResult = true;

    @Override
    public boolean check() throws Exception {
      if (CloudMetadatas.isMachineImageIdentifier(IMAGE)) {
        if ( CheckCounter >= 3 && Topology.isEnabled( Eucalyptus.class )
            && Topology.isEnabled( CloudFormation.class )) {
          try {
            final Stack stack = CloudFormationClient
                .getInstance().describeStack(null, IMAGING_WORKER_STACK_NAME);
            if ("CREATE_COMPLETE".equals(stack.getStackStatus()))
              StackCheckResult = true;
            else
              StackCheckResult = false;
          } catch (final Exception ex) {
            StackCheckResult = false;
          }
          CheckCounter = 0;
        } else
          CheckCounter++;
        return StackCheckResult;
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

  public static class LogServerAddressChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(newValue))
        return;
      try {
        // check address
        InetAddress.getByName(newValue);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change log server to " + newValue + " due to: " + e.getMessage());
      }
    }
  }

  public static class LogServerPortChangeListener implements
      PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      if(t.getValue()!=null && t.getValue().equals(newValue))
        return;
      try {
        int i = Integer.parseInt(newValue);
        if (i<=0 || i>65535)
          throw new ConfigurablePropertyException("Invalid port number");
      } catch (final NumberFormatException ex) {
        throw new ConfigurablePropertyException("Invalid number");
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could not change log server port to " + newValue + " due to: " + e.getMessage());
      }
    }
  }

  static final Set<String> configuredZones = Sets.newHashSet();

  public static List<String> listConfiguredZones() throws Exception {
    if (configuredZones.size() <= 0) {
      List<String> allZones = Lists.newArrayList();
      try {
        final List<ClusterInfoType> clusters = Ec2Client
            .getInstance().describeAvailabilityZones(null, false);
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
