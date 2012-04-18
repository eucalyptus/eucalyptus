package com.eucalyptus.webui.client.view;

import java.util.Set;
import com.eucalyptus.webui.client.service.SearchResultRow;

public interface MultiSelectionChangeHandler {

  void onSelectionChange( Set<SearchResultRow> selections );
  
}
