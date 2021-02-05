package heliopolis.p3x972.cascade.config;

import lombok.Getter;

public enum HttpMethod {
  GET(kong.unirest.HttpMethod.GET),
  POST(kong.unirest.HttpMethod.POST),
  PUT(kong.unirest.HttpMethod.PUT),
  DELETE(kong.unirest.HttpMethod.DELETE),
  PATCH(kong.unirest.HttpMethod.PATCH),
  HEAD(kong.unirest.HttpMethod.HEAD),
  OPTIONS(kong.unirest.HttpMethod.OPTIONS),
  TRACE(kong.unirest.HttpMethod.TRACE);

  @Getter private final kong.unirest.HttpMethod unirestMethod;

  HttpMethod(kong.unirest.HttpMethod unirestMethod) {
    this.unirestMethod = unirestMethod;
  }
}
