// scalinginst model
//

define([
    './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    idAttribute: 'instance_id',
    setInstanceHealth: function (health_status, should_respect_grace_period, options) {
      var url = "/autoscaling?Action=SetInstanceHealth";
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&HealthStatus="+health_status;
      if (should_respect_grace_period != undefined)
        data += "&ShouldRespectGracePeriod=true";
      this.makeAjaxCall(url, data, options);
    },
    terminateInstance: function (decrement_capacity, options) {
      var url = "/autoscaling?Action=TerminateInstanceInAutoScalingGroup";
      var id = this.get('instance_id');
      var data = "_xsrf="+$.cookie('_xsrf')+"&InstanceId="+id+
                 "&ShouldDecrementDesiredCapacity=";
      if (decrement_capacity != undefined)
        data += 'true';
      else data += 'false';
      this.makeAjaxCall(url, data, options);
    },

    sync: function(method, model, options) {
      if(method == 'update') {
        this.setInstanceHealth(model.get('health_status'), model.get('should_respect_grace_period'), options);
      } else if (method=="delete") {
        this.terminateInstance(model.get('decrement_capacity'), options);
      }
    }
  });
  return model;
});
