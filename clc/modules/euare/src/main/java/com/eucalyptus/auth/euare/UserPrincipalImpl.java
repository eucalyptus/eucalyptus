/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare;

import static com.eucalyptus.auth.principal.Certificate.Util.active;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.euare.persist.DatabaseAuthUtils;
import com.eucalyptus.auth.euare.persist.DatabaseManagedPolicyProxy;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicy;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.HasRole;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.Strings;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;

/**
 *
 */
public class UserPrincipalImpl implements UserPrincipal, HasRole {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String name;

  @Nonnull
  private final String path;

  @Nonnull
  private final String userId;

  @Nonnull
  private final String authenticatedId;

  @Nullable
  private final String token;

  @Nonnull
  private final String accountAlias;

  @Nonnull
  private final String accountNumber;

  @Nonnull
  private final String canonicalId;

  private final boolean enabled;

  private final boolean accountAdmin;

  private final boolean systemAdmin;

  private final boolean systemUser;

  @Nullable
  private final String password;

  @Nullable
  private final Long passwordExpires;

  @Nullable
  private final Role role;

  @Nonnull
  private final ImmutableList<AccessKey> keys;

  @Nonnull
  private final ImmutableList<Certificate> certificates;

  @Nonnull
  private final ImmutableList<PolicyVersion> principalPolicies;

  /**
   * TODO - When changing fields, update ptag calculation
   * TODO - When changing fields, update ptag calculation
   * TODO - When changing fields, update ptag calculation
   * TODO - When changing fields, update ptag calculation
   * TODO - When changing fields, update ptag calculation
   * @see #ptag(UserPrincipal)
   */
  @Nullable
  private final String ptag;

  public UserPrincipalImpl( final UserEntity user ) throws AuthException {
    final List<GroupEntity> groups = user.getGroups();
    final AccountEntity account = groups.get( 0 ).getAccount( );

    final List<PolicyVersion> policies = Lists.newArrayList( );
    if ( user.isEnabled( ) ) {
      if ( DatabaseAuthUtils.isAccountAdmin( user.getName( ) ) ) {
        policies.add( PolicyVersions.getAdministratorPolicy( ) );
      } else {
        for ( final GroupEntity group : groups ) {
          if ( group.isUserGroup( ) ) {
            Iterables.addAll(
                policies,
                Iterables.transform(
                    group.getPolicies( ),
                    Functions.compose( PolicyVersions.policyVersion( PolicyScope.User, new EuareResourceName( account.getAccountNumber( ), IamPolicySpec.IAM_RESOURCE_USER, user.getPath( ), user.getName( ) ).toString( ) ), PolicyTransform.INSTANCE ) ) );
          }
        }
        for ( final ManagedPolicyEntity managedPolicy : user.getAttachedPolicies( ) ) {
          final EuareManagedPolicy euareManagedPolicy = new DatabaseManagedPolicyProxy( managedPolicy );
          policies.add( PolicyVersions.policyVersion( euareManagedPolicy, Accounts.getManagedPolicyArn( euareManagedPolicy ) ) );
        }

        for ( final GroupEntity group : groups ) {
          if ( !group.isUserGroup( ) ) {
            Iterables.addAll(
                policies,
                Iterables.transform(
                    group.getPolicies( ),
                    Functions.compose( PolicyVersions.policyVersion( PolicyScope.Group, new EuareResourceName( account.getAccountNumber( ), IamPolicySpec.IAM_RESOURCE_GROUP, group.getPath( ), group.getName( ) ).toString( ) ), PolicyTransform.INSTANCE ) ) );
            for ( final ManagedPolicyEntity managedPolicy : group.getAttachedPolicies( ) ) {
              final EuareManagedPolicy euareManagedPolicy = new DatabaseManagedPolicyProxy( managedPolicy );
              policies.add( PolicyVersions.policyVersion( euareManagedPolicy, Accounts.getManagedPolicyArn( euareManagedPolicy ) ) );
            }
          }
        }
      }
      UserEntity admin;
      try {
        admin = DatabaseAuthUtils.getUniqueUser( User.ACCOUNT_ADMIN, account.getName( ) );
      } catch ( Exception e ) {
        throw new AuthException( e );
      }
      if ( admin != null ) {
        for ( final GroupEntity group : admin.getGroups( ) ) {
          if ( group.isUserGroup( ) ) {
            Iterables.addAll(
                policies,
                Iterables.transform(
                    group.getPolicies( ),
                    Functions.compose( PolicyVersions.policyVersion( PolicyScope.Account, account.getAccountNumber( ) ), PolicyTransform.INSTANCE ) ) );
          }
        }
      }
    }

    this.name = user.getName( );
    this.path = user.getPath();
    this.userId = user.getUserId();
    this.authenticatedId = user.getUserId();
    this.canonicalId = account.getCanonicalId();
    this.token = user.getToken();
    this.accountAlias = account.getName();
    this.accountNumber = account.getAccountNumber();
    this.enabled = user.isEnabled();
    this.accountAdmin = DatabaseAuthUtils.isAccountAdmin( user.getName( ) );
    this.systemAdmin = Accounts.isAdministrativeAccount( account.getName( ) );
    this.systemUser = systemAdmin;
    this.password = user.getPassword();
    this.passwordExpires = password == null ? null : MoreObjects.firstNonNull( user.getPasswordExpires( ), Long.MAX_VALUE );
    this.role = null;
    this.keys = ImmutableList.copyOf( Iterables.filter( Iterables.transform( user.getKeys( ), ekeyWrapper( this ) ), AccessKeys.isActive( ) ) );
    this.certificates = ImmutableList.copyOf(
        Iterables.filter( Iterables.transform( user.getCertificates( ), ecertWrapper( this ) ), propertyPredicate( true, active( ) ) ) );
    this.principalPolicies = ImmutableList.copyOf( policies );
    this.ptag = null;
  }

  public UserPrincipalImpl( final EuareUser user ) throws AuthException {
    final EuareAccount account = user.getAccount();

    final List<PolicyVersion> policies = Lists.newArrayList( );
    if ( user.isEnabled( ) ) {
      if ( user.isAccountAdmin() ) {
        policies.add( PolicyVersions.getAdministratorPolicy() );
      } else {
        Iterables.addAll(
            policies,
            Iterables.transform(
                user.getPolicies( ),
                PolicyVersions.policyVersion( PolicyScope.User, Accounts.getUserArn( user ) ) ) );
        for ( final EuareManagedPolicy managedPolicy : user.getAttachedPolicies( ) ) {
          policies.add( PolicyVersions.policyVersion( managedPolicy, Accounts.getManagedPolicyArn( managedPolicy ) ) );
        }
        for ( final EuareGroup group : Iterables.filter( user.getGroups(), Predicates.not( Accounts.isUserGroup() ) ) ) {
          Iterables.addAll(
              policies,
              Iterables.transform(
                  group.getPolicies( ),
                  PolicyVersions.policyVersion( PolicyScope.Group, Accounts.getGroupArn( group ) ) ) );
          for ( final EuareManagedPolicy managedPolicy : group.getAttachedPolicies( ) ) {
            policies.add( PolicyVersions.policyVersion( managedPolicy, Accounts.getManagedPolicyArn( managedPolicy ) ) );
          }
        }
      }
      EuareUser admin;
      try {
        admin = account.lookupAdmin();
      } catch ( AuthException e ) {
        throw new AuthException( e );
      }
      if ( admin != null ) {
        Iterables.addAll(
            policies,
            Iterables.transform(
                admin.getPolicies(),
                PolicyVersions.policyVersion( PolicyScope.Account, user.getAccountNumber() ) ) ) ;
      }
    }

    this.name = user.getName( );
    this.path = user.getPath();
    this.userId = user.getUserId();
    this.authenticatedId = user.getUserId();
    this.canonicalId = account.getCanonicalId();
    this.token = user.getToken();
    this.accountAlias = account.getName();
    this.accountNumber = account.getAccountNumber();
    this.enabled = user.isEnabled();
    this.accountAdmin = user.isAccountAdmin();
    this.systemAdmin = user.isSystemAdmin();
    this.systemUser = user.isSystemUser();
    this.password = user.getPassword();
    this.passwordExpires = password == null ? null : MoreObjects.firstNonNull( user.getPasswordExpires( ), Long.MAX_VALUE );
    this.role = null;
    this.keys = ImmutableList.copyOf( Iterables.filter( Iterables.transform( user.getKeys( ), keyWrapper( this ) ), AccessKeys.isActive( ) )  );
    this.certificates = ImmutableList.copyOf(
        Iterables.filter( user.getCertificates( ), propertyPredicate( true, active( ) ) ) );
    this.principalPolicies = ImmutableList.copyOf( policies );
    this.ptag = null;
  }

  public UserPrincipalImpl( final EuareRole role, final String sessionName ) throws AuthException {
    final EuareAccount account = role.getAccount( );
    final EuareUser user = account.lookupAdmin();
    final List<PolicyVersion> policies = Lists.newArrayList( );
    Iterables.addAll(
        policies,
        Iterables.transform(
            role.getPolicies( ),
            PolicyVersions.policyVersion( PolicyScope.Role, Accounts.getRoleArn( role ) ) ) );
    for ( final EuareManagedPolicy managedPolicy : role.getAttachedPolicies( ) ) {
      policies.add( PolicyVersions.policyVersion( managedPolicy, Accounts.getManagedPolicyArn( managedPolicy ) ) );
    }
    Iterables.addAll(
        policies,
        Iterables.transform(
            user.getPolicies(),
            PolicyVersions.policyVersion( PolicyScope.Account, user.getAccountNumber( ) ) ) );

    this.name = user.getName( );
    this.path = user.getPath();
    this.userId = user.getUserId();
    this.authenticatedId = role.getRoleId() + ( sessionName == null ? "" : ":" + sessionName );
    this.canonicalId = account.getCanonicalId();
    this.token = null;
    this.accountAlias = account.getName( );
    this.accountNumber = account.getAccountNumber();
    this.enabled = true;
    this.accountAdmin = false;
    this.systemAdmin = false;
    this.systemUser = user.isSystemUser( );
    this.password = null;
    this.passwordExpires = null;
    this.role = immutableRole( role );
    this.keys = ImmutableList.copyOf( Collections.<AccessKey>emptyIterator( ) );
    this.certificates = ImmutableList.copyOf( Collections.<Certificate>emptyIterator() );
    this.principalPolicies = ImmutableList.copyOf( policies );
    this.ptag = null;
  }

  public UserPrincipalImpl(
      final UserPrincipal principal,
      final Iterable<AccessKey> keys
  ) throws AuthException {
    this.name = principal.getName( );
    this.path = principal.getPath();
    this.userId = principal.getUserId();
    this.authenticatedId = principal.getAuthenticatedId();
    this.canonicalId = principal.getCanonicalId();
    this.token = principal.getToken();
    this.accountAlias = principal.getAccountAlias();
    this.accountNumber = principal.getAccountNumber();
    this.enabled = principal.isEnabled();
    this.accountAdmin = principal.isAccountAdmin( );
    this.systemAdmin = principal.isSystemAdmin( );
    this.systemUser = principal.isSystemUser( );
    this.password = null;
    this.passwordExpires = null;
    this.role = principal instanceof HasRole ? ((HasRole) principal).getRole( ) : null;
    this.keys = ImmutableList.copyOf( keys );
    this.certificates = ImmutableList.copyOf( principal.getCertificates() );
    this.principalPolicies = ImmutableList.copyOf( principal.getPrincipalPolicies() );
    this.ptag = null;
  }

  @Nonnull
  public String getName( ) {
    return name;
  }

  @Nonnull
  public String getPath( ) {
    return path;
  }

  @Nonnull
  public String getUserId( ) {
    return userId;
  }

  @Nonnull
  public String getAuthenticatedId( ) {
    return authenticatedId;
  }

  @Nullable
  public String getToken( ) {
    return token;
  }

  @Nonnull
  public String getAccountAlias( ) {
    return accountAlias;
  }

  @Nonnull
  public String getAccountNumber( ) {
    return accountNumber;
  }

  @Nonnull
  public String getCanonicalId( ) {
    return canonicalId;
  }

  public boolean isEnabled( ) {
    return enabled;
  }

  @Override
  public boolean isAccountAdmin() {
    return accountAdmin;
  }

  @Override
  public boolean isSystemAdmin( ) {
    return systemAdmin;
  }

  @Override
  public boolean isSystemUser( ) {
    return systemUser;
  }

  @Override
  @Nullable
  public String getPassword( ) {
    return password;
  }

  @Override
  @Nullable
  public Long getPasswordExpires( ) {
    return passwordExpires;
  }

  @Nullable
  public Role getRole( ) {
    return role;
  }

  @Nonnull
  public ImmutableList<AccessKey> getKeys( ) {
    return keys;
  }

  @Nonnull
  public ImmutableList<Certificate> getCertificates( ) {
    return certificates;
  }

  @Nonnull
  public ImmutableList<PolicyVersion> getPrincipalPolicies( ) {
    return principalPolicies;
  }

  @Nullable
  public String getPTag( ) {
    return ptag;
  }

  /**
   * Calculate the tag for the given principal.
   */
  public static String ptag( final UserPrincipal userPrincipal ) {
    final List<CharSequence> sequences = Lists.newArrayList( );
    sequences.add( userPrincipal.getAccountAlias( ) );
    sequences.add( userPrincipal.getAccountNumber( ) );
    sequences.add( userPrincipal.getAuthenticatedId( ) );
    sequences.add( userPrincipal.getCanonicalId( ) );
    sequences.add( userPrincipal.getName( ) );
    sequences.add( String.valueOf( userPrincipal.getToken( ) ) );
    sequences.add( String.valueOf( userPrincipal.getPassword( ) ) );
    sequences.add( String.valueOf( userPrincipal.getPasswordExpires( ) ) );
    sequences.add( userPrincipal.getPath( ) );
    sequences.add( userPrincipal.getUserId( ) );
    sequences.add( String.valueOf( userPrincipal.isAccountAdmin( ) ) );
    sequences.add( String.valueOf( userPrincipal.isEnabled( ) ) );
    sequences.add( String.valueOf( userPrincipal.isSystemAdmin( ) ) );
    sequences.add( String.valueOf( userPrincipal.isSystemUser( ) ) );
    for ( final AccessKey key : userPrincipal.getKeys( ) ) {
      sequences.add( String.valueOf( key.getAccessKey( ) ) );
      sequences.add( String.valueOf( key.isActive( ) ) );
    }
    for ( final Certificate certificate : userPrincipal.getCertificates( ) ) {
      sequences.add( certificate.getCertificateId( ) );
      sequences.add( String.valueOf( certificate.isActive( ) ) );
    }
    for ( final PolicyVersion policyVersion : userPrincipal.getPrincipalPolicies( ) ) {
      sequences.add( policyVersion.getPolicyVersionId( ) );
      sequences.add( policyVersion.getPolicyHash( ) );
    }
    return BaseEncoding.base64( ).encode( Digest.SHA256.digestBinary( StandardCharsets.UTF_8.encode( CharBuffer.wrap( Strings.concat(
        sequences
    ) ) ) ) );
  }

  private static Role immutableRole( final BaseRole role ) throws AuthException {
    final String accountNumber = role.getAccountNumber( );
    final String roleArn = role.getRoleArn( );
    final String roleId = role.getRoleId( );
    final String path = role.getPath( );
    final String name = role.getName( );
    final String secret = role.getSecret( );
    final String displayName = role.getDisplayName( );
    final OwnerFullName owner = role.getOwner( );
    final PolicyVersion policyVersion = role.getPolicy( );
    return new Role( ) {
      @Override public String getAccountNumber( ) { return accountNumber; }
      @Override public String getRoleArn( ) { return roleArn; }
      @Override public String getRoleId( ) { return roleId; }
      @Override public String getPath( ) { return path; }
      @Override public String getName( ) { return name; }
      @Override public String getSecret( ) { return secret; }
      @Override public String getDisplayName( ) { return displayName; }
      @Override public OwnerFullName getOwner( ) { return owner; }
      @Override public PolicyVersion getPolicy( ) { return policyVersion; }
    };
  }

  private static NonNullFunction<AccessKey,AccessKey> keyWrapper( final UserPrincipal userPrincipal ) {
    return new NonNullFunction<AccessKey, AccessKey>() {
      @Nonnull
      @Override
      public AccessKey apply( final AccessKey accessKey ) {
        return new AccessKey( ){
          @Override public Boolean isActive( ) { return accessKey.isActive( ); }
          @Override public String getAccessKey() { return accessKey.getAccessKey( ); }
          @Override public String getSecretKey( ) { return accessKey.getSecretKey( ); }
          @Override public Date getCreateDate( ) { return accessKey.getCreateDate( ); }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipal; }
        };
      }
    };
  }

  private static NonNullFunction<AccessKeyEntity,AccessKey> ekeyWrapper( final UserPrincipal userPrincipal ) {
    return new NonNullFunction<AccessKeyEntity, AccessKey>() {
      @Nonnull
      @Override
      public AccessKey apply( final AccessKeyEntity accessKey ) {
        return new AccessKey( ){
          @Override public Boolean isActive( ) { return accessKey.isActive( ); }
          @Override public String getAccessKey() { return accessKey.getAccessKey( ); }
          @Override public String getSecretKey( ) { return accessKey.getSecretKey( ); }
          @Override public Date getCreateDate( ) { return accessKey.getCreateDate( ); }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipal; }
        };
      }
    };
  }

  private static NonNullFunction<CertificateEntity,Certificate> ecertWrapper( final UserPrincipal userPrincipal ) {
    return new NonNullFunction<CertificateEntity, Certificate>( ) {
      @Nonnull
      @Override
      public Certificate apply( final CertificateEntity accessKey ) {
        return new Certificate( ){
          @Override public String getCertificateId( ) { return accessKey.getCertificateId( ); }
          @Override public Boolean isActive( ) { return accessKey.isActive(); }
          @Override public String getPem( ) { return accessKey.getPem( ); }
          @Override public X509Certificate getX509Certificate( ) { return X509CertHelper.toCertificate( getPem() ); }
          @Override public Date getCreateDate( ) { return accessKey.getCreateDate( ); }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipal; }
        };
      }
    };
  }

  private enum PolicyTransform implements Function<PolicyEntity,Policy> {
    INSTANCE {
      @Override
      public Policy apply( final PolicyEntity policyEntity ) {
        return new Policy( ) {
          @Override
          public String getName() {
            return policyEntity.getName( );
          }

          @Override
          public String getText() {
            return policyEntity.getText( );
          }

          @Override
          public Integer getPolicyVersion() {
            return policyEntity.getVersion( );
          }
        };
      }
    }
  }
}
