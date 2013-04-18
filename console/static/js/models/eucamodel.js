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

        // Prepopulate the tags for this model
        if (self.get('tags') == null) {
            self.set('tags', new Backbone.Collection(tags.where({res_id: self.get('id')})));
            self.refreshNamedColumns();
        }

        // If global tags are refreshed, update the model
        tags.on('sync add remove reset change', _.debounce(function() {
            self.get('tags').set(tags.where({res_id: self.get('id')}));
        },100));

        // If local tags are refreshed, update the model
        self.get('tags').on('add remove reset change', function() {
            self.refreshNamedColumns();
        });
    },
    // Override set locally so that we can properly update the tags collection
    set: function(key, val, options) {    
        var attrs; 
        if (key == null) return this;

        if (typeof key === 'object') {
            attrs = key;
        } else {
            (attrs = {})[key] = val;
        }

        if (attrs.tags != null && this.get('tags') != null) {
            this.get('tags').set(attrs.tags.models);
            attrs.tags = this.get('tags');
        }

        return Backbone.Model.prototype.set.call(this, key, val, options);
    },
    refreshNamedColumns: function() {
        var self = this;
        _.each(this.namedColumns, function(column) {
            var matched = tags.where({res_id: self.get(column), name: 'Name'});
            if (matched.length) {
                var tag = matched[0];
                self.set('display_' + column, tag.get('value'));
            } else {
                self.set('display_' + column, self.get(column));
            }
        });
    },
    makeAjaxCall: function(url, param, options){
      $.ajax({
        type: "POST",
        url: url,
        data: param,
        dataType: "json",
        async: true,
        success: options.success,
        error: options.error
      });
    }
  });
  return EucaModel;
});
