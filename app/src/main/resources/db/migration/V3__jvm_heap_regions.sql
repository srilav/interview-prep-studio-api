-- JVM heap regions under G1 and the young collection that runs first.
-- New subcategory Java › JVM internals › Heap memory; Young GC joins Garbage collection.
insert into subcategory (id, category_id, name, position) values ('s-java-heap', 'c-java-jvm', 'Heap memory', 1);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-heap-regions', 's-java-heap', 'G1 heap region types', 'g1 heap region types
heap regions
g1 regions', '["Heap regions", "G1 regions"]', 'G1 splits the heap into many equal-sized regions, and each region is given one of four roles at a time: Eden, Survivor, Old or Humongous.', 'Older collectors carved the heap into a few large contiguous spaces. G1 (the default since Java 9) instead uses thousands of fixed-size regions, a power of two between 1 MB and 32 MB chosen from the heap size (newer JDKs allow up to 512 MB), set with -XX:G1HeapRegionSize.

The four roles:
- Eden: new objects are allocated here.
- Survivor: objects that lived through a young GC.
- Old: objects promoted after surviving enough young GCs.
- Humongous: objects at least half a region in size, stored in contiguous regions that count as old.

A region''s role is not fixed. Once it is evacuated it goes back to the free list and can be reused as any type, which is how G1 resizes the young generation between collections to meet its pause-time goal.', '// See the region layout and each collection in the GC log
// java -XX:+UseG1GC -Xlog:gc*,gc+heap=debug -Xmx512m App
// The heap summary prints lines such as:
//   garbage-first heap  total 524288K, used 20480K
//   region size 1024K, 18 young (18432K), 2 survivors (2048K)', 'MEDIUM', 'PUBLISHED', '["gc", "g1", "heap"]', '["G1 garbage collector", "Eden space", "Survivor space", "Old generation", "Humongous region"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-eden', 's-java-heap', 'Eden space', 'eden space
eden', '["Eden"]', 'The part of the young generation where almost every new object is allocated. When it fills up, a young GC runs.', 'Each thread allocates from its own TLAB (thread-local allocation buffer) carved out of Eden, so most allocations are just a pointer bump with no locking.

Most objects die young, so by the time Eden is full the bulk of it is garbage. A young GC copies the few live objects out to Survivor space and then reclaims all Eden regions at once. In G1 the number of Eden regions changes from one collection to the next.', 'for (int i = 0; i < 1_000_000; i++) {
    // Each String is allocated in Eden and becomes garbage right away,
    // so it is reclaimed cheaply by the next young GC.
    String s = "item-" + i;
}', 'EASY', 'PUBLISHED', '["gc", "heap", "young generation"]', '["Survivor space", "Young GC", "TLAB"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-survivor', 's-java-heap', 'Survivor space', 'survivor space
survivor
s0/s1', '["Survivor", "S0/S1"]', 'The part of the young generation that holds objects still alive after a young GC, while they age toward promotion.', 'Every young GC copies live objects from Eden and from the current survivor space into an empty survivor area, and adds one to each object''s age. Classic collectors use two fixed survivor spaces (S0 and S1, also called from and to); G1 uses survivor regions instead.

Objects that reach the tenuring threshold (-XX:MaxTenuringThreshold, at most 15) are promoted to the old generation. If survivor space runs out, objects can be promoted early, which fills the old generation faster.', '// Print the age distribution of surviving objects at each young GC
// java -XX:+UseG1GC -Xlog:gc+age=trace App
// Age table with threshold 15 (max threshold 15)
// - age   1:    1048576 bytes,    1048576 total', 'MEDIUM', 'PUBLISHED', '["gc", "heap", "young generation"]', '["Eden space", "Old generation", "Tenuring threshold"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-old-gen', 's-java-heap', 'Old generation', 'old generation
tenured generation
old gen', '["Tenured generation", "Old gen"]', 'The part of the heap that holds long-lived objects promoted from the young generation.', 'Old objects are collected less often and more expensively than young ones. In G1, old regions are reclaimed in mixed collections: after a concurrent marking cycle finds which old regions are mostly garbage, G1 adds those regions to the next few young GCs. That is the "garbage first" idea behind the name.

If the old generation fills faster than G1 can clean it, the JVM falls back to a full GC, which pauses the application for much longer.', '// Caches and other long-lived structures end up in the old generation
private static final Map<String, Product> CATALOG = new HashMap<>();

static void load(List<Product> products) {
    products.forEach(p -> CATALOG.put(p.sku(), p)); // survive many GCs, then promoted
}', 'MEDIUM', 'PUBLISHED', '["gc", "heap", "old generation"]', '["Survivor space", "Mixed GC", "Full GC"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-humongous', 's-java-heap', 'Humongous region', 'humongous region
humongous object', '["Humongous object"]', 'In G1, an object at least half a region in size skips Eden and is placed directly in one or more contiguous old regions marked Humongous.', 'Copying a very large object between regions would be expensive, so G1 never moves it during a young GC. A humongous object takes whole regions; the unused tail of its last region is wasted.

Frequent humongous allocations fragment the heap and can trigger early concurrent cycles or full GCs. The usual fix is a larger region size (-XX:G1HeapRegionSize) so the objects no longer count as humongous, or smaller buffers.', '// With a 1 MB region size, this 2 MB array is a humongous object
byte[] buffer = new byte[2 * 1024 * 1024];

// Inspect humongous allocations
// java -XX:+UseG1GC -XX:G1HeapRegionSize=1m -Xlog:gc+humongous=debug App', 'HARD', 'PUBLISHED', '["gc", "g1", "heap"]', '["G1 heap region types", "Old generation"]', current_timestamp, current_timestamp);
insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-young-gc', 's-java-gc', 'Young GC', 'young gc
minor gc
young-only collection', '["Minor GC", "Young-only collection"]', 'The collection that runs first and most often: when Eden fills up, the JVM stops the application, copies live young objects out, and frees all of Eden.', 'In G1 the young GC is a stop-the-world evacuation. It finds live objects in Eden and Survivor regions starting from GC roots and the remembered sets that track old-to-young pointers, copies them into fresh Survivor regions (or to Old once they are old enough), and returns the emptied regions to the free list.

It is cheap because its cost depends on how many objects are alive, not on how much garbage there is, and most young objects are already dead. G1 sizes the young generation so these pauses stay within -XX:MaxGCPauseMillis (default 200 ms).', '// Watch young GCs happen
// java -XX:+UseG1GC -Xlog:gc -Xmx256m App
// [0.412s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 24M->3M(256M) 4.120ms', 'MEDIUM', 'PUBLISHED', '["gc", "g1", "young generation"]', '["Eden space", "Survivor space", "Mixed GC", "G1 garbage collector"]', current_timestamp, current_timestamp);
-- Point the existing G1 key at the new region keys.
update catalog_key set related_terms = '["ZGC", "G1 heap region types", "Young GC", "Humongous region"]' where id = 'k-g1';
