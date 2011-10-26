package com.eucalyptus.reporting;

import java.util.Timer;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.reporting.modules.instance.InstanceEventListener;
import com.eucalyptus.reporting.modules.s3.S3EventListener;
import com.eucalyptus.reporting.modules.storage.StorageEventListener;
import com.eucalyptus.reporting.queue.QueueFactory;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.reporting.queue.QueueReceiver;
import com.eucalyptus.reporting.queue.QueueSender;
import com.eucalyptus.reporting.queue.mq.QueueBroker;

@Provides(Empyrean.class)//NOTE:GRZE: have the bootstrapper run earlier in bootstrap
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
public class ReportingBootstrapper
	extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

	private static long POLLER_DELAY_MS = 10000l;

	private StorageEventListener storageListener;
	private static InstanceEventListener instanceListener = null;
	private S3EventListener s3Listener;
	private QueueFactory queueFactory;
	private QueueBroker queueBroker;
	private Timer timer;


	public ReportingBootstrapper()
	{
		this.instanceListener = null;
	}

	@Override
	public boolean check()
	{
		return true;
	}

	@Override
	public void destroy()
	{
		return;
	}

	@Override
	public boolean enable()
	{
		return true;
	}

	@Override
	public boolean disable()
	{
		return true;
	}

	@Override
	public boolean load()
	{
		try {
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to load", ex);
			return false;
		}
	}



	@Override
	public boolean start()
	{
		try {


			/* Start queue broker
			 */
//			queueBroker = QueueBroker.getInstance();
//			queueBroker.startup();
//			log.info("Queue broker started");

			queueFactory = QueueFactory.getInstance();
			queueFactory.startup();

			/* Start storage receiver and storage queue poller thread
			 */
			QueueReceiver storageReceiver =
				queueFactory.getReceiver(QueueIdentifier.STORAGE);
			if (storageListener == null) {
				storageListener = new StorageEventListener();
				log.info("New storage listener instantiated");
			} else {
				log.info("Used existing storage listener");
			}
			storageReceiver.addEventListener(storageListener);

//			final StorageEventPoller poller = new StorageEventPoller(storageReceiver);
//			this.storagePoller = poller;
//			timer = new Timer(true);
//			timer.schedule(new TimerTask() {
//				@Override
//				public void run()
//				{
//					poller.writeEvents();
//				}
//			}, 0, POLLER_DELAY_MS);
			log.info("Storage queue poller started");

			/* Start instance receiver and instance listener
			 */
			QueueReceiver instanceReceiver =
				queueFactory.getReceiver(QueueIdentifier.INSTANCE);
			instanceReceiver.addEventListener(getInstanceListener());


			QueueReceiver s3Receiver =
				queueFactory.getReceiver(QueueIdentifier.S3);
			if (s3Listener == null) {
				s3Listener = new S3EventListener();
				log.info("New s3 listener instantiated");
			} else {
				log.info("Used existing s3 listener");
			}
			s3Receiver.addEventListener(s3Listener);

			ListenerRegistry.getInstance( ).register( InstanceEvent.class, new EventListener( ) {

        @Override
        public void fireEvent( Event event ) {
          if ( event instanceof InstanceEvent ) {
            QueueSender sender = QueueFactory.getInstance( ).getSender( QueueIdentifier.INSTANCE );
            sender.send( ( com.eucalyptus.reporting.event.Event ) event );
          }
        }
      }
                      );
      
      ListenerRegistry.getInstance( ).register( StorageEvent.class, new EventListener( ) {
        
        @Override
        public void fireEvent( Event event ) {
          if ( event instanceof StorageEvent ) {
            QueueSender sender = QueueFactory.getInstance( ).getSender( QueueIdentifier.STORAGE );
            sender.send( ( com.eucalyptus.reporting.event.Event ) event );
          }
        }
      }
                      );
			
			log.info("ReportingBootstrapper started");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("ReportingBootstrapper failed to start", ex);
			return false;
		}
	}

	public static void startTest()
	{
		ReportingBootstrapper bootstrapper = new ReportingBootstrapper();
		bootstrapper.load();
		bootstrapper.start();
		try {
			System.out.println("Sleeping for 60 secs");
			Thread.sleep(60000);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public boolean stop()
	{
		try {
			instanceListener.flush();
			timer.cancel();
			queueFactory.shutdown();
			queueBroker.shutdown();
			log.info("ReportingBootstrapper stopped");
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}

	public static InstanceEventListener getInstanceListener()
	{
		if (instanceListener == null)
			instanceListener = new InstanceEventListener();
		return instanceListener;
	}


}
