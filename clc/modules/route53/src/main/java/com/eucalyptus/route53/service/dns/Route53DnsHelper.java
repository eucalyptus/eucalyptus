/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.dns;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
public class Route53DnsHelper {

  /**
   * Create a canonical absolute name.
   *
   * @param dnsName The name to modify
   * @return The modified name
   */
  public static String absoluteName(final String dnsName) {
    return dnsName==null ?
      null :
      dnsName.endsWith(".") ?
          dnsName.toLowerCase() :
          (dnsName + ".").toLowerCase();
  }

  public static boolean isSystemDomain(final String dnsName) {
    try {
      return DomainNames.isExternalSubdomain(Name.fromString(dnsName, DomainNames.root()));
    } catch (final TextParseException e) {
      return false;
    }
  }

  /**
   * Get list of name to (registration) ip for nameserver subset.
   */
  public static List<Tuple2<String,InetAddress>> getSystemNameservers(final Set<String> subset) {
    final List<Tuple2<Integer, InetAddress>> systemNameservers = DomainNameRecords.getNameserversByNumber();
    final List<Tuple2<String, InetAddress>> systemNameserverSubset = Lists.newArrayList();
    for ( final Tuple2<Integer, InetAddress> systemNameserver : systemNameservers ) {
      final String name = getSystemNameserverName(systemNameserver._1());
      if (subset.contains(name)) {
        systemNameserverSubset.add(Tuple.of(name, systemNameserver._2()));
      }
    }
    return systemNameserverSubset;
  }

  /**
   * Get list of name to (registration) ip for nameservers.
   */
  public static List<Tuple2<String,InetAddress>> getSystemNameservers() {
    return getSystemNameservers(getSystemNameserverNames());
  }

  /**
   * Get four system nameserver names with trailing dot.
   */
  public static Set<String> getSystemNameserverNames() {
    final Set<String> nameserverNames = Sets.newTreeSet();
    // Route53 selects 4 nameservers per domain
    for (int i=0; i<4; i++) {
      nameserverNames.add(getSystemNameserverName(i + 1));
    }
    return nameserverNames;
  }

  public static String getSystemNameserverName(final int number) {
    final Name external = DomainNames.externalSubdomain();
    return DomainNames.absolute(
        Name.fromConstantString("ns" + number),
        external ).toString();
  }
}
