(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    var drawTopArea = function(args) {
       var $header = $('html body').find('.euca-container .euca-header');
       var $logoArea = $header.find('#euca-logo');
       $logoArea.addClass('euca-header logo');
       var imgUrl = $.eucaData.context['url_home'] + $.eucaData.image['logo'];

        //img width hardcoded
       $('<img>').attr('src',imgUrl).attr('height','30px').appendTo($logoArea);

       //navigation area   
       var $naviArea = $header.find('#euca-navigator');
       $naviArea.addClass('euca-header navigator');
       $naviArea.append($('<table>').attr('width','auto').attr('align','center').append(
                            $('<tbody>').append(
			      $('<tr>').append(
                                 $('<td>').attr('valign','middle').css('width','auto').append(
				    $('<a>').attr('href','#').attr('id','top-button').addClass('euca-header navigator').css('display','block').text('Explorer')),
				 $('<td>').attr('valign','middle').css('width','auto').append(
				    $('<img>').attr('src','images/triangle.gif'))))));
       //user area
       var $userArea = $header.find('#euca-user');
       $userArea.addClass('euca-header user');
       $('<span>').addClass('euca-header user').attr('id','name').text($.eucaData.context['username']+' ').append($('<img>').attr('src','images/triangle.gif')).appendTo($userArea);

       //help area 
       var $helpArea = $header.find('#euca-help');
       $helpArea.addClass('euca-header help');
       $('<span>').addClass('euca-header help').attr('id','help').text('help ').append($('<img>').attr('src','images/triangle.gif')).appendTo($helpArea);

       $('html body').find('.euca-container .euca-header').css('display','block');       
    }

    var makeExplorers = function(args) {
        var $itemContainer = $('html body').find('.euca-container .euca-explorer');
        $.each($.eucaData.context.explorers, function(idx, val){
            eucalyptus.explorer({'container':$itemContainer, 'item':val, 'idx':idx});
        });
    }
   
    // show the main divs
    var makeMenutabs = function(args) {
      var $header = $('html body').find('.euca-container .euca-main #euca-main-header');
      var $ul = $('<ul>').addClass('active');
      $.each(args, function(idx, val) {
          var str = '<a href=\"'+val.link+'\">'+val.name+'</a>' 
          $('<li>').append(str).appendTo($ul);
      });
      $ul.appendTo($header);
     // $header.tabs();
    }

    var fillTable = function(args) {
      // fill the table
      allTablesRef['instances'] = $('#instances').dataTable( {
              "bProcessing": true,
              "sAjaxSource": "../ec2?type=instance&Action=DescribeInstances",
              "sAjaxDataProp": "results",
              "aoColumns": [
                { "mDataProp": "id" },
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
              "aoColumns": [
                {
                  "bSortable": false,
                  "fnRender": function(oObj) { return '<input type="checkbox"/>' },
                  "sWidth": "20px",
                },
                { "mDataProp": "name" },
                { "mDataProp": "fingerprint", "bSortable": false }
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

    var addFooter = function(textData){
       var $footer = $('html body').find('.euca-container .euca-footer');
       $footer.removeClass('inactive').addClass('active');
       var $table = $('<table>').append('<tbody>');
       $table.append(
            $('<tr>').append(
		$('<td>').append(
		   $('<p>').text(textData['footer'])),
		$('<td>').append(
       		   $('<p>').html('&nbsp;&nbsp;&nbsp;&nbsp;<a id=\'logout-button\' href=\'/\'>logout</a>'))));
       $table.appendTo($footer);
    }
    drawTopArea();
    makeExplorers();
    // find div.euca-container.euca-main#euca-main-header
    var menus = [{name:'Instances', link:'#tabs-instances'}, 
                  {name:'Images', link:'#tabs-images'},
                  {name:'Keys', link: '#tabs-keys'},
                  {name:'Security Groups', link: '#tabs-groups'},
                  {name:'Elastic IPs', link: '#tabs-addresses'},
                  {name:'Volumes', link: '#tabs-volumes'},
                  {name:'Snapshots', link: '#tabs-snapshots'}];
    makeMenutabs(menus);
    var $eucaTabContainer = $('html body').find('.euca-container .euca-main #euca-main-header');
    $('#all-tabs-container > div').each(function() {
      ($(this)).appendTo($eucaTabContainer);
    });

    fillTable();

    addFooter(args.text);

    var $mainHeader = $('html body').find('.euca-container .euca-main #euca-main-header');
    $mainHeader.tabs();
    var $header = $('html body').find('.euca-container .euca-header');
    var $footer = $('html body').find('.euca-container .euca-footer');

    // event handlers
    $header.find('#euca-navigator').hover(
       function () {
	     $(this).find('#top-button').addClass('mouseon');
             $(this).addClass('mouseon');
       }, 
       function () {
	     $(this).find('#top-button').removeClass('mouseon');
             $(this).removeClass('mouseon');
       }
    );
    $header.find('#euca-navigator').click(function (){
        var $explorer = $('html body').find('.euca-container .euca-explorer');
        if($(this).hasClass('mouseon')){
        //    $(this).removeClass('mouseon');
            $explorer.toggle('blind', {}, 300 );
        }else{
         //   $(this).addClass('mouseon');
            $explorer.toggle('blind', {}, 300 );

        }
    });

    // logout
    $footer.find('#logout-button').click(function(){
	$.cookie('session-id','');
    });
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
