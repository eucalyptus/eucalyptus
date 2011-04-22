package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.ReferentialEvent;

public interface LifecycleEvent extends ReferentialEvent<ServiceConfiguration> {
  public ServiceConfiguration getReference( );
}
