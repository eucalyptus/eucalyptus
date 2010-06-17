package com.eucalyptus.configurable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurableClass {
  String root();
  String alias() default "";
  String description();
  boolean singleton() default true;
  boolean deferred() default false;
}
