package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;

public class EnableComponentEvent extends LifecycleEvent {

  /**
   * @param configuration
   */
  EnableComponentEvent( ServiceConfiguration configuration ) {
    super( configuration );
  }
}
