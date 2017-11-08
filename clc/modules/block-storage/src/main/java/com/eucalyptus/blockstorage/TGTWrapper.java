/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Faults;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

/**
 * Wraps calls to TGT for handling timeouts and error codes Cannot always trust TGT error codes for failure status. Most calls also check the error
 * stream.
 *
 */
public class TGTWrapper {
  private static final String TGTADM = "tgtadm";
  final public static String TGT_SERVICE_NAME = "tgtd";
  final private static Logger LOG = Logger.getLogger(TGTWrapper.class);
  final private static String ROOT_WRAP = StorageProperties.EUCA_ROOT_WRAPPER;

  private static final ReadWriteLock serviceLock = new ReentrantReadWriteLock();
  private static ExecutorService service; // do not access directly, use getExecutor / getExecutorWithInit
  final private static int RESOURCE_NOT_FOUND = 22; // The tgt return code for resource not found.

  // If you change the below INITIATOR_ACCESS_LIST, change the javadoc comments for bindTarget() and unbindTarget()
  final private static String INITIATOR_ACCESS_LIST = "ALL"; // Any initiator can access this target

  // TODO define fault IDs in a enum
  private static final int TGT_HOSED = 2000;
  private static final int TGT_CORRUPTED = 2002;
  private static final Joiner JOINER = Joiner.on(" ").skipNulls();
  private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings().trimResults();
  private static final Pattern LUN_PATTERN = Pattern.compile("\\s*LUN:\\s*(\\d+)\\s*");
  private static final Pattern TARGET_PATTERN = Pattern.compile("\\s*Target\\s*(\\d+):\\s+(\\S+)\\s*");
  private static final Pattern RESOURCE_PATTERN = Pattern.compile("\\s*Backing store path:\\s*(\\S+)\\s*");
  private static final Pattern USER_HEADER_PATTERN = Pattern.compile("\\s*Account information:\\s*");
  private static final Pattern INITIATORS_HEADER_PATTERN = Pattern.compile("\\s*ACL information:\\s*");
  private static final Pattern TRIMMED_ANYTHING_PATTERN = Pattern.compile("\\s*(\\S+)\\s*");

  public static class ResourceNotFoundException extends EucalyptusCloudException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException() {
      super("Resource not found");
    }

    public ResourceNotFoundException(String resource) {
      super("Resource " + resource + " not found");
    }
  }

  public static class OperationFailedException extends EucalyptusCloudException {
    private static final long serialVersionUID = 1L;

    private int errorCode = -1;
    private String outputContent = null;
    private String errorContent = null;

    public OperationFailedException() {
      super("TGT operation failed");
    }

    public OperationFailedException(String message) {
      super("TGT operation failed: " + message);
    }

    public OperationFailedException(String operation, String message) {
      super("TGT operation " + operation + " failed: " + message);
    }

    public OperationFailedException(String output, String errorOutput, int errorCode) {
      super("TGT operation failed");
      this.outputContent = output;
      this.errorCode = errorCode;
      this.errorContent = errorOutput;
    }

    public OperationFailedException(EucalyptusCloudException e) {
      super(e);
    }

    public int getErrorCode() {
      return errorCode;
    }

    public void setErrorCode(int errorCode) {
      this.errorCode = errorCode;
    }

    public String getOutputContent() {
      return outputContent;
    }

    public void setOutput(String output) {
      this.outputContent = output;
    }

    public String getErrorContent() {
      return errorContent;
    }

    public void setErrorContent(String errorOutput) {
      this.errorContent = errorOutput;
    }
  }

  public static class CallTimeoutException extends EucalyptusCloudException {
    private static final long serialVersionUID = 1L;

    public CallTimeoutException() {
      super("Call timed out");
    }

    public CallTimeoutException(String command) {
      super("Call timed for " + command);
    }
  }

  /**
   * Idempotent start
   */
  public static void start() {
    serviceLock.writeLock().lock();
    try {
      if (service == null) {
        service = Executors.newFixedThreadPool(10, Threads.threadFactory( "storage-tgt-pool-%d" ));
      }
    } finally {
      serviceLock.writeLock().unlock();
    }
  }

  public static void stop() {
    serviceLock.writeLock().lock();
    try {
      ExecutorService toShutdown = service;
      service = null;
      toShutdown.shutdownNow();
    } catch (Exception e) {
      LOG.warn("Unable to shutdown thread pool", e);
    } finally {
      serviceLock.writeLock().unlock();
    }
  }

  private static ExecutorService getExecutor() {
    serviceLock.readLock().lock();
    try {
      if (service == null) {
        throw new IllegalStateException("Not started");
      }
      return service;
    } finally {
      serviceLock.readLock().unlock();
    }
  }

  private static ExecutorService getExecutorWithInit() {
    ExecutorService service;
    try {
      service = getExecutor();
    } catch (IllegalStateException e) {
      start();
      service = getExecutor();
    }
    return service;
  }

  /**
   * Separate thread to wait for {@link java.lang.Process Process} to complete and return its exit value
   * 
   */
  public static class ProcessMonitor implements Callable<Integer> {
    private Process process;

    ProcessMonitor(Process process) {
      this.process = process;
    }

    public Integer call() throws Exception {
      process.waitFor();
      return process.exitValue();
    }
  }

  // Implementation of EUCA-3597
  /**
   * executeTGTs the specified tgt command in a separate process. A {@link DirectStorageInfo#timeoutInMillis timeout} is enforced on the process using
   * {@link java.util.concurrent.ExecutorService ExecutorService} framework. If the process does not complete with in the timeout, it is cancelled.
   * 
   * @param command
   * @param timeout
   * @return CommandOutput
   * @throws EucalyptusCloudException
   */
  private static CommandOutput execute(@Nonnull String[] command, @Nonnull Long timeout) throws EucalyptusCloudException, CallTimeoutException {
    try {
      Integer returnValue = -999999;
      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(command);
      StreamConsumer error = new StreamConsumer(process.getErrorStream());
      StreamConsumer output = new StreamConsumer(process.getInputStream());
      error.start();
      output.start();
      Callable<Integer> processMonitor = new ProcessMonitor(process);
      Future<Integer> processController = getExecutorWithInit().submit(processMonitor);
      try {
        returnValue = processController.get(timeout, TimeUnit.MILLISECONDS);
      } catch (TimeoutException tex) {
        String commandStr = buildCommand(command);
        LOG.error(commandStr + " timed out. Cancelling the process, logging a fault and exceptioning out");
        processController.cancel(true);
        Faults.forComponent(Storage.class).havingId(TGT_HOSED).withVar("component", "Storage Controller").withVar("timeout", Long.toString(timeout))
            .log();
        throw new CallTimeoutException("No response from the command " + commandStr + ". Process timed out after waiting for " + timeout
            + " milliseconds");
      }
      output.join();
      error.join();
      LOG.debug("TGTWrapper executed: " + JOINER.join(command) + "\n return=" + returnValue + "\n stdout=" + output.getReturnValue() + "\n stderr="
          + error.getReturnValue());
      return new CommandOutput(returnValue, output.getReturnValue(), error.getReturnValue());
    } catch (CallTimeoutException e) {
      throw e;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }
  }

  /**
   * Calls exclusively for TGT commands, does some analysis of return code and error stream to send better exceptions
   * 
   * @param command
   * @param timeout
   * @return
   *
   * @throws CallTimeoutException
   * @throws OperationFailedException
   * @throws ResourceNotFoundException
   */
  private static CommandOutput executeTGT(@Nonnull String[] command, @Nonnull Long timeout) throws OperationFailedException, CallTimeoutException,
      ResourceNotFoundException {
    CommandOutput output = null;
    try {
      output = execute(command, timeout);
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (CallTimeoutException e) {
      throw e;
    } catch (EucalyptusCloudException e) {
      throw new OperationFailedException(e);
    }

    if (output.returnValue == 22) {
      if (output.error.contains("target"))
        throw new ResourceNotFoundException("target");
      if (output.error.contains("account"))
        throw new ResourceNotFoundException("account");
      if (output.error.contains("logicalunit"))
        throw new ResourceNotFoundException("logicalunit");
      throw new ResourceNotFoundException();
    }
    return output;
  }

  private static String buildCommand(@Nonnull String[] command) {
    StringBuilder builder = new StringBuilder();
    for (String part : command) {
      builder.append(part).append(' ');
    }
    return builder.toString();
  }

  /**
   * Runs pre-checks for startup etc.
   * If we can run a tgtadm command that connects to the tgtd and
   * does not return an error, assume we are OK.
   * 
   * @param timeout
   * @throws EucalyptusCloudException
   */
  public static void precheckService(Long timeout) throws EucalyptusCloudException {
    CommandOutput output = null;
    output = execute(new String[] {ROOT_WRAP, "tgtadm", "--help"}, timeout);
    if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
      String errmsg = "Unable to run 'tgtadm --help' command " + 
          "(SCSI Target Administration Utility). " +
          "Is the scsi-target-utils package installed? " + 
          "Is the tgtd service running? " + 
          "Is /usr/sbin/tgtadm accessible? " + 
          "Error output: " + output.error; 
      LOG.warn(errmsg);
      Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller")
        .withVar("operation", "tgtadm --help")
        .withVar("error", output.error).log();
      throw new EucalyptusCloudException(errmsg);
    }
    checkService(timeout);
  }

  /**
   * Runs a service check on TGT.
   * If we can run a tgtadm command that connects to the tgtd and
   * does not return an error, assume we are OK.
   * 
   * @param timeout
   * @throws EucalyptusCloudException
   */
  public static void checkService(Long timeout) throws EucalyptusCloudException {
    CommandOutput output = null;
    output = execute(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--mode", "target", "--op", "show"}, timeout);
    if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
      String cmdline = "tgtadm --lld iscsi --mode target --op show";
      String errmsg = "Unable to run tgtadm command " + 
          "(SCSI Target Administration Utility). " +
          "Is the scsi-target-utils package installed? " + 
          "Is the tgtd service running? " + 
          "Is /usr/sbin/tgtadm accessible? " +
          "Attempted command: '" + cmdline + "' " +
          "Error output: " + output.error; 
      LOG.warn(errmsg);
      Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller")
        .withVar("operation", cmdline)
        .withVar("error", output.error).log();
      throw new EucalyptusCloudException(errmsg);
    }
  }

  /**
   * Creates a new target with the given name and target ID
   * 
   * @param volumeId
   * @param tid
   * @param name
   * @param timeout
   * @return true on success, false on failure
   * @throws CallTimeoutException
   * @throws EucalyptusCloudException
   */
  public static void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout) throws CallTimeoutException,
      OperationFailedException, ResourceNotFoundException {
    CommandOutput output =
        executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "new", "--mode", "target", "--tid", String.valueOf(tid), "-T", name},
            timeout);
    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      throw new OperationFailedException(output.output, output.error, output.returnValue);
    }
  }

  /**
   * Removes the target from TGT, only operates on the given target
   * 
   * @param volumeId
   * @param tid
   * @param timeout
   * @param force
   * @return
   * @throws EucalyptusCloudException
   */
  public static void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force) throws OperationFailedException,
      ResourceNotFoundException, CallTimeoutException {
    LOG.debug("Tearing down target " + tid + " for volume " + volumeId);
    CommandOutput output = null;
    if (force) {
      output =
          executeTGT(
              new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", "--mode", "target", "--tid", String.valueOf(tid), "--force"},
              timeout);
    } else {
      output =
          executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", "--mode", "target", "--tid", String.valueOf(tid)}, timeout);
    }

    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      throw new OperationFailedException(output.output, output.error, output.returnValue);
    }
  }

  /**
   * Creates a lun in the given target that is backed by the resource in resourcePath (file/block device)
   * 
   * @param volumeId
   * @param tid
   * @param lun
   * @param resourcePath
   * @param timeout
   * @throws ResourceNotFoundException
   * @throws CallTimeoutException
   * @throws EucalyptusCloudException
   */
  public static void createLun(@Nonnull String volumeId, int tid, int lun, @Nonnull String resourcePath, @Nonnull Long timeout)
      throws OperationFailedException, ResourceNotFoundException, CallTimeoutException {
    CommandOutput output =
        executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "new", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun",
            String.valueOf(lun), "-b", resourcePath}, timeout);
    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      if (output.returnValue == 22) {
        throw new ResourceNotFoundException(String.valueOf(tid));
      }
      throw new OperationFailedException("Create lun operation failed");
    }
  }

  /**
   * Removes the target from TGT, only operates on the given target
   * 
   * @param volumeId
   * @param tid
   * @param lun
   * @param timeout
   */
  public static void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws OperationFailedException,
      ResourceNotFoundException, CallTimeoutException {
    LOG.debug("Removing LUN " + lun + " from target " + tid + " for volume " + volumeId);
    CommandOutput output =
        executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", "--mode", "logicalunit", "--tid", String.valueOf(tid),
            "--lun", String.valueOf(lun)}, timeout);
    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      // If the logical unit is not found, return true, since it is not in the target.
      if (output.returnValue == RESOURCE_NOT_FOUND && output.error.contains("can't find the logical unit")) {
        LOG.debug("Volume: " + volumeId + " logical unit already removed.");
      }
      throw new OperationFailedException("Delete lun operation failed");
    }
  }

  /**
   * Binds a chap user/account to the target, requiring initiators to use chap to connect
   * 
   * @param volumeId
   * @param user
   * @param tid
   * @param timeout
   * @throws ResourceNotFoundException
   * @throws CallTimeoutException
   * @throws EucalyptusCloudException
   */
  public static void bindUser(@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout) throws OperationFailedException,
      ResourceNotFoundException, CallTimeoutException {
    CommandOutput output =
        executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "bind", "--mode", "account", "--tid", String.valueOf(tid), "--user",
            user}, timeout);
    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      if (output.returnValue == 22 && output.error.contains("account")) {
        throw new ResourceNotFoundException(user);
      } else if (output.returnValue == 22 && output.error.contains("target")) {
        throw new ResourceNotFoundException("Target " + String.valueOf(tid));
      }
      throw new OperationFailedException("Bind user operation failed");
    }
  }

  /**
   * Binds the target to all host initiators (permits connections)
   * 
   * @param volumeId
   * @param tid
   * @param timeout
   * @return
   * @throws ResourceNotFoundException
   * @throws CallTimeoutException
   * @throws EucalyptusCloudException
   */
  public static void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout) throws OperationFailedException, ResourceNotFoundException,
      CallTimeoutException {
    try {
      CommandOutput output =
          executeTGT(
              new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "bind", "--mode", "target", "--tid", String.valueOf(tid), "-I", INITIATOR_ACCESS_LIST},
              timeout);
    } catch (EucalyptusCloudException e) {

    }
  }

  /**
   * Unbinds all initiators from the given target.
   * 
   * @param volumeId
   * @param tid
   * @param lun
   * @param timeout
   * @return
   */
  public static void unbindTarget(String volumeId, int tid, @Nonnull Long timeout) throws OperationFailedException, ResourceNotFoundException,
      CallTimeoutException {
    LOG.debug("Unbinding target " + tid + " for volume " + volumeId);
    CommandOutput output =
        executeTGT(
            new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "unbind", "--mode", "target", "--tid", String.valueOf(tid), "-I", INITIATOR_ACCESS_LIST},
            timeout);
    if (output.failed() || StringUtils.isNotBlank(output.error)) {
      if (output.returnValue == RESOURCE_NOT_FOUND && output.error.contains("can't find the target")) {
        LOG.debug("Volume: " + volumeId + " target not found, cannot unbind, returning unbind success.");
        throw new ResourceNotFoundException("target " + tid);
      }
      LOG.error("Volume: " + volumeId + " Unable to unbind tid: " + tid);
      throw new OperationFailedException(output.output, output.error, output.returnValue);
    }
  }

  /**
   * Checks if the target exists, and optionally checks the resource for that target too. 
   * 
   * @param volumeId the volume ID, e.g. "vol-de174154"
   * @param tid the target ID, e.g. 32
   * @param resource the backing resource, e.g. "/dev/euca-ebs-storage-vg-vol-de174154/euca-vol-de174154". May be null.
   * @param timeout timeout in milliseconds for each tgtadm command to complete
   * @return If a target with the given tid exists, and the given resource is null, returns true.
   *         If the resource is not null, and that resource must be backing a LUN of that target, returns true.
   *         A LUN ID check is not done, any LUN number in the target will match.
   *         Otherwise returns false.
   */
  public static boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout) throws EucalyptusCloudException {
    // Don't check for user nor initiator list
    return targetConfigured(volumeId, tid, resource, timeout, null, false);
  }
  
  /**
   * Checks if the target exists, and optionally checks its configuration. 
   * 
   * @param volumeId the volume ID, e.g. "vol-de174154"
   * @param tid the target ID, e.g. 32
   * @param resource the backing resource, e.g. "/dev/euca-ebs-storage-vg-vol-de174154/euca-vol-de174154". May be null.
   * @param timeout timeout in milliseconds for each tgtadm command to complete
   * @param user the account user that must be bound to that target. May be null.
   * @param checkInitiators if true, check that the access list (ACL) of initiators is correct. If false, don't check.
   * @return If a target with the given tid exists, and the given resource is null, returns true.
   *         If the resource is not null, and that resource must be backing a LUN of that target, returns true
   *         if the supplied user is the user bound to that target and the initiators list is correct, skipping
   *         either check if they are null/false.
   *         A LUN ID check is not done, any LUN number in the target will match.
   *         Otherwise returns false.
   */
  public static boolean targetConfigured(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout, 
      String user, boolean checkInitiators) throws EucalyptusCloudException {
    try {
      CommandOutput output =
          executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid)}, timeout);
      if (StringUtils.isBlank(output.error)) {
        if (resource != null) {
          output = executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target"}, timeout);
          if (hasResource(output.output, tid, resource, user, checkInitiators)) {
            LOG.debug("Volume " + volumeId + " check for target " + tid + " and resource " + resource + " returning true. Target exists");
            return true;
          } else {
            LOG.debug("Volume: " + volumeId + " Target: " + tid + " Resource: " + resource + 
                (user == null ? "" : " User: " + user) +
                (checkInitiators ? " Initiators: " + INITIATOR_ACCESS_LIST : "") +
                " not found");
            return false;
          }
        } else {
          LOG.debug("Volume " + volumeId + " check for target " + tid + " returning true. Target exists");
          return true;
        }
      } else {
        LOG.debug("Volume: " + volumeId + " Target: " + tid + " not found");
        return false;
      }
    } catch (ResourceNotFoundException e) {
      // Fall through, return false
    } catch (EucalyptusCloudException e) {
      // This case includes call timeouts
      LOG.error("Caught unexpected exception checking for target existence for volume " + volumeId, e);
      throw e;
    }
    return false;
  }

  /**
   * Returns true if target found had the requested lun
   * 
   * @param volumeId
   * @param tid
   * @param lun
   * @param timeout
   * @return
   * @throws EucalyptusCloudException
   */
  public static boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) throws EucalyptusCloudException {
    try {
      CommandOutput output =
          executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid)}, timeout);
      output = executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target"}, timeout);
      return hasLun(output.output, tid, lun);
    } catch (ResourceNotFoundException e) {
      // Fall through, return false
    } catch (EucalyptusCloudException e) {
      // This case includes call timeouts
      LOG.error("Caught unexpected exception checking for target existence for volume " + volumeId, e);
      throw e;
    }
    return false;
  }

  /**
   * Check the output for the given resource string, but only if the tid matches
   * 
   * @param output
   * @param resource
   * @return
   */
  private static boolean hasResource(@Nonnull String output, int tid, @Nonnull String resource, String user, boolean checkInitiators) {
    Matcher targetMatcher = null;
    Matcher resourceMatcher = null;
    Matcher userHeaderMatcher = null;
    Matcher userMatcher = null;
    Matcher initiatorsHeaderMatcher = null;
    Matcher initiatorsMatcher = null;
    String target = null;
    boolean resourceFound = false;
    boolean userHeaderFound = false;
    boolean userFound = false;
    boolean initiatorsHeaderFound = false;
    boolean initiatorsFound = false;
    
    for (String line : LINE_SPLITTER.split(output)) {
      // Look for a target ID line even after we find ours. This ensures that if we
      // don't find what we're looking for before the next target ID line, we won't 
      // find it in a later target because we'll break out at the next target ID.
      targetMatcher = TARGET_PATTERN.matcher(line);
      if (targetMatcher.matches()) {
        if (target == null) {
          if (Integer.parseInt(targetMatcher.group(1)) == tid) {
            // Found the target
            target = targetMatcher.group(1);
          }
        } else {
          // We already found the target, so this is the next target. 
          // So we never found what we were looking for in the target we wanted. Get out.
          break;
        }
        continue;
      }

      if (target == null) {
        // No reason to keep looking at this line
        continue;
      }
      
      if (!resourceFound) {
        // Found the target already, looking for the resource (LUN)
        resourceMatcher = RESOURCE_PATTERN.matcher(line);
        if (resourceMatcher.matches() && resourceMatcher.group(1).equals(resource)) {
          resourceFound = true;
        }
        continue;
      }
      
      // We've already found the target and its LUN.

      // Only check the target's user account if the user parameter is provided
      if (user != null && !userFound) {
        if (!userHeaderFound) {
          // Looking for the account user header line
          userHeaderMatcher = USER_HEADER_PATTERN.matcher(line);
          if (userHeaderMatcher.matches()) {
            userHeaderFound = true;
            continue;
          }
          // Not the header line, fall through to look for initiators
        } else {
          // We found the user account header already, so this line better be the user
          userMatcher = TRIMMED_ANYTHING_PATTERN.matcher(line);
          if (userMatcher.matches()) {
            if (userMatcher.group(1).equals(user)) {
              userFound = true;
              // Fall through to the final check
            } else {
              // The user is not the right one, fail.
              return false;
            }
          } else {
            // Couldn't match anything in the line, so no user.
            return false; 
          }
        }
      }  // end if looking for user
      
      // Only try to match the target's initiator list if the parameter is provided
      if (checkInitiators && !initiatorsFound) {
        if (!initiatorsHeaderFound) {
          // Looking for the initiators header line
          initiatorsHeaderMatcher = INITIATORS_HEADER_PATTERN.matcher(line);
          if (initiatorsHeaderMatcher.matches()) {
            initiatorsHeaderFound = true;
            continue;
          }
          // Not the header line, fall through to the final check
        } else {
          // We found the initiators header already, so this line better be the initiator list
          initiatorsMatcher = TRIMMED_ANYTHING_PATTERN.matcher(line);
          if (initiatorsMatcher.matches()) {
            if (initiatorsMatcher.group(1).equals(INITIATOR_ACCESS_LIST)) {
              initiatorsFound = true;
              // Fall through to the final check
            } else {
              // The initiators list is not right, fail.
              return false;
            }
          } else {
            // Couldn't match anything in the line, so no initiators list.
            return false; 
          }
        }
      }  // end if looking for initiators

      if ((user == null || userFound) &&
          (!checkInitiators || initiatorsFound)) {
        // Found what we needed, we're done
        return true;
      }
    }  // end for each line in the tgtadm output
    // Never found what we were looking for
    return false;
  } // end hasResource()

  /**
   * Check the output for the given lun
   * 
   * @param output
   * @param lun
   * @param tid the target to look in
   * @return
   */
  private static boolean hasLun(@Nonnull String output, int tid, int lun) {
    Matcher targetMatcher = null;
    Matcher lunMatcher = null;
    String target = null;
    for (String line : LINE_SPLITTER.split(output)) {
      // Look for a target ID line even after we find ours. This ensures that if we
      // don't find what we're looking for before the next target ID line, we won't 
      // find it in a later target because we'll break out at the next target ID.
      targetMatcher = TARGET_PATTERN.matcher(line);
      if (targetMatcher.matches()) {
        if (target == null) {
          if (Integer.parseInt(targetMatcher.group(1)) == tid) {
            // Found the target
            target = targetMatcher.group(1);
          }
        } else {
          // We already found the target, so this is the next target. 
          // So we never found what we were looking for in the target we wanted. Get out.
          break;
        }
        continue;
      }

      if (target == null) {
        // No reason to keep looking at this line
        continue;
      }
      // Only try lun match if found the target we're looking for
      lunMatcher = LUN_PATTERN.matcher(line);
      if (lunMatcher.matches() && lunMatcher.group(1).equals(String.valueOf(lun))) {
        return true;
      }
    }
    return false;
  }

  public static void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout) throws OperationFailedException,
      ResourceNotFoundException, CallTimeoutException {
    executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "account", "--user", username, "--password", password},
        timeout);
  }

  public static void deleteUser(@Nonnull String username, @Nonnull Long timeout) throws OperationFailedException, ResourceNotFoundException,
      CallTimeoutException {
    executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "account", "--user", username}, timeout);
  }

  public static boolean userExists(@Nonnull String username, @Nonnull Long timeout) throws OperationFailedException, ResourceNotFoundException,
      CallTimeoutException {
    CommandOutput output = executeTGT(new String[] {ROOT_WRAP, "tgtadm", "--op", "show", "--mode", "account"}, timeout);
    String returnValue = output.output;

    if (returnValue.length() > 0) {
      Pattern p = Pattern.compile(username);
      Matcher m = p.matcher(returnValue);
      if (m.find())
        return true;
      else
        return false;
    }
    return false;
  }

}
