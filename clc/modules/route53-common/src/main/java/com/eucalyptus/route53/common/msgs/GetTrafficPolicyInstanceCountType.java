/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpRequestMapping;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/trafficpolicyinstancecount")
@HttpNoContent
public class GetTrafficPolicyInstanceCountType extends Route53Message {

}
