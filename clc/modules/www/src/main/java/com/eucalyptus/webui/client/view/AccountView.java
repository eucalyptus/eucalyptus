package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.gwt.user.client.ui.IsWidget;

public interface AccountView extends IsWidget {
  
  void initializeTable( int pageSize, ArrayList<SearchResultFieldDesc> columnDescs, SearchResultRangeChangeHandler changeHandler );
  
  void setData( SearchResult data );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void onSelectionChange( Set<SearchResultRow> selection );
  }
  
}
