package com.eucalyptus.component;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.WebServicesException;

public class NoSuchComponentException extends WebServicesException {
  
  public NoSuchComponentException( ) {//TODO:GRZE: go back here and review error reporting 
    super( );
  }
  
  public NoSuchComponentException( String message, Throwable ex ) {
    super( message, ex );
  }
  
  public NoSuchComponentException( String message ) {
    super( message );
  }
  
  public NoSuchComponentException( Throwable ex ) {
    super( ex );
  }
  
}
