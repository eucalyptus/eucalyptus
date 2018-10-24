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

package com.eucalyptus.auth.principal;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Principal.PrincipalType;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Principals {

  private static final String  SYSTEM_ID      = AccountIdentifiers.SYSTEM_ACCOUNT;
  private static final String  NOBODY_ID      = AccountIdentifiers.NOBODY_ACCOUNT;

  private static final SystemUser SYSTEM_USER = new SystemUser( ) {
                                                private final Certificate       cert  = new Certificate( ) {
                                                                                        @Override
                                                                                        public Boolean isActive( ) {
                                                                                          return true;
                                                                                        }

                                                                                        @Override
                                                                                        public String getPem( ) {
                                                                                          return B64.url.encString( PEMFiles.getBytes( getX509Certificate( ) ) );
                                                                                        }

                                                                                        @Override
                                                                                        public X509Certificate getX509Certificate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
                                                                                        }

                                                                                        @Override
                                                                                        public Date getCreateDate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ).getNotBefore( );
                                                                                        }

                                                                                        @Override
                                                                                        public UserPrincipal getPrincipal( ) throws AuthException {
                                                                                          return Principals.systemUser( );
                                                                                        }

                                                                                        @Override
                                                                                        public String getCertificateId( ) {
                                                                                          return SYSTEM_ID;
                                                                                        }
                                                                                      };
                                                private final List<Certificate> certs = new ArrayList<Certificate>( ) {
                                                                                        {
                                                                                          add( cert );
                                                                                        }
                                                                                      };

                                                @Nonnull
                                                @Override
                                                public String getAuthenticatedId( ) {
                                                  return getUserId( );
                                                }

                                                @Nonnull
                                                @Override
                                                public String getUserId( ) {
                                                  return AccountIdentifiers.SYSTEM_ACCOUNT;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getName( ) {
                                                  return AccountIdentifiers.SYSTEM_ACCOUNT;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getPath( ) {
                                                  return "/";
                                                }

                                                @Override
                                                public boolean isEnabled( ) {
                                                  return true;
                                                }

                                                @Override
                                                public String getToken( ) {
                                                  return null;
                                                }

                                                @Override
                                                public String getPassword( ) {
                                                  return null;
                                                }

                                                @Override
                                                public Long getPasswordExpires( ) {
                                                  return null;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<AccessKey> getKeys( ) {
                                                  return null;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<Certificate> getCertificates( ) {
                                                  return certs;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getAccountNumber( ) {
                                                  return getAccount( ).getAccountNumber( );
                                                }

                                                @Nonnull
                                                @Override
                                                public String getAccountAlias( ) {
                                                  return getAccount( ).getAccountAlias();
                                                }

                                                @Nonnull
                                                @Override
                                                public String getCanonicalId( ) {
                                                  return getAccount( ).getCanonicalId();
                                                }

                                                public AccountIdentifiers getAccount( ) {
                                                  return systemAccount( );
                                                }

                                                @Override
                                                public boolean isSystemAdmin( ) {
                                                  return true;
                                                }

                                                @Override
                                                public boolean isSystemUser( ) {
                                                  return true;
                                                }

                                                @Override
                                                public boolean isAccountAdmin( ) {
                                                  return true;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<PolicyVersion> getPrincipalPolicies( ) {
                                                  return Lists.newArrayList( );
                                                }

                                                @Nullable
                                                @Override
                                                public String getPTag() {
                                                  return null;
                                                }
                                              };

  private static final SystemUser NOBODY_USER = new SystemUser( ) {
                                                private final Certificate       cert  = new Certificate( ) {

                                                                                        @Override
                                                                                        public Boolean isActive( ) {
                                                                                          return true;
                                                                                        }

                                                                                        @Override
                                                                                        public String getPem( ) {
                                                                                          return B64.url.encString( PEMFiles.getBytes( getX509Certificate( ) ) );
                                                                                        }

                                                                                        @Override
                                                                                        public X509Certificate getX509Certificate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
                                                                                        }

                                                                                        @Override
                                                                                        public Date getCreateDate( ) {
                                                                                          return null;
                                                                                        }

                                                                                        @Override
                                                                                        public UserPrincipal getPrincipal( ) throws AuthException {
                                                                                          return Principals.nobodyUser( );
                                                                                        }

                                                                                        @Override
                                                                                        public String getCertificateId( ) {
                                                                                          return Principals.NOBODY_ID;
                                                                                        }
                                                                                      };
                                                private final List<Certificate> certs = new ArrayList<Certificate>( ) {
                                                                                        {
                                                                                          add( cert );
                                                                                        }
                                                                                      };

                                                @Nonnull
                                                @Override
                                                public String getAuthenticatedId( ) {
                                                  return getUserId( );
                                                }

                                                @Nonnull
                                                @Override
                                                public String getUserId( ) {
                                                  return AccountIdentifiers.NOBODY_ACCOUNT;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getName( ) {
                                                  return AccountIdentifiers.NOBODY_ACCOUNT;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getPath( ) {
                                                  return "/";
                                                }

                                                @Override
                                                public boolean isEnabled( ) {
                                                  return true;
                                                }

                                                @Override
                                                public String getToken( ) {
                                                  return null;
                                                }

                                                @Override
                                                public String getPassword( ) {
                                                  return null;
                                                }

                                                @Override
                                                public Long getPasswordExpires( ) {
                                                  return null;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<AccessKey> getKeys( ) {
                                                  return null;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<Certificate> getCertificates( ) {
                                                  return certs;
                                                }

                                                @Nonnull
                                                @Override
                                                public String getAccountNumber() {
                                                  return getAccount().getAccountNumber();
                                                }

                                                @Nonnull
                                                @Override
                                                public String getAccountAlias( ) {
                                                  return getAccount( ).getAccountAlias();
                                                }

                                                @Nonnull
                                                @Override
                                                public String getCanonicalId( ) {
                                                  return getAccount( ).getCanonicalId( );
                                                }

                                                public AccountIdentifiers getAccount( ) {
                                                  return NOBODY_ACCOUNT;
                                                }

                                                @Override
                                                public boolean isSystemAdmin( ) {
                                                  return false;
                                                }

                                                @Override
                                                public boolean isSystemUser( ) {
                                                  return false;
                                                }

                                                @Override
                                                public boolean isAccountAdmin( ) {
                                                  return false;
                                                }

                                                @Nonnull
                                                @Override
                                                public List<PolicyVersion> getPrincipalPolicies( ) {
                                                  return Lists.newArrayList( );
                                                }

                                                @Nullable
                                                @Override
                                                public String getPTag() {
                                                  return null;
                                                }
                                              };

  private static final AccountIdentifiers NOBODY_ACCOUNT =
      new SystemAccount(
          AccountIdentifiers.NOBODY_ACCOUNT_ID,
          AccountIdentifiers.NOBODY_ACCOUNT,
          AccountIdentifiers.NOBODY_CANONICAL_ID
          );
  private static final AccountIdentifiers SYSTEM_ACCOUNT =
      new SystemAccount(
          AccountIdentifiers.SYSTEM_ACCOUNT_ID,
          AccountIdentifiers.SYSTEM_ACCOUNT,
          AccountIdentifiers.SYSTEM_CANONICAL_ID
          );

  private static final Set<AccountIdentifiers> FAKE_ACCOUNTS        = ImmutableSet.of( systemAccount(), nobodyAccount() );
  private static final Set<String>  FAKE_ACCOUNT_NUMBERS =
      ImmutableSet.copyOf( Iterables.transform(FAKE_ACCOUNTS, AccountIdentifiers.Properties.accountNumber( ) ) );
  private static final Set<String>  FAKE_CANONICAL_IDS =
      ImmutableSet.copyOf( Iterables.transform(FAKE_ACCOUNTS, AccountIdentifiers.Properties.accountCanonicalId( ) ) );
  private static final Set<SystemUser> FAKE_USERS         = ImmutableSet.of( systemUser(), nobodyUser() );
  private static final Set<String>  FAKE_USER_IDS        =
      ImmutableSet.copyOf( Iterables.transform(FAKE_USERS, Accounts.toUserId() ) );

  public static SystemUser systemUser( ) {
    return SYSTEM_USER;
  }

  public static SystemUser nobodyUser( ) {
    return NOBODY_USER;
  }

  public static AccountIdentifiers nobodyAccount( ) {
    return NOBODY_ACCOUNT;
  }

  public static AccountIdentifiers systemAccount( ) {
    return SYSTEM_ACCOUNT;
  }

  private static final UserFullName SYSTEM_USER_ERN = UserFullName.getInstance( systemUser( ) );
  private static final UserFullName NOBODY_USER_ERN = UserFullName.getInstance( nobodyUser( ) );

  public static OwnerFullName nobodyFullName( ) {
    return NOBODY_USER_ERN;
  }

  public static OwnerFullName systemFullName( ) {
    return SYSTEM_USER_ERN;
  }

  public static boolean isFakeIdentityAccountNumber( final String id ) {
    return FAKE_ACCOUNT_NUMBERS.contains( id );
  }

  public static boolean isFakeIdentityCanonicalId( final String id ) {
    return FAKE_CANONICAL_IDS.contains( id );
  }

  public static boolean isFakeIdentityUserId( final String id ) {
    return FAKE_USER_IDS.contains( id );
  }

  public static boolean isFakeIdentify( String id ) {
    return
        isFakeIdentityAccountNumber( id ) ||
        isFakeIdentityCanonicalId( id ) ||
        isFakeIdentityUserId( id );
  }

  /**
   * Do the given User objects represent the same user.
   *
   * @param user1 The first user to compare
   * @param user2 The second user to compare
   * @return True if the given Users represent the same user.
   */
  public static boolean isSameUser( final User user1,
                                    final User user2 ) {
    return user1 != null && user2 != null &&
        !Strings.isNullOrEmpty( user1.getUserId() ) && !Strings.isNullOrEmpty( user2.getUserId() ) &&
        user1.getUserId().equals( user2.getUserId() );
  }

  /**
   * Get an immutable Set of TypedPrincipals for the given principal.
   *
   * @param userPrincipal The principal
   * @return The set of principals
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static Set<TypedPrincipal> typedSet( @Nonnull final UserPrincipal userPrincipal ) throws AuthException {
    if ( userPrincipal instanceof HasRole && ((HasRole) userPrincipal).getRole( ) != null ) {
      final Role role = ((HasRole) userPrincipal).getRole( );
      final RoleSecurityTokenAttributes attributes =
          RoleSecurityTokenAttributes.forUser( userPrincipal )
              .or( RoleSecurityTokenAttributes.basic( "eucalyptus" ) );
      return ImmutableSet.of(
          TypedPrincipal.of( PrincipalType.AWS, Accounts.getAssumedRoleArn( role, attributes.getSessionName( ) ) ),
          TypedPrincipal.of( PrincipalType.AWS, Accounts.getRoleArn( role ) ),
          TypedPrincipal.of( PrincipalType.AWS, Accounts.getAccountArn( userPrincipal.getAccountNumber( ) ) )
      );
    } else {
      return ImmutableSet.of(
          TypedPrincipal.of( PrincipalType.AWS, Accounts.getUserArn( userPrincipal ) ),
          TypedPrincipal.of( PrincipalType.AWS, Accounts.getAccountArn( userPrincipal.getAccountNumber( ) ) )
      );
    }
  }

  public interface SystemUser extends UserPrincipal {
  }

  private static class SystemAccount implements AccountIdentifiers {
    private final Long accountId;
    private final String accountAlias;
    private final String canonicalId;

    private SystemAccount( final Long accountId,
                           final String accountAlias,
                           final String canonicalId) {
      this.accountId = accountId;
      this.accountAlias = accountAlias;
      this.canonicalId = canonicalId;
    }

    @Override
    public String getAccountNumber( ) {
      return String.format( "%012d", accountId );
    }

    @Override
    public String getAccountAlias( ) {
      return accountAlias;
    }

    @Override
    public String getCanonicalId() {
          return canonicalId;
    }

  }
}
