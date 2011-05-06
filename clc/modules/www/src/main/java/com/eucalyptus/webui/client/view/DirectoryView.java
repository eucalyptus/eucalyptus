package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.google.gwt.user.client.ui.IsWidget;

public interface DirectoryView extends IsWidget {

  void buildTree( ArrayList<CategoryTag> data );
  
}
