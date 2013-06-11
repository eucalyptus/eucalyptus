// scalinggrp model
//
define([
    './eucamodel',
    './scalinginst',
    './scalinginsts',
    './astag',
    './astags',
],
function(EucaModel, ScalingInst, ScalingInstances, Tag, Tags) {
  var model = Backbone.RelationalModel.extend({
    idAttribute: 'name',

    initialize: function() {

      // bump min and max to contain wherever dc lands
      this.on('change:desired_capacity', function() {
        var dc = +this.get('desired_capacity');
        var min = +this.get('min_size');
        var max = +this.get('max_size');
        if (dc > max)
          this.set('max_size', dc);
        if (dc < min &&  dc >= 0) 
          this.set('min_size', dc);
      });

      // bump dc to within new max
      this.on('change:max_size', function() {
        var dc = +this.get('desired_capacity');
        var max = +this.get('max_size');
        if (dc > max)
          this.set('desired_capacity', max);
      });

      // bump dc to within new min
      this.on('change:min_size', function() {
        var dc = +this.get('desired_capacity');
        var min = +this.get('min_size');
        if (dc < min && dc >= 0)
          this.set('desired_capacity', min);
      });

      this.fetchRelated();
      console.log('scaling', this);
      console.log('related instances:', this.get('instances'));
      console.log('related tags:', this.get('tags'));
    }, 

    relations: [
      {
        type: 'HasMany',
        key: 'instances',
        relatedModel: ScalingInst,
        collectionType: ScalingInstances,
        reverseRelation: {
          key: 'scalinggroup',
        }
      },
      {
        type: 'HasMany',
        key: 'tags',
        relatedModel: Tag,
        collectionType: Tags,
        autoFetch: true,
        reverseRelation: {
          key: 'resource_id',
          includeInJSON: 'name'
        }
      }
    ],

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
              required: true
            },
            default_cooldown: {
              min: 0,
              required: true
            },
            desired_capacity: {
              pattern: 'digits',
              // Commented out because of the above bindings that change min and max to suit desired_capacity.
              // Essentially there is no more min/max bounds to stay within.
              /*fn:  function(value, attr, customValue){
                if(parseInt(value) < parseInt(customValue.min_size)){
                  return attr + ' ' + $.i18n.prop('quick_scale_gt_or_eq') + ' ' + customValue.min_size;
                }else if(parseInt(value) > parseInt(customValue.max_size)){
                  return attr + ' ' + $.i18n.prop('quick_scale_lt_or_eq') + ' ' + customValue.max_size;
                }
              },*/
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
        var url = method=='create' ? "/autoscaling?Action=CreateAutoScalingGroup" : "/autoscaling?Action=UpdateAutoScalingGroup";
        var name = model.get('name');
        var data = "_xsrf="+$.cookie('_xsrf');
        data += "&AutoScalingGroupName="+name+
                "&LaunchConfigurationName="+model.get('launch_config')+
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
        if (model.get('zones') != undefined)
          data += build_list_params("AvailabilityZones.member.", model.get('zones'));
        if (model.get('load_balancers') != undefined)
          data += build_list_params("LoadBalancerNames.member.", model.get('load_balancers'));
        if (model.get('tags') != undefined)
          data += build_list_params("Tags.member.", model.get('tags'));
        if (model.get('termination_policies') != undefined)
          data += build_list_params("TerminationPolicies.member.", model.get('termination_policies'));
        $.ajax({
          type:"POST",
          url: url,
          data:data,
          dataType:"json",
          async:true,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                var msg = method=='create' ? $.i18n.prop('create_scaling_group_run_success', DefaultEncoder().encodeForHTML(name)) : $.i18n.prop('update_scaling_group_run_success', DefaultEncoder().encodeForHTML(name));
                notifySuccess(null, msg);
              } else {
                var msg = method=='create' ? $.i18n.prop('create_scaling_group_run_error', DefaultEncoder().encodeForHTML(name)) : $.i18n.prop('update_scaling_group_run_error', DefaultEncoder().encodeForHTML(name));
                notifyError(msg, undefined_error);
              }
            },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('create_scaling_group_run_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
            }
        });
      }
      else if (method == 'delete') {
        var name = model.get('name');
        var data = "_xsrf="+$.cookie('_xsrf')+"&AutoScalingGroupName="+name;
        if (model.get('force_delete') != undefined)
          data += "&ForceDelete=true";
        $.ajax({
          type:"POST",
          url:"/autoscaling?Action=DeleteAutoScalingGroup",
          data:data,
          dataType:"json",
          async:true,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                notifySuccess(null, $.i18n.prop('delete_scaling_group_success', DefaultEncoder().encodeForHTML(name)));
              } else {
                notifyError($.i18n.prop('delete_scaling_group_error', DefaultEncoder().encodeForHTML(name)), undefined_error);
              }
            },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('delete_scaling_group_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
            }
        });
      }
    },

    setDesiredCapacity: function (desired_capacity, honor_cooldown) {
      var name = this.get('name');
      var data = "_xsrf="+$.cookie('_xsrf')+"&AutoScalingGroupName="+this.get('name')+
                 "&DesiredCapacity="+desired_capacity;
      if (honor_cooldown != undefined)
        data += "&HonorCooldown=true";
      $.ajax({
        type:"POST",
        url:"/autoscaling?Action=SetDesiredCapacity",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('quick_scale_success', DefaultEncoder().encodeForHTML(name)));
            } else {
              notifyError($.i18n.prop('quick_scale_error', DefaultEncoder().encodeForHTML(name)), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('quick_scale_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
          },
      });
    }
  });
  return model;
});
