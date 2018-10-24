/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Collection;
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeInstanceTypesType extends VmTypeMessage {

  private Boolean verbose = false;
  private Boolean availability = false;
  @HttpParameterMapping( parameter = "InstanceType" )
  private ArrayList<String> instanceTypes = new ArrayList<String>( );

  public DescribeInstanceTypesType( ) {
  }

  public DescribeInstanceTypesType( Collection<String> instanceTypes ) {
    this.instanceTypes.addAll( instanceTypes );
  }

  public Boolean getVerbose( ) {
    return verbose;
  }

  public void setVerbose( Boolean verbose ) {
    this.verbose = verbose;
  }

  public Boolean getAvailability( ) {
    return availability;
  }

  public void setAvailability( Boolean availability ) {
    this.availability = availability;
  }

  public ArrayList<String> getInstanceTypes( ) {
    return instanceTypes;
  }

  public void setInstanceTypes( ArrayList<String> instanceTypes ) {
    this.instanceTypes = instanceTypes;
  }
}
