define(['models/sgroup'], function(sgroup) {
  return sgroup.extend({
      rules_egress: [], 
      __obj_name__: "SecurityGroup", 
      name: "", 
      tags: {}, 
      rules: [
        {
          from_port: "", 
          ip_protocol: "", 
          parent: null, 
          to_port: "", 
          item: "", 
          __obj_name__: "IPPermissions", 
          ipRanges: "", 
          groups: "", 
          grants: [
            {
              __obj_name__: "GroupOrCIDR", 
              name: "", 
              groupName: "", 
              userId: "", 
              owner_id: "", 
              item: "", 
              group_id: null, 
              cidr_ip: null
            }
          ]
        }
      ], 
      region: [], 
      description: "", 
      item: "", 
      connection: [], 
      vpc_id: null, 
      id: null, 
      owner_id: "",

      validation: {
        name: {
            required: true,
            msg: 'A security group selection is required.'
        }
      },

      finish: function(outputModel) {
        outputModel.set('security_group', this.get('name'));
        //outputModel.set('security_group', this.toJSON());
      }
  });
});
