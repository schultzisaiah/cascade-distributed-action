package heliopolis.p3x972.cascade.config;

import java.time.Duration;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

@Getter
@Builder
@SuppressWarnings("FieldMayBeFinal")
public class CascadeConfig<E> {

  private String actionDescription;
  @Builder.Default private boolean cascadeEnabled = true;
  @Builder.Default private int parallelism = 2;
  @Builder.Default private Duration timeout = Duration.ofSeconds(30);
  private String[] hosts;
  private String uri;
  @Builder.Default private String cascadeFalseParamString = "cascade=false";
  @Builder.Default private HttpMethod method = HttpMethod.POST;
  private Consumer<E> localAction;

  private CascadeConfig(String actionDescription, boolean cascadeEnabled, int parallelism,
      Duration timeout, String[] hosts, String uri, String cascadeFalseParamString,
      HttpMethod method, Consumer<E> localAction) {
    this.actionDescription = actionDescription;
    this.cascadeEnabled = cascadeEnabled;
    this.parallelism = parallelism;
    this.timeout = timeout;
    this.hosts = hosts;
    this.uri = uri;
    this.cascadeFalseParamString = StringUtils.trimToEmpty(cascadeFalseParamString);
    this.method = method;
    this.localAction = localAction;
    validate();
  }

  private void validate() {
    Validate.notBlank(this.actionDescription, "Description is required");
    Validate.isTrue(this.parallelism > 0, "A positive parallelism value is required");
    Validate.notEmpty(this.hosts, "Hosts array is required");
    Validate.notBlank(this.uri, "URI is required");
    Validate.notNull(this.method, "Method is required");
    Validate.notNull(this.localAction, "Local action consumer is required");
  }
}
