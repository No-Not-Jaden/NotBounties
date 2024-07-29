DROP PROCEDURE IF EXISTS edit_bounty;
CREATE PROCEDURE edit_bounty(
    IN p_name VARCHAR(16),
    IN p_uuid CHAR(36),
    IN p_setter VARCHAR(16),
    IN p_suuid CHAR(36),
    IN p_notified BOOLEAN,
    IN p_time BIGINT,
    IN p_whitelist VARCHAR(369),
    IN p_playtime BIGINT,
    IN p_items BLOB,
    IN p_display FLOAT(53),
    IN p_change FLOAT(53)
)
BEGIN
    DECLARE v_count INT;
    DECLARE new_display FLOAT(53);

    -- Check if a row with the given uuid and id exists
    SELECT COUNT(*) INTO v_count
    FROM notbounties
    WHERE uuid = p_uuid AND suuid = p_suuid AND time = p_time;

    -- If exists, update the row
    IF v_count > 0 THEN
        UPDATE notbounties
        SET amount = amount + p_change, display = p_display + p_change
        WHERE uuid = p_uuid AND suuid = p_suuid AND time = p_time;
    ELSE
        -- Otherwise, insert a new row
        SET new_display = p_display + p_change;
        INSERT INTO notbounties(uuid, name, setter, suuid, amount, notified, time, whitelist, playtime, items, display)
        VALUES (p_uuid, p_name, p_setter, p_suuid, p_change, p_notified, p_time, p_whitelist, p_playtime, p_items, new_display);
    END IF;
END;
