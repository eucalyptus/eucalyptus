package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface ImageView extends IsWidget, CanDisplaySearchResult, Clearable, SelectionController {
  
  void setDesc( String desc );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter extends SearchRangeChangeHandler, MultiSelectionChangeHandler, KnowsPageSize {
  }
  
}
