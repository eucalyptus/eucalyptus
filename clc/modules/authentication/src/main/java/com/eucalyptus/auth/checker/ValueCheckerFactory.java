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

package com.eucalyptus.auth.checker;

import java.util.Arrays;
import java.util.HashSet;

import com.google.common.base.Strings;

/**
 * Various minimal input field checkers. Update both of the identical files at the same time!!!
 * {@link com.eucalyptus.auth.checker.ValueCheckerFactory}
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

  public static final HashSet<Character> PASSWORD_SPECIAL = new HashSet<Character>( Arrays.asList( '`', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '=', '+', '[', ']', '{', '}', '\\', '|', ';', ':', '\'', '"', ',', '.', '<', '>', '/', '?' ) );

  public static final HashSet<Character> NAME_SEPARATORS = new HashSet<Character>( Arrays.asList( ';' ) );

  public static final String PASSWORDS_NOT_MATCH = "Passwords do not match";
  public static final String PASSWORD_NOT_CHANGED = "New password must not be the same as old password";

  /**
   * Value checker that records a value for later checks.
   */
  public static class ValueSaver implements ValueChecker {
    private String value;

    ValueSaver() {      
    }
    
    String getValue() {
      return value;
    }

    @Override
    public String check( final String value ) throws InvalidValueException {
      return this.value = value;
    }
  }

  /**
   * Value checker support class for checking two fields.
   */
  public static abstract class SavedValueChecker implements ValueChecker {
    private final ValueSaver valueSaver;

    public SavedValueChecker( final ValueSaver valueSaver  ) {
      this.valueSaver = valueSaver;
    }

    @Override
    public final String check( final String value ) throws InvalidValueException {
      return check( value, valueSaver.getValue() );
    }

    /**
     * Override to perform validation of fields.
     *
     * @param value The value for the final field
     * @param savedValue Value for the saved field
     * @return The value for this checkers field
     * @throws InvalidValueException If values are invalid
     */
    protected abstract String check( String value,
                                     String savedValue ) throws InvalidValueException;
  }

  public static ValueChecker createNonEmptyValueChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Content can not be empty" );
        }
        return value;
      }

    };
  }

  public static ValueSaver createValueSaver() {
    return new ValueSaver();
  }

  /**
   * Create a checker that validates equality of the fields.
   *
   * @param errorMessage The message to display.
   * @return The checker
   */
  public static ValueChecker createEqualityChecker( final String errorMessage,
                                                    final ValueSaver valueSaver ) {
    return new SavedValueChecker( valueSaver ) {
      @Override
      public String check( final String value,
                           final String savedValue ) throws InvalidValueException {
        if ( !Strings.nullToEmpty( savedValue ).equals( value ) ) {
          throw new InvalidValueException( errorMessage );
        }
        return value;
      }

    };
  }

  /**
   * Create a checker that validates inequality of the fields.
   *
   * @param errorMessage The message to display.
   * @return The checker
   */
  public static ValueChecker createInequalityChecker( final String errorMessage,
                                                      final ValueSaver valueSaver ) {
    return new SavedValueChecker( valueSaver ) {
      @Override
      public String check( final String value,
                           final String savedValue ) throws InvalidValueException {
        if ( Strings.nullToEmpty( savedValue ).equals( value ) ) {
          throw new InvalidValueException( errorMessage );
        }
        return value;
      }

    };
  }

  public static ValueChecker createAccountNameChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Account name can not be empty" );
        }
        if ( value.startsWith( "-" ) ) {
          throw new InvalidValueException( "Account name can not start with hyphen" );
        }
        if ( value.contains( "--" ) ) {
          throw new InvalidValueException( "Account name can not have two consecutive hyphens" );
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
  
  public static ValueChecker createUserAndGroupNamesChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "User or group names can not be empty" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) && !USERGROUPNAME_AD.contains( c ) && !NAME_SEPARATORS.contains( c ) ) {
            throw new InvalidValueException( "Containing invalid character for user or group names: " + c );
          }
        }
        return value;
      }

    };
  }

  public static ValueChecker createUserAndGroupNameChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "User or group name can not be empty" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) && !USERGROUPNAME_AD.contains( c ) ) {
            throw new InvalidValueException( "Containing invalid character for user or group name: " + c );
          }
        }
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
          throw new InvalidValueException( "User or group names can not be empty" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( POLICYNAME_EXCLUDE.contains( c ) ) {
            throw new InvalidValueException( "Containing invalid character for user or group names: " + c );
          }
        }
        return value;
      }

    };
  }
  
  public static final int PASSWORD_MINIMAL_LENGTH = 6;
  
  public static ValueChecker createPasswordChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Password can not be empty" );
        }
        int digit = 0;
        int lowerCase = 0;
        int upperCase = 0;
        int special = 0;
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( Character.isDigit( c ) ) {
            digit++;
          } else if ( Character.isLetter( c ) && Character.isLowerCase( c ) ) {
            lowerCase++;
          } else if ( Character.isLetter( c ) && Character.isUpperCase( c ) ) {
            upperCase++;
          } else if ( PASSWORD_SPECIAL.contains( c ) ) {
            special++;
          }
        }
        int length = value.length( );
        if ( length < PASSWORD_MINIMAL_LENGTH ) {
          throw new InvalidValueException( "Password length must be at least 6 characters" );
        }
        int score = 1;
        if ( length >= 12 ) {
          score += 4;
        } else if ( length >= 8 ) {
          score += 2;
        }
        if ( lowerCase > 0 && upperCase > 0 ) {
          score++;
        }
        if ( digit > 1 ) {
          score += 2;
        } else if ( digit > 0 ) {
          score++ ;
        }
        if ( special > 1 ) {
          score += 3;
        } else if ( special > 0 ) {
          score += 2;
        }
        if ( score < 2 ) {
          return WEAK;
        } else if ( score < 4 ) {
          return MEDIUM;
        } else if ( score < 6 ) {
          return STRONG;
        } else {
          return STRONGER;
        }
      }
    };
  }
  
  public static ValueChecker createEmailChecker( ) {
    return new ValueChecker( ) {

      @Override
      public String check( String value ) throws InvalidValueException {
        if ( Strings.isNullOrEmpty( value ) ) {
          throw new InvalidValueException( "Email address can not be empty" );
        }        
        String[] parts = value.split( "@" );
        if ( parts.length < 2 || Strings.isNullOrEmpty( parts[0] ) || Strings.isNullOrEmpty( parts[1] ) ) {
          throw new InvalidValueException( "Does not look like a valid email address: missing user or host" );
        }
        if ( value.split( "\\s+" ).length > 1 ) {
          throw new InvalidValueException( "Email address can not have spaces" );
        }
        return value;
      }

    };
  }

  /**
   * Create a checker that evaluates the given checkers.
   *
   * <p>Checking fails fast, so all checkers may not be evaluated.</p>
   *
   * @param checkers The checkers to run.
   * @return The checker.
   */
  public static ValueChecker checkerForAll( final ValueChecker... checkers ) {
    return new ValueChecker() {
      @Override
      public String check( final String value ) throws InvalidValueException {
        String currentValue = value;
        for ( final ValueChecker checker : checkers ) {
          currentValue = checker == null ?
              value :
              checker.check( value );
        }

        return currentValue;
      }
    };
  }

}
