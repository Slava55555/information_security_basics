#!/bin/bash

PROJECT_ROOT="lab_4"
SRC_DIR="$PROJECT_ROOT/src/main/scala"
CLASS_DIR="$PROJECT_ROOT/target/classes"
MAIN_CLASS="Main"

check_installation() {
    echo "🔍 Проверка зависимостей..."

    # Проверка Java
    if ! command -v java &> /dev/null; then
        echo "❌ Java не установлена!"
        echo "Установите Java JDK: Ubuntu/Debian: sudo apt install openjdk-11-jdk"
        exit 1
    else
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
        echo "✅ Java установлена: $JAVA_VERSION"
    fi

    # Проверка Scala
    if ! command -v scalac &> /dev/null; then
        echo "❌ Scala не установлена!"
        echo "Установите Scala: скачайте с https://www.scala-lang.org/download/"
        exit 1
    else
        SCALA_VERSION=$(scala -version 2>&1 | head -1 | cut -d' ' -f5)
        echo "✅ Scala установлена: $SCALA_VERSION"
    fi

    echo ""
}

check_project_structure() {
    echo "📁 Проверка структуры проекта..."

    if [ ! -d "$SRC_DIR" ]; then
        echo "❌ Директория с исходниками не найдена: $SRC_DIR"
        echo "Создайте структуру:"
        echo "  mkdir -p $SRC_DIR"
        echo "  mv *.scala $SRC_DIR/"
        exit 1
    fi

    echo "🔍 Поиск Scala файлов в $SRC_DIR..."
    SCALA_FILES=$(find "$SRC_DIR" -name "*.scala" 2>/dev/null | tr '\n' ' ')

    if [ -z "$SCALA_FILES" ]; then
        echo "❌ Не найдены Scala файлы!"
        echo "Доступные файлы:"
        find "$PROJECT_ROOT" -name "*.scala" 2>/dev/null || echo "Файлы .scala не найдены"
        exit 1
    fi

    echo "📦 Найдены файлы:"
    echo "$SCALA_FILES" | tr ' ' '\n'
    echo ""
}

compile_project() {
    echo "🧹 Очистка предыдущей сборки..."
    rm -rf "$CLASS_DIR"

    echo "🏗️  Компиляция..."
    mkdir -p "$CLASS_DIR"

    # Компилируем с выводом прогресса
    echo "Выполняю: scalac -d \"$CLASS_DIR\" [файлы]"
    scalac -d "$CLASS_DIR" $SCALA_FILES

    if [ $? -ne 0 ]; then
        echo "❌ Ошибка компиляции!"
        echo "Несовместимость версий Scala"
        exit 1
    fi

    echo "✅ Компиляция успешна!"
    echo ""
}

run_project() {
    echo "🚀 Запуск $MAIN_CLASS..."
    echo "========================================="

    if [ ! -f "$CLASS_DIR/$MAIN_CLASS.class" ]; then
        echo "❌ Главный класс не найден: $CLASS_DIR/$MAIN_CLASS.class"
        exit 1
    fi

    scala -cp "$CLASS_DIR" "$MAIN_CLASS"

    EXIT_CODE=$?
    echo "========================================="

    if [ $EXIT_CODE -eq 0 ]; then
        echo "✅ Программа завершилась успешно"
    else
        echo "⚠️  Программа завершилась с кодом: $EXIT_CODE"
    fi

    return $EXIT_CODE
}

# Функция установки недостающих компонентов (опционально)
offer_installation() {
    echo ""
    echo "📦 Установить недостающие компоненты автоматически?"
    echo "   (только для Ubuntu/Debian)"
    read -p "   [y/N]: " -n 1 -r
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Установка..."
        sudo apt update
        sudo apt install -y openjdk-11-jdk scala
        echo "✅ Установка завершена!"
        echo "Перезапустите скрипт"
    else
        echo "❌ Установка отменена"
        exit 1
    fi
}

# Главная функция
main() {
    clear
    echo "========================================="
    echo "   🚀 Запуск Scala проекта"
    echo "========================================="

    if ! command -v java &> /dev/null || ! command -v scalac &> /dev/null; then
        echo "⚠️  Обнаружены отсутствующие зависимости"
        offer_installation
    fi

    check_installation
    check_project_structure
    compile_project
    run_project
}

main