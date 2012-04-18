package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.SearchPlace;
import com.eucalyptus.webui.client.view.ActionResultView;
import com.eucalyptus.webui.client.view.ActionResultView.ResultType;
import com.google.common.base.Strings;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public abstract class AbstractActionActivity extends AbstractActivity implements ActionResultView.Presenter {
  
  protected interface KeyValue {
    public String getKey( );
    public String getValue( );
  }
  
  protected SearchPlace place;
  protected ClientFactory clientFactory;
  
  protected ActionResultView view;
  
  public AbstractActionActivity( SearchPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    view = clientFactory.getActionResultView( );
    view.display( ResultType.NONE, "", false );
    view.setPresenter( this );
    container.setWidget( view );
    doAction( place.getSearch( ) );
  }

  @Override
  public void onConfirmed( ) {
    // Nothing
  }
  
  protected static KeyValue parseKeyValue( String keyValue ) {
    if ( Strings.isNullOrEmpty( keyValue ) ) {
      return null;
    }
    final String[] parts = keyValue.split( WebAction.KEY_VALUE_SEPARATOR, 2 );
    if ( parts.length < 2 || Strings.isNullOrEmpty( parts[0] ) || Strings.isNullOrEmpty( parts[1] ) ) {
      return null;
    }
    return new KeyValue( ) {
      @Override
      public String getKey( ) {
        return parts[0];
      }
      @Override
      public String getValue( ) {
        return parts[1];
      }
    };
  }
  
  protected abstract void doAction( String action );
  
}
