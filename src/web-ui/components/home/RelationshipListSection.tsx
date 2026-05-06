import { useDispatch, useSelector } from "react-redux";

import type { AppDispatch } from "@/store";
import {
  deleteRelationship,
  selectAllRelationships,
  selectRelationshipsLoading,
} from "@/store/slices/relationships-slice";

type RelationshipListSectionProps = {
  networkId: string;
};

export function RelationshipListSection({ networkId }: RelationshipListSectionProps) {
  const dispatch = useDispatch<AppDispatch>();
  const relationships = useSelector(selectAllRelationships);
  const isLoading = useSelector(selectRelationshipsLoading);

  const onDelete = (relationshipId: string) => {
    dispatch(deleteRelationship({ networkId, relationshipId }));
  };

  return (
    <section className="min-h-0 flex-1 overflow-y-auto p-md">
      <h2 className="mb-sm text-label-caps text-on-surface-variant">Current Relationships</h2>

      {relationships.length === 0 ? (
        <p className="rounded border border-outline-variant bg-surface-container p-sm text-body-md text-on-surface-variant">
          No relationships yet.
        </p>
      ) : (
        <ul className="space-y-sm">
          {relationships.map((relationship) => (
            <li
              key={relationship.relationshipId}
              className="flex items-center justify-between rounded border border-outline-variant bg-surface-container-low p-sm"
            >
              <div className="min-w-0">
                <p className="truncate text-body-md text-on-surface">
                  {relationship.sourceAgentId} {"->"} {relationship.targetAgentId}
                </p>
                <p className="text-label-sm text-on-surface-variant">
                  {relationship.relationshipType} | w={relationship.weight ?? 0} | t={relationship.trustValue ?? 0}
                </p>
              </div>

              <button
                aria-label={`Delete relationship ${relationship.relationshipId}`}
                className="rounded px-sm py-xs text-label-caps text-error transition-colors hover:bg-error/10 disabled:cursor-not-allowed disabled:opacity-50"
                disabled={isLoading}
                onClick={() => onDelete(relationship.relationshipId)}
                type="button"
              >
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
