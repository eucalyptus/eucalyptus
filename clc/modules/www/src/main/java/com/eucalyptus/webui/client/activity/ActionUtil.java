package com.eucalyptus.webui.client.activity;

import com.google.common.base.Strings;

public class ActionUtil {
  
  public static WebAction parseAction( String action ) {
    if ( Strings.isNullOrEmpty( action ) ) {
      return null;
    }
    final String[] parts = action.split( WebAction.ACTION_SEPARATOR, 2 );
    if ( parts.length < 2 || Strings.isNullOrEmpty( parts[0] ) || Strings.isNullOrEmpty( parts[1] ) ) {
      return null;
    }
    final String[] subParts = parts[1].split( WebAction.KEY_VALUE_SEPARATOR, 2 );
    return new WebAction( ) {

      @Override
      public String getAction( ) {
        return parts[0];
      }

      @Override
      public String getValue( String key ) {
        if ( key != null && key.equals( subParts[0] ) ) {
          return subParts[1];
        }
        return null;
      }
      
    };
  }
  
}
