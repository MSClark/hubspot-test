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
public class InvitationServiceImpl implements InvitationService {
    RestTemplate restTemplate = new RestTemplate();

    @Value("${hubspot.get-partners.url}") //Would normally store api keys in a secure location, but this works for demo purposes
    private String getPartnersUrl;

    @Value("${hubspot.post-partners.url}")
    private String postPartnersUrl;

    @Override
    public PartnerList getPartners() { //GET request to get partner list
        return restTemplate.getForObject(getPartnersUrl, PartnerList.class);
    }

    @Override
    public  Map<String, List<Partner>> sortPartnersByCountry(PartnerList partners) throws JsonProcessingException { //Sort partners by country
      ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(partners));
      return partners.getPartners().stream().collect(Collectors.groupingBy(Partner::getCountry));
    }

    @Override
    public ResponseEntity findBestDatesThenSendInvitation(Map<String, List<Partner>> partners) throws JsonProcessingException {
        List<CountryInvitation> countryInvitationResponse = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<String, List<Partner>> entry : partners.entrySet()) {
            String countryName = entry.getKey();
            List<Partner> partnersList = entry.getValue();

            Map<LocalDate, List<Partner>> dateToPartnersMap = new HashMap<>();
            partnersList.forEach(partner -> {
                List<LocalDate> sortedDates = partner.getAvailableDates().stream() //For each partner in a particular country get the list of dates
                        .map(date -> LocalDate.parse(date, formatter))
                        .sorted()
                        .toList();

                for (int i = 0; i < sortedDates.size() - 1; i++) {
                    LocalDate currentDate = sortedDates.get(i);
                    LocalDate nextDate = sortedDates.get(i + 1);

                    if (currentDate.plusDays(1).equals(nextDate)) { //add partner to availability map if they are available on consecutive days
                        dateToPartnersMap.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(partner);
                    }
                }
            });

            Map<Integer, List<Map.Entry<LocalDate, List<Partner>>>> groupedByCount = dateToPartnersMap.entrySet().stream()
                    .collect(Collectors.groupingBy(e -> e.getValue().size())); //Group partners by count of consecutive days they are available


            int maxCount = Collections.max(groupedByCount.keySet()); // Get the dates with the maximum number of partners who can attend
            List<Map.Entry<LocalDate, List<Partner>>> bestDates = groupedByCount.get(maxCount);

            //Choose earliest date if there are multiple.
            Map.Entry<LocalDate, List<Partner>> earliestBestDate = bestDates.stream()
                    .min(Map.Entry.comparingByKey())
                    .orElse(null);

            CountryInvitation countryInvitation = new CountryInvitation();
            countryInvitation.setName(countryName);

            if (earliestBestDate != null) { //build invitation object per country
                countryInvitation.setStartDate(earliestBestDate.getKey().toString());
                countryInvitation.setAttendeeCount(earliestBestDate.getValue().size());
                countryInvitation.setAttendees(earliestBestDate.getValue().stream().map(Partner::getEmail).collect(Collectors.toList()));
            } else {
                countryInvitation.setStartDate(null);
                countryInvitation.setAttendeeCount(0);
                countryInvitation.setAttendees(Collections.emptyList());
            }

            countryInvitationResponse.add(countryInvitation);
        }

        if (countryInvitationResponse.isEmpty()) {
            return null;
        }

        InvitationList invitationList = new InvitationList();
        invitationList.setCountries(countryInvitationResponse);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(invitationList));//debugging

        //TODO extract into separate method

        ResponseEntity<InvitationList> responseEntity = restTemplate.postForEntity(postPartnersUrl, invitationList, InvitationList.class);

        return responseEntity;
    }
}

