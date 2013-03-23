define([
    'models/eucacollection',
    'models/address'
], function(EucaCollection, Address) {
    var Instances = EucaCollection.extend({
      model: Address,
      url: '/ec2?Action=DescribeAddresses'
    });
    return Addresses;
});
