<!DOCTYPE html>
<html>
<head>
    <title>EOSC Observatory - Survey Started</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <style type="text/css">
        @media screen {
            @font-face {
                font-family: 'Lato';
                font-style: normal;
                font-weight: 400;
                src: local('Lato Regular'), local('Lato-Regular'), url(https://fonts.gstatic.com/s/lato/v11/qIIYRU-oROkIk8vfvxw6QvesZW2xOQ-xsNqO47m55DA.woff) format('woff');
            }
            @font-face {
                font-family: 'Lato';
                font-style: normal;
                font-weight: 700;
                src: local('Lato Bold'), local('Lato-Bold'), url(https://fonts.gstatic.com/s/lato/v11/qdgUG4U09HnJwhYI-uK18wLUuEpTyoUstqEm5AMlJo4.woff) format('woff');
            }
        }
        @media screen and (max-width: 600px) {
            .body-padding { padding-left: 15px !important; padding-right: 15px !important; }
            .headline-text { font-size: 15px !important; line-height: 22px !important; }
            .signoff-text { font-size: 15px !important; line-height: 22px !important; }
            .cta-button { padding: 12px 20px !important; font-size: 15px !important; }
            .footer-cell { font-size: 15px !important; }
        }
    </style>
</head>
<body style="-webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; background-color: #f4f4f4; height: 100%; width: 100%; margin: 0; padding: 0;">

<table border="0" cellpadding="0" cellspacing="0" width="100%"
       style="-webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse;">

    <!-- LOGO -->
    <tr>
        <td bgcolor="#008792" align="center"
            style="padding: 40px 10px 0px 10px;">
            <table border="0" cellpadding="0" cellspacing="0" width="100%"
                   style="max-width: 600px; border-collapse: collapse;">
                <tr>
                    <td bgcolor="#ffffff" align="center" valign="top"
                        style="padding: 40px 20px 20px 20px; border-radius: 4px 4px 0px 0px; font-family: 'Lato', Helvetica, Arial, sans-serif;">
                        <img alt="EOSC Observatory"
                             src="https://eoscobservatory.eosc-portal.eu/assets/logo/png/EOSC OPEN SCIENCE OBSERVATORY LOGO.png"
                             width="300" style="display: block; border: 0; margin: 0 auto;"/>
                    </td>
                </tr>
            </table>
        </td>
    </tr>

    <!-- BODY -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 0px 10px 0px 10px;">
            <table border="0" cellpadding="0" cellspacing="0" width="100%"
                   style="max-width: 600px; border-collapse: collapse;">

                <!-- HEADLINE -->
                <tr>
                    <td bgcolor="#ffffff" align="left" class="body-padding headline-text"
                        style="padding: 30px 30px 10px 30px; color: #1a1a1a; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px;">
                        <p style="margin: 0;">
                            The survey <b>${surveyName}</b> is now open for submissions.
                        </p>
                    </td>
                </tr>

                <!-- DATE -->
                <tr>
                    <td bgcolor="#ffffff" align="left" class="body-padding"
                        style="padding: 10px 30px 25px 30px; color: #555555; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 16px; line-height: 24px;">
                        <p style="margin: 0;">Submission period opened: <b>${startDate}</b></p>
                    </td>
                </tr>

                <!-- DIVIDER -->
                <tr>
                    <td bgcolor="#ffffff" class="body-padding" style="padding: 0 30px;">
                        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse;">
                            <tr><td style="border-top: 1px solid #eeeeee; font-size: 0; line-height: 0;">&nbsp;</td></tr>
                        </table>
                    </td>
                </tr>

                <!-- CTA BUTTON -->
                <tr>
                    <td bgcolor="#ffffff" align="center" style="padding: 25px 30px 15px 30px;">
                        <table border="0" cellspacing="0" cellpadding="0" style="border-collapse: collapse;">
                            <tr>
                                <td align="center" bgcolor="#008792" style="border-radius: 3px;">
                                    <a href="${url}" target="_blank" class="cta-button"
                                       style="font-size: 16px; font-family: 'Lato', Helvetica, Arial, sans-serif; color: #ffffff; text-decoration: none; padding: 14px 30px; border-radius: 3px; border: 1px solid #008792; display: inline-block; font-weight: 700;">
                                        Go to survey &rarr;
                                    </a>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>

                <!-- SIGN OFF -->
                <tr>
                    <td bgcolor="#ffffff" align="left" class="body-padding signoff-text"
                        style="padding: 15px 30px 40px 30px; border-radius: 0px 0px 4px 4px; color: #1a1a1a; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px;">
                        <p style="margin: 0;">Kind regards,<br>The EOSC Observatory Team</p>
                    </td>
                </tr>

            </table>
        </td>
    </tr>

    <!-- FOOTER -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 30px 10px 0px 10px;">
            <table border="0" cellpadding="0" cellspacing="0" width="100%"
                   style="max-width: 600px; border-collapse: collapse;">
                <tr>
                    <td align="center" class="footer-cell" bgcolor="#d4eef0"
                        style="padding: 30px; border-radius: 4px; color: #666666; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px; background-color: #d4eef0;">
                        <h2 style="font-size: 20px; font-weight: 400; color: #111111; margin: 0;">Need more help?</h2>
                        <p style="margin: 0;">
                            <a href="mailto:stefania.martziou@openaire.eu" target="_blank"
                               style="color: #008792;">Contact us</a>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>

    <!-- FINE PRINT -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 20px 10px 30px 10px;">
            <table border="0" cellpadding="0" cellspacing="0" width="100%"
                   style="max-width: 600px; border-collapse: collapse;">
                <tr>
                    <td align="center"
                        style="padding: 0 30px; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 13px; color: #999999; line-height: 20px;">
                        <p style="margin: 0;">You received this notification because you are a member of a stakeholder group participating in this survey.</p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>

</table>
</body>
</html>
