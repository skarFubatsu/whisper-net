import { useMemo, useRef } from "react";

export type AgentSubTab = "available" | "deployed" | "all";

type SubTabNavProps = {
  activeTab: AgentSubTab;
  onTabChange: (tab: AgentSubTab) => void;
};

const tabs: Array<{ id: AgentSubTab; label: string }> = [
  { id: "available", label: "Available" },
  { id: "deployed", label: "Deployed" },
  { id: "all", label: "All" },
];

export function SubTabNav({ activeTab, onTabChange }: SubTabNavProps) {
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const activeIndex = useMemo(() => {
    return tabs.findIndex((tab) => tab.id === activeTab);
  }, [activeTab]);

  const moveFocus = (nextIndex: number) => {
    const target = tabRefs.current[nextIndex];
    if (target) {
      target.focus();
      onTabChange(tabs[nextIndex].id);
    }
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === "ArrowRight") {
      event.preventDefault();
      moveFocus((index + 1) % tabs.length);
      return;
    }

    if (event.key === "ArrowLeft") {
      event.preventDefault();
      moveFocus((index - 1 + tabs.length) % tabs.length);
      return;
    }

    if (event.key === "Home") {
      event.preventDefault();
      moveFocus(0);
      return;
    }

    if (event.key === "End") {
      event.preventDefault();
      moveFocus(tabs.length - 1);
    }
  };

  return (
    <div
      aria-label="Agent filters"
      className="flex gap-xs border-b border-outline-variant/40 px-md py-sm"
      role="tablist"
    >
      {tabs.map((tab, index) => {
        const selected = activeTab === tab.id;
        return (
          <button
            aria-selected={selected}
            className={[
              "rounded px-sm py-xs text-label-caps transition-colors",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
              selected
                ? "bg-primary text-on-primary"
                : "bg-transparent text-on-surface-variant hover:bg-surface-container",
            ].join(" ")}
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            onKeyDown={(event) => onKeyDown(event, index)}
            ref={(element) => {
              tabRefs.current[index] = element;
            }}
            role="tab"
            tabIndex={selected ? 0 : -1}
            type="button"
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
