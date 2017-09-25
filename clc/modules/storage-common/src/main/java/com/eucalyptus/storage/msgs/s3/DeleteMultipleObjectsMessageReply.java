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
package com.eucalyptus.storage.msgs.s3;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class DeleteMultipleObjectsMessageReply extends EucalyptusData {

  private ArrayList<DeleteMultipleObjectsEntryVersioned> deleted;
  private ArrayList<DeleteMultipleObjectsError> errors;

  public ArrayList<DeleteMultipleObjectsEntryVersioned> getDeleted( ) {
    return deleted;
  }

  public void setDeleted( ArrayList<DeleteMultipleObjectsEntryVersioned> deleted ) {
    this.deleted = deleted;
  }

  public ArrayList<DeleteMultipleObjectsError> getErrors( ) {
    return errors;
  }

  public void setErrors( ArrayList<DeleteMultipleObjectsError> errors ) {
    this.errors = errors;
  }
}
