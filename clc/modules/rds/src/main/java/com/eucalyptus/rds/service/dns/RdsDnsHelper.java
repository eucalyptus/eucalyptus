/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.dns;

import java.util.concurrent.atomic.AtomicReference;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import com.eucalyptus.bootstrap.SystemIds;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedBytes;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 *
 */
public class RdsDnsHelper {

  private static final AtomicReference<byte[]> offsetBytes = new AtomicReference<>();

  private static final String DIGIT_MAPTABLE = "abcdefghijklmnopqrstuvwxyz0123456789";

  public static Name getRelativeName(final String dbIdentifier, final String accountNumber) throws NameTooLongException {
    return Name.concatenate(
        Name.fromConstantString(dbIdentifier),
    Name.fromConstantString(encode(accountNumber)));
  }

  public static Option<Tuple2<String,String>> getAccountAndIdentifierFromName(final Name name) {
    if (name==null || name.labels() != 2) {
      return Option.none();
    }
    final String dbIdentifier = name.getLabelString(0);
    final String obscuredAccountNumber = name.getLabelString(1);
    return Option.of(Tuple.of(decode(obscuredAccountNumber), dbIdentifier));
  }

  private static String encode(final String value) {
    final byte[] mapOffsets = getMapOffsets();
    final char[] valueChars = value.toCharArray();
    final StringBuilder result = new StringBuilder();
    for (int i=0; i<valueChars.length; i++) {
      int valueIndex = DIGIT_MAPTABLE.indexOf(valueChars[i]);
      result.append(DIGIT_MAPTABLE.charAt((valueIndex + UnsignedBytes.toInt(mapOffsets[i % mapOffsets.length])) % DIGIT_MAPTABLE.length()));
    }
    return result.toString();
  }

  private static String decode(final String value) {
    final byte[] mapOffsets = getMapOffsets();
    final char[] valueChars = value.toCharArray();
    final StringBuilder result = new StringBuilder();
    for (int i=0; i<valueChars.length; i++) {
      int valueIndex = DIGIT_MAPTABLE.indexOf(valueChars[i]);
      int resultIndex = (valueIndex - UnsignedBytes.toInt(mapOffsets[i % mapOffsets.length])) % DIGIT_MAPTABLE.length();
      while(resultIndex < 0) {
        resultIndex += DIGIT_MAPTABLE.length();
      }
      result.append(DIGIT_MAPTABLE.charAt(resultIndex));
    }
    return result.toString();
  }

  private static byte[] getMapOffsets() {
    byte[] offsets = offsetBytes.get();
    if (offsets == null) {
      final String uniqueName = SystemIds.createShortCloudUniqueName("rds");
      final String nameHex = uniqueName.substring(uniqueName.lastIndexOf('.')+1);
      offsets = BaseEncoding.base16().lowerCase().decode(nameHex);
      if ( !offsetBytes.compareAndSet(null, offsets) ) {
        offsets = offsetBytes.get();
      }
    }
    return offsets;
  }
}
