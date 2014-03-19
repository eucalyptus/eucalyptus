package com.eucalyptus.objectstorage.policy;

import com.eucalyptus.auth.policy.PolicySpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the set of IAM actions that are required to perform the S3 operation
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /**
     * The set of IAM policies, from {@link PolicySpec} that
     */
    String[] value();

}
