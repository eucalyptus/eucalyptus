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
import io.vavr.collection.Stream;

public class MetricCollectionTypes extends EucalyptusData {

  private ArrayList<MetricCollectionType> member = new ArrayList<MetricCollectionType>( );

  public MetricCollectionTypes( ) {
  }

  public MetricCollectionTypes( Collection<String> types ) {
    this.member = Stream.ofAll( types ).map( type -> {
      MetricCollectionType metricCollectionType = new MetricCollectionType( );
      metricCollectionType.setMetric( type );
      return metricCollectionType;
    } ).toJavaList( ArrayList::new );
  }

  public ArrayList<MetricCollectionType> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<MetricCollectionType> member ) {
    this.member = member;
  }
}
