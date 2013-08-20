define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(instances) {
    var config = {
      facets: ['state', 'root_device_name', 'group_name',
        'placement', 'instance_type']
      , localize: {
        root_device_name: app.msg('search_facet_instance_root_device'), //'Root Device',
        group_name: app.msg('search_facet_instance_sgroup'), //'Scaling Group',
        placement: app.msg('search_facet_instance_az'), //'Availability Zone',
        state: app.msg('search_facet_instance_status'), //'Status'
        instance_type: app.msg('search_facet_instance_type')
      }
    };
    return new Search(instances, new TagSearch(config, instances));
  }
});
