/*
 * Copyright 2009-$year Eucalyptus Systems, Inc.
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

package com.eucalyptus.stats;

import com.eucalyptus.stats.emitters.FileSystemEmitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HierarchicalFilesystemEventEmitterTest {
    private static String path = "unittesting" + UUID.randomUUID().toString();

    @BeforeClass
    public static void setUp() {
        System.setProperty("euca.run.dir", path);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //Recursive delete test dir
        System.out.println("Cleaning up files in path: " + path);
        recursiveDelete(Paths.get(path));
    }

    protected static void recursiveDelete(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            try (DirectoryStream<Path> dir = Files.newDirectoryStream(p)) {
                for (Path child : dir) {
                    recursiveDelete(child);
                }
            }
        }
        Files.delete(p);
    }


    @Test
    public void testSubmitEvent() throws Exception {
        FileSystemEmitter emitter = new FileSystemEmitter();
        List<String> tags = Lists.asList("tag1", "tag2", new String[]{"tag3"});
        Map<String, Object> testMap = Maps.newHashMap();
        testMap.put("fakekeyString", "fakeresult1");
        testMap.put("fakekeyInteger", 100l);
        testMap.put("fakekeyDouble", 150.505050d);

        long time;
        for (int i = 0; i < 100; i++) {
            System.out.println("Emitting event");
            time = System.nanoTime();
            assert (emitter.emit(new SystemMetric("eucalyptus.testservice" + i,
                    tags,
                    "testdescription" + i,
                    testMap,
                    System.currentTimeMillis(),
                    120)));
            time = System.nanoTime() - time;
            System.out.println("Took: " + time + "ns");
        }
    }

    @Test
    public void testEventPerformanceSmallDepth() throws Exception {
        FileSystemEmitter emitter = new FileSystemEmitter();
        List<String> tags = Lists.asList("tag1", "tag2", new String[]{"tag3"});
        Map<String, Object> testMap = Maps.newHashMap();
        testMap.put("state", "ok");
        testMap.put("test-check", "passed");
        long time;
        int iterCount = 10000;
        long[] data = new long[iterCount];

        for (int i = 0; i < iterCount; i++) {
            data[i] = System.nanoTime();
            emitter.emit(new SystemMetric("eucalyptus.testlevel" + i % 10 + ".testlevel" + i % 100 + ".testservice" + i,
                    tags,
                    "testdescription" + i,
                    testMap,
                    System.currentTimeMillis(),
                    120));
            data[i] = System.nanoTime() - data[i];
        }

        Arrays.sort(data);
        double mean = 0;
        for (int i = 0; i < iterCount; i++) {
            mean += data[i];
        }
        mean = mean / iterCount;
        double median = 0d;
        median = ((double) data[iterCount / 2 - 1] + (double) data[iterCount / 2 + 1]) / 2;
        System.out.println("Min: " + data[0] + " Max: " + data[iterCount - 1] + " Median: " + median + " Mean: " + mean);
    }
}