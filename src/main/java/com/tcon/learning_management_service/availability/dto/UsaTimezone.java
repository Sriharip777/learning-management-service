package com.tcon.learning_management_service.availability.dto;

import lombok.Getter;

@Getter
public enum UsaTimezone {

    // ===== EASTERN TIME =====
    CONNECTICUT("Connecticut", "CT", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    DELAWARE("Delaware", "DE", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    FLORIDA("Florida", "FL", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    GEORGIA("Georgia", "GA", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    INDIANA("Indiana", "IN", "America/Indiana/Indianapolis", "Eastern Time (ET)", "UTC-5/UTC-4"),
    KENTUCKY("Kentucky", "KY", "America/Kentucky/Louisville", "Eastern Time (ET)", "UTC-5/UTC-4"),
    MAINE("Maine", "ME", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    MARYLAND("Maryland", "MD", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    MASSACHUSETTS("Massachusetts", "MA", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    MICHIGAN("Michigan", "MI", "America/Detroit", "Eastern Time (ET)", "UTC-5/UTC-4"),
    NEW_HAMPSHIRE("New Hampshire", "NH", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    NEW_JERSEY("New Jersey", "NJ", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    NEW_YORK("New York", "NY", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    NORTH_CAROLINA("North Carolina", "NC", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    OHIO("Ohio", "OH", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    PENNSYLVANIA("Pennsylvania", "PA", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    RHODE_ISLAND("Rhode Island", "RI", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    SOUTH_CAROLINA("South Carolina", "SC", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    TENNESSEE("Tennessee", "TN", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    VERMONT("Vermont", "VT", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    VIRGINIA("Virginia", "VA", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    WEST_VIRGINIA("West Virginia", "WV", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),
    WASHINGTON_DC("Washington D.C.", "DC", "America/New_York", "Eastern Time (ET)", "UTC-5/UTC-4"),

    // ===== CENTRAL TIME =====
    ALABAMA("Alabama", "AL", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    ARKANSAS("Arkansas", "AR", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    ILLINOIS("Illinois", "IL", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    IOWA("Iowa", "IA", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    KANSAS("Kansas", "KS", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    LOUISIANA("Louisiana", "LA", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    MINNESOTA("Minnesota", "MN", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    MISSISSIPPI("Mississippi", "MS", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    MISSOURI("Missouri", "MO", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    NEBRASKA("Nebraska", "NE", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    NORTH_DAKOTA("North Dakota", "ND", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    OKLAHOMA("Oklahoma", "OK", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    SOUTH_DAKOTA("South Dakota", "SD", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    TEXAS("Texas", "TX", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),
    WISCONSIN("Wisconsin", "WI", "America/Chicago", "Central Time (CT)", "UTC-6/UTC-5"),

    // ===== MOUNTAIN TIME =====
    ARIZONA("Arizona", "AZ", "America/Phoenix", "Mountain Time (MT)", "UTC-7"),
    COLORADO("Colorado", "CO", "America/Denver", "Mountain Time (MT)", "UTC-7/UTC-6"),
    IDAHO("Idaho", "ID", "America/Boise", "Mountain Time (MT)", "UTC-7/UTC-6"),
    MONTANA("Montana", "MT", "America/Denver", "Mountain Time (MT)", "UTC-7/UTC-6"),
    NEW_MEXICO("New Mexico", "NM", "America/Denver", "Mountain Time (MT)", "UTC-7/UTC-6"),
    UTAH("Utah", "UT", "America/Denver", "Mountain Time (MT)", "UTC-7/UTC-6"),
    WYOMING("Wyoming", "WY", "America/Denver", "Mountain Time (MT)", "UTC-7/UTC-6"),
    NEVADA("Nevada", "NV", "America/Los_Angeles", "Pacific Time (PT)", "UTC-8/UTC-7"),

    // ===== PACIFIC TIME =====
    CALIFORNIA("California", "CA", "America/Los_Angeles", "Pacific Time (PT)", "UTC-8/UTC-7"),
    OREGON("Oregon", "OR", "America/Los_Angeles", "Pacific Time (PT)", "UTC-8/UTC-7"),
    WASHINGTON("Washington", "WA", "America/Los_Angeles", "Pacific Time (PT)", "UTC-8/UTC-7"),

    // ===== ALASKA TIME =====
    ALASKA("Alaska", "AK", "America/Anchorage", "Alaska Time (AKT)", "UTC-9/UTC-8"),

    // ===== HAWAII TIME =====
    HAWAII("Hawaii", "HI", "Pacific/Honolulu", "Hawaii Time (HST)", "UTC-10");

    private final String stateName;
    private final String stateCode;
    private final String timezoneId;       // Java ZoneId string
    private final String timezoneLabel;    // Human-readable label
    private final String utcOffset;        // Display offset

    UsaTimezone(String stateName, String stateCode,
                String timezoneId, String timezoneLabel, String utcOffset) {
        this.stateName     = stateName;
        this.stateCode     = stateCode;
        this.timezoneId    = timezoneId;
        this.timezoneLabel = timezoneLabel;
        this.utcOffset     = utcOffset;
    }

    // Find by state code (e.g. "NY" → UsaTimezone.NEW_YORK)
    public static UsaTimezone findByStateCode(String code) {
        for (UsaTimezone tz : values()) {
            if (tz.stateCode.equalsIgnoreCase(code)) return tz;
        }
        throw new IllegalArgumentException("Unknown state code: " + code);
    }

    // Find by timezone ID (e.g. "America/New_York")
    public static UsaTimezone findByTimezoneId(String tzId) {
        for (UsaTimezone tz : values()) {
            if (tz.timezoneId.equalsIgnoreCase(tzId)) return tz;
        }
        throw new IllegalArgumentException("Unknown timezone ID: " + tzId);
    }
}
