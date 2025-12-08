import java.io.{File, FileWriter, PrintWriter}
import scala.collection.mutable
import scala.io.{Source, StdIn}

class AuthService {
  private val users: mutable.Map[String, User] = mutable.Map.empty
  private val BASE_DIR = "lab_4/src/main/resources/files/"
  private val USERS_FILE = BASE_DIR + "users.csv"

  init()

  def register(): Option[User] = {
    println("\n=== РЕГИСТРАЦИЯ ===")
    print("Введите имя пользователя (3-20 символов, только буквы, цифры и _): ")
    val username = StdIn.readLine().trim

    if (!SecurityUtils.isValidUsername(username)) {
      println("Некорректное имя пользователя!")
      println("Имя должно содержать 3-20 символов (буквы, цифры, _), без пробелов")
      return None
    }

    if (users.contains(username)) {
      println("Пользователь с таким именем уже существует!")
      return None
    }

    print("Введите пароль (минимум 3 символа): ")
    val password = StdIn.readLine()

    if (!SecurityUtils.isValidPassword(password)) {
      println("Пароль должен содержать минимум 3 символа!")
      return None
    }

    val passwordHash = SecurityUtils.hashPassword(password)
    val user = User(username, passwordHash)
    users(username) = user

    saveUsersToFile()

    println(s"✓ Пользователь '$username' успешно зарегистрирован!")
    Some(user)
  }

  def login(): Option[User] = {
    println("\n=== ВХОД В СИСТЕМУ ===")
    print("Введите имя пользователя: ")
    val username = StdIn.readLine().trim

    print("Введите пароль: ")
    val password = StdIn.readLine()

    users.get(username) match {
      case Some(user) =>
        val inputHash = SecurityUtils.hashPassword(password)
        if (user.passwordHash == inputHash) {
          println(s"✓ Вход выполнен успешно! Добро пожаловать, $username")
          Some(user)
        } else {
          println("✗ Неверный пароль!")
          None
        }
      case None =>
        println("✗ Пользователь не найден!")
        None
    }
  }

  def adminLogin(): Option[User] = {
    println("\n=== ВХОД АДМИНИСТРАТОРА ===")
    print("Введите пароль администратора: ")
    val password = StdIn.readLine()

    val adminUser = users.get("admin")
    adminUser match {
      case Some(admin) =>
        val inputHash = SecurityUtils.hashPassword(password)
        if (admin.passwordHash == inputHash) {
          println("✓ Вход как администратор выполнен!")
          Some(admin)
        } else {
          println("✗ Неверный пароль администратора!")
          None
        }
      case None =>
        println("✗ Администратор не найден в системе!")
        None
    }
  }

  def getUser(username: String): Option[User] = users.get(username)

  def getAllUsers: Iterable[User] = users.values

  def userExists(username: String): Boolean = users.contains(username)

  def getUserCount: Int = users.size

  def save(): Unit = saveUsersToFile()

  private def saveUsersToFile(): Unit = {
    try {
      val writer = new PrintWriter(new FileWriter(USERS_FILE, false))
      users.values.foreach { user =>
        writer.println(s"${user.username},${user.passwordHash},${user.role}")
      }
      writer.close()
      println(s"Сохранено ${users.size} пользователей в файл")
    } catch {
      case e: Exception =>
        println(s"Ошибка при сохранении пользователей: ${e.getMessage}")
    }
  }

  private def init(): Unit = {
    new File(BASE_DIR).mkdirs()

    loadUsersFromFile()
    
    if (users.isEmpty) {
      createDefaultAdmin()
    }

    println(s"Сервис аутентификации инициализирован. Пользователей: ${users.size}")
  }

  private def createDefaultAdmin(): Unit = {
    val adminHash = SecurityUtils.hashPassword("admin123")
    val admin = User("admin", adminHash, "admin")
    users("admin") = admin
    println("Создан администратор по умолчанию (логин: admin, пароль: admin123)")
  }

  private def loadUsersFromFile(): Unit = {
    val file = new File(USERS_FILE)
    if (file.exists()) {
      try {
        val source = Source.fromFile(file, "UTF-8")
        val lines = source.getLines().toList
        source.close()

        lines.foreach { line =>
          val parts = line.split(",")
          if (parts.length == 3) {
            val username = parts(0)
            val passwordHash = parts(1)
            val role = parts(2)
            users(username) = User(username, passwordHash, role)
          }
        }
        println(s"Загружено ${lines.size} пользователей из файла")
      } catch {
        case e: Exception =>
          println(s"Ошибка при загрузке пользователей: ${e.getMessage}")
      }
    }
  }
}