package com.eucalyptus.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates accounts to a component such that the accounts are visibly
 * mapped to component in output provided to users.
 *
 * Use annotation by providing a list of account aliases that the component
 * uses/relies upon.
 * Example: @ComponentAccounts("serviceaccount1","serviceaccount2")
 *
 * Typically used with values from com.eucalyptus.auth.principal.AccountIdentifiers
 * e.g. @ComponentAccounts(AccountIdentifiers.BLOCK_STORAGE_SYSTEM_ACCOUNT)
 *
 * Created by zhill on 8/4/15.
 */
@Target( { ElementType.TYPE,
           ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface PublicComponentAccounts {
  String[] value() default {};
}
