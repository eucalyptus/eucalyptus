package com.eucalyptus.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscoverableServiceBuilder {
  com.eucalyptus.bootstrap.Component[] value() default {com.eucalyptus.bootstrap.Component.any};
}
