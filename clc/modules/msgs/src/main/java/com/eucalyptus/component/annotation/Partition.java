/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.eucalyptus.component.ComponentId;

/**
 * Declares the component which controls the partition of the annotated component type. e.g.,
 * cluster controllers are in partitions controlled by the cloud controller (Eucalyptus) and is so
 * annotated.
 * 
 * Cases:
 * 
 * 1. No annotation ==> in own partition.
 * 2. @Partition(OWNTYPE.class) ==> OWNTYPE.class in own partition.
 * 3. @Partition(OTHERTYPE.class) ==> sub-component of OTHERTYPE; one-to-one relationship.
 * 4. @Partition(value={OTHERTYPE.class}, manyToOne=true) ==> sub-component of OTHERTYPE;
 * many-to-one relationship.
 * 
 * @note for use on Class<? extends ComponentId>
 */
@Target( { ElementType.TYPE,
    ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface Partition {
  Class<? extends ComponentId>[] value( ) default {};
  /**
   * This service type can have many siblings registered to the same parent w/in a single
   * partition; e.g., many NCs belong in the same partion as their parent CC
   */
  boolean manyToOne( ) default false;
}
