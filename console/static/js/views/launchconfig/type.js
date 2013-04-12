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
      //this.model.validate(_.pick(this.model.toJSON(),'type_number'));
      this.model.validate(_.pick(this.model.toJSON(),'type_names'));
      return this.model.isValid();
    }

  });
});
