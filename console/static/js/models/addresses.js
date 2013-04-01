define([
    'models/eucacollection',
    'models/address'
], function(EucaCollection, Address) {
    var collection = EucaCollection.extend({
      model: Address,
      url: '/ec2?Action=DescribeAddresses'
    });
    return collection;
});
