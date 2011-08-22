package com.eucalyptus.configurable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;

@Target( { ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface ConfigurableField {
  String description( ) default "None available.";
  
  String initial( ) default "";
  
  boolean readonly( ) default true;
  
  String displayName( ) default "None";
  
  ConfigurableFieldType type( ) default ConfigurableFieldType.KEYVALUE;
  
  Class<? extends PropertyChangeListener> changeListener( ) default NoopEventListener.class;
  
}
