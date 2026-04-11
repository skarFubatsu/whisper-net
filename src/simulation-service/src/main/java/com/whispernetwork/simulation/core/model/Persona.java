package com.whispernetwork.simulation.core.model;

/**
 * Psychological traits associated with an agent.
 */
public record Persona(double bias, double stubbornness, double susceptibility, double suspiciousness) {

  /**
   * Validates that all values are in [0, 1].
   */
  public Persona {
    validateRange("bias", bias);
    validateRange("stubbornness", stubbornness);
    validateRange("susceptibility", susceptibility);
    validateRange("suspiciousness", suspiciousness);
  }

  /**
   * Validates a trait range.
   *
   * @param fieldName field name
   * @param value numeric value
   */
  public static void validateRange(String fieldName, double value) {
    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(fieldName + " must be in [0,1]");
    }
  }
}
