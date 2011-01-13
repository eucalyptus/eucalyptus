package com.eucalyptus.auth.euare;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.euare.AddUserToGroupResponseType;
import com.eucalyptus.auth.euare.AddUserToGroupType;
import com.eucalyptus.auth.euare.CreateAccessKeyResponseType;
import com.eucalyptus.auth.euare.CreateAccessKeyType;
import com.eucalyptus.auth.euare.CreateGroupResponseType;
import com.eucalyptus.auth.euare.CreateGroupType;
import com.eucalyptus.auth.euare.CreateLoginProfileResponseType;
import com.eucalyptus.auth.euare.CreateLoginProfileType;
import com.eucalyptus.auth.euare.CreateUserResponseType;
import com.eucalyptus.auth.euare.CreateUserType;
import com.eucalyptus.auth.euare.DeactivateMFADeviceResponseType;
import com.eucalyptus.auth.euare.DeactivateMFADeviceType;
import com.eucalyptus.auth.euare.DeleteAccessKeyResponseType;
import com.eucalyptus.auth.euare.DeleteAccessKeyType;
import com.eucalyptus.auth.euare.DeleteGroupPolicyResponseType;
import com.eucalyptus.auth.euare.DeleteGroupPolicyType;
import com.eucalyptus.auth.euare.DeleteGroupResponseType;
import com.eucalyptus.auth.euare.DeleteGroupType;
import com.eucalyptus.auth.euare.DeleteLoginProfileResponseType;
import com.eucalyptus.auth.euare.DeleteLoginProfileType;
import com.eucalyptus.auth.euare.DeleteServerCertificateResponseType;
import com.eucalyptus.auth.euare.DeleteServerCertificateType;
import com.eucalyptus.auth.euare.DeleteSigningCertificateResponseType;
import com.eucalyptus.auth.euare.DeleteSigningCertificateType;
import com.eucalyptus.auth.euare.DeleteUserPolicyResponseType;
import com.eucalyptus.auth.euare.DeleteUserPolicyType;
import com.eucalyptus.auth.euare.DeleteUserResponseType;
import com.eucalyptus.auth.euare.DeleteUserType;
import com.eucalyptus.auth.euare.EnableMFADeviceResponseType;
import com.eucalyptus.auth.euare.EnableMFADeviceType;
import com.eucalyptus.auth.euare.GetGroupPolicyResponseType;
import com.eucalyptus.auth.euare.GetGroupPolicyType;
import com.eucalyptus.auth.euare.GetGroupResponseType;
import com.eucalyptus.auth.euare.GetGroupType;
import com.eucalyptus.auth.euare.GetLoginProfileResponseType;
import com.eucalyptus.auth.euare.GetLoginProfileType;
import com.eucalyptus.auth.euare.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.GetServerCertificateType;
import com.eucalyptus.auth.euare.GetUserPolicyResponseType;
import com.eucalyptus.auth.euare.GetUserPolicyType;
import com.eucalyptus.auth.euare.GetUserResponseType;
import com.eucalyptus.auth.euare.GetUserType;
import com.eucalyptus.auth.euare.ListAccessKeysResponseType;
import com.eucalyptus.auth.euare.ListAccessKeysType;
import com.eucalyptus.auth.euare.ListGroupPoliciesResponseType;
import com.eucalyptus.auth.euare.ListGroupPoliciesType;
import com.eucalyptus.auth.euare.ListGroupsForUserResponseType;
import com.eucalyptus.auth.euare.ListGroupsForUserType;
import com.eucalyptus.auth.euare.ListGroupsResponseType;
import com.eucalyptus.auth.euare.ListGroupsType;
import com.eucalyptus.auth.euare.ListMFADevicesResponseType;
import com.eucalyptus.auth.euare.ListMFADevicesType;
import com.eucalyptus.auth.euare.ListServerCertificatesResponseType;
import com.eucalyptus.auth.euare.ListServerCertificatesType;
import com.eucalyptus.auth.euare.ListSigningCertificatesResponseType;
import com.eucalyptus.auth.euare.ListSigningCertificatesType;
import com.eucalyptus.auth.euare.ListUserPoliciesResponseType;
import com.eucalyptus.auth.euare.ListUserPoliciesType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.PutGroupPolicyResponseType;
import com.eucalyptus.auth.euare.PutGroupPolicyType;
import com.eucalyptus.auth.euare.PutUserPolicyResponseType;
import com.eucalyptus.auth.euare.PutUserPolicyType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.ResyncMFADeviceResponseType;
import com.eucalyptus.auth.euare.ResyncMFADeviceType;
import com.eucalyptus.auth.euare.UpdateAccessKeyResponseType;
import com.eucalyptus.auth.euare.UpdateAccessKeyType;
import com.eucalyptus.auth.euare.UpdateGroupResponseType;
import com.eucalyptus.auth.euare.UpdateGroupType;
import com.eucalyptus.auth.euare.UpdateLoginProfileResponseType;
import com.eucalyptus.auth.euare.UpdateLoginProfileType;
import com.eucalyptus.auth.euare.UpdateServerCertificateResponseType;
import com.eucalyptus.auth.euare.UpdateServerCertificateType;
import com.eucalyptus.auth.euare.UpdateSigningCertificateResponseType;
import com.eucalyptus.auth.euare.UpdateSigningCertificateType;
import com.eucalyptus.auth.euare.UpdateUserResponseType;
import com.eucalyptus.auth.euare.UpdateUserType;
import com.eucalyptus.auth.euare.UploadServerCertificateResponseType;
import com.eucalyptus.auth.euare.UploadServerCertificateType;
import com.eucalyptus.auth.euare.UploadSigningCertificateResponseType;
import com.eucalyptus.auth.euare.UploadSigningCertificateType;


public class EuareService {
  
  static private final Logger LOG = Logger.getLogger( EuareService.class );
  
  public ListGroupsResponseType listGroups(ListGroupsType request) {
    ListGroupsResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteAccessKeyResponseType deleteAccessKey(DeleteAccessKeyType request) {
    DeleteAccessKeyResponseType reply = request.getReply( );
    return reply;
  }

  public ListSigningCertificatesResponseType listSigningCertificates(ListSigningCertificatesType request) {
    ListSigningCertificatesResponseType reply = request.getReply( );
    return reply;
  }

  public UploadSigningCertificateResponseType uploadSigningCertificate(UploadSigningCertificateType request) {
    UploadSigningCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteUserPolicyResponseType deleteUserPolicy(DeleteUserPolicyType request) {
    DeleteUserPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public PutUserPolicyResponseType putUserPolicy(PutUserPolicyType request) {
    PutUserPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public ListServerCertificatesResponseType listServerCertificates(ListServerCertificatesType request) {
    ListServerCertificatesResponseType reply = request.getReply( );
    return reply;
  }

  public GetUserPolicyResponseType getUserPolicy(GetUserPolicyType request) {
    GetUserPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateLoginProfileResponseType updateLoginProfile(UpdateLoginProfileType request) {
    UpdateLoginProfileResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateServerCertificateResponseType updateServerCertificate(UpdateServerCertificateType request) {
    UpdateServerCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateUserResponseType updateUser(UpdateUserType request) {
    UpdateUserResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteLoginProfileResponseType deleteLoginProfile(DeleteLoginProfileType request) {
    DeleteLoginProfileResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateSigningCertificateResponseType updateSigningCertificate(UpdateSigningCertificateType request) {
    UpdateSigningCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteGroupPolicyResponseType deleteGroupPolicy(DeleteGroupPolicyType request) {
    DeleteGroupPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public ListUsersResponseType listUsers(ListUsersType request) {
    LOG.debug( "YE:" + "processing ListUsers" );
    ListUsersResponseType response = request.getReply( );
    ArrayList<UserType> users = response.getListUsersResult( ).getUsers( ).getMemberList( );
    UserType user = new UserType( );
    user.setArn( "arn:aws:iam::123456789012:user/division_abc/subdivision_xyz/engineering/Andrew" );
    user.setPath( "/division_abc/subdivision_xyz/engineering/" );
    user.setUserId( "AID2MAB8DPLSRHEXAMPLE" );
    user.setUserName( "Andrew" );
    users.add( user );
    response.getListUsersResult( ).setIsTruncated( false );
    return response;
  }

  public UpdateGroupResponseType updateGroup(UpdateGroupType request) {
    UpdateGroupResponseType reply = request.getReply( );
    return reply;
  }

  public GetServerCertificateResponseType getServerCertificate(GetServerCertificateType request) {
    GetServerCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public PutGroupPolicyResponseType putGroupPolicy(PutGroupPolicyType request) {
    PutGroupPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public CreateUserResponseType createUser(CreateUserType request) {
    CreateUserResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteSigningCertificateResponseType deleteSigningCertificate(DeleteSigningCertificateType request) {
    DeleteSigningCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public EnableMFADeviceResponseType enableMFADevice(EnableMFADeviceType request) {
    EnableMFADeviceResponseType reply = request.getReply( );
    return reply;
  }

  public ListUserPoliciesResponseType listUserPolicies(ListUserPoliciesType request) {
    ListUserPoliciesResponseType reply = request.getReply( );
    return reply;
  }

  public ListAccessKeysResponseType listAccessKeys(ListAccessKeysType request) {
    ListAccessKeysResponseType reply = request.getReply( );
    return reply;
  }

  public GetLoginProfileResponseType getLoginProfile(GetLoginProfileType request) {
    GetLoginProfileResponseType reply = request.getReply( );
    return reply;
  }

  public ListGroupsForUserResponseType listGroupsForUser(ListGroupsForUserType request) {
    ListGroupsForUserResponseType reply = request.getReply( );
    return reply;
  }

  public CreateGroupResponseType createGroup(CreateGroupType request) {
    CreateGroupResponseType reply = request.getReply( );
    return reply;
  }

  public UploadServerCertificateResponseType uploadServerCertificate(UploadServerCertificateType request) {
    UploadServerCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public GetGroupPolicyResponseType getGroupPolicy(GetGroupPolicyType request) {
    GetGroupPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteUserResponseType deleteUser(DeleteUserType request) {
    DeleteUserResponseType reply = request.getReply( );
    return reply;
  }

  public DeactivateMFADeviceResponseType deactivateMFADevice(DeactivateMFADeviceType request) {
    DeactivateMFADeviceResponseType reply = request.getReply( );
    return reply;
  }

  public RemoveUserFromGroupResponseType removeUserFromGroup(RemoveUserFromGroupType request) {
    RemoveUserFromGroupResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteServerCertificateResponseType deleteServerCertificate(DeleteServerCertificateType request) {
    DeleteServerCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public ListGroupPoliciesResponseType listGroupPolicies(ListGroupPoliciesType request) {
    ListGroupPoliciesResponseType reply = request.getReply( );
    return reply;
  }

  public CreateLoginProfileResponseType createLoginProfile(CreateLoginProfileType request) {
    CreateLoginProfileResponseType reply = request.getReply( );
    return reply;
  }

  public CreateAccessKeyResponseType createAccessKey(CreateAccessKeyType request) {
    CreateAccessKeyResponseType reply = request.getReply( );
    return reply;
  }

  public GetUserResponseType getUser(GetUserType request) {
    GetUserResponseType reply = request.getReply( );
    return reply;
  }

  public ResyncMFADeviceResponseType resyncMFADevice(ResyncMFADeviceType request) {
    ResyncMFADeviceResponseType reply = request.getReply( );
    return reply;
  }

  public ListMFADevicesResponseType listMFADevices(ListMFADevicesType request) {
    ListMFADevicesResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateAccessKeyResponseType updateAccessKey(UpdateAccessKeyType request) {
    UpdateAccessKeyResponseType reply = request.getReply( );
    return reply;
  }

  public AddUserToGroupResponseType addUserToGroup(AddUserToGroupType request) {
    AddUserToGroupResponseType reply = request.getReply( );
    return reply;
  }

  public GetGroupResponseType getGroup(GetGroupType request) {
    GetGroupResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteGroupResponseType deleteGroup(DeleteGroupType request) {
    DeleteGroupResponseType reply = request.getReply( );
    return reply;
  }

}
