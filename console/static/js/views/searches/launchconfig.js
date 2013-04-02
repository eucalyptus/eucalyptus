define([
  'app',
  'views/searches/generic',
], function(app, Search) {

  // FIXME - Ean - how do we trigger updates
  // to imageForID if the data changes?
  // Or is there a handy way to dynamically look
  // up an image by ID without ending up iterating
  // imageCount * launchConfigCount times?

  return function(images) {

    var imageForID = {
    };
    // FIXME - app.data.images *has* contents, but
    // the result of toJSON() is empty - not sure
    // what to do here
    app.data.images.toJSON().forEach(function(image) {
      console.log("IMAGE ", image);
      imageForID[image.id] = {
        root_device_type: image.root_device_type,
        platform: image.platform
      };
      console.log("ADD IMAGE ", imageForID[image.id]);
    });


    var config = {
      facets: ['all_text', 'os', 'instance_type'],
      localize: {
        state: 'Status',
        't1.micro': 'Micro',
        'm1.small': 'Standard',
        'c1.medium': 'High Memory',
        'm1.large': 'High CPU',
        'os': 'Operating System'
      },
      match: {
        os: function(search, item, add) {
          var img = imageForID[item.image_id];
          if (img && img.platform) {
            add(img.platform);
          }
        }
      },
      search: {
        os: function(search, facetSearch, item, itemsFacetValue, hit) {
          console.log("ITEM", item);
          var img = imageForID[item.image_id];
          console.log("IMAGE FOR ID", img);
          if (img && facetSearch === img.platform) {
            hit();
          }
          return true;
        }
      }
    }
    return new Search(images, config);
  }
});
