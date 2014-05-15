/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.google.common.base.Enums;
import com.google.common.base.Objects;

/**
 *
 */
public enum DatabaseNamingStrategy {

  Database {
    @Override
    public String getDatabaseName( final String context ) {
      return context;
    }

    @Override
    public String getSchemaName( final String context ) {
      return null;
    }
  },
  Schema {
    @Override
    public String getDatabaseName( final String context ) {
      return SHARED_DATABASE_NAME;
    }

    @Override
    public String getSchemaName( final String context ) {
      return context;
    }
  },
  ;

  public static final String SHARED_DATABASE_NAME = "eucalyptus_shared";

  public static DatabaseNamingStrategy defaultStrategy( ) {
    return propertyValueIfPresent(
        "com.eucalyptus.component.defaultDatabaseNamingStrategy",
        DatabaseNamingStrategy.Schema );
  }

  public static DatabaseNamingStrategy overrideStrategy( final DatabaseNamingStrategy strategy ) {
    return propertyValueIfPresent(
        "com.eucalyptus.component.databaseNamingStrategy",
        strategy );
  }

  private static DatabaseNamingStrategy propertyValueIfPresent( final String propertyName,
                                                                final DatabaseNamingStrategy fallback ) {
    return Objects.firstNonNull(
        Enums.valueOfFunction( DatabaseNamingStrategy.class ).apply( System.getProperty( propertyName, "" ) ),
        fallback );
  }

  public abstract String getDatabaseName( String context );

  public abstract String getSchemaName( String context );
}
