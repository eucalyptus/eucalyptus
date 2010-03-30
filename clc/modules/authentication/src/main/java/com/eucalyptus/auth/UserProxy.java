package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
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
   * @see com.eucalyptus.auth.User#setIsAdministrator(java.lang.Boolean)
   * @param admin
   */
  @Override
  public void setIsAdministrator( final Boolean admin ) {
    new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.setIsAdministrator( admin );
          }};
    }};
  }
  
  /**
   * @see com.eucalyptus.auth.User#setIsEnabled(java.lang.Boolean)
   * @param enabled
   */
  @Override
  public void setIsEnabled( final Boolean enabled ) {
    new _this( ) {{ 
      new _mutator( ) { public void set( UserEntity e ) {
            e.setIsEnabled( enabled );
      }};
    }};
  }
  
  /**
   * @see com.eucalyptus.auth.User#setX509Certificate(java.security.cert.X509Certificate)
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
   * @see com.eucalyptus.auth.User#getIsAdministrator()
   * @return
   */
  @Override
  public Boolean getIsAdministrator( ) {
    return this.user.getIsAdministrator( );
  }
  
  /**
   * @see com.eucalyptus.auth.User#getIsEnabled()
   * @return
   */
  @Override
  public Boolean getIsEnabled( ) {
    return this.user.getIsEnabled( );
  }
  
  /**
   * @see com.eucalyptus.auth.User#getName()
   * @return
   */
  @Override
  public String getName( ) {
    return this.user.getName( );
  }
  
  /**
   * @see com.eucalyptus.auth.User#getQueryId()
   * @return
   */
  @Override
  public String getQueryId( ) {
    return this.user.getQueryId( );
  }
  
  /**
   * @see com.eucalyptus.auth.User#getSecretKey()
   * @return
   */
  @Override
  public String getSecretKey( ) {
    return this.user.getSecretKey( );
  }
  
  /**
   * @see com.eucalyptus.auth.User#getX509Certificate()
   * @return
   */
  @Override
  public X509Certificate getX509Certificate( ) {
    return this.user.getX509Certificate( );
  }
  
  @Override
  public String getNumber( ) {
    return this.user.getNumber( );
  }

    
}
