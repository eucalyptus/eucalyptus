define([], function() {
  return function(config) {
    console.log("Create a new config ", config);
    var self = this;
    
    for (var key in config) {
      if (!self[key]) {
          console.log("delegate " + key);
          self[key] = config[key];
      }
    }
  };
  
});
