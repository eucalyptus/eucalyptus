package com.eucalyptus.webui.client.activity;

public interface WebAction {

  public static final String ACTION_SEPARATOR = ":";
  public static final String KEY_VALUE_SEPARATOR = "=";
  
  String getAction( );
  
  String getValue( String key );
  
}
