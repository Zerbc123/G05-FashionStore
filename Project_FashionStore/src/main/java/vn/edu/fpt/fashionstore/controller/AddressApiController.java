package vn.edu.fpt.fashionstore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/address-api")
@CrossOrigin(origins = "*")
public class AddressApiController {

    private final RestTemplate restTemplate;
    private static final String EXTERNAL_API_URL = "https://provinces.open-api.vn/api/v2/";

    public AddressApiController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping
    public ResponseEntity<String> getProvinces(@RequestParam(required = false, defaultValue = "1") int depth) {
        try {
            String url = EXTERNAL_API_URL + "?depth=" + depth;
            System.out.println("Fetching data from: " + url);
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("API Response received successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching provinces: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("[]");
        }
    }
}
