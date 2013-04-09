define([
  'backbone'
], function(Backbone) {
  var EucaCollection = Backbone.Collection.extend({
    sync: function(method, model, options) {
      var collection = this;
      if (method == 'read') {
        //console.log('EUCA COLLECTION: URL ' + collection.url);
        $.when(
          $.ajax({
            type:"POST",
            url: collection.url,
            data:"_xsrf="+$.cookie('_xsrf'),
            dataType:"json",
            async:"true",
          }),
          describe('tag')
        ).done(
          // Success
          function(describe, tags) {
            //console.log('EUCACOLLECTION (success):', collection.url, describe, tags);
            if (describe[0].results) {
              var results = describe[0].results;

              // always generate display_ values for named columns.
              if (collection.namedColumns) {
                _.each(results, function(result, index) {
                  if (typeof result.id == 'undefined') result.id = index;
                  if (tags && tags != '' && tags[0].results) {
                    var tagById = _.groupBy(tags[0].results, 'res_id');
                    var tagSet = _.groupBy(tagById[result.id], 'name');
                    result.tags = tagSet;
                  }
                  _.each(collection.namedColumns, function(column) {
                    var display_id = result[column];
                    if (tags && tags != '' && tags[0].results) {
                      tagSet = _.groupBy(tagById[result[column]], 'name');
                      if (tagSet && tagSet.Name) display_id = tagSet.Name[0].value;
                    }
                    //console.log(column + ':' + result[column] + '->', tagSet);
                    result['display_' + column] = display_id;
                  });
                });
              }
              _.each(results, function(result) {
                if (typeof result['id'] == undefined)
                  result['id'] = "d00d";
              });
              //console.log('MERGED:', results);
              options.success && options.success(model, results, options);
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
