.PHONY: up down logs ps reset

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

ps:
	docker compose ps

reset:
	docker compose down -v
	rm -rf ./docker-data/mariadb ./docker-data/i18n
