package com.example.demo;

import lombok.Data;
import java.util.List;

@Data
public class Country {
    private int attendeeCount;
    private List<String> attendees;
    private String name;
    private String startDate;
}
