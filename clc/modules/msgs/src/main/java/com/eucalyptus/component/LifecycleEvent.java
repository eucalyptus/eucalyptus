package com.eucalyptus.component;

import java.util.Date;
import java.util.List;
import com.eucalyptus.event.ReferentialEvent;

public interface LifecycleEvent extends ReferentialEvent<ServiceConfiguration> {
  public enum Type {
    START, ENABLE, DISABLE, STOP, ERROR, STATE, RESTART
  }
  
  public ServiceConfiguration getReference( );
  
  public Type getLifecycleEventType( );
  
  public interface Check extends LifecycleEvent {
    public abstract List<ServiceCheckRecord> getDetails( );
    
    public abstract String getUuid( );
    
    public abstract Date getTimestamp( );
    
  }
}
