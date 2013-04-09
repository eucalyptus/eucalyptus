define([
    'models/eucacollection',
    'models/sgroup'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/ec2?Action=DescribeSecurityGroups'
    });
});
