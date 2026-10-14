-- Sample taxonomy and keys, matching the Prep Studio prototype.
insert into platform (id, name, kind) values ('p-java', 'Java', 'LANGUAGE');
insert into category (id, platform_id, name, position) values ('c-java-coll', 'p-java', 'Collections', 0);
insert into subcategory (id, category_id, name, position) values ('s-java-map', 'c-java-coll', 'Map', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-hashmap', 's-java-map', 'HashMap', 'hashmap
java.util.hashmap', '["java.util.HashMap"]', 'A hash-table implementation of Map that stores key-value pairs in buckets indexed by the key''s hashCode(), giving average O(1) get and put.', 'Keys are hashed, spread, and mapped to one of 2^n buckets. Collisions chain in a linked list; since Java 8 a bucket with 8 or more entries (and a table of at least 64) becomes a red-black tree, so the worst case is O(log n).

When size exceeds capacity × load factor (default 0.75) the table doubles and entries are rehashed. HashMap is not thread-safe and allows one null key.', 'Map<String, Integer> wordCount = new HashMap<>();
for (String w : "to be or not to be".split(" ")) {
    wordCount.merge(w, 1, Integer::sum);
}
System.out.println(wordCount); // e.g. {not=1, be=2, or=1, to=2}, order not guaranteed', 'MEDIUM', 'PUBLISHED', '["hashing", "collections"]', '["ConcurrentHashMap", "equals/hashCode contract"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-chm', 's-java-map', 'ConcurrentHashMap', 'concurrenthashmap
chm', '["CHM"]', 'A thread-safe Map that allows concurrent reads and fine-grained locked writes without locking the whole table.', 'Reads are lock-free. Writes use CAS on empty bins and synchronize on the first node of a bin otherwise, so threads updating different bins don''t block each other.

Null keys and values are not allowed, because null would be ambiguous in a concurrent get(). Iterators are weakly consistent and never throw ConcurrentModificationException.', 'ConcurrentMap<String, LongAdder> hits = new ConcurrentHashMap<>();
hits.computeIfAbsent("/home", k -> new LongAdder()).increment();', 'HARD', 'IN_REVIEW', '["concurrency", "collections"]', '["HashMap", "LongAdder"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-treemap', 's-java-map', 'TreeMap', 'treemap', '[]', 'A sorted Map backed by a red-black tree; keys stay in natural or Comparator order with O(log n) operations.', 'Implements NavigableMap, so it supports range queries such as floorKey, ceilingKey, headMap and tailMap. Useful for interval scheduling and leaderboards.', 'TreeMap<Integer, String> slots = new TreeMap<>(Map.of(9, "standup", 13, "review", 16, "1:1"));
System.out.println(slots.floorEntry(14)); // 13=review', 'EASY', 'DRAFT', '["sorting"]', '["NavigableMap"]', current_timestamp, current_timestamp);
insert into subcategory (id, category_id, name, position) values ('s-java-list', 'c-java-coll', 'List', 1);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-arraylist', 's-java-list', 'ArrayList', 'arraylist', '[]', 'A resizable-array implementation of List with O(1) random access and amortized O(1) appends.', 'Backed by an Object[] that grows by about 1.5x when full. Inserting or removing in the middle shifts elements, which is O(n). In practice it beats LinkedList for almost every workload because of cache locality.', 'List<String> names = new ArrayList<>(List.of("Ada", "Linus"));
names.add(1, "Grace"); // shifts "Linus" right', 'EASY', 'PUBLISHED', '["arrays"]', '["LinkedList"]', current_timestamp, current_timestamp);
insert into category (id, platform_id, name, position) values ('c-java-conc', 'p-java', 'Concurrency', 1);
insert into subcategory (id, category_id, name, position) values ('s-java-jmm', 'c-java-conc', 'Memory model', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-volatile', 's-java-jmm', 'volatile', 'volatile', '[]', 'A field modifier that guarantees writes are visible to other threads and prevents reordering around the access.', 'A write to a volatile field happens-before every later read of that field. It does not make compound actions such as count++ atomic; use AtomicInteger or a lock for that.

Typical uses: a stop flag, or the instance field in double-checked locking.', 'private volatile boolean running = true;

public void run() {
    while (running) { doWork(); }
}

public void stop() { running = false; }', 'MEDIUM', 'PUBLISHED', '["visibility"]', '["happens-before", "AtomicInteger"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-hb', 's-java-jmm', 'happens-before', 'happens-before', '[]', 'The Java Memory Model rule that defines when one action''s effects are guaranteed to be visible to another.', 'Edges come from program order, monitor unlock → later lock, volatile write → later read, Thread.start() and Thread.join(). Two conflicting accesses not ordered by happens-before form a data race.', '', 'HARD', 'DRAFT', '["jmm"]', '["volatile", "synchronized"]', current_timestamp, current_timestamp);
insert into subcategory (id, category_id, name, position) values ('s-java-exec', 'c-java-conc', 'Executors', 1);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-tpe', 's-java-exec', 'ThreadPoolExecutor', 'threadpoolexecutor', '[]', 'The configurable thread pool behind the Executors factory methods, defined by core size, max size, a work queue and a rejection policy.', 'New tasks start core threads until corePoolSize is reached, then go to the queue. Only when the queue is full do extra threads start, up to maximumPoolSize. With an unbounded queue, maximumPoolSize is never used.

Rejection policies: AbortPolicy (default), CallerRunsPolicy, DiscardPolicy, DiscardOldestPolicy.', 'ExecutorService pool = new ThreadPoolExecutor(
    4, 8, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy());', 'HARD', 'PUBLISHED', '["thread pools"]', '["ExecutorService", "BlockingQueue"]', current_timestamp, current_timestamp);
insert into category (id, platform_id, name, position) values ('c-java-jvm', 'p-java', 'JVM internals', 2);
insert into subcategory (id, category_id, name, position) values ('s-java-gc', 'c-java-jvm', 'Garbage collection', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-g1', 's-java-gc', 'G1 garbage collector', 'g1 garbage collector
g1gc
garbage-first', '["G1GC", "Garbage-First"]', 'The default JVM collector since Java 9. It splits the heap into equal regions and collects the ones with the most garbage first.', 'G1 targets a pause-time goal (-XX:MaxGCPauseMillis, default 200 ms). Young collections evacuate eden and survivor regions; mixed collections also take old regions chosen by liveness. Humongous objects (at least half a region) get contiguous regions.', '// JVM flags
-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms4g -Xmx4g', 'MEDIUM', 'PUBLISHED', '["gc", "tuning"]', '["ZGC", "Heap regions"]', current_timestamp, current_timestamp);
insert into platform (id, name, kind) values ('p-spring', 'Spring Boot', 'FRAMEWORK');
insert into category (id, platform_id, name, position) values ('c-spring-core', 'p-spring', 'Core container', 0);
insert into subcategory (id, category_id, name, position) values ('s-spring-di', 'c-spring-core', 'Dependency injection', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-scope', 's-spring-di', 'Bean scope', 'bean scope
@scope', '["@Scope"]', 'Defines how many instances of a bean the Spring container creates and how long they live.', 'singleton (default, one per container), prototype (new instance per lookup), and the web scopes request, session and application. Injecting a prototype into a singleton yields a single instance unless you use ObjectProvider or a scoped proxy.', '@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportBuilder { }', 'EASY', 'PUBLISHED', '["ioc"]', '["ObjectProvider", "Scoped proxy"]', current_timestamp, current_timestamp);
insert into category (id, platform_id, name, position) values ('c-spring-data', 'p-spring', 'Data access', 1);
insert into subcategory (id, category_id, name, position) values ('s-spring-tx', 'c-spring-data', 'Transactions', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-propagation', 's-spring-tx', 'Transaction propagation', 'transaction propagation
@transactional(propagation)', '["@Transactional(propagation)"]', 'Decides whether a transactional method joins the caller''s transaction, starts a new one, or runs without one.', 'REQUIRED (default) joins or creates. REQUIRES_NEW suspends the outer transaction and commits independently, which suits audit logs. NESTED uses savepoints.

Self-invocation inside the same class bypasses the proxy, so the annotation has no effect.', '@Transactional(propagation = Propagation.REQUIRES_NEW)
public void writeAudit(AuditEntry entry) {
    auditRepository.save(entry);
}', 'MEDIUM', 'PUBLISHED', '["transactions"]', '["Self-invocation", "Savepoint"]', current_timestamp, current_timestamp);
insert into platform (id, name, kind) values ('p-kafka', 'Apache Kafka', 'PLATFORM');
insert into category (id, platform_id, name, position) values ('c-kafka-msg', 'p-kafka', 'Messaging', 0);
insert into subcategory (id, category_id, name, position) values ('s-kafka-cons', 'c-kafka-msg', 'Partitions & consumers', 0);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-group', 's-kafka-cons', 'Consumer group', 'consumer group
group.id', '["group.id"]', 'A set of consumers sharing a group.id. Each partition of a topic is read by exactly one consumer in the group.', 'Adding consumers scales reads up to the partition count; extra consumers sit idle. Offsets are committed per group, so separate groups each receive every message. A rebalance reassigns partitions when members join or leave.', 'Properties props = new Properties();
props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(List.of("orders"));', 'MEDIUM', 'PUBLISHED', '["scaling"]', '["Partition", "Rebalance", "Offset"]', current_timestamp, current_timestamp);
