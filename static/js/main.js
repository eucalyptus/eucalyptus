(function($, eucalyptus) {
  eucalyptus.main= function(args) {
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
              "sAjaxDataProp": "item/instances",
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
              "sAjaxSource": "../ec2?type=key",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "name" },
                { "mDataProp": "fingerprint" }
              ]
      });    
      $('#groups').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=group",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "name" },
                { "mDataProp": "description" }
              ]
      }); 
      $('#addresses').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=address",
              "sAjaxDataProp": "",
              "aoColumns": [
                { "mDataProp": "public_ip" },
                { "mDataProp": "instance_id" }
              ]
      }); 
      $('#volumes').dataTable( {
          "bProcessing": true,
              "sAjaxSource": "../ec2?type=volume",
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
              "sAjaxSource": "../ec2?type=snapshot",
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
    var $header = $('html body').find('.euca-container .euca-main #euca-main-header');
    $header.tabs();
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
