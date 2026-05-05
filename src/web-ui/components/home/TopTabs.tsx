import { useMemo, useRef } from "react";

export type HomeTopTab = "agents" | "relationships";

type TopTabsProps = {
  activeTab: HomeTopTab;
  onTabChange: (tab: HomeTopTab) => void;
  isExpanded: boolean;
};

const tabs: Array<{ id: HomeTopTab; label: string }> = [
  { id: "agents", label: "Agents" },
  { id: "relationships", label: "Relationships" },
];

export function TopTabs({ activeTab, onTabChange, isExpanded }: TopTabsProps) {
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
    <div className="border-b border-outline-variant/60 p-md">
      <div
        aria-label="Home sections"
        className="flex gap-sm"
        role="tablist"
      >
        {tabs.map((tab, index) => {
          const selected = activeTab === tab.id;

          return (
            <button
              aria-controls={`panel-${tab.id}`}
              aria-selected={selected}
              className={[
                "rounded px-md py-sm text-label-caps transition-colors",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
                selected
                  ? "bg-primary text-on-primary"
                  : "bg-surface-container text-on-surface hover:bg-surface-container-high",
                !isExpanded ? "md:w-full" : "",
              ].join(" ")}
              id={`tab-${tab.id}`}
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
              <span className={!isExpanded ? "md:hidden" : ""}>{tab.label}</span>
              <span className={!isExpanded ? "hidden md:inline" : "sr-only"}>
                {tab.label[0]}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
