package com.eucalyptus.auth.euare;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyParseException;
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
import com.eucalyptus.auth.policy.PatternUtils;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.EucalyptusCloudException;

public class EuareService {
  
  static private final Logger LOG = Logger.getLogger( EuareService.class );
  
  public CreateAccountResponseType createAccount(CreateAccountType request) throws EucalyptusCloudException {
    CreateAccountResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to create account by " + requestUser.getName( ) );
    }
    try {
      Account newAccount = Accounts.addAccount( request.getAccountName( ) );
      AccountType account = reply.getCreateAccountResult( ).getAccount( );
      account.setAccountName( newAccount.getName( ) );
      account.setAccountId( newAccount.getId( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.ACCOUNT_ALREADY_EXISTS.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.ENTITY_ALREADY_EXISTS, "Account " + request.getAccountName( ) + " already exists." );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    return reply;
  }
  
  public DeleteAccountResponseType deleteAccount(DeleteAccountType request) throws EucalyptusCloudException {
    DeleteAccountResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to delete account by " + requestUser.getName( ) );
    }
    try {
      Accounts.deleteAccount( request.getAccountName( ), false/*forceDeleteSystem*/, false/*recursive*/ );
    } catch ( Exception e ) {
      if ( e instanceof AuthException ) {
        if ( AuthException.ACCOUNT_DELETE_CONFLICT.equals( e.getMessage( ) ) ) {
          throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.DELETE_CONFLICT, "Account " + request.getAccountName( ) + " can not be deleted." );
        } else if ( AuthException.DELETE_SYSTEM_ACCOUNT.equals( e.getMessage( ) ) ) {
          throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.DELETE_CONFLICT, "System account can not be deleted." );
        }
      }
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }
  
  public ListAccountsResponseType listAccounts(ListAccountsType request) throws EucalyptusCloudException {
    ListAccountsResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to list accounts by " + requestUser.getName( ) );
    }
    Pattern pattern = null;
    if ( request.getNamePattern( ) != null ) {
      pattern = Pattern.compile( PatternUtils.toJavaPattern( request.getNamePattern( ) ) );
    }
    ArrayList<AccountType> accounts = reply.getListAccountsResult( ).getAccounts( );
    try {
      for ( Account account : Accounts.listAllAccounts( ) ) {
        if ( pattern == null || pattern.matcher( account.getName( ) ).matches( ) ) {
          AccountType at = new AccountType( );
          at.setAccountName( account.getName( ) );
          at.setAccountId( account.getId( ) );
          accounts.add( at );
        }
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }
  
  public ListGroupsResponseType listGroups(ListGroupsType request) throws EucalyptusCloudException {
    ListGroupsResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    // TODO(Ye Wen, 01/16/2011): support pagination
    reply.getListGroupsResult( ).setIsTruncated( false );
    ArrayList<GroupType> groups = reply.getListGroupsResult( ).getGroups( ).getMemberList( );
    try {
      for ( Group group : account.getGroups( ) ) {
        if ( Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( group ), account, action, requestUser ) ) {
          if ( request.getPathPrefix( ) != null && group.getPath( ).startsWith( request.getPathPrefix( ) ) ) {
            GroupType g = new GroupType( );
            fillGroupResult( g, group, account.getId( ) );
            groups.add( g );
          }
        }
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteAccessKeyResponseType deleteAccessKey(DeleteAccessKeyType request) throws EucalyptusCloudException {
    DeleteAccessKeyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to delete access key of " + request.getUserName( ) + "by " + requestUser.getName( ) );
    }
    try {
      userFound.removeKey( request.getAccessKeyId( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public ListSigningCertificatesResponseType listSigningCertificates(ListSigningCertificatesType request) throws EucalyptusCloudException {
    ListSigningCertificatesResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to list signing certificates for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/26/2011): support pagination
    ListSigningCertificatesResultType result = reply.getListSigningCertificatesResult( );
    result.setIsTruncated( false );
    ArrayList<SigningCertificateType> certs = result.getCertificates( ).getMemberList( );
    try {
      for ( Certificate cert : userFound.getCertificates( ) ) {
        if ( !cert.isRevoked( ) ) {
          SigningCertificateType c = new SigningCertificateType( );
          c.setUserName( userFound.getName( ) );
          c.setCertificateId( cert.getId( ) );
          c.setCertificateBody( B64.url.decString( cert.getPem( ) ) );
          c.setStatus( cert.isActive( ) ? "Active" : "Inactive" );
          c.setUploadDate( cert.getCreateDate( ) );
          certs.add( c );
        }
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public UploadSigningCertificateResponseType uploadSigningCertificate(UploadSigningCertificateType request) throws EucalyptusCloudException {
    UploadSigningCertificateResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to upload signing certificate of " + request.getUserName( ) + "by " + requestUser.getName( ) );
    }
    String encodedPem = B64.url.encString( request.getCertificateBody( ) );
    Certificate cert = null;
    try {
      for ( Certificate c : userFound.getCertificates( ) ) {
        if ( c.getPem( ).equals( encodedPem ) ) {
          if ( !c.isRevoked( ) ) {
            throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.DUPLICATE_CERTIFICATE, "Trying to upload duplicate certificate: " + c.getId( ) );        
          } else {
            userFound.removeCertificate( c.getId( ) );
          }
        }
      }
      X509Certificate x509 = X509CertHelper.toCertificate( encodedPem );
      if ( x509 == null ) {
        throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.INVALID_CERTIFICATE, "Invalid certificate " + request.getCertificateBody( ) );        
      }
      cert = userFound.addCertificate( x509 );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    SigningCertificateType result = reply.getUploadSigningCertificateResult( ).getCertificate( );
    result.setUserName( userFound.getName( ) );
    result.setCertificateId( cert.getId( ) );
    result.setCertificateBody( request.getCertificateBody( ) );
    result.setStatus( "Active" );
    result.setUploadDate( cert.getCreateDate( ) );
    return reply;
  }

  public DeleteUserPolicyResponseType deleteUserPolicy(DeleteUserPolicyType request) throws EucalyptusCloudException {
    DeleteUserPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to " + action + " for user " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    try {
      userFound.removePolicy( request.getPolicyName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public PutUserPolicyResponseType putUserPolicy(PutUserPolicyType request) throws EucalyptusCloudException {
    PutUserPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to put user policy for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    try {
      userFound.addPolicy( request.getPolicyName( ), request.getPolicyDocument( ) );
    } catch ( PolicyParseException e ) {
      throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.MALFORMED_POLICY_DOCUMENT, "Error in uploaded policy: " + request.getPolicyDocument( ), e );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public ListServerCertificatesResponseType listServerCertificates(ListServerCertificatesType request) throws EucalyptusCloudException {
    //ListServerCertificatesResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public GetUserPolicyResponseType getUserPolicy(GetUserPolicyType request) throws EucalyptusCloudException {
    GetUserPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to get user policies for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    try {
      Policy policy = null;
      for ( Policy p : userFound.getPolicies( ) ) {
        if ( p.getName( ).equals( request.getPolicyName( ) ) ) {
          policy = p;
          break;
        }
      }
      if ( policy != null ) {
        GetUserPolicyResultType result = reply.getGetUserPolicyResult( );
        result.setUserName( request.getUserName( ) );
        result.setPolicyName( request.getPolicyName( ) );
        result.setPolicyDocument( policy.getText( ) );
      } else {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find policy " + request.getPolicyName( ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public UpdateLoginProfileResponseType updateLoginProfile(UpdateLoginProfileType request) throws EucalyptusCloudException {
    UpdateLoginProfileResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to update login profile of " + request.getUserName( ) + "by " + requestUser.getName( ) );
    }
    try {
      if ( request.getPassword( ) != null ) {
        userFound.setPassword( request.getPassword( ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public UpdateServerCertificateResponseType updateServerCertificate(UpdateServerCertificateType request) throws EucalyptusCloudException {
    //UpdateServerCertificateResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public UpdateUserResponseType updateUser(UpdateUserType request) throws EucalyptusCloudException {
    UpdateUserResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to update user by " + requestUser.getName( ) );
    }
    try {
      if ( request.getNewUserName( ) != null && !"".equals( request.getNewUserName( ) ) ) {
        userFound.setName( request.getNewUserName( ) );
      }
      if ( request.getNewPath( ) != null && !"".equals( request.getNewPath( ) ) ) {
        userFound.setPath( sanitizePath( request.getNewPath( ) ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteLoginProfileResponseType deleteLoginProfile(DeleteLoginProfileType request) throws EucalyptusCloudException {
    DeleteLoginProfileResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to delete login profile for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    if ( userFound.getPassword( ) != null ) {
      throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.ENTITY_ALREADY_EXISTS, "User " + requestUser.getName( ) + " already has a login profile" );
    }
    try {
      userFound.setPassword( null );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public UpdateSigningCertificateResponseType updateSigningCertificate(UpdateSigningCertificateType request) throws EucalyptusCloudException {
    UpdateSigningCertificateResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to update signing certificate of " + request.getUserName( ) + "by " + requestUser.getName( ) );
    }
    try {
      Certificate cert = userFound.getCertificate( request.getCertificateId( ) );
      if ( cert.isRevoked( ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find the certificate " + request.getCertificateId( ) );
      }
      cert.setActive( "Active".equalsIgnoreCase( request.getStatus( ) ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteGroupPolicyResponseType deleteGroupPolicy(DeleteGroupPolicyType request) throws EucalyptusCloudException {
    DeleteGroupPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to delete group policy of " + request.getGroupName( ) + " by " + requestUser.getName( ) );
    }
    try {
      groupFound.removePolicy( request.getPolicyName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public ListUsersResponseType listUsers(ListUsersType request) throws EucalyptusCloudException {
    ListUsersResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    // TODO(Ye Wen, 01/16/2011): support pagination
    ListUsersResultType result = reply.getListUsersResult( );
    result.setIsTruncated( false );
    ArrayList<UserType> users = reply.getListUsersResult( ).getUsers( ).getMemberList( );
    try {
      for ( User user : account.getUsers( ) ) {
        if ( Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( user ), account, action, requestUser ) ) {
          if ( request.getPathPrefix( ) != null && user.getPath( ).startsWith( request.getPathPrefix( ) ) ) {
            UserType u = new UserType( );
            fillUserResult( u, user, account.getId( ) );
            users.add( u );
          }
        }
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public UpdateGroupResponseType updateGroup(UpdateGroupType request) throws EucalyptusCloudException {
    UpdateGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to update group " + groupFound.getName( ) + " by " + requestUser.getName( ) );
    }
    try {
      if ( request.getNewGroupName( ) != null && !"".equals( request.getNewGroupName( ) ) ) {
        groupFound.setName( request.getNewGroupName( ) );
      }
      if ( request.getNewPath( ) != null && !"".equals( request.getNewPath( ) ) ) {
        groupFound.setPath( sanitizePath( request.getNewPath( ) ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public GetServerCertificateResponseType getServerCertificate(GetServerCertificateType request) throws EucalyptusCloudException {
    //GetServerCertificateResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public PutGroupPolicyResponseType putGroupPolicy(PutGroupPolicyType request) throws EucalyptusCloudException {
    PutGroupPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to put group policy for " + groupFound.getName( ) + " by " + requestUser.getName( ) );
    }
    try {
      groupFound.addPolicy( request.getPolicyName( ), request.getPolicyDocument( ) );
    } catch ( PolicyParseException e ) {
      throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.MALFORMED_POLICY_DOCUMENT, "Error in uploaded policy: " + request.getPolicyDocument( ), e );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public CreateUserResponseType createUser(CreateUserType request) throws EucalyptusCloudException {
    CreateUserResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, "", account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to create user by " + requestUser.getName( ) );
    }
    if ( !Permissions.canAllocate( PolicySpec.IAM_RESOURCE_USER, "", action, requestUser, 1L ) ) {
      throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.LIMIT_EXCEEDED, "User quota exceeded" );
    }
    try {
      User newUser = account.addUser( request.getUserName( ), sanitizePath( request.getPath( ) ), true, true, null );
      newUser.createKey( );
      newUser.createToken( );
      newUser.createConfirmationCode( );
      newUser.createPassword( );
      UserType u = reply.getCreateUserResult( ).getUser( );
      fillUserResult( u, newUser, account.getId( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.USER_ALREADY_EXISTS.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.ENTITY_ALREADY_EXISTS, "User " + request.getUserName( ) + " already exists." );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    return reply;
  }

  public DeleteSigningCertificateResponseType deleteSigningCertificate(DeleteSigningCertificateType request) throws EucalyptusCloudException {
    DeleteSigningCertificateResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to " + action + " for user " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    try {
      userFound.removeCertificate( request.getCertificateId( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public EnableMFADeviceResponseType enableMFADevice(EnableMFADeviceType request) throws EucalyptusCloudException {
    //EnableMFADeviceResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public ListUserPoliciesResponseType listUserPolicies(ListUserPoliciesType request) throws EucalyptusCloudException {
    ListUserPoliciesResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to list user policies for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/26/2011): support pagination
    ListUserPoliciesResultType result = reply.getListUserPoliciesResult( );
    result.setIsTruncated( false );
    ArrayList<String> policies = result.getPolicyNames( ).getMemberList( );
    try {
      for ( Policy p : userFound.getPolicies( ) ) {
        policies.add( p.getName( ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public ListAccessKeysResponseType listAccessKeys(ListAccessKeysType request) throws EucalyptusCloudException {
    ListAccessKeysResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to list access keys for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/26/2011): add pagination support
    ListAccessKeysResultType result = reply.getListAccessKeysResult( );
    try {
      result.setIsTruncated( false );
      ArrayList<AccessKeyMetadataType> keys = result.getAccessKeyMetadata( ).getMemberList( );
      for ( AccessKey k : userFound.getKeys( ) ) {
        AccessKeyMetadataType key = new AccessKeyMetadataType( );
        key.setUserName( userFound.getName( ) );
        key.setAccessKeyId( k.getId( ) );
        key.setStatus( k.isActive( ) ? "Active" : "Inactive" );
        keys.add( key );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public GetLoginProfileResponseType getLoginProfile(GetLoginProfileType request) throws EucalyptusCloudException {
    GetLoginProfileResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to get login profile for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    if ( userFound.getPassword( ) == null ) {
      throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NOT_AUTHORIZED, "Can not find login profile for " + request.getUserName( ) );
    }
    reply.getGetLoginProfileResult( ).getLoginProfile( ).setUserName( request.getUserName( ) );
    return reply;
  }

  public ListGroupsForUserResponseType listGroupsForUser(ListGroupsForUserType request) throws EucalyptusCloudException {
    ListGroupsForUserResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to get user groups for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/16/2011): support pagination
    reply.getListGroupsForUserResult( ).setIsTruncated( false );
    ArrayList<GroupType> groups = reply.getListGroupsForUserResult( ).getGroups( ).getMemberList( );
    try {
      for ( Group group : userFound.getGroups( ) ) {
        // TODO(Ye Wen, 01/16/2011): do we need to check permission here?
        if ( !group.isUserGroup( ) ) {
          GroupType g = new GroupType( );
          fillGroupResult( g, group, account.getId( ) );
          groups.add( g );
        }
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public CreateGroupResponseType createGroup(CreateGroupType request) throws EucalyptusCloudException {
    CreateGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, "", account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to create group by " + requestUser.getName( ) );
    }
    if ( !Permissions.canAllocate( PolicySpec.IAM_RESOURCE_GROUP, "", action, requestUser, 1L ) ) {
      throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.LIMIT_EXCEEDED, "Group quota exceeded" );
    }
    try {
      Group newGroup = account.addGroup( request.getGroupName( ), sanitizePath( request.getPath( ) ) );
      GroupType g = reply.getCreateGroupResult( ).getGroup( );
      g.setGroupName( newGroup.getName( ) );
      g.setPath( newGroup.getPath( ) );
      g.setGroupId( newGroup.getId( ) );
      g.setArn( ( new EuareResourceName( account.getId( ), PolicySpec.IAM_RESOURCE_GROUP, newGroup.getPath( ), newGroup.getName( ) ) ).toString( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.GROUP_ALREADY_EXISTS.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.ENTITY_ALREADY_EXISTS, "Group " + request.getGroupName( ) + " already exists." );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    return reply;
  }

  public UploadServerCertificateResponseType uploadServerCertificate(UploadServerCertificateType request) throws EucalyptusCloudException {
    //UploadServerCertificateResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public GetGroupPolicyResponseType getGroupPolicy(GetGroupPolicyType request) throws EucalyptusCloudException {
    GetGroupPolicyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to get group policy for " + request.getGroupName( ) + " by " + requestUser.getName( ) );
    }
    try {
      Policy policy = null;
      for ( Policy p : groupFound.getPolicies( ) ) {
        if ( p.getName( ).equals( request.getPolicyName( ) ) ) {
          policy = p;
          break;
        }
      }
      if ( policy != null ) {
        GetGroupPolicyResultType result = reply.getGetGroupPolicyResult( );
        result.setGroupName( request.getGroupName( ) );
        result.setPolicyName( request.getPolicyName( ) );
        result.setPolicyDocument( policy.getText( ) );
      } else {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find policy " + request.getPolicyName( ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteUserResponseType deleteUser(DeleteUserType request) throws EucalyptusCloudException {
    DeleteUserResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userToDelete = null;
    try {
      userToDelete = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userToDelete ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to delete user by " + requestUser.getName( ) );
    }
    try {
      account.deleteUser( request.getUserName( ), false, false );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.USER_DELETE_CONFLICT.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.DELETE_CONFLICT, "Attempted to delete a user with resource attached by " + requestUser.getName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    return reply;
  }

  public DeactivateMFADeviceResponseType deactivateMFADevice(DeactivateMFADeviceType request) throws EucalyptusCloudException {
    //DeactivateMFADeviceResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public RemoveUserFromGroupResponseType removeUserFromGroup(RemoveUserFromGroupType request) throws EucalyptusCloudException {
    RemoveUserFromGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to access user by " + requestUser.getName( ) );
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to access group by " + requestUser.getName( ) );
    }
    try {
      groupFound.removeUserByName( userFound.getName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteServerCertificateResponseType deleteServerCertificate(DeleteServerCertificateType request) throws EucalyptusCloudException {
    //DeleteServerCertificateResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public ListGroupPoliciesResponseType listGroupPolicies(ListGroupPoliciesType request) throws EucalyptusCloudException {
    ListGroupPoliciesResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to list group polices for " + request.getGroupName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/26/2011): support pagination
    ListGroupPoliciesResultType result = reply.getListGroupPoliciesResult( );
    result.setIsTruncated( false );
    ArrayList<String> policies = result.getPolicyNames( ).getMemberList( );
    try {
      for ( Policy p : groupFound.getPolicies( ) ) {
        policies.add( p.getName( ) );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public CreateLoginProfileResponseType createLoginProfile(CreateLoginProfileType request) throws EucalyptusCloudException {
    CreateLoginProfileResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to create login profile for " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    if ( userFound.getPassword( ) != null ) {
      throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.ENTITY_ALREADY_EXISTS, "User " + requestUser.getName( ) + " already has a login profile" );
    }
    try {
      userFound.createPassword( );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    reply.getCreateLoginProfileResult( ).getLoginProfile( ).setUserName( requestUser.getName( ) );
    return reply;
  }

  public CreateAccessKeyResponseType createAccessKey(CreateAccessKeyType request) throws EucalyptusCloudException {
    CreateAccessKeyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to create access key for user " + request.getUserName( ) + " by " + requestUser.getName( ) );
    }
    try {
      AccessKey key = userFound.createKey( );
      AccessKeyType keyResult = reply.getCreateAccessKeyResult( ).getAccessKey( );
      keyResult.setAccessKeyId( key.getId( ) );
      keyResult.setCreateDate( key.getCreateDate( ) );
      keyResult.setSecretAccessKey( key.getKey( ) );
      keyResult.setStatus( key.isActive( ) ? "Active" : "Inactive" );
      keyResult.setUserName( userFound.getName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public GetUserResponseType getUser(GetUserType request) throws EucalyptusCloudException {
    GetUserResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to get user by " + requestUser.getName( ) );
    }
    UserType u = reply.getGetUserResult( ).getUser( );
    fillUserResult( u, userFound, account.getId( ) );
    return reply;
  }

  public ResyncMFADeviceResponseType resyncMFADevice(ResyncMFADeviceType request) throws EucalyptusCloudException {
    //ResyncMFADeviceResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public ListMFADevicesResponseType listMFADevices(ListMFADevicesType request) throws EucalyptusCloudException {
    //ListMFADevicesResponseType reply = request.getReply( );
    throw new EuareException( HttpResponseStatus.BAD_REQUEST, EuareException.NOT_IMPLEMENTED, "Operation not implemented" );
    //return reply;
  }

  public UpdateAccessKeyResponseType updateAccessKey(UpdateAccessKeyType request) throws EucalyptusCloudException {
    UpdateAccessKeyResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to update access key of " + request.getUserName( ) + "by " + requestUser.getName( ) );
    }
    try {
      AccessKey key = userFound.getKey( request.getAccessKeyId( ) );
      key.setActive( "Active".equalsIgnoreCase( request.getStatus( ) ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public AddUserToGroupResponseType addUserToGroup(AddUserToGroupType request) throws EucalyptusCloudException {
    AddUserToGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    User userFound = null;
    try {
      userFound = account.lookupUserByName( request.getUserName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_USER.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find user " + request.getUserName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to access user by " + requestUser.getName( ) );
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_USER, getUserFullName( userFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to access group by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/22/2011): add group level quota?
    try {
      groupFound.addUserByName( userFound.getName( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public GetGroupResponseType getGroup(GetGroupType request) throws EucalyptusCloudException {
    GetGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED,
                                "Not authorized to get group " + request.getGroupName( ) + " by " + requestUser.getName( ) );
    }
    // TODO(Ye Wen, 01/26/2011): Consider pagination
    reply.getGetGroupResult( ).setIsTruncated( false );
    GroupType g = reply.getGetGroupResult( ).getGroup( );
    fillGroupResult( g, groupFound, account.getId( ) );
    ArrayList<UserType> users = reply.getGetGroupResult( ).getUsers( ).getMemberList( );
    try {
      for ( User user : groupFound.getUsers( ) ) {
        UserType u = new UserType( );
        fillUserResult( u, user, account.getId( ) );
        users.add( u );
      }
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( e );
    }
    return reply;
  }

  public DeleteGroupResponseType deleteGroup(DeleteGroupType request) throws EucalyptusCloudException {
    DeleteGroupResponseType reply = request.getReply( );
    reply.getResponseMetadata( ).setRequestId( reply.getCorrelationId( ) );
    String action = PolicySpec.requestToAction( request );
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = ctx.getAccount( );
    Group groupFound = null;
    try {
      groupFound = account.lookupGroupByName( request.getGroupName( ) );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.NO_SUCH_GROUP.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.NOT_FOUND, EuareException.NO_SUCH_ENTITY, "Can not find group " + request.getGroupName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    if ( !Permissions.isAuthorized( PolicySpec.IAM_RESOURCE_GROUP, getGroupFullName( groupFound ), account, action, requestUser ) ) {
      throw new EuareException( HttpResponseStatus.FORBIDDEN, EuareException.NOT_AUTHORIZED, "Not authorized to delete group by " + requestUser.getName( ) );
    }
    try {
      account.deleteGroup( request.getGroupName( ), false );
    } catch ( Exception e ) {
      if ( e instanceof AuthException && AuthException.GROUP_DELETE_CONFLICT.equals( e.getMessage( ) ) ) {
        throw new EuareException( HttpResponseStatus.CONFLICT, EuareException.DELETE_CONFLICT, "Attempted to delete group with resources attached by " + requestUser.getName( ) );
      } else {
        throw new EucalyptusCloudException( e );
      }
    }
    return reply;
  }

  private static String getUserFullName( User user ) {
    if ( "/".equals( user.getPath( ) ) ) {
      return "/" + user.getName( );
    } else {
      return user.getPath( ) + "/" + user.getName( );
    }
  }
  
  private static String getGroupFullName( Group group ) {
    if ( "/".equals( group.getPath( ) ) ) {
      return "/" + group.getName( );
    } else {
      return group.getPath( ) + "/" + group.getName( );
    }
  }
  
  private void fillUserResult( UserType u, User userFound, String accountId ) {
    u.setUserName( userFound.getName( ) );
    u.setUserId( userFound.getId( ) );
    u.setPath( userFound.getPath( ) );
    u.setArn( ( new EuareResourceName( accountId, PolicySpec.IAM_RESOURCE_USER, userFound.getPath( ), userFound.getName( ) ) ).toString( ) );
  }
  
  private void fillGroupResult( GroupType g, Group groupFound, String accountId ) {
    g.setPath( groupFound.getPath( ) );
    g.setGroupName( groupFound.getName( ) );
    g.setGroupId( groupFound.getId( ) );
    g.setArn( ( new EuareResourceName( accountId, PolicySpec.IAM_RESOURCE_GROUP, groupFound.getPath( ), groupFound.getName( ) ) ).toString( ) );
  }
  
  private String sanitizePath( String path ) {
    if ( path == null || "".equals( path ) ) {
      return "/";
    } else if ( !"/".equals( path ) ) {
      if ( path.endsWith( "/" ) ) {
        path = path.substring( 0, path.length( ) - 1 );
      }
    }
    return path;
  }
  
}
