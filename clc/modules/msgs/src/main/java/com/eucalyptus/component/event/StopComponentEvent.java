package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;

public class StopComponentEvent extends LifecycleEvent {

  /**
   * @param configuration
   */
  StopComponentEvent( ServiceConfiguration configuration ) {
    super( configuration );
  }}
