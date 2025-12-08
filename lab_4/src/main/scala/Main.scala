

object Main {
  def main(args: Array[String]): Unit = {
    println("\n" + "=" * 60)
    println("          СИСТЕМА КОНТРОЛЯ ДОСТУПА К ФАЙЛАМ")
    println("=" * 60)
    println("   Реализация дискреционной модели доступа")
    println("   • Регистрация и аутентификация пользователей")
    println("   • Создание и управление файлами")
    println("   • Предоставление и отзыв прав доступа")
    println("   • Роль администратора с полными правами")
    println("=" * 60)

    try {
      // Инициализация сервисов
      val authService = new AuthService()
      val fileService = new FileService(authService)

      var currentUser: Option[User] = None

      // Цикл входа/регистрации
      while (currentUser.isEmpty) {
        showLoginMenu()

        val command = StdIn.readLine().toLowerCase.trim

        command match {
          case "register" =>
            currentUser = authService.register()

          case "login" =>
            currentUser = authService.login()

          case "admin" =>
            currentUser = authService.adminLogin()

          case "help" =>
            showHelp()

          case "exit" =>
            saveAndExit(authService, fileService)

          case _ =>
            println("✗ Неизвестная команда! Введите 'help' для списка команд")
        }
      }

      // Запуск главного меню для авторизованного пользователя
      runMainMenu(currentUser.get, authService, fileService)

    } catch {
      case e: Exception =>
        println(s"\n✗ Критическая ошибка: ${e.getMessage}")
        println("Программа завершена с ошибкой.")
        e.printStackTrace()
    }
  }

  private def saveAndExit(authService: AuthService, fileService: FileService): Unit = {
    println("\n" + "=" * 60)
    println("СОХРАНЕНИЕ ДАННЫХ И ВЫХОД")
    println("=" * 60)

    // Сохраняем данные пользователей
    try {
      authService.save()
    } catch {
      case e: Exception =>
        println(s"⚠ Не удалось сохранить данные пользователей: ${e.getMessage}")
    }

    // Сохраняем данные файлов
    try {
      fileService.saveAll()
    } catch {
      case e: Exception =>
        println(s"⚠ Не удалось сохранить данные файлов: ${e.getMessage}")
    }

    println("\n✓ Все данные сохранены")
    println("До свидания!")
    println("=" * 60)
    System.exit(0)
  }

  private def showHelp(): Unit = {
    println("\n" + "=" * 60)
    println("СПРАВКА ПО КОМАНДАМ")
    println("=" * 60)
    println("  register  - Зарегистрировать нового пользователя")
    println("  login     - Войти под существующим пользователем")
    println("  admin     - Войти как администратор")
    println("             (пароль по умолчанию: admin123)")
    println("  help      - Показать эту справку")
    println("  exit      - Сохранить данные и выйти")
    println("=" * 60)
  }

  private def showLoginMenu(): Unit = {
    println("\n" + "=" * 40)
    println("МЕНЮ ВХОДА / РЕГИСТРАЦИИ")
    println("=" * 40)
    println("Доступные команды:")
    println("  register  - Зарегистрироваться")
    println("  login     - Войти в систему")
    println("  admin     - Войти как администратор")
    println("  help      - Справка по командам")
    println("  exit      - Выйти из программы")
    println("=" * 40)
    print("\nВведите команду: ")
  }

  private def runMainMenu(user: User, authService: AuthService, fileService: FileService): Unit = {
    var running = true

    while (running) {
      showMainMenu(user)

      try {
        val choice = StdIn.readLine().trim

        choice match {
          case "1" => fileService.createFile(user)
          case "2" => fileService.readFile(user)
          case "3" => fileService.writeFile(user)
          case "4" => fileService.appendToFile(user)
          case "5" => fileService.deleteFileContent(user)
          case "6" => fileService.manageFileAccess(user)
          case "7" => fileService.listFiles(user)
          case "8" => fileService.deleteFile(user)
          case "9" => fileService.fileInfo(user)

          case "stats" =>
            showStatistics(authService, fileService)

          case "save" =>
            println("\nСохранение данных...")
            authService.save()
            fileService.saveAll()
            println("✓ Все данные сохранены")

          case "help" =>
            showMainHelp()

          case "0" =>
            running = false
            println(s"\n" + "=" * 60)
            println(s"Выход из системы, ${user.username}!")
            println("=" * 60)
            saveAndExit(authService, fileService)

          case _ =>
            println("✗ Неверная команда! Введите число от 0 до 9, 'save', 'stats', 'help' или '0' для выхода")
        }
      } catch {
        case e: Exception =>
          println(s"\n✗ Ошибка: ${e.getMessage}")
          println("Попробуйте еще раз.")
      }
    }
  }

  private def showStatistics(authService: AuthService, fileService: FileService): Unit = {
    println("\n" + "=" * 60)
    println("СТАТИСТИКА СИСТЕМЫ")
    println("=" * 60)
    println(s"Зарегистрировано пользователей: ${authService.getUserCount}")
    println(s"Создано файлов: ${fileService.getFileCount}")

    // Показываем топ-5 пользователей по количеству файлов
    val users = authService.getAllUsers
    val userFilesCount = users.map { user =>
      (user.username, fileService.getUserFiles(user.username).size)
    }.toList.sortBy(-_._2).take(5)

    if (userFilesCount.nonEmpty) {
      println("\nТоп пользователей по количеству файлов:")
      userFilesCount.foreach { case (username, count) =>
        println(s"  $username: $count файлов")
      }
    }

    println("=" * 60)
  }

  private def showMainHelp(): Unit = {
    println("\n" + "=" * 60)
    println("СПРАВКА ПО ОСНОВНЫМ КОМАНДАМ")
    println("=" * 60)
    println("  1 - Создать новый файл")
    println("  2 - Прочитать содержимое файла")
    println("  3 - Записать в файл (полная перезапись)")
    println("  4 - Дописать текст в конец файла")
    println("  5 - Очистить содержимое файла")
    println("  6 - Управление доступом к файлу")
    println("      (предоставление, изменение, отзыв прав)")
    println("  7 - Просмотреть список доступных файлов")
    println("  8 - Удалить файл полностью")
    println("  9 - Просмотреть информацию о файле")
    println("  stats - Показать статистику системы")
    println("  save - Принудительно сохранить все данные")
    println("  help - Показать эту справку")
    println("  0 - Выйти из системы")
    println("=" * 60)
    println("Важно: Данные автоматически сохраняются после каждой операции.")
    println("=" * 60)
  }

  private def showMainMenu(user: User): Unit = {
    println("\n" + "=" * 60)
    println(s"ГЛАВНОЕ МЕНЮ | Пользователь: ${user.username}" +
      (if (user.isAdmin) " [АДМИНИСТРАТОР]" else ""))
    println("=" * 60)
    println("1. Создать новый файл")
    println("2. Прочитать файл")
    println("3. Записать в файл (перезаписать)")
    println("4. Дописать в файл")
    println("5. Очистить файл")
    println("6. Управление доступом к файлу")
    println("7. Просмотреть мои файлы")
    println("8. Удалить файл")
    println("9. Информация о файле")
    println("-" * 40)
    println("stats - Статистика системы")
    println("save  - Сохранить все данные")
    println("help  - Справка по командам")
    println("0. Выйти из системы")
    println("=" * 60)
    print("\nВыберите действие: ")
  }
}