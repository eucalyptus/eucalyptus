/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.util.FUtils;
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
        FUtils.valueOfFunction( DatabaseNamingStrategy.class ).apply( System.getProperty( propertyName, "" ) ),
        fallback );
  }

  public abstract String getDatabaseName( String context );

  public abstract String getSchemaName( String context );
}
