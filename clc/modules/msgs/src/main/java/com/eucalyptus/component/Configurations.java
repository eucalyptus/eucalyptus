package com.eucalyptus.component;

import java.util.NoSuchElementException;
import com.eucalyptus.bootstrap.Component;

public class Configurations {
  public static Configuration lookup( String componentName ) throws NoSuchElementException {
    return Components.lookup( Configuration.class, componentName );
  }
  public static Configuration lookup( Component component ) throws NoSuchElementException {
    return Components.lookup( Configuration.class, component.name( ) );
  }
  public static boolean contains( String componentName ) {
    return Components.contains( Configuration.class, componentName );
  }
  public static boolean contains( Component component ) {
    return Components.contains( Configuration.class, component.name( ) );
  }
}
