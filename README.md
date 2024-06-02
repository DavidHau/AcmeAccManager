## Project Build

Execute command in terminal: `./mvnw clean package -Dmaven.test.skip=true`

## Local Rest API Testing

Swagger UI is enabled.

- http://localhost:8080/swagger-ui/index.html

## Local Test Data

Dummy accounts are initialized using Flyway.
It is recommended to use Swagger UI page to play around of the APIs.

User `2a31b993-4895-4484-9521-066f741c89b9` has the 2 accounts with 1,000,000 HKD each.

User `d25d28ae-d034-4b86-add4-0278e4d91d7f` has 1 account with 100 HKD for testing cross user money transfer.