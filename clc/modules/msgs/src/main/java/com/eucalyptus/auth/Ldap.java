package com.eucalyptus.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate the mapping between LDAP attributes and
 * User/Group entity fields.
 * @author wenye
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Ldap {
  String[] names();
  EucaLdapMapping converter() default EucaLdapMapping.DEFAULT; 
}