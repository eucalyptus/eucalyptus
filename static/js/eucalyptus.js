(function($, eucalyptus) {
  $(function(){
    $( "#tabs" ).tabs();
  });

  // event handler
  $(function() {
    $(document).ready(function() {
      $('#instances').dataTable( {
            "bProcessing": true,
                "sAjaxSource": "../ec2?Action=DescribeInstances",
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
                "sAjaxSource": "../ec2?Action=DescribeImages",
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
                "sAjaxSource": "../ec2?Action=DescribeKeyPairs",
                "sAjaxDataProp": "",
                "aoColumns": [
                  { "mDataProp": "name" },
                  { "mDataProp": "fingerprint" }
                ]
      });    
      $('#groups').dataTable( {
            "bProcessing": true,
                "sAjaxSource": "../ec2?Action=DescribeSecurityGroups",
                "sAjaxDataProp": "",
                "aoColumns": [
                  { "mDataProp": "name" },
                  { "mDataProp": "description" }
                ]
      }); 
      $('#addresses').dataTable( {
            "bProcessing": true,
                "sAjaxSource": "../ec2?Action=DescribeAddresses",
                "sAjaxDataProp": "",
                "aoColumns": [
                  { "mDataProp": "public_ip" },
                  { "mDataProp": "instance_id" }
                ]
      }); 
      $('#volumes').dataTable( {
            "bProcessing": true,
                "sAjaxSource": "../ec2?Action=DescribeVolumes",
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
                "sAjaxSource": "../ec2?Action=DescribeSnapshots",
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
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
