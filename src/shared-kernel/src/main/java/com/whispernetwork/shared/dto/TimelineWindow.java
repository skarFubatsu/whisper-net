package com.whispernetwork.shared.dto;

/**
 * Pagination and time-window controls for timeline queries.
 */
public record TimelineWindow(
    Long fromEpochMillis,
    Long toEpochMillis,
    Integer limit,
    Long offset) {

  /**
   * Creates a validated timeline window.
   */
  public TimelineWindow {
    if (limit == null) {
      limit = 100;
    }
    if (limit < 1 || limit > 1000) {
      throw new IllegalArgumentException("limit must be in range [1, 1000]");
    }

    if (offset == null) {
      offset = 0L;
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be >= 0");
    }

    if (fromEpochMillis != null && toEpochMillis != null && fromEpochMillis > toEpochMillis) {
      throw new IllegalArgumentException("fromEpochMillis must be <= toEpochMillis");
    }
  }
}
