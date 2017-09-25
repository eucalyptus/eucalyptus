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
package com.eucalyptus.resources.client;

import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.auth.euare.common.msgs.AddRoleToInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleType;
import com.eucalyptus.auth.euare.common.msgs.DeleteInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRoleResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRoleType;
import com.eucalyptus.auth.euare.common.msgs.DeleteServerCertificateResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.EuareMessage;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyResult;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.InstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.common.msgs.ListRolePoliciesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolePoliciesType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesType;
import com.eucalyptus.auth.euare.common.msgs.ListServerCertificatesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListServerCertificatesType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.RemoveRoleFromInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.RemoveRoleFromInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.RoleType;
import com.eucalyptus.auth.euare.common.msgs.ServerCertificateMetadataType;
import com.eucalyptus.auth.euare.common.msgs.ServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.UploadServerCertificateResponseType;
import com.eucalyptus.auth.euare.common.msgs.UploadServerCertificateType;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

/**
 * @author Sang-Min Park
 *
 */
public class EuareClient {
    private static Logger LOG = Logger.getLogger(EuareClient.class);
  private static EuareClient _instance = null;
  private EuareClient(){ }
  public static EuareClient getInstance(){
    if(_instance == null)
      _instance = new EuareClient();
    return _instance;
  }

  private class EuareContext extends AbstractClientContext<EuareMessage, Euare> {
    private EuareContext(final String userId){
      super(userId, Euare.class);
    }
  }

  private class EuareGetServerCertificateTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String certName = null;
    private ServerCertificateType certificate = null;

    private EuareGetServerCertificateTask(final String certName) {
      this.certName = certName;
    }

    private GetServerCertificateType getRequest() {
      final GetServerCertificateType req = new GetServerCertificateType();
      req.setServerCertificateName(this.certName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(getRequest(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final GetServerCertificateResponseType resp = (GetServerCertificateResponseType) response;
      if (resp.getGetServerCertificateResult() != null)
        this.certificate = resp.getGetServerCertificateResult()
        .getServerCertificate();
    }

    public ServerCertificateType getServerCertificate() {
      return this.certificate;
    }
  }

  private class EuareDeleteInstanceProfileTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String profileName = null;

    private EuareDeleteInstanceProfileTask(String profileName) {
      this.profileName = profileName;
    }

    private DeleteInstanceProfileType deleteInstanceProfile() {
      final DeleteInstanceProfileType req = new DeleteInstanceProfileType();
      req.setInstanceProfileName(this.profileName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(deleteInstanceProfile(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final DeleteInstanceProfileResponseType resp = (DeleteInstanceProfileResponseType) response;
    }

  }

  private class EuareAddRoleToInstanceProfileTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String instanceProfileName = null;
    private String roleName = null;

    private EuareAddRoleToInstanceProfileTask(final String instanceProfileName,
        final String roleName) {
      this.instanceProfileName = instanceProfileName;
      this.roleName = roleName;
    }

    private AddRoleToInstanceProfileType addRoleToInstanceProfile() {
      final AddRoleToInstanceProfileType req = new AddRoleToInstanceProfileType();
      req.setRoleName(this.roleName);
      req.setInstanceProfileName(this.instanceProfileName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(addRoleToInstanceProfile(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final AddRoleToInstanceProfileResponseType resp = (AddRoleToInstanceProfileResponseType) response;
    }
  }

  private class EuareRemoveRoleFromInstanceProfileTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String instanceProfileName = null;
    private String roleName = null;

    private EuareRemoveRoleFromInstanceProfileTask(
        final String instanceProfileName, final String roleName) {
      this.instanceProfileName = instanceProfileName;
      this.roleName = roleName;
    }

    private RemoveRoleFromInstanceProfileType removeRoleFromInstanceProfile() {
      final RemoveRoleFromInstanceProfileType req = new RemoveRoleFromInstanceProfileType();
      req.setRoleName(this.roleName);
      req.setInstanceProfileName(this.instanceProfileName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(removeRoleFromInstanceProfile(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final RemoveRoleFromInstanceProfileResponseType resp = (RemoveRoleFromInstanceProfileResponseType) response;
    }
  }

  private class EuareListInstanceProfilesTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String pathPrefix = null;
    private List<InstanceProfileType> instanceProfiles = null;

    private EuareListInstanceProfilesTask(final String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }

    private ListInstanceProfilesType listInstanceProfiles() {
      final ListInstanceProfilesType req = new ListInstanceProfilesType();
      req.setPathPrefix(this.pathPrefix);
      return req;
    }

    public List<InstanceProfileType> getInstanceProfiles() {
      return this.instanceProfiles;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(listInstanceProfiles(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      ListInstanceProfilesResponseType resp = (ListInstanceProfilesResponseType) response;
      try {
        instanceProfiles = resp.getListInstanceProfilesResult()
            .getInstanceProfiles().getMember();
      } catch (Exception ex) {
        ;
      }
    }
  }

  private class EuareCreateInstanceProfileTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String profileName = null;
    private String path = null;
    private InstanceProfileType instanceProfile = null;

    private EuareCreateInstanceProfileTask(String profileName, String path) {
      this.profileName = profileName;
      this.path = path;
    }

    private CreateInstanceProfileType createInstanceProfile() {
      final CreateInstanceProfileType req = new CreateInstanceProfileType();
      req.setInstanceProfileName(this.profileName);
      req.setPath(this.path);
      return req;
    }

    public InstanceProfileType getInstanceProfile() {
      return this.instanceProfile;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(createInstanceProfile(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final CreateInstanceProfileResponseType resp = (CreateInstanceProfileResponseType) response;
      try {
        this.instanceProfile = resp.getCreateInstanceProfileResult()
            .getInstanceProfile();
      } catch (Exception ex) {
        ;
      }

    }
  }

  private class EuareDeleteRoleTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String roleName = null;

    private EuareDeleteRoleTask(String roleName) {
      this.roleName = roleName;
    }

    private DeleteRoleType deleteRole() {
      final DeleteRoleType req = new DeleteRoleType();
      req.setRoleName(this.roleName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(deleteRole(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final DeleteRoleResponseType resp = (DeleteRoleResponseType) response;
    }
  }

  private class EuareCreateRoleTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    String roleName = null;
    String path = null;
    String assumeRolePolicy = null;
    private RoleType role = null;

    private EuareCreateRoleTask(String roleName, String path,
        String assumeRolePolicy) {
      this.roleName = roleName;
      this.path = path;
      this.assumeRolePolicy = assumeRolePolicy;
    }

    private CreateRoleType createRole() {
      final CreateRoleType req = new CreateRoleType();
      req.setRoleName(this.roleName);
      req.setPath(this.path);
      req.setAssumeRolePolicyDocument(this.assumeRolePolicy);
      return req;
    }

    public RoleType getRole() {
      return this.role;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(createRole(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      CreateRoleResponseType resp = (CreateRoleResponseType) response;
      try {
        this.role = resp.getCreateRoleResult().getRole();
      } catch (Exception ex) {
        ;
      }
    }

  }

  private class EuareListRolesTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String pathPrefix = null;
    private List<RoleType> roles = Lists.newArrayList();

    private EuareListRolesTask(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }

    private ListRolesType listRoles() {
      final ListRolesType req = new ListRolesType();
      req.setPathPrefix(this.pathPrefix);
      return req;
    }

    public List<RoleType> getRoles() {
      return this.roles;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(listRoles(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      ListRolesResponseType resp = (ListRolesResponseType) response;
      try {
        this.roles = resp.getListRolesResult().getRoles().getMember();
      } catch (Exception ex) {
        ;
      }
    }
  }

  private class EuarePutRolePolicyTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String roleName = null;
    private String policyName = null;
    private String policyDocument = null;

    private EuarePutRolePolicyTask(String roleName, String policyName,
        String policyDocument) {
      this.roleName = roleName;
      this.policyName = policyName;
      this.policyDocument = policyDocument;
    }

    private PutRolePolicyType putRolePolicy() {
      final PutRolePolicyType req = new PutRolePolicyType();
      req.setRoleName(this.roleName);
      req.setPolicyName(this.policyName);
      req.setPolicyDocument(this.policyDocument);

      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(putRolePolicy(), callback);

    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final PutRolePolicyResponseType resp = (PutRolePolicyResponseType) response;
    }
  }

  private class EuareListRolePoliciesTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String roleName = null;
    private List<String> policies = null;

    private EuareListRolePoliciesTask(final String roleName) {
      this.roleName = roleName;
    }

    private ListRolePoliciesType listRolePolicies() {
      final ListRolePoliciesType req = new ListRolePoliciesType();
      req.setRoleName(this.roleName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(listRolePolicies(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      try {
        final ListRolePoliciesResponseType resp = (ListRolePoliciesResponseType) response;
        this.policies = resp.getListRolePoliciesResult().getPolicyNames()
            .getMemberList();
      } catch (final Exception ex) {
        this.policies = Lists.newArrayList();
      }
    }

    public List<String> getResult() {
      return this.policies;
    }
  }

  private class EuareGetRolePolicyTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String roleName = null;
    private String policyName = null;
    private GetRolePolicyResult result = null;

    private EuareGetRolePolicyTask(final String roleName,
        final String policyName) {
      this.roleName = roleName;
      this.policyName = policyName;
    }

    private GetRolePolicyType getRolePolicy() {
      final GetRolePolicyType req = new GetRolePolicyType();
      req.setRoleName(this.roleName);
      req.setPolicyName(this.policyName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(getRolePolicy(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      try {
        final GetRolePolicyResponseType resp = (GetRolePolicyResponseType) response;
        this.result = resp.getGetRolePolicyResult();
      } catch (final Exception ex) {
        ;
      }
    }

    GetRolePolicyResult getResult() {
      return this.result;
    }
  }

  private class EuareDeleteRolePolicyTask extends
  EucalyptusClientTask<EuareMessage, Euare> {
    private String roleName = null;
    private String policyName = null;

    private EuareDeleteRolePolicyTask(final String roleName,
        final String policyName) {
      this.roleName = roleName;
      this.policyName = policyName;
    }

    private DeleteRolePolicyType deleteRolePolicy() {
      final DeleteRolePolicyType req = new DeleteRolePolicyType();
      req.setRoleName(this.roleName);
      req.setPolicyName(this.policyName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(deleteRolePolicy(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final DeleteRolePolicyResponseType resp = (DeleteRolePolicyResponseType) response;
    }
  }

  private class UploadServerCertificateTask extends EucalyptusClientTask<EuareMessage, Euare> {
    private String certName = null;
    private String certPath = "/";
    private String certPem = null;
    private String pkPem = null;
    private String certChain = null;
    private ServerCertificateMetadataType serverCertificateMetadata = null;

    private UploadServerCertificateTask(final String certName, final String certPath,
        final String certPem, final String pkPem, final String certChain ){
     this.certName = certName;
     if(certPath!=null)
       this.certPath = certPath;
     this.certPem = certPem;
     this.pkPem = pkPem;
     this.certChain = certChain;
    }

    private UploadServerCertificateType uploadServerCertificate(){
        final UploadServerCertificateType req = new UploadServerCertificateType();
        req.setServerCertificateName(this.certName);
        req.setPath(this.certPath);
        req.setCertificateBody(this.certPem);
        req.setPrivateKey(this.pkPem);
        if(this.certChain!=null)
          req.setCertificateChain(this.certChain);
        return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(uploadServerCertificate(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final UploadServerCertificateResponseType resp = (UploadServerCertificateResponseType) response;
      serverCertificateMetadata = resp.getUploadServerCertificateResult().getServerCertificateMetadata();
    }

    public ServerCertificateMetadataType getCertificateMetadata() {
      return serverCertificateMetadata;
    }
  }

  private class DeleteServerCertificateTask extends EucalyptusClientTask<EuareMessage, Euare> {
    private String certName = null;
    private DeleteServerCertificateTask(final String certName){
      this.certName = certName;
    }

    private DeleteServerCertificateType deleteServerCertificate(){
      final DeleteServerCertificateType req = new DeleteServerCertificateType();
      req.setServerCertificateName(this.certName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(deleteServerCertificate(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final DeleteServerCertificateResponseType resp = new DeleteServerCertificateResponseType();
    }
  }

    //TODO: need formatting
    private class DescribeServerCertificateTask extends EucalyptusClientTask<EuareMessage, Euare> {
        private String certName = null;
        private String path = null;
        private ServerCertificateMetadataType result = null;

        private DescribeServerCertificateTask(final String certName, final String path) {
            if (certName == null || certName.isEmpty())
                throw new IllegalArgumentException("Cert name can't be null or empty");
            this.certName = certName;
            this.path = path;
        }

        private ListServerCertificatesType deleteServerCertificate() {
            final ListServerCertificatesType req = new ListServerCertificatesType();
            if (path != null)
                req.setPathPrefix(path);
            return req;
        }

        @Override
        void dispatchInternal(ClientContext<EuareMessage, Euare> context,
                              Checked<EuareMessage> callback) {
            final DispatchingClient<EuareMessage, Euare> client = context.getClient();
            client.dispatch(deleteServerCertificate(), callback);
        }

        @Override
        void dispatchSuccess(ClientContext<EuareMessage, Euare> context,
                             EuareMessage response) {
            final ListServerCertificatesResponseType resp = (ListServerCertificatesResponseType) response;
            if (resp != null && resp.getListServerCertificatesResult() != null) {
                ArrayList<ServerCertificateMetadataType> results =
                        resp.getListServerCertificatesResult().getServerCertificateMetadataList().getMemberList();
                for(ServerCertificateMetadataType cert:results) {
                    if (cert.getServerCertificateName().equals(certName)) {
                        result = cert;
                        break;
                    }
                }
            } else {
                LOG.debug("Can't get response from ListServerCertificates request");
            }
        }

        public ServerCertificateMetadataType getResult() {
            return result;
        }
    }


  public List<RoleType> listRoles(final String userId, final String pathPrefix) {
    final EuareListRolesTask task = new EuareListRolesTask(pathPrefix);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getRoles();
      } else
        throw new EucalyptusActivityException("failed to list IAM roles");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public RoleType createRole(final String userId, final String roleName, final String path,
      final String assumeRolePolicy) {
    final EuareCreateRoleTask task = new EuareCreateRoleTask(roleName, path,
        assumeRolePolicy);

    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getRole();
      } else
        throw new EucalyptusActivityException("failed to create IAM role");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public ServerCertificateType getServerCertificate(final String userId,
      final String certName) {
    final EuareGetServerCertificateTask task = new EuareGetServerCertificateTask(
        certName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getServerCertificate();
      } else
        throw new EucalyptusActivityException(
            "failed to get server certificate");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteRole(final String userId, final String roleName) {
    final EuareDeleteRoleTask task = new EuareDeleteRoleTask(roleName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to delete IAM role");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<InstanceProfileType> listInstanceProfiles(final String userId, String pathPrefix) {
    final EuareListInstanceProfilesTask task = new EuareListInstanceProfilesTask(
        pathPrefix);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getInstanceProfiles();
      } else
        throw new EucalyptusActivityException("failed to delete IAM role");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public InstanceProfileType createInstanceProfile(final String userId, String profileName,
      String path) {
    final EuareCreateInstanceProfileTask task = new EuareCreateInstanceProfileTask(
        profileName, path);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getInstanceProfile();
      } else
        throw new EucalyptusActivityException(
            "failed to create IAM instance profile");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteInstanceProfile(final String userId, String profileName) {
    final EuareDeleteInstanceProfileTask task = new EuareDeleteInstanceProfileTask(
        profileName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to delete IAM instance profile");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void addRoleToInstanceProfile(final String userId, String instanceProfileName,
      String roleName) {
    final EuareAddRoleToInstanceProfileTask task = new EuareAddRoleToInstanceProfileTask(
        instanceProfileName, roleName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to add role to the instance profile");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void removeRoleFromInstanceProfile(final String userId, String instanceProfileName,
      String roleName) {
    final EuareRemoveRoleFromInstanceProfileTask task = new EuareRemoveRoleFromInstanceProfileTask(
        instanceProfileName, roleName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to remove role from the instance profile");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<String> listRolePolicies(final String userId, final String roleName) {
    final EuareListRolePoliciesTask task = new EuareListRolePoliciesTask(
        roleName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException("failed to list role's policies");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public GetRolePolicyResult getRolePolicy(final String userId, String roleName, String policyName) {
    final EuareGetRolePolicyTask task = new EuareGetRolePolicyTask(roleName,
        policyName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException("failed to get role's policy");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void putRolePolicy(final String userId, String roleName, String policyName,
      String policyDocument) {
    final EuarePutRolePolicyTask task = new EuarePutRolePolicyTask(roleName,
        policyName, policyDocument);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to put role's policy");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteRolePolicy(final String userId, String roleName, String policyName) {
    final EuareDeleteRolePolicyTask task = new EuareDeleteRolePolicyTask(
        roleName, policyName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new EuareContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to delete role's policy");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public ServerCertificateMetadataType uploadServerCertificate(final String userId, final String certName, final String certPath, final String certBodyPem,
      final String pkPem, final String certChainPem){
    final UploadServerCertificateTask task =
        new UploadServerCertificateTask(certName, certPath, certBodyPem, pkPem, certChainPem );
    final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareContext(userId));
    try{
      if(result.get()){
        return task.getCertificateMetadata();
      }else
        throw new EucalyptusActivityException("failed to upload server certificate");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteServerCertificate(final String userId, final String certName){
    final DeleteServerCertificateTask task =
        new DeleteServerCertificateTask(certName);
    final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareContext(userId));
    try{
      if(result.get()){
        return;
      }else
        throw new EucalyptusActivityException("failed to delete server certificate");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public ServerCertificateMetadataType describeServerCertificate(final String userId, final String certName, final String path){
    final DescribeServerCertificateTask task =
            new DescribeServerCertificateTask(certName, path);
    final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareContext(userId));
    try{
        if(result.get()){
            return task.getResult();
        }else
            throw new EucalyptusActivityException("failed to delete server certificate");
    }catch(Exception ex){
        throw Exceptions.toUndeclared(ex);
    }
  }
}
