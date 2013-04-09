define([
    'models/eucacollection',
    'models/eip'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/ec2?Action=DescribeAddresses'
    });
});
