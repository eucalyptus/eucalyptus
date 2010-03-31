package com.eucalyptus.auth;

import com.eucalyptus.auth.api.BaseSecurityException;

public class GroupExistsException extends BaseSecurityException {

  public GroupExistsException( ) {
    super( );
  }

  public GroupExistsException( String message, Throwable cause ) {
    super( message, cause );
  }

  public GroupExistsException( String message ) {
    super( message );
  }

  public GroupExistsException( Throwable cause ) {
    super( cause );
  }

}
