package org.ds.ss2.infrastructure;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.QueryStringCondition;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
import java.util.Map;

public class Ss2Stack extends Stack {
    public Ss2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Ss2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "hello-vpc")
                .maxAzs(3)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "hello-cluster")
                .vpc(vpc)
                .build();

        LogGroup logGroup = LogGroup.Builder.create(this, "ecs-service-log-group")
                .retention(RetentionDays.TWO_WEEKS)
                .build();

        Role taskRole = IamComponents.createTaskIamRole(this);

        SecurityGroup albSG = ServiceComponents.createALBSecurityGroup(this, vpc);
        ApplicationListener listener = ServiceComponents.createLoadBalancerAndListener(
                this, vpc, albSG
        );

        TaskDefinition helloTaskA = ServiceComponents.createTaskDefinition(
                "t1", this,"A",logGroup, taskRole
        );

        TaskDefinition helloTaskB = ServiceComponents.createTaskDefinition(
                "t2", this,"B",logGroup, taskRole
        );
        //ServiceComponents.instantiateService("s1",this,helloTask,vpc,cluster);

        FargateService serviceA = ServiceComponents.createService(
                this, "s1", cluster, helloTaskA, vpc, albSG
        );

        FargateService serviceB = ServiceComponents.createService(
                this, "s2", cluster, helloTaskB, vpc, albSG
        );

        listener.addTargets("t1", AddApplicationTargetsProps.builder()
                .port(8080)
                .targets(List.of(serviceA, serviceB))
                .conditions(List.of(
                        ListenerCondition.queryStrings(
                                List.of(QueryStringCondition.builder()
                                        .key("tenant")
                                        .value("foo")
                                        .build()
                                ))
                ))
                .priority(1)
                .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                        .path("/health")
                        .protocol(Protocol.HTTP)
                        .build())
                .build());

    }
}
