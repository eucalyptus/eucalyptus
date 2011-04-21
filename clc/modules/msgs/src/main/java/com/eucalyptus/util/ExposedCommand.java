package com.eucalyptus.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Must be present on all methods executed by <code>CommandServlet</code>
 * 
 * @see CommandServlet
 * @author tom.werges
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposedCommand
{
}
