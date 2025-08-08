/**
 LocalStack is an AWS CDK stack designed for local development and testing.
 It simulates a production-like environment with core infrastructure components:

 - A Virtual Private Cloud (VPC) to isolate network resources.
 - Amazon RDS PostgreSQL instances for database-backed microservices.
 - Health checks to monitor database availability.
 - An Amazon MSK (Managed Streaming for Kafka) cluster for event streaming.
 - An ECS (Elastic Container Service) cluster running Fargate tasks for containerized microservices.
 - An API Gateway service to route external requests to internal microservices.

 This stack enables developers to run and test their microservices architecture
 locally or in isolated environments, mimicking production dependencies.
 */

package com.pm.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Token;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {
    /// VPC for isolating the network environment, spanning multiple Availability Zones.
    private final Vpc vpc;

    /// ECS Cluster to manage Fargate services (microservices running as containers).
    private final Cluster ecsCluster;

    /// Constructs the LocalStack, initializing and wiring all infrastructure components.
    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope, id, props);

        /// Creates the VPC with subnets across 2 AZs for high availability.
        this.vpc = createVpc();

        /// Creates PostgreSQL databases for AuthService and PatientService, each isolated.
        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDB", "auth-service-db");

        DatabaseInstance patientServiceDb =
                createDatabase("PatientServiceDB", "patient-service-db");
        /// /////////////////////////////////////////////////////////////////////////////////


        /// Creates health checks to monitor database availability.
        CfnHealthCheck authDbHealthCheck =
                createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        CfnHealthCheck patientDbHealthCheck =
                createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");
        /// ///////////////////////////////////////////////////////////////////////////////////

        /// Creates a Kafka cluster to support asynchronous event streaming.
        CfnCluster mskCluster = createMskCluster();

        /// Creates ECS cluster to run all containerized microservices.
        this.ecsCluster = createEcsCluster();

        /**
         Deploy AuthService as a Fargate container:
         - Listens on port 4005.
         - Injects JWT_SECRET as an environment variable for authentication.
         - Depends on the AuthService database and its health check.
         */
        FargateService authService =
                createFargateService("AuthService",
                        "auth-service",
                        List.of(4005),
                        authServiceDb,
                        Map.of("JWT_SECRET", "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0"));

        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);



        /**
         Deploy BillingService as a Fargate container:
         - Listens on ports 4001 (HTTP) and 9001 (gRPC).
         - No database dependency
         */
        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001,9001),
                        null,
                        null);


        /**
         Deploy AnalyticsService as a Fargate container:
          - Listens on port 4002.
          - Depends on Kafka for consuming streaming data.
         */
        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002),
                        null,
                        null);

        analyticsService.getNode().addDependency(mskCluster);

        /**
         Deploy PatientService as a Fargate container:
         - Listens on port 4000.
         - Connects to its own PostgreSQL database.
         - Configured to communicate with BillingService over gRPC using host.docker.internal.
         - Depends on BillingService, PatientService DB and its health check, and Kafka.
         */
        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                ));
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);


        /// Create API Gateway service to route and expose external requests to internal microservices.
        createApiGatewayService();
    }

    /**
     Creates a VPC with 2 availability zones for fault tolerance.
     This VPC isolates the network for all services and resources.
     */
    private Vpc createVpc(){
        return Vpc.Builder
                .create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2) /// Spread resources across 2 AZs to improve availability and resilience.
                .build();
    }

    /**
     Creates a PostgreSQL RDS database instance:
     - Version 17.2 of PostgreSQL.
     - Burstable micro instance type to save costs during development.
     - Allocated 20GB of storage.
     - Automatically generated credentials stored securely.
     - Database removed when stack is destroyed (for ephemeral environments).
     */
    private DatabaseInstance createDatabase(String id, String dbName){
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    /**
     Defines a health check for monitoring the database instance's availability.
     The check attempts to open a TCP connection to the DB endpoint at specified intervals.
     */
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP") /// Health check tests TCP port connectivity

                        .port(Token.asNumber(db.getDbInstanceEndpointPort())) /// DB's listening port

                        .ipAddress(db.getDbInstanceEndpointAddress()) /// DB endpoint IP to ping
                        .requestInterval(30) /// Interval between health check attempts in seconds
                        .failureThreshold(3) /// Mark service as unhealthy after 3 failures
                        .build())
                .build();
    }

    /**
     Creates an Amazon MSK (Managed Kafka) cluster with a single broker node:
     - Kafka version 2.8.0.
     - Broker node of instance type kafka.m5.xlarge.
     - Located within the VPC's private subnets for security.

     Kafka cluster is essential for decoupling services via event-driven architecture.
     */
    private CfnCluster createMskCluster() {
        boolean isLocal = "true".equals(System.getenv("IS_LOCALSTACK"));

        if (isLocal) {
            System.out.println("Skipping MSK cluster creation in local mode");
            return null;
        }


        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList())) /// Assigns brokers to private subnets
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    /**
     Creates an ECS Cluster inside the VPC:
     - Enables AWS Cloud Map service discovery under the namespace "patient-management.local".
     This facilitates communication between services using DNS within the cluster.
     */
    private Cluster createEcsCluster(){
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    /**
     Creates an ECS Fargate Service for a microservice:
     - Defines a Fargate task with CPU and memory allocation.
     - Configures container with image, port mappings, and logging.
     - Injects environment variables, including DB connection strings if applicable.
     - Supports additional custom environment variables.
     */
    private FargateService createFargateService(String id,
                                                String imageName,
                                                List<Integer> ports,
                                                DatabaseInstance db,
                                                Map<String, String> additionalEnvVars) {

        /// Creates Fargate task definition with 256 CPU units and 512 MB memory
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, id + "Task")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

        /// Build container definition with image, ports, and logging
        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        /** Specifies the container image to use for this container.
                        Uses a public Docker registry image identified by 'imageName' (e.g., "auth-service"). */
                        .image(ContainerImage.fromRegistry(imageName))
                        /**
                         Defines port mappings between the container and the host environment.
                         For each port in the 'ports' list, creates a PortMapping:
                         containerPort: port exposed inside the container
                         hostPort: port mapped on the host instance running the container (here, mapped 1:1)
                         protocol: TCP is specified as the communication protocol. */
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port) /// map host port to container port
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())

                        /**
                         Configures logging for the container using AWS CloudWatch Logs.
                         This helps capture container stdout/stderr logs centrally for monitoring and troubleshooting. */
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                /**
                                 Creates a new LogGroup dedicated for this container's logs.
                                 Log group name is prefixed with "/ecs/" followed by the image name for easy identification. */
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        /**
                                         Ensures the log group is automatically deleted when the stack is destroyed,
                                         which is useful for ephemeral dev/test environments. */
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        /**
                                         Logs are retained for 1 day only to reduce storage costs.
                                         Adjust retention as needed for production environments. */
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                /**
                                 Sets a log stream prefix within the log group to identify logs by service.
                                 This makes it easier to filter logs in CloudWatch. */
                                .streamPrefix(imageName)
                                .build()));

        /// Create a map to hold environment variables that will be injected into the container at runtime
        Map<String, String> envVars = new HashMap<>();

        /**
         Add Kafka bootstrap server addresses pointing to LocalStack (used for local testing of Kafka)
         These addresses allow the containerized services to connect to the local Kafka broker */
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        /**
         Merge any additional environment variables provided to this method (e.g., secrets, configs)
         This adds flexibility to inject service-specific configuration dynamically */
        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        /// If a database is associated with the service, configure DB-specific environment variables
        if (db != null) {
            /// Format the JDBC connection string using the DB endpoint, port, and a name based on the image
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName /// Usually corresponds to service name, used to form DB name
            ));
            /// Set the username for connecting to the DB; 'admin_user' is generated by CDK
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");

            /// Retrieve the password from Secrets Manager and inject it securely
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());

            /// Configure JPA to auto-update the DB schema (useful for dev; risky for prod)
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");

            /// Force Spring to always run DB initialization scripts at startup
            envVars.put("SPRING_SQL_INIT_MODE", "always");

            /// Set a longer timeout to let HikariCP wait for the DB to become available (prevents crash on cold start)
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        /// Attach all gathered environment variables to the container options
        containerOptions.environment(envVars);

        /// Add the container to the task definition using the specified image, ports, env vars, and logging.
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        /// Build and return the actual ECS Fargate Service with the task definition
        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster) /// attaches service to ECS cluster
                .taskDefinition(taskDefinition) /// sets task definition with env vars, container, etc.
                .assignPublicIp(false) /// Does not assign a public IP (keeps service private)
                .serviceName(imageName) /// sets ECS service name (usually same as image)
                .build();
    }

    private void createApiGatewayService() {
        /**
         Define a new Fargate task definition for the API Gateway service
         Allocates minimal resources: 256 CPU units and 512 MiB of memory */
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

        /** Configure container settings for the API Gateway task
            - Pulls the container image from a public registry
            - Sets environment variables required by the Spring Boot application
            - Maps port 4004 for incoming HTTP traffic
            - Sets up centralized logging via AWS CloudWatch Logs */
        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("api-gateway")) /// Use image named 'api-gateway' from Docker registry

                        /** Define environment variables:
                            - SPRING_PROFILES_ACTIVE: activates the 'prod' profile for production settings
                            - AUTH_SERVICE_URL: points to the internal Auth service (used for user authentication/authorization) */
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                        ))
                        /// Map port 4004 on both host and container, enabling API Gateway to receive traffic
                        .portMappings(List.of(4004).stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port) /// Port exposed inside the container
                                        .hostPort(port) /// Port on the ECS host
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())

                        /// Configure AWS CloudWatch log group for the container
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/api-gateway") /// log group name in CloudWatch (AWS monitoring system)
                                        .removalPolicy(RemovalPolicy.DESTROY) /// deletes logs when stack is destroyed
                                        .retention(RetentionDays.ONE_DAY) /// retains logs for 1 day
                                        .build())
                                .streamPrefix("api-gateway") /// prefix for log streams in CloudWatch (AWS monitoring system)
                                .build()))
                        .build();

        /// Attach the container to the previously created task definition
        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        /** Create an Application Load Balanced Fargate Service to expose the API Gateway externally
            - Automatically provisions an ALB (Application Load Balancer)
            - Registers the service with the ECS cluster
            - Sets desired number of tasks (replicas) to 1
            - Adds a grace period for ECS health checks after deployment */
        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                        .cluster(ecsCluster) /// ECS cluster where the service will run

                        .serviceName("api-gateway") /// ECS service name

                        .taskDefinition(taskDefinition) /// // defines the task specification (CPU, memory, container settings) for the API Gateway service

                        .desiredCount(1) /// Number of task instances

                        .healthCheckGracePeriod(Duration.seconds(60)) /// ECS waits 60 seconds before marking task unhealthy
                        .build();
    }

    public static void main(final String[] args) {
        /// Creates a new CDK application and specify the output directory for synthesized CloudFormation templates
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        /// Defines stack properties, using a bootstrapless synthesizer (avoids the need for pre-deployed CDK resources)
        StackProps props = StackProps.builder()
                /** Use BootstraplessSynthesizer to generate CloudFormation templates without requiring
                    the standard AWS bootstrap resources. This is useful for local or test environments
                    (like LocalStack) where AWS-managed assets (S3 buckets, ECR repositories) are not needed. */
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        /// Instantiate the LocalStack CDK stack, which sets up VPC, databases, ECS services, etc
        new LocalStack(app, "localstack", props);

        /// Synthesize the CDK app into a CloudFormation template
        app.synth();

        /// For the confirmation it works
        System.out.println("App synthesizing in progress...");
    }
}