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

package com.eucalyptus.stats;

import com.eucalyptus.stats.emitters.FileSystemEmitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

//Don't run automatically, only in manual junit runs from ide, etc.
//Uses local file-system resources that may not be available in an automated CI system
@Ignore
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