# Java Memory Model

Visibility, atomicity, and ordering are different properties.

- `volatile` gives visibility and ordering for that variable; it does not make `counter++` atomic.
- `synchronized` and `Lock` provide mutual exclusion and a happens-before handoff.
- `Thread.start()` publishes actions before start to the new thread.
- Successful `Thread.join()` publishes worker actions to the joining thread.
- Safe publication makes an object visible in a valid initialized state; it does not make later mutable operations safe.
- CAS updates a value only when it still equals the expected value.

The experiments in `MemoryModelExperiment`, `VolatileExperiment`, `UnsafeCounter`, and `AtomicExperiment` demonstrate these distinctions.
