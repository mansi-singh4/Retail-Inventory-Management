package com.retailinventory.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ApiGatewayApplication {
  public static void main(String[] args) { SpringApplication.run(ApiGatewayApplication.class, args); }
  @RestController
  static class GatewayInfoController {
    @GetMapping("/health") public Object health() { return java.util.Map.of("status", "UP", "service", "api-gateway"); }
  }
}
