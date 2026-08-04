CREATE TABLE planning_preferences (
    id BIGINT NOT NULL,
    default_servings INTEGER NOT NULL,
    max_cooking_minutes INTEGER NULL,
    CONSTRAINT pk_planning_preferences PRIMARY KEY (id),
    CONSTRAINT ck_planning_preferences_singleton CHECK (id = 1),
    CONSTRAINT ck_planning_preferences_default_servings CHECK (default_servings > 0),
    CONSTRAINT ck_planning_preferences_max_minutes CHECK (max_cooking_minutes IS NULL OR max_cooking_minutes > 0)
);

CREATE TABLE planning_preference_excluded_ingredient (
    planning_preferences_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    CONSTRAINT pk_planning_preference_excluded_ingredient PRIMARY KEY (planning_preferences_id, ingredient_id),
    CONSTRAINT fk_planning_preference_excluded_preferences FOREIGN KEY (planning_preferences_id) REFERENCES planning_preferences(id),
    CONSTRAINT fk_planning_preference_excluded_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient(id)
);

INSERT INTO planning_preferences(id, default_servings, max_cooking_minutes) VALUES (1, 1, NULL);
