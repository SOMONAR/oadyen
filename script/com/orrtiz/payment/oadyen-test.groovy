/*******************************************************************************
* This file is subject to the terms and conditions defined in the file 'LICENSE', 
* which is part of this (source code) package.
*
* Unless required by applicable law or agreed to in writing, software distributed 
* under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
* either express or implied.

* See the License for the specific language governing permissions and limitations
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

logPrefix = "in oadyen-test.groovy: "
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

String hostPort = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostPort.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "hostPort = " + hostPort,module);

String apiHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.api.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "apiHostName = " + apiHostName,module);

String palHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.pal.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "palHostName = " + palHostName,module);

String sdkHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.sdk.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "sdkHostName = " + sdkHostName,module);

String paymentMethodsPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.methods.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentMethodsPath = " + paymentMethodsPath,module);

String paymentResultPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.result.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentResultPath = " + paymentResultPath,module);

String checkoutSessionPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'checkout.session.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "checkoutSessionPath = " + checkoutSessionPath ,module);

String paymentAuthorisePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.authorise.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentAuthorisePath = " + paymentAuthorisePath,module);

String paymentAuthorise3dPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.authorise3d.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentAuthorise3dPath = " + paymentAuthorise3dPath,module);

String paymentCancelPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.cancel.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentCancelPath = " + paymentCancelPath,module);

String paymentCancelOrRefundPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.cancelOrRefund.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentCancelOrRefundPath = " + paymentCancelOrRefundPath,module);

String paymentCapturePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.capture.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentCapturePath = " + paymentCapturePath,module);

String paymentRefundPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.refund.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentRefundPath = " + paymentRefundPath,module);

String recurringDetailsPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'recurring.details.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "recurringDetailsPath = " + recurringDetailsPath,module);

String recurringDisablePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'recurring.disable.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "recurringDisablePath = " + recurringDisablePath,module);

String merchantAccount = EntityUtilProperties.getPropertyValue(systemResourceId, 'defaultMerchantAccount', delegator);
Debug.logInfo(logPrefix + "merchantAccount = " + merchantAccount,module);

Debug.logInfo(logPrefix + " --------------------------",module);
Debug.logInfo(logPrefix + " building the url + parameters",module);

String sessionUrl = hostProtocol + '://' + apiHostName + '/' +  checkoutSessionPath
Debug.logInfo(logPrefix + "sessionUrl = " + sessionUrl,module);

state = parameters.state
Debug.logInfo(logPrefix + "starting with state = " + state,module);

String typeId = parameters.typeId
Debug.logInfo(logPrefix + "typeId = " + typeId,module);

String orderId = parameters.orderId
Debug.logInfo(logPrefix + "orderId = " + orderId,module);

String currencyUomId = parameters.currencyUomId
Debug.logInfo(logPrefix + "currencyUomId = " + currencyUomId,module);

String amount = parameters.amount
Debug.logInfo(logPrefix + "amount = " + amount,module);

String description = parameters.description
Debug.logInfo(logPrefix + "description = " + description,module);

String sru = parameters._SERVER_ROOT_URL_;
Debug.logInfo(logPrefix + "_SERVER_ROOT_URL_ = " + sru,module);

String jsonString = "{" +
        "\"merchantAccount\":\"" + merchantAccount +"\" ," +
        "\"sdkVersion\":\"" + "1.6.0" +"\" ," +
        "\"channel\":\"" + "Web" +"\" ," +
        "\"amount\": {" + 
        "\"currency\":\"" + currencyUomId + "\"," +
        "\"value\":\"" + amount + "\"} ," +
        "\"reference\": \"" + orderId + "\" ," +
        "\"countryCode\":\"" + "NL" + "\"," +
        "\"shopperLocale\":\"" + locale + "\"," +
        "\"origin\":\"" + sru + "\"," +
        "\"returnUrl\":\"" +sru +"/oadyen/control/testReview" + "\"}";
        
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
    JSONToMap jsonMap = new JSONToMap();
    Map<String, Object> jsMap = jsonMap.convert(jsonObject);
    paymentSession = jsMap.paymentSession;
    
    Debug.logInfo(logPrefix + "paymentSession = " + paymentSession,module);
	Debug.logInfo(logPrefix + "--------------------------",module);
    parameters.paymentSession = paymentSession
    
	}
else {
    Debug.logInfo(logPrefix + " code = " + code ,module);
}

Debug.logInfo(logPrefix + " --------------------------",module);