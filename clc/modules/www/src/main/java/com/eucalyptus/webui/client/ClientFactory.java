package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.util.LocalSession;
import com.eucalyptus.webui.client.view.LoginView;
import com.eucalyptus.webui.client.view.StartView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;

public interface ClientFactory {
  
	EventBus getEventBus( );
	
	PlaceController getPlaceController( );
	
	LoginView getLoginView( );
	StartView getStartView( );
	
	LocalSession getLocalSession( );
	
}
