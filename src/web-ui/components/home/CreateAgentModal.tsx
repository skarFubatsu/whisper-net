import { useEffect, useState } from "react";

import { createNewAgent, fetchAgents } from "@/store/slices/agents-slice";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import type { CreateAgentFormData } from "@/types/ui";
import type { AgentRole } from "@/types/api";

type CreateAgentModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

const DEFAULT_FORM_DATA: CreateAgentFormData = {
  agentId: null,
  nickname: "",
  role: "NORMAL",
  bias: 0.5,
  stubbornness: 0.5,
  susceptibility: 0.5,
  suspiciousness: 0.5,
};

const AGENT_ROLES: AgentRole[] = ["NORMAL", "TRIGGER", "RELAY"];

type FormErrors = Partial<Record<keyof CreateAgentFormData, string>>;

/** Validate form data */
function validateForm(data: CreateAgentFormData): FormErrors {
  const errors: FormErrors = {};

  if (!data.nickname.trim()) {
    errors.nickname = "Nickname is required";
  } else if (data.nickname.length > 100) {
    errors.nickname = "Nickname must be 100 characters or less";
  }

  const validateTrait = (value: number, name: string) => {
    if (isNaN(value)) {
      return `${name} must be a number`;
    }
    if (value < 0 || value > 1) {
      return `${name} must be between 0.0 and 1.0`;
    }
    return null;
  };

  const biasError = validateTrait(data.bias, "Bias");
  if (biasError) errors.bias = biasError;

  const stubbornError = validateTrait(data.stubbornness, "Stubbornness");
  if (stubbornError) errors.stubbornness = stubbornError;

  const susceptError = validateTrait(data.susceptibility, "Susceptibility");
  if (susceptError) errors.susceptibility = susceptError;

  const suspError = validateTrait(data.suspiciousness, "Suspiciousness");
  if (suspError) errors.suspiciousness = suspError;

  return errors;
}

export function CreateAgentModal({
  isOpen,
  onClose,
}: CreateAgentModalProps) {
  const dispatch = useAppDispatch();
  const { loading, error: apiError } = useAppSelector((state) => state.agents);

  const [formData, setFormData] = useState<CreateAgentFormData>(
    DEFAULT_FORM_DATA
  );
  const [errors, setErrors] = useState<FormErrors>({});

  useEffect(() => {
    if (!isOpen) {
      setFormData(DEFAULT_FORM_DATA);
      setErrors({});
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const validationErrors = validateForm(formData);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});

    try {
      await dispatch(createNewAgent(formData)).unwrap();
      // Refresh agents list
      await dispatch(fetchAgents());
      onClose();
    } catch (err) {
      // Error is handled by Redux state
    }
  };

  const handleTraitChange = (
    trait: "bias" | "stubbornness" | "susceptibility" | "suspiciousness",
    value: number
  ) => {
    setFormData((prev) => ({
      ...prev,
      [trait]: value,
    }));
    // Clear error for this field when user starts editing
    if (errors[trait]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[trait];
        return newErrors;
      });
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/40 backdrop-blur-[1px] px-md py-lg">
      <div className="w-full max-w-dialog flex-shrink-0 rounded-lg border border-outline-variant bg-surface-container-lowest p-lg shadow-lg">
        <h2 className="mb-lg text-h2 text-on-surface">Create New Agent</h2>

        {apiError && (
          <div className="mb-lg rounded border border-error bg-error/10 p-sm text-label-md text-error">
            {apiError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-lg">
          {/* Nickname Input */}
          <div className="space-y-xs">
            <label
              htmlFor="nickname"
              className="block text-label-md text-on-surface"
            >
              Nickname
            </label>
            <input
              id="nickname"
              type="text"
              value={formData.nickname}
              onChange={(e) => {
                setFormData((prev) => ({
                  ...prev,
                  nickname: e.target.value,
                }));
                if (errors.nickname) {
                  setErrors((prev) => {
                    const newErrors = { ...prev };
                    delete newErrors.nickname;
                    return newErrors;
                  });
                }
              }}
              placeholder="e.g., Alice"
              className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface placeholder:text-on-surface-variant focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            />
            {errors.nickname && (
              <p className="text-label-sm text-error">{errors.nickname}</p>
            )}
          </div>

          {/* Role Dropdown */}
          <div className="space-y-xs">
            <label
              htmlFor="role"
              className="block text-label-md text-on-surface"
            >
              Role
            </label>
            <select
              id="role"
              value={formData.role}
              onChange={(e) => {
                setFormData((prev) => ({
                  ...prev,
                  role: e.target.value as AgentRole,
                }));
              }}
              className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              {AGENT_ROLES.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </div>

          {/* Traits Section */}
          <div className="space-y-md border-t border-outline-variant/60 pt-lg">
            <p className="text-label-md text-on-surface-variant">Traits [0.0–1.0]</p>

            {/* Bias */}
            <TraitInput
              label="Bias"
              value={formData.bias}
              onChange={(val) => handleTraitChange("bias", val)}
              error={errors.bias}
              inputId="bias-input"
              sliderId="bias-slider"
            />

            {/* Stubbornness */}
            <TraitInput
              label="Stubbornness"
              value={formData.stubbornness}
              onChange={(val) => handleTraitChange("stubbornness", val)}
              error={errors.stubbornness}
              inputId="stubbornness-input"
              sliderId="stubbornness-slider"
            />

            {/* Susceptibility */}
            <TraitInput
              label="Susceptibility"
              value={formData.susceptibility}
              onChange={(val) => handleTraitChange("susceptibility", val)}
              error={errors.susceptibility}
              inputId="susceptibility-input"
              sliderId="susceptibility-slider"
            />

            {/* Suspiciousness */}
            <TraitInput
              label="Suspiciousness"
              value={formData.suspiciousness}
              onChange={(val) => handleTraitChange("suspiciousness", val)}
              error={errors.suspiciousness}
              inputId="suspiciousness-input"
              sliderId="suspiciousness-slider"
            />
          </div>

          {/* Buttons */}
          <div className="flex gap-sm border-t border-outline-variant/60 pt-lg">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 rounded border border-outline-variant bg-surface px-md py-xs text-label-caps text-on-surface transition-colors hover:bg-surface-container-high disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 rounded bg-primary px-md py-xs text-label-caps text-on-primary transition-colors hover:bg-primary-container disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-on-primary"
            >
              {loading ? "Creating..." : "Create"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

type TraitInputProps = {
  label: string;
  value: number;
  onChange: (value: number) => void;
  error?: string;
  inputId: string;
  sliderId: string;
};

function TraitInput({
  label,
  value,
  onChange,
  error,
  inputId,
  sliderId,
}: TraitInputProps) {
  return (
    <div className="space-y-xs">
      <label
        htmlFor={inputId}
        className="block text-label-sm text-on-surface"
      >
        {label}
      </label>

      <div className="flex items-center gap-sm">
        <input
          id={sliderId}
          type="range"
          min="0"
          max="1"
          step="0.01"
          value={value}
          onChange={(e) => onChange(parseFloat(e.target.value))}
          aria-label={`${label} slider`}
          aria-valuemin={0}
          aria-valuemax={1}
          aria-valuenow={value}
          aria-valuetext={`${label}: ${value.toFixed(2)}`}
          className="flex-1 h-1 bg-surface-container-high rounded-full appearance-none accent-primary cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
        />
        <input
          id={inputId}
          type="number"
          min="0"
          max="1"
          step="0.01"
          value={value.toFixed(2)}
          onChange={(e) => onChange(parseFloat(e.target.value) || 0)}
          className="w-trait-value rounded border border-outline-variant bg-surface px-xs py-xs text-body-sm text-on-surface text-center focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
        />
      </div>

      {error && (
        <p className="text-label-xs text-error">{error}</p>
      )}
    </div>
  );
}
