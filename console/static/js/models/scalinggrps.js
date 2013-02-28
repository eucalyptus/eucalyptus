define([
    'models/eucacollection',
    'models/scalinggrp'
], function(EucaCollection, Model) {
    return EucaCollection.extend({
	model: Model,
	url: '/autoscaling?Action=DescribeAutoScalingGroups'
    });
});
