package com.eucalyptus.webui.client.view;

public interface LogView {

  public enum LogType {
    INFO,
    ERROR
  }
  
  void log( LogType type, String content );
  void clear( );
  
}
