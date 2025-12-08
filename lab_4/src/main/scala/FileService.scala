import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}

class FileService(authService: AuthService) {
  // Получаем абсолютный путь к корню проекта
  private val BASE_DIR = s"lab_4/src/main/resources/files/"
  private val FILES_FILE = BASE_DIR + "files.csv"
  private val ACCESS_RIGHTS_FILE = BASE_DIR + "access_rights.csv"

  // Хранилище метаданных файлов
  private val files: mutable.Map[String, FileRecord] = mutable.Map.empty

  // Инициализация
  init()

  private def init(): Unit = {
    // Создаем директории
    ensureDirectories()

    println(s"✓ Директория для файлов: $BASE_DIR")

    // Загружаем данные из файлов
    loadAllData()

    println(s"Загружено ${files.size} файлов с метаданными")
  }

  private def ensureDirectories(): Unit = {
    val filesDir = new File(BASE_DIR)

    if (!filesDir.exists()) {
      val created = filesDir.mkdirs()
      println(s"Создана директория для файлов: ${filesDir.getAbsolutePath}")
    }
  }

  private def loadAllData(): Unit = {
    loadFilesFromFile()
    loadAccessRightsFromFile()
  }

  private def loadFilesFromFile(): Unit = {
    val file = new File(FILES_FILE)
    println(s"Пытаюсь загрузить файлы из: ${file.getAbsolutePath}")

    if (file.exists() && file.length() > 0) {
      try {
        val source = Source.fromFile(file, "UTF-8")
        var count = 0
        source.getLines().foreach { line =>
          val parts = line.split(",", -1) // -1 чтобы сохранять пустые значения
          if (parts.length >= 2) {
            val filename = parts(0)
            val owner = parts(1)

            files(filename) = new FileRecord(filename, owner)
            count += 1
            println(s"Загружен файл: $filename (владелец: $owner)")
          }
        }
        source.close()
        if (count > 0) {
          println(s"✓ Загружено $count файлов из сохранения")
        }
      } catch {
        case e: Exception =>
          println(s"✗ Ошибка при загрузке файлов: ${e.getMessage}")
          e.printStackTrace()
      }
    } else {
      println("Файл метаданных не найден или пуст")
    }
  }

  private def loadAccessRightsFromFile(): Unit = {
    val file = new File(ACCESS_RIGHTS_FILE)
    println(s"Пытаюсь загрузить права доступа из: ${file.getAbsolutePath}")

    if (file.exists() && file.length() > 0) {
      try {
        val source = Source.fromFile(file, "UTF-8")
        var count = 0
        source.getLines().foreach { line =>
          val parts = line.split(",")
          if (parts.length == 6) {
            val filename = parts(0)
            val username = parts(1)
            val read = parts(2).toBoolean
            val write = parts(3).toBoolean
            val append = parts(4).toBoolean
            val delete = parts(5).toBoolean

            files.get(filename).foreach { fileRecord =>
              val rights = AccessRights(read, write, append, delete)
              fileRecord.setAccessRights(username, rights)
              count += 1
              println(s"Загружены права: $filename -> $username: $rights")
            }
          }
        }
        source.close()
        if (count > 0) {
          println(s"✓ Загружено $count записей о правах доступа")
        }
      } catch {
        case e: Exception =>
          println(s"✗ Ошибка при загрузке прав доступа: ${e.getMessage}")
          e.printStackTrace()
      }
    } else {
      println("Файл прав доступа не найден или пуст")
    }
  }

  private def saveAllData(): Unit = {
    saveFilesToFile()
    saveAccessRightsToFile()
    println("✓ Метаданные файлов сохранены")
  }

  private def saveFilesToFile(): Unit = {
    try {
      val writer = new PrintWriter(new FileWriter(FILES_FILE, false))
      files.values.foreach { file =>
        // ТОЛЬКО метаданные: имя файла и владелец
        writer.println(s"${file.filename},${file.owner}")
      }
      writer.close()
      println(s"✓ Сохранено ${files.size} записей о файлах в $FILES_FILE")
    } catch {
      case e: Exception =>
        println(s"✗ Ошибка при сохранении файлов: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  private def saveAccessRightsToFile(): Unit = {
    try {
      val writer = new PrintWriter(new FileWriter(ACCESS_RIGHTS_FILE, false))
      var count = 0
      files.values.foreach { file =>
        file.getAllAccessRights.foreach { case (username, rights) =>
          writer.println(s"${file.filename},$username,${rights.read},${rights.write},${rights.append},${rights.delete}")
          count += 1
        }
      }
      writer.close()
      if (count > 0) {
        println(s"✓ Сохранено $count записей о правах доступа в $ACCESS_RIGHTS_FILE")
      }
    } catch {
      case e: Exception =>
        println(s"✗ Ошибка при сохранении прав доступа: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  // Автосохранение после изменений
  private def autoSave(): Unit = {
    try {
      saveAllData()
    } catch {
      case e: Exception =>
        println(s"⚠ Предупреждение: не удалось автосохранение: ${e.getMessage}")
    }
  }

  // Вспомогательный метод для получения полного пути
  private def getFullPath(filename: String): String = {
    BASE_DIR + filename
  }

  // Проверка существования файла
  private def fileExists(filename: String): Boolean = {
    val existsInMemory = files.contains(filename)
    val existsOnDisk = new File(getFullPath(filename)).exists()
    val exists = existsInMemory || existsOnDisk

    if (existsInMemory && !existsOnDisk) {
      println(s"⚠ Предупреждение: файл '$filename' есть в метаданных, но отсутствует на диске")
    }

    exists
  }

  // Получение содержимого файла с диска
  private def getFileContent(filename: String): String = {
    try {
      val path = Paths.get(getFullPath(filename))
      if (Files.exists(path)) {
        val content = new String(Files.readAllBytes(path), "UTF-8")
        content
      } else {
        println(s"Файл '$filename' не найден на диске")
        ""
      }
    } catch {
      case e: Exception =>
        println(s"Ошибка при чтении файла '$filename' с диска: ${e.getMessage}")
        ""
    }
  }

  // Создание файла
  def createFile(user: User): Unit = {
    println("\n=== СОЗДАНИЕ ФАЙЛА ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    if (!SecurityUtils.isValidFilename(filename)) {
      println("✗ Некорректное имя файла!")
      println("   Имя файла не должно содержать: / \\ : * ? \" < > |")
      return
    }

    if (fileExists(filename)) {
      println("✗ Файл с таким именем уже существует!")
      return
    }

    print("Введите начальное содержимое файла (можно оставить пустым): ")
    val initialContent = StdIn.readLine()

    // Создаем реальный файл на диске
    val fullPath = getFullPath(filename)
    val fileObj = new File(fullPath)

    try {
      // Создаем физический файл
      val writer = new PrintWriter(fileObj, "UTF-8")
      writer.write(initialContent)
      writer.close()

      // Создаем запись о файле (ТОЛЬКО метаданные)
      val fileRecord = new FileRecord(filename, user.username)
      files(filename) = fileRecord

      // Автосохранение метаданных
      autoSave()

      println(s"✓ Файл '$filename' успешно создан!")
      println(s"   Путь: ${fileObj.getAbsolutePath}")
      println(s"   Размер: ${initialContent.length} символов")
      println(s"   Владелец: ${user.username}")

      // Проверяем, что файл действительно создался
      if (fileObj.exists()) {
        println(s"   Проверка: файл существует на диске, размер: ${fileObj.length()} байт")
      } else {
        println("   ⚠ Предупреждение: файл не найден на диске после создания!")
      }

    } catch {
      case e: Exception =>
        println(s"✗ Ошибка при создании файла: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  // Чтение файла
  def readFile(user: User): Unit = {
    println("\n=== ЧТЕНИЕ ФАЙЛА ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.checkAccess(user, file, "read")) return

        // Читаем из реального файла на диске
        val content = getFileContent(filename)

        println(s"\n" + "=" * 60)
        println(s"СОДЕРЖИМОЕ ФАЙЛА: '$filename'")
        println(s"Владелец: ${file.owner}")
        println("=" * 60)
        if (content.isEmpty) {
          println("[Файл пуст]")
        } else {
          println(content)
        }
        println("=" * 60)
        println(s"Размер: ${content.length} символов")
        println("=" * 60)

      case None =>
        println("✗ Файл не найден в системе!")
        // Проверяем, может файл есть на диске, но нет в метаданных
        val diskFile = new File(getFullPath(filename))
        if (diskFile.exists()) {
          println(s"   ⚠ Файл существует на диске, но нет в метаданных системы")
          println(s"   Путь: ${diskFile.getAbsolutePath()}")
        }
    }
  }

  // Запись в файл (перезапись)
  def writeFile(user: User): Unit = {
    println("\n=== ПЕРЕЗАПИСЬ ФАЙЛА ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.checkAccess(user, file, "write")) return

        print("Введите новое содержимое файла: ")
        val newContent = StdIn.readLine()

        // Записываем в реальный файл на диске
        try {
          val writer = new PrintWriter(getFullPath(filename), "UTF-8")
          writer.write(newContent)
          writer.close()

          // НЕ обновляем метаданные (они не изменились)
          // Только владелец и имя файла остаются теми же

          // Автосохранение
          autoSave()

          println(s"✓ Файл '$filename' успешно перезаписан!")
          println(s"   Новый размер: ${newContent.length} символов")

          // Проверяем запись
          val fileObj = new File(getFullPath(filename))
          if (fileObj.exists()) {
            println(s"   Проверка: файл обновлен на диске, размер: ${fileObj.length()} байт")
          }
        } catch {
          case e: Exception =>
            println(s"✗ Ошибка при записи файла: ${e.getMessage}")
            e.printStackTrace()
        }

      case None =>
        println("✗ Файл не найден!")
    }
  }

  // Дописывание в файл
  def appendToFile(user: User): Unit = {
    println("\n=== ДОПИСЫВАНИЕ В ФАЙЛ ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.checkAccess(user, file, "append")) return

        print("Введите текст для добавления: ")
        val textToAppend = StdIn.readLine()

        if (textToAppend.isEmpty) {
          println("✗ Не указан текст для добавления!")
          return
        }

        // Дописываем в реальный файл на диске
        try {
          val path = Paths.get(getFullPath(filename))
          Files.write(path, textToAppend.getBytes("UTF-8"), StandardOpenOption.APPEND)

          // НЕ обновляем метаданные

          // Автосохранение
          autoSave()

          println(s"✓ Текст успешно добавлен в файл '$filename'!")
          println(s"   Добавлено: ${textToAppend.length} символов")

          // Показываем новый размер
          val newContent = getFileContent(filename)
          println(s"   Общий размер: ${newContent.length} символов")
        } catch {
          case e: Exception =>
            println(s"✗ Ошибка при дописывании в файл: ${e.getMessage}")
            e.printStackTrace()
        }

      case None =>
        println("✗ Файл не найден!")
    }
  }

  // Удаление содержимого файла
  def deleteFileContent(user: User): Unit = {
    println("\n=== ОЧИСТКА ФАЙЛА ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.checkAccess(user, file, "delete")) return

        print(s"Вы уверены, что хотите очистить файл '$filename'? [y/n]: ")
        val confirm = StdIn.readLine().toLowerCase == "y"

        if (!confirm) {
          println("Очистка отменена")
          return
        }

        // "Удаляем" содержимое (записываем пустую строку)
        try {
          val writer = new PrintWriter(getFullPath(filename), "UTF-8")
          writer.write("")
          writer.close()

          // Автосохранение
          autoSave()

          println(s"✓ Содержимое файла '$filename' очищено!")
        } catch {
          case e: Exception =>
            println(s"✗ Ошибка при очистке файла: ${e.getMessage}")
            e.printStackTrace()
        }

      case None =>
        println("✗ Файл не найден!")
    }
  }

  // Управление доступом к файлу
  def manageFileAccess(user: User): Unit = {
    println("\n=== УПРАВЛЕНИЕ ДОСТУПОМ К ФАЙЛУ ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.canManageAccess(user, file)) {
          println("✗ Вы не являетесь владельцем этого файла!")
          return
        }

        println(s"\nФайл: '$filename' (владелец: ${file.owner})")

        // Показать текущие права доступа
        val currentRights = file.getAllAccessRights
        if (currentRights.nonEmpty) {
          println("\nТекущие права доступа:")
          currentRights.foreach { case (username, rights) =>
            println(s"  - $username: ${AccessControlService.formatAccessRights(rights)}")
          }
        } else {
          println("\nДоступ предоставлен только владельцу")
        }

        // Меню управления доступом
        var managing = true
        while (managing) {
          println("\nДействия:")
          println("  1. Предоставить доступ новому пользователю")
          println("  2. Изменить права существующего пользователя")
          println("  3. Отозвать доступ")
          println("  4. Назад")

          print("Выберите действие: ")
          val action = StdIn.readLine().trim

          action match {
            case "1" =>
              grantAccessToFile(user, file)
              autoSave()
            case "2" =>
              modifyAccessRights(user, file)
              autoSave()
            case "3" =>
              revokeAccess(user, file)
              autoSave()
            case "4" => managing = false
            case _ => println("✗ Неверный выбор!")
          }
        }

      case None =>
        println("✗ Файл не найден!")
    }
  }

  private def grantAccessToFile(owner: User, file: FileRecord): Unit = {
    print("\nВведите имя пользователя, которому предоставляется доступ: ")
    val targetUsername = StdIn.readLine().trim

    if (targetUsername.isEmpty) {
      println("✗ Имя пользователя не может быть пустым!")
      return
    }

    if (!AccessControlService.validateTargetUser(owner, targetUsername, authService)) return

    val rights = AccessControlService.createAccessRightsFromInput()
    file.setAccessRights(targetUsername, rights)

    println(s"\n✓ Права доступа для пользователя '$targetUsername' установлены:")
    println(s"   ${AccessControlService.formatAccessRights(rights)}")
  }

  private def modifyAccessRights(owner: User, file: FileRecord): Unit = {
    print("\nВведите имя пользователя для изменения прав: ")
    val targetUsername = StdIn.readLine().trim

    if (targetUsername.isEmpty) {
      println("✗ Имя пользователя не может быть пустым!")
      return
    }

    if (!authService.userExists(targetUsername)) {
      println("✗ Пользователь не найден!")
      return
    }

    val currentRights = file.getAccessRights(targetUsername)
    if (!currentRights.hasAnyRights) {
      println(s"✗ У пользователя '$targetUsername' нет прав доступа к этому файлу")
      return
    }

    println(s"\nТекущие права пользователя '$targetUsername':")
    println(s"   ${AccessControlService.formatAccessRights(currentRights)}")

    val newRights = AccessControlService.createAccessRightsFromInput()
    file.setAccessRights(targetUsername, newRights)

    println(s"\n✓ Права доступа для пользователя '$targetUsername' обновлены:")
    println(s"   ${AccessControlService.formatAccessRights(newRights)}")
  }

  private def revokeAccess(owner: User, file: FileRecord): Unit = {
    print("\nВведите имя пользователя для отзыва доступа: ")
    val targetUsername = StdIn.readLine().trim

    if (targetUsername.isEmpty) {
      println("✗ Имя пользователя не может быть пустым!")
      return
    }

    if (!authService.userExists(targetUsername)) {
      println("✗ Пользователь не найден!")
      return
    }

    val currentRights = file.getAccessRights(targetUsername)
    if (!currentRights.hasAnyRights) {
      println(s"✗ У пользователя '$targetUsername' нет прав доступа к этому файлу")
      return
    }

    print(s"Вы уверены, что хотите отозвать доступ у пользователя '$targetUsername'? [y/n]: ")
    val confirm = StdIn.readLine().toLowerCase == "y"

    if (confirm) {
      file.removeAccessRights(targetUsername)
      println(s"✓ Доступ для пользователя '$targetUsername' отозван")
    } else {
      println("Отмена отзыва доступа")
    }
  }

  // Просмотр списка доступных файлов
  def listFiles(user: User): Unit = {
    println("\n=== ВАШИ ФАЙЛЫ ===")

    val userFiles = files.values.filter { file =>
      file.owner == user.username ||
        user.isAdmin ||
        file.getAccessRights(user.username).read
    }.toList.sortBy(_.filename)

    if (userFiles.isEmpty) {
      println("У вас пока нет доступных файлов")
      println("Создайте новый файл или попросите владельца предоставить доступ")
    } else {
      println(s"Найдено файлов: ${userFiles.size}")
      println("-" * 60)

      userFiles.foreach { file =>
        val accessType =
          if (file.owner == user.username) "📁 Ваш файл"
          else if (user.isAdmin) "👑 Доступ администратора"
          else s"🔗 Предоставлен доступ"

        val rights = file.getAccessRights(user.username)
        val rightsStr = if (rights.hasAnyRights) s" [${rights}]" else ""

        // Получаем размер файла с диска
        val fileObj = new File(getFullPath(file.filename))
        val fileSize = if (fileObj.exists()) fileObj.length() else 0
        val fileExistsStr = if (fileObj.exists()) "" else " (файл отсутствует на диске!)"

        println(s"$accessType: ${file.filename}$rightsStr$fileExistsStr")
        println(s"    Владелец: ${file.owner}, Размер: $fileSize байт")
      }
      println("-" * 60)
    }
  }

  // Удаление файла (физически)
  def deleteFile(user: User): Unit = {
    println("\n=== УДАЛЕНИЕ ФАЙЛА ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.canDeleteFile(user, file)) {
          println("✗ Вы не являетесь владельцем этого файла!")
          return
        }

        print(s"Вы уверены, что хотите УДАЛИТЬ файл '$filename'? [y/n]: ")
        val confirm = StdIn.readLine().toLowerCase == "y"

        if (!confirm) {
          println("Удаление отменена")
          return
        }

        try {
          val fileObj = new File(getFullPath(filename))
          if (fileObj.delete()) {
            files.remove(filename)

            // Автосохранение
            autoSave()

            println(s"✓ Файл '$filename' удален!")
          } else {
            println(s"✗ Ошибка при удалении файла!")
            println(s"   Путь: ${fileObj.getAbsolutePath()}")
            println(s"   Существует: ${fileObj.exists()}")
          }
        } catch {
          case e: Exception =>
            println(s"✗ Ошибка при удалении файла: ${e.getMessage}")
            e.printStackTrace()
        }

      case None =>
        println("✗ Файл не найден!")
    }
  }

  // Просмотр информации о файле
  def fileInfo(user: User): Unit = {
    println("\n=== ИНФОРМАЦИЯ О ФАЙЛЕ ===")
    print("Введите имя файла: ")
    val filename = StdIn.readLine().trim

    if (filename.isEmpty) {
      println("✗ Имя файла не может быть пустым!")
      return
    }

    files.get(filename) match {
      case Some(file) =>
        if (!AccessControlService.checkAccess(user, file, "read")) return

        val fileObj = new File(getFullPath(filename))

        println(s"\n" + "=" * 60)
        println(s"ИНФОРМАЦИЯ О ФАЙЛЕ: '$filename'")
        println("=" * 60)
        println(s"Владелец: ${file.owner}")
        println(s"Путь на диске: ${fileObj.getAbsolutePath()}")

        if (fileObj.exists()) {
          val content = getFileContent(filename)
          println(s"Размер содержимого: ${content.length} символов")
          println(s"Размер файла: ${fileObj.length()} байт")
          println(s"Последнее изменение: ${new java.util.Date(fileObj.lastModified())}")
        } else {
          println("✗ Физический файл не найден на диске!")
        }

        // Показываем, кому предоставлен доступ
        val accessRights = file.getAllAccessRights
        if (accessRights.nonEmpty) {
          println("\nДоступ предоставлен:")
          accessRights.foreach { case (username, rights) =>
            println(s"  - $username: ${AccessControlService.formatAccessRights(rights)}")
          }
        } else {
          println("\nДоступ предоставлен только владельцу")
        }

        println("=" * 60)

      case None =>
        println("✗ Файл не найден в метаданных!")
        // Проверяем на диске
        val diskFile = new File(getFullPath(filename))
        if (diskFile.exists()) {
          println("   ⚠ Файл существует на диске, но отсутствует в метаданных системы")
          println(s"   Путь: ${diskFile.getAbsolutePath()}")
          println(s"   Размер: ${diskFile.length()} байт")
        }
    }
  }

  // Отладочная информация
  def debugInfo(): Unit = {
    println(s"\n=== ОТЛАДОЧНАЯ ИНФОРМАЦИЯ FILE SERVICE ===")
    println(s"Директория файлов: $BASE_DIR")
    println(s"Файл метаданных: $FILES_FILE")
    println(s"Файл прав доступа: $ACCESS_RIGHTS_FILE")
    println(s"Файлов в памяти: ${files.size}")

    // Проверяем существование директорий
    val baseDirExists = new File(BASE_DIR).exists()
    val filesFileExists = new File(FILES_FILE).exists()
    val rightsFileExists = new File(ACCESS_RIGHTS_FILE).exists()

    println(s"Директория файлов существует: $baseDirExists")
    println(s"Файл метаданных существует: $filesFileExists")
    println(s"Файл прав доступа существует: $rightsFileExists")

    // Список файлов
    if (files.nonEmpty) {
      println("\nСписок файлов в системе:")
      files.values.foreach { file =>
        val diskFile = new File(getFullPath(file.filename))
        val existsOnDisk = diskFile.exists()
        val sizeOnDisk = if (existsOnDisk) diskFile.length() else 0
        println(s"  - ${file.filename}: владелец=${file.owner}, на диске=$existsOnDisk, размер=$sizeOnDisk байт")
      }
    }
  }

  // Принудительное сохранение всех данных
  def saveAll(): Unit = {
    println("Сохранение данных файловой системы...")
    saveAllData()
  }

  // Получить файл по имени
  def getFile(filename: String): Option[FileRecord] = files.get(filename)

  // Получить все файлы пользователя
  def getUserFiles(username: String): Iterable[FileRecord] = {
    files.values.filter(_.owner == username)
  }

  // Получить все файлы
  def getAllFiles: Iterable[FileRecord] = files.values

  // Получить количество файлов
  def getFileCount: Int = files.size
}