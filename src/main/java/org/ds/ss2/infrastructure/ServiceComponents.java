package org.ds.ss2.infrastructure;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServiceComponents {


    private static ContainerDefinitionOptions createContainerDefinitionOptions(String serviceInstance,
                                                                              LogGroup logGroup) {
        software.amazon.awscdk.services.ecs.HealthCheck healthCheck = software.amazon.awscdk.services.ecs.HealthCheck.builder()
                .command(List.of("curl localhost:8080/health"))
                .startPeriod(Duration.seconds(10))
                .interval(Duration.seconds(5))
                .timeout(Duration.seconds(2))
                .retries(3)
                .build();

        ContainerDefinitionOptions containerDefinitionOpts = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("dasmith/hello"))
                .healthCheck(healthCheck)
                .portMappings(
                        List.of(PortMapping.builder()
                                .containerPort(8080)
                                .hostPort(8080)
                                .build())
                )
                .memoryLimitMiB(512)
                .logging(AwsLogDriver.Builder.create().streamPrefix("ss2").build())
                .environment(Map.of("SERVICE_INSTANCE",serviceInstance))
                .logging(
                        LogDriver.awsLogs(
                                AwsLogDriverProps.builder()
                                        .logGroup(logGroup)
                                        .streamPrefix("svc")
                                        .build()
                        )
                )
                .build();

        return containerDefinitionOpts;
    }

    public static TaskDefinition createTaskDefinition(String basename,
                                                      Construct scope,
                                                      String serviceInstance,
                                                      LogGroup logGroup,
                                                      Role taskRole) {

        Function<String,String> makeId = (s) -> String.format("%s%s",basename,s);
        ContainerDefinitionOptions containerDefinitionOpts =
                createContainerDefinitionOptions(serviceInstance,logGroup);

        TaskDefinition helloTaskDef = TaskDefinition.Builder.create(scope, makeId.apply("hello-task"))
                .family("task")
                .compatibility(Compatibility.EC2_AND_FARGATE)
                .cpu("512")
                .memoryMiB("1024")
                .taskRole(taskRole)
                .build();

        helloTaskDef.addContainer(makeId.apply("hello-container"),containerDefinitionOpts);

        return helloTaskDef;
    }

    public static SecurityGroup createALBSecurityGroup(Construct scope, Vpc vpc) {
        SecurityGroup albSG = SecurityGroup.Builder.create(scope, "albSG")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        albSG.addIngressRule(Peer.anyIpv4(), Port.tcp(80));

        return albSG;
    }

    public static ApplicationListener createLoadBalancerAndListener(Construct scope,
                                                             Vpc vpc,
                                                             SecurityGroup albSG) {
        ApplicationLoadBalancer loadBalancer = ApplicationLoadBalancer.Builder.create(scope, "alb")
                .vpc(vpc)
                .internetFacing(true)
                .securityGroup(albSG)
                .build();

        CfnOutput.Builder.create(scope,"albdns")
                .value(loadBalancer.getLoadBalancerDnsName())
                .build();

        ApplicationListener applicationListener = loadBalancer.addListener("public-listener", BaseApplicationListenerProps.builder()
                .port(80)
                .open(true)
                .defaultAction(ListenerAction.fixedResponse(
                        400,FixedResponseOptions.builder()
                                .messageBody("Invalid or missing tenant id in query string")
                                .build()
                ))
                .build());

        return applicationListener;
    }

    public static FargateService createService(Construct scope,
                                               String basename,
                                               Cluster cluster,
                                               TaskDefinition taskDefinition,
                                               Vpc vpc,
                                               SecurityGroup albSG) {
        Function<String,String> makeId = (s) -> String.format("%s%s",basename,s);
        SecurityGroup ecsSG = SecurityGroup.Builder.create(scope, makeId.apply("ecsSG"))
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        ecsSG.addIngressRule(albSG, Port.allTcp());

        FargateService fargateService = FargateService.Builder.create(scope, makeId.apply("hs"))
                .serviceName(makeId.apply("hellosvc"))
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .securityGroups(List.of(ecsSG))
                .assignPublicIp(true)
                .build();

        return fargateService;
    }

}

