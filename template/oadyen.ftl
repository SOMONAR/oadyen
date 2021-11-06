<#--
This file is subject to the terms and conditions defined in the file 'LICENSE', 
which is part of this (source code) package.

Unless required by applicable law or agreed to in writing, software distributed 
under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied.

See the License for the specific language governing permissions and limitations
under the License.
 -->
<#--
Template for oadyen integration
-->
<div class="paymentmethode">
    <input type="radio" id="checkOutPaymentId_oadyen" name="checkOutPaymentId" value="EXT_oadyen" <#if "EXT_oadyen" == checkOutPaymentId>checked="checked"</#if> />
    <label for="checkOutPaymentId_oadyen">${uiLabelMap.PayViaAdyen}</label>
</div>