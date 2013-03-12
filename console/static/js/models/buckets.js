define([
    'models/eucacollection',
    'models/bucket'
], function(EucaCollection, Bucket) {
    return EucaCollection.extend({
	model: Bucket,
	url: '/ec2?Action=DescribeBuckets'
    });
});
