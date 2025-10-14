# Компилятор для учебного языка

## Запуск через Docker:

```bash
# Собрать образ
docker build -t my-compiler .

# Запустить с тестами по умолчанию
docker run my-compiler

# Запустить со своим файлом из директории code (program.txt замените на ваш)
docker run -v ${PWD}/code:/app/code my-compiler /app/code/program.txt