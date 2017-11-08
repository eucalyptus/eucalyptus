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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.StoredResult;
import com.eucalyptus.resources.client.AutoScalingClient;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.resources.client.EuareClient;
import com.google.common.collect.Lists;
/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainEnableVmDatabase extends EventHandlerChain<EnableDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainEnableVmDatabase.class );

  @Override
  public EventHandlerChain<EnableDBInstanceEvent> build() {
    this.append(new CheckDatabaseStack(this));
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
  
  public static class CheckDatabaseStack extends AbstractEventHandler<EnableDBInstanceEvent> {

    protected CheckDatabaseStack(
        EventHandlerChain<EnableDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(EnableDBInstanceEvent evt) throws EventHandlerException {
      boolean stackFound = false;
      final String accountId = EventHandlerChainCreateDbInstance.getAccountByUser(evt.getUserId());
      final String stackName = EventHandlerChainCreateDbInstance.getStackName(accountId);

      try{
        Stack stack = CloudFormationClient.getInstance().describeStack(evt.getUserId(),
            stackName);
        if (stack != null) {
          stackFound = true;
        }
      }catch(final Exception ex){
        stackFound = false;
      }
      
      if(!stackFound)
        throw new EventHandlerException("Could not find stack: " + stackName);
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
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
      final String tagValue = DatabaseServerProperties.DEFAULT_LAUNCHER_TAG;
      
      boolean vmFound = false;
      final int MAX_RETRY_SEC = 600;
      for(int i = 1; i<= MAX_RETRY_SEC; i++) {
        try{
          final List<RunningInstancesItemType> instances = 
              Ec2Client.getInstance().describeInstances(null, Lists.<String>newArrayList());

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
    private static final String PING_DB_NAME = "eucalyptus_cloudwatch_backend";
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
      final String jdbcUrl = getJdbcUrl( instanceIp, evt.getPort() );
     
      boolean connected = false;
      final int MAX_RETRY_SEC = 120;
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
    
    public static String getJdbcUrl(final String instanceIp, final int port) {
      final String jdbcUrl = 
          String.format( "jdbc:postgresql://%s:%s/%s",  instanceIp, port, PING_DB_NAME);
      return jdbcUrl;
    }
    
    public static String getJdbcUrlWithSsl(final String instanceIp, final int port) {
      final String ssl = "ssl=true&sslfactory=com.eucalyptus.database.activities.VmDatabaseSSLSocketFactory";
      final String jdbcUrl = 
          String.format( "jdbc:postgresql://%s:%s/%s?%s",  instanceIp, port, PING_DB_NAME, ssl );
      return jdbcUrl;
    }
    
    public static boolean pingDatabase(final String jdbcUrl, final String userName, final String password) {
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
      
      String accountId = EventHandlerChainCreateDbInstance.getAccountByUser(evt.getUserId());
      try{
        final ServerCertificateType serverCert = EuareClient.getInstance().getServerCertificate(evt.getUserId(),
            EventHandlerChainCreateDbInstance.getCertificateName(accountId));
        final String certBody = serverCert.getCertificateBody();
        final String bodyPem = certBody;

        final ConfigurableProperty certProp = 
            PropertyDirectory.getPropertyEntry("services.database.appendonlysslcert");
        certProp.setValue(bodyPem);
        LOG.debug("Certificate body is updated for postgresql ssl connection");
      }catch(final Exception ex){
        LOG.error("Failed to read the server certificate", ex);
        throw new EventHandlerException("Failed to read the server certificate");
      }
      
      try{
        final ConfigurableProperty portProp = 
            PropertyDirectory.getPropertyEntry("services.database.appendonlyport");
        portProp.setValue(String.format("%d", evt.getPort()));
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set port property", ex);
      }
      
      try{
        final ConfigurableProperty userNameProp = 
            PropertyDirectory.getPropertyEntry("services.database.appendonlyuser");
        userNameProp.setValue(evt.getMasterUserName());
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set username property", ex);
      }
      
      try{
        final ConfigurableProperty passwordProp = 
            PropertyDirectory.getPropertyEntry("services.database.appendonlypassword");
        passwordProp.setValue(evt.getMasterUserPassword());
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set password property", ex);
      }
      
      /// this will trigger db pool init to the designated host
      try{
        final ConfigurableProperty hostProp = 
            PropertyDirectory.getPropertyEntry("services.database.appendonlyhost");
        hostProp.setValue(dbHost);
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set hostname property", ex);
      }
      
      LOG.info("services.database.appendonly* properties are updated");
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
}
