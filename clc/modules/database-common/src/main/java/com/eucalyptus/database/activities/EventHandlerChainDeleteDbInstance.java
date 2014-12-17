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

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.AuthorizeServerCertificate;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.AuthorizeVolumeOperations;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.CreateAutoScalingGroup;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.CreateLaunchConfiguration;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.CreateTags;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.IamInstanceProfileSetup;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.IamRoleSetup;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.SecurityGroupSetup;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.AuthorizePort;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.UploadServerCertificate;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.UserDataSetup;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainDeleteDbInstance extends EventHandlerChain<DeleteDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainDeleteDbInstance.class );

  @Override
  public EventHandlerChain<DeleteDBInstanceEvent> build() {
    this.append(new RollbackRun(this));
    return this;
  }
  
  public static class RollbackRun extends AbstractEventHandler<DeleteDBInstanceEvent> {
    protected RollbackRun(EventHandlerChain<DeleteDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(DeleteDBInstanceEvent evt) throws EventHandlerException {
      EventHandlerChain<NewDBInstanceEvent> createChain = new EventHandlerChainCreateDbInstance();
      createChain.append(new SecurityGroupSetup(createChain));
      createChain.append(new AuthorizePort(createChain));
      createChain.append(new IamRoleSetup(createChain));
      createChain.append(new IamInstanceProfileSetup(createChain));
      createChain.append(new UploadServerCertificate(createChain));
      createChain.append(new AuthorizeServerCertificate(createChain));
      createChain.append(new AuthorizeVolumeOperations(createChain));
      createChain.append(new UserDataSetup(createChain));
      createChain.append(new CreateLaunchConfiguration(createChain, DatabaseServerProperties.IMAGE, DatabaseServerProperties.INSTANCE_TYPE, 
            ( DatabaseServerProperties.KEYNAME != null && DatabaseServerProperties.KEYNAME.length()>0) ? DatabaseServerProperties.KEYNAME : null));
      createChain.append(new CreateAutoScalingGroup(createChain));
      createChain.append(new CreateTags(createChain));
      createChain.append(new ExceptionThrower(createChain));
      try{
        NewDBInstanceEvent createEvent = new NewDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
        createEvent.setDbInstanceIdentifier( evt.getDbInstanceIdentifier());
        createChain.execute(createEvent);
      }catch(final Exception ex) {
        LOG.debug(ex, ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
  
  public static class ExceptionThrower extends AbstractEventHandler<NewDBInstanceEvent> {

    protected ExceptionThrower(EventHandlerChain<NewDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt) throws EventHandlerException {
      throw new EventHandlerException("Exception to trigger intentional rollback");
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
}
