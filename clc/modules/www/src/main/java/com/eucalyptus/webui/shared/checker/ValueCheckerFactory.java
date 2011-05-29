package com.eucalyptus.webui.shared.checker;

import java.util.Arrays;
import java.util.HashSet;
import com.google.common.base.Strings;

public class ValueCheckerFactory {

  public static final HashSet<Character> USERGROUPNAME_EXTRA = new HashSet<Character>( Arrays.asList( Character.valueOf( '+' ),
                                                                                                 Character.valueOf( '=' ),
                                                                                                 Character.valueOf( ',' ),
                                                                                                 Character.valueOf( '.' ),
                                                                                                 Character.valueOf( '@' ),
                                                                                                 Character.valueOf( '-' ),
                                                                                                 Character.valueOf( '_' )
                                                                                                ) );
  
  public static final HashSet<Character> POLICYNAME_EXCLUDE = new HashSet<Character>( Arrays.asList( Character.valueOf( '/' ),
                                                                                                     Character.valueOf( '\\' ),
                                                                                                     Character.valueOf( '*' ),
                                                                                                     Character.valueOf( '?' ),
                                                                                                     Character.valueOf( ' ' )
                                                                                                    ) );

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
        if ( value.split( "\\s+" ).length > 1 ) {
          throw new InvalidValueException( "Account name can not have spaces" );
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
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) ) {
            throw new InvalidValueException( "Containing invalid character for user or group name: " + c );
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
          throw new InvalidValueException( "User or group names can not be empty" );
        }
        for ( int i = 0; i < value.length( ); i++ ) {
          char c = value.charAt( i );
          if ( !Character.isLetterOrDigit( c ) && !USERGROUPNAME_EXTRA.contains( c ) && !Character.isSpace( c ) ) {
            throw new InvalidValueException( "Containing invalid character for user or group names: " + c );
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
        if ( value != null && !value.startsWith( "/" ) ) {
          throw new InvalidValueException( "Path must start with /" );
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
  
}
