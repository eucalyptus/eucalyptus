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

import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.TagInfo;

/**
 * @author Sang-Min Park
 * 
 */
/*
 * In case we want to create multiple instances(ASG) of the imaging service,
 * we should implement the additional methods here.
 */
public class ImagingServiceLaunchers {
  private static Logger LOG = Logger.getLogger(ImagingServiceLaunchers.class);

  private static ImagingServiceLaunchers instance = new ImagingServiceLaunchers();
  public static final String DEFAULT_LAUNCHER_TAG = "euca-internal-imaging-workers";
  private static Map<String, String> launchStateTable = Maps.newConcurrentMap();
  public static final String launcherId = "worker-01"; // version 4.0.0, but it can be any string
  public static final String SERVER_CERTIFICATE_NAME = "euca-internal-imaging-service";
  
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
  
  private boolean tagExists(){
    boolean tagFound = false;
    // lookup tags
    try{
      final List<TagInfo> tags = EucalyptusActivityTasks.getInstance().describeTags(Lists.newArrayList("value"), 
          Lists.newArrayList(DEFAULT_LAUNCHER_TAG));
      if(tags !=null){
        for(final TagInfo tag : tags){
          if(DEFAULT_LAUNCHER_TAG.equals(tag.getValue())){
            if("security-group".equals(tag.getResourceType())){
              tagFound=true;
              break;
            }
          }
        }
      }
    }catch(final Exception ex){
      tagFound=false;
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
            .withServerCertificate(SERVER_CERTIFICATE_NAME)
            .withVolumeOperations()
            .withS3Operations()
            .withUserData()
            .withLaunchConfiguration(emi, instanceType, keyName)
            .withAutoScalingGroup()
            .withTag(DEFAULT_LAUNCHER_TAG)
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
            .withServerCertificate(SERVER_CERTIFICATE_NAME)
            .withVolumeOperations()
            .withS3Operations()
            .withUserData()
            .withLaunchConfiguration(emi, instanceType, keyName)
            .withAutoScalingGroup()
            .withTag(DEFAULT_LAUNCHER_TAG)
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
}
