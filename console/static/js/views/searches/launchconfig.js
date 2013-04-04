define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {

  // FIXME - Ean - how do we trigger updates
  // to imageForID if the data changes?
  // Or is there a handy way to dynamically look
  // up an image by ID without ending up iterating
  // imageCount * launchConfigCount times?

  return function(launchConfigs) {

    var imageForID = {
    };
    // FIXME - app.data.images *has* contents, but
    // the result of toJSON() is empty - not sure
    // what to do here
    app.data.images.toJSON().forEach(function(launhConfig) {
      imageForID[launhConfig.id] = {
        root_device_type: launhConfig.root_device_type,
        platform: launhConfig.platform
      };
    });


    var config = {
      facets: ['all_text', 'os', 'instance_type', 'root_device_type'],
      localize: {
        state: 'Status',
        't1.micro': 'T1 Micro',
        'm1.small': 'M1 Small',
        'c1.medium': 'C1 Medium',
        'm1.large': 'M1 Large',
        'os': 'Operating System'
      },
      match: {
        os: function(search, item, add) {
          var img = imageForID[item.image_id];
          if (img && img.platform) {
            add(img.platform);
          }
        },
        root_device_type: function(search, item, add) {
          var img = imageForID[item.image_id];
          if (img && img.root_device_type == item.root_device_type) {
            add(img.root_device_type);
          }
        }
      },
      search: {
        os: function(search, facetSearch, item, itemsFacetValue, hit) {
          var img = imageForID[item.image_id];
          if (img && facetSearch === img.platform) {
            hit();
          }
          return true;
        },
        root_device_type: function(search, facetSearch, item, itemsFacetValue, hit) {
          var img = imageForID[item.image_id];
          if (img && facetSearch === img.root_device_type) {
            hit();
          }
          return true;
        }
      }
    }
    return new Search(launchConfigs, new TagSearch(config, launchConfigs));
  }
});
