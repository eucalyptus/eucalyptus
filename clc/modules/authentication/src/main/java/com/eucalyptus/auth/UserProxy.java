package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities._anon;
import com.google.common.base.Function;

public class UserProxy implements User {
  
  public static Function<UserEntity,UserProxy> proxyFunction = new Function<UserEntity,UserProxy>() {
    public UserProxy apply( UserEntity arg0 ) {
      return new UserProxy( arg0 );
    }    
  };
  
  private static Logger    LOG = Logger.getLogger( UserProxy.class );
  private final UserEntity searchUser;
  private UserEntity       user;
  
  public UserProxy( UserEntity user ) {
    this.searchUser = new UserEntity( user.getName( ) );
    this.user = user;
  }
  
  class _this extends _anon<UserEntity> {
    private _this( ) {
      super( UserProxy.this.searchUser );
    }
  }
  @Override
  public void setQueryId( final String queryId ) {
    new _this( ) {{
      new _mutator( ) { public void set( UserEntity e ) {
          e.setQueryId( queryId );
        }};
    }};
  }
  @Override
  public void setSecretKey( final String secretKey ) {
    new _this( ) {{
      new _mutator( ) { public void set( UserEntity e ) {
          e.setSecretKey( secretKey );
        }};
    }};
  }
  @Override
  public void revokeSecretKey( ) {
    new _this( ) {{
      new _mutator( ) { public void set( UserEntity e ) {
          e.revokeSecretKey( );
        }};
    }};
  }
  @Override
  public void revokeX509Certificate( ) {
    new _this( ) {{
      new _mutator( ) { public void set( UserEntity e ) {
          e.revokeX509Certificate( );
        }};
    }};
  }

  /**
   * @see com.eucalyptus.auth.principal.User#setIsAdministrator(java.lang.Boolean)
   * @param admin
   */
  @Override
  public void setAdministrator( final Boolean admin ) {
    new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.setAdministrator( admin );
          }};
    }};
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setIsEnabled(java.lang.Boolean)
   * @param enabled
   */
  @Override
  public void setEnabled( final Boolean enabled ) {
    new _this( ) {{ 
      new _mutator( ) { public void set( UserEntity e ) {
            e.setEnabled( enabled );
      }};
    }};
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setX509Certificate(java.security.cert.X509Certificate)
   * @param cert
   */
  @Override
  public void setX509Certificate( final X509Certificate cert ) {
    new _this( ) {{ 
      new _mutator( ) { public void set( UserEntity e ) {
            e.setX509Certificate( cert );
      }};
    }};
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
   * @see com.eucalyptus.auth.principal.User#getIsEnabled()
   * @return
   */
  @Override
  public Boolean isEnabled( ) {
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
  
  @Override
  public BigInteger getNumber( ) {
    return this.user.getNumber( );
  }

    
}
