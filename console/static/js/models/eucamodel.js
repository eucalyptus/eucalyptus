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

        // If this model changes ID then all the tags must change as well.
        self.on('change:id', function() {
            self.get('tags').each(function(t) {
                t.set('res_id', self.get('id'));
            });
        });

        // FIXME: The following two methods ensure that tags will get created on new models.
        //        The methodology does not seem ideal.
        self.on('request', function() {
            if (self.isNew() && self.get('tags').length > 0) {
                self._tmpTags = self.get('tags').clone();
            }
        });

        self.on('sync', function() {
            if (self._tmpTags != null) {
                self._tmpTags.each(function(t) {
                   t.set('res_id', self.get('id'));
                   if (t.get('_new') && !t.get('_deleted')) { 
                       self.get('tags').add(t);
                       t.save();
                   }
                });
            }
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
    parse: function(response) {
        if (response != null && response.results != null) return response.results;
        return response;
    },
    refreshNamedColumns: function() {
        var self = this;
        if (this.promote_ids) this.promote_ids(self);
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
      var xhr = options.xhr = $.ajax({
        type: "POST",
        url: url,
        data: param,
        dataType: "json",
        async: true,
      }).done(options.success)
      .fail(options.error)

      this.trigger('request', this, xhr, options);

      return xhr;
    }
  });
  return EucaModel;
});
