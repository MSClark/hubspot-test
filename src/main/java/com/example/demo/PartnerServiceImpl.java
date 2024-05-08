package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PartnerServiceImpl implements PartnerService {
    RestTemplate restTemplate = new RestTemplate();

    @Value("${hubspot.get-partners.url}")
    private String getPartnersUrl;

    @Value("${hubspot.post-partners.url}")
    private String postPartnersUrl;

    @Override
    public PartnerList getPartners() {
        return restTemplate.getForObject(getPartnersUrl, PartnerList.class);
    }

    @Override
    public  Map<String, List<Partner>> sortPartners(PartnerList partners) throws JsonProcessingException {
      ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(partners));
      return partners.getPartners().stream().collect(Collectors.groupingBy(Partner::getCountry));
    }

    @Override
    public ResponseEntity findBestDates(Map<String, List<Partner>> partners) throws JsonProcessingException {
        List<Country> countryResponses = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<String, List<Partner>> entry : partners.entrySet()) {
            String countryName = entry.getKey();
            List<Partner> partnersList = entry.getValue();

            Map<LocalDate, List<Partner>> dateToPartnersMap = new HashMap<>();
            for (Partner partner : partnersList) {
                List<LocalDate> sortedDates = partner.getAvailableDates().stream()
                        .map(date -> LocalDate.parse(date, formatter))
                        .sorted()
                        .collect(Collectors.toList());

                for (int i = 0; i < sortedDates.size() - 1; i++) {
                    LocalDate currentDate = sortedDates.get(i);
                    LocalDate nextDate = sortedDates.get(i + 1);

                    if (currentDate.plusDays(1).equals(nextDate)) {
                        dateToPartnersMap.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(partner);
                    }
                }
            }

            Optional<Map.Entry<LocalDate, List<Partner>>> bestDateEntry = dateToPartnersMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue(Comparator.comparingInt(List::size)));

            Country country = new Country();
            country.setName(countryName);

            if (bestDateEntry.isPresent() && dateToPartnersMap.containsKey(bestDateEntry.get().getKey().plusDays(1))) {
                Map.Entry<LocalDate, List<Partner>> bestDate = bestDateEntry.get();
                country.setStartDate(bestDate.getKey().toString());
                country.setAttendeeCount(bestDate.getValue().size());
                country.setAttendees(bestDate.getValue().stream().map(Partner::getEmail).collect(Collectors.toList()));
            } else {
                country.setStartDate(null);
                country.setAttendeeCount(0);
                country.setAttendees(Collections.emptyList());
            }

            countryResponses.add(country);
        }

        if (countryResponses.isEmpty()) {
            return null;
        }

        CountriesResponse countriesResponse = new CountriesResponse();
        countriesResponse.setCountries(countryResponses);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(countriesResponse));
        System.out.println(countriesResponse);
        ResponseEntity<CountriesResponse> responseEntity = restTemplate.postForEntity(postPartnersUrl, countriesResponse, CountriesResponse.class);
        return responseEntity;
    }
}

