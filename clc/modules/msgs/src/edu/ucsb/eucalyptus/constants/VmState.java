/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.constants;

import java.util.HashMap;
import java.util.Map;

public enum VmState {
  PENDING("pending",0),
  RUNNING("running",16),
  SHUTTING_DOWN("shutting-down",32),
  TERMINATED("terminated",48),
  BURIED("buried",64);
  private String name;
  private int code;

  VmState( final String name, final int code )
  {
    this.name = name;
    this.code = code;
  }

  public String getName()
  {
    return name;
  }

  public int getCode()
  {
    return code;
  }

public static class Mapper {
  private static Map<String,VmState> stateMap = getStateMap();
  private static Map<String,VmState> getStateMap()
  {
    Map<String,VmState> map = new HashMap<String,VmState>();
    map.put("Extant",VmState.RUNNING);
    map.put("Pending", VmState.PENDING);
    map.put("Teardown", VmState.SHUTTING_DOWN);
    return map;
  }
  public static VmState get( String stateName ) { return Mapper.stateMap.get( stateName );}
}
}

