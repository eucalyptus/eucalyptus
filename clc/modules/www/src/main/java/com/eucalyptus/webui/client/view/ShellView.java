package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface ShellView extends IsWidget {
  
  void showLogConsole( );
  void hideLogConsole( );
  
  void showDetail( );
  void hideDetail( );
  
  DirectoryView getDirectoryView( );
  
  ContentView getContentView( );
  
  FooterView getFooterView( );
  
  HeaderView getHeaderView( );
  
  DetailView getDetailView( );
  
}
