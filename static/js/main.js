/*
 * License
 */
(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    eucalyptus.explorer();
    // show the main divs
    var makeMenutabs = function(args) {
      var $header = $('html body').find('.euca-main-outercontainer .inner-container #euca-tab-container');
      var $ul = $('<ul>').addClass('active');
      $.each(args, function(idx, val) {
          var str = '<a href=\"'+val.link+'\">'+val.name+'</a>' 
          $('<li>').append(str).appendTo($ul);
      });
      $ul.appendTo($header);
    }
     
    // will be deprecated
    var fillTable = function(args) {
      // fill the table
      allTablesRef['instances'] = $('#instances').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=instance&Action=DescribeInstances",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "instances[0].id" },
                { "mDataProp": "image_id" },
                { "mDataProp": "ip_address" },
                { "mDataProp": "private_ip_address" },
                { "mDataProp": "state" }
              ]
      });
      allTablesRef['images'] = $('#images').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=image&Action=DescribeImages",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "location" },
                { "mDataProp": "type" },
                { "mDataProp": "architecture" },
                { "mDataProp": "state" }
              ]
      }); 
      allTablesRef['keys'] = $('#keys').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=key&Action=DescribeKeyPairs",
              "sAjaxDataProp": "results",
              "bAutoWidth" : false,
              "sPaginationType": "full_numbers",
              "sDom": '<"table_keys_header">f<"clear"><"table_keys_top"><"clear">rtp<"clear">',
              "aoColumns": [
                {
                  "bSortable": false,
                  "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(\'keys\')"/>' },
                  "sWidth": "20px",
                },
                { "mDataProp": "name" },
                { "mDataProp": "fingerprint", "bSortable": false }
              ],
              "fnDrawCallback": function( oSettings ) {
		 $('#table_keys_count').html(oSettings.fnRecordsTotal());
              }
      });
      
      return ;

 
      setUpInfoTableLayout('keys', 'key pairs');
      // init delete dialog
      var deleteButtonId = "delete-" + S4();
      var cancelButtonId = "cancel-" + S4();
      $('#keys-delete-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         open: function(event, ui) {
	     $('.ui-widget-overlay').live("click", function() {
	       $('#keys-delete-dialog').dialog("close");
	     });
             $(":button:contains('Delete')").attr('id', deleteButtonId);
             $('#' + deleteButtonId + ' span').text("Yes, delete");
             $(":button:contains('Cancel')").attr('id', cancelButtonId);
             $('#' + cancelButtonId + ' span').text("Cancel");
             $('#' + cancelButtonId).focus();
             $('.ui-dialog-titlebar').append('<div class="help-link"><a href="#">?</a></div>');
           },
         buttons: [
          {
            text: "Delete",
            click: function() { deleteSelectedKeyPairs(); $(this).dialog("close"); }
          },
          {
            text: "Cancel",
            click: function() { $(this).dialog("close"); }
          }
        ]
      });
      // init add dialog
      var createButtonId = "create-" + S4();
      var cancelButtonId = "cancel-" + S4();
      $('#keys-add-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         open: function(event, ui) {
	     $('.ui-widget-overlay').live("click", function() {
	       $('#keys-add-dialog').dialog("close");
	     });
             $('.ui-dialog-titlebar').append('<div class="help-link"><a href="#">?</a></div>');
             $(":button:contains('Create')").attr('id', createButtonId);
             $createButton = $('#' + createButtonId);
             $createButton.prop("disabled", true).addClass("ui-state-disabled");
             $('#' + createButtonId + ' span').text("Create and download");
             $(":button:contains('Cancel')").attr('id', cancelButtonId);
             $('#' + cancelButtonId + ' span').text("Cancel");
             $('#key-name').keypress( function(e) {
               if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) {
                 $('#' + createButtonId).trigger('click');
               } else if ( e.which === 0 ) {
               } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 1 ) {
                 $createButton.prop("disabled", true).addClass("ui-state-disabled");
               } else if ( $(this).val().length == 0 ) {
                 $createButton.prop("disabled", true).addClass("ui-state-disabled");
               } else {
                 $createButton.prop("disabled", false).removeClass("ui-state-disabled");
               }
             });
           },
         buttons: [
          {
            text: "Create", // do not translate here
            click: function() {
              var keyName = $.trim($('#key-name').val());
              var keyPattern = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
              if (keyPattern.test(keyName)) {
                $(this).dialog("close"); addKeyPair(keyName);
              } else {
                $('#keys-add-dialog div.dialog-notifications').html("Name must be less than 256 alphanumeric characters, spaces, dashes, and/or underscores.");
              }
            }
          },
          {
            text: "Cancel", // do not translate here
            click: function() { $(this).dialog("close"); }
          }
        ]
      });

      allTablesRef['groups'] = $('#groups').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=group&Action=DescribeSecurityGroups",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "name" },
                { "mDataProp": "description" }
              ]
      }); 
      allTablesRef['addresses'] = $('#addresses').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=address&Action=DescribeAddresses",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "public_ip" },
                { "mDataProp": "instance_id" }
              ]
      }); 
      allTablesRef['volumes'] = $('#volumes').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=volume&Action=DescribeVolumes",
              "sAjaxDataProp": "results",
              "bAutoWidth" : false,
              "sPaginationType": "full_numbers",
              "sDom": '<"table_volumes_header">f<"clear"><"table_volumes_top">rtp<"clear">',
              "aoColumns": [
                {
                  "bSortable": false,
                  "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(\'volumes\')"/>' },
                  "sWidth": "20px",
                },
                { "mDataProp": "id" },
                {
                  "fnRender": function(oObj) { return oObj.aData.status },
                  "sWidth": "20px",
                },
                { "mDataProp": "size" },
                { "mDataProp": "create_time" },
                { "mDataProp": "snapshot_id" }
              ],
              "fnDrawCallback": function( oSettings ) {
		 $('#table_volumes_count').html(oSettings.fnRecordsTotal());
              }
      });
      setUpInfoTableLayout('volumes', 'volumes');
      // init delete dialog
      $('#volumes-delete-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         buttons: [
          {
            text: "Cancel",
            click: function() { $(this).dialog("close"); }
          },
          {
            text: "Yes, delete",
            click: function() { deleteSelectedVolumes(); $(this).dialog("close"); }
          }
        ]
      });
      // init add dialog
      $('#volumes-add-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         buttons: [
          {
            text: "Create",
            click: function() {
                $(this).dialog("close");
            }
          },
          {
            text: "Cancel",
            click: function() { $(this).dialog("close"); }
          }
        ]
      });

      allTablesRef['snapshots'] = $('#snapshots').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=snapshot&Action=DescribeSnapshots",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "status" },
                { "mDataProp": "progress" },
                { "mDataProp": "volume_id" },
                { "mDataProp": "start_time" },
                { "mDataProp": "owner_id" }
              ]
      });  
    }

    $('html body').find('.euca-container .euca-header-container .inner-container').header({show_logo:true,show_navigation:true,show_user:true,show_help:true});
    var $container = $('html body').find('.euca-main-outercontainer .inner-container #euca-main-container');
    $container.maincontainer();
    $('html body').find('.euca-explorer-container .inner-container').explorer({select: function(evt, ui){ 
                                                                      $container.maincontainer("changeSelected",evt, ui);
                                                                   }});

    $('html body').find('.euca-container .euca-footer-container .inner-container').footer();
/* 
    // find div.euca-container.euca-main#euca-main-header
    var menus = [{name:'Instances', link:'#tabs-instances'}, 
                  {name:'Images', link:'#tabs-images'},
                  {name:'Keys', link: '#tabs-keys'},
                  {name:'Security Groups', link: '#tabs-groups'},
                  {name:'Elastic IPs', link: '#tabs-addresses'},
                  {name:'Volumes', link: '#tabs-volumes'},
                  {name:'Snapshots', link: '#tabs-snapshots'}];

    makeMenutabs(menus);
    var $eucaTabContainer = $('html body').find('.euca-main-outercontainer .inner-container #euca-tab-container');
    $eucaTabContainer.append('<div class="euca-notification" id="euca-notification-container"></div>');
    $('#all-tabs-container > div').each(function() {
      ($(this)).appendTo($eucaTabContainer);
    });

    fillTable();

    var $main = $('html body').find('.euca-main-outercontainer .inner-container #euca-tab-container');
//    $main.tabs();
*/
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
