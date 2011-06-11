package com.eucalyptus.webui.server;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;

public class EuarePermission {

  private static final Logger LOG = Logger.getLogger( EuarePermission.class );
                                                     
  public static boolean allowReadAccount( User requestUser, Account account ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ); // same account
  }
  
  public static boolean allowReadGroup( User requestUser, Account account, Group group ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // same account and ...
             // allowed to list group
             Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_LISTGROUPS, requestUser ) );
  }
  
  public static boolean allowReadUser( User requestUser, Account account, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           requestUser.getUserId( ).equals( user.getUserId( ) ) || // user himself or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // in the same account and ...
             ( requestUser.isAccountAdmin( ) || // account admin or ...
               // allowed to list and get user
               ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_GETUSER, requestUser ) &&
                 Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_LISTUSERS, requestUser ) ) ) );
  }

  public static boolean allowReadAccountPolicy( User requestUser, Account account ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           // user is the account admin
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
             requestUser.isAccountAdmin( ) );
  }
  
  public static boolean allowReadGroupPolicy( User requestUser, Account account, Group group ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // in the same account and ...
             ( requestUser.isAccountAdmin( ) || // account admin or ...
               // allowed to list and get group policy
               ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_GETGROUPPOLICY, requestUser ) &&
                 Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_LISTGROUPPOLICIES, requestUser ) ) ) );
  }

  public static boolean allowReadUserPolicy( User requestUser, Account account, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           requestUser.getUserId( ).equals( user.getUserId( ) ) || // user himself or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // in the same account and ...
             ( requestUser.isAccountAdmin( ) || // account admin or ...
               // allowed to list and get user policy
               ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_GETUSERPOLICY, requestUser ) &&
                 Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_LISTUSERPOLICIES, requestUser ) ) ) );
  }
  
  public static boolean allowReadUserCertificate( User requestUser, Account account, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           requestUser.getUserId( ).equals( user.getUserId( ) ) || // user himself or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // in the same account and ...
             ( requestUser.isAccountAdmin( ) || // account admin or ...
               // allowed to list user certificates
               Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_LISTSIGNINGCERTIFICATES, requestUser ) ) );
  }

  
  public static boolean allowReadUserKey( User requestUser, Account account, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) || // system admin or ...
           requestUser.getUserId( ).equals( user.getUserId( ) ) || // user himself or ...
           ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && // in the same account and ...
             ( requestUser.isAccountAdmin( ) || // account admin or ...
               // allowed to list user certificates
               Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_LISTACCESSKEYS, requestUser ) ) );
  }
  
  public static void authorizeModifyAccount( User requestUser, Account account ) throws EucalyptusServiceException {
    if ( Account.SYSTEM_ACCOUNT.equals( account.getName( ) ) ) {
      throw new EucalyptusServiceException( "Can not change system account name" );
    }
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.ALL_RESOURCE, PolicySpec.ALL_RESOURCE, account, PolicySpec.IAM_CREATEACCOUNTALIAS, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeCreateUser( User requestUser, Account account ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, "", account, PolicySpec.IAM_CREATEUSER, requestUser ) &&
                            Permissions.canAllocate( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, "", PolicySpec.IAM_CREATEUSER, requestUser, 1L ) ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize user creation" );
    }
  }
  
  public static void authorizeCreateGroup( User requestUser, Account account ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, "", account, PolicySpec.IAM_CREATEGROUP, requestUser ) &&
                            Permissions.canAllocate( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, "", PolicySpec.IAM_CREATEGROUP, requestUser, 1L ) ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize group creation" );
    }
  }
  
  public static void authorizeDeleteUser( User requestUser, Account account, User user ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_DELETEUSER, requestUser ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize user deletion" );
    }
  }
  
  public static void authorizeDeleteGroup( User requestUser, Account account, Group group ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_DELETEGROUP, requestUser ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize group deletion" );
    }
  }
  
  public static void authorizeAddAccountPolicy( User requestUser ) throws EucalyptusServiceException {
    if ( !requestUser.isSystemAdmin( ) ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeAddUserPolicy( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_PUTUSERPOLICY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeAddGroupPolicy( User requestUser, Account account, Group group ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_PUTGROUPPOLICY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }

  public static void authorizeDeleteUserPolicy( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_DELETEUSERPOLICY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeDeleteGroupPolicy( User requestUser, Account account, Group group ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_DELETEGROUPPOLICY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }

  public static void authorizeDeleteUserAccessKey( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_DELETEACCESSKEY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeDeleteUserCertificate( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_DELETESIGNINGCERTIFICATE, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeAddUserToGroup( User requestUser, Account account, Group group, User user ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_ADDUSERTOGROUP, requestUser ) &&
                            Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_ADDUSERTOGROUP, requestUser ) ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize adding user to group" );
    }
  }
  
  public static void authorizeRemoveUserFromGroup( User requestUser, Account account, Group group, User user ) throws AuthException {
    boolean allowed = requestUser.isSystemAdmin( ) ||
                      ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) && 
                        ( requestUser.isAccountAdmin( ) ||
                          ( Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_REMOVEUSERFROMGROUP, requestUser ) &&
                            Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_REMOVEUSERFROMGROUP, requestUser ) ) ) );
    if ( !allowed ) {
      throw new AuthException( "Failed to authorize removing user from group" );
    }
  }
  
  public static void authorizeAddUserAccessKey( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_CREATEACCESSKEY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeAddUserCertificate( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_UPLOADSIGNINGCERTIFICATE, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeModifyUserAccessKey( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_UPDATEACCESSKEY, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeModifyUserCertificate( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_UPDATESIGNINGCERTIFICATE, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeModifyUser( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_UPDATEUSER, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeModifyUserPassword( User requestUser, Account account, User user ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                requestUser.getUserId( ).equals( user.getUserId( ) ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_CREATELOGINPROFILE, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static void authorizeModifyGroup( User requestUser, Account account, Group group ) throws EucalyptusServiceException {
    boolean allowed = false;
    try {
      allowed = requestUser.isSystemAdmin( ) ||
                ( requestUser.getAccount( ).getAccountNumber( ).equals( account.getAccountNumber( ) ) &&
                  ( requestUser.isAccountAdmin( ) ||
                    Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, PolicySpec.IAM_UPDATEGROUP, requestUser ) ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    if ( !allowed ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
  }
  
  public static boolean allowProcessAccountSignup( User requestUser ) {
    return requestUser.isSystemAdmin( );
  }

  public static boolean allowProcessUserSignup( User requestUser, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) ||
           ( requestUser.getAccount( ).getAccountNumber( ).equals( user.getAccount( ).getAccountNumber( ) ) &&
             requestUser.isAccountAdmin( ) );
  }
  
  public static boolean allowListGroupsForUser( User requestUser, Account account, User user ) throws AuthException {
    return requestUser.isSystemAdmin( ) ||
           requestUser.getUserId( ).equals( user.getUserId( ) ) || // allow user himself to see his groups
           ( requestUser.isAccountAdmin( ) ||
             Permissions.isAuthorized( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, PolicySpec.IAM_LISTGROUPSFORUSER, requestUser ) );
  }
  
}
