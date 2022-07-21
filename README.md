# bank_fargate

- "Java HTTP" sensor must be disabled

- Environment variable QUEUE_URL must be set to hold the URL of the queue

- AWS SqsClient depends on the credentials file/execution role for the fargate

- Lambda function requires SNS_ARN variable set to the target SNS queue, please make sure to add that to the function configuration before testing


![image](https://user-images.githubusercontent.com/836297/176284390-476e90fb-20ae-4b00-97a6-e86d41b4b815.png)
