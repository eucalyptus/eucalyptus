package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.util.LocalSession;
import com.eucalyptus.webui.client.util.LocalSessionImpl;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.LoginViewImpl;
import com.eucalyptus.webui.client.view.ShellViewImpl;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;

public class ClientFactoryImpl implements ClientFactory
{
	private static final EventBus eventBus = new SimpleEventBus( );
	private static final PlaceController placeController = new PlaceController( eventBus );
	private static final LoginView loginView = new LoginViewImpl( );
	private static final StartView startView = new ShellViewImpl( );
	private static final LocalSession localSession = new LocalSessionImpl( );

	@Override
	public EventBus getEventBus( ) {
		return eventBus;
	}

	@Override
	public PlaceController getPlaceController( ) {
		return placeController;
	}

	@Override
	public LoginView getLoginView( ) {
		return loginView;
	}

  @Override
  public StartView getStartView( ) {
    return startView;
  }

  @Override
  public LocalSession getLocalSession( ) {
    return localSession;
  }

}
