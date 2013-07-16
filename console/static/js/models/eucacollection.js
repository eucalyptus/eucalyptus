define([
  'underscore',
  'backbone',
], function(_, Backbone) {
  var EucaCollection = Backbone.Collection.extend({
    initialize: function() {
        var self = this;
        this.isLoaded = false;
    },

    hasLoaded: function() {
      return this.isLoaded;
    },

    sync: function(method, model, options) {
      var collection = this;

      if (method == 'read') {
          $.ajax({
            url: collection.url,
            data:"_xsrf="+$.cookie('_xsrf')+collection.__more_params__(),
            dataType:"json",
            async:"true",
          }).done(
          function(describe) {
            if (describe.results) {
              var results = describe.results;
              _.each(results, function(r) {
                if (r.tags != null) delete r.tags;
              });
              model.isLoaded = true;
              model.trigger('initialized');
              options.success && options.success(results);
              //collection.resetTags();
            } else {
              ;//TODO: how to notify errors?
              console.log('regrettably, its an error');
            }
          }
        ).fail(
          // Failure
          function(jqXHR, textStatus) {
            var errorCode = jqXHR.status;
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
    },

    __more_params__: function() {
        if (this.params) {
          var ret = "";
          _.map(this.params, function(val, name) {
            switch (typeof val) {
              case 'string':
                ret += '&'+name+'='+val;
                break;
              case 'array':
                var i=1;
                for (var item in val) {
                  ret += '&'+name+'member.'+i+'='+item;
                  i += 1;
                }
            }
          });
          return ret;
        }
        else {
          return "";
        }
    }
  });
  return EucaCollection;
});
