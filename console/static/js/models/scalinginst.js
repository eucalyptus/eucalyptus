// scalinginst model
//

define([
    './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    setInstanceHealth: function (health_status, should_respect_grace_period) {
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&HealthStatus="+health_status;
      if (should_respect_grace_period != undefined)
        data += "&ShouldRespectGracePeriod=true";
      $.ajax({
        type:"POST",
        url:"/autoscaling?Action=SetInstanceHealth",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('manage_scaling_group_set_health_success', DefaultEncoder().encodeForHTML(id)));
            } else {
              notifyError($.i18n.prop('manage_scaling_group_set_health_error', DefaultEncoder().encodeForHTML(id)), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('manage_scaling_group_set_health_error', DefaultEncoder().encodeForHTML(id)), getErrorMessage(jqXHR));
          },
      });
    },
    terminateInstance: function (decrement_capacity) {
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&ShouldDecrementDesiredCapacity=";
      if (decrement_capacity != undefined)
        data += 'true';
      else data += 'false';
      $.ajax({
        type:"POST",
        url:"/autoscaling?Action=TerminateInstanceInAutoScalingGroup",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('manage_scaling_group_terminate_success', DefaultEncoder().encodeForHTML(id)));
            } else {
              notifyError($.i18n.prop('manage_scaling_group_terminate_error', DefaultEncoder().encodeForHTML(id)), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('manage_scaling_group_terminate_error', DefaultEncoder().encodeForHTML(id)), getErrorMessage(jqXHR));
          },
      });
    }
  });
  return model;
});
