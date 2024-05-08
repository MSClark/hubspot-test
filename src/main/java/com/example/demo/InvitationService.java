package com.example.demo;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface InvitationService {
    PartnerList getPartners();
    Map<String, List<Partner>> sortPartnersByCountry(PartnerList partners) throws JsonProcessingException;
    ResponseEntity findBestDatesThenSendInvitation(Map<String, List<Partner>> partners) throws JsonProcessingException;
}
