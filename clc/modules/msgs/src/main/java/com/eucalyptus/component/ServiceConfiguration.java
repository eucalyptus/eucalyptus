package com.eucalyptus.component;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceChecks.Severity;
import com.eucalyptus.util.HasFullName;

public interface ServiceConfiguration extends Serializable, HasFullName<ServiceConfiguration> {
  
  public abstract void setName( String name );
  
  public abstract void setPartition( String partition );

  public InetSocketAddress getSocketAddress( );

  public abstract String getHostName( );
  
  public abstract void setHostName( String hostName );
  
  public abstract Integer getPort( );
  
  public abstract void setPort( Integer port );
  
  public abstract String getServicePath( );
  
  public abstract void setServicePath( String servicePath );
  
  public abstract URI getUri( );
  
  public abstract Boolean isLocal( );
  
  public abstract Component lookupComponent( );
  
  public abstract ComponentId getComponentId( );
  
  public abstract Service lookupService( );

  public abstract ServiceBuilder lookupBuilder( );
  
//  public void fatal( Throwable t );
//
//  public void urgent( Throwable t );
//  
//  public void error( Throwable t );
//  
//  public void warning( Throwable t );
//  
//  public void info( Throwable t );
//  
//  public void debug( Throwable t );

}
