package com.eucalyptus.auth.euare;

import com.eucalyptus.auth.euare.AddUserToGroup;
import com.eucalyptus.auth.euare.AddUserToGroupResponse;
import com.eucalyptus.auth.euare.CreateAccessKey;
import com.eucalyptus.auth.euare.CreateAccessKeyResponse;
import com.eucalyptus.auth.euare.CreateGroup;
import com.eucalyptus.auth.euare.CreateGroupResponse;
import com.eucalyptus.auth.euare.CreateLoginProfile;
import com.eucalyptus.auth.euare.CreateLoginProfileResponse;
import com.eucalyptus.auth.euare.CreateUser;
import com.eucalyptus.auth.euare.CreateUserResponse;
import com.eucalyptus.auth.euare.DeactivateMFADevice;
import com.eucalyptus.auth.euare.DeactivateMFADeviceResponse;
import com.eucalyptus.auth.euare.DeleteAccessKey;
import com.eucalyptus.auth.euare.DeleteAccessKeyResponse;
import com.eucalyptus.auth.euare.DeleteGroup;
import com.eucalyptus.auth.euare.DeleteGroupPolicy;
import com.eucalyptus.auth.euare.DeleteGroupPolicyResponse;
import com.eucalyptus.auth.euare.DeleteGroupResponse;
import com.eucalyptus.auth.euare.DeleteLoginProfile;
import com.eucalyptus.auth.euare.DeleteLoginProfileResponse;
import com.eucalyptus.auth.euare.DeleteServerCertificate;
import com.eucalyptus.auth.euare.DeleteServerCertificateResponse;
import com.eucalyptus.auth.euare.DeleteSigningCertificate;
import com.eucalyptus.auth.euare.DeleteSigningCertificateResponse;
import com.eucalyptus.auth.euare.DeleteUser;
import com.eucalyptus.auth.euare.DeleteUserPolicy;
import com.eucalyptus.auth.euare.DeleteUserPolicyResponse;
import com.eucalyptus.auth.euare.DeleteUserResponse;
import com.eucalyptus.auth.euare.EnableMFADevice;
import com.eucalyptus.auth.euare.EnableMFADeviceResponse;
import com.eucalyptus.auth.euare.GetGroup;
import com.eucalyptus.auth.euare.GetGroupPolicy;
import com.eucalyptus.auth.euare.GetGroupPolicyResponse;
import com.eucalyptus.auth.euare.GetGroupResponse;
import com.eucalyptus.auth.euare.GetLoginProfile;
import com.eucalyptus.auth.euare.GetLoginProfileResponse;
import com.eucalyptus.auth.euare.GetServerCertificate;
import com.eucalyptus.auth.euare.GetServerCertificateResponse;
import com.eucalyptus.auth.euare.GetUser;
import com.eucalyptus.auth.euare.GetUserPolicy;
import com.eucalyptus.auth.euare.GetUserPolicyResponse;
import com.eucalyptus.auth.euare.GetUserResponse;
import com.eucalyptus.auth.euare.ListAccessKeys;
import com.eucalyptus.auth.euare.ListAccessKeysResponse;
import com.eucalyptus.auth.euare.ListGroupPolicies;
import com.eucalyptus.auth.euare.ListGroupPoliciesResponse;
import com.eucalyptus.auth.euare.ListGroups;
import com.eucalyptus.auth.euare.ListGroupsForUser;
import com.eucalyptus.auth.euare.ListGroupsForUserResponse;
import com.eucalyptus.auth.euare.ListGroupsResponse;
import com.eucalyptus.auth.euare.ListMFADevices;
import com.eucalyptus.auth.euare.ListMFADevicesResponse;
import com.eucalyptus.auth.euare.ListServerCertificates;
import com.eucalyptus.auth.euare.ListServerCertificatesResponse;
import com.eucalyptus.auth.euare.ListSigningCertificates;
import com.eucalyptus.auth.euare.ListSigningCertificatesResponse;
import com.eucalyptus.auth.euare.ListUserPolicies;
import com.eucalyptus.auth.euare.ListUserPoliciesResponse;
import com.eucalyptus.auth.euare.ListUsers;
import com.eucalyptus.auth.euare.ListUsersResponse;
import com.eucalyptus.auth.euare.PutGroupPolicy;
import com.eucalyptus.auth.euare.PutGroupPolicyResponse;
import com.eucalyptus.auth.euare.PutUserPolicy;
import com.eucalyptus.auth.euare.PutUserPolicyResponse;
import com.eucalyptus.auth.euare.RemoveUserFromGroup;
import com.eucalyptus.auth.euare.RemoveUserFromGroupResponse;
import com.eucalyptus.auth.euare.ResyncMFADevice;
import com.eucalyptus.auth.euare.ResyncMFADeviceResponse;
import com.eucalyptus.auth.euare.UpdateAccessKey;
import com.eucalyptus.auth.euare.UpdateAccessKeyResponse;
import com.eucalyptus.auth.euare.UpdateGroup;
import com.eucalyptus.auth.euare.UpdateGroupResponse;
import com.eucalyptus.auth.euare.UpdateLoginProfile;
import com.eucalyptus.auth.euare.UpdateLoginProfileResponse;
import com.eucalyptus.auth.euare.UpdateServerCertificate;
import com.eucalyptus.auth.euare.UpdateServerCertificateResponse;
import com.eucalyptus.auth.euare.UpdateSigningCertificate;
import com.eucalyptus.auth.euare.UpdateSigningCertificateResponse;
import com.eucalyptus.auth.euare.UpdateUser;
import com.eucalyptus.auth.euare.UpdateUserResponse;
import com.eucalyptus.auth.euare.UploadServerCertificate;
import com.eucalyptus.auth.euare.UploadServerCertificateResponse;
import com.eucalyptus.auth.euare.UploadSigningCertificate;
import com.eucalyptus.auth.euare.UploadSigningCertificateResponse;


public class EuareService {
  public ListGroupsResponse listGroups(ListGroups request) {
    ListGroupsResponse reply = request.getReply( );
    return reply;
  }

  public DeleteAccessKeyResponse deleteAccessKey(DeleteAccessKey request) {
    DeleteAccessKeyResponse reply = request.getReply( );
    return reply;
  }

  public ListSigningCertificatesResponse listSigningCertificates(ListSigningCertificates request) {
    ListSigningCertificatesResponse reply = request.getReply( );
    return reply;
  }

  public UploadSigningCertificateResponse uploadSigningCertificate(UploadSigningCertificate request) {
    UploadSigningCertificateResponse reply = request.getReply( );
    return reply;
  }

  public DeleteUserPolicyResponse deleteUserPolicy(DeleteUserPolicy request) {
    DeleteUserPolicyResponse reply = request.getReply( );
    return reply;
  }

  public PutUserPolicyResponse putUserPolicy(PutUserPolicy request) {
    PutUserPolicyResponse reply = request.getReply( );
    return reply;
  }

  public ListServerCertificatesResponse listServerCertificates(ListServerCertificates request) {
    ListServerCertificatesResponse reply = request.getReply( );
    return reply;
  }

  public GetUserPolicyResponse getUserPolicy(GetUserPolicy request) {
    GetUserPolicyResponse reply = request.getReply( );
    return reply;
  }

  public UpdateLoginProfileResponse updateLoginProfile(UpdateLoginProfile request) {
    UpdateLoginProfileResponse reply = request.getReply( );
    return reply;
  }

  public UpdateServerCertificateResponse updateServerCertificate(UpdateServerCertificate request) {
    UpdateServerCertificateResponse reply = request.getReply( );
    return reply;
  }

  public UpdateUserResponse updateUser(UpdateUser request) {
    UpdateUserResponse reply = request.getReply( );
    return reply;
  }

  public DeleteLoginProfileResponse deleteLoginProfile(DeleteLoginProfile request) {
    DeleteLoginProfileResponse reply = request.getReply( );
    return reply;
  }

  public UpdateSigningCertificateResponse updateSigningCertificate(UpdateSigningCertificate request) {
    UpdateSigningCertificateResponse reply = request.getReply( );
    return reply;
  }

  public DeleteGroupPolicyResponse deleteGroupPolicy(DeleteGroupPolicy request) {
    DeleteGroupPolicyResponse reply = request.getReply( );
    return reply;
  }

  public ListUsersResponse listUsers(ListUsers request) {
    ListUsersResponse reply = request.getReply( );
    return reply;
  }

  public UpdateGroupResponse updateGroup(UpdateGroup request) {
    UpdateGroupResponse reply = request.getReply( );
    return reply;
  }

  public GetServerCertificateResponse getServerCertificate(GetServerCertificate request) {
    GetServerCertificateResponse reply = request.getReply( );
    return reply;
  }

  public PutGroupPolicyResponse putGroupPolicy(PutGroupPolicy request) {
    PutGroupPolicyResponse reply = request.getReply( );
    return reply;
  }

  public CreateUserResponse createUser(CreateUser request) {
    CreateUserResponse reply = request.getReply( );
    return reply;
  }

  public DeleteSigningCertificateResponse deleteSigningCertificate(DeleteSigningCertificate request) {
    DeleteSigningCertificateResponse reply = request.getReply( );
    return reply;
  }

  public EnableMFADeviceResponse enableMFADevice(EnableMFADevice request) {
    EnableMFADeviceResponse reply = request.getReply( );
    return reply;
  }

  public ListUserPoliciesResponse listUserPolicies(ListUserPolicies request) {
    ListUserPoliciesResponse reply = request.getReply( );
    return reply;
  }

  public ListAccessKeysResponse listAccessKeys(ListAccessKeys request) {
    ListAccessKeysResponse reply = request.getReply( );
    return reply;
  }

  public GetLoginProfileResponse getLoginProfile(GetLoginProfile request) {
    GetLoginProfileResponse reply = request.getReply( );
    return reply;
  }

  public ListGroupsForUserResponse listGroupsForUser(ListGroupsForUser request) {
    ListGroupsForUserResponse reply = request.getReply( );
    return reply;
  }

  public CreateGroupResponse createGroup(CreateGroup request) {
    CreateGroupResponse reply = request.getReply( );
    return reply;
  }

  public UploadServerCertificateResponse uploadServerCertificate(UploadServerCertificate request) {
    UploadServerCertificateResponse reply = request.getReply( );
    return reply;
  }

  public GetGroupPolicyResponse getGroupPolicy(GetGroupPolicy request) {
    GetGroupPolicyResponse reply = request.getReply( );
    return reply;
  }

  public DeleteUserResponse deleteUser(DeleteUser request) {
    DeleteUserResponse reply = request.getReply( );
    return reply;
  }

  public DeactivateMFADeviceResponse deactivateMFADevice(DeactivateMFADevice request) {
    DeactivateMFADeviceResponse reply = request.getReply( );
    return reply;
  }

  public RemoveUserFromGroupResponse removeUserFromGroup(RemoveUserFromGroup request) {
    RemoveUserFromGroupResponse reply = request.getReply( );
    return reply;
  }

  public DeleteServerCertificateResponse deleteServerCertificate(DeleteServerCertificate request) {
    DeleteServerCertificateResponse reply = request.getReply( );
    return reply;
  }

  public ListGroupPoliciesResponse listGroupPolicies(ListGroupPolicies request) {
    ListGroupPoliciesResponse reply = request.getReply( );
    return reply;
  }

  public CreateLoginProfileResponse createLoginProfile(CreateLoginProfile request) {
    CreateLoginProfileResponse reply = request.getReply( );
    return reply;
  }

  public CreateAccessKeyResponse createAccessKey(CreateAccessKey request) {
    CreateAccessKeyResponse reply = request.getReply( );
    return reply;
  }

  public GetUserResponse getUser(GetUser request) {
    GetUserResponse reply = request.getReply( );
    return reply;
  }

  public ResyncMFADeviceResponse resyncMFADevice(ResyncMFADevice request) {
    ResyncMFADeviceResponse reply = request.getReply( );
    return reply;
  }

  public ListMFADevicesResponse listMFADevices(ListMFADevices request) {
    ListMFADevicesResponse reply = request.getReply( );
    return reply;
  }

  public UpdateAccessKeyResponse updateAccessKey(UpdateAccessKey request) {
    UpdateAccessKeyResponse reply = request.getReply( );
    return reply;
  }

  public AddUserToGroupResponse addUserToGroup(AddUserToGroup request) {
    AddUserToGroupResponse reply = request.getReply( );
    return reply;
  }

  public GetGroupResponse getGroup(GetGroup request) {
    GetGroupResponse reply = request.getReply( );
    return reply;
  }

  public DeleteGroupResponse deleteGroup(DeleteGroup request) {
    DeleteGroupResponse reply = request.getReply( );
    return reply;
  }

}
