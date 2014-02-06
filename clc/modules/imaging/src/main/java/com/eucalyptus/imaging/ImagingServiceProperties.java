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

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.eucalyptus.autoscaling.common.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.LaunchConfigurationType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

/**
 * @author Sang-Min Park
 *
 */
@ConfigurableClass(root = "imaging",  description = "Parameters controlling image conversion service")
public class ImagingServiceProperties {
  private static Logger  LOG = Logger.getLogger( ImagingServiceProperties.class );

  @ConfigurableField( displayName="enabled",
      description = "enabling imaging service",
      initial = "false",
      readonly = false,
      type = ConfigurableFieldType.BOOLEAN,
      changeListener = EnabledChangeListener.class)
  public static Boolean IMAGING_SERVICE_ENABLED = false;
  
  @ConfigurableField( displayName = "imaging_emi",
      description = "EMI containing imaging service",
      initial = "NULL",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = EmiChangeListener.class)
  public static String IMAGING_EMI = "NULL";

  @ConfigurableField( displayName = "imaging_instance_type", 
      description = "instance type for imaging instances",
      initial = "m1.small", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = InstanceTypeChangeListener.class)
  public static String IMAGING_INSTANCE_TYPE = "m1.small";

  @ConfigurableField( displayName = "imaging_vm_keyname",
      description = "keyname to use when debugging imager VM",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = KeyNameChangeListener.class)
  public static String IMAGING_VM_KEYNAME = null;
  
  @ConfigurableField( displayName = "imaging_vm_ntp_server", 
      description = "the address of the NTP server used by imaging VMs", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = NTPServerChangeListener.class
      )
  public static String IMAGING_VM_NTP_SERVER = null;
  
  @Provides(Imaging.class)
  @RunDuring(Bootstrap.Stage.Final)
  @DependsLocal(Imaging.class)
  public static class ImagingServicePropertyBootstrapper extends Bootstrapper.Simple {

    private static ImagingServicePropertyBootstrapper singleton;

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
    
    @Override
    public boolean check() throws Exception {
      if ( IMAGING_EMI != null && IMAGING_EMI.startsWith("emi-") ) {
        return true;
      } else {
        //TODO: the name of the service is TBD, change message later
        LOG.debug("Imaging service EMI property is unset.  \"\n" +
            "              + \"Use euca-modify-property -p imaging.imaging_emi=<imaging service emi> \"\n" +
            "              + \"where the emi should point to the image provided in the eucalyptus-TBD package.\" ");
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

  public static class EnabledChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try{
        if( newValue instanceof String) {
          if("true".equals(((String) newValue).toLowerCase()) && 
              "false".equals(t.getValue())){
            try{
              if(ImagingServiceLaunchers.getInstance().shouldEnable())
                ImagingServiceLaunchers.getInstance().enable();
              else{
                throw new ConfigurablePropertyException("cannot enable");
              }
            }catch(ConfigurablePropertyException ex){
              throw ex;
            }catch(Exception ex){
              throw ex;
            }
          }else if("false".equals(((String) newValue).toLowerCase()) &&
              "true".equals(t.getValue())){
            try{
              if(ImagingServiceLaunchers.getInstance().shouldDisable())
                ImagingServiceLaunchers.getInstance().disable();
              else{
                throw new ConfigurablePropertyException("cannot disable");
              }
            }catch(ConfigurablePropertyException ex){
              throw ex;
            }catch(Exception ex){
              throw ex;
            }
          }
        }
      }catch(final ConfigurablePropertyException ex){
        throw ex;
      }catch ( final Exception e ){
        throw new ConfigurablePropertyException("Could not enable/disable imaging service", e);
      }
    }
  }
  
  public static class EmiChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          if(t.getValue()!=null && ! t.getValue().equals(newValue))
            onPropertyChange((String)newValue, null, null);
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
            onPropertyChange(null, (String)newValue, null);
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
            onPropertyChange(null, null, (String)newValue);
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change key name", e);
      }
    }
  }

  private static void onPropertyChange(final String emi, final String instanceType, final String keyname) throws EucalyptusCloudException{
    if (!( Bootstrap.isFinished() &&
             // Topology.isEnabledLocally( Imaging.class ) &&
              Topology.isEnabled( Eucalyptus.class ) ) )
      return;
    
    // should validate the parameters
    if(emi!=null){
      try{
        final List<ImageDetails> images = 
          EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi));
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
    
    // should find the asg name using the special TAG
    // then create a new launch config and replace the old one
    
    /*if((emi!=null && emi.length()>0) || (instanceType!=null && instanceType.length()>0) || (keyname!=null && keyname.length()>0)){
    
      
      final String asgName = asg.getName();
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
          continue;
        }
        if(asgType!=null){
          final String lcName = asgType.getLaunchConfigurationName();
          final LaunchConfigurationType lc = EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(lcName);

          String launchConfigName = null;
          do{
            launchConfigName = String.format("lc-euca-internal-elb-%s-%s-%s", 
                lb.getOwnerAccountNumber(), lb.getDisplayName(), UUID.randomUUID().toString().substring(0, 8));

            if(launchConfigName.length()>255)
              launchConfigName = launchConfigName.substring(0, 255);
          }while(launchConfigName.equals(asgType.getLaunchConfigurationName()));

          final String newEmi = emi != null? emi : lc.getImageId();
          final String newType = instanceType != null? instanceType : lc.getInstanceType();
          String newKeyname = keyname != null ? keyname : lc.getKeyName();

          try{
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(), 
                launchConfigName, lc.getSecurityGroups().getMember().get(0), newKeyname, lc.getUserData());
          }catch(final Exception ex){
            throw new EucalyptusCloudException("failed to create new launch config", ex);
          }
          try{
            EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,asgType.getDesiredCapacity(), launchConfigName);
          }catch(final Exception ex){
            throw new EucalyptusCloudException("failed to update the autoscaling group", ex);
          }
          try{
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(asgType.getLaunchConfigurationName());
          }catch(final Exception ex){
            LOG.warn("unable to delete the old launch configuration", ex);
          } 
          LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
        }
      }catch(final EucalyptusCloudException ex){
        throw ex;
      }catch(final Exception ex){
        throw new EucalyptusCloudException("Unable to update the autoscaling group", ex);
      }
    }
    */
  }
  
  private static class NTPServerChangeListener implements PropertyChangeListener<String> {

    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
     
    }
  }
}
