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
 * along with this program.  If not, see http://www.gnu.org/licenses/.
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
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.InstanceCreatedEvent;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
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
      } );
      
      ListenerRegistry.getInstance( ).register( StorageEvent.class, new EventListener( ) {
        @Override
        public void fireEvent( Event event ) {
          if ( event instanceof StorageEvent ) {
            QueueSender sender = QueueFactory.getInstance( ).getSender( QueueIdentifier.STORAGE );
            sender.send( ( com.eucalyptus.reporting.event.Event ) event );
          }
        }
      } );

      ListenerRegistry.getInstance( ).register( InstanceCreatedEvent.class, new EventListener<InstanceCreatedEvent>( ) {
        @Override
        public void fireEvent( InstanceCreatedEvent event ) {
          log.info( "Instance created event: " + event );
        }
      } );

      ListenerRegistry.getInstance( ).register( InstanceEvent.class, new EventListener<InstanceEvent>( ) {
        @Override
        public void fireEvent( InstanceEvent event ) {
          log.info( "Instance usage event: " + event );
        }
      } );

      ListenerRegistry.getInstance( ).register( ResourceAvailabilityEvent.class, new EventListener<ResourceAvailabilityEvent>( ) {
        @Override
        public void fireEvent( ResourceAvailabilityEvent event ) {
          log.info( "Resource event: " + event );
        }
      } );

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
