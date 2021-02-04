package com.heliopolis.p3x972.cascade.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@JsonInclude(Include.NON_EMPTY)
public class CascadeResults {
  private String coordinatingNode;
  private Double totalRuntimeSeconds;
  @Singular private Map<String, CascadeResult> results;
}
