ALTER TABLE meal_plan_slot
    ADD COLUMN execution_status VARCHAR(16) NOT NULL DEFAULT 'PENDING';

ALTER TABLE meal_plan_slot
    ADD CONSTRAINT ck_meal_plan_slot_execution_status
    CHECK (execution_status IN ('PENDING', 'COMPLETED'));

ALTER TABLE meal_plan
    DROP CONSTRAINT ck_meal_plan_status;

ALTER TABLE meal_plan
    ADD CONSTRAINT ck_meal_plan_status
    CHECK (status IN ('DRAFT', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));
