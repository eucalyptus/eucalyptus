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

package com.eucalyptus.stats.emitters;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.stats.SystemMetric;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import org.apache.log4j.Logger;

import javax.annotation.Nonnull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An event emitter backed by local-host filesystem. Events are written to a filesystem with
 * a specific layout/format.
 * <p/>
 * Events are written in json with service names forming a directory tree using '.' replaced with '/' delimiters
 * in the name.
 * <p/>
 * Each output file is a json document owned by the eucalyptus user and @link{com.eucalyptus.monitoring.config.MonitoringConfiguration.statusGroupName} group has read permissions.
 */
public class FileSystemEmitter implements EventEmitter {
    private static final Logger LOG = Logger.getLogger(FileSystemEmitter.class);
    private static SubDirectory defaultFsRoot = FileSystemEmitterConfiguration.dataOutputFSRoot;
    private Path fsRoot;
    private boolean isEnabled = false;
    private static HashSet<Path> pathCache = new HashSet<Path>();

    public FileSystemEmitter() throws IOException {
        this(defaultFsRoot.toString());
    }

    public FileSystemEmitter(String rootPath) throws IOException {
        try {
            this.fsRoot = Paths.get(rootPath);
            FileSystemEmitterConfiguration.getGroup();
            isEnabled = true;
        } catch (Throwable e) {
            LOG.error("Eucalyptus stats event emitter cannot initialize because the user group " + FileSystemEmitterConfiguration.getStatusGroupName() + " is not found on the host. Please create the user group and restart the jvm process");
            isEnabled = false;
            throw e;
        }
    }

    public void check() throws Exception {
        try {
            FileSystemEmitterConfiguration.getGroup();
            isEnabled = true;
        } catch (Throwable e) {
            LOG.error("Eucalyptus stats event emitter cannot initialize because the user group " + FileSystemEmitterConfiguration.getStatusGroupName() + " is not found on the host. Please create the user group and restart the jvm process");
            isEnabled = false;
            throw e;
        }
    }

    @Override
    public boolean emit(SystemMetric event) {
        if (!isEnabled || event == null) {
            return false;
        }

        try {
            //The resulting file name
            String path = fsRoot + "/" + event.getSensor().replace('.', '/');
            Path filePath = FileSystems.getDefault().getPath(path);
            //Temp file to do the write, then a swap.
            Path tmpPath = FileSystems.getDefault().getPath(path + ".new");
            if ( !pathCache.contains(tmpPath) ) {
              try {
                  LOG.debug("Creating directories tree up to " +  filePath.getParent());
                  Files.createDirectories(filePath.getParent());
              } catch (Exception e) {
                  //cannot create parent paths of entry name
                  throw new RuntimeException("Could not create file path for sensor output. Path='" + path + "'");
              }
              // Set group permissions to directories
              Path c = filePath.getParent();
              while(!c.equals(fsRoot)) {
                  try {
                      LOG.debug("Checking dir " + c);
                      Files.getFileAttributeView(c, PosixFileAttributeView.class).setGroup(FileSystemEmitterConfiguration.getGroup());
                  } catch (Exception ex) {
                      LOG.error("Can't set group permission for " + c);
                  }
                  c = c.getParent();
              }
              pathCache.add(tmpPath);
            }
            tmpPath = Files.createFile(tmpPath, PosixFilePermissions.asFileAttribute(FileSystemEmitterConfiguration.getDataFilePermissions()));
            BufferedWriter fileOut = Files.newBufferedWriter(tmpPath, StandardCharsets.UTF_8);
            fileOut.write(event.toString());
            fileOut.close();
            try {
               Files.getFileAttributeView(tmpPath, PosixFileAttributeView.class).setGroup(FileSystemEmitterConfiguration.getGroup());
            } catch (Exception ex) {
               LOG.error("Can't set group permission for " + tmpPath + " Please make sure that " +
                   System.getProperty( "euca.user" ) + " user is part of " + FileSystemEmitterConfiguration.getGroup() + " group.");
            }
            Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception e) {
            LOG.error("Failed emitting event to file", e);
        }
        return false;
    }


    @Override
    public boolean doesBatching() {
        return false;
    }

    @ConfigurableClass(root="stats.file_system_emitter",description="Configuration for the stats emitter that sends data to local filesystem on host")
    static class FileSystemEmitterConfiguration {
        private static final String STATUS_USER_GROUP_PROPERTY_NAME = "euca.stats_group";
        private static final String STATUS_DATA_FILE_PERMS_PROPERTY_NAME = "euca.stats_data_perms";
        private static final String STATUS_USER_GROUP_PROPERTY_DEFAULT = "eucalyptus-status";
        private static final String DEFAULT_GROUP_PERM_STRING = "rw-r-----";

        @ConfigurableField(displayName = "stats group name", description="group name that owns stats data files", initial=STATUS_USER_GROUP_PROPERTY_DEFAULT)
        public static String stats_group_name = STATUS_USER_GROUP_PROPERTY_DEFAULT;

        @ConfigurableField(displayName = "stats_data_group_permissions", description = "group permissions to place on stats data files in string form. eg. rwxr-x--x", initial=DEFAULT_GROUP_PERM_STRING)
        public static String stats_data_permissions = DEFAULT_GROUP_PERM_STRING;

        public static SubDirectory dataOutputFSRoot = SubDirectory.STATUS; //Output directory

        public static String getStatusGroupName() {
            return System.getProperty(STATUS_USER_GROUP_PROPERTY_NAME, stats_group_name);
        }

        public static Set<PosixFilePermission> getDataFilePermissions() {
            return OUTPUT_FILE_PERMS_SUPPLIER.get();
        }

        public static GroupPrincipal getGroup() {
            return GROUP_SUPPLIER.get();
        }

        /**
         * Reloads new supplier instances to forcibly update to the latest value if needed. In-lieu of a cache-flush mechanism
         * for the memoized values
         */
        public static void forceConfigRefresh() {
            OUTPUT_FILE_PERMS_SUPPLIER = getOutputFilePermSupplier();
            GROUP_SUPPLIER = getGroupSupplier();
        }

        private static final Integer memoizationExpirationSeconds = 10; //Duration of memoization, max frequency of updates to make property changes effective for these configs. Ok because we don't expect them to change
        private static Supplier<Set<PosixFilePermission>> OUTPUT_FILE_PERMS_SUPPLIER = getOutputFilePermSupplier();
        private static Supplier<GroupPrincipal> GROUP_SUPPLIER = getGroupSupplier();

        private static Supplier<Set<PosixFilePermission>> getOutputFilePermSupplier() {
            return Suppliers.memoizeWithExpiration(new Supplier<Set<PosixFilePermission>>() {
                public Set<PosixFilePermission> get() {
                    //Always honor the local setting over global
                    return PosixFilePermissions.fromString(System.getProperty(STATUS_DATA_FILE_PERMS_PROPERTY_NAME, stats_data_permissions));
                }
            }, memoizationExpirationSeconds, TimeUnit.SECONDS);
        }

        private static Supplier<GroupPrincipal> getGroupSupplier() {
            return Suppliers.memoizeWithExpiration(new Supplier<GroupPrincipal>() {
                public GroupPrincipal get() {
                    //Always honor the local setting over global
                    try {
                        return verifyUserGroup(getStatusGroupName(), Paths.get(dataOutputFSRoot.toString()));
                    } catch (IOException e) {
                        LOG.error("Cannot use group name " + getStatusGroupName() + " for stats output. Invalid name or group not found", e);
                        throw Exceptions.toUndeclared("Invalid group name. Not found", e);
                    }
                }
            }, memoizationExpirationSeconds, TimeUnit.SECONDS);
        }

        private static GroupPrincipal verifyUserGroup(@Nonnull String groupName, @Nonnull Path fileSystemRoot) throws IOException {
            try {
                return fileSystemRoot.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(groupName);
            } catch (IOException e) {
                LOG.error("Could not get group information for user group " + FileSystemEmitterConfiguration.getStatusGroupName() + " from filesystem " + fileSystemRoot.toString(), e);
                throw e;
            }
        }
    }

}
