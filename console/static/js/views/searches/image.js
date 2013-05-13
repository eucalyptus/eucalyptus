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
        ownerId: app.msg('search_facet_image_owner'), //'Owner',
        i386 : app.msg('search_facet_image_i386'), //'32-bit',
        x86_64 : app.msg('search_facet_image_x86_64'), //'64-bit',
        root_device_type : app.msg('search_facet_image_root_device'), //'Root Device',
        ebs : app.msg('search_facet_image_ebs'), //'EBS',
        platform: app.msg('search_facet_image_platform'), //'Platform'
        architecture: app.msg('search_facet_image_arch'), //'Architecture'
        description: app.msg('search_facet_image_desc'), //'Description'
        name: app.msg('search_facet_image_name'),
        all_text: app.msg('search_facet_alltext')
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
