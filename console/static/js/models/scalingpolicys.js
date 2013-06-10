define([
    'models/eucacollection',
    'models/scalingpolicy'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/autoscaling?Action=DescribePolicies'
    });
});
