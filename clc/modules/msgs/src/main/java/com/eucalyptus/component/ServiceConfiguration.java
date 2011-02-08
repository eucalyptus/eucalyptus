package com.eucalyptus.component;

import java.io.Serializable;

public interface ServiceConfiguration extends Serializable {
  public abstract String getId( );
  
  public abstract String getName( );
  
  public abstract void setName( String name );
  
  public abstract String getPartition( );

  public abstract void setPartition( String partition );

  public abstract String getHostName( );
  
  public abstract void setHostName( String hostName );
  
  public abstract Integer getPort( );
  
  public abstract void setPort( Integer port );
  
  public abstract String getServicePath( );
  
  public abstract void setServicePath( String servicePath );
  
  public abstract ComponentId getComponentId( );
  
  public abstract String getUri( );
  
  public abstract Boolean isLocal( );
  
}
