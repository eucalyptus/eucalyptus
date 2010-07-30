package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.base.Function;

public class DatabaseWrappedUser implements User, WrappedUser {
  
  public static Function<UserEntity, DatabaseWrappedUser> proxyFunction = new Function<UserEntity, DatabaseWrappedUser>( ) {
                                                                          public DatabaseWrappedUser apply( UserEntity arg0 ) {
                                                                            return new DatabaseWrappedUser( arg0 );
                                                                          }
                                                                        };
  
  private static Logger                                   LOG           = Logger.getLogger( DatabaseWrappedUser.class );
  private final UserEntity                                searchUser;
  private UserEntity                                      user;
  
  public DatabaseWrappedUser( UserEntity user ) {
    this.searchUser = new UserEntity( user.getName( ) );
    this.user = user;
  }
  
  @Override
  public void setQueryId( final String queryId ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setQueryId( queryId );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  @Override
  public void setSecretKey( final String secretKey ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setSecretKey( secretKey );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  @Override
  public void revokeSecretKey( ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.revokeSecretKey( );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  @Override
  public void revokeX509Certificate( ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.revokeX509Certificate( );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setAdministrator(java.lang.Boolean)
   * @param admin
   */
  @Override
  public void setAdministrator( final Boolean admin ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setAdministrator( admin );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setEnabled(java.lang.Boolean)
   * @param enabled
   */
  @Override
  public void setEnabled( final Boolean enabled ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setEnabled( enabled );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setX509Certificate(java.security.cert.X509Certificate)
   * @param cert
   */
  @Override
  public void setX509Certificate( final X509Certificate cert ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setX509Certificate( cert );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getIsAdministrator()
   * @return
   */
  @Override
  public Boolean isAdministrator( ) {
    return this.user.isAdministrator( );
  }
  
  /**
   * Just to make CompositeHelper.goovy happy.
   * 
   * @return
   */
  public Boolean getAdministrator( ) {
    return this.user.isAdministrator( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getIsEnabled()
   * @return
   */
  @Override
  public Boolean isEnabled( ) {
    return this.user.isEnabled( );
  }
  
  /**
   * Just to make CompositeHelper.goovy happy.
   * 
   * @return
   */
  public Boolean getEnabled( ) {
    return this.user.isEnabled( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getName()
   * @return
   */
  @Override
  public String getName( ) {
    return this.user.getName( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getQueryId()
   * @return
   */
  @Override
  public String getQueryId( ) {
    return this.user.getQueryId( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getSecretKey()
   * @return
   */
  @Override
  public String getSecretKey( ) {
    return this.user.getSecretKey( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getX509Certificate()
   * @return
   */
  @Override
  public X509Certificate getX509Certificate( ) {
    return this.user.getX509Certificate( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.credential.CredentialPrincipal#getNumber()
   * @return
   */
  @Override
  public BigInteger getNumber( ) {
    return this.user.getNumber( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#getToken()
   * @return
   */
  @Override
  public String getToken( ) {
    return this.user.getToken( );
  }
  
  /**
   * @see com.eucalyptus.auth.principal.credential.X509Principal#getAllX509Certificates()
   * @return
   */
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return this.user.getAllX509Certificates( );
  }
  
  @Override
  public User getDelegate( ) {
    return this.user;
  }
  
  @Override
  public boolean checkToken( String testToken ) {
    String token = this.user.getToken( );
    boolean ret = false;
    if ( token != null ) {
      ret = token.equals( testToken );
    }
    try {
      Transactions.one( this.searchUser, new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setToken( Crypto.generateSessionToken( t.getName( ) ) );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
    return ret;
  }
  
  @Override
  public String getPassword( ) {
    return this.user.getPassword( );
  }
  
  @Override
  public void setPassword( final String password ) {
    try {
      Transactions.one( this.searchUser, new Tx<User>( ) {
        public void fire( User t ) throws Throwable {
          t.setPassword( password );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  public void setToken( final String token ) {
    try {
      Transactions.one( this.searchUser, new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setToken( token );
        }
      } );
    } catch ( TransactionException e1 ) {
      LOG.debug( e1, e1 );
    }
  }
  
  public UserInfo getUserInfo( ) throws NoSuchUserException {
    return UserInfoStore.getUserInfo( new UserInfo( this.user.getName( ) ) );
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "DatabaseWrappedUser [ " );
    sb.append( "user = " ).append( user ).append( ", " );
    sb.append( "]" );
    return sb.toString( );
  }
}
