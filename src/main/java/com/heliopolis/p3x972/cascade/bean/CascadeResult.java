package com.heliopolis.p3x972.cascade.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@JsonInclude(Include.NON_EMPTY)
public class CascadeResult {
  private String message;
  private boolean success;
  private Double runtimeSeconds;
  private String errorMessage;
}
