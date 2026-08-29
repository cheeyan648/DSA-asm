package control;

/**
 * The outcome of a cross-module operation: whether it happened, a message
 * explaining why, and whatever record it produced.
 *
 * Returned instead of a bare boolean because these operations fail for
 * reasons the user needs to be told - no room is ready, the bill is not
 * settled, the member's tier is too low - and a false on its own cannot say
 * which. Keeping the reason with the result also stops the control classes
 * from printing directly, which would put display work in the wrong layer.
 *
 * @author Wong Chee Yan
 */
public class ServiceResult<T> {

  private final boolean success;
  private final String message;
  private final T value;

  private ServiceResult(boolean success, String message, T value) {
    this.success = success;
    this.message = message;
    this.value = value;
  }

  public static <T> ServiceResult<T> ok(String message, T value) {
    return new ServiceResult<>(true, message, value);
  }

  public static <T> ServiceResult<T> ok(String message) {
    return new ServiceResult<>(true, message, null);
  }

  public static <T> ServiceResult<T> fail(String message) {
    return new ServiceResult<>(false, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean isFailure() {
    return !success;
  }

  public String getMessage() {
    return message;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return (success ? "OK: " : "FAILED: ") + message;
  }
}
