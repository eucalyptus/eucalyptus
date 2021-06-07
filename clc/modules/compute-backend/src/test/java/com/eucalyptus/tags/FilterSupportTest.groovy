/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.tags

import com.eucalyptus.compute.common.internal.tags.Filter
import com.eucalyptus.compute.common.internal.tags.FilterSupport
import com.eucalyptus.util.Strings
import com.google.common.collect.Iterables
import com.google.common.collect.Sets

import static org.junit.Assert.*
import org.junit.Test
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import com.google.common.base.CharMatcher
import com.eucalyptus.system.Ats
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import com.eucalyptus.crypto.util.Timestamps
import com.google.common.base.Functions
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

/**
 * Unit tests for filter support
 */
class FilterSupportTest {

  @Test
  void testWildcards() {
    // Test results are for use with 'like', so literal \ is escaped
    assertEquals( "simple", "value", wildcard( "value", false ) )
    assertEquals( "basic *", "%", wildcard( "*", true ) )
    assertEquals( "basic ?", "_", wildcard( "?", true ) )
    assertEquals( "basic \\", "\\\\", wildcard( "\\", false ) )
    assertEquals( "basic escape *", "*", wildcard( "\\*", false ) )
    assertEquals( "basic escape ?", "?", wildcard( "\\?", false ) )
    assertEquals( "basic escape \\", "\\\\", wildcard( "\\\\", false ) )
    assertEquals( "in string", "%value%", wildcard( "*value*", true ) )
    assertEquals( "starts with", "%value", wildcard( "*value", true ) )
    assertEquals( "ends with", "value%", wildcard( "value*", true ) )
    assertEquals( "starts and ends with", "value%value", wildcard( "value*value", true ) )
    assertEquals( "long", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890%", wildcard( "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890*", true ) )
    assertEquals( "repeated", "%%%%%", wildcard( "*****", true ) )
    assertEquals( "escaped", "*amazon?\\\\", wildcard( "\\*amazon\\?\\\\", false ) )
    assertEquals( "complex", "*amazon?\\\\%._\\\\foo\\\\bar%", wildcard( "\\*amazon\\?\\\\*.?\\foo\\bar*", true ) )
    assertEquals( "escape like", "\\_\\%\\\\", wildcard( "_%\\", false ) )
  }

  @Test
  void testLikeLiteralEscaping() {
    // testWildcards also exercises this functionality
    assertEquals( "not special", "*", FilterSupport.escapeLikeWildcards( "*" ) )
    assertEquals( "basic escape %", "\\%", FilterSupport.escapeLikeWildcards( "%" ) )
    assertEquals( "basic escape _", "\\_", FilterSupport.escapeLikeWildcards( "_" ) )
    assertEquals( "basic escape \\", "\\\\", FilterSupport.escapeLikeWildcards( "\\" ) )
    assertEquals( "complex escape", "text\\%foo\\\\\\_bar\\_\\%baz\\\\", FilterSupport.escapeLikeWildcards( "text%foo\\_bar_%baz\\" ) )
  }

  private String wildcard( String awsFormat, boolean wildcards ) {
    StringBuilder result = new StringBuilder()
    assertEquals( "Wildcards detected",
        wildcards,
        FilterSupport.translateWildcards( awsFormat, result, "_", "%", FilterSupport.SyntaxEscape.Like ) )
    result.toString()
  }

  static abstract class InstanceTestSupport<RT> {
    void assertValidKeys( final FilterSupport<RT> filterSupport ) {
      CharMatcher upperMatcher = CharMatcher.JAVA_UPPER_CASE;
      CharMatcher spaceMatcher = CharMatcher.WHITESPACE;
      CharMatcher specialMatcher = CharMatcher.anyOf( "~`!@#\$%^&*()_+={}[]\\|:;\"'?/><," );
      filterSupport.persistenceFilters.keySet().each { filter ->
        assertFalse( "Invalid filter (upper case character)", upperMatcher.matchesAnyOf( filter ) )
        assertFalse( "Invalid filter (space)", spaceMatcher.matchesAnyOf( filter ) )
        assertFalse( "Invalid filter (special character)", specialMatcher.matchesAnyOf( filter ) )
      }
    }

    void assertValidAliases( final FilterSupport<RT> filterSupport ) {
      final Set<String> directAliases = Sets.newTreeSet( filterSupport.aliases.keySet( ) )
      for ( final String aliased : filterSupport.aliases.values( ) ) {
        // can't currently check for nested alias validity
        Iterables.removeIf( directAliases, Strings.startsWith( "${aliased}." ) )
      }
      assertValidAliases( directAliases, filterSupport.resourceClass )
    }
    
    void assertValidAliases( final Set<String> aliasProperties,
                             final Class target ) {
      aliasProperties.each { property ->
        String[] propPath = property.split( "\\." )
        Class targetType = target;
        propPath.each { propName ->
          boolean found = false;
          ReflectionUtils.doWithFields( targetType, { Field field ->
            if ( field.name.equals( propName ) ) {
              assertTrue( "Field must be a relation or embedded: " + propName, isAnnotatedAsRelation( field ) || Ats.from( field ).has( Embedded.class ) )
              if ( Collection.isAssignableFrom( field.type ) ) { // relation
                targetType = field.genericType.getActualTypeArguments()[0]
              } else {
                targetType = field.type
              }
              found = true
            }
          } as ReflectionUtils.FieldCallback )
          assertTrue( "Property not found " + propName + " on " + targetType, found )
        }
      }
    }

    void assertValidFilters( final FilterSupport<RT> filterSupport ) {
      assertValidFilters( filterSupport, filterSupport.resourceClass, [:] )
    }

    void assertValidFilters( final FilterSupport<RT> filterSupport,
                             final Class target,
                             final Map<Class,List<Class>> searchableSubclasses ) {
      filterSupport.getPersistenceFilters().values().each { property ->
        List<String> propPath = property.property.split( "\\." ) as List
        while ( filterSupport.aliases.values( ).contains( propPath.first( ) ) &&
            !filterSupport.aliases.containsKey( propPath.first( ) ) ) {
          // expand alias
          BiMap<String,String> aliases = HashBiMap.create( filterSupport.aliases ).inverse()
          aliases.get( propPath.remove( 0 ) ).split( "\\." ).reverse().each { propName ->
            propPath.add( 0, propName )              
          }
        }
        Class targetType = target;
        propPath.each { propName ->
          boolean found = false;
          List<Class> searchTargets = [ targetType ]
          searchableSubclasses.get( targetType )?.each { clazz ->
            searchTargets.add( clazz )            
          }
          for ( Class searchTarget : searchTargets ) {
            if ( found ) break;
            ReflectionUtils.doWithFields( searchTarget, { Field field ->
              if ( field.name.equals( propName ) ) {
                if ( Collection.isAssignableFrom( field.type ) ) { // relation
                  assertTrue( "Collection must be relation: " + propName, Ats.from( field ).has( ManyToMany.class ) || Ats.from( field ).has( OneToMany.class ) || Ats.from( field ).has( ManyToOne.class ) || Ats.from( field ).has( OneToOne.class ) )
                  assertFalse( "Collection must not be ElementCollection: " + propName, Ats.from( field ).has( ElementCollection.class ) )
                  targetType = field.genericType.getActualTypeArguments()[0]
                } else {
                  assertTrue( "Field type not supported: " + field.type, (field.type instanceof Class<String> || field.type instanceof Class<Date> || field.type instanceof Class<Boolean> || field.type instanceof Class<Long> || field.type instanceof Class<Integer> ) || Ats.from( field ).has( Embedded.class ) )
                  targetType = field.type
                }
                found = true
              }
            } as ReflectionUtils.FieldCallback )
          }
          assertTrue( "Property not found " + propName + " on " + targetType, found )
        }
        if ( !String.class.equals( targetType ) ) {
          assertNotSame( "Property missing type or conversion function " + property.property + " for " + targetType + " (see FilterSupport.PersistenceFilter.Type)", Functions.identity(), property.valueFunction )
        }
      }
    }

    void assertValid( final FilterSupport<RT> filterSupport ) {
      assertValid( filterSupport, [:] )
    }
    
    void assertValid( final FilterSupport<RT> filterSupport,
                      final Map<Class,List<Class>> searchableSubclasses ) {
      assertValidAliases( filterSupport )
      assertValidFilters( filterSupport, filterSupport.resourceClass, searchableSubclasses )
      assertValidKeys( filterSupport )
      assertValidTagConfig( filterSupport )
    }

    /**
     * Verifies fields used in hibernate queries exist as expected.
     */
    void assertValidTagConfig( final FilterSupport<RT> filterSupport ) {
      if ( filterSupport.tagFieldName ) {
        assertNotNull( "Resource class tags field", ReflectionUtils.findField( filterSupport.resourceClass, filterSupport.tagFieldName, Collection.class ) )
      }

      if ( filterSupport.tagClass ) {
        assertNotNull( "Tag class account field", ReflectionUtils.findField( filterSupport.tagClass, "ownerAccountNumber", String.class ) )
        assertNotNull( "Tag class name field", ReflectionUtils.findField( filterSupport.tagClass, "displayName", String.class ) )
        assertNotNull( "Tag class value field", ReflectionUtils.findField( filterSupport.tagClass, "value", String.class ) )
        assertNotNull( "Tag class resource field", ReflectionUtils.findField( filterSupport.tagClass, filterSupport.resourceFieldName, filterSupport.resourceClass ) )
      }
    }

    void assertMatch( FilterSupport filterSupport,
                      boolean expectedMatch,
                      String filterKey,
                      String filterValue,
                      RT target ) {
      Filter filter = filterSupport.generate( [ (filterKey) : [ filterValue ] as Set ], false, "000000000" )
      assertEquals( "Match asserton for " + filterKey, expectedMatch, filter.asPredicate().apply( target ) )
    }

    Date date( String isoDateText ) {
      Timestamps.parseTimestamp( isoDateText, Timestamps.Type.ISO_8601 );
    }
    
    boolean isAnnotatedAsRelation( final Field field ) {
      return \
          Ats.from( field ).has( ManyToMany.class ) || 
          Ats.from( field ).has( OneToMany.class ) || 
          Ats.from( field ).has( ManyToOne.class ) || 
          Ats.from( field ).has( OneToOne.class )
    }
  }
}
