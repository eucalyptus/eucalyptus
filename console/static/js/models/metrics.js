define([
    'models/eucacollection',
    'models/metric'
], function(EucaCollection, Metric) {
    var collection = EucaCollection.extend({
      model: Metric,
      url: '/monitor?Action=ListMetrics'
    });
    return collection;
});
