/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.message.processors.MessageProcessor;
import org.apache.synapse.message.store.InMemoryMessageStore;
import org.apache.synapse.message.store.MessageStore;
import org.apache.axis2.util.JavaUtils;


import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

/**
 * Create an instance of the given Message Store, and sets properties on it.
 * <p/>
 * &lt;messageStore name="string" class="classname" [sequence = "string" ]&gt;
 * &lt;<processor class="classname">&gt;
 * &lt;</processor>&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * .
 * .
 * &lt;/messageStore&gt;
 */
public class MessageStoreFactory {

    private static final Log log = LogFactory.getLog(MessageStoreFactory.class);

    public static final QName CLASS_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "class");
    public static final QName NAME_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName SEQUENCE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "sequence");

    public static final QName PROCESSOR_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "processor");
    public static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "parameter");
    private static final QName DESCRIPTION_Q
            = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "description");


    @SuppressWarnings({"UnusedDeclaration"})
    public static MessageStore createMessageStore(OMElement elem, Properties properties) {

        OMAttribute clss = elem.getAttribute(CLASS_Q);
        MessageStore messageStore;
        if (clss != null) {
            try {
                Class cls = Class.forName(clss.getAttributeValue());
                messageStore = (MessageStore) cls.newInstance();
            } catch (Exception e) {
                handleException("Error while instantiating the message store", e);
                return null;
            }
        } else {
            messageStore = new InMemoryMessageStore();
        }


        OMAttribute nameAtt = elem.getAttribute(NAME_Q);

        if (nameAtt != null) {
            messageStore.setName(nameAtt.getAttributeValue());
        } else {
            handleException("Message Store name not specified");
        }

        OMAttribute sequenceAtt = elem.getAttribute(SEQUENCE_Q);
        if (sequenceAtt != null) {
            messageStore.setSequence(sequenceAtt.getAttributeValue());
        }

        OMElement descriptionElem = elem.getFirstChildWithName(DESCRIPTION_Q);
        if (descriptionElem != null) {
            messageStore.setDescription(descriptionElem.getText());
        }

        messageStore.setParameters(getParameters(elem));

        OMElement processorElm = elem.getFirstChildWithName(PROCESSOR_Q);
        MessageProcessor processor = null;
        if (processorElm != null) {
            processor = populateMessageProcessor(processorElm);
        } else {
            log.warn("Creating a Message Store without ");
        }

        if(processor != null) {
            messageStore.setMessageProcessor(processor);
        } else {
            log.warn("Message Store Created with out a Message processor. ");
        }
        return messageStore;
    }


    private static Map<String, Object> getParameters(OMElement elem) {
        Iterator params = elem.getChildrenWithName(PARAMETER_Q);
        Map<String, Object> parameters = new HashMap<String, Object>();

        while (params.hasNext()) {
            Object o = params.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute paramName = prop.getAttribute(NAME_Q);
                String paramValue = prop.getText();
                if (paramName != null) {
                    if (paramValue != null) {
                        parameters.put(paramName.getAttributeValue(), paramValue);
                    }
                } else {
                    handleException("Invalid MessageStore parameter - Parameter must have a name ");
                }
            }
        }
        return parameters;
    }

    /**
     * Populate the Message Processor
     *
     * @return
     */
    private static MessageProcessor populateMessageProcessor(OMElement element) {
        OMAttribute classAtt = element.getAttribute(CLASS_Q);
        MessageProcessor processor = null;
        if (classAtt != null) {
            String className = classAtt.getAttributeValue();
            try {
                Class cls = Class.forName(className);
                if (cls != null) {
                    processor = (MessageProcessor) cls.newInstance();
                    Iterator params = element.getChildrenWithName(PARAMETER_Q);
                    Map<String, Object> parameters = new HashMap<String, Object>();

                    while (params.hasNext()) {
                        Object o = params.next();
                        if (o instanceof OMElement) {
                            OMElement prop = (OMElement) o;
                            OMAttribute paramName = prop.getAttribute(NAME_Q);
                            String paramValue = prop.getText();
                            if (paramName != null) {
                                if (paramValue != null) {
                                    parameters.put(paramName.getAttributeValue(), paramValue);
                                }
                            } else {
                                handleException("Invalid Message Processor parameter - Parameter must have a name ");
                            }
                        }
                    }
                    processor.setParameters(parameters);
                } else {
                    throw new SynapseException("Can't find Class " + className);
                }
            } catch (Exception e) {
                log.error("Error while Creating MessageProcessor " + e.getMessage());
                throw new SynapseException(e);
            }
        } else {
            log.warn("Creating Message Store with out a Message processor processor");
        }

        return processor;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
