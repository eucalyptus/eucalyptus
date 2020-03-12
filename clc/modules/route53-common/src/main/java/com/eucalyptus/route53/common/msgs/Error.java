/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Error extends EucalyptusData {

  private String type;

  private String code;

  private String message;

  private ErrorDetail detail;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ErrorDetail getDetail() {
    return detail;
  }

  public void setDetail(ErrorDetail detail) {
    this.detail = detail;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
