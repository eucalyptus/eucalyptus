define([
    'models/eucacollection',
    'models/summary'
], function(EucaCollection, Summary) {
    return EucaCollection.extend({
	model: Summary,
	url: '/ec2?Action=GetCacheSummary'
    });
});
