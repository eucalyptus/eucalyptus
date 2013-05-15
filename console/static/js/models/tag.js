// tag model
//

define([
  'backbone'
], function(Backbone) {
  var model = Backbone.Model.extend({
    validation: {
      // ====================
      // API Reference: 
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateTags.html
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DeleteTags.html
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeTags.html
      // ====================
        
      res_id: {
        pattern: /^\w{1,4}-\w{8}/,
        required: true
      },
      resource_type: {
        oneOf: [ 'customer-gateway', 'dhcp-options', 'image', 'instance', 'internet-gateway', 'network-acl', 'network-interface', 'reserved-instances', 'route-table', 'security-group', 'snapshot', 'spot-instances-request', 'subnet', 'volume', 'vpc', 'vpn-connection', 'vpn-gateway' ],
        required: false
      },
      name: {
        rangeLength: [1, 128],
        required: true
      },
      value: {
        rangeLength: [0, 256],
        required: false
      },
    },
    sync: function(method, model, options) {
      if (method == 'create' || method == 'update') {
        return this.syncMethod_Create(model, options);
      }
      else if (method == 'delete') {
        return this.syncMethod_Delete(model, options);
      }
    },
    syncMethod_Create: function(model, options){
      var data = "_xsrf="+$.cookie('_xsrf');
      data += "&ResourceId.1="+model.get('res_id');
      data += "&Tag.1.Key="+encodeURIComponent(model.get('name'));
      data += "&Tag.1.Value="+encodeURIComponent(model.get('value'));
      $.ajax({
        type:"POST",
        url:"/ec2?Action=CreateTags",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('tag_create_success', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')));
            } else {
              notifyError($.i18n.prop('tag_create_error', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')), undefined_error);
            } 
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('tag_create_error', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')), getErrorMessage(jqXHR));
          }
      });
    },
    syncMethod_Delete: function(model, options){
      var data = "_xsrf="+$.cookie('_xsrf');
      data += "&ResourceId.1="+model.get('res_id');
      data += "&Tag.1.Key="+model.get('name');
      data += "&Tag.1.Value="+model.get('value');
      $.ajax({
        type:"POST",
        url:"/ec2?Action=DeleteTags",
        data:data,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('tag_delete_success', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')));
            } else {
              notifyError($.i18n.prop('tag_delete_error', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('tag_delete_error', DefaultEncoder().encodeForHTML(model.get('name')), model.get('res_id')), getErrorMessage(jqXHR));
          }
      });
    }

  });
  return model;
});
