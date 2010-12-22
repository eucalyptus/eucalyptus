package com.eucalyptus.upgrade;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.junit.runner.notification.Failure;


@Retention(RetentionPolicy.RUNTIME)
@interface TestDescription {
  String value() default "";
}

