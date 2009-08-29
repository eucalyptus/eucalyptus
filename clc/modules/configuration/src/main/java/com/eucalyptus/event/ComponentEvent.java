package com.eucalyptus.event;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;

public class ComponentEvent extends Event {
  ComponentConfiguration configuration;
  Component component;
  boolean local;

  public ComponentEvent( ComponentConfiguration configuration, Component component, boolean local ) {
    super( );
    this.configuration = configuration;
    this.component = component;
    this.local = local;
  }

  public ComponentConfiguration getConfiguration( ) {
    return configuration;
  }

  public Component getComponent( ) {
    return component;
  }

  public boolean isLocal( ) {
    return local;
  }

  
}
