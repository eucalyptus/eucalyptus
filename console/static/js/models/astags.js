define(['./astag'], function(ASTag) {
  return Backbone.Collection.extend({
    //url: '/autoscaling?Action=DescribeTags',
    model: ASTag,
    url: function(models) {
      console.log('fetching AS tags', models);
      var uri = '/autoscaling?Action=DescribeTags';
      _.each(models, function(model, i) {
        uri += '&Filters.member.' + (i+1) + '.auto-scaling-group=' + model.get('name');
      });
      return uri;
    }
  });
});
