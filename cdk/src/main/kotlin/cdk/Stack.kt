package cdk

import software.amazon.awscdk.Duration
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.events.EventPattern
import software.amazon.awscdk.services.events.Rule
import software.amazon.awscdk.services.events.targets.SqsQueue
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.lambda.*
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.ses.CfnTemplate
import software.amazon.awscdk.services.ses.CfnTemplateProps
import software.amazon.awscdk.services.sqs.Queue
import software.constructs.Construct
import software.amazon.awscdk.Stack as AwsStack

class Stack(scope: Construct, id: String) : AwsStack(scope, id) {

    init {

        // Create Bucket to save lambda artifacts

        val bucket = Bucket.Builder.create(this, "LambdaJarBucket")
            .build()

        // Create Table to save users

        val table = Table.Builder.create(this, "UsersTable")
            .tableName("users")
            .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
            .build()

        // Create Email Templates

        CfnTemplate(this, "WelcomeTemplate", CfnTemplateProps.builder()
            .template(CfnTemplate.TemplateProperty.builder()
                .templateName("Welcome")
                .subjectPart("Welcome, {{name}}!")
                .htmlPart("<h1>Hello {{name}},</h1><p>Welcome to Exchange App.</p>")
                .build())
            .build())

        CfnTemplate(this, "SwapTemplate", CfnTemplateProps.builder()
            .template(CfnTemplate.TemplateProperty.builder()
                .templateName("Swap")
                .subjectPart("Successful Swap")
                .htmlPart("<h1>Hello {{name}},</h1><p>Sent: {{sentAmount}}</p><p>Sent: {{receivedAmount}}</p>")
                .build())
            .build())

        // Create Queues

        val queue1 = Queue.Builder.create(this, "SendWelcomeEmailOnUserRegisteredQueue")
            .queueName("retention_send_welcome_email_on_user_registered")
            .build()

        val queue2 = Queue.Builder.create(this, "SendSwapEmailOnSwapSucceedQueue")
            .queueName("retention_save_user_info_on_user_registered")
            .build()

        val queue3 = Queue.Builder.create(this, "SendSwapEmailOnSwapSucceedQueue")
            .queueName("retention_send_swap_email_on_swap_succeed")
            .build()

        // Create EventBridge Rules for Queues

        Rule.Builder.create(this, id)
            .eventPattern(
                EventPattern.builder()
                    .source(listOf("ExchangeApi"))
                    .detailType(listOf("UserRegisteredEvent"))
                    .build()
            )
            .targets(listOf(SqsQueue(queue1), SqsQueue(queue2)))
            .build()

        Rule.Builder.create(this, id)
            .eventPattern(
                EventPattern.builder()
                    .source(listOf("ExchangeApi"))
                    .detailType(listOf("SuccessfulSwapEvent"))
                    .build()
            )
            .targets(listOf(SqsQueue(queue3)))
            .build()

        // Create Lambda Functions

        val function1 = Function.Builder.create(this, "SaveUserInfoLambda")
            .description("Function to save user info")
            .handler("handlers.save_user_info.Handler::handleRequest")
            .code(Code.fromBucket(bucket, "save-user-info/save-user-info-all.jar"))
            .environment(
                mapOf(
                    "TABLE_NAME" to table.tableName
                )
            )
            .defaultConfig()
            .build()

        table.grantWriteData(function1)
        function1.addEventSource(SqsEventSource(queue1))

        val function2 = Function.Builder.create(this, "SendWelcomeEmailLambda")
            .description("Function to send welcome email")
            .handler("handlers.send_welcome_email.Handler::handleRequest")
            .code(Code.fromBucket(bucket, "send-welcome-email/send-welcome-email-all.jar"))
            .environment(
                mapOf(
                    "SOURCE" to "kevicsalazar1994@gmail.com"
                )
            )
            .defaultConfig()
            .build()

        function2.addToRolePolicy(
            PolicyStatement.Builder.create()
                .actions(listOf("ses:*"))
                .resources(listOf("*"))
                .build()
        )

        function2.addEventSource(SqsEventSource(queue2))

        val function3 = Function.Builder.create(this, "SendSwapEmailLambda")
            .description("Function to send welcome email")
            .handler("handlers.send_swap_email.Handler::handleRequest")
            .code(Code.fromBucket(bucket, "send-swap-email/send-swap-email-all.jar"))
            .environment(
                mapOf(
                    "TABLE_NAME" to table.tableName,
                    "SOURCE" to "kevicsalazar1994@gmail.com"
                )
            )
            .defaultConfig()
            .build()

        table.grantReadData(function3)
        function3.addEventSource(SqsEventSource(queue3))
    }
}

private fun Function.Builder.defaultConfig() = runtime(Runtime.JAVA_21)
    .architecture(Architecture.X86_64)
    .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
    .memorySize(512)
    .timeout(Duration.seconds(15))!!