package com.example.housebackend.dto.certification;

import java.util.List;

public record CertificationSubmitRequest(List<String> documentUrls, String reason) {
}
