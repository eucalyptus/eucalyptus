/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 * aLong with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

/**
 * Wraps calls to TGT for handling timeouts and error codes
 * Cannot always trust TGT error codes for failure status. Most calls also check the error stream.
 *
 */
public class TGTWrapper {
	private static final String TGTADM = "tgtadm";
	final public static String  TGT_SERVICE_NAME = "tgtd";
	final private static Logger LOG = Logger.getLogger(TGTWrapper.class);
	final private static String ROOT_WRAP = StorageProperties.EUCA_ROOT_WRAPPER;

	private static final ReadWriteLock serviceLock = new ReentrantReadWriteLock();
	private static ExecutorService service; // do not access directly, use getExecutor / getExecutorWithInit
	final private static int RESOURCE_NOT_FOUND = 22; //The tgt return code for resource not found.
	
	// TODO define fault IDs in a enum
	private static final int TGT_HOSED = 2000;
	private static final int TGT_CORRUPTED = 2002;
	private static final Joiner JOINER = Joiner.on(" ").skipNulls();
	private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings().trimResults();
	private static final Pattern LUN_PATTERN = Pattern.compile("\\s*LUN:\\s*(\\d+)\\s*");
	private static final Pattern TARGET_PATTERN = Pattern.compile("\\s*Target\\s*(\\d+):\\s+(\\S+)\\s*");
	private static final Pattern RESOURCE_PATTERN = Pattern.compile("\\s*Backing store path:\\s*(\\S+)\\s*");
	
	public static class ResourceNotFoundException extends EucalyptusCloudException {
		private static final Long serialVersionUID = 1L;

		public ResourceNotFoundException() {
			super("Resource not found");
		}
		
		public ResourceNotFoundException(String resource) {
			super("Resource " + resource + " not found");
		}
	}

	public static class OperationFailedException extends EucalyptusCloudException {
		private static final Long serialVersionUID = 1L;
		
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
	public static void start( ) {
		serviceLock.writeLock( ).lock( );
		try {
			if ( service == null ) {
				service = Executors.newFixedThreadPool(10);
			}
		} finally {
			serviceLock.writeLock( ).unlock();
		}
	}

	public static void stop( ) {
		serviceLock.writeLock( ).lock( );
		try {
			ExecutorService toShutdown = service;
			service = null;
			toShutdown.shutdownNow( );
		} catch (Exception e) {
			LOG.warn("Unable to shutdown thread pool", e);
		} finally {
			serviceLock.writeLock( ).unlock( );
		}
	}

	private static ExecutorService getExecutor( ) {
		serviceLock.readLock( ).lock( );
		try {
			if ( service == null ) {
				throw new IllegalStateException( "Not started" );
			}
			return service;
		} finally {
			serviceLock.readLock( ).unlock( );
		}
	}

	private static ExecutorService getExecutorWithInit( ) {
		ExecutorService service;
		try {
			service = getExecutor( );
		} catch ( IllegalStateException e ) {
			start( );
			service = getExecutor( );
		}
		return service;
	}
		
	/**
	 * Separate thread to wait for {@link java.lang.Process Process} to 
	 * complete and return its exit value
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
	 * executeTGTs the specified tgt command in a separate process. 
	 * A {@link DirectStorageInfo#timeoutInMillis timeout} is enforced on 
	 * the process using {@link java.util.concurrent.ExecutorService ExecutorService} 
	 * framework. If the process does not complete with in the timeout, it is cancelled.
	 * 
	 * @param command
	 * @param timeout
	 * @return CommandOutput
	 * @throws EucalyptusCloudException
	 */
	private static CommandOutput execute(@Nonnull String[] command, @Nonnull Long timeout)
			throws EucalyptusCloudException, CallTimeoutException {
		try {			
			Integer returnValue = -999999;
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(command);
			StreamConsumer error = new StreamConsumer(process.getErrorStream());
			StreamConsumer output = new StreamConsumer(process.getInputStream());
			error.start();
			output.start();
			Callable<Integer> processMonitor = new ProcessMonitor(process);
			Future<Integer> processController = getExecutorWithInit( ).submit(processMonitor);
			try {
				returnValue = processController.get(timeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException tex) {
				String commandStr = buildCommand(command);
				LOG.error(commandStr + " timed out. Cancelling the process, logging a fault and exceptioning out");
				processController.cancel(true);
				Faults.forComponent(Storage.class).havingId(TGT_HOSED).withVar("component", "Storage Controller").withVar("timeout", Long.toString(timeout)).log();
				throw new CallTimeoutException("No response from the command " + commandStr + ". Process timed out after waiting for " + timeout + " milliseconds");
			}
			output.join();
			error.join();
			LOG.debug("TGTWrapper executed: " + JOINER.join(command) + "\n return=" + returnValue + "\n stdout=" + output.getReturnValue() + "\n stderr=" + error.getReturnValue());
			return new CommandOutput(returnValue, output.getReturnValue(), error.getReturnValue());
		} catch (CallTimeoutException e) {
			throw e;
		} catch (Exception ex) {
			throw new EucalyptusCloudException(ex);
		}
	}
	
	/**
	 * Calls exclusively for TGT commands, does some analysis of return code and error stream to send better exceptions
	 * @param command
	 * @param timeout
	 * @return
	 *
	 * @throws CallTimeoutException
	 * @throws OperationFailedException
	 * @throws ResourceNotFoundException
	 */
	private static CommandOutput executeTGT(@Nonnull String[] command, @Nonnull Long timeout) 
			throws OperationFailedException, CallTimeoutException, ResourceNotFoundException {
		CommandOutput output = null;
		try {
			output = execute(command, timeout);
		} catch(ResourceNotFoundException e) {
			throw e;
		} catch(CallTimeoutException e) {
			throw e;
		} catch(EucalyptusCloudException e) {
			throw new OperationFailedException(e);
		}
		
		if(output.returnValue == 22) {
			if(output.error.contains("target"))
				throw new ResourceNotFoundException("target");
			if(output.error.contains("account"))
				throw new ResourceNotFoundException("account");
			if(output.error.contains("logicalunit"))
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
	 * @param timeout
	 * @throws EucalyptusCloudException
	 */
	public static void precheckService(Long timeout) throws EucalyptusCloudException {		
		CommandOutput output = null;
		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--help" }, timeout);
		if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
			Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", 
					"Storage Controller").withVar("operation", "tgtadm --help").withVar("error", output.error).log();
			throw new EucalyptusCloudException("tgtadm not found: Is tgt installed?");
		}
		
		output = execute(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--mode", "target", "--op", "show"}, timeout);
		if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
			LOG.warn("Unable to connect to tgt daemon. Is tgtd loaded?");
			LOG.info("Attempting to start tgtd ISCSI daemon");
			output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "status" }, timeout);
			if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
				output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "start" }, timeout);
				if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
					Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", 
							"Storage Controller").withVar("operation", "service tgt start").withVar("error", output.error).log();
					throw new EucalyptusCloudException("Unable to start tgt daemon. Cannot proceed.");
				}
			} else {
				output = execute(new String[] { ROOT_WRAP, "service", "tgt", "start" } ,timeout);
				if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
					Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", 
							"Storage Controller").withVar("operation", "service tgt start").withVar("error", output.error).log();
					throw new EucalyptusCloudException("Unable to start tgt daemon. Cannot proceed.");
				}
			}
		}
	}
	
	/**
	 * Runs a service check on TGT.
	 * @param timeout
	 * @throws EucalyptusCloudException
	 */
	public static void checkService(Long timeout) throws EucalyptusCloudException {		
		try {
			CommandOutput output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "status" }, timeout);		
			if (StringUtils.isNotBlank(output.error)) {
				Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", 
						"Storage Controller").withVar("operation", "service tgt status").withVar("error", output.error).log();
				throw new EucalyptusCloudException("tgt service check failed with error: " + output.error);
			}
		} catch(CallTimeoutException e) {
			LOG.error("Call timed out checking service.", e);
			throw e;
		} catch(EucalyptusCloudException e) {
			LOG.error("Check service failed",e);
			throw e;
		}
	}
		
	/**
	 * Creates a new target with the given name and target ID
	 * @param volumeId
	 * @param tid
	 * @param name
	 * @param timeout
	 * @return true on success, false on failure
	 * @throws CallTimeoutException
	 * @throws EucalyptusCloudException
	 */
	public static void createTarget(@Nonnull String volumeId, int tid, @Nonnull String name, @Nonnull Long timeout) 
			throws CallTimeoutException, OperationFailedException, ResourceNotFoundException {
		CommandOutput output = executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "new", 
				"--mode", "target", "--tid", String.valueOf(tid), "-T", name }, timeout);
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
	public static void deleteTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout, boolean force) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException {
		LOG.debug("Tearing down target " + tid + " for volume " + volumeId);
		CommandOutput output = null;
		if(force) {
			output = executeTGT(new String[] { ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", 
					"--mode", "target", "--tid", String.valueOf(tid), "--force" }, timeout);			
		} else {
			output = executeTGT(new String[] { ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", 
					"--mode", "target", "--tid", String.valueOf(tid)}, timeout);
		}
		
		if (output.failed() || StringUtils.isNotBlank(output.error)) {			
			throw new OperationFailedException(output.output, output.error, output.returnValue);
		}
	}
	
	/**
	 * Creates a lun in the given target that is backed by the resource in resourcePath (file/block device)
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
		CommandOutput output = executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "new", 
				"--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun), "-b", resourcePath }, timeout);
		if (output.failed() || StringUtils.isNotBlank(output.error)) {
			if(output.returnValue == 22) {
				throw new ResourceNotFoundException(String.valueOf(tid));
			}
			throw new OperationFailedException("Create lun operation failed");
		}		
	}
	
	/**
	 * Removes the target from TGT, only operates on the given target
	 * @param volumeId
	 * @param tid
	 * @param lun
	 * @param timeout
	 */
	public static void deleteLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException {
		LOG.debug("Removing LUN " + lun + " from target " + tid + " for volume " + volumeId);
		CommandOutput output = executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "delete", 
				"--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun) }, timeout);
		if(output.failed() || StringUtils.isNotBlank(output.error)) {
			//If the logical unit is not found, return true, since it is not in the target.
			if(output.returnValue == RESOURCE_NOT_FOUND && output.error.contains("can't find the logical unit")) {
				LOG.debug("Volume: " + volumeId + " logical unit already removed.");				
			}
			throw new OperationFailedException("Delete lun operation failed");
		}
	}

	/**
	 * Binds a chap user/account to the target, requiring initiators to use chap to connect
	 * @param volumeId
	 * @param user
	 * @param tid
	 * @param timeout
	 * @throws ResourceNotFoundException
	 * @throws CallTimeoutException
	 * @throws EucalyptusCloudException
	 */
	public static void bindUser (@Nonnull String volumeId, @Nonnull String user, int tid, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException {
		CommandOutput output = executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "bind", 
				"--mode", "account", "--tid", String.valueOf(tid), "--user", user }, timeout);
		if (output.failed() || StringUtils.isNotBlank(output.error)) {
			if(output.returnValue == 22 && output.error.contains("account")) {
				throw new ResourceNotFoundException(user);
			} else if(output.returnValue == 22 && output.error.contains("target")) {
				throw new ResourceNotFoundException("Target " + String.valueOf(tid));
			}
			throw new OperationFailedException("Bind user operation failed");
		}
	}

	/**
	 * Binds the target to all host initiators (permits connections)
	 * @param volumeId
	 * @param tid
	 * @param timeout
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws CallTimeoutException
	 * @throws EucalyptusCloudException
	 */
	public static void bindTarget(@Nonnull String volumeId, int tid, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException {
		try {
			CommandOutput output = executeTGT(new String[] {ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", 
					"bind", "--mode", "target", "--tid", String.valueOf(tid), "-I", "ALL" }, timeout);
		} catch(EucalyptusCloudException e) {
			
		}
	}
	
	/**
	 * Unbinds all initiators from the given target.
	 * @param volumeId
	 * @param tid
	 * @param lun
	 * @param timeout
	 * @return
	 */
	public static void unbindTarget(String volumeId, int tid, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException  {
		LOG.debug("Unbinding target " + tid + " for volume " + volumeId);
		CommandOutput output = executeTGT (new String[]{ROOT_WRAP, TGTADM, "--lld", "iscsi", "--op", "unbind", 
				"--mode", "target", "--tid", String.valueOf(tid),  "-I", "ALL"} ,timeout);
		if (output.failed() || StringUtils.isNotBlank(output.error)) {
			if(output.returnValue == RESOURCE_NOT_FOUND && output.error.contains("can't find the target")) {
				LOG.debug("Volume: " + volumeId + " target not found, cannot unbind, returning unbind success.");
				throw new ResourceNotFoundException("target " + tid);
			}
			LOG.error("Volume: " + volumeId + " Unable to unbind tid: " + tid);
			throw new OperationFailedException(output.output, output.error, output.returnValue);
		}
	}
	
	/**
	 * Checks if the target exists. If resource is not null then returns true iff the target exists, and exports a lun backed
	 * by the specified resource. A LUN id check is not done, any LUN in the target will match.
	 * @param volumeId
	 * @param tid
	 * @param resource the backing resource. If null just the tid is checked, otherwise both the tid and resource are checked
	 * @param timeout
	 * @return
	 */
	public static boolean targetExists(@Nonnull String volumeId, int tid, String resource, @Nonnull Long timeout) 
			throws EucalyptusCloudException {
		try {
			CommandOutput output = executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", 
					"--mode", "target", "--tid", String.valueOf(tid) }, timeout);
			if (StringUtils.isBlank(output.error)) {
				if(resource != null) {
					output = executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", 
							"--mode", "target"}, timeout);					
					return hasResource(output.output, tid, resource);
				} else {
					LOG.debug("Volume " + volumeId + " check for target " + tid + " returning true. Target exists");
					return true;
				}
			} else {
				LOG.debug("Volume: " + volumeId + " Target: " + tid + " not found");
			}
		} catch(ResourceNotFoundException e) {
			//Fall through, return false
		} catch(EucalyptusCloudException e) {
			//This case includes call timeouts
			LOG.error("Caught unexpected exception checking for target existence for volume " + volumeId, e);
			throw e;
		}
		return false;
	}
	
	/**
	 * Returns true if target found had the requested lun
	 * @param volumeId
	 * @param tid
	 * @param lun
	 * @param timeout
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public static boolean targetHasLun(@Nonnull String volumeId, int tid, int lun, @Nonnull Long timeout) 
			throws EucalyptusCloudException {
		try {
			CommandOutput output = executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", 
					"--mode", "target", "--tid", String.valueOf(tid) }, timeout);
			output = executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", 
					"--mode", "target"}, timeout); 
			return hasLun(output.output, tid, lun);
		} catch(ResourceNotFoundException e) {
			//Fall through, return false
		} catch(EucalyptusCloudException e) {
			//This case includes call timeouts
			LOG.error("Caught unexpected exception checking for target existence for volume " + volumeId, e);
			throw e;
		}
		return false;
	}
	
	
	/**
	 * Check the output for the given resource string, but only if the tid matches
	 * @param output
	 * @param resource
	 * @return
	 */
	private static boolean hasResource(@Nonnull String output, int tid, @Nonnull String resource) {
		Matcher targetMatcher = null;
		Matcher resourceMatcher = null;
		String target = null;
		for(String line : LINE_SPLITTER.split(output)) {
			targetMatcher = TARGET_PATTERN.matcher(line);
			if(targetMatcher.matches()) {
				target = targetMatcher.group(1);
				if(Integer.parseInt(targetMatcher.group(1)) != tid) {					
					target = null;
					continue;
				}
			} else {
				if(target != null) {
					//Only try lun match if found the target we're looking for
					resourceMatcher = RESOURCE_PATTERN.matcher(line);
					if(resourceMatcher.matches() && resourceMatcher.group(1).equals(resource)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Check the output for the given lun
	 * @param output
	 * @param lun
	 * @param tid the target to look in
	 * @return
	 */
	private static boolean hasLun(@Nonnull String output, int tid, int lun) {		
		Matcher targetMatcher = null;
		Matcher lunMatcher = null;
		String target = null;
		for(String line : LINE_SPLITTER.split(output)) {
			targetMatcher = TARGET_PATTERN.matcher(line);
			if(targetMatcher.matches()) {
				target = targetMatcher.group(1);
				if(Integer.parseInt(targetMatcher.group(1)) != tid) {					
					target = null;
					continue;
				}
			} else {
				if(target != null) {
					//Only try lun match if found the target we're looking for
					lunMatcher = LUN_PATTERN.matcher(line);
					if(lunMatcher.matches() && lunMatcher.group(1).equals(String.valueOf(lun))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void addUser(@Nonnull String username, @Nonnull String password, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException  {
		executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "account", 
				"--user", username, "--password", password }, timeout);
	}

	public static void deleteUser(@Nonnull String username, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException  {
		executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "account", 
				"--user", username }, timeout);
	}

	public static boolean userExists(@Nonnull String username, @Nonnull Long timeout) 
			throws OperationFailedException, ResourceNotFoundException, CallTimeoutException  {
		CommandOutput output = executeTGT(new String[] { ROOT_WRAP, "tgtadm", "--op", "show", "--mode", "account" }, timeout);
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
