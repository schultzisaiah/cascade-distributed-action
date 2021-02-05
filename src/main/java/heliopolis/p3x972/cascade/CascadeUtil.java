package heliopolis.p3x972.cascade;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.compare;

import heliopolis.p3x972.cascade.aux.ServerInfo;
import heliopolis.p3x972.cascade.bean.CascadeResult;
import heliopolis.p3x972.cascade.bean.CascadeResults;
import heliopolis.p3x972.cascade.config.CascadeConfig;
import heliopolis.p3x972.cascade.config.HttpMethod;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class CascadeUtil<T> {

  private static UnirestInstance unirest;

  private final CascadeConfig<T> config;

  private CascadeUtil(CascadeConfig<T> config) {
    this.config = config;
  }

  public static <E> CascadeUtil<E> createInstance(CascadeConfig<E> config) {
    return new CascadeUtil<>(config);
  }

  public CascadeResults run(boolean doCascade) {
    return run(null, doCascade);
  }

  public CascadeResults run(T syncEntity, boolean doCascade) {
    LocalDateTime start = LocalDateTime.now();
    startUnirest();
    List<Callable<Pair<String, CascadeResult>>> thingsToDo = new ArrayList<>();

    thingsToDo.add(() -> Pair.of(ServerInfo.getLocalHostName(), localAction(syncEntity)));
    if (doCascade) {
      thingsToDo.addAll(createCascadeCallables(syncEntity));
    }

    // if cascading to multiple servers, do them in parallel as configured:
    CascadeResults results =
        CascadeResults.builder()
            .results(
                ofNullable(
                        Flux.fromIterable(thingsToDo)
                            .parallel(config.getParallelism())
                            .runOn(Schedulers.parallel())
                            .map(this::handleCallable)
                            .collectSortedList(
                                (a, b) -> compare(a.getKey(), b.getKey()), thingsToDo.size())
                            .block(config.getTimeout()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> a)))
            .totalRuntimeSeconds(secondsSince(start))
            .coordinatingNode(ServerInfo.getLocalHostName())
            .build();

    stopUnirest();
    return results;
  }

  private CascadeResult localAction(T syncEntity) {
    LocalDateTime start = LocalDateTime.now();
    boolean success = false;
    String error = "";
    try {
      config.getLocalAction().accept(syncEntity);
      success = true;
    } catch (Exception e) {
      error =
          format(
              "Failed to perform action: %s - due to %s: %s",
              config.getActionDescription(), e.getClass(), e.getMessage());
    }
    String message =
        success
            ? format("Action success: %s: %s", config.getActionDescription(), syncEntity)
            : error;
    return CascadeResult.builder()
        .message(message)
        .success(success)
        .runtimeSeconds(secondsSince(start))
        .build();
  }

  private List<Callable<Pair<String, CascadeResult>>> createCascadeCallables(T syncEntity) {
    List<Callable<Pair<String, CascadeResult>>> result = new ArrayList<>();
    if (StringUtils.isBlank(ServerInfo.getLocalHostName())) {
      CascadeResult warningMessage =
          CascadeResult.builder()
              .message(
                  "Unable to self-identify server, so won't cascade (to prevent infinite loops). "
                      + "Only the server receiving this command will perform action: "
                      + config.getActionDescription())
              .build();
      result.add(() -> Pair.of("warning", warningMessage));
    } else {
      result.addAll(
          Arrays.stream(config.getHosts())
              .filter(host -> !host.contains(ServerInfo.getLocalHostName()))
              .map(host -> createRestCall(host, config.getMethod(), syncEntity))
              .collect(Collectors.toList()));
    }
    return result;
  }

  private Callable<Pair<String, CascadeResult>> createRestCall(
      String host, HttpMethod method, T syncEntity) {
    return () -> {
      CascadeResult response;
      if (config.isCascadeEnabled()) {
        String url = format("%s%s?%s", host, config.getUri(), config.getCascadeFalseParamString());
        try {
          response =
              ofNullable(
                      unirest
                          .request(method.getUnirestMethod().name(), url)
                          .body(syncEntity)
                          .contentType("application/json")
                          .asObject(CascadeResult.class)
                          .getBody())
                  .orElse(
                      CascadeResult.builder().message("Invalid/unknown cascade response").build());
        } catch (Exception e) {
          response =
              CascadeResult.builder()
                  .errorMessage(format("%s: %s", e.getClass(), e.getMessage()))
                  .build();
        }
      } else {
        response = CascadeResult.builder().success(false).message("Cascading is disabled.").build();
      }
      return Pair.of(normalizeHostDomain(host), response);
    };
  }

  private Pair<String, CascadeResult> handleCallable(Callable<Pair<String, CascadeResult>> c) {
    try {
      return c.call();
    } catch (Exception e) {
      return Pair.of(
          "unexpectedError",
          CascadeResult.builder()
              .errorMessage(format("%s: %s", e.getClass(), e.getMessage()))
              .build());
    }
  }

  private static double secondsSince(LocalDateTime start) {
    return start.until(LocalDateTime.now(), ChronoUnit.MILLIS) / 1000.0;
  }

  private static void startUnirest() {
    unirest = Unirest.spawnInstance();
    unirest.config().addDefaultHeader(HttpHeaders.ACCEPT, "application/json");
  }

  private static void stopUnirest() {
    unirest.shutDown();
    unirest = null;
  }

  private static String normalizeHostDomain(String host) {
    return host.replaceFirst("http[s]?://", "").replaceFirst(":[0-9]+", "");
  }
}
