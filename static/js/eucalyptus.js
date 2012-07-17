(function($, eucalyptus) {
  var eucaData = window.eucaData ? window.eucaData : window.eucaData = {};

  $(document).ready(function() {
  // check cookie
    if ($.cookie('session-id')) {
      eucalyptus.main(eucaData);
    } else {
      eucalyptus.login($.extend(eucaData, {
        doLogin : function(args){
	  // actual login process
          args.onSuccess();
        }
      }));
    }
  });

  // event handler
/*
  $(function() {
    $(document).ready(function() {
      $('#instances').dataTable( {
            "bProcessing": true,
                "sAjaxSource": "../ec2?type=instance",
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
                "sAjaxSource": "../ec2?type=image",
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
    }); // end of document.ready()
  }); // end of event handler 
*/
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
