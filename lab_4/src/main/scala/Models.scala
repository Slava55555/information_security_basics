import scala.collection.mutable

case class User(
                 username: String,
                 passwordHash: String,
                 role: String = "user" // "user" или "admin"
               ) {
  def isAdmin: Boolean = role == "admin"
}

case class AccessRights(
                         read: Boolean = false,
                         write: Boolean = false,
                         append: Boolean = false,
                         delete: Boolean = false
                       ) {
  override def toString: String = {
    val r = if (read) "R" else "-"
    val w = if (write) "W" else "-"
    val a = if (append) "A" else "-"
    val d = if (delete) "D" else "-"
    s"[$r$w$a$d]"
  }

  def hasAnyRights: Boolean = read || write || append || delete
}

class FileRecord(
                  val filename: String,
                  val owner: String
                ) {
  private val accessRights: mutable.Map[String, AccessRights] = mutable.Map.empty

  def getAccessRights(username: String): AccessRights = {
    accessRights.getOrElse(username, AccessRights())
  }

  def setAccessRights(username: String, rights: AccessRights): Unit = {
    if (rights.hasAnyRights) {
      accessRights(username) = rights
    } else {
      accessRights.remove(username)
    }
  }

  def getAllAccessRights: Map[String, AccessRights] = accessRights.toMap

  def removeAccessRights(username: String): Unit = {
    accessRights.remove(username)
  }

  override def toString: String = {
    s"File '$filename' (owner: $owner)"
  }
  
  def getFilePath(baseDir: String): String = {
    baseDir + filename
  }
}