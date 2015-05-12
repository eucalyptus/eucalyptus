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
package com.eucalyptus.imaging.worker;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.ServerCertificateMetadataType;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.imaging.ImagingAdminSystemRoleProvider;
import com.eucalyptus.imaging.ImagingServiceProperties;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.EuareClient;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

/*
 * In case we want to create multiple instances(ASG) of the imaging service,
 * we should implement the additional methods here.
 */
@ConfigurableClass(root = "services.imaging.worker", description = "Parameters controlling image conversion service")
public class ImagingServiceLaunchers {
  private static Logger LOG = Logger.getLogger(ImagingServiceLaunchers.class);

  private static ImagingServiceLaunchers instance = new ImagingServiceLaunchers();

  private static Map<String, String> launchStateTable = Maps.newConcurrentMap();
  public static final String launcherId = "worker-01"; // version 4.0.0, but it
                                                       // can be any string
  public static final String SERVER_CERTIFICATE_NAME = "euca-internal-imaging-service";
  private static final String DEFAULT_SERVER_CERT_PATH = "/euca-internal";

  @ConfigurableField(displayName = "configured",
      description = "Prepare imaging service so a worker can be launched."
          + "If something goes south with the service there is a big chance that setting "
          + "it to false and back to true would solve issues.",
      initial = "false", readonly = false, type = ConfigurableFieldType.BOOLEAN,
      changeListener = EnabledChangeListener.class)
  public static Boolean CONFIGURED = false;

  private ImagingServiceLaunchers() {
  }

  public static ImagingServiceLaunchers getInstance() {
    return instance;
  }

  public boolean shouldDisable() {
    if (isLauncherLocked(launcherId)) {
      // start up in progress
      LOG.warn("Previous stack start up is still in progress");
      return false;
    }
    return stackExists();
  }

  public boolean shouldEnable() {
    if (isLauncherLocked(launcherId))
      return false;
    return !stackExists();
  }

  public boolean isWorkedEnabled() {
    try {
      if (isLauncherLocked(launcherId))
        return false;
      return stackExists();
    } catch (final Exception ex) {
      return false;
    }
  }

  private boolean stackExists() {
    try {
      Stack stack = CloudFormationClient.getInstance().describeStack(
          Accounts.lookupImagingAccount().getUserId(),
          ImagingServiceProperties.IMAGING_WORKER_STACK_NAME);
      if (stack != null) {
        LOG.debug("Found stack " + ImagingServiceProperties.IMAGING_WORKER_STACK_NAME);
        return true; // check status ?
      } else {
        LOG.debug("Imaging worker stack does not exist");
        return false;
      }
    } catch (Exception ex) {
      LOG.warn("Could not describe stack " + ImagingServiceProperties.IMAGING_WORKER_STACK_NAME);
      return false;
    }
  }

  public void enable() throws EucalyptusCloudException {
    if (!this.shouldEnable())
      throw new EucalyptusCloudException(
          "Imaging service is active or still shutting down");

    this.lockLauncher(launcherId);
    try {
      // check that imaging backend is ENABLED
      if (!Topology.isEnabled(ImagingBackend.class)) {
        // if it is not enabled there is a chance that roles and policies were not created
        ImagingAdminSystemRoleProvider roleProvider = new ImagingAdminSystemRoleProvider();
        roleProvider.ensureAccountAndRoleExists();
      }

      // check that CF is ENABLED
      if (!Topology.isEnabled(CloudFormation.class))
        throw new EucalyptusCloudException("CloudFormation is not enabled");
      //check properties
      if ("NULL".equals(ImagingServiceProperties.IMAGE))
        throw new EucalyptusCloudException("You need to set 'services.imaging.worker.image'"
            + "before enabling the service");

      // generate certificate
      ServerCertificateMetadataType metadata = createAndUploadCertificate();
      LOG.debug("Created new certificate "
          + metadata.getServerCertificateName());
      // use CF for stack creation
      String template = getInstance().loadTemplate("worker-cf-template.json");
      ArrayList<Parameter> params = new ArrayList<Parameter>();
      params.add(new Parameter("KeyName", ImagingServiceProperties.KEYNAME));
      params.add(new Parameter("CERTARN", metadata.getArn()));
      params.add(new Parameter("ImageId", ImagingServiceProperties.IMAGE));
      params.add(new Parameter("VmExpirationDays",
          ImagingServiceProperties.EXPIRATION_DAYS));
      params.add(new Parameter("InstanceType",
          ImagingServiceProperties.INSTANCE_TYPE));
      params
          .add(new Parameter("NtpServer", ImagingServiceProperties.NTP_SERVER));
      params
          .add(new Parameter("LogServer", ImagingServiceProperties.LOG_SERVER));
      params.add(new Parameter("LogServerPort",
          ImagingServiceProperties.LOG_SERVER_PORT));
      List<String> zones = ImagingServiceProperties.listConfiguredZones();
      params.add(new Parameter("NumberOfWorkers", Integer.toString( zones.size() )));
      params.add(new Parameter("AvailabilityZones",
          Joiner.on(",").join( zones )));
      params.add(new Parameter("ImagingServiceUrl", String.format("imaging.%s",
          DNSProperties.DOMAIN)));
      params.add(new Parameter("EuareServiceUrl", String.format("euare.%s",
          DNSProperties.DOMAIN)));
      params.add(new Parameter("ComputeServiceUrl", String.format("compute.%s",
          DNSProperties.DOMAIN)));
      LOG.debug("Creating CF stack for the imaging worker");
      CloudFormationClient.getInstance().createStack(Accounts.lookupImagingAccount().getUserId(),
          ImagingServiceProperties.IMAGING_WORKER_STACK_NAME, template, params);
      LOG.debug("Done creating CF stack for the imaging worker");
    } catch (final Exception ex) {
      throw new EucalyptusCloudException(ex);
    } finally {
      this.releaseLauncher(launcherId);
    }
  }

  private ServerCertificateMetadataType findCertificate()
      throws EucalyptusCloudException {
    try {
      return EuareClient.getInstance().describeServerCertificate(
          Accounts.lookupImagingAccount().getUserId(),
          SERVER_CERTIFICATE_NAME, DEFAULT_SERVER_CERT_PATH);
    } catch (Exception ex) {
      throw new EucalyptusCloudException("failed to describe server cert", ex);
    }
  }

  private ServerCertificateMetadataType createAndUploadCertificate()
      throws EucalyptusCloudException {
    ServerCertificateMetadataType existingCert = findCertificate();
    if (existingCert != null) {
      LOG.debug("Certificate already exists, there is no reason to create new one");
      return existingCert;
    }
    String certPem = null;
    String pkPem = null;
    try {
      final KeyPair kp = Certs.generateKeyPair();
      final X509Certificate kpCert = Certs.generateCertificate(kp, String
          .format("Certificate-for-imaging-workers-(%s)",
              SERVER_CERTIFICATE_NAME));
      certPem = new String(PEMFiles.getBytes(kpCert));
      pkPem = new String(PEMFiles.getBytes(kp));
    } catch (final Exception ex) {
      throw new EucalyptusCloudException("failed generating server cert", ex);
    }
    ServerCertificateMetadataType res;
    try {
      res = EuareClient.getInstance().uploadServerCertificate(
          Accounts.lookupImagingAccount().getUserId(),
          SERVER_CERTIFICATE_NAME, DEFAULT_SERVER_CERT_PATH, certPem, pkPem,
          null);
    } catch (final Exception ex) {
      throw new EucalyptusCloudException("failed to upload server cert", ex);
    }
    return res;
  }

  public void disable() throws Exception {
    if (!this.shouldDisable()) {
      LOG.warn("Imaging service instances are not found in the system");
      return;
    }
    this.lockLauncher(launcherId);

    try {
      CloudFormationClient.getInstance().deleteStack(
          Accounts.lookupImagingAccount().getUserId(),
          ImagingServiceProperties.IMAGING_WORKER_STACK_NAME);
      EuareClient.getInstance().deleteServerCertificate(
          Accounts.lookupImagingAccount().getUserId(),
          SERVER_CERTIFICATE_NAME);
    } catch (final Exception ex) {
      throw ex;
    } finally {
      this.releaseLauncher(launcherId);
    }
  }

  public void lockLauncher(final String launcherId) {
    synchronized (launchStateTable) {
      launchStateTable.put(launcherId, "ENABLING");
    }
  }

  public void releaseLauncher(final String launcherId) {
    synchronized (launchStateTable) {
      launchStateTable.remove(launcherId);
    }
  }

  public boolean isLauncherLocked(final String launcherId) {
    synchronized (launchStateTable) {
      return launchStateTable.containsKey(launcherId);
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
          ImagingServiceLaunchers.getInstance().disable();
        } else if ("true".equalsIgnoreCase(newValue)
            && ("false".equalsIgnoreCase(t.getValue())
                || t.getValue()==null
                || "".equals(t.getValue()) )) {
          ImagingServiceLaunchers.getInstance().enable();
        } else if (!("true".equalsIgnoreCase(newValue) || "false"
            .equalsIgnoreCase(newValue)))
          throw new ConfigurablePropertyException("The '" + newValue
              + "' is not a valid value.");
      } catch (final ConfigurablePropertyException ex) {
        throw ex;
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
            "Could disable/enable imaging service workers due to: "
                + e.getMessage(), e);
      }
    }
  }

  private String loadTemplate(final String resourceName) {
    try {
      return Resources.toString(
          Resources.getResource(getClass(), resourceName), Charsets.UTF_8);
    } catch (final IOException e) {
      throw Exceptions.toUndeclared(e);
    }
  }
}
