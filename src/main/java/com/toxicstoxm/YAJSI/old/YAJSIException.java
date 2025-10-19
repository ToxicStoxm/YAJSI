package com.toxicstoxm.YAJSI.old;

import lombok.Builder;

/**
 * Exception used by {@link SettingsManagerOLD} to indicate various errors.
 * @author ToxicStoxm
 */
public class YAJSIException extends RuntimeException {

  @Builder
  protected YAJSIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
