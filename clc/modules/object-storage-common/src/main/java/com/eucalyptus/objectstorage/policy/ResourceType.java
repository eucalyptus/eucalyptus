package com.eucalyptus.objectstorage.policy;

import com.eucalyptus.auth.policy.PolicySpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the resource type that the message evaluates to for IAM evaluation
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceType {
    /**
     * The resource type associated with the operation, from {@link PolicySpec}. e.g. S3_RESOURCE_OBJECT, S3_RESOURCE_BUCKET
     */
    String value();

}
