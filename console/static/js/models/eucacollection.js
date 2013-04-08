define([
  'underscore',
  'sharedtags',
  'backbone'
], function(_, tags, Backbone) {
  var EucaCollection = Backbone.Collection.extend({
    initialize: function() {
        var self = this;
        tags.on('add change reset', function() {
            self.resetTags();
        });        
    },
    resetTags: function() {
        var self = this;
        this.each(function(m) {
            var newtags = tags.where({res_id: m.get('id')});
            m.get('tags').set(newtags);
        });
    },
    sync: function(method, model, options) {
      var collection = this;

      if (method == 'read') {
          $.ajax({
            type:"POST",
            url: collection.url,
            data:"_xsrf="+$.cookie('_xsrf'),
            dataType:"json",
            async:"true",
          }).done(
          function(describe) {
            if (describe.results) {
              var results = describe.results;
              _.each(results, function(r) { r.tags = new Backbone.Collection(); });
//              options.success && options.success(model, results, options);
              options.success && options.success(results);
              collection.resetTags();
            } else {
              ;//TODO: how to notify errors?
              console.log('regrettably, its an error');
            }
          }
        ).fail(
          // Failure
          function(jqXHR, textStatus) {
            //console.log('EUCACOLLECTION (error for '+name+') : '+textStatus);
            options.error && options.error(jqXHR, textStatus, options);
          }
        );
      }
    },

    columnSort: function(key, direction) {
      this.comparator = function(model) {
        return model.get(key);
      };
      this.sort();
    }
  });
  return EucaCollection;
});
