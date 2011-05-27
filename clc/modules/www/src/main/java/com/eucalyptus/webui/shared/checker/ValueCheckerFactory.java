package com.eucalyptus.webui.shared.checker;

import com.google.common.base.Strings;

public class ValueCheckerFactory {

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
  
}
