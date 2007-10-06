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

package org.apache.synapse.core.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.statistics.StatisticsCollector;
import org.apache.synapse.statistics.StatisticsUtils;
import org.apache.synapse.util.UUIDGenerator;

/**
 * This is the Axis2 implementation of the SynapseEnvironment
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);

    private SynapseConfiguration synapseConfig;
    private ConfigurationContext configContext;

    /** The StatisticsCollector object */
    private StatisticsCollector statisticsCollector;

    public Axis2SynapseEnvironment() {}

    public Axis2SynapseEnvironment(ConfigurationContext cfgCtx,
        SynapseConfiguration synapseConfig) {

        this.configContext = cfgCtx;
        this.synapseConfig = synapseConfig;
    }

    public boolean injectMessage(final MessageContext synCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext");
        }
        synCtx.setEnvironment(this);
        if (synCtx.isResponse()) {
            //Process statistics related to a sequence which has send mediator as a child,end point
            StatisticsUtils.processEndPointStatistics(synCtx);
            StatisticsUtils.processProxyServiceStatistics(synCtx);
            StatisticsUtils.processSequenceStatistics(synCtx);
        }

        // if this is a response to a proxy service
        if (synCtx.getProperty(SynapseConstants.PROXY_SERVICE) != null) {

            if (synCtx.getConfiguration().getProxyService((String) synCtx.getProperty(
                    SynapseConstants.PROXY_SERVICE)).getTargetOutSequence() != null) {

                String sequenceName = synCtx.getConfiguration().getProxyService((String) synCtx.
                        getProperty(SynapseConstants.PROXY_SERVICE)).getTargetOutSequence();
                Mediator outSequence = synCtx.getSequence(sequenceName);

                if (outSequence != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Using the sequence named " + sequenceName
                                + " for the outgoing message mediation of the proxy service "
                                + synCtx.getProperty(SynapseConstants.PROXY_SERVICE));
                    }
                    outSequence.mediate(synCtx);
                } else {
                    log.error("Unable to find the out-sequence " +
                            "specified by the name " + sequenceName);
                    throw new SynapseException("Unable to find the " +
                            "out-sequence specified by the name " + sequenceName);
                }

            } else if (synCtx.getConfiguration().getProxyService((String) synCtx.getProperty(
                    SynapseConstants.PROXY_SERVICE)).getTargetInLineOutSequence() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using the anonymous out-sequence specified in the proxy service "
                            + synCtx.getProperty(SynapseConstants.PROXY_SERVICE)
                            + " for outgoing message mediation");
                }
                synCtx.getConfiguration().getProxyService((String) synCtx.getProperty(
                        SynapseConstants.PROXY_SERVICE)).getTargetInLineOutSequence().mediate(synCtx);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Proxy service " + synCtx.getProperty(SynapseConstants.PROXY_SERVICE)
                            + " does not specifies an out-sequence - sending the response back");
                }
                Axis2Sender.sendBack(synCtx);
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Using Main Sequence for injected message");
            }
            return synCtx.getMainSequence().mediate(synCtx);
        }
        return true;
    }

    public void injectAsync(final MessageContext synCtx, SequenceMediator seq) {
        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext for asynchronous mediation using the "
                + seq.getName() == null? "Anonymous" : seq.getName());
        }
        synCtx.setEnvironment(this);
        // todo: do we need to have this in here ? ruwan
        if (synCtx.isResponse()) {
            //Process statistics related to a sequence which has send mediator as a child,end point
            StatisticsUtils.processEndPointStatistics(synCtx);
            StatisticsUtils.processProxyServiceStatistics(synCtx);
            StatisticsUtils.processSequenceStatistics(synCtx);
        }

        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
            .getConfigurationContext().getThreadPool().execute(new SynapseWorker(seq, synCtx));

    }

    /**
     * This will be used for sending the message provided, to the endpoint specified by the
     * EndpointDefinition using the axis2 environment.
     *
     * @param endpoint - EndpointDefinition to be used to find the endpoint information
     *                      and the properties of the sending process
     * @param synCtx   - Synapse MessageContext to be sent
     */
    public void send(EndpointDefinition endpoint, MessageContext synCtx) {
        if (synCtx.isResponse()) {

            if (endpoint != null) {
                // not sure whether we need to collect statistics here
                StatisticsUtils.processEndPointStatistics(synCtx);
                StatisticsUtils.processProxyServiceStatistics(synCtx);
                StatisticsUtils.processAllSequenceStatistics(synCtx);

                Axis2Sender.sendOn(endpoint, synCtx);

            } else {
                Axis2Sender.sendBack(synCtx);
            }
        } else {
            Axis2Sender.sendOn(endpoint, synCtx);
        }
    }

    /**
     * This method will be used to create a new MessageContext in the Axis2 environment for
     * synapse. This will set all the relevant parts to the messagecontext, but for this message
     * context to be usefull creator has to fill in the data like envelope and operation context
     * and so on. This will set a default envelope of type soap12 and a new messageID for the
     * created message along with the ConfigurationContext is being set in to the message
     * correctly.
     *
     * @return Synapse MessageContext with the underlying axis2 message context set
     */
    public MessageContext createMessageContext() {

        if (log.isDebugEnabled()) {
            log.debug("Creating Message Context");
        }

        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(this.configContext);
        axis2MC.setServiceContext(new ServiceContext());
//        axis2MC.setOperationContext(new OperationContext());
        MessageContext mc = new Axis2MessageContext(axis2MC, synapseConfig, this);
        mc.setMessageID(UUIDGenerator.getUUID());
        try {
			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
		} catch (Exception e) {
			e.printStackTrace();
		}

        return mc;
    }

    /**
     * This method returns the StatisticsCollector
     *
     * @return Retruns the StatisticsCollector
     */
    public StatisticsCollector getStatisticsCollector() {
        return statisticsCollector;
    }

    /**
     * To set the StatisticsCollector
     *
     * @param collector - Statistics collector to be set
     */
    public void setStatisticsCollector(StatisticsCollector collector) {
        this.statisticsCollector = collector;
    }

    /**
     * This inner class will be used as the executer for the injectAsync method for the
     * sequence mediation
     */
    private class SynapseWorker implements Runnable {

        /** Mediator to be executed */
        private Mediator seq = null;

        /** MessageContext to be mediated using the mediator */
        private MessageContext synCtx = null;

        /**
         * Constructor of the SynapseWorker which sets the sequence and the message context
         *
         * @param seq    - Sequence Mediator to be set
         * @param synCtx - Synapse MessageContext to be set
         */
        public SynapseWorker(Mediator seq, MessageContext synCtx) {
            this.seq = seq;
            this.synCtx = synCtx;
        }

        /**
         * Constructor od the SynapseWorker which sets the provided message context and the
         * main sequence as the sequence for mediation
         *
         * @param synCtx - Synapse MessageContext to be set
         */
        public SynapseWorker(MessageContext synCtx) {
            this.synCtx = synCtx;
            seq = synCtx.getMainSequence();
        }

        /**
         * Execution method of the thread. This will just call the mediation of the specified
         * Synapse MessageContext using the specified Sequence Mediator
         */
        public void run() {
            seq.mediate(synCtx);
        }
    }

}
