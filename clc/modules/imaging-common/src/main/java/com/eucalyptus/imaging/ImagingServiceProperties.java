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
import com.eucalyptus.component.ServiceConfigurations;
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
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

/**
 * @author Sang-Min Park
 *
 */
@ConfigurableClass(root = "imaging",  description = "Parameters controlling image conversion service")
public class ImagingServiceProperties {
  private static Logger  LOG = Logger.getLogger( ImagingServiceProperties.class );
  // IMAGING_WORKER_ENABLED in in the ImagingServiceLaunchers
  @ConfigurableField( displayName = "imaging_worker_emi",
      description = "EMI containing imaging worker",
      initial = "NULL",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = EmiChangeListener.class)
  public static String IMAGING_WORKER_EMI = "NULL";

  @ConfigurableField( displayName = "imaging_worker_instance_type", 
      description = "instance type for imaging worker",
      initial = "m1.small", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = InstanceTypeChangeListener.class)
  public static String IMAGING_WORKER_INSTANCE_TYPE = "m1.small";
  
  @ConfigurableField( displayName = "imaging_worker_availability_zones", 
      description = "availability zones for imaging worker", 
      initial = "",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = AvailabilityZonesChangeListener.class )
  public static String IMAGING_WORKER_AVAILABILITY_ZONES = "";

  @ConfigurableField( displayName = "imaging_worker_keyname",
      description = "keyname to use when debugging imaging worker",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = KeyNameChangeListener.class)
  public static String IMAGING_WORKER_KEYNAME = null;
  
  @ConfigurableField( displayName = "imaging_worker_ntp_server", 
      description = "address of the NTP server used by imaging worker", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = NTPServerChangeListener.class
      )
  public static String IMAGING_WORKER_NTP_SERVER = null;

  @ConfigurableField( displayName = "import_task_expiration_hours",
     description = "expiration hours of import volume/instance tasks",
     readonly = false,
     initial = "168",
     type = ConfigurableFieldType.KEYVALUE,
     changeListener = ImportTaskExpirationHoursListener.class)
  public static String IMPORT_TASK_EXPIRATION_HOURS = "168"; 

  @ConfigurableField( displayName = "import_task_timeout_minutes",
     description = "expiration time in minutes of import tasks",
     readonly = false,
     initial = "180",
     type = ConfigurableFieldType.KEYVALUE,
     changeListener = ImportTaskTimeoutMinutesListener.class)
  public static String IMPORT_TASK_TIMEOUT_MINUTES = "180";  

  @ConfigurableField( displayName = "imaging_worker_log_server",
     description = "address/ip of the server that collects logs from imaging wokrers",
     readonly = false,
     type = ConfigurableFieldType.KEYVALUE,
     changeListener = LogServerAddressChangeListener.class)
  public static String IMAGING_WORKER_LOG_SERVER = null;

  @ConfigurableField( displayName = "imaging_worker_log_server_port",
      description = "UDP port that log server is listerning to",
      readonly = false,
      initial = "514",
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = LogServerPortChangeListener.class)
  public static String IMAGING_WORKER_LOG_SERVER_PORT = "514";
  
  @ConfigurableField( displayName="enabled",
      description = "enabling imaging worker healthcheck",
      initial = "true",
      readonly = false,
      type = ConfigurableFieldType.BOOLEAN)
  public static Boolean IMAGING_WORKER_HEALTHCHECK = true;
  
  
  public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-imaging-workers";
  public static String CREDENTIALS_STR = "euca-"+B64.standard.encString("setup-credential");
  
  @Provides(Imaging.class)
  @RunDuring(Bootstrap.Stage.Final)
  @DependsLocal(Imaging.class)
  public static class ImagingServicePropertyBootstrapper extends Bootstrapper.Simple {

    private static ImagingServicePropertyBootstrapper singleton;
    private static final Callable<String> imageNotConfiguredFaultRunnable =
        Faults.forComponent( Imaging.class ).havingId( 1015 ).logOnFirstRun();

    public static Bootstrapper getInstance() {
      synchronized ( ImagingServicePropertyBootstrapper.class ) {
        if ( singleton == null ) {
          singleton = new ImagingServicePropertyBootstrapper( );
          LOG.info( "Creating Imaging Bootstrapper instance." );
        } else {
          LOG.debug( "Returning Imaging Balancing Bootstrapper instance." );
        }
        }
        return singleton;
    }
    
    private static int CheckCounter = 0;
    private static boolean EmiCheckResult = true;
    @Override
    public boolean check() throws Exception {
      if ( CloudMetadatas.isMachineImageIdentifier( IMAGING_WORKER_EMI ) ) {
        if(CheckCounter == 3){
          try{
            final List<ImageDetails> emis =
                EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(IMAGING_WORKER_EMI), false);
            if(IMAGING_WORKER_EMI.equals(emis.get(0).getImageId()))
              EmiCheckResult = true;
            else 
              EmiCheckResult =  false;
          }catch(final Exception ex){
            EmiCheckResult=false;
          }
          CheckCounter = 0;
        }else
          CheckCounter++;
        return EmiCheckResult;
      } else {
        try {
          //GRZE: do this bit in the way that it allows getting the information with out needing to spelunk log files.
          final ServiceConfiguration localService = Components.lookup( Imaging.class ).getLocalServiceConfiguration( );
          final CheckException ex = Faults.failure( localService, imageNotConfiguredFaultRunnable.call( ).split("\n")[1] );
          Faults.submit( localService, localService.lookupStateMachine().getTransitionRecord(), ex );
        } catch ( Exception e ) {
          LOG.debug( e );
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
              EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
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

  public static class LogServerAddressChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        // check address
        InetAddress.getByName(newValue);
        if(t.getValue()!=null && ! t.getValue().equals(newValue) && newValue.length()>0)
          onPropertyChange(null, null, null, null, newValue, null);
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change log server to " + newValue, e);
      }
    }
  }
  
  public static class LogServerPortChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        Integer.parseInt(newValue);
        if(t.getValue()!=null && ! t.getValue().equals(newValue) && newValue.length()>0)
          onPropertyChange(null, null, null, null, null, newValue);
      }catch(final NumberFormatException ex){
        throw new ConfigurablePropertyException("Invalid number");
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change log server port to " + newValue, e);
      }
    }
  }
  
  public static class ImportTaskTimeoutMinutesListener implements PropertyChangeListener<String> {

    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        Integer.parseInt(newValue);
      }catch(final NumberFormatException ex){
        throw new ConfigurablePropertyException("Invalid number");
      }
    }
  }

  public static class ImportTaskExpirationHoursListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        Integer.parseInt(newValue);
      }catch(final NumberFormatException ex){
        throw new ConfigurablePropertyException("Invalid number");
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
  
  public static String getWorkerUserData(String ntpServer, String logServer, String logServerPort) {
    Map<String,String> kvMap = new HashMap<String,String>();
    if(ntpServer != null)
      kvMap.put("ntp_server", ntpServer);
    if(logServer != null)
      kvMap.put("log_server", logServer);
    if(logServerPort != null)
      kvMap.put("log_server_port", logServerPort);
    
    kvMap.put("imaging_service_url", String.format("imaging.%s",DNSProperties.DOMAIN));
    kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.DOMAIN));
    kvMap.put("compute_service_url",String.format("compute.%s", DNSProperties.DOMAIN));
  
  //final ServiceConfiguration dns = Topology.lookup(Dns.class);
    final List<String> dnsHosts = Lists.newArrayList(Iterables.transform(ServiceConfigurations.list(Eucalyptus.class),
        new Function<ServiceConfiguration, String>() {
          @Override
          public String apply(ServiceConfiguration arg0) {
            return arg0.getInetAddress().getHostAddress();
          }
    }));
    final List<String> enabledDns = Lists.newArrayList(Collections2.transform(Topology.enabledServices(Eucalyptus.class), 
        new Function<ServiceConfiguration, String>(){
          @Override
          public String apply(ServiceConfiguration arg0) {
            return arg0.getInetAddress().getHostAddress();
          }
    }));
    
    final StringBuilder sbDns= new StringBuilder();
    for(final String address : enabledDns){
      if(sbDns.length()<=0)
        sbDns.append(address);
      else
        sbDns.append(","+address);
    }
    for(final String address : dnsHosts){
      if(! enabledDns.contains(address)){
        if(sbDns.length()<=0)
          sbDns.append(address);
        else
          sbDns.append(","+address);
      } 
    }
    kvMap.put("dns_server", sbDns.toString());

    final StringBuilder sb = new StringBuilder();
    for (String key : kvMap.keySet()){
      String value = kvMap.get(key);
      sb.append(String.format("%s=%s;", key, value));
    }
    return sb.toString();
  }

  private static void onPropertyChange(final String emi, final String instanceType, 
      final String keyname, final String ntpServers, String logServer, String logServerPort) throws EucalyptusCloudException{
    if (!( Bootstrap.isFinished() &&
             // Topology.isEnabledLocally( Imaging.class ) &&
              Topology.isEnabled( Eucalyptus.class ) ) )
      return;
    
    // should validate the parameters
    if(emi!=null){
      try{
        final List<ImageDetails> images = 
          EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi), false);
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
            EucalyptusActivityTasks.getInstance().describeKeyPairs(Lists.newArrayList(keyname));
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
       final List<TagDescription> tags = EucalyptusActivityTasks.getInstance().describeAutoScalingTags(); 
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
          final DescribeAutoScalingGroupsResponseType resp = EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName));
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
          final LaunchConfigurationType lc = EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(lcName);

          String tmpLaunchConfigName = null;
          do{
            tmpLaunchConfigName = String.format("lc-euca-internal-imaging-%s", 
                UUID.randomUUID().toString().substring(0, 8));
          }while(tmpLaunchConfigName.equals(asgType.getLaunchConfigurationName()));

          final String newEmi = emi != null? emi : lc.getImageId();
          final String newType = instanceType != null? instanceType : lc.getInstanceType();
          String newKeyname = keyname != null ? keyname : lc.getKeyName();
          String newUserdata = lc.getUserData();
          if(ntpServers!=null ){
            newUserdata = B64.standard.encString(String.format("%s\n%s",
                    ImagingServiceProperties.CREDENTIALS_STR,
                    getWorkerUserData(ntpServers,
                        ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER,
                        ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER_PORT)));
          }
          if(logServer!=null ){
            newUserdata = B64.standard.encString(String.format("%s\n%s",
            		ImagingServiceProperties.CREDENTIALS_STR,
            		getWorkerUserData(ImagingServiceProperties.IMAGING_WORKER_NTP_SERVER,
                        logServer,
                        ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER_PORT)));
          }
          if(logServerPort!=null ){
            newUserdata = B64.standard.encString(String.format("%s\n%s",
            		ImagingServiceProperties.CREDENTIALS_STR,
                    getWorkerUserData(ImagingServiceProperties.IMAGING_WORKER_NTP_SERVER,
                        ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER,
                        logServerPort)));
          }
          try{
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(), 
                tmpLaunchConfigName, lc.getSecurityGroups().getMember().get(0), newKeyname, newUserdata);
          }catch(final Exception ex){
            LOG.warn("Failed to create temporary launch config", ex);
            throw new EucalyptusCloudException("failed to create temporary launch config", ex);
          }
          try{
            EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,
                asgType.getDesiredCapacity(), tmpLaunchConfigName);
          }catch(final Exception ex){
            LOG.warn("Failed to update the autoscaling group", ex);
            throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
          }
          try{
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(
                asgType.getLaunchConfigurationName());
          }catch(final Exception ex){
            LOG.warn("unable to delete the old launch configuration", ex);
          } 
          
          try{// new launch config with the same old name
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(), 
                asgType.getLaunchConfigurationName(), lc.getSecurityGroups().getMember().get(0), newKeyname, newUserdata);
          }catch(final Exception ex){
            throw new EucalyptusCloudException("unable to create the new launch config", ex);
          }
          
          try{
            EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,
                asgType.getDesiredCapacity(), asgType.getLaunchConfigurationName());
          }catch(final Exception ex){
            throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
          }
          
          try{
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(
                tmpLaunchConfigName);
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
  
  static final Set<String> configuredZones = Sets.newHashSet();
  public static List<String> listConfiguredZones() throws Exception{
    if(configuredZones.size()<=0){
      List<String> allZones = Lists.newArrayList();
      try{
        final List<ClusterInfoType> clusters = 
            EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
        for(final ClusterInfoType c : clusters)
          allZones.add(c.getZoneName());
      }catch(final Exception ex){
        throw new Exception("failed to lookup availability zones", ex);
      }

      if(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES!= null &&
          ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.length()>0){
        if(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.contains(",")){
          final String[] tokens = ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.split(",");
          for(final String zone : tokens){
            if(allZones.contains(zone))
              configuredZones.add(zone);
          } 
        }else{
          if(allZones.contains(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES))
            configuredZones.add(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES);
        }
      }else{
        configuredZones.addAll(allZones);
      }
    }
    
    return Lists.newArrayList(configuredZones);
  }
}
