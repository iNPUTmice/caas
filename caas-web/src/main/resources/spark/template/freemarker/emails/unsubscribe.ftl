<#ftl output_format="HTML">
<#macro unsubscribe>
<div id="unsubscribe" style="max-width: 500px;text-align: center; margin: 0px auto;">
    <p style="font-size: 13px;color: #666;">
        <a href="${rootUrl}/unsubscribe/${subscriber.unsubscribeCode}" style="font-size: 13px;color: #666;">Unsubscribe</a>
        from receiving alerts about ${subscriber.domain}
    </p>
</div>
</#macro>
