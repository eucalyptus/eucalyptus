/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.autoscaling.common.msgs;

import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import java.util.Collection;

public class TargetGroupArns extends EucalyptusData {

  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.ELB_TARGETGROUPARN )
  @HttpParameterMapping( parameter = "member" )
  private ArrayList<String> member = new ArrayList<String>( );

  public TargetGroupArns( ) {
  }

  public TargetGroupArns( Collection<String> arns ) {
    if ( arns != null ) member.addAll( arns );
  }

  public ArrayList<String> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<String> member ) {
    this.member = member;
  }
}