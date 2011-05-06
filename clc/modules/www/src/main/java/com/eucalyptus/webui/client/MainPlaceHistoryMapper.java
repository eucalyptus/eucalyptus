package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.place.ServicePlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers( { StartPlace.Tokenizer.class } )
public interface MainPlaceHistoryMapper extends PlaceHistoryMapper {

}
