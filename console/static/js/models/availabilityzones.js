define([
    'models/eucacollection',
    'models/availabilityzone'
], function(EucaCollection, AvailabilityZone) {
    var collection = EucaCollection.extend({
      model: AvailabilityZone,
      url: '/ec2?Action=DescribeAvailabilityZones'
    });
    return collection;
});
