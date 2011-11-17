package com.eucalyptus.auth.euare;

import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.ComponentId.ComponentMessage;
import com.eucalyptus.component.id.Euare;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import java.util.Date;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

@ComponentMessage(Euare.class)
public class EuareMessage extends BaseMessage {
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_PUTGROUPPOLICY )
public class PutGroupPolicyType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String policyName;
  String policyDocument;
  public PutGroupPolicyType() {  }
}
public class UploadSigningCertificateResultType extends EucalyptusData {
  SigningCertificateType certificate = new SigningCertificateType( );
  public UploadSigningCertificateResultType() {  }
}
public class ListAccessKeysResponseType extends EuareMessage {
  ListAccessKeysResultType listAccessKeysResult = new ListAccessKeysResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListAccessKeysResponseType() {  }
}
public class AccessKeyMetadataListTypeType extends EucalyptusData {
  public AccessKeyMetadataListTypeType() {  }
  ArrayList<AccessKeyMetadataType> memberList = new ArrayList<AccessKeyMetadataType>();
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTGROUPPOLICIES )
public class ListGroupPoliciesType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String marker;
  BigInteger maxItems;
  public ListGroupPoliciesType() {  }
}
public class ListUserPoliciesResponseType extends EuareMessage {
  ListUserPoliciesResultType listUserPoliciesResult = new ListUserPoliciesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListUserPoliciesResponseType() {  }
}
public class ResyncMFADeviceResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ResyncMFADeviceResponseType() {  }
}
public class ListMFADevicesResultType extends EucalyptusData {
  MfaDeviceListTypeType mfaDevices = new MfaDeviceListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListMFADevicesResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATELOGINPROFILE )
public class UpdateLoginProfileType extends EuareMessage {
  String delegateAccount;
  String userName;
  String password;
  public UpdateLoginProfileType() {  }
}
public class GetLoginProfileResponseType extends EuareMessage {
  GetLoginProfileResultType getLoginProfileResult = new GetLoginProfileResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetLoginProfileResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETGROUP )
public class GetGroupType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String marker;
  BigInteger maxItems;
  public GetGroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_CREATEGROUP )
public class CreateGroupType extends EuareMessage {
  String delegateAccount;
  String path;
  String groupName;
  public CreateGroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATESERVERCERTIFICATE )
public class UpdateServerCertificateType extends EuareMessage {
  String serverCertificateName;
  String newPath;
  String newServerCertificateName;
  public UpdateServerCertificateType() {  }
}
public class MfaDeviceListTypeType extends EucalyptusData {
  public MfaDeviceListTypeType() {  }
  ArrayList<MFADeviceType> memberList = new ArrayList<MFADeviceType>();
}
public class UploadServerCertificateResponseType extends EuareMessage {
  UploadServerCertificateResultType uploadServerCertificateResult = new UploadServerCertificateResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UploadServerCertificateResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEGROUPPOLICY )
public class DeleteGroupPolicyType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String policyName;
  public DeleteGroupPolicyType() {  }
}
public class DeleteAccessKeyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteAccessKeyResponseType() {  }
}
public class AccessKeyType extends EuareMessage {
  String userName;
  String accessKeyId;
  String status;
  String secretAccessKey;
  Date createDate;
  public AccessKeyType() {  }
}
public class AddUserToGroupResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public AddUserToGroupResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETESERVERCERTIFICATE )
public class DeleteServerCertificateType extends EuareMessage {
  String serverCertificateName;
  public DeleteServerCertificateType() {  }
}
public class UpdateAccessKeyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateAccessKeyResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPLOADSERVERCERTIFICATE )
public class UploadServerCertificateType extends EuareMessage {
  String path;
  String serverCertificateName;
  String certificateBody;
  String privateKey;
  String certificateChain;
  public UploadServerCertificateType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATEGROUP )
public class UpdateGroupType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String newPath;
  String newGroupName;
  public UpdateGroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_ADDUSERTOGROUP )
public class AddUserToGroupType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String userName;
  public AddUserToGroupType() {  }
}
public class PutGroupPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public PutGroupPolicyResponseType() {  }
}
public class ListSigningCertificatesResponseType extends EuareMessage {
  ListSigningCertificatesResultType listSigningCertificatesResult = new ListSigningCertificatesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListSigningCertificatesResponseType() {  }
}
public class GetUserResponseType extends EuareMessage {
  GetUserResultType getUserResult = new GetUserResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetUserResponseType() {  }
}
public class AccessKeyMetadataType extends EucalyptusData {
  String userName;
  String accessKeyId;
  String status;
  Date createDate;
  public AccessKeyMetadataType() {  }
}
public class GetGroupPolicyResponseType extends EuareMessage {
  GetGroupPolicyResultType getGroupPolicyResult = new GetGroupPolicyResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetGroupPolicyResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_REMOVEUSERFROMGROUP )
public class RemoveUserFromGroupType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String userName;
  public RemoveUserFromGroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTSIGNINGCERTIFICATES )
public class ListSigningCertificatesType extends EuareMessage {
  String delegateAccount;
  String userName;
  String marker;
  BigInteger maxItems;
  public ListSigningCertificatesType() {  }
}
public class ListServerCertificatesResponseType extends EuareMessage {
  ListServerCertificatesResultType listServerCertificatesResult = new ListServerCertificatesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListServerCertificatesResponseType() {  }
}
public class DeleteGroupResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteGroupResponseType() {  }
}
public class UpdateGroupResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateGroupResponseType() {  }
}
public class CreateAccessKeyResponseType extends EuareMessage {
  CreateAccessKeyResultType createAccessKeyResult = new CreateAccessKeyResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateAccessKeyResponseType() {  }
}
public class ListGroupsResponseType extends EuareMessage {
  ListGroupsResultType listGroupsResult = new ListGroupsResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListGroupsResponseType() {  }
}
public class ServerCertificateType extends EuareMessage {
  ServerCertificateMetadataType serverCertificateMetadata = new ServerCertificateMetadataType( );
  String certificateBody;
  String certificateChain;
  public ServerCertificateType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_CREATEACCESSKEY )
public class CreateAccessKeyType extends EuareMessage {
  String delegateAccount;
  String userName;
  public CreateAccessKeyType() {  }
}
public class EnableMFADeviceResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public EnableMFADeviceResponseType() {  }
}
public class GetGroupResultType extends EucalyptusData {
  GroupType group = new GroupType( );
  UserListTypeType users = new UserListTypeType( );
  Boolean isTruncated;
  String marker;
  public GetGroupResultType() {  }
}
public class UpdateUserResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateUserResponseType() {  }
}
public class ListGroupPoliciesResponseType extends EuareMessage {
  ListGroupPoliciesResultType listGroupPoliciesResult = new ListGroupPoliciesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListGroupPoliciesResponseType() {  }
}
public class ListServerCertificatesResultType extends EucalyptusData {
  ServerCertificateMetadataListTypeType serverCertificateMetadataList = new ServerCertificateMetadataListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListServerCertificatesResultType() {  }
}
public class GetUserPolicyResultType extends EucalyptusData {
  String userName;
  String policyName;
  String policyDocument;
  public GetUserPolicyResultType() {  }
}
public class DeactivateMFADeviceResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeactivateMFADeviceResponseType() {  }
}
public class UserListTypeType extends EucalyptusData {
  public UserListTypeType() {  }
  ArrayList<UserType> memberList = new ArrayList<UserType>();
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETLOGINPROFILE )
public class GetLoginProfileType extends EuareMessage {
  String delegateAccount;
  String userName;
  public GetLoginProfileType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTUSERPOLICIES )
public class ListUserPoliciesType extends EuareMessage {
  String delegateAccount;
  String userName;
  String marker;
  BigInteger maxItems;
  public ListUserPoliciesType() {  }
}
public class ResponseMetadataType extends EucalyptusData {
  String requestId;
  public ResponseMetadataType() {  }
}
public class PutUserPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public PutUserPolicyResponseType() {  }
}
public class CreateGroupResponseType extends EuareMessage {
  CreateGroupResultType createGroupResult = new CreateGroupResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateGroupResponseType() {  }
}
public class CertificateListTypeType extends EucalyptusData {
  public CertificateListTypeType() {  }
  ArrayList<SigningCertificateType> memberList = new ArrayList<SigningCertificateType>();
}
public class CreateUserResponseType extends EuareMessage {
  CreateUserResultType createUserResult = new CreateUserResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateUserResponseType() {  }
}
public class GetGroupPolicyResultType extends EucalyptusData {
  String groupName;
  String policyName;
  String policyDocument;
  public GetGroupPolicyResultType() {  }
}
public class ErrorDetailType extends EucalyptusData {
  public ErrorDetailType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATEACCESSKEY )
public class UpdateAccessKeyType extends EuareMessage {
  String delegateAccount;
  String userName;
  String accessKeyId;
  String status;
  public UpdateAccessKeyType() {  }
}
public class DeleteServerCertificateResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteServerCertificateResponseType() {  }
}
public class UploadSigningCertificateResponseType extends EuareMessage {
  UploadSigningCertificateResultType uploadSigningCertificateResult = new UploadSigningCertificateResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UploadSigningCertificateResponseType() {  }
}
public class GetUserPolicyResponseType extends EuareMessage {
  GetUserPolicyResultType getUserPolicyResult = new GetUserPolicyResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetUserPolicyResponseType() {  }
}
public class ErrorType extends EucalyptusData {
  String type;
  String code;
  String message;
  ErrorDetailType detail = new ErrorDetailType( );
  public ErrorType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATEUSER )
public class UpdateUserType extends EuareMessage {
  String delegateAccount;
  String userName;
  String newPath;
  String newUserName;
  String enabled;
  String passwordExpiration;
  String regStatus;
  public UpdateUserType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_PUTUSERPOLICY )
public class PutUserPolicyType extends EuareMessage {
  String delegateAccount;
  String userName;
  String policyName;
  String policyDocument;
  public PutUserPolicyType() {  }
}
public class DeleteUserResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteUserResponseType() {  }
}
public class CreateUserResultType extends EucalyptusData {
  UserType user= new UserType( );
  public CreateUserResultType() {  }
}
public class UploadServerCertificateResultType extends EucalyptusData {
  ServerCertificateMetadataType serverCertificateMetadata = new ServerCertificateMetadataType( );
  public UploadServerCertificateResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DEACTIVATEMFADEVICE )
public class DeactivateMFADeviceType extends EuareMessage {
  String userName;
  String serialNumber;
  public DeactivateMFADeviceType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_RESYNCMFADEVICE )
public class ResyncMFADeviceType extends EuareMessage {
  String userName;
  String serialNumber;
  String authenticationCode1;
  String authenticationCode2;
  public ResyncMFADeviceType() {  }
}
public class UserType extends EuareMessage {
  String path;
  String userName;
  String userId;
  String arn;
  String enabled;
  String regStatus;
  String passwordExpiration;
  public UserType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETUSERPOLICY )
public class GetUserPolicyType extends EuareMessage {
  String delegateAccount;
  String userName;
  String policyName;
  public GetUserPolicyType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEUSERPOLICY )
public class DeleteUserPolicyType extends EuareMessage {
  String delegateAccount;
  String userName;
  String policyName;
  public DeleteUserPolicyType() {  }
}
public class CreateLoginProfileResponseType extends EuareMessage {
  CreateLoginProfileResultType createLoginProfileResult = new CreateLoginProfileResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateLoginProfileResponseType() {  }
}
public class DeleteLoginProfileResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteLoginProfileResponseType() {  }
}
public class ListAccessKeysResultType extends EucalyptusData {
  AccessKeyMetadataListTypeType accessKeyMetadata = new AccessKeyMetadataListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListAccessKeysResultType() {  }
}
public class ListGroupsForUserResultType extends EucalyptusData {
  GroupListTypeType groups = new GroupListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListGroupsForUserResultType() {  }
}
public class GetUserResultType extends EucalyptusData {
  UserType user = new UserType( );
  public GetUserResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETESIGNINGCERTIFICATE )
public class DeleteSigningCertificateType extends EuareMessage {
  String delegateAccount;
  String userName;
  String certificateId;
  public DeleteSigningCertificateType() {  }
}
public class LoginProfileType extends EuareMessage {
  String userName;
  public LoginProfileType() {  }
}
public class UpdateServerCertificateResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateServerCertificateResponseType() {  }
}
public class ListUsersResponseType extends EuareMessage {
  ListUsersResultType listUsersResult = new ListUsersResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListUsersResponseType() {  }
}
public class ServerCertificateMetadataListTypeType extends EucalyptusData {
  public ServerCertificateMetadataListTypeType() {  }
  ArrayList<ServerCertificateMetadataType> memberList = new ArrayList<ServerCertificateMetadataType>();
}
public class DeleteGroupPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteGroupPolicyResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEACCESSKEY )
public class DeleteAccessKeyType extends EuareMessage {
  String delegateAccount;
  String userName;
  String accessKeyId;
  public DeleteAccessKeyType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTGROUPS )
public class ListGroupsType extends EuareMessage {
  String delegateAccount;
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListGroupsType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTGROUPSFORUSER )
public class ListGroupsForUserType extends EuareMessage {
  String delegateAccount;
  String userName;
  String marker;
  BigInteger maxItems;
  public ListGroupsForUserType() {  }
}
public class GroupType extends EuareMessage {
  String path;
  String groupName;
  String groupId;
  String arn;
  public GroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATESIGNINGCERTIFICATE )
public class UploadSigningCertificateType extends EuareMessage {
  String delegateAccount;
  String userName;
  String certificateBody;
  public UploadSigningCertificateType() {  }
}
public class UpdateLoginProfileResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateLoginProfileResponseType() {  }
}
public class ListGroupsResultType extends EucalyptusData {
  GroupListTypeType groups = new GroupListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListGroupsResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETGROUPPOLICY )
public class GetGroupPolicyType extends EuareMessage {
  String delegateAccount;
  String groupName;
  String policyName;
  public GetGroupPolicyType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_ENABLEMFADEVICE )
public class EnableMFADeviceType extends EuareMessage {
  String userName;
  String serialNumber;
  String authenticationCode1;
  String authenticationCode2;
  public EnableMFADeviceType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETELOGINPROFILE )
public class DeleteLoginProfileType extends EuareMessage {
  String delegateAccount;
  String userName;
  public DeleteLoginProfileType() {  }
}
public class SigningCertificateType extends EuareMessage {
  String userName;
  String certificateId;
  String certificateBody;
  String privateKey;
  String status;
  Date uploadDate;
  public SigningCertificateType() {  }
}
public class ListMFADevicesResponseType extends EuareMessage {
  ListMFADevicesResultType listMFADevicesResult = new ListMFADevicesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListMFADevicesResponseType() {  }
}
public class ListGroupPoliciesResultType extends EucalyptusData {
  PolicyNameListTypeType policyNames = new PolicyNameListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListGroupPoliciesResultType() {  }
}
public class ListUsersResultType extends EucalyptusData {
  UserListTypeType users = new UserListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListUsersResultType() {  }
}
public class DeleteSigningCertificateResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteSigningCertificateResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTACCESSKEYS )
public class ListAccessKeysType extends EuareMessage {
  String delegateAccount;
  String userName;
  String marker;
  BigInteger maxItems;
  public ListAccessKeysType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEUSER )
public class DeleteUserType extends EuareMessage {
  String delegateAccount;
  String userName;
  Boolean isRecursive;
  public DeleteUserType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_CREATELOGINPROFILE )
public class CreateLoginProfileType extends EuareMessage {
  String delegateAccount;
  String userName;
  String password;
  public CreateLoginProfileType() {  }
}
public class RemoveUserFromGroupResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public RemoveUserFromGroupResponseType() {  }
}
public class GetServerCertificateResultType extends EucalyptusData {
  ServerCertificateType serverCertificate = new ServerCertificateType( );
  public GetServerCertificateResultType() {  }
}
public class GetServerCertificateResponseType extends EuareMessage {
  GetServerCertificateResultType getServerCertificateResult = new GetServerCertificateResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetServerCertificateResponseType() {  }
}
public class ListUserPoliciesResultType extends EucalyptusData {
  PolicyNameListTypeType policyNames = new PolicyNameListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListUserPoliciesResultType() {  }
}
public class GroupListTypeType extends EucalyptusData {
  public GroupListTypeType() {  }
  ArrayList<GroupType> memberList = new ArrayList<GroupType>();
}
public class CreateGroupResultType extends EucalyptusData {
  GroupType group = new GroupType( );
  public CreateGroupResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETSERVERCERTIFICATE )
public class GetServerCertificateType extends EuareMessage {
  String serverCertificateName;
  public GetServerCertificateType() {  }
}
public class UpdateSigningCertificateResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateSigningCertificateResponseType() {  }
}
public class GetLoginProfileResultType extends EucalyptusData {
  LoginProfileType loginProfile = new LoginProfileType( );
  public GetLoginProfileResultType() {  }
}
public class PolicyNameListTypeType extends EucalyptusData {
  public PolicyNameListTypeType() {  }
  ArrayList<String> memberList = new ArrayList<String>();
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETUSER )
public class GetUserType extends EuareMessage {
  String delegateAccount;
  String userName;
  Boolean showExtra;
  public GetUserType() {  }
}
public class ServerCertificateMetadataType extends EucalyptusData {
  String path;
  String serverCertificateName;
  String serverCertificateId;
  String arn;
  Date uploadDate;
  public ServerCertificateMetadataType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTMFADEVICES )
public class ListMFADevicesType extends EuareMessage {
  String userName;
  String marker;
  BigInteger maxItems;
  public ListMFADevicesType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTSERVERCERTIFICATES )
public class ListServerCertificatesType extends EuareMessage {
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListServerCertificatesType() {  }
}
public class ErrorResponseType extends EuareMessage {
  String requestId;
  HttpResponseStatus httpStatus;
  public ErrorResponseType() {  }
  ArrayList<ErrorType> errorList = new ArrayList<ErrorType>();
}
public class GetGroupResponseType extends EuareMessage {
  GetGroupResultType getGroupResult = new GetGroupResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetGroupResponseType() {  }
}
public class ListSigningCertificatesResultType extends EucalyptusData {
  CertificateListTypeType certificates = new CertificateListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListSigningCertificatesResultType() {  }
}
public class MFADeviceType extends EuareMessage {
  String userName;
  String serialNumber;
  public MFADeviceType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_CREATEUSER )
public class CreateUserType extends EuareMessage {
  String delegateAccount;
  String path;
  String userName;
  public CreateUserType() {  }
}
public class DeleteUserPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteUserPolicyResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEGROUP )
public class DeleteGroupType extends EuareMessage {
  String delegateAccount;
  String groupName;
  Boolean isRecursive;
  public DeleteGroupType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATESIGNINGCERTIFICATE )
public class UpdateSigningCertificateType extends EuareMessage {
  String delegateAccount;
  String userName;
  String certificateId;
  String status;
  public UpdateSigningCertificateType() {  }
}
public class CreateAccessKeyResultType extends EucalyptusData {
  AccessKeyType accessKey = new AccessKeyType( );
  public CreateAccessKeyResultType() {  }
}
public class ListGroupsForUserResponseType extends EuareMessage {
  ListGroupsForUserResultType listGroupsForUserResult = new ListGroupsForUserResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListGroupsForUserResponseType() {  }
}
public class CreateLoginProfileResultType extends EucalyptusData {
  LoginProfileType loginProfile = new LoginProfileType( );
  public CreateLoginProfileResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTUSERS )
public class ListUsersType extends EuareMessage {
  String delegateAccount;
  String pathPrefix;
  String marker;
  BigInteger maxItems;
  public ListUsersType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_CREATEACCOUNTALIAS )
public class CreateAccountAliasType extends EuareMessage {
  String delegateAccount;
  String accountAlias;
  public CreateAccountAliasType() {  }
}
public class CreateAccountAliasResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateAccountAliasResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_DELETEACCOUNTALIAS )
public class DeleteAccountAliasType extends EuareMessage {
  String delegateAccount;
  String accountAlias;
  public DeleteAccountAliasType() {  }
}
public class DeleteAccountAliasResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteAccountAliasResponseType() {  }
}
public class ListAccountAliasesResultType extends EucalyptusData {
  AccountAliasListTypeType accountAliases = new AccountAliasListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListAccountAliasesResultType() {  }
}
public class ListAccountAliasesResponseType extends EuareMessage {
  ListAccountAliasesResultType listAccountAliasesResult = new ListAccountAliasesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListAccountAliasesResponseType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_LISTACCOUNTALIASES )
public class ListAccountAliasesType extends EuareMessage {
  String delegateAccount;
  String marker;
  BigInteger maxItems;
  public ListAccountAliasesType() {  }
}
public class AccountAliasListTypeType extends EucalyptusData {
  public AccountAliasListTypeType() {  }
  ArrayList<String> memberList = new ArrayList<String>();
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETACCOUNTSUMMARY )
public class GetAccountSummaryType extends EuareMessage {
  String delegateAccount;
  public GetAccountSummaryType() {  }
}
public class GetAccountSummaryResponseType extends EuareMessage {
  GetAccountSummaryResultType getAccountSummaryResult = new GetAccountSummaryResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetAccountSummaryResponseType() {  }
}
public class GetAccountSummaryResultType extends EucalyptusData {
  SummaryMapTypeType summaryMap = new SummaryMapTypeType( );
  public GetAccountSummaryResultType() {  }
}
public class SummaryMapTypeType extends EucalyptusData {
  public SummaryMapTypeType() {  }
  ArrayList<SummaryMapTypeEntryType> entryList = new ArrayList<SummaryMapTypeEntryType>();
}
public class SummaryMapTypeEntryType extends EucalyptusData {
  String key;
  BigInteger value;
  public SummaryMapTypeEntryType() {  }
}

/**
 * Eucalyptus extended messages for account management
 */
public class CreateAccountType extends EuareMessage {
  String accountName;
  public CreateUserType() {  }
}
public class CreateAccountResponseType extends EuareMessage {
  CreateAccountResultType createAccountResult = new CreateAccountResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateAccountResponseType() {  }
}
public class CreateAccountResultType extends EucalyptusData {
  AccountType account = new AccountType( );
  public CreateAccountResultType() {  }
}
public class AccountType extends EuareMessage {
  String accountName;
  String accountId;
  public AccountType() {  }
}

public class DeleteAccountType extends EuareMessage {
  String accountName;
  Boolean recursive;
  public DeleteAccountType() {  }
}
public class DeleteAccountResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteAccountResponseType() {  }
}

public class ListAccountsType extends EuareMessage {
  public ListAccountsType() {  }
}
public class ListAccountsResponseType extends EuareMessage {
  ListAccountsResultType listAccountsResult = new ListAccountsResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListAccountsResponseType() {  }
}
public class ListAccountsResultType extends EucalyptusData {
  AccountListTypeType accounts = new AccountListTypeType( );
  public ListAccountsResultType() {   }
}
public class AccountListTypeType extends EucalyptusData {
  public AccountListTypeType() {  }
  ArrayList<AccountType> memberList = new ArrayList<AccountType>();
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPLOADSIGNINGCERTIFICATE )
public class CreateSigningCertificateType extends EuareMessage {
  String delegateAccount;
  String userName;
  public CreateSigningCertificateType() {  }
}
public class CreateSigningCertificateResponseType extends EuareMessage {
  CreateSigningCertificateResultType createSigningCertificateResult = new CreateSigningCertificateResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public CreateSigningCertificateResponseType() {  }
}
public class CreateSigningCertificateResultType extends EucalyptusData {
  SigningCertificateType certificate = new SigningCertificateType( );
  public CreateSigningCertificateResultType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_GETUSER )
public class GetUserInfoType extends EuareMessage {
  String delegateAccount;
  String userName;
  String infoKey;
  public GetUserInfoType() {  }
}
public class GetUserInfoResponseType extends EuareMessage {
  GetUserInfoResultType getUserInfoResult = new GetUserInfoResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetUserInfoResponseType() {  }
}
public class GetUserInfoResultType extends EucalyptusData {
  UserInfoListTypeType infos = new UserInfoListTypeType( );
  public GetUserInfoResultType() {  }
}
public class UserInfoListTypeType extends EucalyptusData {
  ArrayList<UserInfoType> memberList = new ArrayList<UserInfoType>();
  public UserInfoListTypeType() {  }
}
public class UserInfoType extends EuareMessage  {
  String key;
  String value;
  public UserInfoType() {  }
}
@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = PolicySpec.IAM_UPDATEUSER )
public class UpdateUserInfoType extends EuareMessage {
  String delegateAccount;
  String userName;
  String infoKey;
  String infoValue;
  public UpdateUserInfoType() {  }
}
public class UpdateUserInfoResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public UpdateUserInfoResponseType() {  }
}
public class PutAccountPolicyType extends EuareMessage {
  String accountName;
  String policyName;
  String policyDocument;
  public PutAccountPolicyType() {  }
}
public class PutAccountPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public PutAccountPolicyResponseType() {  }
}
public class ListAccountPoliciesType extends EuareMessage {
  String accountName;
  String marker;
  BigInteger maxItems;
  public ListAccountPoliciesType() {  }
}
public class ListAccountPoliciesResponseType extends EuareMessage {
  ListAccountPoliciesResultType listAccountPoliciesResult = new ListAccountPoliciesResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public ListAccountPoliciesResponseType() {  }
}
public class ListAccountPoliciesResultType extends EucalyptusData {
  PolicyNameListTypeType policyNames = new PolicyNameListTypeType( );
  Boolean isTruncated;
  String marker;
  public ListAccountPoliciesResultType() {  }
}
public class GetAccountPolicyType extends EuareMessage {
  String accountName;
  String policyName;
  public GetAccountPolicyType() {  }
}
public class GetAccountPolicyResponseType extends EuareMessage {
  GetAccountPolicyResultType getAccountPolicyResult = new GetAccountPolicyResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetAccountPolicyResponseType() {  }
}
public class GetAccountPolicyResultType extends EucalyptusData {
  String accountName;
  String policyName;
  String policyDocument;
  public GetAccountPolicyResultType() {  }
}
public class DeleteAccountPolicyType extends EuareMessage {
  String accountName;
  String policyName;
  public DeleteAccountPolicyType() {  }
}
public class DeleteAccountPolicyResponseType extends EuareMessage {
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public DeleteAccountPolicyResponseType() {  }
}
public class GetLdapSyncStatusType extends EuareMessage {
  public GetLdapSyncStatusType() {  }
}
public class GetLdapSyncStatusResponseType extends EuareMessage {
  GetLdapSyncStatusResultType getLdapSyncStatusResult = new GetLdapSyncStatusResultType( );
  ResponseMetadataType responseMetadata = new ResponseMetadataType( );
  public GetLdapSyncStatusResponseType() {  }
}
public class GetLdapSyncStatusResultType extends EucalyptusData {
  Boolean syncEnabled;
  Boolean inSync;
  public GetLdapSyncStatusResultType() {  }
}