/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This is a revised version of {@link com.google.gwt.place.shared.PlaceHistoryHandler}
 * for better handling of error cases.
 */
package com.eucalyptus.webui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.History;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an extended version of {@link com.google.gwt.place.shared.PlaceHistoryHandler},
 * which differentiates the default place and the error place, for better error handling.
 */
/**
 * Monitors {@link PlaceChangeEvent}s and
 * {@link com.google.gwt.user.client.History} events and keep them in sync.
 */
public class ExPlaceHistoryHandler extends PlaceHistoryHandler {
  
  private static final Logger log = Logger.getLogger(ExPlaceHistoryHandler.class.getName());

  private final Historian historian;

  private final PlaceHistoryMapper mapper;

  private PlaceController placeController;

  private Place defaultPlace = Place.NOWHERE;
  // This is the place to go when no matching place
  private Place errorPlace = Place.NOWHERE;

  /**
   * Create a new PlaceHistoryHandler with a {@link DefaultHistorian}. The
   * DefaultHistorian is created via a call to GWT.create(), so an alternative
   * default implementation can be provided through &lt;replace-with&gt; rules
   * in a {@code gwt.xml} file.
   * 
   * @param mapper a {@link PlaceHistoryMapper} instance
   */
  public ExPlaceHistoryHandler(PlaceHistoryMapper mapper) {
    this(mapper, (Historian) GWT.create(DefaultHistorian.class));
  }

  /**
   * Create a new PlaceHistoryHandler.
   * 
   * @param mapper a {@link PlaceHistoryMapper} instance
   * @param historian a {@link Historian} instance
   */
  public ExPlaceHistoryHandler(PlaceHistoryMapper mapper, Historian historian) {
    super( mapper, historian );
    this.mapper = mapper;
    this.historian = historian;
  }

  /**
   * Handle the current history token. Typically called at application start, to
   * ensure bookmark launches work.
   */
  @Override
  public void handleCurrentHistory() {
    handleHistoryToken(historian.getToken());
  }
  
  @Override
  public HandlerRegistration register(PlaceController placeController,
      EventBus eventBus, Place defaultPlace) {
    return register(placeController, eventBus, defaultPlace, defaultPlace);
  }

  /**
   * Initialize this place history handler.
   * 
   * @return a registration object to de-register the handler
   */
  public HandlerRegistration register(PlaceController placeController,
      EventBus eventBus, Place defaultPlace, Place errorPlace) {
    this.placeController = placeController;
    this.defaultPlace = defaultPlace;
    this.errorPlace = errorPlace;

    final HandlerRegistration placeReg = eventBus.addHandler(
        PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
          public void onPlaceChange(PlaceChangeEvent event) {
            log().log( Level.INFO, "Place changed" );
            Place newPlace = event.getNewPlace();
            historian.newItem(tokenForPlace(newPlace), false);
          }
        });

    final HandlerRegistration historyReg = historian.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        String token = event.getValue();
        log().log( Level.INFO, "History changed: " + token );        
        handleHistoryToken(token);
      }
    });

    return new HandlerRegistration() {
      public void removeHandler() {
        ExPlaceHistoryHandler.this.defaultPlace = Place.NOWHERE;
        ExPlaceHistoryHandler.this.placeController = null;
        placeReg.removeHandler();
        historyReg.removeHandler();
      }
    };
  }

  /**
   * Visible for testing.
   */
  Logger log() {
    return log;
  }

  private void handleHistoryToken(String token) {

    Place newPlace = null;

    if ("".equals(token)) {
      newPlace = defaultPlace;
    }

    if (newPlace == null) {
      newPlace = mapper.getPlace(token);
    }

    if (newPlace == null) {
      log().warning("Unrecognized history token: " + token);
      newPlace = errorPlace;
    }

    placeController.goTo(newPlace);
  }

  private String tokenForPlace(Place newPlace) {
    if (defaultPlace.equals(newPlace)) {
      return "";
    }

    String token = mapper.getToken(newPlace);
    if (token != null) {
      return token;
    }

    log().warning("Place not mapped to a token: " + newPlace);
    return "";
  }
}
