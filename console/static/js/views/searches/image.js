define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(images) {

    var USER_ID = "601265054777"; //FIXME

    var config = {
      facets: ['all_text', 'architecture', 'description', 'name',
        'ownerId', 'platform', 'root_device_type']

      , localize: {
        ownerId: 'Owner',
        i386 : '32-bit',
        x86_64 : '64-bit',
        root_device_type : 'Root Device',
        ebs : 'EBS'
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
