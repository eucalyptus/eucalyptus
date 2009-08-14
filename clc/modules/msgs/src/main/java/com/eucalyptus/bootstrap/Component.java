package com.eucalyptus.bootstrap;

public class Component {
  public enum Name {
    eucalyptus, walrus, dns, storage, database, jetty
  }
  private Name name;
  private ResourceProvider resourceProvider;
  private Component( Name name, ResourceProvider resourceProvider ) {
    super( );
    this.name = name;
    this.resourceProvider = resourceProvider;
  }
  public Name getName( ) {
    return name;
  }
  public ResourceProvider getResourceProvider( ) {
    return resourceProvider;
  }
  
}
