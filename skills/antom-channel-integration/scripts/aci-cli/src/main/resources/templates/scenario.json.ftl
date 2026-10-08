{
  "id": "success",
  "category": "success",
  "confirmed": false,
  "description": "Replace with independently confirmed institution protocol data; this placeholder intentionally fails delivery.",
  "input": {},
  "context": {"present": true, "channelCode": "[=channelCode]", "merchantId": "synthetic-test-merchant", "runtimeEnv": null},
  "http": {"calls": <#if capability.notification>0<#else>1</#if><#if !capability.notification>, "response": {"statusCode": 200, "headers": {}, "body": "{}"}</#if>},
<#if !capability.notification>
  "expectedRequest": {"method": "POST", "contentType": "JSON", "pathParameters": {}, "headers": {}, "queryParameters": {}, "formParameters": {}, "attributes": {}, "body": "{}"},
</#if>
  "security": {<#assign first=true><#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none"><#if !first>,</#if>
    "[=direction][=step.operation?cap_first]": {"calls": 0}<#assign first=false></#if></#list></#list>
  },
  "resultCode": [],
  "expectedResult": {}
}
