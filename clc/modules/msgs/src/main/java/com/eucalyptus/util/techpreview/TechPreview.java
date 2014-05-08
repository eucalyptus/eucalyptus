/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util.techpreview;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares this code to be tech preview. The only appropriate use for this is to use it for
 * excluding implementations with this annotation from default system behaviours.
 * 
 * For example, a {@link com.eucalyptus.component.ComponentId} annotated with this should not be
 * included in default behaviours, like service groups.
 */
@Target( { ElementType.TYPE,
          ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface TechPreview {
  /**
   * Name of the system property to be used when enable the thing marked as TP for the annotated element.
   *
   * The property value must be "true" to enable.
   */
  String enableByDefaultProperty( ) default "enable.tech.preview";
}
