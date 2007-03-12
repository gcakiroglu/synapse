/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.mediators.bsf;

import org.apache.axiom.om.impl.exception.XMLComparisonException;
import org.apache.synapse.mediators.AbstractTestCase;

/**
 *
 *
 */

public class ScriptMediatorSerializationTest extends AbstractTestCase {

    ScriptMediatorFactory mediatorFactory;
    ScriptMediatorSerializer scriptMediatorSerializer;
    InlineScriptMediatorSerializer inlineScriptMediatorSerializer;

    public ScriptMediatorSerializationTest() {
        mediatorFactory = new ScriptMediatorFactory();
        scriptMediatorSerializer = new ScriptMediatorSerializer();
        inlineScriptMediatorSerializer = new InlineScriptMediatorSerializer();
    }

    public void testScriptMediatorSerializationSenarioOne() throws XMLComparisonException {
        String inputXml = "<script xmlns=\"http://ws.apache.org/ns/synapse\" key=\"script-key\" function=\"funOne\"></script> ";
        assertTrue(serialization(inputXml, mediatorFactory, scriptMediatorSerializer));
        assertTrue(serialization(inputXml, scriptMediatorSerializer));
    }

    public void testScriptMediatorSerializationSenarioTwo() throws XMLComparisonException {
        String inputXml = "<script xmlns=\"http://ws.apache.org/ns/synapse\" key=\"script-key\" ></script> ";
        assertTrue(serialization(inputXml, mediatorFactory, scriptMediatorSerializer));
        assertTrue(serialization(inputXml, scriptMediatorSerializer));
    }

    public void testInlineScriptMediatorSerializationSenarioOne() throws XMLComparisonException {
        String inputXml = "<syn:script.js xmlns:syn=\"http://ws.apache.org/ns/synapse\" " +
                "><![CDATA[nvar symbol = mc.getPayloadXML()..*::Code.toString();mc.setPayloadXML(<m:getQuote xmlns:m=\"http://services.samples/xsd\">\n" +
                "<m:request><m:symbol>{symbol}</m:symbol></m:request></m:getQuote>);]]></syn:script.js> ";
        assertTrue(serialization(inputXml, mediatorFactory, inlineScriptMediatorSerializer));
        assertTrue(serialization(inputXml, inlineScriptMediatorSerializer));
    }
}
