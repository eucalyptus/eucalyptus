package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface ConfigView extends IsWidget, CanDisplaySearchResult, Clearable {

  void setPresenter( Presenter presenter );
  
  public interface Presenter extends SearchRangeChangeHandler, SingleSelectionChangeHandler, KnowsPageSize {  
  }
  
}
