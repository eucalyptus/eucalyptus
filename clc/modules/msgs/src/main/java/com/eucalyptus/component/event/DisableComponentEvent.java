package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;

public class DisableComponentEvent extends LifecycleEvent {

  /**
   * @param configuration
   */
  DisableComponentEvent( ServiceConfiguration configuration ) {
    super( configuration );
  }
}
