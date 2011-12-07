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
package org.apache.synapse.samples.framework;

import java.util.Properties;

/**
 * Stores the results after executing a client
 */
public class SampleClientResult {

    private boolean gotResponse;
    private boolean isFinished;
    private Exception exception;
    private Properties clientProperties = new Properties();

    public void setGotResponse(boolean gotResponse) {
        this.gotResponse = gotResponse;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public boolean gotResponse() {
        return gotResponse;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public void addProperty(String pName, String pValue){
        clientProperties.setProperty(pName,pValue);
    }

    public String getProperty(String pName){
        return clientProperties.getProperty(pName);
    }
}