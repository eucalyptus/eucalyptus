define([
    'models/eucacollection',
    'models/bucket'
], function(EucaCollection, Bucket) {
    return EucaCollection.extend({
	model: Bucket,
	url: '/s3?Action=DescribeBuckets'
    });
});
