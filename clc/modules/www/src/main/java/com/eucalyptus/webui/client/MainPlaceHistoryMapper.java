package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.place.AccountPlace;
import com.eucalyptus.webui.client.place.ErrorSinkPlace;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ServicePlace;
import com.eucalyptus.webui.client.place.StartPlace;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers( {
  StartPlace.Tokenizer.class,
  ErrorSinkPlace.Tokenizer.class,
  ServicePlace.Tokenizer.class,
  AccountPlace.Tokenizer.class,
} )
public interface MainPlaceHistoryMapper extends PlaceHistoryMapper {

}
