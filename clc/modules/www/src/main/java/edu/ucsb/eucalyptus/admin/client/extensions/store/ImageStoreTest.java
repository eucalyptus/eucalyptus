package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;


public class ImageStoreTest implements EntryPoint {

  public void onModuleLoad() {
    ImageStoreClient imageStoreClient = new ImageStoreClient("mySessionId");
    ImageStoreWidget imageStore = new ImageStoreWidget(imageStoreClient);
    RootPanel.get("ImageStore").add(imageStore);
  }

}
