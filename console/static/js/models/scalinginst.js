// scalinginst model
//

define([
    './eucamodel'
], function(EucaModel) {
  var model = Backbone.RelationalModel.extend({
    idAttribute: 'instance_id',
    setInstanceHealth: function (health_status, should_respect_grace_period) {
      var url = "/autoscaling?Action=SetInstanceHealth";
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&HealthStatus="+health_status;
      if (should_respect_grace_period != undefined)
        data += "&ShouldRespectGracePeriod=true";
      this.makeAjaxCall(url, data, options);
    },
    terminateInstance: function (decrement_capacity) {
      var url = "/autoscaling?Action=TerminateInstanceInAutoScalingGroup";
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&ShouldDecrementDesiredCapacity=";
      if (decrement_capacity != undefined)
        data += 'true';
      else data += 'false';
      this.makeAjaxCall(url, data, options);
    }
  });
  return model;
});
