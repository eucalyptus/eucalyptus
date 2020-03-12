/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import java.util.List;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 *
 */
@ConfigurableClass(root = "services.route53", description = "Parameters controlling route53")
public class Route53ServiceProperties {

  @ConfigurableField(
      description = "Limit for resource records sets per hosted zone.",
      initialInt = 1_000 )
  public static Integer HOSTEDZONE_RRSET_LIMIT = 1_000;

  @ConfigurableField(
      description = "Limit for vpc associations per hosted zone.",
      initialInt = 100 )
  public static Integer HOSTEDZONE_VPC_LIMIT = 100;

  @ConfigurableField(
      description = "TTL for new public hosted zones SOA resource record set.",
      initialInt = 900 )
  public static Integer HOSTEDZONE_SOA_TTL_DEFAULT = 900;

  @ConfigurableField(
      description = "Email for new public hosted zones SOA resource record set.",
      changeListener = PropertyChangeListeners.RegexMatchListener.class,
      initial = "root" )
  @PropertyChangeListeners.RegexMatchListener.RegexMatch(
      message = "email address required",
      regex = "[a-z0-9._-]+(?:@[a-z0-9._-]+)?" )
  public static String HOSTEDZONE_SOA_EMAIL_DEFAULT = "root";

  @ConfigurableField(
      description = "Values for new public hosted zones SOA resource record set ([zone-serial-number] [refresh-time] [retry-time] [expire-time] [negative caching TTL])",
      initial = "1 7200 900 1209600 86400" )
  public static String HOSTEDZONE_SOA_VALUES_DEFAULT = "1 7200 900 1209600 86400";

  @ConfigurableField(
      description = "TTL for new public hosted zones NS resource record set.",
      initialInt = 900 )
  public static Integer HOSTEDZONE_NS_TTL_DEFAULT = 900;

  @ConfigurableField(
      description = "Nameserver names for new public hosted zones NS resource record set (comma separated list)",
      changeListener = PropertyChangeListeners.RegexMatchListener.class )
  @PropertyChangeListeners.RegexMatchListener.RegexMatch(
      message = "List of host names required",
      regex = "(?:[a-z0-9.-]+\\s*,?\\s*)*" )
  public static String HOSTEDZONE_NS_NAMES_DEFAULT = "";

  @ConfigurableField(
      description = "Maximum number of tags per resource.",
      initialInt = 10 )
  public static Integer MAX_TAGS = 10;

  public static Integer getHostedZoneRRSetLimit() {
    return MoreObjects.firstNonNull(HOSTEDZONE_RRSET_LIMIT, 100);
  }

  public static Integer getHostedZoneVpcLimit() {
    return MoreObjects.firstNonNull(HOSTEDZONE_VPC_LIMIT, 1_000);
  }

  public static Integer getHostedZoneSoaTtlDefault() {
    return MoreObjects.firstNonNull(HOSTEDZONE_SOA_TTL_DEFAULT, 900);
  }

  public static String getHostedZoneSoaEmailDefault() {
    return MoreObjects.firstNonNull(Strings.emptyToNull(HOSTEDZONE_SOA_EMAIL_DEFAULT), "root").trim();
  }

  public static String getHostedZoneSoaValueDefault() {
    return MoreObjects.firstNonNull(Strings.emptyToNull(HOSTEDZONE_SOA_VALUES_DEFAULT), "1 7200 900 1209600 86400").trim();
  }

  public static Integer getHostedZoneNsTtlDefault() {
    return MoreObjects.firstNonNull(HOSTEDZONE_NS_TTL_DEFAULT, 900);
  }

  public static List<String> getHostedZoneNsNamesDefault() {
    return Splitter.on(CharMatcher.anyOf(" ,"))
        .trimResults()
        .omitEmptyStrings()
        .splitToList(Strings.nullToEmpty(HOSTEDZONE_NS_NAMES_DEFAULT));
  }

  public static int getMaxTags() {
    return MoreObjects.firstNonNull( MAX_TAGS, 10 );
  }
}
