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
<div>You have a new message from <b>${user}</b></div>
<br/>
<div>
    <#noautoesc>
        ${content}
    </#noautoesc>
</div>

<div> Have a nice day!</div>
</body>
</html>