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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.imaging.backend.worker.ImagingServiceLauncher;
import com.eucalyptus.imaging.backend.worker.ImagingServiceLauncher.Builder;
import com.eucalyptus.imaging.common.EucalyptusActivityTasks;
import com.eucalyptus.imaging.ImagingServiceProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park
 * 
 */
/*
 * In case we want to create multiple instances(ASG) of the imaging service,
 * we should implement the additional methods here.
 */
@ConfigurableClass(root = "imaging",  description = "Parameters controlling image conversion service")
public class ImagingServiceLaunchers {
  private static Logger LOG = Logger.getLogger(ImagingServiceLaunchers.class);

  private static ImagingServiceLaunchers instance = new ImagingServiceLaunchers();
  
  private static Map<String, String> launchStateTable = Maps.newConcurrentMap();
  public static final String launcherId = "worker-01"; // version 4.0.0, but it can be any string
  public static final String SERVER_CERTIFICATE_NAME = "euca-internal-imaging-service";
  
  @ConfigurableField( displayName="enabled",
      description = "enabling imaging worker",
      initial = "true",
      readonly = false,
      type = ConfigurableFieldType.BOOLEAN,
      changeListener = EnabledChangeListener.class)
  public static Boolean IMAGING_WORKER_ENABLED = true;
  
  private ImagingServiceLaunchers(){  }
  public static ImagingServiceLaunchers getInstance(){
    return instance;
  }
  
  public boolean shouldDisable() {
    if(isLauncherLocked(launcherId))
      return false;
    return tagExists();  
  }

  public boolean shouldEnable() {
    if(isLauncherLocked(launcherId))
      return false;
    return !tagExists();
  }
  
  public boolean isWorkedEnabled() {
    try{
      if(isLauncherLocked(launcherId))
        return false;
      if( tagExists() )
        return true;
      else
        return false;
    }catch(final Exception ex){
      return false;
    }
  }
  
  private boolean tagExists(){
    boolean tagFound = false;
    // lookup tags
    try{
      final List<TagInfo> tags = EucalyptusActivityTasks.getInstance().describeTags(Lists.newArrayList("value"),
          Lists.newArrayList(ImagingServiceProperties.DEFAULT_LAUNCHER_TAG));
      if(tags !=null){
        for(final TagInfo tag : tags){
          if(ImagingServiceProperties.DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            if("security-group".equals(tag.getResourceType())){
              tagFound=true;
              break;
            }
          }
        }
      }
    }catch(final Exception ex){
      // in error situation, better to assume that the imaging worker is running fine
      tagFound=true;
    }
    return tagFound;
  }
  /*
   * In 4.0, we will have have only one ASG maintaining a system-wide imaging service.
   * In the future, we can implement multiple ASGs by managing multiple launcher instances here.
   */
  public void enable() throws EucalyptusCloudException {
    if(! this.shouldEnable())
      throw new EucalyptusCloudException("Imaging service instances are found in the system");
    
    this.lockLauncher(launcherId);
    try{
      final String emi = ImagingServiceProperties.IMAGING_WORKER_EMI;
      final String instanceType = ImagingServiceProperties.IMAGING_WORKER_INSTANCE_TYPE;
      final String keyName = ImagingServiceProperties.IMAGING_WORKER_KEYNAME;

      ImagingServiceLauncher launcher = null;
      try{
        ImagingServiceLauncher.Builder builder = 
            new ImagingServiceLauncher.Builder(launcherId);
        launcher = builder
            .checkAdmission()
            .withSecurityGroup()
            .withRole()
            .withInstanceProfile()
            .withRolePermissions()
            .withServerCertificate(SERVER_CERTIFICATE_NAME)
            .withVolumeOperations()
            .withS3Operations()
            .withUserData()
            .withLaunchConfiguration(emi, instanceType, keyName)
            .withAutoScalingGroup()
            .withTag(ImagingServiceProperties.DEFAULT_LAUNCHER_TAG)
            .build();
      }catch(final Exception ex){
        throw new EucalyptusCloudException("Failed to prepare imaging service launcher", ex);
      }

      try{
        launcher.launch();
      }catch(final Exception ex){
        throw new EucalyptusCloudException("Failed launching image service instance", ex);
      }
    }catch(final Exception ex){
      this.releaseLauncher(launcherId);
      throw ex;
    }
  }
  
  public void disable() throws EucalyptusCloudException {
    if(! this.shouldDisable())
      throw new EucalyptusCloudException("Imaging service instances are not found in the system");
    this.lockLauncher(launcherId);

    try{
      final String emi = ImagingServiceProperties.IMAGING_WORKER_EMI;
      final String instanceType = ImagingServiceProperties.IMAGING_WORKER_INSTANCE_TYPE;
      final String keyName = ImagingServiceProperties.IMAGING_WORKER_KEYNAME;
      final String ntpServers = ImagingServiceProperties.IMAGING_WORKER_NTP_SERVER;

      ImagingServiceLauncher launcher = null;
      try{
        ImagingServiceLauncher.Builder builder = 
            new ImagingServiceLauncher.Builder(launcherId);
        launcher = builder
            .withSecurityGroup()
            .withRole()
            .withInstanceProfile()
            .withRolePermissions()
            .withServerCertificate(SERVER_CERTIFICATE_NAME)
            .withVolumeOperations()
            .withS3Operations()
            .withUserData()
            .withLaunchConfiguration(emi, instanceType, keyName)
            .withAutoScalingGroup()
            .withTag(ImagingServiceProperties.DEFAULT_LAUNCHER_TAG)
            .build();
      }catch(final Exception ex){
        throw new EucalyptusCloudException("Failed to prepare imaging service launcher", ex);
      }

      try{
        launcher.destroy();
      }catch(final Exception ex){
        throw new EucalyptusCloudException("Failed destroying image service instance", ex);
      }
    }catch(final Exception ex){
      this.releaseLauncher(launcherId);
      throw ex;
    }
  }

  public void lockLauncher(final String launcherId){
    synchronized(launchStateTable){
      launchStateTable.put(launcherId, "ENABLING");
    }
  }

  public void releaseLauncher(final String launcherId){
    synchronized(launchStateTable){
      launchStateTable.remove(launcherId);
    }
  }

  public boolean isLauncherLocked(final String launcherId){
    synchronized(launchStateTable){
      return launchStateTable.containsKey(launcherId);
    }
  }
  
  public static class EnabledChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try{
        if("false".equals(((String) newValue).toLowerCase()) &&
            "true".equals(t.getValue())){
          try{
            if(ImagingServiceLaunchers.getInstance().shouldDisable())
              ImagingServiceLaunchers.getInstance().disable();
          }catch(ConfigurablePropertyException ex){
            throw ex;
          }catch(Exception ex){
            throw ex;
          }
        }else if ("true".equals((String) newValue)){
          ;
        }else
          throw new ConfigurablePropertyException("Invalid property value");
      }catch(final ConfigurablePropertyException ex){
        throw ex;
      }catch ( final Exception e ){
        throw new ConfigurablePropertyException("Could not disable imaging service workers", e);
      }
    }
  }

}
