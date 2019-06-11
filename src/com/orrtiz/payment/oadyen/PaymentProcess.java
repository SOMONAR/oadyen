/*******************************************************************************
 * (C) Copyright 2018 Somonar B.V.
 * Licensed under the Apache License under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.orttiz.payment.oadyen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.order.order.*;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

public class PaymentProcess {
    
    public static final String module = AdyenClient.class.getName();
    public static final String resource = "AccountingUiLabels";
    public static final String resourceErr = "AccountingErrorUiLabels";
    public static final String commonResource = "CommonUiLabels";
    public static final String oAdyenResource = "oadyen-UiLabels";
        
    /** Initiate AdyenEvents Request 
     * @throws IOException */
    
    public static String payloadResponse (HttpServletRequest request, HttpServletResponse response) throws IOException {
    	Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        
        Map<String,Object> paramMap = UtilHttp.getParameterMap(request);
        
        Debug.logInfo("in " + module + " locale is: " + locale,module);
        Debug.logInfo("in " + module + " delegator is: " + delegator.getDelegatorName(), module);
        Debug.logInfo("in " + module + " tenantId is: " + delegator.getDelegatorTenantId(), module);
        String orderId = request.getParameter("orderId");
        Debug.logInfo("in " + module + " orderId is: " + orderId,module);
        
        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "AdyenEvents.problemsGettingOrderHeader", locale));
            return "error";
        }
        
        // get the order total
        BigDecimal bd =  new BigDecimal("100.0");
        bd.setScale(0);
        bd.stripTrailingZeros();
        String orderTotalLong = orderHeader.getBigDecimal("grandTotal").multiply(bd).toPlainString();
        int index_point = orderTotalLong.indexOf(".");
        orderTotalLong = orderTotalLong.substring(0, index_point);
        String orderTotal = orderHeader.getBigDecimal("grandTotal").toPlainString();
        String currencyUom = orderHeader.getString("currencyUom");
        return "success";
    }
    
public static String redirectResponse (HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        
        Map<String,Object> paramMap = UtilHttp.getParameterMap(request);
        
        Debug.logInfo("in " + module + " - redirectResponse - paramMap is: " + paramMap,module);
        Debug.logInfo("in " + module + " locale is: " + locale,module);
        Debug.logInfo("in " + module + " delegator is: " + delegator.getDelegatorName(), module);
        Debug.logInfo("in " + module + " tenantId is: " + delegator.getDelegatorTenantId(), module);
        String orderId = request.getParameter("transactionid");
        Debug.logInfo("in " + module + " orderId is: " + orderId,module);
        
        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "AdyenEvents.problemsGettingOrderHeader", locale));
            return "error";
        }
        // get the order total
        BigDecimal bd =  new BigDecimal("100.0");
        bd.setScale(0);
        bd.stripTrailingZeros();
        String orderTotalLong = orderHeader.getBigDecimal("grandTotal").multiply(bd).toPlainString();
        int index_point = orderTotalLong.indexOf(".");
        orderTotalLong = orderTotalLong.substring(0, index_point);
        String orderTotal = orderHeader.getBigDecimal("grandTotal").toPlainString();
        String currencyUom = orderHeader.getString("currencyUom");
        
        // attempt to start a transaction
        boolean okay = true;
        boolean beganTransaction = false;
        
        try {
            beganTransaction = TransactionUtil.begin();
            okay = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            
            Debug.logInfo("okay is: " + okay,module);
            
            if (okay) {
                // set the payment preference
                okay = setPaymentPreferences(delegator, dispatcher, userLogin, orderId, request, orderTotal);
            }
        } catch (Exception e) {
            String errMsg = "Error handling adyen redirect";
            Debug.logError(e, errMsg, module);
            try {
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericTransactionException gte2) {
                Debug.logError(gte2, "Unable to rollback transaction", module);
            }
        } finally {
            if (!okay) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Failure in processing adyen redirect", null);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback transaction", module);
                }
            } else {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to commit transaction", module);
                }
            }
        }
        if (okay) {
            // attempt to release the offline hold on the order (workflow)
              OrderChangeHelper.releaseInitialOrderHold(dispatcher, orderId);
        }
        request.setAttribute("orderId", orderId);
        return "success";
    }

private static boolean setPaymentPreference(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPreference, HttpServletRequest request, String orderTotal) {
    Locale locale = UtilHttp.getLocale(request);
    String paymentType = "Betaling klant";
    String paymentAmount = orderTotal;
    String paymentStatus = "Pending";
    String transactionId = "";
    
    List <GenericValue> toStore = new LinkedList <GenericValue> ();
    
    // The component returns the timestamp in the format 'hh:mm:ss Jan 1, 2000 PST'
    // Parse this into a valid Timestamp Object
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss MMM d, yyyy z");
    java.sql.Timestamp authDate = null;
    try {
        authDate = new java.sql.Timestamp(sdf.parse(sdf.format(cal.getTime())).getTime());
    } catch (ParseException e) {
        Debug.logError(e, "Cannot parse date string: " + sdf.format(cal.getTime()), module);
        authDate = UtilDateTime.nowTimestamp();
    } catch (NullPointerException e) {
        Debug.logError(e, "Cannot parse date string: " + sdf.format(cal.getTime()), module);
        authDate = UtilDateTime.nowTimestamp();
    }
    
    paymentPreference.set("maxAmount", new BigDecimal(paymentAmount));
    if (paymentStatus.equals("Completed")) {
        paymentPreference.set("statusId", "PAYMENT_RECEIVED");
    } else if (paymentStatus.equals("Pending")) {
        paymentPreference.set("statusId", "PAYMENT_NOT_RECEIVED");
    } else {
        paymentPreference.set("statusId", "PAYMENT_CANCELLED");
    }
    toStore.add(paymentPreference);
    
    Delegator delegator = paymentPreference.getDelegator();
    
    // create the PaymentGatewayResponse
    String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
    GenericValue response = delegator.makeValue("PaymentGatewayResponse");
    response.set("paymentGatewayResponseId", responseId);
    response.set("paymentServiceTypeEnumId", "PRDS_PAY_EXTERNAL");
    response.set("orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
    response.set("paymentMethodTypeId", paymentPreference.get("paymentMethodTypeId"));
    response.set("paymentMethodId", paymentPreference.get("paymentMethodId"));
    
    // set the auth info
    response.set("amount", new BigDecimal(paymentAmount));
    response.set("referenceNum", transactionId);
    response.set("gatewayCode", paymentStatus);
    response.set("gatewayFlag", paymentStatus.substring(0,1));
    response.set("gatewayMessage", paymentType);
    response.set("transactionDate", authDate);
    toStore.add(response);
    
    try {
        delegator.storeAll(toStore);
    } catch (GenericEntityException e) {
        Debug.logError(e, "Cannot set payment preference/payment info", module);
        return false;
    }
    
    // create a payment record too
    Map <String, Object> results = null;
    try {
        String comment = UtilProperties.getMessage(oAdyenResource, "PaymentTransactionViaAdyen", locale);
        results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin,
                "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "comments", comment));
    } catch (GenericServiceException e) {
        Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.failedToExecuteServiceCreatePaymentFromPreference", locale));
        return false;
    }

    if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
        Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
        request.setAttribute("_ERROR_MESSAGE_", results.get(ModelService.ERROR_MESSAGE));
        return false;
    }
    return true;
}

private static boolean setPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String orderId, HttpServletRequest request, String orderTotal) {
    Debug.logVerbose("Setting payment preferences..", module);
    List <GenericValue> paymentPrefs = null;
    try {
        Map <String, String> paymentFields = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED");
        paymentPrefs = delegator.findByAnd("OrderPaymentPreference", paymentFields, null, false);
    } catch (GenericEntityException e) {
        Debug.logError(e, "Cannot get payment preferences for order #" + orderId, module);
        return false;
    }
    if (paymentPrefs.size() > 0) {
        Iterator <GenericValue> i = paymentPrefs.iterator();
        while (i.hasNext()) {
            GenericValue pref = i.next();
            boolean okay = setPaymentPreference(dispatcher, userLogin, pref, request, orderTotal);
            if (!okay)
                return false;
        }
    }
    return true;
}
}
