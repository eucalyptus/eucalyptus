package com.eucalyptus.configurable;

import java.lang.reflect.Field;

public interface ConfigurablePropertyBuilder {
  public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException;
}
