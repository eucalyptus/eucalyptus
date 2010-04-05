package com.eucalyptus.config;

import java.util.List;

public interface ComponentConfigurationProvider<T extends ComponentConfiguration> {
  public abstract List<T> list( T type ) throws ServiceRegistrationException;
  
  public abstract T lookupByName( String name, T type ) throws ServiceRegistrationException;
  
  public abstract T lookupByHost( String host, T type ) throws ServiceRegistrationException;
  
  public abstract T lookup( String name, String host, Integer port, T type ) throws ServiceRegistrationException;
  
  public abstract T store( T t ) throws ServiceRegistrationException;
  
  public abstract T remove( T t ) throws ServiceRegistrationException;
}