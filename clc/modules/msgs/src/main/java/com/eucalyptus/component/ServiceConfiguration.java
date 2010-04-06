package com.eucalyptus.component;

import java.io.Serializable;

public interface ServiceConfiguration extends Serializable {
  public abstract String getName( );
  
  public abstract void setName( String name );
  
  public abstract String getHostName( );
  
  public abstract void setHostName( String hostName );
  
  public abstract Integer getPort( );
  
  public abstract void setPort( Integer port );
  
  public abstract String getServicePath( );
  
  public abstract void setServicePath( String servicePath );
  
  public abstract com.eucalyptus.bootstrap.Component getComponent( );
  
  public abstract String getUri( );
  
  public abstract Boolean isLocal( );
}
