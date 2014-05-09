package com.eucalyptus.objectstorage.policy;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the set of ACL permissions required to execute the request.
 * Setting ownerOnly declares the operation to be only executable by the resource owner account.
 * ownerOnly is typically used for S3 operations that don't have a corresponding ACL
 * operation. IAM evaluation for users within the owning account must still be
 * performed
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresACLPermission {
    /**
     * The set of ACL permissions, from {@link ObjectStorageProperties.Permission} that
     */
    ObjectStorageProperties.Permission[] bucket();

    ObjectStorageProperties.Permission[] object();

    boolean ownerOnly() default false;
}
