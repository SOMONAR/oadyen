
<#--
(C) Copyright 2018 Somonar B.V.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 -->
<div id="adyen">
here comes the result from chckt.checkout
</div>
<script language="JavaScript" type="text/javascript">
// Initiate the Checkout form.
	/**
	* Configure Adyen Checkout
	*/
	
	var sdkConfigObj = { 'context' : 'test' };
	var node = '#adyen';
	if(paymentSession) {
		var paymentSession = '${StringUtil.wrapString(paymentSession)}';
		var checkout = chckt.checkout(paymentSession, node, sdkConfigObj); 
		chckt.hooks.beforeComplete = function(node, paymentData) {
	}
   // 'node' is a reference to the Checkout container HTML node.
   // 'paymentData' is the result of the payment, and contains the 'payload'.

   return false; // Indicates that you want to replace the default handling.
};
</script>