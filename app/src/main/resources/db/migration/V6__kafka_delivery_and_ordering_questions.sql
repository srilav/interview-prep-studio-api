-- Apache Kafka delivery guarantees, idempotency, transactions and ordering interview questions.
-- New: Apache Kafka › Messaging › Delivery & ordering questions.
-- Question keys use the question as the term and the short answer as the definition.
insert into subcategory (id, category_id, name, position) values ('s-kafka-qa', 'c-kafka-msg', 'Delivery & ordering questions', 1);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-exactly-once', 's-kafka-qa', 'Can Kafka process an order exactly once, or do you need at-least-once delivery with an idempotent consumer?', 'can kafka process an order exactly once, or do you need at-least-once delivery with an idempotent consumer?
exactly-once processing
exactly-once semantics
eos
at-least-once plus idempotency', '["Exactly-once processing", "Exactly-once semantics", "EOS", "At-least-once plus idempotency"]', 'Exactly-once delivery over a network isn''t possible on its own. You get exactly-once processing from at-least-once delivery plus an idempotent receiver. Kafka transactions give it natively only for Kafka-to-Kafka pipelines.', 'A sender can''t tell a lost message from a lost acknowledgement (the Two Generals'' Problem), so it must retry, and retries create duplicates. Exactly-once is therefore built from two parts: deliver at least once, and make processing the same message twice have the same effect as once.

Three tools, each with its own scope:
- Idempotent producer (enable.idempotence=true): the broker drops duplicates caused by producer retries. Covers producer to broker only.
- Idempotent consumer: the consumer records each message''s business key (for example orderId) under a UNIQUE constraint, in the same database transaction as its side effects. A redelivered message hits the constraint and is skipped. Covers Kafka to a database, end to end.
- Kafka transactions (read-process-write): the output records and the consumed offsets commit atomically. Covers Kafka to Kafka only. A database write or an HTTP call inside the loop is not rolled back with the transaction.

For order processing that writes to a database or calls a payment provider, use the idempotent consumer. Pass the same idempotency key to the payment provider (Stripe and others accept one) so the charge is also deduplicated.', '// Producer: one key per order, chosen once and reused on every retry
String orderId = order.id();
producer.send(new ProducerRecord<>("orders", orderId, toJson(order)));

// Consumer: the UNIQUE key makes a redelivery a no-op
int inserted = jdbc.update(
    "insert into processed_orders (order_id, payload) values (?, ?) on conflict (order_id) do nothing",
    record.key(), record.value());
if (inserted == 1) {
    chargePayment(record.key());   // pass orderId as the provider''s idempotency key
}', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "delivery semantics"]', '["Idempotent producer", "Idempotent consumer", "Kafka transactions", "sendOffsetsToTransaction"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-idempotent-producer', 's-kafka-qa', 'What does enable.idempotence=true do on a Kafka producer?', 'what does enable.idempotence=true do on a kafka producer?
idempotent producer
enable.idempotence
producer id
pid
sequence number', '["Idempotent producer", "enable.idempotence", "Producer ID", "PID", "Sequence number"]', 'The broker gives the producer an ID, and the producer numbers every batch per partition. The broker drops a batch whose sequence it has already written, so retries can''t duplicate or reorder messages. It needs acks=all and at most 5 in-flight requests.', 'How it works:
- On start, the producer gets a producer ID (PID) and an epoch from the broker.
- Each batch sent to a partition carries the PID and a sequence number that increases by one per batch.
- The partition leader remembers the last sequences per PID. A batch it has already written is acknowledged but not written again. A batch that skips ahead is rejected with OutOfOrderSequenceException.

Settings it requires (set automatically when you enable it):
- acks=all, so a batch counts as written only when all in-sync replicas have it.
- retries greater than 0 (default Integer.MAX_VALUE, bounded by delivery.timeout.ms).
- max.in.flight.requests.per.connection at most 5. Up to 5 unacknowledged batches can be in flight and order is still kept.

Since Kafka 3.0, enable.idempotence=true and acks=all are the defaults. If you explicitly set a conflicting value, such as acks=1, the producer fails to start with a ConfigException.

Scope: it deduplicates retries within one producer session for one partition. It does not deduplicate two separate send() calls of the same business event, or sends after a restart without a transactional.id. Use a business key and an idempotent consumer for those.', 'Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");   // default since Kafka 3.0
props.put(ProducerConfig.ACKS_CONFIG, "all");

try (Producer<String, String> producer = new KafkaProducer<>(props)) {
    // The key routes every event of this order to the same partition
    producer.send(new ProducerRecord<>("orders", orderId, payload), (metadata, e) -> {
        if (e != null) {
            log.error("send failed for {}", orderId, e);
        }
    });
}', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "producer"]', '["Exactly-once processing", "Ordering with at-least-once", "Kafka transactions"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-idempotent-consumer', 's-kafka-qa', 'How does a Kafka consumer that writes to a database process each order exactly once?', 'how does a kafka consumer that writes to a database process each order exactly once?
idempotent consumer
consumer deduplication
inbox table
processed_orders', '["Idempotent consumer", "Consumer deduplication", "Inbox table", "processed_orders"]', 'Disable auto-commit. In one database transaction, insert the order''s key into a table with a UNIQUE constraint (ON CONFLICT DO NOTHING) and apply the business change. Commit the database, then commit the Kafka offset. A redelivered message inserts 0 rows and is skipped.', 'Kafka delivers to consumers at least once: a consumer can crash after processing a message but before committing its offset, and the message is delivered again. The consumer makes this safe itself.

Steps for each record:
1. Begin a database transaction.
2. INSERT the business key (orderId) into processed_orders, which has a UNIQUE or primary key constraint, with ON CONFLICT DO NOTHING.
3. If 1 row was inserted, apply the business change in the same transaction. If 0 rows, it''s a duplicate: roll back or skip.
4. Commit the database transaction.
5. After the batch, commit the Kafka offsets with commitSync().

If the consumer dies between 4 and 5, the batch is delivered again, but every key is already in processed_orders, so nothing happens twice.

Points to get right:
- The dedup insert and the business write must share one transaction. Otherwise a crash between them leaves the key recorded with the work not done.
- A check-then-insert (SELECT, then INSERT if missing) races under concurrency. Let the constraint decide.
- An external call (payment, email) is not covered by the database rollback. Give it the same key as its own idempotency key.
- On a failure you can''t retry, send the record to a dead-letter topic instead of blocking the partition forever.
- Expire old keys (for example after a few days) if the table grows large, keeping them longer than any possible redelivery.', '@Transactional
public void handle(ConsumerRecord<String, String> record) {
    int inserted = jdbc.update(
        "insert into processed_orders (order_id, payload, processed_at) values (?, ?, now()) "
            + "on conflict (order_id) do nothing",
        record.key(), record.value());
    if (inserted == 0) {
        log.info("duplicate {} ignored", record.key());
        return;
    }
    orders.applyPayment(record.key(), parse(record.value()));   // same transaction
}

// Poll loop (enable.auto.commit=false)
while (running) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        handler.handle(record);        // database commit per record
    }
    if (!records.isEmpty()) {
        consumer.commitSync();         // offsets only after the work is durable
    }
}', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "consumer", "idempotency"]', '["Exactly-once processing", "At-least-once consumer properties", "Lost update"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-headers', 's-kafka-qa', 'How is metadata passed with a Kafka message?', 'how is metadata passed with a kafka message?
kafka headers
record headers
kip-82
trace id propagation', '["Kafka headers", "Record headers", "KIP-82", "Trace ID propagation"]', 'Custom metadata travels in record headers: key-value pairs with a String key and a byte[] value, beside the key and value. The broker and producer also stamp topic, partition, offset and timestamp, which the consumer reads from ConsumerRecord.', 'Two kinds of metadata:

1. Custom metadata in headers (KIP-82, Kafka 0.11+)
- Each header is a String key and a byte[] value (which may be null). A record can carry several headers, and keys can repeat.
- Use them for things that describe the message rather than the business payload: trace and span IDs (OpenTelemetry''s traceparent), correlation and causation IDs, the source service, schema version, content type, an idempotency key.
- Headers let a consumer or interceptor route, trace or filter without deserialising the payload.

2. System metadata on every record
- Topic and partition: where the record is stored.
- Offset: its position in the partition.
- Timestamp and timestamp type: CreateTime (set by the producer) or LogAppendTime (set by the broker).
- Key: the partitioning key.
- Leader epoch, and serialized key and value sizes.

In Spring Kafka, read a header with @Header(KafkaHeaders.RECEIVED_PARTITION) or @Header("trace-id") on a listener parameter. Spring itself adds headers such as __TypeId__ for JSON type information.', '// Producer: add headers to the record
ProducerRecord<String, String> record = new ProducerRecord<>("orders", orderId, payload);
record.headers()
    .add("trace-id", traceId.getBytes(StandardCharsets.UTF_8))
    .add("source-service", "checkout".getBytes(StandardCharsets.UTF_8));
producer.send(record);

// Consumer: read headers and system metadata
for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(100))) {
    Header trace = r.headers().lastHeader("trace-id");
    String traceId = trace == null ? null : new String(trace.value(), StandardCharsets.UTF_8);
    log.info("topic={} partition={} offset={} ts={} key={} trace={}",
        r.topic(), r.partition(), r.offset(), r.timestamp(), r.key(), traceId);
}', 'EASY', 'PUBLISHED', '["kafka", "interview question", "headers"]', '["Idempotent consumer", "Consumer group"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-at-least-once-consumer', 's-kafka-qa', 'Which consumer properties give at-least-once reads in Kafka?', 'which consumer properties give at-least-once reads in kafka?
at-least-once consumer
enable.auto.commit
auto.offset.reset
isolation.level
commitsync', '["At-least-once consumer", "enable.auto.commit", "auto.offset.reset", "isolation.level", "commitSync"]', 'Set enable.auto.commit=false and commit offsets yourself, with commitSync(), only after the records are processed. Use auto.offset.reset=earliest so a new group doesn''t skip data, and isolation.level=read_committed if producers use transactions.', 'The rule: never commit an offset before the work for that record is durable. A crash before the commit means the records are read again, never lost.

Properties:
- enable.auto.commit=false. With the default true, the consumer commits in the background every auto.commit.interval.ms (5 s), and can commit records your code hasn''t finished.
- auto.offset.reset=earliest. Applies only when the group has no committed offset, or the offset has been deleted by retention. The default latest would start a new group at the end of the log and skip existing messages.
- isolation.level=read_committed. Skips records from aborted transactions and waits for open ones. The default is read_uncommitted. It only matters if producers use transactions.

Committing:
- commitSync() blocks until the broker confirms, and retries. The safe default.
- commitAsync() doesn''t block or retry. A failed async commit retried after a newer one succeeded could move the offset backwards, so a common pattern is commitAsync() in the loop and commitSync() on shutdown and in a rebalance listener''s onPartitionsRevoked.
- commitSync() with no arguments commits the positions returned by the last poll(). To commit part of a batch, pass a map of TopicPartition to OffsetAndMetadata(offset + 1).

The cost: anything that fails after processing but before the commit is delivered again. Pair at-least-once reads with an idempotent consumer.', 'Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processors");
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
    consumer.subscribe(List.of("orders"));
    while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
            process(record);              // business logic first
        }
        if (!records.isEmpty()) {
            consumer.commitSync();        // then the offsets
        }
    }
}', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "consumer", "offsets"]', '["Idempotent consumer", "AckMode", "How many records poll returns"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-listener-ackmode', 's-kafka-qa', 'What are @KafkaListener and AckMode in Spring Kafka, and which AckMode suits at-least-once?', 'what are @kafkalistener and ackmode in spring kafka, and which ackmode suits at-least-once?
@kafkalistener
ackmode
manual_immediate
acknowledgment
spring kafka', '["@KafkaListener", "AckMode", "MANUAL_IMMEDIATE", "Acknowledgment", "Spring Kafka"]', '@KafkaListener turns a method into a consumer that a Spring listener container polls for. AckMode decides when that container commits offsets: RECORD, BATCH (the default), TIME, COUNT, MANUAL or MANUAL_IMMEDIATE.', '@KafkaListener
- Spring creates a listener container (ConcurrentMessageListenerContainer) that runs the poll loop on its own threads, calls your method for each record (or each batch), handles errors through the configured error handler and commits offsets.
- concurrency sets the number of consumer threads. Each partition is still read by only one of them.
- Spring Kafka sets enable.auto.commit=false by default and commits offsets itself according to AckMode.

AckMode (ContainerProperties.AckMode):
- RECORD: commit after the listener returns for each record.
- BATCH (default): commit after all records from one poll() have been processed.
- TIME and COUNT: commit after a time period or a number of records.
- MANUAL: your method receives an Acknowledgment and calls acknowledge(). The commits are queued and done after the current poll''s records are processed.
- MANUAL_IMMEDIATE: acknowledge() commits right away on the listener thread.

Commits are synchronous by default (ContainerProperties.syncCommits=true).

For at-least-once, BATCH or RECORD already only commit after the listener returns successfully. Use MANUAL_IMMEDIATE when you want to decide in code exactly when a record counts as done, for example only after an async step is confirmed. Always acknowledge on the listener thread, and pair it with an idempotent handler.

With a KafkaTransactionManager on the container, offsets are sent to the producer transaction instead, and AckMode doesn''t apply.', '@Bean
ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.setConcurrency(3);   // at most one thread per partition is used
    return factory;
}

@KafkaListener(topics = "orders", groupId = "order-processors")
public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
    orderHandler.handle(record);   // idempotent: dedups on the order key
    ack.acknowledge();             // commit this offset now
}', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "spring kafka"]', '["At-least-once consumer properties", "Idempotent consumer", "Kafka transactions"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-transactional-producer', 's-kafka-qa', 'Why would a producer use a Kafka transaction to send messages to a topic?', 'why would a producer use a kafka transaction to send messages to a topic?
kafka transactions
transactional producer
transactional.id
begintransaction
atomic multi-topic write', '["Kafka transactions", "Transactional producer", "transactional.id", "beginTransaction", "Atomic multi-topic write"]', 'For atomic, all-or-nothing writes: several records across topics and partitions become visible together or not at all. It''s also the basis of exactly-once read-process-write, where the consumed offsets commit in the same transaction as the output.', 'Two reasons to use one:

1. Atomic writes to several topics or partitions
When an order completes you may publish a BillingEvent to billing and an InventoryEvent to inventory. Without a transaction, a failure between the two sends leaves one published and the other not. With a transaction, consumers using isolation.level=read_committed see both or neither.

2. Read-process-write (stream processing)
The service consumes from one topic and produces to another. The transaction includes the output records and the input offsets, so a crash can''t leave the output written but the input unconsumed. Kafka Streams does this when processing.guarantee=exactly_once_v2.

How it works:
- Set a transactional.id that stays the same across restarts of the same logical producer instance. It turns on idempotence and lets the coordinator fence a zombie instance with the same ID (older epoch).
- initTransactions(), then beginTransaction(), send(), and commitTransaction() or abortTransaction().
- The transaction coordinator writes commit or abort markers to every partition involved. read_committed consumers only receive records up to the last stable offset.

What it doesn''t cover: database writes, HTTP calls and other side effects aren''t part of the Kafka transaction. For those, use the outbox pattern or an idempotent consumer.

Cost: extra round trips to the coordinator, and read_committed consumers wait for open transactions. Commit batches of records per transaction, not one transaction per message.', 'props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-service-1");   // stable per instance
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.initTransactions();

try {
    producer.beginTransaction();
    producer.send(new ProducerRecord<>("billing", orderId, billingJson));
    producer.send(new ProducerRecord<>("inventory", orderId, inventoryJson));
    producer.commitTransaction();          // both visible to read_committed consumers, or neither
} catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
    producer.close();                      // fatal: another instance took over this transactional.id
} catch (KafkaException e) {
    producer.abortTransaction();           // neither record is ever seen by read_committed consumers
}', 'HARD', 'PUBLISHED', '["kafka", "interview question", "transactions", "producer"]', '["sendOffsetsToTransaction", "Idempotent producer", "At-least-once consumer properties"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-send-offsets-to-tx', 's-kafka-qa', 'In Kafka exactly-once, what does linking input offsets to output messages mean?', 'in kafka exactly-once, what does linking input offsets to output messages mean?
sendoffsetstotransaction
read-process-write
consume-transform-produce
kip-447', '["sendOffsetsToTransaction", "Read-process-write", "Consume-transform-produce", "KIP-447"]', 'The consumer''s offsets are committed by the producer, inside the same transaction as the output records, through sendOffsetsToTransaction(). Both commit or both abort, so a crash can''t leave output written while the input is read again.', 'The problem without it: the service reads a record, writes the result to the output topic, then crashes before committing the input offset. On restart it reads the same record again and writes the result a second time.

With linked offsets:
1. Poll a batch with enable.auto.commit=false.
2. producer.beginTransaction().
3. Send the output records.
4. producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata()), where offsets maps each TopicPartition to OffsetAndMetadata(lastOffset + 1).
5. producer.commitTransaction().

Behind the scenes, the offsets are written to __consumer_offsets as part of the transaction. The coordinator writes commit markers for the output partitions and the offsets together.

On a crash before commitTransaction(), the transaction is aborted (on restart, or when it times out after transaction.timeout.ms). read_committed consumers never see the aborted output, and the group''s committed offset hasn''t moved, so the input is processed again and the output written once.

Details that matter:
- Pass consumer.groupMetadata(), not just the group ID. It carries the generation and member ID, so the broker can fence an instance that lost its partitions in a rebalance (KIP-447, Kafka 2.5+).
- After abortTransaction(), seek the consumer back to the last committed offsets of its partitions. Otherwise the next poll continues past the aborted batch.
- Downstream consumers must use isolation.level=read_committed, or they''ll see the aborted records.
- In Spring Kafka, a KafkaTransactionManager on the listener container does all of this for you.', 'producer.initTransactions();
while (running) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    if (records.isEmpty()) {
        continue;
    }
    try {
        producer.beginTransaction();
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (ConsumerRecord<String, String> r : records) {
            producer.send(new ProducerRecord<>("orders-priced", r.key(), price(r.value())));
            offsets.put(new TopicPartition(r.topic(), r.partition()), new OffsetAndMetadata(r.offset() + 1));
        }
        producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
        producer.commitTransaction();          // output and input offsets commit together
    } catch (ProducerFencedException e) {
        producer.close();                      // fatal: another instance took over
        break;
    } catch (KafkaException e) {
        producer.abortTransaction();
        rewindToCommitted(consumer);           // seek each partition back to its committed offset
    }
}', 'HARD', 'PUBLISHED', '["kafka", "interview question", "transactions", "exactly-once"]', '["Kafka transactions", "Exactly-once processing", "At-least-once consumer properties"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-poll-records', 's-kafka-qa', 'When a Kafka consumer calls poll(), how many records does it get?', 'when a kafka consumer calls poll(), how many records does it get?
max.poll.records
fetch.max.bytes
max.partition.fetch.bytes
fetch.min.bytes
max.poll.interval.ms', '["max.poll.records", "fetch.max.bytes", "max.partition.fetch.bytes", "fetch.min.bytes", "max.poll.interval.ms"]', 'Anywhere from 0 up to max.poll.records (default 500). The count also depends on what has already been fetched, the byte limits fetch.max.bytes (50 MiB) and max.partition.fetch.bytes (1 MiB), and how much data is waiting.', 'poll(timeout) returns whatever records are ready, up to these limits:

Count:
- max.poll.records (500): the most records one poll() returns. The rest stay buffered in the consumer for the next poll.

Bytes per fetch request to a broker:
- fetch.max.bytes (52,428,800 bytes = 50 MiB): the most data per fetch response.
- max.partition.fetch.bytes (1,048,576 bytes = 1 MiB): the most data per partition. With large messages, these byte limits can cut a poll to far fewer than 500 records.
- These are soft limits: if the first record batch is larger, it''s still returned so the consumer can make progress.

Waiting:
- fetch.min.bytes (1 byte): the broker waits until at least this much data is available...
- fetch.max.wait.ms (500 ms): ...or until this long has passed.
- If nothing arrives before the poll timeout, poll() returns an empty batch.

Why it matters: max.poll.interval.ms (300,000 ms = 5 minutes) is the longest allowed gap between two poll() calls. If processing one batch takes longer (for example 500 records × 1 s each), the consumer is removed from the group, its partitions are rebalanced, and its uncommitted records are processed again by another member. Fix it by lowering max.poll.records, speeding up processing, or raising max.poll.interval.ms. Heartbeats run on a background thread (session.timeout.ms, default 45 s in Kafka 3.0+), so a slow batch fails max.poll.interval.ms, not the session timeout.', 'props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);               // default 500
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);       // 5 minutes per batch
props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 64 * 1024);          // wait for 64 KB...
props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 200);              // ...or 200 ms
props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1_048_576);

ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
log.info("got {} records from {} partitions", records.count(), records.partitions().size());', 'MEDIUM', 'PUBLISHED', '["kafka", "interview question", "consumer", "tuning"]', '["At-least-once consumer properties", "Consumer group", "AckMode"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-ordering-at-least-once', 's-kafka-qa', 'How does Kafka keep messages in order while guaranteeing at-least-once delivery?', 'how does kafka keep messages in order while guaranteeing at-least-once delivery?
kafka ordering
per-partition ordering
message key
max.in.flight.requests.per.connection', '["Kafka ordering", "Per-partition ordering", "Message key", "max.in.flight.requests.per.connection"]', 'Order holds only within a partition. Key related events (orderId) so they share a partition. Use an idempotent producer so retries can''t reorder or duplicate. One thread per partition processes records in sequence and commits after, so a replay repeats them in order.', 'Producer side:
- Message key: Kafka hashes the key (murmur2) to pick the partition, so every event for one order lands in one partition, in send order.
- enable.idempotence=true: with retries on, a failed batch could otherwise be retried after a later batch succeeded, writing 2 before 1. Sequence numbers make the broker reject out-of-order batches, so order holds with up to 5 in-flight requests.
- Without idempotence, set max.in.flight.requests.per.connection=1 to keep order with retries, at a large cost in throughput.
- acks=all, so an acknowledged message survives a leader failover.

Consumer side:
- Within a group, each partition is assigned to exactly one consumer, which reads it in offset order.
- Process each partition''s records one after another, and commit after processing (enable.auto.commit=false).
- After a crash, the next owner restarts from the last committed offset and replays records in their original order. Some are processed twice, so handlers must be idempotent.

What breaks ordering:
- Processing one partition''s records in parallel (a thread pool inside the listener). Use a per-key queue if you need parallelism, as Confluent''s Parallel Consumer does.
- Retry topics and dead-letter topics: a failed record is retried later while newer records for the same key go ahead.
- Adding partitions: the key''s hash maps to a different partition, so old and new events for one key can be read out of order.
- Sending events for one entity with different keys, or with no key.', '// Producer: same key, same partition, in send order
producer.send(new ProducerRecord<>("orders", orderId, "CREATED"));
producer.send(new ProducerRecord<>("orders", orderId, "PAID"));
producer.send(new ProducerRecord<>("orders", orderId, "SHIPPED"));

// Consumer: process each partition''s records in order, then commit
ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
for (TopicPartition tp : records.partitions()) {
    for (ConsumerRecord<String, String> r : records.records(tp)) {
        handler.apply(r.key(), r.value());   // idempotent, sequential per partition
    }
}
consumer.commitSync();', 'HARD', 'PUBLISHED', '["kafka", "interview question", "ordering", "delivery semantics"]', '["Idempotent producer", "Does Kafka keep order by default", "At-least-once consumer properties"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-kafka-ordering-default', 's-kafka-qa', 'Does Kafka maintain message ordering by default?', 'does kafka maintain message ordering by default?
kafka ordering by default
topic vs partition ordering
sticky partitioner
global ordering', '["Kafka ordering by default", "Topic vs partition ordering", "Sticky partitioner", "Global ordering"]', 'Only within a partition. With no key, records spread across partitions, so a multi-partition topic has no overall order. Since Kafka 3.0 the producer is idempotent by default, so with a key (or one partition) order survives retries.', 'What you get out of the box:
- Within one partition: yes. Records are appended and read in offset order.
- Across a topic with several partitions: no. Consumers of different partitions run independently.

Where order is lost:
- No key: since Kafka 2.4 the default partitioner is "sticky". It fills a batch for one partition, then moves to another. Events for one order can land in different partitions and be read out of order.
- Retries without idempotence: with retries and up to 5 requests in flight, batch 2 can be written before a retried batch 1.

Since Kafka 3.0 (producer), enable.idempotence=true and acks=all are the defaults. So with a key per entity, or a single partition, order holds even across retries. Before 3.0, or with idempotence switched off, you had to set it yourself.

Summary:
- No key, several partitions: not ordered.
- Same key, several partitions: ordered per key.
- One partition: ordered for the whole topic, but only one consumer in a group can read it.
- Idempotence off, with retries and more than 1 request in flight: not ordered.

Usually you need order per entity (per order or per customer), not for the whole topic. Keying by that entity keeps the topic scalable.', '// No key: the sticky partitioner may put these on different partitions
producer.send(new ProducerRecord<>("orders", "order-42 CREATED"));
producer.send(new ProducerRecord<>("orders", "order-42 PAID"));

// Keyed: both always go to the same partition, so PAID is read after CREATED
producer.send(new ProducerRecord<>("orders", "order-42", "CREATED"));
producer.send(new ProducerRecord<>("orders", "order-42", "PAID"));', 'EASY', 'PUBLISHED', '["kafka", "interview question", "ordering"]', '["Ordering with at-least-once", "Idempotent producer"]', current_timestamp, current_timestamp);
