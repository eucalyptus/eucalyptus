define([
    'models/eucacollection',
    'models/instance'
], function(EucaCollection, Instance) {
    var Instances = EucaCollection.extend({
	model: Instance,
	url: '/ec2?Action=DescribeInstances'
    });
    return Instances;
});
