import java.security.MessageDigest

object SecurityUtils {
  // Хеширование пароля
  def hashPassword(password: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(password.getBytes("UTF-8"))
    hash.map("%02x".format(_)).mkString
  }

  // Проверка корректности имени пользователя
  def isValidUsername(username: String): Boolean = {
    username != null && username.trim.nonEmpty &&
      username.length >= 3 && username.length <= 20 &&
      !username.contains(" ") && username.matches("^[a-zA-Z0-9_]+$")
  }

  // Проверка корректности пароля
  def isValidPassword(password: String): Boolean = {
    password != null && password.length >= 3
  }

  // Проверка корректности имени файла
  def isValidFilename(filename: String): Boolean = {
    filename != null && filename.trim.nonEmpty &&
      filename.length <= 100 &&
      !filename.contains("/") && !filename.contains("\\") &&
      !filename.contains(":") && !filename.contains("*") &&
      !filename.contains("?") && !filename.contains("\"") &&
      !filename.contains("<") && !filename.contains(">") &&
      !filename.contains("|")
  }
}