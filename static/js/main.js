(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    var drawTopArea = function(args) {
       var $logoArea = $('html body').find('.euca-container .euca-header #euca-logo');
       $logoArea.addClass('euca-header logo');
       var imgUrl = $.eucaData.context['url_home'] + $.eucaData.image['logo'];
       $('<img>').attr('src',imgUrl).attr('width','300').appendTo($logoArea);
    
       var $naviArea = $('html body').find('.euca-container .euca-header #euca-navigator');
       $naviArea.addClass('euca-header navigator');
       //$('<a>').attr('href','#').attr('id','top-button').addClass('ui-state-default ui-corner-all').text('Explore').appendTo($naviArea);

       $('<a>').attr('href','#').attr('id','top-button').addClass('euca-header navigator').text('Explore').appendTo($naviArea);
       
       $('html body').find('.euca-container .euca-header').css('display','block');
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
    var getTabElem = function(args) {
      var elems = [
      	$('<div>').attr('id','tabs-instances').addClass('active').append(
	 "<table id=\"instances\" class=\"display\">"+
           "<thead>"+
	     "<tr>"+
		"<th>id</th>"+
		"<th>image id</th>"+
		"<th>public IP</th>"+
		"<th>private IP</th>"+
		"<th>state</th>"+
	    "</tr>"+
          "</thead>"+
          "<tbody>"+
	    "<tr>"+
              "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
	    "</tr>"+ 
          "</tbody>"+ 
        "</table>"),
      $('<div>').attr('id','tabs-images').addClass('active').append(
        "<table id=\"images\" class=\"display\">"+
          "<thead>"+
	     "<tr>"+
   	       "<th>id</th>"+
   	       "<th>location</th>"+
      	       "<th>type</th>"+
 	       "<th>architecture</th>"+
 	       "<th>state</th>"+
	     "</tr>"+
           "</thead>"+
           "<tbody>"+
	     "<tr>"+
 	       "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
	     "</tr>"+
           "</tbody>"+
        "</table>"),
      $('<div>').attr('id','tabs-keys').addClass('active').append(
        "<table id=\"keys\" class=\"display\">"+
           "<thead>"+
             "<tr>"+
                "<th>name</th>"+
                "<th>fingerprint</th>"+
        	"</tr>"+
            "</thead>"+
            "<tbody>"+
               "<tr>"+ 
                  "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
               "</tr>"+
	    "</tbody>"+
        "</table>"),
      $('<div>').attr('id','tabs-groups').addClass('active').append(
	"<table id=\"groups\" class=\"display\">"+
	  "<thead>"+
            "<tr>"+
              "<th>name</th>"+
              "<th>description</th>"+
            "</tr>"+
  	  "</thead>"+
	  "<tbody>"+
            "<tr>"+
             "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
            "</tr>"+
	  "</tbody>"+
	"</table>"),
      $('<div>').attr('id','tabs-addresses').addClass('active').append(
	"<table id=\"addresses\ class=\"display\">"+
	  "<thead>"+
	    "<tr>"+
	      "<th>public IP</th>"+
	      "<th>Instance ID</th>"+
	    "</tr>"+
 	  "</thead>"+
	  "<tbody>"+
	    "<tr>"+
	      "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
	    "</tr>"+
	  "</tbody>"+
	"</table>"),
      $('<div>').attr('id','tabs-volumes').addClass('active').append(
	"<table id=\"volumes\" class=\"display\">"+
	  "<thead>"+
	    "<tr>"+
	      "<th>id</th>"+
	      "<th>size</th>"+
	      "<th>status</th>"+
	      "<th>creation time</th>"+
	      "<th>snapshot id</th>"+
	    "</tr>"+
	  "</thead>"+
	  "<tbody>"+
	    "<tr>"+
    	      "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+
	    "</tr>"+
	  "</tbody>"+
        "</table>"),
      $('<div>').attr('id','tabs-snapshots').addClass('active').append(
         "<table id=\"snapshots\" class=\"display\">"+
	   "<thead>"+
	     "<tr>"+
	       "<th>id</th>"+
	       "<th>status</th>"+
	       "<th>progress</th>"+
	       "<th>volume id</th>"+
	       "<th>start time</th>"+
	       "<th>owner id</th>"+
	     "</tr>"+
           "</thead>"+
           "<tbody>"+
	     "<tr>"+
    	       "<td colspan=\"5\" class=\"dataTables_empty\">Loading data from server</td>"+ 
	     "</tr>"+
	   "</tbody>"+
         "</table>")
      ];
      return elems;
    }
    var fillTable = function(args) {
      // fill the table
      $('#instances').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=instance&Action=DescribeInstances",
              "sAjaxDataProp": "instances",
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "image_id" },
                { "mDataProp": "ip_address" },
                { "mDataProp": "private_ip_address" },
                { "mDataProp": "state" }
              ]
      });
      $('#images').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=image&Action=DescribeImages",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "location" },
                { "mDataProp": "type" },
                { "mDataProp": "architecture" },
                { "mDataProp": "state" }
              ]
      }); 
      $('#keys').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=key&Action=DescribeKeyPairs",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "name" },
                { "mDataProp": "fingerprint" }
              ]
      });    
      $('#groups').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=group&Action=DescribeSecurityGroups",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "name" },
                { "mDataProp": "description" }
              ]
      }); 
      $('#addresses').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=address&Action=DescribeAddresses",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "public_ip" },
                { "mDataProp": "instance_id" }
              ]
      }); 
      $('#volumes').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=volume&Action=DescribeVolumes",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "id" },
                { "mDataProp": "size" },
                { "mDataProp": "status" },
                { "mDataProp": "create_time" },
                { "mDataProp": "snapshot_id" }
              ]
      });    
      $('#snapshots').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=snapshot&Action=DescribeSnapshots",
              "sAjaxDataProp": "",
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
    // find div.euca-container.euca-main#euca-main-header
    var menus = [{name:'Instances', link:'#tabs-instances'}, 
                  {name:'Images', link:'#tabs-images'},
                  {name:'Keys', link: '#tabs-keys'},
                  {name:'Security Groups', link: '#tabs-groups'},
                  {name:'Elastic IPs', link: '#tabs-addresses'},
                  {name:'Volumes', link: '#tabs-volumes'},
                  {name:'Snapshots', link: '#tabs-snapshots'}];
    makeMenutabs(menus);
    $.each(getTabElem(), function (idx, val){
      var $body = $('html body').find('.euca-container .euca-main #euca-main-header'); //-browser');
      val.appendTo($body);  
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
       }, 
       function () {
	     $(this).find('#top-button').removeClass('mouseon');
       }
    );
    $header.find('#euca-navigator').click(function (){
        if($(this).hasClass('mouseon')){
            $(this).removeClass('mouseon');
        }else{
            $(this).addClass('mouseon');
        }
    });

    // logout
    $footer.find('#logout-button').click(function(){
	$.cookie('session-id','');
    });
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
