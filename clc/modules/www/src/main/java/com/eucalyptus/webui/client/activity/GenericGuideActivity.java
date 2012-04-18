package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.service.GuideItem;
import com.eucalyptus.webui.client.view.ItemView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class GenericGuideActivity extends AbstractActivity {
  
  private ClientFactory clientFactory;
  private String snippet;
  
  public GenericGuideActivity( ClientFactory clientFactory, String snippet ) {
    this.clientFactory = clientFactory;
    this.snippet = snippet;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    ItemView view = clientFactory.createItemView( );
    container.setWidget( view );
    doLoadItems( view );
  }

  private void doLoadItems( final ItemView view ) {
    clientFactory.getBackendService( ).getGuide( clientFactory.getLocalSession( ).getSession( ), snippet, new AsyncCallback<ArrayList<GuideItem>>( ) {

      @Override
      public void onFailure( Throwable caught ) {       
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
      }

      @Override
      public void onSuccess( ArrayList<GuideItem> result ) {
        view.display( result );
      }
      
    } );
    
  }
  
}
