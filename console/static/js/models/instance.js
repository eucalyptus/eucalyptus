// instance model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({

      //
      // API Reference: http://docs.aws.amazon.com/AWSSDKforPHP/latest/index.html#m=AmazonEC2/run_instances
      //

      image_id: null,
      min_count: null,
      max_count: null,
      tags: [{}],

      //options
      key_name: null,
      security_group: null,
      security_group_id: null,
      user_data: null,
      instance_type: null,
      placement: {availability_zone: null, group_name: null, tenancy: null},
      kernel_id: null,
      ramdisk_id: null,
      block_device_mappings: {
          virtual_name: null, 
          device_name: null, 
          ebs: {
                snapshot_id: null, 
                volume_size: null, 
                delete_on_termination: null, 
                volume_type: null, 
                iopts: null
              }, 
          no_device: null
      },
      monitoring_enabled: null,
      subnet_id: null,
      disable_api_termination: null,
      instance_initiated_shutdown_behavior: null,
      license: [],
      addressing_type: null,
      private_ip_addresses: null,
      client_token: null,
      network_interface: [],
      i_am_instance_profile: [],
      ebs_optimized: null,
      curlopts: [],
      return_curl_handle: null,

      namedColumns: ['id'],


      validation: {
        image_id: {
          required: true
        },

        min_count: {
          required: true,
          pattern: 'number'
        },

        max_count: {
          required: true,
          pattern: 'number'
        },

        instance_type: {
          required: false,
          oneOf: ['t1.micro', 'm1.small', 'm1.medium', 'm1.large', 'm1.xlarge', 'm2.xlarge', 'm2.2xlarge', 'm2.4xlarge', 'm3.xlarge', 'm3.2xlarge', 'c1.medium', 'c1.xlarge', 'hi1.4xlarge', 'hs1.8xlarge', 'cc1.4xlarge', 'cc2.8xlarge', 'cg1.4xlarge']
        }
      },

      sync: function(method, model, options) {
          if (method == 'create') {
            var data = "_xsrf="+$.cookie('_xsrf');
            data += "&ImageId=" + model.get('image_id');
            data += "&MinCount=" + model.get('min_count');
            data += "&MaxCount=" + model.get('max_count');

            if (model.get('key_name') != undefined)
              data += "&KeyName="+model.get('key_name');
            if (model.get('security_group') != undefined)
              data += "&=SecurityGroup="+model.get('security_group');
            if (model.get('security_group_id') != undefined)
              data += "&SecurityGroupId=" + model.get('security_group_id');
            if (model.get('user_data') != undefined)
              data += "&UserData=" + model.get('user_data');
            if (model.get('instance_type') != undefined)
              data += "&InstanceType="+model.get('instance_type');
            if (model.get('kernel_id') != undefined)
              data += "&KernelId="+model.get('kernel_id');
            if (model.get('RamdiskId') != undefined)
              data += "&RamdiskId="+model.get('ramdisk_id');
            if (model.get('placement') != undefined) {
              var placements = model.get('placement');
              $.each(placements, function(idx, placement) {
                data += "&Placement."+(idx)+".AvailabilityZone="+placement.availability_zone;
                data += "&Placement."+(idx)+".GroupName="+placement.group_name;
                data += "&Placement."+(idx)+".Tenancy="+placement.tenancy;
              });
            }
            if (model.get('block_device_mappings') != undefined) {
              var mappings = model.get('block_device_mappings');
              $.each(mappings, function(idx, mapping) {
                if(mapping.device_name != undefined)
                  data += "&BlockDeviceMapping."+(idx)+".DeviceName="+mapping.device_name;

                if(mapping.no_device != undefined) {
                  data += "&BlockDeviceMapping."+(idx)+".NoDevice="+mapping.no_device;
                }
                if (mapping.virtual_name != undefined) {
                  data += "&BlockDeviceMapping."+(idx)+".VirtualName="+mapping.virtual_name;
                } else if (mapping.ebs != undefined) {
                  data += "&BlockDeviceMapping."+(idx)+".Ebs.SnapshotId="+mapping.ebs.snapshot_id;
                  data += "&BlockDeviceMapping."+(idx)+".Ebs.VolumeSize="+mapping.ebs.volume_size;
                  data += "&BlockDeviceMapping."+(idx)+".Ebs.DeleteOnTermination="+mapping.ebs.delete_on_termination;
                  if(mapping.ebs.volume_type != undefined)
                  data += "&BlockDeviceMapping."+(idx)+".Ebs.VolumeType="+mapping.ebs.volume_type;
                  if(mapping.ebs.iopts != undefined) 
                    data += "&BlockDeviceMapping."+(idx)+".Ebs.Iopts="+mapping.ebs.iopts;
                }
              });
            }
            if (model.get('monitoring_enabled') != undefined)
              data += "&Monitoring.Enabled="+model.get('monitoring_enabled');
            if (model.get('subnet_id') != undefined)
              data += "&SubnetId="+model.get('subnet_id');
            if (model.get('disable_api_termination') != undefined)
              data += "&DisableApiTermination="+model.get('disable_api_termination');
            if (model.get('instance_initiated_shutdown_behavior') != undefined)
              data += "&InstanceInitiatedShutdownBehavior="+model.get('instance_initiated_shutdown_behavior');
            if(model.get('license') != undefined)
              data += "&License="+model.get('license');
            if(model.get('private_ip_address') != undefined)
              data += "&PrivateIpAddress="+model.get('private_ip_address');
            if(model.get('client_token') != undefined)
              data += "&ClientToken="+model.get('client_token');
            if(model.get('network_interface') != undefined) {
              var interfaces = model.get('network_interface');
              $.each(interfaces, function(idx, interface) {
                data += "&NetworkInterface."+(idx)+".NetworkInterfaceId="+interface.network_interface_id;
                data += "&NetworkInterface."+(idx)+".DeviceIndex="+interface.device_index;
                data += "&NetworkInterface."+(idx)+".SubnetId="+interface.subnet_id;
                data += "&NetworkInterface."+(idx)+".Description="+interface.description;
                data += "&NetworkInterface."+(idx)+".PrivateIpAddress="+interface.private_ip_address;
                data += "&NetworkInterface."+(idx)+".SecurityGroupId="+interface.security_group_id;
                data += "&NetworkInterface."+(idx)+".DeleteOnTermination="+interface.delete_on_termination;
                data += "&NetworkInterface."+(idx)+".SecondaryPrivateIpAddressCount="+interface.secondary_private_ip_address_count;
                if(interface.private_ip_addresses != undefined) {
                  var privips = interface.private_ip_addresses;
                  $.each(privips, function(jdx, privip) {
                    data += "&NetworkInterface."+(idx)+".PrivateIpAddresses."+(jdx)+".PrivateIpAddress="+privip.private_ip_address;
                    data += "&NetworkInterface."+(idx)+".PrivateIpAddresses."+(jdx)+".Primary="+privip.primary;
                  });

                }
              });
            }
            if(model.get('i_am_instance_profile') != undefined) {
              var profs = model.get('i_am_instance_profile');
              $.each(profs, function(idx, prof) {
                data += "&IamInstanceProfile."+(idx)+".Arn="+prof.arn;
                data += "&IamInstanceProfile."+(idx)+".Name="+prof.name;
              });
            }
            if(model.get('addressing_type') != undefined)
              data += "&AddressingType="+model.get('addressing_type');
            if(model.get('ebs_optimized') != undefined)
              data += "&EbsOptimized=" + model.get('ebs_optimized');
            if(model.get('curlopts') != undefined) 
              data += "&curlopts=" + model.get('curlopts');
            if(model.get('return_curl_handle') != undefined)
              data += "&return_curl_handle=" + model.get('return_curl_handle');
              
            var self = this;
            $.ajax({
              type:"POST",
              url:"/ec2?Action=RunInstances",
              data:data,
              dataType:"json",
              async:true,
              success:
                function(data, textStatus, jqXHR){
                  if ( data.results ) {
                    console.log("RUN INSTANCE: ", data);
                    notifySuccess(null, $.i18n.prop('instance_run_success', DefaultEncoder().encodeForHTML(data.results[0].id)));
                    self.tags(data.results, model);
                  } else {
                    notifyError($.i18n.prop('instance_run_error', DefaultEncoder().encodeForHTML(model.name), DefaultEncoder().encodeForHTML(model.name)), undefined_error);
                  }
                },
              error:
                function(jqXHR, textStatus, errorThrown){
                  notifyError($.i18n.prop('instance_run_error', DefaultEncoder().encodeForHTML(model.name), DefaultEncoder().encodeForHTML(model.name)), getErrorMessage(jqXHR));
                }
            });

          }
          else if (method == 'delete' && model.get('instance_id') != undefined) {
            $.ajax({
              type:"POST",
              url:"/ec2?Action=TerminateInstances",
              data:"_xsrf="+$.cookie('_xsrf')+"&InstanceId="+model.get('instance_id'),
              dataType:"json",
              async:true,
              success:
                function(data, textStatus, jqXHR){
                  if ( data.results ) {
                    notifySuccess(null, $.i18n.prop('delete_launch_config_success', DefaultEncoder().encodeForHTML(name)));
                  } else {
                    notifyError($.i18n.prop('delete_launch_config_error', DefaultEncoder().encodeForHTML(name)), undefined_error);
                  }
                },
              error:
                function(jqXHR, textStatus, errorThrown){
                  notifyError($.i18n.prop('delete_launch_config_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
                }
            });
          }
        },

        tags: function(instanceData, model) {
          var tagData = "_xsrf="+$.cookie('_xsrf');
          // each instance gets each tag
          if(model.get('tags') != undefined) {
            $.each(instanceData, function(idx, inst) {
              tagData += "&ResourceId." + idx + "=" + inst.id;
            });
            _.each(model.get('tags'), function(tag, jdx, tags) {
              tagData += "&Tag." + jdx + ".Key=" + tag.get('name');
              tagData += "&Tag." + jdx + ".Value=" + tag.get('value');
            });
          
            $.ajax({
              type: "POST",
              url: "/ec2?Action=CreateTags",
              data: tagData,
              dataType: "json",
              async: "true",
              success: 
                function(data, textStatus, jqXHR) {
                  console.log("RUN INSTANCE TAGS SUCCESS: ", data.return);   
                },
              error: 
                function(jqXHR, textStatus, errorThrown) {
                  console.log("RUN INSTANCE TAGS FAIL: ", data.return);
                }
            });
          }
        }

    });
    return model;
});
