/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.imaging.ImagingServiceProperties;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.Ec2Client;
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
    return ImagingServiceProperties.stackExists(false);
  }

  public boolean shouldEnable() {
    if (isLauncherLocked(launcherId))
      return false;
    return !ImagingServiceProperties.stackExists(false);
  }

  public boolean isWorkedEnabled() {
    try {
      if (isLauncherLocked(launcherId))
        return false;
      return ImagingServiceProperties.stackExists(false);
    } catch (final Exception ex) {
      return false;
    }
  }

  public void enable() throws EucalyptusCloudException {
    if (!this.shouldEnable())
      throw new EucalyptusCloudException(
          "Imaging service is active or still shutting down");

    this.lockLauncher(launcherId);
    try {
      // check that CF is ENABLED
      if (!Topology.isEnabled(CloudFormation.class))
        throw new EucalyptusCloudException("CloudFormation is not enabled");
      //check properties
      if ("NULL".equals(ImagingServiceProperties.IMAGE))
        throw new EucalyptusCloudException("You need to set 'services.imaging.worker.image'"
            + "before enabling the service");
      UserPrincipal imagingUser = Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT );
      // create default VPC if needed
      if (Networking.getInstance().supports( NetworkingFeature.Vpc )) {
        if (!Ec2Client.getInstance().hasDefaultVPC(imagingUser.getUserId())) {
            Ec2Client.getInstance().createDefaultVPC(imagingUser.getAccountNumber());
        }
      }
      // generate certificate
      ServerCertificateMetadataType metadata = createAndUploadCertificate();
      // use CF for stack creation
      String template = getInstance().loadTemplate("worker-cf-template.json");
      ArrayList<Parameter> params = new ArrayList<Parameter>();
      params.add(new Parameter("KeyName", ImagingServiceProperties.KEYNAME));
      params.add(new Parameter("CERTARN", metadata.getArn()));
      params.add(new Parameter("ImageId", ImagingServiceProperties.IMAGE));
      if (ImagingServiceProperties.INIT_SCRIPT != null)
        params.add(new Parameter("InitScript", ImagingServiceProperties.INIT_SCRIPT));
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
          DNSProperties.getDomain())));
      params.add(new Parameter("EuareServiceUrl", String.format("euare.%s",
          DNSProperties.getDomain())));
      params.add(new Parameter("ComputeServiceUrl", String.format("compute.%s",
          DNSProperties.getDomain())));
      LOG.debug("Creating CF stack for the imaging worker");
      CloudFormationClient.getInstance().createStack(
          imagingUser.getUserId( ),
          ImagingServiceProperties.IMAGING_WORKER_STACK_NAME, template, params);
      LOG.debug("Done creating CF stack for the imaging worker");
    } catch (final Exception ex) {
      LOG.error(ex);
      throw new EucalyptusCloudException(ex);
    } finally {
      this.releaseLauncher(launcherId);
    }
  }

  private ServerCertificateMetadataType findCertificate()
      throws EucalyptusCloudException {
    try {
      return EuareClient.getInstance().describeServerCertificate(
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
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
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
          SERVER_CERTIFICATE_NAME, DEFAULT_SERVER_CERT_PATH, certPem, pkPem,
          null);
      LOG.debug("Created new certificate " + res.getServerCertificateName());
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
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
          ImagingServiceProperties.IMAGING_WORKER_STACK_NAME);
      EuareClient.getInstance().deleteServerCertificate(
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
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
            "Could not disable/enable imaging service workers due to: "
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
