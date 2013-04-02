define([
  'underscore',
  'backbone',
  'backbone-validation',
  'sharedtags'
], function(_, Backbone, BackboneValidation, tags) {
  _.extend(Backbone.Model.prototype, Backbone.Validation.mixin);
  var EucaModel = Backbone.Model.extend({
    initialize: function() {
        var self = this;
        if (self.get('tags') == null) self.set('tags', new Backbone.Collection());
        self.get('tags').on('add reset change', function() {
            self.refreshNamedColumns();
        });
        self.get('tags').on('add', function(model, models) {
            console.log(arguments);
            tags.add(model);
        });
        self.get('tags').on('change', function(model, models) {
            self.trigger('change');
        });
        _.each(this.namedColumns, function(c) {
            self.on('change:'+c, function() { 
                self.refreshNamedColumns();
            });
        });
        self.refreshNamedColumns();
    },
    refreshNamedColumns: function() {
        var self = this;
        _.each(this.namedColumns, function(column) {
            var matched = self.get('tags').where({res_id: self.get('id'), name: 'Name'});
            if (matched.length) {
                var tag = matched[0];
                self.set('display_' + column, tag.get('value'));
            } else {
                self.set('display_' + column, self.get(column));
            }
        });
    }
  });
  return EucaModel;
});
