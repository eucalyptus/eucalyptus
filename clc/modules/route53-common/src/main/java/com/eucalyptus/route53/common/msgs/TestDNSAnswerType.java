/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/testdnsanswer")
@HttpNoContent
public class TestDNSAnswerType extends Route53Message {

  @HttpParameterMapping(parameter = "edns0clientsubnetip")
  @FieldRange(max = 45)
  private String eDNS0ClientSubnetIP;

  @HttpParameterMapping(parameter = "edns0clientsubnetmask")
  @FieldRange(max = 3)
  private String eDNS0ClientSubnetMask;

  @Nonnull
  @HttpParameterMapping(parameter = "hostedzoneid")
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  @HttpParameterMapping(parameter = "recordname")
  @FieldRange(max = 1024)
  private String recordName;

  @Nonnull
  @HttpParameterMapping(parameter = "recordtype")
  @FieldRegex(FieldRegexValue.ENUM_RRTYPE)
  private String recordType;

  @HttpParameterMapping(parameter = "resolverip")
  @FieldRange(max = 45)
  private String resolverIP;

  public String getEDNS0ClientSubnetIP() {
    return eDNS0ClientSubnetIP;
  }

  public void setEDNS0ClientSubnetIP(final String eDNS0ClientSubnetIP) {
    this.eDNS0ClientSubnetIP = eDNS0ClientSubnetIP;
  }

  public String getEDNS0ClientSubnetMask() {
    return eDNS0ClientSubnetMask;
  }

  public void setEDNS0ClientSubnetMask(final String eDNS0ClientSubnetMask) {
    this.eDNS0ClientSubnetMask = eDNS0ClientSubnetMask;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getRecordName() {
    return recordName;
  }

  public void setRecordName(final String recordName) {
    this.recordName = recordName;
  }

  public String getRecordType() {
    return recordType;
  }

  public void setRecordType(final String recordType) {
    this.recordType = recordType;
  }

  public String getResolverIP() {
    return resolverIP;
  }

  public void setResolverIP(final String resolverIP) {
    this.resolverIP = resolverIP;
  }

}
