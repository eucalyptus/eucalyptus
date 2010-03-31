package com.eucalyptus.configurable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurableField {
  String description() default "None available.";
  String initial() default "";
  boolean readonly() default true;
  String displayName() default "None";
  ConfigurableFieldType type() default ConfigurableFieldType.KEYVALUE;
}
