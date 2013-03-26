define([
  'underscore',
  'backbone',
  'backbone-validation'
], function(_, Backbone, BackboneValidation) {
  _.extend(Backbone.Model.prototype, Backbone.Validation.mixin);
  var EucaModel = Backbone.Model.extend({});
  return EucaModel;
});
