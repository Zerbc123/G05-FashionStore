package vn.edu.fpt.fashionstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "vn.edu.fpt.fashionstore.repository")
@EntityScan(basePackages = "vn.edu.fpt.fashionstore.entity")
@ComponentScan(basePackages = "vn.edu.fpt.fashionstore")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
