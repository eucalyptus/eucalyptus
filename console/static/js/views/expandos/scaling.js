define([
  'app',
  './eucaexpandoview',
  'text!./scaling.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      var id = tmp.get('name');
      this.model = new Backbone.Model();
      this.model.set('group', tmp);
      this.scope = this.model;
      this._do_init();
    }
  });
});
