define([
    'models/eucacollection',
    'models/scalinggrp'
], function(EucaCollection, ScalingGrp) {
    return EucaCollection.extend({
	model: ScalingGrp,
	url: '/autoscaling?Action=DescribeAutoScalingGroups'
    });
});
