
package com.rahim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication(scanBasePackages = "com.rahim.cryptointel")
@SpringBootApplication()
public class CryptoApplication {
  public static void main(String[] args) {
    SpringApplication.run(CryptoApplication.class, args);
  }
}
