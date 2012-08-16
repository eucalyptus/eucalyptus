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
 ************************************************************************/

/* Constats */
var RETURN_KEY_CODE = 13;
var RETURN_MAC_KEY_CODE = 10;
var BACKSPACE_KEY_CODE = 8;

var DOM_BINDING = {header:'.euca-container .euca-header-container .inner-container',
                   main:'.euca-main-outercontainer .inner-container #euca-main-container',
                   notification:'.euca-container .euca-notification-container .inner-container #euca-notification',
                   explorer:'.euca-explorer-container .inner-container',
                   footer:'.euca-container .euca-footer-container .inner-container',
                  };

function escapeHTML(input) {
  return $('<div/>').text(input).html();
}

function deleteSelectedVolumes() {
  var rowsToDelete = getAllSelectedRows('volumes', 1);
  for ( i = 0; i<rowsToDelete.length; i++ ) {
    var volumeId = rowsToDelete[i];
    $.ajax({
      type:"GET",
      url:"/ec2?type=key&Action=DeleteVolume&VolumeId=" + volumeId,
      data:"_xsrf="+$.cookie('_xsrf'),
      dataType:"json",
      async:"true",
      success:
        (function(volumeId) {
          return function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              successNotification("Scheduled volume delete for " + volumeId);
              allTablesRef['volumes'].fnReloadAjax();
            } else {
              errorNotification("Failed to schedule volume delete for " + volumeId);
            }
           }
        })(volumeId),
      error:
        (function(volumeId) {
          return function(jqXHR, textStatus, errorThrown){
            errorNotification("Failed to schedule volumvolumese delete for " + volumeId);
          }
        })(volumeId)
    });
  }
}

function S4() {
  return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

function hideNotification(notification) {
  $("#"+notification).detach();
}

function successNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
//  alert(message);
  $('#euca-notification-container').append('<div class="success" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
  // hide after 30 sec
  setTimeout(function(){ hideNotification(nId) }, 1000 * 30);
}

function errorNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
//  alert(message);
  $('#euca-notification-container').append('<div class="error" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
}
