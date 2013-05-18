define([
  'app',
  './eucaexpandoview',
  'text!./sgroup.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('sgroup', tmp);
      var ruleList = [];
      var rules = tmp.get('rules');
      if (rules) {
        for (var rule in rules) {
          var tmp = rules[rule];
          var protocol = tmp['ip_protocol'];
          var port = tmp['from_port'];
          if(tmp['to_port'] !== tmp['from_port'])
            port += '-'+tmp['to_port']; 
          var type = '';
          if(protocol === 'icmp'){
            // TODO : define icmp type
            ;
          }
          var portOrType = type ? type: port;
          var src = [];
          var grants = tmp['grants'];
          $.each(grants, function(idx, grant){
            if(grant.cidr_ip && grant.cidr_ip.length>0){
              src.push(grant.cidr_ip);
            }else if(grant.owner_id && grant.owner_id.length>0){
              if(self.model.get('owner_id') === grant.owner_id)
                src.push(grant.groupName);
              else
                src.push(grant.owner_id+'/'+grant.groupName);
            }
          });
          src = src.join(', '); 
          ruleList.push({protocol:protocol, port:portOrType, source:src});
        }
        this.model.set('rules', ruleList);
      }
      this.scope = this.model;
      this._do_init();
    
      var tmptags = this.model.get('sgroup').get('tags');
      tmptags.on('add remove reset sort sync', function() {
        self.render();
      });
    },
    remove : function() {
      this.model.destroy();
    }
  });
});
