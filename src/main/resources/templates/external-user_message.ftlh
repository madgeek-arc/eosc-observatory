<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>${subject}</title>
</head>
<body>
<div style="margin-top: 10px">EOSC Observatory</div>
<div>
    <#if user?? >
        <p>You have a new message from <b>${user}</b>.</p>
    <#else>
        <p>Someone replied to your message anonymously.</p>
    </#if>
    <p>In case you need to reply, please start a new conversation from the Contact Form.</p>
</div>
<br/>
<div>
    <#noautoesc>
        ${content}
    </#noautoesc>
</div>

<div> Have a nice day!</div>
</body>
</html>