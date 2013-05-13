define([
  'views/searches/generic',
  'views/searches/tagsearch',
  'app'
], function(Search, TagSearch, app) {
  return function(images) {

    var USER_ID = "601265054777";
    app.data.sgroup.each(function(securityGroup) {
      securityGroup = securityGroup.toJSON();
      if ('default' === securityGroup.name) {
        USER_ID = securityGroup.owner_id;
      }
    });
    var config = {
      facets: ['all_text', 'architecture', 'description', 'name',
        'ownerId', 'platform', 'root_device_type']

      , localize: {
        all_text: all_text_facet,
        ownerId: owner_facet,
        i386: '32-bit',
        x86_64: '64-bit',
        root_device_type: root_device_type_facet,
        ebs: ebs_type_facet,
        architecture: architecture_facet,
        platform: platform_facet,
        name: name_facet,
        description: description_facet
      }

      , match: {
        ownerId: function(search, item, add) {
          add('me');
        }
      }

      , search: {
        ownerId: function(search, facetSearch, item, itemsFacetValue, hit) {
          if (facetSearch === 'me') {
            if (itemsFacetValue === USER_ID) {
              hit();
            }
          }
        }
      }
    };

    return new Search(images, new TagSearch(config, images));
  }
});
