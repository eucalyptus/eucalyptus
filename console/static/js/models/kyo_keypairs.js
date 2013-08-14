define([
    'models/eucacollection',
    'models/keypair'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/ec2?Action=DescribeKeyPairs'
    });
});
