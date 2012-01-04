package com.eucalyptus.webui.shared.checker;

import java.util.Arrays;
import java.util.HashSet;
import com.google.common.base.Strings;

/**
 * Various minimal input field checkers.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class ValueCheckerFactory {

  public static final HashSet<Character> USERGROUPNAME_EXTRA = new HashSet<Character>( Arrays.asList( '+', '=', ',', '.', '@', '-' ) );
  
  public static final HashSet<Character> POLICYNAME_EXCLUDE = new HashSet<Character>( Arrays.asList( '/', '\\', '*', '?', ' ' ) );

  public static final HashSet<Character> PASSWORD_SPECIAL = new HashSet<Character>( Arrays.asList( '`', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '=', '+', '[', ']', '{', '}', '\\', '|', ';', ':', '\'', '"', ',', '.', '<', '>', '/', '?' ) );

  public static final HashSet<Character> WHITESPACES = new HashSet<Character>( Arrays.asList( '\t', ' ' ) );
  
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
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) && !WHITESPACES.contains( c ) ) {
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
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) ) {
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
  
}
