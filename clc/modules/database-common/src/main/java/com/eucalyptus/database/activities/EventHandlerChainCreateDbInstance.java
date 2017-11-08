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
package com.eucalyptus.database.activities;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.ServerCertificateMetadataType;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.bootstrap.DatabaseInfo;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.configurable.StaticDatabasePropertyEntry;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.resources.client.EuareClient;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

public class EventHandlerChainCreateDbInstance extends
    EventHandlerChain<NewDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainCreateDbInstance.class );

  private static final String DATABASE_VM_STACK_NAME = "euca-internal-db-service";
  private static final String SERVER_CERTIFICATE_NAME = "euca-internal-db-service";
  private static final String DEFAULT_SERVER_CERT_PATH = "/euca-internal";

  @Override
  public EventHandlerChain<NewDBInstanceEvent> build() {
    this.append(new AdmissionControl(this));
    this.append(new CreateStack(this));
    return this;
  }

  public static String getStackName(String accountId) {
    return String.format("%s-%s", DATABASE_VM_STACK_NAME , accountId);
  }

  public static String getCertificateName(String accountId) {
    return String.format("%s-%s", SERVER_CERTIFICATE_NAME, accountId);
  }
  
  public static class CreateStack extends AbstractEventHandler<NewDBInstanceEvent> {
    private String dbName;

    public CreateStack(EventHandlerChain<NewDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt) throws EventHandlerException {
      try {
        if (!Topology.isEnabled(CloudFormation.class))
          throw new EventHandlerException("CloudFormation is not enabled");
        this.dbName = evt.getDbName();
        final String accountId = getAccountByUser(evt.getUserId());
        final String certificateName = getCertificateName(accountId);
        // generate certificate
        X509Certificate kpCert = null;
        ServerCertificateMetadataType certMetadata = findCertificate(evt.getUserId());
        if (certMetadata != null) {
          LOG.debug("Certificate already exists, there is no reason to create new one");
          ServerCertificateType sType = EuareClient.getInstance().getServerCertificate(evt.getUserId(),
              certificateName);
          kpCert = X509CertHelper.pemToCertificate(sType.getCertificateBody());
        } else {
          String certPem = null;
          String pkPem = null;
          try {
            final KeyPair kp = Certs.generateKeyPair();
            kpCert = Certs.generateCertificate(kp, String
                .format("Certificate for (%s)", certificateName));
            certPem = new String(PEMFiles.getBytes(kpCert));
            pkPem = new String(PEMFiles.getBytes(kp));
          } catch (final Exception ex) {
            throw new EventHandlerException("failed generating server cert", ex);
          }
          try {
            certMetadata = EuareClient.getInstance().uploadServerCertificate(evt.getUserId(),
                certificateName, DEFAULT_SERVER_CERT_PATH, certPem, pkPem,
                null);
          } catch (final Exception ex) {
            throw new EventHandlerException("failed to upload server cert", ex);
          }
          LOG.debug("Created new certificate " + certMetadata.getServerCertificateName());
        }

        String encryptedPassword = null;
        try{
          final Cipher cipher = Ciphers.RSA_PKCS1.get();
          cipher.init(Cipher.ENCRYPT_MODE, kpCert.getPublicKey(), Crypto.getSecureRandomSupplier( ).get( ));
          byte[] bencPassword = cipher.doFinal(evt.getMasterUserPassword().getBytes());
          encryptedPassword = new String(Base64.encode(bencPassword));
        }catch(final Exception ex) {
          LOG.error("Failed to encrypt DB password");
          throw new EventHandlerException("Failed to encrypt the password");
        }

        // use CF for stack creation
        String template = loadTemplate("database-cf-template.json");
        ArrayList<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter("KeyName", DatabaseServerProperties.KEYNAME));
        params.add(new Parameter("CERTARN", certMetadata.getArn()));
        params.add(new Parameter("ImageId", DatabaseServerProperties.IMAGE));
        params.add(new Parameter("VmExpirationDays",
            DatabaseServerProperties.EXPIRATION_DAYS));
        params.add(new Parameter("InstanceType",
            DatabaseServerProperties.INSTANCE_TYPE));
        params.add(new Parameter("NtpServer", DatabaseServerProperties.NTP_SERVER));
        params.add(new Parameter("PasswordEncrypted", encryptedPassword));
        params.add(new Parameter("VolumeId", DatabaseServerProperties.VOLUME));
        if (DatabaseInfo.getDatabaseInfo().getAppendOnlyPort() != null &&
            !DatabaseInfo.getDatabaseInfo().getAppendOnlyPort().isEmpty())
          params.add(new Parameter("DBPort", Integer.toString(evt.getPort())));
        if (DatabaseServerProperties.INIT_SCRIPT != null) {
          params.add(new Parameter("InitScript", DatabaseServerProperties.INIT_SCRIPT));
        }
        List<String> zones = DatabaseServerProperties.listConfiguredZones();
        params.add(new Parameter("AvailabilityZones",
            Joiner.on(",").join( zones )));
        params.add(new Parameter("EuareServiceUrl", String.format("euare.%s",
            DNSProperties.getDomain())));
        params.add(new Parameter("ComputeServiceUrl", String.format("compute.%s",
            DNSProperties.getDomain())));
        LOG.debug("Creating CF stack for the database worker acct: " + accountId);
        CloudFormationClient.getInstance().createStack(evt.getUserId(),
            getStackName(accountId), template, params);
        LOG.debug("Done creating CF stack for the database worker acct: " + accountId);
      } catch (EventHandlerException ex) {
        throw ex;
      } catch (final Exception ex) {
        throw new EventHandlerException(ex.getMessage());
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      // set configured back to false if create failed for system user
      if (DatabaseServerProperties.REPORTING_DB_NAME.equals(this.dbName)) {
        try {
          StaticDatabasePropertyEntry.update( "com.eucalyptus.database.activities.DatabaseServerProperties.configured",
              "services.database.worker.configured", "false" );
        } catch (Exception e) {
          LOG.warn("Can't set services.database.worker.configured to false dues to: " + e.getMessage());
        }
      }
    }

    private ServerCertificateMetadataType findCertificate(String userId)
        throws EucalyptusCloudException {
      try {
        return EuareClient.getInstance().describeServerCertificate(userId,
            getCertificateName(getAccountByUser(userId)), DEFAULT_SERVER_CERT_PATH);
      } catch (Exception ex) {
        throw new EucalyptusCloudException("failed to describe server cert", ex);
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

  public static String getAccountByUser(String userId) throws EventHandlerException {
    try{
      return Accounts.lookupPrincipalByUserId(userId).getAccountNumber();
    }catch(final AuthException ex){
      throw new EventHandlerException("Failed to lookup user's account", ex);
    }
  }
  // make sure the cloud is ready to launch database server instances
  // e.g., lack of resources will keep the launcher from creating resources
  public static class AdmissionControl extends AbstractEventHandler<NewDBInstanceEvent> {
    private String dbName;
    public AdmissionControl(EventHandlerChain<NewDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt) throws EventHandlerException {
      boolean stackFound = false;
      final String userId = evt.getUserId();
      final String accountId = getAccountByUser(userId);
      String stackName = getStackName(accountId);
      this.dbName = evt.getDbName();
      try{
        Stack stack = CloudFormationClient.getInstance().describeStack(userId, stackName);
        if (stack != null) {
          stackFound = true;
        }
      }catch(final Exception ex){
        stackFound = false;
      }

      if(stackFound)
        throw new EventHandlerException("Existing stack ("+ stackName +") found");
     
      // this will stop the whole instance launch chain
      final String emi = DatabaseServerProperties.IMAGE;
      List<ImageDetails> images = null;
      try{
        images = Ec2Client.getInstance().describeImages(null, Lists.newArrayList(emi));
        if(images==null || images.size()<=0 ||! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
          throw new EventHandlerException("No such EMI is found: "+emi);
      }catch(final EventHandlerException ex){
        throw ex;
      }catch(final Exception ex){
        throw new EventHandlerException("failed to validate the db server EMI", ex);
      }
      
      final String volumeId = DatabaseServerProperties.VOLUME;
      Volume volumeToUse = null;
      try{
        List<Volume> volumes = Ec2Client.getInstance().describeVolumes(null, Lists.newArrayList(volumeId));
        if(volumes==null || volumes.size()<=0 || ! volumes.get(0).getVolumeId().toLowerCase().equals(volumeId.toLowerCase()))
          throw new EventHandlerException("No such volume id is found: " + volumeId);
        if(! "available".equals(volumes.get(0).getStatus()))
          throw new EventHandlerException("Volume is not available");
        else
          volumeToUse = volumes.get(0);
      }catch(final EventHandlerException ex) {
        throw ex;
      }catch(final Exception ex) {
        throw new EventHandlerException("failed to validate the db server volume ID", ex);
      }
      // check if volume is in a configured zone
      List<String> configuredZones = null;
      try {
        configuredZones = DatabaseServerProperties.listConfiguredZones();
      } catch (Exception e) {
        LOG.error("Can't validate AZ for volume due to: " + e.getMessage());
      }
      if (configuredZones != null && !configuredZones.contains(volumeToUse.getAvailabilityZone()))
        throw new EventHandlerException("Volume is in an AZ that is not configured for database service");
      
      List<ClusterInfoType> clusters = null;
      try{
        clusters = Ec2Client.getInstance().describeAvailabilityZones(null, true);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to validate the zones", ex);
      }
      // are there enough resources in the zone?
      final String instanceType = DatabaseServerProperties.INSTANCE_TYPE;
      int numVm = 1;
      final int capacity = findAvailableResources(clusters, volumeToUse.getAvailabilityZone(), instanceType);
      if(capacity<numVm)
        throw new EventHandlerException("not enough resource in the zone " + volumeToUse.getAvailabilityZone());
      
      // check if the keyname is configured and exists
      final String keyName = DatabaseServerProperties.KEYNAME;
      if(keyName!=null && keyName.length()>0){
        try{
          final List<DescribeKeyPairsResponseItemType> keypairs = 
              Ec2Client.getInstance().describeKeyPairs(null, Lists.newArrayList(keyName));
          if(keypairs==null || keypairs.size()<=0 || !keypairs.get(0).getKeyName().equals(keyName))
            throw new Exception();
        }catch(Exception ex){
          throw new EventHandlerException(String.format("The configured keyname %s is not found", 
              DatabaseServerProperties.KEYNAME));
        }
      }
    }
    private int findAvailableResources(final List<ClusterInfoType> clusters, final String zoneName, final String instanceType){
      // parse euca-describe-availability-zones verbose response
      // WARNING: this is not a standard API!
      
      for(int i =0; i<clusters.size(); i++){
        final ClusterInfoType cc = clusters.get(i);
        if(zoneName.equals(cc.getZoneName())){
          for(int j=i+1; j< clusters.size(); j++){
            final ClusterInfoType candidate = clusters.get(j);
            if(candidate.getZoneName()!=null && candidate.getZoneName().toLowerCase().contains(instanceType.toLowerCase())){
              //<zoneState>0002 / 0002   2    512    10</zoneState>
              final String state = candidate.getZoneState();
              final String[] tokens = state.split("/");
              if(tokens!=null && tokens.length>0){
                try{
                  String strNum = tokens[0].trim().replaceFirst("0+", "");
                  if(strNum.length()<=0)
                    strNum="0";
                  
                  return Integer.parseInt(strNum);
                }catch(final NumberFormatException ex){
                  break;
                }catch(final Exception ex){
                  break;
                }
              }
            }
          }
          break;
        }
      }
      return Integer.MAX_VALUE; // when check fails, let's assume its abundant
    }
    
    @Override
    public void rollback() {
      // set configured back to false if create failed for system user
      if (DatabaseServerProperties.REPORTING_DB_NAME.equals(this.dbName)) {
        try {
          StaticDatabasePropertyEntry.update( "com.eucalyptus.database.activities.DatabaseServerProperties.configured",
              "services.database.worker.configured", "false" );
        } catch (Exception e) {
          LOG.warn("Can't set services.database.worker.configured to false dues to: " + e.getMessage());
        }
      }
    }
  }
}
