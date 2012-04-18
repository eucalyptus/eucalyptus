package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.google.gwt.user.client.ui.IsWidget;

public interface DetailView extends IsWidget, Clearable {
  
  void setTitle( String title );
  
  void showData( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> values );
  
  void disableSave( );
  
  void setPresenter( Presenter presenter );

  void setController( Controller controller );
  
  public interface Presenter extends ValueSaveHandler {
    void onAction( String key );
    void onHide( );
  }
  
  public interface Controller {
    void hideDetail( );    
  }
  
}
