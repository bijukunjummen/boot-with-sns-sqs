## Sample With Spring Boot Webflux and DynamoDB (using AWS SDK 2+)

This sample demonstrates an end to end reactive application using Spring Boot 2 Webflux
and reactive/non-blocking AWS SDK 2+

## Setting up a local stack version of SNS/SQS

Follow the instructions [here](https://github.com/localstack/localstack)
to setup a local version of SNS/SQS.

Start up localstack:

```
localstack start --docker
```


## Start up application:
```
./gradlew bootRun
```


## Testing

Make sure that a table has been created in dynamoDB:

```
 aws --endpoint-url=http://localhost:8000 dynamodb describe-table --table-name hotels
```

Create Hotel entities:

```
http -v :8080/hotels id=1 name=test1 address=address1 zip=zip1 state=OR
http -v :8080/hotels id=2 name=test2 address=address2 zip=zip2 state=OR
http -v :8080/hotels id=3 name=test3 address=address3 zip=zip3 state=WA
```


Get Hotels by State names:

```
http "http://localhost:8080/hotels?state=OR"
http "http://localhost:8080/hotels?state=WA"
```

Get Hotels by ID:

```
http "http://localhost:8080/hotels/1"
http "http://localhost:8080/hotels/2"
http "http://localhost:8080/hotels/3"
```

Update Hotel:
```
http PUT :8080/hotels id=1 name=test1updated address=address1 zip=zip1 state=OR
```
