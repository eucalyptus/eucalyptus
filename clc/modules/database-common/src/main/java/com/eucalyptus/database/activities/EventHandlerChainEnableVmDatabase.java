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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.database.activities.EventHandlerChainCreateDbInstance.CreateTags;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.StoredResult;
import com.eucalyptus.resources.client.Ec2Client;
import com.google.common.collect.Lists;
/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainEnableVmDatabase extends EventHandlerChain<EnableDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainEnableVmDatabase.class );

  @Override
  public EventHandlerChain<EnableDBInstanceEvent> build() {
    this.append(new WaitOnVm(this));
    this.append(new WaitOnDb(this));
    this.append(new UpdateProperties(this));
    return this;
  }
  
  private static String getSystemUserId(){ 
    try{
      return Accounts.lookupSystemAdmin().getUserId();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public static class WaitOnVm extends AbstractEventHandler<EnableDBInstanceEvent> implements StoredResult<String> {
    private String instanceId = null;
    protected WaitOnVm(EventHandlerChain<EnableDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(EnableDBInstanceEvent evt) throws EventHandlerException {
      final String tagKey = "Name";
      final String tagValue = CreateTags.DEFAULT_DB_RES_TAG;
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      
      boolean vmFound = false;
      final int MAX_RETRY_SEC = 600;
      for(int i = 1; i<= MAX_RETRY_SEC; i++) {
        try{
          final List<RunningInstancesItemType> instances = 
              Ec2Client.getInstance().describeInstances(systemUserId, Lists.<String>newArrayList());

          for(final RunningInstancesItemType instance : instances) {
            final List<ResourceTag> tags = instance.getTagSet();
            final String state = instance.getStateName();
            if (tags != null ) {
              for(final ResourceTag tag : tags) {
                if(tagKey.equals(tag.getKey()) && tagValue.equals(tag.getValue()) && "running".equals(state)) {
                  vmFound = true;
                  instanceId = instance.getInstanceId();
                  break;
                }
              }
            }
            if(vmFound)
              break;
          }
        }catch(final Exception ex){
          vmFound = false; 
        }
        if(vmFound)
          break;
        try{
          Thread.sleep(1000);
        }catch(final Exception ex){
          ;
        }
        if( i % 10 == 0) {
          LOG.info("Looking for running database VM");
        }
      }
      
      if(!vmFound)
        throw new EventHandlerException("Cannot find a running database VM");
      
      LOG.info("Database VM ("+instanceId +") is found");
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }

    @Override
    public List<String> getResult() {
      if(this.instanceId != null){
        return Lists.newArrayList(this.instanceId);
      }else { 
        return Lists.newArrayList();
      }
    }
  }
  
  public static class WaitOnDb extends AbstractEventHandler<EnableDBInstanceEvent> implements StoredResult<String> {
    private static final String PING_DB_NAME = "eucalyptus_reporting_backend";
    private String instanceIp = null;
    protected WaitOnDb(EventHandlerChain<EnableDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(EnableDBInstanceEvent evt) throws EventHandlerException {
      // ping on remote database
      try {
        final String instanceId = this.getChain().findHandler(WaitOnVm.class).getResult().get(0);
        final List<RunningInstancesItemType> instances = 
            Ec2Client.getInstance().describeInstances(getSystemUserId(), Lists.newArrayList(instanceId));
        for(final RunningInstancesItemType instance : instances) {
          if(instanceId.equals(instance.getInstanceId())) {
            instanceIp = instance.getIpAddress();
            break;
          }
        }
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to lookup vm ip", ex);
      }
      
      if(instanceIp == null)
        throw new EventHandlerException("Failed to lookup vm ip");
      /// TODO spark: ssl option
      // "jdbc:postgresql://%s:%s/%s?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
      final String jdbcUrl = 
          String.format( "jdbc:postgresql://%s:%s/%s",  instanceIp, evt.getPort(), PING_DB_NAME );
     
      boolean connected = false;
      final int MAX_RETRY_SEC = 600;
      for(int i = 1; i<= MAX_RETRY_SEC; i++) {
        if (pingDatabase(jdbcUrl, evt.getMasterUserName(), evt.getMasterUserPassword())) {
          connected = true;
          break;   
        }
        try{
          Thread.sleep(1000);
        }catch(final Exception ex){
          ;
        }
        if( i % 10 == 0) {
          LOG.info("Trying to connect to database at "+ instanceIp);
        }
      }
      
      if(!connected) {
        throw new EventHandlerException("Failed to connect to database at "+instanceIp);
      }
      LOG.info("Database host "+instanceIp+" is connected");
    }
    
    private boolean pingDatabase(final String jdbcUrl, final String userName, final String password) {
      try ( final Connection conn = DriverManager.getConnection( jdbcUrl, userName, password ) ) { 
        try ( final PreparedStatement statement = 
            conn.prepareStatement( "SELECT USER" ) ) { 
          try ( final ResultSet result = statement.executeQuery( ) ) { 
            return true;
          }   
        }   
      }catch(final Exception ex){
        return false;
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }

    @Override
    public List<String> getResult() {
      if(instanceIp != null)
        return Lists.newArrayList(instanceIp);
      else
        return Lists.newArrayList();
    }
  }
  
  public static class UpdateProperties extends AbstractEventHandler<EnableDBInstanceEvent> {
    protected UpdateProperties(EventHandlerChain<EnableDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(EnableDBInstanceEvent evt) throws EventHandlerException {
      String dbHost = null;
      try{
        dbHost = this.getChain().findHandler(WaitOnDb.class).getResult().get(0);
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to look up database host");
      }
      
      try{
        final ConfigurableProperty portProp = 
            PropertyDirectory.getPropertyEntry("cloud.db.appendonlyport");
        portProp.setValue(String.format("%d", evt.getPort()));
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set port property", ex);
      }
      
      try{
        final ConfigurableProperty userNameProp = 
            PropertyDirectory.getPropertyEntry("cloud.db.appendonlyuser");
        userNameProp.setValue(evt.getMasterUserName());
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set username property", ex);
      }
      
      try{
        final ConfigurableProperty passwordProp = 
            PropertyDirectory.getPropertyEntry("cloud.db.appendonlypassword");
        passwordProp.setValue(evt.getMasterUserPassword());
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set password property", ex);
      }
      
      /// this will trigger db pool init to the designated host
      try{
        final ConfigurableProperty hostProp = 
            PropertyDirectory.getPropertyEntry("cloud.db.appendonlyhost");
        hostProp.setValue(dbHost);
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set hostname property", ex);
      }
      
      LOG.info("cloud.db.appendonly* properties are updated");
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
}
