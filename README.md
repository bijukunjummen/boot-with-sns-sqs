## Sample With Spring Boot Webflux and SNS/SQS (using AWS SDK 2+)

This sample demonstrates an end to end reactive application using Spring Boot 2 Webflux and reactive/non-blocking AWS SDK 2+

## Setting up a local stack version of SNS/SQS

Follow the instructions [here](https://github.com/localstack/localstack)
to setup a local version of SNS/SQS.

Start up localstack:

```
cd localstack
./start-localstack-mac.sh
# OR docker-compose up
```


## Start up application:
```
./gradlew bootRun
```


## Testing

Send a message to SNS:

```
aws --endpoint http://localhost:4575 sns publish --topic-arn arn:aws:sns:us-east-1:123456789012:sample-hello-world-topic --message hello
```

Watch the application console to see that the message has been processed


```
2020-03-20 17:55:57.497  INFO 92661 --- [pollingThread-2] sample.msg.SnsEventReceiver              : Received: [Message(MessageId=e0c0f36e-703d-4bcd-b778-ca7be2c0e60f, ReceiptHandle=e0c0f36e-703d-4bcd-b778-ca7be2c0e60f#3ddb54ad-3cd1-4b63-a535-1d6a62226268, MD5OfBody=21db2e2b56addc516325f67fbd2061ef, Body={"Message": "hello", "Type": "Notification", "TopicArn": "arn:aws:sns:us-east-1:123456789012:sample-hello-world-topic", "MessageId": "14a80e31-36d2-49a6-8d1f-8d46f122ca1f"}, Attributes={}, MessageAttributes={})]
2020-03-20 17:55:57.497  INFO 92661 --- [     parallel-1] sample.msg.MessageListenerRunner         : Processed Message hello
2020-03-20 17:55:57.528  INFO 92661 --- [     parallel-1] sample.msg.SnsEventReceiver              : Deleted queue message handle=e0c0f36e-703d-4bcd-b778-ca7be2c0e60f#3ddb54ad-3cd1-4b63-a535-1d6a6222626
``` 