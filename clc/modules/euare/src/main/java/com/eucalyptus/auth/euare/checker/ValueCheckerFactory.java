/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.checker;

import java.util.Arrays;
import java.util.HashSet;

import com.google.common.base.Strings;

/**
 * Various minimal input field checkers. Update both of the identical files at the same time!!!
 * {@link ValueCheckerFactory}
 * {@link com.eucalyptus.webui.shared.checker.ValueCheckerFactory}
 */
public class ValueCheckerFactory {

  public static final HashSet<Character> USERGROUPNAME_EXTRA = new HashSet<Character>( Arrays.asList( '+', '=', ',', '.', '@', '-' ) );
  
  // special characters for those from Active Directory sync
  public static final HashSet<Character> USERGROUPNAME_AD = new HashSet<Character>( Arrays.asList( '_', ' ' ) );
  
  // REGEX used for LDAP sync to sanitizing user/gropu names.
  // !!! ALWAYS update this if the above two sets change
  public static final String INVALID_USERGROUPNAME_CHARSET_REGEX = "[^a-zA-Z0-9+=,.@\\-_ ]";
  
  public static final String INVALID_ACCOUNTNAME_CHARSET_REGEX = "[^a-z0-9\\-]";
  
  public static final HashSet<Character> POLICYNAME_EXCLUDE = new HashSet<Character>( Arrays.asList( '/', '\\', '*', '?', ' ' ) );

  public static ValueChecker createAccountNameChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Account name can not be empty" );
        }
        if ( value.length( ) > 63 ) {
          throw new InvalidValueException( "Name too long" );
        }
        if ( value.startsWith( "-" ) ) {
          throw new InvalidValueException( "Account name can not start with hyphen" );
        }
        if ( value.contains( "--" ) ) {
          throw new InvalidValueException( "Account name can not have two consecutive hyphens" );
        }
        if ( value.matches( "^[0-9]{12}$" ) ) {
          throw new InvalidValueException( "Account name must not be 12 digits" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( !( Character.isLetterOrDigit( c ) || c == '-' ) || Character.isUpperCase( c ) ) {
            throw new InvalidValueException( "Containing invalid character for account name: " + c );
          }
        }
        return value;
      }

    };
  }
  
  public static ValueChecker createUserNameChecker( ) {
    return new ValueChecker( ) {
      @Override
      public String check( String value ) throws InvalidValueException {
        checkUserOrGroupName( value, 64 );
        return value;
      }

    };
  }

  public static ValueChecker createGroupNameChecker( ) {
    return new ValueChecker( ) {
      @Override
      public String check( String value ) throws InvalidValueException {
        checkUserOrGroupName( value, 128 );
        return value;
      }

    };
  }

  public static ValueChecker createPathChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( value == null || ( value != null && !value.startsWith( "/" ) ) ) {
          throw new InvalidValueException( "Path must start with /" );
        }
        if ( value.length( ) > 512 ) {
          throw new InvalidValueException( "Path too long" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( c < 0x21 || c > 0x7E ) {
            throw new InvalidValueException( "Invalid path character: " + c );
          }
        }
        return value;
      }

    };
  }

  public static ValueChecker createPolicyNameChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Policy names can not be empty" );
        }
        if ( value.length( ) > 128 ) {
          throw new InvalidValueException( "Name too long" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( POLICYNAME_EXCLUDE.contains( c ) ) {
            throw new InvalidValueException( "Containing invalid character for policy names: " + c );
          }
        }
        return value;
      }

    };
  }

  public static ValueChecker createManagedPolicyNameChecker( ) {
    return value -> {
      if ( Strings.isNullOrEmpty( value ) ) {
        throw new InvalidValueException( "Policy names can not be empty" );
      }
      if ( value.length( ) > 128 ) {
        throw new InvalidValueException( "Policy name too long" );
      }
      if ( !value.matches( "[\\w+=,.@-]+" ) ) {
        throw new InvalidValueException( "Invalid characters in policy name" );
      }
      return value;
    };
  }

  private static void checkUserOrGroupName( final String value, final int maxLength ) throws InvalidValueException {
    if ( Strings.isNullOrEmpty( value ) ) {
      throw new InvalidValueException( "User or group name can not be empty" );
    }
    if ( value.length( ) > maxLength ) {
      throw new InvalidValueException( "Name too long" );
    }
    for ( int i = 0; i < value.length( ); i++ ) {
      char c = value.charAt( i );
      if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) && !USERGROUPNAME_AD.contains( c ) ) {
        throw new InvalidValueException( "Containing invalid character for user or group name: " + c );
      }
    }
  }

}
