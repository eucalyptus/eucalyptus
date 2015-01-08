/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

package com.eucalyptus.compute.conversion;

import static com.eucalyptus.auth.policy.PolicySpec.EC2_IMPORTINSTANCE;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_IMPORTVOLUME;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_CANCELCONVERSIONTASK;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_DESCRIBECONVERSIONTASKS;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_INSTANCE;
import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_VOLUME;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_EC2;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.imaging.ImagingServiceException;
import com.eucalyptus.imaging.ImagingTask;
import com.eucalyptus.imaging.VolumeImagingTask;
import com.eucalyptus.imaging.ImagingTasks;
import com.eucalyptus.imaging.ImportTaskState;
import com.eucalyptus.imaging.ImportInstanceImagingTask;
import com.eucalyptus.imaging.ImportVolumeImagingTask;
import com.eucalyptus.imaging.worker.ImagingServiceLaunchers;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.Collections;

import edu.ucsb.eucalyptus.msgs.CancelConversionTaskResponseType;
import edu.ucsb.eucalyptus.msgs.CancelConversionTaskType;
import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DescribeConversionTasksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeConversionTasksType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceType;
import edu.ucsb.eucalyptus.msgs.ImportVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

public class ImportManager {
  private static Logger    LOG                           = Logger.getLogger( ImportManager.class );  
  /**
   * <ol>
   * <li>Persist import instance request state
   * </ol>
   */
  public ImportInstanceResponseType ImportInstance( final ImportInstanceType request ) throws Exception {
    final ImportInstanceResponseType reply = request.getReply( );
    final Context context = Contexts.lookup( );
    try{
      if (!Bootstrap.isFinished() ||
           !Topology.isEnabled( Imaging.class )){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
      }
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
    }
    
    try{
      if (! Permissions.isAuthorized(
          VENDOR_EC2,
          EC2_RESOURCE_INSTANCE,
          "",
          null,
          EC2_IMPORTINSTANCE,
          context.getAuthContext() ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import instance." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import instance." );
    }
    
    try{
      if(ImagingServiceLaunchers.getInstance().shouldEnable())
        ImagingServiceLaunchers.getInstance().enable();
    }catch(Exception ex){
      LOG.error("Failed to enable imaging service workers");
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Could not launch imaging service workers");
    }
        
    ImportInstanceImagingTask task = null;
    try{
      task = ImagingTasks.createImportInstanceTask(request);
    }catch(final ImagingServiceException ex){
       throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to import instance", ex);
      throw new ImagingServiceException("Failed to import instance", ex);
    }
    reply.setConversionTask(task.getTask());
    reply.set_return(true);  
    return reply;
  }
  
  /**
   * <ol>
   * <li>Persist import volume request state
   * </ol>
   */
  public static ImportVolumeResponseType ImportVolume( ImportVolumeType request ) throws Exception {
    final ImportVolumeResponseType reply = request.getReply( );
    final Context context = Contexts.lookup( );
    try{
      if (!Bootstrap.isFinished() ||
           !Topology.isEnabled( Imaging.class )){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
      }
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "For import, Imaging service should be enabled");
    }
    
    try{
      if (! Permissions.isAuthorized(
          VENDOR_EC2,
          EC2_RESOURCE_VOLUME,
          "",
          null,
          EC2_IMPORTVOLUME,
          context.getAuthContext() ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import volume." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to import volume." );
    }
    
    try{
      if(ImagingServiceLaunchers.getInstance().shouldEnable())
        ImagingServiceLaunchers.getInstance().enable();
    }catch(Exception ex){
      LOG.error("Failed to enable imaging service workers");
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Could not launch imaging service workers");
    }

    ImportVolumeImagingTask task = null;
    try{
      task = ImagingTasks.createImportVolumeTask(request);
    }catch(final ImagingServiceException ex){
       throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to import volume", ex);
      throw new ImagingServiceException("Failed to import volume", ex);
    }
    reply.setConversionTask(task.getTask());
    reply.set_return(true);
    return reply;
  }
  
  /**
   * <ol>
   * <li>Persist cancellation request state
   * <li>Submit cancellations for any outstanding import sub-tasks to imaging service
   * </ol>
   */
  public CancelConversionTaskResponseType CancelConversionTask( CancelConversionTaskType request ) throws Exception {
    final CancelConversionTaskResponseType reply = request.getReply( );
    final Context context = Contexts.lookup( );
    try{
      if (! Permissions.isAuthorized(
          VENDOR_EC2,
          EC2_RESOURCE_VOLUME,
          "",
          null,
          EC2_CANCELCONVERSIONTASK,
          context.getAuthContext() ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to cancel conversion task." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to cancel conversion task." );
    }
    
    try{
      final ImagingTask task = ImagingTasks.lookup(request.getConversionTaskId());
      final ImportTaskState state = task.getState();
      if(state.equals(ImportTaskState.NEW) || 
          state.equals(ImportTaskState.PENDING) ||
          state.equals(ImportTaskState.CONVERTING) ||
          state.equals(ImportTaskState.INSTANTIATING) ) {
        ImagingTasks.setState(AccountFullName.getInstance(Contexts.lookup().getAccount()), request.getConversionTaskId(), 
            ImportTaskState.CANCELLING, ImportTaskState.STATE_MSG_USER_CANCELLATION);
      }
      reply.set_return(true);
    }catch(final NoSuchElementException ex){
      throw new ImagingServiceException(ImagingServiceException.DEFAULT_CODE, "Conversion task not found");
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Failed to cancel conversion task", ex);
    }
    return reply;
  }
  
  /**
   * <ol>
   * <li>Describe import tasks from persisted state
   * </ol>
   */
  public DescribeConversionTasksResponseType DescribeConversionTasks( DescribeConversionTasksType request ) throws Exception {
    DescribeConversionTasksResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    boolean verbose = request.getConversionTaskIdSet( ).remove( "verbose" );
    Collection<String> ownerInfo = ( ctx.isAdministrator( ) && verbose )
        ? Collections.<String> emptyList( )
            : Collections.singleton( ctx.getAccount( ).getAccountNumber( ) );
    try{
      if (! Permissions.isAuthorized(
          VENDOR_EC2,
          EC2_RESOURCE_VOLUME,
          "",
          null,
          EC2_DESCRIBECONVERSIONTASKS,
          ctx.getAuthContext() ) ) {
        throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to describe conversion tasks." );
      }
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException( ImagingServiceException.DEFAULT_CODE, "Not authorized to describe conversion tasks." );
    }

    final Predicate<? super VolumeImagingTask> requestedAndAccessible = RestrictedTypes.filteringFor( VolumeImagingTask.class )
        .byId( request.getConversionTaskIdSet( ) )
        .byOwningAccount( ownerInfo )
        .byPrivileges()
        .buildPredicate( );
    Iterable<VolumeImagingTask> tasksToList = 
        ImagingTasks.getVolumeImagingTasks();
    for ( VolumeImagingTask task : Iterables.filter( tasksToList, requestedAndAccessible ) ) {
      ConversionTask t = task.getTask( );
      reply.getConversionTasks().add( t );
    }
    return reply;
  }
}
