define([
    'models/eucacollection',
    'models/balancer'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/elb?Action=DescribeLoadBalancers'
    });
});
