object AccessControlService {
  // Проверка доступа к файлу
  def checkAccess(user: User, file: FileRecord, action: String): Boolean = {
    // Администратор имеет полный доступ ко всем файлам
    if (user.isAdmin) return true

    // Владелец имеет полный доступ к своему файлу
    if (user.username == file.owner) return true

    // Проверяем права доступа для других пользователей
    val rights = file.getAccessRights(user.username)

    val hasAccess = action match {
      case "read" => rights.read
      case "write" => rights.write
      case "append" => rights.append
      case "delete" => rights.delete
      case _ => false
    }

    if (!hasAccess) {
      println(s"✗ Отказано в доступе: у вас нет прав на выполнение действия '$action'")
      println(s"   Ваши права на этот файл: ${rights}")
    }

    hasAccess
  }

  // Проверка прав на удаление файла
  def canDeleteFile(user: User, file: FileRecord): Boolean = {
    user.isAdmin || user.username == file.owner
  }

  // Проверка прав на управление доступом
  def canManageAccess(user: User, file: FileRecord): Boolean = {
    user.isAdmin || user.username == file.owner
  }

  // Проверка существования целевого пользователя для предоставления доступа
  def validateTargetUser(owner: User, targetUsername: String, authService: AuthService): Boolean = {
    if (targetUsername == owner.username) {
      println("✗ Нельзя предоставить доступ самому себе!")
      return false
    }

    authService.getUser(targetUsername) match {
      case Some(_) => true
      case None =>
        println("✗ Пользователь не найден!")
        false
    }
  }

  // Создание объекта прав доступа из пользовательского ввода
  def createAccessRightsFromInput(): AccessRights = {
    println("\nУстановите права доступа:")
    print("Разрешить чтение (R)? [y/n]: ")
    val read = StdIn.readLine().toLowerCase == "y"
    print("Разрешить запись/перезапись (W)? [y/n]: ")
    val write = StdIn.readLine().toLowerCase == "y"
    print("Разрешить дописывание (A)? [y/n]: ")
    val append = StdIn.readLine().toLowerCase == "y"
    print("Разрешить очистку файла (D)? [y/n]: ")
    val delete = StdIn.readLine().toLowerCase == "y"

    AccessRights(read, write, append, delete)
  }

  // Форматированное отображение прав доступа
  def formatAccessRights(rights: AccessRights): String = {
    val permissions = List(
      if (rights.read) "Чтение" else "",
      if (rights.write) "Запись" else "",
      if (rights.append) "Дописывание" else "",
      if (rights.delete) "Очистка" else ""
    ).filter(_.nonEmpty)

    if (permissions.isEmpty) "нет прав"
    else permissions.mkString(", ")
  }
}