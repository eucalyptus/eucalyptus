/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.activities;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;

public class EventHandlerChainModifyAttributes extends EventHandlerChain<ModifyAttributesEvent> {
  private static Logger LOG  = Logger.getLogger(EventHandlerChainModifyAttributes.class);
  @Override
  public EventHandlerChain<ModifyAttributesEvent> build() {
    this.insert(new AttributesVerifier(this));
    this.insert(new AccessLogPolicyCreator(this));
    this.insert(new AccessLogPolicyRemover(this));
    this.insert(new AttributePersistence(this));
    return this;
  }
  
  private static class AttributesVerifier extends AbstractEventHandler<ModifyAttributesEvent> {
    protected AttributesVerifier(
        EventHandlerChain<? extends ModifyAttributesEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(ModifyAttributesEvent evt) throws EventHandlerException {
      /* Verify AccessLog attributes */
      final AccessLog accessLog = evt.getAttributes().getAccessLog();
      if (accessLog == null)
        return;
      
      final boolean accessLogEnabled = accessLog.getEnabled();
      if (accessLogEnabled) {
        final String bucketName = accessLog.getS3BucketName();
        final String bucketPrefix = 
            com.google.common.base.Objects.firstNonNull(accessLog.getS3BucketPrefix(), "");
        final Integer emitInterval = 
            com.google.common.base.Objects.firstNonNull(accessLog.getEmitInterval(), 60);
        if (bucketName == null || bucketName.length() <=0)
          throw new EventHandlerException("Bucket name must be specified");
        if(emitInterval < 5 || emitInterval > 60) {
          throw new EventHandlerException("Access log's emit interval must be between 5 and 60 minutes");
        }
      }
      /* End verifying AccessLog attributes */
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
  
  private static class AccessLogPolicyCreator extends  AbstractEventHandler<ModifyAttributesEvent> {
    static final String ACCESSLOG_ROLE_POLICY_NAME = "euca-internal-loadbalancer-vm-policy-accesslog";
    private static final String ACCESSLOG_ROLE_POLICY_DOCUMENT=
        "{\"Statement\":"
        + "[ {"
        + "\"Effect\":\"Allow\","
        + "\"Action\":[\"s3:ListBucket\"],"
        + "\"Resource\":\"arn:aws:s3:::BUCKETNAME_PLACEHOLDER\""
        + " },{"
        + "\"Action\": [\"s3:GetObject\", \"s3:PutObject\", \"s3:GetObjectAcl\", \"s3:PutObjectAcl\"],"
        + "\"Effect\": \"Allow\","
        + "\"Resource\": [\"arn:aws:s3:::BUCKETNAME_PLACEHOLDER/BUCKETPREFIX_PLACEHOLDER\"]"
        + "}]}";
    
    protected AccessLogPolicyCreator(
        EventHandlerChain<? extends ModifyAttributesEvent> chain) {
      super(chain);
    }
    private String roleName = null;
    private boolean policyAdded = false;
    @Override
    public void apply(ModifyAttributesEvent evt) throws EventHandlerException {
      final AccessLog accessLog = evt.getAttributes().getAccessLog();
      if (accessLog == null || !accessLog.getEnabled())
        return;

      final String bucketName = accessLog.getS3BucketName();
      final String bucketPrefix = 
            com.google.common.base.Objects.firstNonNull(accessLog.getS3BucketPrefix(), "");
     
      this.roleName = String.format("%s-%s-%s", 
          EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, 
          evt.getContext().getAccount().getAccountNumber(), 
          evt.getLoadBalancer());
      final String policyName = ACCESSLOG_ROLE_POLICY_NAME;
      try{
        final List<String> policies = 
            EucalyptusActivityTasks.getInstance().listRolePolicies(this.roleName);
        if(policies.contains(policyName)){
          EucalyptusActivityTasks.getInstance().deleteRolePolicy(this.roleName, policyName);
        } 
      }catch(final Exception ex){
        ;
      }
      
      String policyDocument = ACCESSLOG_ROLE_POLICY_DOCUMENT.replace("BUCKETNAME_PLACEHOLDER", bucketName);
      if (bucketPrefix.length() > 0) {
        policyDocument = policyDocument.replace("BUCKETPREFIX_PLACEHOLDER", bucketPrefix+"/*");
      }else{
        policyDocument = policyDocument.replace("BUCKETPREFIX_PLACEHOLDER", "*");
      }

      try{
        EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, policyDocument);
        policyAdded = true;
      }catch(final Exception ex){
        throw new EventHandlerException("failed to put role policy for loadbalancer vm's access to S3 buckets");
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if(policyAdded) {
        final String policyName = ACCESSLOG_ROLE_POLICY_NAME;
        try{
          EucalyptusActivityTasks.getInstance().deleteRolePolicy(this.roleName, policyName);
        }catch(final Exception ex) {
          ;
        }
      }
    }
  }
  
  private static class AccessLogPolicyRemover extends AbstractEventHandler<ModifyAttributesEvent> {
    protected AccessLogPolicyRemover(
        EventHandlerChain<? extends ModifyAttributesEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(ModifyAttributesEvent evt) throws EventHandlerException {
      final AccessLog accessLog = evt.getAttributes().getAccessLog();
      if (accessLog == null || accessLog.getEnabled())
        return;
      final String roleName = String.format("%s-%s-%s", 
          EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, 
          evt.getContext().getAccount().getAccountNumber(), 
          evt.getLoadBalancer());
      final String policyName = AccessLogPolicyCreator.ACCESSLOG_ROLE_POLICY_NAME;
     
      try{
        EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName);
      }catch(final Exception ex) {
        ;
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    } 
  }
  
  private static class AttributePersistence extends AbstractEventHandler<ModifyAttributesEvent> {
    protected AttributePersistence(
        EventHandlerChain<? extends ModifyAttributesEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(ModifyAttributesEvent evt) throws EventHandlerException {
      final AccessLog accessLog = evt.getAttributes().getAccessLog();
      if(accessLog == null)
        return;
      
      final boolean accessLogEnabled = accessLog.getEnabled();
      String bucketName = null;
      String bucketPrefix = null;
      Integer emitInterval = null;
      if(accessLogEnabled) {
        bucketName = accessLog.getS3BucketName();
        bucketPrefix = 
            com.google.common.base.Objects.firstNonNull(accessLog.getS3BucketPrefix(), "");
        emitInterval = 
            com.google.common.base.Objects.firstNonNull(accessLog.getEmitInterval(), 60);
      } else {
        bucketName = "";
        bucketPrefix = "";
        emitInterval = 60;
      }
      try ( final TransactionResource db = Entities.transactionFor(LoadBalancer.class) ) {
        final LoadBalancer lb = Entities.uniqueResult(
            LoadBalancer.named(evt.getContext().getUserFullName(), evt.getLoadBalancer()));
        lb.setAccessLogEnabled(accessLogEnabled);
        lb.setAccessLogEmitInterval(emitInterval);
        lb.setAccessLogS3BucketName(bucketName);
        lb.setAccessLogS3BucketPrefix(bucketPrefix);
        Entities.persist(lb);
        db.commit();
      }catch(final NoSuchElementException ex) {
        throw new EventHandlerException("No such loadbalancer is found");
      }catch(final Exception ex) {
        throw new EventHandlerException("Unknown error occured while updating database", ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
     ; 
    }    
  }
}
