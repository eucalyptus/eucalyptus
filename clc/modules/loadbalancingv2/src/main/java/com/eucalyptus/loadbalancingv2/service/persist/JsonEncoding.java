/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.EnumSet;

import static com.eucalyptus.util.Json.JsonOption.OmitNullValues;
import static com.eucalyptus.util.Json.JsonOption.UpperCamelPropertyNaming;


public class JsonEncoding {

  private static final ObjectMapper mapper =
      Json.mapper(EnumSet.of(OmitNullValues, UpperCamelPropertyNaming));
  private static final ObjectReader reader = mapper.reader( );
  private static final ObjectWriter writer = mapper.writer( );

  public static <T extends EucalyptusData> Option<T> read(final Class<T> type, final String text) {
    try {
      if (Strings.isNullOrEmpty(text)) {
        return Option.none();
      } else {
        return Option.some(Json.readObject(reader, type, text));
      }
    } catch (Exception e) {
      return Option.none();
    }
  }

  public static String write(final EucalyptusData data) {
    try {
      return writer.writeValueAsString(data);
    } catch (IOException e) {
      throw Exceptions.toUndeclared("Error writing json data", e);
    }
  }
}
