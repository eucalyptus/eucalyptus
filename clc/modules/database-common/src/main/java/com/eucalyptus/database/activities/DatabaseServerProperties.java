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

import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.util.StringUtils;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
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
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.UploadServerCertificate;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.StoredResult;
import com.eucalyptus.resources.client.AutoScalingClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.resources.client.EuareClient;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HostSpecifier;
@ConfigurableClass(root = "services.database.worker", description = "Parameters controlling database information.", singleton = true)
public class DatabaseServerProperties {
   private static Logger  LOG = Logger.getLogger( DatabaseServerProperties.class );
   @ConfigurableField( displayName = "configured",
       description = "Configure DB service so a VM can be launched. If something goes south with the service there"
          + "is a chance that setting it to false and back to true would solve issues",
       initial = "false",
       readonly = false,
       type = ConfigurableFieldType.BOOLEAN,
       changeListener = EnabledChangeListener.class)
   public static boolean CONFIGURED = false;
   
   @ConfigurableField( displayName = "image",
       description = "EMI containing database server",
       initial = "NULL",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = EmiChangeListener.class)
   public static String IMAGE = "NULL";
   
   @ConfigurableField( displayName = "volume", 
       description = "volume containing database files",
       initial = "NULL",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = VolumeChangeListener.class)
   public static String VOLUME = "NULL";

   @ConfigurableField( displayName = "instance_type", 
       description = "instance type for database server",
       initial = "m1.small", 
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = InstanceTypeChangeListener.class)
   public static String INSTANCE_TYPE = "m1.small";
   
   @ConfigurableField( displayName = "availability_zones", 
       description = "availability zones for database server", 
       initial = "",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = AvailabilityZonesChangeListener.class )
   public static String AVAILABILITY_ZONES = "";

   @ConfigurableField( displayName = "keyname",
       description = "keyname to use when debugging database server",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = KeyNameChangeListener.class)
   public static String KEYNAME = null;

   @ConfigurableField( displayName = "ntp_server", 
       description = "address of the NTP server used by database server", 
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = NTPServerChangeListener.class
       )
   public static String NTP_SERVER = null;
   
   @ConfigurableField( displayName = "expiration_days", 
       description = "days after which the VMs expire", 
       readonly = false,
       initial = "180",
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = VMExpirationDaysChangeListener.class
       )
   public static String EXPIRATION_DAYS = "180";
   
   @ConfigurableField(displayName = "init_script",
       description = "bash script that will be executed before service configuration and start up",
       readonly = false, 
       type = ConfigurableFieldType.KEYVALUE, changeListener = InitScriptChangeListener.class)
   public static String INIT_SCRIPT = null;

   @ConfigurableField(displayName = "log_server",
       description = "address/ip of the server that collects logs from database server",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = LogServerAddressChangeListener.class)
   public static String LOG_SERVER = null;

   @ConfigurableField(displayName = "log_server_port",
       description = "UDP port that log server is listerning to",
       readonly = false, initial = "514",
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = LogServerPortChangeListener.class)
   public static String LOG_SERVER_PORT = "514";

   @Provides(Reporting.class)
   @RunDuring(Bootstrap.Stage.Final)
   @DependsLocal(Reporting.class)
   public static class ReportingPropertyBootstrapper extends DatabaseServerPropertyBootstrapper {
   }
   
   @Provides(CloudWatchBackend.class)
   @RunDuring(Bootstrap.Stage.Final)
   @DependsLocal(CloudWatchBackend.class)
   public static class CloudWatchPropertyBootstrapper extends DatabaseServerPropertyBootstrapper {
   }

   public abstract static class DatabaseServerPropertyBootstrapper extends Bootstrapper.Simple {
     @Override
     public boolean check( ) throws Exception {
       if ("localhost".equals(DatabaseInfo.getDatabaseInfo().getAppendOnlyHost()))
         return true;
       
       String vmHost = null;
       try{
         final DatabaseInfo dbInfo = DatabaseInfo.getDatabaseInfo();
         // get the host addr
         vmHost = dbInfo.getAppendOnlyHost();
           // get the port
         final String port = dbInfo.getAppendOnlyPort();
         final String userName = dbInfo.getAppendOnlyUser();
         final String password = dbInfo.getAppendOnlyPassword();
         
         // get jdbc url and ping
         final String jdbcUrl =  
             EventHandlerChainEnableVmDatabase.WaitOnDb.getJdbcUrlWithSsl(vmHost, Integer.parseInt(port));
         
         if(EventHandlerChainEnableVmDatabase.WaitOnDb.pingDatabase(jdbcUrl, userName, password))
           return true;
         else
           return false;
       }catch(final Exception ex) {
           LOG.warn("Error pinging append-only database at " + vmHost);
           return false;
       }
     }
     
     @Override
     public boolean enable( ) throws Exception {
       synchronized (DatabaseServerPropertyBootstrapper.class) {
         if(PersistenceContexts.remoteConnected())
           return true;
         try {
           Groovyness.run( "setup_persistence_remote.groovy" );
           LOG.info("Remote persistence contexts are initialized");
         } catch(final Exception ex){
           LOG.error("Failed to setup remote persistence contexts", ex);
           return false;
         }
         return true;
       }
     }
   }
   
   public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-db-servers";
   private static AtomicBoolean launchLock = new AtomicBoolean();
   private static final String DB_INSTANCE_IDENTIFIER = "postgresql";
   private static final int DB_PORT = 5432;
   
   public static String getCredentialsString() {
     final String credStr = String.format("euca-%s:%s;",
         B64.standard.encString("setup-credential"), EXPIRATION_DAYS);
     return credStr;
   }
   
   private static class NewDBRunner implements Callable<Boolean>{
    @Override
    public Boolean call() throws Exception {
      try{
        String masterPassword =  DatabaseInfo.getDatabaseInfo().getAppendOnlyPassword();
        if (masterPassword == null || masterPassword.length()<=0) 
          masterPassword = Crypto.generateAlphanumericId(8, "").toLowerCase();
        final String masterUserName = "eucalyptus";
        boolean vmCreated = false;
        try {
          // creates a random password for master db user
          final NewDBInstanceEvent evt = new NewDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
          evt.setMasterUserName(masterUserName);
          evt.setMasterUserPassword(masterPassword);
          evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
          evt.setPort(DB_PORT);
          DatabaseEventListeners.getInstance().fire(evt);
          vmCreated = true;
        } catch ( final Exception e ) {
          LOG.error( "failed to create a database vm", e );
          vmCreated = false;
        }
        
        if (!vmCreated)
          return false;

        try {
          final EnableDBInstanceEvent evt = new EnableDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
          evt.setMasterUserName(masterUserName);
          evt.setMasterUserPassword(masterPassword);
          evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
          evt.setPort(DB_PORT);
          DatabaseEventListeners.getInstance().fire(evt);
        } catch ( final Exception e) {
          LOG.error( "failed to enable remote database", e);
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
   
   private static class DisableDBRunner implements Callable<Boolean>{
    @Override
    public Boolean call() throws Exception {
     try{
       final DisableDBInstanceEvent evt = new DisableDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
       evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
       DatabaseEventListeners.getInstance().fire(evt);
     }catch(final Exception e) {
       LOG.error("failed to disable remote database", e);
       final ConfigurableProperty prop = PropertyDirectory.getPropertyEntry("cloud.db.db_server_enabled");
       if (prop!=null)
         prop.setValue("true");
       return false;
     }finally{
       launchLock.set(false);
     }
     LOG.info("Remote database is disabled");
     return true;
    }
   }
   
   private static class DestroyDBRunner implements Callable<Boolean>{
     @Override
     public Boolean call() throws Exception {
       try {
         final DeleteDBInstanceEvent evt = new DeleteDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
         evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
         DatabaseEventListeners.getInstance().fire(evt);
       } catch ( final Exception e ) {
         LOG.error( "failed to handle DeleteDbInstanceEvent", e );
         return false;
       } finally {
         launchLock.set(false);
       }
       LOG.info("Database VM is destroyed");
       return true;
     }
    }
   
   public static class EnabledChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
       try{
         if("false".equals(((String) newValue).toLowerCase()) &&
             "true".equals(t.getValue())){
           /// disable vm-based database
           if (! launchLock.compareAndSet(false, true))
             throw new ConfigurablePropertyException("the property is currently being updated");
           
           try{
             Callable<Boolean> disableDbRun = new DisableDBRunner();
             Threads.enqueue(Eucalyptus.class,  DatabaseServerProperties.class, disableDbRun);
           }catch(final Exception ex){
             throw ex;
           }
         }else if ("true".equals(((String) newValue).toLowerCase()) && 
             "false".equals(t.getValue())){
           /// enable vm-based database
           if (! launchLock.compareAndSet(false, true))
             throw new ConfigurablePropertyException("the property is currently being updated");
           try{
             Callable<Boolean> newDbRun = new NewDBRunner();
             Threads.enqueue(Eucalyptus.class,  DatabaseServerProperties.class, newDbRun);
           }catch(final Exception ex){
             throw ex;
           }
         }else
           ; // do nothing
       }catch ( final Exception e ){
         throw new ConfigurablePropertyException("Could not toggle database server", e);
       }
     }
   }

  public static class LogServerAddressChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
      throws ConfigurablePropertyException {
      try {
       // check address
        InetAddress.getByName(newValue);
        if (t.getValue() != null && !t.getValue().equals(newValue) && newValue.length() > 0)
          onPropertyChange(null, null, null, null, null, newValue, null, null);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException(
         "Could not change log server to " + newValue, e);
      }
    }
  }

  public static class LogServerPortChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
       throws ConfigurablePropertyException {
      try {
         Integer.parseInt(newValue);
         if (t.getValue() != null && !t.getValue().equals(newValue) && newValue.length() > 0)
           onPropertyChange(null, null, null, null, null, null, newValue, null);
      } catch (final NumberFormatException ex) {
         throw new ConfigurablePropertyException("Invalid number");
      } catch (final Exception e) {
         throw new ConfigurablePropertyException(
             "Could not change log server port to " + newValue, e);
      }
    }
  }

   public static class EmiChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
       try {
         if ( newValue instanceof String  ) {
           if(t.getValue()!=null && ! t.getValue().equals(newValue) && ((String)newValue).length()>0)
             onPropertyChange((String)newValue, null, null, null, null, null, null, null);
         }
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change EMI ID", e);
       }
     }
   }
   
   public static class VolumeChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          if(t.getValue()!=null && ! t.getValue().equals(newValue) && ((String)newValue).length()>0)
            onPropertyChange(null, (String)newValue, null, null, null, null, null, null);
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change VOLUME ID", e);
      }
    }
   }

   public static class InstanceTypeChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
       try {
         if ( newValue instanceof String ) {
           if(newValue == null || ((String) newValue).equals(""))
             throw new EucalyptusCloudException("Instance type cannot be unset");
           if(t.getValue()!=null && ! t.getValue().equals(newValue))
             onPropertyChange(null, null, (String)newValue, null, null, null, null, null);
         }
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change instance type", e);
       }
     }
   }

   public static class KeyNameChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
       try {
         if ( newValue instanceof String ) {   
           if(t.getValue()!=null && ! t.getValue().equals(newValue))
             onPropertyChange(null, null, null, (String)newValue, null, null, null, null);
         }
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change key name", e);
       }
     }
   }
   
   public static class AvailabilityZonesChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange(ConfigurableProperty t, Object newValue)
         throws ConfigurablePropertyException {
       try{
         final String zones = (String) newValue;
         if(zones.length()==0){
           return;
         }
         
         final List<String> availabilityZones = Lists.newArrayList();
         if(zones.contains(",")){
           final String[] tokens = zones.split(",");
           if((tokens.length-1) != StringUtils.countOccurrencesOf(zones, ","))
             throw new EucalyptusCloudException("Invalid availability zones");
           for(final String zone : tokens)
             availabilityZones.add(zone);
         }else{
           availabilityZones.add(zones);
         }
         
         try{
           final List<ClusterInfoType> clusters = 
               Ec2Client.getInstance().describeAvailabilityZones(null, false);
           final List<String> clusterNames = Lists.newArrayList();
           for(final ClusterInfoType cluster : clusters){
             clusterNames.add(cluster.getZoneName());
           }
           for(final String zone : availabilityZones){
             if(!clusterNames.contains(zone))
               throw new ConfigurablePropertyException(zone +" is not found in availability zones");
           }
         }catch(final Exception ex){
           throw new ConfigurablePropertyException("Faield to check availability zones", ex);
         }
       }catch(final ConfigurablePropertyException ex){
         throw ex;
       }catch(final Exception ex){
         throw new ConfigurablePropertyException("Failed to check availability zones", ex);
       }
     }
   }

   
   public static class NTPServerChangeListener implements PropertyChangeListener<String> {
     @Override
     public void fireChange(ConfigurableProperty t, String newValue)
         throws ConfigurablePropertyException {
       try {
         if ( newValue instanceof String ) {
           if(((String) newValue).contains(",")){
             final String[] addresses = (newValue).split(",");
             if((addresses.length-1) != StringUtils.countOccurrencesOf((String) newValue, ","))
               throw new EucalyptusCloudException("Invalid address");
                 
             for(final String address : addresses){
               if(!HostSpecifier.isValid(String.format("%s.com",address)))
                 throw new EucalyptusCloudException("Invalid address");
             }
           }else{
             final String address = newValue;
             if(address != null && ! address.equals("")){
               if(!HostSpecifier.isValid(String.format("%s.com", address)))
                 throw new EucalyptusCloudException("Invalid address");
             }
           }
         }else
           throw new EucalyptusCloudException("Address is not string type");
         
         onPropertyChange(null, null, null, null, newValue, null, null, null);
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change ntp server address", e);
       }
     }
   }
   
   public static class VMExpirationDaysChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        final int newExp = Integer.parseInt(newValue);
        if(newExp <= 0)
          throw new Exception();
      }catch(final Exception ex) {
        throw new ConfigurablePropertyException("The value must be number type and bigger than 0");
      }
    }
   }

   public static class InitScriptChangeListener implements PropertyChangeListener<String> {
     @Override
     public void fireChange(ConfigurableProperty t, String newValue)
         throws ConfigurablePropertyException {
       try {
         // init script can be set to empty
         if (t.getValue() != null && !t.getValue().equals(newValue))
           onPropertyChange(null, null, null, null, null, null, null, newValue);
       } catch (final Exception e) {
         throw new ConfigurablePropertyException("Could not change init script", e);
       }
     }
   }

   private static void onPropertyChange(final String emi, final String volumeId, final String instanceType, 
       final String keyname, final String ntpServers, String logServer, String logServerPort,
       String initScript) throws EucalyptusCloudException{
     if (!( Bootstrap.isFinished() && Topology.isEnabled( Eucalyptus.class ) ) )
       return;
     
     // should validate the parameters
     if(emi!=null){
       try{
         final List<ImageDetails> images = 
           Ec2Client.getInstance().describeImages(null, Lists.newArrayList(emi));
         if(images == null || images.size()<=0)
           throw new EucalyptusCloudException("No such EMI is found in the system");
         if(! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
           throw new EucalyptusCloudException("No such EMI is found in the system");
       }catch(final EucalyptusCloudException ex){
         throw ex;
       }catch(final Exception ex){
         throw new EucalyptusCloudException("Failed to verify EMI in the system");
       }
     }
     
     if(volumeId!=null) {
       if( ! volumeId.toLowerCase().startsWith("vol-"))
         throw new EucalyptusCloudException("No such volume is found");
       
       try{
         final List<Volume> volumes = 
             Ec2Client.getInstance().describeVolumes(null, Lists.newArrayList(volumeId));
         if(! ( volumeId.equals(volumes.get(0).getVolumeId()) && "available".equals(volumes.get(0).getStatus()))) {
           throw new Exception();
         }
       }catch(final Exception ex) {
         throw new EucalyptusCloudException("No such volume id is found");
       }
     } 
     
     if(instanceType != null){
       ;
     }
     
     if(keyname != null && ! keyname.equals("")){
       try{
         final List<DescribeKeyPairsResponseItemType> keypairs = 
             Ec2Client.getInstance().describeKeyPairs(null, Lists.newArrayList(keyname));
         if(keypairs ==null || keypairs.size()<=0)
           throw new EucalyptusCloudException("No such keypair is found in the system");
         if(! keypairs.get(0).getKeyName().equals(keyname))
           throw new EucalyptusCloudException("No such keypair is found in the system");
       }catch(final EucalyptusCloudException ex){
         throw ex;
       }catch(final Exception ex){
         throw new EucalyptusCloudException("Failed to verify the keyname in the system");
       }
     }
     
     if(ntpServers != null){
       ; // already sanitized
     }
     
     // should find the asg name using the special TAG
     // then create a new launch config and replace the old one
     if((emi!=null && emi.length()>0) || (instanceType!=null && instanceType.length()>0) 
         || (keyname!=null && keyname.length()>0) || (ntpServers!=null && ntpServers.length()>0)
         || (logServer!=null && logServer.length()>0) || (logServerPort!=null && logServerPort.length()>0)
         | initScript!=null ) {
       String asgName = null;
       LOG.warn("Changing launch configuration");//TODO: remove
       try{
        final List<TagDescription> tags = AutoScalingClient.getInstance().describeAutoScalingTags(null); 
        for(final TagDescription tag : tags){
          if(DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            asgName = tag.getResourceId();
            break;
          }
        }
       }catch(final Exception ex){
         return; // ASG not created yet; do nothing.
       }
       if(asgName==null)
         return;
       
       try{
         AutoScalingGroupType asgType = null;
         try{
           final DescribeAutoScalingGroupsResponseType resp = 
               AutoScalingClient.getInstance().describeAutoScalingGroups(null, Lists.newArrayList(asgName));
           if(resp.getDescribeAutoScalingGroupsResult() != null && 
               resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()!=null &&
               resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()!=null &&
               resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().size()>0){
             asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
           }
         }catch(final Exception ex){
           LOG.warn("can't find autoscaling group named "+asgName);
           return;
         }
         
         /// Replace the parameters but use the same launch config name; otherwise "disabling" will fail
         if(asgType!=null){
           final String lcName = asgType.getLaunchConfigurationName();
           final LaunchConfigurationType lc = AutoScalingClient.getInstance().describeLaunchConfiguration(null, lcName);

           String tmpLaunchConfigName = null;
           do{
             tmpLaunchConfigName = String.format("lc-euca-internal-db-%s", 
                 UUID.randomUUID().toString().substring(0, 8));
           }while(tmpLaunchConfigName.equals(asgType.getLaunchConfigurationName()));

           final String newEmi = emi != null? emi : lc.getImageId();
           final String newType = instanceType != null? instanceType : lc.getInstanceType();
           String newKeyname = keyname != null ? keyname : lc.getKeyName();
           
           if (lc.getUserData() == null || lc.getUserData().length() < 0)
             throw new EucalyptusCloudException("ASG group for internal-db has invalid user data");
           String newUserdata = new String(Base64.decode(lc.getUserData().getBytes()));
           String encryptedPasword = null;
           String serverCertArn = null;
           int start = newUserdata.indexOf(CONFIG_COMMMENT);
           if (start < 0) {
             throw new EucalyptusCloudException("ASG group for internal-db has invalid user data");
           } else {
             int i = newUserdata.indexOf(PASSWORD_PROPERTY, start);
             int j = newUserdata.indexOf("\n", i);
             encryptedPasword = newUserdata.substring(i + PASSWORD_PROPERTY.length() + 1, j);
             i = newUserdata.indexOf(CERT_PROPERTY, start);
             j = newUserdata.indexOf("\n", i);
             serverCertArn = newUserdata.substring(i + CERT_PROPERTY.length() + 1, j);
           }
           
           if(volumeId != null) {
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 getCredentialsString(),
                 getServerUserData(volumeId, DatabaseServerProperties.NTP_SERVER,
                     DatabaseServerProperties.LOG_SERVER,
                     DatabaseServerProperties.LOG_SERVER_PORT,
                     DatabaseServerProperties.INIT_SCRIPT,
                     encryptedPasword, serverCertArn)));
           }

           if(initScript != null) {
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 getCredentialsString(),
                 getServerUserData(DatabaseServerProperties.VOLUME, NTP_SERVER,
                     DatabaseServerProperties.LOG_SERVER,
                     DatabaseServerProperties.LOG_SERVER_PORT,
                     DatabaseServerProperties.INIT_SCRIPT,
                     encryptedPasword, serverCertArn)));
           }
           
           if(ntpServers!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 getCredentialsString(),
                 getServerUserData(DatabaseServerProperties.VOLUME, ntpServers,
                     DatabaseServerProperties.LOG_SERVER,
                     DatabaseServerProperties.LOG_SERVER_PORT,
                     DatabaseServerProperties.INIT_SCRIPT,
                     encryptedPasword, serverCertArn)));
           }
           
           if(logServer!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 getCredentialsString(),
                 getServerUserData(DatabaseServerProperties.VOLUME,
                     DatabaseServerProperties.NTP_SERVER,
                     logServer,
                     DatabaseServerProperties.LOG_SERVER_PORT,
                     DatabaseServerProperties.INIT_SCRIPT,
                     encryptedPasword, serverCertArn)));
           }
           
           if(logServerPort!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 getCredentialsString(),
                 getServerUserData(DatabaseServerProperties.VOLUME, 
                     DatabaseServerProperties.NTP_SERVER,
                     DatabaseServerProperties.LOG_SERVER,
                     logServerPort,
                     DatabaseServerProperties.INIT_SCRIPT,
                     encryptedPasword, serverCertArn)));
           }
           
           try{
             AutoScalingClient.getInstance().createLaunchConfiguration(null, newEmi, newType, lc.getIamInstanceProfile(), 
                 tmpLaunchConfigName, lc.getSecurityGroups().getMember().get(0), newKeyname, newUserdata);
           }catch(final Exception ex){
             LOG.warn("Failed to create temporary launch config", ex);
             throw new EucalyptusCloudException("failed to create temporary launch config", ex);
           }
           try{
             AutoScalingClient.getInstance().updateAutoScalingGroup(null, asgName, null,
                 asgType.getDesiredCapacity(), tmpLaunchConfigName);
           }catch(final Exception ex){
             LOG.warn("Failed to update the autoscaling group", ex);
             throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
           }
           try{
             AutoScalingClient.getInstance().deleteLaunchConfiguration(null, 
                 asgType.getLaunchConfigurationName());
           }catch(final Exception ex){
             LOG.warn("unable to delete the old launch configuration", ex);
           } 
           
           try{// new launch config with the same old name
             AutoScalingClient.getInstance().createLaunchConfiguration(null, newEmi, newType, lc.getIamInstanceProfile(), 
                 asgType.getLaunchConfigurationName(), lc.getSecurityGroups().getMember().get(0), newKeyname, newUserdata);
           }catch(final Exception ex){
             throw new EucalyptusCloudException("unable to create the new launch config", ex);
           }
           
           try{
             AutoScalingClient.getInstance().updateAutoScalingGroup(null, asgName, null,
                 asgType.getDesiredCapacity(), asgType.getLaunchConfigurationName());
           }catch(final Exception ex){
             throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
           }
           
           try{
             AutoScalingClient.getInstance().deleteLaunchConfiguration(null, tmpLaunchConfigName);
           }catch(final Exception ex){
             LOG.warn("unable to delete the temporary launch configuration", ex);
           }

           // copy all tags from image to ASG
           try {
             final List<ImageDetails> images =
                 Ec2Client.getInstance().describeImages(null, Lists.newArrayList(emi));
             // image should exist at this point
             for(ResourceTag tag:images.get(0).getTagSet())
               AutoScalingClient.getInstance().createOrUpdateAutoscalingTags(null, tag.getKey(), tag.getValue(), asgName);
           } catch (final Exception ex) {
             LOG.warn("unable to propogate tags from image to ASG", ex);
           }
           
           LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
         }
       }catch(final EucalyptusCloudException ex){
         throw ex;
       }catch(final Exception ex){
         throw new EucalyptusCloudException("Unable to update the autoscaling group", ex);
       }
     }
   }
   
   static final private String CONFIG_COMMMENT = "#System generated DB worker config";
   static final private String PASSWORD_PROPERTY = "master_password_encrypted";
   static final private String CERT_PROPERTY = "server_cert_arn";

   static String getServerUserData(final String volumeId, final String ntpServer, final String logServer,
       final String logServerPort, String initScript, String masterPasswordEnc, String serverCertArn) {
     Map<String,String> kvMap = new HashMap<String,String>();
     if(volumeId != null)
       kvMap.put("volume_id", volumeId);
     if(ntpServer != null)
       kvMap.put("ntp_server", ntpServer);
     if(logServer != null)
       kvMap.put("log_server", logServer);
     if(logServerPort != null)
       kvMap.put("log_server_port", logServerPort);

     kvMap.put(PASSWORD_PROPERTY, masterPasswordEnc);
     kvMap.put(CERT_PROPERTY, serverCertArn);
     kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.DOMAIN));
     kvMap.put("compute_service_url", String.format("compute.%s", DNSProperties.DOMAIN));
   
     final StringBuilder sb = new StringBuilder("#!/bin/bash").append("\n");
     if (initScript != null && initScript.length()>0)
       sb.append(initScript);
     sb.append("\n");
     sb.append(CONFIG_COMMMENT);
     sb.append("\n");
     sb.append("mkdir -p /etc/eucalyptus-database-server/\n");
     sb.append("yum -y --disablerepo \\* --enablerepo eucalyptus-service-image install eucalyptus-database-server\n");
     sb.append("echo \"");
     for (String key : kvMap.keySet()) {
       String value = kvMap.get(key);
       sb.append(String.format("\n%s=%s", key, value));
     }
     sb.append("\n\" > /etc/eucalyptus-database-server/database-server.conf");
     sb.append("\ntouch /var/lib/eucalyptus-database-server/ntp.lock");
     sb.append("\nchown -R eucalyptus:eucalyptus /etc/eucalyptus-database-server");
     sb.append("\nservice eucalyptus-database-server start");
     return sb.toString();
   }
   
   static final Set<String> configuredZones = Sets.newHashSet();
   public static List<String> listConfiguredZones() throws Exception{
     if(configuredZones.size()<=0){
       List<String> allZones = Lists.newArrayList();
       try{
         final List<ClusterInfoType> clusters = 
             Ec2Client.getInstance().describeAvailabilityZones(null, false);
         for(final ClusterInfoType c : clusters)
           allZones.add(c.getZoneName());
       }catch(final Exception ex){
         throw new Exception("failed to lookup availability zones", ex);
       }

       if(AVAILABILITY_ZONES!= null &&
           AVAILABILITY_ZONES.length()>0){
         if(AVAILABILITY_ZONES.contains(",")){
           final String[] tokens = AVAILABILITY_ZONES.split(",");
           for(final String zone : tokens){
             if(allZones.contains(zone))
               configuredZones.add(zone);
           } 
         }else{
           if(allZones.contains(AVAILABILITY_ZONES))
             configuredZones.add(AVAILABILITY_ZONES);
         }
       }else{
         configuredZones.addAll(allZones);
       }
     }
     
     return Lists.newArrayList(configuredZones);
   }
   
   public static class RemoteDatabaseChecker implements EventListener<ClockTick> {
     static final int CHECK_EVERY_SECONDS = 60;
     static Date lastChecked = null;
     public static void register( ) {
           Listeners.register( ClockTick.class, new RemoteDatabaseChecker() );
     }

     @Override
     public void fireEvent(ClockTick event) {
       if (!( Bootstrap.isFinished() &&
                 Topology.isEnabledLocally( Eucalyptus.class ) )) 
         return;
       if ( Topology.isEnabled(Reporting.class))
         return;
       
       if (lastChecked == null ) {
         lastChecked = new Date();
       } else {
         int elapsedSec = (int)(((new Date()).getTime() - lastChecked.getTime())/1000.0);
         if(elapsedSec < CHECK_EVERY_SECONDS) {
           return;
         }
         lastChecked = new Date();
       }
       
       try{
         final ConfigurableProperty hostProp = 
             PropertyDirectory.getPropertyEntry("cloud.db.appendonlyhost");
         if("localhost".equals(hostProp.getValue()))
           return;
       }catch(final Exception ex){
         return;
       }
       
       // describe autoscaling group and finds the instances
       final List<String> instances = Lists.newArrayList();
       String asgName = null;
       try{
        final List<TagDescription> tags = AutoScalingClient.getInstance().describeAutoScalingTags(null); 
        for(final TagDescription tag : tags){
          if(DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            asgName = tag.getResourceId();
            break;
          }
        }
       }catch(final Exception ex){
         return; // ASG not created yet; do nothing.
       }
       if(asgName==null)
         return;

       AutoScalingGroupType asgType = null;
       try{
         final DescribeAutoScalingGroupsResponseType resp = 
             AutoScalingClient.getInstance().describeAutoScalingGroups(null, Lists.newArrayList(asgName));
         if(resp.getDescribeAutoScalingGroupsResult() != null && 
             resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()!=null &&
             resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()!=null &&
             resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().size()>0){
           asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
         }
         
         if(asgType.getInstances()!=null && asgType.getInstances().getMember()!=null)
           instances.addAll( Collections2.transform(asgType.getInstances().getMember(), new Function<Instance, String>(){
            @Override
            public String apply(Instance arg0) {
              return arg0.getInstanceId();
            }
           }));
       }catch(final Exception ex){
         LOG.warn("Can't find autoscaling group named "+asgName);
         return;
       }       
       
       // get the ip address of the running instance
       final List<String> runningIps = Lists.newArrayList();
       try{
         final List<RunningInstancesItemType> ec2Instances = Ec2Client.getInstance().describeInstances(null, instances );
         for(final RunningInstancesItemType inst : ec2Instances ) {
           if ("running".equals(inst.getStateName())){
             runningIps.add(inst.getIpAddress());
           }
         }
       }catch(final Exception ex){
         LOG.warn("Can't get the ip address of the running instance", ex);
         return;
       }
       if (runningIps.size()>1) {
         LOG.warn("There are more than 1 instances running remote databases."); 
       }else if (runningIps.size()==0) {
         return;
       }
       
       final String instanceIp = runningIps.get(0);
       if (instanceIp == null || instanceIp.length()<=0){
         LOG.warn("Invalid IP address for the instance running remote databases.");
         return;
       }

       // see if the ip address matches with the property
       // if not update the property
       try{
         final ConfigurableProperty hostProp = 
             PropertyDirectory.getPropertyEntry("cloud.db.appendonlyhost");
         final String curHost = hostProp.getValue();
         if("localhost".equals(curHost))
           return;
         else if ( ! instanceIp.equals(curHost)) {
           hostProp.setValue(instanceIp);
           LOG.info("Updated the property cloud.db.appendonlyhost to " + instanceIp);
         }
       }catch(final Exception ex) {
         LOG.error("Failed to update the property: cloud.db.appendonlyhost", ex);
       }
     }
   }

}