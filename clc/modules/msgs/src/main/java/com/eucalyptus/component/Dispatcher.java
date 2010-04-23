package com.eucalyptus.component;

import java.net.URI;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface Dispatcher {
  
  public abstract void dispatch( BaseMessage msg );
  
  public abstract BaseMessage send( BaseMessage msg ) throws EucalyptusCloudException;
  
  @SuppressWarnings( "unchecked" )
  public abstract <REPLY> REPLY send( BaseMessage message, Class<REPLY> replyType ) throws EucalyptusCloudException;
  
  public abstract Component getComponent( );
  
  public abstract String getName( );
  
  public abstract URI getAddress( );
  
  public abstract boolean isLocal( );
  
  public abstract String toString( );
  
}