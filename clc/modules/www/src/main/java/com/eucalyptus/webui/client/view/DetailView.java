package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.google.gwt.user.client.ui.IsWidget;

public interface DetailView extends IsWidget {
  
  void setTitle( String title );
  
  void showData( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> values );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void hideDetail( );
  }
  
}
