define([
    'models/eucacollection',
    'models/loadbalancer'
], function(EucaCollection, LoadBalancer) {
    var collection = EucaCollection.extend({
      model: LoadBalancer,
      url: '/elb?Action=DescribeLoadBalancers'
    });
    return collection;
});
