package com.eucalyptus.auth.policy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( { ElementType.TYPE } )
@Retention( RetentionPolicy.RUNTIME )
public @interface PolicyResourceType {
  String value( );
}
