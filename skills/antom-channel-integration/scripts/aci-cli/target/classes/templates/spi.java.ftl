/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].spi;

import com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=serviceInterface];
import [=packageName].template.ChannelInvocationTemplate;
import [=packageName].model.ChannelOperation;
<#if family == "notify">
import [=packageName].extension.ChannelNotificationExtension;
<#else>
import com.alibaba.fastjson.JSONObject;
import [=packageName].extension.ChannelApiExtension;
import [=packageName].model.ChannelHttpResult;
import java.util.Collections;
import java.util.Map;
</#if>
<#assign importedTypes=[]>
<#list capabilities as method><#if method.family == family>
<#if !importedTypes?seq_contains(method.requestType)>
import [=method.requestType];
<#assign importedTypes=importedTypes+[method.requestType]>
</#if>
<#if !importedTypes?seq_contains(method.responseType)>
import [=method.responseType];
<#assign importedTypes=importedTypes+[method.responseType]>
</#if>
</#if></#list>
import org.springframework.stereotype.Component;

/** Selected SDK methods with their institution mappings implemented inline. */
@Component
public class [=serviceClass] implements [=serviceInterface] {
    private final ChannelInvocationTemplate template;

    /** Wire the shared invocation flow; each selected method owns its mapping hooks. */
    public [=serviceClass](ChannelInvocationTemplate template) {
        this.template = template;
    }
<#list capabilities as method><#if method.family == family>

    /** {@inheritDoc} */
    @Override
    public [=method.responseName] [=method.method]([=method.requestName] request) {
<#if method.notification>
        return template.executeNotification(ChannelOperation.[=method.operation], request,
                new ChannelNotificationExtension<[=method.responseName]>() {
<#assign capability=method><#include "notification.java.ftl">
                });
<#else>
        return template.executeDynamicUrl(ChannelOperation.[=method.operation], request,
                new ChannelApiExtension<[=method.requestName], [=method.responseName]>() {
<#assign capability=method><#include "mapping.java.ftl">
                });
</#if>
    }
</#if></#list>
}
