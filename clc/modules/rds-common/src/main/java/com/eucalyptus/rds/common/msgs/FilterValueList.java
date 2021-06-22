/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class FilterValueList extends EucalyptusData {

  @HttpParameterMapping(parameter = "Value")
  @FieldRegex(FieldRegexValue.ESTRING_1024)
  private ArrayList<String> member = new ArrayList<>();

  public ArrayList<String> getMember() {
    return member;
  }

  public void setMember(final ArrayList<String> member) {
    this.member = member;
  }
}
