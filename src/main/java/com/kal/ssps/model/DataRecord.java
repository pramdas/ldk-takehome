package com.kal.ssps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataRecord {
    @JsonProperty
    private String device;
    @JsonProperty
    private String title;
    @JsonProperty
    private String country;
    private Long sps;
    public DataRecord() {}

    public DataRecord(String device, String title, String country) {
        this.device = device;
        this.title = title;
        this.country = country;
        this.sps = 1L;
    }

    public DataRecord(String device, String title, String country, Long sps) {
        this(device, title, country);
        this.sps = sps;
    }

    public String getDevice() {
        return device;
    }

    public String getTitle() {
        return title;
    }

    public String getCountry() {
        return country;
    }

    public Long getSps() {
        return sps;
    }

    public void setSps(Long sps) {
        this.sps = sps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataRecord)) return false;

        DataRecord that = (DataRecord) o;

        if (getDevice() != null ? !getDevice().equals(that.getDevice()) : that.getDevice() != null) return false;
        if (getTitle() != null ? !getTitle().equals(that.getTitle()) : that.getTitle() != null) return false;
        return getCountry() != null ? getCountry().equals(that.getCountry()) : that.getCountry() == null;
    }

    @Override
    public int hashCode() {
        int result = getDevice() != null ? getDevice().hashCode() : 0;
        result = 31 * result + (getTitle() != null ? getTitle().hashCode() : 0);
        result = 31 * result + (getCountry() != null ? getCountry().hashCode() : 0);
        return result;
    }

    public String toString() {
        return new com.google.gson.Gson().toJson(this);
    }

}