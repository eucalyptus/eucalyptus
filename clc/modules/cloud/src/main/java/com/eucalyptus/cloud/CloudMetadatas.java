/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud;

import java.util.Collection;
import java.util.List;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

public class CloudMetadatas {
  public static <T extends CloudMetadata> Function<T, String> toDisplayName( ) {
    return new Function<T, String>( ) {
      
      @Override
      public String apply( T arg0 ) {
        return arg0.getDisplayName( );
      }
    };
  }
  
  public static <T extends CloudMetadata> Predicate<T> filterById( final Collection<String> requestedIdentifiers ) {
    return filterByProperty( requestedIdentifiers, toDisplayName() );
  } 
   
  public static <T extends CloudMetadata> Predicate<T> filterByProperty( final Collection<String> requestedValues,
	                                                                           final Function<? super T,String> extractor ) {

   return new Predicate<T>( ) {
      @Override
      public boolean apply( T input ) {
        return requestedValues == null || requestedValues.isEmpty( ) || requestedValues.contains( extractor.apply( input ) );
      }
    };
  }
  
  public static <T extends CloudMetadata> Predicate<T> filterPrivilegesById( final Collection<String> requestedIdentifiers ) {
    return Predicates.and( filterById( requestedIdentifiers ), RestrictedTypes.filterPrivileged( ) );
  }
  
  public static <T extends CloudMetadata> Predicate<T> filterByOwningAccount( final Collection<String> requestedIdentifiers ) {
    return new Predicate<T>( ) {
      @Override
      public boolean apply( T input ) {
        return requestedIdentifiers == null || requestedIdentifiers.isEmpty( ) || requestedIdentifiers.contains( input.getOwner( ).getAccountNumber( ) );
      }
    };
  }
  
  public static <T extends CloudMetadata> FilterBuilder<T> filteringFor( final Class<T> metadataClass ) {
    return new FilterBuilder<T>(metadataClass  );
  }

  public static class FilterBuilder<T extends CloudMetadata> {
    private final Class<T> metadataClass;
    private final List<Predicate<? super T>> predicates = Lists.newArrayList();

    private FilterBuilder( final Class<T> metadataClass ) {
      this.metadataClass = metadataClass;
    }

    public FilterBuilder<T> byId( final Collection<String> requestedIdentifiers ) {
      predicates.add( filterById( requestedIdentifiers ) );
      return this;
    }

    public <T extends CloudMetadata> Predicate<T> filterByProperty( final Collection<String> requestedValues,
                     final Function<? super T,String> extractor ) {
       return new Predicate<T>( ) {
         @Override
         public boolean apply( T input ) {
           return requestedValues == null || requestedValues.isEmpty() || requestedValues.contains( extractor.apply(input) );
         }
       };
     }

    public FilterBuilder<T> byProperty(final Collection<String> requestedValues, final Function<? super T, String> extractor) {
      predicates.add(filterByProperty(requestedValues, extractor));
      return this;

    }
    public FilterBuilder<T> byPrivileges() {
      predicates.add( RestrictedTypes.filterPrivileged() );
      return this;
    }

    public FilterBuilder<T> byPrivilegesWithoutOwner() {
      predicates.add( RestrictedTypes.filterPrivilegedWithoutOwner() );
      return this;
    }

    public FilterBuilder<T> byOwningAccount( final Collection<String> requestedIdentifiers ) {
      predicates.add( filterByOwningAccount( requestedIdentifiers ) );
      return this;
    }

    public FilterBuilder<T> byPredicate( final Predicate<? super T> predicate ) {
      predicates.add( predicate );
      return this;
    }

//TODO:JDK7:Restore the original code for this (does not compile with OpenJDK 1.6.0_24)    
//    public Predicate<? super T> buildPredicate() {
//      return Predicates.and( predicates );
//    }

    public Predicate<? super T> buildPredicate() {
      return buildPredicate( predicates );
    }

    private static <ST> Predicate<ST> buildPredicate( final List<Predicate<? super ST>> predicates ) {
      return Predicates.and( predicates );
    }    
  }
}
