package com.springboot;

import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.OfferRequest;
import com.springboot.controller.ApiResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.math.BigInteger;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CartOfferApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setup() {
        // Create offers for different test scenarios
        createOffer(1, "FLATX", 10, Arrays.asList("p1")); // Rs 10 off for p1
        createOffer(1, "FLATP", 20, Arrays.asList("p2")); // 20% off for p2
        createOffer(2, "FLATX", 50, Arrays.asList("p1")); // Rs 50 off for p1 at different restaurant
        createOffer(3, "FLATP", 15, Arrays.asList("p1", "p2")); // 15% off for both p1 and p2
        createOffer(4, "FLATX", 100, Arrays.asList("p3")); // Rs 100 off for p3
        createOffer(5, "FLATP", 25, Arrays.asList("p1", "p2", "p3")); // 25% off for all segments
    }

    private void createOffer(int restaurantId, String offerType, int value, List<String> segments) {
        OfferRequest offerRequest = new OfferRequest(restaurantId, offerType, value, segments);
        restTemplate.postForEntity("/api/v1/offer", offerRequest, Object.class);
    }

    // 1. Basic Offer Creation Tests
    @Test
    public void testCreateFlatAmountOffer() {
        List<String> segments = Arrays.asList("p1");
        OfferRequest offerRequest = new OfferRequest(10, "FLATX", 50, segments);
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/v1/offer", offerRequest, ApiResponse.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Assert.assertEquals("success", response.getBody().getResponse_msg());
    }

    @Test
    public void testCreatePercentageOffer() {
        List<String> segments = Arrays.asList("p2");
        OfferRequest offerRequest = new OfferRequest(10, "FLATP", 30, segments);
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/v1/offer", offerRequest, ApiResponse.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Assert.assertEquals("success", response.getBody().getResponse_msg());
    }

    // 2. Flat Amount Discount Tests
    @Test
    public void testFlatAmountOffForP1() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1); // p1 segment
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(190, responseBody.get("cart_value")); // 200 - 10
    }

    @Test
    public void testFlatAmountOffLargerDiscount() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1); // p1 segment
        request.setRestaurant_id(2);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(150, responseBody.get("cart_value")); // 200 - 50
    }

    // 3. Percentage Discount Tests
    @Test
    public void testPercentageOffForP2() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(2); // p2 segment
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(160, responseBody.get("cart_value")); // 200 - (20% of 200)
    }

    @Test
    public void testPercentageOffWithLargeAmount() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(1000);
        request.setUser_id(2); // p2 segment
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(800, responseBody.get("cart_value")); // 1000 - (20% of 1000)
    }

    // 4. Edge Cases Tests
    @Test
    public void testNoApplicableOffer() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1);
        request.setRestaurant_id(10); // Non-existent restaurant
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(200, responseBody.get("cart_value")); // No change
    }

    @Test
    public void testZeroCartValue() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(0);
        request.setUser_id(1);
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(0, responseBody.get("cart_value")); // Should remain 0
    }

    @Test
    public void testVerySmallCartValue() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(5);
        request.setUser_id(1);
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(0, responseBody.get("cart_value")); // 5 - 10 = 0 (minimum)
    }

    // 5. Multiple Segment Tests
    @Test
    public void testMultiSegmentOfferForP1() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1); // p1 segment
        request.setRestaurant_id(3);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(170, responseBody.get("cart_value")); // 200 - (15% of 200)
    }

    @Test
    public void testMultiSegmentOfferForP2() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(2); // p2 segment
        request.setRestaurant_id(3);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(170, responseBody.get("cart_value")); // 200 - (15% of 200)
    }

    @Test
    public void testAllSegmentsOffer() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(400);
        request.setUser_id(1); // p1 segment
        request.setRestaurant_id(5);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(300, responseBody.get("cart_value")); // 400 - (25% of 400)
    }

    // 6. Invalid Input Tests
    @Test
    public void testNegativeCartValue() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(-100);
        request.setUser_id(1);
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(0, responseBody.get("cart_value")); // Should handle negative values
    }

    @Test
    public void testVeryLargeCartValue() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(1000000);
        request.setUser_id(2);
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(800000, responseBody.get("cart_value")); // 1000000 - (20% of 1000000)
    }

    @Test
    public void testMultipleOffersForSameRestaurant() {
        // Create multiple offers for same restaurant
        createOffer(6, "FLATX", 10, Arrays.asList("p1"));
        createOffer(6, "FLATP", 20, Arrays.asList("p1"));
        
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1);
        request.setRestaurant_id(6);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(190, responseBody.get("cart_value")); // Should apply first matching offer
    }

    // 7. User Segment Tests
    @Test
    public void testNonExistingUserSegment() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(99); // Non-existing user
        request.setRestaurant_id(1);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(200, responseBody.get("cart_value")); // No offer should be applied
    }

    // 8. Offer Type Tests
    @Test
    public void testOfferTypeComparison() {
        // Create two offers for same segment but different types
        createOffer(7, "FLATX", 50, Arrays.asList("p1")); // 50 Rs off
        createOffer(7, "FLATP", 40, Arrays.asList("p1")); // 40% off
        
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1);
        request.setRestaurant_id(7);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(150, responseBody.get("cart_value")); // Should apply first offer (FLATX)
    }

    @Test
    public void testRoundingBehavior() {
        createOffer(8, "FLATP", 33, Arrays.asList("p1")); // 33% off
        
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(100);
        request.setUser_id(1);
        request.setRestaurant_id(8);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        Assert.assertEquals(67, responseBody.get("cart_value")); // Should round down 100 - (33% of 100)
    }

    // 9. Response Time Tests
    @Test
    public void testApplyOfferResponseTime() {
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(1);
        request.setRestaurant_id(1);
        
        long startTime = System.currentTimeMillis();
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Assert.assertEquals(200, response.getStatusCodeValue());
        Assert.assertTrue("Response time should be under 200ms but was " + duration + "ms", duration < 200);
    }
    
    @Test
    public void testCreateOfferResponseTime() {
        List<String> segments = Arrays.asList("p1");
        OfferRequest offerRequest = new OfferRequest(20, "FLATX", 50, segments);
        
        long startTime = System.currentTimeMillis();
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/v1/offer", offerRequest, ApiResponse.class);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Assert.assertEquals(200, response.getStatusCodeValue());
        Assert.assertTrue("Response time should be under 200ms but was " + duration + "ms", duration < 200);
    }
    
    // 10. Data Type Validation Tests
    @Test
    public void testReturnedDataTypes() {
        // Create a specific offer for this test
        createOffer(25, "FLATP", 15, Arrays.asList("p1"));
        
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(100);
        request.setUser_id(1);
        request.setRestaurant_id(25);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        
        // Check that cart_value is an Integer
        Object cartValue = responseBody.get("cart_value");
        Assert.assertTrue("Cart value should be an Integer but was " + cartValue.getClass().getName(), 
                          cartValue instanceof Integer);
        Assert.assertEquals(85, cartValue); // 100 - (15% of 100)
    }
    
    @Test
    public void testApiResponseTypes() {
        List<String> segments = Arrays.asList("p4");
        OfferRequest offerRequest = new OfferRequest(30, "FLATX", 25, segments);
        
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/v1/offer", offerRequest, ApiResponse.class);
        Assert.assertEquals(200, response.getStatusCodeValue());
        
        ApiResponse apiResponse = response.getBody();
        Assert.assertNotNull(apiResponse);
        
        // Verify response_msg is a String
        Assert.assertTrue("Response message should be a String",
                          apiResponse.getResponse_msg() instanceof String);
        Assert.assertEquals("success", apiResponse.getResponse_msg());
    }
    
    // 11. Combined Edge Cases Tests
    @Test
    public void testBulkOfferCreation() {
        long startTime = System.currentTimeMillis();
        
        // Create 10 offers in a loop
        for (int i = 50; i < 60; i++) {
            createOffer(i, (i % 2 == 0) ? "FLATX" : "FLATP", 10 + i, Arrays.asList("p" + (i % 3 + 1)));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Test should complete in reasonable time - adjust threshold as needed
        Assert.assertTrue("Bulk creation should be under 1000ms but was " + duration + "ms", duration < 1000);
        
        // Test a couple of the created offers
        ApplyOfferRequest request = new ApplyOfferRequest();
        request.setCart_value(200);
        request.setUser_id(3); // p3 segment for restaurant 50 (should match what we created above)
        request.setRestaurant_id(50);
        
        ResponseEntity<HashMap> response = restTemplate.postForEntity("/api/v1/cart/apply_offer", request, HashMap.class);
        Map<String, Object> responseBody = response.getBody();
        Assert.assertNotNull(responseBody);
        
        // Restaurant 50 should have a FLATX discount of 60 for p3 segment
        Assert.assertEquals(140, responseBody.get("cart_value")); // 200 - 60
    }
    
    @Test
    public void testComplexOfferScenario() {
        // Create a mix of offers for restaurant 150
        createOffer(150, "FLATX", 50, Arrays.asList("p1"));
        createOffer(150, "FLATP", 30, Arrays.asList("p2"));
        createOffer(150, "FLATP", 25, Arrays.asList("p1", "p2", "p3"));
        
        // Test p1 user - should get FLATX 50 off
        ApplyOfferRequest request1 = new ApplyOfferRequest();
        request1.setCart_value(300);
        request1.setUser_id(1);
        request1.setRestaurant_id(150);
        
        long startTime1 = System.currentTimeMillis();
        ResponseEntity<HashMap> response1 = restTemplate.postForEntity("/api/v1/cart/apply_offer", request1, HashMap.class);
        long duration1 = System.currentTimeMillis() - startTime1;
        
        Assert.assertTrue("Response time should be under 200ms", duration1 < 200);
        Assert.assertEquals(250, response1.getBody().get("cart_value")); // 300 - 50
        
        // Test p2 user - should get FLATP 30% off
        ApplyOfferRequest request2 = new ApplyOfferRequest();
        request2.setCart_value(300);
        request2.setUser_id(2);
        request2.setRestaurant_id(150);
        
        ResponseEntity<HashMap> response2 = restTemplate.postForEntity("/api/v1/cart/apply_offer", request2, HashMap.class);
        Assert.assertEquals(210, response2.getBody().get("cart_value")); // 300 - (30% of 300)
        
        // Test p3 user - should get multi-segment offer FLATP 25% off
        ApplyOfferRequest request3 = new ApplyOfferRequest();
        request3.setCart_value(300);
        request3.setUser_id(3);
        request3.setRestaurant_id(150);
        
        ResponseEntity<HashMap> response3 = restTemplate.postForEntity("/api/v1/cart/apply_offer", request3, HashMap.class);
        Assert.assertEquals(225, response3.getBody().get("cart_value")); // 300 - (25% of 300)
    }
}
