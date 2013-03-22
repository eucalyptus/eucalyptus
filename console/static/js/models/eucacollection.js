define([
], function() {
  var EucaCollection = Backbone.Collection.extend({
    sync: function(method, model, options) {
      var collection = this;
      if (method == 'read') {
        $.when(
          $.ajax({
            type:"POST",
            url: collection.url,
            data:"_xsrf="+$.cookie('_xsrf'),
            dataType:"json",
            async:"true",
          }),
          $.ajax({
            type:"POST",
            url: '/ec2?Action=DescribeTags',
            data: {
                "_xsrf": $.cookie('_xsrf'),
            },
            dataType:"json",
            async:"true",
          })
        ).done(
          // Success
          function(describe, tags) {
            //console.log('EUCACOLLECTION (success):', describe, tags);
            if (describe[0].results) {
              var results = describe[0].results;
              if (collection.namedColumns && tags && tags[0].results) {
                _.each(results, function(result) {
                  var tagById = _.groupBy(tags[0].results, 'res_id');
                  var tagSet = _.groupBy(tagById[result.id], 'name');
                  result.tags = tagSet;
                  _.each(collection.namedColumns, function(column) {
                    tagSet = _.groupBy(tagById[result[column]], 'name');
                    //console.log(column + ':' + result[column] + '->', tagSet);
                    result['display_' + column] = tagSet && tagSet.Name ? 
                                                  tagSet.Name[0].value : result[column];
                  });
                });
              }
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
            console.log('EUCACOLLECTION (error for +'+name+') : '+textStatus);
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
