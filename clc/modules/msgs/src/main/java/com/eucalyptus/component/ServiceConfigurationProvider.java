package com.eucalyptus.component;

import java.util.List;

public interface ServiceConfigurationProvider<T extends ServiceConfiguration> {
  public abstract <T extends ServiceConfiguration> List<T> list( T type ) throws ServiceRegistrationException;
  
  public abstract <T extends ServiceConfiguration> T store( T t ) throws ServiceRegistrationException;
  
  public abstract <T extends ServiceConfiguration> T remove( T t ) throws ServiceRegistrationException;

  public abstract <T extends ServiceConfiguration> T lookup( T type ) throws ServiceRegistrationException;
}