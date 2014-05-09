package com.eucalyptus.objectstorage.policy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the operation can be executed only in a 'Administrator' context
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOverrideAllowed {
    //If set, indicates that the operation is allowed for Admin-ONLY. The Eucalyptus administrator context.
    boolean adminOnly() default false;
}
