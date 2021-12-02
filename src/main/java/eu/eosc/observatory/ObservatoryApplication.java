package eu.eosc.observatory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class ObservatoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservatoryApplication.class, args);
    }

}
