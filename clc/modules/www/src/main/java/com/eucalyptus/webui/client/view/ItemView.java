package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.GuideItem;
import com.google.gwt.user.client.ui.IsWidget;

public interface ItemView extends IsWidget {

  void display( ArrayList<GuideItem> items );
  
}
