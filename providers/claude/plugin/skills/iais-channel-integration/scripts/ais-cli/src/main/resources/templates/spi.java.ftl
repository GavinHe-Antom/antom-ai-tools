/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].spi;

import com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=serviceInterface];
import [=packageName].template.ChannelInvocationTemplate;
import [=packageName].model.ChannelOperation;
<#list capabilities as method><#if method.family == family>
import [=method.requestType];
import [=method.responseType];
import [=packageName].customize.api.[=method.title]Mapping;
</#if></#list>
import org.springframework.stereotype.Component;

/** Selected SDK methods only. Business edits belong in customize. */
@Component
public class [=serviceClass] implements [=serviceInterface] {
    private final ChannelInvocationTemplate template;
<#list capabilities as method><#if method.family == family>
    private final [=method.title]Mapping [=method.method]Mapping;
</#if></#list>

    /** Wire platform invocation and channel mapping extensions. */
    public [=serviceClass](ChannelInvocationTemplate template<#list capabilities as method><#if method.family == family>,
            [=method.title]Mapping [=method.method]Mapping</#if></#list>) {
        this.template = template;
<#list capabilities as method><#if method.family == family>
        this.[=method.method]Mapping = [=method.method]Mapping;
</#if></#list>
    }
<#list capabilities as method><#if method.family == family>

    /** {@inheritDoc} */
    @Override
    public [=method.responseName] [=method.method]([=method.requestName] request) {
<#if method.notification>
        return template.executeNotification(ChannelOperation.[=method.operation], request, [=method.method]Mapping);
<#else>
        return template.executeDynamicUrl(ChannelOperation.[=method.operation], request, [=method.method]Mapping);
</#if>
    }
</#if></#list>
}
