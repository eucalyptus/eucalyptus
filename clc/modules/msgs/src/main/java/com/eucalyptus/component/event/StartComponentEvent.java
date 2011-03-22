package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;

public class StartComponentEvent extends LifecycleEvent {

  /**
   * @param configuration
   */
  StartComponentEvent( ServiceConfiguration configuration ) {
    super( configuration );
  }
}
