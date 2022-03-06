package org.ds.ss2.infrastructure;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
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


        TaskDefinition helloTask = ServiceComponents.createTaskDefinition(
                "s1", this,"A",logGroup, taskRole
        );
        ServiceComponents.instantiateService("s1",this,helloTask,vpc,cluster);

    }
}
