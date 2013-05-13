define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(instances) {
    var config = {
      facets: ['state', 'root_device_name', 'group_name',
        'placement', 'instance_type'],
      localize: {
        root_device_name: root_device_name_facet,
        group_name: group_name_facet,
        placement: availability_zones_facet,
        state: status_facet,
        instance_type: instance_type_facet
      }
    };
    return new Search(instances, new TagSearch(config, instances));
  }
});
