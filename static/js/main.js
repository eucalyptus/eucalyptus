(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    eucalyptus.explorer();
    // show the main divs
    var makeMenutabs = function(args) {
      var $header = $('html body').find('.euca-container .euca-main #euca-tab-container');
      var $ul = $('<ul>').addClass('active');
      $.each(args, function(idx, val) {
          var str = '<a href=\"'+val.link+'\">'+val.name+'</a>' 
          $('<li>').append(str).appendTo($ul);
      });
      $ul.appendTo($header);
     // $header.tabs();
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
              "sDom": '<"table_keys_new">f<"clear"><"table_keys_top">rtp<"clear">',
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
      setUpInfoTableLayout('keys');
      // init delete dialog
      $('#keys-delete-dialog').dialog({
         autoOpen: false,
         modal: true,
         buttons: [
          {
            text: "Cancel",
            click: function() { $(this).dialog("close"); }
          },
          {
            text: "Yes, delete",
            click: function() { deleteSelectedKeyPairs(); $(this).dialog("close"); }
          }
        ]
      });
      // init add dialog
      $('#keys-add-dialog').dialog({
         autoOpen: false,
         modal: true,
         buttons: [
          {
            text: "Create and download",
            click: function() {
              var keyName = $.trim($('#key-name').val());
              var keyPattern = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
              if (keyPattern.test(keyName)) {
                $(this).dialog("close"); addKeyPair(keyName);
              } else {
                $('#keys-add-dialog div.dialog-notifications').html("Name must be less than 256 alphanumeric characters, spaces, dashes, and/or underscores");
              }
            }
          },
          {
            text: "Cancel",
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
              "sDom": '<"table_volumes_new">f<"clear"><"table_volumes_top">rtp<"clear">',
              "aoColumns": [
                {
                  "bSortable": false,
                  "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(\'volumes\')"/>' },
                  "sWidth": "20px",
                },
                { "mDataProp": "id" },
                { "mDataProp": "size" },
                { "mDataProp": "status" },
                { "mDataProp": "create_time" },
                { "mDataProp": "snapshot_id" }
              ],
              "fnDrawCallback": function( oSettings ) {
		 $('#table_volumes_count').html(oSettings.fnRecordsTotal());
              }
      });
      setUpInfoTableLayout('volumes');

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

    $('html body').find('.euca-container .euca-header').header();
    var $container = $('html body').find('.euca-container .euca-main #euca-main-container');
    $container.maincontainer();
    $('html body').find('.euca-container .euca-explorer').explorer({select: function(evt, ui){ 
                                                                      $container.maincontainer("changeSelected",evt, ui);
                                                                   }});

    // find div.euca-container.euca-main#euca-main-header
    var menus = [{name:'Instances', link:'#tabs-instances'}, 
                  {name:'Images', link:'#tabs-images'},
                  {name:'Keys', link: '#tabs-keys'},
                  {name:'Security Groups', link: '#tabs-groups'},
                  {name:'Elastic IPs', link: '#tabs-addresses'},
                  {name:'Volumes', link: '#tabs-volumes'},
                  {name:'Snapshots', link: '#tabs-snapshots'}];

    makeMenutabs(menus);
    var $eucaTabContainer = $('html body').find('.euca-container .euca-main #euca-tab-container');
    $eucaTabContainer.append('<div class="euca-notification" id="euca-notification-container"></div>');
    $('#all-tabs-container > div').each(function() {
      ($(this)).appendTo($eucaTabContainer);
    });

    fillTable();

    // calls eucalyptus.footer widget
    $('html body').find('.euca-container .euca-footer').footer();

    var $main = $('html body').find('.euca-container .euca-main #euca-tab-container');
    $main.tabs();

  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
