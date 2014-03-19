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
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 * 
 */
public class ImagingServiceLauncher {
  private static Logger LOG = Logger.getLogger(ImagingServiceLauncher.class);
  private String launcherId = null;
  private List<AbstractAction> actions = null;
  
  private ImagingServiceLauncher(final String launcherId,
      final List<AbstractAction> actions) {
    this.launcherId = launcherId;
    this.actions = actions;
  }
  
  private static class EnableWorker implements Callable<Boolean>{
    private List<AbstractAction> workerActions = null;
    private String launcherId = null;
    private EnableWorker(final String launcherId, final List<AbstractAction> actions){
      this.launcherId = launcherId;
      this.workerActions = actions;
    }
    
    @Override
    public Boolean call() throws Exception {
      final List<AbstractAction> executed = Lists.newArrayList();
      try{
        // / order of actions is important because some actions depend on prior
        // actions performed successfully.
        // / we do not have a mechanism to express dependencies between actions
        for (final AbstractAction action : this.workerActions) {
          try {
            executed.add(action);
            if (!action.apply()) {
              LOG.debug("Imaging service's launch has been terminated at " + action);
              break;
            }
          } catch (final Exception ex) {
            LOG.error("Failed to execute imageservice launcher-action", ex);
            for (final AbstractAction executedAction : Lists.reverse(executed)) {
              try {
                executedAction.rollback();
              } catch (final Exception eex) {
                LOG.error("Failed to rollback imageservice launcher-action", ex);
              }
            }
            throw new EucalyptusCloudException(
                "Imaging service launch failed while executing "
                    + action.toString(), ex);
          }
        }
      }catch(final Exception ex){
        ;
      }finally{
        ImagingServiceLaunchers.getInstance().releaseLauncher(this.launcherId);
      }
      return true;
    }
  }


  private static class DisableWorker implements Callable<Boolean>{
    private List<AbstractAction> workerActions = null;
    private String launcherId = null;
    private DisableWorker(final String launcherId, final List<AbstractAction> actions){
      this.launcherId = launcherId;
      this.workerActions = actions;
    }
    @Override
    public Boolean call() throws Exception {
      final List<AbstractAction> executed = Lists.newArrayList();
      try{
        for (final AbstractAction action : workerActions) {
          try {
            executed.add(action); // an action is idempotent, but 'apply' will gather states necessary to rollback
            action.apply();
          } catch (final Exception ex) {
            LOG.error("Failed to execute imageservice launcher-action", ex);
            break;
          }
        }
        
        boolean rollbackFailed = false;
        for (final AbstractAction executedAction : Lists.reverse(executed)) {
          try {
            executedAction.rollback();
          } catch (final Exception ex) {
            LOG.error("Failed to rollback imageservice launcher-action", ex);
            rollbackFailed = true;
          }
        }
        if(rollbackFailed || executed.size() != workerActions.size())
          throw new EucalyptusCloudException("failed to destroy imaging service");
      }catch(final Exception ex){
        ;
      }finally{
        ImagingServiceLaunchers.getInstance().releaseLauncher(this.launcherId);
      }
      
      return true;
    } 
  }
  public void launch() throws EucalyptusCloudException {
    LOG.debug(String.format("Enabling image service (%s)", this.launcherId));
    final EnableWorker worker = new EnableWorker(this.launcherId, this.actions);
    try{
      Threads.enqueue(Imaging.class, ImagingServiceLauncher.class, worker);
    }catch(final Exception ex){
      throw ex;
    }
  }
  
  public void destroy() throws EucalyptusCloudException {
    LOG.debug(String.format("Disabling imaging service (%s)", this.launcherId));
    final DisableWorker worker = new DisableWorker(this.launcherId, this.actions);
    try{
      Threads.enqueue(Imaging.class,  ImagingServiceLauncher.class, worker);
    }catch(final Exception ex){
      throw ex;
    }
  }

  public String getLauncherId() {
    return this.launcherId;
  }

  @Override
  public String toString() {
    return String.format("ImagingService launcher - ", this.launcherId);
  }

  public static class Builder {
    private String launcherId = null;
    private List<AbstractAction> actions = Lists.newArrayList();
    private LOOKUP lookupAction = new LOOKUP();

    public Builder(final String launcherId) {
      this.launcherId = launcherId;
    }

    class LOOKUP implements
        Function<Class<? extends AbstractAction>, AbstractAction> {
      @Override
      public AbstractAction apply(Class<? extends AbstractAction> arg0) {
        for (final AbstractAction action : actions) {
          if (action.getClass().isAssignableFrom(arg0))
            return action;
        }
        return null;
      }
    }

    public Builder withRole() {
      final ImagingServiceActions.IamRoleSetup roleSetup = new ImagingServiceActions.IamRoleSetup(
          lookupAction, this.launcherId);
      actions.add(roleSetup);
      return this;
    }

    public Builder withInstanceProfile() {
      final ImagingServiceActions.IamInstanceProfileSetup instanceProfileSetup = new ImagingServiceActions.IamInstanceProfileSetup(
          lookupAction, this.launcherId);
      actions.add(instanceProfileSetup);
      return this;
    }

    public Builder withServerCertificate(final String certName) {
      final ImagingServiceActions.UploadServerCertificate uploadCert = 
          new ImagingServiceActions.UploadServerCertificate(
          lookupAction, this.launcherId, certName);
      actions.add(uploadCert);
      final ImagingServiceActions.AuthorizeServerCertificate authCert = new ImagingServiceActions.AuthorizeServerCertificate(
          lookupAction, this.launcherId);
      actions.add(authCert);
      return this;
    }
    
    public Builder withVolumeOperations() {
      final ImagingServiceActions.AuthorizeVolumeOperations authVols = 
          new ImagingServiceActions.AuthorizeVolumeOperations(this.lookupAction, this.launcherId);
      actions.add(authVols);
      return this;
    }
    
    public Builder withS3Operations() {
      final ImagingServiceActions.AuthorizeS3Operations authS3 =
          new ImagingServiceActions.AuthorizeS3Operations(this.lookupAction, this.launcherId);
      actions.add(authS3);
      return this;
    }

    public Builder withUserData() {
      final ImagingServiceActions.UserDataSetup userData = new ImagingServiceActions.UserDataSetup(
          lookupAction, this.launcherId);
      actions.add(userData);
      return this;
    }

    public Builder withLaunchConfiguration(final String emi,
        final String instanceType) {
      ImagingServiceActions.CreateLaunchConfiguration launchConfig = new ImagingServiceActions.CreateLaunchConfiguration(
          lookupAction, this.launcherId, emi, instanceType);
      actions.add(launchConfig);
      return this;
    }

    public Builder withLaunchConfiguration(final String emi,
        final String instanceType, final String keyName) {
      ImagingServiceActions.CreateLaunchConfiguration launchConfig = new ImagingServiceActions.CreateLaunchConfiguration(
          lookupAction, this.launcherId, emi, instanceType, keyName);
      actions.add(launchConfig);
      return this;
    }

    public Builder withAutoScalingGroup() {
      final ImagingServiceActions.CreateAutoScalingGroup asg = new ImagingServiceActions.CreateAutoScalingGroup(
          lookupAction, this.launcherId);
      actions.add(asg);
      return this;
    }

    public Builder withSecurityGroup() {
      final ImagingServiceActions.SecurityGroupSetup sg = new ImagingServiceActions.SecurityGroupSetup(
          lookupAction, this.launcherId);
      actions.add(sg);
      return this;
    }

    public Builder withTag(final String tagName) {
      final ImagingServiceActions.CreateTags tag = new ImagingServiceActions.CreateTags(
          lookupAction, this.launcherId, tagName);
      actions.add(tag);
      return this;
    }

    public Builder withTag() {
      final ImagingServiceActions.CreateTags tag = new ImagingServiceActions.CreateTags(
          lookupAction, this.launcherId);
      actions.add(tag);
      return this;
    }

    public Builder checkAdmission() {
      final ImagingServiceActions.AdmissionControl control = new ImagingServiceActions.AdmissionControl(
          lookupAction, this.launcherId);
      actions.add(control);
      return this;
    }

    public ImagingServiceLauncher build() {
      return new ImagingServiceLauncher(this.launcherId, actions);
    }
  }
}
