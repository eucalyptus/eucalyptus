package com.eucalyptus.component;

import java.util.List;

public interface ServiceConfigurationProvider<T extends ServiceConfiguration> {
  public abstract List<T> list( T type ) throws ServiceRegistrationException;
  
  public abstract T store( T t ) throws ServiceRegistrationException;
  
  public abstract T remove( T t ) throws ServiceRegistrationException;

  public abstract T lookup( T type ) throws ServiceRegistrationException;
}