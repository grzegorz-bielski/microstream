package slick.generated
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(AkkaProjectionOffsetStore.schema, Channel.schema, EventJournal.schema, EventTag.schema, FlywaySchemaHistory.schema, Snapshot.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table AkkaProjectionOffsetStore
   *  @param projectionName Database column projection_name SqlType(varchar), Length(255,true)
   *  @param projectionKey Database column projection_key SqlType(varchar), Length(255,true)
   *  @param currentOffset Database column current_offset SqlType(varchar), Length(255,true)
   *  @param manifest Database column manifest SqlType(varchar), Length(4,true)
   *  @param mergeable Database column mergeable SqlType(bool)
   *  @param lastUpdated Database column last_updated SqlType(int8) */
  case class AkkaProjectionOffsetStoreRow(projectionName: String, projectionKey: String, currentOffset: String, manifest: String, mergeable: Boolean, lastUpdated: Long)
  /** GetResult implicit for fetching AkkaProjectionOffsetStoreRow objects using plain SQL queries */
  implicit def GetResultAkkaProjectionOffsetStoreRow(implicit e0: GR[String], e1: GR[Boolean], e2: GR[Long]): GR[AkkaProjectionOffsetStoreRow] = GR{
    prs => import prs._
    AkkaProjectionOffsetStoreRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[Boolean], <<[Long]))
  }
  /** Table description of table akka_projection_offset_store. Objects of this class serve as prototypes for rows in queries. */
  class AkkaProjectionOffsetStore(_tableTag: Tag) extends profile.api.Table[AkkaProjectionOffsetStoreRow](_tableTag, "akka_projection_offset_store") {
    def * = (projectionName, projectionKey, currentOffset, manifest, mergeable, lastUpdated) <> (AkkaProjectionOffsetStoreRow.tupled, AkkaProjectionOffsetStoreRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(projectionName), Rep.Some(projectionKey), Rep.Some(currentOffset), Rep.Some(manifest), Rep.Some(mergeable), Rep.Some(lastUpdated))).shaped.<>({r=>import r._; _1.map(_=> AkkaProjectionOffsetStoreRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column projection_name SqlType(varchar), Length(255,true) */
    val projectionName: Rep[String] = column[String]("projection_name", O.Length(255,varying=true))
    /** Database column projection_key SqlType(varchar), Length(255,true) */
    val projectionKey: Rep[String] = column[String]("projection_key", O.Length(255,varying=true))
    /** Database column current_offset SqlType(varchar), Length(255,true) */
    val currentOffset: Rep[String] = column[String]("current_offset", O.Length(255,varying=true))
    /** Database column manifest SqlType(varchar), Length(4,true) */
    val manifest: Rep[String] = column[String]("manifest", O.Length(4,varying=true))
    /** Database column mergeable SqlType(bool) */
    val mergeable: Rep[Boolean] = column[Boolean]("mergeable")
    /** Database column last_updated SqlType(int8) */
    val lastUpdated: Rep[Long] = column[Long]("last_updated")

    /** Primary key of AkkaProjectionOffsetStore (database name akka_projection_offset_store_pkey) */
    val pk = primaryKey("akka_projection_offset_store_pkey", (projectionName, projectionKey))

    /** Index over (projectionName) (database name projection_name_index) */
    val index1 = index("projection_name_index", projectionName)
  }
  /** Collection-like TableQuery object for table AkkaProjectionOffsetStore */
  lazy val AkkaProjectionOffsetStore = new TableQuery(tag => new AkkaProjectionOffsetStore(tag))

  /** Entity class storing rows of table Channel
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Length(255,true)
   *  @param createdAt Database column created_at SqlType(timestamptz) */
  case class ChannelRow(id: Long, name: String, createdAt: Option[java.sql.Timestamp])
  /** GetResult implicit for fetching ChannelRow objects using plain SQL queries */
  implicit def GetResultChannelRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[java.sql.Timestamp]]): GR[ChannelRow] = GR{
    prs => import prs._
    ChannelRow.tupled((<<[Long], <<[String], <<?[java.sql.Timestamp]))
  }
  /** Table description of table channel. Objects of this class serve as prototypes for rows in queries. */
  class Channel(_tableTag: Tag) extends profile.api.Table[ChannelRow](_tableTag, "channel") {
    def * = (id, name, createdAt) <> (ChannelRow.tupled, ChannelRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), createdAt)).shaped.<>({r=>import r._; _1.map(_=> ChannelRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("created_at")

    /** Uniqueness Index over (name) (database name channel_name_idx) */
    val index1 = index("channel_name_idx", name, unique=true)
    /** Uniqueness Index over (name) (database name channel_name_key) */
    val index2 = index("channel_name_key", name, unique=true)
  }
  /** Collection-like TableQuery object for table Channel */
  lazy val Channel = new TableQuery(tag => new Channel(tag))

  /** Entity class storing rows of table EventJournal
   *  @param ordering Database column ordering SqlType(bigserial), AutoInc
   *  @param persistenceId Database column persistence_id SqlType(varchar), Length(255,true)
   *  @param sequenceNumber Database column sequence_number SqlType(int8)
   *  @param deleted Database column deleted SqlType(bool), Default(false)
   *  @param writer Database column writer SqlType(varchar), Length(255,true)
   *  @param writeTimestamp Database column write_timestamp SqlType(int8), Default(None)
   *  @param adapterManifest Database column adapter_manifest SqlType(varchar), Length(255,true), Default(None)
   *  @param eventSerId Database column event_ser_id SqlType(int4)
   *  @param eventSerManifest Database column event_ser_manifest SqlType(varchar), Length(255,true)
   *  @param eventPayload Database column event_payload SqlType(bytea)
   *  @param metaSerId Database column meta_ser_id SqlType(int4), Default(None)
   *  @param metaSerManifest Database column meta_ser_manifest SqlType(varchar), Length(255,true), Default(None)
   *  @param metaPayload Database column meta_payload SqlType(bytea), Default(None) */
  case class EventJournalRow(ordering: Long, persistenceId: String, sequenceNumber: Long, deleted: Boolean = false, writer: String, writeTimestamp: Option[Long] = None, adapterManifest: Option[String] = None, eventSerId: Int, eventSerManifest: String, eventPayload: Array[Byte], metaSerId: Option[Int] = None, metaSerManifest: Option[String] = None, metaPayload: Option[Array[Byte]] = None)
  /** GetResult implicit for fetching EventJournalRow objects using plain SQL queries */
  implicit def GetResultEventJournalRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Boolean], e3: GR[Option[Long]], e4: GR[Option[String]], e5: GR[Int], e6: GR[Array[Byte]], e7: GR[Option[Int]], e8: GR[Option[Array[Byte]]]): GR[EventJournalRow] = GR{
    prs => import prs._
    EventJournalRow.tupled((<<[Long], <<[String], <<[Long], <<[Boolean], <<[String], <<?[Long], <<?[String], <<[Int], <<[String], <<[Array[Byte]], <<?[Int], <<?[String], <<?[Array[Byte]]))
  }
  /** Table description of table event_journal. Objects of this class serve as prototypes for rows in queries. */
  class EventJournal(_tableTag: Tag) extends profile.api.Table[EventJournalRow](_tableTag, "event_journal") {
    def * = (ordering, persistenceId, sequenceNumber, deleted, writer, writeTimestamp, adapterManifest, eventSerId, eventSerManifest, eventPayload, metaSerId, metaSerManifest, metaPayload) <> (EventJournalRow.tupled, EventJournalRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(ordering), Rep.Some(persistenceId), Rep.Some(sequenceNumber), Rep.Some(deleted), Rep.Some(writer), writeTimestamp, adapterManifest, Rep.Some(eventSerId), Rep.Some(eventSerManifest), Rep.Some(eventPayload), metaSerId, metaSerManifest, metaPayload)).shaped.<>({r=>import r._; _1.map(_=> EventJournalRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7, _8.get, _9.get, _10.get, _11, _12, _13)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ordering SqlType(bigserial), AutoInc */
    val ordering: Rep[Long] = column[Long]("ordering", O.AutoInc)
    /** Database column persistence_id SqlType(varchar), Length(255,true) */
    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255,varying=true))
    /** Database column sequence_number SqlType(int8) */
    val sequenceNumber: Rep[Long] = column[Long]("sequence_number")
    /** Database column deleted SqlType(bool), Default(false) */
    val deleted: Rep[Boolean] = column[Boolean]("deleted", O.Default(false))
    /** Database column writer SqlType(varchar), Length(255,true) */
    val writer: Rep[String] = column[String]("writer", O.Length(255,varying=true))
    /** Database column write_timestamp SqlType(int8), Default(None) */
    val writeTimestamp: Rep[Option[Long]] = column[Option[Long]]("write_timestamp", O.Default(None))
    /** Database column adapter_manifest SqlType(varchar), Length(255,true), Default(None) */
    val adapterManifest: Rep[Option[String]] = column[Option[String]]("adapter_manifest", O.Length(255,varying=true), O.Default(None))
    /** Database column event_ser_id SqlType(int4) */
    val eventSerId: Rep[Int] = column[Int]("event_ser_id")
    /** Database column event_ser_manifest SqlType(varchar), Length(255,true) */
    val eventSerManifest: Rep[String] = column[String]("event_ser_manifest", O.Length(255,varying=true))
    /** Database column event_payload SqlType(bytea) */
    val eventPayload: Rep[Array[Byte]] = column[Array[Byte]]("event_payload")
    /** Database column meta_ser_id SqlType(int4), Default(None) */
    val metaSerId: Rep[Option[Int]] = column[Option[Int]]("meta_ser_id", O.Default(None))
    /** Database column meta_ser_manifest SqlType(varchar), Length(255,true), Default(None) */
    val metaSerManifest: Rep[Option[String]] = column[Option[String]]("meta_ser_manifest", O.Length(255,varying=true), O.Default(None))
    /** Database column meta_payload SqlType(bytea), Default(None) */
    val metaPayload: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]]("meta_payload", O.Default(None))

    /** Primary key of EventJournal (database name event_journal_pkey) */
    val pk = primaryKey("event_journal_pkey", (persistenceId, sequenceNumber))

    /** Uniqueness Index over (ordering) (database name event_journal_ordering_idx) */
    val index1 = index("event_journal_ordering_idx", ordering, unique=true)
  }
  /** Collection-like TableQuery object for table EventJournal */
  lazy val EventJournal = new TableQuery(tag => new EventJournal(tag))

  /** Entity class storing rows of table EventTag
   *  @param eventId Database column event_id SqlType(int8)
   *  @param tag Database column tag SqlType(varchar), Length(256,true) */
  case class EventTagRow(eventId: Long, tag: String)
  /** GetResult implicit for fetching EventTagRow objects using plain SQL queries */
  implicit def GetResultEventTagRow(implicit e0: GR[Long], e1: GR[String]): GR[EventTagRow] = GR{
    prs => import prs._
    EventTagRow.tupled((<<[Long], <<[String]))
  }
  /** Table description of table event_tag. Objects of this class serve as prototypes for rows in queries. */
  class EventTag(_tableTag: Tag) extends profile.api.Table[EventTagRow](_tableTag, "event_tag") {
    def * = (eventId, tag) <> (EventTagRow.tupled, EventTagRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(eventId), Rep.Some(tag))).shaped.<>({r=>import r._; _1.map(_=> EventTagRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_id SqlType(int8) */
    val eventId: Rep[Long] = column[Long]("event_id")
    /** Database column tag SqlType(varchar), Length(256,true) */
    val tag: Rep[String] = column[String]("tag", O.Length(256,varying=true))

    /** Primary key of EventTag (database name event_tag_pkey) */
    val pk = primaryKey("event_tag_pkey", (eventId, tag))

    /** Foreign key referencing EventJournal (database name fk_event_journal) */
    lazy val eventJournalFk = foreignKey("fk_event_journal", eventId, EventJournal)(r => r.ordering, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table EventTag */
  lazy val EventTag = new TableQuery(tag => new EventTag(tag))

  /** Entity class storing rows of table FlywaySchemaHistory
   *  @param installedRank Database column installed_rank SqlType(int4), PrimaryKey
   *  @param version Database column version SqlType(varchar), Length(50,true), Default(None)
   *  @param description Database column description SqlType(varchar), Length(200,true)
   *  @param `type` Database column type SqlType(varchar), Length(20,true)
   *  @param script Database column script SqlType(varchar), Length(1000,true)
   *  @param checksum Database column checksum SqlType(int4), Default(None)
   *  @param installedBy Database column installed_by SqlType(varchar), Length(100,true)
   *  @param installedOn Database column installed_on SqlType(timestamp)
   *  @param executionTime Database column execution_time SqlType(int4)
   *  @param success Database column success SqlType(bool) */
  case class FlywaySchemaHistoryRow(installedRank: Int, version: Option[String] = None, description: String, `type`: String, script: String, checksum: Option[Int] = None, installedBy: String, installedOn: java.sql.Timestamp, executionTime: Int, success: Boolean)
  /** GetResult implicit for fetching FlywaySchemaHistoryRow objects using plain SQL queries */
  implicit def GetResultFlywaySchemaHistoryRow(implicit e0: GR[Int], e1: GR[Option[String]], e2: GR[String], e3: GR[Option[Int]], e4: GR[java.sql.Timestamp], e5: GR[Boolean]): GR[FlywaySchemaHistoryRow] = GR{
    prs => import prs._
    FlywaySchemaHistoryRow.tupled((<<[Int], <<?[String], <<[String], <<[String], <<[String], <<?[Int], <<[String], <<[java.sql.Timestamp], <<[Int], <<[Boolean]))
  }
  /** Table description of table flyway_schema_history. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class FlywaySchemaHistory(_tableTag: Tag) extends profile.api.Table[FlywaySchemaHistoryRow](_tableTag, "flyway_schema_history") {
    def * = (installedRank, version, description, `type`, script, checksum, installedBy, installedOn, executionTime, success) <> (FlywaySchemaHistoryRow.tupled, FlywaySchemaHistoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(installedRank), version, Rep.Some(description), Rep.Some(`type`), Rep.Some(script), checksum, Rep.Some(installedBy), Rep.Some(installedOn), Rep.Some(executionTime), Rep.Some(success))).shaped.<>({r=>import r._; _1.map(_=> FlywaySchemaHistoryRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column installed_rank SqlType(int4), PrimaryKey */
    val installedRank: Rep[Int] = column[Int]("installed_rank", O.PrimaryKey)
    /** Database column version SqlType(varchar), Length(50,true), Default(None) */
    val version: Rep[Option[String]] = column[Option[String]]("version", O.Length(50,varying=true), O.Default(None))
    /** Database column description SqlType(varchar), Length(200,true) */
    val description: Rep[String] = column[String]("description", O.Length(200,varying=true))
    /** Database column type SqlType(varchar), Length(20,true)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type", O.Length(20,varying=true))
    /** Database column script SqlType(varchar), Length(1000,true) */
    val script: Rep[String] = column[String]("script", O.Length(1000,varying=true))
    /** Database column checksum SqlType(int4), Default(None) */
    val checksum: Rep[Option[Int]] = column[Option[Int]]("checksum", O.Default(None))
    /** Database column installed_by SqlType(varchar), Length(100,true) */
    val installedBy: Rep[String] = column[String]("installed_by", O.Length(100,varying=true))
    /** Database column installed_on SqlType(timestamp) */
    val installedOn: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("installed_on")
    /** Database column execution_time SqlType(int4) */
    val executionTime: Rep[Int] = column[Int]("execution_time")
    /** Database column success SqlType(bool) */
    val success: Rep[Boolean] = column[Boolean]("success")

    /** Index over (success) (database name flyway_schema_history_s_idx) */
    val index1 = index("flyway_schema_history_s_idx", success)
  }
  /** Collection-like TableQuery object for table FlywaySchemaHistory */
  lazy val FlywaySchemaHistory = new TableQuery(tag => new FlywaySchemaHistory(tag))

  /** Entity class storing rows of table Snapshot
   *  @param persistenceId Database column persistence_id SqlType(varchar), Length(255,true)
   *  @param sequenceNumber Database column sequence_number SqlType(int8)
   *  @param created Database column created SqlType(int8)
   *  @param snapshotSerId Database column snapshot_ser_id SqlType(int4)
   *  @param snapshotSerManifest Database column snapshot_ser_manifest SqlType(varchar), Length(255,true)
   *  @param snapshotPayload Database column snapshot_payload SqlType(bytea)
   *  @param metaSerId Database column meta_ser_id SqlType(int4), Default(None)
   *  @param metaSerManifest Database column meta_ser_manifest SqlType(varchar), Length(255,true), Default(None)
   *  @param metaPayload Database column meta_payload SqlType(bytea), Default(None) */
  case class SnapshotRow(persistenceId: String, sequenceNumber: Long, created: Long, snapshotSerId: Int, snapshotSerManifest: String, snapshotPayload: Array[Byte], metaSerId: Option[Int] = None, metaSerManifest: Option[String] = None, metaPayload: Option[Array[Byte]] = None)
  /** GetResult implicit for fetching SnapshotRow objects using plain SQL queries */
  implicit def GetResultSnapshotRow(implicit e0: GR[String], e1: GR[Long], e2: GR[Int], e3: GR[Array[Byte]], e4: GR[Option[Int]], e5: GR[Option[String]], e6: GR[Option[Array[Byte]]]): GR[SnapshotRow] = GR{
    prs => import prs._
    SnapshotRow.tupled((<<[String], <<[Long], <<[Long], <<[Int], <<[String], <<[Array[Byte]], <<?[Int], <<?[String], <<?[Array[Byte]]))
  }
  /** Table description of table snapshot. Objects of this class serve as prototypes for rows in queries. */
  class Snapshot(_tableTag: Tag) extends profile.api.Table[SnapshotRow](_tableTag, "snapshot") {
    def * = (persistenceId, sequenceNumber, created, snapshotSerId, snapshotSerManifest, snapshotPayload, metaSerId, metaSerManifest, metaPayload) <> (SnapshotRow.tupled, SnapshotRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(persistenceId), Rep.Some(sequenceNumber), Rep.Some(created), Rep.Some(snapshotSerId), Rep.Some(snapshotSerManifest), Rep.Some(snapshotPayload), metaSerId, metaSerManifest, metaPayload)).shaped.<>({r=>import r._; _1.map(_=> SnapshotRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8, _9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column persistence_id SqlType(varchar), Length(255,true) */
    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255,varying=true))
    /** Database column sequence_number SqlType(int8) */
    val sequenceNumber: Rep[Long] = column[Long]("sequence_number")
    /** Database column created SqlType(int8) */
    val created: Rep[Long] = column[Long]("created")
    /** Database column snapshot_ser_id SqlType(int4) */
    val snapshotSerId: Rep[Int] = column[Int]("snapshot_ser_id")
    /** Database column snapshot_ser_manifest SqlType(varchar), Length(255,true) */
    val snapshotSerManifest: Rep[String] = column[String]("snapshot_ser_manifest", O.Length(255,varying=true))
    /** Database column snapshot_payload SqlType(bytea) */
    val snapshotPayload: Rep[Array[Byte]] = column[Array[Byte]]("snapshot_payload")
    /** Database column meta_ser_id SqlType(int4), Default(None) */
    val metaSerId: Rep[Option[Int]] = column[Option[Int]]("meta_ser_id", O.Default(None))
    /** Database column meta_ser_manifest SqlType(varchar), Length(255,true), Default(None) */
    val metaSerManifest: Rep[Option[String]] = column[Option[String]]("meta_ser_manifest", O.Length(255,varying=true), O.Default(None))
    /** Database column meta_payload SqlType(bytea), Default(None) */
    val metaPayload: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]]("meta_payload", O.Default(None))

    /** Primary key of Snapshot (database name snapshot_pkey) */
    val pk = primaryKey("snapshot_pkey", (persistenceId, sequenceNumber))
  }
  /** Collection-like TableQuery object for table Snapshot */
  lazy val Snapshot = new TableQuery(tag => new Snapshot(tag))
}
