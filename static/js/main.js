(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    eucalyptus.explorer();
    // show the main divs
    var makeMenutabs = function(args) {
      var $header = $('html body').find('.euca-container .euca-main #euca-main-container');
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
                  "fnRender": function(oObj) { return '<input type="checkbox"/>' },
                  "sWidth": "20px",
                },
                { "mDataProp": "name" },
                { "mDataProp": "fingerprint", "bSortable": false }
              ],
              "fnDrawCallback": function( oSettings ) {
		 $('#table_keys_count').html(oSettings.fnRecordsTotal());
              }
      });
      // TODO: create a function so some or all of this can be reused for all tables
      $('div.table_keys_new').addClass('euca-table-add');
      $('div.table_keys_new').html('<a class="table-key-new" href="#">Add</a>');
      $('#keys_filter').append('&nbsp<a class="table-refresh" href="#">Refresh</a>');
      $('div#keys_filter a.table-refresh').click( function () {
        allTablesRef['keys'].fnReloadAjax();
      });
      $('div.table_keys_top').addClass('euca-table-length');
      $('div.table_keys_top').html('<div class="euca-table-action actionmenu"></div><div class="euca-table-size"><span id="table_keys_count"></span> key pairs found. Showing <span class="show">10</span> | <span class="show">25</span> | <span class="show">50</span> | <span class="show">all</span></div>');
      // TODO: highlight selected
      $("div.table_keys_top span.show").click( function () {
	 if ( this.innerHTML == "10" ) {
           allTablesRef['keys'].fnSettings()._iDisplayLength = 10;
           allTablesRef['keys'].fnDraw();
         } else if ( this.innerHTML == "25" ) {
           allTablesRef['keys'].fnSettings()._iDisplayLength = 25;
           allTablesRef['keys'].fnDraw();
         } else if ( this.innerHTML == "50" ) {
           allTablesRef['keys'].fnSettings()._iDisplayLength = 50;
           allTablesRef['keys'].fnDraw();
	 } else {
	   allTablesRef['keys'].fnSettings()._iDisplayLength = -1;
           allTablesRef['keys'].fnDraw();
         }
      });
      //action menu
      menuContent = '<ul><li><a href="#">Actions<span class="arrow"></span></a><ul>' +
             '<li><a href="#" id="keys-delete">Delete</a></li>' +
             '</ul></li></ul>';
      $menuDiv = $("div.table_keys_top div.euca-table-action");
      $menuDiv.html(menuContent);
      $('#keys-delete').click(function() {
        deleteAction('keys');
      });
      $('a.table-key-new').click(function() {
        $("#key-add-dialog").dialog('open');
      });
      //TODO: figure out why 'ul li a' selector does not work
      $menuDiv.children('ul').children('li').children('a').click(function(){
        parentUL = $(this).parent().parent();
        if (parentUL.hasClass('activemenu')){
          parentUL.removeClass('activemenu');
        }
        else {
          parentUL.addClass('activemenu');
        }
      });
      // init delete dialog
      // TODO: figure out focus
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
      $('#key-add-dialog').dialog({
         autoOpen: false,
         modal: true,
         buttons: [
          {
            text: "Create and download",
            click: function() { $(this).dialog("close"); addKeyPair($.trim($('#key-name').val())); }
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
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "size" },
                { "mDataProp": "status" },
                { "mDataProp": "create_time" },
                { "mDataProp": "snapshot_id" }
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

    $('html body').find('.euca-container .euca-header').header();
    $('html body').find('.euca-container .euca-main #euca-main-container').maincontainer();
    $('html body').find('.euca-container .euca-explorer').explorer({select: jQuery.eucalyptus.maincontainer.prototype.changeSelected});

    // find div.euca-container.euca-main#euca-main-header
    var menus = [{name:'Instances', link:'#tabs-instances'}, 
                  {name:'Images', link:'#tabs-images'},
                  {name:'Keys', link: '#tabs-keys'},
                  {name:'Security Groups', link: '#tabs-groups'},
                  {name:'Elastic IPs', link: '#tabs-addresses'},
                  {name:'Volumes', link: '#tabs-volumes'},
                  {name:'Snapshots', link: '#tabs-snapshots'}];

    makeMenutabs(menus);
    var $eucaTabContainer = $('html body').find('.euca-container .euca-main #euca-main-container');
    $eucaTabContainer.append('<div class="euca-notification" id="euca-notification-container"></div>');
    $('#all-tabs-container > div').each(function() {
      ($(this)).appendTo($eucaTabContainer);
    });

    fillTable();

    // calls eucalyptus.footer widget
    $('html body').find('.euca-container .euca-footer').footer();

    var $main = $('html body').find('.euca-container .euca-main #euca-main-container');
    $main.tabs();

  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
