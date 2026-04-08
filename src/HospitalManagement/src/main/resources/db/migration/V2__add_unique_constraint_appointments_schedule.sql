ALTER TABLE appointments
ADD CONSTRAINT uk_appointments_schedule_id UNIQUE (schedule_id);