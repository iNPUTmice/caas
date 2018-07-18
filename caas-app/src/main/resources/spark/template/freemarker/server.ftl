<#ftl output_format="HTML">
<#import "page.ftl" as page>
<#import "graph.ftl" as graph>
<#import "stat.ftl" as stat>
<#import "result.ftl" as result>
<#assign description="Server compliance result for ${domain}" in page>
<#assign title="${page.project_name}: Compliance result for ${domain}" in page>
<#assign stylesheets=["/css/server.css","/css/graph.css","/css/stat.css"] in page>
<#assign scripts=["/js/graph.js","/js/d3.min.js","/js/server.js"] in page>

<@page.page>

    <script>
        var data = JSON.parse('${historic_data?no_esc}');
        var domain = "${domain}";
        var timestamp = "${timestamp}";
    </script>

    <h2> Compliance status for ${domain}</h2>

    <@stat.stat></@stat.stat>

   <button id="download_report" onclick="print_report('${softwareName!}','${softwareVersion!}')">Download report
   </button>
    <br><br>

    <#if softwareName??>
        Server is running ${softwareName} ${softwareVersion!}
    <#else>
        Server is running unknown software
    </#if>

    <br><br>

    <@graph.graph>
    </@graph.graph>

    <@result.result>
    </@result.result>

    <div id="server_run">
        Tests last ran ${timeSince}<br>
        <button onclick="location.href='/live/${domain}'">Rerun tests</button>
    </div>

    <div id="additional_server">
        <div class="card" id="subscribe_server">
            <h3>Subscribe to periodic reports for this server</h3>
            <form id="form_subscribe" action="#subscribe" method="post">
                <div>
                    <label for="email" class="input_label_subscribe">E-Mail</label>
                    <input name="email" class="input_subscribe" type="text"/>
                </div>
                <div>
                    <input type="submit" class="button" id="subscribe_button" value="Subscribe"/>
                </div>
            </form>
        </div>

        <div class="card" id="embed_server">
            <h3>Add badge to your website</h3>
            ${badgeCode?no_esc}
            <br><br>
            <div class="code">
                ${badgeCode}
            </div>
        </div>
    </div>

    <#if helps??>
    <div id="help_container">
        <#list helps as help>
            <#if help.isPossible()??>
            <div class="help card" id="${help.getName()}">

                <a class="close" href="#${help.getName()}">&times;</a>
                <h3>For <a href="/test/${help.getName()}">${tests[help.getName()].full_name()}</a>:</h3>
                <ul>

                    <#if help.getSince()??>
                       <li>
                           Make sure your server is at least ${help.getSince()}
                           <#if softwareVersion??>(currently ${softwareVersion})</#if>
                       </li>
                    </#if>

                   <#if help.modulesRequired??>
                   <li>
                       <#if softwareName == "Openfire" >
                          Install the following plugin:
                       <#else>
                          Add the following modules to your configuration file:
                       </#if>
                       <div class="modules">
                       <#list help.getModulesRequired() as module>
                           <div class="module">
                               <div class="${module.getType()}">
                               <#if module.getLink()??>
                               <a href="${module.getLink()}">
                                   ${module.getName()}
                               </a>
                               <#else>
                                   ${module.getName()}
                               </#if>

                               <#if module.getType() == "community_prosody">
                               <br><br>
                               This module doesn't come with Prosody
                                   installation.
                               You will have to download it by following
                               <a href="https://prosody.im/doc/installing_modules">
                                   these instructions
                               </a>
                               </#if>
                               </div>
                           </div>
                       </#list>
                       </div>
                   </li>
                   </#if>

                   <#if help.getInstructions()??>
                   <div class="instructions">
                       <li>
                           ${help.getInstructions()?no_esc}
                       </li>
                   </div>
                   </#if>

                </ul>
            </div>
            </#if>
        </#list>
    </div>
    </#if>

</@page.page>