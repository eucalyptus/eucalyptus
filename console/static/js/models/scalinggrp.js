// scalinggrp model
//
define([
    './eucamodel',
],
function(EucaModel) {
  var model = EucaModel.extend({
    validation: {
            name: {
                minLength: 1,
                required: true
            },
            min_size: {
                pattern: 'digits'
            },
            desired_capacity: {
                pattern: 'digits'
            },
            max_size: {
                pattern: 'digits'
            },
            default_cooldown: {
                pattern: 'digits'
            },
            instance: {
            },
            health_check_period: {
            },
            created_time: {
            },
            enabled_metrics: {
            },
            availability_zones: {
            },
            member: {
            },
            health_check_type: {
            },
            launch_config_name: {
            },
            placement_group: {
            },
            tags: {
            },
            suspended_processes: {
            },
            autoscaling_group_arn: {
            },
            load_balancers: {
            },
            termination_policies: {
            },
            connection: {
            },
            vpc_zone_identifier: {
            },
        },
    sync: function(method, model, options) {
      var collection = this;
      if (method == 'create') {
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
          url:"/autoscaling?Action=CreateAutoScalingGroup",
          data:data,
          dataType:"json",
          async:true,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                notifySuccess(null, $.i18n.prop('create_scaling_group_run_success', DefaultEncoder().encodeForHTML(name)));
              } else {
                notifyError($.i18n.prop('create_scaling_group_run_error', DefaultEncoder().encodeForHTML(name)), undefined_error);
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
