define([
    'models/eucacollection',
    'models/scalinggrp'
], function(EucaCollection, ScalingGrp) {
    var collection = EucaCollection.extend({
	  model: ScalingGrp,
	  url: '/autoscaling?Action=DescribeAutoScalingGroups'
    });
    return collection;
});
