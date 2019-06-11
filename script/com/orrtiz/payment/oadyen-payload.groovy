/*******************************************************************************
 * (C) Copyright 2018 Somonar B.V.
 * Licensed under the Apache License under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.orrtiz.payment.adyen;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


import org.ofbiz.base.conversion.JSONConverters.JSONToMap;
import org.ofbiz.base.lang.JSON;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilIO;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.util.EntityUtilProperties;

String systemResourceId="oadyen"
String resource = "oadyen-UiLabels";
String resourceErr = "oadyen-ErrorUiLabels";
String commonResource = "CommonUiLabels";
Locale locale = (Locale) context.get("locale");

logPrefix = "in oadyen-payload.groovy: "
Debug.logInfo(logPrefix + "--------------------------",module);
Debug.logInfo(logPrefix + "set generics",module);
Debug.logInfo(logPrefix + "locale = " + locale,module);
Debug.logInfo(logPrefix + "get configuration variables",module);

String apiEnvironment = EntityUtilProperties.getPropertyValue(systemResourceId, 'apiEnvironment', delegator);
Debug.logInfo(logPrefix + "apiEnvironment = " + apiEnvironment,module);

String apiKey = EntityUtilProperties.getPropertyValue(systemResourceId, 'apiKey.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "apiKey = " + apiKey,module);

String hostProtocol = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostProtocol.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "hostProtocol = " + hostProtocol,module);

String apiHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.api.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "apiHostName = " + apiHostName,module);

String paymentResultPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.result.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentResultPath = " + paymentResultPath,module);

String sessionUrl = hostProtocol + '://' + apiHostName + '/' +  paymentResultPath
Debug.logInfo(logPrefix + "sessionUrl = " + sessionUrl,module);

//Debug.logInfo(logPrefix + "parameters = " + parameters,module);
String payload = parameters.payload;
Debug.logInfo(logPrefix + "payload = " + payload,module);

String jsonString = "{" +
		"\"payload\":\"" + payload +  "\"}";
Debug.logInfo(logPrefix + "jsonString = " + jsonString,module);

Debug.logInfo(logPrefix + "--------------------------",module);
Debug.logInfo(logPrefix + "setting up the session",module);
HttpClient http = new HttpClient(sessionUrl);
http.setHostVerificationLevel(SSLUtil.HOSTCERT_NO_CHECK);
http.setAllowUntrusted(true);
http.setDebug(true);

Map <String, String> headers = new LinkedHashMap <String, String>();
headers.put("contenttype", "application/json");
headers.put("X-API-Key", apiKey);

http.setHeaders(headers);
http.setContentType("application/json");
http.setKeepAlive(true);

String retResponse = null;
try {
    retResponse = http.post(jsonString)
}
catch (Exception e) {
    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "problemsConnectingWithadyen", locale));
    return "error";
}

int code = 0;
try {
    code = http.getResponseCode();
}
catch (HttpClientException e) {
    Debug.logError(e, "adyen response code not receiving", module);
    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "problemsgettingresponsecodeadyen", locale));
    return "error";
}

// successful response

if (code == 200){
	Debug.logInfo(logPrefix + "--------------------------",module);
    Debug.logInfo(logPrefix + " code = " + code ,module);
    Debug.logInfo(logPrefix + " retResponse = " + retResponse ,module);
    
	Debug.logInfo(logPrefix + "--------------------------",module);
    JSON jsonObject = JSON.from(retResponse)
	Debug.logInfo(logPrefix + "--------------------------",module);
    JSONToMap jsonMap = new JSONToMap();
    Map<String, Object> jsMap = jsonMap.convert(jsonObject);
    pspReference = jsMap.pspReference
    parameters.pspReference = pspReference
    Debug.logInfo(logPrefix + "pspReference = " + pspReference,module);
    authResponse = jsMap.authResponse
    parameters.authResponse = authResponse
    Debug.logInfo(logPrefix + "authResponse = " + authResponse,module);
    merchantReference = jsMap.merchantReference
    parameters.orderId = merchantReference
    parameters.merchantReference = merchantReference
    Debug.logInfo(logPrefix + "merchantReference = " + merchantReference,module);
    paymentMethod = jsMap.paymentMethod
    parameters.paymentMethod = paymentMethod
    Debug.logInfo(logPrefix + "paymentMethod = " + paymentMethod,module);
    additionalData = jsMap.additionalData
    parameters.additionalData = additionalData
    Debug.logInfo(logPrefix + "additionalData = " + additionalData,module);
    
    
	}
else {
    Debug.logInfo(logPrefix + " code = " + code ,module);
}