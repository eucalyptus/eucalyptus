package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.google.gwt.user.client.ui.IsWidget;

public interface DownloadView extends IsWidget {
  
  void displayImageDownloads( ArrayList<DownloadInfo> downloads );

  void displayToolDownloads( ArrayList<DownloadInfo> downloads );
  
}
