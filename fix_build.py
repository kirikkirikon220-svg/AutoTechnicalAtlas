#!/usr/bin/env python3

from pathlib import Path
import subprocess
import sys
import shutil

ROOT = Path.cwd()

def run(cmd, check=True):
    print(f"\n>>> {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=ROOT)
    if check and result.returncode != 0:
        print(f"\nОШИБКА: команда завершилась с кодом {result.returncode}")
        sys.exit(result.returncode)
    return result.returncode

print("=" * 60)
print("AUTO TECHNICAL ATLAS — FIX GRADLE BUILD")
print("=" * 60)

if not (ROOT / "settings.gradle.kts").exists():
    print("ОШИБКА: скрипт нужно запускать из ~/AutoTechnicalAtlas")
    print(f"Текущая папка: {ROOT}")
    sys.exit(1)

print(f"\nКорень проекта: {ROOT}")

# ---------------------------------------------------------
# 1. Проверяем Git
# ---------------------------------------------------------

if not shutil.which("git"):
    print("ОШИБКА: git не найден.")
    sys.exit(1)

run(["git", "status", "--short"], check=False)

# ---------------------------------------------------------
# 2. Проверяем Gradle
# ---------------------------------------------------------

gradle = shutil.which("gradle")

if not gradle:
    print("""
ОШИБКА: Gradle не установлен в Termux.

Но Android SDK/Gradle локально нам вообще не нужен для сборки.
GitHub Actions собирает APK самостоятельно.

Для исправления Wrapper нужен только один раз генератор Wrapper.
""")

    print("Проверяем, нет ли gradle в известных местах...")

    candidates = [
        Path.home() / "gradle" / "bin" / "gradle",
        Path("/data/data/com.termux/files/usr/bin/gradle"),
        Path("/usr/bin/gradle"),
    ]

    for candidate in candidates:
        if candidate.exists():
            gradle = str(candidate)
            print(f"Найден Gradle: {gradle}")
            break

if not gradle:
    print("""
Gradle в Termux не найден.

Сейчас установим его через Termux:

    pkg update
    pkg install gradle

После установки снова:

    cd ~/AutoTechnicalAtlas
    python3 fix_build.py
""")
    sys.exit(2)

print(f"\nGradle найден: {gradle}")

# ---------------------------------------------------------
# 3. Проверяем версию
# ---------------------------------------------------------

run([gradle, "--version"], check=False)

# ---------------------------------------------------------
# 4. Создаём Gradle Wrapper
# ---------------------------------------------------------

print("\nСоздаём Gradle Wrapper...")

run([
    gradle,
    "wrapper",
    "--gradle-version",
    "8.11.1",
    "--distribution-type",
    "bin"
])

# ---------------------------------------------------------
# 5. Проверяем файлы Wrapper
# ---------------------------------------------------------

required = [
    ROOT / "gradlew",
    ROOT / "gradlew.bat",
    ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar",
    ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties",
]

print("\nПроверка Wrapper:")

for file in required:
    if file.exists():
        print(f"OK  {file.relative_to(ROOT)}")
    else:
        print(f"FAIL {file.relative_to(ROOT)}")
        sys.exit(1)

# ---------------------------------------------------------
# 6. Делаем gradlew исполняемым
# ---------------------------------------------------------

run(["chmod", "+x", "gradlew"])

# ---------------------------------------------------------
# 7. Проверяем Gradle Wrapper
# ---------------------------------------------------------

run(["./gradlew", "--version"])

# ---------------------------------------------------------
# 8. Проверяем структуру Android-проекта
# ---------------------------------------------------------

print("\nПроверяем Android-проект...")

required_project_files = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
]

for rel in required_project_files:
    file = ROOT / rel

    if file.exists():
        print(f"OK  {rel}")
    else:
        print(f"FAIL {rel}")

# ---------------------------------------------------------
# 9. Git status
# ---------------------------------------------------------

print("\nИзменения:")

run(["git", "status", "--short"], check=False)

# ---------------------------------------------------------
# 10. Добавляем Wrapper
# ---------------------------------------------------------

run(["git", "add", "gradlew", "gradlew.bat", "gradle/wrapper"])

# ---------------------------------------------------------
# 11. Commit
# ---------------------------------------------------------

commit_message = "Fix Gradle Wrapper for GitHub Actions"

result = subprocess.run(
    ["git", "commit", "-m", commit_message],
    cwd=ROOT
)

if result.returncode != 0:
    print("\nНовый commit не создан.")
    print("Возможно, Wrapper уже был закоммичен.")
else:
    print("\nCommit создан.")

# ---------------------------------------------------------
# 12. Push
# ---------------------------------------------------------

print("\nОтправляем исправление в GitHub...")

run(["git", "push", "origin", "main"])

# ---------------------------------------------------------
# 13. Итог
# ---------------------------------------------------------

print("\n" + "=" * 60)
print("ГОТОВО")
print("=" * 60)

print("""
Gradle Wrapper добавлен в проект.

GitHub Actions теперь сможет использовать:

    ./gradlew assembleRelease

Следующая сборка Release должна автоматически запуститься
после push в main.
""")

print("Репозиторий:")
print("https://github.com/kirikkirikon220-svg/AutoTechnicalAtlas")

