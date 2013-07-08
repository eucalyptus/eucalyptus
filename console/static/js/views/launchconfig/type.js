define([
  'app',
	'dataholder',
  'text!./type.html!strip',
  'rivets',
  '../shared/type'
	], function( app, dataholder, template, rivets, Type) {
  return Type.extend({
    tpl: template,

    isValid: function() {
      if (this.model.get('errors') == undefined) {
        this.model.set('errors', new Backbone.Model());
      }
      this.model.get('errors').clear();
      this.model.get('errors').set(this.model.validate(_.pick(this.model.toJSON(),'lc_name'))); 
      return this.model.isValid();
    },
  });
});
