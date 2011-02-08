package com.eucalyptus.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NOTE: this annotation can only be used with Component values that are
 * {@link ComponentId#isCloudLocal()}
 */
@Target( { ElementType.TYPE, ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface DependsRemote {
  Class[] value();
}
