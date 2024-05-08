package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/partner-api")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @RequestMapping(value = "/partners", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity getPartners() throws JsonProcessingException {
         return invitationService.findBestDatesThenSendInvitation(invitationService.sortPartnersByCountry(invitationService.getPartners()));
    }

}
