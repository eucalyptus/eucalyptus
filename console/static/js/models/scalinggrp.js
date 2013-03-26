// scalinggrp model
//
define([
    './eucamodel',
],
 function(EucaModel) {
    var model = EucaModel.extend({
        validation: {
            name: {
                minLength: 1,
                required: true
            },
            min_size: {
                pattern: 'digits'
            },
            desired_capacity: {
                pattern: 'digits'
            },
            max_size: {
                pattern: 'digits'
            },
            default_cooldown: {
                pattern: 'digits'
            },
            instance: {
            },
            health_check_period: {
            },
            created_time: {
            },
            enabled_metrics: {
            },
            availability_zones: {
            },
            member: {
            },
            health_check_type: {
            },
            launch_config_name: {
            },
            placement_group: {
            },
            tags: {
            },
            suspended_processes: {
            },
            autoscaling_group_arn: {
            },
            load_balancers: {
            },
            termination_policies: {
            },
            connection: {
            },
            vpc_zone_identifier: {
            },
        }
    });
    return model;
});
