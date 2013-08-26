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
    app.data.images.toJSON().forEach(function(launchConfig) {
      imageForID[launchConfig.id] = {
        root_device_type: launchConfig.root_device_type,
        platform: launchConfig.platform
      };
    });
    var groupForID = {
    };
    app.data.scalinggrp.toJSON().forEach(function(group) {
      groupForID[group.launch_config_name] = {
        availability_zone: group.availability_zones[0],
        scaling_group: group.name
      };
    });

    var config = {
      field: 'launchconfig',
      facets: ['all_text', 'instance_type', 'availability_zone', 'root_device_type', 'scaling_group'],
      localize: {
        state: app.msg('search_facet_launchconfig_status'), //'Status',
        'os': app.msg('search_facet_launchconfig_os'), //'Operating System'
        ebs : app.msg('search_facet_image_ebs') //'EBS'
      },
      match: {
        availability_zone: function(search, item, add) {
          var found = {};
          for (var key in groupForID) {
            var val = groupForID[key];
            if (!found[val.availability_zone]) {
              found[val.availability_zone] = true;
              add(val.availability_zone);
            }
          }
        },
        scaling_group: function(search, item, add) {
          var found = {};
          for (var key in groupForID) {
            var val = groupForID[key];
            found[val.scaling_group] = true;
            add(val.scaling_group);
          }
        },
        root_device_type: function(search, item, add) {
          var found = {};
          for (var key in imageForID) {
            var val = imageForID[key];
            if (!found[val.root_device_type]) {
              found[val.root_device_type] = true;
              add(val.root_device_type);
            }
          }
        }
      },
      search: {
        availability_zone: function(search, facetSearch, item, itemsFacetValue, hit) {
          var group = groupForID[item.name];
          if (group && facetSearch === group.availability_zone) {
            hit();
          }
          return true;
        },
        scaling_group: function(search, facetSearch, item, itemsFacetValue, hit) {
          var group = groupForID[item.name];
          if (group && facetSearch === group.scaling_group) {
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
