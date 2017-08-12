
package org.kie.remote.services.ws.deployment.generated;

import javax.xml.ws.WebFault;
import org.kie.remote.services.ws.common.WebServiceFaultInfo;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "DeploymentServiceException", targetNamespace = "http://services.remote.kie.org/6.5.1.1/deployment")
public class DeploymentWebServiceException
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private WebServiceFaultInfo faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public DeploymentWebServiceException(String message, WebServiceFaultInfo faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param cause
     * @param message
     */
    public DeploymentWebServiceException(String message, WebServiceFaultInfo faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.kie.remote.services.ws.common.WebServiceFaultInfo
     */
    public WebServiceFaultInfo getFaultInfo() {
        return faultInfo;
    }

}