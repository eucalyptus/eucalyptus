package com.eucalyptus.webui.client.view;

import java.util.List;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.google.gwt.user.client.ui.IsWidget;

public interface DirectoryView extends IsWidget {

  void buildTree( List<CategoryTag> data );
  
  void setSearchHandler( SearchHandler handler );
  
}
