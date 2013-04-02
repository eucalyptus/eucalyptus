define([
    'models/eucacollection',
    'models/snapshot'
], function(EucaCollection, Snapshot) {
    var Snapshots = EucaCollection.extend({
	model: Snapshot,
	url: '/ec2?Action=DescribeSnapshots',
    });
    return Snapshots;
});
