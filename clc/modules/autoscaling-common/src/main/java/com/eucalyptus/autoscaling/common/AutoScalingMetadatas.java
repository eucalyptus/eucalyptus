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
package com.eucalyptus.autoscaling.common;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingMetadataWithResourceName;
import java.util.Collection;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 *
 */
public class AutoScalingMetadatas extends RestrictedTypes {
  
  public static <T extends AutoScalingMetadataWithResourceName> Function<T, String> toArn( ) {
    return new Function<T, String>( ) {
      @Override
      public String apply( T metadata ) {
        return metadata == null ? null : metadata.getArn();
      }
    };
  }
  
  public static <T extends AutoScalingMetadataWithResourceName> Predicate<T> filterByArn( final Collection<String> requestedArns ) {
    return filterByProperty( requestedArns, toArn() );
  }

  public static <T extends AutoScalingMetadataWithResourceName> Predicate<T> filterPrivilegesByIdOrArn( final Class<T> metadataClass,
                                                                                                        final Collection<String> requestedItems ) {
    final Collection<String> names = AutoScalingResourceName.simpleNames( requestedItems );
    final Collection<String> arns =  AutoScalingResourceName.arns( requestedItems );
    return Predicates.and(
        !arns.isEmpty() && !names.isEmpty() ? 
          Predicates.<T>or(
              AutoScalingMetadatas.<T>filterById( names ),
              AutoScalingMetadatas.<T>filterByArn( arns ) ) :
          !arns.isEmpty() ?
              AutoScalingMetadatas.<T>filterByArn( arns ) :
              AutoScalingMetadatas.<T>filterById( names ),
        RestrictedTypes.filteringFor( metadataClass ).byPrivileges( ).buildPredicate( ) );
  }

}
