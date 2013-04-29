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
//          options.success && options.success(model, results, options);
            options.success && options.success(results);
          }
        });
      }
    },
    clone: function(clean, exclude) {  // specifying clean=true indicates only key and value will be copied
      if (typeof clean != undefined && clean == true) {
        var ret = new this.constructor();
        for (var i=0; i<this.length; i+=1) {
          var tmp = this.at(i);
          if (typeof exclude != undefined && exclude(tmp)) {
            continue;
          }
          ret.add(new Tag({name:tmp.get('name'), value:tmp.get('value')}));
        }
        return ret;
      } else {
        return this.clone();
      }
    }
  });
});
