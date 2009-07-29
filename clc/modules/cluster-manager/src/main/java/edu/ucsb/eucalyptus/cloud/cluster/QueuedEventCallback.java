package edu.ucsb.eucalyptus.cloud.cluster;
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

public abstract class QueuedEventCallback<TYPE> {

  private static Logger LOG = Logger.getLogger( QueuedEventCallback.class );

  protected AtomicBoolean stopped;
  private Lock canHas;
  private boolean e = false;
  private Condition jobPending;

  protected QueuedEventCallback()
  {
    this.stopped = new AtomicBoolean( false );
    this.canHas = new ReentrantLock();
    this.jobPending = canHas.newCondition();
  }

  protected void notifyHandler()
  {
    canHas.lock();
    e = true;
    this.jobPending.signalAll();
    canHas.unlock();
  }

  public void waitForEvent()
  {
    canHas.lock();
    try {
      while(!e)
        this.jobPending.await();
    } catch ( InterruptedException e ) {
    } finally {
      e = false;
      canHas.unlock();
    }
  }

  public boolean isStopped()
  {
    return stopped.get();
  }

  public void stop()
  {
    this.stopped.lazySet( true );
  }

  protected boolean sleep( int ms )
  {
    long startTime = System.currentTimeMillis();
    while ( ( System.currentTimeMillis() - startTime ) < ms )
      try
      {
        Thread.sleep( 500 );
      }
      catch ( InterruptedException e ) {}
    return true;
  }

  public abstract void process( Client cluster, TYPE msg ) throws Exception;

}
