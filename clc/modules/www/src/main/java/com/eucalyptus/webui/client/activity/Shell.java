package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.event.AuthenticationDoneEvent;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class Shell extends AbstractActivity {
  
  private ClientFactory clientFactory;
  
  public Shell( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    // TODO Auto-generated method stub
    
  }
  
}
