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
 ************************************************************************/
package com.eucalyptus.tags

import static org.junit.Assert.*
import org.junit.Test
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import com.eucalyptus.entities.AbstractPersistent
import com.eucalyptus.cloud.CloudMetadata
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

  static abstract class InstanceTest<RT extends AbstractPersistent & CloudMetadata> {
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

    void assertValidFilters( final FilterSupport<RT> filterSupport  ) {
      filterSupport.getPersistenceFilters().values().each { property ->
        String[] propPath = property.property.split( "\\." )
        Class targetType = filterSupport.resourceClass;
        propPath.each { propName ->
          boolean found = false;
          ReflectionUtils.doWithFields( targetType, { Field field ->
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
          assertTrue( "Property not found " + propName + " in " + property.property, found )
        }
        if ( !String.class.equals( targetType ) ) {
          assertNotSame( "Property missing type or conversion function " + property.property + " for " + targetType + " (see FilterSupport.PersistenceFilter.Type)", Functions.identity(), property.valueFunction )
        }
      }
    }

    void assertValid( final FilterSupport<RT> filterSupport ) {
      assertValidFilters( filterSupport )
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
      Filter filter = filterSupport.generate( [ (filterKey) : [ filterValue ] as Set ], "000000000" )
      assertEquals( "Match asserton for " + filterKey, expectedMatch, filter.asPredicate().apply( target ) )
    }

    Date date( String isoDateText ) {
      Timestamps.parseTimestamp( isoDateText ).getTime();
    }
  }
}
