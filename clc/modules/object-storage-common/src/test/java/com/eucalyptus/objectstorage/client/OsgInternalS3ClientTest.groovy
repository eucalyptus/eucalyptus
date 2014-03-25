/*
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 */

package com.eucalyptus.objectstorage.client

import com.amazonaws.auth.BasicAWSCredentials
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.junit.Ignore
import org.junit.Test

/**
 * Created by zhill on 3/24/14.
 *
 * Manual performance testing for client instantiation
 */
@Ignore("Manual development test")
class OsgInternalS3ClientTest {

    @Test
    void perfTestClientConstruction() {
        //Ensure logging is off.
        Logger rootLogger = Logger.getRootLogger()
        rootLogger.setLevel(Level.FATAL)

        int iterations = 100000
        println("Doing " + iterations + " s3 client instantiations to get latency measurements")

        long[] timing = new long[iterations]
        OsgInternalS3Client client
        BasicAWSCredentials fakeCreds = new BasicAWSCredentials("fakeaccesskey", "fakesecretkey")

        for (int i = 0; i < iterations ; i++) {
            timing[i] = System.nanoTime()
            client = new OsgInternalS3Client(fakeCreds, false)
            timing[i] = System.nanoTime() - timing[i]
            client = null
        }

        Arrays.sort(timing)

        //Show basics on timing.
        long mean = 0;
        for (int i = 0; i < iterations ; i++) {
            mean += timing[i]
            //println("Value: " + timing[i] + " ns")
        }

        mean = Math.ceil((double)mean / (double)iterations)
        println("Mean = " + (mean/1000) + "us")
        int onePercentileIndex = Math.floor((double)iterations * 0.01)
        println("Median = " + (timing[iterations / 2]/1000) + "us")
        println("99th percentile = " + (timing[iterations - onePercentileIndex - 1]/1000)+ "us")
        println("1st percentile = " + (timing[onePercentileIndex]/1000) + "us")

    }
}
