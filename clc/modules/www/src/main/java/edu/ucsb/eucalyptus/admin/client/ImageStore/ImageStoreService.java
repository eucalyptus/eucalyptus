package edu.ucsb.eucalyptus.admin.client.ImageStore;

import java.io.IOException;
import java.io.Serializable;

import java.util.AbstractMap;

import com.google.gwt.user.client.rpc.RemoteService;  
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.IsSerializable;


@RemoteServiceRelativePath("ImageStoreService")
public interface ImageStoreService extends RemoteService {

    String requestJSON(String sessionId, Method method, String uri,
                       Parameter[] params);

    public static enum Method implements IsSerializable { GET, POST; }

    public static class Parameter implements IsSerializable {
        private String name = "";
        private String value = "";

        public Parameter() {}

        public Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
