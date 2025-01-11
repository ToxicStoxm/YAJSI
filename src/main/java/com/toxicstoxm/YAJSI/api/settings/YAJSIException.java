package com.toxicstoxm.YAJSI.api.settings;

import lombok.Builder;

/**
 * Excption used by {@link SettingsManager} to indicate various errors.
 * @author ToxicStoxm
 */
public class YAJSIException extends RuntimeException {

  @Builder
  protected YAJSIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
