package com.eucalyptus.imaging;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

public class ImagingTasks {
  private static Logger    LOG                           = Logger.getLogger( ImagingTasks.class );
  
  public static VolumeImagingTask createImportVolumeTask(ImportVolumeType request) throws ImagingServiceException {
    final VolumeImagingTask transform = TypeMappers.transform( request, VolumeImagingTask.class );
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
      try{
        Entities.persist(transform);
        db.commit( );
      }catch(final Exception ex){
        throw new ImagingServiceException("Failed to persist VolumeImagingTask", ex);
      }
    }
    return transform;
      
      
      /*Callable<CreateVolumeResponseType> threadedCreate = new Callable<CreateVolumeResponseType >() {
        @Override
        public CreateVolumeResponseType call() throws Exception {
          final CreateVolumeResponseType volume = AsyncRequests.sendSync( Topology.lookup( Eucalyptus.class ), new CreateVolumeType() {
            {
              //GRZE:TODO: come back here and setup impersonation when the time is ready
              this.setSize( Integer.toString( task.getVolumeSize() ) );
              this.setAvailabilityZone( task.getAvailabilityZone() );
              try {
                this.setEffectiveUserId( Accounts.lookupAccountByName( "eucalyptus" ).lookupAdmin().getUserId() );
              } catch ( AuthException ex ) {
                LOG.error( ex );
              }

            }
          } );
          Callback<VolumeImagingTask> update = new Callback<VolumeImagingTask>() {
            @Override
            public void fire( VolumeImagingTask input ) {
              input.setVolumeId( volume.getVolume().getVolumeId() );
            }
          };
          Transactions.each( ( VolumeImagingTask ) ImagingTasks.exampleWithId( task.getDisplayName() ), update );
          return volume;
        }
      };
      Threads.enqueue( Imaging.class, ImportManager.class, threadedCreate );
      reply.setConversionTask( task.getTask( ) );
      LOG.info( reply );
    } catch (final ImagingServiceException ex){
      throw ex;
    } catch ( Exception ex ) {
      throw ex;
    }*/
  }
  
  public static VolumeImagingTask getVolumeImagingTask(final OwnerFullName owner, final String taskId) 
      throws NoSuchElementException{
    VolumeImagingTask task = null;
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
      final VolumeImagingTask sample = VolumeImagingTask.named(owner, taskId);
      try{
        task = Entities.uniqueResult(sample);
      }catch(final TransactionException ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
    
    return task;
  } 
  
  public static List<ImagingTask> getImagingTasks(final OwnerFullName owner, final List<String> taskIdList){
    final List<ImagingTask> result = Lists.newArrayList();
    try ( final TransactionResource db =
        Entities.transactionFor( ImagingTask.class ) ) {
      final ImagingTask sample = ImagingTask.named(owner);
      final List<ImagingTask> tasks = Entities.query(sample, true);
      
      for(final ImagingTask candidate : tasks){
        if(taskIdList.contains(candidate.getDisplayName()))
          result.add(candidate);
      }
    }
    return result;
  }
  
  // return all imaging tasks in DB
  public static List<ImagingTask> getImagingTasks(){
    List<ImagingTask> result = Lists.newArrayList();
    try ( final TransactionResource db =
        Entities.transactionFor( ImagingTask.class ) ) {
      result = Entities.query(ImagingTask.named(), true);
    }
    return result;
  }
 
  
  private static List<? extends ImagingTask> list( ) throws TransactionException {
    return list( (List<String>) Collections.EMPTY_LIST );
  }
  
  private static List<? extends ImagingTask> list( List<String> taskIds ) throws TransactionException {
    return Transactions.filter( exampleWithId( null ), RestrictedTypes.filterById( taskIds ) );
  }
  
  private static <T extends ImagingTask> T lookup( String taskId ) throws TransactionException {
    return ( T ) Transactions.find( exampleWithId( taskId ) );
  }
  
  private static ImagingTask exampleWithId( String displayName ) {
    return new ImagingTask( displayName );
  }
  
  private static ImagingTask create( OwnerFullName owner, String displayName ) {
    return new ImagingTask( owner, displayName );
  }
  
}
