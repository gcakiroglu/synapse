package org.apache.synapse.receivers;

import org.apache.axis2.engine.*;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.clientapi.MessageSender;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 5:54:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseMessageReceiver extends SynapseAbstractMessageReceiver
        implements MessageReceiver {

    /**
     * Field log
     */
    protected Log log = LogFactory.getLog(getClass());

    /**
     * Field scope
     */
    private String scope;

    /**
     * Field method
     */
    private Method method;

    /**
     * Field classLoader
     */
    private ClassLoader classLoader;

    /**
     * Constructor RawXMLProvider
     */
    public SynapseMessageReceiver() {
        scope = Constants.APPLICATION_SCOPE;
    }

    public void invokeBusinessLogic(MessageContext msgContext)
            throws AxisFault {
        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            /**
             * Injecting messageContext for medaite method
             * Will not be on use in the M1
             */

            Integer oldSynapseState = (Integer) msgContext
                    .getProperty(SynapseConstants.SYNAPSE_STATE);
            Boolean mediatorState = DependencyManager
                    .mediatorBusinessLogicProvider(obj, msgContext);

            Integer newSynapseState = new Integer(
                    (oldSynapseState.intValue()) + 1);
            msgContext.setProperty(SynapseConstants.SYNAPSE_STATE,
                        newSynapseState);

            if (mediatorState.booleanValue()) {
                /**
                 * New AxisEngine is created and loops for more rules
                 */
                moreRules(msgContext);

            } else {
                /**
                 * Mediation is successful and route the message to the relevant location.
                 */
                synapseAsClient(msgContext);

            }

        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }

    }

    private void moreRules(MessageContext returnMsgCtx)
            throws AxisFault {

        /**
         * states related to Synapse
         */
        MessageContext newContext = new MessageContext(
                returnMsgCtx.getSystemContext());
        newContext.setProperty(SynapseConstants.MEDEATOT_STATE,
                returnMsgCtx.getProperty(SynapseConstants.MEDEATOT_STATE));

        newContext.setProperty(SynapseConstants.SYNAPSE_STATE,
                returnMsgCtx.getProperty(SynapseConstants.SYNAPSE_STATE));
        newContext.setProperty(SynapseConstants.VALUE_FALSE,
                returnMsgCtx.getProperty(SynapseConstants.VALUE_FALSE));
        newContext.setProperty(SynapseConstants.RULE_STATE,
                returnMsgCtx.getProperty(SynapseConstants.RULE_STATE));
        newContext.setProperty(
                SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER,
                returnMsgCtx.getProperty(
                        SynapseConstants.SynapseRuleEngine.SYNAPSE_RECEIVER));
        newContext.setProperty(
                SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST,
                returnMsgCtx.getProperty(
                        SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST));
        newContext.setServerSide(true);
        newContext.setEnvelope(returnMsgCtx.getEnvelope());
        newContext.setServiceContextID(returnMsgCtx.getServiceContextID());

        /**
         * Need to understand the looping
         */
        AxisEngine engine = new AxisEngine(returnMsgCtx.getSystemContext());
        engine.receive(newContext);

    }

    private void synapseAsClient(MessageContext msgCtx) throws AxisFault {
        MessageSender msgSender = new MessageSender();

        msgSender.setTo(new EndpointReference(
                "http://localhost:8080/axis2/services/MyService"));
        msgSender.setSenderTransport(Constants.TRANSPORT_HTTP);

        msgSender.send("", msgCtx.getEnvelope().getBody().getFirstElement());
    }
}
