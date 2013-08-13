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
      tags: new Backbone.Collection(),

      //options
      key_name: null,
      security_group: null,
      security_group_id: null,
      user_data: null,
      instance_type: null,
      placement: null,
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
          pattern: 'digits',
          min: 1,
          max: 99
        },

        max_count: {
          required: true,
          pattern: 'digits',
          min: 1,
          max: 99
        },

        instance_type: {
          required: false,
          oneOf: ['t1.micro', 'm1.small', 'm1.medium', 'm1.large', 'm1.xlarge', 'm2.xlarge', 'm2.2xlarge', 'm2.4xlarge', 'm3.xlarge', 'm3.2xlarge', 'c1.medium', 'c1.xlarge', 'hi1.4xlarge', 'hs1.8xlarge', 'cc1.4xlarge', 'cc2.8xlarge', 'cg1.4xlarge']
        }
      },
      initialize: function() {
        if(!this.get('platform')) {
          this.set('platform', 'linux');
        }
        EucaModel.prototype.initialize.call(this);
      },

      sync: function(method, model, options) {
          if (method == 'create') {
            var data = new Array();
            data.push({name: "_xsrf", value: $.cookie('_xsrf')});
            data.push({name: "ImageId", value: model.get('image_id')});
            data.push({name: "MinCount", value: model.get('min_count')});
            data.push({name: "MaxCount", value: model.get('max_count')});

            if (model.get('key_name') != undefined && model.get('key_name') !== 'none')
              data.push({name: "KeyName", value: model.get('key_name')});
            if (model.get('security_group') != undefined) {
              var groups = model.get('security_group');
              $.each(groups, function(idx, group) {
                data.push({name: "SecurityGroup." + (idx+1) , value: group});
              });
            }
            if (model.get('security_group_id') != undefined)
              data.push({name: "SecurityGroupId", value:  model.get('security_group_id')});
            if (model.get('user_data') != undefined)
              data.push({name: "UserData", value: model.get('user_data')});
            if (model.get('instance_type') != undefined)
              data.push({name: "InstanceType", value: model.get('instance_type')});
            if (model.get('kernel_id') != undefined)
              data.push({name: "KernelId", value: model.get('kernel_id')});
            if (model.get('ramdisk_id') != undefined)
              data.push({name: "RamdiskId", value: model.get('ramdisk_id')});
            if (model.get('placement') != undefined) {
              var placement = model.get('placement');
              data.push({name: "Placement.AvailabilityZone", value: placement});
            }
            if (model.get('block_device_mappings') != undefined) {
              var mappings = model.get('block_device_mappings');
              $.each(eval(mappings), function(idx, mapping) {
                if (mapping.device_name == '/dev/sda') { // root, folks!
                  console.log("adding root device mapping vol_size="+mapping.ebs.volume_size);
                  data.push({name: "BlockDeviceMapping."+(idx+1)+".DeviceName", value: mapping.device_name});
                  data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.VolumeSize", value: mapping.volume_size});
                  data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.DeleteOnTermination", value: mapping.delete_on_termination});
                }
                else if (mapping.ephemeral_name == 'ephemeral0') { // ephemeral device, folks!
                  console.log("adding ephemeral mapping :"+mapping.ephemeral_name);
                  data.push({name: "BlockDeviceMapping."+(idx+1)+".DeviceName", value: mapping.device_name});
                  data.push({name: "BlockDeviceMapping."+(idx+1)+".VirtualName", value: mapping.ephemeral_name});
                }
                else { // or, normal mappings
                  console.log("adding ebs mapping snapshot="+mapping.ebs.snapshot_id+" vol_size="+mapping.ebs.volume_size);
                  if(mapping.device_name != undefined)
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".DeviceName", value: mapping.device_name});

                  if(mapping.no_device != undefined) {
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".NoDevice", value: mapping.no_device});
                  }
                  if (mapping.virtual_name != undefined) {
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".VirtualName", value: mapping.virtual_name});
                  } else if (mapping.ebs != undefined) {
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.SnapshotId", value: mapping.ebs.snapshot_id});
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.VolumeSize", value: mapping.ebs.volume_size});
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.DeleteOnTermination", value: mapping.ebs.delete_on_termination});
                    if(mapping.ebs.volume_type != undefined)
                    data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.VolumeType", value: mapping.ebs.volume_type});
                    if(mapping.ebs.iopts != undefined) 
                      data.push({name: "BlockDeviceMapping."+(idx+1)+".Ebs.Iopts", value: mapping.ebs.iopts});
                  }
                }
              });
            }
            if (model.get('monitoring_enabled') != undefined)
              data.push({name: "Monitoring.Enabled", value: model.get('monitoring_enabled')});
            if (model.get('subnet_id') != undefined)
              data.push({name: "SubnetId", value: model.get('subnet_id')});
            if (model.get('disable_api_termination') != undefined)
              data.push({name: "DisableApiTermination", value: model.get('disable_api_termination')});
            if (model.get('instance_initiated_shutdown_behavior') != undefined)
              data.push({name: "InstanceInitiatedShutdownBehavior", value: model.get('instance_initiated_shutdown_behavior')});
            if(model.get('license') != undefined)
              data.push({name: "License", value: model.get('license')});
            if(model.get('private_ip_address') != undefined)
              data.push({name: "PrivateIpAddress", value: model.get('private_ip_address')});
            if(model.get('client_token') != undefined)
              data.push({name: "ClientToken", value: model.get('client_token')});
            if(model.get('network_interface') != undefined) {
              var interfaces = model.get('network_interface');
              $.each(interfaces, function(idx, interface) {
                data.push({name: "NetworkInterface."+(idx+1)+".NetworkInterfaceId", value: interface.network_interface_id});
                data.push({name: "NetworkInterface."+(idx+1)+".DeviceIndex", value: interface.device_index});
                data.push({name: "NetworkInterface."+(idx+1)+".SubnetId", value: interface.subnet_id});
                data.push({name: "NetworkInterface."+(idx+1)+".Description", value: interface.description});
                data.push({name: "NetworkInterface."+(idx+1)+".PrivateIpAddress", value: interface.private_ip_address});
                data.push({name: "NetworkInterface."+(idx+1)+".SecurityGroupId", value: interface.security_group_id});
                data.push({name: "NetworkInterface."+(idx+1)+".DeleteOnTermination",value:  interface.delete_on_termination});
                data.push({name: "NetworkInterface."+(idx+1)+".SecondaryPrivateIpAddressCount", value: interface.secondary_private_ip_address_count});
                if(interface.private_ip_addresses != undefined) {
                  var privips = interface.private_ip_addresses;
                  $.each(privips, function(jdx, privip) {
                    data.push({name: "NetworkInterface."+(idx+1)+".PrivateIpAddresses."+(jdx+1)+".PrivateIpAddress", value: privip.private_ip_address});
                    data.push({name: "NetworkInterface."+(idx+1)+".PrivateIpAddresses."+(jdx+1)+".Primary", value: privip.primary});
                  });

                }
              });
            }
            if(model.get('i_am_instance_profile') != undefined) {
              var profs = model.get('i_am_instance_profile');
              $.each(profs, function(idx, prof) {
                data.push({name: "IamInstanceProfile."+(idx+1)+".Arn", value: prof.arn});
                data.push({name: "IamInstanceProfile."+(idx+1)+".Name", value: prof.name});
              });
            }
            if(model.get('addressing_type') != undefined)
              data.push({name: "AddressingType", value: model.get('addressing_type')});
            if(model.get('ebs_optimized') != undefined)
              data.push({name: "EbsOptimized", value:  model.get('ebs_optimized')});
            if(model.get('curlopts') != undefined) 
              data.push({name: "curlopts", value:  model.get('curlopts')});
            if(model.get('return_curl_handle') != undefined)
              data.push({name: "return_curl_handle", value:  model.get('return_curl_handle')});
             
            var user_file = model.get('files') == undefined ? "none" : model.get('files');
            var self = this;

            $(model.get('fileinput')()).fileupload({
              url:"/ec2?Action=RunInstances",
              formData: data,
              dataType:"json",
              fileInput: null,
              paramName: "user_data_file",
            });

            // remove name tags from tags model - they're set elsewhere below
            var extra_name_tags = model.get('tags').where({name: 'Name'});
            model.get('tags').remove(extra_name_tags, {silent:true});

            var the_tags = model.get('tags').toJSON();
            var name_tags = model.get('names').toJSON();
            $(model.get('fileinput')()).fileupload("send", {
              files: user_file,
              success:
                function(data, textStatus, jqXHR){
                  if ( data.results ) {
                    notifySuccess(null, $.i18n.prop('instance_run_success', DefaultEncoder().encodeForHTML(data.results[0].id)));
                    self.set_tags(data.results, the_tags);
                    self.set_tags(data.results, name_tags, true);
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

        set_tags: function(instanceData, tags, unique) {
          var self = this;
          if(tags.length > 0) {
            if (unique == undefined || unique == false) {
              // each instance gets each tag
              var tagData = "_xsrf="+$.cookie('_xsrf');
              $.each(instanceData, function(idx, inst) {
                tagData += "&ResourceId." + (idx+1) + "=" + inst.id;
              });
              _.each(tags, function(tag, jdx, tags) {
                tagData += "&Tag." + (jdx+1) + ".Key=" + tag.name;
                tagData += "&Tag." + (jdx+1) + ".Value=" + tag.value;
              });
              self.create_tags(tagData);
            } else {
              // each instance receives one of the tags - expecting a tag for each instance
              $.each(instanceData, function(idx, inst) {
                var tagData = "_xsrf="+$.cookie('_xsrf');
                tagData += "&ResourceId.1=" + inst.id;
                tagData += "&Tag.1.Key=" + tags[idx].name;
                tagData += "&Tag.1.Value=" + tags[idx].value;
                self.create_tags(tagData);
              });
            }
          }
        },

        create_tags: function(tagData) {
          $.ajax({
            type: "POST",
            url: "/ec2?Action=CreateTags",
            data: tagData,
            dataType: "json",
            async: "true",
            success: 
              function(data, textStatus, jqXHR) {
                //console.log("RUN INSTANCE TAGS SUCCESS: ", data.return);   
              },
            error: 
              function(jqXHR, textStatus, errorThrown) {
                //console.log("RUN INSTANCE TAGS FAIL: ", data.return);
              }
          });
        },

        parse: function(response) {
            var response = EucaModel.prototype.parse.call(this, response);
            response.state = response.state;
            response.status = response.state;
            if (response.state == undefined) {
              response.state = response._state.name;
              response.status = response._state.name;
            }
            return response;
        }

    });
    return model;
});
