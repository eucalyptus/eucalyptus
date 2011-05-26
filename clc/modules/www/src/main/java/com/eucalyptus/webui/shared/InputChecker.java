package com.eucalyptus.webui.shared;

import com.google.common.base.Strings;

public class InputChecker {

  public static String checkAccountName( String name ) {
    if ( Strings.isNullOrEmpty( name ) ) {
      return "Account name can not be empty";
    }
    if ( name.split( "\\s+" ).length > 1 ) {
      return "Account name can not have spaces";
    }
    return null;
  }
  
}
