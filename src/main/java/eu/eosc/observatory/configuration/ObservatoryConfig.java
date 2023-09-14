package eu.eosc.observatory.configuration;

import gr.athenarc.recaptcha.annotation.EnableReCaptcha;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = "eu.openminted.registry.core")
@EnableReCaptcha
@EnableConfigurationProperties(value = {ApplicationProperties.class, PrivacyProperties.class})
public class ObservatoryConfig {

}
