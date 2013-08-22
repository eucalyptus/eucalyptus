define([
  'app',
  './eucaexpandoview',
  'text!./scaling.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('group', tmp);
      if (tmp.get('instances')) {
        this.model.set('current', tmp.get('instances').length);
      }
      else {
        this.model.set('current', '0');
      }
      var policies = app.data.scalingpolicys.where({as_name:tmp.get('name')}); 
      _.each(policies, function(p) {
        var pattern = /^Percent.*/;
        if(pattern.test(p.get('adjustment_type'))) {
          p.set('measure', app.msg('create_scaling_group_policy_measure_percent'));
        } else {
          p.set('measure', app.msg('create_scaling_group_policy_measure_instance'));
        }
      });
      this.model.set('policies', policies);
      this.scope = this.model;
      this._do_init();
    }
  });
});
