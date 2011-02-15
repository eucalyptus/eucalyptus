package com.eucalyptus.upgrade;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestDescription {
  String value() default "";
}

