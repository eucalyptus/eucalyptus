define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(instances) {
    var config = {
      field: 'scalinginst',
      facets: ['availability_zone', 'launch_config_name', 'lifecycle_state']
      , localize: {
        availability_zone: app.msg('search_facet_instance_az'),
        launch_config_name: app.msg('search_facet_scalinggroup_lc'),
        lifecycle_state: app.msg('search_facet_scalinggroup_lifecycle_state'),
        //root_device_name: app.msg('search_facet_instance_root_device'), //'Root Device',
        //group_name: app.msg('search_facet_instance_sgroup'), //'Scaling Group',
        //placement: app.msg('search_facet_instance_az'), //'Availability Zone',
        //state: app.msg('search_facet_instance_status'), //'Status'
        //instance_type: app.msg('search_facet_instance_type')
      }
    };
    return new Search(instances, new TagSearch(config, instances));
  }
});
