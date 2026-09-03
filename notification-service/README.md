# Forge Notification Service

Independent Kafka consumer for `OrderConfirmed` events. It owns notification attempts and provider state in the notification PostgreSQL database; it never reads the Forge database.

Run with `mvn test` from this directory. Configuration is supplied through environment variables. The local provider reference is deterministic and must be replaced by a credential-backed adapter before production.
