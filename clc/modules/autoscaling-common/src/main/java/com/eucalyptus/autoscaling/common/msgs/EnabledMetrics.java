/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.autoscaling.common.msgs;

import java.util.ArrayList;
import java.util.Collection;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javaslang.collection.Stream;

public class EnabledMetrics extends EucalyptusData {

  private ArrayList<EnabledMetric> member = new ArrayList<EnabledMetric>( );

  public EnabledMetrics( ) {
  }

  public EnabledMetrics( Collection<String> enabledMetrics ) {
    if ( enabledMetrics != null )
      this.member = Stream.ofAll( enabledMetrics ).map( metric -> {
        EnabledMetric enabledMetric = new EnabledMetric( );
        enabledMetric.setMetric( metric );
        return enabledMetric;
      } ).toJavaList( ArrayList::new );
  }

  public ArrayList<EnabledMetric> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<EnabledMetric> member ) {
    this.member = member;
  }
}
