package com.eucalyptus.component;

import java.net.URI;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface Dispatcher {
  
  public abstract void dispatch( BaseMessage msg );
  
  public abstract <R extends BaseMessage> R send( BaseMessage msg ) throws EucalyptusCloudException;
  
  public abstract String getName( );
  
  public abstract URI getAddress( );
  
  public abstract boolean isLocal( );
  
  public abstract String toString( );
  
}
