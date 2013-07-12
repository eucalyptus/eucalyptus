define([
    'models/eucacollection',
    'models/alarm'
], function(EucaCollection, Alarm) {
    var collection = EucaCollection.extend({
      model: Alarm,
      url: '/monitor?Action=DescribeAlarms'
    });
    return collection;
});
