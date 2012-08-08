/*
 * License
 */

/* Constats */
var RETURN_KEY_CODE = 13;
var RETURN_MAC_KEY_CODE = 10;
var BACKSPACE_KEY_CODE = 8;
/*
function updateActionMenu(context) {
  $parentDiv = $(context).parents('div.dataTables_wrapper').parent();
  selectedRows = $parentDiv.eucatable('getAllSelectedRows');
  if ( selectedRows.length == 0 ) {
    $parentDiv.eucatable('deactivateMenu');
  } else {
    $parentDiv.eucatable('activateMenu');
  }
}
*/
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
