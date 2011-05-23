package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {
  
  void setUser( String user );
  
  UserSettingView getUserSetting( );
  
  void setSearchHandler( SearchHandler handler );
  
}
