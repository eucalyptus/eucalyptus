define([
    'models/eucacollection',
    'models/alarm'
], function(EucaCollection, Alarm) {
    var collection = EucaCollection.extend({
      model: Alarm,
      url: '/ec2?Action=DescribeAlarms'
    });
    return collection;
});
