define([
    'models/eucacollection',
    'models/zone'
], function(EucaCollection, Zone) {
    return EucaCollection.extend({
	model: Zone,
	url: '/ec2?Action=DescribeAvailabilityZones'
    });
});
