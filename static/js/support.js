function selectCheckboxChanged(context, tableName) {
  dataTable = allTablesRef[tableName];
  switch(context.checked) {
    case true:
      var rows = dataTable.fnGetVisiableTrNodes();
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null ) cb.checked = true;
      }
      // activate action menu
      $('div.table_' + tableName + '_top div.euca-table-action').removeClass('inactive');
      break;
    case false:
      var rows = dataTable.fnGetVisiableTrNodes();
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null ) cb.checked = false;
      }
      // deactivate action menu
      $('div.table_' + tableName + '_top div.euca-table-action').addClass('inactive');
      break;
  }
}

function updateActionMenu(tableName) {
  if ( getAllSelectedRows(tableName, 1).length == 0 ) {
    $('div.table_' + tableName + '_top div.euca-table-action').addClass('inactive');
  } else {
    $('div.table_' + tableName + '_top div.euca-table-action').removeClass('inactive');
  }
}

function getAllSelectedRows(tableName, idIndex) {
  dataTable = allTablesRef[tableName];
  var rows = dataTable.fnGetVisiableTrNodes();
  var selectedRows = [];
  for ( i = 0; i<rows.length; i++ ) {
    cb = rows[i].firstChild.firstChild;
    if ( cb != null && cb.checked == true ) {
      if ( rows[i].childNodes[idIndex] != null )
        selectedRows.push(rows[i].childNodes[idIndex].firstChild.nodeValue);
    }
  }
  return selectedRows;
}

function escapeHTML(input) {
  return $('<div/>').text(input).html();
}

function deleteAction(tableName, idColumnIndex) {
  // hide menu
  $menuUl = $("div.table_" + tableName + "_top div.euca-table-action ul");
  $menuUl.removeClass('activemenu');

  var rowsToDelete = getAllSelectedRows(tableName, idColumnIndex);

  if ( rowsToDelete.length > 0 ) {
    // show delete dialog box
    $deleteNames = $("#" + tableName + "-delete-names");
    $deleteNames.html('');
    for ( i = 0; i<rowsToDelete.length; i++ ) {
      t = escapeHTML(rowsToDelete[i]);
      $deleteNames.append(t).append("<br/>");
    }
    $("#" + tableName + "-delete-dialog").dialog('open');
  }
}

function deleteSelectedKeyPairs() {
  var rowsToDelete = getAllSelectedRows('keys', 1);
  for ( i = 0; i<rowsToDelete.length; i++ ) {
    var keyName = rowsToDelete[i];
    $.ajax({
      type:"GET",
      url:"/ec2?type=key&Action=DeleteKeyPair&KeyName=" + keyName,
      data:"_xsrf="+$.cookie('_xsrf'),
      dataType:"json",
      async:"true",
      success:
        (function(keyName) {
          return function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              successNotification("Deleted keypair " + keyName);
              allTablesRef['keys'].fnReloadAjax();
            } else {
              errorNotification("Failed to delete keypair " + keyName);
            }
           }
        })(keyName),
      error:
        (function(keyName) {
          return function(jqXHR, textStatus, errorThrown){
            errorNotification("Failed to delete keypair " + keyName);
          }
        })(keyName)
    });
  }
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

/*
 * Adds custom look and feel to resources table view
 */
function setUpInfoTableLayout(tableName) {
  $resourceTable = $('div.table_' + tableName + '_new');
  $resourceTable.addClass('euca-table-add');
  $resourceTable.html('<a id="table-' + tableName + '-new" class="add-resource" "href="#">Add</a>');
  $('#' + tableName + '_filter').append('&nbsp<a class="table-refresh" href="#">Refresh</a>');
  $('div#' + tableName + '_filter a.table-refresh').click( function () {
    allTablesRef[tableName].fnReloadAjax();
  });
  $resourceTableTop = $('div.table_' + tableName + '_top');
  $resourceTableTop.addClass('euca-table-length');
  $resourceTableTop.html('<div class="euca-table-action actionmenu inactive"></div><div class="euca-table-size"><span id="table_' + tableName + '_count"></span> ' + tableName + ' found. Showing <span class="show selected">10</span> | <span class="show">25</span> | <span class="show">50</span> | <span class="show">all</span></div>');

  $resourceTableTop.find("span.show").click( function () {
    $(this).parent().children('span').each( function() {
      $(this).removeClass("selected");
    });
    if ( this.innerHTML == "10" ) {
      allTablesRef[tableName].fnSettings()._iDisplayLength = 10;
      allTablesRef[tableName].fnDraw();
      $(this).addClass("selected");
    } else if ( this.innerHTML == "25" ) {
      allTablesRef[tableName].fnSettings()._iDisplayLength = 25;
      allTablesRef[tableName].fnDraw();
      $(this).addClass("selected");
    } else if ( this.innerHTML == "50" ) {
      allTablesRef[tableName].fnSettings()._iDisplayLength = 50;
      allTablesRef[tableName].fnDraw();
      $(this).addClass("selected");
    } else {
      allTablesRef[tableName].fnSettings()._iDisplayLength = -1;
      allTablesRef[tableName].fnDraw();
      $(this).addClass("selected");
    }
  });
  //action menu
  menuContent = '<ul><li><a href="#">More actions<span class="arrow"></span></a><ul>' +
                '<li><a href="#" id="' + tableName + '-delete">Delete</a></li>' +
                '</ul></li></ul>';
  $menuDiv = $resourceTableTop.find("div.euca-table-action");
  $menuDiv.html(menuContent);
  $('#' + tableName + '-delete').click( function() {
    deleteAction(tableName, 1);
  });
  $('#table-' + tableName + '-new').click( function() {
    $('#' + tableName + '-add-dialog').dialog('open');
  });
  $menuDiv.find('ul > li > a').click( function(){
    parentUL = $(this).parent().parent();
    if ( !parentUL.parent().hasClass('inactive') ) {
      if ( parentUL.hasClass('activemenu') ){
        parentUL.removeClass('activemenu');
      } else {
        parentUL.addClass('activemenu');
      }
    }
  });
}

function addKeyPair(keyName) {
  $.ajax({
    type:"GET",
    url:"/ec2?type=key&Action=CreateKeyPair",
    data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
    dataType:"json",
    async:"false",
    success:
      function(data, textStatus, jqXHR){
         if (data.results && data.results.material) {
           $.generateFile({
             filename    : keyName,
             content     : data.results.material,
             script      : '/support?Action=DownloadFile&_xsrf=' + $.cookie('_xsrf')
           });
           // TODO: can we wait till file is saved by user?
           successNotification("Added keypair " + keyName);
           // refresh table
           allTablesRef['keys'].fnReloadAjax();
         } else {
           errorNotification("Failed to create keypair " + keyName);
         }
      },
    error:
      function(jqXHR, textStatus, errorThrown){
        errorNotification("Failed to create keypair " + keyName);
      }
  });
}

function S4() {
  return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

function hideNotification(notification) {
  $("#"+notification).detach();
}

function successNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
  $('#euca-notification-container').append('<div class="success" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
  // hide after 30 sec
  setTimeout(function(){ hideNotification(nId) }, 1000 * 30);
}

function errorNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
  $('#euca-notification-container').append('<div class="error" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
}
