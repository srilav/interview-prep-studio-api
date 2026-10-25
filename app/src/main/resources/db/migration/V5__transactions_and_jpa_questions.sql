-- Database transaction, isolation and Spring Data JPA / Hibernate interview questions.
-- New: Databases › Transactions & concurrency › Interview questions, Databases › ORM › Interview questions,
-- Spring Boot › Data access › JPA & Hibernate questions.
-- Question keys use the question as the term and the short answer as the definition.
insert into platform (id, name, kind) values ('p-db', 'Databases', 'PLATFORM');
insert into category (id, platform_id, name, position) values ('c-db-tx', 'p-db', 'Transactions & concurrency', 0);
insert into subcategory (id, category_id, name, position) values ('s-db-tx-qa', 'c-db-tx', 'Interview questions', 0);
insert into category (id, platform_id, name, position) values ('c-db-orm', 'p-db', 'ORM', 1);
insert into subcategory (id, category_id, name, position) values ('s-db-orm-qa', 'c-db-orm', 'Interview questions', 0);
insert into subcategory (id, category_id, name, position) values ('s-spring-jpa-qa', 'c-spring-data', 'JPA & Hibernate questions', 1);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-lost-update', 's-db-tx-qa', 'An update is not atomic and the commit happened on the latest value: what is a lost update, and how do you prevent it?', 'an update is not atomic and the commit happened on the latest value: what is a lost update, and how do you prevent it?
lost update
read-modify-write race', '["Lost update", "Read-modify-write race"]', 'Two transactions read the same row, each computes a new value in application code, and the later commit overwrites the earlier one. Fix it with an atomic UPDATE, a row lock (FOR UPDATE), a version check, or a stricter isolation level.', 'A lost update happens with read-modify-write split across statements:
1. T1 and T2 both SELECT balance = 1000.
2. T1 writes 800 (1000 - 200) and commits.
3. T2 writes 900 (1000 - 100) and commits. T1''s withdrawal is gone; the right answer is 700.

Each UPDATE is atomic on its own. What is not atomic is the read plus the write, because the read happened in an earlier statement.

"The commit happened on the latest value": under READ COMMITTED (the default in PostgreSQL, Oracle and SQL Server), an UPDATE that finds the row locked waits for the other transaction, then re-reads the latest committed version and applies itself to that. So UPDATE ... SET balance = balance - 100 is safe, because the arithmetic happens inside the statement on the latest value. UPDATE ... SET balance = 900 is not, because 900 was computed from a stale read.

Ways to prevent it:
- Atomic statement: SET balance = balance - :amount WHERE id = :id AND balance >= :amount. Check the row count.
- Pessimistic lock: SELECT ... FOR UPDATE, so the second reader waits and then sees 800.
- Optimistic lock: a version column; UPDATE ... WHERE id = :id AND version = :v. 0 rows updated means someone else won, so retry.
- Isolation: PostgreSQL REPEATABLE READ and SERIALIZABLE abort the second writer ("could not serialize access due to concurrent update"). MySQL InnoDB REPEATABLE READ does not: its UPDATE reads the latest committed row, so the stale value from the earlier SELECT still wins.', '// Lost update: the new value is computed from a stale read
int balance = jdbc.queryForObject("select balance from wallet where id = ?", Integer.class, id);
jdbc.update("update wallet set balance = ? where id = ?", balance - amount, id);

// Atomic: the database computes it from the latest committed value
int rows = jdbc.update(
    "update wallet set balance = balance - ? where id = ? and balance >= ?", amount, id, amount);
if (rows == 0) {
    throw new InsufficientFundsException(id);
}

// Optimistic: fails (0 rows) if someone changed the row since we read it
int updated = jdbc.update(
    "update wallet set balance = ?, version = version + 1 where id = ? and version = ?",
    balance - amount, id, version);', 'MEDIUM', 'PUBLISHED', '["transactions", "concurrency", "interview question"]', '["Transaction isolation levels", "Optimistic locking", "SELECT FOR UPDATE"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-isolation-levels', 's-db-tx-qa', 'What are the transaction isolation levels, and which anomalies does each allow?', 'what are the transaction isolation levels, and which anomalies does each allow?
transaction isolation levels
dirty read
non-repeatable read
phantom read', '["Transaction isolation levels", "Dirty read", "Non-repeatable read", "Phantom read"]', 'READ UNCOMMITTED allows dirty reads; READ COMMITTED prevents them; REPEATABLE READ also prevents non-repeatable reads; SERIALIZABLE prevents all anomalies. Real databases differ from the ANSI table, so check your engine.', 'The SQL standard defines levels by the anomalies they forbid, from weakest to strongest:
- READ UNCOMMITTED: dirty reads, non-repeatable reads and phantoms are all possible.
- READ COMMITTED: no dirty reads; non-repeatable reads and phantoms are possible.
- REPEATABLE READ: no dirty or non-repeatable reads; phantoms are possible.
- SERIALIZABLE: none of them.

The anomalies:
- Dirty read: you see another transaction''s uncommitted change, which may later be rolled back.
- Non-repeatable read: you read a row twice and get different values because someone committed in between.
- Phantom: you run the same range query twice and get a different set of rows.
- Lost update and write skew are not in the ANSI table, but they matter just as much.

How engines actually behave:
- PostgreSQL: default READ COMMITTED. READ UNCOMMITTED behaves as READ COMMITTED. REPEATABLE READ is snapshot isolation: it prevents phantoms too, but allows write skew. SERIALIZABLE uses SSI.
- MySQL InnoDB: default REPEATABLE READ. Plain SELECTs read a snapshot, while UPDATE and locking reads see the latest data and take next-key locks.
- Oracle: default READ COMMITTED; its SERIALIZABLE is really snapshot isolation.
- SQL Server: default READ COMMITTED with locks; READ_COMMITTED_SNAPSHOT and SNAPSHOT add versioned reads.

In Spring, set it with @Transactional(isolation = Isolation.REPEATABLE_READ).', '@Transactional(isolation = Isolation.REPEATABLE_READ)
public Report monthlyReport(YearMonth month) {
    // Both queries see the same snapshot, so the totals are consistent
    BigDecimal credits = ledger.sumCredits(month);
    BigDecimal debits = ledger.sumDebits(month);
    return new Report(month, credits, debits);
}

// Plain JDBC
connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);', 'MEDIUM', 'PUBLISHED', '["transactions", "isolation", "interview question"]', '["Serializable isolation", "Lost update", "Transaction propagation"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-serializable', 's-db-tx-qa', 'How does a database implement SERIALIZABLE isolation?', 'how does a database implement serializable isolation?
serializable isolation
ssi
serializable snapshot isolation
two-phase locking
write skew', '["Serializable isolation", "SSI", "Serializable snapshot isolation", "Two-phase locking", "Write skew"]', 'Either by blocking (two-phase locking with range locks, as in SQL Server and MySQL) or by detecting conflicts (serializable snapshot isolation, as in PostgreSQL and CockroachDB), which aborts one transaction with SQLSTATE 40001 that the application must retry.', 'SERIALIZABLE promises that the result equals some one-at-a-time order of the committed transactions. Running them truly one at a time would be too slow, so engines use one of two approaches.

1. Two-phase locking (pessimistic): MySQL InnoDB, SQL Server.
- Reads take shared locks and writes take exclusive locks, and all are held until commit.
- Range, gap or next-key locks on indexes stop other transactions inserting rows that would match your WHERE clause, which prevents phantoms. In InnoDB, SERIALIZABLE turns plain SELECTs into locking reads.
- Cost: transactions wait for each other, and deadlocks appear; the database kills one victim, which you retry.

2. Serializable snapshot isolation (optimistic): PostgreSQL 9.1+, CockroachDB.
- Each transaction reads a snapshot, and readers never block writers.
- The database records what each transaction read (SIREAD predicate locks) and looks for read-write dependencies between concurrent transactions. When it finds a dangerous structure (two consecutive read-write conflicts), it aborts one of them.
- The error is SQLSTATE 40001 (serialization_failure). The whole transaction must be retried, so keep it short and free of side effects such as sending email.

Write skew shows why snapshot isolation alone is not enough. Two doctors are on call, and the rule is that at least one must stay. Each transaction checks that the other is still on call and removes itself. Under snapshot isolation both commit and nobody is on call. Under SERIALIZABLE, one of them fails.

Oracle''s SERIALIZABLE is snapshot isolation and allows write skew; use SELECT ... FOR UPDATE there.', '@Retryable(retryFor = CannotAcquireLockException.class, maxAttempts = 3, backoff = @Backoff(50))
@Transactional(isolation = Isolation.SERIALIZABLE)
public void goOffCall(long doctorId, long shiftId) {
    // PostgreSQL SSI aborts one of two concurrent calls with SQLSTATE 40001,
    // which Spring translates to CannotAcquireLockException
    if (doctors.countOnCall(shiftId) < 2) {
        throw new IllegalStateException("at least one doctor must stay on call");
    }
    doctors.setOnCall(doctorId, shiftId, false);
}
// @Retryable must be applied outside the transaction (its interceptor runs first by default),
// so each attempt starts a fresh transaction.', 'HARD', 'PUBLISHED', '["transactions", "isolation", "interview question"]', '["Transaction isolation levels", "Lost update", "Optimistic locking"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-orm', 's-db-orm-qa', 'What is an ORM framework, and what are its trade-offs?', 'what is an orm framework, and what are its trade-offs?
orm
object-relational mapping', '["ORM", "Object-relational mapping"]', 'An ORM maps classes to tables, fields to columns and objects to rows, and generates the SQL for you. It speeds up CRUD work but can hide expensive queries such as N+1 and makes complex SQL harder to express.', 'What an ORM does:
- Mapping: an @Entity class is a table, a field is a column, an instance is a row, and associations become foreign keys or join tables.
- Query generation: turns method calls or an object query language (JPQL, HQL) into SQL.
- Hydration: turns result rows into objects, and tracks identity so one row is one object within a session.
- Change tracking: detects modified objects and writes them back (dirty checking).
- Transactions, caching and lazy loading.

Examples: Hibernate and EclipseLink (JPA providers) in Java, SQLAlchemy and Django ORM in Python, Entity Framework in .NET, Prisma and TypeORM in TypeScript.

Pros: less boilerplate, the database is mostly swappable, and bind parameters are used by default.

Cons:
- Hidden queries: lazy loading causes N+1.
- Complex reporting queries are awkward; use native SQL or a query builder such as jOOQ.
- Object-relational impedance mismatch: inheritance, identity and collections do not map cleanly onto tables.
- It does not make you immune to SQL injection: concatenating user input into a JPQL or native query string is still injectable.', '@Entity
@Table(name = "employee")
class Employee {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Department department;
}

// The ORM writes the SQL and maps the rows back to objects
List<Employee> team = em.createQuery(
        "select e from Employee e where e.department.name = :name", Employee.class)
    .setParameter("name", "IT")   // bound parameter, not string concatenation
    .getResultList();', 'EASY', 'PUBLISHED', '["orm", "interview question"]', '["JPA", "Hibernate", "N+1 query problem"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-n-plus-one', 's-db-orm-qa', 'What is the N+1 query problem? Give an example.', 'what is the n+1 query problem? give an example.
n+1 query problem
n+1 selects', '["N+1 query problem", "N+1 selects"]', 'One query loads N parent rows, then the code touches a lazy association on each one, firing one extra query per parent: N+1 round trips instead of one or two. Fix it by fetching the association up front with a join or a batch.', 'Example: list 100 departments with their employee counts.
- Query 1: select * from department, which returns 100 rows.
- The loop calls dept.getEmployees().size(). The collection is lazy, so each call runs select * from employee where department_id = ?.
- Total: 101 queries. Each is fast, but 101 network round trips are not, and the cost grows with the data.

It is easy to miss because the code looks like a plain loop over objects. It shows up in SQL logs or a query counter.

Fixes:
- Join fetch: load parents and children in one query.
- Batch fetch: load children for many parents at once with where department_id in (...), giving 1 + N/batch queries.
- Projection: if you only need counts or a few columns, select exactly that in one query (group by).

Making the association EAGER is not the fix: it loads the data even when nobody needs it, and with JPQL queries it can still be fetched one query per row.', '// N+1: 1 query for departments, then 1 per department
for (Department d : em.createQuery("select d from Department d", Department.class).getResultList()) {
    System.out.println(d.getName() + ": " + d.getEmployees().size());
}

// 1 query: fetch the collection with the parents
List<Department> ds = em.createQuery(
    "select distinct d from Department d left join fetch d.employees", Department.class)
    .getResultList();

// 1 query: only the numbers
List<Object[]> counts = em.createQuery(
    "select d.name, count(e) from Department d left join d.employees e group by d.name", Object[].class)
    .getResultList();', 'MEDIUM', 'PUBLISHED', '["orm", "performance", "interview question"]', '["ORM", "JOIN FETCH", "Lazy loading"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-hibernate-n-plus-one', 's-spring-jpa-qa', 'How does N+1 happen in Hibernate, and how do you fix it with JOIN FETCH, @EntityGraph and @BatchSize?', 'how does n+1 happen in hibernate, and how do you fix it with join fetch, @entitygraph and @batchsize?
join fetch
@entitygraph
@batchsize
multiplebagfetchexception', '["JOIN FETCH", "@EntityGraph", "@BatchSize", "MultipleBagFetchException"]', 'Collections (@OneToMany, @ManyToMany) are lazy by default, so touching them per entity fires a query each; to-one associations are eager and can do the same. Fix per query with JOIN FETCH or @EntityGraph, or globally reduce it with @BatchSize.', 'JPA defaults: @OneToMany and @ManyToMany are LAZY; @ManyToOne and @OneToOne are EAGER. A JPQL query does not honour EAGER with a join, so an eager to-one association is loaded by a separate select per row: also N+1. Prefer LAZY everywhere and choose what to fetch per query.

Fixes:
- JOIN FETCH in JPQL: one SQL join. Hibernate 6 removes duplicate parents automatically; in Hibernate 5 add DISTINCT.
- @EntityGraph(attributePaths = "employees") on a Spring Data repository method: the same join without writing JPQL.
- @BatchSize(size = 50) on the collection, or spring.jpa.properties.hibernate.default_batch_fetch_size=50: lazy loads are grouped into where department_id in (...).

Pitfalls:
- Join fetching two List collections at once throws MultipleBagFetchException (and produces a cartesian product). Fetch one per query, use Set, or use batch fetching.
- Join fetch of a collection combined with pagination makes Hibernate paginate in memory (warning HHH90003004). Page the parent ids first, then fetch.
- Detect it in tests by counting statements, for example with Hibernate statistics.', 'public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("select d from Department d left join fetch d.employees where d.name = :name")
    List<Department> findWithEmployees(@Param("name") String name);

    @EntityGraph(attributePaths = "employees")
    List<Department> findAllBy();
}

@Entity
class Department {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "department")
    @BatchSize(size = 50)              // lazy loads come in groups of 50
    private Set<Employee> employees = new HashSet<>();
}', 'MEDIUM', 'PUBLISHED', '["hibernate", "jpa", "performance", "interview question"]', '["N+1 query problem", "Lazy loading", "Hibernate first-level cache"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-spring-data-jpa-hibernate', 's-spring-jpa-qa', 'Does Spring Data JPA use Hibernate? How do JPA, Hibernate and Spring Data JPA relate?', 'does spring data jpa use hibernate? how do jpa, hibernate and spring data jpa relate?
spring data jpa vs hibernate
jpa provider', '["Spring Data JPA vs Hibernate", "JPA provider"]', 'Yes, by default. JPA is the specification, Hibernate is the provider that implements it and generates the SQL, and Spring Data JPA is a repository layer on top of JPA. spring-boot-starter-data-jpa brings Hibernate in.', 'Three layers:
- JPA (Jakarta Persistence): a specification. It defines @Entity, EntityManager, JPQL, lock modes and the persistence-context rules, but contains no implementation.
- Hibernate ORM: a JPA provider. It implements EntityManager, generates SQL, does dirty checking, lazy loading and caching. It also works on its own without Spring, through its Session API.
- Spring Data JPA: generates repository implementations (JpaRepository, query methods such as findByEmail, @Query). Under the hood SimpleJpaRepository calls the EntityManager, which is Hibernate''s.

spring-boot-starter-data-jpa depends on hibernate-core, so Hibernate is the default. You can swap in another provider such as EclipseLink, but that is rare.

Practical consequences: Hibernate behaviour such as N+1, LazyInitializationException, dirty checking and flush timing all apply to Spring Data repositories. You can reach Hibernate-specific APIs with entityManager.unwrap(Session.class).', '// Spring Data JPA: you write only the interface
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);   // query derived from the method name
}

// What it does underneath: JPA''s EntityManager, implemented by Hibernate
User user = entityManager
    .createQuery("select u from User u where u.email = :email", User.class)
    .setParameter("email", email)
    .getSingleResult();

Session session = entityManager.unwrap(Session.class);   // Hibernate''s native API', 'EASY', 'PUBLISHED', '["jpa", "hibernate", "spring data", "interview question"]', '["ORM", "JPA", "Hibernate first-level cache"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-dirty-checking', 's-spring-jpa-qa', 'Inside a @Transactional method, why is an entity change saved without calling save()?', 'inside a @transactional method, why is an entity change saved without calling save()?
dirty checking
flush
persistence context
@query', '["Dirty checking", "Flush", "Persistence context", "@Query"]', 'Entities loaded in a transaction are managed by the persistence context. At flush, before commit or before a query that needs it, Hibernate compares each managed entity with its snapshot and issues UPDATEs for the changed ones. This is dirty checking.', 'What happens in a @Transactional service method:
1. Spring starts a transaction and binds an EntityManager (persistence context) to the thread.
2. findById runs a SELECT, puts the entity in the persistence context and keeps a snapshot of its loaded state.
3. You call a setter. Nothing is sent to the database yet.
4. On commit, Hibernate flushes: it compares managed entities with their snapshots and runs an UPDATE for each changed one, then the transaction commits.

Flush also happens before a JPQL or native query whose results could be affected (FlushModeType.AUTO), or when you call flush().

Notes:
- repository.save() on a managed entity is redundant. It matters for new entities (persist) and detached ones (merge).
- @Transactional(readOnly = true) sets Hibernate''s flush mode to MANUAL, so changes are not written and snapshots can be skipped.
- An exception that triggers rollback discards the changes.
- Custom reads: @Query with JPQL (entity names) or nativeQuery = true (table names). Entities returned by either are managed and dirty-checked too.
- Dirty checking writes whatever is in memory, which is how a lost update happens when two transactions edit the same row.', '@Service
class DepartmentService {
    private final DepartmentRepository repository;

    DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void rename(long id, String newName) {
        Department dept = repository.findById(id).orElseThrow();   // SELECT, entity is managed
        dept.setName(newName);                                       // only changes memory
        // no save(): at commit Hibernate flushes
        // update department set name = ? where id = ?
    }
}', 'MEDIUM', 'PUBLISHED', '["jpa", "hibernate", "transactions", "interview question"]', '["Hibernate first-level cache", "@Modifying", "Transaction propagation"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-modifying', 's-spring-jpa-qa', 'If @Transactional already allows updates, when do you need @Modifying?', 'if @transactional already allows updates, when do you need @modifying?
@modifying
clearautomatically
flushautomatically', '["@Modifying", "clearAutomatically", "flushAutomatically"]', '@Transactional sets the transaction boundary. @Modifying marks a repository @Query as an UPDATE, DELETE or INSERT so Spring Data runs it with executeUpdate() and returns the row count. Without it the query is run as a select and fails.', 'Two ways to change data:
- Entity changes (dirty checking): load, modify, let the flush write it. Needs only a transaction.
- A statement in @Query, such as update User u set u.status = :s where ...: needs @Modifying. Spring Data then calls Query.executeUpdate() and returns int (rows affected) or void. Without it, Spring Data calls getResultList() on a DML statement, which fails at runtime.

@Modifying queries still need a transaction. Query methods you declare in your repository interface do not get one by default (only the inherited CRUD methods do, from SimpleJpaRepository), so put @Transactional on the repository method or call it from a transactional service. Otherwise you get TransactionRequiredException.

The statement goes straight to the database and bypasses the persistence context:
- flushAutomatically = true flushes pending entity changes first, so the statement sees them.
- clearAutomatically = true clears the persistence context afterwards, so later reads in the same transaction do not return stale entities.
- @Version is not incremented, and entity lifecycle callbacks (@PreUpdate) and cascades do not run.', 'public interface UserRepository extends JpaRepository<User, Long> {

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.status = :status where u.id = :id")
    int updateStatus(@Param("id") long id, @Param("status") Status status);
}

@Transactional
public void activate(long id) {
    User before = users.findById(id).orElseThrow();   // PENDING, now in the persistence context
    users.updateStatus(id, Status.ACTIVE);            // SQL update, bypasses the entity
    User after = users.findById(id).orElseThrow();    // ACTIVE; without clearAutomatically it would still be PENDING
}', 'MEDIUM', 'PUBLISHED', '["spring data", "jpa", "interview question"]', '["Dirty checking", "Bulk update", "Hibernate first-level cache"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-first-level-cache-scope', 's-spring-jpa-qa', 'Is Hibernate''s first-level cache shared across threads, instances or pods?', 'is hibernate''s first-level cache shared across threads, instances or pods?
hibernate first-level cache
second-level cache
persistence context scope', '["Hibernate first-level cache", "Second-level cache", "Persistence context scope"]', 'No. The first-level cache is the persistence context of one EntityManager (Hibernate Session). In Spring it lives for one transaction, or one web request with open-in-view, on one thread. Sharing across threads needs the second-level cache; across pods, a distributed provider.', 'First-level cache (L1):
- It is the persistence context: an identity map from (entity type, id) to the managed object, plus snapshots for dirty checking.
- Always on and cannot be disabled.
- Scope in Spring: one per transaction, bound to the current thread. With spring.jpa.open-in-view=true (the Spring Boot default for web apps), one per HTTP request.
- Two concurrent requests in the same pod get separate caches, which is why they can both read the same old value (a lost update).
- It is not a cross-request cache: a repeated findById returns the same object only within the same persistence context.

Second-level cache (L2):
- Off by default. Scoped to the SessionFactory, so one per JVM, shared by all sessions in that pod.
- Needs a provider through JCache, such as Ehcache or Caffeine, plus @Cache on the entities.
- Holds dehydrated entity data, not objects.

Across pods: each pod has its own SessionFactory, so a local L2 cache can serve stale data after another pod writes. Use a clustered provider (Infinispan, Hazelcast, Redis through Redisson) or keep L2 for read-mostly reference data.

Hibernate invalidates the L2 regions touched by bulk @Modifying statements, but changes made outside Hibernate (another service, plain SQL) are invisible to it.', '@Transactional
public void sameTransaction(long id) {
    User a = users.findById(id).orElseThrow();   // SELECT
    User b = users.findById(id).orElseThrow();   // no SQL: served from the persistence context
    assert a == b;                               // same object
}

// Two requests, two threads, two persistence contexts:
// each runs its own SELECT and gets its own User object.

// application.yml for a per-JVM second-level cache
// spring.jpa.properties.hibernate.cache.use_second_level_cache: true
// spring.jpa.properties.hibernate.cache.region.factory_class: jcache
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class Country { @Id String code; String name; }', 'MEDIUM', 'PUBLISHED', '["hibernate", "caching", "interview question"]', '["Dirty checking", "@Modifying", "Lost update"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-transactional-lost-update', 's-spring-jpa-qa', 'Can a @Transactional service that loads an entity, changes it and lets it save still lose updates?', 'can a @transactional service that loads an entity, changes it and lets it save still lose updates?
optimistic locking
@version
pessimistic locking
@lock', '["Optimistic locking", "@Version", "Pessimistic locking", "@Lock"]', 'Yes. @Transactional gives atomic commit, not mutual exclusion. Two requests can load the same row, compute in memory, and the second flush overwrites the first. Use @Version, a PESSIMISTIC_WRITE lock, an atomic @Modifying update, or stricter isolation.', 'Under READ COMMITTED, two requests run deductMoney(wallet, 200) and deductMoney(wallet, 100):
- Both SELECT balance = 1000 into their own persistence contexts.
- One flushes update wallet set balance = 800, the other update wallet set balance = 900.
- The second waits for the first''s row lock, then overwrites it. Final balance 900 instead of 700, with no error.

Fixes:
1. Optimistic locking: add @Version. Hibernate writes where id = ? and version = ?; if 0 rows match, it throws ObjectOptimisticLockingFailureException. Retry the whole transaction, with the retry outside it. Best when conflicts are rare.
2. Pessimistic locking: @Lock(LockModeType.PESSIMISTIC_WRITE) on the finder issues SELECT ... FOR UPDATE, so the second request waits and then reads 800. Set a lock timeout (jakarta.persistence.lock.timeout) and keep the transaction short. Best for hot rows.
3. Atomic update: a @Modifying query such as set w.balance = w.balance - :amount where w.id = :id and w.balance >= :amount. Fastest, but the logic must fit in one statement.
4. Isolation: @Transactional(isolation = REPEATABLE_READ or SERIALIZABLE) on PostgreSQL makes the second writer fail with a serialization error, which you retry. MySQL REPEATABLE READ does not prevent this case.

synchronized in the service does not help: it is per JVM, and the race is just as real across pods.', '@Entity
class Wallet {
    @Id Long id;
    BigDecimal balance;
    @Version Long version;            // optimistic: update ... where id = ? and version = ?
}

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)                 // select ... for update
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findForUpdate(@Param("id") long id);

    @Modifying
    @Query("update Wallet w set w.balance = w.balance - :amount where w.id = :id and w.balance >= :amount")
    int deduct(@Param("id") long id, @Param("amount") BigDecimal amount);
}

@Transactional
public void deductMoney(long id, BigDecimal amount) {
    Wallet w = wallets.findForUpdate(id).orElseThrow();   // second caller waits here
    w.balance = w.balance.subtract(amount);
}', 'HARD', 'PUBLISHED', '["jpa", "transactions", "concurrency", "interview question"]', '["Lost update", "Serializable isolation", "Dirty checking"]', current_timestamp, current_timestamp);

insert into catalog_key (id, subcategory_id, term, search_text, aliases, short_definition, detail, java_example, difficulty, status, tags, related_terms, created_at, updated_at) values ('k-q-bulk-update', 's-spring-jpa-qa', 'Why are @Modifying queries called bulk updates, even when they change one row?', 'why are @modifying queries called bulk updates, even when they change one row?
bulk update
bulk delete
executeupdate', '["Bulk update", "Bulk delete", "executeUpdate"]', 'Because JPA runs them as set-based statements (executeUpdate) that act on whatever rows the WHERE clause matches, without loading entities. The row count does not matter; skipping the entity lifecycle is what makes it a bulk operation.', 'JPA''s term is "bulk update and delete operations" for UPDATE and DELETE statements run with Query.executeUpdate(). Compare activating 10,000 inactive users:

Entity way:
- SELECT loads 10,000 rows and builds 10,000 managed entities in the heap.
- A loop calls setStatus on each.
- Flush sends 10,000 UPDATE statements (fewer round trips with hibernate.jdbc.batch_size, but still one per row).
- Validation, @PreUpdate callbacks, @Version and cascades all run.

Bulk way:
- One statement: update User u set u.status = ''ACTIVE'' where u.status = ''INACTIVE''.
- The database does the work in place: one round trip, nothing loaded into memory, and it returns the row count.

What makes it bulk is the execution path, not the size. The same path is taken whether the WHERE clause matches 0, 1 or a million rows:
- Spring Data calls executeUpdate instead of getResultList (that is what @Modifying switches).
- The persistence context is bypassed, so managed entities can go stale (use clearAutomatically).
- Entity callbacks, cascades and automatic @Version increments do not happen. HQL''s update versioned ... increments the version explicitly.

Use bulk for set-wide changes such as status flips, counters and purging old rows. Use entity updates when per-row business rules must run.', '// Entity way: 1 select + N updates, callbacks run
@Transactional
public void activateAllOneByOne() {
    for (User u : users.findByStatus(Status.INACTIVE)) {
        u.setStatus(Status.ACTIVE);
    }
}

// Bulk way: 1 statement, returns the row count
public interface UserRepository extends JpaRepository<User, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update User u set u.status = ''ACTIVE'' where u.status = ''INACTIVE''")
    int activateAllInactive();

    @Transactional
    @Modifying
    @Query("delete from Session s where s.expiresAt < :now")
    int purgeExpired(@Param("now") Instant now);
}', 'MEDIUM', 'PUBLISHED', '["jpa", "hibernate", "performance", "interview question"]', '["@Modifying", "Dirty checking", "Hibernate first-level cache"]', current_timestamp, current_timestamp);
