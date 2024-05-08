package com.example.demo;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PartnerService {
    PartnerList getPartners();
    Map<String, List<Partner>> sortPartners(PartnerList partners) throws JsonProcessingException;
    ResponseEntity findBestDates(Map<String, List<Partner>> partners) throws JsonProcessingException;
}
