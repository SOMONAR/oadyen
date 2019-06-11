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

import java.math.BigDecimal;
import java.util.Collection;

import org.ofbiz.base.conversion.JSONConverters.JSONToMap;
import org.ofbiz.base.lang.JSON;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.contact.ContactHelper;

logPrefix = "in oadyen.groovy: "
Debug.logInfo(logPrefix + "--------------------------",module);

Debug.logInfo(logPrefix + "set generics",module);

String systemResourceId="oadyen"
String resource = "oadyen-UiLabels";
String resourceErr = "oadyen-ErrorUiLabels";
String commonResource = "CommonUiLabels";
Locale locale = (Locale) context.get("locale");

//Debug.logInfo(logPrefix + "get configuration variables",module);

String apiEnvironment = EntityUtilProperties.getPropertyValue(systemResourceId, 'apiEnvironment', delegator);
//Debug.logInfo(logPrefix + "apiEnvironment = " + apiEnvironment,module);

String apiKey = EntityUtilProperties.getPropertyValue(systemResourceId, 'apiKey.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "apiKey = " + apiKey,module);

String hostProtocol = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostProtocol.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "hostProtocol = " + hostProtocol,module);

String hostPort = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostPort.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "hostPort = " + hostPort,module);

String apiHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.api.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "apiHostName = " + apiHostName,module);

String palHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.pal.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "palHostName = " + palHostName,module);

String sdkHostName = EntityUtilProperties.getPropertyValue(systemResourceId, 'HostName.sdk.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "sdkHostName = " + sdkHostName,module);

String paymentMethodsPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.methods.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentMethodsPath = " + paymentMethodsPath,module);

String paymentResultPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.result.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentResultPath = " + paymentResultPath,module);

String checkoutSessionPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'checkout.session.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "checkoutSessionPath = " + checkoutSessionPath ,module);

String paymentAuthorisePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.authorise.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentAuthorisePath = " + paymentAuthorisePath,module);

String paymentAuthorise3dPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.authorise3d.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentAuthorise3dPath = " + paymentAuthorise3dPath,module);

String paymentCancelPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.cancel.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "paymentCancelPath = " + paymentCancelPath,module);

String paymentCancelOrRefundPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.cancelOrRefund.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentCancelOrRefundPath = " + paymentCancelOrRefundPath,module);

String paymentCapturePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.capture.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentCapturePath = " + paymentCapturePath,module);

String paymentRefundPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'payment.refund.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "paymentRefundPath = " + paymentRefundPath,module);

String recurringDetailsPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'recurring.details.' + apiEnvironment, delegator);
Debug.logInfo(logPrefix + "recurringDetailsPath = " + recurringDetailsPath,module);

String recurringDisablePath = EntityUtilProperties.getPropertyValue(systemResourceId, 'recurring.disable.' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "recurringDisablePath = " + recurringDisablePath,module);

String merchantAccount = EntityUtilProperties.getPropertyValue(systemResourceId, 'defaultMerchantAccount', delegator);
//Debug.logInfo(logPrefix + "merchantAccount = " + merchantAccount,module);

//Debug.logInfo(logPrefix + " --------------------------",module);
//Debug.logInfo(logPrefix + " building the url + parameters",module);

String sessionUrl = hostProtocol + '://' + apiHostName + '/' +  checkoutSessionPath
//Debug.logInfo(logPrefix + "sessionUrl = " + sessionUrl,module);

String defaultReturnPath = EntityUtilProperties.getPropertyValue(systemResourceId, 'defaultReturnPath' + apiEnvironment, delegator);
//Debug.logInfo(logPrefix + "defaultRedirectUrl = " + defaultRedirectUrl ,module);

String orderId = parameters.orderId
//Debug.logInfo(logPrefix + "orderId = " + orderId,module);


// get the order header
try {
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
} catch (GenericEntityException e) {
    Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "adyenEvents.problemsGettingOrderHeader", locale));
    return "error";
}

// get the order total
BigDecimal bd =  new BigDecimal("100.0");
bd.setScale(0);
bd.stripTrailingZeros();
//BigDecimal orderAmount = new BigDecimal(orderHeader.grandTotal)
//orderAmount.setScale(0)
//orderAmount.stripTrailingZeros()
//Debug.logInfo(logPrefix + "orderAmount = " + orderAmount,module);
//String orderTotalLong = orderAmount.multiply(bd).toPlainString();
//Debug.logInfo(logPrefix + "orderTotalLong = " + orderTotalLong,module);
orderTotalLong = orderHeader.getBigDecimal("grandTotal").multiply(bd).toPlainString();
int index_point = orderTotalLong.indexOf(".");
orderTotalLong = orderTotalLong.substring(0, index_point);
//Debug.logInfo(logPrefix + "orderTotalLong = " + orderTotalLong,module);
String orderTotal = orderHeader.getBigDecimal("grandTotal").toPlainString();
//Debug.logInfo(logPrefix + "orderTotal = " + orderTotal,module);


// get some details from the order
OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
String currencyUomId = orh.getCurrency()
//Debug.logInfo(logPrefix + "currencyUomId = " + currencyUomId,module);

String description = parameters.description
//Debug.logInfo(logPrefix + "description = " + description,module);

String productStoreId = orh.getProductStoreId()
//Debug.logInfo(logPrefix + "productStoreId = " + productStoreId,module);

//building the payment Options
webSiteUrl = (String) context.get("webSiteUrl");
//Debug.logInfo(logPrefix + "webSiteUrl = " + webSiteUrl,module);
String sru = parameters._SERVER_ROOT_URL_;
//Debug.logInfo(logPrefix + "_SERVER_ROOT_URL_ = " + sru,module);

String cp = parameters._CONTROL_PATH_;
//Debug.logInfo(logPrefix + "_CONTROL_PATH_ = " + cp,module);

returnUrl = sru + cp +"/" + defaultReturnPath;

placingParty = orh.getPlacingParty();
//Debug.logInfo(logPrefix + "placingParty = " + placingParty,module);
billToParty = orh.getBillToParty();
//Debug.logInfo(logPrefix + "billToParty = " + billToParty,module);
// building the customer

// email address
String emailAddress = null;
Collection emCol = ContactHelper.getContactMech(placingParty, "PRIMARY_EMAIL", "EMAIL_ADDRESS", false);
if (UtilValidate.isEmpty(emCol)) {
    emCol = ContactHelper.getContactMech(placingParty, null, "EMAIL_ADDRESS", false);
}
if (!UtilValidate.isEmpty(emCol)) {
    GenericValue emVl = (GenericValue) emCol.iterator().next();
    if (emVl != null) {
        emailAddress = emVl.getString("infoString");
    }
} else {
    emailAddress = "";
}
//Debug.logInfo(logPrefix + "emailAddress: " + emailAddress, module);

// shipping address
String address1 = null;
String address2 = null;
String city = null;
String state = null;
String zipCode = null;
String country = null;
Collection adCol = ContactHelper.getContactMech(placingParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
if (UtilValidate.isEmpty(adCol)) {
    adCol = ContactHelper.getContactMech(placingParty, null, "POSTAL_ADDRESS", false);
}
if (!UtilValidate.isEmpty(adCol)) {
    GenericValue adVl = (GenericValue) adCol.iterator().next();
    if (adVl != null) {
        GenericValue addr = null;
        try {
            addr = adVl.getDelegator().findOne("PostalAddress", UtilMisc.toMap("contactMechId",
                    adVl.getString("contactMechId")), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (addr != null) {
            address1 = addr.getString("address1");
            address2 = addr.getString("address2");
            city = addr.getString("city");
            state = addr.getString("stateProvinceGeoId");
            zipCode = addr.getString("postalCode");
            country = addr.getString("countryGeoId");
            if (address2 == null) {
                address2 = "";
            }
        }
    }
}
geo = from("Geo").where("geoId", country).queryOne();


// phone number
String phoneNumber = null;
Collection phCol = ContactHelper.getContactMech(placingParty, "PHONE_HOME", "TELECOM_NUMBER", false);
if (UtilValidate.isEmpty(phCol)) {
    phCol = ContactHelper.getContactMech(placingParty, null, "TELECOM_NUMBER", false);
}
if (!UtilValidate.isEmpty(phCol)) {
    GenericValue phVl = (GenericValue) phCol.iterator().next();
    if (phVl != null) {
        GenericValue tele = null;
        try {
            tele = phVl.getDelegator().findOne("TelecomNumber", UtilMisc.toMap("contactMechId",
                    phVl.getString("contactMechId")), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (tele != null) {
            phoneNumber = ""; // reset the string
            String cc = tele.getString("countryCode");
            String ac = tele.getString("areaCode");
            String nm = tele.getString("contactNumber");
            if (UtilValidate.isNotEmpty(cc)) {
                phoneNumber = phoneNumber + cc + "-";
            }
            if (UtilValidate.isNotEmpty(ac)) {
                phoneNumber = phoneNumber + ac + "-";
            }
            phoneNumber = phoneNumber + nm;
        } else {
            phoneNumber = "";
        }
    }
}
//Debug.logInfo(logPrefix + "phoneNumber: " + phoneNumber, module);
String jsonCustomer =
        "{" +
        "\"locale\": \"" + locale + "\" ," +
        "\"first_name\": \"" + placingParty.firstName + "\" ," +
        "\"last_name\": \"" + placingParty.lastName + "\" ," +
        "\"address1\": \"" + address1 + "\" ," +
        "\"address2\": \"" + address2 + "\" ," +
        "\"house_number\": \"" + address1 + "\" ," +
        "\"zip_code\": \"" + zipCode + "\" ," +
        "\"city\": \"" + city + "\" ," +
        "\"state\": \"" + state + "\" ," +
        "\"country\": \"" + country + "\" ," +
        "\"email\": \"" + emailAddress + "\"" +
        "}"
//Debug.logInfo(logPrefix + "jsonCustomer = " + jsonCustomer,module);

String webSiteId = orh.getWebSiteId();
//Debug.logInfo(logPrefix + "webSiteId = " + webSiteId,module);
googleAccount = from("WebAnalyticsConfig").where("webSiteId", webSiteId, "webAnalyticsTypeId", "GOOGLE_ANALYTICS" ).queryOne()
//Debug.logInfo(logPrefix + "googleAccount = " + googleAccount,module);
// building the analytics

    String jsonAnalytics =
        "{" +
        "\"account\": \"" + googleAccount.webAnalyticsCode + "\"" +
        "}"
//Debug.logInfo(logPrefix + "jsonAnalytics = " + jsonAnalytics,module);


// building the jsonString
String jsonString = "{" +
        "\"merchantAccount\":\"" + merchantAccount +"\" ," +
        "\"sdkVersion\":\"" + "1.6.0" +"\" ," +
        "\"channel\":\"" + "Web" +"\" ," +
        "\"amount\": {" + 
        "\"currency\":\"" + currencyUomId + "\"," +
        "\"value\":\"" + orderTotalLong + "\"} ," +
        "\"reference\": \"" + orderId + "\" ," +
        "\"countryCode\":\"" + geo.geoCode + "\"," +
        "\"shopperLocale\":\"" + locale + "\"," +
        "\"origin\":\"" + sru + "\"," +
        "\"returnUrl\":\"" returnUrl + "\"}";
        
//Debug.logInfo(logPrefix + "jsonString = " + jsonString,module);

//Debug.logInfo(logPrefix + " --------------------------",module);
//Debug.logInfo(logPrefix + " setting up the session",module);
HttpClient http = new HttpClient(sessionUrl);
http.setHostVerificationLevel(SSLUtil.HOSTCERT_NO_CHECK);
http.setAllowUntrusted(true);
http.setDebug(false);

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
    response.sendRedirect("test");
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
if (code == 200){
	//Debug.logInfo(logPrefix + " --------------------------",module);
    //Debug.logInfo(logPrefix + " code = " + code ,module);
    //Debug.logInfo(logPrefix + " retResponse = " + retResponse ,module);
    
	//Debug.logInfo(logPrefix + " --------------------------",module);
    JSON jsonObject = JSON.from(retResponse)
    //Debug.logInfo(logPrefix + " jsonObject = " + jsonObject,module);
    
    JSONToMap jsonMap = new JSONToMap();
    Map<String, Object> jsMap = jsonMap.convert(jsonObject);
    paymentSession = jsMap.paymentSession
	//Debug.logInfo(logPrefix + " --------------------------",module);
    //Debug.logInfo(logPrefix + " paymentSession = " + paymentSession,module);
    parameters.paymentSession = paymentSession
    //request.setAttribute("paymentSession", paymentSession)
    //Debug.logInfo(logPrefix + "redirectUrl = " + redirectUrl,module);
    paymentUrl = redirectUrl
    //Debug.logInfo(logPrefix + "paymentUrl = " + paymentUrl,module);
    
}
else {
    //Debug.logInfo(logPrefix + "code = " + code ,module);
}

//Debug.logInfo(logPrefix + "--------------------------",module);
