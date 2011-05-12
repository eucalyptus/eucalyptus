package com.eucalyptus.webui.client.view;

import com.eucalyptus.webui.client.service.SearchRange;

public interface SearchResultRangeChangeHandler {

  void handleRangeChange( SearchRange range );
  
}
