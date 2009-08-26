package com.eucalyptus.bootstrap;

import java.util.List;

public enum Component {
  eucalyptus,
  walrus,
  dns,
  storage,
  database,
  www,
  any(true);
  private boolean local   = false;
  private boolean enabled = false;
  private boolean hasKeys = false;

  
  private Component() {}
  private Component( boolean whatever ) {
    this.local = true;
    this.enabled = true;
  }

  public void markHasKeys( ) {
    this.hasKeys = true;
  }

  public boolean isHasKeys( ) {
    return hasKeys;
  }

  public void markEnabled( ) {
    this.enabled = true;
  }

  public boolean isEnabled( ) {
    return enabled;
  }

  public boolean isLocal( ) {
    return local;
  }

  public void markLocal( ) {
    this.local = true;
  }

  public ResourceProvider getResourceProvider( ) {
    return resourceProvider;
  }

  public void setResourceProvider( ResourceProvider resourceProvider ) {
    this.resourceProvider = resourceProvider;
  }

  public List<Bootstrapper> getBootstrappers( ) {
    return bootstrappers;
  }

  private ResourceProvider   resourceProvider;
  private List<Bootstrapper> bootstrappers;

  public boolean add( Bootstrapper arg0 ) {
    return bootstrappers.add( arg0 );
  }

}
