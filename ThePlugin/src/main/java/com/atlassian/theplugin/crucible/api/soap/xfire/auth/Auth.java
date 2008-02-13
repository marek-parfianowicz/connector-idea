
package com.atlassian.theplugin.crucible.api.soap.xfire.auth;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import com.atlassian.theplugin.crucible.api.soap.xfire.auth.RpcAuthServiceName;

/**
 * This class was generated by Apache CXF (incubator) 2.0.4-incubator
 * Tue Feb 12 14:48:58 CET 2008
 * Generated source version: 2.0.4-incubator
 * 
 */

@WebServiceClient(name = "Auth", targetNamespace = "http://rpc.spi.crucible.atlassian.com/", wsdlLocation = "file:/C:/Dev/ThePlugin/src/main/java/com/atlassian/theplugin/crucible/api/soap/auth.wsdl")
public class Auth extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://rpc.spi.crucible.atlassian.com/", "Auth");
    public final static QName AuthPort = new QName("http://rpc.spi.crucible.atlassian.com/", "AuthPort");
    static {
        URL url = null;
        try {
            url = new URL("file:/C:/Dev/ThePlugin/src/main/java/com/atlassian/theplugin/crucible/api/soap/auth.wsdl");
        } catch (MalformedURLException e) {
            System.err.println("Can not initialize the default wsdl from file:/C:/Dev/ThePlugin/src/main/java/com/atlassian/theplugin/crucible/api/soap/auth.wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public Auth(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Auth(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Auth() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return
     *     returns AuthPort
     */
    @WebEndpoint(name = "AuthPort")
    public RpcAuthServiceName getAuthPort() {
        return (RpcAuthServiceName)super.getPort(AuthPort, RpcAuthServiceName.class);
    }

}
