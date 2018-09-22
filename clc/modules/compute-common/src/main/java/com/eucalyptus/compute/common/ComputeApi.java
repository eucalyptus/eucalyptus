/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart(Compute.class)
public interface ComputeApi {

  ModifyInstanceTypeAttributeResponseType modifyInstanceType( ModifyInstanceTypeAttributeType request );
}
