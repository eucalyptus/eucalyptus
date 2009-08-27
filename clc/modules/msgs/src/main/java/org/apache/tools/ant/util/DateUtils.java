/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package org.apache.tools.ant.util;

import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper methods to deal with date/time formatting with a specific
 * defined format (<a href="http://www.w3.org/TR/NOTE-datetime">ISO8601</a>)
 * or a plurialization correct elapsed time in minutes and seconds.
 *
 * @since Ant 1.5
 *
 */
public final class DateUtils {

    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 60;
    private static final int TEN = 10;
    /**
     * ISO8601-like pattern for date-time. It does not support timezone.
     *  <tt>yyyy-MM-ddTHH:mm:ss</tt>
     */
    public static final String ISO8601_DATETIME_PATTERN
            = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * ISO8601-like pattern for date. <tt>yyyy-MM-dd</tt>
     */
    public static final String ISO8601_DATE_PATTERN
            = "yyyy-MM-dd";

    /**
     * ISO8601-like pattern for time.  <tt>HH:mm:ss</tt>
     */
    public static final String ISO8601_TIME_PATTERN
            = "HH:mm:ss";

    /**
     * Format used for SMTP (and probably other) Date headers.
     */
    public static final DateFormat DATE_HEADER_FORMAT
        = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);


// code from Magesh moved from DefaultLogger and slightly modified
    private static final MessageFormat MINUTE_SECONDS
            = new MessageFormat("{0}{1}");

    private static final double[] LIMITS = {0, 1, 2};

    private static final String[] MINUTES_PART = {"", "1 minute ", "{0,number} minutes "};

    private static final String[] SECONDS_PART = {"0 seconds", "1 second", "{1,number} seconds"};

    private static final ChoiceFormat MINUTES_FORMAT =
            new ChoiceFormat(LIMITS, MINUTES_PART);

    private static final ChoiceFormat SECONDS_FORMAT =
            new ChoiceFormat(LIMITS, SECONDS_PART);

    static {
        MINUTE_SECONDS.setFormat(0, MINUTES_FORMAT);
        MINUTE_SECONDS.setFormat(1, SECONDS_FORMAT);
    }

    /** private constructor */
    private DateUtils() {
    }


    /**
     * Format a date/time into a specific pattern.
     * @param date the date to format expressed in milliseconds.
     * @param pattern the pattern to use to format the date.
     * @return the formatted date.
     */
    public static String format(long date, String pattern) {
        return format(new Date(date), pattern);
    }


    /**
     * Format a date/time into a specific pattern.
     * @param date the date to format expressed in milliseconds.
     * @param pattern the pattern to use to format the date.
     * @return the formatted date.
     */
    public static String format(Date date, String pattern) {
        DateFormat df = createDateFormat(pattern);
        return df.format(date);
    }


    /**
     * Format an elapsed time into a plurialization correct string.
     * It is limited only to report elapsed time in minutes and
     * seconds and has the following behavior.
     * <ul>
     * <li>minutes are not displayed when 0. (ie: "45 seconds")</li>
     * <li>seconds are always displayed in plural form (ie "0 seconds" or
     * "10 seconds") except for 1 (ie "1 second")</li>
     * </ul>
     * @param millis the elapsed time to report in milliseconds.
     * @return the formatted text in minutes/seconds.
     */
    public static String formatElapsedTime(long millis) {
        long seconds = millis / ONE_SECOND;
        long minutes = seconds / ONE_MINUTE;
        Object[] args = {new Long(minutes), new Long(seconds % ONE_MINUTE)};
        return MINUTE_SECONDS.format(args);
    }

    /**
     * return a lenient date format set to GMT time zone.
     * @param pattern the pattern used for date/time formatting.
     * @return the configured format for this pattern.
     */
    private static DateFormat createDateFormat(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(gmt);
        sdf.setLenient(true);
        return sdf;
    }

    /**
     * Calculate the phase of the moon for a given date.
     *
     * <p>Code heavily influenced by hacklib.c in <a
     * href="http://www.nethack.org/">Nethack</a></p>
     *
     * <p>The Algorithm:
     *
     * <pre>
     * moon period = 29.53058 days ~= 30, year = 365.2422 days
     *
     * days moon phase advances on first day of year compared to preceding year
     *  = 365.2422 - 12*29.53058 ~= 11
     *
     * years in Metonic cycle (time until same phases fall on the same days of
     *  the month) = 18.6 ~= 19
     *
     * moon phase on first day of year (epact) ~= (11*(year%19) + 18) % 30
     *  (18 as initial condition for 1900)
     *
     * current phase in days = first day phase + days elapsed in year
     *
     * 6 moons ~= 177 days
     * 177 ~= 8 reported phases * 22
     * + 11/22 for rounding
     * </pre>
     *
     * @param cal the calander.
     *
     * @return The phase of the moon as a number between 0 and 7 with
     *         0 meaning new moon and 4 meaning full moon.
     *
     * @since 1.2, Ant 1.5
     */
    public static int getPhaseOfMoon(Calendar cal) {
        // CheckStyle:MagicNumber OFF
        int dayOfTheYear = cal.get(Calendar.DAY_OF_YEAR);
        int yearInMetonicCycle = ((cal.get(Calendar.YEAR) - 1900) % 19) + 1;
        int epact = (11 * yearInMetonicCycle + 18) % 30;
        if ((epact == 25 && yearInMetonicCycle > 11) || epact == 24) {
            epact++;
        }
        return (((((dayOfTheYear + epact) * 6) + 11) % 177) / 22) & 7;
        // CheckStyle:MagicNumber ON
    }

    /**
     * Returns the current Date in a format suitable for a SMTP date
     * header.
     * @return the current date.
     * @since Ant 1.5.2
     */
    public static String getDateForHeader() {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        int offset = tz.getOffset(cal.get(Calendar.ERA),
                                  cal.get(Calendar.YEAR),
                                  cal.get(Calendar.MONTH),
                                  cal.get(Calendar.DAY_OF_MONTH),
                                  cal.get(Calendar.DAY_OF_WEEK),
                                  cal.get(Calendar.MILLISECOND));
        StringBuffer tzMarker = new StringBuffer(offset < 0 ? "-" : "+");
        offset = Math.abs(offset);
        int hours = offset / (ONE_HOUR * ONE_MINUTE * ONE_SECOND);
        int minutes = offset / (ONE_MINUTE * ONE_SECOND) - ONE_HOUR * hours;
        if (hours < TEN) {
            tzMarker.append("0");
        }
        tzMarker.append(hours);
        if (minutes < TEN) {
            tzMarker.append("0");
        }
        tzMarker.append(minutes);
        return DATE_HEADER_FORMAT.format(cal.getTime()) + tzMarker.toString();
    }

    /**
     * Parse a string as a datetime using the ISO8601_DATETIME format which is
     * <code>yyyy-MM-dd'T'HH:mm:ss</code>
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the format.
     * @exception ParseException if the supplied string cannot be parsed by
     * this pattern.
     * @since Ant 1.6
     */
    public static Date parseIso8601DateTime(String datestr)
        throws ParseException {
        return new SimpleDateFormat(ISO8601_DATETIME_PATTERN).parse(datestr);
    }

    /**
     * Parse a string as a date using the ISO8601_DATE format which is
     * <code>yyyy-MM-dd</code>
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the format.
     * @exception ParseException if the supplied string cannot be parsed by
     * this pattern.
     * @since Ant 1.6
     */
    public static Date parseIso8601Date(String datestr) throws ParseException {
        return new SimpleDateFormat(ISO8601_DATE_PATTERN).parse(datestr);
    }

    /**
     * Parse a string as a date using the either the ISO8601_DATETIME
     * or ISO8601_DATE formats.
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the formats.
     * @exception ParseException if the supplied string cannot be parsed by
     * either of these patterns.
     * @since Ant 1.6
     */
    public static Date parseIso8601DateTimeOrDate(String datestr)
        throws ParseException {
        try {
            return parseIso8601DateTime(datestr);
        } catch (ParseException px) {
            return parseIso8601Date(datestr);
        }
    }
}
