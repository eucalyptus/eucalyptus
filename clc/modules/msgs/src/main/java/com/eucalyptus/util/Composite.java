package com.eucalyptus.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that this annotated class is backed by data from the specified list of classes.
 * @author decker
 */
@Target({ ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Composite {
  /**
   * Map the intersection of the fields of the specified classes onto this object.
   * The ordering of the Class[] entries determines the precedence of getting the original value.
   * All value writes propagate to each class type.
   * @return
   */
  Class[] value();
}
