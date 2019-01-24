<#assign project_name="XMPP Compliance Tester">
<#assign default_description= "Choose a Jabber server from a list of servers by comparing their current and historic support for features, or test your current server for features">
<#macro page>
    <#assign nav_bar_height=80>
<!DOCTYPE html>
<html>
<head>
    <meta property="og:title" content="${title!"Check your XMPP server for compliance"}">
    <title>${title!project_name}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <#if root_url??>
    <meta name="twitter:card" content="summary_large_image">
    <meta property="og:image" content="${root_url}/img/cover.png">
    </#if>
    <meta name="twitter:creator" content="@iNPUTmice">
    <meta property="og:description" content="${description!default_description}">
    <meta property="og:locale" content="en_US">
    <meta charset="UTF-8">
    <style>
        #nav_bar, #brand, #nav_list_menu {
            height: ${nav_bar_height}px;
        }

        #brand {
            height: ${nav_bar_height - 10}px;
        }

        #brand_img {
            height: ${nav_bar_height - 25}px;
        }

        #content {
            margin-top: ${nav_bar_height + 10}px;
        }
    </style>
    <link rel="stylesheet" href="/css/page.css">
    <#if stylesheets??>
        <#list stylesheets as css>
    <link rel="stylesheet" href="${css}">
        </#list>
    </#if>

    <script src="/js/jquery.min.js"></script>
    <script src="/js/menu.js"></script>
    <#if scripts??>
        <#list scripts as script>
    <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>

<#----          Icons begin          ---->
    <link rel="apple-touch-icon" sizes="180x180" href="/icons/apple-touch-icon.png">
    <link rel="icon" type="image/png" sizes="32x32" href="/icons/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="/icons/favicon-16x16.png">
    <link rel="manifest" href="/icons/site.webmanifest">
    <link rel="mask-icon" href="/icons/safari-pinned-tab.svg" color="#5bbad5">
    <link rel="shortcut icon" href="/icons/favicon.ico">
    <meta name="msapplication-TileColor" content="#00aba9">
    <meta name="msapplication-config" content="/icons/browserconfig.xml">
    <meta name="theme-color" content="#ffffff">
</head>

<body>
<nav id="nav_bar">
    <a href="/">
    <div id="brand">
        <img id="brand_img" src="/img/logo.svg">
        <h1 id="brand_name">${project_name}</h1>
    </div>
    </a>
    <div id="hamburger" onclick="menu_toggle()">
        <span></span>
        <span></span>
        <span></span>
    </div>
    <div id="nav_list_menu" onclick="menu_toggle()">
        <a href="/add">
            Test a server
        </a>
        <a href="/">
            Servers
        </a>
        <a href="/tests/">
            Tests
        </a>
        <a href="/about/">
            About
        </a>
    </div>
</nav>
<div id="content">
            <#nested>
</div>
<footer>
    <small>
        Copyright &copy; 2019 <a href="https://gultsch.de/">Daniel Gultsch</a>, <a href="https://rishiraj.me">Rishi
        Raj</a>.
        ${footerText!}
    </small>
</footer>
</body>
</html>
</#macro>
