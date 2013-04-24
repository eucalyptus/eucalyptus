define([
  'app',
  './eucaexpandoview',
  'text!./ipaddress.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('eip', tmp);
      this.model.set('instance', app.data.instances.get(this.model.get('eip').get('instance_id')));
      this.scope = this.model;
      this._do_init();
    },
  });
});
