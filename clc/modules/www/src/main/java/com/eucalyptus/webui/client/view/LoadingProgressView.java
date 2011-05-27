package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface LoadingProgressView extends IsWidget {
  
  void setProgress( int percent );
  
}
