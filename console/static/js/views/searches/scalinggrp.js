define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(sgroups) {
    var config = {
      field: 'scalinggrp',
      facets: ['all_text', 'launch_config_name', 'health_check_type', 'availability_zones'],
      localize: {
        availability_zones: app.msg('search_facet_instance_az'),
        launch_config_name: app.msg('search_facet_scalinggroup_lc'),
        EC2 : app.msg('search_facet_scalinggroup_ec2') //'Compute'
      }
    }
    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
