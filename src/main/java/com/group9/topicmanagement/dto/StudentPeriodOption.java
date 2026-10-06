package com.group9.topicmanagement.dto;

/** Only the student's registered periods, without exposing unpublished scores. */
public record StudentPeriodOption(Long id, String name) {}
