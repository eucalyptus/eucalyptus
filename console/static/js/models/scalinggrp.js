// scalinggrp model
//
define([
    './eucamodel',
    './astags',
],
function(EucaModel, tags) {
  var model = EucaModel.extend({
    idAttribute: 'name',

    initialize: function() {
      // this doesn't properly deal with scaling group tags, but keeps
      // tagsearch from breaking
      this.set('tags', new tags());

      // default to empty array
      if(!this.get('instances')) {
        this.set('instances', []);
      }

      // thinking this should not call super since super deals with a lot of tag related
      // stuff and scaling group tags are a different animal
      //EucaModel.prototype.initialize.call(this);
    }, 

    defaults: {
        default_cooldown: 120
    },

    validation: {
           
            // ====================
            // API Reference: http://docs.aws.amazon.com/AutoScaling/latest/APIReference/API_AutoScalingGroup.html
            // ====================

            autoscaling_group_arn: {
              rangeLength: [1, 1600],
              required: false
            },
            name: {
              rangeLength: [1, 255],
              required: true
            },
            availability_zones: {
              required: true
            },
            created_time: {
              pattern: /^\d{4}-\d{2}-\d{2}T\d{2}\:\d{2}\:\d{2}\.\w+/,
              required: false
            },
            default_cooldown: {
              min: 0,
              required: true
            },
            desired_capacity: {
              pattern: 'digits',
              fn:  function(value, attr, customValue){
                if(parseInt(value) < parseInt(customValue.min_size)){
                  return attr + ' ' + $.i18n.prop('quick_scale_gt_or_eq') + ' ' + customValue.min_size;
                }else if(parseInt(value) > parseInt(customValue.max_size)){
                  return attr + ' ' + $.i18n.prop('quick_scale_lt_or_eq') + ' ' + customValue.max_size;
                }
              },
              //msg: $.i18n.prop('quick_scale_mustbe_number'),
              required: true
            },
            enabled_metrics: {
              required: false
            },
            health_check_period: {
              min: 0,
              required: false
            },
            health_check_type: {
              oneOf: ['EC2', 'ELB'],
              rangeLength: [1, 32],
              required: true
            },
            instances: {
              required: false
            },
            launch_config_name: {
              rangeLength: [1, 255],
              required: true
            },
            load_balancers: {
              required: false
            },
            max_size: {
              min: 0,
              required: true,
              pattern: 'digits'
            },
            min_size: {
              min: 0,
              required: true,
              pattern: 'digits'
            },
            placement_group: {
              rangeLength: [1, 255],
              required: false
            },
            status: {
              rangeLength: [1, 255],
              required: false
            },
            suspended_processes: {
              required: false
            },
            tags: {
              required: false
            },
            termination_policies: {
              required: false
            },
            vpc_zone_identifier: {
              rangeLength: [1, 255],
              required: false
            },
            member: {
            },
            connection: {
            },
        },

    sync: function(method, model, options) {
      var collection = this;
      if (method == 'create' || method == 'update') {
        var url = (method=='create' || options.overrideUpdate == true) ? "/autoscaling?Action=CreateAutoScalingGroup" : "/autoscaling?Action=UpdateAutoScalingGroup";
        var name = model.get('name');
        var data = "_xsrf="+$.cookie('_xsrf');
        data += "&AutoScalingGroupName="+name+
                "&LaunchConfigurationName="+model.get('launch_config_name')+
                "&MinSize="+model.get('min_size')+
                "&MaxSize="+model.get('max_size');
        if (model.get('default_cooldown') != undefined)
          data += "&DefaultCooldown="+model.get('default_cooldown');
        if (model.get('hc_type') != undefined)
          data += "&HealthCheckType="+model.get('hc_type');
        if (model.get('hc_period') != undefined)
          data += "&HealthCheckGracePeriod="+model.get('hc_period');
        if (model.get('desired_capacity') != undefined)
          data += "&DesiredCapacity="+model.get('desired_capacity');
        if (model.get('availability_zones') != undefined)
          data += build_list_params("AvailabilityZones.member.", model.get('availability_zones'));
        if (model.get('load_balancers') != undefined)
          data += build_list_params("LoadBalancerNames.member.", model.get('load_balancers'));
        if (model.get('tags') != undefined) 
          data += build_list_params("Tags.member.", model.get('tags').toJSON());
        if (model.get('termination_policies') != undefined)
          data += build_list_params("TerminationPolicies.member.", model.get('termination_policies'));
        return this.makeAjaxCall(url, data, options);
      }
      else if (method == 'delete') {
        var url = "/autoscaling?Action=DeleteAutoScalingGroup";
        var name = model.get('name');
        var data = "_xsrf="+$.cookie('_xsrf')+"&AutoScalingGroupName="+name;
        if (model.get('force_delete') != undefined)
          data += "&ForceDelete=true";
        return this.makeAjaxCall(url, data, options);
      }
    },

    setDesiredCapacity: function (desired_capacity, honor_cooldown, options) {
      var url = "/autoscaling?Action=SetDesiredCapacity";
      var name = this.get('name');
      var data = "_xsrf="+$.cookie('_xsrf')+"&AutoScalingGroupName="+this.get('name')+
                 "&DesiredCapacity="+desired_capacity;
      if (honor_cooldown != undefined)
        data += "&HonorCooldown=true";
      this.makeAjaxCall(url, data, options);
    },

    isNew: function() {
        return this.get('autoscaling_group_arn') == null;
    }
  });
  return model;
});
