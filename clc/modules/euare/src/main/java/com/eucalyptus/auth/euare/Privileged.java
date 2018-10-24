/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare;

import static com.eucalyptus.auth.euare.common.policy.IamPolicySpec.*;

import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationLimitProvider;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.ServerCertificate;
import com.eucalyptus.auth.euare.policy.PolicyArnKey;
import com.eucalyptus.auth.euare.principal.EuareAccessKey;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareCertificate;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicy;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicyVersion;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.euare.principal.GlobalNamespace;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class Privileged {

  public static EuareAccount createAccount(
      @Nonnull  AuthContext requestUser,
      @Nullable String accountName,
      @Nullable String password,
      @Nullable String email
  ) throws AuthException {
    if ( !requestUser.isSystemUser( ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, "", (AccountFullName)null, IAM_CREATEACCOUNT, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }

    if ( accountName != null ) {
      Accounts.reserveGlobalName( GlobalNamespace.Account_Alias, accountName );
    }
    EuareAccount newAccount = Accounts.addAccount( accountName );
    Map<String, String> info = null;
    if ( email != null ) {
      info = Maps.newHashMap( );
      info.put( EuareUser.EMAIL, email );
    }
    EuareUser admin = newAccount.addUser( User.ACCOUNT_ADMIN, "/", true/*enabled*/, info );
    admin.resetToken();
    if ( password != null ) {
      admin.setPassword( Crypto.generateEncryptedPassword( password ) );
      admin.setPasswordExpires( System.currentTimeMillis( ) + AuthenticationLimitProvider.Values.getDefaultPasswordExpiry( ) );
    }
    return newAccount;
  }

  public static void deleteAccount( AuthContext requestUser, EuareAccount account, boolean recursive ) throws AuthException {
    if ( !requestUser.isSystemUser() ||
        !RestrictedTypes.filterPrivileged( ).apply( account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    Accounts.deleteAccount( account.getName(), false/*forceDeleteSystem*/, recursive );
  }

  public static boolean allowListOrReadAccountPolicy( AuthContext requestUser, EuareAccount account ) throws AuthException {
    return requestUser.isSystemUser() &&
        RestrictedTypes.filterPrivileged( ).apply( account ) &&
        Permissions.isAuthorized( requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, IAM_LISTACCOUNTPOLICIES ), account.getAccountNumber(), Accounts.getAccountFullName(account) ) &&
        Permissions.isAuthorized( requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, IAM_GETACCOUNTPOLICY ), account.getAccountNumber(), Accounts.getAccountFullName(account) );
  }

  public static void modifyAccount( AuthContext requestUser, EuareAccount account, String newName ) throws AuthException {
    if ( Accounts.isSystemAccount( account.getName( ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, Accounts.getAccountFullName(account), account, IAM_CREATEACCOUNTALIAS, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    try {
      Accounts.reserveGlobalName( GlobalNamespace.Account_Alias, newName );
      account.setName( newName );
    } catch ( AuthException ae ) {
      if ( AuthException.INVALID_NAME.equals( ae.getMessage( ) ) ) {
        throw ae;
      }
      throw new AuthException( AuthException.CONFLICT );
    }
  }

  public static void deleteAccountAlias( AuthContext requestUser, EuareAccount account, String alias ) throws AuthException {
    if ( Accounts.isSystemAccount( account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, Accounts.getAccountFullName(account), account, IAM_DELETEACCOUNTALIAS, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( alias ) ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    // Only one alias is allowed by AWS IAM spec. Overwrite the current alias if matches.
    if ( alias.equals( account.getAccountAlias( ) ) ) {
      account.setNameUnsafe( account.getAccountNumber( ) );
    }
  }

  public static List<String> listAccountAliases( AuthContext requestUser, EuareAccount account ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, Accounts.getAccountFullName(account), account, IAM_LISTACCOUNTALIASES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    List<String> aliases = Lists.newArrayList( );
    if ( account.hasAccountAlias( ) ) {
      aliases.add( account.getAccountAlias( ) );
    }
    return aliases;
  }

  public static EuareAccount getAccountSummary( AuthContext requestUser, EuareAccount account ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ACCOUNT, Accounts.getAccountFullName(account), account, IAM_GETACCOUNTSUMMARY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return account;
  }

  public static EuareGroup createGroup( AuthContext requestUser, EuareAccount account, String groupName, String path ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, "", account, IAM_CREATEGROUP, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_GROUP, "", IAM_CREATEGROUP, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.addGroup( groupName, path );
  }

  public static boolean allowListGroup( AuthContext requestUser, EuareAccount account, EuareGroup group ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_GROUP, IAM_LISTGROUPS ),
            account.getAccountNumber( ),
            Accounts.getGroupFullName( group ) );
  }

  public static boolean allowReadGroup( AuthContext requestUser, EuareAccount account, EuareGroup group ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_GETGROUP, requestUser );
  }

  public static void deleteGroup( AuthContext requestUser, EuareAccount account, EuareGroup group, boolean recursive ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_DELETEGROUP, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteGroup( group.getName( ), recursive );
  }

  public static void modifyGroup( AuthContext requestUser, EuareAccount account, EuareGroup group, String newName, String newPath ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_UPDATEGROUP, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Strings.isNullOrEmpty( newName ) ) {
      group.setName( newName );
    }
    if ( !Strings.isNullOrEmpty( newPath ) ) {
      group.setPath( newPath );
    }
  }

  public static EuareUser createUser( AuthContext requestUser, EuareAccount account, String userName, String path ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, "", account, IAM_CREATEUSER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_USER, "", IAM_CREATEUSER, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.addUser( userName, path, true, null );
  }

  public static boolean allowReadUser( AuthContext requestUser, EuareAccount account, User user ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_GETUSER, requestUser );
  }

  public static boolean allowListUser( AuthContext requestUser, EuareAccount account, User user ) throws AuthException {
    return Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_USER, IAM_LISTUSERS ),
            account.getAccountNumber( ),
            Accounts.getUserFullName( user ) );
  }

  public static void deleteUser( AuthContext requestUser, EuareAccount account, EuareUser user, boolean recursive ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DELETEUSER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( user.isAccountAdmin( ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteUser( user.getName( ), false/*forceDeleteAdmin*/, recursive );
  }

  public static void modifyUser( AuthContext requestUser, EuareAccount account, EuareUser user, String newName, String newPath, Boolean enabled, Long passwordExpires, Map<String, String> info ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPDATEUSER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Strings.isNullOrEmpty( newName ) ) {
      // Not allowed to modify admin user
      if ( user.isAccountAdmin( ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      user.setName( newName );
    }
    if ( !Strings.isNullOrEmpty( newPath ) ) {
      // Not allowed to modify admin user
      if ( user.isAccountAdmin( ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      user.setPath( newPath );
    }
    if ( enabled != null ) {
      // Not allowed to disable system admin (admin@eucalyptus) user
      // Only system user can disable account admin
      if ( user.isSystemAdmin( ) && user.isAccountAdmin( ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      } else if ( user.isAccountAdmin( ) && !requestUser.isSystemUser( ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      user.setEnabled( enabled );
    }
    if ( passwordExpires != null ) {
      if ( passwordExpires > new Date( ).getTime( ) ) {
        user.setPasswordExpires( passwordExpires );
      }
    }
    if ( info != null ) {
      user.setInfo( info );
    }
  }

  public static void updateUserInfoItem( AuthContext requestUser, EuareAccount account, EuareUser user, String key, String value ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPDATEUSER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( value != null ) {
      user.setInfo( key, value );
    } else {
      user.removeInfo( key );
    }
  }

  public static void addUserToGroup( AuthContext requestUser, EuareAccount account, User user, EuareGroup group ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_ADDUSERTOGROUP, requestUser ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_ADDUSERTOGROUP, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !group.hasUser( user.getName( ) ) ) {
      group.addUserByName( user.getName( ) );
    }
  }

  public static void removeUserFromGroup( AuthContext requestUser, EuareAccount account, User user, EuareGroup group ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_REMOVEUSERFROMGROUP, requestUser ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_REMOVEUSERFROMGROUP, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( group.hasUser( user.getName( ) ) ) {
      group.removeUserByName( user.getName( ) );
    }
  }

  public static List<EuareGroup> listGroupsForUser( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_LISTGROUPSFORUSER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    List<EuareGroup> groups = Lists.newArrayList( );
    for ( EuareGroup g : user.getGroups( ) ) {
      if ( !g.isUserGroup( ) ) {
        groups.add( g );
      }
    }
    return groups;
  }

  public static EuareRole createRole( AuthContext requestUser, EuareAccount account, String roleName, String path, String assumeRolePolicy ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, "", account, IAM_CREATEROLE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_ROLE, "", IAM_CREATEROLE, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.addRole( roleName, path, assumeRolePolicy );
  }

  public static boolean allowListRole( AuthContext requestUser, EuareAccount account, BaseRole role ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_ROLE, IAM_LISTROLES ),
            account.getAccountNumber( ),
            Accounts.getRoleFullName( role ) );
  }

  public static boolean allowReadRole( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_GETROLE, requestUser );
  }

  public static void deleteRole( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_DELETEROLE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteRole( role.getName( ) );
  }

  public static void updateAssumeRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, String assumeRolePolicy ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_UPDATEASSUMEROLEPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    role.setAssumeRolePolicy( assumeRolePolicy );
  }

  public static EuareInstanceProfile createInstanceProfile( AuthContext requestUser, EuareAccount account, String instanceProfileName, String path ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, "", account, IAM_CREATEINSTANCEPROFILE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, "", IAM_CREATEINSTANCEPROFILE, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.addInstanceProfile( instanceProfileName, path );
  }

  public static boolean allowListInstanceProfile( AuthContext requestUser, EuareAccount account, BaseInstanceProfile instanceProfile ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, IAM_LISTINSTANCEPROFILES ),
            account.getAccountNumber( ),
            Accounts.getInstanceProfileFullName( instanceProfile ) );
  }

  public static boolean allowReadInstanceProfile( AuthContext requestUser, EuareAccount account, EuareInstanceProfile instanceProfile ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, Accounts.getInstanceProfileFullName( instanceProfile ), account, IAM_GETINSTANCEPROFILE, requestUser );
  }

  public static void deleteInstanceProfile( AuthContext requestUser, EuareAccount account, EuareInstanceProfile instanceProfile ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, Accounts.getInstanceProfileFullName( instanceProfile ), account, IAM_DELETEINSTANCEPROFILE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteInstanceProfile( instanceProfile.getName( ) );
  }

  public static void addRoleToInstanceProfile( AuthContext requestUser, EuareAccount account, EuareInstanceProfile instanceProfile, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, Accounts.getInstanceProfileFullName( instanceProfile ), account, IAM_ADDROLETOINSTANCEPROFILE, requestUser ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_ADDROLETOINSTANCEPROFILE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    final EuareRole currentRole = instanceProfile.getRole();
    if ( currentRole != null && currentRole.getName().equals( role.getName() ) ) {
      throw new AuthException( AuthException.CONFLICT );
    }
    instanceProfile.setRole( role );
  }

  public static void removeRoleFromInstanceProfile( AuthContext requestUser, EuareAccount account, EuareInstanceProfile instanceProfile, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, Accounts.getInstanceProfileFullName( instanceProfile ), account, IAM_REMOVEROLEFROMINSTANCEPROFILE, requestUser ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_REMOVEROLEFROMINSTANCEPROFILE, requestUser )) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    instanceProfile.setRole( null );
  }

  public static List<EuareInstanceProfile> listInstanceProfilesForRole( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_LISTINSTANCEPROFILESFORROLE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return role.getInstanceProfiles();
  }

  public static boolean allowListInstanceProfileForRole( AuthContext requestUser, EuareAccount account, EuareInstanceProfile instanceProfile ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_INSTANCE_PROFILE, IAM_LISTINSTANCEPROFILESFORROLE ),
            account.getAccountNumber( ),
            Accounts.getInstanceProfileFullName( instanceProfile ) );
  }

  public static boolean allowReadPolicy( AuthContext requestUser, EuareAccount account, EuareManagedPolicy policy ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), account, IAM_GETPOLICY, requestUser );
  }

  public static boolean allowReadPolicyVersion( AuthContext requestUser, EuareAccount account, EuareManagedPolicy policy ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), account, IAM_GETPOLICYVERSION, requestUser );
  }

  public static boolean allowListPolicy( AuthContext requestUser, EuareAccount account, EuareManagedPolicy policy ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_POLICY, IAM_LISTPOLICIES ),
            account.getAccountNumber( ),
            Accounts.getManagedPolicyFullName( policy ) );
  }

  public static boolean allowListPolicyVersion( AuthContext requestUser, EuareAccount account, EuareManagedPolicy policy ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_POLICY, IAM_LISTPOLICYVERSIONS ),
            account.getAccountNumber( ),
            Accounts.getManagedPolicyFullName( policy ) );
  }

  public static EuareManagedPolicy createManagedPolicy(
      final AuthContext requestUser,
      final EuareAccount account,
      final String name,
      final String path,
      final String description,
      final String policy
  ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, "", account, IAM_CREATEPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_POLICY, "", IAM_CREATEPOLICY, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.addPolicy( name, path, description, policy );
  }

  public static void deletePolicy(
      final AuthContext requestUser,
      final EuareAccount account,
      final EuareManagedPolicy policy
  ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), account, IAM_DELETEPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deletePolicy( policy.getName( ) );
  }

  public static EuareManagedPolicyVersion createManagedPolicyVersion(
      final AuthContext requestUser,
      final EuareAccount account,
      final EuareManagedPolicy managedPolicy,
      final String policy,
      final boolean setAsDefault
  ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( managedPolicy ), account, IAM_CREATEPOLICYVERSION, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return managedPolicy.addPolicyVersion( policy, setAsDefault );
  }

  public static void deleteManagedPolicyVersion(
      final AuthContext requestUser,
      final EuareAccount account,
      final EuareManagedPolicy managedPolicy,
      final Integer versionId
  ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( managedPolicy ), account, IAM_DELETEPOLICYVERSION, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    managedPolicy.deletePolicyVersion( versionId );
  }

  public static void setDefaultPolicyVersion(
      final AuthContext requestUser,
      final EuareAccount account,
      final EuareManagedPolicy policy,
      final Integer versionId
  ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), account, IAM_SETDEFAULTPOLICYVERSION, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    policy.setPolicyVersion( versionId );
  }

  private static void authWithPolicy( final EuareManagedPolicy policy, final Callable<Void> callable ) throws AuthException {
    try {
      PolicyEvaluationContext.builder( )
          .attr( PolicyArnKey.CONTEXT_KEY, Accounts.getManagedPolicyArn( policy ) )
          .build( )
          .doWithContext( callable );
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw Exceptions.toUndeclared( e );
    }
  }

  public static void attachGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group, EuareManagedPolicy policy ) throws AuthException  {
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_ATTACHGROUPPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_ATTACHGROUPPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( group.getAttachedPolicies( ).size( ) >= AuthenticationLimitProvider.Values.getPolicyAttachmentLimit( ) ) {
        throw new AuthException( AuthException.QUOTA_EXCEEDED );
      }
      return null;
    } );
    group.attachPolicy( policy );
  }

  public static void attachRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, EuareManagedPolicy policy ) throws AuthException  {
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_ATTACHROLEPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_ATTACHROLEPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( role.getAttachedPolicies( ).size( ) >= AuthenticationLimitProvider.Values.getPolicyAttachmentLimit( ) ) {
        throw new AuthException( AuthException.QUOTA_EXCEEDED );
      }
      return null;
    } );
    role.attachPolicy( policy );
  }

  public static void attachUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user, EuareManagedPolicy policy ) throws AuthException  {
    if ( user.isAccountAdmin( ) ) { // policy attached to admin is reserved, could be used for account policy
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_ATTACHUSERPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_ATTACHUSERPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( user.getAttachedPolicies( ).size( ) >= AuthenticationLimitProvider.Values.getPolicyAttachmentLimit( ) ) {
        throw new AuthException( AuthException.QUOTA_EXCEEDED );
      }
      return null;
    } );
    user.attachPolicy( policy );
  }

  public static void detachGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group, EuareManagedPolicy policy ) throws AuthException  {
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_DETACHGROUPPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_DETACHGROUPPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      return null;
    } );
    group.detachPolicy( policy );
  }

  public static void detachRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, EuareManagedPolicy policy ) throws AuthException  {
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_DETACHROLEPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_DETACHROLEPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      return null;
    } );
    role.detachPolicy( policy );
  }

  public static void detachUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user, EuareManagedPolicy policy ) throws AuthException  {
    authWithPolicy( policy, () -> {
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DETACHUSERPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_DETACHUSERPOLICY, requestUser ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      return null;
    } );
    user.detachPolicy( policy );
  }

  public static List<EuareManagedPolicy> listGroupAttachedPolicies( AuthContext requestUser, EuareAccount account, EuareGroup group ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_LISTATTACHEDGROUPPOLICIES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return group.getAttachedPolicies( );
  }

  public static List<EuareManagedPolicy> listRoleAttachedPolicies( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_LISTATTACHEDROLEPOLICIES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return role.getAttachedPolicies( );
  }

  public static List<EuareManagedPolicy> listUserAttachedPolicies( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_LISTATTACHEDUSERPOLICIES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return user.getAttachedPolicies( );
  }

  public static List<EuareGroup> listGroupsWithAttachedPolicy( AuthContext requestUser, EuareManagedPolicy policy ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_LISTENTITIESFORPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return policy.getGroups( );
  }

  public static List<EuareRole> listRolesWithAttachedPolicy( AuthContext requestUser, EuareManagedPolicy policy ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_LISTENTITIESFORPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return policy.getRoles( );
  }

  public static List<EuareUser> listUsersWithAttachedPolicy( AuthContext requestUser, EuareManagedPolicy policy ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_POLICY, Accounts.getManagedPolicyFullName( policy ), policy.getAccount( ), IAM_LISTENTITIESFORPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return policy.getUsers( );
  }

  public static void putAccountPolicy( AuthContext requestUser, EuareAccount account, String name, String policy ) throws AuthException, PolicyParseException {
    if ( !requestUser.isSystemUser( ) ||
        !RestrictedTypes.filterPrivileged( ).apply( account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    // Can not add policy to system account "eucalyptus"
    if ( EuareAccount.SYSTEM_ACCOUNT.equals( account.getName( ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    EuareUser admin = account.lookupAdmin();
    admin.putPolicy( name, policy );
  }

  public static void putGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group, String name, String policy ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_PUTGROUPPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    group.putPolicy( name, policy );
  }

  public static void putUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user, String name, String policy ) throws AuthException, PolicyParseException {
    // Policy attached to account admin is the account policy. Only system admin can put policy to an account.
    if ( user.isAccountAdmin( ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_PUTUSERPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    user.putPolicy( name, policy );
  }

  public static void putRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, String name, String policy ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_PUTROLEPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    role.putPolicy( name, policy );
  }

  public static void deleteAccountPolicy( AuthContext requestUser, EuareAccount account, String name ) throws AuthException {
    if ( !requestUser.isSystemUser() ||
        !RestrictedTypes.filterPrivileged( ).apply( account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    EuareUser admin = account.lookupAdmin();
    admin.removePolicy( name );
  }

  public static void deleteGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group, String name ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_DELETEGROUPPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    group.removePolicy( name );
  }

  public static void deleteUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user, String name ) throws AuthException {
    // Policy attached to account admin is the account policy.
    if ( user.isAccountAdmin( ) ||
        !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DELETEUSERPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    user.removePolicy( name );
  }

  public static void deleteRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, String name ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_DELETEROLEPOLICY, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    role.removePolicy( name );
  }

  public static List<Policy> listAccountPolicies( AuthContext requestUser, EuareAccount account ) throws AuthException {
    if ( !allowListOrReadAccountPolicy( requestUser, account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    EuareUser admin = account.lookupAdmin();
    return admin.getPolicies( );
  }

  public static List<Policy> listGroupPolicies( AuthContext requestUser, EuareAccount account, EuareGroup group ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_GROUP, Accounts.getGroupFullName( group ), account, IAM_LISTGROUPPOLICIES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return group.getPolicies( );
  }

  public static List<Policy> listUserPolicies( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !allowListUserPolicy( requestUser, account, user ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return user.getPolicies( );
  }

  public static List<Policy> listRolePolicies( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_LISTROLEPOLICIES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return role.getPolicies( );
  }

  public static Policy getAccountPolicy( AuthContext requestUser, EuareAccount account, String policyName ) throws AuthException {
    if ( !allowListOrReadAccountPolicy( requestUser, account ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( policyName ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    EuareUser admin = account.lookupAdmin();
    Policy policy = null;
    for ( Policy p : admin.getPolicies( ) ) {
      if ( p.getName( ).equals( policyName ) ) {
        policy = p;
        break;
      }
    }
    return policy;
  }

  public static Policy getGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group, String policyName ) throws AuthException {
    if ( !allowReadGroupPolicy( requestUser, account, group ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( policyName ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    Policy policy = null;
    for ( Policy p : group.getPolicies( ) ) {
      if ( p.getName( ).equals( policyName ) ) {
        policy = p;
        break;
      }
    }
    return policy;
  }

  public static Policy getUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user, String policyName ) throws AuthException {
    if ( !allowReadUserPolicy( requestUser, account, user ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( policyName ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    Policy policy = null;
    for ( Policy p : user.getPolicies( ) ) {
      if ( p.getName( ).equals( policyName ) ) {
        policy = p;
        break;
      }
    }
    return policy;
  }

  public static Policy getRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role, String policyName ) throws AuthException {
    if ( !allowReadRolePolicy( requestUser, account, role ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( policyName ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    Policy policy = null;
    for ( Policy p : role.getPolicies( ) ) {
      if ( p.getName( ).equals( policyName ) ) {
        policy = p;
        break;
      }
    }
    return policy;
  }

  public static boolean allowReadGroupPolicy( AuthContext requestUser, EuareAccount account, EuareGroup group ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_GROUP, IAM_GETGROUPPOLICY ),
            account.getAccountNumber(),
            Accounts.getGroupFullName( group ) );
  }

  public static boolean allowReadRolePolicy( AuthContext requestUser, EuareAccount account, EuareRole role ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_ROLE, Accounts.getRoleFullName( role ), account, IAM_GETROLEPOLICY, requestUser );
  }

  public static boolean allowListUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    return !user.isAccountAdmin( ) && // we are not looking at account admin's policies and authorized
        Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_LISTUSERPOLICIES, requestUser );
  }

  public static boolean allowReadUserPolicy( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    return !user.isAccountAdmin( ) && // we are not looking at account admin's policies and authorized
        Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_GETUSERPOLICY, requestUser );
  }

  public static boolean allowListAccessKeys( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    return Permissions.isAuthorized(
        requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_USER, IAM_LISTACCESSKEYS ),
        account.getAccountNumber( ),
        Accounts.getUserFullName( user ) ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ;
  }

  public static boolean allowListSigningCertificates( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    return Permissions.isAuthorized(
        requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_USER, IAM_LISTSIGNINGCERTIFICATES ),
        account.getAccountNumber( ),
        Accounts.getUserFullName( user ) ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ;
  }

  public static AccessKey createAccessKey( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_CREATEACCESSKEY, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( user.getKeys( ).size( ) >= AuthenticationLimitProvider.Values.getAccessKeyLimit( ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return user.createKey( );
  }

  public static void deleteAccessKey( AuthContext requestUser, EuareAccount account, EuareUser user, String keyId ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DELETEACCESSKEY, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    user.removeKey( keyId );
  }

  public static List<AccessKey> listAccessKeys( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !allowListAccessKeys( requestUser, account, user ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return user.getKeys();
  }

  public static void modifyAccessKey( AuthContext requestUser, EuareAccount account, EuareUser user, String keyId, String status ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPDATEACCESSKEY, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( keyId ) ) {
      throw new AuthException( AuthException.EMPTY_KEY_ID );
    }
    if ( Strings.isNullOrEmpty( status ) ) {
      throw new AuthException( AuthException.EMPTY_STATUS );
    }
    final EuareAccessKey key = user.getKey( keyId );
    key.setActive( "Active".equalsIgnoreCase( status ) );
  }

  public static Certificate createSigningCertificate( AuthContext requestUser, EuareAccount account, EuareUser user, KeyPair keyPair ) throws AuthException {
    // Use the official UPLOADSIGNINGCERTIFICATE action here
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPLOADSIGNINGCERTIFICATE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) )  ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( user.getCertificates( ).size( ) >= AuthenticationLimitProvider.Values.getSigningCertificateLimit( ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    X509Certificate x509 = Certs.generateCertificate( keyPair, user.getName() );
    String certificateId;
    try {
      x509.checkValidity( );
      certificateId = Identifiers.generateCertificateIdentifier( x509 );
    } catch ( Exception e ) {
      throw new AuthException( "Invalid X509 Certificate", e );
    }
    Accounts.reserveGlobalName( GlobalNamespace.Signing_Certificate_Id, certificateId );
    return user.addCertificate( certificateId, x509 );
  }

  public static Certificate uploadSigningCertificate( AuthContext requestUser, EuareAccount account, EuareUser user, String certBody ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPLOADSIGNINGCERTIFICATE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( user.getCertificates( ).size( ) >= AuthenticationLimitProvider.Values.getSigningCertificateLimit( ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    if ( Strings.isNullOrEmpty( certBody ) ) {
      throw new AuthException( AuthException.EMPTY_CERT );
    }
    final String encodedPem = B64.url.encString( certBody );
    final X509Certificate x509 = X509CertHelper.toCertificate( encodedPem );
    if ( x509 == null ) {
      throw new AuthException( AuthException.INVALID_CERT );
    }
    String certificateId;
    try {
      certificateId = Identifiers.generateCertificateIdentifier( x509 );
    } catch ( CertificateEncodingException e ) {
      throw new AuthException( "Invalid X509 Certificate", e );
    }
    Accounts.reserveGlobalName( GlobalNamespace.Signing_Certificate_Id, certificateId );
    for ( Certificate c : user.getCertificates( ) ) {
      if ( c.getPem( ).equals( encodedPem ) ) {
        throw new AuthException( AuthException.CONFLICT );
      }
    }
    return user.addCertificate( certificateId, x509 );
  }

  public static List<Certificate> listSigningCertificates( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !allowListSigningCertificates( requestUser, account, user ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    List<Certificate> certs = Lists.newArrayList( );
    for ( Certificate cert : user.getCertificates( ) ) {
      certs.add( cert );
    }
    return certs;
  }

  public static void deleteSigningCertificate( AuthContext requestUser, EuareAccount account, EuareUser user, String certId ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DELETESIGNINGCERTIFICATE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    user.removeCertificate( certId );
  }

  public static void modifySigningCertificate( AuthContext requestUser, EuareAccount account, EuareUser user, String certId, String status ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPDATESIGNINGCERTIFICATE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( Strings.isNullOrEmpty( status ) ) {
      throw new AuthException( AuthException.EMPTY_STATUS );
    }
    if ( Strings.isNullOrEmpty( certId ) ) {
      throw new AuthException( AuthException.EMPTY_CERT_ID );
    }
    EuareCertificate cert = user.getCertificate( certId );
    cert.setActive( "Active".equalsIgnoreCase( status ) );
  }

  /**
   * Is the user allows to perform actions that should be restricted to the
   * account administrator or a system user with appropriate permissions.
   *
   * An authorization check is required in addition to this check.
   */
  private static boolean accountAdminActionPermittedIfAuthorized( final AuthContext requestUser,
                                                                  final User user ) {
    return !user.isAccountAdmin( ) || requestUser.isAccountAdmin( ) || requestUser.isSystemUser( );
  }

  public static void createLoginProfile( AuthContext requestUser, EuareAccount account, EuareUser user, String password ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_CREATELOGINPROFILE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    setUserPassword( user, password );
  }

  public static void deleteLoginProfile( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_DELETELOGINPROFILE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    user.setPassword( null );
  }

  public static boolean allowReadLoginProfile( AuthContext requestUser, EuareAccount account, EuareUser user ) throws AuthException {
    return Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_GETLOGINPROFILE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user );
  }

  public static void updateLoginProfile( AuthContext requestUser, EuareAccount account, EuareUser user, String newPass ) throws AuthException {
    if ( !(Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_USER, Accounts.getUserFullName( user ), account, IAM_UPDATELOGINPROFILE, requestUser ) &&
        accountAdminActionPermittedIfAuthorized( requestUser, user ) ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( null == user.getPassword( ) ) {
      throw new AuthException( AuthException.NO_SUCH_LOGIN_PROFILE );
    }
    setUserPassword( user, newPass );
  }

  private static void setUserPassword( EuareUser user, String newPass ) throws AuthException {
    if ( Strings.isNullOrEmpty( newPass ) || user.getName( ).equals( newPass ) || newPass.length( ) > EuareUser.MAX_PASSWORD_LENGTH ) {
      throw new AuthException( AuthException.INVALID_PASSWORD );
    }
    String newEncrypted = Crypto.generateEncryptedPassword( newPass );
    user.setPassword( newEncrypted );
    user.setPasswordExpires( System.currentTimeMillis( ) + AuthenticationLimitProvider.Values.getDefaultPasswordExpiry( ) );
  }

  public static void createServerCertificate( final AuthContext requestUser,
                                              final EuareAccount account,
                                              final String pemCertBody,
                                              final String pemCertChain,
                                              final String path,
                                              final String certName,
                                              final String pemPk ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, certName, account, IAM_UPLOADSERVERCERTIFICATE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }

    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, "", IAM_UPLOADSERVERCERTIFICATE, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }

    account.addServerCertificate(certName, pemCertBody, pemCertChain, path, pemPk);
  }

  public static List<ServerCertificate> listServerCertificate( final AuthContext requestUser,
                                                               final EuareAccount account,
                                                               final String pathPrefix ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, pathPrefix, account, IAM_LISTSERVERCERTIFICATES, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return account.listServerCertificates(pathPrefix);
  }

  public static ServerCertificate getServerCertificate( final AuthContext requestUser,
                                                        final EuareAccount account,
                                                        final String certName ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, certName, account, IAM_GETSERVERCERTIFICATE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    return account.lookupServerCertificate(certName);
  }

  public static void deleteServerCertificate( final AuthContext requestUser,
                                              final EuareAccount account,
                                              final String certName ) throws AuthException{
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, certName, account, IAM_DELETESERVERCERTIFICATE, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteServerCertificate(certName);
  }

  public static void updateServerCertificate( final AuthContext requestUser,
                                              final EuareAccount account,
                                              final String certName,
                                              final String newCertName,
                                              final String newPath ) throws AuthException {
    if ( ! ( Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, certName, account, IAM_UPDATESERVERCERTIFICATE, requestUser ) &&
             Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_SERVER_CERTIFICATE, newCertName, account, IAM_UPDATESERVERCERTIFICATE, requestUser ) ) ) {
            throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.updateServerCeritificate(certName, newCertName, newPath);
  }

  /* open id methods */
  public static EuareOpenIdConnectProvider createOpenIdConnectProvider( AuthContext requestUser, EuareAccount account, String url, List<String> clientIDs, List<String> thumbprints ) throws AuthException, PolicyParseException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, "", account, IAM_CREATEOPENIDCONNECTPROVIDER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    if ( !Permissions.canAllocate( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, "", IAM_CREATEOPENIDCONNECTPROVIDER, requestUser, 1L ) ) {
      throw new AuthException( AuthException.QUOTA_EXCEEDED );
    }
    return account.createOpenIdConnectProvider( url, clientIDs, thumbprints );
  }

  public static void deleteOpenIdConnectProvider( AuthContext requestUser, EuareAccount account, EuareOpenIdConnectProvider provider ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, provider.getUrl(), account, IAM_DELETEOPENIDCONNECTPROVIDER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.deleteOpenIdConnectProvider( provider.getUrl( ) );
  }

  public static boolean allowListOpenIdConnectProviders( AuthContext requestUser, EuareAccount account, String url) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, IAM_LISTOPENIDCONNECTPROVIDERS ),
            account.getAccountNumber( ),
            url );
  }

  public static boolean allowGetOpenIdConnectProvider( AuthContext requestUser, EuareAccount account, String url ) throws AuthException {
    return
        Permissions.isAuthorized(
            requestUser.evaluationContext( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, IAM_GETOPENIDCONNECTPROVIDER ),
            account.getAccountNumber( ),
            url );
  }

  public static void addClientIdToOpenIdConnectProvider( AuthContext requestUser, EuareAccount account, EuareOpenIdConnectProvider provider, String clientId ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, provider.getUrl(), account, IAM_ADDCLIENTIDTOOPENIDCONNECTPROVIDER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.addClientIdToOpenIdConnectProvider( clientId, provider.getUrl( ) );
  }

  public static void removeClientIdFromOpenIdConnectProvider( AuthContext requestUser, EuareAccount account, EuareOpenIdConnectProvider provider, String clientId ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, provider.getUrl(), account, IAM_REMOVECLIENTIDFROMOPENIDCONNECTPROVIDER, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.removeClientIdFromOpenIdConnectProvider( clientId, provider.getUrl( ) );
  }

  public static void updateOpenIdConnectProviderThumbprint( AuthContext requestUser, EuareAccount account, EuareOpenIdConnectProvider provider, List<String> thumbprints ) throws AuthException {
    if ( !Permissions.isAuthorized( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER, provider.getUrl(), account, IAM_UPDATEOPENIDCONNECTPROVIDERTHUMBPRINT, requestUser ) ) {
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
    account.updateOpenIdConnectProviderThumbprint( provider.getUrl( ), thumbprints );
  }
}
