

package com.eucalyptus.objectstorage.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ObjectStorageManagerTest {

    @Test
    public void extractIntervalTest() throws Exception {
        String jobSchedule = "interval: 500000 ";
        jobSchedule = jobSchedule.substring( "interval:".length(), jobSchedule.length() );
        int intervalSecs = -1;
        try {
            intervalSecs = Integer.parseInt(jobSchedule.trim());
        }
        catch(NumberFormatException nfe) {
            assertTrue("failed to parse number", false);
        }
        assertTrue("expected parsing to result in 500000", intervalSecs == 500000);
    }

}
