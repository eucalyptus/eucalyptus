package com.eucalyptus.component;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import com.eucalyptus.bootstrap.CanBootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;

public interface ServiceConfiguration extends Serializable, HasFullName<ServiceConfiguration>, HasStateMachine<ServiceConfiguration, Component.State, Component.Transition> {
  
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

  public abstract Boolean isVmLocal( );
  
  public abstract Partition lookupPartition( );
  
  public abstract CanBootstrap lookupBootstrapper( );

  public abstract StateMachine<ServiceConfiguration, Component.State, Component.Transition> lookupStateMachine( );
  
  public abstract State lookupState( );

  public abstract ComponentId getComponentId( );
  
  public abstract Boolean isHostLocal( );

  public abstract InetAddress getInetAddress( );

}
