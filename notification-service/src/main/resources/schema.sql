CREATE TABLE IF NOT EXISTS notification_attempts (
  event_id uuid PRIMARY KEY,
  order_id text NOT NULL,
  customer_id text,
  channel text NOT NULL,
  status text NOT NULL,
  provider_reference text,
  error_message text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
