define([
  'views/searches/generic',
], function(Search) {
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
        root_device : function(search, item, add) {
          console.log("ITEM IS ", item);
        },
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

    return new Search(images, config);
  }
});
