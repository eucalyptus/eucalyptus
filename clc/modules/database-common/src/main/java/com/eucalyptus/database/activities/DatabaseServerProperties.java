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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.DatabaseInfo;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.resources.client.AutoScalingClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HostSpecifier;

/**
 * @author Sang-Min Park
 *
 */

  @ConfigurableClass(root = "cloud.db", description = "Parameters controlling database information.", singleton = true)
 public class DatabaseServerProperties {
   private static Logger  LOG = Logger.getLogger( DatabaseServerProperties.class );
   @ConfigurableField( displayName = "db_server_enabled",
       description = "enable db server within a VM",
       initial = "false",
       readonly = false,
       type = ConfigurableFieldType.BOOLEAN,
       changeListener = EnabledChangeListener.class)
   public static boolean DB_SERVER_ENABLED = false;
   
   @ConfigurableField( displayName = "db_server_emi",
       description = "EMI containing database server",
       initial = "NULL",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = EmiChangeListener.class)
   public static String DB_SERVER_EMI = "NULL";

   @ConfigurableField( displayName = "db_server_instance_type", 
       description = "instance type for database server",
       initial = "m1.small", 
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = InstanceTypeChangeListener.class)
   public static String DB_SERVER_INSTANCE_TYPE = "m1.small";
   
   @ConfigurableField( displayName = "db_server_availability_zones", 
       description = "availability zones for database server", 
       initial = "",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = AvailabilityZonesChangeListener.class )
   public static String DB_SERVER_AVAILABILITY_ZONES = "";

   @ConfigurableField( displayName = "db_server_keyname",
       description = "keyname to use when debugging database server",
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = KeyNameChangeListener.class)
   public static String DB_SERVER_KEYNAME = null;
   

   @ConfigurableField( displayName = "db_server_ntp_server", 
       description = "address of the NTP server used by database server", 
       readonly = false,
       type = ConfigurableFieldType.KEYVALUE,
       changeListener = NTPServerChangeListener.class
       )
   public static String DB_SERVER_NTP_SERVER = null;
   
   
   public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-db-servers";
   public static String CREDENTIALS_STR = "euca-"+B64.standard.encString("setup-credential");
   private static AtomicBoolean launchLock = new AtomicBoolean();
   private static final String DB_INSTANCE_IDENTIFIER = "postgresql";
   private static final int DB_PORT = 5432;
   private static class NewDBRunner implements Callable<Boolean>{
    @Override
    public Boolean call() throws Exception {
      try{
        final String masterPassword = Crypto.generateAlphanumericId(8, "").toLowerCase();
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
          //throw e;
        } 
        
        try {
          final EnableDBInstanceEvent evt = new EnableDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
          evt.setMasterUserName(masterUserName);
          if(vmCreated)
            evt.setMasterUserPassword(masterPassword);
          else {
            final String pwd = DatabaseInfo.getDatabaseInfo().getAppendOnlyPassword();
            if(pwd != null && pwd.length()>0) {
              evt.setMasterUserPassword(masterPassword);
              LOG.debug("Master database password is set from properties");
            }
          }
          evt.setDbInstanceIdentifier(DB_INSTANCE_IDENTIFIER);
          evt.setPort(DB_PORT);
          DatabaseEventListeners.getInstance().fire(evt);
        } catch ( final Exception e) {
          LOG.error( "failed to enable remote database", e);
          throw e;
        }
      } catch (final Exception ex) { 
        final ConfigurableProperty prop = PropertyDirectory.getPropertyEntry("cloud.db.db_server_enabled");
        if (prop!=null)
          prop.setValue("false");
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
   
   public static class EmiChangeListener implements PropertyChangeListener {
     @Override
     public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
       try {
         if ( newValue instanceof String  ) {
           if(t.getValue()!=null && ! t.getValue().equals(newValue) && ((String)newValue).length()>0)
             onPropertyChange((String)newValue, null, null, null, null, null);
         }
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change EMI ID", e);
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
             onPropertyChange(null, (String)newValue, null, null, null, null);
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
             onPropertyChange(null, null, (String)newValue, null, null, null);
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
             final String[] addresses = ((String)newValue).split(",");
             if((addresses.length-1) != StringUtils.countOccurrencesOf((String) newValue, ","))
               throw new EucalyptusCloudException("Invalid address");
                 
             for(final String address : addresses){
               if(!HostSpecifier.isValid(String.format("%s.com",address)))
                 throw new EucalyptusCloudException("Invalid address");
             }
           }else{
             final String address = (String) newValue;
             if(address != null && ! address.equals("")){
               if(!HostSpecifier.isValid(String.format("%s.com", address)))
                 throw new EucalyptusCloudException("Invalid address");
             }
           }
         }else
           throw new EucalyptusCloudException("Address is not string type");
         
         onPropertyChange(null, null, null, (String) newValue, null, null);
       } catch ( final Exception e ) {
         throw new ConfigurablePropertyException("Could not change ntp server address", e);
       }
     }
   }

   private static void onPropertyChange(final String emi, final String instanceType, 
       final String keyname, final String ntpServers, String logServer, String logServerPort) throws EucalyptusCloudException{
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
         || (logServer!=null && logServer.length()>0) || (logServerPort!=null && logServerPort.length()>0) ){
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
           String newUserdata = lc.getUserData();
           if(ntpServers!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                     DatabaseServerProperties.CREDENTIALS_STR,
                     getServerUserData(ntpServers,
                         null,
                         null)));
           }
           if(logServer!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 DatabaseServerProperties.CREDENTIALS_STR,
                 getServerUserData(DatabaseServerProperties.DB_SERVER_NTP_SERVER,
                         logServer,
                         null)));
           }
           if(logServerPort!=null ){
             newUserdata = B64.standard.encString(String.format("%s\n%s",
                 DatabaseServerProperties.CREDENTIALS_STR,
                 getServerUserData(DatabaseServerProperties.DB_SERVER_NTP_SERVER,
                         null,
                         logServerPort)));
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
           
           LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
         }
       }catch(final EucalyptusCloudException ex){
         throw ex;
       }catch(final Exception ex){
         throw new EucalyptusCloudException("Unable to update the autoscaling group", ex);
       }
     }
   }
   
   static String getServerUserData(String ntpServer, String logServer, String logServerPort) {
     Map<String,String> kvMap = new HashMap<String,String>();
     if(ntpServer != null)
       kvMap.put("ntp_server", ntpServer);
     if(logServer != null)
       kvMap.put("log_server", logServer);
     if(logServerPort != null)
       kvMap.put("log_server_port", logServerPort);
     
     kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.DOMAIN));
     kvMap.put("compute_service_url", String.format("compute.%s", DNSProperties.DOMAIN));
   
     final StringBuilder sb = new StringBuilder();
     for (String key : kvMap.keySet()){
       String value = kvMap.get(key);
       sb.append(String.format("%s=%s;", key, value));
     }
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

       if(DB_SERVER_AVAILABILITY_ZONES!= null &&
           DB_SERVER_AVAILABILITY_ZONES.length()>0){
         if(DB_SERVER_AVAILABILITY_ZONES.contains(",")){
           final String[] tokens = DB_SERVER_AVAILABILITY_ZONES.split(",");
           for(final String zone : tokens){
             if(allZones.contains(zone))
               configuredZones.add(zone);
           } 
         }else{
           if(allZones.contains(DB_SERVER_AVAILABILITY_ZONES))
             configuredZones.add(DB_SERVER_AVAILABILITY_ZONES);
         }
       }else{
         configuredZones.addAll(allZones);
       }
     }
     
     return Lists.newArrayList(configuredZones);
   }

}