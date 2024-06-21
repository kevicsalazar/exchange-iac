package cdk.stacks


import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.ec2.Vpc
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.iam.ManagedPolicy
import software.amazon.awscdk.services.iam.Role
import software.amazon.awscdk.services.iam.ServicePrincipal
import software.constructs.Construct

class ExchangeApiStack(scope: Construct, id: String) : Stack(scope, id) {

    init {

        // Deploy Image to Fargate

        val vpc = Vpc.Builder.create(this, "MyExchangeVpc")
            .maxAzs(3)
            .build()

        val cluster = Cluster.Builder.create(this, "MyExchangeCluster")
            .vpc(vpc)
            .build()

        val executionRole = Role.Builder.create(this, "ExchangeTaskExecutionRole")
            .assumedBy(ServicePrincipal("ecs-tasks.amazonaws.com"))
            .managedPolicies(
                listOf(
                    ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")
                )
            )
            .build()

        val taskDefinition = FargateTaskDefinition.Builder.create(this, "MyExchangeFargateTaskDef")
            .memoryLimitMiB(512)
            .executionRole(executionRole)
            .cpu(256)
            .build()

        val repo = "891377324743.dkr.ecr.us-east-1.amazonaws.com/exchange-api"
        val tag = "build-36e7731c-c689-4f3f-a228-636812ecf9ea"

        val containerDefinitionProps = ContainerDefinitionProps.builder()
            .taskDefinition(taskDefinition)
            .image(ContainerImage.fromRegistry("$repo:$tag"))
            .memoryLimitMiB(512)
            .cpu(256)
            .portMappings(
                listOf(
                    PortMapping.builder()
                        .containerPort(8080)
                        .protocol(Protocol.TCP)
                        .build()
                )
            )
            .logging(
                LogDriver.awsLogs(
                    AwsLogDriverProps.Builder()
                        .streamPrefix("MyExchangeContainerLogs")
                        .build()
                )
            )
            .build()

        taskDefinition.addContainer("MyExchangeContainer", containerDefinitionProps)

        FargateService.Builder.create(this, "MyExchangeFargateService")
            .cluster(cluster)
            .taskDefinition(taskDefinition)
            .desiredCount(2)
            .build()
    }
}