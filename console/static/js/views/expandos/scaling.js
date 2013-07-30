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
      this.model.set('policies', app.data.scalingpolicys.where({as_name:tmp.get('name')}));
      this.scope = this.model;
      this._do_init();
    }
  });
});
