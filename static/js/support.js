/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
