/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
