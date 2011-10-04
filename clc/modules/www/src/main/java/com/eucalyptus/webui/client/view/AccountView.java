package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface AccountView extends IsWidget, CanDisplaySearchResult, Clearable, SelectionController {
    
  void enableNewButton( boolean enabled );
  void enableDelButton( boolean enabled );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter extends SearchRangeChangeHandler, MultiSelectionChangeHandler, KnowsPageSize {
    void onCreateAccount( );
    void onDeleteAccounts( );
    void onCreateUsers( );
    void onCreateGroups( );
    void onAddPolicy( );
    void onApprove( );
    void onReject( );
  }
  
}
