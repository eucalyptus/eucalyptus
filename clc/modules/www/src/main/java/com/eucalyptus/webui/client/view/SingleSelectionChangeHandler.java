package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.client.service.SearchResultRow;

public interface SingleSelectionChangeHandler {

  void onSelectionChange( SearchResultRow selection );
  
}
