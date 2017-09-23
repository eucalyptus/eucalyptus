/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.simplequeue.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SendMessageBatchResult extends EucalyptusData {

  private ArrayList<SendMessageBatchResultEntry> sendMessageBatchResultEntry = new ArrayList<SendMessageBatchResultEntry>( );
  private ArrayList<BatchResultErrorEntry> batchResultErrorEntry = new ArrayList<BatchResultErrorEntry>( );

  public ArrayList<SendMessageBatchResultEntry> getSendMessageBatchResultEntry( ) {
    return sendMessageBatchResultEntry;
  }

  public void setSendMessageBatchResultEntry( ArrayList<SendMessageBatchResultEntry> sendMessageBatchResultEntry ) {
    this.sendMessageBatchResultEntry = sendMessageBatchResultEntry;
  }

  public ArrayList<BatchResultErrorEntry> getBatchResultErrorEntry( ) {
    return batchResultErrorEntry;
  }

  public void setBatchResultErrorEntry( ArrayList<BatchResultErrorEntry> batchResultErrorEntry ) {
    this.batchResultErrorEntry = batchResultErrorEntry;
  }
}
