package com.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.service.Dog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import com.springboot.service.Animal;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class AutowiredController {


	List<OfferRequest> allOffers = new ArrayList<>();

	@PostMapping(path = "/api/v1/offer")
	public ApiResponse postOperation(@RequestBody OfferRequest offerRequest) {
		System.out.println(offerRequest);
		allOffers.add(offerRequest);
		return new ApiResponse("success");
	}

	@PostMapping(path = "/api/v1/cart/apply_offer")
	public Map<String, Object> applyOffer(@RequestBody ApplyOfferRequest applyOfferRequest) throws Exception {
		System.out.println(applyOfferRequest);
		int cartVal = Math.max(0, applyOfferRequest.getCart_value()); // Ensure non-negative cart value
		SegmentResponse segmentResponse = getSegmentResponse(applyOfferRequest.getUser_id());
		Optional<OfferRequest> matchRequest = allOffers.stream()
				.filter(x -> x.getRestaurant_id() == applyOfferRequest.getRestaurant_id())
				.filter(x -> x.getCustomer_segment().contains(segmentResponse.getSegment()))
				.findFirst();

		if (matchRequest.isPresent()) {
			System.out.println("got a match");
			OfferRequest gotOffer = matchRequest.get();

			if (gotOffer.getOffer_type().equals("FLATX")) {
				cartVal = Math.max(0, cartVal - gotOffer.getOffer_value());
			} else {
				cartVal = (int) Math.max(0, cartVal - cartVal * gotOffer.getOffer_value() * (0.01));
			}
		}
		Map<String, Object> response = new HashMap<>();
		response.put("cart_value", cartVal);
		return response;
	}

	private SegmentResponse getSegmentResponse(int userid) {
		SegmentResponse segmentResponse = new SegmentResponse();
		try {
			String urlString = "http://localhost:1080/api/v1/user_segment?" + "user_id=" + userid;
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("accept", "application/json");

			InputStream responseStream = connection.getInputStream();
			ObjectMapper mapper = new ObjectMapper();
			segmentResponse = mapper.readValue(responseStream, SegmentResponse.class);
			System.out.println("got segment response" + segmentResponse);

		} catch (Exception e) {
			System.out.println(e);
			segmentResponse.setSegment("unknown");
		}
		return segmentResponse;
	}


}
