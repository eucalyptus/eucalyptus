package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public interface StartView extends IsWidget {
  
  AcceptsOneWidget getCloudRegSnippetDisplay( );

  AcceptsOneWidget getDownloadSnippetDisplay( );

  AcceptsOneWidget getServiceSnippetDisplay( );

  AcceptsOneWidget getIamSnippetDisplay( );
  
}
