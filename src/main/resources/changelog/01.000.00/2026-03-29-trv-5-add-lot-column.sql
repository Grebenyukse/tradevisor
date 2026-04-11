--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-5/lot-signals-column
--rollback ALTER TABLE tradevisor.signals DROP COLUMN IF EXISTS risk_lot;
--rollback ALTER TABLE tradevisor.signals DROP COLUMN IF EXISTS tp_ticks;
--rollback ALTER TABLE tradevisor.signals DROP COLUMN IF EXISTS sl_ticks;
--rollback ALTER TABLE tradevisor.signals DROP COLUMN IF EXISTS tp_2_sl_ratio;
ALTER TABLE tradevisor.signals ADD COLUMN IF NOT EXISTS risk_lot real null;
ALTER TABLE tradevisor.signals ADD COLUMN IF NOT EXISTS tp_ticks real null;
ALTER TABLE tradevisor.signals ADD COLUMN IF NOT EXISTS sl_ticks real null;
ALTER TABLE tradevisor.signals ADD COLUMN IF NOT EXISTS tp_2_sl_ratio real null;



