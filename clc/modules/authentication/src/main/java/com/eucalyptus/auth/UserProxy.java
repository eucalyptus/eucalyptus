package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities._anon;
import com.eucalyptus.util.EucalyptusCloudException;
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
    try {
      new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.setQueryId( queryId );
          }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }
  @Override
  public void setSecretKey( final String secretKey ) {
    try {
      new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.setSecretKey( secretKey );
          }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }
  @Override
  public void revokeSecretKey( ) {
    try {
      new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.revokeSecretKey( );
        }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }
  @Override
  public void revokeX509Certificate( ) {
    try {
      new _this( ) {{
        new _mutator( ) { public void set( UserEntity e ) {
            e.revokeX509Certificate( );
        }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }

  /**
   * @see com.eucalyptus.auth.principal.User#setAdministrator(java.lang.Boolean)
   * @param admin
   */
  @Override
  public void setAdministrator( final Boolean admin ) {
    try {
      new _this( ) {{
          new _mutator( ) { public void set( UserEntity e ) {
              e.setAdministrator( admin );
          }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setEnabled(java.lang.Boolean)
   * @param enabled
   */
  @Override
  public void setEnabled( final Boolean enabled ) {
    try {
      new _this( ) {{ 
        new _mutator( ) { public void set( UserEntity e ) {
              e.setEnabled( enabled );
        }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
    }
  }
  
  /**
   * @see com.eucalyptus.auth.principal.User#setX509Certificate(java.security.cert.X509Certificate)
   * @param cert
   */
  @Override
  public void setX509Certificate( final X509Certificate cert ) {
    try {
      new _this( ) {{ 
        new _mutator( ) { public void set( UserEntity e ) {
              e.setX509Certificate( cert );
        }}.set();
      }};
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
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
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return this.user.getAllX509Certificates( );
  }

    
}
