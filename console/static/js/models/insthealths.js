define([
    'models/eucacollection',
    'models/insthealth'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/elb?Action=DescribeInstanceHealth'
    });
});
