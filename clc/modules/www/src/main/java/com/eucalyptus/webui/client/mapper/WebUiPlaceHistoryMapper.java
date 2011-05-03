package com.eucalyptus.webui.client.mapper;

import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers( { LoginPlace.Tokenizer.class, StartPlace.Tokenizer.class })
public interface WebUiPlaceHistoryMapper extends PlaceHistoryMapper {

}
