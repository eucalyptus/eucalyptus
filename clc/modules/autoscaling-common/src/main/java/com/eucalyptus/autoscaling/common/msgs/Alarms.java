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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javaslang.collection.Stream;

public class Alarms extends EucalyptusData {

  private ArrayList<Alarm> member = new ArrayList<Alarm>( );

  public Alarms( ) {
  }

  public Alarms( Iterable<String> alarmArns ) {
    this.member = Stream.ofAll( alarmArns ).map( alarmArn -> {
      final int nameIndex = alarmArn.indexOf( ":alarm:" );
      final String alarmName = nameIndex > 0 ? alarmArn.substring( nameIndex + 7 ) : null;
      final Alarm alarm = new Alarm( );
      alarm.setAlarmARN( alarmArn );
      alarm.setAlarmName( alarmName );
      return alarm;
    } ).toJavaList( ArrayList::new );
  }

  public ArrayList<Alarm> getMember( ) {
    return member;
  }

  public void setMember( ArrayList<Alarm> member ) {
    this.member = member;
  }
}
