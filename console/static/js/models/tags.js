define([
    'underscore',
    'backbone',
    'models/tag'
], function(_, Backbone, Tag) {
    return Backbone.Collection.extend({
        model: Tag,
        url: '/ec2?Action=DescribeTags',
        sync: function(method, model, options) {
          var self = this;
          if (method == 'read') {
              $.ajax({
                type:"POST",
                url: self.url,
                data:"_xsrf="+$.cookie('_xsrf'),
                dataType:"json",
                async:"true",
              }).done(function(data) {
                var results = data.results;
                _.each(results, function(r) { r.id = r.res_id + '-' + r.name; });
                if (results) {
                  options.success && options.success(model, results, options);
                }
              });
           }
        }
    });
});
