define([
    'models/eucacollection',
    'models/region'
], function(EucaCollection, Region) {
    var collection = EucaCollection.extend({
      model: Region,
      url: '/ec2?Action=DescribeRegions'
    });
    return collection;
});
