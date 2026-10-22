CREATE TABLE rematch_rooms (
    origin_battle_id uuid PRIMARY KEY REFERENCES battles(id),
    battle_id uuid UNIQUE NOT NULL REFERENCES battles(id),
    proposed_by uuid NOT NULL REFERENCES users(id)
);
