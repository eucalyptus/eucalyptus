package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.google.gwt.user.client.ui.IsWidget;

public interface DirectoryView extends IsWidget {

  void buildTree( ArrayList<QuickLinkTag> data );
  
  void changeSelection( QuickLink link );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void switchQuickLink( String search );
  }
  
}
