package com.eucalyptus.auth.euare;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;

public class ListGroups extends EuareMessage {
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListGroups() {  }
}
public class UpdateUserResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateUserResponse() {  }
}
public class EuareMessage extends BaseMessage {
}
public class DeleteGroupResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteGroupResponse() {  }
}
public class GetLoginProfile extends EuareMessage {
  String userName;
  public GetLoginProfile() {  }
}
public class UploadServerCertificate extends EuareMessage {
  String path;
  String serverCertificateName;
  String certificateBody;
  String privateKey;
  String certificateChain;
  public UploadServerCertificate() {  }
}
public class GetGroupResult extends EucalyptusData {
  Group group;
  UserListType users;
  Boolean isTruncated;
  String marker;
  public GetGroupResult() {  }
}
public class DeleteUserPolicy extends EuareMessage {
  String userName;
  String policyName;
  public DeleteUserPolicy() {  }
}
public class CreateGroupResponse extends EuareMessage {
  CreateGroupResult createGroupResult;
  ResponseMetadata responseMetadata;
  public CreateGroupResponse() {  }
}
public class UpdateAccessKey extends EuareMessage {
  String userName;
  String accessKeyId;
  String status;
  public UpdateAccessKey() {  }
}
public class ServerCertificateMetadata extends EucalyptusData {
  String path;
  String serverCertificateName;
  String serverCertificateId;
  String arn;
  Date uploadDate;
  public ServerCertificateMetadata() {  }
}
public class CreateAccessKeyResult extends EucalyptusData {
  AccessKey accessKey;
  public CreateAccessKeyResult() {  }
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class ListUsers extends EuareMessage {
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListUsers() {  }
}
public class GroupListType extends EucalyptusData {
  public GroupListType() {  }
  ArrayList<Group> memberList = new ArrayList<Group>();
}
public class DeleteLoginProfileResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteLoginProfileResponse() {  }
}
public class DeleteUserResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteUserResponse() {  }
}
public class ListGroupsForUserResult extends EucalyptusData {
  GroupListType groups;
  Boolean isTruncated;
  String marker;
  public ListGroupsForUserResult() {  }
}
public class UpdateServerCertificateResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateServerCertificateResponse() {  }
}
public class ResyncMFADeviceResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public ResyncMFADeviceResponse() {  }
}
public class CreateAccessKey extends EuareMessage {
  String userName;
  public CreateAccessKey() {  }
}
public class CertificateListType extends EucalyptusData {
  public CertificateListType() {  }
  ArrayList<SigningCertificate> memberList = new ArrayList<SigningCertificate>();
}
public class GetUser extends EuareMessage {
  String userName;
  public GetUser() {  }
}
public class ResyncMFADevice extends EuareMessage {
  String userName;
  String serialNumber;
  String authenticationCode1;
  String authenticationCode2;
  public ResyncMFADevice() {  }
}
public class ServerCertificateMetadataListType extends EucalyptusData {
  public ServerCertificateMetadataListType() {  }
  ArrayList<ServerCertificateMetadata> memberList = new ArrayList<ServerCertificateMetadata>();
}
public class ListGroupsResponse extends EuareMessage {
  ListGroupsResult listGroupsResult;
  ResponseMetadata responseMetadata;
  public ListGroupsResponse() {  }
}
public class ErrorResponse extends EucalyptusData {
  String requestId;
  public ErrorResponse() {  }
  ArrayList<Error> errorList = new ArrayList<Error>();
}
public class ListMFADevicesResponse extends EuareMessage {
  ListMFADevicesResult listMFADevicesResult;
  ResponseMetadata responseMetadata;
  public ListMFADevicesResponse() {  }
}
public class GetServerCertificateResult extends EucalyptusData {
  ServerCertificate serverCertificate;
  public GetServerCertificateResult() {  }
}
public class EnableMFADevice extends EuareMessage {
  String userName;
  String serialNumber;
  String authenticationCode1;
  String authenticationCode2;
  public EnableMFADevice() {  }
}
public class ListGroupPoliciesResult extends EucalyptusData {
  PolicyNameListType policyNames;
  Boolean isTruncated;
  String marker;
  public ListGroupPoliciesResult() {  }
}
public class GetUserResult extends EucalyptusData {
  User user;
  public GetUserResult() {  }
}
public class GetGroup extends EuareMessage {
  String groupName;
  String marker;
  BigInteger maxItems;
  public GetGroup() {  }
}
public class PolicyNameListType extends EucalyptusData {
  public PolicyNameListType() {  }
  ArrayList<String> memberList = new ArrayList<String>();
}
public class PutGroupPolicyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public PutGroupPolicyResponse() {  }
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  ErrorDetail detail;
  public Error() {  }
}
public class GetUserPolicy extends EuareMessage {
  String userName;
  String policyName;
  public GetUserPolicy() {  }
}
public class CreateLoginProfile extends EuareMessage {
  String userName;
  String password;
  public CreateLoginProfile() {  }
}
public class PutGroupPolicy extends EuareMessage {
  String groupName;
  String policyName;
  String policyDocument;
  public PutGroupPolicy() {  }
}
public class UploadSigningCertificate extends EuareMessage {
  String userName;
  String certificateBody;
  public UploadSigningCertificate() {  }
}
public class ListAccessKeysResponse extends EuareMessage {
  ListAccessKeysResult listAccessKeysResult;
  ResponseMetadata responseMetadata;
  public ListAccessKeysResponse() {  }
}
public class GetServerCertificate extends EuareMessage {
  String serverCertificateName;
  public GetServerCertificate() {  }
}
public class GetLoginProfileResponse extends EuareMessage {
  GetLoginProfileResult getLoginProfileResult;
  ResponseMetadata responseMetadata;
  public GetLoginProfileResponse() {  }
}
public class ListSigningCertificatesResult extends EucalyptusData {
  CertificateListType certificates;
  Boolean isTruncated;
  String marker;
  public ListSigningCertificatesResult() {  }
}
public class CreateUser extends EuareMessage {
  String path;
  String userName;
  public CreateUser() {  }
}
public class ListAccessKeysResult extends EucalyptusData {
  AccessKeyMetadataListType accessKeyMetadata;
  Boolean isTruncated;
  String marker;
  public ListAccessKeysResult() {  }
}
public class GetGroupPolicyResult extends EucalyptusData {
  String groupName;
  String policyName;
  String policyDocument;
  public GetGroupPolicyResult() {  }
}
public class ListMFADevices extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListMFADevices() {  }
}
public class ListUsersResult extends EucalyptusData {
  UserListType users;
  Boolean isTruncated;
  String marker;
  public ListUsersResult() {  }
}
public class DeleteGroup extends EuareMessage {
  String groupName;
  public DeleteGroup() {  }
}
public class UpdateSigningCertificate extends EuareMessage {
  String userName;
  String certificateId;
  String status;
  public UpdateSigningCertificate() {  }
}
public class ListServerCertificates extends EuareMessage {
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListServerCertificates() {  }
}
public class AccessKeyMetadataListType extends EucalyptusData {
  public AccessKeyMetadataListType() {  }
  ArrayList<AccessKeyMetadata> memberList = new ArrayList<AccessKeyMetadata>();
}
public class UpdateSigningCertificateResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateSigningCertificateResponse() {  }
}
public class DeleteGroupPolicy extends EuareMessage {
  String groupName;
  String policyName;
  public DeleteGroupPolicy() {  }
}
public class UpdateAccessKeyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateAccessKeyResponse() {  }
}
public class RemoveUserFromGroup extends EuareMessage {
  String groupName;
  String userName;
  public RemoveUserFromGroup() {  }
}
public class MfaDeviceListType extends EucalyptusData {
  public MfaDeviceListType() {  }
  ArrayList<MFADevice> memberList = new ArrayList<MFADevice>();
}
public class ListGroupPolicies extends EuareMessage {
  String groupName;
  String marker;
  BigInteger maxItems;
  public ListGroupPolicies() {  }
}
public class CreateGroupResult extends EucalyptusData {
  Group group;
  public CreateGroupResult() {  }
}
public class ListGroupsForUser extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListGroupsForUser() {  }
}
public class LoginProfile extends EuareMessage {
  String userName;
  public LoginProfile() {  }
}
public class ListUserPoliciesResponse extends EuareMessage {
  ListUserPoliciesResult listUserPoliciesResult;
  ResponseMetadata responseMetadata;
  public ListUserPoliciesResponse() {  }
}
public class AddUserToGroupResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public AddUserToGroupResponse() {  }
}
public class ListSigningCertificatesResponse extends EuareMessage {
  ListSigningCertificatesResult listSigningCertificatesResult;
  ResponseMetadata responseMetadata;
  public ListSigningCertificatesResponse() {  }
}
public class DeleteAccessKeyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteAccessKeyResponse() {  }
}
public class DeleteLoginProfile extends EuareMessage {
  String userName;
  public DeleteLoginProfile() {  }
}
public class UpdateGroupResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateGroupResponse() {  }
}
public class UpdateLoginProfile extends EuareMessage {
  String userName;
  String password;
  public UpdateLoginProfile() {  }
}
public class DeleteAccessKey extends EuareMessage {
  String userName;
  String accessKeyId;
  public DeleteAccessKey() {  }
}
public class GetUserPolicyResponse extends EuareMessage {
  GetUserPolicyResult getUserPolicyResult;
  ResponseMetadata responseMetadata;
  public GetUserPolicyResponse() {  }
}
public class ListUserPolicies extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListUserPolicies() {  }
}
public class UploadServerCertificateResult extends EucalyptusData {
  ServerCertificateMetadata serverCertificateMetadata;
  public UploadServerCertificateResult() {  }
}
public class UpdateServerCertificate extends EuareMessage {
  String serverCertificateName;
  String newPath;
  String newServerCertificateName;
  public UpdateServerCertificate() {  }
}
public class ListUsersResponse extends EuareMessage {
  ListUsersResult listUsersResult;
  ResponseMetadata responseMetadata;
  public ListUsersResponse() {  }
}
public class MFADevice extends EuareMessage {
  String userName;
  String serialNumber;
  public MFADevice() {  }
}
public class ListGroupsResult extends EucalyptusData {
  GroupListType groups;
  Boolean isTruncated;
  String marker;
  public ListGroupsResult() {  }
}
public class PutUserPolicyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public PutUserPolicyResponse() {  }
}
public class AccessKey extends EuareMessage {
  String userName;
  String accessKeyId;
  String status;
  String secretAccessKey;
  Date createDate;
  public AccessKey() {  }
}
public class DeleteSigningCertificate extends EuareMessage {
  String userName;
  String certificateId;
  public DeleteSigningCertificate() {  }
}
public class CreateLoginProfileResponse extends EuareMessage {
  CreateLoginProfileResult createLoginProfileResult;
  ResponseMetadata responseMetadata;
  public CreateLoginProfileResponse() {  }
}
public class DeleteUser extends EuareMessage {
  String userName;
  public DeleteUser() {  }
}
public class DeleteSigningCertificateResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteSigningCertificateResponse() {  }
}
public class ServerCertificate extends EuareMessage {
  ServerCertificateMetadata serverCertificateMetadata;
  String certificateBody;
  String certificateChain;
  public ServerCertificate() {  }
}
public class CreateGroup extends EuareMessage {
  String path;
  String groupName;
  public CreateGroup() {  }
}
public class ListUserPoliciesResult extends EucalyptusData {
  PolicyNameListType policyNames;
  Boolean isTruncated;
  String marker;
  public ListUserPoliciesResult() {  }
}
public class DeleteUserPolicyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteUserPolicyResponse() {  }
}
public class ListServerCertificatesResult extends EucalyptusData {
  ServerCertificateMetadataListType serverCertificateMetadataList;
  Boolean isTruncated;
  String marker;
  public ListServerCertificatesResult() {  }
}
public class ListServerCertificatesResponse extends EuareMessage {
  ListServerCertificatesResult listServerCertificatesResult;
  ResponseMetadata responseMetadata;
  public ListServerCertificatesResponse() {  }
}
public class GetGroupPolicyResponse extends EuareMessage {
  GetGroupPolicyResult getGroupPolicyResult;
  ResponseMetadata responseMetadata;
  public GetGroupPolicyResponse() {  }
}
public class UpdateUser extends EuareMessage {
  String userName;
  String newPath;
  String newUserName;
  public UpdateUser() {  }
}
public class GetLoginProfileResult extends EucalyptusData {
  LoginProfile loginProfile;
  public GetLoginProfileResult() {  }
}
public class CreateAccessKeyResponse extends EuareMessage {
  CreateAccessKeyResult createAccessKeyResult;
  ResponseMetadata responseMetadata;
  public CreateAccessKeyResponse() {  }
}
public class AddUserToGroup extends EuareMessage {
  String groupName;
  String userName;
  public AddUserToGroup() {  }
}
public class CreateUserResponse extends EuareMessage {
  CreateUserResult createUserResult;
  ResponseMetadata responseMetadata;
  public CreateUserResponse() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class GetServerCertificateResponse extends EuareMessage {
  GetServerCertificateResult getServerCertificateResult;
  ResponseMetadata responseMetadata;
  public GetServerCertificateResponse() {  }
}
public class ListGroupPoliciesResponse extends EuareMessage {
  ListGroupPoliciesResult listGroupPoliciesResult;
  ResponseMetadata responseMetadata;
  public ListGroupPoliciesResponse() {  }
}
public class EnableMFADeviceResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public EnableMFADeviceResponse() {  }
}
public class GetUserResponse extends EuareMessage {
  GetUserResult getUserResult;
  ResponseMetadata responseMetadata;
  public GetUserResponse() {  }
}
public class UserListType extends EucalyptusData {
  public UserListType() {  }
  ArrayList<User> memberList = new ArrayList<User>();
}
public class Group extends EuareMessage {
  String path;
  String groupName;
  String groupId;
  String arn;
  public Group() {  }
}
public class GetGroupPolicy extends EuareMessage {
  String groupName;
  String policyName;
  public GetGroupPolicy() {  }
}
public class SigningCertificate extends EuareMessage {
  String userName;
  String certificateId;
  String certificateBody;
  String status;
  Date uploadDate;
  public SigningCertificate() {  }
}
public class CreateUserResult extends EucalyptusData {
  User user;
  public CreateUserResult() {  }
}
public class RemoveUserFromGroupResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public RemoveUserFromGroupResponse() {  }
}
public class UploadSigningCertificateResponse extends EuareMessage {
  UploadSigningCertificateResult uploadSigningCertificateResult;
  ResponseMetadata responseMetadata;
  public UploadSigningCertificateResponse() {  }
}
public class AccessKeyMetadata extends EucalyptusData {
  String userName;
  String accessKeyId;
  String status;
  Date createDate;
  public AccessKeyMetadata() {  }
}
public class CreateLoginProfileResult extends EucalyptusData {
  LoginProfile loginProfile;
  public CreateLoginProfileResult() {  }
}
public class DeactivateMFADevice extends EuareMessage {
  String userName;
  String serialNumber;
  public DeactivateMFADevice() {  }
}
public class UpdateGroup extends EuareMessage {
  String groupName;
  String newPath;
  String newGroupName;
  public UpdateGroup() {  }
}
public class DeleteServerCertificateResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteServerCertificateResponse() {  }
}
public class GetUserPolicyResult extends EucalyptusData {
  String userName;
  String policyName;
  String policyDocument;
  public GetUserPolicyResult() {  }
}
public class GetGroupResponse extends EuareMessage {
  GetGroupResult getGroupResult;
  ResponseMetadata responseMetadata;
  public GetGroupResponse() {  }
}
public class UpdateLoginProfileResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public UpdateLoginProfileResponse() {  }
}
public class PutUserPolicy extends EuareMessage {
  String userName;
  String policyName;
  String policyDocument;
  public PutUserPolicy() {  }
}
public class DeactivateMFADeviceResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeactivateMFADeviceResponse() {  }
}
public class DeleteGroupPolicyResponse extends EuareMessage {
  ResponseMetadata responseMetadata;
  public DeleteGroupPolicyResponse() {  }
}
public class ListSigningCertificates extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListSigningCertificates() {  }
}
public class ListMFADevicesResult extends EucalyptusData {
  MfaDeviceListType mfaDevices;
  Boolean isTruncated;
  String marker;
  public ListMFADevicesResult() {  }
}
public class UploadSigningCertificateResult extends EucalyptusData {
  SigningCertificate certificate;
  public UploadSigningCertificateResult() {  }
}
public class ListAccessKeys extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListAccessKeys() {  }
}
public class UploadServerCertificateResponse extends EuareMessage {
  UploadServerCertificateResult uploadServerCertificateResult;
  ResponseMetadata responseMetadata;
  public UploadServerCertificateResponse() {  }
}
public class DeleteServerCertificate extends EuareMessage {
  String serverCertificateName;
  public DeleteServerCertificate() {  }
}
public class ListGroupsForUserResponse extends EuareMessage {
  ListGroupsForUserResult listGroupsForUserResult;
  ResponseMetadata responseMetadata;
  public ListGroupsForUserResponse() {  }
}
public class User extends EuareMessage {
  String path;
  String userName;
  String userId;
  String arn;
  public User() {  }
}
