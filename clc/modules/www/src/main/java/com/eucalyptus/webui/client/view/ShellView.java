package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface ShellView extends IsWidget {
  
  void showLogConsole( );
  void hideLogConsole( );
  
  DirectoryView getDirectoryView( );
  
  ContentView getContentView( );
  
  FooterView getFooterView( );
  
  HeaderView getHeaderView( );
  
}
