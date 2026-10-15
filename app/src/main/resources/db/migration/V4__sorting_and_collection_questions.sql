-- Sorting (Comparator, Comparable, selection sort) and collection interview questions.
-- New: Java › Algorithms › Sorting, Java › Collections › Sorting & ordering, Java › Collections › Interview questions.
-- Question keys use the question as the term and the short answer as the definition.
insert into category (id, platform_id, name, position) values ('c-java-algo', 'p-java', 'Algorithms', 3);
insert into subcategory (id, category_id, name, position) values ('s-java-sort', 'c-java-algo', 'Sorting', 0);
insert into subcategory (id, category_id, name, position) values ('s-java-order', 'c-java-coll', 'Sorting & ordering', 2);
insert into subcategory (id, category_id, name, position) values ('s-java-coll-qa', 'c-java-coll', 'Interview questions', 3);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-selection-sort', 's-java-sort', 'Selection sort', 'selection sort', '[]', 'A simple in-place sort: on each pass, find the smallest remaining element and swap it into the next position. Always O(n²) comparisons, at most n - 1 swaps.', 'Pass i scans positions i..n-1 for the minimum and swaps it into position i, so after pass i the first i + 1 elements are sorted and final.

- Time: O(n²) in every case, even on sorted input, because each pass scans the whole unsorted part.
- Space: O(1), sorts in place.
- Not stable: the long-distance swap can move an element past an equal one.
- Few writes (at most n - 1 swaps), which is its one advantage over insertion sort.

Passing a Comparator makes it work for any type and any order: Comparator.naturalOrder() for ascending, Comparator.reverseOrder() for descending. For Strings, natural order is case-sensitive (uppercase letters sort before lowercase); use String.CASE_INSENSITIVE_ORDER to ignore case.', 'package dsa;

import java.util.Arrays;
import java.util.Comparator;

public class SelectionSort {
    public static void main(String[] args) {
        String[] girls = {"Lalitha", "Raagini", "Sridevi", "Sarita", "Mum", "Irishita", "Ahna"};
        selectionSort(girls, Comparator.naturalOrder());
        System.out.println(Arrays.toString(girls));
        // [Ahna, Irishita, Lalitha, Mum, Raagini, Sarita, Sridevi]
    }

    static <T> void selectionSort(T[] a, Comparator<? super T> cmp) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (cmp.compare(a[j], a[min]) < 0) {
                    min = j;
                }
            }
            T tmp = a[min];
            a[min] = a[i];
            a[i] = tmp;
        }
    }
}', 'EASY', 'PUBLISHED', '["sorting", "dsa"]', '["Comparator", "Insertion sort", "Arrays.sort"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-comparator', 's-java-order', 'Comparator', 'comparator
java.util.comparator
comparator.comparing', '["java.util.Comparator", "Comparator.comparing"]', 'A functional interface that defines an ordering outside the class being sorted: compare(a, b) returns negative, zero or positive when a sorts before, equal to or after b.', 'Use it when the class has no natural order, or you need a different one (by length, by a field, descending).

Factory and combinator methods:
- Comparator.comparing(Person::name): orders by a key that a function extracts. It takes a function, not the data.
- thenComparing(...): tie-breaker.
- reversed(), Comparator.reverseOrder(): descending.
- Comparator.naturalOrder(): uses the elements'' compareTo.
- nullsFirst / nullsLast: handle null elements.

A comparator should be consistent with equals when used in a TreeSet or TreeMap, because those treat compare(a, b) == 0 as a duplicate.', 'record Student(String name, int marks) {}

List<Student> students = new ArrayList<>(List.of(
    new Student("Sarita", 82), new Student("Ahna", 91), new Student("Mum", 82)));

// Highest marks first, then by name
students.sort(Comparator.comparingInt(Student::marks).reversed()
                        .thenComparing(Student::name));
// [Ahna 91, Mum 82, Sarita 82]', 'MEDIUM', 'PUBLISHED', '["sorting", "functional interfaces"]', '["Comparable", "Selection sort", "TreeMap"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-comparable', 's-java-order', 'Comparable', 'comparable
compareto', '["compareTo"]', 'An interface a class implements to give itself one natural order, through compareTo(other). String, Integer and LocalDate implement it.', 'Arrays.sort, Collections.sort, TreeSet and TreeMap use compareTo when you pass no Comparator. A class can have only one natural order; anything else needs a Comparator.

Comparable vs Comparator:
- Comparable lives inside the class (java.lang, compareTo(T o), one argument). Comparator lives outside it (java.util, compare(T a, T b), two arguments).
- Comparable gives one default order; you can write as many Comparators as you like.
- You cannot add Comparable to a class you don''t own; a Comparator works for any class.

Avoid return this.x - other.x for ints: it overflows for large values. Use Integer.compare.', 'record Version(int major, int minor) implements Comparable<Version> {
    @Override
    public int compareTo(Version o) {
        int c = Integer.compare(major, o.major);
        return c != 0 ? c : Integer.compare(minor, o.minor);
    }
}

List<Version> vs = new ArrayList<>(List.of(new Version(2, 1), new Version(1, 9), new Version(2, 0)));
Collections.sort(vs); // [1.9, 2.0, 2.1]', 'EASY', 'PUBLISHED', '["sorting"]', '["Comparator", "TreeMap"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-comparing-error', 's-java-coll-qa', 'Why does Comparator.comparing(array).compare() not compile, and how do you sort a String array in ascending order?', 'why does comparator.comparing(array).compare() not compile, and how do you sort a string array in ascending order?
sort string array ascending
sort array descending', '["Sort String array ascending", "Sort array descending"]', 'comparing() takes a key-extractor function, not the array, and compare() needs two arguments and only returns an int. To sort, call Arrays.sort(arr) or Arrays.sort(arr, comparator).', 'Two mistakes in Comparator.comparing(girls).compare():
1. Comparator.comparing expects a Function that extracts a sort key from one element, such as String::length. Passing the array does not compile.
2. compare(a, b) compares exactly two elements and returns an int. It never rearranges anything, so even with arguments it would not sort.

A Comparator only describes an order; a sort method applies it:
- Ascending: Arrays.sort(girls), or Arrays.sort(girls, Comparator.naturalOrder())
- Descending: Arrays.sort(girls, Comparator.reverseOrder())
- Ignoring case: Arrays.sort(girls, String.CASE_INSENSITIVE_ORDER)
- By length, then alphabetically: Arrays.sort(girls, Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()))
- A List: list.sort(comparator) or Collections.sort(list)

To write the sort yourself, see the Selection sort key.', 'String[] girls = {"Lalitha", "Raagini", "Sridevi", "Sarita", "Mum", "Irishita", "Ahna"};

Arrays.sort(girls);
System.out.println(Arrays.toString(girls));
// [Ahna, Irishita, Lalitha, Mum, Raagini, Sarita, Sridevi]

Arrays.sort(girls, Comparator.reverseOrder());
// [Sridevi, Sarita, Raagini, Mum, Lalitha, Irishita, Ahna]

Arrays.sort(girls, Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
// [Mum, Ahna, Sarita, Lalitha, Raagini, Sridevi, Irishita]', 'EASY', 'PUBLISHED', '["sorting", "interview question"]', '["Comparator", "Selection sort", "Arrays.sort"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-sort-algos', 's-java-coll-qa', 'Which algorithms do Arrays.sort and Collections.sort use, and are they stable?', 'which algorithms do arrays.sort and collections.sort use, and are they stable?
timsort
dual-pivot quicksort', '["TimSort", "Dual-pivot quicksort"]', 'Primitive arrays use dual-pivot quicksort, which is not stable. Object arrays and lists use TimSort, which is stable and O(n log n) worst case.', 'Arrays.sort(int[]) and the other primitive overloads use dual-pivot quicksort: very fast in practice, O(n log n) on average. Stability does not matter for primitives because equal values are indistinguishable.

Arrays.sort(Object[]), Collections.sort and List.sort use TimSort, a merge sort and insertion sort hybrid. It is stable, so elements that compare equal keep their original order, which lets you sort by one field and then by another. It runs in O(n) on input that is already sorted, O(n log n) in the worst case, and needs up to n/2 extra space.

Arrays.parallelSort splits large arrays across the common ForkJoinPool.', 'record Order(String customer, int amount) {}

List<Order> orders = new ArrayList<>(List.of(
    new Order("Ahna", 30), new Order("Mum", 10), new Order("Ahna", 10)));

orders.sort(Comparator.comparing(Order::customer));
// Stable: the two Ahna orders keep their relative order (30 before 10)
// [Ahna 30, Ahna 10, Mum 10]', 'MEDIUM', 'PUBLISHED', '["sorting", "interview question"]', '["Comparator", "Selection sort"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-hashmap-hashtable', 's-java-coll-qa', 'What is the difference between HashMap, Hashtable and ConcurrentHashMap?', 'what is the difference between hashmap, hashtable and concurrenthashmap?
hashmap vs hashtable', '["HashMap vs Hashtable"]', 'HashMap is not thread-safe and allows one null key. Hashtable is a legacy class that locks the whole table on every call. ConcurrentHashMap is thread-safe with fine-grained locking. Neither of the last two allows nulls.', '- HashMap: no synchronization, fastest for single-threaded use. Allows one null key and null values. Iterators are fail-fast.
- Hashtable: from Java 1.0. Every method is synchronized, so only one thread can use it at a time. No null keys or values. Kept only for old code.
- ConcurrentHashMap: reads take no lock, writes lock a single bin, so many threads work in parallel. No nulls, because get() returning null must mean the key is absent. Iterators are weakly consistent and never throw ConcurrentModificationException. Adds atomic operations such as compute, merge and putIfAbsent.
- Collections.synchronizedMap(map) wraps any map with one lock, like Hashtable, and you must still synchronize on it while iterating.

In new code, use HashMap for one thread and ConcurrentHashMap for many.', 'Map<String, Integer> visits = new ConcurrentHashMap<>();

// Safe from many threads: merge is atomic
visits.merge("/home", 1, Integer::sum);

// Check-then-act on a plain HashMap is a race, even if each call is synchronized:
// if (!map.containsKey(k)) map.put(k, v);   // use putIfAbsent instead', 'MEDIUM', 'PUBLISHED', '["concurrency", "interview question"]', '["HashMap", "ConcurrentHashMap"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-equals-hashcode', 's-java-coll-qa', 'What is the equals and hashCode contract, and what breaks if you override only equals?', 'what is the equals and hashcode contract, and what breaks if you override only equals?
equals/hashcode contract', '["equals/hashCode contract"]', 'Objects that are equal must have the same hashCode. If you override equals but not hashCode, HashMap and HashSet look in the wrong bucket and fail to find equal objects.', 'The contract:
- If a.equals(b), then a.hashCode() == b.hashCode().
- Equal hash codes do not imply equal objects (collisions are allowed).
- Both must stay the same while the object is in a hash collection, so use immutable fields.

HashMap first uses hashCode to choose a bucket, then equals to find the entry in it. With the default identity hashCode, two equal objects almost always land in different buckets, so contains, get and remove miss, and a HashSet can hold duplicates.

Records generate equals and hashCode from all components. Otherwise use Objects.hash or let the IDE generate both together.', 'class Point {
    final int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) {
        return o instanceof Point p && p.x == x && p.y == y;
    }
    // missing hashCode
}

Set<Point> set = new HashSet<>();
set.add(new Point(1, 2));
System.out.println(set.contains(new Point(1, 2))); // usually false

// Fix: add  @Override public int hashCode() { return Objects.hash(x, y); }
// or use    record Point(int x, int y) {}', 'MEDIUM', 'PUBLISHED', '["hashing", "interview question"]', '["HashMap", "HashSet"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-hashset', 's-java-coll-qa', 'How does HashSet make sure there are no duplicates?', 'how does hashset make sure there are no duplicates?
hashset internals', '["HashSet internals"]', 'A HashSet is a HashMap underneath: each element is stored as a key with a shared dummy value. add() returns false when an equal key (same hashCode and equals) is already there.', 'HashSet.add(e) calls map.put(e, PRESENT) on its internal HashMap. The map finds the bucket from e.hashCode() and compares with equals; if an equal key exists, put returns the old value and add returns false.

So uniqueness depends entirely on equals and hashCode. Related sets:
- LinkedHashSet: also keeps insertion order.
- TreeSet: sorted, and uses compareTo or a Comparator instead of equals to detect duplicates.

Operations are O(1) on average. One null element is allowed.', 'Set<String> names = new HashSet<>();
System.out.println(names.add("Ahna")); // true
System.out.println(names.add("Ahna")); // false, already present

// Remove duplicates from a list but keep the first-seen order
List<String> unique = new ArrayList<>(new LinkedHashSet<>(List.of("Mum", "Ahna", "Mum")));
// [Mum, Ahna]', 'EASY', 'PUBLISHED', '["hashing", "interview question"]', '["HashMap", "equals/hashCode contract", "TreeSet"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-arraylist-linkedlist', 's-java-coll-qa', 'When would you use ArrayList instead of LinkedList?', 'when would you use arraylist instead of linkedlist?
arraylist vs linkedlist', '["ArrayList vs LinkedList"]', 'Almost always. ArrayList gives O(1) index access and is cache-friendly. LinkedList only wins when you add or remove at both ends, and ArrayDeque usually does that better.', '- ArrayList: backed by an array. get(i) is O(1). Appending is amortized O(1). Inserting or removing in the middle shifts elements, O(n), but that shift is a fast memory copy.
- LinkedList: a doubly linked list. get(i) is O(n) because it walks the nodes. Insert or remove is O(1) only once you already hold the position (through a ListIterator). Each element costs an extra node object with two pointers, and the nodes are scattered in memory, so iteration is slower.

Use ArrayList by default. For a queue or stack, use ArrayDeque.', 'List<Integer> list = new ArrayList<>();
for (int i = 0; i < 1_000; i++) list.add(i);
int x = list.get(500);        // O(1)

Deque<Integer> queue = new ArrayDeque<>();
queue.offerLast(1);           // enqueue
queue.pollFirst();            // dequeue, both O(1)', 'EASY', 'PUBLISHED', '["lists", "interview question"]', '["ArrayList", "ArrayDeque"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-fail-fast', 's-java-coll-qa', 'What is a fail-fast iterator, and how do you remove elements while iterating?', 'what is a fail-fast iterator, and how do you remove elements while iterating?
fail-fast vs fail-safe
concurrentmodificationexception', '["Fail-fast vs fail-safe", "ConcurrentModificationException"]', 'A fail-fast iterator throws ConcurrentModificationException if the collection is changed structurally other than through the iterator. Remove with Iterator.remove() or removeIf().', 'ArrayList, HashMap and most java.util collections keep a modCount. The iterator records it and checks on every next(); a mismatch means someone else changed the collection, so it throws instead of returning wrong data. The check is best-effort, not a thread-safety guarantee.

Removing inside a for-each loop is the classic way to trigger it. Safe options:
- iterator.remove()
- collection.removeIf(predicate)
- Build a new collection with streams.

Fail-safe (weakly consistent) iterators, as in ConcurrentHashMap and CopyOnWriteArrayList, work on the live structure or a snapshot and never throw, but may not show recent changes.', 'List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4));

// for (Integer n : nums) if (n % 2 == 0) nums.remove(n);  // ConcurrentModificationException

Iterator<Integer> it = nums.iterator();
while (it.hasNext()) {
    if (it.next() % 2 == 0) it.remove();
}
// or simply: nums.removeIf(n -> n % 2 == 0);
System.out.println(nums); // [1, 3]', 'MEDIUM', 'PUBLISHED', '["iteration", "interview question"]', '["ConcurrentHashMap", "CopyOnWriteArrayList"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-map-order', 's-java-coll-qa', 'How do HashMap, LinkedHashMap and TreeMap differ in ordering, and how would you build an LRU cache?', 'how do hashmap, linkedhashmap and treemap differ in ordering, and how would you build an lru cache?
lru cache
linkedhashmap', '["LRU cache", "LinkedHashMap"]', 'HashMap has no order, LinkedHashMap keeps insertion (or access) order, TreeMap keeps keys sorted. An LRU cache is a LinkedHashMap in access order that removes its eldest entry when full.', '- HashMap: iteration order is unspecified and can change after a resize. O(1) average.
- LinkedHashMap: a HashMap plus a doubly linked list through the entries. Iterates in insertion order, or in access order if you pass accessOrder = true. O(1).
- TreeMap: a red-black tree. Keys are sorted by Comparable or a Comparator, with range methods such as firstKey, floorKey and headMap. O(log n).

LRU cache: in access order, every get moves the entry to the end, so the eldest entry is the least recently used. Override removeEldestEntry to evict it once size exceeds capacity. Not thread-safe; wrap it or use a library such as Caffeine for concurrent use.', 'class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(16, 0.75f, true); // true = access order
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

LruCache<String, Integer> cache = new LruCache<>(2);
cache.put("a", 1); cache.put("b", 2);
cache.get("a");        // a is now most recent
cache.put("c", 3);     // evicts b
System.out.println(cache.keySet()); // [a, c]', 'MEDIUM', 'PUBLISHED', '["maps", "interview question"]', '["HashMap", "TreeMap"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-immutable-lists', 's-java-coll-qa', 'What is the difference between List.of, Arrays.asList and Collections.unmodifiableList?', 'what is the difference between list.of, arrays.aslist and collections.unmodifiablelist?
list.of vs arrays.aslist', '["List.of vs Arrays.asList"]', 'List.of is truly immutable and rejects nulls. Arrays.asList is a fixed-size view of an array: set works, add and remove throw. unmodifiableList is a read-only view that still shows changes to the list underneath.', '- List.of(...) (Java 9+): immutable. Any change throws UnsupportedOperationException. Null elements throw NullPointerException, and so does contains(null).
- Arrays.asList(...): wraps the array. set() writes through to the array, but add and remove throw because the size is fixed. Allows nulls.
- Collections.unmodifiableList(list): a read-only wrapper. Changes made to the original list are visible through it, so it is not a snapshot.
- List.copyOf(list) (Java 10+): an immutable snapshot.

Return List.of or List.copyOf from APIs so callers cannot change your internal state.', 'List<String> a = Arrays.asList("x", "y");
a.set(0, "z");         // OK, writes through to the array
// a.add("w");         // UnsupportedOperationException: fixed size

List<String> b = List.of("x", "y");
// b.set(0, "z");      // UnsupportedOperationException: immutable

List<String> source = new ArrayList<>(List.of("x"));
List<String> view = Collections.unmodifiableList(source);
source.add("y");
System.out.println(view); // [x, y], the view sees the change', 'EASY', 'PUBLISHED', '["immutability", "interview question"]', '["ArrayList"]', current_timestamp, current_timestamp);
